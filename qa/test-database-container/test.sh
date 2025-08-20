#!/bin/bash
run_test() {
  DB_TYPE="$1"
  check_db_type "$DB_TYPE"
  echo Running migration tests with database "$1"...

  export_db_config_vars "$DB_TYPE"

  set -e  # Exit on error (except where we trap it)

  MAX_RETRIES=30
  SLEEP_SEC=2
  COMPOSE_FILE="./docker-compose.yaml"
  MAVEN_WRAPPER="../../mvnw"

  # Bring down possible running containers from a broken execution
  cleanup_containers

  # Start containers
  docker compose -f "${COMPOSE_FILE}" up -d "$DB_TYPE"
  echo "⏳ Waiting for $DB_TYPE to be ready..."

  # Wait for container to be available
  retries=0
  until wait_for_db "$DB_TYPE" 2>/dev/null | grep -q "i_am_up"; do
    retries=$((retries+1))
    if [ "$retries" -ge "$MAX_RETRIES" ]; then
      echo "❌ $DB_TYPE did not become ready in time!"
      cleanup_containers
      exit 1
    fi
    echo "⏳ $DB_TYPE not ready yet... waiting $SLEEP_SEC seconds"
    sleep "$SLEEP_SEC"
  done

  echo "🚀 $DB_TYPE is ready!"

  if [ "$DB_TYPE" = "sqlserver" ]; then
    create_sql_server_db
  fi

  # Run Maven build, capture exit code
  set +e  # allow Maven to fail without stopping the script
  "$MAVEN_WRAPPER" -f ../pom.xml clean install -Prolling-update,mt-"$DB_TYPE"
  BUILD_EXIT_CODE=$?
  set -e

  # Always bring down containers at the end
  cleanup_containers

  # Exit with Maven's exit code
  exit $BUILD_EXIT_CODE
}

check_db_type() {
  case "$1" in
    mysql|mariadb|postgres|oracle|sqlserver)
      ;;  # do nothing, valid case
    *)
      echo "Unknown database '$1' - supported databases are {mysql|mariadb|postgres|oracle|sqlserver}"
      exit 1
      ;;
  esac
}

export_db_config_vars() {
  DB_NAME="operaton"

  case "$1" in
    mysql)
      export MT_MYSQL_USERNAME="root"
      export MT_MYSQL_PASSWORD="123456"
      export MT_MYSQL_HOST_PORT="33060"
      export MT_MYSQL_DATABASE="$DB_NAME"
      ;;
    mariadb)
      export MT_MARIADB_USERNAME="root"
      export MT_MARIADB_PASSWORD="123456"
      export MT_MARIADB_HOST_PORT="33070"
      export MT_MARIADB_DATABASE="$DB_NAME"
      ;;
    postgres)
      export MT_POSTGRES_USERNAME="root"
      export MT_POSTGRES_PASSWORD="123456"
      export MT_POSTGRES_HOST_PORT="54320"
      export MT_POSTGRES_DATABASE="$DB_NAME"
      ;;
    oracle)
      export MT_ORACLE_USERNAME="SYSTEM"
      export MT_ORACLE_PASSWORD="123456"
      export MT_ORACLE_HOST_PORT="15210"
      export MT_ORACLE_DATABASE="$DB_NAME"
      ;;
    sqlserver)
      export MT_MSSQL_USERNAME="sa"
      export MT_MSSQL_PASSWORD="P@ssw0rd123ABC"
      export MT_MSSQL_HOST_PORT="14330"
      export MT_MSSQL_DATABASE="$DB_NAME"
      ;;
    *)
      echo "Usage: $0 {mysql|mariadb|postgres|oracle|sqlserver}"
      exit 1
      ;;
  esac
}

wait_for_db() {
  case "$1" in
      mysql)
        docker exec -i "$1" mysql -u"$MT_MYSQL_USERNAME" -p"$MT_MYSQL_PASSWORD" -e "SELECT 'i_am_up';"
        ;;
      mariadb)
        docker exec -i "$1" mariadb -u"$MT_MARIADB_USERNAME" -p"$MT_MARIADB_PASSWORD" -e "SELECT 'i_am_up';"
        ;;
      postgres)
        docker exec -i "$1" psql -U "$MT_POSTGRES_USERNAME" -d "$MT_POSTGRES_DATABASE" -c "SELECT 'i_am_up';"
        ;;
      oracle)
        docker exec -i "$1" bash -c "echo \"SELECT 'i_am_up' FROM dual;\" | sqlplus -S ${MT_ORACLE_USERNAME}/${MT_ORACLE_PASSWORD}@//localhost:1521/${MT_ORACLE_DATABASE}"
        ;;
      sqlserver)
        docker exec -i "$1" /opt/mssql-tools18/bin/sqlcmd -S localhost -C -N -U "$MT_MSSQL_USERNAME" -P "$MT_MSSQL_PASSWORD" -Q "SELECT 'i_am_up';"
        ;;
      *)
        echo "Unsupported database $1"
        exit 1
        ;;
    esac
}

create_sql_server_db() {
  docker exec -i "$DB_TYPE" \
        /opt/mssql-tools18/bin/sqlcmd -S localhost -C -N -U "$MT_MSSQL_USERNAME" -P "$MT_MSSQL_PASSWORD" -Q "CREATE DATABASE [$MT_MSSQL_DATABASE];"

    DB_LIST=$(docker exec -i "$DB_TYPE" \
          /opt/mssql-tools18/bin/sqlcmd -S localhost -C -N -U "$MT_MSSQL_USERNAME" -P "$MT_MSSQL_PASSWORD" -Q "SELECT name FROM sys.databases;")

    if echo "$DB_LIST" | grep -qw "$MT_MSSQL_DATABASE"; then
      echo "✅ Test database $MT_MSSQL_DATABASE created successfully!"
    else
      echo "❌ Test database $MT_MSSQL_DATABASE could not be created!"
      cleanup_containers
      exit 1
    fi
}

cleanup_containers() {
  docker compose -f "${COMPOSE_FILE}" down
}

run_test "$1"
