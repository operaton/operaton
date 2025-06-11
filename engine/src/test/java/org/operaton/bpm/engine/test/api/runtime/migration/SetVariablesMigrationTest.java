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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels.USER_TASK_ID;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.engine.variable.Variables;

class SetVariablesMigrationTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @AfterEach
  void clearAuthentication() {
    rule.getIdentityService().clearAuthentication();
  }

  @AfterEach
  void resetEngineConfig() {
    rule.getProcessEngineConfiguration()
        .setRestrictUserOperationLogToAuthenticatedUsers(true);
  }

  @Test
  void shouldSetVariable() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(Collections.singletonMap("foo", "bar"))
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testHelper.snapshotBeforeMigration.getVariables()).isEmpty();
    assertThat(testHelper.snapshotAfterMigration.getVariables())
        .extracting("name", "value", "executionId")
        .containsExactly(tuple("foo", "bar", processInstance.getId()));
  }

  @Test
  void shouldSetVariables() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    Map<String, Object> variables = new HashMap<>();
    variables.put("foo", "bar");
    variables.put("bar", 5);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(variables)
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testHelper.snapshotBeforeMigration.getVariables()).isEmpty();
    assertThat(testHelper.snapshotAfterMigration.getVariables())
        .extracting("name", "value", "executionId")
        .containsExactlyInAnyOrder(
            tuple("foo", "bar", processInstance.getId()),
            tuple("bar", 5, processInstance.getId())
        );
  }

  @Test
  void shouldSetUntypedVariable() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(Variables.putValue("foo", "bar"))
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testHelper.snapshotBeforeMigration.getVariables()).isEmpty();
    assertThat(testHelper.snapshotAfterMigration.getVariables())
        .extracting("name", "value", "executionId")
        .containsExactly(tuple("foo", "bar", processInstance.getId()));
  }

  @Test
  void shouldSetUntypedVariables() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(
            Variables.putValue("foo", "bar")
                .putValue("bar", 5)
        )
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testHelper.snapshotBeforeMigration.getVariables()).isEmpty();
    assertThat(testHelper.snapshotAfterMigration.getVariables())
        .extracting("name", "value", "executionId")
        .containsExactlyInAnyOrder(
            tuple("foo", "bar", processInstance.getId()),
            tuple("bar", 5, processInstance.getId())
        );
  }

  @Test
  void shouldSetMapOfTypedVariable() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(Collections.singletonMap("foo", Variables.shortValue((short)5)))
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testHelper.snapshotBeforeMigration.getVariables()).isEmpty();
    assertThat(testHelper.snapshotAfterMigration.getVariables())
        .extracting("name", "value", "executionId")
        .containsExactly(tuple("foo", (short)5, processInstance.getId()));
  }

  @Test
  void shouldSetVariableMapOfTypedVariable() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(
            Variables.putValueTyped("foo", Variables.stringValue("bar"))
        )
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testHelper.snapshotBeforeMigration.getVariables()).isEmpty();
    assertThat(testHelper.snapshotAfterMigration.getVariables())
        .extracting("name", "value", "executionId")
        .containsExactly(tuple("foo", "bar", processInstance.getId()));
  }

  @Test
  void shouldSetTypedAndUntypedVariables() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(
            Variables.putValue("foo", "bar")
                .putValueTyped("bar", Variables.integerValue(5))
        )
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testHelper.snapshotBeforeMigration.getVariables()).isEmpty();
    assertThat(testHelper.snapshotAfterMigration.getVariables())
        .extracting("name", "value", "executionId")
        .containsExactlyInAnyOrder(
            tuple("foo", "bar", processInstance.getId()),
            tuple("bar", 5, processInstance.getId())
        );
  }

  @Test
  void shouldSetNullVariables() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(null)
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testHelper.snapshotBeforeMigration.getVariables()).isEmpty();
    assertThat(testHelper.snapshotAfterMigration.getVariables()).isEmpty();
  }

  @Test
  void shouldSetEmptyVariables() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(new HashMap<>())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testHelper.snapshotBeforeMigration.getVariables()).isEmpty();
    assertThat(testHelper.snapshotAfterMigration.getVariables()).isEmpty();
  }

  @Test
  void shouldSetTransientVariable() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(
        modify(ProcessModels.ONE_TASK_PROCESS)
            .activityBuilder(USER_TASK_ID)
              .operatonExecutionListenerClass("end", ReadTransientVariableExecutionListener.class)
            .done()
        );
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(Variables.putValueTyped("foo", Variables.stringValue("bar", true)))
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testHelper.snapshotBeforeMigration.getVariables()).isEmpty();
    assertThat(testHelper.snapshotAfterMigration.getVariables()).isEmpty();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldWriteOperationLog() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(
            Variables.putValue("foo", "bar")
                .putValueTyped("bar", Variables.integerValue(5))
        )
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    rule.getIdentityService().setAuthenticatedUserId("user");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    List<UserOperationLogEntry> operationLogEntries = rule.getHistoryService()
        .createUserOperationLogQuery()
        .list();

    assertThat(operationLogEntries)
        .extracting("operationType", "userId", "property", "newValue")
        .containsExactlyInAnyOrder(
            tuple("Migrate", "user", "processDefinitionId", targetProcessDefinition.getId()),
            tuple("Migrate", "user", "nrOfInstances", "1"),
            tuple("Migrate", "user", "nrOfSetVariables", "2"),
            tuple("Migrate", "user", "async", "false")
        );
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldWriteOperationLogUnauthenticated() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(
            Variables.putValue("foo", "bar")
                .putValueTyped("bar", Variables.integerValue(5))
        )
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    rule.getProcessEngineConfiguration().setRestrictUserOperationLogToAuthenticatedUsers(false);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    List<UserOperationLogEntry> operationLogEntries = rule.getHistoryService()
        .createUserOperationLogQuery()
        .list();

    assertThat(operationLogEntries)
        .extracting("operationType", "userId", "property", "newValue")
        .containsExactlyInAnyOrder(
            tuple("Migrate", null, "processDefinitionId", targetProcessDefinition.getId()),
            tuple("Migrate", null, "nrOfInstances", "1"),
            tuple("Migrate", null, "nrOfSetVariables", "2"),
            tuple("Migrate", null, "async", "false")
        );
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldWriteOperationLogForEmptyVariables() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(new HashMap<>())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    rule.getIdentityService().setAuthenticatedUserId("user");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    List<UserOperationLogEntry> operationLogEntries = rule.getHistoryService()
        .createUserOperationLogQuery()
        .list();

    assertThat(operationLogEntries)
        .extracting("operationType", "userId", "property", "newValue")
        .containsExactlyInAnyOrder(
            tuple("Migrate", "user", "processDefinitionId", targetProcessDefinition.getId()),
            tuple("Migrate", "user", "nrOfInstances", "1"),
            tuple("Migrate", "user", "nrOfSetVariables", "0"),
            tuple("Migrate", "user", "async", "false")
        );
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldNotWriteOperationLogForVariablesNull() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .startProcessInstanceById(sourceProcessDefinition.getId());

    rule.getIdentityService().setAuthenticatedUserId("user");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    List<UserOperationLogEntry> operationLogEntries = rule.getHistoryService()
        .createUserOperationLogQuery()
        .list();

    assertThat(operationLogEntries)
        .extracting("operationType", "userId", "property", "newValue")
        .containsExactlyInAnyOrder(
            tuple("Migrate", "user", "processDefinitionId", targetProcessDefinition.getId()),
            tuple("Migrate", "user", "nrOfInstances", "1"),
            tuple("Migrate", "user", "async", "false")
        );
  }

  // helper ////////////////////////////////////////////////////////////////////////////////////////

  public static class ReadTransientVariableExecutionListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) throws Exception {
      Object variable = execution.getVariable("foo");
      assertThat(variable).isNotNull();
    }
  }

}
