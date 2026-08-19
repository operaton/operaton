#!/usr/bin/env sh
# ================================================================================
# File: env.sh
# Description: Replaces environment variables in asset files.
# Usage: Run this script in your terminal, ensuring APP_PREFIX and ASSET_DIRS are set.
# ================================================================================

# Set the exit flag to exit immediately if any command fails
set -e

# Check if APP_PREFIX is set
: "${APP_PREFIX:?APP_PREFIX must be set (e.g. APP_PREFIX='APP_PREFIX_')}"

# Check if ASSET_DIRS is set
: "${ASSET_DIR:?Must set ASSET_DIR to one path}"

# The web apps read their settings from config.json at startup, so that is the
# only file that carries placeholders. Substituting just this file (rather than
# every built asset) keeps the rewrite predictable and leaves the JS bundle,
# fonts and images untouched.
CONFIG_FILE="${CONFIG_FILE:-$ASSET_DIR/config.json}"

# Check if the file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Warning: '$CONFIG_FILE' not found, skipping configuration."
    exit 0
fi

echo "Configuring: $CONFIG_FILE"

# Iterate through each environment variable that starts with APP_PREFIX
env | grep "^${APP_PREFIX}" | while IFS='=' read -r key value; do
    # Display the variable being replaced
    echo "  • Replacing ${key} → ${value}"

    # Escape backslashes and ampersands for sed replacement. Uses sed rather
    # than the bash-only ${var//a/b} so this stays a POSIX sh script, which is
    # what the shebang and the nginx entrypoint promise.
    escaped=$(printf '%s' "$value" | sed -e 's/[\\&|]/\\&/g')

    sed -i "s|${key}|${escaped}|g" "$CONFIG_FILE"
done