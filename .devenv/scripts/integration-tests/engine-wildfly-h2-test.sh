#!/usr/bin/env bash
pushd $(pwd) > /dev/null
cd $(git rev-parse --show-toplevel) || exit 1
./mvnw -Pengine-integration,wildfly,h2-in-memory verify -f qa
popd > /dev/null
