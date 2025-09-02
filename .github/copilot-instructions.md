# Operaton Repository - Copilot Instructions

## Repository Overview

**Operaton** is a native BPMN 2.0 process engine that runs inside the Java Virtual Machine. It's a fork of Camunda 7 BPM platform and provides a complete stack for process automation including web applications for human workflow management, operations, and monitoring.

- **Repository Size**: Large multi-module Maven project (~35+ modules)
- **Languages**: Java 17+, JavaScript/TypeScript (Angular 1.8.x), HTML/CSS, SQL
- **Primary Frameworks**: Spring Boot 3.5.5, Quarkus 3.26.0, Angular 1.8.x, MyBatis 3.5.19
- **Database Support**: H2, PostgreSQL, MySQL, MariaDB, Oracle, SQL Server, DB2
- **Runtime Containers**: Tomcat, WildFly, Standalone (Operaton Run)

## Build Instructions

### Prerequisites

- **JDK 17 or newer** (required) - verify with `java -version`
- **Maven 3.9.0+** (Maven Wrapper provided - use `./mvnw` or `mvnw.cmd`)
- **Node.js v20+** and **npm 10+** (for frontend builds)

### Essential Build Commands

**Always run commands from the repository root** - the Maven Wrapper requires this for multi-module projects.

#### Core Build Commands
```bash
# Full build (includes tests and frontend) - ~15-20 minutes
./mvnw clean install

# Fast build (skip tests and frontend) - ~5-8 minutes  
./mvnw clean install -DskipTests -Dskip.frontend.build=true

# Build specific modules (after dependencies built)
./mvnw clean install -pl engine -DskipTests

# Clean all modules
./mvnw clean

# Clean up code to common coding standards using OpenRewrite and rules in `rewrite.yml`- ~12 minutes
.devenv/scripts/maintenance/code-cleanup.sh
```

#### Critical Build Order Dependencies
The engine module **requires** these dependencies built first:
```bash
# Build core dependencies first if building selectively
./mvnw clean install -pl parent,bom,commons,model-api,engine-dmn,juel -DskipTests
```

### Testing Commands

```bash
# Run all tests (can take 30+ minutes)
./mvnw test

# Run specific test categories  
./mvnw test -Dtest.includes=bpmn -pl engine
./mvnw test -Dtest.excludes=bpmn.async -pl engine

# Use predefined test profiles
./mvnw test -PtestBpmn -pl engine
./mvnw test -PtestExceptBpmn -pl engine
```

### Maven Profiles for Integration Testing

Compose profiles for **runtime container + testsuite + database**:

```bash
# Engine integration tests with H2
..devenv/scripts/build/build-and-run-integration-tests.sh

# Webapp integration tests with PostgreSQL
.devenv/scripts/build/build-and-run-integration-tests.sh --distro=wildfly --testsuite=webapps --db=postgresql

# Database testing with Testcontainers
./mvnw test -Ppostgresql,testcontainers -pl engine
```

**Available Profiles:**
- **Runtime**: `tomcat`, `wildfly`
- **Testsuite**: `engine-integration`, `webapps-integration`  
- **Database**: `h2`, `postgresql`, `mysql`, `oracle`, `sqlserver` (only H2 and PostgreSQL for engine-integration)

### Frontend Build Commands

```bash
cd webapps/frontend

# Install dependencies
npm install

# Build for production
npm run build

# Development server
npm start

# Linting and formatting
npm run lint
npm run prettier
```

### Common Build Issues & Solutions

#### 1. Network Download Failures
**Problem**: `chromedriver.storage.googleapis.com: No address associated with hostname`
**Solution**: Network connectivity issue with external dependencies. Skip QA modules:
```bash
./mvnw clean install -DskipTests -pl '!qa'
```

#### 2. Compilation Errors in Engine Module
**Problem**: `package org.operaton.bpm.dmn.engine does not exist`
**Solution**: Missing dependencies. Build dependencies first:
```bash
./mvnw clean install -pl parent,bom,commons,model-api,engine-dmn,juel -DskipTests
./mvnw clean install -pl engine -DskipTests
```

#### 3. Frontend Linting Issues
**Problem**: ESLint JSON formatting errors
**Solution**: Run prettier to fix formatting:
```bash
cd webapps/frontend && npm run prettier
```

#### 4. Out of Memory Errors
**Solution**: Increase Maven memory:
```bash
export MAVEN_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"
```

#### 5. Resuming Failed Builds
**Problem**: Build fails partway through large multi-module build
**Solution**: Use Maven reactor resume:
```bash
# Maven will suggest the resume command after failure, e.g.:
./mvnw <args> -rf :operaton-engine
```

## Camunda 7 backports

For issues labeled with 'backport:c7' the following rules apply:

The backporting commit needs proper attribution of the original commit, including references to the backported commit, its original author and reference to the original issue.
The commit body should be formed like:
```

Related to https://github.com/camunda/camunda-bpm-platform/issues/{ISSUE}

Backported commit {HASH} from the camunda-bpm-platform repository.
Original author: {AUTHOR_NAME} <{AUTHOR_EMAIL}>
```

Squash changes to a single commit.

### Namespace change
Usually the changed files are located in the same module path, except for having operaton instead of camunda in their package name.

See [PR#256](https://github.com/operaton/operaton/pull/256/commits/a31bff0dd829abdbe2b66cf59ceae2f54dd9f9e2) as a reference for a proper backport.

## Project Layout & Architecture

### Core Module Structure

```
operaton/
├── engine/                    # Core BPMN 2.0 process engine
├── engine-dmn/                # DMN decision engine
├── engine-rest/               # REST API implementation
├── engine-cdi/                # CDI integration
├── engine-spring/             # Spring Framework integration
├── webapps/                   # Web applications
│   ├── frontend/              # Angular frontend (Cockpit, Tasklist, Admin)
│   └── assembly/              # Webapp packaging
├── distro/                    # Distribution assemblies
│   ├── tomcat/                # Tomcat distribution
│   ├── wildfly/               # WildFly distribution  
│   └── run/                   # Standalone Operaton Run
├── spring-boot-starter/       # Spring Boot integration
├── quarkus-extension/         # Quarkus integration
├── qa/                        # Quality assurance & integration tests
├── examples/                  # Example applications
└── docs/decisions/            # Architectural Decision Records (ADRs)
```

### Key Configuration Files

- **Build**: `pom.xml` (root + module poms), `mvnw`, `mvnw.cmd`
- **Frontend**: `webapps/frontend/package.json`, `webpack.*.js`, `.eslintrc`, `.stylelintrc.json`
- **CI/CD**: `.github/workflows/build.yml`, `.github/workflows/integration-build.yml`
- **Database**: `engine/src/main/resources/org/operaton/bpm/engine/db/`
- **Tests**: Uses JUnit 5, prefers `ProcessEngineExtension` for engine tests

### GitHub Workflows & CI

- **build.yml**: Main build workflow (triggered on PR/push to main)
- **integration-build.yml**: Comprehensive integration tests (scheduled daily)
- **migration-test.yml**: Database migration testing
- **SonarCloud integration** for code quality analysis

### Development Guidelines

#### Testing Best Practices
- **Use JUnit 5** (not JUnit 4) for new tests
- **Prefer AssertJ assertions** (not JUnit Assertions) for test assertions
- **Use ProcessEngineExtension** for engine tests requiring ProcessEngine objects
- Consider package-private methods for testing access
- Use `@RegisterExtension` pattern for test setup

#### Architectural Decision Records (ADRs)
- Located in `docs/decisions/`
- Review existing ADRs before significant changes
- Follow MADR format for new architectural decisions
- Create ADRs for changes affecting multiple components or core functionality

### Common Development Workflows

#### Making Code Changes
1. **Build core dependencies** if working on engine:
   ```bash
   ./mvnw clean install -pl parent,bom,commons,model-api,engine-dmn,juel -DskipTests
   ```

2. **Build and test your module**:
   ```bash
   ./mvnw clean test -pl <your-module>
   ```

3. **Run linting** for frontend changes:
   ```bash
   cd webapps/frontend && npm run lint
   ```

4. Code cleanup to comply to coding standards
   ```bash
   .devenv/scripts/maintenance/code-cleanup.sh
   ```

5. **Full validation** before PR:
   ```bash
   ./mvnw clean install -DskipTests
   ```

#### Database Development
- SQL scripts in `engine/src/main/resources/org/operaton/bpm/engine/db/`
- Use Liquibase for schema management
- Test with H2 locally, PostgreSQL for integration

#### Frontend Development  
- Angular 1.8.2 based (legacy version for compatibility)
- Uses Webpack for bundling
- ESLint + Stylelint for code quality
- Development server: `npm start` in `webapps/frontend/`

## Trust These Instructions

These instructions are based on comprehensive repository analysis and testing of build commands. **Trust these documented commands and only search for additional information if:**

1. The documented commands fail with errors not mentioned in "Common Build Issues"
2. You need to work with modules not covered in the project layout
3. You encounter configuration files not mentioned in this guide
4. You need database-specific setup not covered in the testing sections

The build commands, module dependencies, and common issues have been verified through actual execution in this repository environment.