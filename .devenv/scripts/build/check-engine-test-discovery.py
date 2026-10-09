#!/usr/bin/env python3
# Copyright 2026 the Operaton contributors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at:
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Check actual Surefire discovery using the engine's inherited POM filters.

Run after installing reactor dependencies. No new dependencies are needed. The
checker lives outside JUnit so the discovery bug cannot silently skip it, too.
"""

import argparse
from collections import Counter
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[3]
ENGINE = ROOT / "engine"
WORK = ENGINE / "target" / "test-discovery"
MAIL = "org.operaton.bpm.engine.impl.bpmn.behavior.MailActivityBehaviorTest"
UTIL = "org.operaton.bpm.engine.impl.util.ClassNameUtilTest"
NESTED = "org.operaton.bpm.engine.test.junit5.ProcessEngineExtensionEngineRetentionTest"
TEST_CASE = "org.operaton.bpm.engine.test.jobexecutor.JobExecutorTestCase"
NASHORN = "org.operaton.bpm.engine.test.bpmn.scripttask.ScriptTaskNashornTest"


def cases(class_name, methods):
    return {(class_name, method) for method in methods}


# Intentional, explicit canaries: neither a nonzero total nor an outer suite
# element proves that top-level, parameterized and nested tests all executed.
MAIL_CASES = cases(MAIL, [
    "setFrom_emailThrowsEmailException_wrapped",
    "setCharset_whenCharsetExpressionNull_doesNotSetCharset",
    "createEmail_prefersHtmlWhenHtmlPresent",
    "createEmail_returnsTextOnlyWhenOnlyTextPresent",
    "createTextOnlyEmail_returnsSimpleEmail",
    "createHtmlEmail_returnsHtmlEmail",
    "setFrom_withExplicitFrom_callsEmailSetFrom",
    "setSubject_withExplicitSubject_setsIt",
    "execute_configuresAndSendsEmail",
    "setCharset_whenCharsetExpressionPresent_setsCharset",
    "setSubject_withNull_setsEmptyString",
    "setMailServerProperties_setsAllPropertiesFromProcessEngineConfig",
    "setFrom_withNullUsesDefaultFromFromProcessEngineConfig",
    "createEmail_withNoContent_throws",
    "execute_sendThrowsEmailException_wrapped",
] + [f"{method}[{index}]" for method in (
    "recipients_nullBehavior_respected",
    "recipients_splitsAndTrimsAndAddsAllRecipients",
    "recipients_emailThrowsEmailException_wrapped",
) for index in range(1, 4)])
UTIL_CASES = cases(UTIL, [
    f"getClassNameWithoutPackage_with{kind}_shouldReturnSimpleName[{index}]"
    for kind, count in (("Class", 4), ("Object", 3))
    for index in range(1, count + 1)
])
NESTED_CASES = {
    (NESTED + "$ProcessEngineExtensionFirstTests", "testProcessEngineExtensionInitialAvailability"),
    (NESTED + "$ProcessEngineExtensionSecondTests", "testExtensionsProcessEngineRetention"),
}
ALL_CASES = MAIL_CASES | UTIL_CASES | NESTED_CASES

# Keep profile properties intact: -Dtest would replace the POM includes/excludes
# and make a broken default configuration appear healthy.
SCENARIOS = (
    ("default", [], [], ALL_CASES),
    ("bpmn-profile", ["testBpmn"], [], MAIL_CASES),
    ("except-bpmn-profile", ["testExceptBpmn"], [], UTIL_CASES | NESTED_CASES),
    ("dotted-include", [], ["-Dtest.includes=impl.bpmn.behavior|impl.util"], MAIL_CASES | UTIL_CASES),
    ("dotted-exclude", [], ["-Dtest.includes=impl.bpmn.behavior|impl.util",
                           "-Dtest.excludes=impl.bpmn.behavior"], UTIL_CASES),
    ("no-match", [], ["-Dtest.includes=__NoEngineTestMatches__"], set()),
)


class DiscoveryError(Exception):
    pass


def normalized_method(name):
    # Surefire writes parameter type signatures, then [invocation index]. Keep
    # the index so losing one parameterized invocation cannot pass the check.
    return re.sub(r"\([^)]*\)(?=\[\d+\]$|$)", "", name)


def check_reports(directory, expected):
    actual = Counter()
    for report in sorted(directory.glob("TEST-*.xml")):
        try:
            root = ET.parse(report).getroot()
        except (ET.ParseError, OSError) as error:
            raise DiscoveryError(f"Cannot read {report}: {error}") from error
        if root.tag not in ("testsuite", "testsuites"):
            raise DiscoveryError(f"{report.name}: unexpected report root {root.tag}")
        for suite in root.iter("testsuite"):
            for status in ("failures", "errors", "skipped"):
                if suite.get(status, "0") != "0":
                    raise DiscoveryError(f"{report.name}: {status}={suite.get(status)}")
        for testcase in root.iter("testcase"):
            if any(testcase.find(status) is not None for status in ("failure", "error", "skipped")):
                raise DiscoveryError(f"{report.name}: unsuccessful testcase {testcase.get('name')}")
            identity = (testcase.get("classname", ""), normalized_method(testcase.get("name", "")))
            actual[identity] += 1

    wanted = Counter({identity: 1 for identity in expected})
    if actual != wanted:
        missing = sorted((wanted - actual).elements())
        unexpected = sorted((actual - wanted).elements())
        details = [f"Expected {sum(wanted.values())} successful testcases, found {sum(actual.values())}"]
        if missing:
            details.append("Missing: " + ", ".join(f"{cls}#{method}" for cls, method in missing))
        if unexpected:
            details.append("Unexpected or duplicate: " + ", ".join(f"{cls}#{method}" for cls, method in unexpected))
        raise DiscoveryError("\n".join(details))


def prepare_canaries():
    source = ENGINE / "target" / "test-classes"
    destination = WORK / "classes"
    destination.mkdir(parents=True)
    for class_name in (MAIL, UTIL, NESTED, TEST_CASE, NASHORN):
        relative = Path(*class_name.split("."))
        main = source / relative.with_suffix(".class")
        if not main.is_file():
            raise DiscoveryError(f"Compiled canary is missing: {main}")
        files = [main]
        if class_name != TEST_CASE:
            files.extend(sorted(main.parent.glob(main.stem + "$*.class")))
        # The TestCase outer class must be excluded. Do not scan its nested
        # classes directly: selecting those legitimately includes their parent.
        for file in files:
            target = destination / file.relative_to(source)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(file, target)


def run_maven(runner, forwarded, arguments, log_name):
    command = [runner, *forwarded, "-pl", "engine", "-Dmaven.build.cache.enabled=false",
               "-DskipTests=false", "-Dmaven.test.skip=false", "-DskipTests.operaton-engine=false",
               "-Dmaven.test.failure.ignore=false", *arguments]
    log = WORK / "logs" / f"{log_name}.log"
    log.parent.mkdir(parents=True, exist_ok=True)
    print(f"Running {shlex.join(command)}\nLog: {log}", flush=True)
    with log.open("w", encoding="utf-8") as output:
        result = subprocess.run(command, cwd=ROOT, stdout=output, stderr=subprocess.STDOUT, check=False)
    if result.returncode:
        print("\n".join(log.read_text(encoding="utf-8", errors="replace").splitlines()[-80:]), file=sys.stderr)
        raise DiscoveryError(f"Maven failed with exit code {result.returncode}; see {log}")


def check_forwarded_arguments(arguments):
    # Forward settings, local repository and ordinary JVM/Maven options, but
    # never let caller selectors replace the configuration under examination.
    forbidden = ("-P", "--activate-profiles", "-pl", "--projects", "-f", "--file",
                 "-am", "--also-make", "-N", "--non-recursive", "-r", "--resume")
    value_options = {"-s", "--settings", "-gs", "--global-settings", "-t", "--toolchains",
                     "-gt", "--global-toolchains", "-l", "--log-file", "-T", "--threads",
                     "-b", "--builder", "--color"}
    arguments = iter(arguments)
    for argument in arguments:
        # These are flags, not the attached-value form of -f/--file.
        if argument in ("-fae", "-ff", "-fn"):
            continue
        if any(argument.startswith(prefix) for prefix in forbidden):
            raise DiscoveryError(f"Do not override test selection or project scope: {argument}")
        if argument in value_options or argument in ("-D", "--define"):
            value = next(arguments, None)
            if value is None or value.startswith("-"):
                raise DiscoveryError(f"Expected a value after Maven option: {argument}")
            if argument in value_options:
                continue
            property_name = value.split("=", 1)[0]
        elif argument.startswith("-D"):
            property_name = argument[2:].split("=", 1)[0]
        elif argument.startswith("--define="):
            property_name = argument.partition("=")[2].split("=", 1)[0]
        else:
            if not argument.startswith("-") or argument == "--":
                raise DiscoveryError(f"Do not forward Maven lifecycle phases or goals: {argument}")
            continue
        if property_name.startswith(("test", "surefire.includes", "surefire.excludes", "dependenciesToScan")):
            raise DiscoveryError(f"Do not override test selection or project scope: {argument}")


def run_checks(runner, forwarded):
    check_forwarded_arguments(forwarded)
    # A build-cache hit restores production classes and reports, but not
    # test-classes. Compile once even when called after a successful CI build.
    # Only this guard-owned directory is reset; normal reports remain intact.
    if WORK.exists():
        shutil.rmtree(WORK)
    run_maven(runner, forwarded, ["-Ph2-in-memory", "test-compile"], "compile")
    prepare_canaries()
    for name, profiles, properties, expected in SCENARIOS:
        run_maven(runner, forwarded,
                  ["-P" + ",".join(["h2-in-memory", "testDiscovery", *profiles]),
                   f"-Dtest.discovery.scenario={name}", *properties, "surefire:test"], name)
        directory = WORK / "reports" / name
        try:
            check_reports(directory, expected)
        except DiscoveryError as error:
            raise DiscoveryError(f"Discovery scenario {name} failed ({directory}):\n{error}") from error
        print(f"PASS {name}: {len(expected)} successful testcases", flush=True)


class ReportCheckerTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="operaton-discovery-checker-")
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)

    def write_report(self, identities, filename="TEST-canaries.xml", status=None):
        suite = ET.Element("testsuite", tests=str(len(identities)), failures="0", errors="0", skipped="0")
        for class_name, method in identities:
            testcase = ET.SubElement(suite, "testcase", classname=class_name, name=method)
            if status:
                ET.SubElement(testcase, status)
        ET.ElementTree(suite).write(self.directory / filename, encoding="utf-8")

    def test_complete_report_passes(self):
        self.write_report(ALL_CASES)
        check_reports(self.directory, ALL_CASES)

    def test_missing_reports_fail(self):
        with self.assertRaises(DiscoveryError):
            check_reports(self.directory, ALL_CASES)

    def test_empty_report_fails(self):
        self.write_report([])
        with self.assertRaises(DiscoveryError):
            check_reports(self.directory, ALL_CASES)

    def test_nested_only_report_fails(self):
        self.write_report(NESTED_CASES)
        with self.assertRaises(DiscoveryError):
            check_reports(self.directory, ALL_CASES)

    def test_missing_parameterized_invocation_fails(self):
        self.write_report(ALL_CASES - {next(iter(UTIL_CASES))})
        with self.assertRaises(DiscoveryError):
            check_reports(self.directory, ALL_CASES)

    def test_failed_errored_or_skipped_case_fails(self):
        for status in ("failure", "error", "skipped"):
            with self.subTest(status=status):
                self.write_report(ALL_CASES, status=status)
                with self.assertRaises(DiscoveryError):
                    check_reports(self.directory, ALL_CASES)

    def test_unexpected_or_duplicate_case_fails(self):
        for extra in (next(iter(ALL_CASES)), (NASHORN, "testJavascriptProcessVarVisibility"),
                      (TEST_CASE + "$JobExecutorTest", "testBasicJobExecutorOperation")):
            with self.subTest(extra=extra):
                self.write_report([*ALL_CASES, extra])
                with self.assertRaises(DiscoveryError):
                    check_reports(self.directory, ALL_CASES)

    def test_nested_cases_in_separate_reports_pass(self):
        self.write_report(ALL_CASES - NESTED_CASES)
        for index, identity in enumerate(sorted(NESTED_CASES)):
            self.write_report([identity], f"TEST-nested-{index}.xml")
        check_reports(self.directory, ALL_CASES)

    def test_no_match_requires_zero_cases(self):
        check_reports(self.directory, set())
        self.write_report([])
        check_reports(self.directory, set())
        self.write_report(NESTED_CASES)
        with self.assertRaises(DiscoveryError):
            check_reports(self.directory, set())

    def test_parameterized_signature_is_normalized(self):
        self.assertEqual(normalized_method("method(Class, String)[4]"), "method[4]")
        self.assertEqual(normalized_method("method"), "method")

    def test_malformed_report_fails(self):
        (self.directory / "TEST-broken.xml").write_text("<testsuite", encoding="utf-8")
        with self.assertRaises(DiscoveryError):
            check_reports(self.directory, ALL_CASES)

    def test_suite_failure_without_case_fails(self):
        (self.directory / "TEST-broken.xml").write_text('<testsuite failures="1"/>', encoding="utf-8")
        with self.assertRaises(DiscoveryError):
            check_reports(self.directory, set())

    def test_unknown_report_format_fails(self):
        (self.directory / "TEST-broken.xml").write_text("<unknown/>", encoding="utf-8")
        with self.assertRaises(DiscoveryError):
            check_reports(self.directory, set())


class ForwardedArgumentTests(unittest.TestCase):
    def test_settings_repository_and_normal_options_pass(self):
        check_forwarded_arguments([
            "-s", "settings.xml", "--global-settings", "global-settings.xml",
            "-t", "toolchains.xml", "--global-toolchains=global-toolchains.xml",
            "-Dmaven.repo.local=/tmp/repository", "--offline", "-T", "1C", "--batch-mode",
            "--define", "user.language=en", "-D", "user.region=US", "-fae", "-ff", "-fn",
        ])

    def test_lifecycle_goals_and_scope_overrides_fail(self):
        for argument in ("clean", "test", "surefire:test", "-am", "--also-make", "-amd",
                         "--also-make-dependents", "-N", "--non-recursive", "-rf:operaton-engine",
                         "--resume-from=:operaton-engine", "-r", "--resume", "-PtestBpmn",
                         "-plengine", "--projects=engine", "-fengine/pom.xml", "--"):
            with self.subTest(argument=argument), self.assertRaises(DiscoveryError):
                check_forwarded_arguments([argument])

    def test_property_aliases_cannot_override_selection(self):
        for arguments in (["-Dtest=SomeTest"], ["-D", "test=SomeTest"],
                          ["--define=test=SomeTest"], ["--define", "surefire.includes=SomeTest"],
                          ["-Dtest.includes=bpmn"], ["-Dsurefire.excludes=SomeTest"],
                          ["-DdependenciesToScan=some:artifact"]):
            with self.subTest(arguments=arguments), self.assertRaises(DiscoveryError):
                check_forwarded_arguments(arguments)

    def test_missing_option_values_fail(self):
        for arguments in (["-s"], ["--settings", "--offline"], ["-D"]):
            with self.subTest(arguments=arguments), self.assertRaises(DiscoveryError):
                check_forwarded_arguments(arguments)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runner", default="./mvnw", help="Maven executable (default: ./mvnw)")
    parser.add_argument("--self-test", action="store_true", help="test the report checker without invoking Maven")
    parser.add_argument("maven_args", nargs=argparse.REMAINDER, help="Maven settings/options after --")
    options = parser.parse_args()
    forwarded = options.maven_args[1:] if options.maven_args[:1] == ["--"] else options.maven_args
    tests = unittest.TestSuite(unittest.defaultTestLoader.loadTestsFromTestCase(test_class)
                               for test_class in (ReportCheckerTests, ForwardedArgumentTests))
    result = unittest.TextTestRunner(verbosity=1).run(tests)
    if not result.wasSuccessful():
        return 1
    if options.self_test:
        return 0
    try:
        run_checks(options.runner, forwarded)
    except (DiscoveryError, OSError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print("Engine test discovery checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
