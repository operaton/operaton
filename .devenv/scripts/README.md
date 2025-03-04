# Scripts

This directory contains scripts that are used to automate various tasks. 
The scripts are intended to be run from the root of the repository.

# Directory `build`

This directory contains scripts that are used to build the project.

## `build-and-run-integration-tests.sh`

Use this script to perform various integration tests. 
The script will build the project and run the integration tests.

The script has the following options:

- `--testsuite=<TESTSUITE>` - The test suite to run. Valid values: `engine` (default), `webapps`.
- `--container=<TESTSUITE>` - The application container to perform the test on. Valid values: `tomcat` (default), `wildfly`.
- `--db=<DATABASE>` - The database to use. Valid values: `h2` (default), `postgres`.
- `--no-build` - Skip the build step.
- `--no-test` - Skip the test step.

Example:

```bash
./build/build-and-run-integration-tests.sh --testsuite=engine --container=wildfly --db=h2
```
