#!/usr/bin/env bash
set -euo pipefail

BASE_URL="https://googlechromelabs.github.io/chrome-for-testing"

echo "➡️  Detecting current Stable-Version..."
VERSION=$(curl -s "$BASE_URL/last-known-good-versions.json" | jq -r '.channels.Stable.version')
MILESTONE=$(echo "$VERSION" | cut -d. -f1)

echo "📦 Stable Chrome Version: $VERSION (Milestone $MILESTONE)"

JSON_URL="$BASE_URL/latest-versions-per-milestone-with-downloads.json"

CHROME_URL=$(curl -s "$JSON_URL" \
  | jq -r --arg m "$MILESTONE" '.milestones[$m].downloads.chrome[] | select(.platform=="linux64").url')

DRIVER_URL=$(curl -s "$JSON_URL" \
  | jq -r --arg m "$MILESTONE" '.milestones[$m].downloads.chromedriver[] | select(.platform=="linux64").url')

echo "⬇️  Downloading Chrome: $CHROME_URL"
echo "⬇️  Downloading Chromedriver: $DRIVER_URL"

curl -sSL "$CHROME_URL" -o /tmp/chrome.zip
unzip -q /tmp/chrome.zip -d /tmp/
sudo rm -rf /opt/chrome
sudo mv /tmp/chrome-linux64 /opt/chrome
sudo ln -sf /opt/chrome/chrome /usr/bin/google-chrome
echo "ℹ️ Installed $(google-chrome --version) to $(which google-chrome)"

curl -sSL "$DRIVER_URL" -o /tmp/driver.zip
unzip -q /tmp/driver.zip -d /tmp/
sudo mv /tmp/chromedriver-linux64/chromedriver /usr/local/bin/
sudo chmod +x /usr/local/bin/chromedriver
echo "ℹ️ Installed $(chromedriver --version | sed 's/ (.*//') to $(which chromedriver)"

echo "✅ Installation complete!"
