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

import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.migration.MigratingProcessInstanceValidationException;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.TransitionInstance;
import org.operaton.bpm.engine.test.api.mgmt.AlwaysFailingDelegate;
import org.operaton.bpm.engine.test.api.runtime.migration.models.AsyncProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.EventSubProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.MultiInstanceProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.operaton.bpm.engine.test.util.MigratingProcessInstanceValidationReportAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
@SuppressWarnings("java:S1874") // Use of synchronous execute() method is a acceptable in test code
class MigrationTransitionInstancesTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @AfterEach
  void resetClock() {
    ClockUtil.reset();
  }

  @Test
  void testMigrateAsyncBeforeTransitionInstance() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
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
        .transition("userTask")
      .done());

    testHelper.assertJobMigrated("userTask", "userTask", AsyncContinuationJobHandler.TYPE);

    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(job.getId());

    // and complete the task and process instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncBeforeTransitionInstanceChangeActivityId() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(
        modify(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS)
        .changeElementId("userTask", "userTaskReplacement"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask", "userTaskReplacement")
        .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertJobMigrated("userTask", "userTaskReplacement", AsyncContinuationJobHandler.TYPE);

    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(job.getId());

    // and complete the task and process instance
    testHelper.completeTask("userTaskReplacement");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncBeforeTransitionInstanceConcurrent() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask", "userTask")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .createProcessInstanceById(migrationPlan.getSourceProcessDefinitionId())
        .startBeforeActivity("userTask")
        .startBeforeActivity("userTask")
        .execute();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    TransitionInstance[] transitionInstances = testHelper.snapshotAfterMigration.getActivityTree().getTransitionInstances("userTask");

    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").concurrent().noScope().id(transitionInstances[0].getExecutionId()).up()
          .child("userTask").concurrent().noScope().id(transitionInstances[1].getExecutionId())
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .transition("userTask")
        .transition("userTask")
      .done());

    assertThat(testHelper.snapshotAfterMigration.getJobs()).hasSize(2);

    // and it is possible to successfully execute the migrated job
    for (Job job : testHelper.snapshotAfterMigration.getJobs()) {
      rule.getManagementService().executeJob(job.getId());
      testHelper.completeTask("userTask");
    }

    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncAfterTransitionInstance() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertJobMigrated("userTask1", "userTask1", AsyncContinuationJobHandler.TYPE);

    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(job.getId());

    // and complete the task and process instance
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncAfterTransitionInstanceChangeActivityId() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(
        modify(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS)
        .changeElementId("userTask1", "userTaskReplacement1")
        .changeElementId("userTask2", "userTaskReplacement2"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask1", "userTaskReplacement1")
        .mapActivities("userTask2", "userTaskReplacement2")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertJobMigrated("userTask1", "userTaskReplacement1", AsyncContinuationJobHandler.TYPE);

    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(job.getId());

    // and complete the task and process instance
    testHelper.completeTask("userTaskReplacement2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncBeforeTransitionInstanceRemoveIncomingFlow() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process")
        .startEvent()
        .serviceTask("serviceTask").operatonExpression("${true}")
        .userTask("userTask").operatonAsyncBefore()
        .endEvent()
        .done();
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(model)
        .removeFlowNode("serviceTask"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask", "userTask")
        .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertJobMigrated("userTask", "userTask", AsyncContinuationJobHandler.TYPE);

    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(job.getId());

    // and complete the task and process instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncBeforeTransitionInstanceAddIncomingFlow() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process")
        .startEvent()
        .serviceTask("serviceTask").operatonExpression("${true}")
        .userTask("userTask").operatonAsyncBefore()
        .endEvent()
        .done();
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(modify(model).removeFlowNode("serviceTask"));
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask", "userTask")
        .build();

    // when
    ProcessInstance processInstance = rule.getRuntimeService().createProcessInstanceById(sourceProcessDefinition.getId())
        .startBeforeActivity("userTask")
        .execute();

    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertJobMigrated("userTask", "userTask", AsyncContinuationJobHandler.TYPE);

    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(job.getId());

    // and complete the task and process instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncAfterTransitionInstanceRemoveOutgoingFlowCase1() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS)
        .removeFlowNode("endEvent")
        .removeFlowNode("userTask2"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertJobMigrated("userTask1", "userTask1", AsyncContinuationJobHandler.TYPE);

    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(job.getId());
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncAfterTransitionInstanceRemoveOutgoingFlowCase2() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_AFTER_SUBPROCESS_USER_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask1", "userTask1")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertJobMigrated("userTask1", "userTask1", AsyncContinuationJobHandler.TYPE);

    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(job.getId());

    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }


  @Test
  void testMigrateAsyncAfterTransitionInstanceAddOutgoingFlowCase1() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(modify(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS)
        .removeFlowNode("endEvent")
        .removeFlowNode("userTask2"));
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertJobMigrated("userTask1", "userTask1", AsyncContinuationJobHandler.TYPE);

    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(job.getId());

    // and complete the process instance
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncAfterTransitionInstanceAddOutgoingFlowCase2() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS)
        .activityBuilder("userTask1")
        .userTask("userTask3")
        .endEvent()
        .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertJobMigrated("userTask1", "userTask1", AsyncContinuationJobHandler.TYPE);


    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(job.getId());

    // and complete the process instance
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncAfterTransitionInstanceAddOutgoingFlowCase3() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS)
        .changeElementId("flow1", "flow2")
        .activityBuilder("userTask1")
        .sequenceFlowId("flow3")
        .userTask("userTask3")
        .endEvent()
        .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    var migrationBuilder = rule.getRuntimeService()
        .newMigration(migrationPlan)
        .processInstanceIds(processInstance.getId());

    // when then
    assertThatThrownBy(migrationBuilder::execute)
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        MigratingProcessInstanceValidationException ex = (MigratingProcessInstanceValidationException) e;
        assertThat(ex.getValidationReport())
          .hasTransitionInstanceFailures("userTask1",
            "Transition instance is assigned to a sequence flow that cannot be matched in the target activity"
          );
      });
  }

  @Test
  void testMigrateAsyncAfterTransitionInstanceReplaceOutgoingFlow() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS)
        .changeElementId("flow1", "flow2"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertJobMigrated("userTask1", "userTask1", AsyncContinuationJobHandler.TYPE);

    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(job.getId());

    // and complete the task and process instance
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateTransitionInstanceJobProperties() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(migrationPlan.getSourceProcessDefinitionId());

    Job jobBeforeMigration = rule.getManagementService().createJobQuery().singleResult();
    rule.getManagementService().setJobPriority(jobBeforeMigration.getId(), 42);

    Date newDueDate = new DateTime().plusHours(10).toDate();
    rule.getManagementService().setJobDuedate(jobBeforeMigration.getId(), newDueDate);
    rule.getManagementService().setJobRetries(jobBeforeMigration.getId(), 52);
    rule.getManagementService().suspendJobById(jobBeforeMigration.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);

    assertThat(job.getPriority()).isEqualTo(42);
    assertThat(job.getDuedate()).isCloseTo(newDueDate, 1000);
    assertThat(job.getRetries()).isEqualTo(52);
    assertThat(job.isSuspended()).isTrue();
  }

  @Test
  void testMigrateAsyncBeforeStartEventTransitionInstanceCase1() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_START_EVENT_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_START_EVENT_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("startEvent", "startEvent")
        .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertJobMigrated("startEvent", "startEvent", AsyncContinuationJobHandler.TYPE);

    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    assertThat(((JobEntity) job).getJobHandlerConfigurationRaw()).as("Replace this non-API assert with a proper test case that fails when the wrong atomic operation is used").isEqualTo("process-start");
    rule.getManagementService().executeJob(job.getId());

    // and complete the task and process instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncBeforeStartEventTransitionInstanceCase2() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_START_EVENT_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_SUBPROCESS_START_EVENT_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("startEvent", "subProcessStart")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    var migrationBuilder = rule.getRuntimeService()
        .newMigration(migrationPlan)
        .processInstanceIds(processInstance.getId());

    // when then
    assertThatThrownBy(migrationBuilder::execute)
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        MigratingProcessInstanceValidationException ex = (MigratingProcessInstanceValidationException) e;
        assertThat(ex.getValidationReport())
          .hasTransitionInstanceFailures("startEvent",
            "A transition instance that instantiates the process can only be migrated to a process-level flow node"
          );
      });
  }

  @Test
  void testMigrateAsyncBeforeStartEventTransitionInstanceCase3() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_SUBPROCESS_START_EVENT_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_SUBPROCESS_START_EVENT_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("subProcessStart", "subProcessStart")
        .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertJobMigrated("subProcessStart", "subProcessStart", AsyncContinuationJobHandler.TYPE);

    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(job.getId());

    // and complete the task and process instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncBeforeStartEventTransitionInstanceCase4() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_SUBPROCESS_START_EVENT_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_START_EVENT_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("subProcessStart", "startEvent")
        .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertJobMigrated("subProcessStart", "startEvent", AsyncContinuationJobHandler.TYPE);

    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(job.getId());

    // and complete the task and process instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncBeforeTransitionInstanceAddParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_SUBPROCESS_USER_TASK_PROCESS);

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
          .transition("userTask")
      .done());

    testHelper.assertJobMigrated("userTask", "userTask", AsyncContinuationJobHandler.TYPE);

    // and it is possible to successfully execute the migrated job
    Job job = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(job.getId());

    // and complete the task and process instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncBeforeTransitionInstanceConcurrentAddParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_SUBPROCESS_USER_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask", "userTask")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .createProcessInstanceById(migrationPlan.getSourceProcessDefinitionId())
        .startBeforeActivity("userTask")
        .startBeforeActivity("userTask")
        .execute();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope()
            .child("userTask").concurrent().noScope().up()
            .child("userTask").concurrent().noScope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess")
          .transition("userTask")
          .transition("userTask")
      .done());

    assertThat(testHelper.snapshotAfterMigration.getJobs()).hasSize(2);

    // and it is possible to successfully execute the migrated job
    for (Job job : testHelper.snapshotAfterMigration.getJobs()) {
      rule.getManagementService().executeJob(job.getId());
      testHelper.completeTask("userTask");
    }

    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncBeforeTransitionInstanceWithIncident() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS)
        .changeElementId("userTask", "newUserTask"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask", "newUserTask")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(migrationPlan.getSourceProcessDefinitionId());

    Job job = rule.getManagementService().createJobQuery().singleResult();
    rule.getManagementService().setJobRetries(job.getId(), 0);

    Incident incidentBeforeMigration = rule.getRuntimeService().createIncidentQuery().singleResult();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    Incident incidentAfterMigration = rule.getRuntimeService().createIncidentQuery().singleResult();

    assertThat(incidentAfterMigration).isNotNull();
    // and it is still the same incident
    assertThat(incidentAfterMigration.getId()).isEqualTo(incidentBeforeMigration.getId());
    assertThat(incidentAfterMigration.getConfiguration()).isEqualTo(job.getId());

    // and the activity and process definition references were updated
    assertThat(incidentAfterMigration.getActivityId()).isEqualTo("newUserTask");
    assertThat(incidentAfterMigration.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());

    // and it is possible to successfully execute the migrated job
    rule.getManagementService().executeJob(job.getId());

    // and complete the task and process instance
    testHelper.completeTask("newUserTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }


  @Test
  void testMigrateAsyncBeforeInnerMultiInstance() {
    // given
    BpmnModelInstance model = modify(MultiInstanceProcessModels.PAR_MI_ONE_TASK_PROCESS)
      .asyncBeforeInnerMiActivity("userTask");

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    List<Job> jobs = testHelper.snapshotAfterMigration.getJobs();
    assertThat(jobs).hasSize(3);

    testHelper.assertJobMigrated(jobs.get(0), "userTask");
    testHelper.assertJobMigrated(jobs.get(1), "userTask");
    testHelper.assertJobMigrated(jobs.get(2), "userTask");

    // and it is possible to successfully execute the migrated jobs
    for (Job job : jobs) {
      rule.getManagementService().executeJob(job.getId());
    }

    // and complete the task and process instance
    testHelper.completeAnyTask("userTask");
    testHelper.completeAnyTask("userTask");
    testHelper.completeAnyTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateAsyncAfterInnerMultiInstance() {
    // given
    BpmnModelInstance model = modify(MultiInstanceProcessModels.PAR_MI_ONE_TASK_PROCESS)
      .asyncAfterInnerMiActivity("userTask");

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(migrationPlan.getSourceProcessDefinitionId());

    testHelper.completeAnyTask("userTask");
    testHelper.completeAnyTask("userTask");
    testHelper.completeAnyTask("userTask");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    List<Job> jobs = testHelper.snapshotAfterMigration.getJobs();
    assertThat(jobs).hasSize(3);

    testHelper.assertJobMigrated(jobs.get(0), "userTask");
    testHelper.assertJobMigrated(jobs.get(1), "userTask");
    testHelper.assertJobMigrated(jobs.get(2), "userTask");

    // and it is possible to successfully execute the migrated jobs
    for (Job job : jobs) {
      rule.getManagementService().executeJob(job.getId());
    }

    // and complete the process instance
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testCannotMigrateAsyncBeforeTransitionInstanceToNonAsyncActivity() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    var migrationBuilder = rule.getRuntimeService()
        .newMigration(migrationPlan)
        .processInstanceIds(processInstance.getId());

    // when then
    assertThatThrownBy(migrationBuilder::execute)
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        MigratingProcessInstanceValidationException ex = (MigratingProcessInstanceValidationException) e;
        assertThat(ex.getValidationReport())
          .hasTransitionInstanceFailures("userTask",
            "Target activity is not asyncBefore"
          );
      });
  }

  @Test
  void testCannotMigrateAsyncAfterTransitionInstanceToNonAsyncActivity() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_AFTER_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask1", "userTask")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    testHelper.completeTask("userTask1");

    var migrationBuilder = rule.getRuntimeService()
        .newMigration(migrationPlan)
        .processInstanceIds(processInstance.getId());

    // when then
    assertThatThrownBy(migrationBuilder::execute)
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        MigratingProcessInstanceValidationException ex = (MigratingProcessInstanceValidationException) e;
        assertThat(ex.getValidationReport())
          .hasTransitionInstanceFailures("userTask1",
            "Target activity is not asyncAfter"
          );
      });
  }

  @Test
  void testCannotMigrateUnmappedTransitionInstance() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    var migrationBuilder = rule.getRuntimeService()
        .newMigration(migrationPlan)
        .processInstanceIds(processInstance.getId());

    // when then
    assertThatThrownBy(migrationBuilder::execute)
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        MigratingProcessInstanceValidationException ex = (MigratingProcessInstanceValidationException) e;
        assertThat(ex.getValidationReport())
          .hasTransitionInstanceFailures("userTask",
            "There is no migration instruction for this instance's activity"
          );
      });
  }

  @Test
  void testCannotMigrateUnmappedTransitionInstanceAtNonLeafActivity() {
    // given
    BpmnModelInstance model = modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
      .operatonAsyncBefore(true)
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    var migrationBuilder = rule.getRuntimeService()
        .newMigration(migrationPlan)
        .processInstanceIds(processInstance.getId());

    // when then
    assertThatThrownBy(migrationBuilder::execute)
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        MigratingProcessInstanceValidationException ex = (MigratingProcessInstanceValidationException) e;
        assertThat(ex.getValidationReport())
          .hasTransitionInstanceFailures("subProcess",
            "There is no migration instruction for this instance's activity"
          );
      });
  }

  @Test
  void testCannotMigrateUnmappedTransitionInstanceWithIncident() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);

    // the user task is not mapped in the migration plan, i.e. there is no instruction to migrate the job
    // and the incident
    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(migrationPlan.getSourceProcessDefinitionId());

    Job job = rule.getManagementService().createJobQuery().singleResult();
    rule.getManagementService().setJobRetries(job.getId(), 0);

    var migrationBuilder = rule.getRuntimeService()
        .newMigration(migrationPlan)
        .processInstanceIds(processInstance.getId());

    // when then
    assertThatThrownBy(migrationBuilder::execute)
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .satisfies(e -> {
        MigratingProcessInstanceValidationException ex = (MigratingProcessInstanceValidationException) e;
        assertThat(ex.getValidationReport())
          .hasTransitionInstanceFailures("userTask",
            "There is no migration instruction for this instance's activity"
          );
      });

  }

  @Test
  void testMigrateAsyncBeforeTransitionInstanceToDifferentProcessKey() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS)
        .changeElementId(ProcessModels.PROCESS_KEY, "new" + ProcessModels.PROCESS_KEY));

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertJobMigrated("userTask", "userTask", AsyncContinuationJobHandler.TYPE);
  }

  @Test
  void testMigrateAsyncAfterCompensateEventSubProcessStartEvent() {
    // given
    BpmnModelInstance model = modify(EventSubProcessModels.COMPENSATE_EVENT_SUBPROCESS_PROCESS)
        .flowNodeBuilder("eventSubProcessStart")
        .operatonAsyncAfter()
        .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("eventSubProcess", "eventSubProcess")
      .mapActivities("eventSubProcessStart", "eventSubProcessStart")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().createProcessInstanceById(sourceProcessDefinition.getId())
      .startBeforeActivity("eventSubProcess")
      .execute();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertJobMigrated("eventSubProcessStart", "eventSubProcessStart", AsyncContinuationJobHandler.TYPE);
  }

  @Disabled("Does not apply since asyncAfter cannot be used with boundary events")
  @Test
  void testMigrateAsyncAfterBoundaryEventWithChangedEventScope() {
    BpmnModelInstance sourceProcess = modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
      .activityBuilder("userTask1")
        .boundaryEvent("boundary").message("Message").operatonAsyncAfter()
        .userTask("afterBoundaryTask")
        .endEvent()
      .done();
    BpmnModelInstance targetProcess = modify(sourceProcess).swapElementIds("userTask1", "userTask2");

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetProcess);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("boundary", "boundary")
        .mapActivities("userTask1", "userTask1")
        .mapActivities("userTask2", "userTask2")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertJobMigrated("boundary", "boundary", AsyncContinuationJobHandler.TYPE);
  }

  @Test
  void testFailMigrateFailedJobIncident() {
    // given
    BpmnModelInstance model = ProcessModels.newModel()
      .startEvent()
      .serviceTask("serviceTask")
      .operatonAsyncBefore()
      .operatonClass(AlwaysFailingDelegate.class.getName())
      .endEvent()
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(model).changeElementId("serviceTask", "newServiceTask"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    String processInstanceId = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId()).getId();
    testHelper.executeAvailableJobs();
    var runtimeService = rule.getRuntimeService().newMigration(migrationPlan)
        .processInstanceIds(processInstanceId);

    // when
    assertThatThrownBy(runtimeService::execute)
      .isInstanceOf(MigratingProcessInstanceValidationException.class)
      .hasMessageContaining("ENGINE-23004 Cannot migrate process instance")
      .hasMessageContaining("Process instance contains not migrated incidents");
  }

}
