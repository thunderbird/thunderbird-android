#!/usr/bin/env bash
set -uo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${DIR}/tests/_asserts.sh"
source "${DIR}/checks.sh"

test_escalated_marker() { # already-escalated body must be detected so we notify only once
  local body
  body=$'banner\n<!-- pr-sentinel-escalated -->\n<!-- pr-sentinel-status-comment -->'
  assert_contains "$body" "pr-sentinel-escalated" "escalated marker detected"
}

test_decide_escalation() { # boundaries just under each threshold
  assert_eq "$(decide_escalation 86399)"  "none"     "<24h -> none"
  assert_eq "$(decide_escalation 259199)" "escalate" "<72h -> escalate"
}

main() {
  test_escalated_marker
  test_decide_escalation

  if [[ "$fail" -ne 0 ]]; then echo "TESTS FAILED"; exit 1; fi
  echo "ALL TESTS PASSED"
}

main
