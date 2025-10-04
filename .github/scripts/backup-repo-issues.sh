#!/usr/bin/env bash
set -euo pipefail

REPO_URL=$1
TARGET_DIR=$2

REPO=$(echo "$REPO_URL" | awk -F/ '{print $(NF-1) "/" $NF}')
mkdir -p "$TARGET_DIR"

# Get all issues (open only) and save as individual JSON files
gh issue list --repo "$REPO" --state open --limit 1000 --json number,title,body,labels,author \
  | jq -c '.[]' | while read -r issue; do
    ID=$(echo "$issue" | jq -r '.number')
    echo "$issue" > "$TARGET_DIR/issue-$ID.json"
    echo "Saved issue #$ID"
done
