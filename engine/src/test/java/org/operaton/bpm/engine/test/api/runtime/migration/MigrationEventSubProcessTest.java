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
import java.util.HashMap;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.impl.jobexecutor.TimerStartEventSubprocessJobHandler;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.ActivityStatistics;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.migration.MigrationPlanValidationException;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.EventSubProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.engine.test.util.ClockTestUtil;
import org.operaton.bpm.engine.test.util.MigrationPlanValidationReportAssert;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.impl.migration.validation.instruction.ConditionalEventUpdateEventTriggerValidator.MIGRATION_CONDITIONAL_VALIDATION_ERROR_MSG;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.EventSubProcessModels.CONDITIONAL_EVENT_SUBPROCESS_PROCESS;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MigrationEventSubProcessTest {

  public static final String SIGNAL_NAME = "Signal";
  protected static final String EVENT_SUB_PROCESS_START_ID = "eventSubProcessStart";
  protected static final String EVENT_SUB_PROCESS_TASK_ID = "eventSubProcessTask";
  protected static final String USER_TASK_ID = "userTask";

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @Test
  void testMigrateActiveEventSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.MESSAGE_EVENT_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.MESSAGE_EVENT_SUBPROCESS_PROCESS);

    ProcessInstance processInstance = rule.getRuntimeService()
      .createProcessInstanceById(sourceProcessDefinition.getId())
      .startBeforeActivity(EVENT_SUB_PROCESS_TASK_ID)
      .execute();

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("eventSubProcess", "eventSubProcess")
        .mapActivities(EVENT_SUB_PROCESS_TASK_ID, EVENT_SUB_PROCESS_TASK_ID)
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
    .hasProcessDefinitionId(targetProcessDefinition.getId())
    .matches(
      describeExecutionTree(null).scope().id(processInstance.getId())
        .child(EVENT_SUB_PROCESS_TASK_ID).scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("eventSubProcess"))
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("eventSubProcess", testHelper.getSingleActivityInstanceBeforeMigration("eventSubProcess").getId())
          .activity(EVENT_SUB_PROCESS_TASK_ID, testHelper.getSingleActivityInstanceBeforeMigration(EVENT_SUB_PROCESS_TASK_ID).getId())
        .done());

    testHelper.assertEventSubscriptionRemoved(EVENT_SUB_PROCESS_START_ID, EventSubProcessModels.MESSAGE_NAME);
    testHelper.assertEventSubscriptionCreated(EVENT_SUB_PROCESS_START_ID, EventSubProcessModels.MESSAGE_NAME);

    // and it is possible to complete the process instance
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateActiveEventSubProcessToEmbeddedSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.MESSAGE_EVENT_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);

    ProcessInstance processInstance = rule.getRuntimeService()
      .createProcessInstanceById(sourceProcessDefinition.getId())
      .startBeforeActivity(EVENT_SUB_PROCESS_TASK_ID)
      .execute();

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("eventSubProcess", "subProcess")
        .mapActivities(EVENT_SUB_PROCESS_TASK_ID, USER_TASK_ID)
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
    .hasProcessDefinitionId(targetProcessDefinition.getId())
    .matches(
      describeExecutionTree(null).scope().id(processInstance.getId())
        .child(USER_TASK_ID).scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("eventSubProcess"))
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess", testHelper.getSingleActivityInstanceBeforeMigration("eventSubProcess").getId())
          .activity(USER_TASK_ID, testHelper.getSingleActivityInstanceBeforeMigration(EVENT_SUB_PROCESS_TASK_ID).getId())
        .done());

    testHelper.assertEventSubscriptionRemoved(EVENT_SUB_PROCESS_START_ID, EventSubProcessModels.MESSAGE_NAME);
    assertThat(testHelper.snapshotAfterMigration.getEventSubscriptions()).isEmpty();

    // and it is possible to complete the process instance
    testHelper.completeTask(USER_TASK_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateActiveEmbeddedSubProcessToEventSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.MESSAGE_EVENT_SUBPROCESS_PROCESS);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("subProcess", "eventSubProcess")
        .mapActivities(USER_TASK_ID, EVENT_SUB_PROCESS_TASK_ID)
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
    .hasProcessDefinitionId(targetProcessDefinition.getId())
    .matches(
      describeExecutionTree(null).scope().id(processInstance.getId())
        .child(EVENT_SUB_PROCESS_TASK_ID).scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("eventSubProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
          .activity(EVENT_SUB_PROCESS_TASK_ID, testHelper.getSingleActivityInstanceBeforeMigration(USER_TASK_ID).getId())
        .done());

    testHelper.assertEventSubscriptionCreated(EVENT_SUB_PROCESS_START_ID, EventSubProcessModels.MESSAGE_NAME);

    // and it is possible to complete the process instance
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateActiveErrorEventSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.ERROR_EVENT_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.ERROR_EVENT_SUBPROCESS_PROCESS);

    ProcessInstance processInstance = rule.getRuntimeService()
      .createProcessInstanceById(sourceProcessDefinition.getId())
      .startBeforeActivity(EVENT_SUB_PROCESS_TASK_ID)
      .execute();

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("eventSubProcess", "eventSubProcess")
        .mapActivities(EVENT_SUB_PROCESS_TASK_ID, EVENT_SUB_PROCESS_TASK_ID)
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then it is possible to complete the process instance
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateActiveCompensationEventSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.COMPENSATE_EVENT_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.COMPENSATE_EVENT_SUBPROCESS_PROCESS);

    ProcessInstance processInstance = rule.getRuntimeService()
      .createProcessInstanceById(sourceProcessDefinition.getId())
      .startBeforeActivity(EVENT_SUB_PROCESS_TASK_ID)
      .execute();

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("eventSubProcess", "eventSubProcess")
        .mapActivities(EVENT_SUB_PROCESS_TASK_ID, EVENT_SUB_PROCESS_TASK_ID)
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then it is possible to complete the process instance
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateActiveEscalationEventSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.ESCALATION_EVENT_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.ESCALATION_EVENT_SUBPROCESS_PROCESS);

    ProcessInstance processInstance = rule.getRuntimeService()
      .createProcessInstanceById(sourceProcessDefinition.getId())
      .startBeforeActivity(EVENT_SUB_PROCESS_TASK_ID)
      .execute();

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("eventSubProcess", "eventSubProcess")
        .mapActivities(EVENT_SUB_PROCESS_TASK_ID, EVENT_SUB_PROCESS_TASK_ID)
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then it is possible to complete the process instance
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateTaskAddEventSubProcess() {
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.MESSAGE_EVENT_SUBPROCESS_PROCESS);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities(USER_TASK_ID, EVENT_SUB_PROCESS_TASK_ID)
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
    .hasProcessDefinitionId(targetProcessDefinition.getId())
    .matches(
      describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
        .child(EVENT_SUB_PROCESS_TASK_ID).scope()
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("eventSubProcess")
          .activity(EVENT_SUB_PROCESS_TASK_ID, testHelper.getSingleActivityInstanceBeforeMigration(USER_TASK_ID).getId())
        .done());

    testHelper.assertEventSubscriptionCreated(EVENT_SUB_PROCESS_START_ID, EventSubProcessModels.MESSAGE_NAME);

    // and it is possible to complete the process instance
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateEventSubprocessMessageKeepTrigger() {
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.MESSAGE_EVENT_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.MESSAGE_EVENT_SUBPROCESS_PROCESS);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities(USER_TASK_ID, USER_TASK_ID)
        .mapActivities(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID)
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertEventSubscriptionMigrated(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID, EventSubProcessModels.MESSAGE_NAME);

    // and it is possible to trigger the event subprocess
    rule.getRuntimeService().correlateMessage(EventSubProcessModels.MESSAGE_NAME);
    assertThat(rule.getTaskService().createTaskQuery().count()).isOne();

    // and complete the process instance
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateEventSubprocessTimerKeepTrigger() {
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.TIMER_EVENT_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.TIMER_EVENT_SUBPROCESS_PROCESS);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities(USER_TASK_ID, USER_TASK_ID)
        .mapActivities(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID)
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertJobMigrated(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID, TimerStartEventSubprocessJobHandler.TYPE);

    // and it is possible to trigger the event subprocess
    Job timerJob = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(timerJob.getId());
    assertThat(rule.getTaskService().createTaskQuery().count()).isOne();

    // and complete the process instance
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateEventSubprocessSignalKeepTrigger() {
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.SIGNAL_EVENT_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.SIGNAL_EVENT_SUBPROCESS_PROCESS);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities(USER_TASK_ID, USER_TASK_ID)
        .mapActivities(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID)
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertEventSubscriptionMigrated(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID, EventSubProcessModels.SIGNAL_NAME);

    // and it is possible to trigger the event subprocess
    rule.getRuntimeService().signalEventReceived(EventSubProcessModels.SIGNAL_NAME);
    assertThat(rule.getTaskService().createTaskQuery().count()).isOne();

    // and complete the process instance
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateConditionalBoundaryEventKeepTrigger() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CONDITIONAL_EVENT_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CONDITIONAL_EVENT_SUBPROCESS_PROCESS);

    var migrationInstructionBuilder = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(USER_TASK_ID, USER_TASK_ID)
      .mapActivities(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID);

    // when conditional event sub process is migrated without update event trigger
    assertThatThrownBy(migrationInstructionBuilder::build)
      // then
      .isInstanceOf(MigrationPlanValidationException.class)
      .hasMessageContaining(MIGRATION_CONDITIONAL_VALIDATION_ERROR_MSG);
  }


  @Test
  void testMigrateEventSubprocessChangeStartEventType() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.SIGNAL_EVENT_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.TIMER_EVENT_SUBPROCESS_PROCESS);
    var migrationPlanBuilder = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities(USER_TASK_ID, USER_TASK_ID)
        .mapActivities(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID);

    // when then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        MigrationPlanValidationException ex = (MigrationPlanValidationException) e;
        MigrationPlanValidationReportAssert.assertThat(ex.getValidationReport())
          .hasInstructionFailures(EVENT_SUB_PROCESS_START_ID,
            "Events are not of the same type (signalStartEvent != startTimerEvent)"
          );
      });
  }

  @Test
  void testMigrateEventSubprocessTimerIncident() {
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.TIMER_EVENT_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.TIMER_EVENT_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities(USER_TASK_ID, USER_TASK_ID)
        .mapActivities(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID)
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    Job timerTriggerJob = rule.getManagementService().createJobQuery().singleResult();
    // create an incident
    rule.getManagementService().setJobRetries(timerTriggerJob.getId(), 0);
    Incident incidentBeforeMigration = rule.getRuntimeService().createIncidentQuery().singleResult();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    Incident incidentAfterMigration = rule.getRuntimeService().createIncidentQuery().singleResult();
    assertThat(incidentAfterMigration).isNotNull();

    assertThat(incidentAfterMigration.getId()).isEqualTo(incidentBeforeMigration.getId());
    assertThat(incidentAfterMigration.getConfiguration()).isEqualTo(timerTriggerJob.getId());

    assertThat(incidentAfterMigration.getActivityId()).isEqualTo(EVENT_SUB_PROCESS_START_ID);
    assertThat(incidentAfterMigration.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());

    // and it is possible to complete the process
    rule.getManagementService().executeJob(timerTriggerJob.getId());
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateNonInterruptingEventSubprocessMessageTrigger() {
    BpmnModelInstance nonInterruptingModel = modify(EventSubProcessModels.MESSAGE_EVENT_SUBPROCESS_PROCESS)
      .startEventBuilder(EVENT_SUB_PROCESS_START_ID)
      .interrupting(false)
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(nonInterruptingModel);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(nonInterruptingModel);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities(USER_TASK_ID, USER_TASK_ID)
        .mapActivities(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID)
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertEventSubscriptionMigrated(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID, EventSubProcessModels.MESSAGE_NAME);

    // and it is possible to trigger the event subprocess
    rule.getRuntimeService().correlateMessage(EventSubProcessModels.MESSAGE_NAME);
    assertThat(rule.getTaskService().createTaskQuery().count()).isEqualTo(2);

    // and complete the process instance
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.completeTask(USER_TASK_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testUpdateEventMessage() {
    // given
    BpmnModelInstance sourceProcess = EventSubProcessModels.MESSAGE_EVENT_SUBPROCESS_PROCESS;
    BpmnModelInstance targetProcess = modify(EventSubProcessModels.MESSAGE_EVENT_SUBPROCESS_PROCESS)
      .renameMessage(EventSubProcessModels.MESSAGE_NAME, "new" + EventSubProcessModels.MESSAGE_NAME);

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetProcess);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(USER_TASK_ID, USER_TASK_ID)
      .mapActivities(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID).updateEventTrigger()
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionMigrated(
      EVENT_SUB_PROCESS_START_ID, EventSubProcessModels.MESSAGE_NAME,
      EVENT_SUB_PROCESS_START_ID, "new" + EventSubProcessModels.MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    rule.getRuntimeService().correlateMessage("new" + EventSubProcessModels.MESSAGE_NAME);
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testUpdateEventSignal() {
    // given
    BpmnModelInstance sourceProcess = EventSubProcessModels.SIGNAL_EVENT_SUBPROCESS_PROCESS;
    BpmnModelInstance targetProcess = modify(EventSubProcessModels.SIGNAL_EVENT_SUBPROCESS_PROCESS)
      .renameSignal(EventSubProcessModels.SIGNAL_NAME, "new" + EventSubProcessModels.SIGNAL_NAME);

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetProcess);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(USER_TASK_ID, USER_TASK_ID)
      .mapActivities(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID).updateEventTrigger()
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionMigrated(
      EVENT_SUB_PROCESS_START_ID, EventSubProcessModels.SIGNAL_NAME,
      EVENT_SUB_PROCESS_START_ID, "new" + EventSubProcessModels.SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    rule.getRuntimeService().signalEventReceived("new" + EventSubProcessModels.SIGNAL_NAME);
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testUpdateEventTimer() {
    // given
    ClockTestUtil.setClockToDateWithoutMilliseconds();

    BpmnModelInstance sourceProcess = EventSubProcessModels.TIMER_EVENT_SUBPROCESS_PROCESS;
    BpmnModelInstance targetProcess = modify(EventSubProcessModels.TIMER_EVENT_SUBPROCESS_PROCESS)
      .removeChildren(EVENT_SUB_PROCESS_START_ID)
      .startEventBuilder(EVENT_SUB_PROCESS_START_ID)
        .timerWithDuration("PT50M")
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetProcess);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(USER_TASK_ID, USER_TASK_ID)
      .mapActivities(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID).updateEventTrigger()
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    Date newDueDate = new DateTime(ClockUtil.getCurrentTime()).plusMinutes(50).toDate();
    testHelper.assertJobMigrated(
        testHelper.snapshotBeforeMigration.getJobs().get(0),
      EVENT_SUB_PROCESS_START_ID,
        newDueDate);

    // and it is possible to successfully complete the migrated instance
    Job jobAfterMigration = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(jobAfterMigration.getId());

    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testUpdateEventMessageWithExpression() {

    // given
    String newMessageNameWithExpression = "new" + EventSubProcessModels.MESSAGE_NAME + "-${var}";
    BpmnModelInstance sourceProcess = EventSubProcessModels.MESSAGE_INTERMEDIATE_EVENT_SUBPROCESS_PROCESS;
    BpmnModelInstance targetProcess = modify(EventSubProcessModels.MESSAGE_INTERMEDIATE_EVENT_SUBPROCESS_PROCESS)
        .renameMessage(EventSubProcessModels.MESSAGE_NAME, newMessageNameWithExpression);

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetProcess);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities(USER_TASK_ID, USER_TASK_ID)
        .mapActivities("eventSubProcess", "eventSubProcess")
        .mapActivities("catchMessage", "catchMessage").updateEventTrigger()
        .build();
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("var", "foo");

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan, variables);

    // then
    String resolvedMessageName = "new" + EventSubProcessModels.MESSAGE_NAME + "-foo";
    testHelper.assertEventSubscriptionMigrated(
        "catchMessage", EventSubProcessModels.MESSAGE_NAME,
        "catchMessage", resolvedMessageName);

    // and it is possible to successfully complete the migrated instance
    rule.getRuntimeService().correlateMessage(resolvedMessageName);
    testHelper.completeTask(USER_TASK_ID);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testUpdateEventSignalWithExpression() {
    // given
    String newSignalNameWithExpression = "new" + EventSubProcessModels.MESSAGE_NAME + "-${var}";
    BpmnModelInstance sourceProcess = EventSubProcessModels.SIGNAL_EVENT_SUBPROCESS_PROCESS;
    BpmnModelInstance targetProcess = modify(EventSubProcessModels.SIGNAL_EVENT_SUBPROCESS_PROCESS)
        .renameSignal(EventSubProcessModels.SIGNAL_NAME, newSignalNameWithExpression);

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetProcess);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities(USER_TASK_ID, USER_TASK_ID)
        .mapActivities(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID).updateEventTrigger()
        .build();

    HashMap<String, Object> variables = new HashMap<>();
    variables.put("var", "foo");

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan, variables);

    // then
    String resolvedSignalName = "new" + EventSubProcessModels.MESSAGE_NAME + "-foo";
    testHelper.assertEventSubscriptionMigrated(
      EVENT_SUB_PROCESS_START_ID, EventSubProcessModels.SIGNAL_NAME,
      EVENT_SUB_PROCESS_START_ID, resolvedSignalName);

    // and it is possible to successfully complete the migrated instance
    rule.getRuntimeService().signalEventReceived(resolvedSignalName);
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testUpdateConditionalEventExpression() {
    // given
    BpmnModelInstance sourceProcess = EventSubProcessModels.FALSE_CONDITIONAL_EVENT_SUBPROCESS_PROCESS;
    BpmnModelInstance targetProcess = modify(CONDITIONAL_EVENT_SUBPROCESS_PROCESS);

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetProcess);


    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(USER_TASK_ID, USER_TASK_ID)
      .mapActivities(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID).updateEventTrigger()
      .build();

    // when process is migrated without update event trigger
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then condition is migrated and has new condition expr
    testHelper.assertEventSubscriptionMigrated(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID, null);

    // and it is possible to successfully complete the migrated instance
    testHelper.setAnyVariable(testHelper.snapshotAfterMigration.getProcessInstanceId());
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void shouldRemainActiveAfterUserTaskBecomesNoneScope() {
    BpmnModelInstance sourceModel = modify(ProcessModels.ONE_TASK_PROCESS)
          .addSubProcessTo(ProcessModels.PROCESS_KEY)
            .id(EventSubProcessModels.EVENT_SUB_PROCESS_ID)
            .triggerByEvent()
            .embeddedSubProcess()
              .startEvent(EVENT_SUB_PROCESS_START_ID).message(EventSubProcessModels.MESSAGE_NAME)
              .userTask(EVENT_SUB_PROCESS_TASK_ID)
                .boundaryEvent().condition("${true == false}")
              .endEvent()
            .endEvent()
          .subProcessDone()
          .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceModel);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(EventSubProcessModels.MESSAGE_EVENT_SUBPROCESS_PROCESS);

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());
    rule.getRuntimeService().correlateMessage("Message");

    ActivityStatistics activityStatistics = rule.getManagementService()
        .createActivityStatisticsQuery(sourceProcessDefinition.getId())
        .singleResult();

    // assume
    assertThat(activityStatistics.getId()).isEqualTo(EVENT_SUB_PROCESS_TASK_ID);
    assertThat(activityStatistics.getInstances()).isEqualTo(1);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    activityStatistics = rule.getManagementService()
        .createActivityStatisticsQuery(targetProcessDefinition.getId())
        .singleResult();

    // assume
    assertThat(activityStatistics.getId()).isEqualTo(EVENT_SUB_PROCESS_TASK_ID);
    assertThat(activityStatistics.getInstances()).isEqualTo(1);
  }

  @Test
  void shouldRemainActiveAfterUserTaskBecomesScope() {
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(EventSubProcessModels.MESSAGE_EVENT_SUBPROCESS_PROCESS);

        BpmnModelInstance targetModel = modify(ProcessModels.ONE_TASK_PROCESS)
          .addSubProcessTo(ProcessModels.PROCESS_KEY)
            .id(EventSubProcessModels.EVENT_SUB_PROCESS_ID)
            .triggerByEvent()
            .embeddedSubProcess()
              .startEvent(EVENT_SUB_PROCESS_START_ID).message(EventSubProcessModels.MESSAGE_NAME)
              .userTask(EVENT_SUB_PROCESS_TASK_ID)
                .boundaryEvent().condition("${true == false}")
              .endEvent()
            .endEvent()
          .subProcessDone()
          .done();

    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetModel);

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());
    rule.getRuntimeService().correlateMessage("Message");

    ActivityStatistics activityStatistics = rule.getManagementService()
        .createActivityStatisticsQuery(sourceProcessDefinition.getId())
        .singleResult();

    // assume
    assertThat(activityStatistics.getId()).isEqualTo(EVENT_SUB_PROCESS_TASK_ID);
    assertThat(activityStatistics.getInstances()).isEqualTo(1);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    activityStatistics = rule.getManagementService()
        .createActivityStatisticsQuery(targetProcessDefinition.getId())
        .singleResult();

    // assume
    assertThat(activityStatistics.getId()).isEqualTo(EVENT_SUB_PROCESS_TASK_ID);
    assertThat(activityStatistics.getInstances()).isEqualTo(1);
  }

}
