#!/bin/bash

set -e

sha1_regex='^[a-f0-9]{40}$'
sha256_regex='^[A-Fa-f0-9]{64}$'

# Default values
workflows_path=".github/workflows"
dry_run=true
debug=false
action_has_error=false

# Parse command-line arguments
while [ "$#" -gt 0 ]; do
  case "$1" in
    --workflows-path) workflows_path=$2; shift 2;;
    --no-dry-run) dry_run=false; shift;;
    --debug) debug=true; shift;;
    *) echo "Unknown argument: $1"; exit 1;;
  esac
done

function debug() {
  if [[ "$debug" == true ]]; then
    echo "DEBUG: $*"
  fi
}

function fail() {
  echo "ERROR: $*"
  exit 1
}

function assert_uses_version() {
  local uses="$1"
  [[ "$uses" == *@* ]]
}

function assert_uses_sha() {
  local uses="$1"
  if [[ "$uses" == docker://* ]]; then
    [[ "$uses" =~ sha256:$sha256_regex ]]
  else
    local sha_part
    sha_part=$(echo "$uses" | awk -F'@' '{print $2}' | awk '{print $1}')
    [[ "$sha_part" =~ $sha1_regex ]]
  fi
}

function run_assertions() {
  local uses="$1"
  has_error=false

  debug "Processing uses=$uses"

  if assert_uses_version "$uses" && ! assert_uses_sha "$uses"; then
    local message="$uses is not pinned to a full length commit SHA."

    if [[ "$dry_run" == true ]]; then
      echo "WARNING: $message"
    else
      echo "ERROR: $message" >&2
    fi

    has_error=true
  else
    debug "$uses passed all checks."
  fi

  $has_error && return 1 || return 0
}

function check_workflow() {
  local file="$1"
  local file_has_error=false

  echo ""
  echo "Processing $file..."

  if ! grep -q "jobs:" "$file"; then
    fail "The $(basename "$file") workflow does not contain jobs."
  fi

  jobs=$(sed -n '/jobs:/,/^[^ ]/p' "$file")

  while read -r line; do
    if [[ "$line" =~ ^[[:space:]]*uses: || "$line" =~ ^[[:space:]]*-\ uses: ]]; then
        uses=$(echo "$line" | awk -F: '{print $2}' | xargs)
        run_assertions "$uses" || file_has_error=true
    fi
  done <<< "$jobs"

  $file_has_error && return 1 || echo "No issues were found in $file." && return 0
}

# Main script logic
while IFS= read -r -d '' file; do
  if [[ -f "$file" ]]; then
    check_workflow "$file" || action_has_error=true
  fi
done < <(find "$workflows_path" -type f \( -name '*.yaml' -o -name '*.yml' \) -print0)

if [[ "$dry_run" != true && "$action_has_error" == true ]]; then
  echo ""
  fail "At least one workflow contains an unpinned GitHub Action version." >&2
fi

exit 0
