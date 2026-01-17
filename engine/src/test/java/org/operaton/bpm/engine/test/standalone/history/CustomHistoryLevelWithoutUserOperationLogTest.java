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
package org.operaton.bpm.engine.test.standalone.history;
import java.util.List;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.impl.RuntimeServiceImpl;
import org.operaton.bpm.engine.impl.TaskServiceImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

import static org.operaton.bpm.engine.EntityTypes.JOB;
import static org.operaton.bpm.engine.EntityTypes.JOB_DEFINITION;
import static org.operaton.bpm.engine.EntityTypes.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.EntityTypes.PROCESS_INSTANCE;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.OPERATION_TYPE_SET_JOB_RETRIES;
import static org.assertj.core.api.Assertions.assertThat;

public class CustomHistoryLevelWithoutUserOperationLogTest {

  public static final String USER_ID = "demo";
  private static final String ONE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";
  protected static final String ONE_TASK_CASE = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";

  static HistoryLevel customHistoryLevelFullWUOL = new CustomHistoryLevelFullWithoutUserOperationLog();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(configuration -> {
      configuration.setJdbcUrl("jdbc:h2:mem:CustomHistoryLevelWithoutUserOperationLogTest");
      configuration.setCustomHistoryLevels(List.of(customHistoryLevelFullWUOL));
      configuration.setHistory("aCustomHistoryLevelWUOL");
      configuration.setDatabaseSchemaUpdate(DB_SCHEMA_UPDATE_CREATE_DROP);
    })
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);

  HistoryService historyService;
  RuntimeService runtimeService;
  ManagementService managementService;
  IdentityService identityService;
  RepositoryService repositoryService;
  TaskService taskService;
  CaseService caseService;
  ProcessEngineConfigurationImpl processEngineConfiguration;

  ProcessInstance process;
  Task userTask;
  String processTaskId;

  @BeforeEach
  void setUp() {
    identityService.setAuthenticatedUserId(USER_ID);
  }

  @AfterEach
  void tearDown() {
    identityService.clearAuthentication();
  }

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryProcessInstanceOperationsByProcessDefinitionKey() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.suspendProcessInstanceByProcessDefinitionKey("oneTaskProcess");
    runtimeService.activateProcessInstanceByProcessDefinitionKey("oneTaskProcess");

    // then
    assertThat(query().entityType(PROCESS_INSTANCE).count()).isZero();
  }

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryProcessDefinitionOperationsById() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    repositoryService.suspendProcessDefinitionById(process.getProcessDefinitionId(), true, null);
    repositoryService.activateProcessDefinitionById(process.getProcessDefinitionId(), true, null);

    // then
    assertThat(query().entityType(PROCESS_DEFINITION).count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  void testQueryJobOperations() {
    // given
    process = runtimeService.startProcessInstanceByKey("process");

    // when
    managementService.suspendJobDefinitionByProcessDefinitionId(process.getProcessDefinitionId());
    managementService.activateJobDefinitionByProcessDefinitionId(process.getProcessDefinitionId());
    managementService.suspendJobByProcessInstanceId(process.getId());
    managementService.activateJobByProcessInstanceId(process.getId());

    // then
    assertThat(query().entityType(JOB_DEFINITION).count()).isZero();
    assertThat(query().entityType(JOB).count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedServiceTask.bpmn20.xml"})
  void testQueryJobRetryOperationsById() {
    // given
    process = runtimeService.startProcessInstanceByKey("failedServiceTask");
    Job job = managementService.createJobQuery().processInstanceId(process.getProcessInstanceId()).singleResult();

    managementService.setJobRetries(job.getId(), 10);

    // then
    assertThat(query().entityType(JOB).operationType(OPERATION_TYPE_SET_JOB_RETRIES).count()).isZero();
  }

  // ----- PROCESS INSTANCE MODIFICATION -----

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryProcessInstanceModificationOperation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processInstance.getId();

    repositoryService.createProcessDefinitionQuery().singleResult();

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("theTask")
      .execute();

    UserOperationLogQuery logQuery = query()
      .entityType(EntityTypes.PROCESS_INSTANCE)
      .operationType(UserOperationLogEntry.OPERATION_TYPE_MODIFY_PROCESS_INSTANCE);

    assertThat(logQuery.count()).isZero();
  }

  // ----- ADD VARIABLES -----

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryAddExecutionVariablesMapOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.setVariables(process.getId(), createMapForVariableAddition());

    // then
    verifyVariableOperationAsserts(UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE);
  }

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryAddTaskVariableOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setVariable(processTaskId, "testVariable1", "THIS IS TESTVARIABLE!!!");

    // then
    verifyVariableOperationAsserts(UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE);
  }

  // ----- PATCH VARIABLES -----

    @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryPatchExecutionVariablesOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    ((RuntimeServiceImpl) runtimeService)
      .updateVariables(process.getId(), createMapForVariableAddition(), createCollectionForVariableDeletion());

    // then
   verifyVariableOperationAsserts(UserOperationLogEntry.OPERATION_TYPE_MODIFY_VARIABLE);
  }

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryPatchTaskVariablesOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    ((TaskServiceImpl) taskService)
      .updateVariablesLocal(processTaskId, createMapForVariableAddition(), createCollectionForVariableDeletion());

    // then
    verifyVariableOperationAsserts(UserOperationLogEntry.OPERATION_TYPE_MODIFY_VARIABLE);
  }

  // ----- REMOVE VARIABLES -----

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryRemoveExecutionVariableOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.removeVariable(process.getId(), "testVariable1");

    // then
    verifyVariableOperationAsserts(UserOperationLogEntry.OPERATION_TYPE_REMOVE_VARIABLE);
  }

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryByEntityTypes() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setAssignee(processTaskId, "foo");
    taskService.setVariable(processTaskId, "foo", "bar");

    // then
    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .entityTypeIn(EntityTypes.TASK, EntityTypes.VARIABLE);

    assertThat(query.count()).isZero();
  }

  // ----- DELETE VARIABLE HISTORY -----

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryDeleteVariableHistoryOperationOnRunningInstance() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariable(process.getId(), "testVariable", "test");
    runtimeService.setVariable(process.getId(), "testVariable", "test2");
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().singleResult().getId();

    // when
    historyService.deleteHistoricVariableInstance(variableInstanceId);

    // then
    verifyVariableOperationAsserts(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryDeleteVariableHistoryOperationOnHistoryInstance() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariable(process.getId(), "testVariable", "test");
    runtimeService.deleteProcessInstance(process.getId(), "none");
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().singleResult().getId();

    // when
    historyService.deleteHistoricVariableInstance(variableInstanceId);

    // then
    verifyVariableOperationAsserts(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testQueryDeleteVariableHistoryOperationOnCase() {
    // given
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase");
    caseService.setVariable(caseInstance.getId(), "myVariable", 1);
    caseService.setVariable(caseInstance.getId(), "myVariable", 2);
    caseService.setVariable(caseInstance.getId(), "myVariable", 3);
    HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();

    // when
    historyService.deleteHistoricVariableInstance(variableInstance.getId());

    // then
    verifyVariableOperationAsserts(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Test
  void testQueryDeleteVariableHistoryOperationOnStandaloneTask() {
    // given
    Task task = taskService.newTask();
    taskService.saveTask(task);
    taskService.setVariable(task.getId(), "testVariable", "testValue");
    taskService.setVariable(task.getId(), "testVariable", "testValue2");
    HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();

    // when
    historyService.deleteHistoricVariableInstance(variableInstance.getId());

    // then
    verifyVariableOperationAsserts(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);

    taskService.deleteTask(task.getId(), true);
  }

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryDeleteVariablesHistoryOperationOnRunningInstance() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariable(process.getId(), "testVariable", "test");
    runtimeService.setVariable(process.getId(), "testVariable", "test2");
    runtimeService.setVariable(process.getId(), "testVariable2", "test");
    runtimeService.setVariable(process.getId(), "testVariable2", "test2");
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(2);

    // when
    historyService.deleteHistoricVariableInstancesByProcessInstanceId(process.getId());

    // then
    verifyVariableOperationAsserts(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryDeleteVariablesHistoryOperationOnHistoryInstance() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariable(process.getId(), "testVariable", "test");
    runtimeService.setVariable(process.getId(), "testVariable2", "test");
    runtimeService.deleteProcessInstance(process.getId(), "none");
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(2);

    // when
    historyService.deleteHistoricVariableInstancesByProcessInstanceId(process.getId());

    // then
    verifyVariableOperationAsserts(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryDeleteVariableAndVariablesHistoryOperationOnRunningInstance() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariable(process.getId(), "testVariable", "test");
    runtimeService.setVariable(process.getId(), "testVariable", "test2");
    runtimeService.setVariable(process.getId(), "testVariable2", "test");
    runtimeService.setVariable(process.getId(), "testVariable2", "test2");
    runtimeService.setVariable(process.getId(), "testVariable3", "test");
    runtimeService.setVariable(process.getId(), "testVariable3", "test2");
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().variableName("testVariable").singleResult().getId();

    // when
    historyService.deleteHistoricVariableInstance(variableInstanceId);
    historyService.deleteHistoricVariableInstancesByProcessInstanceId(process.getId());

    // then
    verifyVariableOperationAsserts(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryDeleteVariableAndVariablesHistoryOperationOnHistoryInstance() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariable(process.getId(), "testVariable", "test");
    runtimeService.setVariable(process.getId(), "testVariable2", "test");
    runtimeService.setVariable(process.getId(), "testVariable3", "test");
    runtimeService.deleteProcessInstance(process.getId(), "none");
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().variableName("testVariable").singleResult().getId();

    // when
    historyService.deleteHistoricVariableInstance(variableInstanceId);
    historyService.deleteHistoricVariableInstancesByProcessInstanceId(process.getId());

    // then
    verifyVariableOperationAsserts(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  // --------------- CMMN --------------------

  @Test
  @Deployment(resources = {ONE_TASK_CASE})
  void testQueryByCaseDefinitionId() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    Task task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();

    // when
    taskService.setAssignee(task.getId(), "demo");

    // then

    UserOperationLogQuery query = historyService
      .createUserOperationLogQuery()
      .caseDefinitionId(caseDefinitionId);

    assertThat(query.count()).isZero();

    taskService.setAssignee(task.getId(), null);
  }

  @Test
  void testQueryByDeploymentId() {
    // given
    String deploymentId = repositoryService
        .createDeployment()
        .addClasspathResource(ONE_TASK_PROCESS)
        .deploy()
        .getId();

    // when
    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .deploymentId(deploymentId);

    // then
    assertThat(query.count()).isZero();

    repositoryService.deleteDeployment(deploymentId, true);
  }

  protected Map<String, Object> createMapForVariableAddition() {
    Map<String, Object> variables =  new HashMap<>();
    variables.put("testVariable1", "THIS IS TESTVARIABLE!!!");
    variables.put("testVariable2", "OVER 9000!");

    return variables;
  }

  protected Collection<String> createCollectionForVariableDeletion() {
    Collection<String> variables = new ArrayList<>();
    variables.add("testVariable3");
    variables.add("testVariable4");

    return variables;
  }

  protected void verifyVariableOperationAsserts(String operationType) {
    UserOperationLogQuery logQuery = query().entityType(EntityTypes.VARIABLE).operationType(operationType);
    assertThat(logQuery.count()).isZero();
  }

  protected UserOperationLogQuery query() {
    return historyService.createUserOperationLogQuery();
  }

}
