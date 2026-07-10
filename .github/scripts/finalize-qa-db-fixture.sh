#!/usr/bin/env bash

# Copyright 2025 the Operaton contributors.
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

DATABASE_VERSION=$1
RELEASE_VERSION=$2

if [ -z "$DATABASE_VERSION" ]; then
  echo "⚠️ You must provide the DATABASE_VERSION as the first argument (e.g. 7.25.0). Exiting..."
  exit 1
fi

if [ -z "$RELEASE_VERSION" ]; then
  echo "⚠️ You must provide the RELEASE_VERSION as the second argument. Exiting..."
  exit 1
fi

# Derive fixture suffix from DATABASE_VERSION (e.g. 7.25.0 -> 725)
if [[ "$DATABASE_VERSION" =~ ^([0-9]+)\.([0-9]+)\. ]]; then
  MAJOR="${BASH_REMATCH[1]}"
  MINOR="${BASH_REMATCH[2]}"
  FIXTURE_SUFFIX="${MAJOR}${MINOR}"
else
  echo "⚠️ Cannot parse DATABASE_VERSION '$DATABASE_VERSION'. Expected format: MAJOR.MINOR.PATCH. Exiting..."
  exit 1
fi

POM_FILE="qa/test-db-instance-migration/test-fixture-${FIXTURE_SUFFIX}/pom.xml"

if [ ! -f "$POM_FILE" ]; then
  echo "⚠️ Fixture pom.xml not found: $POM_FILE. Exiting..."
  exit 1
fi

echo "🔄 Updating operaton.version.current to '$RELEASE_VERSION' in $POM_FILE"
sed -i "s|<operaton.version.current>.*</operaton.version.current>|<operaton.version.current>${RELEASE_VERSION}</operaton.version.current>|" "$POM_FILE"
echo "✅ Updated $POM_FILE"
