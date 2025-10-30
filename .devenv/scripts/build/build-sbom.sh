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

# Check that npx is installed
if ! command -v npx &> /dev/null; then
    echo "❌ npx could not be found. Please install Node.js and npm."
    exit 1
fi

echo "Generating CycloneDX SBOM for Node.js frontend module..."
docker run --rm -v "$(pwd)":/repo aquasec/trivy:latest fs --scanners vuln --format cyclonedx --output /repo/target/sbom/operaton-webapps.cyclonedx-json.sbom /repo/webapps/frontend

echo "Generating CycloneDX SBOM for Maven modules..."
./mvnw org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom \
  -Pdistro,distro-run,distro-tomcat,distro-wildfly,distro-webjar,distro-starter,distro-serverless \
  -Ddeploy.skip=false \
  -DoutputDirectory=target/sbom \
  -DoutputName=operaton-modules \
  -DoutputFormat=json \
  -DprojectType=application \
  -DskipAttach=true \
  -DskipNotDeployed=true
if [ ! -f target/sbom/operaton-modules.json ]; then
    echo "❌ SBOM file target/sbom/operaton-modules.json not found. Maven plugin may have failed."
    exit 1
fi
mv target/sbom/operaton-modules.json target/sbom/operaton-modules.cyclonedx-json.sbom

echo "📦 SBOM files generated in target/sbom/"