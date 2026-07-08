#!/usr/bin/env bash
# Pure PR Sentinel check functions. No network, no side effects.
# Each check_* echoes "" on pass, or a single-line failure message.
#
# Backticks in the messages below are literal Markdown for the PR comment, not
# command substitution — single quotes are intentional.
# shellcheck disable=SC2016

COMMIT_MESSAGE_TYPES_REGEX='feat|fix|docs|style|refactor|perf|test|chore|build|ci|revert'
COMMIT_MESSAGE_SCOPE_REGEX='\([a-zA-Z0-9._ -/:]+\)'

check_title() {
  local title="$1"
  local pattern="^(${COMMIT_MESSAGE_TYPES_REGEX})(${COMMIT_MESSAGE_SCOPE_REGEX})?!?: .+$"
  if [[ "$title" =~ $pattern ]]; then printf ''
  else printf 'Title must follow Conventional Commits (e.g. `fix(ui): correct padding`).'; fi
}

check_linked_issue() {
  local body="$1"
  local pattern='(^|[^[:alnum:]_])(close|closes|closed|fix|fixes|fixed|resolve|resolves|resolved) #[0-9]+'
  local rc=1
  shopt -s nocasematch
  if [[ "$body" =~ $pattern ]]; then rc=0; fi
  shopt -u nocasematch
  if [[ "$rc" -eq 0 ]]; then printf ''
  else printf 'Link an issue in the description using a closing keyword (e.g. `Closes #123`).'; fi
}

check_ai_disclosure() {
  local body="$1" checked
  checked="$(printf '%s\n' "$body" | tr -d '\r' | awk '
    /^##[[:space:]]+AI Disclosure/ { in_section = 1; next }
    /^##[[:space:]]/ && in_section { in_section = 0 }
    in_section { print }
  ' | grep -cE '^[[:space:]]*[-*][[:space:]]+\[[xX]\]' || true)"
  if [[ "$checked" -eq 1 ]]; then printf ''
  elif [[ "$checked" -eq 0 ]]; then printf 'Complete the **AI Disclosure** section (tick exactly one box).'
  else printf 'AI Disclosure has %s boxes ticked; tick exactly one.' "$checked"; fi
}

check_commit_subject() {
  local sha="$1" subject="$2"
  # Group 3 captures the <description> only (type and optional scope are excluded).
  local pattern="^(${COMMIT_MESSAGE_TYPES_REGEX})(${COMMIT_MESSAGE_SCOPE_REGEX})?!?: (.+)$"
  if [[ ! "$subject" =~ $pattern ]]; then
    printf 'Commit `%s`: subject must follow Conventional Commits.' "$sha"; return
  fi
  local desc="${BASH_REMATCH[3]}"
  MAX_COMMIT_DESC_SIZE=70
  if (( ${#desc} > MAX_COMMIT_DESC_SIZE )); then
    printf 'Commit `%s`: description is %s chars (max %d).' "$sha" "${#desc}" "$MAX_COMMIT_DESC_SIZE"; return
  fi
  printf ''
}

check_commit_coauthor() {
  local sha="$1" message="$2"
  if printf '%s\n' "$message" | grep -qiE '^Co-authored-by:'; then
    printf 'Commit `%s`: remove the `Co-authored-by:` trailer.' "$sha"
  else printf ''; fi
}

is_exempt() {
  # Only bot authors are exempt; everyone else (maintainers, drafts, contributors)
  # must comply with the PR guidelines.
  if [[ "$1" == "Bot" ]]; then printf 'bot'; fi
}

decide_escalation() {
  local age="$1"
  if   (( age >= 259200 )); then printf 'close'
  elif (( age >= 86400 ));  then printf 'escalate'
  else printf 'none'; fi
}

# Convert an ISO-8601 UTC timestamp (e.g. 2020-01-01T00:00:00Z) to epoch seconds.
# Works with both GNU date (-d, on CI/Linux) and BSD date (-j -f, on macOS).
iso_to_epoch() {
  local iso="$1"
  date -u -d "$iso" +%s 2>/dev/null || date -u -j -f '%Y-%m-%dT%H:%M:%SZ' "$iso" +%s 2>/dev/null
}

# Format epoch seconds as a human-readable UTC timestamp. GNU/BSD compatible.
epoch_to_display() {
  local epoch="$1"
  date -u -d "@${epoch}" '+%Y-%m-%d %H:%M UTC' 2>/dev/null || date -u -r "${epoch}" '+%Y-%m-%d %H:%M UTC' 2>/dev/null
}
