---
# These are optional metadata elements. Feel free to remove any of them.
status: "proposed"
date: 2025-10-02
---

# Camunda 7 to Operaton Runtime Resources Migration

## Context and Problem Statement

There is a problem with code migration for production runtimes if any platform code with FQN was used in resources like scripts or configurations. It is necessary to migrate it in a database

<!-- This is an optional element. Feel free to remove. -->
## Decision Drivers

* Camunda 7 users with large engine instances in production that are difficult to inspect should be able to migrate their data easily

## Considered Options

* SQL scripts as a standard engine cross-version migration for any database
* Liquibase custom task unified for any database
* Dedicated application/task/module as a standalone application
* Configuration parameter and custom engine mode for migration

## Decision Outcome

**Chosen option:** “Configuration parameter and custom engine mode for migration” — because it provides a consistent approach, is easy to test, can be deployed separately, and reuses a large portion of existing code.

<!-- This is an optional element. Feel free to remove. -->
### Consequences

* **Good:** Can be used in any engine build option (Liquibase, standalone, Spring Boot). It is enabled only when the user explicitly switches to migration mode.
* **Bad:** The engine source code will be bloated with migration logic that may become obsolete in the future.

<!-- This is an optional element. Feel free to remove. -->
## Pros and Cons of the Options

### SQL scripts as standard engine cross-version migration for every possible database

Create database-specific SQL scripts for artifact migration.

* **Good:** Simple in terms of runtime; scripts can be executed without any dependency on Operaton itself.
* **Good:** Consistent with cross-version migration.
* **Bad:** Cannot reuse engine source code.
* **Bad:** Database-specific, with deep differences between DBMS implementations.

### Liquibase custom task unified for any database

Create liquibase custom task (it is possible to create java custom task) with unified way of artifacts migration for any database (use ansi sql for data retrieval and java code for business logic)

* **Good:** Unified for every database.
* **Good:** Easy to test.
* **Neutral:** Independent from Operaton itself.
* **Bad:** Cannot reuse engine code. The engine’s Liquibase usage is limited to provided artifacts, so it cannot depend on the engine code without major implications.

### Dedicated application/task/module as standalone application

Create an application that can be deployed separately from the engine to migrate data from the database in the background.

* **Good:** Highly adjustable.
* **Good:** Separated from the core module.
* **Neutral:** Possible to reuse engine code, but only in a specific manner (as a dependency).
* **Bad:** Too verbose — users need to download and deploy another application.
* **Bad:** Hard to maintain relative to the scope of the issue.

### Configuration parameter and custom engine mode for migration

Add one more configuration parameter, `databaseSchemaUpdate`, that enables a new engine mode in which data will be migrated by the engine.

* **Good:** Easy to develop and maintain as Java code.
* **Good:** Can be used in any engine build option (Liquibase, standalone, Spring Boot).
* **Good:** Easy to test as a single solution for every DBMS.
* **Good:** Can be enabled only when needed.
* **Neutral-Bad:** Relatively separated from the core module, but less than a fully independent module.
* **Bad:** Cannot be executed together with other `databaseSchemaUpdate` modes.
* **Bad:** Cannot be executed without the engine as a lightweight artifact.

## More Information

Additional notes identified during data migration analysis:

1. The data migration mechanism should preserve Camunda 7 backward compatibility. This is necessary for production use with large deployments and data (mostly scripts) that cannot be easily inspected.  
   The most important implication is the data preservation strategy: data should not be updated but duplicated with changes (the history should remain unchanged).
2. Parallel operation (Camunda 7 and Operaton nodes running on the same database) is useful in some cases. It can be used to minimize downtime and support forward/backward compatibility.
3. Some cases cannot be covered by a standard mechanism due to specific details. Corner cases can be handled by custom implementations. For instance, downtime-free migration might depend on the deployment landscape and can be achieved with custom batch-processing solutions.

SQL scripts and Liquibase custom task implementations were tested but had significant issues and were too costly.  
SQL scripts are highly dependent on DBMS type and version and cannot consistently replicate time-based ID generation.  
The Liquibase custom task solution would affect the Liquibase artifact too much (currently, the artifact contains only Liquibase and SQL script files).