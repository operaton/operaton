# Rolling Update Test Suite

How this test suite works (`$CURRENT` refers to current minor version, `$PREVIOUS` refers to `$CURRENT - 1`):

1. Create DB schema for `$PREVIOUS`
1. Execute test setup with `$PREVIOUS` engine
1. Patch DB schema to `$CURRENT`
1. Execute test setup with `$CURRENT` engine
1. Run tests with `$PREVIOUS` engine

## Executing Tests

Run `mvn clean install -Prolling-update,${DATABASE}`.

| DB         | Command                                         |
|------------|-------------------------------------------------|
| H2         | `mvn clean install -Prolling-update,h2`         |
| PostgreSQL | `mvn clean install -Prolling-update,postgresql` |
| MySQL      | `mvn clean install -Prolling-update,mysql`      |
| MariaDB    | `mvn clean install -Prolling-update,mariadb`    |
| Oracle     | `mvn clean install -Prolling-update,oracle`     |
| SQL Server | `mvn clean install -Prolling-update,sqlserver`  |
| DB2        | `mvn clean install -Prolling-update,db2`        |

### Running tests with the Maven Wrapper

With `mvnw`, from the root of the project,
Run: `./mvnw clean install -f qa/test-db-rolling-update/pom.xml -Prolling-update,${database-id}`

| DB         | Command                                                                         |
|------------|---------------------------------------------------------------------------------|
| H2         | `./mvnw clean install -f qa/test-db-rolling-update -Prolling-update,h2`         |
| PostgreSQL | `./mvnw clean install -f qa/test-db-rolling-update -Prolling-update,postgresql` |
| MySQL      | `./mvnw clean install -f qa/test-db-rolling-update -Prolling-update,mysql`      |
| MariaDB    | `./mvnw clean install -f qa/test-db-rolling-update -Prolling-update,mariadb`    |
| Oracle     | `./mvnw clean install -f qa/test-db-rolling-update -Prolling-update,oracle`     |
| SQL Server | `./mvnw clean install -f qa/test-db-rolling-update -Prolling-update,sqlserver`  |
| DB2        | `./mvnw clean install -f qa/test-db-rolling-update -Prolling-update,db2`        |
