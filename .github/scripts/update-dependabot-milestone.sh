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
# Public iCal feed of the Operaton Release Calendar; used for due dates of newly created milestones
RELEASE_CALENDAR_ICS="${RELEASE_CALENDAR_ICS:-https://calendar.google.com/calendar/ical/2492fa418b8818bcb8137cf3e0ea3dcf8e13faa03ef7fecc5b0307b732dd5eca%40group.calendar.google.com/public/basic.ics}"

OWNER="${GITHUB_REPOSITORY%%/*}"
REPO="${GITHUB_REPOSITORY##*/}"

# --- Helper: write a key=value pair to GITHUB_ENV if available ---
export_env() {
  local key="$1" val="$2"
  if [[ -n "${GITHUB_ENV:-}" ]]; then
    echo "$key=$val" >> "$GITHUB_ENV"
  fi
}

# --- Helper: number of dependabot entries for a target branch ---
count_entries() {
  yq '.updates[]."target-branch"' "$DEPENDABOT_FILE" | grep -Fcx "$1" || true
}

# --- Helper: number of the open milestone with the given title (empty if none) ---
find_milestone() {
  echo "$ALL_MILESTONES" | jq -r --arg title "$1" '.[] | select(.title == $title) | .number' | head -1
}

# --- Helper: due date (YYYY-MM-DD) of 'Operaton <version>' in the release calendar (empty if none) ---
calendar_due_date() {
  local version="$1" ics
  if ! ics=$(curl -sfL --max-time 30 "$RELEASE_CALENDAR_ICS"); then
    echo "::warning::Could not read the release calendar; milestone '$version' gets no due date." >&2
    return 0
  fi
  tr -d '\r' <<< "$ics" | awk -v version="$version" '
    /^BEGIN:VEVENT/ { summary = ""; start = "" }
    /^SUMMARY:/     { summary = substr($0, 9) }
    /^DTSTART/      { sub(/^[^:]*:/, ""); start = substr($0, 1, 8) }
    /^END:VEVENT/ {
      split(summary, words, " ")
      if (words[1] == "Operaton" && words[2] == version && start ~ /^[0-9]{8}$/) {
        print substr(start, 1, 4) "-" substr(start, 5, 2) "-" substr(start, 7, 2)
        exit
      }
    }'
}

# --- Helper: number of the open milestone with the given title; creates it if missing ---
ensure_milestone() {
  local title="$1" number due_date
  local -a fields=(-f "title=$title")
  number=$(find_milestone "$title")
  if [[ -n "$number" ]]; then
    echo "✅ Found milestone '$title' (#$number)" >&2
    echo "$number"
    return 0
  fi
  due_date=$(calendar_due_date "$title")
  if [[ -n "$due_date" ]]; then
    fields+=(-f "due_on=${due_date}T00:00:00Z")
  fi
  number=$(gh api -X POST "repos/${OWNER}/${REPO}/milestones" "${fields[@]}" --jq '.number') || {
    echo "::error::Could not create milestone '$title'. Check GITHUB_TOKEN has 'issues: write' permission." >&2
    return 1
  }
  echo "🆕 Created milestone '$title' (#$number, due: ${due_date:-none})" >&2
  echo "$number"
}

# --- Helper: next release version from the pom.xml on the given branch (X.Y.Z-SNAPSHOT → X.Y.Z) ---
next_version_from_pom() {
  local branch="$1" version
  git fetch --quiet --depth=1 origin "$branch"
  version=$(git show FETCH_HEAD:pom.xml | yq -p xml '.project.version')
  version="${version%-SNAPSHOT}"
  if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "::error::Unrecognized project version '$version' in pom.xml on '$branch'." >&2
    return 1
  fi
  echo "$version"
}

# --- Helper: set the milestone of all entries for a target branch ---
# Edits the file textually; 'yq -i' would reformat the whole file.
set_milestone() {
  local branch="$1" milestone="$2" tmp
  tmp=$(mktemp)
  awk -v branch="$branch" -v milestone="$milestone" '
    function flush(   i) {
      for (i = 1; i <= n; i++) {
        if (matched && lines[i] ~ /^    milestone:/) sub(/milestone:.*/, "milestone: " milestone, lines[i])
        print lines[i]
      }
      n = 0; matched = 0
    }
    /^  - / || /^[^ ]/ { flush() }
    {
      lines[++n] = $0
      value = $0
      if (sub(/^    target-branch:[ ]*/, "", value)) {
        gsub(/["\047]|[ ]+$/, "", value)
        if (value == branch) matched = 1
      }
    }
    END { flush() }' "$DEPENDABOT_FILE" > "$tmp"
  mv "$tmp" "$DEPENDABOT_FILE"
}

# --- Helper: append entries for a new release branch, cloned from an existing release branch ---
append_release_entries() {
  local template_branch="$1" new_branch="$2" milestone="$3" entries
  entries=$(yq '[.updates[] | select(."target-branch" == "'"$template_branch"'")
      | ."target-branch" = "'"$new_branch"'" | .milestone = '"$milestone"']' "$DEPENDABOT_FILE" | sed 's/^/  /')
  printf '\n# Release %s branch - all updates\n%s\n' "${new_branch#release/}" "$entries" >> "$DEPENDABOT_FILE"
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
TARGET_BRANCH=""
NEW_RELEASE_LINE=false

if [[ "$RELEASE_TARGET_BRANCH" == release/* ]]; then
  # Patch release: use the actual source branch directly (not a computed value)
  TARGET_BRANCH="$RELEASE_TARGET_BRANCH"
  if [[ "$(count_entries "$RELEASE_TARGET_BRANCH")" -eq 0 ]]; then
    # First release from this branch: its entries get created below
    NEW_RELEASE_LINE=true
    echo "ℹ️  No entries for '$RELEASE_TARGET_BRANCH' in $DEPENDABOT_FILE — creating them."
  fi
elif [[ "$RELEASE_TARGET_BRANCH" == "main" ]]; then
  # Minor/major release from main: update main entries to the next minor milestone
  TARGET_BRANCH="main"
  echo "ℹ️  Release from main — updating 'main'."
else
  echo "::error::Unexpected release source branch: '$RELEASE_TARGET_BRANCH'"
  exit 1
fi

echo "🎯 Target branch to update: $TARGET_BRANCH"

ALL_MILESTONES=$(gh api --paginate "repos/${OWNER}/${REPO}/milestones?state=open&per_page=100" | jq -s 'add // []') || {
  echo "::error::GitHub API call failed. Check GITHUB_TOKEN is set and has repo access."
  exit 1
}

NEXT_PATCH_VERSION="${MAJOR}.${MINOR}.$((PATCH + 1))"
NEXT_MINOR_VERSION="${MAJOR}.$((MINOR + 1)).0"

if [[ "$NEW_RELEASE_LINE" == true ]]; then
  # --- Step 3a: New release line: create entries for the release branch ---
  MILESTONE_TITLE=$(next_version_from_pom "$RELEASE_TARGET_BRANCH")
  echo "🔍 Next version on '$RELEASE_TARGET_BRANCH': $MILESTONE_TITLE"
  MILESTONE_NUMBER=$(ensure_milestone "$MILESTONE_TITLE")

  TEMPLATE_BRANCH=$(yq '.updates[]."target-branch"' "$DEPENDABOT_FILE" | grep '^release/' | sort -uV | tail -1 || true)
  if [[ -z "$TEMPLATE_BRANCH" ]]; then
    echo "::error::No existing release branch entries in $DEPENDABOT_FILE to use as template for '$RELEASE_TARGET_BRANCH'."
    exit 1
  fi

  echo "📝 Adding entries for '$RELEASE_TARGET_BRANCH' (cloned from '$TEMPLATE_BRANCH') → $MILESTONE_NUMBER ($MILESTONE_TITLE)..."
  append_release_entries "$TEMPLATE_BRANCH" "$RELEASE_TARGET_BRANCH" "$MILESTONE_NUMBER"
  CHANGE_SUMMARY="adds entries for \`$RELEASE_TARGET_BRANCH\` with milestone **$MILESTONE_TITLE**"

  # A release line starts with its X.Y.0 release; main then continues with the next minor
  if [[ "$PATCH" -eq 0 ]]; then
    MAIN_MILESTONE_NUMBER=$(find_milestone "$NEXT_MINOR_VERSION")
    if [[ -n "$MAIN_MILESTONE_NUMBER" ]]; then
      echo "📝 Updating milestone for target-branch 'main' → $MAIN_MILESTONE_NUMBER ($NEXT_MINOR_VERSION)..."
      set_milestone "main" "$MAIN_MILESTONE_NUMBER"
      CHANGE_SUMMARY="$CHANGE_SUMMARY and sets milestone **$NEXT_MINOR_VERSION** for \`main\`"
    else
      echo "::warning::No open milestone '$NEXT_MINOR_VERSION' found; 'main' entries left unchanged."
    fi
  fi
else
  # --- Step 3b: Existing entries: move them to the next milestone ---
  if [[ "$TARGET_BRANCH" == release/* ]]; then
    VERSION_CANDIDATES=("$NEXT_PATCH_VERSION" "$NEXT_MINOR_VERSION")
  else
    VERSION_CANDIDATES=("$NEXT_MINOR_VERSION")
  fi

  echo "🔍 Milestone candidates: ${VERSION_CANDIDATES[*]}"

  MILESTONE_NUMBER=""
  MILESTONE_TITLE=""

  for CANDIDATE in "${VERSION_CANDIDATES[@]}"; do
    FOUND_NUMBER=$(find_milestone "$CANDIDATE")
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

  echo "📝 Updating milestone for target-branch '$TARGET_BRANCH' → $MILESTONE_NUMBER ($MILESTONE_TITLE)..."
  set_milestone "$TARGET_BRANCH" "$MILESTONE_NUMBER"
  CHANGE_SUMMARY="sets milestone **$MILESTONE_TITLE** for \`$TARGET_BRANCH\` entries"
fi

# Verify the update actually changed something
if git diff --quiet "$DEPENDABOT_FILE"; then
  echo "::error::No change on $DEPENDABOT_FILE. No entries matched target-branch '$TARGET_BRANCH' or milestone was already $MILESTONE_NUMBER."
  exit 1
fi

echo "✅ $DEPENDABOT_FILE updated."

# --- Step 4: Export for workflow PR step ---
export_env "TARGET_BRANCH"    "$TARGET_BRANCH"
export_env "RELEASE_VERSION"  "$VERSION"
export_env "MILESTONE_TITLE"  "$MILESTONE_TITLE"
export_env "CHANGE_SUMMARY"   "$CHANGE_SUMMARY"

echo "📤 Exported: TARGET_BRANCH=$TARGET_BRANCH, RELEASE_VERSION=$VERSION, MILESTONE_TITLE=$MILESTONE_TITLE"
