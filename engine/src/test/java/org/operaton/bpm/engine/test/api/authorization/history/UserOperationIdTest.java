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
package org.operaton.bpm.engine.test.api.authorization.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * Tests the operationId field in historic tables, which helps to correlate records from different tables.
 *
 * @author Svetlana Dorokhova
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class UserOperationIdTest {

  @RegisterExtension
  public static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  public ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected static final String PROCESS_KEY = "oneTaskProcess";
  protected String deploymentId;

  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected HistoryService historyService;
  protected TaskService taskService;
  protected FormService formService;
  protected IdentityService identityService;

  @BeforeEach
  public void init() {
    runtimeService = engineRule.getRuntimeService();
    repositoryService = engineRule.getRepositoryService();
    historyService = engineRule.getHistoryService();
    taskService = engineRule.getTaskService();
    formService = engineRule.getFormService();
    identityService = engineRule.getIdentityService();
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testResolveTaskOperationId() {
    // given
    identityService.setAuthenticatedUserId("demo");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.resolveTask(taskId, getVariables());

    //then
    List<UserOperationLogEntry> userOperationLogEntries = historyService.createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_RESOLVE)
        .taskId(taskId)
        .list();
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().list();
    verifySameOperationId(userOperationLogEntries, historicDetails);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testSubmitTaskFormOperationId() {
    // given
    identityService.setAuthenticatedUserId("demo");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    formService.submitTaskForm(taskId, getVariables());

    //then
    List<UserOperationLogEntry> userOperationLogEntries = historyService.createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_COMPLETE)
        .taskId(taskId)
        .list();
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().list();
    verifySameOperationId(userOperationLogEntries, historicDetails);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testSetTaskVariablesOperationId() {
    // given
    identityService.setAuthenticatedUserId("demo");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setVariables(taskId, getVariables());

    //then
    List<UserOperationLogEntry> userOperationLogEntries = historyService.createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE)
        .taskId(taskId)
        .list();
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().list();
    verifySameOperationId(userOperationLogEntries, historicDetails);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testWithoutAuthentication() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.resolveTask(taskId, getVariables());

    //then
    List<UserOperationLogEntry> userOperationLogEntries = historyService.createUserOperationLogQuery()
        .taskId(taskId)
        .list();
    assertThat(userOperationLogEntries).isEmpty();
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().list();
    assertThat(!historicDetails.isEmpty()).isTrue();
    //history detail records must have null userOperationId as user operation log was not created
    for (HistoricDetail historicDetail: historicDetails) {
      assertThat(historicDetail.getUserOperationId()).isNull();
    }
  }

  @Test
  public void testSetTaskVariablesInServiceTask() {
    // given
    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .userTask()
        .serviceTask()
          .operatonExpression("${execution.setVariable('foo', 'bar')}")
        .endEvent()
        .done();
    testRule.deploy(bpmnModelInstance);

    identityService.setAuthenticatedUserId("demo");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.complete(task.getId());

    //then
    HistoricDetail historicDetail = historyService.createHistoricDetailQuery().singleResult();
    // no user operation log id is set for this update, as it is not written as part of the user operation
    assertThat(historicDetail.getUserOperationId()).isNull();
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testStartProcessOperationId() {
    // given
    identityService.setAuthenticatedUserId("demo");

    // when
    ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS_KEY, getVariables());

    //then
    List<UserOperationLogEntry> userOperationLogEntries = historyService.createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_CREATE)
        .processInstanceId(pi.getId())
        .list();
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().list();

    assertThat(userOperationLogEntries).isNotEmpty();
    assertThat(historicDetails).isNotEmpty();
    verifySameOperationId(userOperationLogEntries, historicDetails);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testStartProcessAtActivityOperationId() {
    // given
    identityService.setAuthenticatedUserId("demo");

    // when
    ProcessInstance pi = runtimeService.createProcessInstanceByKey(PROCESS_KEY)
            .startBeforeActivity("theTask")
            .setVariables(getVariables())
            .execute();

    //then
    List<UserOperationLogEntry> userOperationLogEntries = historyService.createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_CREATE)
        .processInstanceId(pi.getId())
        .list();
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().list();

    assertThat(userOperationLogEntries).isNotEmpty();
    assertThat(historicDetails).isNotEmpty();
    verifySameOperationId(userOperationLogEntries, historicDetails);
  }

  private void verifySameOperationId(List<UserOperationLogEntry> userOperationLogEntries, List<HistoricDetail> historicDetails) {
    assertThat(!userOperationLogEntries.isEmpty()).as("Operation log entry must exist").isTrue();
    String operationId = userOperationLogEntries.get(0).getOperationId();
    assertThat(operationId).isNotNull();
    assertThat(!historicDetails.isEmpty()).as("Some historic details are expected to be present").isTrue();
    for (UserOperationLogEntry userOperationLogEntry: userOperationLogEntries) {
      assertThat(userOperationLogEntry.getOperationId()).as("OperationIds must be the same").isEqualTo(operationId);
    }
    for (HistoricDetail historicDetail : historicDetails) {
      assertThat(historicDetail.getUserOperationId()).as("OperationIds must be the same").isEqualTo(operationId);
    }
  }

  protected VariableMap getVariables() {
    return Variables.createVariables()
        .putValue("aVariableName", "aVariableValue")
        .putValue("anotherVariableName", "anotherVariableValue");
  }

}
