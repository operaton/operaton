#!/usr/bin/env bash
pushd $(pwd) > /dev/null
cd $(git rev-parse --show-toplevel) || exit 1
./mvnw -DskipTests -Pdistro,distro-webjar,distro-tomcat,tomcat,h2-in-memory clean install
popd > /dev/null
