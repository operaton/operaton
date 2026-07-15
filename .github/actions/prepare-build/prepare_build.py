#!/usr/bin/env python3
"""CI prepare job: analyzes PR context and emits build optimization flags."""
import json
import os
import sys
import urllib.error
import urllib.request

ENGINE_DEPS = (
    "pom.xml",
    "parent/",
    "bom/",
    "engine/",
    "engine-dmn/",
    "engine-rest/",
    "engine-cdi/",
    "engine-spring/",
    "commons/",
    "model-api/",
    "juel/",
    "database/",
    "test-utils/",
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
        except urllib.error.URLError as exc:
            print(f"Warning: GitHub API connection error: {exc.reason}", file=sys.stderr)
            return []
        except (json.JSONDecodeError, Exception) as exc:
            print(f"Warning: unexpected error fetching changed files: {exc}", file=sys.stderr)
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
