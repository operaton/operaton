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
# Lists the branches that are under maintenance, together with the milestone that pull
# requests against them should be assigned to.
#
# The maintained branches are `main` plus every `release/*` branch that exists on the
# remote. The milestone is looked up in .github/dependabot.yml, which already maps a
# target branch to its milestone and is kept current by the Update Dependabot Milestone
# workflow. Branches without an entry there get no milestone.
#
# Options:
#   --dependabot-file=<path>  Override the Dependabot configuration. Only used for testing.
#
# Prints a JSON array of objects with the keys `branch`, `slug`, `maintenance` and
# `milestone` and, when $GITHUB_OUTPUT is set, writes it as the output `branches`.
# `maintenance` is false for `main` and true for the release branches.
#
set -euo pipefail

DEPENDABOT_FILE=".github/dependabot.yml"

for arg in "$@"; do
  case "$arg" in
    --dependabot-file=*)
      DEPENDABOT_FILE="${arg#*=}"
      ;;
    *)
      echo "⚠️ Unknown option: $arg" >&2
      exit 1
      ;;
  esac
done

cd "$(git rev-parse --show-toplevel)" || exit 1

if [ ! -f "$DEPENDABOT_FILE" ]; then
  echo "⚠️ $DEPENDABOT_FILE not found. Exiting..." >&2
  exit 1
fi

BRANCHES=("main")
while IFS= read -r BRANCH; do
  [ -n "$BRANCH" ] && BRANCHES+=("$BRANCH")
done < <(git for-each-ref --format='%(refname:short)' refs/remotes/origin/release/ | sed 's|^origin/||' | sort -u)

RESULT="[]"

for BRANCH in "${BRANCHES[@]}"; do
  MILESTONE=$(yq "[.updates[] | select(.\"target-branch\" == \"$BRANCH\" and has(\"milestone\")) | .milestone] | .[0] // \"\"" "$DEPENDABOT_FILE")
  if [ "$MILESTONE" = "null" ]; then
    MILESTONE=""
  fi

  if [ "$BRANCH" = "main" ]; then
    MAINTENANCE=false
  else
    MAINTENANCE=true
  fi

  if [ -n "$MILESTONE" ]; then
    echo "🌿 $BRANCH (maintenance: $MAINTENANCE, milestone: $MILESTONE)"
  else
    echo "🌿 $BRANCH (maintenance: $MAINTENANCE, no milestone found in $DEPENDABOT_FILE)"
  fi

  RESULT=$(jq -c \
    --arg branch "$BRANCH" \
    --arg slug "${BRANCH//\//-}" \
    --argjson maintenance "$MAINTENANCE" \
    --arg milestone "$MILESTONE" \
    '. + [{branch: $branch, slug: $slug, maintenance: $maintenance, milestone: $milestone}]' \
    <<< "$RESULT")
done

echo "$RESULT"

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "branches=$RESULT" >> "$GITHUB_OUTPUT"
fi
