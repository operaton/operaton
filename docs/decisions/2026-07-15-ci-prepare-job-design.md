# CI Build Optimization: `prepare` Job Design

**Date:** 2026-07-15
**Status:** Approved

## Goal

Reduce CI build time by adding a lightweight `prepare` job to `build.yml` that inspects the PR/push context and emits optimization flags consumed by the `build` job. The heuristics start simple (skip tests for non-code dependabot PRs) and are designed to grow without touching the workflow YAML.

---

## Components

| Component | Path | Purpose |
|-----------|------|---------|
| Composite action | `.github/actions/prepare-build/action.yml` | Declares inputs/outputs, invokes the Python script |
| Python script | `.github/actions/prepare-build/prepare_build.py` | All heuristic logic; writes flags to `$GITHUB_OUTPUT` via GitHub REST API |
| Build script | `.devenv/scripts/build/build.sh` | Two new flags: `--skip-engine-tests`, `--webapps-only` |
| Engine POM | `engine/pom.xml` | Remove undocumented `skipTests.operaton-engine` property and its surefire `<skip>` reference |
| Build workflow | `.github/workflows/build.yml` | New `prepare` job on `ubuntu-slim`; `build` job consumes its outputs |
| AI docs | `.github/copilot-instructions.md`, `AGENTS.md` | New "Faster Builds" section documenting the flags; cross-reference between the two files |

---

## Workflow Structure

```
prepare (ubuntu-slim, lightweight)
    │
    ├── lint-adr (existing, unchanged)
    │
    └── build (needs: prepare)
            │
            └── publish (existing, unchanged)
```

`prepare` runs in parallel with `lint-adr`. `build` waits for both.

---

## Composite Action

**Path:** `.github/actions/prepare-build/action.yml`

**Inputs:**
- `github-token` (required) — passed from the workflow as `${{ secrets.GITHUB_TOKEN }}`

**Outputs:**
- `skip_tests` — `"true"` or `"false"`
- `skip_engine_tests` — `"true"` or `"false"`
- `webapps_only` — `"true"` or `"false"`

**Steps:**
1. Sparse checkout of `.github/actions/prepare-build/` — required so GitHub Actions can locate the local composite action files
2. Run `prepare_build.py` with `GITHUB_TOKEN`, `GITHUB_OUTPUT`, and GitHub context env vars

The Python script does not use the checked-out source for changed-file detection; it fetches changed files via the GitHub REST API.

---

## Python Script

**Path:** `.github/actions/prepare-build/prepare_build.py`

Uses only Python stdlib (`urllib.request`, `json`, `os`, `sys`). No third-party packages.

**Context sources:**
- `GITHUB_EVENT_NAME` — `pull_request` or `push`
- `GITHUB_ACTOR` — PR author login
- `GITHUB_HEAD_REF` — PR branch name (only set on `pull_request` events)
- `GITHUB_EVENT_PATH` — path to the full event payload JSON (for PR number)
- `GITHUB_REPOSITORY` — `owner/repo`
- `GITHUB_TOKEN` — for REST API calls
- `GITHUB_OUTPUT` — file to write outputs to

**Changed-file detection:**
- On `pull_request`: calls `GET /repos/{owner}/{repo}/pulls/{number}/files` (paginated)
- On `push`: all heuristics that require file inspection are skipped (full build)

**Heuristics (evaluated in order, PR events only):**

### 1. `skip_tests`
**Condition:** `GITHUB_ACTOR == "dependabot[bot]"` AND `GITHUB_HEAD_REF` starts with `dependabot/github_actions` or `dependabot/npm_and_yarn`

**Rationale:** These PRs update CI action versions or JavaScript package locks — no Java source changes, no value in running tests.

**Effect:** Sets `skip_tests=true`. Skips evaluation of remaining heuristics.

### 2. `skip_engine_tests`
**Condition:** No changed file has a path under any of:
- `engine/`
- `engine-dmn/`
- `engine-rest/`
- `engine-cdi/`
- `engine-spring/`
- `commons/`
- `model-api/`
- `juel/`

**Rationale:** Engine tests dominate build time. If none of the engine module or its in-reactor dependencies changed, engine tests provide no signal.

**Effect:** Sets `skip_engine_tests=true`.

### 3. `webapps_only`
**Condition:** All changed files are under `webapps/`

**Rationale:** Webapps changes don't affect the engine, engine-rest, or any other module. Building the full reactor is wasteful.

**Effect:** Sets `webapps_only=true`.

**Edge cases:**
- If the GitHub API returns an empty file list (e.g., PR not yet ready), all heuristics that require file inspection default to `"false"` (safe: full build runs).
- On `workflow_dispatch`, `GITHUB_HEAD_REF` is empty and `GITHUB_EVENT_NAME` is not `pull_request`, so all outputs are `"false"`.
- On fork PRs, `secrets.GITHUB_TOKEN` is read-only but still valid for the `pulls/*/files` API. If the API call fails for any reason, the script logs a warning and returns `[]`, falling back to a full build — the safe direction.

**Default env vars:** The script reads `GITHUB_EVENT_NAME`, `GITHUB_ACTOR`, `GITHUB_HEAD_REF`, `GITHUB_REPOSITORY`, `GITHUB_EVENT_PATH`, and `GITHUB_OUTPUT` from the environment. These are injected automatically by the GitHub Actions runner in every step. They are also declared explicitly in `action.yml`'s `env:` block for self-documentation.

**Default:** All outputs default to `"false"` if no heuristic fires.

---

## `build.sh` Additions

Two new flags added to `.devenv/scripts/build/build.sh`:

### `--skip-engine-tests`
Appends `-Dtest.excludes=org/operaton/bpm/engine` to the Maven command.

Uses the existing `test.excludes` Surefire integration in `engine/pom.xml`:
```xml
<exclude>%regex[.*(${test.excludes}).*Test.*.class]</exclude>
```
The pattern `org/operaton/bpm/engine` matches all engine test class paths. The default value (`$.`) never matches, so the flag is a no-op unless explicitly set.

### `--webapps-only`
Appends `-pl webapps/assembly -am` to the Maven command.

**Verified reactor (29 modules):** Root POM → Parent → Commons → Model APIs → JUEL → DMN → Testing utils → Engine → BOM → Engine-REST → Webapp Root → Webapp Assembly.

**Important:** `-pl webapps -am` is insufficient — it resolves only the 4-module aggregator POM chain and misses the `webapps/assembly` submodule and all its dependencies. Always use `-pl webapps/assembly -am`.

Verified to work from a completely empty Maven local repository — parent POMs and BOMs are in the reactor and built in the correct order by `-am`.

---

## `engine/pom.xml` Cleanup

Remove the undocumented `skipTests.operaton-engine` feature:
1. Delete the `<skipTests.operaton-engine>false</skipTests.operaton-engine>` property
2. Replace `<skip>${skipTests.operaton-engine}</skip>` in the surefire plugin config with `<skip>false</skip>` (hard-coded, since the new `test.excludes` approach handles engine test skipping)

---

## `build.yml` Changes

### New `prepare` job

```yaml
prepare:
  name: Prepare
  runs-on: ubuntu-slim
  permissions:
    contents: read
    pull-requests: read
  outputs:
    skip_tests: ${{ steps.prepare.outputs.skip_tests }}
    skip_engine_tests: ${{ steps.prepare.outputs.skip_engine_tests }}
    webapps_only: ${{ steps.prepare.outputs.webapps_only }}
  steps:
    - name: Checkout (sparse, for composite action)
      uses: actions/checkout@v7.0.0
      with:
        sparse-checkout: .github/actions/prepare-build
    - name: Prepare build
      id: prepare
      uses: ./.github/actions/prepare-build
      with:
        github-token: ${{ secrets.GITHUB_TOKEN }}
```

### Updated `build` job header

```yaml
build:
  name: Build
  needs: [lint-adr, prepare]
  runs-on: ubuntu-24.04
```

### Updated Maven Build step

The existing `skip_tests` from `workflow_dispatch` is merged with the prepare output:

```yaml
SKIP_TESTS="${{ github.event.inputs.skip_tests || needs.prepare.outputs.skip_tests || 'false' }}"
SKIP_ENGINE_TESTS="${{ needs.prepare.outputs.skip_engine_tests || 'false' }}"
WEBAPPS_ONLY="${{ needs.prepare.outputs.webapps_only || 'false' }}"

if [ "$SKIP_TESTS" = "true" ]; then
  BUILD_CMD="$BUILD_CMD --skip-tests"
fi
if [ "$SKIP_ENGINE_TESTS" = "true" ]; then
  BUILD_CMD="$BUILD_CMD --skip-engine-tests"
fi
if [ "$WEBAPPS_ONLY" = "true" ]; then
  BUILD_CMD="$BUILD_CMD --webapps-only"
fi
```

---

## AI Tool Documentation

GitHub Copilot reads `.github/copilot-instructions.md`; OpenAI agents read `AGENTS.md`. They cannot be auto-synchronized. Strategy: add the same "Faster Builds" section to both files and add a mutual cross-reference.

**New section added to both files:**

```markdown
### Faster Builds

Use `.devenv/scripts/build/build.sh` flags for targeted builds:

| Flag | Maven effect | CI condition |
|------|-------------|-------------|
| `--skip-tests` | `-DskipTests` | All dependabot GitHub Actions / npm PRs |
| `--skip-engine-tests` | `-Dtest.excludes=org/operaton/bpm/engine` | No changes in engine or its deps |
| `--webapps-only` | `-pl webapps/assembly -am` | All changes are within `webapps/` |

**Note:** `-pl webapps -am` is NOT sufficient (resolves only the aggregator POM).
Use `-pl webapps/assembly -am`, which builds the full 29-module chain including engine and engine-rest.

The CI `prepare` job (`.github/actions/prepare-build/`) sets these flags automatically
based on PR branch name, actor, and changed files.
```

---

## Extensibility

Adding new heuristics requires only editing `prepare_build.py` — the workflow YAML and composite action do not need to change unless a new output variable is introduced.

Future candidates:
- Skip DMN tests when no `engine-dmn/` changes
- Skip frontend build when no `webapps/frontend/` changes (`-Dskip.frontend.build=true`)
- Skip spring-boot-starter tests when only `webapps/` or `engine/` changed
