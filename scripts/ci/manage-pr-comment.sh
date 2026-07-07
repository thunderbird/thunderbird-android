#!/usr/bin/env bash

set -euo pipefail

# This script manages a single PR comment by using a unique identifier.
# Usage:
#   ./manage-pr-comment.sh <pr-number> <identifier> <message> <status: invalid|valid>

if [[ $# -ne 4 ]]; then
    echo "Usage: $0 <pr-number> <identifier> <message> <status>"
    exit 1
fi

PR_NUMBER="$1"
IDENTIFIER="$2"
MESSAGE="$3"
STATUS="$4"

COMMENT_ID=$(gh api "repos/{owner}/{repo}/issues/${PR_NUMBER}/comments" | \
    jq -r ".[] | select(.body | contains(\"${IDENTIFIER}\")) | .id" | head -n 1)

if [[ -z "$COMMENT_ID" || "$COMMENT_ID" == "null" ]]; then
    COMMENT_ID=""
fi

FULL_MESSAGE="${MESSAGE}${IDENTIFIER}"

if [[ "$STATUS" == "invalid" ]]; then
    if [[ -n "$COMMENT_ID" ]]; then
        echo "Updating existing comment $COMMENT_ID"
        gh api -X PATCH "repos/{owner}/{repo}/issues/comments/${COMMENT_ID}" -f body="$FULL_MESSAGE" > /dev/null
    else
        echo "Creating new comment"
        gh api -X POST "repos/{owner}/{repo}/issues/${PR_NUMBER}/comments" -f body="$FULL_MESSAGE" > /dev/null
    fi
elif [[ "$STATUS" == "valid" ]]; then
    if [[ -n "$COMMENT_ID" ]]; then
        RESOLVED_MESSAGE="✅ **Validation Passed**: All report and feature-flag labels are correctly set.${IDENTIFIER}"
        echo "Marking comment $COMMENT_ID as resolved"
        gh api -X PATCH "repos/{owner}/{repo}/issues/comments/${COMMENT_ID}" -f body="$RESOLVED_MESSAGE" > /dev/null
    else
        echo "PR is valid and no comment exists. Nothing to do."
    fi
fi
