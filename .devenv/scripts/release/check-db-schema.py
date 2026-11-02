#!/usr/bin/env python3
"""Report database schema changes for an upcoming release.

Compares a release branch (or any ref) with the previous final release: the highest vX.Y.Z
tag below the version being released (milestone and RC tags are ignored, the
same rule as .github/scripts/prepare-deploy.sh). Prints a markdown report:

  - schema version (`operaton.dbscheme.current.version` in database/pom.xml)
    at the previous release and on the branch
  - changed files in the engine db directory
  - whether the change is version-only (upgrade scripts that only insert an
    ACT_GE_SCHEMA_LOG row) or structural
  - the commits that touched the db directory, with their PRs and the issues
    those PRs close
  - a verdict for the release type

READ-ONLY: queries GitHub through `gh`, works in shallow clones.

Usage:
  check-db-schema.py --branch release/2.2.x --version 2.2.1 [--repo OWNER/REPO]

Exit codes: 0 no schema change, 2 schema changed (see verdict), 1 error.
"""
import argparse
import base64
import difflib
import json
import re
import subprocess
import sys

DB_DIR = "engine/src/main/resources/org/operaton/bpm/engine/db/"
UPGRADE_RE = re.compile(r"upgrade/(\w+)_engine_([\d.]+)_to_([\d.]+)\.sql$")
SCHEMA_LOG_INSERT = re.compile(
    r"^insert into ACT_GE_SCHEMA_LOG\s+values\s*\([^)]*\);?$", re.IGNORECASE)


def gh(*args):
    out = subprocess.run(["gh", "api", *args], check=True, capture_output=True, text=True)
    return json.loads(out.stdout) if out.stdout.strip() else None


def gh_paginated(path):
    out = subprocess.run(["gh", "api", "--paginate", "--slurp", path], check=True,
                         capture_output=True, text=True).stdout
    return [item for page in json.loads(out) for item in page] if out.strip() else []


def version_tuple(v):
    return tuple(int(x) for x in v.split("."))


def previous_final_tag(repo, version):
    base = version.split("-")[0]
    finals = [t["name"] for t in gh_paginated(f"repos/{repo}/tags?per_page=100")
              if re.fullmatch(r"v\d+\.\d+\.\d+", t["name"])]
    lower = [t for t in finals if version_tuple(t[1:]) < version_tuple(base)]
    return max(lower, key=lambda t: version_tuple(t[1:])) if lower else None


def file_at(repo, path, ref):
    data = gh(f"repos/{repo}/contents/{path}?ref={ref}")
    return base64.b64decode(data["content"]).decode()


def schema_version(repo, ref):
    m = re.search(r"<operaton.dbscheme.current.version>([^<]+)<", file_at(repo, "database/pom.xml", ref))
    return m.group(1) if m else "?"


def db_tree(repo, ref, sub=""):
    """Map of path (relative to DB_DIR) -> blob sha at ref."""
    result = {}
    # no trailing slash: GitHub ignores ?ref= on ".../dir/" and serves the default branch
    for e in gh(f"repos/{repo}/contents/{(DB_DIR + sub).rstrip('/')}?ref={ref}"):
        if e["type"] == "dir":
            result.update(db_tree(repo, ref, sub + e["name"] + "/"))
        elif e["type"] == "file":
            result[sub + e["name"]] = e["sha"]
    return result


def sql_statements(text):
    lines = [l.strip() for l in text.splitlines()]
    body = " ".join(l for l in lines if l and not l.startswith("--"))
    return [s.strip() + ";" for s in body.split(";") if s.strip()]


def pulls_for_commit(repo, sha):
    result = []
    for pr in gh(f"repos/{repo}/commits/{sha}/pulls") or []:
        issues = subprocess.run(
            ["gh", "pr", "view", str(pr["number"]), "--repo", repo, "--json", "closingIssuesReferences",
             "-q", "[.closingIssuesReferences[].number]"], capture_output=True, text=True).stdout.strip()
        result.append((pr["number"], pr["title"], pr["html_url"], json.loads(issues or "[]")))
    return result


def main():
    p = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    p.add_argument("--branch", required=True, help="release branch, e.g. release/2.2.x or main")
    p.add_argument("--version", required=True, help="version to release, e.g. 2.2.1 or 2.3.0-M1")
    p.add_argument("--repo", default="operaton/operaton")
    a = p.parse_args()

    prev = previous_final_tag(a.repo, a.version)
    if not prev:
        print(f"No final release tag below {a.version}; nothing to compare.")
        return 0

    old_v, new_v = schema_version(a.repo, prev), schema_version(a.repo, a.branch)
    old_tree, new_tree = db_tree(a.repo, prev), db_tree(a.repo, a.branch)
    files = []
    for path in sorted(old_tree.keys() | new_tree.keys()):
        if old_tree.get(path) != new_tree.get(path):
            status = "added" if path not in old_tree else "removed" if path not in new_tree else "modified"
            files.append((path, status))

    print(f"## Database schema: {a.branch} ({a.version}) vs {prev}\n")
    print(f"- Schema version: `{old_v}` at {prev} → `{new_v}` on {a.branch}")
    if not files and old_v == new_v:
        print("- No changes in the db directory.\n\n**Verdict:** no schema change; "
              "release notes keep \"no changes to the database schema\".")
        return 0

    structural = []
    for name, status in files:
        path = DB_DIR + name
        if UPGRADE_RE.search(name) and status == "added":
            stmts = sql_statements(file_at(a.repo, path, a.branch))
            if not all(SCHEMA_LOG_INSERT.match(st) for st in stmts):
                structural.append(name)
        elif name.startswith(("create/", "drop/")) and status == "modified":
            diff = difflib.unified_diff(file_at(a.repo, path, prev).splitlines(),
                                        file_at(a.repo, path, a.branch).splitlines(), n=0, lineterm="")
            changed = [l[1:].strip() for l in diff
                       if l[:1] in "+-" and not l.startswith(("+++", "---")) and l[1:].strip()]
            if not all(l.lower().startswith("values (") for l in changed):
                structural.append(name)
        elif not name.startswith("liquibase/"):
            structural.append(name)

    print(f"- Changed files in `{DB_DIR}` ({len(files)}):")
    for name, status in files:
        print(f"  - `{name}` ({status})")
    kind = "structural" if structural else "version-only (upgrade scripts only add an ACT_GE_SCHEMA_LOG row)"
    print(f"- Kind: **{kind}**")
    for s in structural:
        print(f"  - structural change in `{s}`")

    print("\n### Commits that touched the db directory\n")
    # commits on the branch that touched the db dir and are not in the previous release.
    # Backports carry a different SHA and are matched by subject.
    path_q = f"path={DB_DIR.rstrip('/')}&per_page=100"
    prev_commits = gh_paginated(f"repos/{a.repo}/commits?sha={prev}&{path_q}")
    prev_shas = {c["sha"] for c in prev_commits}
    prev_subjects = {c["commit"]["message"].splitlines()[0] for c in prev_commits}
    commits = [c for c in gh_paginated(f"repos/{a.repo}/commits?sha={a.branch.replace('/', '%2F')}&{path_q}")
               if c["sha"] not in prev_shas and c["commit"]["message"].splitlines()[0] not in prev_subjects]
    for c in commits:
        sha, subject = c["sha"], c["commit"]["message"].splitlines()[0]
        print(f"- [{sha[:10]}]({c['html_url']}) {subject}")
        for num, title, url, issues in pulls_for_commit(a.repo, sha):
            closes = ", ".join(f"[#{i}](https://github.com/{a.repo}/issues/{i})" for i in issues)
            print(f"  - PR [#{num}]({url}) {title}" + (f" — closes {closes}" if closes else ""))

    patch = int(a.version.split("-")[0].split(".")[2]) > 0
    print("\n**Verdict:** ", end="")
    if patch:
        print(f"🔴 BLOCKER — patch release {a.version} changes the database schema; "
              f"release branches take no schema changes.")
    else:
        print(f"schema {old_v} → {new_v} ({'structural' if structural else 'version-only'}). "
              f"The release notes' *Database Schema* section must state this, name the upgrade scripts, "
              f"and link the PRs/issues above. A final release deploys `operaton-sql-scripts` {new_v} "
              f"to Maven Central; check it is not there yet.")
    return 2


if __name__ == "__main__":
    try:
        sys.exit(main())
    except subprocess.CalledProcessError as e:
        sys.exit(f"ERROR: {' '.join(e.cmd)}: {e.stderr.strip()}")
