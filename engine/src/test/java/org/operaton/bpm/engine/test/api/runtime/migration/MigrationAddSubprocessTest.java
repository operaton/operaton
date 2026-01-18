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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.bpmn.multiinstance.DelegateEvent;
import org.operaton.bpm.engine.test.bpmn.multiinstance.DelegateExecutionListener;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.builder.ParallelGatewayBuilder;
import org.operaton.bpm.model.bpmn.instance.UserTask;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationAddSubprocessTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @Test
  void testScopeUserTaskMigration() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS);

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
          .child(null).scope()
            .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("userTask"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("subProcess")
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
  void testConcurrentScopeUserTaskMigration() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS_SUB_PROCESS);

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
          .child(null).scope()
            .child(null).concurrent().noScope()
              .child("userTask1").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("userTask1")).up().up()
            .child(null).concurrent().noScope()
              .child("userTask2").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("userTask2"))
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("subProcess")
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
  void testUserTaskMigration() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);

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
          .child("userTask").scope()
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("subProcess")
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
  void testConcurrentUserTaskMigration() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS);

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
          .child(null).scope()
            .child("userTask1").concurrent().noScope().up()
            .child("userTask2").concurrent().noScope()
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("subProcess")
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
  void testConcurrentThreeUserTaskMigration() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
        .<ParallelGatewayBuilder>getBuilderForElementById("fork")
        .userTask("userTask3")
        .endEvent()
        .done());
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS)
      .<ParallelGatewayBuilder>getBuilderForElementById("fork")
        .userTask("userTask3")
        .endEvent()
        .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask2")
      .mapActivities("userTask2", "userTask3")
      .mapActivities("userTask3", "userTask1")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope()
            .child("userTask1").concurrent().noScope().up()
            .child("userTask2").concurrent().noScope().up()
            .child("userTask3").concurrent().noScope()
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("subProcess")
            .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask3").getId())
            .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
            .activity("userTask3", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
        .done());

    List<Task> migratedTasks = testHelper.snapshotAfterMigration.getTasks();
    assertThat(migratedTasks).hasSize(3);

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
  void testNestedScopesMigration1() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.DOUBLE_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .mapActivities("subProcess", "outerSubProcess")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
            .child("userTask").scope()
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("outerSubProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
            .beginScope("innerSubProcess")
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
  void testNestedScopesMigration2() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.DOUBLE_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .mapActivities("subProcess", "innerSubProcess")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope()
            .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("outerSubProcess")
            .beginScope("innerSubProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
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
  void testMultipleInstancesOfScope() {
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.DOUBLE_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .mapActivities("subProcess", "outerSubProcess")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().createProcessInstanceById(sourceProcessDefinition.getId())
        .startBeforeActivity("subProcess")
        .startBeforeActivity("subProcess")
        .execute();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).concurrent().noScope()
            .child(null).scope()
              .child("userTask").scope().up().up().up()
          .child(null).concurrent().noScope()
            .child(null).scope()
              .child("userTask").scope()
        .done());

    ActivityInstance activityInstance = testHelper.snapshotBeforeMigration.getActivityTree();
    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("outerSubProcess", activityInstance.getActivityInstances("subProcess")[0].getId())
            .beginScope("innerSubProcess")
              .activity("userTask", activityInstance.getActivityInstances("subProcess")[0].getActivityInstances("userTask")[0].getId())
            .endScope()
          .endScope()
          .beginScope("outerSubProcess", activityInstance.getActivityInstances("subProcess")[1].getId())
            .beginScope("innerSubProcess")
              .activity("userTask", activityInstance.getActivityInstances("subProcess")[1].getActivityInstances("userTask")[0].getId())
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
  void testChangeActivityId() {
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask2")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .createProcessInstanceById(sourceProcessDefinition.getId())
        .startBeforeActivity("userTask1")
        .execute();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask2").scope()
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("subProcess")
            .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
        .done());

    Task migratedTask = testHelper.snapshotAfterMigration.getTaskForKey("userTask2");
    assertThat(migratedTask).isNotNull();
    assertThat(migratedTask.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());

    // and it is possible to successfully complete the migrated instance
    rule.getTaskService().complete(migratedTask.getId());
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testChangeScopeActivityId() {
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS_SUB_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask2")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .createProcessInstanceById(sourceProcessDefinition.getId())
        .startBeforeActivity("userTask1")
        .execute();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope()
            .child("userTask2").scope()
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("subProcess")
            .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
        .done());

    Task migratedTask = testHelper.snapshotAfterMigration.getTaskForKey("userTask2");
    assertThat(migratedTask).isNotNull();
    assertThat(migratedTask.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());

    // and it is possible to successfully complete the migrated instance
    rule.getTaskService().complete(migratedTask.getId());
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testListenerInvocationForNewlyCreatedScope() {
    // given
    DelegateEvent.clearEvents();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, DelegateExecutionListener.class.getName())
      .done()
    );

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
    assertThat(event.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());
    assertThat(event.getCurrentActivityId()).isEqualTo("subProcess");

    DelegateEvent.clearEvents();
  }

  @Test
  void testSkipListenerInvocationForNewlyCreatedScope() {
    // given
    DelegateEvent.clearEvents();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, DelegateExecutionListener.class.getName())
      .done()
    );

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
  void testIoMappingInvocationForNewlyCreatedScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
      .operatonInputParameter("foo", "bar")
      .done()
    );

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

    ActivityInstance activityInstance = rule.getRuntimeService().getActivityInstance(processInstance.getId());
    assertThat(inputVariable.getActivityInstanceId()).isEqualTo(activityInstance.getActivityInstances("subProcess")[0].getId());
  }

  @Test
  void testSkipIoMappingInvocationForNewlyCreatedScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
      .operatonInputParameter("foo", "bar")
      .done()
    );

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
  void testDeleteMigratedInstance() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS_SUB_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to delete the process instance
    String processInstanceId = testHelper.snapshotBeforeMigration.getProcessInstanceId();
    rule.getRuntimeService().deleteProcessInstance(processInstanceId, null);
    testHelper.assertProcessEnded(processInstanceId);
  }

  @Test
  @Disabled("Fails with 'Could not match expected tree'")
  void testAddParentScopeToMultiInstance() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(
      modify(ProcessModels.ONE_TASK_PROCESS)
        .<UserTask>getModelElementById("userTask").builder()
        .multiInstance()
          .parallel()
          .operatonCollection("collectionVar")
          .operatonElementVariable("elementVar")
        .done());
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(
      modify(ProcessModels.SUBPROCESS_PROCESS)
        .<UserTask>getModelElementById("userTask").builder()
        .multiInstance()
          .parallel()
          .operatonCollection("collectionVar")
          .operatonElementVariable("elementVar")
        .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask#multiInstanceBody", "userTask#multiInstanceBody")
      .mapActivities("userTask", "userTask")
      .build();

    List<String> miElements = new ArrayList<>();
    miElements.add("a");
    miElements.add("b");
    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId(),
            Variables.createVariables().putValue("collectionVar", miElements));

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("subProcess")
            .beginMiBody("userTask")
              .activity("userTask")
              .activity("userTask")
              .activity("userTask")
        .done());

    // the element variables still exist
    List<Task> migratedTasks = testHelper.snapshotAfterMigration.getTasks();
    assertThat(migratedTasks).hasSize(2);

    List<String> collectedElementsVars = new ArrayList<>();
    for (Task migratedTask : migratedTasks) {
      collectedElementsVars.add((String) rule.getTaskService().getVariable(migratedTask.getId(), "elementVar"));
    }

    assertThat(collectedElementsVars)
            .contains("a")
            .contains("b");

    // and it is possible to successfully complete the migrated instance
    for (Task migratedTask : migratedTasks) {
      rule.getTaskService().complete(migratedTask.getId());
    }

    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTwoScopes() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.DOUBLE_SUBPROCESS_PROCESS);

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
          .child(null).scope()
            .child("userTask").scope()
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("outerSubProcess")
            .beginScope("innerSubProcess")
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
  void testAddTwoConcurrentScopes() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.DOUBLE_PARALLEL_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then there is only one instance of outerSubProcess
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope()
            .child(null).concurrent().noScope()
              .child("userTask1").scope().up().up()
            .child(null).concurrent().noScope()
              .child("userTask2").scope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("outerSubProcess")
            .beginScope("innerSubProcess1")
              .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
            .endScope()
            .beginScope("innerSubProcess2")
              .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
        .done());

    List<Task> migratedTasks = testHelper.snapshotAfterMigration.getTasks();
    assertThat(migratedTasks).hasSize(2);

    // and it is possible to successfully complete the migrated instance
    for (Task migratedTask : migratedTasks) {
      rule.getTaskService().complete(migratedTask.getId());
    }
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testCanMigrateParentScopeWayTooHigh() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.TRIPLE_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess1")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then there is only one instance of outerSubProcess
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
            .child(null).scope()
              .child("userTask").scope()
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("subProcess1", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
            .beginScope("subProcess2")
              .beginScope("subProcess3")
                .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    Task migratedTask = testHelper.snapshotAfterMigration.getTaskForKey("userTask");
    assertThat(migratedTask).isNotNull();

    // and it is possible to successfully complete the migrated instance
    rule.getTaskService().complete(migratedTask.getId());
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMoveConcurrentActivityIntoSiblingScope() {

    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_TASK_AND_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS);

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
          .child(null).concurrent().noScope()
            .child("userTask2").scope().up().up()
          .child(null).concurrent().noScope()
            .child("userTask1").scope()
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("subProcess")
            .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
          .endScope()
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

  @Test
  void testAddScopeDoesNotBecomeAsync() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
        .activityBuilder("subProcess")
        .operatonAsyncBefore()
      .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then the async flag for the subprocess was not relevant for instantiation
    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginScope("subProcess")
            .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertThat(testHelper.snapshotAfterMigration.getJobs()).isEmpty();
  }

}
