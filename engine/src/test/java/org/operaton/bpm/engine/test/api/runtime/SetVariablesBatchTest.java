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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.runtime.VariableInstanceQuery;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.dmn.businessruletask.TestPojo;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.batch.BatchExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.util.ExecutableProcessUtil.USER_TASK_PROCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class SetVariablesBatchTest {

  protected static final String PROCESS_KEY = "process";
  protected static final Date TEST_DATE = new Date(1457326800000L);

  protected static final VariableMap SINGLE_VARIABLE = Variables.putValue("foo", "bar");

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension engineTestRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  BatchExtension batchRule = new BatchExtension(engineRule, engineTestRule);
  BatchHelper helper = new BatchHelper(engineRule);

  RuntimeService runtimeService;
  HistoryService historyService;
  ManagementService managementService;

  @BeforeEach
  void deployProcess() {
    BpmnModelInstance process = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
          .userTask()
        .endEvent()
        .done();
    engineTestRule.deploy(process);
  }

  @AfterEach
  void clearAuthentication() {
    ClockUtil.reset();
    engineRule.getIdentityService()
        .setAuthenticatedUserId(null);
  }

  @Test
  void shouldSetByIds() {
    // given
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    List<String> processInstances = List.of(processInstanceIdOne, processInstanceIdTwo);

    Batch batch = runtimeService.setVariablesAsync(processInstances, SINGLE_VARIABLE);

    // assume
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    assertThat(query.list())
        .extracting("processInstanceId", "name", "value", "batchId")
          .containsExactly(tuple(null, "foo", "bar", batch.getId()));

    // when
    batchRule.syncExec(batch);

    // then
    assertThat(query.list())
        .extracting("processInstanceId", "name", "value")
        .containsExactlyInAnyOrder(
            tuple(processInstanceIdOne, "foo", "bar"),
            tuple(processInstanceIdTwo, "foo", "bar")
        );
  }

  @Test
  void shouldSetByIds_TypedValue() {
    // given
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    List<String> processInstances = List.of(processInstanceIdOne, processInstanceIdTwo);

    Batch batch = runtimeService.setVariablesAsync(processInstances,
        Variables.putValue("foo", Variables.stringValue("bar")));

    // assume
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    assertThat(query.list())
        .extracting("processInstanceId", "name", "value", "batchId")
          .containsExactly(tuple(null, "foo", "bar", batch.getId()));

    // when
    batchRule.syncExec(batch);

    // then
    assertThat(query.list())
        .extracting("processInstanceId", "name", "value")
        .containsExactlyInAnyOrder(
            tuple(processInstanceIdOne, "foo", "bar"),
            tuple(processInstanceIdTwo, "foo", "bar")
        );

  }

  @Test
  void shouldSetByIds_MixedTypedAndUntypedValues() {
    // given
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    List<String> processInstances = List.of(processInstanceIdOne, processInstanceIdTwo);

    Batch batch = runtimeService.setVariablesAsync(processInstances,
        Variables
            .putValue("foo", Variables.stringValue("bar"))
            .putValue("bar", "foo"));

    // assume
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    assertThat(query.list())
        .extracting("processInstanceId", "name", "value", "batchId")
          .containsExactlyInAnyOrder(
              tuple(null, "foo", "bar", batch.getId()),
              tuple(null, "bar", "foo", batch.getId())
          );

    // when
    batchRule.syncExec(batch);

    // then
    assertThat(query.list())
        .extracting("processInstanceId", "name", "value")
        .containsExactlyInAnyOrder(
            tuple(processInstanceIdOne, "foo", "bar"),
            tuple(processInstanceIdOne, "bar", "foo"),

            tuple(processInstanceIdTwo, "foo", "bar"),
            tuple(processInstanceIdTwo, "bar", "foo")
        );

  }

  @Test
  void shouldSetByIds_ObjectValue() {
    // given
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    List<String> processInstances = List.of(processInstanceIdOne, processInstanceIdTwo);

    TestPojo pojo = new TestPojo("bar", 3D);
    Batch batch = runtimeService.setVariablesAsync(processInstances,
        Variables.putValue("foo", Variables.objectValue(pojo)));

    // assume
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    assertThat(query.list())
        .extracting("processInstanceId", "name", "value", "batchId")
        .containsExactly(tuple(null, "foo", pojo, batch.getId()));

    // when
    batchRule.syncExec(batch);

    // then
    assertThat(query.list())
        .extracting("processInstanceId", "name", "value")
        .containsExactlyInAnyOrder(
            tuple(processInstanceIdOne, "foo", pojo),
            tuple(processInstanceIdTwo, "foo", pojo)
        );
  }

  @Test
  void shouldSetByIds_MultipleVariables() {
    // given
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    List<String> processInstances = List.of(processInstanceIdOne, processInstanceIdTwo);

    Batch batch = runtimeService.setVariablesAsync(processInstances,
        Variables
            .putValue("variableOne", "string")
            .putValue("variableTwo", 42)
            .putValue("variableThree", (short) 3));

    // assume
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    assertThat(query.list())
        .extracting("processInstanceId", "name", "value", "batchId")
          .containsExactlyInAnyOrder(
              tuple(null, "variableOne", "string", batch.getId()),
              tuple(null, "variableTwo", 42, batch.getId()),
              tuple(null, "variableThree", (short) 3, batch.getId())
          );

    // when
    batchRule.syncExec(batch);

    // then
    assertThat(query.list())
        .extracting("processInstanceId", "name", "value")
        .containsExactlyInAnyOrder(
            tuple(processInstanceIdOne, "variableOne", "string"),
            tuple(processInstanceIdOne, "variableTwo", 42),
            tuple(processInstanceIdOne, "variableThree", (short) 3),

            tuple(processInstanceIdTwo, "variableOne", "string"),
            tuple(processInstanceIdTwo, "variableTwo", 42),
            tuple(processInstanceIdTwo, "variableThree", (short) 3)
        );
  }

  @Test
  void shouldSetByIds_VariablesAsMap() {
    // given
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    List<String> processInstanceIds = List.of(processInstanceIdOne, processInstanceIdTwo);

    Map<String, Object> variablesMap = new HashMap<>();
    variablesMap.put("foo", "bar");
    variablesMap.put("bar", "foo");

    Batch batch = runtimeService.setVariablesAsync(processInstanceIds, variablesMap);

    // assume
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    assertThat(query.list())
        .extracting("processInstanceId", "name", "value", "batchId")
          .containsExactlyInAnyOrder(
              tuple(null, "foo", "bar", batch.getId()),
              tuple(null, "bar", "foo", batch.getId())
          );

    // when
    batchRule.syncExec(batch);

    // then
    assertThat(query.list())
        .extracting("processInstanceId", "name", "value")
        .containsExactlyInAnyOrder(
            tuple(processInstanceIdOne, "foo", "bar"),
            tuple(processInstanceIdOne, "bar", "foo"),

            tuple(processInstanceIdTwo, "foo", "bar"),
            tuple(processInstanceIdTwo, "bar", "foo")
        );
  }

  @Test
  void shouldSetByRuntimeQuery() {
    // given
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery();
    Batch batch = runtimeService.setVariablesAsync(runtimeQuery, SINGLE_VARIABLE);

    // assume
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    assertThat(query.list())
        .extracting("processInstanceId", "name", "value", "batchId")
          .containsExactly(tuple(null, "foo", "bar", batch.getId()));

    // when
    batchRule.syncExec(batch);

    // then
    assertThat(query.list())
        .extracting("processInstanceId", "name", "value")
        .containsExactlyInAnyOrder(
            tuple(processInstanceIdOne, "foo", "bar"),
            tuple(processInstanceIdTwo, "foo", "bar")
        );
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void shouldSetByIdsAndQueries() {
    // given
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    String processInstanceIdThree = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    ProcessInstanceQuery runtimeQuery =
        runtimeService.createProcessInstanceQuery()
            .processInstanceId(processInstanceIdTwo);

    HistoricProcessInstanceQuery historyQuery =
        historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(processInstanceIdThree);

    Batch batch = runtimeService.setVariablesAsync(Collections.singletonList(processInstanceIdOne),
        runtimeQuery,
        historyQuery,
        SINGLE_VARIABLE);

    // assume
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    assertThat(query.list())
        .extracting("processInstanceId", "name", "value", "batchId")
          .containsExactly(tuple(null, "foo", "bar", batch.getId()));

    // when
    batchRule.syncExec(batch);

    // then
    assertThat(query.list())
        .extracting("processInstanceId", "name", "value")
        .containsExactlyInAnyOrder(
            tuple(processInstanceIdOne, "foo", "bar"),
            tuple(processInstanceIdTwo, "foo", "bar"),
            tuple(processInstanceIdThree, "foo", "bar")
        );
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void shouldSetByHistoryQuery() {
    // given
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    HistoricProcessInstanceQuery historyQuery = historyService.createHistoricProcessInstanceQuery();
    Batch batch = runtimeService.setVariablesAsync(historyQuery, SINGLE_VARIABLE);

    // assume
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    assertThat(query.list())
        .extracting("processInstanceId", "name", "value", "batchId")
          .containsExactly(tuple(null, "foo", "bar", batch.getId()));

    // when
    batchRule.syncExec(batch);

    // then
    assertThat(query.list())
        .extracting("processInstanceId", "name", "value")
        .containsExactlyInAnyOrder(
            tuple(processInstanceIdOne, "foo", "bar"),
            tuple(processInstanceIdTwo, "foo", "bar")
        );
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void shouldSetByHistoryQuery_WithFinishedInstances() {
    // given
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    runtimeService.deleteProcessInstance(processInstanceIdOne, "dunno");

    HistoricProcessInstanceQuery historyQuery = historyService.createHistoricProcessInstanceQuery();
    Batch batch = runtimeService.setVariablesAsync(historyQuery, SINGLE_VARIABLE);

    // assume
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    assertThat(query.list())
        .extracting("processInstanceId", "name", "value", "batchId")
          .containsExactly(tuple(null, "foo", "bar", batch.getId()));

    // when
    batchRule.syncExec(batch);

    // then
    assertThat(query.list())
        .extracting("processInstanceId", "name", "value")
        .containsExactlyInAnyOrder(
            tuple(processInstanceIdTwo, "foo", "bar")
        );
  }

  @Test
  void shouldThrowException_TransientVariable() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    List<String> processInstanceIds = Collections.singletonList(processInstanceId);
    var variables = Variables.putValue("foo", Variables.stringValue("bar", true));

    // when/then
    assertThatThrownBy(() -> runtimeService.setVariablesAsync(processInstanceIds, variables))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("ENGINE-13044 Setting transient variable 'foo' " +
          "asynchronously is currently not supported.");
  }

  @Test
  void shouldThrowException_JavaSerializationForbidden() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery();
    var variables = Variables.putValue("foo", Variables.serializedObjectValue()
      .serializedValue("foo")
      .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
      .create());

    // when/then
    assertThatThrownBy(() -> runtimeService.setVariablesAsync(runtimeQuery, variables))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("ENGINE-17007 Cannot set variable with name foo. " +
          "Java serialization format is prohibited");
  }

  @Test
  void shouldThrowException_NoProcessInstancesFound() {
    // given
    ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery();

    // when/then
    assertThatThrownBy(() -> runtimeService.setVariablesAsync(runtimeQuery, SINGLE_VARIABLE))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("processInstanceIds is empty");
  }

  @Test
  void shouldThrowException_QueriesAndIdsNull() {
    // when/then
    assertThatThrownBy(() -> runtimeService.setVariablesAsync(null,
        null,
        null,
        SINGLE_VARIABLE))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("No process instances found.");

  }

  @Test
  void shouldThrowException_VariablesNull() {
    // given
    ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery();

    // when/then
    assertThatThrownBy(() -> runtimeService.setVariablesAsync(runtimeQuery, null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("variables is null");

  }

  @Test
  void shouldThrowException_VariablesEmpty() {
    // given
    ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery();
    Map<String, Object> variables = Collections.emptyMap();

    // when/then
    assertThatThrownBy(() -> runtimeService.setVariablesAsync(runtimeQuery, variables))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("variables is empty");
  }

  @Test
  void shouldCreateDeploymentAwareBatchJobs_ByIds() {
    // given
    engineRule.getProcessEngineConfiguration()
      .setInvocationsPerBatchJob(2);

    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    deployProcess();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    List<Deployment> list = engineRule.getRepositoryService().createDeploymentQuery().list();
    String deploymentIdOne = list.get(0).getId();
    String deploymentIdTwo = list.get(1).getId();

    List<String> processInstanceIds = List.of(processInstanceIdOne, processInstanceIdTwo);

    // when
    Batch batch = runtimeService.setVariablesAsync(processInstanceIds, SINGLE_VARIABLE);

    batchRule.executeSeedJobs(batch);

    // then
    List<Job> executionJobs = batchRule.getExecutionJobs(batch);
    assertThat(executionJobs)
        .extracting("deploymentId")
        .containsExactlyInAnyOrder(deploymentIdOne, deploymentIdTwo);

    // clear
    managementService.deleteBatch(batch.getId(), true);
  }

  @Test
  void shouldCreateDeploymentAwareBatchJobs_ByRuntimeQuery() {
    // given
    engineRule.getProcessEngineConfiguration()
      .setInvocationsPerBatchJob(2);

    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    deployProcess();
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);

    List<Deployment> list = engineRule.getRepositoryService().createDeploymentQuery().list();
    String deploymentIdOne = list.get(0).getId();
    String deploymentIdTwo = list.get(1).getId();

    ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery();

    // when
    Batch batch = runtimeService.setVariablesAsync(runtimeQuery, SINGLE_VARIABLE);

    batchRule.executeSeedJobs(batch);

    // then
    List<Job> executionJobs = batchRule.getExecutionJobs(batch);
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
    engineRule.getProcessEngineConfiguration()
      .setInvocationsPerBatchJob(2);

    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    deployProcess();
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);

    List<Deployment> list = engineRule.getRepositoryService().createDeploymentQuery().list();
    String deploymentIdOne = list.get(0).getId();
    String deploymentIdTwo = list.get(1).getId();

    HistoricProcessInstanceQuery historyQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    Batch batch = runtimeService.setVariablesAsync(historyQuery, SINGLE_VARIABLE);

    batchRule.executeSeedJobs(batch);

    // then
    List<Job> executionJobs = batchRule.getExecutionJobs(batch);
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
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);

    engineRule.getIdentityService()
        .setAuthenticatedUserId("demo");

    ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery();

    // when
    Batch batch = runtimeService.setVariablesAsync(runtimeQuery, SINGLE_VARIABLE);

    // then
    List<UserOperationLogEntry> logs = historyService.createUserOperationLogQuery()
        .list();

    assertThat(logs)
        .extracting("property", "orgValue", "newValue", "operationType",
            "entityType", "category", "userId")
        .containsExactlyInAnyOrder(
            tuple("nrOfInstances", null, "1", "SetVariables", "ProcessInstance", "Operator", "demo"),
            tuple("nrOfVariables", null, "1", "SetVariables", "ProcessInstance", "Operator", "demo"),
            tuple("async", null, "true", "SetVariables", "ProcessInstance", "Operator", "demo")
        );

    // clear
    managementService.deleteBatch(batch.getId(), true);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldNotLogInstanceOperation() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);

    ProcessInstanceQuery runtimeQuery = runtimeService.createProcessInstanceQuery();

    // when
    Batch batch = runtimeService.setVariablesAsync(runtimeQuery, SINGLE_VARIABLE);

    engineRule.getIdentityService()
        .setAuthenticatedUserId("demo");

    batchRule.syncExec(batch);

    // then
    List<UserOperationLogEntry> logs = historyService.createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE)
        .list();

    assertThat(logs).isEmpty();
  }

  @Test
  void shouldCreateProcessInstanceRelatedBatchJobsForSingleInvocations() {
    // given
    engineRule.getProcessEngineConfiguration().setInvocationsPerBatchJob(1);

    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    List<String> processInstanceIds = List.of(processInstanceIdOne, processInstanceIdTwo);

    // when
    Batch batch = runtimeService.setVariablesAsync(processInstanceIds, SINGLE_VARIABLE);

    batchRule.executeSeedJobs(batch);

    // then
    List<Job> executionJobs = batchRule.getExecutionJobs(batch);
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

    String processInstanceIdOne = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    List<String> processInstanceIds = List.of(processInstanceIdOne, processInstanceIdTwo);

    // when
    Batch batch = runtimeService.setVariablesAsync(processInstanceIds, SINGLE_VARIABLE);

    batchRule.executeSeedJobs(batch);

    // then
    List<Job> executionJobs = batchRule.getExecutionJobs(batch);
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
    runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    Batch batch = runtimeService.setVariablesAsync(runtimeService.createProcessInstanceQuery(), SINGLE_VARIABLE);
    helper.executeSeedJob(batch);
    List<Job> executionJobs = helper.getExecutionJobs(batch, Batch.TYPE_SET_VARIABLES);

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

  @Test
  void setVariablesAsyncOnCompletedProcessInstance() {
        // given set variables on completed process instance
        engineTestRule.deploy(USER_TASK_PROCESS);
        String id = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
        Batch batch = runtimeService.setVariablesAsync(List.of(id), SINGLE_VARIABLE);
        Task task = engineRule.getTaskService().createTaskQuery().processInstanceId(id).singleResult();
        engineRule.getTaskService().complete(task.getId());

        // when executing batch then no exception is thrown
        assertThatCode(() -> batchRule.syncExec(batch)).doesNotThrowAnyException();
    }

  @Test
  void setVariablesAsyncOnCompletedProcessInstanceWithQuery() {
        // given set variables on completed process instance
        engineTestRule.deploy(USER_TASK_PROCESS);
        String id = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
        Batch batch = runtimeService.setVariablesAsync(runtimeService.createProcessInstanceQuery().processInstanceId(id), SINGLE_VARIABLE);
        Task task = engineRule.getTaskService().createTaskQuery().processInstanceId(id).singleResult();
        engineRule.getTaskService().complete(task.getId());

        // when executing batch then no exception is thrown
        assertThatCode(() -> batchRule.syncExec(batch)).doesNotThrowAnyException();
    }

  @Test
  void setVariablesAsyncOnCompletedProcessInstanceWithHistoricQuery() {
        // given set variables on completed process instance
        engineTestRule.deploy(USER_TASK_PROCESS);
        String id = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
        Batch batch = runtimeService.setVariablesAsync(historyService.createHistoricProcessInstanceQuery().processInstanceId(id), SINGLE_VARIABLE);
        Task task = engineRule.getTaskService().createTaskQuery().processInstanceId(id).singleResult();
        engineRule.getTaskService().complete(task.getId());

        // when executing batch then no exception is thrown
        assertThatCode(() -> batchRule.syncExec(batch)).doesNotThrowAnyException();
    }

  @Test
  void setVariablesSyncOnCompletedProcessInstance() {
        // given completed process
        engineTestRule.deploy(USER_TASK_PROCESS);
        String id = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
        Task task = engineRule.getTaskService().createTaskQuery().processInstanceId(id).singleResult();
        engineRule.getTaskService().complete(task.getId());

        // when setting variables then exception is thrown
        assertThatThrownBy(() -> runtimeService.setVariables(id, SINGLE_VARIABLE))
                .isInstanceOf(NullValueException.class);
    }


  @Test
  void setVariablesAsyncOnBatchWithOneCompletedInstance() {
        // given set variables batch with one completed process instance
        engineTestRule.deploy(USER_TASK_PROCESS);
        String id1 = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
        String id2 = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
        Batch batch = runtimeService.setVariablesAsync(List.of(id1, id2), SINGLE_VARIABLE);
        Task task = engineRule.getTaskService().createTaskQuery().processInstanceId(id1).singleResult();
        engineRule.getTaskService().complete(task.getId());

        // when executing the bacth
        batchRule.syncExec(batch);

        // then no exception is thrown and the variables are set for the existing process
        assertThat(runtimeService.getVariables(id2)).isEqualTo(SINGLE_VARIABLE);
    }
}
