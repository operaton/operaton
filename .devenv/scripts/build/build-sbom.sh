#!/usr/bin/env bash
set -euo pipefail
echo "Generating SBOM files for Operaton modules..."

# Check that npx is installed
if ! command -v npx &> /dev/null; then
    echo "❌ npx could not be found. Please install Node.js and npm."
    exit 1
fi

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

echo "Generating CycloneDX SBOM for Node.js frontend module..."
npx @cyclonedx/cyclonedx-npm --output-file target/sbom/operaton-webapps.cyclonedx-json.sbom webapps/frontend/package-lock.json

echo "📦 SBOM files generated in target/sbom/"