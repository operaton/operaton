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

# Checks that a CycloneDX SBOM covers every jar of a distribution archive.
#
# Usage: check-sbom-coverage.sh <sbom.json> <distribution.zip>
#
# Every jar, war, ear and rar in the archive is inspected, also those nested in
# other jars (Spring Boot fat jars, wars). A jar is covered when the SBOM lists
# its SHA-1 hash or the Maven coordinates of its own
# META-INF/maven/<groupId>/<artifactId>/pom.properties. Every further
# pom.properties in a jar belongs to a shaded library and must be listed by its
# coordinates as well. Exits with 1 and lists what is not covered otherwise.
# Requires jq, unzip, sha1sum and awk.
#
# The script starts few processes per jar, because that is slow on Windows.
# Lists go through files, not process substitution or here-strings: both can
# deadlock in Git Bash when the data gets large.

set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <sbom.json> <distribution.zip>" >&2
  exit 2
fi
SBOM="$1"
ARCHIVE="$2"

# Jars of the bundled servers that are not published as Maven artifacts, so no
# scanner can identify them. Such a jar is covered when the SBOM lists the
# server distribution it comes from, in the version from its path.
#   <regular expression for the path>|<package URL of the server, \1 = version from the path>
SERVER_ONLY_JARS=(
  '/apache-tomcat-([^/]+)/bin/bootstrap\.jar$|pkg:maven/org.apache.tomcat/tomcat@\1'
  '/apache-tomcat-([^/]+)/webapps/docs/appdev/sample/sample\.war$|pkg:maven/org.apache.tomcat/tomcat@\1'
  '/wildfly-([^/]+)/bin/client/jboss-cli-client\.jar$|pkg:maven/org.wildfly/wildfly-dist@\1'
  '/wildfly-([^/]+)/bin/client/jboss-client\.jar$|pkg:maven/org.wildfly/wildfly-dist@\1'
  '/wildfly-([^/]+)/bin/wildfly-elytron-tool\.jar$|pkg:maven/org.wildfly/wildfly-dist@\1'
  '/wildfly-([^/]+)/modules/system/layers/base/org/jboss/as/console/main/hal-console-[^/]+-resources\.jar$|pkg:maven/org.wildfly/wildfly-dist@\1'
)

WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

# Package URLs without qualifiers, and SHA-1 hashes of all components.
jq -r '[.components[]? | recurse(.components[]?)][]
       | (.purl // empty | split("#")[0] | split("?")[0]),
         (.hashes[]? | select(.alg == "SHA-1") | .content | ascii_downcase)' < "$SBOM" > "$WORK_DIR/known.txt"

# All archives found so far: files on disk and the paths shown in reports.
FILES=()
LOCATIONS=()
queue_archives() {
  find "$1" -type f \( -name '*.jar' -o -name '*.war' -o -name '*.ear' -o -name '*.rar' \) | sort > "$WORK_DIR/found.txt"
  local FOUND
  while IFS= read -r FOUND; do
    FILES+=("$FOUND")
    LOCATIONS+=("$2${FOUND#"$1/"}")
  done < "$WORK_DIR/found.txt"
}

mkdir "$WORK_DIR/0"
unzip -q "$ARCHIVE" -d "$WORK_DIR/0"
queue_archives "$WORK_DIR/0" "${ARCHIVE##*/}!/"

# Collects "jar <index> <own coordinates>" and "shaded <index> <coordinates>"
# records, which are checked against the SBOM at the end.
RECORDS="$WORK_DIR/records.tsv"
: > "$RECORDS"
for ((i = 0; i < ${#FILES[@]}; i++)); do
  FILE="${FILES[$i]}"
  if ! unzip -Z1 "$FILE" > "$WORK_DIR/entries.txt" 2>/dev/null; then
    printf 'jar\t%s\t\n' "$i" >> "$RECORDS"
    continue
  fi

  POMS=()
  grep -E '^META-INF/maven/[^/]+/[^/]+/pom\.properties$' "$WORK_DIR/entries.txt" > "$WORK_DIR/poms.txt" || true
  while IFS= read -r ENTRY; do
    POMS+=("$ENTRY")
  done < "$WORK_DIR/poms.txt"
  NESTED_NAMES=()
  grep -E '\.(jar|war|ear|rar)$' "$WORK_DIR/entries.txt" > "$WORK_DIR/nested.txt" || true
  while IFS= read -r ENTRY; do
    NESTED_NAMES+=("$ENTRY")
  done < "$WORK_DIR/nested.txt"

  # unzip -c prints a header line before the content of each file.
  COORDINATES=()
  if [[ ${#POMS[@]} -gt 0 ]]; then
    unzip -c "$FILE" "${POMS[@]}" > "$WORK_DIR/poms-content.txt"
    GROUP_ID="" ARTIFACT_ID="" VERSION=""
    while IFS= read -r LINE; do
      LINE="${LINE%$'\r'}"
      case "$LINE" in
        groupId=*) GROUP_ID="${LINE#groupId=}" ;;
        artifactId=*) ARTIFACT_ID="${LINE#artifactId=}" ;;
        version=*) VERSION="${LINE#version=}" ;;
        *" inflating: "* | *" extracting: "*)
          if [[ -n "$GROUP_ID" && -n "$ARTIFACT_ID" && -n "$VERSION" ]]; then
            COORDINATES+=("pkg:maven/$GROUP_ID/$ARTIFACT_ID@$VERSION")
          fi
          GROUP_ID="" ARTIFACT_ID="" VERSION=""
          ;;
      esac
    done < "$WORK_DIR/poms-content.txt"
    if [[ -n "$GROUP_ID" && -n "$ARTIFACT_ID" && -n "$VERSION" ]]; then
      COORDINATES+=("pkg:maven/$GROUP_ID/$ARTIFACT_ID@$VERSION")
    fi
  fi

  # The jar's own coordinates are those its file name is built from, or the
  # only ones it contains.
  NAME="${FILE##*/}"
  OWN=""
  for PURL in "${COORDINATES[@]+"${COORDINATES[@]}"}"; do
    ARTIFACT_VERSION="${PURL##*/}"
    if [[ "$NAME" == "${ARTIFACT_VERSION/@/-}"* ]]; then
      OWN="$PURL"
    fi
  done
  if [[ -z "$OWN" && ${#COORDINATES[@]} -eq 1 ]]; then
    OWN="${COORDINATES[0]}"
  fi
  SERVER=""
  for RULE in "${SERVER_ONLY_JARS[@]}"; do
    if [[ -z "$OWN" && "${LOCATIONS[$i]}" =~ ${RULE%%|*} ]]; then
      SERVER="${RULE#*|}"
      SERVER="${SERVER/\\1/${BASH_REMATCH[1]}}"
    fi
  done
  printf 'jar\t%s\t%s\t%s\n' "$i" "$OWN" "$SERVER" >> "$RECORDS"
  for PURL in "${COORDINATES[@]+"${COORDINATES[@]}"}"; do
    if [[ "$PURL" != "$OWN" ]]; then
      printf 'shaded\t%s\t%s\n' "$i" "$PURL" >> "$RECORDS"
    fi
  done

  # Queue the archives nested in this one. Their names are passed one by one,
  # because not every unzip build lets a wildcard match across directories.
  if [[ ${#NESTED_NAMES[@]} -gt 0 ]]; then
    NESTED_DIR="$WORK_DIR/$((i + 1))"
    mkdir "$NESTED_DIR"
    # Exit code 1 means warnings only, e.g. a launch script in front of the jar.
    unzip -qq "$FILE" "${NESTED_NAMES[@]}" -d "$NESTED_DIR" || [[ $? -eq 1 ]]
    queue_archives "$NESTED_DIR" "${LOCATIONS[$i]}!/"
  fi
done

# sha1sum keeps the order of its arguments, so line n is the hash of FILES[n].
printf '%s\0' "${FILES[@]}" | xargs -0 sha1sum | cut -d ' ' -f 1 | sed 's/^\\//' > "$WORK_DIR/sha1.txt"
printf '%s\n' "${LOCATIONS[@]}" > "$WORK_DIR/locations.txt"

awk -F '\t' -v known="$WORK_DIR/known.txt" -v sha1="$WORK_DIR/sha1.txt" -v locations="$WORK_DIR/locations.txt" '
  BEGIN {
    while ((getline line < known) > 0) covered[line] = 1
    n = 0; while ((getline line < sha1) > 0) hash[n++] = line
    n = 0; while ((getline line < locations) > 0) location[n++] = line
  }
  $1 == "jar" && !(hash[$2] in covered) && !($3 != "" && ($3 in covered)) && !($4 != "" && ($4 in covered)) {
    print location[$2] " (" ($3 != "" ? $3 : $4 != "" ? "part of " $4 : "no pom.properties") ", SHA-1 " hash[$2] ")"
  }
  $1 == "shaded" && !($3 in covered) {
    print $3 " (shaded in " location[$2] ")"
  }
' "$RECORDS" > "$WORK_DIR/missing.txt"

MISSING=$(grep -c . "$WORK_DIR/missing.txt" || true)
echo "${ARCHIVE##*/}: ${#FILES[@]} jars, $(grep -c '^shaded' "$RECORDS" || true) shaded libraries, $MISSING not covered by $SBOM"
sed 's/^/    /' "$WORK_DIR/missing.txt"
[[ "$MISSING" -eq 0 ]]
