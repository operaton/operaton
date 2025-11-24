# GitHub Workflows

This directory contains the GitHub Actions workflows for the Operaton project. Each workflow is defined in a separate YAML file and serves a specific purpose in the CI/CD pipeline.

## Workflows

### Build

- **Filename**: `build.yml`
- **Description**: This workflow builds Operaton and caches/restores any dependencies to improve the workflow execution time.
- **Triggers**:
    - On push to the `main` branch (with certain paths ignored)
    - On pull request to the `main` branch (with certain paths ignored)

### Integration Build

- **Filename**: `integration-build.yml`
- **Description**: This workflow runs integration tests on a schedule and can be manually triggered. It supports various configurations for Java versions, test suites, distributions, and databases.
- **Triggers**:
  - Scheduled daily at 2:00 AM UTC
  - Manually triggered via `workflow_dispatch`

### Release

- **Filename**: `release.yml`
- **Description**: This workflow handles the release process, including versioning and deployment.
- **Triggers**:
    - Manually triggered via `workflow_dispatch`

### Maintenance

- **Filename**: `maintenance.yml`
- **Description**: This workflow performs maintenance tasks such as updating the Slack invitation URL.
- **Triggers**:
    - Manually triggered via `workflow_dispatch` with a Slack URL input

### Nightly Trigger

- **Filename**: `nightly-trigger.yml`
- **Description**: This workflow triggers nightly integration builds on all release branches. It detects all branches with `release/` prefix and dispatches the Integration Build workflow for each branch with comprehensive test configurations.
- **Triggers**:
    - Scheduled daily at 3:00 AM UTC
    - Manually triggered via `workflow_dispatch`
- **Features**:
    - Automatically detects all release branches
    - Triggers integration builds with Java 17, 21, and 25
    - Tests engine and webapps test suites
    - Tests across operaton, tomcat, and wildfly distributions
    - Tests with h2 and postgresql databases

## Actions Used

| Action                                 | Version | Description                                             |
|----------------------------------------|---------|---------------------------------------------------------|
| `actions/checkout`                     | v4      | Checks out the repository code.                         |
| `actions/cache`                        | v4      | Caches dependencies to improve workflow execution time. |
| `mikepenz/action-junit-report`         | v5.5.1  | Publishes JUnit test reports.                           |
| `actions/upload-artifact`              | v4      | Uploads build artifacts.                                |
| `actions/setup-java`                   | v4      | Sets up the Java environment.                           |
| `stefanzweifel/git-auto-commit-action` | v5      | Automatically commits changes to the repository.        |
| `jreleaser/release-action`             | v2      | Releases the project.                                   |

For more details on each workflow, you can view the workflow files in the [.github/workflows](https://github.com/operaton/operaton/tree/main/.github/workflows) directory.