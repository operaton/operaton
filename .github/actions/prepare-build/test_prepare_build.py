import os
import sys
import tempfile
import unittest
import urllib.error
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, os.path.dirname(__file__))
from prepare_build import (
    build_module_graph,
    changed_properties,
    check_docs_only,
    check_needs_real_frontend,
    check_skip_engine_tests,
    check_skip_tests,
    classify_changes,
    compute_core_api,
    compute_downstream,
    discover_modules,
    discover_test_jar_producers,
    decide,
    get_changed_files,
    is_non_build_file,
    map_file_to_module,
    relevant_test_jar_producers,
)

REPO_ROOT = Path(__file__).resolve().parents[3]


def make_pom(root, module_dir, group_id, artifact_id,
             parent=None, deps=(), produces_test_jar=False, bom_imports=(),
             modules=(), extra_xml=""):
    """Write a minimal pom.xml. parent/deps/bom_imports are (groupId, artifactId)
    tuples, modules are sub-module names, extra_xml is appended verbatim."""
    path = Path(root) / module_dir / "pom.xml"
    path.parent.mkdir(parents=True, exist_ok=True)
    parent_xml = ""
    if parent:
        parent_xml = (f"<parent><groupId>{parent[0]}</groupId>"
                      f"<artifactId>{parent[1]}</artifactId>"
                      f"<version>1.0</version></parent>")
    deps_xml = "".join(
        f"<dependency><groupId>{g}</groupId><artifactId>{a}</artifactId></dependency>"
        for g, a in deps)
    dep_mgmt_xml = ""
    if bom_imports:
        dep_mgmt_xml = "<dependencyManagement><dependencies>" + "".join(
            f"<dependency><groupId>{g}</groupId><artifactId>{a}</artifactId>"
            f"<version>1.0</version><scope>import</scope><type>pom</type></dependency>"
            for g, a in bom_imports) + "</dependencies></dependencyManagement>"
    modules_xml = ""
    if modules:
        modules_xml = "<modules>" + "".join(
            f"<module>{m}</module>" for m in modules) + "</modules>"
    build_xml = ""
    if produces_test_jar:
        build_xml = (
            '<build><plugins><plugin><groupId>org.apache.maven.plugins</groupId>'
            '<artifactId>maven-jar-plugin</artifactId><executions><execution>'
            '<goals><goal>test-jar</goal></goals></execution></executions>'
            '</plugin></plugins></build>')
    path.write_text(
        '<?xml version="1.0"?>'
        '<project xmlns="http://maven.apache.org/POM/4.0.0">'
        f'{parent_xml}'
        f'<groupId>{group_id}</groupId><artifactId>{artifact_id}</artifactId>'
        f'<version>1.0</version>'
        f'{dep_mgmt_xml}'
        f'<dependencies>{deps_xml}</dependencies>'
        f'{build_xml}'
        f'{modules_xml}'
        f'{extra_xml}'
        '</project>')


class FixtureRepo(unittest.TestCase):
    """Small synthetic reactor: root -> parent -> database -> engine, etc."""

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        r = self.tmp.name
        make_pom(r, ".", "org.operaton.bpm", "operaton-root")
        make_pom(r, "parent", "org.operaton.bpm", "operaton-parent",
                 parent=("org.operaton.bpm", "operaton-root"))
        make_pom(r, "database", "org.operaton.bpm", "operaton-database-settings",
                 parent=("org.operaton.bpm", "operaton-parent"))
        make_pom(r, "commons/typed-values", "org.operaton.commons", "operaton-commons-typed-values",
                 parent=("org.operaton.bpm", "operaton-parent"))
        make_pom(r, "juel", "org.operaton.bpm.juel", "operaton-juel",
                 parent=("org.operaton.bpm", "operaton-parent"))
        make_pom(r, "bom/internal-dependencies", "org.operaton.bpm",
                 "operaton-core-internal-dependencies",
                 parent=("org.operaton.bpm", "operaton-parent"))
        make_pom(r, "engine", "org.operaton.bpm", "operaton-engine",
                 parent=("org.operaton.bpm", "operaton-database-settings"),
                 bom_imports=[("org.operaton.bpm", "operaton-core-internal-dependencies")],
                 deps=[("org.operaton.commons", "operaton-commons-typed-values"),
                       ("org.operaton.bpm.juel", "operaton-juel"),
                       ("org.mybatis", "mybatis")])
        make_pom(r, "engine-rest/engine-rest", "org.operaton.bpm", "operaton-engine-rest",
                 parent=("org.operaton.bpm", "operaton-parent"),
                 deps=[("org.operaton.bpm", "operaton-engine")])
        make_pom(r, "webapps", "org.operaton.bpm.webapp", "operaton-webapps-root",
                 parent=("org.operaton.bpm", "operaton-parent"))
        make_pom(r, "webapps/assembly", "org.operaton.bpm.webapp", "operaton-webapp",
                 parent=("org.operaton.bpm.webapp", "operaton-webapps-root"),
                 deps=[("org.operaton.bpm", "operaton-engine-rest")])
        make_pom(r, "webapps-neo", "org.operaton.bpm.webapp-neo", "operaton-webapp-neo-root",
                 parent=("org.operaton.bpm", "operaton-database-settings"))
        make_pom(r, "webapps-neo/assembly", "org.operaton.bpm.webapp-neo", "operaton-webapp-neo",
                 parent=("org.operaton.bpm.webapp-neo", "operaton-webapp-neo-root"),
                 deps=[("org.operaton.bpm", "operaton-engine-rest")])
        make_pom(r, "distro/webjar-neo", "org.operaton.bpm", "operaton-webapp-webjar-neo",
                 parent=("org.operaton.bpm", "operaton-parent"),
                 deps=[("org.operaton.bpm.webapp-neo", "operaton-webapp-neo")])
        make_pom(r, "spring-boot-starter/starter", "org.operaton.bpm.springboot", "operaton-starter",
                 parent=("org.operaton.bpm", "operaton-parent"),
                 deps=[("org.operaton.bpm", "operaton-engine")])
        make_pom(r, "clients/java/client", "org.operaton.bpm", "operaton-external-task-client",
                 parent=("org.operaton.bpm", "operaton-parent"))
        make_pom(r, "spin/core", "org.operaton.spin", "operaton-spin-core",
                 parent=("org.operaton.bpm", "operaton-parent"),
                 produces_test_jar=True)
        make_pom(r, "spin/dataformat-xml-dom", "org.operaton.spin", "operaton-spin-dataformat-xml-dom",
                 parent=("org.operaton.bpm", "operaton-parent"),
                 deps=[("org.operaton.spin", "operaton-spin-core")])
        self.root = r
        self.module_dirs = discover_modules(r)

    def tearDown(self):
        self.tmp.cleanup()


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


class TestCheckDocsOnly(unittest.TestCase):

    def test_markdown_only(self):
        self.assertTrue(check_docs_only(
            ["README.md", "docs/decisions/0001-adr.md"]))

    def test_license_and_notice(self):
        self.assertTrue(check_docs_only(["LICENSE", "NOTICE.txt", "CONTRIBUTORS.md"]))

    def test_mixed_with_code(self):
        self.assertFalse(check_docs_only(["README.md", "engine/src/main/java/Foo.java"]))

    def test_empty_returns_false(self):
        self.assertFalse(check_docs_only([]))

    def test_non_root_txt_is_not_docs(self):
        self.assertFalse(check_docs_only(["engine/src/test/resources/data.txt"]))


class TestModuleDiscovery(FixtureRepo):

    def test_discovers_nested_modules(self):
        self.assertIn("engine", self.module_dirs)
        self.assertIn("engine-rest/engine-rest", self.module_dirs)
        self.assertIn("clients/java/client", self.module_dirs)

    def test_root_is_not_a_module(self):
        self.assertNotIn(".", self.module_dirs)
        self.assertNotIn("", self.module_dirs)

    def test_map_file_to_deepest_module(self):
        self.assertEqual(
            map_file_to_module("webapps/assembly/src/main/java/Foo.java", self.module_dirs),
            "webapps/assembly")
        self.assertEqual(
            map_file_to_module("webapps/somefile.js", self.module_dirs),
            "webapps")

    def test_map_unknown_path_returns_none(self):
        self.assertIsNone(map_file_to_module("unknown-dir/Foo.java", self.module_dirs))
        self.assertIsNone(map_file_to_module("rootfile.sh", self.module_dirs))


class TestClassifyChanges(FixtureRepo):

    def test_single_module_change(self):
        c = classify_changes(
            ["spring-boot-starter/starter/src/main/java/Foo.java"], self.module_dirs)
        self.assertFalse(c.full_build)
        self.assertEqual(c.changed_modules, ["spring-boot-starter/starter"])

    def test_multi_module_change(self):
        c = classify_changes(
            ["webapps/assembly/src/x.java", "clients/java/client/src/y.java"],
            self.module_dirs)
        self.assertFalse(c.full_build)
        self.assertEqual(c.changed_modules,
                         ["clients/java/client", "webapps/assembly"])

    def test_docs_only(self):
        c = classify_changes(["README.md", "engine/README.md"], self.module_dirs)
        self.assertTrue(c.docs_only)
        self.assertFalse(c.full_build)
        self.assertEqual(c.changed_modules, [])

    def test_any_pom_change_forces_full_build(self):
        c = classify_changes(
            ["webapps/assembly/pom.xml", "webapps/assembly/src/x.java"],
            self.module_dirs)
        self.assertTrue(c.full_build)

    def test_parent_change_forces_full_build(self):
        c = classify_changes(["parent/pom.xml"], self.module_dirs)
        self.assertTrue(c.full_build)

    def test_qa_change_forces_full_build(self):
        c = classify_changes(["qa/integration-tests-engine/src/Foo.java"],
                             self.module_dirs)
        self.assertTrue(c.full_build)

    def test_ci_change_forces_full_build(self):
        c = classify_changes([".github/workflows/pr-build.yml"], self.module_dirs)
        self.assertTrue(c.full_build)
        c = classify_changes([".github/actions/mvnd-setup/action.yml"], self.module_dirs)
        self.assertTrue(c.full_build)

    def test_root_level_file_forces_full_build(self):
        c = classify_changes(["mvnw"], self.module_dirs)
        self.assertTrue(c.full_build)

    def test_unmapped_path_forces_full_build(self):
        c = classify_changes(["mystery/thing.java"], self.module_dirs)
        self.assertTrue(c.full_build)

    def test_empty_files_forces_full_build(self):
        c = classify_changes([], self.module_dirs)
        self.assertTrue(c.full_build)

    def test_mixed_module_and_global_escalates(self):
        c = classify_changes(
            ["webapps/assembly/src/x.java", ".devenv/scripts/build/build.sh"],
            self.module_dirs)
        self.assertTrue(c.full_build)


class TestCoreApi(FixtureRepo):

    def core_api(self):
        graph = build_module_graph(self.root)
        return compute_core_api(graph)

    def test_core_api_contains_engine_and_upstream(self):
        core = self.core_api()
        self.assertIn("engine", core)
        self.assertIn("commons/typed-values", core)
        self.assertIn("juel", core)
        # parent chain of engine
        self.assertIn("database", core)
        self.assertIn("parent", core)

    def test_core_api_excludes_downstream(self):
        core = self.core_api()
        self.assertNotIn("engine-rest/engine-rest", core)
        self.assertNotIn("webapps", core)
        self.assertNotIn("spring-boot-starter/starter", core)
        self.assertNotIn("clients/java/client", core)

    def test_skip_engine_tests_when_core_api_untouched(self):
        self.assertTrue(check_skip_engine_tests(
            [".github/workflows/build.yml", "webapps/assembly/src/x.java"],
            self.core_api()))

    def test_no_skip_when_core_api_module_touched(self):
        self.assertFalse(check_skip_engine_tests(
            ["commons/typed-values/src/main/java/Foo.java"], self.core_api()))

    def test_no_skip_when_root_pom_touched(self):
        self.assertFalse(check_skip_engine_tests(["pom.xml"], self.core_api()))

    def test_no_skip_when_engine_package_module_touched(self):
        # engine-cdi/engine-rest/etc. have tests in org.operaton.bpm.engine.*
        # packages; the excludes regex would wrongly skip them.
        self.assertFalse(check_skip_engine_tests(
            ["engine-rest/engine-rest/src/main/java/Foo.java"], self.core_api()))
        self.assertFalse(check_skip_engine_tests(
            ["engine-cdi/src/main/java/Foo.java"], self.core_api()))

    def test_no_skip_on_empty_files(self):
        self.assertFalse(check_skip_engine_tests([], self.core_api()))

    def test_core_api_contains_imported_bom(self):
        # engine imports the BOM in <dependencyManagement>; a version bump
        # there changes what the engine is built and tested against
        self.assertIn("bom/internal-dependencies", self.core_api())

    def test_no_skip_when_imported_bom_touched(self):
        self.assertFalse(check_skip_engine_tests(
            ["bom/internal-dependencies/pom.xml"], self.core_api()))

    def test_no_skip_when_build_wide_files_touched(self):
        for f in (".mvn/wrapper/maven-wrapper.properties", ".mvn/maven.config",
                  "bom/some-other-bom/pom.xml", "parent/pom.xml"):
            with self.subTest(f=f):
                self.assertFalse(check_skip_engine_tests([f], self.core_api()))


class TestComputeDownstream(FixtureRepo):

    def graph(self):
        return build_module_graph(self.root)

    def test_downstream_of_engine_includes_dependents(self):
        down = compute_downstream(self.graph(), ["engine"])
        self.assertIn("engine-rest/engine-rest", down)
        self.assertIn("spring-boot-starter/starter", down)

    def test_downstream_transitive(self):
        # webapps/assembly depends on engine-rest, which depends on engine
        down = compute_downstream(self.graph(), ["engine"])
        self.assertIn("webapps/assembly", down)

    def test_downstream_excludes_unrelated_and_upstream(self):
        down = compute_downstream(self.graph(), ["clients/java/client"])
        self.assertNotIn("engine", down)
        self.assertNotIn("webapps/assembly", down)

    def test_downstream_of_leaf_is_empty(self):
        down = compute_downstream(self.graph(), ["webapps/assembly"])
        self.assertEqual(down, set())


class TestCheckNeedsRealFrontend(FixtureRepo):

    def graph(self):
        return build_module_graph(self.root)

    def test_true_when_changed_module_itself_is_frontend_sensitive(self):
        self.assertTrue(check_needs_real_frontend(["webapps/assembly"], self.graph()))

    def test_true_when_downstream_includes_frontend_sensitive_module(self):
        # engine -> engine-rest -> webapps/assembly, and engine -> spring-boot-starter/starter
        self.assertTrue(check_needs_real_frontend(["engine"], self.graph()))

    def test_false_when_closure_has_no_frontend_sensitive_module(self):
        self.assertFalse(check_needs_real_frontend(["clients/java/client"], self.graph()))

    def test_true_when_neo_frontend_module_changed(self):
        # webapps-neo (root pom) runs the npm build of webapps-neo/frontend
        self.assertTrue(check_needs_real_frontend(["webapps-neo"], self.graph()))

    def test_true_when_neo_webjar_changed(self):
        self.assertTrue(check_needs_real_frontend(["distro/webjar-neo"], self.graph()))


class TestWebappsNeo(FixtureRepo):
    """Changes to the new webapps (webapps-neo) are kept apart from the legacy
    webapps, so a neo-only change neither builds nor tests the legacy ones."""

    def graph(self):
        return build_module_graph(self.root)

    def test_neo_frontend_file_maps_to_neo_root_module(self):
        # webapps-neo/frontend has no pom.xml; its npm build lives in webapps-neo
        self.assertEqual(
            map_file_to_module("webapps-neo/frontend/src/app.tsx", self.module_dirs),
            "webapps-neo")

    def test_neo_assembly_file_maps_to_neo_assembly(self):
        self.assertEqual(
            map_file_to_module("webapps-neo/assembly/src/main/java/Foo.java", self.module_dirs),
            "webapps-neo/assembly")

    def test_neo_only_change_is_narrowed_to_neo(self):
        c = classify_changes(
            ["webapps-neo/frontend/src/app.tsx", "webapps-neo/frontend/package-lock.json"],
            self.module_dirs)
        self.assertFalse(c.full_build)
        self.assertEqual(c.changed_modules, ["webapps-neo"])

    def test_neo_change_does_not_reach_legacy_webapps(self):
        down = compute_downstream(self.graph(), ["webapps-neo"])
        self.assertIn("webapps-neo/assembly", down)
        self.assertIn("distro/webjar-neo", down)
        self.assertNotIn("webapps/assembly", down)

    def test_legacy_change_does_not_reach_neo_webapps(self):
        down = compute_downstream(self.graph(), ["webapps/assembly"])
        self.assertNotIn("webapps-neo/assembly", down)
        self.assertNotIn("distro/webjar-neo", down)

    def test_neo_change_skips_engine_tests(self):
        core = compute_core_api(self.graph())
        self.assertNotIn("webapps-neo", core)
        self.assertTrue(check_skip_engine_tests(
            ["webapps-neo/frontend/src/app.tsx"], core))


class TestRelevantTestJarProducers(FixtureRepo):
    """Only producers whose test-jar could actually be consumed within the
    current build's scope need the (slower) real-test-compile treatment."""

    def graph(self):
        return build_module_graph(self.root)

    def test_includes_producer_whose_consumer_is_changed(self):
        # spin/dataformat-xml-dom depends on spin/core's test-jar
        relevant = relevant_test_jar_producers(
            ["spin/core"], ["spin/dataformat-xml-dom"], self.graph())
        self.assertIn("spin/core", relevant)

    def test_excludes_producer_with_no_consumer_in_scope(self):
        relevant = relevant_test_jar_producers(
            ["spin/core"], ["clients/java/client"], self.graph())
        self.assertNotIn("spin/core", relevant)

    def test_excludes_producer_that_is_itself_changed(self):
        # spin/core will be rebuilt for real by phase 3 anyway, no prelim needed
        relevant = relevant_test_jar_producers(
            ["spin/core"], ["spin/core"], self.graph())
        self.assertNotIn("spin/core", relevant)

    def test_empty_producers_returns_empty(self):
        self.assertEqual(
            relevant_test_jar_producers([], ["spin/dataformat-xml-dom"], self.graph()),
            [])


class TestDiscoverTestJarProducers(FixtureRepo):

    def test_finds_module_with_test_jar_execution(self):
        producers = discover_test_jar_producers(self.root)
        self.assertIn("spin/core", producers)

    def test_excludes_modules_without_test_jar_execution(self):
        producers = discover_test_jar_producers(self.root)
        self.assertNotIn("engine", producers)
        self.assertNotIn("spin/dataformat-xml-dom", producers)


class TestDiscoverTestJarProducersRealRepo(unittest.TestCase):
    """These modules' test-jars are real compile-time dependencies elsewhere;
    if maven.test.skip=true ever produces an empty test-jar for one of them,
    downstream test-compile silently breaks. Guards against a new producer
    module being added without being covered by the phase-1 repair pass."""

    def test_known_producers_discovered(self):
        producers = discover_test_jar_producers(REPO_ROOT)
        for m in ("spin/core", "engine-cdi", "engine-spring",
                  "model-api/xml-model", "engine-rest/engine-rest"):
            self.assertIn(m, producers)


class TestNonBuildFiles(FixtureRepo):
    """Rule 1: files that cannot change the PR build are dropped before
    classification instead of escalating to a full build."""

    def test_non_build_files(self):
        for f in ("AGENTS.md", "engine/README.md", ".claude/skills/x/y.py",
                  ".devcontainer/devcontainer.json", "docs/decisions/process.png",
                  ".gitignore", "jreleaser.yml", ".github/workflows/build.yml",
                  ".github/zizmor.yml", ".github/labels/labels.yml",
                  ".devenv/scripts/tools/nullmarked-coverage.py",
                  ".devenv/scripts/maintenance/code-cleanup.sh"):
            with self.subTest(f=f):
                self.assertTrue(is_non_build_file(f))

    def test_build_files(self):
        for f in (".github/workflows/pr-build.yml", ".github/actions/prepare-build/action.yml",
                  ".github/scripts/jacoco-create-flag-files.sh",
                  ".devenv/scripts/build/build.sh", ".mvn/maven.config", "pom.xml", "mvnw",
                  "engine/src/main/resources/notes.md", "engine/src/main/java/Foo.java"):
            with self.subTest(f=f):
                self.assertFalse(is_non_build_file(f))

    def test_non_build_file_next_to_code_does_not_escalate(self):
        c = classify_changes(["AGENTS.md", "engine/src/main/java/Foo.java"], self.module_dirs)
        self.assertFalse(c.full_build)
        self.assertEqual(c.changed_modules, ["engine"])

    def test_only_non_build_files_skip_tests(self):
        c = classify_changes(["jreleaser.yml", ".claude/skills/x/y.py",
                              "docs/decisions/process.bpmn"], self.module_dirs)
        self.assertTrue(c.docs_only)

    def test_skip_engine_tests_ignores_non_build_files(self):
        core = compute_core_api(build_module_graph(self.root))
        self.assertFalse(check_skip_engine_tests(["AGENTS.md"], core))
        self.assertTrue(check_skip_engine_tests(
            ["AGENTS.md", "webapps/assembly/src/x.java"], core))


NODE_PLUGIN_MGMT = (
    "<build><pluginManagement><plugins><plugin>"
    "<groupId>com.github.eirslett</groupId><artifactId>frontend-maven-plugin</artifactId>"
    "<configuration><nodeVersion>v${version.nodejs}</nodeVersion></configuration>"
    "</plugin></plugins></pluginManagement></build>")
NODE_PLUGIN_BOUND = (
    "<build><plugins><plugin>"
    "<groupId>com.github.eirslett</groupId><artifactId>frontend-maven-plugin</artifactId>"
    "</plugin></plugins></build>")


def property_patch(name, old, new, extra=()):
    lines = ["@@ -1,3 +1,3 @@", "   <properties>",
             f"-    <{name}>{old}</{name}>", f"+    <{name}>{new}</{name}>"]
    return "\n".join(lines + list(extra) + ["   </properties>"])


class PomFixtureRepo(unittest.TestCase):
    """Reactor with version properties defined in parent/pom.xml."""

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        r = self.tmp.name
        make_pom(r, ".", "org.operaton.bpm", "operaton-root",
                 modules=["parent", "bom/internal-dependencies", "engine", "webapps",
                          "quarkus-extension", "clients/java/client"],
                 extra_xml="<build><pluginManagement><plugins><plugin>"
                           "<groupId>io.quarkus</groupId>"
                           "<artifactId>quarkus-extension-maven-plugin</artifactId>"
                           "<version>${version.quarkus}</version>"
                           "</plugin></plugins></pluginManagement></build>")
        make_pom(r, "parent", "org.operaton.bpm", "operaton-parent",
                 parent=("org.operaton.bpm", "operaton-root"),
                 extra_xml="<properties><version.nodejs>24.20.0</version.nodejs>"
                           "<version.jackson>2.21.0</version.jackson>"
                           "<version.quarkus>3.33.0</version.quarkus>"
                           "<version.joda>2.14</version.joda>"
                           "<version.client.lib>${version.joda}</version.client.lib>"
                           "</properties>" + NODE_PLUGIN_MGMT)
        make_pom(r, "bom/internal-dependencies", "org.operaton.bpm",
                 "operaton-core-internal-dependencies",
                 parent=("org.operaton.bpm", "operaton-parent"),
                 extra_xml="<dependencyManagement><dependencies><dependency>"
                           "<groupId>com.fasterxml.jackson.core</groupId>"
                           "<artifactId>jackson-databind</artifactId>"
                           "<version>${version.jackson}</version>"
                           "</dependency></dependencies></dependencyManagement>")
        make_pom(r, "engine", "org.operaton.bpm", "operaton-engine",
                 parent=("org.operaton.bpm", "operaton-parent"),
                 bom_imports=[("org.operaton.bpm", "operaton-core-internal-dependencies")])
        make_pom(r, "webapps", "org.operaton.bpm.webapp", "operaton-webapps-root",
                 parent=("org.operaton.bpm", "operaton-parent"),
                 extra_xml=NODE_PLUGIN_BOUND)
        make_pom(r, "quarkus-extension", "org.operaton.bpm.quarkus", "operaton-quarkus",
                 parent=("org.operaton.bpm", "operaton-parent"), modules=["engine"],
                 extra_xml="<dependencyManagement><dependencies><dependency>"
                           "<groupId>io.quarkus</groupId><artifactId>quarkus-bom</artifactId>"
                           "<version>${version.quarkus}</version><scope>import</scope>"
                           "<type>pom</type></dependency></dependencies></dependencyManagement>")
        make_pom(r, "quarkus-extension/engine", "org.operaton.bpm.quarkus", "operaton-quarkus-engine",
                 parent=("org.operaton.bpm.quarkus", "operaton-quarkus"))
        make_pom(r, "clients/java/client", "org.operaton.bpm", "operaton-external-task-client",
                 parent=("org.operaton.bpm", "operaton-parent"))
        res = Path(r) / "clients/java/client/src/main/resources/client.properties"
        res.parent.mkdir(parents=True)
        res.write_text("lib.version=${version.client.lib}\n")
        self.root = r
        self.module_dirs = discover_modules(r)

    def tearDown(self):
        self.tmp.cleanup()

    def classify(self, files, patches=None):
        return classify_changes(files, self.module_dirs, self.root, patches)


class TestLeafPomChanges(PomFixtureRepo):
    """Rule 2: a pom.xml of a leaf module narrows to that module."""

    def test_leaf_module_pom_is_narrowed(self):
        c = self.classify(["clients/java/client/pom.xml"])
        self.assertFalse(c.full_build)
        self.assertEqual(c.changed_modules, ["clients/java/client"])

    def test_aggregator_pom_forces_full_build(self):
        self.assertTrue(self.classify(["quarkus-extension/pom.xml"]).full_build)

    def test_build_wide_poms_force_full_build(self):
        for f in ("pom.xml", "parent/pom.xml", "bom/internal-dependencies/pom.xml"):
            with self.subTest(f=f):
                self.assertTrue(self.classify([f]).full_build)

    def test_without_root_any_pom_forces_full_build(self):
        c = classify_changes(["clients/java/client/pom.xml"], self.module_dirs)
        self.assertTrue(c.full_build)


class TestChangedProperties(unittest.TestCase):

    def test_property_value_change(self):
        self.assertEqual(changed_properties(
            property_patch("version.nodejs", "24.20.0", "24.21.0")), {"version.nodejs"})

    def test_several_properties(self):
        patch = property_patch("version.nodejs", "1", "2",
                               extra=["-    <version.npm>1</version.npm>",
                                      "+    <version.npm>2</version.npm>"])
        self.assertEqual(changed_properties(patch), {"version.nodejs", "version.npm"})

    def test_other_change_is_not_a_property_change(self):
        patch = property_patch("version.nodejs", "1", "2",
                               extra=["+    <module>new-module</module>"])
        self.assertIsNone(changed_properties(patch))
        self.assertIsNone(changed_properties(
            "@@ -1 +1 @@\n-      <type>tar.gz</type>\n+      <type>zip</type>\n"
            "+      <classifier>x</classifier>"))

    def test_added_property_is_not_a_value_change(self):
        self.assertIsNone(changed_properties("@@ -1 +1 @@\n+    <version.x>1</version.x>"))

    def test_missing_patch(self):
        self.assertIsNone(changed_properties(None))
        self.assertIsNone(changed_properties(""))


class TestPropertyBumps(PomFixtureRepo):
    """Rule 3: a pom change that only bumps version properties narrows to the
    modules referencing those properties."""

    def bump(self, name, pom="parent/pom.xml"):
        return self.classify([pom], {pom: property_patch(name, "1", "2")})

    def test_plugin_management_property_narrows_to_plugin_users(self):
        c = self.bump("version.nodejs")
        self.assertFalse(c.full_build)
        self.assertEqual(c.changed_modules, ["webapps"])

    def test_root_pom_plugin_management_property(self):
        c = self.bump("version.quarkus", pom="pom.xml")
        self.assertFalse(c.full_build)
        # quarkus-extension uses it in <dependencyManagement>; no module binds
        # the managed plugin, the extension's children inherit via -amd
        self.assertEqual(c.changed_modules, ["quarkus-extension"])

    def test_property_used_in_build_wide_bom_forces_full_build(self):
        self.assertTrue(self.bump("version.jackson").full_build)

    def test_property_used_via_derived_property_in_filtered_resource(self):
        c = self.bump("version.joda")
        self.assertFalse(c.full_build)
        self.assertEqual(c.changed_modules, ["clients/java/client"])

    def test_property_bump_combined_with_code_change(self):
        pom = "parent/pom.xml"
        c = self.classify([pom, "clients/java/client/src/main/java/Foo.java"],
                          {pom: property_patch("version.nodejs", "1", "2")})
        self.assertEqual(c.changed_modules, ["clients/java/client", "webapps"])

    def test_non_property_change_of_parent_forces_full_build(self):
        pom = "parent/pom.xml"
        self.assertTrue(self.classify([pom], {pom: "@@ -1 +1 @@\n+  <x/>"}).full_build)
        self.assertTrue(self.classify([pom], {pom: None}).full_build)

    def test_unreferenced_property_is_narrowed_to_nothing_but_still_builds(self):
        # no module references it: nothing to test, but not a skip either
        _, _, modules = decide(["parent/pom.xml"], self.root,
                               {"parent/pom.xml": property_patch("version.unused", "1", "2")})
        self.assertEqual(modules, "")

    def test_node_bump_does_not_run_engine_tests(self):
        skip_tests, _, modules = decide(
            ["parent/pom.xml"], self.root,
            {"parent/pom.xml": property_patch("version.nodejs", "1", "2")})
        self.assertEqual(skip_tests, "false")
        down = compute_downstream(build_module_graph(self.root), modules.split(","))
        self.assertNotIn("engine", set(modules.split(",")) | down)


class TestAgainstRealRepo(unittest.TestCase):
    """Sanity-check discovery and core-api derivation against the actual reactor."""

    @classmethod
    def setUpClass(cls):
        cls.modules = discover_modules(REPO_ROOT)
        cls.core = compute_core_api(build_module_graph(REPO_ROOT))

    def test_known_modules_discovered(self):
        for m in ("engine", "webapps/assembly", "spring-boot-starter/starter",
                  "clients/java/client", "engine-rest/engine-rest"):
            self.assertIn(m, self.modules)

    def test_core_api_plausible(self):
        self.assertIn("engine", self.core)
        self.assertIn("juel", self.core)
        self.assertTrue(any(m.startswith("model-api/") for m in self.core))
        self.assertNotIn("webapps/assembly", self.core)
        self.assertNotIn("clients/java/client", self.core)
        self.assertNotIn("spring-boot-starter/starter", self.core)

    def test_core_api_contains_imported_internal_bom(self):
        self.assertIn("bom/internal-dependencies", self.core)

    def test_node_bump_narrows_to_frontend_modules(self):
        _, _, modules = decide(
            ["parent/pom.xml"], REPO_ROOT,
            {"parent/pom.xml": property_patch("version.nodejs", "1", "2")})
        modules = set(modules.split(","))
        self.assertIn("webapps", modules)
        self.assertIn("webapps-neo", modules)
        down = compute_downstream(build_module_graph(REPO_ROOT), modules)
        self.assertNotIn("engine", modules | down)


class TestGetChangedFilesFallback(unittest.TestCase):

    def test_url_error_returns_empty(self):
        with patch("urllib.request.urlopen",
                   side_effect=urllib.error.URLError("connection refused")):
            result = get_changed_files("token", "org/repo", 123)
        self.assertEqual(result, [])

    def test_http_error_returns_empty(self):
        with patch("urllib.request.urlopen",
                   side_effect=urllib.error.HTTPError(
                       url="", code=403, msg="Forbidden", hdrs=None, fp=None)):
            result = get_changed_files("token", "org/repo", 123)
        self.assertEqual(result, [])

    def test_json_decode_error_returns_empty(self):
        with patch("urllib.request.urlopen") as mock_open:
            mock_open.return_value.__enter__ = lambda s: s
            mock_open.return_value.__exit__ = lambda s, *a: False
            mock_open.return_value.read.return_value = b"not-json{"
            result = get_changed_files("token", "org/repo", 123)
        self.assertEqual(result, [])


if __name__ == "__main__":
    unittest.main()
