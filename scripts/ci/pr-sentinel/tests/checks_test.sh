#!/usr/bin/env bash
# Test inputs contain literal backticks (Markdown), not command substitution.
# shellcheck disable=SC2016
set -uo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=scripts/ci/pr-sentinel/tests/_asserts.sh
source "${DIR}/tests/_asserts.sh"
# shellcheck source=scripts/ci/pr-sentinel/checks.sh
source "${DIR}/checks.sh"

test_title() {
    assert_empty    "$(check_title 'feat(ui): add thing')" "title valid scoped"
    assert_empty    "$(check_title 'fix: bug')"            "title valid basic"
    assert_nonempty "$(check_title 'add thing')"           "title missing type"
    assert_nonempty "$(check_title 'ci: add thing')"       "invalid scope"
}

test_linked_issue() {
  # fn ----------- evaluate ------------------------------------------------------------ message
  assert_empty    "$(check_linked_issue 'Closes #12')"                                  "linked Closes cap"
  assert_empty    "$(check_linked_issue 'text fixes #9')"                               "linked mid-sentence"
  assert_nonempty "$(check_linked_issue 'prefixes #5')"                                 "linked substring rejected"
  assert_nonempty "$(check_linked_issue 'no link')"                                     "linked missing"
  assert_empty    "$(check_linked_issue 'Part of #12')"                                 "linked via Part of"
  # closing keyword inside code must NOT count (template placeholder, examples)
  assert_nonempty "$(check_linked_issue 'e.g. `Closes #1234` (template placeholder)')"  "linked in inline code rejected"
  assert_nonempty "$(check_linked_issue $'intro\n```\nCloses #7\n```\noutro')"          "linked in fenced code rejected"
  assert_empty    "$(check_linked_issue 'See `foo` then Closes #8')"                    "real link alongside inline code passes"
}

test_ai_disclosure() {
  BODY_ONE=$'## AI Disclosure\n\n- [ ] no AI\n- [x] assisted\n- [ ] created\n\n## Contribution Checklist\n\n- [x] read'
  BODY_NONE=$'## AI Disclosure\n\n- [ ] no AI\n- [ ] assisted\n- [ ] created\n\n## Contribution Checklist\n\n- [x] read'
  BODY_TWO=$'## AI Disclosure\n\n- [x] no AI\n- [x] assisted\n- [ ] created'
  assert_empty    "$(check_ai_disclosure "$BODY_ONE")"  "ai exactly one"
  assert_nonempty "$(check_ai_disclosure "$BODY_NONE")" "ai none"
  assert_nonempty "$(check_ai_disclosure "$BODY_TWO")"  "ai two"
}

test_commit_subject() { # max description length = 70
  assert_empty    "$(check_commit_subject abc1234 'feat: ok')"                        "commit subj valid"
  assert_nonempty "$(check_commit_subject abc1234 'bad subject no type')"             "commit subj bad type"
  assert_nonempty "$(check_commit_subject abc1234 'build: several changes')"          "commit subj bad scope"
  assert_nonempty "$(check_commit_subject abc1234 "feat: $(printf 'x%.0s' {1..71})")" "commit subj desc >70"
  assert_empty    "$(check_commit_subject abc1234 "feat: $(printf 'x%.0s' {1..70})")" "commit subj desc ==70"
}

test_commit_coauthor() {
  assert_empty    "$(check_commit_coauthor abc1234 $'feat: ok\n\nbody')"                       "commit no coauthor"
  assert_nonempty "$(check_commit_coauthor abc1234 $'fix: x\n\nCo-authored-by: A <a@x.com>')"  "commit coauthor"
  assert_nonempty "$(check_commit_coauthor abc1234 $'fix: x\n\nCo-Authored-By: A <a@x.com>')"  "commit coauthor pascal case"
  assert_nonempty "$(check_commit_coauthor abc1234 $'fix: x\n\nco-authored-by: a <a@x.com>')"  "commit coauthor lower"
}

test_exemptions() { # only bots are exempt now
  assert_eq "$(is_exempt Bot)"   "bot" "bot is exempt"
  assert_eq "$(is_exempt User)"  ""    "regular user not exempt"
}

test_escalation() {
  assert_eq "$(decide_escalation 0)"      "none"     "esc none"
  assert_eq "$(decide_escalation 86400)"  "escalate" "esc 24h"
  assert_eq "$(decide_escalation 259200)" "close"    "esc 72h"
}

test_date_helpers() { # GNU/BSD portable
  assert_eq "$(iso_to_epoch '1970-01-01T00:00:00Z')" "0"          "iso_to_epoch epoch 0"
  assert_eq "$(iso_to_epoch '2020-01-01T00:00:00Z')" "1577836800" "iso_to_epoch 2020"
  assert_eq "$(epoch_to_display 0)"                  "1970-01-01 00:00 UTC" "epoch_to_display 0"
}

main() {
  test_title
  test_linked_issue
  test_ai_disclosure
  test_commit_subject
  test_commit_coauthor
  test_exemptions
  test_escalation
  test_date_helpers

  if [[ "$fail" -ne 0 ]]; then echo "TESTS FAILED"; exit 1; fi
  echo "ALL TESTS PASSED"
}

main
