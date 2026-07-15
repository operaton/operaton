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
