#!/usr/bin/env bash

COMPARISON_VERSION=""

show_help() {
  echo "Usage: $0 [--comparison-version <version>] [--help]"
  echo ""
  echo "Options:"
  echo "  --comparison-version <version>   Set the clirr comparison version (overrides default)"
  echo "  --help                           Show this help message and exit"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --comparison-version)
      COMPARISON_VERSION="$2"
      shift 2
      ;;
    --help)
      show_help
      exit 0
      ;;
    *)
      shift
      ;;
  esac
done

CMD="./mvnw -am -Pcheck-api-compatibility -DskipTests -Dskip.frontend.build=true \
-pl model-api/bpmn-model,model-api/cmmn-model,model-api/dmn-model,model-api/xml-model,engine,engine-dmn/engine,engine-dmn/feel-api,engine-dmn/feel-juel,\
connect/http-client,connect/soap-http-client,spin/core,spin/dataformat-json-jackson,spin/dataformat-xml-dom"

if [[ -n "$COMPARISON_VERSION" ]]; then
  CMD="$CMD -Dclirr.comparisonVersion=$COMPARISON_VERSION"
fi

echo "Checking API compatibility with previous release..."
echo "$CMD"

eval "$CMD"

# Remove empty files under target/reports/clirr and print info log
find target/reports/clirr -type f -empty | while read -r file; do
  rm "$file"
  echo "[INFO] Removed empty clirr report: $file"
done

# Create clirr.md with links to remaining non-empty files
CLIRR_DIR="target/reports/clirr"
MD_FILE="$CLIRR_DIR/clirr.md"
TARGET_ZIPFILE="target/clirr-reports.zip"

echo "## all" > "$MD_FILE"
find "$CLIRR_DIR/all" -type f -name '*.txt' ! -empty | sort | while read -r file; do
  relpath="${file#$CLIRR_DIR/}"
  echo "- [${relpath#all/}]($relpath)" >> "$MD_FILE"
done

echo -e "\n## restrictive" >> "$MD_FILE"
find "$CLIRR_DIR/restrictive" -type f -name '*.txt' ! -empty | sort | while read -r file; do
  relpath="${file#$CLIRR_DIR/}"
  echo "- [${relpath#restrictive/}]($relpath)" >> "$MD_FILE"
done

zip -qr "$TARGET_ZIPFILE" "$CLIRR_DIR"
echo "Clirr reports zipped to: $TARGET_ZIPFILE"
echo "✅ Done!"
