#!/bin/bash
set -e

DB_TYPE="$1"
TEST_DB_CONTAINER_PATH=./test-database-container

case "$DB_TYPE" in
  mysql)
    source "$TEST_DB_CONTAINER_PATH"/mysql/qa-mysql.sh
    test_mysql
    ;;
  mariadb)
    source "$TEST_DB_CONTAINER_PATH"/mariadb/qa-mariadb.sh
    test_mariadb
    ;;
  postgres)
    source "$TEST_DB_CONTAINER_PATH"/postgres/qa-postgres.sh
    test_postgres
    ;;
  sqlserver)
    source "$TEST_DB_CONTAINER_PATH"/sqlserver/qa-sqlserver.sh
    test_sqlserver
    ;;
  oracle)
    source "$TEST_DB_CONTAINER_PATH"/oracle/qa-oracle.sh
    test_oracle
    ;;
  *)
    echo "Usage: $0 {mysql|mariadb|postgres|oracle|sqlserver}"
    exit 1
    ;;
esac
