#!/usr/bin/env bash
# Smoke test a single Operaton Docker SNAPSHOT image.
# Starts the container on a dedicated port, runs browser flows, then tears down.
#
# Usage:
#   smoke-test.sh [--image=operaton|wildfly|tomcat] [--tag=SNAPSHOT] [--port=PORT] [--keep]
#
# Examples:
#   .devenv/scripts/smoketest/smoke-test.sh --image=operaton
#   .devenv/scripts/smoketest/smoke-test.sh --image=wildfly --port=18081
#   .devenv/scripts/smoketest/smoke-test.sh --image=tomcat --tag=2.1.2
#
# Ports (defaults, chosen to avoid conflicts with common local services):
#   operaton  → 18080
#   wildfly   → 18081
#   tomcat    → 18082
#
# Requirements: docker, node (with playwright installed)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ── Defaults ────────────────────────────────────────────────────────────────
IMAGE_NAME="operaton"
TAG="SNAPSHOT"
PORT=""
KEEP=false

# ── Argument parsing ─────────────────────────────────────────────────────────
for arg in "$@"; do
  case "$arg" in
    --image=*)   IMAGE_NAME="${arg#*=}" ;;
    --tag=*)     TAG="${arg#*=}" ;;
    --port=*)    PORT="${arg#*=}" ;;
    --keep)      KEEP=true ;;
    --help|-h)
      sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) echo "Unknown argument: $arg"; exit 1 ;;
  esac
done

# Default port per image
if [ -z "$PORT" ]; then
  case "$IMAGE_NAME" in
    operaton) PORT=18080 ;;
    wildfly)  PORT=18081 ;;
    tomcat)   PORT=18082 ;;
    *) echo "Unknown image '$IMAGE_NAME'. Use operaton, wildfly, or tomcat."; exit 1 ;;
  esac
fi

FULL_IMAGE="operaton/${IMAGE_NAME}:${TAG}"
CONTAINER_NAME="operaton-smoke-${IMAGE_NAME}"
BASE_URL="http://localhost:${PORT}"

echo "╔══════════════════════════════════════════════════════╗"
echo "  Smoke test: ${FULL_IMAGE} on port ${PORT}"
echo "╚══════════════════════════════════════════════════════╝"

# ── Ensure playwright is available ──────────────────────────────────────────
BROWSER_SCRIPT="${SCRIPT_DIR}/browser-flows.mjs"
PLAYWRIGHT_DIR="/tmp/operaton-smoketest"
mkdir -p "$PLAYWRIGHT_DIR"
if [ ! -d "${PLAYWRIGHT_DIR}/node_modules/playwright" ]; then
  echo "Installing playwright..."
  cd "$PLAYWRIGHT_DIR" && npm install playwright --save-quiet 2>/dev/null || true
fi
# Install/update browser executable (no-op if already current)
cd "$PLAYWRIGHT_DIR" && npx playwright install chromium --quiet 2>/dev/null || true

# ── Teardown on exit ─────────────────────────────────────────────────────────
cleanup() {
  if [ "$KEEP" = false ]; then
    echo "Stopping container ${CONTAINER_NAME}..."
    docker rm -f "$CONTAINER_NAME" 2>/dev/null || true
  else
    echo "Container ${CONTAINER_NAME} left running (--keep)."
  fi
}
trap cleanup EXIT

# ── Pull & start ─────────────────────────────────────────────────────────────
docker rm -f "$CONTAINER_NAME" 2>/dev/null || true
echo "Pulling ${FULL_IMAGE}..."
docker pull "$FULL_IMAGE"

echo "Starting container on port ${PORT}..."
docker run -d --name "$CONTAINER_NAME" -p "${PORT}:8080" "$FULL_IMAGE"

# ── Wait for readiness ───────────────────────────────────────────────────────
echo "Waiting for ${BASE_URL}/operaton/app/ ..."
READY=false
for i in $(seq 1 60); do
  if curl -sf "${BASE_URL}/operaton/app/" >/dev/null 2>&1; then
    echo "Ready after $((i * 3))s"
    READY=true
    break
  fi
  sleep 3
done

if [ "$READY" = false ]; then
  echo "ERROR: Container did not become ready within 180s"
  docker logs "$CONTAINER_NAME" 2>&1 | tail -20
  exit 1
fi

# ── Run browser flows ────────────────────────────────────────────────────────
echo "Running browser flows..."
SCREENSHOT_DIR="${SCRIPT_DIR}/screenshots/${IMAGE_NAME}"
mkdir -p "$SCREENSHOT_DIR"

# Copy script next to node_modules so ESM import { chromium } from 'playwright' resolves
cp "$BROWSER_SCRIPT" "${PLAYWRIGHT_DIR}/browser-flows.mjs"
node "${PLAYWRIGHT_DIR}/browser-flows.mjs" "$BASE_URL" "$SCREENSHOT_DIR"
EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
  echo ""
  echo "✅ Smoke test PASSED: ${FULL_IMAGE}"
else
  echo ""
  echo "❌ Smoke test FAILED: ${FULL_IMAGE}"
  echo "Screenshots: ${SCREENSHOT_DIR}"
  exit $EXIT_CODE
fi
