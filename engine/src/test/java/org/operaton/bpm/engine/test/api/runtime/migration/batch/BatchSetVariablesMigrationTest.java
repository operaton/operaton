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
package org.operaton.bpm.engine.test.api.runtime.migration.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.engine.variable.Variables;

class BatchSetVariablesMigrationTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);
  BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @AfterEach
  void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @AfterEach
  void clearAuthentication() {
    engineRule.getIdentityService().clearAuthentication();
  }

  @AfterEach
  void resetEngineConfig() {
    engineRule.getProcessEngineConfiguration()
        .setRestrictUserOperationLogToAuthenticatedUsers(true);
  }

  @Test
  void shouldCreateBatchVariable() {
    // given
    ProcessDefinition sourceProcessDefinition =
        migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    Batch batch = helper.migrateProcessInstancesAsync(5, sourceProcessDefinition,
        targetProcessDefinition, Variables.putValue("foo", "bar"));

    // when
    helper.completeSeedJobs(batch);

    // then
    VariableInstance batchVariable = engineRule.getRuntimeService()
        .createVariableInstanceQuery()
        .batchIdIn(batch.getId())
        .singleResult();

    assertThat(batchVariable)
        .extracting("name", "value")
        .containsExactly("foo", "bar");
  }

  @Test
  void shouldCreateBatchVariables() {
    // given
    ProcessDefinition sourceProcessDefinition =
        migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    // when
    Batch batch = helper.migrateProcessInstancesAsync(5, sourceProcessDefinition,
        targetProcessDefinition, Variables.putValue("foo", "bar").putValue("bar", "foo"));

    // then
    List<VariableInstance> batchVariables = engineRule.getRuntimeService()
        .createVariableInstanceQuery()
        .batchIdIn(batch.getId())
        .list();

    assertThat(batchVariables)
        .extracting("name", "value")
        .containsExactlyInAnyOrder(
            tuple("foo", "bar"),
            tuple("bar", "foo")
        );
  }

  @Test
  void shouldRemoveBatchVariable() {
    // given
    ProcessDefinition sourceProcessDefinition =
        migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    Batch batch = helper.migrateProcessInstancesAsync(5, sourceProcessDefinition,
        targetProcessDefinition, Variables.putValue("foo", "bar"));

    // when
    helper.completeBatch(batch);

    // then
    List<VariableInstance> batchVariables = engineRule.getRuntimeService()
        .createVariableInstanceQuery()
        .batchIdIn(batch.getId())
        .list();

    assertThat(batchVariables).isEmpty();
  }

  @Test
  void shouldSetVariables() {
    // given
    ProcessDefinition sourceProcessDefinition =
        migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    Batch batch = helper.migrateProcessInstancesAsync(5, sourceProcessDefinition,
        targetProcessDefinition, Variables.putValue("foo", "bar"));

    // when
    helper.completeBatch(batch);

    // then
    List<VariableInstance> variables = engineRule.getRuntimeService()
        .createVariableInstanceQuery()
        .list();

    assertThat(variables)
        .extracting("processDefinitionId", "name", "value")
        .containsExactlyInAnyOrder(
            tuple(targetProcessDefinition.getId(), "foo", "bar"),
            tuple(targetProcessDefinition.getId(), "foo", "bar"),
            tuple(targetProcessDefinition.getId(), "foo", "bar"),
            tuple(targetProcessDefinition.getId(), "foo", "bar"),
            tuple(targetProcessDefinition.getId(), "foo", "bar")
        );
  }

  @Test
  void shouldThrowException_TransientVariable() {
    // given
    ProcessDefinition sourceProcessDefinition =
        migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    String processInstanceId = engineRule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId()).getId();

    MigrationPlan migrationPlan = engineRule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .setVariables(Variables.putValue("foo", Variables.stringValue("bar", true))).build();

    var migrationPlanExecutionBuilder = engineRule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(processInstanceId);

    // when/then
    assertThatThrownBy(migrationPlanExecutionBuilder::executeAsync)
        .isInstanceOf(BadUserRequestException.class)
        .hasMessageContaining("ENGINE-13044 Setting transient variable 'foo' " +
            "asynchronously is currently not supported.");
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldLogOperationOnCreation() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    // when
    helper.migrateProcessInstancesAsync(5, sourceProcessDefinition,
        targetProcessDefinition, Variables.putValue("foo", "bar"), true);

    // then
    List<UserOperationLogEntry> operationLogEntries = engineRule.getHistoryService()
        .createUserOperationLogQuery()
        .list();

    assertThat(operationLogEntries)
        .extracting("operationType", "userId", "property", "newValue")
        .containsExactlyInAnyOrder(
            Tuple.tuple("Migrate", "user", "processDefinitionId", targetProcessDefinition.getId()),
            Tuple.tuple("Migrate", "user", "nrOfInstances", "5"),
            Tuple.tuple("Migrate", "user", "nrOfSetVariables", "1"),
            Tuple.tuple("Migrate", "user", "async", "true")
        );
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldNotLogOperationOnExecution() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    Batch batch = helper.migrateProcessInstancesAsync(5, sourceProcessDefinition,
        targetProcessDefinition, Variables.putValue("foo", "bar"));

    engineRule.getIdentityService().setAuthenticatedUserId("user");

    // when
    helper.completeBatch(batch);

    // then
    List<UserOperationLogEntry> operationLogEntries = engineRule.getHistoryService()
        .createUserOperationLogQuery()
        .list();

    assertThat(operationLogEntries)
        .extracting("operationType")
        .hasSize(7)
        .containsOnly("Execute");
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldNotLogOperationOnExecutionUnauthenticated() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    Batch batch = helper.migrateProcessInstancesAsync(5, sourceProcessDefinition,
        targetProcessDefinition, Variables.putValue("foo", "bar"));

    engineRule.getProcessEngineConfiguration()
        .setRestrictUserOperationLogToAuthenticatedUsers(false);

    // when
    helper.completeBatch(batch);

    // then
    List<UserOperationLogEntry> operationLogEntries = engineRule.getHistoryService()
        .createUserOperationLogQuery()
        .list();

    assertThat(operationLogEntries)
        .extracting("operationType")
        .hasSize(7)
        .containsOnly("Execute");
  }

}
