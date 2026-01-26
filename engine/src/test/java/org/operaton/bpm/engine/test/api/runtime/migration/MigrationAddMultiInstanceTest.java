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
import org.operaton.bpm.engine.test.api.runtime.migration.models.MultiInstanceProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

import static org.operaton.bpm.engine.test.util.MigrationPlanValidationReportAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationAddMultiInstanceTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @Test
  void testAddMultiInstanceBody() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_ONE_TASK_PROCESS);
    var migrationPlanBuilder = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("userTask",
            "Target activity 'userTask' is a descendant of multi-instance body 'userTask#multiInstanceBody' "
            + "that is not mapped from the source process definition."
          );
      });
  }

  @Test
  void testRemoveAndAddMultiInstanceBody() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_ONE_TASK_PROCESS);
    var migrationPlanBuilder = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("userTask",
            "Target activity 'userTask' is a descendant of multi-instance body 'userTask#multiInstanceBody' "
            + "that is not mapped from the source process definition."
          );
      });
  }

  @Test
  void testAddMultiInstanceBodyWithDeeperNestedMapping() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_SUBPROCESS_PROCESS);
    var migrationPlanBuilder = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        exception.printStackTrace();
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("userTask",
            "Target activity 'userTask' is a descendant of multi-instance body 'subProcess#multiInstanceBody' "
            + "that is not mapped from the source process definition."
          );
      });
  }


}
