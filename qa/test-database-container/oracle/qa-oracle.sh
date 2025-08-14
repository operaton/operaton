#!/bin/bash
test_oracle() {
  set -e  # Exit on error (except where we trap it)

  # Configuration
  export MT_ORACLE_CONTAINER_NAME="oracle_21"
  export MT_ORACLE_USERNAME="SYSTEM"
  export MT_ORACLE_PASSWORD="123456"

  export MT_ORACLE_HOST_PORT="15210"
  export MT_ORACLE_DATABASE="operaton"

  MAX_RETRIES=30
  SLEEP_SEC=2
  COMPOSE_FILE="./test-database-container/oracle/docker-compose.yaml"
  MAVEN_WRAPPER="../mvnw"

  # Bring down possible running containers from a broken execution
  cleanup_containers

  # Start containers
  docker compose -f "${COMPOSE_FILE}" up -d
  echo "⏳ Waiting for OracleDB to be ready inside container $MT_ORACLE_CONTAINER_NAME..."

  # Wait for container to be available
  retries=0
  until docker exec -i "$MT_ORACLE_CONTAINER_NAME" \
      bash -c "echo \"SELECT 'i_am_up' FROM dual;\" | sqlplus -S ${MT_ORACLE_USERNAME}/${MT_ORACLE_PASSWORD}@//localhost:1521/${MT_ORACLE_DATABASE}" 2>/dev/null | grep -q "i_am_up"; do
    retries=$((retries+1))
    if [ "$retries" -ge "$MAX_RETRIES" ]; then
      echo "❌ OracleDB did not become ready in time!"
      cleanup_containers
      exit 1
    fi
    echo "⏳ OracleDB not ready yet... waiting $SLEEP_SEC seconds"
    sleep "$SLEEP_SEC"
  done

  echo "🚀 OracleDB is ready!"

  # Run Maven build, capture exit code
  set +e  # allow Maven to fail without stopping the script
  "$MAVEN_WRAPPER" clean install -Prolling-update,mt-oracle
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
