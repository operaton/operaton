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

### Create Changelog Task for Noteworthy Changes

- **Filename**: `update-changelog-noteworthy.yml`
- **Description**: This workflow creates a task issue for Copilot when an issue or pull request is closed and labeled with "noteworthy". The task issue contains detailed instructions and context for Copilot to create a comprehensive changelog entry.
- **Triggers**:
    - When an issue is closed with the "noteworthy" label
    - When a pull request is merged with the "noteworthy" label
- **Features**:
    - Automatically identifies related PRs for issues
    - Categorizes changes based on labels (bug fixes, features, documentation, etc.)
    - Creates a new issue assigned to @copilot with detailed instructions
    - Includes PR details (files changed, lines added/removed, merge date)
    - Provides source description and context for creating the changelog entry
- **Usage**: Simply add the "noteworthy" label to any issue or PR that should be highlighted in the changelog before closing/merging it. A task issue will be automatically created and assigned to Copilot to update the changelog.

### Label PR

- **Filename**: `label-pr.yml`
- **Description**: Automatically assigns and removes labels on pull requests based on changed file paths and PR title patterns. Labels not managed by this workflow (e.g. `breaking`, `noteworthy`, `released`, `backport:*`) are never touched.
- **Triggers**:
    - On pull request opened, synchronized, reopened, or edited (title change)
- **Jobs**:
    - `path-labels`: Uses `actions/labeler@v5` with `.github/labels/gh-labeler.yml`. Assigns labels such as `lang:java`, `scope:*`, `distro:*`, `database`, `qa`. Removes managed labels when files no longer match (`sync-labels: true`).
    - `title-labels`: Uses `TimonVS/pr-labeler-action` with `.github/labels/pr-labeler.yml`. Assigns `database:*` and `integration:*` labels based on PR title regex (primarily Dependabot bump PRs).
- **Config files**:
    - `.github/labels/gh-labeler.yml` — path-based rules
    - `.github/labels/pr-labeler.yml` — title-based rules

### Sync Labels

- **Filename**: `sync-labels.yml`
- **Description**: Syncs label definitions from `.github/labels/labels.yml` to the GitHub repository.
- **Triggers**:
    - On push to the `main` branch when `.github/labels/labels.yml` or `.github/workflows/sync-labels.yml` changes
    - On pull request when `.github/labels/labels.yml` or `.github/workflows/sync-labels.yml` changes
- **Jobs**:
    - `labeler`: Uses `crazy-max/ghaction-github-labeler` to sync labels. Runs in dry-run mode on pull requests to preview changes.
- **Config files**:
    - `.github/labels/labels.yml` — label definitions

## Actions Used

| Action                                  | Version | Description                                             |
|-----------------------------------------|---------|---------------------------------------------------------|
| `actions/checkout`                      | v4      | Checks out the repository code.                         |
| `actions/cache`                         | v4      | Caches dependencies to improve workflow execution time. |
| `mikepenz/action-junit-report`          | v5.5.1  | Publishes JUnit test reports.                           |
| `actions/upload-artifact`               | v4      | Uploads build artifacts.                                |
| `actions/setup-java`                    | v4      | Sets up the Java environment.                           |
| `stefanzweifel/git-auto-commit-action`  | v5      | Automatically commits changes to the repository.        |
| `jreleaser/release-action`              | v2      | Releases the project.                                   |
| `actions/labeler`                       | v5      | Applies and removes PR labels based on changed file paths. |
| `TimonVS/pr-labeler-action`             | v5      | Applies and removes PR labels based on PR title patterns.  |
| `crazy-max/ghaction-github-labeler`     | v6      | Syncs label definitions from `.github/labels/labels.yml`.  |

For more details on each workflow, you can view the workflow files in the [.github/workflows](https://github.com/operaton/operaton/tree/main/.github/workflows) directory.
