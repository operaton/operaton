#!/usr/bin/env bash
pushd $(pwd)
cd $(git rev-parse --show-toplevel) || exit 1
./mvnw -Pdistro,distro-run,distro-tomcat,distro-wildfly,distro-webjar,distro-starter,distro-serverless,testcontainers h2-in-memory clean install
popd
