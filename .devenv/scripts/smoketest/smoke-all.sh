#!/usr/bin/env bash
# Run smoke tests against all three Operaton SNAPSHOT Docker images sequentially.
#
# Usage:
#   smoke-all.sh [--tag=SNAPSHOT]
#
# Each image runs on a dedicated port (18080, 18081, 18082) to avoid conflicts.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TAG="SNAPSHOT"

for arg in "$@"; do
  case "$arg" in
    --tag=*) TAG="${arg#*=}" ;;
    --help|-h) sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
  esac
done

PASS=()
FAIL=()

run_test() {
  local image="$1"
  if bash "${SCRIPT_DIR}/smoke-test.sh" --image="$image" --tag="$TAG"; then
    PASS+=("$image")
  else
    FAIL+=("$image")
  fi
  echo ""
}

run_test operaton
run_test wildfly
run_test tomcat

echo "╔══════════════════════════════════════════════════════╗"
echo "  Smoke test summary (tag: ${TAG})"
echo "╠══════════════════════════════════════════════════════╣"
for img in "${PASS[@]:-}"; do [ -n "$img" ] && echo "  ✅ ${img}"; done
for img in "${FAIL[@]:-}"; do [ -n "$img" ] && echo "  ❌ ${img}"; done
echo "╚══════════════════════════════════════════════════════╝"

if [ ${#FAIL[@]} -gt 0 ]; then
  exit 1
fi
