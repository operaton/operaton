#!/usr/bin/env sh
pushd $(pwd)
cd $(git rev-parse --show-toplevel) || exit 1
./mvnw -Pengine-integration,tomcat,h2-in-memory verify -f qa
popd
