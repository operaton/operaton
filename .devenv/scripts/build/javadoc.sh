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

set -e

CMD="./mvnw package javadoc:javadoc javadoc:aggregate -Pdistro,distro-wildfly,distro-webjar,javadocs \
 -pl '!distro/wildfly/modules,!distro/wildfly/assembly,!distro/wildfly/webapp,!distro/wildfly/distro,!engine-rest/engine-rest-openapi,!engine-rest/docs' \
 -DskipTests=true -Dskip.frontend.build=true"

echo $CMD


eval $CMD

TARGET_DIR="target/javadoc"
TEMP_DIR="$(pwd)/target/apidocs"
TARGET_ZIPFILE="$(pwd)/target/javadoc.zip"
APIDOC_BASEDIR=$(find target -type d -name apidocs)

rm -rf $TEMP_DIR
mv $APIDOC_BASEDIR target
rm -rf $TARGET_DIR
mv $TEMP_DIR $TARGET_DIR

echo "Javadocs generated at: $TARGET_DIR, zipping to $TARGET_ZIPFILE"
pushd $(pwd)
cd $TARGET_DIR
echo $(pwd)
zip -qr $TARGET_ZIPFILE .
echo "Javadocs zipped to: $TARGET_ZIPFILE"
popd
echo "✅ Done!"
