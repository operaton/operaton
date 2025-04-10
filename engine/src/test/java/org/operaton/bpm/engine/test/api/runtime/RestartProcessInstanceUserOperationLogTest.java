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
import static org.assertj.core.api.Assertions.fail;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 *
 * @author Anna Pazola
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class RestartProcessInstanceUserOperationLogTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder()
      .cacheForConfigurationResource(false)
      .build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(rule);
  BatchRestartHelper helper = new BatchRestartHelper(rule);

  RuntimeService runtimeService;
  
  BpmnModelInstance instance;
  static final Date START_DATE = new Date(1457326800000L);

  @BeforeEach
  void setClock() {
    ClockUtil.setCurrentTime(START_DATE);
  }

  @AfterEach
  void resetEngineConfig() {
    rule.getProcessEngineConfiguration()
        .setRestrictUserOperationLogToAuthenticatedUsers(true);
  }

  @BeforeEach
  void createBpmnModelInstance() {
    this.instance = Bpmn.createExecutableProcess("process1")
        .startEvent("start")
        .userTask("user1")
        .sequenceFlowId("seq")
        .userTask("user2")
        .endEvent("end")
        .done();
  }

  @AfterEach
  void resetClock() {
    ClockUtil.reset();
  }

  @AfterEach
  void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @Test
  void testLogCreationAsync() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    rule.getIdentityService().setAuthenticatedUserId("userId");

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("process1");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("process1");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId()).startAfterActivity("user1").processInstanceIds(processInstance1.getId(), processInstance2.getId()).executeAsync();
    rule.getIdentityService().clearAuthentication();

    // then
    List<UserOperationLogEntry> opLogEntries = rule.getHistoryService().createUserOperationLogQuery().operationType("RestartProcessInstance").list();
    assertThat(opLogEntries).hasSize(2);

    Map<String, UserOperationLogEntry> entries = asMap(opLogEntries);


    UserOperationLogEntry asyncEntry = entries.get("async");
    assertThat(asyncEntry).isNotNull();
    assertThat(asyncEntry.getEntityType()).isEqualTo("ProcessInstance");
    assertThat(asyncEntry.getOperationType()).isEqualTo("RestartProcessInstance");
    assertThat(asyncEntry.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
    assertThat(asyncEntry.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
    assertThat(asyncEntry.getProcessInstanceId()).isNull();
    assertThat(asyncEntry.getOrgValue()).isNull();
    assertThat(asyncEntry.getNewValue()).isEqualTo("true");
    assertThat(asyncEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry numInstancesEntry = entries.get("nrOfInstances");
    assertThat(numInstancesEntry).isNotNull();
    assertThat(numInstancesEntry.getEntityType()).isEqualTo("ProcessInstance");
    assertThat(numInstancesEntry.getOperationType()).isEqualTo("RestartProcessInstance");
    assertThat(numInstancesEntry.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
    assertThat(numInstancesEntry.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
    assertThat(numInstancesEntry.getProcessInstanceId()).isNull();
    assertThat(numInstancesEntry.getOrgValue()).isNull();
    assertThat(numInstancesEntry.getNewValue()).isEqualTo("2");
    assertThat(numInstancesEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertThat(numInstancesEntry.getOperationId()).isEqualTo(asyncEntry.getOperationId());
  }

  @Test
  void testLogCreationSync() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    rule.getIdentityService().setAuthenticatedUserId("userId");

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("process1");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("process1");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    runtimeService.restartProcessInstances(processDefinition.getId()).startAfterActivity("user1").processInstanceIds(processInstance1.getId(), processInstance2.getId()).execute();
    rule.getIdentityService().clearAuthentication();

    // then
    List<UserOperationLogEntry> opLogEntries = rule.getHistoryService().createUserOperationLogQuery().operationType("RestartProcessInstance").list();
    assertThat(opLogEntries).hasSize(2);

    Map<String, UserOperationLogEntry> entries = asMap(opLogEntries);


    UserOperationLogEntry asyncEntry = entries.get("async");
    assertThat(asyncEntry).isNotNull();
    assertThat(asyncEntry.getEntityType()).isEqualTo("ProcessInstance");
    assertThat(asyncEntry.getOperationType()).isEqualTo("RestartProcessInstance");
    assertThat(asyncEntry.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
    assertThat(asyncEntry.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
    assertThat(asyncEntry.getProcessInstanceId()).isNull();
    assertThat(asyncEntry.getOrgValue()).isNull();
    assertThat(asyncEntry.getNewValue()).isEqualTo("false");
    assertThat(asyncEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry numInstancesEntry = entries.get("nrOfInstances");
    assertThat(numInstancesEntry).isNotNull();
    assertThat(numInstancesEntry.getEntityType()).isEqualTo("ProcessInstance");
    assertThat(numInstancesEntry.getOperationType()).isEqualTo("RestartProcessInstance");
    assertThat(numInstancesEntry.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
    assertThat(numInstancesEntry.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
    assertThat(numInstancesEntry.getProcessInstanceId()).isNull();
    assertThat(numInstancesEntry.getOrgValue()).isNull();
    assertThat(numInstancesEntry.getNewValue()).isEqualTo("2");
    assertThat(numInstancesEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertThat(numInstancesEntry.getOperationId()).isEqualTo(asyncEntry.getOperationId());
  }

  @Test
  void testNoCreationOnSyncBatchJobExecution() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId()).startBeforeActivity("user1").processInstanceIds(processInstance.getId()).executeAsync();

    helper.completeSeedJobs(batch);

    // when
    rule.getIdentityService().setAuthenticatedUserId("userId");
    helper.executeJobs(batch);
    rule.getIdentityService().clearAuthentication();

    // then
    assertThat(rule.getHistoryService().createUserOperationLogQuery().entityType(EntityTypes.PROCESS_INSTANCE).count()).isZero();
  }

  @Test
  void shouldNotLogOnSyncExecutionUnauthenticated() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    Batch batch = runtimeService.restartProcessInstances(processDefinition.getId()).startBeforeActivity("user1").processInstanceIds(processInstance.getId()).executeAsync();

    helper.completeSeedJobs(batch);

    rule.getProcessEngineConfiguration().setRestrictUserOperationLogToAuthenticatedUsers(false);

    // when
    helper.executeJobs(batch);

    // then
    assertThat(rule.getHistoryService().createUserOperationLogQuery().entityType(EntityTypes.PROCESS_INSTANCE).count()).isZero();
  }

  @Test
  void testNoCreationOnJobExecutorBatchJobExecution() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    runtimeService.restartProcessInstances(processDefinition.getId())
      .startAfterActivity("user1")
      .processInstanceIds(List.of(processInstance.getId()))
      .executeAsync();

    // when
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    // then
    assertThat(rule.getHistoryService().createUserOperationLogQuery().count()).isZero();
  }

  @Test
  void shouldNotLogOnExecutionUnauthenticated() {
    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    runtimeService.restartProcessInstances(processDefinition.getId())
      .startAfterActivity("user1")
      .processInstanceIds(List.of(processInstance.getId()))
      .executeAsync();

    rule.getProcessEngineConfiguration().setRestrictUserOperationLogToAuthenticatedUsers(false);

    // when
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    // then
    assertThat(rule.getHistoryService().createUserOperationLogQuery().count()).isZero();
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
}
