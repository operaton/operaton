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

# Check if npx is installed
if ! command -v npx >/dev/null 2>&1; then
  echo "Error: npx is not installed. Please install Node.js and npm (which includes npx)."
  exit 2
fi

VERSION=""

API_SPEC_PATH="engine-rest/engine-rest-openapi/target/generated-sources/openapi-json/openapi.json"
if [[ ! -f "$API_SPEC_PATH" ]]; then
  echo "Info: API spec file not found at $API_SPEC_PATH. Generating it using Maven..."
  ./mvnw -DskipTests -am -pl engine-rest/engine-rest-openapi verify
fi

VERSION=$(jq -r '.info.version | sub("-SNAPSHOT"; "")' "$API_SPEC_PATH")
TARGET_FILE="target/rest-api/${VERSION}/index.html"

npx @redocly/cli build-docs $API_SPEC_PATH --output $TARGET_FILE
echo "REST API documentation generated at: $TARGET_FILE"
