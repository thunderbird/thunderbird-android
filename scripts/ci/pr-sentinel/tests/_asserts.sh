#!/usr/bin/env bash
# Shared assertion helpers for the PR Sentinel test suites.
# Source this file, then use the assert_* helpers. It initializes `fail=0`;
# each suite's main() should end with:
#   if [[ "$fail" -ne 0 ]]; then echo "TESTS FAILED"; exit 1; fi
#   echo "ALL TESTS PASSED"

# `fail` is read by each sourcing suite's main(), not within this file.
export fail=0

assert_eq() { # $1=got $2=want $3=label
  if [[ "$1" == "$2" ]]; then
    echo "ok   - $3"
  else echo "FAIL - $3: got [$1] want [$2]"
    fail=1
  fi
}

assert_empty() { # $1=value $2=label
  if [[ -z "$1" ]]; then
    echo "ok   - $2 (pass)"
  else
    echo "FAIL - $2: expected pass, got [$1]"
    fail=1
  fi
}

assert_nonempty() { # $1=value $2=label
  if [[ -n "$1" ]]; then
    echo "ok   - $2 (fail msg present)"
  else
    echo "FAIL - $2: expected a message"
    fail=1
  fi
}

assert_contains() { # $1=haystack $2=needle $3=label (literal match)
  if grep -qF "$2" <<<"$1"; then
    echo "ok   - $3"
  else
    echo "FAIL - $3: [$2] not found in output"
    fail=1
  fi
}
