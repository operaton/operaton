#!/usr/bin/env sh
pushd $(pwd)
cd $(git rev-parse --show-toplevel) || exit 1
./mvnw -DskipTests -Pdistro,distro-webjar,distro-tomcat,tomcat,h2-in-memory clean install
popd
