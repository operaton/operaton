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

if [ ! -f "mvnw" ]; then
  echo "⚠️ Maven Wrapper not found. You must execute this script from the project root directory. Exiting..."
  exit 1
fi

if [ -z "$1" ]; then
  echo "⚠️ You must provide the new version as an argument. Exiting..."
  exit 1
fi
NEW_VERSION=$1
CURRENT_VERSION=$(./mvnw help:evaluate -N -q -Dexpression=project.version -DforceStdout |tail -n 1)

# compute the next version
if [[ $NEW_VERSION =~ ^(.*-)([0-9]+)$ ]]; then
  # Version ends with "-<number>", increment that number (e.g. rc-1 -> rc-2)
  PREFIX="${BASH_REMATCH[1]}"
  NUMBER="${BASH_REMATCH[2]}"
  NEXT_NUMBER=$((NUMBER + 1))
  NEXT_VERSION="${PREFIX}${NEXT_NUMBER}"
else
  # Semantic versioning: Increment minor level and set patch to 0 (e.g. 1.2.3 -> 1.3.0)
  if [[ $NEW_VERSION =~ ^([0-9]+)\.([0-9]+)\.([0-9]+) ]]; then
    MAJOR="${BASH_REMATCH[1]}"
    MINOR="${BASH_REMATCH[2]}"
    NEXT_MINOR=$((MINOR + 1))
    NEXT_VERSION="${MAJOR}.${NEXT_MINOR}.0"
  else
    NEXT_VERSION="$NEW_VERSION"
  fi
fi

NEW_VERSION_WITHOUT_SNAPSHOT=$(echo $NEW_VERSION | sed 's/-SNAPSHOT//')
CURRENT_VERSION_WITHOUT_SNAPSHOT=$(echo $CURRENT_VERSION | sed 's/-SNAPSHOT//')
NEXT_VERSION_WITHOUT_SNAPSHOT=$(echo $NEXT_VERSION | sed 's/-SNAPSHOT//')


echo "🔍 Current project version is $CURRENT_VERSION, updating to $NEW_VERSION_WITHOUT_SNAPSHOT" and setting next version to $NEXT_VERSION

echo "🔄 Updating version in pom.xml files"

./mvnw versions:set -q -DnewVersion=$NEW_VERSION

echo "🔄 Updating version in package.json files"
PACKAGE_JSON_FILES=(\
  $(find . -name package.json) \
  $(find . -name package-lock.json) \
)

for PACKAGE_JSON_FILE in "${PACKAGE_JSON_FILES[@]}"; do
  sed -i '' -e "s/\"version\": \"$CURRENT_VERSION\"/\"version\": \"$NEW_VERSION\"/" $PACKAGE_JSON_FILE
done

echo "🔄 Updating version in pom.xml files that are not part of the reactor"
POM_FILES=(\
./distro/run/core/pom.xml \
./distro/run/distro/pom.xml \
./distro/run/assembly/pom.xml \
./distro/run/pom.xml \
./distro/run/qa/example-plugin/pom.xml \
./distro/run/qa/runtime/pom.xml \
./distro/run/qa/integration-tests/pom.xml \
./distro/run/qa/pom.xml \
./distro/run/modules/webapps/pom.xml \
./distro/run/modules/example/pom.xml \
./distro/run/modules/oauth2/pom.xml \
./distro/run/modules/pom.xml \
./distro/run/modules/rest/pom.xml \
)

for POM_FILE in "${POM_FILES[@]}"; do
  sed -i "s/$CURRENT_VERSION/$NEW_VERSION/g" $POM_FILE
done

echo "🔄 Updating version in jreleaser.yml"
sed -i '' -E "s/previousTagName: v.+/previousTagName: v$CURRENT_VERSION_WITHOUT_SNAPSHOT/" jreleaser.yml

MISSED_FILES=$(grep -R "$CURRENT_VERSION" --include pom.xml --include package.json  .)
if [ -n "$MISSED_FILES" ]; then
  echo "⚠️ The following files still contain the old version:"
  echo "$MISSED_FILES"
fi

echo "✅ Version updated to $NEW_VERSION"
