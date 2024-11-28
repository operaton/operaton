#!/usr/bin/env bash
JACOCO_EXEC_FILES=$(find . -name jacoco.exec | paste -sd ',' -)
TARGET_DIR=target/jacoco-collected

echo "📦 Collecting JaCoCo exec files"
mkdir -p $TARGET_DIR
for FILE in $(echo $JACOCO_EXEC_FILES | tr "," "\n"); do
  TARGET_FILE_NAME=$(echo $FILE | sed 's/\.\///g' | sed 's/target\///g' | sed 's/\//_/g' )
  cp -v $FILE $TARGET_DIR/$TARGET_FILE_NAME
done
