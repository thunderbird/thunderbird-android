#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib.sh"

usage() {
  cat <<'USAGE'
Usage: report-pr.sh <pr-number> [--dry-run] [-h|--help]

Evaluate a pull request and report the result: manage the consolidated PR Sentinel
status comment and the 'pr-sentinel: needs updates' / 'pr-sentinel: ready for review'
labels, and exit non-zero when the PR is non-compliant and not exempt.

Arguments:
  <pr-number>   Pull request number to evaluate (required, numeric).

Options:
  --dry-run     Log state-changing actions without executing them.
  -h, --help    Show this help and exit.

Environment:
  GH_TOKEN, GH_REPO   Passed through to gh (required in CI).
  DRY_RUN=1           Alternative to --dry-run.
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
  exit 2;
fi
if [[ ! "$PR_NUMBER" =~ ^[0-9]+$ ]]; then
  echo "Error: <pr-number> must be numeric: $PR_NUMBER" >&2
  usage >&2
  exit 2
fi

export DRY_RUN   # read by lib.sh's gh_write

result="$("${SCRIPT_DIR}/evaluate-pr.sh" "$PR_NUMBER")"
compliant="$(jq -r '.compliant' <<<"$result")"
exempt="$(jq -r '.exempt' <<<"$result")"
exempt_reason="$(jq -r '.exempt_reason' <<<"$result")"
draft="$(jq -r '.draft' <<<"$result")"
missing_markdown="$(jq -r '.missing_markdown' <<<"$result")"
topics="$(jq -r '.topics' <<<"$result")"

# Draft PRs are skipped entirely: clear any prior comment/labels (e.g. when a ready
# PR is converted back to draft) so the cron won't escalate, then stop.
if [[ "$draft" == "true" ]]; then
  delete_status_comment "$PR_NUMBER"
  clear_sentinel_labels "$PR_NUMBER"
  echo "PR #${PR_NUMBER}: draft; skipping Sentinel checks."
  exit 0
fi

if [[ "$compliant" == "true" ]]; then
  delete_status_comment "$PR_NUMBER"
  mark_ready_for_review "$PR_NUMBER"
  echo "PR #${PR_NUMBER}: compliant; marked ready for review."
  exit 0
fi

# Non-compliant: post/refresh the consolidated comment first (feedback survives).
upsert_status_comment "$PR_NUMBER" "$(render_status_body "$missing_markdown" "$topics")"

if [[ "$exempt" == "true" ]]; then
  echo "PR #${PR_NUMBER}: non-compliant but exempt (${exempt_reason}); not labeling."
  exit 0
fi

# Swap to "needs updates" (fails loud if the label is missing) and fail the check.
mark_needs_updates "$PR_NUMBER"
echo "::error::PR #${PR_NUMBER} is not ready to merge — see the PR Sentinel comment."
exit 1
