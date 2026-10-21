#!/usr/bin/env bash

# Copyright 2026 the Operaton contributors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at:
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Computes the release version from the current pom.xml version plus an
# optional preliminary-release qualifier, and applies it to the workspace
# (pom.xml/package.json versions, QA db fixture). Used by both the
# 'release_build' and 'documentation' jobs so they apply the exact same
# release version to their independent checkouts.
#
# Reads QUALIFIER from $1 or $GITHUB_EVENT_INPUTS_PRELIMINARY_RELEASE_QUALIFIER.
# Writes version/base_version/database_version/is_prerelease to $GITHUB_OUTPUT
# when that variable is set.

set -e

QUALIFIER="${1:-${GITHUB_EVENT_INPUTS_PRELIMINARY_RELEASE_QUALIFIER}}"

BASE_RELEASE_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout | tail -n 1 | sed -e 's/-SNAPSHOT//')

if [[ -n "$QUALIFIER" ]]; then
  if [[ ! "$QUALIFIER" =~ ^(M[0-9]|RC[0-9])$ ]]; then
    echo "::error::Preliminary Release Qualifier '$QUALIFIER' must match M[0-9] or RC[0-9]."
    exit 1
  fi
  if [[ ! "$BASE_RELEASE_VERSION" =~ ^[0-9]+\.[0-9]+\.0$ ]]; then
    echo "::error::Preliminary Release Qualifier can only be used for .0 versions. Current version is '$BASE_RELEASE_VERSION'."
    exit 1
  fi
  RELEASE_VERSION="${BASE_RELEASE_VERSION}-${QUALIFIER}"
  IS_PRERELEASE="true"
  echo "⚙ Releasing version '$RELEASE_VERSION' (Preliminary Release)"
else
  RELEASE_VERSION="$BASE_RELEASE_VERSION"
  IS_PRERELEASE="false"
  echo "⚙ Releasing version '$RELEASE_VERSION'"
fi

DATABASE_VERSION=$(grep '<operaton.dbscheme.current.version>' database/pom.xml | sed -e 's/.*<operaton.dbscheme.current.version>\(.*\)<\/operaton.dbscheme.current.version>.*/\1/')
echo "database_version=$DATABASE_VERSION"

if [[ -n "$GITHUB_OUTPUT" ]]; then
  echo "version=$RELEASE_VERSION" >> "$GITHUB_OUTPUT"
  echo "base_version=$BASE_RELEASE_VERSION" >> "$GITHUB_OUTPUT"
  echo "database_version=$DATABASE_VERSION" >> "$GITHUB_OUTPUT"
  echo "is_prerelease=$IS_PRERELEASE" >> "$GITHUB_OUTPUT"
fi

.github/scripts/set-project-version.sh "$RELEASE_VERSION"

if [[ -z "$QUALIFIER" ]]; then
  .github/scripts/finalize-qa-db-fixture.sh "$DATABASE_VERSION" "$RELEASE_VERSION"
else
  # prevent deployment of non-final sql scripts - a db schemaa can only deployed once
  touch distro/sql-script/.skip-deploy
fi
