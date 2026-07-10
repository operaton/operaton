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
package org.operaton.bpm.engine.test.api.runtime;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.runtime.ExecutionQuery;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.runtime.VariableInstanceQuery;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.batch.BatchExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class CorrelateAllMessageBatchTest {

  protected static final String PROCESS_ONE_KEY = "process";
  protected static final String PROCESS_TWO_KEY = "process-two";
  protected static final String PROCESS_THREE_KEY = "process-three";
  protected static final String MESSAGE_ONE_REF = "message";
  protected static final String MESSAGE_TWO_REF = "message-two";
  protected static final Date TEST_DATE = new Date(1457326800000L);

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension engineTestRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  BatchExtension rule = new BatchExtension(engineRule, engineTestRule);
  BatchHelper helper = new BatchHelper(engineRule);

  RuntimeService runtimeService;
  HistoryService historyService;
  ManagementService managementService;

  @BeforeEach
  void deployProcessIntermediateMessageOne() {
    BpmnModelInstance process = Bpmn.createExecutableProcess(PROCESS_ONE_KEY)
      .startEvent()
      .intermediateCatchEvent("messageCatch")
      .message(MESSAGE_ONE_REF)
      .userTask("task")
      .endEvent()
      .done();
    engineTestRule.deploy(process);
  }

  @AfterEach
  void clearAuthentication() {
    engineRule.getIdentityService().setAuthenticatedUserId(null);
  }

  @AfterEach
  void resetConfiguration() {
    ClockUtil.reset();
    engineRule.getProcessEngineConfiguration()
      .setInvocationsPerBatchJob(ProcessEngineConfigurationImpl.DEFAULT_INVOCATIONS_PER_BATCH_JOB);
  }

  @Test
  void shouldCorrelateAllWithInstanceIds() {
    // given
    deployProcessIntermediateMessageTwo();
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();
    String processInstanceIdThree = runtimeService.startProcessInstanceByKey(PROCESS_TWO_KEY).getId();

    List<String> processInstances = List.of(processInstanceIdOne, processInstanceIdThree);

    Batch batch = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF)
      .processInstanceIds(processInstances)
      .correlateAllAsync();

    ExecutionQuery taskExecutionQueryInstanceOne = runtimeService.createExecutionQuery()
      .activityId("task")
      .processInstanceId(processInstanceIdOne);
    ExecutionQuery taskExecutionQueryInstanceTwo = runtimeService.createExecutionQuery()
      .activityId("task")
      .processInstanceId(processInstanceIdTwo);
    ExecutionQuery taskExecutionQueryInstanceThree = runtimeService.createExecutionQuery()
      .activityId("task")
      .processInstanceId(processInstanceIdThree);

    // assume
    assertThat(taskExecutionQueryInstanceOne.count()).isZero();
    assertThat(taskExecutionQueryInstanceTwo.count()).isZero();
    assertThat(taskExecutionQueryInstanceThree.count()).isZero();

    // when
    rule.syncExec(batch);

    // then
    assertThat(taskExecutionQueryInstanceOne.count()).isOne();
    assertThat(taskExecutionQueryInstanceTwo.count()).isZero();
    assertThat(taskExecutionQueryInstanceThree.count()).isZero();
  }

  @Test
  void shouldCorrelateAllWithInstanceQuery() {
    // given
    deployProcessIntermediateMessageTwo();
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();
    String processInstanceIdThree = runtimeService.startProcessInstanceByKey(PROCESS_TWO_KEY).getId();

    ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery()
      .processInstanceIds(of(processInstanceIdOne, processInstanceIdThree).collect(toSet()));

    Batch batch = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF)
      .processInstanceQuery(runtimeQuery)
      .correlateAllAsync();

    ExecutionQuery taskExecutionQueryInstanceOne = runtimeService.createExecutionQuery()
      .activityId("task")
      .processInstanceId(processInstanceIdOne);
    ExecutionQuery taskExecutionQueryInstanceTwo = runtimeService.createExecutionQuery()
      .activityId("task")
      .processInstanceId(processInstanceIdTwo);
    ExecutionQuery taskExecutionQueryInstanceThree = runtimeService.createExecutionQuery()
      .activityId("task")
      .processInstanceId(processInstanceIdThree);

    // assume
    assertThat(taskExecutionQueryInstanceOne.count()).isZero();
    assertThat(taskExecutionQueryInstanceTwo.count()).isZero();
    assertThat(taskExecutionQueryInstanceThree.count()).isZero();

    // when
    rule.syncExec(batch);

    // then
    assertThat(taskExecutionQueryInstanceOne.count()).isOne();
    assertThat(taskExecutionQueryInstanceTwo.count()).isZero();
    assertThat(taskExecutionQueryInstanceThree.count()).isZero();
  }

  @Test
  void shouldCorrelateAllWithHistoricInstanceQuery() {
    // given
    deployProcessIntermediateMessageTwo();
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();
    String processInstanceIdThree = runtimeService.startProcessInstanceByKey(PROCESS_TWO_KEY).getId();

    HistoricProcessInstanceQuery historyQuery = historyService.createHistoricProcessInstanceQuery()
      .processInstanceIds(of(processInstanceIdOne, processInstanceIdThree).collect(toSet()));

    Batch batch = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF)
      .historicProcessInstanceQuery(historyQuery)
      .correlateAllAsync();

    ExecutionQuery taskExecutionQueryInstanceOne = runtimeService.createExecutionQuery()
      .activityId("task")
      .processInstanceId(processInstanceIdOne);
    ExecutionQuery taskExecutionQueryInstanceTwo = runtimeService.createExecutionQuery()
      .activityId("task")
      .processInstanceId(processInstanceIdTwo);
    ExecutionQuery taskExecutionQueryInstanceThree = runtimeService.createExecutionQuery()
      .activityId("task")
      .processInstanceId(processInstanceIdThree);

    // assume
    assertThat(taskExecutionQueryInstanceOne.count()).isZero();
    assertThat(taskExecutionQueryInstanceTwo.count()).isZero();
    assertThat(taskExecutionQueryInstanceThree.count()).isZero();

    // when
    rule.syncExec(batch);

    // then
    assertThat(taskExecutionQueryInstanceOne.count()).isOne();
    assertThat(taskExecutionQueryInstanceTwo.count()).isZero();
    assertThat(taskExecutionQueryInstanceThree.count()).isZero();
  }

  @Test
  void shouldCorrelateAllWithoutMessage() {
    // given
    deployProcessIntermediateMessageTwo();
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();
    String processInstanceIdThree = runtimeService.startProcessInstanceByKey(PROCESS_TWO_KEY).getId();

    List<String> processInstances = List.of(processInstanceIdOne, processInstanceIdThree);

    Batch batch = runtimeService.createMessageCorrelationAsync(null)
      .processInstanceIds(processInstances)
      .correlateAllAsync();

    ExecutionQuery taskExecutionQueryInstanceOne = runtimeService.createExecutionQuery()
      .activityId("task")
      .processInstanceId(processInstanceIdOne);
    ExecutionQuery taskExecutionQueryInstanceTwo = runtimeService.createExecutionQuery()
      .activityId("task")
      .processInstanceId(processInstanceIdTwo);
    ExecutionQuery taskExecutionQueryInstanceThree = runtimeService.createExecutionQuery()
      .activityId("task")
      .processInstanceId(processInstanceIdThree);

    // assume
    assertThat(taskExecutionQueryInstanceOne.count()).isZero();
    assertThat(taskExecutionQueryInstanceTwo.count()).isZero();
    assertThat(taskExecutionQueryInstanceThree.count()).isZero();

    // when
    rule.syncExec(batch);

    // then
    assertThat(taskExecutionQueryInstanceOne.count()).isOne();
    assertThat(taskExecutionQueryInstanceTwo.count()).isZero();
    assertThat(taskExecutionQueryInstanceThree.count()).isOne();
  }

  @Test
  void shouldNotCorrelateStartMessageEvent() {
    // given
    deployProcessStartMessageOne();
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();

    List<String> processInstances = Collections.singletonList(processInstanceIdOne);

    Batch batch = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF)
      .processInstanceIds(processInstances)
      .correlateAllAsync();

    ExecutionQuery taskExecutionQueryInstanceOne = runtimeService.createExecutionQuery()
      .activityId("task")
      .processInstanceId(processInstanceIdOne);
    ExecutionQuery taskExecutionQueryInstanceThree = runtimeService.createExecutionQuery()
      .activityId("task")
      .processDefinitionKey(PROCESS_THREE_KEY);

    // assume
    assertThat(taskExecutionQueryInstanceOne.count()).isZero();
    assertThat(taskExecutionQueryInstanceThree.count()).isZero();

    // when
    rule.syncExec(batch);

    // then
    assertThat(taskExecutionQueryInstanceOne.count()).isOne();
    assertThat(taskExecutionQueryInstanceThree.count()).isZero();
  }

  @Test
  void shouldSetVariablesOnCorrelation() {
    // given
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();

    List<String> processInstances = List.of(processInstanceIdOne, processInstanceIdTwo);

    Batch batch = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF)
      .processInstanceIds(processInstances)
      .setVariable("foo", "bar")
      .correlateAllAsync();

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // assume
    assertThat(query.list()).extracting("processInstanceId", "name", "value", "batchId")
      .containsExactly(tuple(null, "foo", "bar", batch.getId()));

    // when
    rule.syncExec(batch);

    // then
    assertThat(query.list()).extracting("processInstanceId", "name", "value")
      .containsExactlyInAnyOrder(tuple(processInstanceIdOne, "foo", "bar"), tuple(processInstanceIdTwo, "foo", "bar"));
  }

  @Test
  void shouldThrowException_NoProcessInstancesFound() {
    // given
    var messageCorrelationAsyncBuilder = runtimeService.createMessageCorrelationAsync(
      MESSAGE_ONE_REF).processInstanceIds(Collections.emptyList());
    // when/then
    assertThatThrownBy(messageCorrelationAsyncBuilder::correlateAllAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("process instance ids is empty");
  }

  @Test
  void shouldThrowException_QueriesAndIdsNull() {
    // given
    var messageCorrelationAsync = runtimeService.createMessageCorrelationAsync(
      MESSAGE_ONE_REF);
    // when/then
    assertThatThrownBy(messageCorrelationAsync::correlateAllAsync)
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("No process instances found");

  }

  @Test
  void shouldThrowException_NullProcessInstanceIds() {
    // given
    var messageCorrelationAsyncBuilder = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF);
    // when/then
    assertThatThrownBy(() -> messageCorrelationAsyncBuilder.processInstanceIds(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("processInstanceIds");
  }

  @Test
  void shouldThrowException_NullProcessInstanceQuery() {
    // given
    var messageCorrelationAsyncBuilder = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF);
    // when/then
    assertThatThrownBy(() -> messageCorrelationAsyncBuilder.processInstanceQuery(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("processInstanceQuery");
  }

  @Test
  void shouldThrowException_NullHistoricProcessInstanceQuery() {
    // given
    var messageCorrelationAsyncBuilder = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF);
    // when/then
    assertThatThrownBy(() -> messageCorrelationAsyncBuilder.historicProcessInstanceQuery(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("historicProcessInstanceQuery");
  }

  @Test
  void shouldThrowException_NullVariableName() {
    // given
    var messageCorrelationAsyncBuilder = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF);
    // when/then
    assertThatThrownBy(() -> messageCorrelationAsyncBuilder.setVariable(null, "bar"))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("variableName");
  }

  @Test
  void shouldThrowException_JavaSerializationForbidden() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY);
    ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery();
    var messageCorrelationAsyncBuilder = runtimeService.createMessageCorrelationAsync(
        MESSAGE_ONE_REF)
      .processInstanceQuery(runtimeQuery)
      .setVariables(Variables.putValue("foo", Variables.serializedObjectValue()
        .serializedValue("foo")
        .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
        .create()));

    // when/then
    assertThatThrownBy(messageCorrelationAsyncBuilder::correlateAllAsync)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("ENGINE-17007 Cannot set variable with name foo. " +
          "Java serialization format is prohibited");
  }

  @Test
  void shouldCreateDeploymentAwareBatchJobs_ByIds() {
    // given
    engineRule.getProcessEngineConfiguration().setInvocationsPerBatchJob(2);

    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();
    deployProcessIntermediateMessageOne();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();

    List<Deployment> list = engineRule.getRepositoryService().createDeploymentQuery().list();
    String deploymentIdOne = list.get(0).getId();
    String deploymentIdTwo = list.get(1).getId();

    List<String> processInstances = List.of(processInstanceIdOne, processInstanceIdTwo);

    // when
    Batch batch = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF)
      .processInstanceIds(processInstances)
      .correlateAllAsync();

    rule.executeSeedJobs(batch);

    // then
    List<Job> executionJobs = rule.getExecutionJobs(batch);
    assertThat(executionJobs)
      .extracting("deploymentId")
      .containsExactlyInAnyOrder(deploymentIdOne, deploymentIdTwo);

    // clear
    managementService.deleteBatch(batch.getId(), true);
  }

  @Test
  void shouldCreateDeploymentAwareBatchJobs_ByRuntimeQuery() {
    // given
    engineRule.getProcessEngineConfiguration().setInvocationsPerBatchJob(2);

    runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY);
    deployProcessIntermediateMessageOne();
    runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY);

    List<Deployment> list = engineRule.getRepositoryService().createDeploymentQuery().list();
    String deploymentIdOne = list.get(0).getId();
    String deploymentIdTwo = list.get(1).getId();

    ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery();

    // when
    Batch batch = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF)
      .processInstanceQuery(runtimeQuery)
      .correlateAllAsync();

    rule.executeSeedJobs(batch);

    // then
    List<Job> executionJobs = rule.getExecutionJobs(batch);
    assertThat(executionJobs)
      .extracting("deploymentId")
      .containsExactlyInAnyOrder(deploymentIdOne, deploymentIdTwo);

    // clear
    managementService.deleteBatch(batch.getId(), true);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void shouldCreateDeploymentAwareBatchJobs_ByHistoryQuery() {
    // given
    engineRule.getProcessEngineConfiguration().setInvocationsPerBatchJob(2);

    runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY);
    deployProcessIntermediateMessageOne();
    runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY);

    List<Deployment> list = engineRule.getRepositoryService().createDeploymentQuery().list();
    String deploymentIdOne = list.get(0).getId();
    String deploymentIdTwo = list.get(1).getId();

    HistoricProcessInstanceQuery historyQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    Batch batch = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF)
      .historicProcessInstanceQuery(historyQuery)
      .correlateAllAsync();

    rule.executeSeedJobs(batch);

    // then
    List<Job> executionJobs = rule.getExecutionJobs(batch);
    assertThat(executionJobs)
      .extracting("deploymentId")
      .containsExactlyInAnyOrder(deploymentIdOne, deploymentIdTwo);

    // clear
    managementService.deleteBatch(batch.getId(), true);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldLogOperation() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY);

    engineRule.getIdentityService().setAuthenticatedUserId("demo");

    ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery();

    // when
    Batch batch = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF)
      .processInstanceQuery(runtimeQuery)
      .setVariable("foo", "bar")
      .correlateAllAsync();

    // then
    List<UserOperationLogEntry> logs = historyService.createUserOperationLogQuery().list();

    assertThat(logs)
      .extracting("property", "orgValue", "newValue", "operationType", "entityType", "category", "userId")
      .containsExactlyInAnyOrder(
          tuple("messageName", null, MESSAGE_ONE_REF, "CorrelateMessage", "ProcessInstance", "Operator", "demo"),
          tuple("nrOfInstances", null, "1", "CorrelateMessage", "ProcessInstance", "Operator", "demo"),
          tuple("nrOfVariables", null, "1", "CorrelateMessage", "ProcessInstance", "Operator", "demo"),
          tuple("async", null, "true", "CorrelateMessage", "ProcessInstance", "Operator", "demo"));

    // clear
    managementService.deleteBatch(batch.getId(), true);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldNotLogInstanceOperation() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY);

    ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery();

    // when
    Batch batch = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF)
      .processInstanceQuery(runtimeQuery)
      .setVariable("foo", "bar")
      .correlateAllAsync();

    // then
    List<UserOperationLogEntry> logs = historyService.createUserOperationLogQuery()
      .operationType(UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE)
      .list();

    assertThat(logs).isEmpty();

    // clear
    managementService.deleteBatch(batch.getId(), true);
  }

  @Test
  void shouldCreateProcessInstanceRelatedBatchJobsForSingleInvocations() {
    // given
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();

    List<String> processInstanceIds = List.of(processInstanceIdOne, processInstanceIdTwo);

    // when
    Batch batch = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF)
      .processInstanceIds(processInstanceIds)
      .correlateAllAsync();

    rule.executeSeedJobs(batch);

    // then
    List<Job> executionJobs = rule.getExecutionJobs(batch);
    assertThat(executionJobs)
      .extracting("processInstanceId")
      .containsExactlyInAnyOrder(processInstanceIdOne, processInstanceIdTwo);

    // clear
    managementService.deleteBatch(batch.getId(), true);
  }

  @Test
  void shouldNotCreateProcessInstanceRelatedBatchJobsForMultipleInvocations() {
    // given
    engineRule.getProcessEngineConfiguration().setInvocationsPerBatchJob(2);

    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();

    List<String> processInstanceIds = List.of(processInstanceIdOne, processInstanceIdTwo);

    // when
    Batch batch = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF)
      .processInstanceIds(processInstanceIds)
      .correlateAllAsync();

    rule.executeSeedJobs(batch);

    // then
    List<Job> executionJobs = rule.getExecutionJobs(batch);
    assertThat(executionJobs)
      .extracting("processInstanceId")
      .containsOnlyNulls();

    // clear
    managementService.deleteBatch(batch.getId(), true);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldSetExecutionStartTimeInBatchAndHistory() {
    // given
    ClockUtil.setCurrentTime(TEST_DATE);

    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_ONE_KEY).getId();
    Batch batch = runtimeService.createMessageCorrelationAsync(MESSAGE_ONE_REF)
        .processInstanceIds(Collections.singletonList(processInstanceIdOne))
        .correlateAllAsync();
    helper.executeSeedJob(batch);
    List<Job> executionJobs = helper.getExecutionJobs(batch, Batch.TYPE_CORRELATE_MESSAGE);

    // when
    helper.executeJob(executionJobs.get(0));

    // then
    HistoricBatch historicBatch = historyService.createHistoricBatchQuery().singleResult();
    batch = managementService.createBatchQuery().singleResult();

    assertThat(batch.getExecutionStartTime()).isCloseTo(TEST_DATE, 1000);
    assertThat(historicBatch.getExecutionStartTime()).isCloseTo(TEST_DATE, 1000);

    // clear
    managementService.deleteBatch(batch.getId(), true);
  }

  protected void deployProcessIntermediateMessageTwo() {
    BpmnModelInstance process = Bpmn.createExecutableProcess(PROCESS_TWO_KEY)
      .startEvent()
      .intermediateCatchEvent()
      .message(MESSAGE_TWO_REF)
      .userTask("task")
      .endEvent()
      .done();
    engineTestRule.deploy(process);
  }

  protected void deployProcessStartMessageOne() {
    BpmnModelInstance process = Bpmn.createExecutableProcess(PROCESS_THREE_KEY)
      .startEvent()
      .message(MESSAGE_ONE_REF)
      .userTask("task")
      .endEvent()
      .done();
    engineTestRule.deploy(process);
  }

}
