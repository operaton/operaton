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

# Directory `maintenance`

## `code-cleanup.sh`

Use this script to perform code cleanup tasks. 
The cleanups are performed using OpenRewrite.

Execute this script from the root of the repository:

```bash
.devenv/scripts/maintenance/code-cleanup.sh
```

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
