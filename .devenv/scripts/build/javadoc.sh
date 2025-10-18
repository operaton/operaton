#!/usr/bin/env bash
set -e

CMD="./mvnw package javadoc:javadoc javadoc:aggregate -Pdistro,distro-wildfly,distro-webjar,javadocs \
 -pl '!distro/wildfly/modules,!distro/wildfly/assembly,!distro/wildfly/webapp,!distro/wildfly/distro,!engine-rest/engine-rest-openapi,!engine-rest/docs' \
 -DskipTests=true -Dskip.frontend.build=true"

echo $CMD


eval $CMD

# The aggregated javadocs are in target/javadoc/apidocs, but we want them in target/javadoc/
APIDOC_BASEDIR="target/javadoc"
TARGET_ZIPZILE="target/javadoc.zip"

mv $APIDOC_BASEDIR/apidocs/* $APIDOC_BASEDIR
rm -rf $APIDOC_BASEDIR/apidocs

echo "Javadocs generated at: $APIDOC_BASEDIR, zipping to $TARGET_ZIPZILE"
zip -qr $TARGET_ZIPZILE $APIDOC_BASEDIR
echo "Javadocs zipped to: $TARGET_ZIPZILE"
echo "✅ Done!"
