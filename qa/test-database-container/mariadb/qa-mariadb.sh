#!/bin/bash
test_mariadb() {
  set -e  # Exit on error (except where we trap it)

  # Configuration
  export MT_MARIADB_CONTAINER_NAME="mariadb_11_8"
  export MT_MARIADB_USERNAME="root"
  export MT_MARIADB_PASSWORD="123456"

  export MT_MARIADB_HOST_PORT="33070"
  export MT_MARIADB_DATABASE="operaton"

  MAX_RETRIES=30
  SLEEP_SEC=2
  COMPOSE_FILE="./test-database-container/mariadb/docker-compose.yaml"
  MAVEN_WRAPPER="../mvnw"

  # Bring down possible running containers from a broken execution
  cleanup_containers

  # Start containers
  docker compose -f "${COMPOSE_FILE}" up -d
  echo "⏳ Waiting for MariaDB to be ready inside container $MT_MARIADB_CONTAINER_NAME..."

  # Wait for container to be available
  retries=0
  until docker exec -i "$MT_MARIADB_CONTAINER_NAME" \
      mariadb -u"$MT_MARIADB_USERNAME" -p"$MT_MARIADB_PASSWORD" -e "SELECT 'i_am_up';" 2>/dev/null | grep -q "i_am_up"; do
    retries=$((retries+1))
    if [ "$retries" -ge "$MAX_RETRIES" ]; then
      echo "❌ MariaDB did not become ready in time!"
      cleanup_containers
      exit 1
    fi
    echo "⏳ MariaDB not ready yet... waiting $SLEEP_SEC seconds"
    sleep "$SLEEP_SEC"
  done

  echo "🚀 MariaDB is ready!"

  # Run Maven build, capture exit code
  set +e  # allow Maven to fail without stopping the script
  "$MAVEN_WRAPPER" clean install -Prolling-update,mt-mariadb
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
