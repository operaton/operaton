---
name: operaton-db
description: Use when changing the Operaton database schema - adding or changing tables, columns or indexes, writing upgrade scripts, initializing a new schema version (init-database-version.py), or verifying that upgrades work on all supported databases.
---

# Evolving the Operaton database schema

Supported databases: `h2`, `db2`, `mariadb`, `mssql`, `mysql`, `oracle`, `postgres`. **Every schema change touches all seven.** A change that exists for only some databases needs a stated reason in the commit (example: MSSQL already had `ACT_IDX_TASK_PARENT_TASK_ID`, so its upgrade script only adds the history index).

Schema files live in `engine/src/main/resources/org/operaton/bpm/engine/db/`.

## When schema changes are allowed

- Only in a **minor** release. Patch releases never change the schema. If an issue needs a schema change, tell the reporter it goes into the next minor schema version.
- Released upgrade scripts are immutable. Fix mistakes with a new script in the next version.

## Initializing a new schema version

Do this once per minor schema version, **before the first change for it**. Check first: if `upgrade/*_engine_<prev>_to_<new>.sql` already exists on `origin/main` (fetch first), it is initialized, do not run it again.

```bash
.devenv/scripts/maintenance/init-database-version.py --new-version 7.26.0 [--database h2,postgres] [--previous-artifact-version 2.2.1] [-y]
```

It generates: the version in all `create/activiti.<db>.create.engine.sql`, empty `upgrade/<db>_engine_<prev>_to_<new>.sql` with the `ACT_GE_SCHEMA_LOG` insert, the Liquibase changeset plus tag in `liquibase/operaton-changelog.xml`, the instance-migration fixture `qa/test-db-instance-migration/test-fixture-7xx`, and versions in `database/pom.xml` and `distro/sql-script/pom.xml`.

Review the output, then fix by hand what the script copies wrongly:
- fixture pom/`operaton.cfg.xml` engine versions: the fixture for an already released schema uses the release that shipped it, the new fixture uses the current `-SNAPSHOT`
- `previous artifact version` must be the release that delivered the previous schema

Commit on its own: `chore(engine): Initialize database schema version 7.xx.0`.

## Making a schema change

Per change, update **all** of these, per database:

| File | What |
|---|---|
| `create/activiti.<db>.create.<engine\|history\|...>.sql` | new table/column/index for fresh installs |
| `drop/activiti.<db>.drop.<engine\|history\|...>.sql` | matching drop (indexes are dropped explicitly) |
| `upgrade/<db>_engine_<prev>_to_<new>.sql` | the upgrade for existing installs |
| MyBatis mappings, entities | as needed |

Do **not** edit `liquibase/baseline/` (frozen 7.16 baseline).

Dialect pitfalls:
- Oracle identifiers are limited to 30 characters (`ACT_IDX_HI_TASK_PARENT_TASK_ID` is 29).
- Index drop syntax differs: MySQL/MariaDB `drop index X on T;`, MSSQL `drop index T.X;`, others `drop index X;`.
- Create and upgrade scripts must stay in sync: a fresh install and an upgraded install must have the same indexes and constraints.
- Name indexes `ACT_IDX_<TABLE-ish>_<COLUMN-ish>`; history tables use `ACT_IDX_HI_...`.

## Indexes: build them online where it is safe

Large `ACT_RU_*` / `ACT_HI_*` tables must not be write-locked during an upgrade.

| DB | Upgrade script syntax | Why |
|---|---|---|
| postgres | `create index concurrently if not exists X on T(C);` | no write lock |
| mysql, mariadb | `create index X on T(C) algorithm=inplace lock=none;` | InnoDB online DDL |
| oracle, mssql | plain `create index` | `ONLINE` is Enterprise-only and **fails on Standard edition**, which would break upgrades |
| db2, h2 | plain `create index` | no equivalent |

Create scripts always use plain `create index` (the tables are empty).

`create index concurrently` cannot run in a transaction, so the Liquibase changeset of that version needs `runInTransaction="false"`. Consequences, apply them together:
- Put the `ACT_GE_SCHEMA_LOG` insert **last** in the upgrade scripts of that version. The changeset is not atomic, and a failed run must be re-runnable without a duplicate log id.
- A failed concurrent build leaves an `INVALID` index, and `if not exists` skips it. Document in the script: drop the index and re-run.
- Add the issue link as a comment in the script.

## Testing

Run what applies; state in the PR what was and was not run.

1. **H2, create scripts + queries**: `./mvnw test -pl engine -Dtest=<affected tests>` (build dependencies first, see `AGENTS.md`).
2. **Upgrade scripts on real databases**: create the schema from the create scripts, drop the new objects, run the upgrade script as written (autocommit), assert the objects exist (Postgres: also `pg_index.indisvalid`), then run the drop scripts without errors. Containers: `postgres:17`, `mysql:8.4`, `mariadb:11.4`.
3. **Liquibase end to end** (needed whenever the changelog or `runInTransaction` changes): run the full changelog on an empty database, e.g. `liquibase/liquibase:4.33` with `JAVA_OPTS=-Ddb.name=postgres`, `--search-path` and workdir set to the `liquibase/` directory with `upgrade/` mounted beside it, then check the objects and the schema log row.
4. **Instance migration**: `cd qa/test-database-container && ./test.sh <db> instance-migration` (needs a full `./mvnw clean install` first; see `qa/test-db-instance-migration/README.md`).
5. Oracle, MSSQL and DB2 containers are heavy: when they were not run, say so and reason from the dialect (review the SQL against the pitfalls above).

## Documenting

- Schema changes need an ADR when they change the persistence layer in a significant way (`docs/decisions/`, see `operaton:adr` and `AGENTS.md`). A single index or column usually does not.
- Commit message: list per-database differences and why (online syntax, already-existing objects).
- Mark the issue/PR for the release notes with the noteworthy label (see `operaton:release`) and call out manual steps for users, such as a long index build or an `INVALID` index.
