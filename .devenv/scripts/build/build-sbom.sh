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

# Generates the SBOMs shipped with the run, tomcat and wildfly distributions
# (distro/*/assembly/resources/sbom.cdx.json). Each SBOM combines
#   - the Maven dependency tree (cyclonedx-maven-plugin),
#   - the npm dependencies of the frontends (trivy fs),
#   - every jar in the assembled distribution archive (trivy rootfs), which
#     adds what the Maven tree does not show: libraries shaded into other
#     jars, and jars that come from bundled servers.
# Finally every jar in the archive is checked to be covered by the SBOM.
#
# The distributions must be built first, so that the archives can be scanned
# and the Maven plugin can resolve every Operaton module, for example:
#   ./mvnw install -DskipTests -Dskip.frontend.build=true \
#     -Pdistro,distro-run,distro-tomcat,distro-wildfly,distro-webjar,distro-webjar-neo,distro-starter,distro-serverless
# The archives then have to be assembled again to ship the new SBOMs.

set -euo pipefail
echo "Generating SBOM files for Operaton distributions..."

# Pinned CycloneDX CLI version used for CI installs (see GitHub releases).
CYCLONEDX_VERSION="v0.33.1"
# Pinned trivy image, so that the inventory does not change with the scanner.
TRIVY_IMAGE="aquasec/trivy:0.76.2"
# Trivy downloads its Java DB (about 1 GB) to identify jars without Maven
# metadata by their SHA-1. The cache directory keeps it between runs.
TRIVY_CACHE_DIR="${TRIVY_CACHE_DIR:-$HOME/.cache/trivy}"

DISTROS=("run" "tomcat" "wildfly")

# jq program that reads an SBOM and the scan of a distribution, and writes the
# SBOM with the components only the scan found. Components are matched by
# their package URL without qualifiers (the Maven plugin adds "?type=jar",
# trivy does not), so each library is listed once.
ADD_DISTRIBUTION_COMPONENTS='
  def normalize: split("#")[0] | split("?")[0];
  def all_components: [.components[]? | recurse(.components[]?)];
  input as $sbom
  | input as $scan
  | ($sbom | all_components) as $existing
  | ([$existing[] | select(.purl) | {key: (.purl | normalize), value: true}] | from_entries) as $known
  | ([$existing[] | select(."bom-ref") | {key: ."bom-ref", value: true}] | from_entries) as $refs
  | ([$scan | all_components[] | .hashes[]?.content] | group_by(.) | map({key: .[0], value: length}) | from_entries) as $sharing
  # Trivy reports a library shaded into a jar with the hash of the host jar.
  # Only the component the jar file is named after keeps that hash.
  | def own_hashes:
      . as $c
      | [.hashes[]? | select($sharing[.content] == 1
          or any($c.properties[]? | select(.name == "aquasecurity:trivy:FilePath") | .value | split("/") | last;
                 startswith("\($c.name)-\($c.version)")))];
    [$scan | all_components[]
     | select(.type == "library" and .purl and ($known[.purl | normalize] | not))]
  | group_by(.purl | normalize)
  | map((.[0].purl | normalize) as $purl
        | ([.[] | own_hashes | select(length > 0)] | first // []) as $hashes
        | .[0]
          + {"bom-ref": (if $refs[$purl] then "\($purl)#distribution" else $purl end),
             properties: ([.[].properties[]?] | unique),
             hashes: $hashes}
        | del(.components)
        | if .hashes == [] then del(.hashes) else . end) as $added
  | $sbom
  | .components += $added
  | .metadata.component.name = $name
  | del(.vulnerabilities)
'

# The distribution archive built by distro/<distro>/distro.
distro_archive() {
  case "$1" in
    run) echo "distro/run/distro/target/operaton-bpm-$2.zip" ;;
    *) echo "distro/$1/distro/target/operaton-bpm-$1-$2.zip" ;;
  esac
}

VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout | tail -n 1)
for DISTRO in "${DISTROS[@]}"; do
  ARCHIVE=$(distro_archive "$DISTRO" "$VERSION")
  if [[ ! -f "$ARCHIVE" ]]; then
    echo "❌ $ARCHIVE not found. Build the distributions first, see the comment at the top of $0." >&2
    exit 1
  fi
done

for TOOL in docker jq unzip sha1sum; do
  if ! command -v "$TOOL" >/dev/null 2>&1; then
    echo "❌ $TOOL is required to build the SBOMs." >&2
    exit 1
  fi
done

if command -v cyclonedx >/dev/null 2>&1; then
  echo "CycloneDX CLI already available."
else
  if [[ "${CI:-}" != "" ]]; then
    # Download the pinned binary directly from GitHub releases. Homebrew now
    # refuses the untrusted cyclonedx/cyclonedx tap in CI, so we bypass it.
    case "$(uname -m)" in
      x86_64|amd64) CYCLONEDX_ARCH="x64" ;;
      aarch64|arm64) CYCLONEDX_ARCH="arm64" ;;
      *) echo "❌ Unsupported CI architecture: $(uname -m)" >&2; exit 1 ;;
    esac
    echo "Installing CycloneDX CLI ${CYCLONEDX_VERSION} (cyclonedx-linux-${CYCLONEDX_ARCH}) for CI environment..."
    CYCLONEDX_BIN_DIR="${RUNNER_TEMP:-/tmp}/cyclonedx-cli"
    mkdir -p "$CYCLONEDX_BIN_DIR"
    curl -fsSL -o "$CYCLONEDX_BIN_DIR/cyclonedx" \
      "https://github.com/CycloneDX/cyclonedx-cli/releases/download/${CYCLONEDX_VERSION}/cyclonedx-linux-${CYCLONEDX_ARCH}"
    chmod +x "$CYCLONEDX_BIN_DIR/cyclonedx"
    export PATH="$CYCLONEDX_BIN_DIR:$PATH"
  elif command -v brew >/dev/null 2>&1; then
    echo "CycloneDX CLI not found — installing via Homebrew..."
    brew install cyclonedx/cyclonedx/cyclonedx-cli
  else
    echo "Homebrew not found. Please install Homebrew or install CycloneDX CLI manually:" >&2
    echo "  https://brew.sh/" >&2
    echo "Or install the CycloneDX CLI from https://github.com/CycloneDX/cyclonedx-cli" >&2
    exit 1
  fi
fi

CYCLONEDX="cyclonedx"
if command -v cyclonedx >/dev/null 2>&1; then
  echo "CycloneDX CLI installation verified."
elif [[ -f /home/linuxbrew/.linuxbrew/bin/cyclonedx ]]; then
  CYCLONEDX="/home/linuxbrew/.linuxbrew/bin/cyclonedx"
  echo "CycloneDX CLI installation verified."
else
  echo "❌ CycloneDX CLI installation failed." >&2
  exit 1
fi


mkdir -p target/sbom
echo "Generating CycloneDX SBOM for Maven modules..."
for DISTRO in "${DISTROS[@]}"; do
    echo "  - Generating SBOM for distro: $DISTRO"
    MAVEN_LOG="target/sbom/maven-$DISTRO.log"
    ./mvnw org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom \
      -Pdistro-serverless,distro-"$DISTRO" \
      -Ddeploy.skip=false \
      -DoutputDirectory=target/sbom \
      -DoutputName=operaton-modules-"$DISTRO" \
      -DoutputFormat=json \
      -DprojectType=application \
      -DskipAttach=true \
      -DskipNotDeployed=true 2>&1 | tee "$MAVEN_LOG"
    # A dependency whose POM cannot be resolved is listed without any of its
    # own dependencies, and the build still succeeds. This happens to Operaton
    # modules outside the reactor of this call when they are not installed.
    if grep -q "The POM for .* is missing" "$MAVEN_LOG"; then
        echo "❌ The SBOM for $DISTRO lacks the dependencies of these artifacts:" >&2
        grep -o "The POM for .* is missing" "$MAVEN_LOG" | sort -u >&2
        exit 1
    fi
    if [[ ! -f target/sbom/operaton-modules-"$DISTRO".json ]]; then
        echo "❌ SBOM file target/sbom/operaton-modules-$DISTRO.json not found. Maven plugin may have failed."
        exit 1
    fi
    mv target/sbom/operaton-modules-"$DISTRO".json target/sbom/operaton-modules-"$DISTRO".cyclonedx-json.sbom
done

echo "Generating CycloneDX SBOMs for Node.js frontend modules..."
# The npm dependencies are invisible to the Maven plugin, so each frontend is
# scanned with trivy. Entries are <sbom name>:<frontend directory>.
FRONTENDS=("webapps:webapps/frontend" "webapps-neo:webapps-neo/frontend")
FRONTEND_SBOMS=()
for FRONTEND in "${FRONTENDS[@]}"; do
  FRONTEND_NAME="${FRONTEND%%:*}"
  FRONTEND_DIR="${FRONTEND#*:}"
  FRONTEND_SBOM="target/sbom/operaton-$FRONTEND_NAME.cyclonedx-json.sbom"
  echo "  - Generating SBOM for frontend: $FRONTEND_DIR"
  # No "--scanners vuln": the SBOM is an inventory, not a scan result. Embedded
  # findings are frozen at build time and trivy writes rating methods that are
  # not valid CycloneDX, which makes consumers such as Dependency-Track reject
  # the whole document (see #3855). Vulnerability analysis is the consumer's job.
  docker run --rm -v "$(pwd)":/repo "$TRIVY_IMAGE" fs --format cyclonedx --output "/repo/$FRONTEND_SBOM" "/repo/$FRONTEND_DIR"
  FRONTEND_SBOMS+=("$FRONTEND_SBOM")
done

echo "Generating CycloneDX SBOMs for the assembled distributions..."
# The Maven tree misses libraries that other jars shade (for example OGNL in
# MyBatis) and the jars of the bundled Tomcat and WildFly servers. Trivy reads
# the META-INF/maven/**/pom.properties of every jar, also of nested jars, and
# identifies jars without them by their SHA-1 (Java DB).
mkdir -p "$TRIVY_CACHE_DIR"
for DISTRO in "${DISTROS[@]}"; do
  ARCHIVE=$(distro_archive "$DISTRO" "$VERSION")
  SCAN_DIR="target/sbom/distribution-$DISTRO"
  echo "  - Scanning $ARCHIVE"
  rm -rf "$SCAN_DIR"
  mkdir -p "$SCAN_DIR"
  unzip -q "$ARCHIVE" -d "$SCAN_DIR"
  # Trivy also reads SBOM files it finds. The archive still contains the SBOM
  # of the previous run, which must not feed into the new one.
  find "$SCAN_DIR" -name '*.cdx.json' -delete
  docker run --rm -v "$(pwd)":/repo -v "$TRIVY_CACHE_DIR":/root/.cache/trivy "$TRIVY_IMAGE" \
    rootfs --format cyclonedx --output "/repo/target/sbom/operaton-distribution-$DISTRO.cyclonedx-json.sbom" "/repo/$SCAN_DIR"
  # target/sbom is kept as a build artifact, the unpacked archive is not.
  rm -rf "$SCAN_DIR"
done

echo "Merging CycloneDX SBOMs for each distribution..."
for DISTRO in "${DISTROS[@]}"; do
  OUTPUT_FILE="distro/$DISTRO/assembly/resources/sbom.cdx.json"

  # Like the Maven aggregate, the frontend inventory covers the whole reactor
  # and is merged into every distribution.
  $CYCLONEDX merge --input-files \
    target/sbom/operaton-modules-"$DISTRO".cyclonedx-json.sbom \
    "${FRONTEND_SBOMS[@]}" \
    --output-file "$OUTPUT_FILE" \
    --input-format json

  # Add the components only the distribution scan found (not part of the merge
  # above, which would list everything both sources report twice), override
  # metadata.component.name to match the distro and drop the vulnerabilities
  # section (trivy still emits an empty one, which the merge keeps). The
  # merged SBOM must contain the inventory only.
  cat "$OUTPUT_FILE" target/sbom/operaton-distribution-"$DISTRO".cyclonedx-json.sbom \
    | jq -n --arg name "operaton-$DISTRO" "$ADD_DISTRIBUTION_COMPONENTS" > "$OUTPUT_FILE.tmp"
  mv "$OUTPUT_FILE.tmp" "$OUTPUT_FILE"
  ADDED_FILE="target/sbom/distribution-only-$DISTRO.txt"
  jq -r '.components[] | select(.purl // "" | startswith("pkg:maven/"))
         | select(any(.properties[]?; .name == "aquasecurity:trivy:FilePath")) | .purl' < "$OUTPUT_FILE" | sort > "$ADDED_FILE"
  echo "  - Added $(grep -c . "$ADDED_FILE" || true) components found only in the $DISTRO distribution (see $ADDED_FILE)"

  # Fail the build if the merged SBOM does not validate against the CycloneDX
  # schema (the spec version is detected from the document).
  echo "  - Validating $OUTPUT_FILE"
  $CYCLONEDX validate --input-file "$OUTPUT_FILE" --input-format json --fail-on-errors

  # Fail if any jar of the distribution, or any library shaded into one, is
  # not in the SBOM.
  echo "  - Checking that $OUTPUT_FILE covers every jar"
  .devenv/scripts/build/check-sbom-coverage.sh "$OUTPUT_FILE" "$(distro_archive "$DISTRO" "$VERSION")"
done

echo "📦 SBOM files generated in target/sbom/"