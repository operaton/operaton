#!/usr/bin/env bash

set -e

show_help() {
  cat <<EOF
Usage: $(basename "$0") [--cleanups=<list>] [--help]

Options:
  --cleanups=LIST   Comma-separated list of cleanups to run. Valid values:
                    imports, code, tests
                    If not specified, all cleanups are run.
  --help            Show this help and exit.

Examples:
  $0
      # Run all cleanups (imports, code, tests)
  $0 --cleanups=imports,code
      # Only run imports and code cleanups
EOF
}

VALID_CLEANUPS=("imports" "code" "tests")
CLEANUPS=("${VALID_CLEANUPS[@]}")

for arg in "$@"; do
  case $arg in
    --cleanups=*)
      IFS=',' read -ra REQ_CLEANUPS <<< "${arg#*=}"
      CLEANUPS=()
      for c in "${REQ_CLEANUPS[@]}"; do
        found=0
        for v in "${VALID_CLEANUPS[@]}"; do
          if [[ "$c" == "$v" ]]; then
            found=1
            CLEANUPS+=("$c")
            break
          fi
        done
        if [[ $found -eq 0 ]]; then
          echo "Error: Invalid cleanup value: '$c'" >&2
          echo "Valid values: ${VALID_CLEANUPS[*]}" >&2
          exit 1
        fi
      done
      ;;
    --help)
      show_help
      exit 0
      ;;
    *)
      echo "Unknown option: $arg" >&2
      show_help
      exit 1
      ;;
  esac
done

PROFILES=distro,distro-run,distro-wildfly,distro-starter,distro-serverless

RECIPES=()
RUN_IMPORTS=0

# Run selected cleanups
for cleanup in "${CLEANUPS[@]}"; do
  case "$cleanup" in
    imports)
      RUN_IMPORTS=1
      ;;
    code)
      RECIPES+=("org.operaton.recipe.CodeCleanup")
      ;;
    tests)
      RECIPES+=("org.operaton.recipe.TestsCleanup")
      ;;
  esac
done

if [[ $RUN_IMPORTS -eq 1 ]]; then
  echo "ℹ️  Running: ./mvnw -U org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.activeRecipes=org.openrewrite.java.OrderImports -Drewrite.activeStyles=org.operaton.style.OperatonJavaStyle -Drewrite.exportDatatables=true -Dskip.frontend.build=true -P$PROFILES"
  ./mvnw -U org.openrewrite.maven:rewrite-maven-plugin:run \
    -Drewrite.activeRecipes=org.openrewrite.java.OrderImports \
    -Drewrite.activeStyles=org.operaton.style.OperatonJavaStyle \
    -Drewrite.exportDatatables=true \
    -Dskip.frontend.build=true \
    -P$PROFILES
fi

if [[ ${#RECIPES[@]} -gt 0 ]]; then
  echo "ℹ️  Running: ./mvnw -U org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.activeRecipes=$(IFS=,; echo "${RECIPES[*]}") -Dskip.frontend.build=true -P$PROFILES"
  ./mvnw -U org.openrewrite.maven:rewrite-maven-plugin:run \
    -Drewrite.activeRecipes="$(IFS=,; echo "${RECIPES[*]}")" \
    -Dskip.frontend.build=true \
    -P$PROFILES
fi
