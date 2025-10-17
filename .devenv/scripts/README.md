# Scripts

This directory contains scripts that are used to automate various tasks. 
The scripts are intended to be run from the root of the repository.

# Directory `build`

This directory contains scripts that are used to build the project.

## `build.sh`

Use this script to perform the build of the project.

The script has the following options:

- `--profile=<PROFILE>` - The build profile to use. Valid values: `fast`, `normal` (default), `max`. This will activate a different amount of Maven profiles.
- `--reports` - Execute Reporting plugins to generate update reports, code statistics.
- `--skip-tests` - Skip the test execution.

Any further arguments will be passed to the Maven build. 

### Examples

To build the project with the `fast` profile and skip the tests, you can run:

```bash
build.sh --profile=fast --skip-tests
```

Build the project with the `normal` profile and generate reports:
```bash
build.sh --reports
```

Build the project with the `max` profile and execute just a specific test:
```bash
build.sh --profile=max -Dsurefire.includes="**/MyTest*"
```

## `build-and-run-integration-tests.sh`

Use this script to perform various integration tests. 
The script will build the project and run the integration tests.

The script has the following options:

- `--testsuite=<TESTSUITE>` - The test suite to run. Valid values: `engine` (default), `webapps`.
- `--distro=<TESTSUITE>` - The application distribution to perform the test on. Valid values: `tomcat` (default), `wildfly`.
- `--db=<DATABASE>` - The database to use. Valid values: `h2` (default), `postgresql` `postgresql-xa` `mysql` `mariadb`, `oracle` `db2` `sqlserver`.
- `--no-build` - Skip the build step.
- `--no-test` - Skip the test step.

Example:

```bash
./build/build-and-run-integration-tests.sh --testsuite=engine --distro=wildfly --db=h2
```

## `build-and-run-database-update-tests.sh`

Use this script to perform database upgrade tests.

The script has the following options:

- `--db=<DATABASE>` - The database to use. Valid values: `h2` (default), `postgresql` `postgresql-xa` `mysql` `mariadb`, `oracle` `db2` `sqlserver`.
- `--no-build` - Skip the build step.
- `--no-test` - Skip the test step.

Example:

```bash
./build/build-and-run-database-update-tests.sh --db=h2
```

## `check-api-compatibility.sh`

This script checks the API compatibility of the main Java modules against a previous version using the Maven Clirr plugin.

**Usage:**
```bash
.devenv/scripts/build/check-api-compatibility.sh [--comparison-version <version>] [--help]
```

**Options:** 
- `--comparison-version <version>` Sets the Clirr comparison version (overrides the default).</version>
- `--help` Shows help and exits.


**Example:**
```bash
.devenv/scripts/build/check-api-compatibility.sh --comparison-version 7.24.0
``` 

Results are available under `target/reports/clirr/` and in the `target/reports/clirr/clirr.md` file.

## `rest-api-doc.sh`

This script prepares REST API documentation from the OpenAPI specification using [redocly](https://redocly.com/).
The output is an HTML file and the OpenAPI specification located at `target/rest-api/<VERSION>`.

The version is extracted from the OpenAPI specification file.

The script checks if the OpenAPI specification exists and generates it using Maven if necessary.

**Usage:**
```bash
.devenv/scripts/build/rest-api-doc.sh
```

**Options:** 
none

**Requirements:**

Node.js, `npm`, `npx` and `jq` must be installed.  

# Directory `maintenance`

## `code-cleanup.sh`

Use this script to perform code cleanup tasks. 
The cleanups are performed using OpenRewrite.

Execute this script from the root of the repository:

```bash
.devenv/scripts/maintenance/code-cleanup.sh
```

### Options

- `--cleanups=LIST`  
  Comma-separated list of cleanups to run. Valid values: `imports`, `code`, `tests`.  
  If not specified, all cleanups are run.
- `--help`  
  Show usage and exit.

### Examples

Run all cleanups (imports, code, tests):

```bash
.devenv/scripts/maintenance/code-cleanup.sh
```

Run only imports and code cleanups:

```bash
.devenv/scripts/maintenance/code-cleanup.sh --cleanups=imports,code
```

Show help:

```bash
.devenv/scripts/maintenance/code-cleanup.sh --help
```

If an invalid cleanup value is specified, the script prints an error and exits.

## `init-database-version.py` — Database Version Maintenance

This script automates the process of initializing and updating the database version across all supported databases and related files.

### Usage
```bash
.devenv/scripts/maintenance/init-database-version.py [OPTIONS]
```

#### Options:

- `--new-version <version>`: Specify the new database version (semantic versioning: `major.minor.patch`). If omitted, the script proposes the next minor version.
- `--database <db1,db2,...>`: Comma-separated list of databases to update. If omitted, all supported databases are selected.
- `-y, --assume-yes`: Run non-interactively, accepting all default answers.

**Example: Perform interactive update**

```bash
.devenv/scripts/maintenance/init-database-version.py
```

**Example: Perform non-interactive update to the next minor version for all databases**

```bash
.devenv/scripts/maintenance/init-database-version.py -y
```

**Example: Non-interactive update to a specific version for selected databases**

```bash
python3 .devenv/scripts/maintenance/init-database-version.py --new-version 7.25.0 --database postgres,mysql -y
```

### What it does:

- Reads and updates the current and previous database version in database/pom.xml.
- Updates SQL and Liquibase changelog files for the selected databases.
- Creates new upgrade scripts and test fixtures for the new version.
- Updates references in QA and test modules.

### Notes
- Always run this script from the repository root.
- Review and commit the changes after running the script.
