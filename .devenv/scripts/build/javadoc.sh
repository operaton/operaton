#!/usr/bin/env bash
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
