#!/bin/bash

# Purpose: Evaluates "mvn dependency:list" for the multi-module project
#          and lists the unique <groupId>:<artifactId>:<type>:<version>
# Usage:   Run this script from the root of the project.
#          ./.devenv/scripts/tools/list-unique-dependencies.sh

echo "🔍 Gathering dependency information..."

# Run mvn dependency:list and capture the output
# The "grep -E" filters for lines that typically represent dependencies.
mvn_output=$(mvn dependency:list)

echo "⚙️ Processing dependencies..."

# Process the output to extract and uniq dependencies
# - grep: Filters lines that look like dependencies (groupId:artifactId:type:version:scope)
# - sed: Removes leading whitespace and any potentialclassifier before the version if it exists.
# - sort -u: Sorts the lines and removes duplicates.
echo "$mvn_output" | \
  grep -E '^.*\s+[a-zA-Z0-9_\.-]+:[a-zA-Z0-9_\.-]+:(jar|war|ear|pom|bundle|test-jar):[a-zA-Z0-9_\.-]+(:[a-zA-Z0-9_\.-]+)?(:compile|:test|:provided|:runtime|:system|:import)' | \
  awk '{print $3}' | \
  #sed 's/:[^:]*$//' | \
  sort -u

echo "✅ Done."
