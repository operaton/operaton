#!/usr/bin/env bash
TARGET_DIR=$1

echo "📦 Collecting distros"
mkdir -p $TARGET_DIR
mv distro/sql-script/target/operaton-sql-scripts-*.tar.gz $TARGET_DIR
mv distro/tomcat/distro/target/operaton-bpm-tomcat-*.tar.gz $TARGET_DIR
