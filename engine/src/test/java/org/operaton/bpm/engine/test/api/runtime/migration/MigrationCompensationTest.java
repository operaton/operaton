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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.migration.MigratingProcessInstanceValidationException;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.migration.MigrationPlanValidationException;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.api.runtime.migration.models.CompensationModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.operaton.bpm.engine.test.util.MigratingProcessInstanceValidationReportAssert.assertThat;
import static org.operaton.bpm.engine.test.util.MigrationPlanValidationReportAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationCompensationTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @Test
  void testCannotMigrateActivityInstanceForCompensationThrowingEvent() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");
    testHelper.completeTask("userTask2");

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("compensationEvent", "compensationEvent")
      .mapActivities("compensationHandler", "compensationHandler")
      .build();

    // when / then
    assertThatThrownBy(() -> testHelper.migrateProcessInstance(migrationPlan, processInstance))
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        var exception = (MigratingProcessInstanceValidationException) e;
        assertThat(exception.getValidationReport())
          .hasProcessInstanceId(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .hasActivityInstanceFailures("compensationEvent",
            "The type of the source activity is not supported for activity instance migration"
          );
      });
  }

  @Test
  void testCannotMigrateActivityInstanceForCancelEndEvent() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.TRANSACTION_COMPENSATION_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.TRANSACTION_COMPENSATION_MODEL);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask");

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("transactionEndEvent", "transactionEndEvent")
      .mapActivities("compensationHandler", "compensationHandler")
      .build();

    // when / then
    assertThatThrownBy(() -> testHelper.migrateProcessInstance(migrationPlan, processInstance))
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        var exception = (MigratingProcessInstanceValidationException) e;
        assertThat(exception.getValidationReport())
          .hasProcessInstanceId(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .hasActivityInstanceFailures("transactionEndEvent",
            "The type of the source activity is not supported for activity instance migration"
          );
      });
  }

  @Test
  void testCannotMigrateActiveCompensationWithoutInstructionForThrowingEventCase1() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");
    testHelper.completeTask("userTask2");

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("compensationHandler", "compensationHandler")
      .build();

    // when / then
    assertThatThrownBy(() -> testHelper.migrateProcessInstance(migrationPlan, processInstance))
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        var exception = (MigratingProcessInstanceValidationException) e;
        assertThat(exception.getValidationReport())
          .hasProcessInstanceId(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .hasActivityInstanceFailures("compensationEvent",
            "There is no migration instruction for this instance's activity",
            "The type of the source activity is not supported for activity instance migration"
          );
      });
  }

  @Test
  void testCannotMigrateActiveCompensationWithoutInstructionForThrowingEventCase2() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_END_EVENT_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_END_EVENT_MODEL);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");
    testHelper.completeTask("userTask2");

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("compensationHandler", "compensationHandler")
      .build();

    // when / then
    assertThatThrownBy(() -> testHelper.migrateProcessInstance(migrationPlan, processInstance))
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        var exception = (MigratingProcessInstanceValidationException) e;
        assertThat(exception.getValidationReport())
          .hasProcessInstanceId(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .hasActivityInstanceFailures("compensationEvent",
            "There is no migration instruction for this instance's activity",
            "The type of the source activity is not supported for activity instance migration"
          );
      });
  }

  @Test
  void testCannotMigrateWithoutMappingCompensationBoundaryEvents() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask2", "userTask2")
      .build();

    // when / then
    assertThatThrownBy(() -> testHelper.migrateProcessInstance(migrationPlan, processInstance))
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        var exception = (MigratingProcessInstanceValidationException) e;
        assertThat(exception.getValidationReport())
          .hasProcessInstanceId(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .hasActivityInstanceFailures(
              sourceProcessDefinition.getId(),
              "Cannot migrate subscription for compensation handler 'compensationHandler'. "
              + "There is no migration instruction for the compensation boundary event");
      });
  }

  @Test
  void testCannotRemoveCompensationEventSubscriptions() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask2", "userTask2")
      .build();

    // when / then
    assertThatThrownBy(() -> testHelper.migrateProcessInstance(migrationPlan, processInstance))
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        var exception = (MigratingProcessInstanceValidationException) e;
        assertThat(exception.getValidationReport())
          .hasProcessInstanceId(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .hasActivityInstanceFailures(
              sourceProcessDefinition.getId(),
              "Cannot migrate subscription for compensation handler 'compensationHandler'. "
              + "There is no migration instruction for the compensation boundary event");
      });
  }

  @Test
  void testCanRemoveCompensationBoundaryWithoutEventSubscriptions() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);
    testHelper.completeTask("userTask1");

    // then
    assertThat(testHelper.snapshotAfterMigration.getEventSubscriptions()).isEmpty();

    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCannotTriggerAddedCompensationForCompletedInstances() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testHelper.snapshotAfterMigration.getEventSubscriptions()).isEmpty();

    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCanTriggerAddedCompensationForActiveInstances() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask1")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.completeTask("userTask1");
    assertThat(rule.getRuntimeService().createEventSubscriptionQuery().count()).isOne();

    testHelper.completeTask("userTask2");
    testHelper.completeTask("compensationHandler");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCanMigrateWithCompensationSubscriptionsInMigratingScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

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
  void testCanMigrateWithCompensationSubscriptionsInMigratingScopeAssertActivityInstance() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // a migrated process instance
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // when triggering compensation
    testHelper.completeTask("userTask2");

    // then the activity instance tree is correct
    ActivityInstance activityInstance = rule.getRuntimeService().getActivityInstance(processInstance.getId());

    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("compensationEvent")
        .activity("compensationHandler")
      .done());
  }

  @Test
  void testCanMigrateWithCompensationSubscriptionsInMigratingScopeAssertExecutionTree() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

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
          .done());
  }

  @Test
  void testCanMigrateWithCompensationSubscriptionsInMigratingScopeChangeIds() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(CompensationModels.ONE_COMPENSATION_TASK_MODEL)
        .changeElementId("userTask1", "newUserTask1")
        .changeElementId("compensationBoundary", "newCompensationBoundary")
        .changeElementId("compensationHandler", "newCompensationHandler"));

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "newCompensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertEventSubscriptionMigrated("compensationHandler", "newCompensationHandler", null);

    // and the compensation can be triggered and completed
    testHelper.completeTask("userTask2");
    testHelper.completeTask("newCompensationHandler");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCanMigrateWithCompensationEventScopeExecution() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("subProcess", "subProcess")
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertEventSubscriptionMigrated("subProcess", "subProcess", null);
    testHelper.assertEventSubscriptionMigrated("compensationHandler", "compensationHandler", null);

    // and the compensation can be triggered and completed
    testHelper.completeTask("userTask2");
    testHelper.completeTask("compensationHandler");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCanMigrateWithCompensationEventScopeExecutionAssertActivityInstance() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("subProcess", "subProcess")
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // when
    testHelper.completeTask("userTask2");

    // then
    ActivityInstance activityInstance = rule.getRuntimeService().getActivityInstance(processInstance.getId());

    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("compensationEvent")
        .beginScope("subProcess")
          .activity("compensationHandler")
      .done());
  }

  @Test
  void testCanMigrateWithCompensationEventScopeExecutionAssertExecutionTree() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("subProcess", "subProcess")
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    Execution eventScopeExecution = rule.getRuntimeService()
      .createExecutionQuery()
      .activityId("subProcess")
      .singleResult();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree("userTask2").scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("subProcess").scope().eventScope().id(eventScopeExecution.getId())
          .done());
  }

  @Test
  void testCanMigrateWithCompensationEventScopeExecutionChangeIds() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL)
        .changeElementId("subProcess", "newSubProcess")
        .changeElementId("userTask1", "newUserTask1")
        .changeElementId("compensationBoundary", "newCompensationBoundary")
        .changeElementId("compensationHandler", "newCompensationHandler"));

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("subProcess", "newSubProcess")
        .mapActivities("compensationBoundary", "newCompensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertEventSubscriptionMigrated("subProcess", "newSubProcess", null);
    testHelper.assertEventSubscriptionMigrated("compensationHandler", "newCompensationHandler", null);

    // and the compensation can be triggered and completed
    testHelper.completeTask("userTask2");
    testHelper.completeTask("newCompensationHandler");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCanMigrateWithCompensationEventScopeExecutionChangeIdsAssertActivityInstance() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL)
        .changeElementId("subProcess", "newSubProcess")
        .changeElementId("userTask1", "newUserTask1")
        .changeElementId("compensationBoundary", "newCompensationBoundary")
        .changeElementId("compensationHandler", "newCompensationHandler"));

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("subProcess", "newSubProcess")
        .mapActivities("compensationBoundary", "newCompensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // when
    testHelper.completeTask("userTask2");

    // then
    ActivityInstance activityInstance = rule.getRuntimeService().getActivityInstance(processInstance.getId());

    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("compensationEvent")
        .beginScope("newSubProcess")
          .activity("newCompensationHandler")
      .done());
  }

  @Test
  void testCanMigrateWithCompensationEventScopeExecutionChangeIdsAssertExecutionTree() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL)
        .changeElementId("subProcess", "newSubProcess")
        .changeElementId("userTask1", "newUserTask1")
        .changeElementId("compensationBoundary", "newCompensationBoundary")
        .changeElementId("compensationHandler", "newCompensationHandler"));

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("subProcess", "newSubProcess")
        .mapActivities("compensationBoundary", "newCompensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    Execution eventScopeExecution = rule.getRuntimeService()
      .createExecutionQuery()
      .activityId("subProcess")
      .singleResult();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree("userTask2").scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("newSubProcess").scope().eventScope().id(eventScopeExecution.getId())
          .done());

  }

  @Test
  void testCanMigrateEventScopeVariables() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("subProcess", "subProcess")
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    Execution subProcessExecution = rule.getRuntimeService()
        .createExecutionQuery()
        .activityId("userTask1")
        .singleResult();
    rule.getRuntimeService().setVariableLocal(subProcessExecution.getId(), "foo", "bar");

    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("foo");
    testHelper.assertVariableMigratedToExecution(beforeMigration, beforeMigration.getExecutionId());

    // and the compensation can be triggered and completed
    testHelper.completeTask("userTask2");
    testHelper.completeTask("compensationHandler");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCanMigrateWithEventSubProcessHandler() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("subProcess", "subProcess")
        .mapActivities("eventSubProcessStart", "eventSubProcessStart")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertEventSubscriptionMigrated("eventSubProcess", "eventSubProcess", null);

    // and the compensation can be triggered and completed
    testHelper.completeTask("userTask2");
    testHelper.completeTask("eventSubProcessTask");
    testHelper.completeTask("compensationHandler");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCanMigrateWithEventSubProcessHandlerAssertActivityInstance() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("subProcess", "subProcess")
        .mapActivities("eventSubProcessStart", "eventSubProcessStart")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    testHelper.completeTask("userTask1");
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // when compensation is triggered
    testHelper.completeTask("userTask2");

    // then
    ActivityInstance activityInstance = rule.getRuntimeService().getActivityInstance(processInstance.getId());

    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("compensationEvent")
        .beginScope("subProcess")
          .beginScope("eventSubProcess")
            .activity("eventSubProcessTask")
      .done());
  }

  @Test
  void testCanMigrateWithEventSubProcessHandlerAssertExecutionTree() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("subProcess", "subProcess")
        .mapActivities("eventSubProcessStart", "eventSubProcessStart")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    testHelper.completeTask("userTask1");

    Execution eventScopeExecution = rule.getRuntimeService()
      .createExecutionQuery()
      .activityId("subProcess")
      .singleResult();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree("userTask2").scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("subProcess").scope().eventScope().id(eventScopeExecution.getId())
          .done());

  }

  @Test
  void testCanMigrateWithEventSubProcessHandlerChangeIds() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL)
        .changeElementId("eventSubProcess", "newEventSubProcess"));

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .mapActivities("subProcess", "subProcess")
        .mapActivities("eventSubProcessStart", "eventSubProcessStart")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertEventSubscriptionMigrated("eventSubProcess", "newEventSubProcess", null);

    // and the compensation can be triggered and completed
    testHelper.completeTask("userTask2");
    testHelper.completeTask("eventSubProcessTask");
    testHelper.completeTask("compensationHandler");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCanMigrateSiblingEventScopeExecutions() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.DOUBLE_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("subProcess", "outerSubProcess")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    // starting a second instances of the sub process
    rule.getRuntimeService().createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("subProcess")
      .execute();

    List<Execution> subProcessExecutions = rule.getRuntimeService().createExecutionQuery().activityId("userTask1").list();
    for (Execution subProcessExecution : subProcessExecutions) {
      // set the same variable to a distinct value
      rule.getRuntimeService().setVariableLocal(subProcessExecution.getId(), "var", subProcessExecution.getId());
    }

    testHelper.completeAnyTask("userTask1");
    testHelper.completeAnyTask("userTask1");


    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then the variable snapshots during compensation are not shared
    testHelper.completeAnyTask("userTask2");

    List<Task> compensationTasks = rule.getTaskService()
        .createTaskQuery()
        .taskDefinitionKey("compensationHandler")
        .list();
    assertThat(compensationTasks).hasSize(2);

    Object value1 = rule.getTaskService().getVariable(compensationTasks.get(0).getId(), "var");
    Object value2 = rule.getTaskService().getVariable(compensationTasks.get(1).getId(), "var");
    assertThat(value2).isNotEqualTo(value1);
  }

  @Test
  void testCannotMigrateWithoutCompensationStartEventCase1() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask2", "userTask2")
      .mapActivities("compensationBoundary", "compensationBoundary")
      .build();

    // when / then
    assertThatThrownBy(() -> testHelper.migrateProcessInstance(migrationPlan, processInstance))
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        var exception = (MigratingProcessInstanceValidationException) e;
        assertThat(exception.getValidationReport())
          .hasProcessInstanceId(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .hasActivityInstanceFailures(sourceProcessDefinition.getId(),
              "Cannot migrate subscription for compensation handler 'eventSubProcess'. "
              + "There is no migration instruction for the compensation start event");
      });
  }

  @Test
  void testCannotMigrateWithoutCompensationStartEventCase2() {
    // given
    BpmnModelInstance model = modify(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL)
        .removeFlowNode("compensationBoundary");

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when / then
    assertThatThrownBy(() -> testHelper.migrateProcessInstance(migrationPlan, processInstance))
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        var exception = (MigratingProcessInstanceValidationException) e;
        assertThat(exception.getValidationReport())
          .hasProcessInstanceId(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .hasActivityInstanceFailures(sourceProcessDefinition.getId(),
              "Cannot migrate subscription for compensation handler 'eventSubProcess'. "
              + "There is no migration instruction for the compensation start event");
      });
  }

  @Test
  void testEventScopeHierarchyPreservation() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.DOUBLE_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.DOUBLE_SUBPROCESS_MODEL);
    var migrationBuilder = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("outerSubProcess", "innerSubProcess")
        .mapActivities("innerSubProcess", "outerSubProcess");

    // when / then
    assertThatThrownBy(migrationBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("innerSubProcess",
            "The closest mapped ancestor 'outerSubProcess' is mapped to scope 'innerSubProcess' "
            + "which is not an ancestor of target scope 'outerSubProcess'"
          );
      });
  }

  @Test
  void testCompensationBoundaryHierarchyPreservation() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL)
        .addSubProcessTo(ProcessModels.PROCESS_KEY)
          .id("addedSubProcess")
          .embeddedSubProcess()
          .startEvent()
          .endEvent()
        .done());
    var migrationBuilder = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("subProcess", "addedSubProcess")
        .mapActivities("compensationBoundary", "compensationBoundary");

    // when / then
    assertThatThrownBy(migrationBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("compensationBoundary",
            "The closest mapped ancestor 'subProcess' is mapped to scope 'addedSubProcess' "
            + "which is not an ancestor of target scope 'compensationBoundary'"
          );
      });
  }

  @Test
  void testCannotMapCompensateStartEventWithoutMappingEventScopeCase1() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL);
    var migrationBuilder = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("eventSubProcessStart", "eventSubProcessStart");

    // when / then
    assertThatThrownBy(migrationBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("eventSubProcessStart",
            "The source activity's event scope (subProcess) must be mapped to the target activity's event scope (subProcess)"
          );
      });
  }

  @Test
  void testCannotMapCompensateStartEventWithoutMappingEventScopeCase2() {
    // given
    BpmnModelInstance model = modify(CompensationModels.COMPENSATION_EVENT_SUBPROCESS_MODEL)
        .removeFlowNode("compensationBoundary");

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);
    var migrationBuilder = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("eventSubProcessStart", "eventSubProcessStart");

    // when / then
    assertThatThrownBy(migrationBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("eventSubProcessStart",
            "The source activity's event scope (subProcess) must be mapped to the target activity's event scope (subProcess)"
          );
      });
  }
}
