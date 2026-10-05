#!/usr/bin/env python3
"""CI prepare job: analyzes changed files and emits build optimization outputs.

Outputs (GITHUB_OUTPUT key=value lines):
  skip_tests        "true" if no tests need to run (dependabot non-code / docs-only PR)
  skip_engine_tests "true" if the heavy engine suite can be excluded from a full build
  changed_modules   comma-separated module dirs for a narrowed -pl build ("" = full build)

Principle: in doubt, execute more. Every uncertain case degrades to a full build.
"""
import argparse
import functools
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from collections import namedtuple
from pathlib import Path

# Changes here can affect anything; profile-gated qa/ modules are unsafe for -pl.
FULL_BUILD_PREFIXES = (
    "parent/", "bom/", "database/", "qa/", ".github/", ".devenv/", ".mvn/",
)

# Modules (outside the core-api set) with test classes in org.operaton.bpm.engine.*
# packages: -Dtest.excludes=org/operaton/bpm/engine would wrongly skip their tests.
ENGINE_PACKAGE_TEST_PREFIXES = (
    "engine-cdi/", "engine-spring/", "engine-rest/", "quarkus-extension/", "test-utils/",
)

# Build-wide inputs (Maven version and config, parent poms, BOMs) can change how
# the engine is built and tested without touching an engine module: never skip
# engine tests when one of them changes.
NEVER_SKIP_ENGINE_PREFIXES = ("parent/", "bom/", ".mvn/")

ENGINE_ARTIFACT_ID = "operaton-engine"
ENGINE_TEST_EXCLUDES = "org/operaton/bpm/engine"

DOCS_BASENAMES = ("LICENSE", "NOTICE", "CONTRIBUTORS")

# Files that cannot change what the PR build compiles or tests. They are
# dropped before classification, so e.g. an AGENTS.md edit next to an engine
# fix no longer escalates the PR to a full build.
NON_BUILD_PATTERNS = tuple(re.compile(p) for p in (
    r"(?!.*/src/).*\.md",             # docs, except packaged resources
    r"\.claude/.*",
    r"\.devcontainer/.*",
    r"\.sonarlint/.*",
    r"docs/.*",
    r"\.gitignore",
    r"jreleaser\.yml",
    r"\.github/workflows/(?!pr-build\.yml$).*",
    r"\.github/(zizmor\.yml|dependabot\.yml|auto-merge\.config\.yml)",
    r"\.github/(labels|ISSUE_TEMPLATE|PULL_REQUEST_TEMPLATE|jreleaser)/.*",
    r"\.devenv/scripts/(maintenance|release|smoketest|tools)/.*",
))

# Matches one added or removed `<name>value</name>` line of a unified diff.
PROPERTY_LINE = re.compile(r"^[+-]\s*<([\w.-]+)>[^<]*</\1>\s*$")

# Text files scanned for property references (resource filtering).
PROPERTY_SCAN_SKIP_DIRS = {".git", "node_modules", "target", ".idea"}
# This script and its tests mention property names as sample data.
PROPERTY_SCAN_SKIP_PREFIXES = (".github/actions/prepare-build/",)
PROPERTY_SCAN_MAX_BYTES = 1_000_000

SKIP_DIRS = {".git", "node_modules", "target", "src", ".idea"}

Classification = namedtuple("Classification", "full_build docs_only changed_modules")


def get_changed_files(token, repo, pr_number):
    """Fetch all changed file paths for a PR via GitHub REST API (paginated)."""
    return list(get_changed_files_with_patches(token, repo, pr_number))


def get_changed_files_with_patches(token, repo, pr_number):
    """{path: unified diff patch or None} for a PR. GitHub omits the patch
    for large diffs; callers must treat None as "unknown change"."""
    files = {}
    page = 1
    while True:
        url = (
            f"https://api.github.com/repos/{repo}/pulls/{pr_number}/files"
            f"?per_page=100&page={page}"
        )
        try:
            data = _github_api(token, url)
        except Exception as exc:
            print(f"Warning: error fetching changed files: {exc}", file=sys.stderr)
            return {}
        if not data:
            break
        files.update((f["filename"], f.get("patch")) for f in data)
        if len(data) < 100:
            break
        page += 1
    return files


def _github_api(token, url):
    req = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def git_toplevel():
    return subprocess.run(["git", "rev-parse", "--show-toplevel"],
                          capture_output=True, text=True, check=True).stdout.strip()


def get_changed_files_from_git(ref, repo_root):
    """Changed files vs merge-base(ref, HEAD), incl. uncommitted and untracked."""
    def git(*args):
        return subprocess.run(["git", *args], cwd=repo_root,
                              capture_output=True, text=True, check=True).stdout
    base = git("merge-base", ref, "HEAD").strip()
    diff = git("diff", "--name-only", base).split()
    untracked = git("ls-files", "--others", "--exclude-standard").split()
    return sorted(set(diff) | set(untracked))


def get_patches_from_git(ref, repo_root, files):
    """{pom path: patch} vs merge-base(ref, HEAD), for property-change analysis."""
    base = subprocess.run(["git", "merge-base", ref, "HEAD"], cwd=repo_root,
                          capture_output=True, text=True, check=True).stdout.strip()
    patches = {}
    for f in files:
        if f.rsplit("/", 1)[-1] == "pom.xml":
            patches[f] = subprocess.run(["git", "diff", base, "--", f], cwd=repo_root,
                                        capture_output=True, text=True).stdout
    return patches


def is_non_build_file(path):
    """True if the file cannot affect the PR build's outputs or tests."""
    return any(p.fullmatch(path) for p in NON_BUILD_PATTERNS)


def check_skip_tests(actor, head_ref):
    """True if actor is dependabot[bot] and branch is github_actions or npm_and_yarn."""
    if actor != "dependabot[bot]":
        return False
    return head_ref.startswith("dependabot/github_actions") or \
           head_ref.startswith("dependabot/npm_and_yarn")


def check_docs_only(changed_files):
    """True if every changed file is documentation (cannot affect build outputs)."""
    if not changed_files:
        return False
    def is_docs(f):
        name = f.rsplit("/", 1)[-1]
        if f.endswith(".md") or name.startswith(DOCS_BASENAMES):
            return True
        return "/" not in f and f.endswith(".txt")
    return all(is_docs(f) for f in changed_files)


def discover_modules(root):
    """All directories (relative to root) containing a pom.xml, excluding root."""
    root = Path(root)
    modules = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        if "pom.xml" in filenames:
            rel = Path(dirpath).relative_to(root).as_posix()
            if rel != ".":
                modules.append(rel)
    return sorted(modules)


def discover_test_jar_producers(root):
    """Modules that bind maven-jar-plugin's test-jar goal, i.e. whose test
    sources are a real compile-time dependency of other modules. With
    -Dmaven.test.skip=true these modules would install an empty test-jar,
    silently breaking test-compile of whoever consumes it."""
    root = Path(root)
    producers = []
    for module in discover_modules(root):
        try:
            proj = ET.parse(root / module / "pom.xml").getroot()
        except ET.ParseError:
            continue
        goals = proj.findall(".//{*}plugin/{*}executions/{*}execution/{*}goals/{*}goal")
        if any(g.text == "test-jar" for g in goals):
            producers.append(module)
    return sorted(producers)


def relevant_test_jar_producers(producers, changed_modules, graph):
    """Narrow producers to ones whose real test-jar actually matters for the
    current build: excludes producers with no consumer in the affected
    closure (nothing needs their test-jar), and producers that are
    themselves in the closure (phase 3 rebuilds them for real anyway, so no
    preliminary pass is needed for those)."""
    closure = set(changed_modules) | compute_downstream(graph, changed_modules)
    return [p for p in producers
            if p not in closure and compute_downstream(graph, [p]) & closure]


def map_file_to_module(path, module_dirs):
    """Deepest module directory containing the file, or None."""
    module_set = set(module_dirs)
    parent = path.rsplit("/", 1)[0] if "/" in path else None
    while parent:
        if parent in module_set:
            return parent
        parent = parent.rsplit("/", 1)[0] if "/" in parent else None
    return None


def _parse_pom(root, module):
    try:
        return ET.parse(Path(root) / module / "pom.xml").getroot()
    except (ET.ParseError, OSError):
        return None


def is_leaf_module(root, module):
    """True for a module that aggregates no sub-modules, in any profile."""
    proj = _parse_pom(root, module)
    return proj is not None and not proj.findall(".//{*}modules")


def changed_properties(patch):
    """Names of the properties whose value a pom patch changes, or None when
    the patch changes anything else (or is unknown)."""
    if not patch:
        return None
    added, removed = set(), set()
    for line in patch.splitlines():
        if not line or line[0] not in "+-" or line.startswith(("+++", "---")):
            continue
        if not line[1:].strip():
            continue
        m = PROPERTY_LINE.match(line)
        if not m:
            return None
        (added if line[0] == "+" else removed).add(m.group(1))
    if not added or added != removed:
        return None
    return added


def _list_files(root):
    """Tracked files (relative paths); falls back to a directory walk."""
    try:
        out = subprocess.run(["git", "ls-files", "-z"], cwd=root,
                             capture_output=True, text=True, check=True).stdout
        return [f for f in out.split("\0") if f]
    except (OSError, subprocess.CalledProcessError):
        files = []
        for dirpath, dirnames, filenames in os.walk(root):
            dirnames[:] = [d for d in dirnames if d not in PROPERTY_SCAN_SKIP_DIRS]
            files += [(Path(dirpath) / n).relative_to(root).as_posix() for n in filenames]
        return files


@functools.lru_cache(maxsize=4)
def _text_files(root):
    """[(path, text)] of the repo's text files, read once per root."""
    result = []
    for rel in _list_files(root):
        path = Path(root) / rel
        try:
            if path.stat().st_size > PROPERTY_SCAN_MAX_BYTES:
                continue
            result.append((rel, path.read_text(encoding="utf-8")))
        except (OSError, UnicodeDecodeError):
            continue
    return tuple(result)


def _plugin_users(root, module_dirs, artifact_id):
    """Modules that bind the plugin (build/plugins, not pluginManagement)."""
    users = set()
    for module in module_dirs:
        proj = _parse_pom(root, module)
        if proj is None:
            continue
        bound = proj.findall("{*}build/{*}plugins/{*}plugin") + proj.findall(
            "{*}profiles/{*}profile/{*}build/{*}plugins/{*}plugin")
        for plugin in bound:
            aid = plugin.find("{*}artifactId")
            if aid is not None and aid.text == artifact_id:
                users.add(module)
    return users


def _pom_usages(root, module, prop):
    """[(in_plugin_management, plugin artifactId or None)] for each element
    of module's pom that references ${prop}."""
    proj = _parse_pom(root, module)
    if proj is None:
        return None
    parents = {child: parent for parent in proj.iter() for child in parent}
    ref = "${" + prop + "}"
    usages = []
    for el in proj.iter():
        if ref not in (el.text or ""):
            continue
        parent = parents.get(el)
        if parent is not None and parent.tag.rsplit("}", 1)[-1] == "properties":
            continue  # defines another property; followed by the caller
        plugin, in_mgmt, node = None, False, el
        while node is not None:
            tag = node.tag.rsplit("}", 1)[-1]
            if tag == "plugin" and plugin is None:
                aid = node.find("{*}artifactId")
                plugin = aid.text if aid is not None else ""
            if tag == "pluginManagement":
                in_mgmt = True
            node = parents.get(node)
        usages.append((in_mgmt, plugin))
    return usages


def modules_affected_by_properties(root, props, module_dirs):
    """Modules whose build a value change of props can affect, or None when
    that cannot be narrowed down safely.

    A property referenced in a pom affects that module (and, via -amd, its
    children and dependents). A property referenced only in a pluginManagement
    plugin configuration affects the modules that bind that plugin. A
    reference in any other file (resource filtering) affects the module that
    contains it."""
    pom_modules = set(module_dirs) | {""}
    files = _text_files(str(root))
    affected = set()
    todo, seen = list(props), set()
    while todo:
        prop = todo.pop()
        if prop in seen:
            continue
        seen.add(prop)
        refs = ("${" + prop + "}", "@" + prop + "@")
        for path, text in files:
            if is_non_build_file(path) or path.startswith(PROPERTY_SCAN_SKIP_PREFIXES) \
                    or not any(r in text for r in refs):
                continue
            module = path.rsplit("/", 1)[0] if "/" in path else ""
            if path.rsplit("/", 1)[-1] != "pom.xml" or module not in pom_modules:
                module = map_file_to_module(path, module_dirs)
                if module is None:
                    return None
                affected.add(module)
                continue
            # "" is the root pom: a non-plugin usage there affects everything,
            # which classify_changes turns into a full build
            usages = _pom_usages(root, module, prop)
            if usages is None:
                return None
            for in_mgmt, plugin in usages:
                if in_mgmt and plugin:
                    affected |= _plugin_users(root, module_dirs, plugin)
                else:
                    affected.add(module)
            # a property defined from this one (<a>${prop}</a>) propagates
            proj = _parse_pom(root, module)
            for props_el in proj.findall(".//{*}properties"):
                for p in props_el:
                    if "${" + prop + "}" in (p.text or ""):
                        todo.append(p.tag.rsplit("}", 1)[-1])
    return affected


def _classify_pom_change(path, root, module_dirs, patches):
    """Modules affected by a pom.xml change, or None for a full build."""
    if root is None:
        return None
    module = path.rsplit("/", 1)[0] if "/" in path else ""
    props = changed_properties((patches or {}).get(path))
    if props is not None:
        affected = modules_affected_by_properties(root, props, module_dirs)
        if affected is not None:
            return affected
    # leaf module outside the build-wide prefixes: narrow to the module
    if module in module_dirs and not path.startswith(FULL_BUILD_PREFIXES) \
            and is_leaf_module(root, module):
        return {module}
    return None


def classify_changes(changed_files, module_dirs, root=None, patches=None):
    """Decide between docs-only, narrowed module build, and full build.

    root enables the pom.xml rules (leaf module poms, property-only version
    bumps); patches maps changed pom paths to their unified diff."""
    if not changed_files:
        return Classification(full_build=True, docs_only=False, changed_modules=[])
    if check_docs_only(changed_files):
        return Classification(full_build=False, docs_only=True, changed_modules=[])
    build_files = [f for f in changed_files if not is_non_build_file(f)]
    if not build_files:
        return Classification(full_build=False, docs_only=True, changed_modules=[])
    modules = set()
    for f in build_files:
        name = f.rsplit("/", 1)[-1]
        if name == "pom.xml":
            affected = _classify_pom_change(f, root, module_dirs, patches)
            if affected is None:
                return Classification(True, False, [])
            modules |= affected
            continue
        if f.startswith(FULL_BUILD_PREFIXES) or "/" not in f:
            return Classification(True, False, [])
        module = map_file_to_module(f, module_dirs)
        if module is None:
            return Classification(True, False, [])
        modules.add(module)
    if any(m == "" or (m + "/").startswith(FULL_BUILD_PREFIXES) for m in modules):
        return Classification(True, False, [])
    return Classification(False, False, sorted(modules))


def _pom_coords(el, default_group=None):
    def txt(parent, tag):
        e = parent.find("{*}" + tag)
        return e.text.strip() if e is not None and e.text else None
    gid = txt(el, "groupId") or default_group
    if gid == "${project.groupId}":
        gid = default_group
    return gid, txt(el, "artifactId")


def _is_bom_import(dep):
    scope = dep.find("{*}scope")
    return scope is not None and scope.text and scope.text.strip() == "import"


def build_module_graph(root):
    """Parse poms: {module_dir: set(module_dirs it depends on)}
    (deps, parent and BOMs imported in <dependencyManagement>)."""
    root = Path(root)
    poms = {}  # module_dir -> parsed data
    for module in discover_modules(root):
        try:
            tree = ET.parse(root / module / "pom.xml")
        except ET.ParseError as exc:
            print(f"Warning: unparseable pom {module}/pom.xml: {exc}", file=sys.stderr)
            continue
        proj = tree.getroot()
        parent_el = proj.find("{*}parent")
        parent_coords = _pom_coords(parent_el) if parent_el is not None else None
        default_group = parent_coords[0] if parent_coords else None
        coords = _pom_coords(proj, default_group)
        deps = [_pom_coords(d, coords[0])
                for d in proj.findall("{*}dependencies/{*}dependency")]
        deps += [_pom_coords(d, coords[0])
                 for d in proj.findall("{*}dependencyManagement/{*}dependencies/{*}dependency")
                 if _is_bom_import(d)]
        if parent_coords:
            deps.append(parent_coords)
        poms[module] = (coords, deps)
    coord_to_dir = {coords: module for module, (coords, _) in poms.items()}
    return {module: {coord_to_dir[d] for d in deps if d in coord_to_dir}
            for module, (_, deps) in poms.items()}


def compute_core_api(graph):
    """Engine module + its transitive in-repo upstream (deps and parent poms)."""
    if "engine" not in graph:
        # engine module missing from checkout: no basis to skip anything
        return set(graph)
    core, todo = set(), ["engine"]
    while todo:
        m = todo.pop()
        if m in core:
            continue
        core.add(m)
        todo.extend(graph.get(m, ()))
    return core


def compute_downstream(graph, seed_modules):
    """Transitive dependents of seed_modules (mirrors `mvn -amd`), seeds excluded."""
    reverse = {}
    for module, deps in graph.items():
        for dep in deps:
            reverse.setdefault(dep, set()).add(module)
    down, todo = set(), list(seed_modules)
    while todo:
        m = todo.pop()
        for dependent in reverse.get(m, ()):
            if dependent not in down:
                down.add(dependent)
                todo.append(dependent)
    return down


# The modules whose build actually produces the npm-built frontend content
# (gated by skip.frontend.build): webapps/assembly and distro/webjar for the
# legacy webapps, webapps-neo (root pom builds webapps-neo/frontend) and
# distro/webjar-neo for the new webapps. Anything that depends on one of them,
# directly or transitively, can observe whether that content is real or
# missing — e.g. spring-boot-starter/starter-webapp boots a real server and
# asserts an admin page returns 200, which 404s without the real npm build.
LEGACY_FRONTEND_PRODUCER_MODULES = ("webapps/assembly", "distro/webjar")
NEO_FRONTEND_PRODUCER_MODULES = ("webapps-neo", "distro/webjar-neo")
FRONTEND_PRODUCER_MODULES = LEGACY_FRONTEND_PRODUCER_MODULES + NEO_FRONTEND_PRODUCER_MODULES


def check_needs_real_frontend(changed_modules, graph):
    """True if changed_modules or their transitive dependents could observe
    the actual built frontend content."""
    closure = set(changed_modules) | compute_downstream(graph, changed_modules)
    frontend_sensitive = set(FRONTEND_PRODUCER_MODULES) | compute_downstream(
        graph, FRONTEND_PRODUCER_MODULES)
    return not closure.isdisjoint(frontend_sensitive)


def check_skip_engine_tests(changed_files, core_api_modules):
    """True if no change can affect engine behavior or engine-packaged tests."""
    changed_files = [f for f in changed_files if not is_non_build_file(f)]
    if not changed_files:
        return False
    guarded = (tuple(m + "/" for m in core_api_modules) + ENGINE_PACKAGE_TEST_PREFIXES
               + NEVER_SKIP_ENGINE_PREFIXES)
    for f in changed_files:
        if f == "pom.xml" or f.startswith(guarded):
            return False
    return True


def _write_output(key, value, output_file):
    with open(output_file, "a") as fh:
        fh.write(f"{key}={value}\n")


def decide(changed_files, repo_root, patches=None):
    """Full decision from a changed-file list: (skip_tests, skip_engine_tests, changed_modules).
    patches ({pom path: unified diff}) enables the property-bump analysis."""
    module_dirs = discover_modules(repo_root)
    if not module_dirs:
        print("Warning: no pom.xml files found — full build.", file=sys.stderr)
        return "false", "false", ""
    c = classify_changes(changed_files, module_dirs, repo_root, patches)
    if c.docs_only:
        return "true", "false", ""
    if not c.full_build:
        return "false", "false", ",".join(c.changed_modules)
    core = compute_core_api(build_module_graph(repo_root))
    skip_engine = check_skip_engine_tests(changed_files, core)
    return "false", "true" if skip_engine else "false", ""


def analyze_recent(token, repo, count, repo_root):
    """Replay the classification over the last N merged PRs and print decisions."""
    prs, page = [], 1
    while len(prs) < count:
        url = (f"https://api.github.com/repos/{repo}/pulls"
               f"?state=closed&sort=updated&direction=desc&per_page=100&page={page}")
        data = _github_api(token, url)
        if not data:
            break
        prs.extend(p for p in data if p.get("merged_at"))
        page += 1
    shortcuts = 0
    for pr in prs[:count]:
        patches = get_changed_files_with_patches(token, repo, pr["number"])
        files = list(patches)
        skip_tests, skip_engine, modules = decide(files, repo_root, patches)
        if skip_tests == "true":
            verdict = "skip all tests"
        elif modules:
            verdict = f"narrowed build: -pl {modules}"
        elif skip_engine == "true":
            verdict = "full build without engine tests"
        else:
            verdict = "full build"
        if verdict != "full build":
            shortcuts += 1
        print(f"#{pr['number']:>5} {len(files):>3} files  {verdict}  [{pr['title'][:60]}]")
    print(f"\n{shortcuts}/{min(count, len(prs))} PRs would have taken a shortcut.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--diff-ref", metavar="REF",
                        help="local mode: classify git diff vs merge-base(REF, HEAD)")
    parser.add_argument("--analyze-recent", type=int, metavar="N",
                        help="replay classification over the last N merged PRs")
    parser.add_argument("--list-test-jar-producers", nargs="?", const="",
                        metavar="CHANGED_MODULES",
                        help="print comma-separated modules whose test-jar is a "
                             "real compile-time dependency elsewhere. With a "
                             "comma-separated CHANGED_MODULES value, narrows to "
                             "producers actually relevant to that build's closure")
    parser.add_argument("--needs-real-frontend", metavar="MODULES",
                        help="print true/false: do MODULES (comma-separated) or "
                             "their dependents need the real built webapp frontend")
    args = parser.parse_args()

    repo = os.environ.get("GITHUB_REPOSITORY", "operaton/operaton")
    token = os.environ.get("GITHUB_TOKEN", "")
    output_file = os.environ.get("GITHUB_OUTPUT", "/dev/stdout")

    if args.list_test_jar_producers is not None:
        repo_root = git_toplevel()
        producers = discover_test_jar_producers(repo_root)
        changed = [m for m in args.list_test_jar_producers.split(",") if m]
        if changed:
            producers = relevant_test_jar_producers(
                producers, changed, build_module_graph(repo_root))
        print(",".join(producers))
        return

    if args.needs_real_frontend is not None:
        repo_root = git_toplevel()
        modules = [m for m in args.needs_real_frontend.split(",") if m]
        graph = build_module_graph(repo_root)
        print("true" if check_needs_real_frontend(modules, graph) else "false")
        return

    if args.analyze_recent:
        analyze_recent(token, repo, args.analyze_recent, os.getcwd())
        return

    if args.diff_ref:
        repo_root = git_toplevel()
        changed_files = get_changed_files_from_git(args.diff_ref, repo_root)
        print(f"Changed files vs {args.diff_ref} ({len(changed_files)}): "
              f"{changed_files[:10]}{'...' if len(changed_files) > 10 else ''}",
              file=sys.stderr)
        patches = get_patches_from_git(args.diff_ref, repo_root, changed_files)
        skip_tests, skip_engine, modules = decide(changed_files, repo_root, patches)
        _write_output("skip_tests", skip_tests, output_file)
        _write_output("skip_engine_tests", skip_engine, output_file)
        _write_output("changed_modules", modules, output_file)
        return

    event_name = os.environ.get("GITHUB_EVENT_NAME", "")
    actor = os.environ.get("GITHUB_ACTOR", "")
    head_ref = os.environ.get("GITHUB_HEAD_REF", "")
    event_path = os.environ.get("GITHUB_EVENT_PATH", "")

    skip_tests, skip_engine, modules = "false", "false", ""

    if event_name != "pull_request":
        # push or workflow_dispatch: always full build
        print(f"Event '{event_name}' is not pull_request — emitting defaults.")
    elif check_skip_tests(actor, head_ref):
        skip_tests = "true"
        print(f"skip_tests=true — dependabot non-code PR: {head_ref}")
    else:
        pr_number = None
        if event_path and os.path.exists(event_path):
            with open(event_path) as fh:
                pr_number = json.load(fh).get("pull_request", {}).get("number")
        changed_files, patches = [], {}
        if pr_number and token and repo:
            patches = get_changed_files_with_patches(token, repo, pr_number)
            changed_files = list(patches)
            preview = changed_files[:10]
            suffix = "..." if len(changed_files) > 10 else ""
            print(f"Changed files ({len(changed_files)}): {preview}{suffix}")
        else:
            print("Warning: missing pr_number/token/repo — full build.", file=sys.stderr)
        skip_tests, skip_engine, modules = decide(changed_files, os.getcwd(), patches)

    print(f"skip_tests={skip_tests}, skip_engine_tests={skip_engine}, "
          f"changed_modules={modules or '<full build>'}")
    _write_output("skip_tests", skip_tests, output_file)
    _write_output("skip_engine_tests", skip_engine, output_file)
    _write_output("changed_modules", modules, output_file)


if __name__ == "__main__":
    main()
