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

set -euo pipefail

# --- Inputs ---
RELEASE_TAG="${RELEASE_TAG:?RELEASE_TAG is required}"
RELEASE_TARGET_BRANCH="${RELEASE_TARGET_BRANCH:?RELEASE_TARGET_BRANCH is required}"
GITHUB_REPOSITORY="${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
# GITHUB_TOKEN is used implicitly by the gh CLI

DEPENDABOT_FILE=".github/dependabot.yml"

# --- Helper: write a key=value pair to GITHUB_ENV if available ---
export_env() {
  local key="$1" val="$2"
  if [[ -n "${GITHUB_ENV:-}" ]]; then
    echo "$key=$val" >> "$GITHUB_ENV"
  fi
}

# --- Step 1: Parse version ---
VERSION="${RELEASE_TAG#v}"

# Secondary guard: exit 0 (silent cancel) for preliminary qualifiers
if [[ "$VERSION" =~ -M[0-9]+|-RC[0-9]+ ]]; then
  echo "ℹ️  Preliminary release '$VERSION' detected — skipping."
  exit 0
fi

if [[ ! "$VERSION" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  echo "::error::Unrecognized version format: '$VERSION'"
  exit 1
fi

MAJOR="${BASH_REMATCH[1]}"
MINOR="${BASH_REMATCH[2]}"
PATCH="${BASH_REMATCH[3]}"

echo "📦 Release version: $VERSION (MAJOR=$MAJOR, MINOR=$MINOR, PATCH=$PATCH)"
echo "🌿 Release source branch: $RELEASE_TARGET_BRANCH"
