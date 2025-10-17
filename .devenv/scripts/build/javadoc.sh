#!/usr/bin/env bash
set -e

CMD="./mvnw package javadoc:javadoc javadoc:aggregate -Pdistro,distro-wildfly,distro-webjar,javadocs \
 -pl '!distro/wildfly/modules,!distro/wildfly/assembly,!distro/wildfly/webapp,!distro/wildfly/distro,!engine-rest/engine-rest-openapi,!engine-rest/docs' \
 -DskipTests=true -Dskip.frontend.build=true"

echo $CMD


eval $CMD

# The aggregated javadocs are in target/javadoc/<version>/apidocs, but we want them in target/javadoc/<version>
APIDOC_BASEDIR=$(find target -type d -name apidocs | sed -e 's|/apidocs||')
mv $APIDOC_BASEDIR/apidocs/* $APIDOC_BASEDIR
rm -rf $APIDOC_BASEDIR/apidocs
