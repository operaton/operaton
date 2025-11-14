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

echo "Creating a flag file 'executeJacoco' for each module containing tests. \
This triggers activation of the 'coverage' profile."

# Blacklist: Pfade (relativ zum Repo-Root, ohne führendes ./) die ignoriert werden sollen
BLACKLIST=(
  "qa/performance-tests-engine"
  "qa/integration-tests-webapps"
  "qa/integration-tests-engine"
  "webapps"
)

# Prüft ob ein normalisiertes Verzeichnis auf der Blacklist steht oder darunter liegt
is_blacklisted() {
  local dir_norm="$1"
  for bp in "${BLACKLIST[@]}"; do
    if [[ "$dir_norm" == "$bp" || "$dir_norm" == "$bp/"* ]]; then
      return 0
    fi
  done
  return 1
}

find . -type d | while read -r dir; do
  # Normalisieren: entferne führendes "./"
  dir_norm="${dir#./}"

  # Überspringe git-root "." und leere Strings
  if [[ -z "$dir_norm" || "$dir_norm" == "." ]]; then
    continue
  fi

  # Überspringe blacklisted Pfade
  if is_blacklisted "$dir_norm"; then
    # optional: echo "Skipping blacklisted path: $dir_norm"
    continue
  fi

  if [[ -d "$dir/src/test/java" || -d "$dir/target/generated-test-sources/java" ]]; then
    # Create an empty file target/executeJacoco if the condition is met
    mkdir -p "$dir/target"
    touch "$dir/target/executeJacoco"
  fi
done

echo "🚩 Created flag files for Jacoco"
