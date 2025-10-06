---
# These are optional metadata elements. Feel free to remove any of them.
status: "proposed"
date: 2025-10-02
---

# Camunda 7 to Operaton runtime resources migration

## Context and Problem Statement

There is a problem with code migration for production runtimes if any platform code with FQN were used in resources like scripts or configurations. It is necessasry to be migrated it in database

<!-- This is an optional element. Feel free to remove. -->
## Decision Drivers

* Camunda 7 users with large engines in production that are hard to inspect should be able to migrate their data easy way (otherwise migration from Camunda 7 will be paused)

## Considered Options

* SQL scripts as standard engine cross-version migration for every possible database
* Liquibase custom task unified for any database
* Dedicated application/task/module as standalone application
* Configuration parameter and custom engine mode for migration 

## Decision Outcome

Chosen option: "Configuration parameter and custom engine mode for migration", because it is uniformed way, easy to test, possible to deploy separately and reuse a lot of code.

<!-- This is an optional element. Feel free to remove. -->
### Consequences

* Good, because can be used in any engine build variant, includes spring boot, run, application server webapp and be tested in any. Also will be enabled only when user manually enables migration in configuration
* Bad, because engine source code will be bloated with migration code that may be useless in future

<!-- This is an optional element. Feel free to remove. -->
## Pros and Cons of the Options

### SQL scripts as standard engine cross-version migration for every possible database

Create database specific sql scripts for artifacts migration 

* Good, because simple as it possible (in terms of solution runtime simplicity, scripts can be executed without any dependency on Operaton itself)
* Good, because consistent with cross-version migration
* Bad, because cannot reuse engine source code
* Bad, because database-specific with deep differences between each dbms

### Liquibase custom task unified for any database

Create liquibase custom task (it is possible to create java custom task) with unified way of artifacts migration for any database (use ansi sql for data retreival and java code for business logic)

* Good, because unified for every database
* Good, because easy to test
* Neutral, because independent to Operaton itself
* Bad, because cannot reuse engine code (engine liquibase usage limited as provided artifact so it cannot depend on engine code without large implications)

### Dedicated application/task/module as standalone application

Create application to be deployed separately from engine and migrate data from database in background 

* Good, because most adjustable
* Good, because easy to drop support
* Neutral, because it is possible to reuse engine code, but specific way (as dependency)
* Bad, because too verbose (users will needed download and deploy another one application)
* Bad, because hard to maintain and develop for not so big problem

### Configuration parameter and custom engine mode for migration

Create another one configuration `databaseSchemaUpdate` parameter variant that will enable new engine mode and data will be migrated by engine

* Good, because easy to develop and maintain as java code
* Good, because can be used in any engine build variants (not also liquibase or standalone or spring boot)
* Good, because easy to test as single solution for every dbms
* Good, because may be used only when it is needed and enabled explicitly
* Neutral-bad, because may be dropped lately, but harder than fully separated module
* Bad, because cannot be executed with another databaseSchemaUpdate modes
* Bad, because cannot be executed without engine as a lightweight artifact

## More Information

There are several statements were found in data migration practical cases analysis:

1. Data migration mechanism should save Camunda 7 backward compatibility. It may be necessary for production usages with big deployments and data (mostly scripts) that cannot be inspected well.
The most important implication is data preserving strategy: data should not be updated but be duplicated with changes (history should be unchanged also)
2. Parallel running (Camunda 7 and Operaton nodes on same database) is high useful for some cases. It can be used for downtime minimization or forward and backward compatibility
3. Some cases cannot be covered by standard mechanism because some specific details. It is possible to cover corner cases with custom implementations. For instance, downtime-free migration might depends on deployment landscape and can be solved with custom batch-processing solutions

SQL Scripts and Liquibase custom task implementations were tried but had some big problems that costs too much. SQL Scripts are highly depends on dbms type and version and cannot replicate time based id generation consistently. Liquibase custom task solution will affects liquibase artifact too much (current artifact contains liquibase and sql scripts files only)