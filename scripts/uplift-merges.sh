#!/bin/bash

# Check if gh is installed
if ! command -v gh &> /dev/null; then
    echo "Error: gh (GitHub CLI) is not installed."
    exit 1
fi

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "Error: jq is not installed."
    exit 1
fi

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo "Error: git is not installed."
    exit 1
fi

# Default values
dry_run=true
repo=${GITHUB_REPOSITORY:-thunderbird/thunderbird-android}
label="task: uplift to beta"
branch="beta"

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
    *)
      echo "Unknown argument: $arg"
      exit 1
      ;;
  esac
done

# Check if on the correct branch
current_branch=$(git branch --show-current)
if [ "$current_branch" != "$branch" ]; then
    echo "Error: You are not on the $branch branch. Please switch to the $branch branch."
    exit 1
fi

echo "Dry run: $dry_run, to disable dry run pass --no-dry-run"
echo "Label: \"$label\""
echo ""

# Fetch the uplift commits from the GitHub repository
json_data=$(gh pr list --repo "$repo" --label "$label" --state closed --json "mergedAt,mergeCommit,number,url")

# Sort by mergedAt
sorted_commits=$(echo "$json_data" | jq -c '. | sort_by(.mergedAt) | .[]')

# Check if there are no commits to cherry-pick
if [ -z "$sorted_commits" ]; then
  echo "No commits to cherry-pick."
  exit 0
fi

# Generate git cherry-pick commands
for commit in $sorted_commits; do
    oid=$(echo "$commit" | jq -r '.mergeCommit.oid')
    pr_number=$(echo "$commit" | jq -r '.number')
    pr_url=$(echo "$commit" | jq -r '.url')
    echo "Cherry-picking $oid from $pr_url"

    if [ "$dry_run" = false ]; then
        if git cherry-pick -m 1 "$oid"; then
          gh pr edit "$pr_number" --remove-label "$label"
        else
          echo "Failed to cherry-pick $oid"
          exit 1
        fi
    else
        echo "git cherry-pick -m 1 $oid"
        echo "gh pr edit $pr_number --remove-label \"$label\""
    fi
    echo ""
done
