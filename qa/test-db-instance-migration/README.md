Instance Migration Tests
========================

These modules test scenarios in which process instances are started before upgrading Operaton from a minor version to another minor version and completed after. The tests ensure that object instances persisted in the database tables by the engine in a previous minor version can be successfully processed by succeeding versions. The most common use case is migration of execution trees.

Executing Tests
---------------

### Prerequisites

A full `./mvnw clean install` from the root of the project is required first (see also `qa/README.md`). The modules of this suite depend on artifacts of the current build, for example `operaton-qa-upgrade-scenario-util` (module `qa/test-db-util`) and the `operaton-sql-scripts` of the current database schema version.

The fixtures for released versions do not need a local build. They resolve the released engine (and its Liquibase changelog) from Maven Central.

### Running tests with H2

With `mvn`, run `mvn clean install -Pinstance-migration,${database-id}` where `${database-id}` is for example `h2`.

With the Maven Wrapper, from the root of the project,
run: `./mvnw clean install -f qa/test-db-instance-migration/pom.xml -Pinstance-migration,${database-id}`
where `${database-id}` is for example `h2`.

### Running tests with other databases

Fixtures and tests run in separate JVMs and need to share one database. Use `qa/test-database-container/test.sh`, which starts the database in a container, runs the suite and removes the container afterwards:

```bash
cd qa/test-database-container
./test.sh ${database} instance-migration
```

where `${database}` is one of `h2`, `mysql`, `mariadb`, `postgres`, `oracle` or `sqlserver`.

Project Structure
-----------------

* `test-fixture-724`: Creates the `7.24.0` database schema with engine `2.1.6`, the last release before the `7.25.0` schema, and starts all process instances that the tests use. All scenarios are registered with origin version `7.24.0`.
* Any later `test-fixture-7xx`: Applies the Liquibase changelog of the released engine that introduced schema `7.xx`, so the database is upgraded from the previous version. Starts process instances with that engine version.
* `test-migration`: Executes test cases to assure that process instances started by any of the `test-fixture` modules can be completed with the current engine version

Scope of the tests
------------------

The instances are created by a recent engine, not by the engine version that originally introduced a feature. Therefore only behavior that a recent engine can still produce is tested here. Tests that assert data as written by an old engine (e.g. columns that were added later and are `null` for old rows, or the structure of activity instance trees before the introduction of event subprocess scopes) cannot be reproduced and are not part of this suite.
