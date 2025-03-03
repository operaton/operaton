#!/usr/bin/env bash
pushd $(pwd) > /dev/null
cd $(git rev-parse --show-toplevel) || exit 1
.devenv/scripts/integration-tests/engine-wildfly-h2-build.sh
.devenv/scripts/integration-tests/engine-wildfly-h2-test.sh
popd > /dev/null
