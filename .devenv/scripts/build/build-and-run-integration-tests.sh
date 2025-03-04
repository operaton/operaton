#!/usr/bin/env bash

EXECUTE_BUILD=true
EXECUTE_TEST=true
TEST_SUITE="engine"
DATABASE="h2"
VALID_TEST_SUITES=("engine" "webapps")

parse_args() {
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --db=*)
        DATABASE="${1#*=}"
        ;;
      --testsuite=*)
        TEST_SUITE="${1#*=}"
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

  if [[ ! " ${VALID_TEST_SUITES[@]} " =~ " ${TEST_SUITE} " ]]; then
    echo "Error: TEST_SUITE must be either 'engine' or 'webapps'."
    exit 1
  fi
}

# main script
parse_args "$@"

