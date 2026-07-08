#!/usr/bin/env bash
set -uo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=scripts/ci/pr-sentinel/tests/_asserts.sh
source "${DIR}/tests/_asserts.sh"

# Fake `gh` on PATH that records each invocation to a log file.
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cat >"${tmp}/gh" <<'EOF'
#!/usr/bin/env bash
echo "REAL_GH $*" >>"$GH_CALL_LOG"
EOF
chmod +x "${tmp}/gh"
export PATH="${tmp}:${PATH}"
export GH_CALL_LOG="${tmp}/calls.log"

# shellcheck source=scripts/ci/pr-sentinel/lib.sh
source "${DIR}/lib.sh"

test_dry_run_skips_gh() { # gh must NOT be invoked; a notice is printed to stderr
  local err
  : >"$GH_CALL_LOG"
  DRY_RUN=1
  err="$(gh_write api -X POST 'repos/o/r/issues/1/comments' -f body=x 2>&1 >/dev/null)"
  assert_eq       "$(wc -l <"$GH_CALL_LOG" | tr -d ' ')" "0" "dry-run does not invoke gh"
  assert_contains "$err" "[dry-run]" "dry-run prints a notice"
}

test_real_mode_invokes_gh() { # gh IS invoked
  : >"$GH_CALL_LOG"
  DRY_RUN=0
  gh_write api -X POST 'repos/o/r/issues/1/comments' -f body=x
  assert_contains "$(cat "$GH_CALL_LOG")" "REAL_GH api -X POST" "real mode invokes gh"
}

main() {
  test_dry_run_skips_gh
  test_real_mode_invokes_gh

  if [[ "$fail" -ne 0 ]]; then echo "TESTS FAILED"; exit 1; fi
  echo "ALL TESTS PASSED"
}

main
