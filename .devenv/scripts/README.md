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
- `--skip-engine-tests` - Skip only engine tests (appends `-Dtest.excludes=org/operaton/bpm/engine`). Useful when no engine-related files changed.
- `--webapps-only` - Build only `webapps/assembly` and its transitive dependencies (appends `-pl webapps/assembly -am -Dmaven.test.skip=true`). Use when only `webapps/` files changed. Note: `-pl webapps -am` is insufficient — it resolves only the aggregator POM chain. Always use `-pl webapps/assembly -am -Dmaven.test.skip=true`.

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

`jq` must be installed.  

# Directory `smoketest`

Automated smoke tests against the Operaton Docker SNAPSHOT images. Used during release preparation (PREPARE phase) and post-release verification (PERFORM phase).

Each image runs on a dedicated port to avoid conflicts with locally-running Operaton instances or other services on 8080:

| Image | Port |
|-------|------|
| `operaton/operaton` | 18080 |
| `operaton/wildfly` | 18081 |
| `operaton/tomcat` | 18082 |

**Requirements:** `docker`, `node` (playwright is installed automatically on first run into `/tmp`)

## `smoke-all.sh`

Run smoke tests against all three SNAPSHOT images sequentially.

```bash
.devenv/scripts/smoketest/smoke-all.sh
# Specific tag:
.devenv/scripts/smoketest/smoke-all.sh --tag=2.1.2
```

## `smoke-test.sh`

Run the smoke test for a single image.

**Options:**
- `--image=operaton|wildfly|tomcat` — which image to test (default: `operaton`)
- `--tag=TAG` — Docker tag to pull (default: `SNAPSHOT`)
- `--port=PORT` — override the default port
- `--keep` — leave the container running after the test

```bash
.devenv/scripts/smoketest/smoke-test.sh --image=operaton
.devenv/scripts/smoketest/smoke-test.sh --image=wildfly --tag=2.1.2
```

## `browser-flows.mjs`

Node.js/Playwright script that drives the Tasklist and Cockpit browser flows. Called automatically by `smoke-test.sh`. Can also be run directly:

```bash
node .devenv/scripts/smoketest/browser-flows.mjs http://localhost:18080 /tmp/screenshots
```

Screenshots are saved to `.devenv/scripts/smoketest/screenshots/<image>/` during automated runs.

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

# Directory `release`

Helpers for the post-release announcement phase (the ANNOUNCE action of the `operaton-release` skill). They prepare and list only — they never publish or delete.

## `list-merged-branches.sh`

List the branches of merged PRs in a release milestone, with links, so a human can review and delete them. Read-only; never deletes.

```bash
.devenv/scripts/release/list-merged-branches.sh "<MILESTONE>" [--repo OWNER/REPO]
```

## `update-website.py`

Bump the `#changelog` card in operaton.org `index.html` and scaffold a blog post under `_posts/` from the most recent release post. Edits a local operaton.org checkout; does not commit, push, or open a PR.

```bash
.devenv/scripts/release/update-website.py --version 2.1.2 --summary "..." [--repo PATH]
```

When several releases ship the same day, pass the highest version; the single changelog card always shows the highest. See `.devenv/scripts/release/README.md` for details.
