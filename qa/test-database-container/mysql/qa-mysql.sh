#!/bin/bash
test_mysql() {
  set -e  # Exit on error (except where we trap it)

  # Configuration
  export MT_MYSQL_CONTAINER_NAME="mysql_9_2_0"
  export MT_MYSQL_USERNAME="root"
  export MT_MYSQL_PASSWORD="123456"

  export MT_MYSQL_HOST_PORT="33060"
  export MT_MYSQL_DATABASE="operaton"

  MAX_RETRIES=30
  SLEEP_SEC=2
  COMPOSE_FILE="./test-database-container/mysql/docker-compose.yaml"
  MAVEN_WRAPPER="../mvnw"

  # Bring down possible running containers from a broken execution
  cleanup_containers

  # Start containers
  docker compose -f "${COMPOSE_FILE}" up -d
  echo "⏳ Waiting for MySQL to be ready inside container $MT_MYSQL_CONTAINER_NAME..."

  # Wait for container to be available
  retries=0
  until docker exec -i "$MT_MYSQL_CONTAINER_NAME" \
      mysql -u"$MT_MYSQL_USERNAME" -p"$MT_MYSQL_PASSWORD" -e "SELECT 'i_am_up';" 2>/dev/null | grep -q "i_am_up"; do
    retries=$((retries+1))
    if [ "$retries" -ge "$MAX_RETRIES" ]; then
      echo "❌ MySQL did not become ready in time!"
      cleanup_containers
      exit 1
    fi
    echo "⏳ MySQL not ready yet... waiting $SLEEP_SEC seconds"
    sleep "$SLEEP_SEC"
  done

  echo "🚀 MySQL is ready!"

  # Run Maven build, capture exit code
  set +e  # allow Maven to fail without stopping the script
  "$MAVEN_WRAPPER" clean install -Prolling-update,mt-mysql
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