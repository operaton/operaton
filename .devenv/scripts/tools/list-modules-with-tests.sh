#!/usr/bin/env sh

BLACKLIST=( \
  "./engine-cdi" \
  "./test-utils/junit5-extension" \
  "./commons/testing" \
)

function is_blacklisted () {
  for bl in "${BLACKLIST[@]}"; do
    if [ "$1" = "$bl" ]; then
      return 0
    fi
  done
  return 1
}

function count_junit5_tests () {
  grep --include \*.java  -R -E "import org\.junit\.jupiter\..*;" -m 1 -o -h "$1" | wc -l
}
function count_junit4_tests () {
  grep --include \*.java -R -E "import org\.junit\.[A-Za-z0-9_]+;" -m 1 -o -h "$1" | wc -l
}
function marker () {
  if [ "$1" -eq "0" ]; then
    echo "✅"
  else
    echo "❌"
  fi
}
function separator () {
  printf '=%.0s' {1.."$1"}
}

printf "| %-9s | %-88s | %-25s | %-25s |\n" "status" "directory" "# JUnit 4 test cases" "# JUnit 5 test cases"
printf "| %-9s | %-88s | %-25s | %-25s |\n" $(printf '=%.0s' {1..7}) $(printf '=%.0s' {1..88}) $(printf '=%.0s' {1..25}) $(printf '=%.0s' {1..25})

find . -type d | while read -r dir; do
  if [[ -d "$dir/src/test/java" ]] && ! is_blacklisted "$dir" ; then
    nr_ju4_tests=$(count_junit4_tests "$dir")
    nr_ju5_tests=$(count_junit5_tests "$dir")
    migration_marker=$(marker "$nr_ju4_tests")
    printf "| %-9s | %-90s | %-25s | %-25s | \n" "$migration_marker" "$dir" "$nr_ju4_tests" "$nr_ju5_tests"
  fi
done
