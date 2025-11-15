#!/usr/bin/env bash

MVN_ARGS=()
PROFILES=()
BUILD_PROFILE="normal"
SKIP_TESTS="false"
REPORT_PLUGINS="false"
RUNNER="./mvnw"
VALID_BUILD_PROFILES=("fast" "normal" "max")
VALID_RUNNERS=("mvn" "./mvnw" "mvnd")

##########################################################################
check_valid_values() {
  local param_name=$1
  local value=$2
  shift 2
  local array=("$@")
  for item in "${array[@]}"; do
    if [[ "$value" == "$item" ]]; then
      return 0
    fi
  done
  echo "Error: Argument '$param_name' must be one of: [${array[*]}], but was '$value'"
  exit 1
}

##########################################################################
parse_args() {
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --profile=*)
        BUILD_PROFILE="${1#*=}"
        ;;
      --skip-tests)
        SKIP_TESTS="true"
        ;;
      --reports)
        REPORT_PLUGINS="true"
        ;;
      --runner=*)
        runner_param="${1#*=}"
        case "$runner_param" in
          mvn|mvnd)
            RUNNER="$runner_param"
            ;;
          mvnw)
            RUNNER="./mvnw"
            ;;
        esac
        ;;
      *)
        MVN_ARGS+=("$1")
        ;;
    esac
    shift
  done

  check_valid_values "profile" "$BUILD_PROFILE" "${VALID_BUILD_PROFILES[@]}"
  check_valid_values "runner" "$RUNNER" "${VALID_RUNNERS[@]}"
}

##########################################################################
# main script
parse_args "$@"

# Check if RUNNER exists, fallback if not
if ! command -v ${RUNNER} &> /dev/null; then
  echo "⚠️  Warning: Runner '${RUNNER}' not found. Falling back to './mvnw'."
  RUNNER="./mvnw"
fi

pushd $(pwd) > /dev/null
cd $(git rev-parse --show-toplevel) || exit 1
PROJECT_ROOT=$(pwd)

MVN_ARGS+=(clean install)

if [ "$REPORT_PLUGINS" = "true" ]; then
  MVN_ARGS+=(versions:dependency-updates-aggregate-report)
  MVN_ARGS+=(versions:plugin-updates-aggregate-report)
  # MVN_ARGS+=(dependency:analyze-report) TODO Disabled due to issue #1095
  MVN_ARGS+=(-Dsave=true -Ddisplay=false io.github.orhankupusoglu:sloc-maven-plugin:sloc)
  MVN_ARGS+=(-Dbuildplan.appendOutput=true -Dbuildplan.outputFile=$PROJECT_ROOT/target/reports/buildplan.txt fr.jcgay.maven.plugins:buildplan-maven-plugin:list)
fi

if ([ "$SKIP_TESTS" = "true" ]); then
  MVN_ARGS+=(-DskipTests)
fi

case "$BUILD_PROFILE" in
  "fast")
    PROFILES+=(distro h2-in-memory)
    ;;
  "normal")
    PROFILES+=(distro distro-webjar distro-run distro-tomcat h2-in-memory check-api-compatibility)
    ;;
  "max")
    PROFILES+=(distro distro-run distro-tomcat distro-wildfly distro-webjar distro-starter h2-in-memory check-api-compatibility quarkus-tests)
    ;;
esac

MVN_CMD="$RUNNER -P$(IFS=,; echo "${PROFILES[*]}") $(echo "${MVN_ARGS[*]}")"
echo "ℹ️ $MVN_CMD"
$MVN_CMD

if [[ $? -ne 0 ]]; then
  echo "❌ Error: Build failed"
  popd > /dev/null
  exit 1
fi
popd > /dev/null

