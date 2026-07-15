# CI Build Optimization: `prepare` Job Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a lightweight `prepare` job to `build.yml` that inspects PR context and emits optimization flags (`skip_tests`, `skip_engine_tests`, `webapps_only`) consumed by the `build` job, backed by a Python heuristics script in a reusable composite action.

**Architecture:** A composite action at `.github/actions/prepare-build/` wraps a Python stdlib script that fetches changed files via the GitHub REST API and applies three heuristics. `build.sh` gains two new flags (`--skip-engine-tests`, `--webapps-only`) that translate to Maven arguments. The `build` job gains `needs: [lint-adr, prepare]` and consumes the prepare outputs.

**Tech Stack:** Bash (build.sh), Python 3 stdlib only (no third-party packages), GitHub Actions composite actions, Maven Surefire `test.excludes`.

## Global Constraints

- Python script must use only stdlib (`json`, `os`, `sys`, `urllib.request`, `urllib.error`) — no `requests` or other packages
- `--skip-engine-tests` maps to `-Dtest.excludes=org/operaton/bpm/engine` (uses existing Surefire regex in `engine/pom.xml`)
- `--webapps-only` maps to `-pl webapps/assembly -am` (NOT `-pl webapps -am`)
- Heuristics fire on `pull_request` events only; push to `main`/`release/**` always runs full build
- `ubuntu-slim` is a valid self-hosted runner label in this project (see `label-pr.yml`)
- All outputs default to `"false"` when no heuristic fires

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `.devenv/scripts/build/build.sh` | Modify | Add `--skip-engine-tests` and `--webapps-only` flags |
| `engine/pom.xml` | Modify | Remove `skipTests.operaton-engine` property and its surefire `<skip>` reference |
| `.github/actions/prepare-build/action.yml` | Create | Composite action: declares inputs/outputs, runs Python script |
| `.github/actions/prepare-build/prepare_build.py` | Create | Heuristic logic: fetches changed files, writes flags to `$GITHUB_OUTPUT` |
| `.github/actions/prepare-build/test_prepare_build.py` | Create | Unit tests for the three heuristic functions |
| `.github/workflows/build.yml` | Modify | Add `prepare` job; update `build` job `needs` and Maven Build step |
| `.github/copilot-instructions.md` | Modify | Add "Faster Builds" section + cross-reference to `AGENTS.md` |
| `AGENTS.md` | Modify | Add "Faster Builds" section + cross-reference to `copilot-instructions.md` |

---

### Task 1: Add `--skip-engine-tests` and `--webapps-only` to `build.sh`

**Files:**
- Modify: `.devenv/scripts/build/build.sh`

**Interfaces:**
- Produces: `--skip-engine-tests` flag → appends `-Dtest.excludes=org/operaton/bpm/engine` to MVN_ARGS
- Produces: `--webapps-only` flag → appends `-pl webapps/assembly -am` to MVN_ARGS

- [ ] **Step 1: Add variables and cases to `build.sh`**

Add two new variables after line 6 (`SKIP_TESTS="false"`):

```bash
SKIP_ENGINE_TESTS="false"
WEBAPPS_ONLY="false"
```

Add two new cases inside the `parse_args` `case` block, after the `--skip-tests)` case:

```bash
      --skip-engine-tests)
        SKIP_ENGINE_TESTS="true"
        ;;
      --webapps-only)
        WEBAPPS_ONLY="true"
        ;;
```

- [ ] **Step 2: Add Maven arg blocks after the existing `--skip-tests` block**

After the block at line 86-88:
```bash
if ([ "$SKIP_TESTS" = "true" ]); then
  MVN_ARGS+=(-DskipTests)
fi
```

Add:
```bash
if [ "$SKIP_ENGINE_TESTS" = "true" ]; then
  MVN_ARGS+=(-Dtest.excludes=org/operaton/bpm/engine)
fi

if [ "$WEBAPPS_ONLY" = "true" ]; then
  MVN_ARGS+=(-pl webapps/assembly -am)
fi
```

- [ ] **Step 3: Verify the flags appear in the printed command**

From repo root:

```bash
.devenv/scripts/build/build.sh --skip-engine-tests --runner=mvnw 2>&1 | grep "ℹ️"
```

Expected output contains `-Dtest.excludes=org/operaton/bpm/engine`.

```bash
.devenv/scripts/build/build.sh --webapps-only --runner=mvnw 2>&1 | grep "ℹ️"
```

Expected output contains `-pl webapps/assembly -am`.

- [ ] **Step 4: Commit**

```bash
git add .devenv/scripts/build/build.sh
git commit -m "feat(build): add --skip-engine-tests and --webapps-only flags to build.sh"
```

---

### Task 2: Remove `skipTests.operaton-engine` from `engine/pom.xml`

**Files:**
- Modify: `engine/pom.xml`

**Interfaces:**
- Consumes: nothing new
- Produces: engine tests now skipped exclusively via `-Dtest.excludes=org/operaton/bpm/engine`

- [ ] **Step 1: Remove the `skipTests.operaton-engine` property**

In `engine/pom.xml`, remove line 20:

```xml
    <skipTests.operaton-engine>false</skipTests.operaton-engine>
```

The `<properties>` block should now read:
```xml
  <properties>
    <test.includes />
    <!-- without a special test profile we don't want to exclude anything, this expressions should never match -->
    <test.excludes>$.</test.excludes>
    <history.level>full</history.level>
```

- [ ] **Step 2: Replace the surefire `<skip>` reference**

In `engine/pom.xml` at line 459, change:
```xml
          <skip>${skipTests.operaton-engine}</skip>
```
to:
```xml
          <skip>false</skip>
```

- [ ] **Step 3: Verify the engine validate phase still passes**

```bash
./mvnw -pl engine validate --no-transfer-progress -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Verify `test.excludes` skips all engine tests**

```bash
./mvnw -pl engine test --no-transfer-progress -Dtest.excludes=org/operaton/bpm/engine 2>&1 | grep -E "Tests run|BUILD|Skipped"
```

Expected: All engine tests skipped, `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add engine/pom.xml
git commit -m "chore(engine): remove undocumented skipTests.operaton-engine, use test.excludes"
```

---

### Task 3: Python heuristics script + unit tests

**Files:**
- Create: `.github/actions/prepare-build/prepare_build.py`
- Create: `.github/actions/prepare-build/test_prepare_build.py`

**Interfaces:**
- Produces: `check_skip_tests(actor: str, head_ref: str) -> bool`
- Produces: `check_skip_engine_tests(changed_files: list[str]) -> bool`
- Produces: `check_webapps_only(changed_files: list[str]) -> bool`
- Produces: `get_changed_files(token: str, repo: str, pr_number: int) -> list[str]`
- Produces: `main()` — reads env vars, runs heuristics, writes to `$GITHUB_OUTPUT`

- [ ] **Step 1: Write the failing unit tests**

Create `.github/actions/prepare-build/test_prepare_build.py`:

```python
import unittest
import sys
import os

sys.path.insert(0, os.path.dirname(__file__))
from prepare_build import check_skip_tests, check_skip_engine_tests, check_webapps_only


class TestCheckSkipTests(unittest.TestCase):

    def test_dependabot_github_actions(self):
        self.assertTrue(check_skip_tests(
            "dependabot[bot]", "dependabot/github_actions/actions/checkout-4"))

    def test_dependabot_npm_and_yarn(self):
        self.assertTrue(check_skip_tests(
            "dependabot[bot]", "dependabot/npm_and_yarn/webpack-5.99.0"))

    def test_dependabot_maven_not_skipped(self):
        self.assertFalse(check_skip_tests(
            "dependabot[bot]", "dependabot/maven/org.junit.junit-4.14"))

    def test_not_dependabot_actor(self):
        self.assertFalse(check_skip_tests(
            "kthoms", "dependabot/github_actions/actions/checkout-4"))

    def test_non_dependabot_branch(self):
        self.assertFalse(check_skip_tests(
            "dependabot[bot]", "feature/my-feature"))


class TestCheckSkipEngineTests(unittest.TestCase):

    def test_webapps_only_change(self):
        files = ["webapps/assembly/src/main/java/Foo.java",
                 "webapps/frontend/src/app/app.js"]
        self.assertTrue(check_skip_engine_tests(files))

    def test_engine_changed(self):
        files = ["engine/src/main/java/org/operaton/bpm/engine/Foo.java"]
        self.assertFalse(check_skip_engine_tests(files))

    def test_engine_dmn_changed(self):
        files = ["engine-dmn/src/main/java/Foo.java"]
        self.assertFalse(check_skip_engine_tests(files))

    def test_engine_rest_changed(self):
        files = ["engine-rest/src/main/java/Foo.java"]
        self.assertFalse(check_skip_engine_tests(files))

    def test_commons_changed(self):
        files = ["commons/utils/src/main/java/Foo.java"]
        self.assertFalse(check_skip_engine_tests(files))

    def test_model_api_changed(self):
        files = ["model-api/bpmn-model/src/main/java/Foo.java"]
        self.assertFalse(check_skip_engine_tests(files))

    def test_juel_changed(self):
        files = ["juel/src/main/java/Foo.java"]
        self.assertFalse(check_skip_engine_tests(files))

    def test_empty_files_returns_false(self):
        self.assertFalse(check_skip_engine_tests([]))

    def test_mixed_no_engine(self):
        files = ["docs/README.md", "spring-boot-starter/src/main/java/Foo.java"]
        self.assertTrue(check_skip_engine_tests(files))


class TestCheckWebappsOnly(unittest.TestCase):

    def test_all_under_webapps(self):
        files = ["webapps/assembly/src/main/java/Foo.java",
                 "webapps/frontend/src/app/app.js"]
        self.assertTrue(check_webapps_only(files))

    def test_mixed_paths(self):
        files = ["webapps/assembly/src/main/java/Foo.java",
                 "engine/src/main/java/Bar.java"]
        self.assertFalse(check_webapps_only(files))

    def test_empty_files_returns_false(self):
        self.assertFalse(check_webapps_only([]))

    def test_non_webapps_only(self):
        files = ["engine/src/main/java/Bar.java"]
        self.assertFalse(check_webapps_only(files))


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd .github/actions/prepare-build
python3 -m unittest test_prepare_build -v 2>&1 | head -20
```

Expected: `ModuleNotFoundError: No module named 'prepare_build'`

- [ ] **Step 3: Create `prepare_build.py`**

Create `.github/actions/prepare-build/prepare_build.py`:

```python
#!/usr/bin/env python3
"""CI prepare job: analyzes PR context and emits build optimization flags."""
import json
import os
import sys
import urllib.error
import urllib.request

ENGINE_DEPS = (
    "engine/",
    "engine-dmn/",
    "engine-rest/",
    "engine-cdi/",
    "engine-spring/",
    "commons/",
    "model-api/",
    "juel/",
)


def get_changed_files(token, repo, pr_number):
    """Fetch all changed file paths for a PR via GitHub REST API (paginated)."""
    files = []
    page = 1
    while True:
        url = (
            f"https://api.github.com/repos/{repo}/pulls/{pr_number}/files"
            f"?per_page=100&page={page}"
        )
        req = urllib.request.Request(
            url,
            headers={
                "Authorization": f"Bearer {token}",
                "Accept": "application/vnd.github+json",
                "X-GitHub-Api-Version": "2022-11-28",
            },
        )
        try:
            with urllib.request.urlopen(req) as resp:
                data = json.loads(resp.read())
        except urllib.error.HTTPError as exc:
            print(f"Warning: GitHub API error {exc.code}: {exc.reason}", file=sys.stderr)
            return []
        if not data:
            break
        files.extend(f["filename"] for f in data)
        if len(data) < 100:
            break
        page += 1
    return files


def check_skip_tests(actor, head_ref):
    """True if actor is dependabot[bot] and branch is github_actions or npm_and_yarn."""
    if actor != "dependabot[bot]":
        return False
    return head_ref.startswith("dependabot/github_actions") or \
           head_ref.startswith("dependabot/npm_and_yarn")


def check_skip_engine_tests(changed_files):
    """True if no changed file is under engine or its in-reactor dependencies."""
    if not changed_files:
        return False
    return not any(f.startswith(dep) for f in changed_files for dep in ENGINE_DEPS)


def check_webapps_only(changed_files):
    """True if every changed file is under webapps/."""
    if not changed_files:
        return False
    return all(f.startswith("webapps/") for f in changed_files)


def _write_output(key, value, output_file):
    with open(output_file, "a") as fh:
        fh.write(f"{key}={value}\n")


def main():
    event_name = os.environ.get("GITHUB_EVENT_NAME", "")
    actor = os.environ.get("GITHUB_ACTOR", "")
    head_ref = os.environ.get("GITHUB_HEAD_REF", "")
    repo = os.environ.get("GITHUB_REPOSITORY", "")
    token = os.environ.get("GITHUB_TOKEN", "")
    output_file = os.environ.get("GITHUB_OUTPUT", "/dev/stdout")
    event_path = os.environ.get("GITHUB_EVENT_PATH", "")

    skip_tests = "false"
    skip_engine_tests = "false"
    webapps_only = "false"

    if event_name != "pull_request":
        # push or workflow_dispatch: always full build
        print(f"Event '{event_name}' is not pull_request — emitting defaults.")
        _write_output("skip_tests", skip_tests, output_file)
        _write_output("skip_engine_tests", skip_engine_tests, output_file)
        _write_output("webapps_only", webapps_only, output_file)
        return

    # Read PR number from event payload
    pr_number = None
    if event_path and os.path.exists(event_path):
        with open(event_path) as fh:
            event = json.load(fh)
        pr_number = event.get("pull_request", {}).get("number")

    # Heuristic 1: skip_tests (short-circuits remaining heuristics)
    if check_skip_tests(actor, head_ref):
        skip_tests = "true"
        print(f"skip_tests=true — dependabot non-code PR: {head_ref}")
        _write_output("skip_tests", skip_tests, output_file)
        _write_output("skip_engine_tests", skip_engine_tests, output_file)
        _write_output("webapps_only", webapps_only, output_file)
        return

    # Fetch changed files for file-based heuristics
    changed_files = []
    if pr_number and token and repo:
        changed_files = get_changed_files(token, repo, pr_number)
        preview = changed_files[:10]
        suffix = "..." if len(changed_files) > 10 else ""
        print(f"Changed files ({len(changed_files)}): {preview}{suffix}")
    else:
        print("Warning: missing pr_number/token/repo — skipping file-based heuristics.",
              file=sys.stderr)

    # Heuristic 2: skip_engine_tests
    if check_skip_engine_tests(changed_files):
        skip_engine_tests = "true"

    # Heuristic 3: webapps_only
    if check_webapps_only(changed_files):
        webapps_only = "true"

    print(f"skip_engine_tests={skip_engine_tests}, webapps_only={webapps_only}")
    _write_output("skip_tests", skip_tests, output_file)
    _write_output("skip_engine_tests", skip_engine_tests, output_file)
    _write_output("webapps_only", webapps_only, output_file)


if __name__ == "__main__":
    main()
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd .github/actions/prepare-build
python3 -m unittest test_prepare_build -v
```

Expected: all tests pass, `OK` at the end. Example output:
```
test_dependabot_github_actions (test_prepare_build.TestCheckSkipTests) ... ok
test_dependabot_maven_not_skipped (test_prepare_build.TestCheckSkipTests) ... ok
...
----------------------------------------------------------------------
Ran 17 tests in 0.001s

OK
```

- [ ] **Step 5: Commit**

```bash
cd /path/to/repo/root
git add .github/actions/prepare-build/prepare_build.py \
        .github/actions/prepare-build/test_prepare_build.py
git commit -m "feat(ci): add prepare_build.py with PR heuristics and unit tests"
```

---

### Task 4: Composite action definition

**Files:**
- Create: `.github/actions/prepare-build/action.yml`

**Interfaces:**
- Consumes: `prepare_build.py` from Task 3 (same directory, referenced via `${{ github.action_path }}`)
- Produces: composite action with inputs `github-token` and outputs `skip_tests`, `skip_engine_tests`, `webapps_only`

- [ ] **Step 1: Create `action.yml`**

Create `.github/actions/prepare-build/action.yml`:

```yaml
name: Prepare Build
description: Analyzes PR context and emits build optimization flags (skip_tests, skip_engine_tests, webapps_only)

inputs:
  github-token:
    description: GitHub token for REST API access to changed files
    required: true

outputs:
  skip_tests:
    description: '"true" if all tests should be skipped'
    value: ${{ steps.prepare.outputs.skip_tests }}
  skip_engine_tests:
    description: '"true" if engine tests should be skipped'
    value: ${{ steps.prepare.outputs.skip_engine_tests }}
  webapps_only:
    description: '"true" if only webapps/assembly and its deps should be built'
    value: ${{ steps.prepare.outputs.webapps_only }}

runs:
  using: composite
  steps:
    - name: Run prepare script
      id: prepare
      shell: bash
      env:
        GITHUB_TOKEN: ${{ inputs.github-token }}
      run: python3 ${{ github.action_path }}/prepare_build.py
```

- [ ] **Step 2: Validate the YAML is parseable**

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/actions/prepare-build/action.yml')); print('YAML valid')"
```

Expected: `YAML valid`

- [ ] **Step 3: Commit**

```bash
git add .github/actions/prepare-build/action.yml
git commit -m "feat(ci): add prepare-build composite action"
```

---

### Task 5: Wire `prepare` job into `build.yml`

**Files:**
- Modify: `.github/workflows/build.yml`

**Interfaces:**
- Consumes: `.github/actions/prepare-build/action.yml` from Task 4 (via `uses: ./.github/actions/prepare-build`)
- Consumes: `needs.prepare.outputs.{skip_tests,skip_engine_tests,webapps_only}`
- Consumes: `--skip-engine-tests` and `--webapps-only` flags in `build.sh` from Task 1

- [ ] **Step 1: Add the `prepare` job**

In `.github/workflows/build.yml`, insert a new `prepare` job between the `lint-adr` job and the `build` job (after line 94, before `build:`):

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
      - name: Checkout action files
        uses: actions/checkout@v7.0.0
        with:
          sparse-checkout: |
            .github/actions/prepare-build
      - name: Prepare build
        id: prepare
        uses: ./.github/actions/prepare-build
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}

```

- [ ] **Step 2: Add `needs` to the `build` job**

Change the `build` job header from:
```yaml
  build:
    name: Build
    runs-on: ubuntu-24.04
    permissions:
```
to:
```yaml
  build:
    name: Build
    needs: [lint-adr, prepare]
    runs-on: ubuntu-24.04
    permissions:
```

- [ ] **Step 3: Update the Maven Build step to consume prepare outputs**

Replace the current skip-tests block (lines 153–157):
```yaml
          # Add skip-tests flag if enabled
          SKIP_TESTS="${{ github.event.inputs.skip_tests || 'false' }}"
          if [ "$SKIP_TESTS" = "true" ]; then
            BUILD_CMD="$BUILD_CMD --skip-tests"
          fi
```

with:
```yaml
          # Add skip-tests flag (workflow_dispatch input or prepare job heuristic)
          SKIP_TESTS_INPUT="${{ github.event.inputs.skip_tests || 'false' }}"
          SKIP_TESTS_PREPARE="${{ needs.prepare.outputs.skip_tests || 'false' }}"
          if [ "$SKIP_TESTS_INPUT" = "true" ] || [ "$SKIP_TESTS_PREPARE" = "true" ]; then
            BUILD_CMD="$BUILD_CMD --skip-tests"
          fi

          # Add skip-engine-tests flag if set by prepare job
          SKIP_ENGINE_TESTS="${{ needs.prepare.outputs.skip_engine_tests || 'false' }}"
          if [ "$SKIP_ENGINE_TESTS" = "true" ]; then
            BUILD_CMD="$BUILD_CMD --skip-engine-tests"
          fi

          # Add webapps-only flag if set by prepare job
          WEBAPPS_ONLY="${{ needs.prepare.outputs.webapps_only || 'false' }}"
          if [ "$WEBAPPS_ONLY" = "true" ]; then
            BUILD_CMD="$BUILD_CMD --webapps-only"
          fi
```

- [ ] **Step 4: Validate the workflow YAML**

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/build.yml')); print('YAML valid')"
```

Expected: `YAML valid`

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "feat(ci): add prepare job to build.yml for build optimization heuristics"
```

---

### Task 6: Add "Faster Builds" documentation to AI instruction files

**Files:**
- Modify: `.github/copilot-instructions.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: documented flags in both files; mutual cross-references

- [ ] **Step 1: Add "Faster Builds" section to `copilot-instructions.md`**

In `.github/copilot-instructions.md`, insert after the `#### Core Build Commands` block (after the `./mvnw clean` line), before `#### Critical Build Order Dependencies`:

```markdown
#### Faster Builds

Use `.devenv/scripts/build/build.sh` flags for targeted builds:

| Flag | Maven effect | When the CI uses it |
|------|-------------|---------------------|
| `--skip-tests` | `-DskipTests` | dependabot GitHub Actions / npm PRs |
| `--skip-engine-tests` | `-Dtest.excludes=org/operaton/bpm/engine` | No changes in engine or its deps |
| `--webapps-only` | `-pl webapps/assembly -am` | All changes within `webapps/` |

> **Important:** `-pl webapps -am` is NOT sufficient (resolves only the aggregator POM).
> Always use `-pl webapps/assembly -am`, which builds the 29-module chain including engine and engine-rest.

The CI `prepare` job (`.github/actions/prepare-build/`) sets these flags automatically
based on PR branch name, actor, and changed files.

> See also `AGENTS.md` in the repository root for comprehensive build guidelines.
```

- [ ] **Step 2: Add "Faster Builds" section to `AGENTS.md`**

In `AGENTS.md`, insert after the `### Core Build Commands` block (after the `./mvnw clean` line), before `### Critical Build Dependencies`:

```markdown
### Faster Builds

Use `.devenv/scripts/build/build.sh` flags for targeted builds:

| Flag | Maven effect | When the CI uses it |
|------|-------------|---------------------|
| `--skip-tests` | `-DskipTests` | dependabot GitHub Actions / npm PRs |
| `--skip-engine-tests` | `-Dtest.excludes=org/operaton/bpm/engine` | No changes in engine or its deps |
| `--webapps-only` | `-pl webapps/assembly -am` | All changes within `webapps/` |

> **Important:** `-pl webapps -am` is NOT sufficient (resolves only the aggregator POM).
> Always use `-pl webapps/assembly -am`, which builds the 29-module chain including engine and engine-rest.

The CI `prepare` job (`.github/actions/prepare-build/`) sets these flags automatically
based on PR branch name, actor, and changed files.

> See also `.github/copilot-instructions.md` for GitHub Copilot-specific guidelines.
```

- [ ] **Step 3: Verify both files parse as valid Markdown**

```bash
python3 -c "
data = open('.github/copilot-instructions.md').read()
assert '### Faster Builds' in data or '#### Faster Builds' in data, 'Section missing from copilot-instructions.md'
data = open('AGENTS.md').read()
assert '### Faster Builds' in data, 'Section missing from AGENTS.md'
print('Both files contain Faster Builds section')
"
```

Expected: `Both files contain Faster Builds section`

- [ ] **Step 4: Commit**

```bash
git add .github/copilot-instructions.md AGENTS.md
git commit -m "docs: add Faster Builds section to AI instruction files"
```
