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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.migration.MigrationPlanValidationException;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.api.runtime.migration.models.CompensationModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.operaton.bpm.engine.test.util.MigrationPlanValidationReportAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationCompensationAddSubProcessTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @Test
  void testCase1() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_TWO_TASKS_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertEventSubscriptionMigrated("compensationHandler", "compensationHandler", null);

    // and the compensation can be triggered and completed
    testHelper.completeTask("userTask2");
    testHelper.completeTask("compensationHandler");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  /**
   * The guarantee given by the API is: Compensation can be triggered in the scope that it could be triggered before
   *   migration. Thus, it should not be possible to trigger compensation from the new sub process instance but only from the
   *   parent scope, i.e. the process definition instance
   */
  @Test
  void testCase1CannotTriggerCompensationInNewScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(
        modify(CompensationModels.COMPENSATION_TWO_TASKS_SUBPROCESS_MODEL)
        .endEventBuilder("subProcessEnd")
          .compensateEventDefinition()
          .waitForCompletion(true)
          .compensateEventDefinitionDone()
        .done());

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then compensation is only caught outside of the subProcess
    testHelper.completeTask("userTask2");

    ActivityInstance activityInstance = rule.getRuntimeService().getActivityInstance(processInstance.getId());

    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("compensationEvent") // note: this is not subProcess and subProcessEnd
        .beginScope("subProcess")
          .activity("compensationHandler")
      .done());
  }

  @Test
  void testCase1AssertExecutionTree() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_TWO_TASKS_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then the execution tree is correct
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask2").scope().up()
          .child("subProcess").scope().eventScope()
        .done());
  }

  @Test
  void testCase2() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertEventSubscriptionMigrated("compensationHandler", "compensationHandler", null);

    // and the compensation can be triggered and completed
    testHelper.completeTask("userTask2");
    testHelper.completeTask("compensationHandler");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCase2AssertExecutionTree() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree("userTask2").scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("subProcess").scope().eventScope()
        .done());
  }

  @Test
  void testCase2AssertActivityInstance() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // when throwing compensation
    testHelper.completeTask("userTask2");

    // then the activity instance tree is correct
    ActivityInstance activityInstance = rule.getRuntimeService().getActivityInstance(processInstance.getId());

    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("compensationEvent")
        .beginScope("subProcess")
          .activity("compensationHandler")
      .done());
  }

  @Test
  void testNoListenersCalled() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL)
      .activityBuilder("subProcess")
        .operatonExecutionListenerExpression(
            ExecutionListener.EVENTNAME_START,
            "${execution.setVariable('foo', 'bar')}")
      .done());

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testHelper.snapshotAfterMigration.getVariables()).isEmpty();
  }

  @Disabled("CAM-6035")
  @Test
  void testNoInputMappingExecuted() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL)
      .activityBuilder("subProcess")
        .operatonInputParameter("foo", "bar")
      .done());

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testHelper.snapshotAfterMigration.getVariables()).isEmpty();
  }

  @Test
  void testVariablesInParentEventScopeStillAccessible() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.DOUBLE_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("subProcess", "outerSubProcess")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .mapActivities("userTask2", "userTask2")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    Execution subProcessExecution = rule.getRuntimeService().createExecutionQuery().activityId("userTask1").singleResult();
    rule.getRuntimeService().setVariableLocal(subProcessExecution.getId(), "foo", "bar");

    testHelper.completeTask("userTask1");

    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // when throwing compensation
    testHelper.completeAnyTask("userTask2");

    // then the variable snapshot is available
    Task compensationTask = rule.getTaskService().createTaskQuery().singleResult();
    assertThat(rule.getTaskService().getVariable(compensationTask.getId(), "foo")).isEqualTo("bar");
  }

  @Test
  void testCannotAddScopeOnTopOfEventSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(CompensationModels.DOUBLE_SUBPROCESS_MODEL)
        .addSubProcessTo("innerSubProcess")
        .id("eventSubProcess")
        .triggerByEvent()
        .embeddedSubProcess()
        .startEvent("eventSubProcessStart")
          .compensateEventDefinition()
          .compensateEventDefinitionDone()
        .endEvent()
        .done());
    var migrationPlanBuilder = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("subProcess", "outerSubProcess")
        .mapActivities("eventSubProcessStart", "eventSubProcessStart")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .mapActivities("userTask2", "userTask2");

    // when then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        MigrationPlanValidationException ex = (MigrationPlanValidationException) e;
        assertThat(ex.getValidationReport())
          .hasInstructionFailures("eventSubProcessStart",
            "The source activity's event scope (subProcess) must be mapped to the target activity's event scope (innerSubProcess)"
          );
      });
  }

}
