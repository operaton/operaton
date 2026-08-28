#!/usr/bin/env bash
# Copyright 2026 the Operaton contributors.
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
#
# -------------------------------------------------------------------------------------------------
#
# Updates the Node.js and npm versions used by the build.
#
# The versions are maintained as the properties <version.nodejs> and <version.npm>
# in parent/pom.xml, from where the frontend-maven-plugin picks them up. This script
# sets them to the latest Node.js LTS release and to the npm version bundled with
# that release. When the Node.js major version changes, the hardcoded node-version
# inputs of the CI workflows are updated as well, so that the CI toolchain and the
# Maven build stay in lockstep.
#
# Options:
#   --pin-major       Stay on the Node.js major version that is currently configured.
#                     Used for maintenance branches, which must not jump to a new LTS line.
#   --index-url=<url> Override the Node.js release index. Only used for testing.
#
# When $GITHUB_OUTPUT is set, the following outputs are written:
#   changed, node_version, npm_version, major_changed
#
set -euo pipefail

INDEX_URL="https://nodejs.org/dist/index.json"
PIN_MAJOR=false

for arg in "$@"; do
  case "$arg" in
    --pin-major)
      PIN_MAJOR=true
      ;;
    --index-url=*)
      INDEX_URL="${arg#*=}"
      ;;
    *)
      echo "⚠️ Unknown option: $arg" >&2
      exit 1
      ;;
  esac
done

sed_inplace() {
  if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' -E "$@"
  else
    sed -i -E "$@"
  fi
}

emit_output() {
  if [ -n "${GITHUB_OUTPUT:-}" ]; then
    echo "$1=$2" >> "$GITHUB_OUTPUT"
  fi
}

cd "$(git rev-parse --show-toplevel)" || exit 1

POM="parent/pom.xml"
# Workflows that pin the Node.js major version for the CI toolchain. The Node.js
# version of slack-link-check.yml is unrelated to the build and stays untouched.
WORKFLOWS=(.github/workflows/build.yml .github/workflows/pr-build.yml)

extract_property() {
  sed -nE "s|.*<$1>(.+)</$1>.*|\1|p" "$POM" | head -1
}

CURRENT_NODE=$(extract_property "version.nodejs")
CURRENT_NPM=$(extract_property "version.npm")

if [ -z "$CURRENT_NODE" ] || [ -z "$CURRENT_NPM" ]; then
  echo "⚠️ Could not read <version.nodejs>/<version.npm> from $POM. Exiting..." >&2
  exit 1
fi

CURRENT_MAJOR="${CURRENT_NODE%%.*}"
echo "ℹ️ Currently configured: Node.js $CURRENT_NODE, npm $CURRENT_NPM"

echo "🔄 Fetching Node.js release index from $INDEX_URL"
if [[ "$INDEX_URL" == file://* ]]; then
  INDEX=$(cat "${INDEX_URL#file://}")
else
  INDEX=$(curl -fsSL "$INDEX_URL")
fi

# The index is ordered newest first, so the first LTS entry is the latest LTS release.
if [ "$PIN_MAJOR" = true ]; then
  echo "ℹ️ Restricting the search to the Node.js $CURRENT_MAJOR.x line"
  RELEASE=$(jq -r --arg major "v${CURRENT_MAJOR}." \
    'map(select(.lts != false and (.version | startswith($major)))) | .[0] // empty' <<< "$INDEX")
else
  RELEASE=$(jq -r 'map(select(.lts != false)) | .[0] // empty' <<< "$INDEX")
fi

if [ -z "$RELEASE" ]; then
  echo "⚠️ No matching Node.js LTS release found. Exiting..." >&2
  exit 1
fi

LATEST_NODE=$(jq -r '.version | ltrimstr("v")' <<< "$RELEASE")
LATEST_NPM=$(jq -r '.npm // empty' <<< "$RELEASE")

if [ -z "$LATEST_NPM" ]; then
  echo "⚠️ Node.js $LATEST_NODE does not declare a bundled npm version. Exiting..." >&2
  exit 1
fi

echo "ℹ️ Latest LTS release: Node.js $LATEST_NODE, bundled npm $LATEST_NPM"

if [ "$LATEST_NODE" = "$CURRENT_NODE" ] && [ "$LATEST_NPM" = "$CURRENT_NPM" ]; then
  echo "✅ Already up to date. Nothing to do."
  emit_output "changed" "false"
  emit_output "major_changed" "false"
  emit_output "node_version" "$LATEST_NODE"
  emit_output "npm_version" "$LATEST_NPM"
  exit 0
fi

echo "🔄 Updating $POM"
sed_inplace "s|<version.nodejs>[^<]+</version.nodejs>|<version.nodejs>${LATEST_NODE}</version.nodejs>|" "$POM"
sed_inplace "s|<version.npm>[^<]+</version.npm>|<version.npm>${LATEST_NPM}</version.npm>|" "$POM"

LATEST_MAJOR="${LATEST_NODE%%.*}"
MAJOR_CHANGED=false

if [ "$LATEST_MAJOR" != "$CURRENT_MAJOR" ]; then
  MAJOR_CHANGED=true
  for WORKFLOW in "${WORKFLOWS[@]}"; do
    echo "🔄 Updating node-version in $WORKFLOW"
    sed_inplace "s|^([[:space:]]*node-version:[[:space:]]*)${CURRENT_MAJOR}[[:space:]]*$|\1${LATEST_MAJOR}|" "$WORKFLOW"
  done
fi

emit_output "changed" "true"
emit_output "major_changed" "$MAJOR_CHANGED"
emit_output "node_version" "$LATEST_NODE"
emit_output "npm_version" "$LATEST_NPM"

echo "✅ Done! Node.js $CURRENT_NODE -> $LATEST_NODE, npm $CURRENT_NPM -> $LATEST_NPM"
