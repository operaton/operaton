#!/usr/bin/env bash
# Debug script to run the auto-merge workflow locally using act.
#
# Usage:
#   GITHUB_TOKEN=<your-token> GITHUB_USER=<your-login> .github/scripts/workflow-debug/debug-auto-merge.sh
#
# Requirements:
#   - act  (https://github.com/nektos/act)
#   - A GitHub personal access token with repo + pull_requests read/write scope
#     passed via the GITHUB_TOKEN environment variable.
#   - Your GitHub login passed via the GITHUB_USER environment variable.
#
# The script triggers the workflow via the 'workflow_dispatch' event so that
# act does not need to simulate a scheduled cron run.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

if [[ -z "${GITHUB_TOKEN:-}" ]]; then
  echo "ERROR: GITHUB_TOKEN environment variable is not set." >&2
  echo "       Export a GitHub personal access token before running this script:" >&2
  echo "       export GITHUB_TOKEN=<your-token>" >&2
  exit 1
fi

if [[ -z "${GITHUB_USER:-}" ]]; then
  echo "ERROR: GITHUB_USER environment variable is not set." >&2
  echo "       Export your GitHub login before running this script:" >&2
  echo "       export GITHUB_USER=<your-login>" >&2
  exit 1
fi

if ! command -v act &>/dev/null; then
  echo "ERROR: 'act' is not installed." >&2
  echo "       Install it from https://github.com/nektos/act or via:" >&2
  echo "         brew install act          (macOS)" >&2
  echo "         gh extension install https://github.com/nektos/gh-act  (gh CLI)" >&2
  exit 1
fi

# ---------------------------------------------------------------------------
# Write a minimal workflow_dispatch event payload.
# The workflow reads open PRs from the GitHub API at runtime, so we only
# need to provide a valid repository context here.
# ---------------------------------------------------------------------------
EVENT_FILE="$(mktemp /tmp/auto-merge-event-XXXXXX.json)"
trap 'rm -f "${EVENT_FILE}"' EXIT

cat > "${EVENT_FILE}" <<EOF
{
  "action": "workflow_dispatch",
  "repository": {
    "name": "operaton",
    "full_name": "operaton/operaton",
    "owner": {
      "login": "operaton"
    }
  },
  "sender": {
    "login": "${GITHUB_USER}"
  }
}
EOF

echo "============================================================"
echo "  Workflow:    .github/workflows/auto-merge.yml"
echo "  Event:       workflow_dispatch"
echo "  Sender:      ${GITHUB_USER}"
echo "============================================================"
echo ""
echo "Running act..."
echo ""

cd "${REPO_ROOT}"

act workflow_dispatch \
  --workflows ".github/workflows/auto-merge.yml" \
  --eventpath "${EVENT_FILE}" \
  --secret "GITHUB_TOKEN=${GITHUB_TOKEN}" \
  --env "GITHUB_TOKEN=${GITHUB_TOKEN}" \
  --verbose \
  "$@"
