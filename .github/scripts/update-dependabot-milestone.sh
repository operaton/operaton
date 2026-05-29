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

# --- Step 2: Determine target branch ---
# COMPUTED_RELEASE_BRANCH is used only when the release comes from main (to find pre-existing entries)
COMPUTED_RELEASE_BRANCH="release/${MAJOR}.${MINOR}.x"

TARGET_BRANCH=""

if [[ "$RELEASE_TARGET_BRANCH" == release/* ]]; then
  # Patch release: use the actual source branch directly (not a computed value)
  HAS_ENTRIES=$(yq '.updates[]."target-branch"' "$DEPENDABOT_FILE" | grep -Fcx "$RELEASE_TARGET_BRANCH" || true)
  if [[ "$HAS_ENTRIES" -gt 0 ]]; then
    TARGET_BRANCH="$RELEASE_TARGET_BRANCH"
  else
    echo "::error::Release was tagged from '$RELEASE_TARGET_BRANCH' but no entries for '$RELEASE_TARGET_BRANCH' found in $DEPENDABOT_FILE."
    exit 1
  fi
elif [[ "$RELEASE_TARGET_BRANCH" == "main" ]]; then
  # Minor/major release from main: prefer pre-existing release branch entries
  HAS_RELEASE_BRANCH_ENTRIES=$(yq '.updates[]."target-branch"' "$DEPENDABOT_FILE" | grep -Fcx "$COMPUTED_RELEASE_BRANCH" || true)
  if [[ "$HAS_RELEASE_BRANCH_ENTRIES" -gt 0 ]]; then
    TARGET_BRANCH="$COMPUTED_RELEASE_BRANCH"
    echo "ℹ️  Release from main — found entries for '$COMPUTED_RELEASE_BRANCH', updating those."
  else
    TARGET_BRANCH="main"
    echo "ℹ️  Release from main — no '$COMPUTED_RELEASE_BRANCH' entries found, updating 'main'."
  fi
else
  echo "::error::Unexpected release source branch: '$RELEASE_TARGET_BRANCH'"
  exit 1
fi

echo "🎯 Target branch to update: $TARGET_BRANCH"

# --- Step 3: Compute next version candidates ---
NEXT_PATCH_VERSION="${MAJOR}.${MINOR}.$((PATCH + 1))"
NEXT_MINOR_VERSION="${MAJOR}.$((MINOR + 1)).0"

if [[ "$TARGET_BRANCH" == release/* ]]; then
  VERSION_CANDIDATES=("$NEXT_PATCH_VERSION" "$NEXT_MINOR_VERSION")
else
  VERSION_CANDIDATES=("$NEXT_MINOR_VERSION")
fi

echo "🔍 Milestone candidates: ${VERSION_CANDIDATES[*]}"

# --- Step 4: Milestone lookup via GitHub API ---
OWNER="${GITHUB_REPOSITORY%%/*}"
REPO="${GITHUB_REPOSITORY##*/}"

ALL_MILESTONES=$(gh api --paginate "repos/${OWNER}/${REPO}/milestones?state=open&per_page=100") || {
  echo "::error::GitHub API call failed. Check GITHUB_TOKEN is set and has repo access."
  exit 1
}

MILESTONE_NUMBER=""
MILESTONE_TITLE=""

for CANDIDATE in "${VERSION_CANDIDATES[@]}"; do
  FOUND_NUMBER=$(echo "$ALL_MILESTONES" | jq -r --arg title "$CANDIDATE" '.[] | select(.title == $title) | .number' | head -1)
  if [[ -n "$FOUND_NUMBER" ]]; then
    MILESTONE_NUMBER="$FOUND_NUMBER"
    MILESTONE_TITLE="$CANDIDATE"
    echo "✅ Found milestone '$CANDIDATE' (#$MILESTONE_NUMBER)"
    break
  fi
done

if [[ -z "$MILESTONE_NUMBER" ]]; then
  echo "::error::No open milestone found matching any of: ${VERSION_CANDIDATES[*]}. Please create the milestone before releasing."
  exit 1
fi
