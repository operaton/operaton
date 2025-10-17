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

# Script to build REST API documentation using redocly

API_SPEC_PATH="engine-rest/engine-rest-openapi/target/generated-sources/openapi-json/openapi.json"
if [[ ! -f "$API_SPEC_PATH" ]]; then
  echo "Info: API spec file not found at $API_SPEC_PATH. Generating it using Maven..."
  ./mvnw -DskipTests -am -pl engine-rest/engine-rest-openapi verify
fi

# Extract version from the OpenAPI spec, removing "-SNAPSHOT" and patch version
VERSION=$(jq -r '.info.version | sub("-SNAPSHOT"; "")' "$API_SPEC_PATH" | sed 's/\.[0-9]*$//')
INDEX_PAGE=engine-rest/engine-rest-openapi/src/main/redocly/index.html
TARGET_DIR="target/rest-api/${VERSION}"

mkdir -p "$TARGET_DIR"
cp $INDEX_PAGE $TARGET_DIR
cp $API_SPEC_PATH $TARGET_DIR/operaton-rest-api.json
echo "REST API documentation copied to: $TARGET_DIR"
