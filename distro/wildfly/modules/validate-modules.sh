#!/bin/bash

# Copyright 2025 the Operaton contributors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at:
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


# This script validates Wildfly module.xml files to ensure
# that all referenced resource-root paths exist relative to the module.xml location.
# It requires 'xmllint' to be installed on the system (part of libxml2-utils).

# Directory where module.xml files are expected
# This path is relative to the Maven project's base directory.
MODULES_DIR="target/modules"

# Flag to track if any validation fails
# This variable is now in the main script's scope.
validation_failed=0

echo "Starting Wildfly module.xml validation in $MODULES_DIR..."

# Check if the modules directory exists
if [ ! -d "$MODULES_DIR" ]; then
    echo "WARNING: Module directory '$MODULES_DIR' not found. Skipping validation."
    exit 0
fi

# Find all module.xml files within the MODULES_DIR.
# Using 'while IFS= read -r module_file' with process substitution to read each file path.
# This ensures that changes to 'validation_failed' persist in the main shell's scope,
# resolving the subshell issue encountered previously with pipes.
# If 'find' produces no output, the loop will simply not execute, and the script will exit with success.
while IFS= read -r module_file; do
    echo "  Processing $module_file"
    # Get the directory of the current module.xml file
    module_dir=$(dirname "$module_file")

    # Extract all 'path' attributes from 'resource-root' elements using xmllint.
    # The xpath expression uses 'local-name()' to be namespace-agnostic.
    # '2>/dev/null' suppresses potential xmllint warnings.
    # The 'sed' command cleans up the output, removing 'path="' and trailing '"',
    # and crucially, also stripping any leading or trailing whitespace from the path.
    resource_paths_output=$(xmllint --xpath "//*[local-name()='module']/*[local-name()='resources']/*[local-name()='resource-root']/@path" "$module_file" 2>/dev/null | \
                            sed 's/path="//;s/"$//;s/^[[:space:]]*//;s/[[:space:]]*$//')

    if [ -z "$resource_paths_output" ]; then
        echo "    No <resource-root> paths found in $module_file. Skipping resource existence check."
        continue # Move to the next module.xml file
    fi

    # Iterate over each extracted relative path.
    # Using a here-string (<<<) to feed the multi-line string output directly to the while loop,
    # preventing it from running in a separate subshell that would lose 'validation_failed' context.
    while IFS= read -r relative_path; do
        # Skip empty lines that might result from sed processing
        if [ -z "$relative_path" ]; then
            continue
        fi

        # Construct the absolute path to the resource file
        # This assumes the resource is directly inside the module_dir or a subdirectory.
        absolute_resource_path="$module_dir/$relative_path"

        # Check if the referenced file exists and is a regular file
        if [ ! -f "$absolute_resource_path" ]; then
            echo "    ERROR: Resource not found for $module_file:"
            echo "      Referenced path: '$relative_path'"
            echo "      Expected absolute path: '$absolute_resource_path'"
            # Set the flag to indicate failure. This modification is now in the main shell.
            validation_failed=1
        else
            echo "    Resource found: '$relative_path' (at '$absolute_resource_path')"
        fi
    done <<< "$resource_paths_output"
done < <(find "$MODULES_DIR" -name "module.xml")

# After checking all module.xml files, determine the script's exit status
if [ "$validation_failed" -eq 1 ]; then
    echo "----------------------------------------------------------------------"
    echo "Wildfly module.xml validation FAILED: One or more referenced resources were not found."
    echo "----------------------------------------------------------------------"
    exit 1 # Exit with a non-zero status to fail the Maven build
else
    echo "----------------------------------------------------------------------"
    echo "Wildfly module.xml validation PASSED: All referenced resources found."
    echo "----------------------------------------------------------------------"
    exit 0 # Exit with zero status to indicate success
fi
