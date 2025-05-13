#!/bin/bash

# This script is used to perform merges of main->beta and beta->release.
# Be sure to review merge results and ensure correctness before pushing
# to the repository.
# To merge into beta: do_merge.sh beta
# To merge into release: do_merge.sh release

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 <into-branch>"
  exit 1
fi

into_branch=$1
from_branch="main"
if [ "${into_branch}" = "release" ]; then
  from_branch="beta"
fi

echo "Before merging ${from_branch} into ${into_branch} please confirm that you have:"
if [ "${into_branch}" = "beta" ]; then
  echo "1) Locked the main branch with the 'CLOSED TREE (main)' ruleset"
  echo "2) Sent a message to the #tb-mobile-dev:mozilla.org matrix channel to let them know:"
  echo "   - You will be performing the merge from main into beta"
  echo "   - The main branch is locked and cannot be changed during the merge"
  echo "   - You will let them know when the merge is complete and main is re-opened"
else
  echo "1) Sent a message to the #tb-mobile-dev:mozilla.org matrix channel to let them know"
  echo "   - You will be performing the merge from beta into release"
  echo "   - You will let them know when the merge is complete"
fi
read -p "Continue with merge? [y/N]: " answer
answer=${answer,,}
if [[ "$answer" == "y" || "$answer" == "yes" ]]; then
  echo "Merging ${from_branch} into ${into_branch}"
else
  exit 1
fi
echo

set -ex
git checkout ${into_branch}
git pull
git config merge.ours.driver true
git config merge.merge_gradle.driver "python3 scripts/ci/merges/merge_gradle.py %A %B"
set +e
git merge "origin/${from_branch}"
ret=$?
set +x

if [ "${from_branch}" = "beta" ]; then
  if [ -e "app-thunderbird/src/beta/res/raw/changelog_master.xml" ]; then
    set -ex
    git rm --force app-thunderbird/src/beta/res/raw/changelog_master.xml
    set +ex
  fi
fi

echo
if [ "$ret" -eq 0 ]; then
  echo "Merge succeeded. Next steps:"
  echo "1) Review merge results and ensure correctness"
  echo "2) Ensure feature flags are following the rules"
  echo "3) Push the merge"
  if [ "${into_branch}" = "beta" ]; then
      echo "4) Submit a pull request that increments the version in main"
      echo "5) Open a new milestone for the new version on github"
      echo "6) Once the version increment is merged into main, unlock the branch"
      echo "7) Send a message to the #tb-mobile-dev:mozilla.org channel to notify of merge completion and that main is re-opened"
  else
      echo "4) Close the milestone for the version that was previously in release"
      echo "5) Send a message to the #tb-mobile-dev:mozilla.org channel to notify of merge completion"
  fi
else
  echo "Merge failed. Next steps:"
  echo "1) Fix conflicts"
  echo "2) Add fixed files with: git add"
  echo "3) Continue the merge with: git merge --continue"
fi
