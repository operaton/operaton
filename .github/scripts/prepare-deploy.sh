#!/usr/bin/env bash
# generate a .skip-deploy file if there are no changes in the db directory since the last release tag
# this activates a profile that skips the deployment of sql scripts in the release workflow
set -e

DB_DIR=engine/src/main/resources/org/operaton/bpm/engine/db

# Last release tag reachable from HEAD (i.e. the previous release on this branch)
LAST_RELEASE_TAG=$(git tag --merged HEAD | grep '^v' | sort -V | tail -1)

if [ -z "$LAST_RELEASE_TAG" ]; then
    echo "⚠️ No release tag found; sql scripts will be deployed."
    exit 0
fi

# Check if there are any changes in the db directory since the last release tag
if git diff --quiet "$LAST_RELEASE_TAG" -- "$DB_DIR"; then
    echo "No database changes detected since last release tag $LAST_RELEASE_TAG."
    touch distro/sql-script/.skip-deploy
else
    echo "Database changes detected since last release tag $LAST_RELEASE_TAG; sql scripts will be deployed."
fi
