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

echo "Merging ${from_branch} into ${into_branch}"

set -ex
git checkout ${into_branch}
git pull
git config merge.ours.driver true
git config merge.merge_gradle.driver "python3 scripts/merges/merge_gradle.py %A %B"
set +e
git merge "origin/${from_branch}"
ret=$?
set +x

if [ "$ret" -eq 0 ]; then
  echo "Merge succeeded"
else
  echo "Merge failed. Next steps:"
  echo "1) Fix conflicts"
  echo "2) Add fixed files with: git add"
  echo "3) Continue the merge with: git merge --continue"
fi

if [ "${from_branch}" = "beta" ]; then
  if [ -e "app-thunderbird/src/beta/res/raw/changelog_master.xml" ]; then
    set -ex
    git rm app-thunderbird/src/beta/res/raw/changelog_master.xml
    set +ex
  fi
fi
