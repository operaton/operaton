#!/usr/bin/env bash
# Deploys the SNAPSHOT artifacts of a local staging repository (written by the build job via
# altDeploymentRepository) to a remote Maven repository, without running the build lifecycle.
#
# Every version directory carries a maven-metadata.xml that lists each staged file with its
# classifier and extension. One deploy:deploy-file call per GAV uploads them; the resolver
# merges the remote metadata. Non-SNAPSHOT versions (e.g. distro/sql-script) are skipped.
#
# Usage: deploy-staged-snapshots.sh <staging-dir> <repository-url> <repository-id> [maven args...]
set -euo pipefail

STAGING_DIR=$(realpath "$1")
REPOSITORY_URL=$2
REPOSITORY_ID=$3
shift 3
PARALLELISM=${DEPLOY_PARALLELISM:-4}
DEPLOY_PLUGIN=org.apache.maven.plugins:maven-deploy-plugin:3.2.0:deploy-file

# Prints the deploy-file arguments for one version directory, one per line
deploy_args() {
  local version_dir=$1
  python3 -I - "$version_dir" <<'EOF'
import os, sys
import xml.etree.ElementTree as ET

version_dir = sys.argv[1]
metadata = ET.parse(os.path.join(version_dir, "maven-metadata.xml")).getroot()
pom = main = None
attached = []
for sv in metadata.iter("snapshotVersion"):
    classifier = sv.findtext("classifier") or ""
    extension = sv.findtext("extension")
    value = sv.findtext("value")
    artifact_id = metadata.findtext("artifactId")
    name = f"{artifact_id}-{value}" + (f"-{classifier}" if classifier else "") + f".{extension}"
    path = os.path.join(version_dir, name)
    if not os.path.isfile(path):
        sys.exit(f"missing staged file {path}")
    if extension == "pom" and not classifier:
        pom = path
    elif not classifier:
        main = path
    else:
        attached.append((path, classifier, extension))
if pom is None:
    sys.exit(f"no pom staged in {version_dir}")
print(f"-DpomFile={pom}")
print(f"-Dfile={main or pom}")
if attached:
    print("-Dfiles=" + ",".join(a[0] for a in attached))
    print("-Dclassifiers=" + ",".join(a[1] for a in attached))
    print("-Dtypes=" + ",".join(a[2] for a in attached))
EOF
}

deploy_version_dir() {
  local version_dir=$1
  shift
  local args
  mapfile -t args < <(deploy_args "$version_dir")
  echo "🚀 Deploying ${version_dir#"$STAGING_DIR"/}"
  # The root pom sets deploy.skip=true, which would make deploy-file a silent no-op
  ./mvnw --non-recursive --quiet "$DEPLOY_PLUGIN" \
    "${args[@]}" \
    -Ddeploy.skip=false \
    -DgeneratePom=false \
    -Durl="$REPOSITORY_URL" \
    -DrepositoryId="$REPOSITORY_ID" \
    "$@"
}
export -f deploy_args deploy_version_dir
export STAGING_DIR REPOSITORY_URL REPOSITORY_ID DEPLOY_PLUGIN

mapfile -t VERSION_DIRS < <(find "$STAGING_DIR" -type d -name '*-SNAPSHOT' | sort)
if [[ ${#VERSION_DIRS[@]} -eq 0 ]]; then
  echo "❌ No SNAPSHOT artifacts found in $STAGING_DIR"
  exit 1
fi
echo "📦 Deploying ${#VERSION_DIRS[@]} SNAPSHOT artifacts from $STAGING_DIR to $REPOSITORY_URL"

printf '%s\0' "${VERSION_DIRS[@]}" \
  | xargs -0 -P "$PARALLELISM" -I{} bash -c 'deploy_version_dir "$@"' _ {} "$@"
