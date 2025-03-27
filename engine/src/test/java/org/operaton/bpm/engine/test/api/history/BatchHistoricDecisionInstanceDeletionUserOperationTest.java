/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.api.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class BatchHistoricDecisionInstanceDeletionUserOperationTest {

  protected static final String DECISION = "decision";

  public static final String USER_ID = "userId";

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  protected static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected DecisionService decisionService;
  protected HistoryService historyService;
  protected ManagementService managementService;
  protected IdentityService identityService;

  protected List<String> decisionInstanceIds;

  @BeforeEach
  public void setup() {
    decisionInstanceIds = new ArrayList<>();

    testRule.deploy("org/operaton/bpm/engine/test/api/dmn/Example.dmn");

    VariableMap variables = Variables.createVariables()
        .putValue("status", "silver")
        .putValue("sum", 723);

    for (int i = 0; i < 10; i++) {
      decisionService.evaluateDecisionByKey(DECISION).variables(variables).evaluate();
    }

    List<HistoricDecisionInstance> decisionInstances = historyService.createHistoricDecisionInstanceQuery().list();
    for(HistoricDecisionInstance decisionInstance : decisionInstances) {
      decisionInstanceIds.add(decisionInstance.getId());
    }
  }

  @AfterEach
  public void removeBatches() {
    for (Batch batch : managementService.createBatchQuery().list()) {
      managementService.deleteBatch(batch.getId(), true);
    }

    // remove history of completed batches
    for (HistoricBatch historicBatch : historyService.createHistoricBatchQuery().list()) {
      historyService.deleteHistoricBatch(historicBatch.getId());
    }
  }

  @AfterEach
  public void clearAuthentication() {
    identityService.clearAuthentication();
  }

  @Test
  public void testCreationByIds() {
    // when
    identityService.setAuthenticatedUserId(USER_ID);
    historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, "a-delete-reason");
    identityService.clearAuthentication();

    // then
    List<UserOperationLogEntry> opLogEntries = engineRule.getHistoryService().createUserOperationLogQuery().list();
    assertThat(opLogEntries).hasSize(3);

    Map<String, UserOperationLogEntry> entries = asMap(opLogEntries);

    UserOperationLogEntry asyncEntry = entries.get("async");
    assertThat(asyncEntry).isNotNull();
    assertThat(asyncEntry.getEntityType()).isEqualTo(EntityTypes.DECISION_INSTANCE);
    assertThat(asyncEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    assertThat(asyncEntry.getProcessDefinitionId()).isNull();
    assertThat(asyncEntry.getProcessDefinitionKey()).isNull();
    assertThat(asyncEntry.getProcessInstanceId()).isNull();
    assertThat(asyncEntry.getOrgValue()).isNull();
    assertThat(asyncEntry.getNewValue()).isEqualTo("true");

    UserOperationLogEntry numInstancesEntry = entries.get("nrOfInstances");
    assertThat(numInstancesEntry).isNotNull();
    assertThat(numInstancesEntry.getEntityType()).isEqualTo(EntityTypes.DECISION_INSTANCE);
    assertThat(numInstancesEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    assertThat(numInstancesEntry.getProcessDefinitionId()).isNull();
    assertThat(numInstancesEntry.getProcessDefinitionKey()).isNull();
    assertThat(numInstancesEntry.getProcessInstanceId()).isNull();
    assertThat(numInstancesEntry.getOrgValue()).isNull();
    assertThat(numInstancesEntry.getNewValue()).isEqualTo("10");

    UserOperationLogEntry deleteReasonEntry = entries.get("deleteReason");
    assertThat(deleteReasonEntry).isNotNull();
    assertThat(deleteReasonEntry.getEntityType()).isEqualTo(EntityTypes.DECISION_INSTANCE);
    assertThat(deleteReasonEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    assertThat(deleteReasonEntry.getProcessDefinitionId()).isNull();
    assertThat(deleteReasonEntry.getProcessDefinitionKey()).isNull();
    assertThat(deleteReasonEntry.getProcessInstanceId()).isNull();
    assertThat(deleteReasonEntry.getOrgValue()).isNull();
    assertThat(deleteReasonEntry.getNewValue()).isEqualTo("a-delete-reason");

    assertThat(asyncEntry.getOperationId()).isEqualTo(numInstancesEntry.getOperationId());
    assertThat(deleteReasonEntry.getOperationId()).isEqualTo(asyncEntry.getOperationId());
    assertThat(deleteReasonEntry.getOperationId()).isEqualTo(numInstancesEntry.getOperationId());
  }

  @Test
  public void testCreationByQuery() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    historyService.deleteHistoricDecisionInstancesAsync(query, "a-delete-reason");
    identityService.clearAuthentication();

    // then
    List<UserOperationLogEntry> opLogEntries = engineRule.getHistoryService().createUserOperationLogQuery().list();
    assertThat(opLogEntries).hasSize(3);

    Map<String, UserOperationLogEntry> entries = asMap(opLogEntries);

    UserOperationLogEntry asyncEntry = entries.get("async");
    assertThat(asyncEntry).isNotNull();
    assertThat(asyncEntry.getEntityType()).isEqualTo(EntityTypes.DECISION_INSTANCE);
    assertThat(asyncEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    assertThat(asyncEntry.getProcessDefinitionId()).isNull();
    assertThat(asyncEntry.getProcessDefinitionKey()).isNull();
    assertThat(asyncEntry.getProcessInstanceId()).isNull();
    assertThat(asyncEntry.getOrgValue()).isNull();
    assertThat(asyncEntry.getNewValue()).isEqualTo("true");

    UserOperationLogEntry numInstancesEntry = entries.get("nrOfInstances");
    assertThat(numInstancesEntry).isNotNull();
    assertThat(numInstancesEntry.getEntityType()).isEqualTo(EntityTypes.DECISION_INSTANCE);
    assertThat(numInstancesEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    assertThat(numInstancesEntry.getProcessDefinitionId()).isNull();
    assertThat(numInstancesEntry.getProcessDefinitionKey()).isNull();
    assertThat(numInstancesEntry.getProcessInstanceId()).isNull();
    assertThat(numInstancesEntry.getOrgValue()).isNull();
    assertThat(numInstancesEntry.getNewValue()).isEqualTo("10");

    UserOperationLogEntry deleteReasonEntry = entries.get("deleteReason");
    assertThat(deleteReasonEntry).isNotNull();
    assertThat(deleteReasonEntry.getEntityType()).isEqualTo(EntityTypes.DECISION_INSTANCE);
    assertThat(deleteReasonEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    assertThat(deleteReasonEntry.getProcessDefinitionId()).isNull();
    assertThat(deleteReasonEntry.getProcessDefinitionKey()).isNull();
    assertThat(deleteReasonEntry.getProcessInstanceId()).isNull();
    assertThat(deleteReasonEntry.getOrgValue()).isNull();
    assertThat(deleteReasonEntry.getNewValue()).isEqualTo("a-delete-reason");

    assertThat(asyncEntry.getOperationId()).isEqualTo(deleteReasonEntry.getOperationId());
    assertThat(numInstancesEntry.getOperationId()).isEqualTo(asyncEntry.getOperationId());
    assertThat(deleteReasonEntry.getOperationId()).isEqualTo(numInstancesEntry.getOperationId());
  }

  @Test
  public void testCreationByIdsAndQuery() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, query, "a-delete-reason");
    identityService.clearAuthentication();

    // then
    List<UserOperationLogEntry> opLogEntries = engineRule.getHistoryService().createUserOperationLogQuery().list();
    assertThat(opLogEntries).hasSize(3);

    Map<String, UserOperationLogEntry> entries = asMap(opLogEntries);

    UserOperationLogEntry asyncEntry = entries.get("async");
    assertThat(asyncEntry).isNotNull();
    assertThat(asyncEntry.getEntityType()).isEqualTo(EntityTypes.DECISION_INSTANCE);
    assertThat(asyncEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    assertThat(asyncEntry.getProcessDefinitionId()).isNull();
    assertThat(asyncEntry.getProcessDefinitionKey()).isNull();
    assertThat(asyncEntry.getProcessInstanceId()).isNull();
    assertThat(asyncEntry.getOrgValue()).isNull();
    assertThat(asyncEntry.getNewValue()).isEqualTo("true");

    UserOperationLogEntry numInstancesEntry = entries.get("nrOfInstances");
    assertThat(numInstancesEntry).isNotNull();
    assertThat(numInstancesEntry.getEntityType()).isEqualTo(EntityTypes.DECISION_INSTANCE);
    assertThat(numInstancesEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    assertThat(numInstancesEntry.getProcessDefinitionId()).isNull();
    assertThat(numInstancesEntry.getProcessDefinitionKey()).isNull();
    assertThat(numInstancesEntry.getProcessInstanceId()).isNull();
    assertThat(numInstancesEntry.getOrgValue()).isNull();
    assertThat(numInstancesEntry.getNewValue()).isEqualTo("10");

    UserOperationLogEntry deleteReasonEntry = entries.get("deleteReason");
    assertThat(deleteReasonEntry).isNotNull();
    assertThat(deleteReasonEntry.getEntityType()).isEqualTo(EntityTypes.DECISION_INSTANCE);
    assertThat(deleteReasonEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    assertThat(deleteReasonEntry.getProcessDefinitionId()).isNull();
    assertThat(deleteReasonEntry.getProcessDefinitionKey()).isNull();
    assertThat(deleteReasonEntry.getProcessInstanceId()).isNull();
    assertThat(deleteReasonEntry.getOrgValue()).isNull();
    assertThat(deleteReasonEntry.getNewValue()).isEqualTo("a-delete-reason");

    assertThat(asyncEntry.getOperationId()).isEqualTo(deleteReasonEntry.getOperationId());
    assertThat(numInstancesEntry.getOperationId()).isEqualTo(asyncEntry.getOperationId());
    assertThat(deleteReasonEntry.getOperationId()).isEqualTo(numInstancesEntry.getOperationId());
  }

  @Test
  public void testNoCreationOnSyncBatchJobExecution() {
    // given
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, null);

    // when
    engineRule.getIdentityService().setAuthenticatedUserId(USER_ID);
    executeJobs(batch);
    engineRule.getIdentityService().clearAuthentication();

    // then
    assertThat(engineRule.getHistoryService().createUserOperationLogQuery().entityType(EntityTypes.DECISION_INSTANCE).count()).isZero();
  }

  @Test
  public void testNoCreationOnSyncBatchJobExecutionByIds() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(query, null);

    // when
    engineRule.getIdentityService().setAuthenticatedUserId(USER_ID);
    executeJobs(batch);
    engineRule.getIdentityService().clearAuthentication();

    // then
    assertThat(engineRule.getHistoryService().createUserOperationLogQuery().entityType(EntityTypes.DECISION_INSTANCE).count()).isZero();
  }

  @Test
  public void testNoCreationOnSyncBatchJobExecutionByIdsAndQuery() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);
    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, query, null);

    // when
    engineRule.getIdentityService().setAuthenticatedUserId(USER_ID);
    executeJobs(batch);
    engineRule.getIdentityService().clearAuthentication();

    // then
    assertThat(engineRule.getHistoryService().createUserOperationLogQuery().entityType(EntityTypes.DECISION_INSTANCE).count()).isZero();
  }

  @Test
  public void testNoCreationOnJobExecutorBatchJobExecutionByIds() {
    // given
    // given
    historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, null);

    // when
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    // then
    assertThat(engineRule.getHistoryService().createUserOperationLogQuery().count()).isZero();
  }

  @Test
  public void testNoCreationOnJobExecutorBatchJobExecutionByQuery() {
    // given
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);
    historyService.deleteHistoricDecisionInstancesAsync(query, null);

    // when
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    // then
    assertThat(engineRule.getHistoryService().createUserOperationLogQuery().count()).isZero();
  }

  @Test
  public void testNoCreationOnJobExecutorBatchJobExecutionByIdsAndQuery() {
    // given
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);
    historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, query, null);

    // when
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    // then
    assertThat(engineRule.getHistoryService().createUserOperationLogQuery().count()).isZero();
  }

  protected Map<String, UserOperationLogEntry> asMap(List<UserOperationLogEntry> logEntries) {
    Map<String, UserOperationLogEntry> map = new HashMap<>();

    for (UserOperationLogEntry entry : logEntries) {

      UserOperationLogEntry previousValue = map.put(entry.getProperty(), entry);
      if (previousValue != null) {
        fail("expected only entry for every property");
      }
    }

    return map;
  }

  protected void executeJobs(Batch batch) {
    Job job = managementService.createJobQuery().jobDefinitionId(batch.getSeedJobDefinitionId()).singleResult();

    // seed job
    managementService.executeJob(job.getId());

    for (Job pending : managementService.createJobQuery().jobDefinitionId(batch.getBatchJobDefinitionId()).list()) {
      managementService.executeJob(pending.getId());
    }
  }

}
