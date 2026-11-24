#!/usr/bin/env bash
set -uo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${DIR}/tests/_asserts.sh"

# Fake `gh` that records each invocation and reports every label as existing
# (exit 0), so require_label passes without a network call.
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cat >"${tmp}/gh" <<'EOF'
#!/usr/bin/env bash
echo "gh $*" >>"$GH_CALL_LOG"
EOF
chmod +x "${tmp}/gh"
export PATH="${tmp}:${PATH}"
export GH_CALL_LOG="${tmp}/calls.log"
export GH_REPO="o/r"

source "${DIR}/lib.sh"

DEL_NEEDS="-X DELETE repos/{owner}/{repo}/issues/1/labels/pr-sentinel%3A%20needs%20updates"
DEL_READY="-X DELETE repos/{owner}/{repo}/issues/1/labels/pr-sentinel%3A%20ready%20for%20review"
ADD_NEEDS="labels[]=pr-sentinel: needs updates"
ADD_READY="labels[]=pr-sentinel: ready for review"

test_mark_ready_for_review() { # compliant -> drop needs, add ready
  local calls
  : >"$GH_CALL_LOG"
  mark_ready_for_review 1
  calls="$(cat "$GH_CALL_LOG")"
  assert_contains     "$calls" "$DEL_NEEDS" "ready: removes needs-updates"
  assert_contains     "$calls" "$ADD_READY" "ready: adds ready-for-review"
  assert_not_contains "$calls" "$ADD_NEEDS" "ready: does not add needs-updates"
}

test_mark_needs_updates() { # non-compliant -> drop ready, add needs
  local calls
  : >"$GH_CALL_LOG"
  mark_needs_updates 1
  calls="$(cat "$GH_CALL_LOG")"
  assert_contains     "$calls" "$DEL_READY" "needs: removes ready-for-review"
  assert_contains     "$calls" "$ADD_NEEDS" "needs: adds needs-updates"
  assert_not_contains "$calls" "$ADD_READY" "needs: does not add ready-for-review"
}

test_clear_sentinel_labels() { # draft/skip -> drop both, add none
  local calls
  : >"$GH_CALL_LOG"
  clear_sentinel_labels 1
  calls="$(cat "$GH_CALL_LOG")"
  assert_contains     "$calls" "$DEL_NEEDS" "clear: removes needs-updates"
  assert_contains     "$calls" "$DEL_READY" "clear: removes ready-for-review"
  assert_not_contains "$calls" "-X POST"    "clear: adds no label"
}

main() {
  test_mark_ready_for_review
  test_mark_needs_updates
  test_clear_sentinel_labels

  if [[ "$fail" -ne 0 ]]; then echo "TESTS FAILED"; exit 1; fi
  echo "ALL TESTS PASSED"
}

main
