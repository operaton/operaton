#!/usr/bin/env bash
# generate a .skip-deploy file if there are no changes in the db directory since the previous final release
# this activates a profile that skips the deployment of sql scripts in the release workflow
set -e

DB_DIR=engine/src/main/resources/org/operaton/bpm/engine/db

# Previous final release: the highest vX.Y.Z tag below the version being released.
# Milestone and RC tags are ignored, because their sql scripts are never deployed.
# Tags are not limited to HEAD's history: X.Y.0 releases are tagged on release/X.Y.x,
# so main and newer release branches cannot reach them.
RELEASE_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout | tail -n 1)
RELEASE_TAG="v${RELEASE_VERSION%%-*}"
LAST_RELEASE_TAG=$( { git tag -l 'v*' | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$'; echo "$RELEASE_TAG"; } \
    | sort -uV | grep -F -x -B1 "$RELEASE_TAG" | head -n 1)
if [[ "$LAST_RELEASE_TAG" == "$RELEASE_TAG" ]]; then
    LAST_RELEASE_TAG=""
fi

if [[ -z "$LAST_RELEASE_TAG" ]]; then
    echo "⚠️ No previous release tag found; sql scripts will be deployed."
    exit 0
fi

# Check if there are any changes in the db directory since the last release tag
if git diff --quiet "$LAST_RELEASE_TAG" -- "$DB_DIR"; then
    echo "No database changes detected since last release tag $LAST_RELEASE_TAG."
    touch distro/sql-script/.skip-deploy
else
    echo "Database changes detected since last release tag $LAST_RELEASE_TAG; sql scripts will be deployed."
fi
