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

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

public class MigrationAddBoundaryEventsTest {

  public static final String AFTER_BOUNDARY_TASK = "afterBoundary";
  public static final String MESSAGE_NAME = "Message";
  public static final String SIGNAL_NAME = "Signal";
  public static final String TIMER_DATE = "2016-02-11T12:13:14Z";
  public static final String ERROR_CODE = "Error";
  public static final String ESCALATION_CODE = "Escalation";

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @Test
  void testAddMessageBoundaryEventToUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddMessageBoundaryEventToUserTaskAndCorrelateMessage() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to correlate the message and successfully complete the migrated instance
    testHelper.correlateMessage(MESSAGE_NAME);
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddMessageBoundaryEventToScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SCOPE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddMessageBoundaryEventToScopeUserTaskAndCorrelateMessage() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SCOPE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to correlate the message and successfully complete the migrated instance
    testHelper.correlateMessage(MESSAGE_NAME);
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddMessageBoundaryEventToConcurrentUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
      .activityBuilder("userTask1")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask1");
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddMessageBoundaryEventToConcurrentUserTaskAndCorrelateMessage() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
      .activityBuilder("userTask1")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to correlate the message and successfully complete the migrated instance
    testHelper.correlateMessage(MESSAGE_NAME);
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddMessageBoundaryEventToConcurrentScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_SCOPE_TASKS)
      .activityBuilder("userTask1")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask1");
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddMessageBoundaryEventToConcurrentScopeUserTaskAndCorrelateMessage() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_SCOPE_TASKS)
      .activityBuilder("userTask1")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to correlate the message and successfully complete the migrated instance
    testHelper.correlateMessage(MESSAGE_NAME);
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddMessageBoundaryEventToSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddMessageBoundaryEventToSubProcessAndCorrelateMessage() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to correlate the message and successfully complete the migrated instance
    testHelper.correlateMessage(MESSAGE_NAME);
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddMessageBoundaryEventToSubProcessWithScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddMessageBoundaryEventToSubProcessWithScopeUserTaskAndCorrelateMessage() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to correlate the message and successfully complete the migrated instance
    testHelper.correlateMessage(MESSAGE_NAME);
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddMessageBoundaryEventToParallelSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_SUBPROCESS_PROCESS)
      .activityBuilder("subProcess1")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess1", "subProcess1")
      .mapActivities("subProcess2", "subProcess2")
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask1");
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddMessageBoundaryEventToParallelSubProcessAndCorrelateMessage() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_SUBPROCESS_PROCESS)
      .activityBuilder("subProcess1")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess1", "subProcess1")
      .mapActivities("subProcess2", "subProcess2")
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to correlate the message and successfully complete the migrated instance
    testHelper.correlateMessage(MESSAGE_NAME);
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddSignalBoundaryEventToUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").signal(SIGNAL_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddSignalBoundaryEventToUserTaskAndSendSignal() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").signal(SIGNAL_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to send the signal and successfully complete the migrated instance
    testHelper.sendSignal(SIGNAL_NAME);
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddSignalBoundaryEventToScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SCOPE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").signal(SIGNAL_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddSignalBoundaryEventToScopeUserTaskAndSendSignal() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SCOPE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").signal(SIGNAL_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to send the signal and successfully complete the migrated instance
    testHelper.sendSignal(SIGNAL_NAME);
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddSignalBoundaryEventToConcurrentUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
      .activityBuilder("userTask1")
        .boundaryEvent("boundary").signal(SIGNAL_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask1");
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddSignalBoundaryEventToConcurrentUserTaskAndSendSignal() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
      .activityBuilder("userTask1")
        .boundaryEvent("boundary").signal(SIGNAL_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to send the signal and successfully complete the migrated instance
    testHelper.sendSignal(SIGNAL_NAME);
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddSignalBoundaryEventToConcurrentScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_SCOPE_TASKS)
      .activityBuilder("userTask1")
        .boundaryEvent("boundary").signal(SIGNAL_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask1");
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddSignalBoundaryEventToConcurrentScopeUserTaskAndSendSignal() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_SCOPE_TASKS)
      .activityBuilder("userTask1")
        .boundaryEvent("boundary").signal(SIGNAL_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to send the signal and successfully complete the migrated instance
    testHelper.sendSignal(SIGNAL_NAME);
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddSignalBoundaryEventToSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("boundary").signal(SIGNAL_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddSignalBoundaryEventToSubProcessAndCorrelateSignal() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("boundary").signal(SIGNAL_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to correlate the signal and successfully complete the migrated instance
    testHelper.sendSignal(SIGNAL_NAME);
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddSignalBoundaryEventToSubProcessWithScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("boundary").signal(SIGNAL_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddSignalBoundaryEventToSubProcessWithScopeUserTaskAndCorrelateSignal() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("boundary").signal(SIGNAL_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to correlate the signal and successfully complete the migrated instance
    testHelper.sendSignal(SIGNAL_NAME);
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddSignalBoundaryEventToParallelSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_SUBPROCESS_PROCESS)
      .activityBuilder("subProcess1")
        .boundaryEvent("boundary").signal(SIGNAL_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess1", "subProcess1")
      .mapActivities("subProcess2", "subProcess2")
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask1");
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddSignalBoundaryEventToParallelSubProcessAndCorrelateSignal() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_SUBPROCESS_PROCESS)
      .activityBuilder("subProcess1")
        .boundaryEvent("boundary").signal(SIGNAL_NAME)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess1", "subProcess1")
      .mapActivities("subProcess2", "subProcess2")
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to correlate the signal and successfully complete the migrated instance
    testHelper.sendSignal(SIGNAL_NAME);
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTimerBoundaryEventToUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertBoundaryTimerJobCreated("boundary");

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTimerBoundaryEventToUserTaskAndSendTimerWithDate() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to send the timer and successfully complete the migrated instance
    testHelper.triggerTimer();
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTimerBoundaryEventToScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SCOPE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertBoundaryTimerJobCreated("boundary");

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTimerBoundaryEventToScopeUserTaskAndSendTimerWithDate() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SCOPE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to send the timer and successfully complete the migrated instance
    testHelper.triggerTimer();
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTimerBoundaryEventToConcurrentUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
      .activityBuilder("userTask1")
        .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertBoundaryTimerJobCreated("boundary");

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask1");
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTimerBoundaryEventToConcurrentUserTaskAndSendTimerWithDate() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
      .activityBuilder("userTask1")
        .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to send the timer and successfully complete the migrated instance
    testHelper.triggerTimer();
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTimerBoundaryEventToConcurrentScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_SCOPE_TASKS)
      .activityBuilder("userTask1")
        .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertBoundaryTimerJobCreated("boundary");

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask1");
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTimerBoundaryEventToConcurrentScopeUserTaskAndSendTimerWithDate() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_SCOPE_TASKS)
      .activityBuilder("userTask1")
        .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to send the timer and successfully complete the migrated instance
    testHelper.triggerTimer();
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTimerBoundaryEventToSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertBoundaryTimerJobCreated("boundary");

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTimerBoundaryEventToSubProcessAndCorrelateTimerWithDate() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to correlate the timer and successfully complete the migrated instance
    testHelper.triggerTimer();
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTimerBoundaryEventToSubProcessWithScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertBoundaryTimerJobCreated("boundary");

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTimerBoundaryEventToSubProcessWithScopeUserTaskAndCorrelateTimerWithDate() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to correlate the timer and successfully complete the migrated instance
    testHelper.triggerTimer();
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTimerBoundaryEventToParallelSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_SUBPROCESS_PROCESS)
      .activityBuilder("subProcess1")
        .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess1", "subProcess1")
      .mapActivities("subProcess2", "subProcess2")
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertBoundaryTimerJobCreated("boundary");

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask1");
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddTimerBoundaryEventToParallelSubProcessAndCorrelateTimerWithDate() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.PARALLEL_SUBPROCESS_PROCESS)
      .activityBuilder("subProcess1")
        .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess1", "subProcess1")
      .mapActivities("subProcess2", "subProcess2")
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to correlate the timer and successfully complete the migrated instance
    testHelper.triggerTimer();
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.completeTask("userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddMultipleBoundaryEvents() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("timerBoundary").timerWithDate(TIMER_DATE)
      .moveToActivity("userTask")
        .boundaryEvent("messageBoundary").message(MESSAGE_NAME)
      .moveToActivity("userTask")
        .boundaryEvent("signalBoundary").signal(SIGNAL_NAME)
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionCreated("messageBoundary", MESSAGE_NAME);
    testHelper.assertEventSubscriptionCreated("signalBoundary", SIGNAL_NAME);
    testHelper.assertBoundaryTimerJobCreated("timerBoundary");

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddErrorBoundaryEventToSubProcessAndThrowError() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .endEventBuilder("subProcessEnd")
        .error(ERROR_CODE) // let the end event of the subprocess throw an error
      .moveToActivity("subProcess")
        .boundaryEvent().error(ERROR_CODE)
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testAddEscalationBoundaryEventToSubProcessAndThrowEscalation() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .endEventBuilder("subProcessEnd").escalation(ESCALATION_CODE) // let the end event of the subprocess escalate
      .moveToActivity("subProcess")
        .boundaryEvent().escalation(ESCALATION_CODE) // catch escalation with boundary event
        .userTask(AFTER_BOUNDARY_TASK)
        .endEvent()
      .done()
    );

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.completeTask(AFTER_BOUNDARY_TASK);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

}
