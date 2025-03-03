#!/usr/bin/env bash
if [ -z "$1" ]; then
  echo "⚠️ SLACK_INVITATION_URL is not set. Exiting..."
  exit 1
fi

SLACK_INVITATION_URL=$1
EXPECTED_PREFIX="https://join.slack.com/t/operaton/shared_invite/"

if [[ "$SLACK_INVITATION_URL" != $EXPECTED_PREFIX* ]]; then
  echo "⚠️ Invalid SLACK_INVITATION_URL. Exiting..."
  exit 1
fi

sed_inplace() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' -E "$@"
    else
        sed -i -E "$@"
    fi
}

pushd $(pwd) > /dev/null
cd $(git rev-parse --show-toplevel) || exit 1

AFFECTED_FILES=(README.md CONTRIBUTING.md)

for AFFECTED_FILE in "${AFFECTED_FILES[@]}"; do
  echo "🔄 Updating Slack Invitation URL in $AFFECTED_FILE"
  sed_inplace "s|${EXPECTED_PREFIX}[a-zA-Z0-9\-]+|${SLACK_INVITATION_URL}|" $AFFECTED_FILE
done

echo "✅  Done!"

popd > /dev/null
