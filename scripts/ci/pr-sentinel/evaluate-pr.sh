#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/checks.sh"

usage() {
  cat <<'USAGE'
Usage: evaluate-pr.sh <pr-number> [-h|--help]

Evaluate a pull request against the PR Sentinel mandatory checks and print a JSON
verdict to stdout: {compliant, exempt, exempt_reason, missing_markdown}.

Arguments:
  <pr-number>            Pull request number to evaluate (required, numeric).

Options:
  -h, --help             Show this help and exit.

Environment:
  GH_TOKEN, GH_REPO      Passed through to gh.
  PR_JSON, COMMITS_JSON  Optional fixtures; when set, the API is not called (used by tests).
USAGE
}

PR_NUMBER=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    -*) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
    *)
      if [[ -n "$PR_NUMBER" ]]; then echo "Unexpected argument: $1" >&2; usage >&2; exit 2; fi
      PR_NUMBER="$1"
      ;;
  esac
  shift
done
if [[ -z "$PR_NUMBER" ]]; then echo "Error: <pr-number> is required." >&2; usage >&2; exit 2; fi
if [[ ! "$PR_NUMBER" =~ ^[0-9]+$ ]]; then echo "Error: <pr-number> must be numeric: $PR_NUMBER" >&2; exit 2; fi

PR_JSON="${PR_JSON:-$(gh api "repos/{owner}/{repo}/pulls/${PR_NUMBER}")}"
COMMITS_JSON="${COMMITS_JSON:-$(gh api --paginate "repos/{owner}/{repo}/pulls/${PR_NUMBER}/commits")}"

title="$(jq -r '.title // ""' <<<"$PR_JSON")"
body="$(jq -r '.body // ""' <<<"$PR_JSON")"
author_type="$(jq -r '.user.type // ""' <<<"$PR_JSON")"
draft="$(jq -r '.draft // false' <<<"$PR_JSON")"

missing=()
# Deduped set of doc-link topics, one per kind of failing check. render_status_body
# maps these keys to the matching "How to fix" links so only relevant links are shown.
topics=""
add_topic() { case " $topics " in *" $1 "*) ;; *) topics="${topics}${topics:+ }$1" ;; esac; }

m="$(check_title "$title")";        if [[ -n "$m" ]]; then missing+=("$m"); add_topic commit; fi
m="$(check_linked_issue "$body")";  if [[ -n "$m" ]]; then missing+=("$m"); add_topic issue; fi
m="$(check_ai_disclosure "$body")"; if [[ -n "$m" ]]; then missing+=("$m"); add_topic workflow; fi

merge_shas=()
while IFS= read -r commit; do
  [[ -z "$commit" ]] && continue
  sha="$(jq -r '.sha[0:7]' <<<"$commit")"
  parents="$(jq -r '.parents | length' <<<"$commit")"
  if (( parents >= 2 )); then merge_shas+=("$sha"); continue; fi   # merge commit: flag below, skip format checks
  message="$(jq -r '.commit.message' <<<"$commit")"
  subject="${message%%$'\n'*}"
  m="$(check_commit_subject "$sha" "$subject")";  if [[ -n "$m" ]]; then missing+=("$m"); add_topic commit; fi
  m="$(check_commit_coauthor "$sha" "$message")"; if [[ -n "$m" ]]; then missing+=("$m"); add_topic workflow; fi
done < <(jq -c '.[]' <<<"$COMMITS_JSON")

# Merge commits are not allowed: ask the author to rebase onto the base branch.
if [[ ${#merge_shas[@]} -gt 0 ]]; then
  merge_list=""
  for s in "${merge_shas[@]}"; do merge_list="${merge_list:+$merge_list, }\`${s}\`"; done
  missing+=("Merge commit(s) found (${merge_list}) — rebase onto the base branch instead of merging.")
  add_topic workflow
fi

if [[ ${#missing[@]} -eq 0 ]]; then compliant=true; else compliant=false; fi

exempt_reason="$(is_exempt "$author_type")"
if [[ -n "$exempt_reason" ]]; then exempt=true; else exempt=false; fi

missing_markdown=""
if [[ ${#missing[@]} -gt 0 ]]; then
  for item in "${missing[@]}"; do missing_markdown+="- [ ] ${item}"$'\n'; done
fi

jq -n \
  --argjson compliant "$compliant" \
  --argjson exempt "$exempt" \
  --arg exempt_reason "$exempt_reason" \
  --argjson draft "$draft" \
  --arg missing_markdown "$missing_markdown" \
  --arg topics "$topics" \
  '{compliant:$compliant, exempt:$exempt, exempt_reason:$exempt_reason, draft:$draft, missing_markdown:$missing_markdown, topics:$topics}'
