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
"""Tests for the annotation-only diff guard."""

import importlib.util
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location(
    "guard", HERE / "annotation-only-diff.py")
guard = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guard)

CLEAN_DIFF = '''diff --git a/A.java b/A.java
--- a/A.java
+++ b/A.java
@@ -1,4 +1,6 @@
+import org.jspecify.annotations.NullMarked;
+import org.jspecify.annotations.Nullable;
-public class JobEntity {
+public @NullMarked class JobEntity {
-  protected String lockOwner;
+  protected @Nullable String lockOwner;
-  public String getLockOwner() {
+  public @Nullable String getLockOwner() {
'''

REQUIRE_NON_NULL_DIFF = '''diff --git a/A.java b/A.java
--- a/A.java
+++ b/A.java
@@ -1,2 +1,2 @@
-    return lockOwner;
+    return requireNonNull(lockOwner);
'''

GUARD_DIFF = '''diff --git a/A.java b/A.java
--- a/A.java
+++ b/A.java
@@ -1,2 +1,3 @@
+    if (parent == null) { return null; }
'''

WIDENING_DIFF = '''diff --git a/A.java b/A.java
--- a/A.java
+++ b/A.java
@@ -1,2 +1,2 @@
-  public void setJobId(String jobId) {
+  public void setJobId(@Nullable String jobId) {
'''


class AnnotationOnlyTest(unittest.TestCase):

    def test_clean_annotation_diff_passes(self):
        self.assertEqual(guard.check_diff(CLEAN_DIFF), [])

    def test_require_non_null_is_rejected(self):
        offenders = guard.check_diff(REQUIRE_NON_NULL_DIFF)
        self.assertEqual(len(offenders), 1)
        self.assertIn('requireNonNull', offenders[0][1])

    def test_added_null_guard_is_rejected(self):
        offenders = guard.check_diff(GUARD_DIFF)
        self.assertEqual(len(offenders), 1)

    def test_signature_change_that_only_adds_nullable_passes(self):
        self.assertEqual(guard.check_diff(WIDENING_DIFF), [])


class SubclassWideningTest(unittest.TestCase):
    """Review Focus 3: a subclass must not widen a parent's non-null setter."""

    def test_widening_detector_finds_conflict(self):
        parent = 'public void setJobId(String jobId) {'
        child = 'public void setJobId(@Nullable String jobId) {'
        self.assertTrue(guard.widens_parameter(parent, child))

    def test_matching_signatures_do_not_conflict(self):
        same = 'public void setJobId(String jobId) {'
        self.assertFalse(guard.widens_parameter(same, same))


if __name__ == '__main__':
    unittest.main()
