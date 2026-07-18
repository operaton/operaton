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
    check_docs_only,
    check_needs_real_frontend,
    check_skip_engine_tests,
    check_skip_tests,
    classify_changes,
    compute_core_api,
    compute_downstream,
    discover_modules,
    discover_test_jar_producers,
    get_changed_files,
    map_file_to_module,
    relevant_test_jar_producers,
)

REPO_ROOT = Path(__file__).resolve().parents[3]


def make_pom(root, module_dir, group_id, artifact_id,
             parent=None, deps=(), produces_test_jar=False):
    """Write a minimal pom.xml. parent/deps are (groupId, artifactId) tuples."""
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
        f'<dependencies>{deps_xml}</dependencies>'
        f'{build_xml}'
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
        make_pom(r, "engine", "org.operaton.bpm", "operaton-engine",
                 parent=("org.operaton.bpm", "operaton-database-settings"),
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
        c = classify_changes([".github/workflows/build.yml"], self.module_dirs)
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
