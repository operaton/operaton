#!/usr/bin/env bash

EXECUTE_BUILD=true
EXECUTE_TEST=true
TEST_SUITE="engine"
DATABASE="h2"
DISTRO="tomcat"
VALID_TEST_SUITES=("engine" "webapps")
VALID_DISTROS=("operaton" "tomcat" "wildfly")
VALID_DATABASES=("h2" "postgresql")

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
      --testsuite=*)
        TEST_SUITE="${1#*=}"
        ;;
      --distro=*)
        DISTRO="${1#*=}"
        ;;
      --db=*)
        DATABASE="${1#*=}"
        ;;
      --no-build)
        EXECUTE_BUILD=false
        ;;
      --no-test)
        EXECUTE_TEST=false
        ;;
    esac
    shift
  done

  check_valid_values "testsuite" "$TEST_SUITE" "${VALID_TEST_SUITES[@]}"
  check_valid_values "distro" "$DISTRO" "${VALID_DISTROS[@]}"
  check_valid_values "db" "$DATABASE" "${VALID_DATABASES[@]}"
}

run_build () {
  PROFILES=(distro distro-webjar h2-in-memory)

  if [[ "$DISTRO" == "operaton" ]]; then
    PROFILES+=(distro-run integration-test-operaton-run)
  fi
  if [[ "$DISTRO" == "tomcat" ]]; then
    PROFILES+=(tomcat distro-tomcat)
  fi
  if [[ "$DISTRO" == "wildfly" ]]; then
    PROFILES+=(wildfly distro-wildfly)
  fi

  echo "ℹ️ Building $TEST_SUITE integration tests for distro $DISTRO with $DATABASE database using profiles: [${PROFILES[*]}]"
  ./mvnw -DskipTests -P$(IFS=,; echo "${PROFILES[*]}") clean install
  if [[ $? -ne 0 ]]; then
    echo "❌ Error: Build failed"
    exit 1
  fi
}

##########################################################################
run_tests () {
  PROFILES=()

  case "$TEST_SUITE" in
    engine)
      PROFILES+=(engine-integration)
      ;;
    webapps)
      PROFILES+=(webapps-integration)
      ;;
  esac

  case "$DISTRO" in
    operaton)
      PROFILES+=(integration-test-operaton-run)
      QA_DIR=distro/run/qa
      ;;
    tomcat)
      PROFILES+=(tomcat)
      QA_DIR=qa
      ;;
    wildfly)
      PROFILES+=(wildfly)
      QA_DIR=qa
      ;;
  esac

  case "$DATABASE" in
    h2)
      PROFILES+=(h2)
      ;;
    postgresql)
      PROFILES+=(postgresql)
      ;;
  esac

  echo "ℹ️ Running $TEST_SUITE integration tests for distro $DISTRO with $DATABASE database using profiles: [${PROFILES[*]}]"
  ./mvnw -P$(IFS=,; echo "${PROFILES[*]}") clean verify -f $QA_DIR
  if [[ $? -ne 0 ]]; then
    echo "❌ Error: Build failed"
    exit 1
  fi
}

##########################################################################
# main script
parse_args "$@"

pushd $(pwd) > /dev/null
cd $(git rev-parse --show-toplevel) || exit 1

if [[ "$EXECUTE_BUILD" == true ]]; then
  run_build
else
  echo "ℹ️ Skipping build"
fi

if [[ "$EXECUTE_TEST" == true ]]; then
  run_tests
else
  echo "ℹ️ Skipping tests"
fi


popd > /dev/null
