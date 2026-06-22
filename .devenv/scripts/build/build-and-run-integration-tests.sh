#!/usr/bin/env bash

# Copyright 2025 the Operaton contributors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at:
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

EXECUTE_BUILD=true
EXECUTE_TEST=true
TEST_SUITE="engine"
DATABASE="h2"
DISTRO="tomcat"
RUNNER="./mvnw"
VALID_TEST_SUITES=("engine" "webapps" "db-rolling-update")
VALID_DISTROS=("operaton" "tomcat" "wildfly")
VALID_DATABASES=("h2" "postgresql" "postgresql-xa" "mysql" "mariadb" "oracle" "db2" "sqlserver")
VALID_RUNNERS=("mvn" "./mvnw" "mvnd")
##########################################################################
check_valid_values() {
  local param_name=$1
  local value=$2
  shift 2
  local array=("$@")
  for item in "${array[@]}"; do
    if [[ "$value" == "$item" ]]; then
      return 0
    fi
  done
  echo "Error: Argument '$param_name' must be one of: [${array[*]}], but was '$value'"
  exit 1
}

##########################################################################
parse_args() {
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --testsuite=*)
        TEST_SUITE="${1#*=}"
        ;;
      --distro=*)
        DISTRO="${1#*=}"
        ;;
      --db=*)
        DATABASE="${1#*=}"
        ;;
      --no-build)
        EXECUTE_BUILD=false
        ;;
      --no-test)
        EXECUTE_TEST=false
        ;;
      --runner=*)
        runner_param="${1#*=}"
        case "$runner_param" in
          mvn|mvnd)
            RUNNER="$runner_param"
            ;;
          mvnw)
            RUNNER="./mvnw"
            ;;
        esac
        ;;
    esac
    shift
  done
  check_valid_values "testsuite" "$TEST_SUITE" "${VALID_TEST_SUITES[@]}"
  check_valid_values "distro" "$DISTRO" "${VALID_DISTROS[@]}"
  check_valid_values "db" "$DATABASE" "${VALID_DATABASES[@]}"
  check_valid_values "runner" "$RUNNER" "${VALID_RUNNERS[@]}"
}

run_build () {
  PROFILES=(distro distro-webjar h2-in-memory)

  if [[ "$DISTRO" == "operaton" ]]; then
    PROFILES+=(distro-run integration-test-operaton-run)
  fi
  if [[ "$DISTRO" == "tomcat" ]]; then
    PROFILES+=(tomcat distro-tomcat)
    if [[ "$TEST_SUITE" == "engine" ]]; then
      MVN_ARGS+=(-Dskip.frontend.build=true)
    fi
  fi
  if [[ "$DISTRO" == "wildfly" ]]; then
    PROFILES+=(wildfly distro-wildfly)
    if [[ "$TEST_SUITE" == "engine" ]]; then
      MVN_ARGS+=(-Dskip.frontend.build=true)
    fi
  fi

  echo "ℹ️ Building $TEST_SUITE integration tests for distro $DISTRO with $DATABASE database using profiles: [${PROFILES[*]}]"
  echo "$RUNNER -DskipTests -P$(IFS=,; echo "${PROFILES[*]}") clean install"
  $RUNNER -DskipTests -P$(IFS=,; echo "${PROFILES[*]}") clean install
  if [[ $? -ne 0 ]]; then
    echo "❌ Error: Build failed"
    popd > /dev/null
    exit 1
  fi
}

##########################################################################
run_tests () {
  PROFILES=()
  MVN_ARGS+=(clean verify)

  case "$TEST_SUITE" in
    engine)
      PROFILES+=(engine-integration)
      ;;
    webapps)
      PROFILES+=(webapps-integration)
      ;;
  esac

  case "$DISTRO" in
    operaton)
      PROFILES+=(integration-test-operaton-run)
      MVN_ARGS+=(-f distro/run/qa)
      ;;
    tomcat)
      PROFILES+=(tomcat)
      MVN_ARGS+=(-f qa)
      ;;
    wildfly)
      PROFILES+=(wildfly)
      MVN_ARGS+=(-f qa)
      ;;
  esac

  PROFILES+=($DATABASE)

  echo "ℹ️ Running $TEST_SUITE integration tests for distro $DISTRO with $DATABASE database using profiles: [${PROFILES[*]}]"
  echo "$RUNNER -P$(IFS=,; echo "${PROFILES[*]}") $(echo "${MVN_ARGS[*]}")"
  $RUNNER -P$(IFS=,; echo "${PROFILES[*]}") $(echo "${MVN_ARGS[*]}")
  if [[ $? -ne 0 ]]; then
    echo "❌ Error: Build failed"
    popd > /dev/null
    exit 1
  else
    echo "✅ Integration tests completed successfully"
  fi
}

##########################################################################
# main script
parse_args "$@"

# Check if RUNNER exists, fallback if not
if ! command -v ${RUNNER} &> /dev/null; then
  echo "⚠️  Warning: Runner '${RUNNER}' not found. Falling back to './mvnw'."
  RUNNER="./mvnw"
fi

pushd $(pwd) > /dev/null
cd $(git rev-parse --show-toplevel) || exit 1

if [[ "$EXECUTE_BUILD" == true ]]; then
  run_build
else
  echo "ℹ️ Skipping build"
fi

if [[ "$EXECUTE_TEST" == true ]]; then
  run_tests
else
  echo "ℹ️ Skipping tests"
fi

popd > /dev/null
