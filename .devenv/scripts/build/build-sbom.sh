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
echo "Generating SBOM files for Operaton modules..."

if command -v cyclonedx >/dev/null 2>&1; then
  echo "CycloneDX CLI already available."
else
  if command -v brew >/dev/null 2>&1; then
    echo "CycloneDX CLI not found — installing via Homebrew..."
    brew install cyclonedx/cyclonedx/cyclonedx-cli
  else
    echo "Homebrew not found. Please install Homebrew or install CycloneDX CLI manually:" >&2
    echo "  https://brew.sh/" >&2
    echo "Or install the CycloneDX CLI from https://github.com/CycloneDX/cyclonedx-cli" >&2
    exit 1
  fi
fi


DISTROS=("run" "tomcat" "wildfly")
echo "Generating CycloneDX SBOM for Maven modules..."
for DISTRO in "${DISTROS[@]}"; do
    echo "  - Generating SBOM for distro: $DISTRO"
    ./mvnw org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom \
      -Pdistro-serverless,distro-"$DISTRO" \
      -Ddeploy.skip=false \
      -DoutputDirectory=target/sbom \
      -DoutputName=operaton-modules-"$DISTRO" \
      -DoutputFormat=json \
      -DprojectType=application \
      -DskipAttach=true \
      -DskipNotDeployed=true
    if [ ! -f target/sbom/operaton-modules-"$DISTRO".json ]; then
        echo "❌ SBOM file target/sbom/operaton-modules $DISTRO.json not found. Maven plugin may have failed."
        exit 1
    fi
    mv target/sbom/operaton-modules-"$DISTRO".json target/sbom/operaton-modules-"$DISTRO".cyclonedx-json.sbom
done

echo "Generating CycloneDX SBOM for Node.js frontend module..."
mkdir -p target/sbom
docker run --rm -v "$(pwd)":/repo aquasec/trivy:latest fs --scanners vuln --format cyclonedx --output /repo/target/sbom/operaton-webapps.cyclonedx-json.sbom /repo/webapps/frontend

echo "Merging CycloneDX SBOMs for each distribution..."
for DISTRO in "${DISTROS[@]}"; do
  cyclonedx merge --input-files \
    target/sbom/operaton-modules-"$DISTRO".cyclonedx-json.sbom \
    target/sbom/operaton-webapps.cyclonedx-json.sbom \
    --output-file distro/"$DISTRO"/assembly/resources/sbom.cdx.json \
    --input-format json
done

echo "📦 SBOM files generated in target/sbom/"