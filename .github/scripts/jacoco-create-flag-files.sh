#!/usr/bin/env bash

echo "Creating a flag file 'executeJacoco' for each module containing tests. \
This triggers activation of the 'coverage' profile."
find . -type d | while read -r dir; do
if [[ -d "$dir/src/test/java" || -d "$dir/target/generated-test-sources/java" ]]; then
  # Create an empty file target/executeJacoco if the condition is met
  mkdir -p "$dir/target"
  touch "$dir/target/executeJacoco"
fi
done
echo "🚩 Created flag files for Jacoco"
