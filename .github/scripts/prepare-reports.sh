#!/usr/bin/env bash
SLOC_FILES=$(find . -name sloc.txt | paste -sd ',' -)
TARGET_DIR=target/reports/sloc

echo "📦 Collecting sloc.txt files"
mkdir -p $TARGET_DIR
for FILE in $(echo $SLOC_FILES | tr "," "\n"); do
  TARGET_FILE_NAME=$(echo $FILE | sed 's/\.\///g' | sed 's/target\///g' | sed 's/\//_/g' )
  mv -v $FILE $TARGET_DIR/$TARGET_FILE_NAME
done
