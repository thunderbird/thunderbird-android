#!/usr/bin/env bash

set -euo pipefail

# This script validates that a pull request has the required report labels.
# It uses the GitHub CLI (gh) to fetch PR information, allowing it to be tested locally.
#
# Usage:
#   ./validate-pr-report-labels.sh <pr-number>

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <pr-number>"
    exit 1
fi

PR_NUMBER="$1"

pr_data=$(gh pr view "$PR_NUMBER" --json labels,body)

labels_json=$(echo "$pr_data" | jq '.labels')
pr_body=$(echo "$pr_data" | jq -r '.body')

echo "Current labels:"
echo "$labels_json" | jq -r '.[].name'

include_count="$(jq '[.[] | select(.name == "report: include")] | length' <<< "$labels_json")"
exclude_count="$(jq '[.[] | select(.name == "report: exclude")] | length' <<< "$labels_json")"
highlight_count="$(jq '[.[] | select(.name == "report: highlight")] | length' <<< "$labels_json")"

total_count=$((include_count + exclude_count + highlight_count))

if [ "$total_count" -eq 0 ]; then
    echo "valid=false"
    echo "message=Missing report label. Set exactly one of: \`report: include\`, \`report: exclude\` OR \`report: highlight\`."
    exit 0
elif [ "$total_count" -gt 1 ]; then
    echo "valid=false"
    echo "message=Only one report label is allowed: \`report: include\`, \`report: exclude\` OR \`report: highlight\`."
    exit 0
fi

feature_flag_count="$(jq '[.[] | select(.name == "feature-flag")] | length' <<< "$labels_json")"
if [ "$feature_flag_count" -gt 0 ]; then
    pr_feature_flag_key="$(jq -nr --arg body "$pr_body" '
      try (
        $body
        | split("\n")
        | .[]
        | sub("^\\s+"; "")
        | select(test("^feature-flag:\\s*`[^`]+`"; "i"))
        | capture("`(?<flag>[^`]+)`")
        | .flag
      ) catch ""
    ' | head -n 1)"

    if [ -z "$pr_feature_flag_key" ]; then
        echo "valid=false"
        echo "message=PR body must contain the feature flag key in the format: 'feature-flag: \`<feature-flag-key>\`'."
        exit 0
    fi
fi

echo "valid=true"
