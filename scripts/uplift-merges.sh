#!/bin/bash

function fail() {
  echo "Error: $*"
  exit 1
}

# Check if tools are installed
command -v gh &> /dev/null || fail "gh (GitHub CLI) is not installed"
command -v jq &> /dev/null || fail "jq is not installed"
command -v git &> /dev/null || fail "git is not installed"

# Default values
dry_run=true
repo=${GITHUB_REPOSITORY:-thunderbird/thunderbird-android}
label="task: uplift to beta"
branch="beta"
push=false

# Parse command-line arguments
for arg in "$@"; do
  case $arg in
    --no-dry-run)
      dry_run=false
      shift
      ;;
    --release)
      label="task: uplift to release"
      branch="release"
      shift
      ;;
    --beta)
      label="task: uplift to beta"
      branch="beta"
      shift
      ;;
    --push)
      push=true
      shift
      ;;
    *)
      fail "Unknown argument: $arg"
      ;;
  esac
done

# Check if on the correct branch
current_branch=$(git branch --show-current)
if [ "$current_branch" != "$branch" ]; then
    fail "You are not on the $branch branch. Please switch to the $branch branch."
fi

if [ "$dry_run" = true ]
then
  echo "Dry run in progress, to disable pass --no-dry-run"
fi

echo "Label: \"$label\""
echo ""

# Fetch the uplift commits from the GitHub repository
json_data=$(gh pr list --repo "$repo" --label "$label" --state merged --json "mergedAt,mergeCommit,number,url,title" | jq -c .)

# Sort by mergedAt
sorted_commits=$(echo "$json_data" | jq -c '. | sort_by(.mergedAt) | .[]')

# Check if there are no commits to cherry-pick
if [ -z "$sorted_commits" ]; then
  echo "No commits to cherry-pick."
  exit 0
fi

# Generate git cherry-pick commands
while IFS= read -r commit
do
    oid=$(echo "$commit" | jq -r '.mergeCommit.oid')
    pr_number=$(echo "$commit" | jq -r '.number')
    pr_url=$(echo "$commit" | jq -r '.url')
    pr_title=$(echo "$commit" | jq -r '.title')
    echo "Cherry-picking $oid from $pr_url ($pr_title)"

    if [ "$dry_run" = false ]; then
        git cherry-pick -m 1 "$oid" || fail "Failed to cherry-pick $oid"
        if [ "$push" = true ]; then
          git push || fail "Failed to push $oid"
        fi

        gh pr edit "$pr_number" --repo "$repo" --remove-label "$label" || fail "Failed to remove label from $pr_number"
    else
        echo "git cherry-pick -m 1 $oid"
        [ "$push" = true ] && echo git push
        echo "gh pr edit $pr_number --repo \"$repo\" --remove-label \"$label\""
    fi
    echo ""
done <<< "$sorted_commits"
