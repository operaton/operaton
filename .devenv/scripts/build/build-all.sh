#!/usr/bin/env bash
pushd $(pwd) > /dev/null
cd $(git rev-parse --show-toplevel) || exit 1
./mvnw -Pdistro,distro-run,distro-tomcat,tomcat,distro-wildfly,wildfly,distro-webjar,distro-starter,distro-serverless,testcontainers,h2-in-memory \
  clean install
popd > /dev/null

