# Repository Guidelines

## Repository Overview

**Operaton** is a native BPMN 2.0 process engine that runs inside the Java Virtual Machine. It's a fork of Camunda 7 BPM platform, providing a complete stack for process automation including:
- Core BPMN 2.0 process engine with DMN decision engine
- REST API for remote process interaction
- Web applications (Cockpit, Tasklist, Admin) for process operations and task management
- Integration with Spring Boot, Quarkus, Jakarta EE, and CDI
- Support for multiple databases (H2, PostgreSQL, MySQL, MariaDB, Oracle, SQL Server, DB2)

**Key Technologies:**
- Java 17+ (tested on 17, 21, 25)
- Maven 3.9.0+ multi-module project (~35+ modules)
- Spring Boot 3.5.6, Spring Framework 6.2.11
- Quarkus 3.28.3
- MyBatis 3.5.19 for persistence
- Angular 1.8.2 for webapps frontend (legacy version maintained for compatibility)
- Liquibase 4.33.0 for schema management

## Essential Build Commands

**Always run commands from the repository root** - the Maven Wrapper requires this for multi-module projects.

### Core Build Commands

```bash
# Full build (includes tests and frontend) - ~15-20 minutes
./mvnw clean install

# Fast build (skip tests and frontend) - ~5-8 minutes
./mvnw clean install -DskipTests -Dskip.frontend.build=true

# Build specific module (after dependencies are built)
./mvnw clean install -pl engine -DskipTests

# Clean all modules
./mvnw clean
```

On Windows, use `mvnw` or `mvnw.cmd` instead of `./mvnw`.

### Critical Build Dependencies

The `engine` module requires these dependencies to be built first:
```bash
./mvnw clean install -pl parent,bom,commons,model-api,engine-dmn,juel -DskipTests
```

If you get compilation errors like "package org.operaton.bpm.dmn.engine does not exist", build these dependencies first.

### Testing Commands

```bash
# Run all tests (can take 30+ minutes)
./mvnw test

# Run tests for a specific module
./mvnw test -pl engine

# Run specific test categories using test.includes/excludes
./mvnw test -Dtest.includes=bpmn -pl engine
./mvnw test -Dtest.excludes=bpmn.async -pl engine

# Use predefined test profiles
./mvnw test -PtestBpmn -pl engine
./mvnw test -PtestExceptBpmn -pl engine
```

### Code Cleanup

Before submitting changes, run the code cleanup script to ensure compliance with coding standards:
```bash
.devenv/scripts/maintenance/code-cleanup.sh
```

This uses OpenRewrite with recipes defined in `rewrite.yml` to apply consistent formatting, import organization, and code quality improvements.

### Integration Tests

Integration tests are located in `qa/` directory and test the engine in actual runtime containers:

```bash
# Engine integration tests with H2
.devenv/scripts/build/build-and-run-integration-tests.sh

# Webapp integration tests with PostgreSQL and WildFly
.devenv/scripts/build/build-and-run-integration-tests.sh --distro=wildfly --testsuite=webapps --db=postgresql

# Database testing with Testcontainers
./mvnw test -Ppostgresql,testcontainers -pl engine
```

**Available Profiles:**
- **Runtime**: `tomcat`, `wildfly`, `wildfly-domain`
- **Testsuite**: `engine-integration`, `webapps-integration`
- **Database**: `h2`, `postgresql`, `mysql`, `oracle`, `sqlserver` (only H2 and PostgreSQL for engine-integration)

Compose profiles as: `./mvnw clean install -P<testsuite>,<runtime>,<database>`

### Frontend Development

```bash
cd webapps/frontend

# Install dependencies
npm install

# Development server (with hot reload)
npm start

# Build for production
npm run build

# Linting and formatting
npm run lint
npm run prettier
```

Run the backend server for development:
```bash
cd webapps/assembly
mvn jetty:run -Pdevelop
```

Webapps are then available at http://localhost:8080 (login: `jonny1`/`jonny1`).

## High-Level Architecture

### Core Engine Design

The process engine follows a **layered architecture**:

1. **API Layer** (`org.operaton.bpm.engine.*`)
   - Public service interfaces: `ProcessEngine`, `RuntimeService`, `TaskService`, `HistoryService`, `RepositoryService`, `IdentityService`, `ManagementService`, `AuthorizationService`, `ExternalTaskService`
   - Process engine is the central entry point, providing access to all services
   - Services are stateless and thread-safe

2. **Implementation Layer** (`org.operaton.bpm.engine.impl.*`)
   - Service implementations (e.g., `RuntimeServiceImpl`, `TaskServiceImpl`)
   - Uses **Command Pattern** extensively: all operations are encapsulated as command objects (found in `impl/cmd/`)
   - Commands are executed through interceptor chain for transaction management, logging, etc.

3. **Persistence Layer** (`org.operaton.bpm.engine.impl.persistence.*`)
   - MyBatis-based persistence with database-specific SQL scripts
   - Entity classes and mappers
   - Database scripts in `engine/src/main/resources/org/operaton/bpm/engine/db/`

4. **BPMN Execution** (`org.operaton.bpm.engine.impl.bpmn.*`)
   - BPMN parsing and model representation
   - **Strategy Pattern**: `ActivityBehavior` implementations define execution behavior for different BPMN elements
   - Process Virtual Machine (PVM) abstractions in `impl/pvm/`

5. **DMN Decision Engine** (`engine-dmn/` module)
   - Integrated DMN (Decision Model and Notation) engine
   - FEEL (Friendly Enough Expression Language) expression evaluation

### Key Design Patterns

- **Command Pattern**: All engine operations (e.g., `StartProcessInstanceCmd`, `CompleteTaskCmd`) for transactional execution
- **Strategy Pattern**: `ActivityBehavior` for pluggable BPMN element behavior
- **Observer Pattern**: Process listeners (`TaskListener`, `ExecutionListener`) for event handling
- **Factory Pattern**: `ProcessEngineConfiguration` for engine creation
- **Interceptor Chain**: Transaction management, authorization checks, logging

### Module Structure

```
operaton/
├── engine/                    # Core process engine (BPMN execution, APIs)
├── engine-dmn/                # DMN decision engine
├── engine-rest/               # REST API (JAX-RS based)
├── engine-cdi/                # CDI/Jakarta EE integration
├── engine-spring/             # Spring Framework integration
├── spring-boot-starter/       # Spring Boot auto-configuration
├── quarkus-extension/         # Quarkus extension
├── webapps/                   # Web applications
│   ├── frontend/              # Angular 1.8.2 frontend (Cockpit, Tasklist, Admin)
│   └── assembly/              # Webapp packaging and server integration
├── distro/                    # Distribution assemblies
│   ├── tomcat/                # Tomcat distribution
│   ├── wildfly/               # WildFly/JBoss distribution
│   └── run/                   # Standalone Operaton Run (embedded Tomcat)
├── qa/                        # Integration test suites
├── database/                  # Database compatibility testing
├── examples/                  # Example applications
├── commons/                   # Shared utilities
├── model-api/                 # BPMN/CMMN/DMN model API
└── docs/decisions/            # Architectural Decision Records (ADRs)
```

### REST API Architecture

- JAX-RS based REST API in `engine-rest/` module
- Stateless endpoints exposing engine services
- JSON serialization/deserialization
- Deployed in webapps or can be embedded standalone

### Web Applications

Three main applications built with Angular 1.8.2:
- **Cockpit**: Process operations and monitoring
- **Tasklist**: User task management interface
- **Admin**: User, group, and authorization management

Built with Webpack, uses operaton-commons-ui shared library.

### Database Layer

- Multi-database support with database-specific scripts
- Liquibase for schema migrations
- MyBatis for ORM
- Scripts organized by database type in `engine/src/main/resources/org/operaton/bpm/engine/db/`

## Development Guidelines

### Testing Best Practices

**Use JUnit 5** (not JUnit 4) for all new tests:

```java
public class MyProcessEngineTest {
  ProcessEngine processEngine;
  RuntimeService runtimeService;

  @RegisterExtension
  static ProcessEngineExtension extension = ProcessEngineExtension.builder()
      .configurationResource("my-config.xml")
      .build();

  @Test
  void testProcessExecution() {
    assertThat(processEngine).isNotNull();
    // test logic
  }
}
```

**Key testing guidelines:**
- Use `ProcessEngineExtension` for tests requiring a process engine
- Prefer AssertJ assertions over JUnit assertions
- Use Mockito for mocking; consider package-private methods for testability
- Mock services when possible for faster unit tests
- Integration tests go in `qa/` modules

### Code Style and Formatting

**Java:**
- 2 spaces indentation (no tabs)
- 120 characters line length
- K&R brace style (opening brace on same line)
- Import order (defined in `rewrite.yml`):
  - `java.*`
  - `jakarta.*`
  - (blank line)
  - All other imports (alphabetically)
  - (blank line)
  - `org.operaton.*`
  - (blank line)
  - Static imports

**JavaScript/Frontend:**
- 2 spaces indentation
- 80 characters line length
- Single quotes, semicolons required
- Automatically formatted via Prettier/ESLint

**Run code cleanup before committing:**
```bash
.devenv/scripts/maintenance/code-cleanup.sh
```

### License Headers

All new files must include the Apache 2.0 license header:

```java
/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

### Architectural Decision Records (ADRs)

Before implementing significant changes:
1. Review existing ADRs in `docs/decisions/`
2. Create new ADRs for architectural changes using the MADR template at `docs/decisions/adr-template.md`
3. ADRs are required for changes affecting:
   - Core APIs or system architecture
   - Major dependencies (databases, frameworks)
   - BPMN/DMN execution behavior
   - Breaking changes
   - Database schema or persistence layer

### Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

**Scopes:** `engine`, `engine-rest`, `webapps`, `run`, `springboot`, `quarkus`, etc.

**Example:**
```
feat(engine): Support BPEL execution

- implements execution for BPEL standard
- BPEL models are mapped to internal ActivityBehavior classes

related to #123
```

### Backporting from Camunda 7

For issues labeled `backport:c7`:
- Attribute the original commit properly
- Include backport metadata in commit body:
  ```
  Related to https://github.com/camunda/camunda-bpm-platform/issues/{ISSUE}

  Backported commit {HASH} from the camunda-bpm-platform repository.
  Original author: {AUTHOR_NAME} <{AUTHOR_EMAIL}>
  ```
- Adapt namespace changes: `org.camunda.bpm` → `org.operaton.bpm`
- Migrate JUnit 4 → JUnit 5 and JUnit assertions → AssertJ assertions
- Verify backport with full build: `./mvnw clean install`
- Run integration tests if dependencies changed

## Common Development Workflows

### Making Changes to Engine Code

1. Build core dependencies:
   ```bash
   ./mvnw clean install -pl parent,bom,commons,model-api,engine-dmn,juel -DskipTests
   ```

2. Make your changes to the engine module

3. Build and test:
   ```bash
   ./mvnw clean test -pl engine
   ```

4. Run code cleanup:
   ```bash
   .devenv/scripts/maintenance/code-cleanup.sh
   ```

5. Full validation before PR:
   ```bash
   ./mvnw clean install -DskipTests
   ```

### Working with Frontend

1. Navigate to frontend directory:
   ```bash
   cd webapps/frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start development server:
   ```bash
   npm start
   ```

4. In another terminal, start backend:
   ```bash
   cd webapps/assembly
   mvn jetty:run -Pdevelop
   ```

5. Lint and format before committing:
   ```bash
   npm run lint
   npm run prettier
   ```

### Database Development

- SQL scripts are in `engine/src/main/resources/org/operaton/bpm/engine/db/`
- Test locally with H2, use PostgreSQL for integration tests
- Use Testcontainers for database testing: `./mvnw test -Ppostgresql,testcontainers -pl engine`

## Common Build Issues

### Network Download Failures
**Problem:** `chromedriver.storage.googleapis.com: No address associated with hostname`
**Solution:** Skip QA modules:
```bash
./mvnw clean install -DskipTests -pl '!qa'
```

### Compilation Errors in Engine Module
**Problem:** `package org.operaton.bpm.dmn.engine does not exist`
**Solution:** Build dependencies first:
```bash
./mvnw clean install -pl parent,bom,commons,model-api,engine-dmn,juel -DskipTests
```

### Frontend Linting Issues
**Problem:** ESLint/Prettier formatting errors
**Solution:**
```bash
cd webapps/frontend && npm run prettier
```

### Out of Memory Errors
**Solution:** Increase Maven memory:
```bash
export MAVEN_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"
```

### Resuming Failed Builds
After a build failure, Maven will suggest a resume command:
```bash
./mvnw <args> -rf :operaton-engine
```

## Important Notes

- **Namespace change from Camunda:** All packages use `org.operaton.bpm` instead of `org.camunda.bpm`
- **Process engine is a singleton:** Building a ProcessEngine is expensive; store in static field or JNDI
- **Thread safety:** Engine and services are thread-safe
- **Database support:** H2 for local dev, PostgreSQL/MySQL/Oracle/SQL Server/DB2 for production
- **Angular 1.8.2:** Webapps use legacy Angular version for backwards compatibility
- **Documentation:** Currently referencing Camunda 7 Manual at https://docs.camunda.org/manual/7.22/ (Operaton docs under construction at https://docs.operaton.org/)

## Resources

- **Documentation:** https://docs.operaton.org/
- **Forum:** https://forum.operaton.org/
- **Issue Tracker:** https://github.com/operaton/operaton/issues
- **Contributing Guide:** CONTRIBUTING.md
- **Testing Guide:** TESTING.md
- **ADRs:** docs/decisions/
- **Javadoc:** https://operaton.github.io/operaton/javadoc/operaton/1.0/index.html
