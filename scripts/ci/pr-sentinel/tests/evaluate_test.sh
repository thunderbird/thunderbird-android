#!/usr/bin/env bash
set -uo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${DIR}/tests/_asserts.sh"

run() { PR_JSON="$1" COMMITS_JSON="$2" bash "${DIR}/evaluate-pr.sh" 1; }

test_compliant() { # good title + body + commit
  local pr out
  pr=$(jq -n '{title:"feat: x", body:"Closes #1\n\n## AI Disclosure\n- [x] a\n- [ ] b\n- [ ] c", draft:false, user:{type:"User"}, author_association:"NONE"}')
  out="$(run "$pr" '[{"sha":"abc1234def","parents":[{}],"commit":{"message":"feat: ok"}}]')"
  assert_eq "$(jq -r .compliant <<<"$out")" "true" "compliant PR"
}

test_noncompliant() { # missing issue + bad commit; not exempt
  local pr commits out
  pr=$(jq -n '{title:"bad title", body:"no link\n\n## AI Disclosure\n- [ ] a\n- [ ] b\n- [ ] c", draft:false, user:{type:"User"}, author_association:"NONE"}')
  commits='[{"sha":"abc1234def","parents":[{}],"commit":{"message":"nope\n\nCo-authored-by: X <x@x.com>"}}]'
  out="$(run "$pr" "$commits")"
  assert_eq       "$(jq -r .compliant <<<"$out")" "false" "non-compliant PR"
  assert_eq       "$(jq -r .exempt <<<"$out")" "false" "not exempt"
  assert_contains "$(jq -r .missing_markdown <<<"$out")" "AI Disclosure" "missing list mentions AI Disclosure"
  assert_contains "$(jq -r .topics <<<"$out")" "commit" "topics include commit"
  assert_contains "$(jq -r .topics <<<"$out")" "issue" "topics include issue"
  assert_contains "$(jq -r .topics <<<"$out")" "workflow" "topics include workflow"
}

test_topic_filtering() { # only the linked-issue check fails -> topics is exactly "issue"
  local pr out
  pr=$(jq -n '{title:"feat: x", body:"no link\n\n## AI Disclosure\n- [x] a\n- [ ] b\n- [ ] c", draft:false, user:{type:"User"}, author_association:"NONE"}')
  out="$(run "$pr" '[{"sha":"abc1234","parents":[{}],"commit":{"message":"feat: ok"}}]')"
  assert_eq "$(jq -r .topics <<<"$out")" "issue" "only linked-issue failing -> topics=issue"
}

test_bot_exempt() { # bot author is exempt even when non-compliant
  local pr out
  pr=$(jq -n '{title:"bad", body:"", draft:false, user:{type:"Bot"}, author_association:"NONE"}')
  out="$(run "$pr" '[]')"
  assert_eq "$(jq -r .exempt <<<"$out")" "true" "bot exempt flag"
  assert_eq "$(jq -r .exempt_reason <<<"$out")" "bot" "bot exempt reason"
}

test_merge_commit() { # merge commit present (2 parents) -> non-compliant, asks to rebase
  local pr commits out
  pr=$(jq -n '{title:"feat: x", body:"Closes #1\n\n## AI Disclosure\n- [x] a\n- [ ] b\n- [ ] c", draft:false, user:{type:"User"}, author_association:"NONE"}')
  commits='[{"sha":"aaaaaaa","parents":[{}],"commit":{"message":"feat: ok"}},{"sha":"m111111","parents":[{},{}],"commit":{"message":"Merge branch main"}}]'
  out="$(run "$pr" "$commits")"
  assert_eq       "$(jq -r .compliant <<<"$out")" "false" "merge commit -> non-compliant"
  assert_contains "$(jq -r .missing_markdown <<<"$out")" "Merge commit" "merge commit flagged"
  assert_contains "$(jq -r .missing_markdown <<<"$out")" "rebase" "asks to rebase"
  assert_contains "$(jq -r .topics <<<"$out")" "workflow" "merge -> workflow topic"
}

test_member_not_exempt() { # author_association MEMBER is no longer exempt
  local pr out
  pr=$(jq -n '{title:"bad title", body:"", draft:false, user:{type:"User"}, author_association:"MEMBER"}')
  out="$(run "$pr" '[]')"
  assert_eq "$(jq -r .exempt <<<"$out")" "false" "member is not exempt"
}

test_draft() { # draft flag is surfaced so report-pr.sh can skip drafts
  local pr out
  pr=$(jq -n '{title:"bad", body:"", draft:true, user:{type:"User"}, author_association:"NONE"}')
  out="$(run "$pr" '[]')"
  assert_eq "$(jq -r .draft <<<"$out")" "true" "draft flag surfaced"
}

main() {
  test_compliant
  test_noncompliant
  test_topic_filtering
  test_bot_exempt
  test_merge_commit
  test_member_not_exempt
  test_draft

  if [[ "$fail" -ne 0 ]]; then echo "TESTS FAILED"; exit 1; fi
  echo "ALL TESTS PASSED"
}

main
