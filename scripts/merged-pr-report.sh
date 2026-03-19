#!/usr/bin/env bash
#
# merged-prs-report
#
# Generates monthly reports of pull requests merged into the main, beta,
# and release branches of a GitHub repository.
#
# Usage:
#   ./merged-prs-report YEAR MONTH [TARGET_DIR] [--skip-excluded]
#
# Example:
#   ./merged-prs-report 2026 02
#   ./merged-prs-report 2026 02 ./reports
#   ./merged-prs-report 2026 02 . --skip-excluded
#
# Arguments:
#   YEAR           Four-digit year (e.g. 2026)
#   MONTH          Two-digit month (01-12)
#   TARGET_DIR     (Optional) Target directory for reports (default: current directory)
#   --skip-excluded (Optional) If set, PRs with "report: exclude" label are omitted from the report
#
# Output:
#   - Markdown report: merged-prs-YEAR-MONTH.md
#   - CSV report: merged-prs-YEAR-MONTH.csv
#
# For each PR, the reports include:
#   - branch       Target branch (main, beta, release)
#   - PR           PR number and link
#   - merged       Merge date (YYYY-MM-DD)
#   - title        PR title
#   - report       Status from labels (Highlight, Include, Exclude, Review)
#   - beta         First beta tag containing the merge commit (if any)
#   - release      First release tag containing the merge commit (if any)
#
# The CSV report also includes:
#   - author       PR author
#   - sha          Merge commit SHA
#   - url          Link to the PR
#   - comment      Empty column for manual notes
#
# Requirements:
#   - git: Installed and run from within the repository
#   - gh: GitHub CLI authenticated with access to the repository
#   - jq: JSON processor installed
#   - macOS/BSD date command (uses `date -j` for date arithmetic)
#
set -Eeuo pipefail

if [[ $# -lt 2 || $# -gt 4 ]]; then
  echo "Usage: $0 YEAR MONTH [TARGET_DIR] [--skip-excluded]"
  echo "Example: $0 2026 02"
  echo "Example: $0 2026 02 ./reports"
  echo "Example: $0 2026 02 . --skip-excluded"
  exit 1
fi

YEAR="$1"
MONTH="$2"
TARGET_DIR="."
SKIP_EXCLUDED=false

# Simple argument parsing for optional TARGET_DIR and --skip-excluded
if [[ $# -ge 3 ]]; then
  if [[ "$3" == "--skip-excluded" ]]; then
    SKIP_EXCLUDED=true
  else
    TARGET_DIR="$3"
  fi
fi
if [[ $# -eq 4 ]]; then
  if [[ "$4" == "--skip-excluded" ]]; then
    SKIP_EXCLUDED=true
  else
    # If the 4th arg is not the flag, it's an error in this simple logic
    # but let's just ignore it or assume it's the target dir if $3 was something else
    :
  fi
fi

OWNER="thunderbird"
REPO="thunderbird-android"
BRANCHES=("main" "beta" "release")
DEFAULT_STATUS="Review"

# Validate input format
if [[ ! "$YEAR" =~ ^[0-9]{4}$ ]]; then
  echo "Error: YEAR must be a four-digit number (e.g. 2026)"
  exit 1
fi
if [[ ! "$MONTH" =~ ^(0[1-9]|1[0-2])$ ]]; then
  echo "Error: MONTH must be two digits (01-12)"
  exit 1
fi

# Date arithmetic for START and END dates (macOS/BSD specific)
START="${YEAR}-${MONTH}-01"
if ! END="$(date -j -v+1m -v-1d -f "%Y-%m-%d" "$START" +%Y-%m-%d 2>/dev/null)"; then
  echo "Error: Failed to calculate date range. Ensure you are on macOS or have BSD date installed."
  exit 1
fi

# Create target directory if it doesn't exist
mkdir -p "$TARGET_DIR"

MD_OUT="${TARGET_DIR}/merged-prs-${YEAR}-${MONTH}.md"
CSV_OUT="${TARGET_DIR}/merged-prs-${YEAR}-${MONTH}.csv"

# Temporary setup for git operations
TMP_REPO="$(mktemp -d)"
CACHE_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_REPO" "$CACHE_DIR"' EXIT

BETA_CACHE_FILE="$CACHE_DIR/beta_cache.txt"
RELEASE_CACHE_FILE="$CACHE_DIR/release_cache.txt"
touch "$BETA_CACHE_FILE" "$RELEASE_CACHE_FILE"

echo "Preparing temporary repo for tag analysis in $TMP_REPO..."
git init -q "$TMP_REPO"
git -C "$TMP_REPO" remote add origin "https://github.com/$OWNER/$REPO.git"

echo "Fetching tags and branch heads from origin..."
git -C "$TMP_REPO" fetch origin main beta release --tags --quiet

map_report_status() {
  local labels_json="$1"

  if jq -e '.[] | select(.name == "report: highlight")' >/dev/null <<< "$labels_json"; then
    echo "Highlight"
  elif jq -e '.[] | select(.name == "report: include")' >/dev/null <<< "$labels_json"; then
    echo "Include"
  elif jq -e '.[] | select(.name == "report: exclude")' >/dev/null <<< "$labels_json"; then
    echo "Exclude"
  else
    echo "$DEFAULT_STATUS"
  fi
}

map_version() {
  local sha="$1"
  local branch="$2"
  local pattern="$3"

  if [[ -z "$sha" ]]; then
    echo "-"
    return
  fi

  # If commit is not in the branch history, it's not applicable
  if ! git -C "$TMP_REPO" merge-base --is-ancestor "$sha" "origin/$branch" 2>/dev/null; then
    echo "-"
    return
  fi

  # Find the first tag (by version sorting) that contains this commit
  local first_tag
  first_tag="$(git -C "$TMP_REPO" tag --list "$pattern" --contains "$sha" --sort=version:refname | head -n 1)"

  if [[ -n "$first_tag" ]]; then
    echo "$first_tag"
  else
    echo "Not released yet"
  fi
}

get_cached_value() {
  local cache_file="$1"
  local sha="$2"

  local result
  result="$(awk -F '\t' -v key="$sha" '$1 == key { print $2; exit }' "$cache_file")"

  if [[ -n "$result" ]]; then
    echo "$result"
    return 0
  fi

  return 1
}

set_cached_value() {
  local cache_file="$1"
  local sha="$2"
  local value="$3"

  printf '%s\t%s\n' "$sha" "$value" >> "$cache_file"
}

escape_md() {
  printf '%s' "$1" | sed 's/|/\\|/g'
}

escape_csv() {
  local value="$1"
  value="${value//\"/\"\"}"
  printf '"%s"' "$value"
}

{
  echo "# Merged PR Report (${YEAR}-${MONTH})"
  echo
  echo "**Repository:** $OWNER/$REPO  "
  echo "**Range:** $START -> $END"
  echo
} > "$MD_OUT"

echo "Branch,Number,Merged,Author,Title,Report,Beta,Release,SHA,URL,Comment" > "$CSV_OUT"

for BRANCH in "${BRANCHES[@]}"; do
  echo "Processing $BRANCH..."

  echo "## Branch: $BRANCH" >> "$MD_OUT"
  echo >> "$MD_OUT"
  echo "| PR | Merged | Title | Report | Beta | Release |" >> "$MD_OUT"
  echo "|---|---|---|---|---|---|" >> "$MD_OUT"

  prs_json="$(gh pr list \
    --repo "$OWNER/$REPO" \
    --state merged \
    --base "$BRANCH" \
    --search "merged:$START..$END" \
    --json number,title,url,mergedAt,mergeCommit,labels,author \
    --limit 1000)"

  sorted_prs_json="$(jq 'sort_by(.mergedAt)' <<< "$prs_json")"

  if [[ "$(jq 'length' <<< "$sorted_prs_json")" -eq 0 ]]; then
    echo "| - | - | _No merged PRs in this range._ | - | - | - |" >> "$MD_OUT"
    echo >> "$MD_OUT"
    continue
  fi

  while IFS= read -r pr; do
    number="$(jq -r '.number' <<< "$pr")"
    title="$(jq -r '.title' <<< "$pr")"
    title_md="$(escape_md "$title")"
    url="$(jq -r '.url' <<< "$pr")"
    merged_at="$(jq -r '.mergedAt | split("T")[0]' <<< "$pr")"
    sha="$(jq -r '.mergeCommit.oid // empty' <<< "$pr")"
    author="$(jq -r '.author.login // "ghost"' <<< "$pr")"
    labels_json="$(jq -c '.labels // []' <<< "$pr")"
    status="$(map_report_status "$labels_json")"

    if [[ "$SKIP_EXCLUDED" == "true" && "$status" == "Exclude" ]]; then
      continue
    fi

    if [[ -n "$sha" ]]; then
      # Beta tag analysis
      if ! beta_version="$(get_cached_value "$BETA_CACHE_FILE" "$sha" 2>/dev/null)"; then
        beta_version="$(map_version "$sha" "beta" 'THUNDERBIRD_*_0b*')"
        set_cached_value "$BETA_CACHE_FILE" "$sha" "$beta_version"
      fi

      # Release tag analysis
      if ! release_version="$(get_cached_value "$RELEASE_CACHE_FILE" "$sha" 2>/dev/null)"; then
        release_version="$(map_version "$sha" "release" 'THUNDERBIRD_*_0')"
        set_cached_value "$RELEASE_CACHE_FILE" "$sha" "$release_version"
      fi
    else
      beta_version="-"
      release_version="-"
    fi

    echo "| [#$number]($url) | $merged_at | $title_md | $status | $beta_version | $release_version |" >> "$MD_OUT"

    printf '%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' \
      "$(escape_csv "$BRANCH")" \
      "$(escape_csv "$number")" \
      "$(escape_csv "$merged_at")" \
      "$(escape_csv "$author")" \
      "$(escape_csv "$title")" \
      "$(escape_csv "$status")" \
      "$(escape_csv "$beta_version")" \
      "$(escape_csv "$release_version")" \
      "$(escape_csv "$sha")" \
      "$(escape_csv "$url")" \
      "$(escape_csv "")" \
      >> "$CSV_OUT"
  done < <(jq -c '.[]' <<< "$sorted_prs_json")

  echo >> "$MD_OUT"
done

echo "Wrote Markdown report to $MD_OUT"
echo "Wrote CSV report to $CSV_OUT"
