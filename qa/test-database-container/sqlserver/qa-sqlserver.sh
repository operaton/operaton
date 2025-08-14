#!/bin/bash
test_sqlserver() {
  set -e  # Exit on error (except where we trap it)

  # Configuration
  export MT_MSSQL_CONTAINER_NAME="sqlserver_2022"
  export MT_MSSQL_USERNAME="sa"
  export MT_MSSQL_PASSWORD="P@ssw0rd123ABC"

  export MT_MSSQL_HOST_PORT="14330"
  export MT_MSSQL_DATABASE="operaton"

  MAX_RETRIES=30
  SLEEP_SEC=2
  COMPOSE_FILE="./test-database-container/sqlserver/docker-compose.yaml"
  MAVEN_WRAPPER="../mvnw"

  # Bring down possible running containers from a broken execution
  cleanup_containers

  # Start containers
  docker compose -f "${COMPOSE_FILE}" up -d
  echo "⏳ Waiting for MsSQL to be ready inside container $MT_MSSQL_CONTAINER_NAME..."

  # Wait for container to be available
  retries=0
  until docker exec -i "$MT_MSSQL_CONTAINER_NAME" \
    /opt/mssql-tools18/bin/sqlcmd -S localhost -C -N -U "$MT_MSSQL_USERNAME" -P "$MT_MSSQL_PASSWORD" -Q "SELECT 'i_am_up';" 2>/dev/null | grep -q "i_am_up"; do
    retries=$((retries+1))
    if [ "$retries" -ge "$MAX_RETRIES" ]; then
      echo "❌ MsSQL did not become ready in time!"
      cleanup_containers
      exit 1
    fi
    echo "⏳ MsSQL not ready yet... waiting $SLEEP_SEC seconds"
    sleep "$SLEEP_SEC"
  done

  echo "🚗 MsSQL is ready! - creating test database $MT_MSSQL_DATABASE ..."
  docker exec -i "$MT_MSSQL_CONTAINER_NAME" \
      /opt/mssql-tools18/bin/sqlcmd -S localhost -C -N -U "$MT_MSSQL_USERNAME" -P "$MT_MSSQL_PASSWORD" -Q "CREATE DATABASE [$MT_MSSQL_DATABASE];"

  DB_LIST=$(docker exec -i "$MT_MSSQL_CONTAINER_NAME" \
        /opt/mssql-tools18/bin/sqlcmd -S localhost -C -N -U "$MT_MSSQL_USERNAME" -P "$MT_MSSQL_PASSWORD" -Q "SELECT name FROM sys.databases;")

  if echo "$DB_LIST" | grep -qw "$MT_MSSQL_DATABASE"; then
    echo "✅ Test database $MT_MSSQL_DATABASE created successfully!"
  else
    echo "❌ Test database $MT_MSSQL_DATABASE could not be created!"
    cleanup_containers
    exit 1
  fi

  # Run Maven build, capture exit code
  set +e  # allow Maven to fail without stopping the script
  "$MAVEN_WRAPPER" clean install -Prolling-update,mt-sqlserver
  BUILD_EXIT_CODE=$?
  set -e

  # Always bring down containers at the end
  cleanup_containers

  # Exit with Maven's exit code
  exit $BUILD_EXIT_CODE
}

cleanup_containers() {
  docker compose -f "${COMPOSE_FILE}" down
}
