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

import org.operaton.bpm.engine.impl.pvm.PvmTransition;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.pvm.delegate.SignallableActivityBehavior;
import org.operaton.bpm.engine.migration.MigratingProcessInstanceValidationException;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.util.MigratingProcessInstanceValidationReportAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationSignallableServiceTaskTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @Test
  void testCannotMigrateActivityInstance() {
    // given
    BpmnModelInstance model = ProcessModels.newModel()
      .startEvent()
      .serviceTask("serviceTask")
      .operatonClass(SignallableServiceTaskDelegate.class.getName())
      .endEvent()
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("serviceTask", "serviceTask")
      .build();

    // when/then
    assertThatThrownBy(() -> testHelper.createProcessInstanceAndMigrate(migrationPlan))
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        var exception = (MigratingProcessInstanceValidationException) e;
        assertThat(exception.getValidationReport())
          .hasActivityInstanceFailures("serviceTask",
            "The type of the source activity is not supported for activity instance migration"
          );
      });
  }

  @Test
  void testCannotMigrateAsyncActivityInstance() {
    // given
    BpmnModelInstance model = ProcessModels.newModel()
      .startEvent()
      .serviceTask("serviceTask")
      .operatonAsyncBefore()
      .operatonClass(SignallableServiceTaskDelegate.class.getName())
      .endEvent()
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("serviceTask", "serviceTask")
      .build();

    String processInstanceId = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId()).getId();
    testHelper.executeAvailableJobs();
    var migrationBuilder = rule.getRuntimeService().newMigration(migrationPlan)
        .processInstanceIds(processInstanceId);

    // when/then
    assertThatThrownBy(migrationBuilder::execute)
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        var exception = (MigratingProcessInstanceValidationException) e;
        assertThat(exception.getValidationReport())
          .hasActivityInstanceFailures("serviceTask",
            "The type of the source activity is not supported for activity instance migration"
          );
      });
  }

  public static class SignallableServiceTaskDelegate implements SignallableActivityBehavior {

    @Override
    public void execute(ActivityExecution execution) throws Exception {
      // no-op
    }

    @Override
    public void signal(ActivityExecution execution, String signalEvent, Object signalData) throws Exception {
      PvmTransition transition = execution.getActivity().getOutgoingTransitions().get(0);
      execution.leaveActivityViaTransition(transition);
    }

  }
}
