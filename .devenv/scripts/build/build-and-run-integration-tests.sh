#!/usr/bin/env bash

EXECUTE_BUILD=true
EXECUTE_TEST=true
TEST_SUITE="engine"
DATABASE="h2"
DISTRO="tomcat"
VALID_TEST_SUITES=("engine" "webapps" "engine-integration")
VALID_DISTROS=("operaton" "tomcat" "wildfly")
VALID_DATABASES=("h2" "postgresql" "postgresql-xa" "mysql" "mariadb", "oracle" "db2" "sqlserver")

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
    if [[ "$TEST_SUITE" == "engine" ]]; then
      MVN_ARGS+=(-Dskip.frontend.build=true)
    fi
  fi
  if [[ "$DISTRO" == "wildfly" ]]; then
    PROFILES+=(wildfly distro-wildfly)
    if [[ "$TEST_SUITE" == "engine" ]]; then
      MVN_ARGS+=(-Dskip.frontend.build=true)
    fi
  fi

  echo "ℹ️ Building $TEST_SUITE integration tests for distro $DISTRO with $DATABASE database using profiles: [${PROFILES[*]}]"
  echo "./mvnw -DskipTests -P$(IFS=,; echo "${PROFILES[*]}") clean install"
  ./mvnw -DskipTests -P$(IFS=,; echo "${PROFILES[*]}") clean install
  if [[ $? -ne 0 ]]; then
    echo "❌ Error: Build failed"
    popd > /dev/null
    exit 1
  fi
}

##########################################################################
run_tests () {
  PROFILES=()
  MVN_ARGS+=(clean verify)

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
      MVN_ARGS+=(-f distro/run/qa)
      ;;
    tomcat)
      PROFILES+=(tomcat)
      MVN_ARGS+=(-f qa)
      ;;
    wildfly)
      PROFILES+=(wildfly)
      MVN_ARGS+=(-f qa)
      ;;
  esac

  PROFILES+=($DATABASE)

  echo "ℹ️ Running $TEST_SUITE integration tests for distro $DISTRO with $DATABASE database using profiles: [${PROFILES[*]}]"
  echo "./mvnw -P$(IFS=,; echo "${PROFILES[*]}") $(echo "${MVN_ARGS[*]}")"
  ./mvnw -P$(IFS=,; echo "${PROFILES[*]}") $(echo "${MVN_ARGS[*]}")
  if [[ $? -ne 0 ]]; then
    echo "❌ Error: Build failed"
    popd > /dev/null
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
