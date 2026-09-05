#!/usr/bin/env bash
# Run every *_test.sh in this directory and report an aggregate result.
# Each suite exits non-zero on failure; this runner mirrors that.
set -uo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

overall=0
passed=0
failed=0

for t in "$DIR"/*_test.sh; do
  [[ -e "$t" ]] || continue   # no matches -> skip the literal glob
  name="$(basename "$t")"
  echo "==> ${name}"
  if bash "$t"; then
    passed=$((passed + 1))
  else
    failed=$((failed + 1))
    overall=1
  fi
  echo
done

echo "================================"
echo "Suites: $((passed + failed)) | passed: ${passed} | failed: ${failed}"
if [[ "$overall" -ne 0 ]]; then echo "SOME TESTS FAILED"; exit 1; fi
echo "ALL SUITES PASSED"
