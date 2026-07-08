#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/ci/pr-sentinel/lib.sh
source "${SCRIPT_DIR}/lib.sh"
# shellcheck source=scripts/ci/pr-sentinel/checks.sh
source "${SCRIPT_DIR}/checks.sh"

usage() {
  cat <<'USAGE'
Usage: escalate-pr.sh <pr-number> [--dry-run] [-h|--help]

Escalate or close a non-compliant pull request based on how long its PR Sentinel status
comment has existed: escalate (add a close-countdown banner) after 1 day, close after 3 days.

Arguments:
  <pr-number>   Pull request number to act on (required, numeric).

Options:
  --dry-run     Log state-changing actions without executing them.
  -h, --help    Show this help and exit.

Environment:
  GH_TOKEN, GH_REPO   Passed through to gh (required in CI).
  DRY_RUN=1           Alternative to --dry-run.
  NOW_EPOCH           Override "now" (epoch seconds) for testing.
USAGE
}

PR_NUMBER=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    --dry-run) DRY_RUN=1 ;;
    -*) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
    *)
      if [[ -n "$PR_NUMBER" ]]; then echo "Unexpected argument: $1" >&2; usage >&2; exit 2; fi
      PR_NUMBER="$1"
      ;;
  esac
  shift
done

if [[ -z "$PR_NUMBER" ]]; then
  echo "Error: <pr-number> is required." >&2
  usage >&2
  exit 2
fi

if [[ ! "$PR_NUMBER" =~ ^[0-9]+$ ]]; then
  echo "Error: <pr-number> must be numeric: $PR_NUMBER" >&2
  usage >&2
  exit 2
fi
export DRY_RUN   # read by lib.sh's gh_write

NOW_EPOCH="${NOW_EPOCH:-$(date -u +%s)}"

# Print an action summary, phrased for real vs dry-run.
# $1 = wording when the action ran; $2 = wording when it was only simulated.
summary() {
  if is_dry_run; then echo "PR #${PR_NUMBER}: [dry-run] $2"; else echo "PR #${PR_NUMBER}: $1"; fi
}

# Draft PRs are skipped (defensive; report-pr.sh normally unlabels them on draft).
pr_json="$(gh api "repos/{owner}/{repo}/pulls/${PR_NUMBER}")"
if [[ "$(jq -r '.draft' <<<"$pr_json")" == "true" ]]; then
  echo "PR #${PR_NUMBER}: draft; skipping."
  exit 0
fi

created_at="$(get_status_comment_created_at "$PR_NUMBER")"
if [[ -z "$created_at" ]]; then
  echo "PR #${PR_NUMBER}: no status comment; skipping."
  exit 0
fi

t0="$(iso_to_epoch "$created_at")"             # GNU/BSD portable
age=$(( NOW_EPOCH - t0 ))
action="$(decide_escalation "$age")"
close_iso="$(epoch_to_display "$(( t0 + 259200 ))")"
author="$(jq -r '.user.login' <<<"$pr_json")"

case "$action" in
  none)
    echo "PR #${PR_NUMBER}: age ${age}s (<24h); no action."
    ;;
  escalate)
    body="$(get_status_comment_body "$PR_NUMBER")"
    if grep -q "$PR_SENTINEL_ESCALATED_MARKER" <<<"$body"; then
      echo "PR #${PR_NUMBER}: already escalated; not re-notifying."
    else
      banner="> ⚠️ @${author} — this PR will be **auto-closed on ${close_iso}** if the items below are not resolved."
      new_body="${banner}"$'\n\n'"${body}"$'\n'"${PR_SENTINEL_ESCALATED_MARKER}"
      id="$(find_status_comment_id "$PR_NUMBER")"
      gh_write api -X PATCH "repos/{owner}/{repo}/issues/comments/${id}" -f body="$new_body"
      summary "escalated (close on ${close_iso})" "would escalate (close on ${close_iso})"
    fi
    ;;
  close)
    gh_write api -X POST "repos/{owner}/{repo}/issues/${PR_NUMBER}/comments" \
      -f body="🤖 @${author} — closing this PR because the required items were not resolved within 3 days. Please address them and reopen. ${PR_SENTINEL_MARKER}"
    gh_write pr close "$PR_NUMBER"
    summary "closed" "would close"
    ;;
esac
