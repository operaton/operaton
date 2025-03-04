#!/usr/bin/env bash
pushd > /dev/null || exit 1
cd $(git rev-parse --show-toplevel) || exit 2
./.devenv/scripts/integration-tests/engine-tomcat-h2-build.sh
./.devenv/scripts/integration-tests/engine-tomcat-h2-test.sh
popd > /dev/null || exit 1
