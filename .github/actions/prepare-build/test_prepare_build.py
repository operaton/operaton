import json
import unittest
import urllib.error
import sys
import os
from unittest.mock import patch

sys.path.insert(0, os.path.dirname(__file__))
from prepare_build import check_skip_tests, check_skip_engine_tests, check_webapps_only, get_changed_files


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

    def test_engine_cdi_changed(self):
        files = ["engine-cdi/src/main/java/Foo.java"]
        self.assertFalse(check_skip_engine_tests(files))

    def test_engine_spring_changed(self):
        files = ["engine-spring/src/main/java/Foo.java"]
        self.assertFalse(check_skip_engine_tests(files))

    def test_parent_changed(self):
        files = ["parent/pom.xml"]
        self.assertFalse(check_skip_engine_tests(files))

    def test_bom_changed(self):
        files = ["bom/pom.xml"]
        self.assertFalse(check_skip_engine_tests(files))

    def test_database_changed(self):
        files = ["database/pom.xml"]
        self.assertFalse(check_skip_engine_tests(files))

    def test_test_utils_changed(self):
        files = ["test-utils/src/main/java/Foo.java"]
        self.assertFalse(check_skip_engine_tests(files))

    def test_root_pom_changed(self):
        files = ["pom.xml"]
        self.assertFalse(check_skip_engine_tests(files))

    def test_empty_files_returns_false(self):
        self.assertFalse(check_skip_engine_tests([]))

    def test_mixed_no_engine(self):
        files = ["docs/README.md", "spring-boot-starter/src/main/java/Foo.java"]
        self.assertTrue(check_skip_engine_tests(files))


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
