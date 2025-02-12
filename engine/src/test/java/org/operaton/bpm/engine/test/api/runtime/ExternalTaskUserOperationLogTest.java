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
package org.operaton.bpm.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * 
 * @author Tobias Metzke
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class ExternalTaskUserOperationLogTest {

  protected ProcessEngineRule rule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(rule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(rule).around(testRule);

  private static String PROCESS_DEFINITION_KEY = "oneExternalTaskProcess";
  private static String PROCESS_DEFINITION_KEY_2 = "twoExternalTaskWithPriorityProcess";

  protected RuntimeService runtimeService;
  protected ExternalTaskService externalTaskService;

  @Before
  public void initServices() {
    runtimeService = rule.getRuntimeService();
    externalTaskService = rule.getExternalTaskService();
  }
  
  @After
  public void removeAllRunningAndHistoricBatches() {
    HistoryService historyService = rule.getHistoryService();
    ManagementService managementService = rule.getManagementService();
    for (Batch batch : managementService.createBatchQuery().list()) {
      managementService.deleteBatch(batch.getId(), true);
    }
    // remove history of completed batches
    for (HistoricBatch historicBatch : historyService.createHistoricBatchQuery().list()) {
      historyService.deleteHistoricBatch(historicBatch.getId());
    }
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  public void testSetRetriesLogCreationForOneExternalTaskId() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
    rule.getIdentityService().setAuthenticatedUserId("userId");

    // when
    ExternalTask externalTask = externalTaskService.createExternalTaskQuery().singleResult();
    externalTaskService.setRetries(externalTask.getId(), 5);
    rule.getIdentityService().clearAuthentication();
    // then
    List<UserOperationLogEntry> opLogEntries = rule.getHistoryService().createUserOperationLogQuery().list();
    assertThat(opLogEntries).hasSize(1);

    Map<String, UserOperationLogEntry> entries = asMap(opLogEntries);

    UserOperationLogEntry retriesEntry = entries.get("retries");
    Assert.assertNotNull(retriesEntry);
    assertThat(retriesEntry.getEntityType()).isEqualTo(EntityTypes.EXTERNAL_TASK);
    assertThat(retriesEntry.getOperationType()).isEqualTo("SetExternalTaskRetries");
    assertThat(retriesEntry.getExternalTaskId()).isEqualTo(externalTask.getId());
    assertThat(retriesEntry.getProcessInstanceId()).isEqualTo(externalTask.getProcessInstanceId());
    assertThat(retriesEntry.getProcessDefinitionId()).isEqualTo(externalTask.getProcessDefinitionId());
    assertThat(retriesEntry.getProcessDefinitionKey()).isEqualTo(externalTask.getProcessDefinitionKey());
    Assert.assertNull(retriesEntry.getOrgValue());
    assertThat(retriesEntry.getNewValue()).isEqualTo("5");
    assertThat(retriesEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  public void testSetRetriesLogCreationSync() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
    
    List<ExternalTask> list = externalTaskService.createExternalTaskQuery().list();
    List<String> externalTaskIds = new ArrayList<>();

    for (ExternalTask task : list) {
      externalTaskIds.add(task.getId());
    }

    // when
    rule.getIdentityService().setAuthenticatedUserId("userId");
    externalTaskService.setRetries(externalTaskIds, 5);
    rule.getIdentityService().clearAuthentication();
    // then
    List<UserOperationLogEntry> opLogEntries = rule.getHistoryService().createUserOperationLogQuery().list();
    assertThat(opLogEntries).hasSize(3);

    Map<String, UserOperationLogEntry> entries = asMap(opLogEntries);

    UserOperationLogEntry asyncEntry = entries.get("async");
    Assert.assertNotNull(asyncEntry);
    assertThat(asyncEntry.getEntityType()).isEqualTo(EntityTypes.EXTERNAL_TASK);
    assertThat(asyncEntry.getOperationType()).isEqualTo("SetExternalTaskRetries");
    Assert.assertNull(asyncEntry.getExternalTaskId());
    Assert.assertNull(asyncEntry.getProcessDefinitionId());
    Assert.assertNull(asyncEntry.getProcessDefinitionKey());
    Assert.assertNull(asyncEntry.getProcessInstanceId());
    Assert.assertNull(asyncEntry.getOrgValue());
    assertThat(asyncEntry.getNewValue()).isEqualTo("false");
    assertThat(asyncEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry numInstancesEntry = entries.get("nrOfInstances");
    Assert.assertNotNull(numInstancesEntry);
    assertThat(numInstancesEntry.getEntityType()).isEqualTo(EntityTypes.EXTERNAL_TASK);
    assertThat(numInstancesEntry.getOperationType()).isEqualTo("SetExternalTaskRetries");
    Assert.assertNull(numInstancesEntry.getExternalTaskId());
    Assert.assertNull(numInstancesEntry.getProcessDefinitionId());
    Assert.assertNull(numInstancesEntry.getProcessDefinitionKey());
    Assert.assertNull(numInstancesEntry.getProcessInstanceId());
    Assert.assertNull(numInstancesEntry.getOrgValue());
    assertThat(numInstancesEntry.getNewValue()).isEqualTo("2");
    assertThat(numInstancesEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry retriesEntry = entries.get("retries");
    Assert.assertNotNull(retriesEntry);
    assertThat(retriesEntry.getEntityType()).isEqualTo(EntityTypes.EXTERNAL_TASK);
    assertThat(retriesEntry.getOperationType()).isEqualTo("SetExternalTaskRetries");
    Assert.assertNull(retriesEntry.getExternalTaskId());
    Assert.assertNull(retriesEntry.getProcessDefinitionId());
    Assert.assertNull(retriesEntry.getProcessDefinitionKey());
    Assert.assertNull(retriesEntry.getProcessInstanceId());
    Assert.assertNull(retriesEntry.getOrgValue());
    assertThat(retriesEntry.getNewValue()).isEqualTo("5");
    assertThat(retriesEntry.getOperationId()).isEqualTo(asyncEntry.getOperationId());
    assertThat(retriesEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  public void testSetRetriesLogCreationAsync() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    // when
    rule.getIdentityService().setAuthenticatedUserId("userId");
    externalTaskService.setRetriesAsync(null, externalTaskService.createExternalTaskQuery(), 5);
    rule.getIdentityService().clearAuthentication();
    // then
    List<UserOperationLogEntry> opLogEntries = rule.getHistoryService().createUserOperationLogQuery().list();
    assertThat(opLogEntries).hasSize(3);

    Map<String, UserOperationLogEntry> entries = asMap(opLogEntries);

    UserOperationLogEntry asyncEntry = entries.get("async");
    Assert.assertNotNull(asyncEntry);
    assertThat(asyncEntry.getEntityType()).isEqualTo(EntityTypes.EXTERNAL_TASK);
    assertThat(asyncEntry.getOperationType()).isEqualTo("SetExternalTaskRetries");
    Assert.assertNull(asyncEntry.getExternalTaskId());
    Assert.assertNull(asyncEntry.getProcessDefinitionId());
    Assert.assertNull(asyncEntry.getProcessDefinitionKey());
    Assert.assertNull(asyncEntry.getProcessInstanceId());
    Assert.assertNull(asyncEntry.getOrgValue());
    assertThat(asyncEntry.getNewValue()).isEqualTo("true");
    assertThat(asyncEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry numInstancesEntry = entries.get("nrOfInstances");
    Assert.assertNotNull(numInstancesEntry);
    assertThat(numInstancesEntry.getEntityType()).isEqualTo(EntityTypes.EXTERNAL_TASK);
    assertThat(numInstancesEntry.getOperationType()).isEqualTo("SetExternalTaskRetries");
    Assert.assertNull(numInstancesEntry.getExternalTaskId());
    Assert.assertNull(numInstancesEntry.getProcessDefinitionId());
    Assert.assertNull(numInstancesEntry.getProcessDefinitionKey());
    Assert.assertNull(numInstancesEntry.getProcessInstanceId());
    Assert.assertNull(numInstancesEntry.getOrgValue());
    assertThat(numInstancesEntry.getNewValue()).isEqualTo("2");
    assertThat(numInstancesEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry retriesEntry = entries.get("retries");
    Assert.assertNotNull(retriesEntry);
    assertThat(retriesEntry.getEntityType()).isEqualTo(EntityTypes.EXTERNAL_TASK);
    assertThat(retriesEntry.getOperationType()).isEqualTo("SetExternalTaskRetries");
    Assert.assertNull(retriesEntry.getExternalTaskId());
    Assert.assertNull(retriesEntry.getProcessDefinitionId());
    Assert.assertNull(retriesEntry.getProcessDefinitionKey());
    Assert.assertNull(retriesEntry.getProcessInstanceId());
    Assert.assertNull(retriesEntry.getOrgValue());
    assertThat(retriesEntry.getNewValue()).isEqualTo("5");
    assertThat(retriesEntry.getOperationId()).isEqualTo(asyncEntry.getOperationId());
    assertThat(retriesEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }
  
  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityExpression.bpmn20.xml")
  public void testSetPriorityLogCreation() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY_2, Collections.<String, Object>singletonMap("priority", 14));
    ExternalTask externalTask = externalTaskService.createExternalTaskQuery().priorityHigherThanOrEquals(1).singleResult();
    
    // when
    rule.getIdentityService().setAuthenticatedUserId("userId");
    externalTaskService.setPriority(externalTask.getId(), 78L);
    rule.getIdentityService().clearAuthentication();
    
    // then
    List<UserOperationLogEntry> opLogEntries = rule.getHistoryService().createUserOperationLogQuery().list();
    assertThat(opLogEntries).hasSize(1);

    UserOperationLogEntry entry = opLogEntries.get(0);
    Assert.assertNotNull(entry);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.EXTERNAL_TASK);
    assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_PRIORITY);
    assertThat(entry.getExternalTaskId()).isEqualTo(externalTask.getId());
    assertThat(entry.getProcessInstanceId()).isEqualTo(externalTask.getProcessInstanceId());
    assertThat(entry.getProcessDefinitionId()).isEqualTo(externalTask.getProcessDefinitionId());
    assertThat(entry.getProcessDefinitionKey()).isEqualTo(externalTask.getProcessDefinitionKey());
    assertThat(entry.getProperty()).isEqualTo("priority");
    assertThat(entry.getOrgValue()).isEqualTo("14");
    assertThat(entry.getNewValue()).isEqualTo("78");
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }
  
  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  public void testUnlockLogCreation() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
    ExternalTask externalTask = externalTaskService.createExternalTaskQuery().singleResult();
    externalTaskService.fetchAndLock(1, "aWorker").topic(externalTask.getTopicName(), 3000L).execute();
    
    // when
    rule.getIdentityService().setAuthenticatedUserId("userId");
    externalTaskService.unlock(externalTask.getId());
    rule.getIdentityService().clearAuthentication();
    
    // then
    List<UserOperationLogEntry> opLogEntries = rule.getHistoryService().createUserOperationLogQuery().list();
    assertThat(opLogEntries).hasSize(1);

    UserOperationLogEntry entry = opLogEntries.get(0);
    Assert.assertNotNull(entry);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.EXTERNAL_TASK);
    assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_UNLOCK);
    assertThat(entry.getExternalTaskId()).isEqualTo(externalTask.getId());
    assertThat(entry.getProcessInstanceId()).isEqualTo(externalTask.getProcessInstanceId());
    assertThat(entry.getProcessDefinitionId()).isEqualTo(externalTask.getProcessDefinitionId());
    assertThat(entry.getProcessDefinitionKey()).isEqualTo(externalTask.getProcessDefinitionKey());
    Assert.assertNull(entry.getProperty());
    Assert.assertNull(entry.getOrgValue());
    Assert.assertNull(entry.getNewValue());
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  protected Map<String, UserOperationLogEntry> asMap(List<UserOperationLogEntry> logEntries) {
    Map<String, UserOperationLogEntry> map = new HashMap<>();

    for (UserOperationLogEntry entry : logEntries) {

      UserOperationLogEntry previousValue = map.put(entry.getProperty(), entry);
      if (previousValue != null) {
        Assert.fail("expected only entry for every property");
      }
    }

    return map;
  }
}
