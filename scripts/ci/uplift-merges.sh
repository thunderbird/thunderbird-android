#!/bin/bash

function fail() {
  echo "Error: $*"
  exit 1
}

function drydo() {
  if [ "$dry_run" = true ]; then
    echo "$@"
  else
    eval "$@"
  fi
}

function usage() {
  echo "Usage: $0 [--release | --beta] [--no-dry-run] [--push]"
  echo
  echo "  --release      Required: merge into release branch (mutually exclusive with --beta)"
  echo "  --beta         Required: merge into beta branch (mutually exclusive with --release)"
  echo "  --no-dry-run   Optional: actually perform the merge"
  echo "  --push         Optional: push changes after completion"
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
branch=""
push=false

milestones=$(gh api repos/${repo}/milestones --jq 'map(select(.state == "open" and .due_on != null)) | sort_by(.due_on)' | jq -c)

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
      expected_milestone=$(echo $milestones | jq -r '.[1].title')
      target_milestone=$(echo $milestones | jq -r '.[0].title')
      shift
      ;;
    --beta)
      label="task: uplift to beta"
      branch="beta"
      expected_milestone=$(echo $milestones | jq -r '.[2].title')
      target_milestone=$(echo $milestones | jq -r '.[1].title')
      shift
      ;;
    --push)
      push=true
      shift
      ;;
    *)
      usage
      ;;
  esac
done

if [[ -z "$branch" ]]; then
  usage
fi

# Check if on the correct branch
current_branch=$(git branch --show-current)
if [ "$current_branch" != "$branch" ]; then
    fail "You are not on the $branch branch. Please switch to the $branch branch."
    true
fi

# Check correct number of milestones
milestone_count=$(echo "$milestones" | jq 'length')
if [ "$milestone_count" != 3 ]; then
    fail "Expected 3 open milestones with due date on https://github.com/${repo}/milestones but found $milestone_count"
fi

# Status Info
if [ "$dry_run" = true ]
then
  echo "Dry run in progress, to disable pass --no-dry-run"
fi

echo "Label: \"$label\""
echo ""

# Fetch the uplift commits from the GitHub repository
json_data=$(gh pr list --repo "$repo" --label "$label" --state merged --limit 99 --json "mergedAt,mergeCommit,number,url,title,milestone" | jq -c .)

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
    pr_milestone=$(echo "$commit" | jq -r '.milestone.title')

    echo "Updating branches"
    git checkout main && git pull --quiet
    git checkout $current_branch && git pull --quiet

    echo "Cherry-picking $oid from $pr_url ($pr_title)"
    if [ "$pr_milestone" != "$expected_milestone" ]; then
        fail "PR https://github.com/$repo/pull/$pr_number is on milestone $pr_milestone but expected $expected_milestone"
    fi

    if [ "$dry_run" = true ]; then
        dry_run_branch="${branch}-dry-run"
        if git show-ref --verify --quiet refs/heads/${dry_run_branch}; then
            git branch -D "${dry_run_branch}"
        fi
        git checkout -b "${dry_run_branch}"
        echo "Using $dry_run_branch for cherry-picks"
    fi
    if ! git cherry-pick -m 1 "$oid"; then
        fail "Failed to cherry-pick $oid"
    fi
    if [ "$push" = true ]; then
      drydo git push || fail "Failed to push $oid"
    fi

    drydo gh pr edit "$pr_number" --repo "$repo" --remove-label '"$label"' --milestone '"$target_milestone"' || fail "Failed to remove label from $pr_number"
    echo ""
done <<< "$sorted_commits"
