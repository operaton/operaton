#!/usr/bin/env bash
#
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
# Records the resolved dependencies and plugins of every reactor module for a set of
# profile combinations. Used to verify that build refactorings do not change resolution.
#
# Usage:
#   dependency-snapshot.sh <label>                  record snapshot <label>
#   dependency-snapshot.sh --diff <label1> <label2> compare two snapshots
#
# Snapshots are written to $SNAPSHOT_DIR (default: $TMPDIR/operaton-dependency-snapshots).
# Set PROFILE_SET to record only one profile set (e.g. PROFILE_SET=default), or
# SKIP_SETS to a space-separated list of profile sets to leave out.
# Set EXTRA_ARGS to pass additional arguments to every Maven invocation.

set -euo pipefail

SNAPSHOT_DIR="${SNAPSHOT_DIR:-${TMPDIR:-/tmp}/operaton-dependency-snapshots}"
EXTRA_ARGS="${EXTRA_ARGS:-}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

PROFILE_SETS=(
  "default="
  "release=-Psonatype-oss-release"
  "distro=-Pdistro,distro-run,distro-tomcat,distro-wildfly,distro-starter,distro-webjar,distro-webjar-neo"
  "it-engine-tomcat=-Pdistro,engine-integration,tomcat,h2"
  "it-webapps-wildfly=-Pdistro,webapps-integration,wildfly,postgresql"
  "migration=-Pinstance-migration,rolling-update,h2"
)

# Turns maven log output into "<artifactId> <resolved entry>" lines, sorted.
extract() {
  local marker="$1"
  awk -v marker="$marker" '
    /--- dependency:[^ ]*:(list|resolve-plugins) .* @ / {
      match($0, /@ [^ ]+/); module = substr($0, RSTART + 2, RLENGTH - 2); inlist = 0; next
    }
    index($0, marker) { inlist = 1; next }
    inlist && /\[INFO\]    [^ ]/ {
      line = $0; sub(/.*\[INFO\]    /, "", line); sub(/ -- module.*/, "", line)
      print module " " line; next
    }
    inlist { inlist = 0 }
  ' | sort -u
}

record() {
  local label="$1" out="$SNAPSHOT_DIR/$1"
  mkdir -p "$out"
  for entry in "${PROFILE_SETS[@]}"; do
    local name="${entry%%=*}" profiles="${entry#*=}"
    local log="$out/build-$name.log"
    if [[ -n "${PROFILE_SET:-}" && "$PROFILE_SET" != "$name" ]] || [[ " ${SKIP_SETS:-} " == *" $name "* ]]; then
      continue
    fi
    echo "### [$label] profile set '$name' ($profiles)"
    # shellcheck disable=SC2086
    (cd "$ROOT_DIR" && ./mvnw install dependency:list dependency:resolve-plugins \
      -DskipTests -DskipITs=true -Dskip.frontend.build=true -Dmaven.build.cache.enabled=false \
      -Dsort=true $profiles $EXTRA_ARGS) > "$log" 2>&1 || { echo "Build failed, see $log"; exit 1; }
    extract "The following files have been resolved:" < "$log" > "$out/deps-$name.txt"
    extract "The following plugins have been resolved:" < "$log" > "$out/plugins-$name.txt"
    echo "    $(wc -l < "$out/deps-$name.txt") dependency lines, $(wc -l < "$out/plugins-$name.txt") plugin lines"
  done
}

compare() {
  diff -r -u -x 'build-*.log' "$SNAPSHOT_DIR/$1" "$SNAPSHOT_DIR/$2"
}

case "${1:-}" in
  --diff) compare "$2" "$3" ;;
  "") echo "Usage: $0 <label> | --diff <label1> <label2>"; exit 2 ;;
  *) record "$1" ;;
esac
