# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

A more detailed contributor guide lives in [`AGENTS.md`](AGENTS.md); GitHub Copilot's variant is at [`.github/copilot-instructions.md`](.github/copilot-instructions.md). Testing details are in [`TESTING.md`](TESTING.md). Architectural Decision Records live in [`docs/decisions/`](docs/decisions/).

## Project

Operaton is a native BPMN 2.0 process engine for the JVM, forked from the Camunda 7 community edition. It is a large multi-module Maven project (~35+ modules) on **JDK 17+** (tested on 17, 21, 25). All packages use `org.operaton.bpm` (formerly `org.camunda.bpm`).

Key versions: Spring Boot 3.5+, Quarkus 3.26+, MyBatis 3.5.x, Liquibase 4.33, Angular 1.8.2 (legacy, kept for webapp compatibility).

## Build commands

Always run from the repo root — the Maven Wrapper requires it for the multi-module reactor.

```bash
# Full build with tests + frontend (~15-20 min)
./mvnw clean install

# Fast build (~5-8 min)
./mvnw clean install -DskipTests -Dskip.frontend.build=true

# Skip QA modules if external network is flaky
./mvnw clean install -DskipTests -pl '!qa'
```

### Engine selective builds

The `engine` module depends on other modules in the reactor. If you build it in isolation and hit `package org.operaton.bpm.dmn.engine does not exist`, first install its dependencies:

```bash
./mvnw clean install -pl parent,bom,commons,model-api,engine-dmn,juel -DskipTests
./mvnw clean install -pl engine -DskipTests
```

`TESTING.md` recommends `-f <module>/pom.xml` over `-pl` for tiered modules like `qa/`.

### Memory tuning

If Maven OOMs during the full build:

```bash
export MAVEN_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"
```

### Resuming

After a failure, use the resume hint Maven prints, e.g. `./mvnw <args> -rf :operaton-engine`.

## Testing

```bash
./mvnw test                                       # everything (30+ min)
./mvnw test -pl engine                            # single module
./mvnw test -Dtest.includes=bpmn -pl engine       # include package substring
./mvnw test -Dtest.excludes=bpmn.async -pl engine # exclude takes precedence
./mvnw test -PtestBpmn -pl engine                 # predefined profile
```

Predefined engine test profiles: `testBpmn`, `testCmmn`, `testBpmnCmmn`, `testExceptBpmn`, `testExceptCmmn`, `testExceptBpmnCmmn`.

### Integration & DB tests

Integration suites live under `qa/`. Compose profiles as `<testsuite>,<runtime>,<database>`:

- **Runtime**: `tomcat`, `wildfly`, `wildfly-domain`
- **Testsuite**: `engine-integration`, `webapps-integration`
- **Database**: `h2`, `postgresql`, `mysql`, `oracle`, `sqlserver`, `db2` (only `h2` and `postgresql` work with `engine-integration`)

```bash
.devenv/scripts/build/build-and-run-integration-tests.sh
.devenv/scripts/build/build-and-run-integration-tests.sh --distro=wildfly --testsuite=webapps --db=postgresql
./mvnw test -Ppostgresql,testcontainers -pl engine
```

### Test conventions

- **JUnit 5/6 only** — do not write JUnit 4 tests. Use `@RegisterExtension static ProcessEngineExtension extension = ProcessEngineExtension.builder()...build();` to obtain a shared `ProcessEngine`; fields like `ProcessEngine processEngine;` are injected.
- **AssertJ** for assertions, not JUnit/Hamcrest.
- Favor composition with the extension over inheriting a base test class.
- The JUnit 6 / JUnit 5 variants of `operaton-engine` are pulled in via `<classifier>junit6</classifier>` / `<classifier>junit5</classifier>` (the standalone `operaton-bpm-junit5` artifact is deprecated).

## Webapps frontend

```bash
cd webapps/frontend
npm install
npm start          # dev server
npm run build
npm run lint
npm run prettier   # fixes ESLint/Prettier formatting failures
```

Run the backend alongside the frontend:

```bash
cd webapps/assembly
mvn jetty:run -Pdevelop
# webapps at http://localhost:8080, login jonny1/jonny1
```

## Code cleanup

Before pushing, normalize style/imports with the OpenRewrite recipes in `rewrite.yml`:

```bash
.devenv/scripts/maintenance/code-cleanup.sh    # ~12 min
```

Java: 2-space indent, 120-col, K&R braces. Import order (enforced by `rewrite.yml`): `java.*`, `jakarta.*`, blank, all others alphabetically, blank, `org.operaton.*`, blank, static imports.

JS/Frontend: 2-space indent, 80-col, single quotes, semicolons — Prettier/ESLint enforce.

All new files need the Apache 2.0 license header (see `AGENTS.md` for the template).

## Architecture

The engine is layered. Reading multiple files in `engine/src/main/java/org/operaton/bpm/engine/` is needed to understand it because each layer is split across many packages:

1. **Public API** (`org.operaton.bpm.engine.*`) — `ProcessEngine` is the entry point and exposes stateless, thread-safe services: `RuntimeService`, `TaskService`, `HistoryService`, `RepositoryService`, `IdentityService`, `ManagementService`, `AuthorizationService`, `ExternalTaskService`, `DecisionService`, `FilterService`, `FormService`, `CaseService`.
2. **Implementation** (`impl/`) — Service `*Impl` classes route every operation through the **Command pattern**: each public call is a `Command` object in `impl/cmd/` executed via an interceptor chain (transactions, auth, logging, context).
3. **PVM / BPMN execution** (`impl/pvm/`, `impl/bpmn/`) — Process Virtual Machine plus BPMN element `ActivityBehavior` strategies. Listeners (`TaskListener`, `ExecutionListener`) are the observer hooks.
4. **Persistence** (`impl/persistence/`) — MyBatis entities + mappers. DB-specific DDL/DML lives in `engine/src/main/resources/org/operaton/bpm/engine/db/`. Liquibase manages schema; multi-DB scripts are organized per vendor.
5. **DMN engine** — Separate module `engine-dmn/` with FEEL expression evaluation, invoked from the BPMN engine for decision tasks.

The **process engine is expensive to build** — treat it as a singleton (static field, JNDI, or container-managed). All public services are thread-safe.

### Modules at a glance

```
engine/                    Core BPMN engine
engine-dmn/                DMN decision engine
engine-rest/               JAX-RS REST API
engine-cdi/                CDI / Jakarta EE integration
engine-spring/             Spring Framework integration
spring-boot-starter/       Spring Boot auto-config
quarkus-extension/         Quarkus extension
webapps/                   Cockpit, Tasklist, Admin (Angular 1.8.2) + assembly
distro/                    Tomcat, WildFly, and standalone Run distributions
qa/                        Integration tests (runtime × testsuite × DB matrix)
database/                  DB compatibility test harness
commons/, model-api/, juel/, freemarker-template-engine/, spin/, connect/
```

## Conventions

- **Commit style:** Conventional Commits — `<type>(<scope>): <subject>`. Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`. Scopes are module-flavored (`engine`, `engine-rest`, `webapps`, `run`, `springboot`, `quarkus`, …).
- **Camunda 7 backports** (issues labeled `backport:c7`): squash to one commit; footer must reference the original Camunda issue, commit hash, and original author (see `AGENTS.md` / `copilot-instructions.md` for the exact template). Apply namespace + JUnit 5 + AssertJ migrations as you port. PR [#256](https://github.com/operaton/operaton/pull/256) is the reference example.
- **ADRs:** required for changes to core APIs, persistence, BPMN/DMN execution semantics, major deps, or anything breaking. Template at `docs/decisions/adr-template.md`.

## Docs caveat

Operaton's own docs (https://docs.operaton.org/) are under construction. The reference manual currently links to Camunda 7's docs at https://docs.camunda.org/manual/7.22/ — usage is generally compatible because of the fork.
