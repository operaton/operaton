#!/usr/bin/env sh
pushd $(pwd)
cd $(git rev-parse --show-toplevel) || exit 1
.devenv/scripts/integration-tests/engine-tomcat-h2-build.sh
popd
