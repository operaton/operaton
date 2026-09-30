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
"""Known-answer tests for entity-nullability-evidence, run against the real repo."""

import importlib.util
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]

spec = importlib.util.spec_from_file_location(
    "evidence", HERE / "entity-nullability-evidence.py")
evidence = importlib.util.module_from_spec(spec)
spec.loader.exec_module(evidence)


class DdlParsingTest(unittest.TestCase):

    def setUp(self):
        self.index = evidence.ddl_index(ROOT)

    def test_mysql_table_terminator_does_not_overrun(self):
        # mysql closes tables with ") ENGINE=InnoDB ...;" not ");".
        # A naive ^\); terminator swallows later tables and reports
        # extra NOT NULL columns for ACT_RU_EXT_TASK.
        cols = self.index['mysql']['ACT_RU_EXT_TASK']
        self.assertEqual(
            sorted(c for c, nn in cols.items() if nn),
            ['ID_', 'PRIORITY_', 'REV_'])

    def test_oracle_default_before_not_null_is_detected(self):
        # oracle writes "DEFAULT 1 NOT NULL", others "NOT NULL DEFAULT 1".
        self.assertTrue(
            self.index['oracle']['ACT_RU_JOB']['SUSPENSION_STATE_'])

    def test_primary_key_counts_as_not_null(self):
        # ACT_RU_TASK.ID_ is spelled NOT NULL only under db2, but every
        # dialect declares primary key (ID_).
        for dialect in evidence.DIALECTS:
            self.assertTrue(self.index[dialect]['ACT_RU_TASK']['ID_'], dialect)

    def test_unconstrained_column_is_false(self):
        self.assertFalse(self.index['h2']['ACT_RU_JOB']['LOCK_OWNER_'])

    def test_agreement_count(self):
        self.assertEqual(
            evidence.notnull_dialect_count(self.index, 'ACT_RU_JOB', 'ID_'), 7)
        self.assertEqual(
            evidence.notnull_dialect_count(self.index, 'ACT_RU_JOB', 'LOCK_OWNER_'), 0)
        # postgres alone leaves VAR_SCOPE_ unconstrained.
        self.assertEqual(
            evidence.notnull_dialect_count(self.index, 'ACT_RU_VARIABLE', 'VAR_SCOPE_'), 6)


class ResultMapTest(unittest.TestCase):

    def test_property_to_column(self):
        maps = evidence.parse_result_maps(
            ROOT / 'engine/src/main/resources/org/operaton/bpm/engine/impl'
                   '/mapping/entity/Job.xml')
        job = maps['org.operaton.bpm.engine.impl.persistence.entity.JobEntity']
        self.assertEqual(job.columns['id'], 'ID_')
        self.assertEqual(job.columns['lockOwner'], 'LOCK_OWNER_')
        self.assertEqual(job.table, 'ACT_RU_JOB')

    def test_discriminator_is_not_a_property(self):
        # TYPE_ appears only as a <discriminator>, so no field maps to it.
        maps = evidence.parse_result_maps(
            ROOT / 'engine/src/main/resources/org/operaton/bpm/engine/impl'
                   '/mapping/entity/Job.xml')
        job = maps['org.operaton.bpm.engine.impl.persistence.entity.JobEntity']
        self.assertNotIn('TYPE_', job.columns.values())

    def test_constructor_args_are_detected(self):
        maps = evidence.parse_result_maps(
            ROOT / 'engine/src/main/resources/org/operaton/bpm/engine/impl'
                   '/mapping/entity/Metrics.xml')
        key = ('org.operaton.bpm.engine.impl.persistence.entity'
               '.MetricIntervalEntity')
        self.assertEqual(
            sorted(maps[key].constructor_columns), ['INTERVAL_', 'NAME_', 'REPORTER_'])

    def test_reused_result_map_is_flagged(self):
        # jobResultMap is referenced many times in Job.xml; a select that
        # omits a NOT NULL column would feed null into a non-null setter.
        maps = evidence.parse_result_maps(
            ROOT / 'engine/src/main/resources/org/operaton/bpm/engine/impl'
                   '/mapping/entity/Job.xml')
        job = maps['org.operaton.bpm.engine.impl.persistence.entity.JobEntity']
        self.assertGreater(job.reference_count, 1)


class UpgradeCrossCheckTest(unittest.TestCase):

    def test_upgrade_relaxations_are_collected(self):
        relaxed = evidence.upgrade_relaxations(ROOT)
        self.assertIsInstance(relaxed, set)


if __name__ == '__main__':
    unittest.main()
