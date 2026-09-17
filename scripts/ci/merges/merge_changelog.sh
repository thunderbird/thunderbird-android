#!/usr/bin/env bash

# This merge driver exists to override the -Xtheirs CLI option used when merging
# two trees together (e.g. main -> beta).

A="$1"  # File A (ours)
O="$2"  # Common ancestor
B="$3"  # File B (theirs)

git merge-file -- "$A" "$O" "$B"
