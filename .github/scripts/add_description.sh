#!/bin/bash
# Search for all pom.xml files in the current directory and subdirectories
find . -name "pom.xml" | while read pom_file; do
  # Check if the <description> tag already exists
  if ! grep -q "<description>" "$pom_file"; then
    echo "Adding <description> tag to $pom_file..."
    # Add the <description> tag before the closing </project> tag
    sed -i '/<\/project>/i \ \ <description>${project.name}</description>' "$pom_file"
  else
    echo "Description already present in $pom_file."
  fi
done