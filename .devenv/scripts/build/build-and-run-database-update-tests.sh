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
DATABASE="h2"
VALID_DATABASES=("h2" "postgresql" "postgresql-xa" "mysql" "mariadb" "oracle" "db2" "sqlserver")

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
      --db=*)
        DATABASE="${1#*=}"
        ;;
      --no-build)
        EXECUTE_BUILD=false
        ;;
      --no-test)
        EXECUTE_TEST=false
        ;;
    esac
    shift
  done

  check_valid_values "db" "$DATABASE" "${VALID_DATABASES[@]}"
}

run_build () {
  echo "ℹ️ Building BOMs and engine modules"
  echo "./mvnw install -f bom && ./mvnw install -DskipTests -am -pl distro/sql-script"
  ./mvnw install -f bom && ./mvnw install -DskipTests -am -pl distro/sql-script
  if [[ $? -ne 0 ]]; then
    echo "❌ Error: Build failed"
    popd > /dev/null
    exit 1
  fi
}

##########################################################################
run_tests () {
  PROFILES=(rolling-update $DATABASE)
  MVN_ARGS+=(clean install)

  echo "ℹ️ Running database update tests for database $DATABASE using profiles: [${PROFILES[*]}]"
  echo "./mvnw -P$(IFS=,; echo "${PROFILES[*]}") $(echo "${MVN_ARGS[*]}") -f qa"
  ./mvnw -P$(IFS=,; echo "${PROFILES[*]}") $(echo "${MVN_ARGS[*]}") -f qa
  if [[ $? -ne 0 ]]; then
    echo "❌ Error: Build failed"
    popd > /dev/null
    exit 1
  else
    echo "✅ Database update tests completed successfully"
  fi
}

##########################################################################
# main script
parse_args "$@"

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
