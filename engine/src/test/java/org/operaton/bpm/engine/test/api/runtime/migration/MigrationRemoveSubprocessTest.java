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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.migration.MigrationPlanValidationException;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.bpmn.multiinstance.DelegateEvent;
import org.operaton.bpm.engine.test.bpmn.multiinstance.DelegateExecutionListener;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.engine.test.util.MigrationPlanValidationReportAssert;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationRemoveSubprocessTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @Test
  void testRemoveScopeForNonScopeActivity() {

    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree("userTask").scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    Task migratedTask = testHelper.snapshotAfterMigration.getTaskForKey("userTask");
    assertThat(migratedTask).isNotNull();
    assertThat(migratedTask.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());

    // and it is possible to successfully complete the migrated instance
    rule.getTaskService().complete(migratedTask.getId());
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testRemoveScopeForScopeActivity() {

    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("userTask"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    Task migratedTask = testHelper.snapshotAfterMigration.getTaskForKey("userTask");
    assertThat(migratedTask).isNotNull();
    assertThat(migratedTask.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());

    // and it is possible to successfully complete the migrated instance
    rule.getTaskService().complete(migratedTask.getId());
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testRemoveScopeForConcurrentNonScopeActivity() {

    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask1").concurrent().noScope().up()
          .child("userTask2").concurrent().noScope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
          .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
        .done());

    List<Task> migratedTasks = testHelper.snapshotAfterMigration.getTasks();
    assertThat(migratedTasks).hasSize(2);
    for (Task migratedTask : migratedTasks) {
      assertThat(migratedTask.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());
    }

    // and it is possible to successfully complete the migrated instance
    for (Task migratedTask : migratedTasks) {
      rule.getTaskService().complete(migratedTask.getId());
    }
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testRemoveScopeForConcurrentScopeActivity() {

    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS_SUB_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).concurrent().noScope()
            .child("userTask1").scope().up().up()
          .child(null).concurrent().noScope()
            .child("userTask2").scope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
          .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
        .done());

    List<Task> migratedTasks = testHelper.snapshotAfterMigration.getTasks();
    assertThat(migratedTasks).hasSize(2);
    for (Task migratedTask : migratedTasks) {
      assertThat(migratedTask.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());
    }

    // and it is possible to successfully complete the migrated instance
    for (Task migratedTask : migratedTasks) {
      rule.getTaskService().complete(migratedTask.getId());
    }
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }


  @Test
  void testRemoveConcurrentScope() {

    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask")
      .mapActivities("userTask2", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").concurrent().noScope().up()
          .child("userTask").concurrent().noScope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
        .done());

    List<Task> migratedTasks = testHelper.snapshotAfterMigration.getTasks();
    assertThat(migratedTasks).hasSize(2);
    for (Task migratedTask : migratedTasks) {
      assertThat(migratedTask.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());
    }

    // and it is possible to successfully complete the migrated instance
    for (Task migratedTask : migratedTasks) {
      rule.getTaskService().complete(migratedTask.getId());
    }
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testRemoveConcurrentScope2() {

    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_TASK_AND_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess1", "subProcess")
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask2").concurrent().noScope().up()
          .child(null).concurrent().noScope()
          .child("userTask1").scope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
          .beginScope("subProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess1").getId())
            .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
        .done());

    List<Task> migratedTasks = testHelper.snapshotAfterMigration.getTasks();
    assertThat(migratedTasks).hasSize(2);
    for (Task migratedTask : migratedTasks) {
      assertThat(migratedTask.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());
    }

    // and it is possible to successfully complete the migrated instance
    for (Task migratedTask : migratedTasks) {
      rule.getTaskService().complete(migratedTask.getId());
    }
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  @Disabled("Missing feature CAM-5407")
  void testRemoveScopeAndMoveToConcurrentActivity() {

    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_TASK_AND_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask2").concurrent().noScope().up()
          .child(null).concurrent().noScope()
            .child("userTask1").scope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
          .beginScope("subProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
            .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
        .done());

    List<Task> migratedTasks = testHelper.snapshotAfterMigration.getTasks();
    assertThat(migratedTasks).hasSize(2);
    for (Task migratedTask : migratedTasks) {
      assertThat(migratedTask.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());
    }

    // and it is possible to successfully complete the migrated instance
    for (Task migratedTask : migratedTasks) {
      rule.getTaskService().complete(migratedTask.getId());
    }
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  /**
   * Remove when implementing CAM-5407
   */
  @Test
  void testCannotRemoveScopeAndMoveToConcurrentActivity() {

    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_TASK_AND_SUBPROCESS_PROCESS);
    var migrationPlanBuilder = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("subProcess", "subProcess")
        .mapActivities("userTask1", "userTask1")
        .mapActivities("userTask2", "userTask2");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        MigrationPlanValidationReportAssert.assertThat(exception.getValidationReport())
          .hasInstructionFailures("userTask2",
            "The closest mapped ancestor 'subProcess' is mapped to scope 'subProcess' which is not an ancestor of target scope 'userTask2'"
          );
      });
  }

  @Test
  void testRemoveMultipleScopes() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.DOUBLE_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree("userTask").scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .done());


    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    Task migratedTask = testHelper.snapshotAfterMigration.getTaskForKey("userTask");
    assertThat(migratedTask).isNotNull();
    assertThat(migratedTask.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());

    // and it is possible to successfully complete the migrated instance
    rule.getTaskService().complete(migratedTask.getId());
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }


  @Test
  void testEndListenerInvocationForRemovedScope() {
    // given
    DelegateEvent.clearEvents();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, DelegateExecutionListener.class.getName())
      .done()
    );
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    List<DelegateEvent> recordedEvents = DelegateEvent.getEvents();
    assertThat(recordedEvents).hasSize(1);

    DelegateEvent event = recordedEvents.get(0);
    assertThat(event.getProcessDefinitionId()).isEqualTo(sourceProcessDefinition.getId());
    assertThat(event.getCurrentActivityId()).isEqualTo("subProcess");
    assertThat(event.getActivityInstanceId()).isEqualTo(testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId());

    DelegateEvent.clearEvents();
  }

  @Test
  void testSkipListenerInvocationForRemovedScope() {
    // given
    DelegateEvent.clearEvents();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, DelegateExecutionListener.class.getName())
      .done()
    );
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(migrationPlan.getSourceProcessDefinitionId());
    rule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .skipCustomListeners()
      .execute();

    // then
    List<DelegateEvent> recordedEvents = DelegateEvent.getEvents();
    assertThat(recordedEvents).isEmpty();

    DelegateEvent.clearEvents();
  }

  @Test
  void testIoMappingInvocationForRemovedScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
      .operatonOutputParameter("foo", "bar")
      .done());
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    ProcessInstance processInstance = rule.getRuntimeService()
      .startProcessInstanceById(migrationPlan.getSourceProcessDefinitionId());
    rule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .execute();

    // then
    VariableInstance inputVariable = rule.getRuntimeService().createVariableInstanceQuery().singleResult();
    assertThat(inputVariable).isNotNull();
    assertThat(inputVariable.getName()).isEqualTo("foo");
    assertThat(inputVariable.getValue()).isEqualTo("bar");
    assertThat(inputVariable.getActivityInstanceId()).isEqualTo(processInstance.getId());
  }

  @Test
  void testSkipIoMappingInvocationForRemovedScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
      .operatonOutputParameter("foo", "bar")
      .done());
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    ProcessInstance processInstance = rule.getRuntimeService()
      .startProcessInstanceById(migrationPlan.getSourceProcessDefinitionId());
    rule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .skipIoMappings()
      .execute();

    // then
    assertThat(rule.getRuntimeService().createVariableInstanceQuery().count()).isZero();
  }


  @Test
  void testCannotRemoveParentScopeAndMoveOutOfGrandParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.TRIPLE_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.TRIPLE_SUBPROCESS_PROCESS);
    var migrationPlanBuilder = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("subProcess1", "subProcess1")
        .mapActivities("subProcess3", "subProcess1")
        .mapActivities("userTask", "userTask");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        MigrationPlanValidationReportAssert.assertThat(exception.getValidationReport())
          .hasInstructionFailures("subProcess3",
            "The closest mapped ancestor 'subProcess1' is mapped to scope 'subProcess1' which is not an ancestor of target scope 'subProcess1'"
          );
      });
  }

}
