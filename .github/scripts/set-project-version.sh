#!/usr/bin/env bash

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

echo "🔍 Current project version is $CURRENT_VERSION, updating to $NEW_VERSION"
echo "🔄 Updating version in pom.xml files"

./mvnw versions:set -q -DnewVersion=$NEW_VERSION

echo "🔄 Updating version in package.json files"
PACKAGE_JSON_FILES=(\
  $(find . -name package.json) \
  $(find . -name package-lock.json) \
)

for PACKAGE_JSON_FILE in ${PACKAGE_JSON_FILES[@]}; do
  sed -i '' -e "s/\"version\": \"$CURRENT_VERSION\"/\"version\": \"$1\"/" $PACKAGE_JSON_FILE
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
./distro/wildfly26/pom.xml \
./distro/wildfly26/modules/pom.xml \
./distro/wildfly26/subsystem/pom.xml \
)

for POM_FILE in ${POM_FILES[@]}; do
  sed -i "s/$CURRENT_VERSION/$NEW_VERSION/g" $POM_FILE
done

echo "🔄 Updating version in license-book.txt"
sed -i "s/$CURRENT_VERSION/$NEW_VERSION/g" ./distro/license-book/src/main/resources/license-book.txt

MISSED_FILES=$(grep -R "$CURRENT_VERSION" --include pom.xml --include package.json --include license-book.txt .)
if [ -n "$MISSED_FILES" ]; then
  echo "❌ The following files still contain the old version:"
  echo "$MISSED_FILES"
  exit 1
fi

echo "✅ Version updated to $NEW_VERSION"
