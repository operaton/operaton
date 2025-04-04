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
- `--db=<DATABASE>` - The database to use. Valid values: `h2` (default), `postgresql`.
- `--no-build` - Skip the build step.
- `--no-test` - Skip the test step.

Example:

```bash
./build/build-and-run-integration-tests.sh --testsuite=engine --distro=wildfly --db=h2
```
