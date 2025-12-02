---
# These are optional metadata elements. Feel free to remove any of them.
status: "proposed"
date: 2025-10-02
decision-makers: ["@hauptmedia", "@ungerts", "@kthoms", "@SevDan"]
---

# Camunda 7 to Operaton Runtime Resources Migration

## Context and Problem Statement

There is an issue with code migration for production runtimes when platform classes referenced by FQNs are used in resources such as scripts or configurations. These references should be migrated inside the database.

Artifacts scope:

* **In scope:** BPMN and DMN resources (scripts, forms) stored in runtime tables
* **Out of scope:** all resources stored in history tables

## Decision Drivers

* Must provide smooth migration with only few additional steps for running processes in the new environment
* Must remain database‑agnostic
* Must prevent potential data corruption at all costs
* Should be included in the standard distribution
* Should avoid manual data transformations (SQL scripts, ETL tasks, etc.)
* Should be predictable in terms of time, CPU, and memory consumption

## Considered Options

* SQL scripts as standard engine cross‑version migration for any database
* Liquibase custom task unified for any database
* Dedicated application/task/module as a standalone application
* Configuration parameter and custom engine mode for data migration
* Configuration parameter that enables a special compatibility mode without data changes

## Decision Outcome

**Chosen option:** *Configuration parameter and custom engine mode for migration* — because it provides a consistent approach, is easy to test, can be deployed independently, and reuses a large portion of existing engine code.

### Consequences

* **Good:** Can be used in any engine build option (Liquibase, standalone, Spring Boot). It activates only when explicitly enabled by the user.
* **Bad:** The engine source code becomes more complex due to migration logic that may become obsolete over time.

## Pros and Cons of the Options

### SQL scripts as standard engine cross‑version migration for every database

Create database‑specific SQL scripts for migrating artifacts.

* **Good:** Simple at runtime; scripts can be executed without any dependency on Operaton.
* **Good:** Consistent with existing cross‑version migration mechanisms.
* **Bad:** Cannot reuse engine code.
* **Bad:** Database‑specific with major differences across DBMS implementations.

### Liquibase custom task unified for any database

Create a Liquibase custom task (implemented in Java) that performs unified artifact migration using ANSI SQL for data retrieval and Java for business logic.

* **Good:** Unified across all supported databases.
* **Good:** Easy to test.
* **Neutral:** Independent of the Operaton engine.
* **Bad:** Cannot reuse engine code. The engine's current Liquibase usage is limited to schema artifacts, so relying on engine code would introduce major constraints.

### Dedicated application/task/module as a standalone application

A separately deployed application that migrates data directly from the database in the background.

* **Good:** Highly flexible.
* **Good:** Fully separated from the core engine.
* **Neutral:** Can reuse engine code, but only as a dependency.
* **Bad:** Too heavy — users would need to deploy an additional application.
* **Bad:** Hard to maintain compared to the actual scope of the problem.

### Configuration parameter and custom engine mode for migration

Add a configuration parameter `camundaCompatibilityMode` that enables a dedicated engine mode for data migration when set to `data-migration`.

* **Good:** Straightforward to develop and maintain as Java code.
* **Good:** Works with any engine packaging (Liquibase, standalone, Spring Boot).
* **Good:** Easy to test as a single unified mechanism across all DBMS types.
* **Good:** Enabled only when explicitly required.
* **Neutral‑Bad:** Somewhat separated from the core module, but still shares parts of it.
* **Bad:** Cannot run together with other `camundaCompatibilityMode` modes.
* **Bad:** Cannot run without the engine; not a lightweight migration artifact.

### Configuration parameter enabling compatibility mode without data changes

Add a configuration parameter that enables an engine mode that dynamically adapts resource loading and interprets Camunda 7‑style resources as Operaton‑compatible ones, without modifying stored data.

* **Good:** No runtime overhead.
* **Good:** Very easy to use in backward‑compatible runtime setups (old artifacts run unchanged).
* **Bad:** Unmodified data may cause problems in the future or introduce unexpected behavior.
* **Bad:** Limited long‑term viability.

## More Information

Additional insights from the data migration analysis:

1. The migration mechanism must preserve Camunda 7 backward compatibility. This is essential for production usage with large deployments where artifacts (especially scripts) cannot be manually inspected.  
   The key result is that data should *not* be overwritten but duplicated with the necessary changes, while historical data remains untouched.
2. Parallel operation (Camunda 7 and Operaton nodes on the same database) is beneficial in some deployment scenarios. It allows minimizing downtime and supporting forward/backward compatibility.
3. Some cases will inevitably fall outside the scope of a universal mechanism. These can be handled via custom extensions. For example, downtime‑free migration might require custom batch‑processing tailored to the deployment landscape.

Both SQL scripts and Liquibase custom task prototypes were evaluated but exhibited major limitations and high cost.  
SQL scripts depend strongly on DBMS type and version, and cannot reliably reproduce time‑based ID generation.  
The Liquibase custom task approach would require significant changes to the Liquibase artifact, which currently contains only SQL and Liquibase files.
