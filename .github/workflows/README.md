# GitHub Workflows

This directory contains the GitHub Actions workflows for the Operaton project. Each workflow is defined in a separate YAML file and
serves a specific purpose in the CI/CD pipeline.

## Workflows

### Build

- **Filename**: `build.yml`
- **Description**: This workflow builds Operaton and caches/restores any dependencies to improve the workflow execution time.
- **Triggers**:
    - On push to the `main` branch (with certain paths ignored)
    - On pull request to the `main` branch (with certain paths ignored)

### Integration Build

- **Filename**: `integration-build.yml`
- **Description**: This workflow runs integration tests on a schedule and can be manually triggered. It supports various
  configurations for Java versions, test suites, distributions, and databases.
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
- **Description**: This workflow triggers nightly integration builds on all release branches. It detects all branches with
  `release/` prefix and dispatches the Integration Build workflow for each branch with comprehensive test configurations.
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
- **Description**: This workflow creates a task issue for Copilot when an issue or pull request is closed and labeled with "
  noteworthy". The task issue contains detailed instructions and context for Copilot to create a comprehensive changelog entry.
- **Triggers**:
    - When an issue is closed with the "noteworthy" label
    - When a pull request is merged with the "noteworthy" label
- **Features**:
    - Automatically identifies related PRs for issues
    - Categorizes changes based on labels (bug fixes, features, documentation, etc.)
    - Creates a new issue assigned to @copilot with detailed instructions
    - Includes PR details (files changed, lines added/removed, merge date)
    - Provides source description and context for creating the changelog entry
- **Usage**: Simply add the "noteworthy" label to any issue or PR that should be highlighted in the changelog before closing/merging
  it. A task issue will be automatically created and assigned to Copilot to update the changelog.

### Label PR

- **Filename**: `label-pr.yml`
- **Description**: Automatically assigns and removes labels on pull requests based on changed file paths and PR title patterns.
  Labels not managed by this workflow (e.g. `breaking`, `noteworthy`, `released`, `backport:*`) are never touched.
- **Triggers**:
    - On pull request opened, synchronized, reopened, or edited (title change)
- **Jobs**:
    - `path-labels`: Uses `actions/labeler` with `.github/labels/gh-labeler.yml`. Assigns labels such as `lang:java`, `scope:*`,
      `distro:*`, `database`, `qa`. Removes managed labels when files no longer match (`sync-labels: true`).
    - `title-labels`: Uses `TimonVS/pr-labeler-action` with `.github/labels/pr-labeler.yml`. Assigns `database:*` and
      `integration:*` labels based on PR title regex (primarily Dependabot bump PRs).
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

The versions below are a high-level summary. The authoritative action pins are defined in the individual workflow files and may vary
by workflow over time.

| Action                                      | Version used in workflows* | Description                                                |
|---------------------------------------------|----------------------------|------------------------------------------------------------|
| `actions/checkout`                          | v7.0.1                     | Checks out the repository code.                            |
| `actions/cache` (incl. `/restore`, `/save`) | v6.1.0                     | Caches dependencies to improve workflow execution time.    |
| `actions/setup-java`                        | v5.7.0                     | Sets up the Java environment.                              |
| `actions/setup-node`                        | v7.0.0                     | Sets up the Node.js environment.                           |
| `actions/upload-artifact`                   | v7.0.1                     | Uploads build artifacts.                                   |
| `actions/download-artifact`                 | v8.0.1                     | Downloads build artifacts.                                 |
| `actions/github-script`                     | v9.0.0                     | Runs inline JavaScript against the GitHub API.             |
| `actions/labeler`                           | v7.0.0                     | Applies and removes PR labels based on changed file paths. |
| `actions/stale`                             | v11.0.0                    | Marks/closes stale issues and PRs.                         |
| `actions/configure-pages`                   | v6.0.0                     | Configures GitHub Pages.                                   |
| `actions/deploy-pages`                      | v5.0.0                     | Deploys GitHub Pages.                                      |
| `actions/upload-pages-artifact`             | v5.0.0                     | Uploads a GitHub Pages artifact.                           |
| `mikepenz/action-junit-report`              | v6.4.2                     | Publishes JUnit test reports.                              |
| `stefanzweifel/git-auto-commit-action`      | v7.2.0                     | Automatically commits changes to the repository.           |
| `EndBug/add-and-commit`                     | v11.0.0                    | Automatically commits and pushes changes.                  |
| `jreleaser/release-action`                  | 2.5.0                      | Releases the project.                                      |
| `TimonVS/pr-labeler-action`                 | v5.0.0                     | Applies and removes PR labels based on PR title patterns.  |
| `crazy-max/ghaction-github-labeler`         | v6.0.0                     | Syncs label definitions from `.github/labels/labels.yml`.  |
| `peter-evans/create-pull-request`           | v8.1.1                     | Opens a pull request from workflow changes.                |
| `DavidAnson/markdownlint-cli2-action`       | v24.2.0                    | Lints Markdown files.                                      |
| `docker/setup-buildx-action`                | v4.2.0                     | Sets up Docker Buildx.                                     |

\* See the individual workflow files for the exact pinned version used by each job.

### Pinning actions to commit SHAs

All actions in this directory are pinned to a full-length commit SHA rather than a mutable version tag, with the corresponding
release tag kept as a trailing comment for readability:

```yaml
uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
```

This protects against a tag being retargeted (accidentally or maliciously) to point at different, unreviewed code after the workflow
was last audited — a tag like `@v7` or even `@v7.0.1` can be moved by the upstream repository at any time, while a commit SHA is
immutable. When bumping an action to a newer release, resolve the new tag to its commit SHA (e.g. via `git ls-remote --tags <repo>`
or the GitHub API) and update both the SHA and the version comment together.

For more details on each workflow, you can view the workflow files in
the [.github/workflows](https://github.com/operaton/operaton/tree/main/.github/workflows) directory.
