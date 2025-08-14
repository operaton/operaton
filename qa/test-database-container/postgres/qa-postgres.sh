#!/bin/bash
test_postgres() {
  set -e  # Exit on error (except where we trap it)

  # Configuration
  export MT_POSTGRES_CONTAINER_NAME="postgres_17"
  export MT_POSTGRES_USERNAME="root"
  export MT_POSTGRES_PASSWORD="123456"

  export MT_POSTGRES_HOST_PORT="54320"
  export MT_POSTGRES_DATABASE="operaton"

  MAX_RETRIES=30
  SLEEP_SEC=2
  COMPOSE_FILE="./test-database-container/postgres/docker-compose.yaml"
  MAVEN_WRAPPER="../mvnw"

  # Bring down possible running containers from a broken execution
  cleanup_containers

  # Start containers
  docker compose -f "${COMPOSE_FILE}" up -d
  echo "⏳ Waiting for PostgreSQL to be ready inside container $MT_POSTGRES_CONTAINER_NAME..."

  # Wait for container to be available
  retries=0
  until docker exec -i "$MT_POSTGRES_CONTAINER_NAME" \
      psql -U "$MT_POSTGRES_USERNAME" -d "$MT_POSTGRES_DATABASE" -c "SELECT 'i_am_up';" 2>/dev/null | grep -q "i_am_up"; do
    retries=$((retries+1))
    if [ "$retries" -ge "$MAX_RETRIES" ]; then
      echo "❌ PostgreSQL did not become ready in time!"
      cleanup_containers
      exit 1
    fi
    echo "⏳ PostgreSQL not ready yet... waiting $SLEEP_SEC seconds"
    sleep "$SLEEP_SEC"
  done

  echo "🚀 PostgreSQL is ready!"

  # Run Maven build, capture exit code
  set +e  # allow Maven to fail without stopping the script
  "$MAVEN_WRAPPER" clean install -Prolling-update,mt-postgres
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