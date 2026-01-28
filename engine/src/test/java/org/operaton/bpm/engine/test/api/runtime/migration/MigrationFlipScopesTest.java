/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.api.runtime.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.migration.MigrationPlanValidationException;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.engine.test.util.MigrationPlanValidationReportAssert;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationFlipScopesTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @Test
  void testCannotFlipAncestorScopes() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.DOUBLE_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.DOUBLE_SUBPROCESS_PROCESS);
    var migrationPlanBuilder = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("outerSubProcess", "innerSubProcess")
        .mapActivities("innerSubProcess", "outerSubProcess")
        .mapActivities("userTask", "userTask");

    // when then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        MigrationPlanValidationException ex = (MigrationPlanValidationException) e;
        MigrationPlanValidationReportAssert.assertThat(ex.getValidationReport())
          .hasInstructionFailures("innerSubProcess",
            "The closest mapped ancestor 'outerSubProcess' is mapped to scope 'innerSubProcess' which is not an ancestor of target scope 'outerSubProcess'"
          );
      });
  }
}
