# Rolling Update Test Suite

How this test suite works (`$CURRENT` refers to current minor version, `$PREVIOUS` refers to `$CURRENT - 1`):

1. Create DB schema for `$PREVIOUS`
1. Execute test setup with `$PREVIOUS` engine
1. Patch DB schema to `$CURRENT`
1. Execute test setup with `$CURRENT` engine
1. Run tests with `$PREVIOUS` engine

## Preparing build

Before execution of the tests make sure that the latest version of the `bom`, `distro/sql-script` modules 
and dependent modules (esp. engine and what it depends on) are built.

This can be done by running the following commands from the root of the project:

```bash
./mvnw install -f bom 
./mvnw install -DskipTests -am -pl distro/sql-script
```

## Executing Tests

Run: `./mvnw clean install -f qa/test-db-rolling-update/pom.xml -Prolling-update,${database-id}`

| DB         | Command                                                                         |
|------------|---------------------------------------------------------------------------------|
| H2         | `./mvnw clean install -f qa -Prolling-update,h2`         |
| PostgreSQL | `./mvnw clean install -f qa -Prolling-update,postgresql` |
| MySQL      | `./mvnw clean install -f qa -Prolling-update,mysql`      |
| MariaDB    | `./mvnw clean install -f qa -Prolling-update,mariadb`    |
| Oracle     | `./mvnw clean install -f qa -Prolling-update,oracle`     |
| SQL Server | `./mvnw clean install -f qa -Prolling-update,sqlserver`  |
| DB2        | `./mvnw clean install -f qa -Prolling-update,db2`        |

## Debugging

### Connect to local h2 database

The build command 
```bash
mvn clean install -Prolling-update,h2
```
creates a local H2 database at `target/h2/process-engine.mv.db`.

You can connect to this database using the H2 console.

Execute from the root of the project:
```bash
#!/usr/bin/env bash
H2_JAR=$(find ~/.m2/repository/com/h2database/h2 -name \*.jar | grep 2.3 |head -n 1)
if [ -z "$H2_JAR" ]; then
  echo "H2 JAR not found. Please ensure H2 is installed in your local Maven repository."
  exit 1
fi
H2_URL="jdbc:h2:$(pwd)/qa/test-db-rolling-update/target/h2/process-engine;AUTO_SERVER=TRUE;AUTO_SERVER_PORT=18080;LOCK_TIMEOUT=10000"
echo "Connect to H2 database at \n   $H2_URL \n   with credentials 'sa'/'sa'."
java -cp "$H2_JAR" org.h2.tools.Server \
  -tcpAllowOthers \
  -tcpPort 9092 \
  -webAllowOthers \
  -webPort 8082 \
  -ifNotExists \
  -baseDir target/h2
```

This opens the H2 console at `http://localhost:8082` and allows you to connect to the database using the printed JDBC URL and credentials.
