#!/usr/bin/env bash

MVN_ARGS=()
PROFILES=()
BUILD_PROFILE="normal"
SKIP_TESTS="false"
SKIP_ENGINE_TESTS="false"
CHANGED_MODULES=""
AFFECTED_BY=""
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
      --skip-engine-tests)
        SKIP_ENGINE_TESTS="true"
        ;;
      --changed-modules=*)
        CHANGED_MODULES="${1#*=}"
        ;;
      --affected-by=*)
        AFFECTED_BY="${1#*=}"
        ;;
      --webapps-only)
        CHANGED_MODULES="webapps/assembly"
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
run_mvn() {
  local profiles_csv="$1"
  shift
  local cmd="$RUNNER -P$profiles_csv $*"
  echo "ℹ️ $cmd"
  if ! $cmd; then
    echo "❌ Error: Build failed"
    popd > /dev/null
    exit 1
  fi
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

# --affected-by: derive changed modules locally from the git diff
if [ -n "$AFFECTED_BY" ]; then
  PREPARE_OUT=$(python3 "$PROJECT_ROOT/.github/actions/prepare-build/prepare_build.py" --diff-ref "$AFFECTED_BY") || exit 1
  CHANGED_MODULES=$(echo "$PREPARE_OUT" | grep '^changed_modules=' | cut -d= -f2-)
  if echo "$PREPARE_OUT" | grep -q '^skip_tests=true'; then
    SKIP_TESTS="true"
  fi
  if echo "$PREPARE_OUT" | grep -q '^skip_engine_tests=true'; then
    SKIP_ENGINE_TESTS="true"
  fi
  echo "ℹ️ Affected by '$AFFECTED_BY': changed_modules=${CHANGED_MODULES:-<full build>} skip_tests=$SKIP_TESTS skip_engine_tests=$SKIP_ENGINE_TESTS"
fi

if [ "$REPORT_PLUGINS" = "true" ]; then
  MVN_ARGS+=(versions:dependency-updates-aggregate-report)
  MVN_ARGS+=(versions:plugin-updates-aggregate-report)
  # MVN_ARGS+=(dependency:analyze-report) TODO Disabled due to issue #1095
  MVN_ARGS+=(-Dsave=true -Ddisplay=false io.github.orhankupusoglu:sloc-maven-plugin:sloc)
  MVN_ARGS+=(-Dbuildplan.appendOutput=true -Dbuildplan.outputFile=$PROJECT_ROOT/target/reports/buildplan.txt fr.jcgay.maven.plugins:buildplan-maven-plugin:list)
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

PROFILES_CSV=$(IFS=,; echo "${PROFILES[*]}")

# check-api-compatibility binds the clirr-maven-plugin (API compatibility
# report against the previous release) in ~17 public-API modules. It's
# irrelevant to phases that only need real artifacts installed quickly
# (compile-only passes), so those use this narrower profile list instead.
FAST_PROFILES=()
for p in "${PROFILES[@]}"; do
  [ "$p" != "check-api-compatibility" ] && FAST_PROFILES+=("$p")
done
FAST_PROFILES_CSV=$(IFS=,; echo "${FAST_PROFILES[*]}")

TEST_ARGS=()
if [ "$SKIP_ENGINE_TESTS" = "true" ]; then
  TEST_ARGS+=(-Dtest.excludes=org/operaton/bpm/engine)
fi

if [ -n "$CHANGED_MODULES" ]; then
  # Two-phase affected build:
  # 1. compile + install the whole reactor without tests
  # 2. run tests only in the changed modules and their dependents (-amd)
  #
  # Phase 1 deliberately builds every module rather than a -pl/-amd slice.
  # CI runs on fresh checkouts with nothing in the local repository, and the
  # reactor must be self-contained. A `-pl <changed> -am -amd` reactor is NOT:
  # -amd pulls in the changed modules' dependents (distro assemblies, webapps,
  # starters), but their own dependencies that are not also upstream of the
  # changed modules never enter the reactor, so they cannot be resolved.
  #
  # -Dmaven.test.skip=true (not -DskipTests) additionally skips compiling and
  # packaging test sources, since phase 1 never runs any tests anyway.
  # -Dskip.frontend.build=true skips the webapps npm build for the same reason,
  # unless the affected closure could actually observe the built frontend
  # (e.g. spring-boot-starter/starter-webapp boots a real server and asserts
  # a page returns 200, which 404s without the real npm build) — computed via
  # the same pom dependency graph used for the rest of this feature. Both
  # passes also drop check-api-compatibility (see FAST_PROFILES above) since
  # neither runs any tests or represents a real verification of the change.
  #
  # Caveat: maven-jar-plugin's test-jar goal also honors maven.test.skip and
  # silently produces NO artifact at all (not even an empty jar) when test
  # sources aren't compiled. A few modules' test-jars are real compile-time
  # dependencies of other modules (e.g. engine-cdi's "tests-quarkus"
  # classified jar is needed by quarkus-extension/engine/qa) — skip them
  # reactor-wide and that resolution fails inside the very same build. So
  # those producer modules are built for real (tests compiled + packaged,
  # only *execution* skipped, deliberately -DskipTests not maven.test.skip
  # here) in a small preliminary pass; the local repo then already has their
  # real test-jars before the fast full-reactor pass rebuilds (and
  # reinstalls everything else) with tests skipped. Narrowed to only
  # producers whose test-jar is actually reachable from this build's
  # affected closure — e.g. a clients/java/client-only change needs none of
  # them.
  PREPARE_PY="$PROJECT_ROOT/.github/actions/prepare-build/prepare_build.py"
  TEST_JAR_PRODUCERS=$(python3 "$PREPARE_PY" --list-test-jar-producers="$CHANGED_MODULES") || exit 1
  NEEDS_REAL_FRONTEND=$(python3 "$PREPARE_PY" --needs-real-frontend="$CHANGED_MODULES") || exit 1
  FRONTEND_ARGS=(-Dskip.frontend.build=true)
  if [ "$NEEDS_REAL_FRONTEND" = "true" ]; then
    FRONTEND_ARGS=()
  fi
  if [ -n "$TEST_JAR_PRODUCERS" ]; then
    run_mvn "$FAST_PROFILES_CSV" install -pl "$TEST_JAR_PRODUCERS" -am -DskipTests "${FRONTEND_ARGS[@]}" "${MVN_ARGS[@]}"
  fi
  run_mvn "$FAST_PROFILES_CSV" clean install -Dmaven.test.skip=true "${FRONTEND_ARGS[@]}" "${MVN_ARGS[@]}"
  if [ "$SKIP_TESTS" != "true" ]; then
    run_mvn "$PROFILES_CSV" verify -pl "$CHANGED_MODULES" -amd "${TEST_ARGS[@]}" "${MVN_ARGS[@]}"
  fi
else
  FULL_ARGS=(clean install)
  if [ "$SKIP_TESTS" = "true" ]; then
    FULL_ARGS+=(-DskipTests)
  fi
  run_mvn "$PROFILES_CSV" "${FULL_ARGS[@]}" "${TEST_ARGS[@]}" "${MVN_ARGS[@]}"
fi

popd > /dev/null
