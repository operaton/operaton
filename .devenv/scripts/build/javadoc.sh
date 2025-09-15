#!/usr/bin/env bash

CMD="./mvnw package javadoc:javadoc javadoc:aggregate -Pdistro,distro-wildfly,distro-webjar,javadocs \
 -pl '!distro/wildfly/modules,!engine-rest/engine-rest-openapi' \
 -DskipTests=true -Dskip.frontend.build=true"

# -pl '!distro/wildfly/modules,!engine-rest/engine-rest-openapi' \
echo $CMD


eval $CMD

# The aggregated javadocs are in target/javadoc/operaton/<version>/apidocs, but we want them in target/javadoc/operaton/<version>
APIDOC_BASEDIR=$(find target -type d -name apidocs | sed -e 's|/apidocs||')
mv $APIDOC_BASEDIR/apidocs/* $APIDOC_BASEDIR
rm -rf $APIDOC_BASEDIR/apidocs
