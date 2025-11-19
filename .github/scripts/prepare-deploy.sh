#!/usr/bin/env bash
# generate a .skip-deploy file if there are no changes in the db directory since the last release tag
# this activates a profile that skips the deployment of sql scripts in the release workflow
set -e

# Find the last release tag that contains the last commit that modified the db directory
LAST_RELEASE_TAG=$(git tag --contains $(git log --pretty=format:%H -- engine/src/main/resources/org/operaton/bpm/engine/db | tail -1) | grep '^v' | sort -V | tail -1)

# Check if there are any changes in the db directory since the last release tag
git diff --quiet $LAST_RELEASE_TAG -- engine/src/main/resources/org/operaton/bpm/engine/db
if [ $? -eq 0 ]; then
    echo "No database changes detected since last release tag $LAST_RELEASE_TAG."
    touch distro/sql-script/.skip-deploy
fi