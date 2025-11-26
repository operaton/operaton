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

DATABASE="h2-in-memory"
RUNNER="./mvnw"
VALID_DATABASES=("h2-in-memory" "postgresql" "postgresql-xa" "mysql" "mariadb" "oracle" "db2" "db2-115" "sqlserver")
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
      --db=*)
        DATABASE="${1#*=}"
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
  check_valid_values "db" "$DATABASE" "${VALID_DATABASES[@]}"
  check_valid_values "runner" "$RUNNER" "${VALID_RUNNERS[@]}"
}

##########################################################################
run_tests () {
  PROFILES=($DATABASE)

  echo "ℹ️ Running unit tests for the engine with $DATABASE database using profiles: [${PROFILES[*]}]"
  echo "$RUNNER verify -pl engine -am -P$(IFS=,; echo "${PROFILES[*]}") verify"
  $RUNNER -P$(IFS=,; echo "${PROFILES[*]}") clean verify
  if [[ $? -ne 0 ]]; then
    echo "❌ Error: Tests failed"
    popd > /dev/null
    exit 1
  else
    echo "✅ Unit tests completed successfully"
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

run_tests

popd > /dev/null
