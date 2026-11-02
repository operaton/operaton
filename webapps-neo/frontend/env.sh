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

# ------------------------------------------------------------------------------
# Application path
#
# The SPA can be served from a sub-path. That path is not a config.json setting:
# the browser learns it from <base href> in the shell (which is why every URL the
# bundle emits is relative), and nginx has to serve the bundle from there too.
#
# Empty — the default — is the server root and leaves index.html and nginx.conf
# byte-identical to the build output. Under Spring Boot the equivalent rewrite is
# done per request by SpaIndexTransformer.
# ------------------------------------------------------------------------------
NGINX_CONF="${NGINX_CONF:-/etc/nginx/nginx.conf}"
INDEX_FILE="${INDEX_FILE:-$ASSET_DIR/index.html}"
APP_PATH_PLACEHOLDER="${APP_PREFIX}APPLICATION_PATH"

# POSIX sh has no indirect expansion; eval is how you read "${APP_PREFIX}APPLICATION_PATH".
eval "APPLICATION_PATH=\${${APP_PATH_PLACEHOLDER}:-}"

# "app-neo", "/app-neo" and "/app-neo/" all mean the same thing; "" and "/" mean
# the root. Normalise to either "" or "/app-neo" so the substitutions below can
# simply append a slash.
APPLICATION_PATH=$(printf '%s' "$APPLICATION_PATH" | sed -e 's|^/*||' -e 's|/*$||')
if [ -n "$APPLICATION_PATH" ]; then
    APPLICATION_PATH="/$APPLICATION_PATH"
fi

if [ -f "$INDEX_FILE" ]; then
    echo "Configuring: $INDEX_FILE (application path '${APPLICATION_PATH:-/}')"
    # Replace the existing tag rather than inserting a second one: with two base
    # tags the first wins, silently.
    sed -i "s|<base href=\"[^\"]*\"|<base href=\"${APPLICATION_PATH}/\"|" "$INDEX_FILE"
else
    echo "Warning: '$INDEX_FILE' not found, skipping application path."
fi

# nginx.conf ships with the placeholder rather than a usable default, because a
# `location /` serving the root and a `location /app-neo/` serving a sub-path are
# not the same block. It is always substituted, so nginx never sees the marker.
if [ -f "$NGINX_CONF" ]; then
    # Lines marked #APP_PATH_ONLY make sense only under a sub-path — at the root
    # they would redirect "/" to itself.
    if [ -n "$APPLICATION_PATH" ]; then
        sed -i "s|#APP_PATH_ONLY ||g" "$NGINX_CONF"
    fi
    sed -i "s|${APP_PATH_PLACEHOLDER}|${APPLICATION_PATH}|g" "$NGINX_CONF"
fi

# The web apps read their settings from config.json at startup, so that is the
# only file that carries placeholders. Substituting just this file (rather than
# every built asset) keeps the rewrite predictable and leaves the JS bundle,
# fonts and images untouched. The one exception is the application path above,
# which cannot live in config.json: it has to be in place before the bundle —
# and therefore the code that reads config.json — can be loaded at all.
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
