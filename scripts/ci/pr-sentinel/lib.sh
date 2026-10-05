#!/usr/bin/env bash
set -euo pipefail

# Shared PR Sentinel helpers for comment + label + close orchestration.
# Requires: gh, jq. Env: GH_TOKEN, GH_REPO.
# These constants are consumed by scripts that source this file.
PR_SENTINEL_LABEL_NEEDS_UPDATES="pr-sentinel: needs updates"
PR_SENTINEL_LABEL_READY_FOR_REVIEW="pr-sentinel: ready for review"
PR_SENTINEL_MARKER="<!-- pr-sentinel-status-comment -->"
PR_SENTINEL_ESCALATED_MARKER="<!-- pr-sentinel-escalated -->"

# Dry-run: when enabled, state-changing gh calls are logged instead of executed.
# Enable via `DRY_RUN=1` env or the `--dry-run` flag parsed by the entry scripts.
DRY_RUN="${DRY_RUN:-0}"
is_dry_run() { [[ "$DRY_RUN" == "1" || "$DRY_RUN" == "true" ]]; }

# Wrapper for every state-changing gh invocation. In dry-run it prints the
# intended command to stderr and skips it; otherwise it runs gh (stdout muted).
gh_write() {
  if is_dry_run; then
    echo "[dry-run] would run: gh $*" >&2
    return 0
  fi
  gh "$@" >/dev/null
}

require_label_needs_updates() {
  require_label "$PR_SENTINEL_LABEL_NEEDS_UPDATES"
}

require_label_ready_for_review() {
  require_label "$PR_SENTINEL_LABEL_READY_FOR_REVIEW"
}

require_label() { # $1 label name
  local label_name=$1
  local encoded
  encoded="$(jq -rn --arg s "$label_name" '$s|@uri')"
  if ! gh api "repos/{owner}/{repo}/labels/${encoded}" >/dev/null 2>&1; then
    echo "::error::Label '${label_name}' is missing — a maintainer must create it."
    return 1
  fi
}

find_status_comment_id() {
  local pr="$1"
  gh api --paginate "repos/{owner}/{repo}/issues/${pr}/comments" \
    --jq ".[] | select(.body | contains(\"${PR_SENTINEL_MARKER}\")) | .id" | head -n1
}

get_status_comment_created_at() {
  local pr="$1"
  gh api --paginate "repos/{owner}/{repo}/issues/${pr}/comments" \
    --jq ".[] | select(.body | contains(\"${PR_SENTINEL_MARKER}\")) | .created_at" | head -n1
}

get_status_comment_body() {
  local pr="$1"
  gh api --paginate "repos/{owner}/{repo}/issues/${pr}/comments" \
    --jq ".[] | select(.body | contains(\"${PR_SENTINEL_MARKER}\")) | .body" | head -n1
}

render_status_body() {
  local missing="$1" topics="${2:-}"

  # Build the "How to fix" list from only the topics whose checks failed.
  local howto=""
  case " $topics " in *" commit "*)
    howto+="- Conventional Commit title & commit messages: [Git Commit Guide](https://github.com/thunderbird/thunderbird-android/blob/main/docs/contributing/git-commit-guide.md#-commit-message-format)"$'\n' ;;
  esac
  case " $topics " in *" issue "*)
    howto+="- Linking an issue: [Linking a pull request to an issue](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue#linking-a-pull-request-to-an-issue-using-a-keyword)"$'\n' ;;
  esac
  case " $topics " in *" workflow "*)
    howto+="- All checks & auto-close policy: [Contribution Workflow](https://github.com/thunderbird/thunderbird-android/blob/main/docs/contributing/contribution-workflow.md#-automated-pr-checks-pr-sentinel)"$'\n' ;;
  esac

  {
    echo "🤖 **PR Sentinel — this PR isn't ready to merge yet**"
    echo ""
    echo "Please address the following so a maintainer can review:"
    echo ""
    printf '%s' "$missing"
    echo ""
    if [[ -n "$howto" ]]; then
      echo ""
      echo "📖 **How to fix**"
      echo ""
      printf '%s' "$howto"
      echo ""
    fi
    echo "_This check runs automatically. Update the PR to resolve it; PRs left unresolved are auto-closed 3 days after this first notice._"
    echo ""
    echo "$PR_SENTINEL_MARKER"
  }
}

upsert_status_comment() {
  local pr="$1" body="$2" id
  id="$(find_status_comment_id "$pr")"
  if [[ -n "$id" ]]; then
    gh_write api -X PATCH "repos/{owner}/{repo}/issues/comments/${id}" -f body="$body"
  else
    gh_write api -X POST "repos/{owner}/{repo}/issues/${pr}/comments" -f body="$body"
  fi
}

delete_status_comment() {
  local pr="$1" id
  id="$(find_status_comment_id "$pr")"
  if [[ -n "$id" ]]; then
    gh_write api -X DELETE "repos/{owner}/{repo}/issues/comments/${id}"
  fi
}

add_label() { # $1=PR number, $2=label name
  local pr="$1" label_name="$2"
  gh_write api -X POST "repos/{owner}/{repo}/issues/${pr}/labels" \
    -f "labels[]=${label_name}"
}

remove_label() { # $1=PR number, $2=label name (tolerates 404 when not applied)
  local pr="$1" label_name="$2" encoded
  encoded="$(jq -rn --arg s "$label_name" '$s|@uri')"
  gh_write api -X DELETE "repos/{owner}/{repo}/issues/${pr}/labels/${encoded}" || true
}

# --- Sentinel label state transitions (the two labels are mutually exclusive) ---

# Non-compliant PR: drop "ready for review", then require + add "needs updates".
mark_needs_updates() { # $1=PR number
  local pr="$1"
  remove_label "$pr" "$PR_SENTINEL_LABEL_READY_FOR_REVIEW"
  require_label_needs_updates
  add_label "$pr" "$PR_SENTINEL_LABEL_NEEDS_UPDATES"
}

# Compliant PR: drop "needs updates", then require + add "ready for review".
mark_ready_for_review() { # $1=PR number
  local pr="$1"
  remove_label "$pr" "$PR_SENTINEL_LABEL_NEEDS_UPDATES"
  require_label_ready_for_review
  add_label "$pr" "$PR_SENTINEL_LABEL_READY_FOR_REVIEW"
}

# Draft / skipped PR: drop both Sentinel labels, add none.
clear_sentinel_labels() { # $1=PR number
  local pr="$1"
  remove_label "$pr" "$PR_SENTINEL_LABEL_NEEDS_UPDATES"
  remove_label "$pr" "$PR_SENTINEL_LABEL_READY_FOR_REVIEW"
}
