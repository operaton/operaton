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

import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.EventSubProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.TransactionModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationTransactionTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testRule = new MigrationTestExtension(rule);

  @Test
  void testContinueProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(TransactionModels.ONE_TASK_TRANSACTION);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(TransactionModels.ONE_TASK_TRANSACTION);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("transaction", "transaction")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    ProcessInstance processInstance = testRule.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testRule.completeTask("userTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testContinueProcessTriggerCancellation() {
    // given
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(TransactionModels.ONE_TASK_TRANSACTION);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(TransactionModels.CANCEL_BOUNDARY_EVENT);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("transaction", "transaction")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    ProcessInstance processInstance = testRule.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testRule.completeTask("userTask");
    testRule.completeTask("afterBoundaryTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testAssertTrees() {
    // given
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(TransactionModels.ONE_TASK_TRANSACTION);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(TransactionModels.ONE_TASK_TRANSACTION);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("transaction", "transaction")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testRule.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testRule.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testRule.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope().id(testRule.getSingleExecutionIdForActivityBeforeMigration("userTask")).up()
        .done());

    testRule.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("transaction", testRule.getSingleActivityInstanceBeforeMigration("transaction").getId())
          .activity("userTask", testRule.getSingleActivityInstanceBeforeMigration("userTask").getId())
      .done());
  }

  @Test
  void testAddTransactionContinueProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(TransactionModels.ONE_TASK_TRANSACTION);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    ProcessInstance processInstance = testRule.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testRule.completeTask("userTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testAddTransactionTriggerCancellation() {
    // given
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(TransactionModels.CANCEL_BOUNDARY_EVENT);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    ProcessInstance processInstance = testRule.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testRule.completeTask("userTask");
    testRule.completeTask("afterBoundaryTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testAddTransactionAssertTrees() {
    // given
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(TransactionModels.ONE_TASK_TRANSACTION);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testRule.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testRule.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testRule.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope()
        .done());

    testRule.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("transaction")
          .activity("userTask", testRule.getSingleActivityInstanceBeforeMigration("userTask").getId())
      .done());
  }

  @Test
  void testRemoveTransactionContinueProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(TransactionModels.ONE_TASK_TRANSACTION);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    ProcessInstance processInstance = testRule.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testRule.completeTask("userTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testRemoveTransactionAssertTrees() {
    // given
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(TransactionModels.ONE_TASK_TRANSACTION);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testRule.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testRule.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree("userTask").scope().id(testRule.snapshotBeforeMigration.getProcessInstanceId())
        .done());

    testRule.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("userTask", testRule.getSingleActivityInstanceBeforeMigration("userTask").getId())
      .done());
  }

  @Test
  void testMigrateTransactionToEmbeddedSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(TransactionModels.ONE_TASK_TRANSACTION);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("transaction", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    ProcessInstance processInstance = testRule.createProcessInstanceAndMigrate(migrationPlan);

    // then
    assertThat(testRule.getSingleActivityInstanceAfterMigration("subProcess").getId()).isEqualTo(testRule.getSingleActivityInstanceBeforeMigration("transaction").getId());

    testRule.completeTask("userTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateEventSubProcessToTransaction() {
    // given
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(EventSubProcessModels.MESSAGE_EVENT_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(TransactionModels.ONE_TASK_TRANSACTION);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("eventSubProcess", "transaction")
      .mapActivities("eventSubProcessTask", "userTask")
      .build();

    // when
    ProcessInstance processInstance = rule.getRuntimeService()
      .createProcessInstanceById(sourceProcessDefinition.getId())
      .startBeforeActivity("eventSubProcessTask")
      .execute();

    testRule.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testRule.getSingleActivityInstanceAfterMigration("transaction").getId()).isEqualTo(testRule.getSingleActivityInstanceBeforeMigration("eventSubProcess").getId());

    testRule.completeTask("userTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

}
