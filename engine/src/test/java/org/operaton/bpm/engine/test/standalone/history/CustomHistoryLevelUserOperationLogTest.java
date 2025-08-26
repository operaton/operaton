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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.impl.RuntimeServiceImpl;
import org.operaton.bpm.engine.impl.TaskServiceImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.repository.ProcessDefinition;
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
import static org.operaton.bpm.engine.history.UserOperationLogEntry.*;
import static org.operaton.bpm.engine.impl.cmd.AbstractSetBatchStateCmd.SUSPENSION_STATE_PROPERTY;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;

public class CustomHistoryLevelUserOperationLogTest {

  public static final String USER_ID = "demo";
  protected static final String ONE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";
  protected static final String ONE_TASK_CASE = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";

  static HistoryLevel customHistoryLevelUOL = new CustomHistoryLevelUserOperationLog();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(configuration -> {
      configuration.setJdbcUrl("jdbc:h2:mem:CustomHistoryLevelUserOperationLogTest");
      configuration.setCustomHistoryLevels(Arrays.asList(customHistoryLevelUOL));
      configuration.setHistory("aCustomHistoryLevelUOL");
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
    List<UserOperationLogEntry> logs = query().list();
    for (UserOperationLogEntry log : logs) {
      historyService.deleteUserOperationLogEntry(log.getId());
    }
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void testQueryProcessInstanceOperationsById() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.suspendProcessInstanceById(process.getId());
    runtimeService.activateProcessInstanceById(process.getId());

    runtimeService.deleteProcessInstance(process.getId(), "a delete reason");

    // then
    assertThat(query().entityType(PROCESS_INSTANCE).count()).isEqualTo(4);

    UserOperationLogEntry deleteEntry = query()
        .entityType(PROCESS_INSTANCE)
        .processInstanceId(process.getId())
        .operationType(OPERATION_TYPE_DELETE)
        .singleResult();

    assertThat(deleteEntry).isNotNull();
    assertThat(deleteEntry.getProcessInstanceId()).isEqualTo(process.getId());
    assertThat(deleteEntry.getProcessDefinitionId()).isNotNull();
    assertThat(deleteEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(deleteEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry suspendEntry = query()
      .entityType(PROCESS_INSTANCE)
      .processInstanceId(process.getId())
      .operationType(OPERATION_TYPE_SUSPEND)
      .singleResult();

    assertThat(suspendEntry).isNotNull();
    assertThat(suspendEntry.getProcessInstanceId()).isEqualTo(process.getId());
    assertThat(suspendEntry.getProcessDefinitionId()).isNotNull();
    assertThat(suspendEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");

    assertThat(suspendEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(suspendEntry.getNewValue()).isEqualTo("suspended");
    assertThat(suspendEntry.getOrgValue()).isNull();
    assertThat(suspendEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry activateEntry = query()
        .entityType(PROCESS_INSTANCE)
        .processInstanceId(process.getId())
        .operationType(OPERATION_TYPE_ACTIVATE)
        .singleResult();

    assertThat(activateEntry).isNotNull();
    assertThat(activateEntry.getProcessInstanceId()).isEqualTo(process.getId());
    assertThat(activateEntry.getProcessDefinitionId()).isNotNull();
    assertThat(activateEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");

    assertThat(activateEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(activateEntry.getNewValue()).isEqualTo("active");
    assertThat(activateEntry.getOrgValue()).isNull();
    assertThat(activateEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryProcessDefinitionOperationsByKey() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    repositoryService.suspendProcessDefinitionByKey("oneTaskProcess", true, null);
    repositoryService.activateProcessDefinitionByKey("oneTaskProcess", true, null);
    repositoryService.deleteProcessDefinitions().byKey("oneTaskProcess").cascade().delete();

    // then
    assertThat(query().entityType(PROCESS_DEFINITION).count()).isEqualTo(5);

    UserOperationLogEntry suspendDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionKey("oneTaskProcess")
      .operationType(OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION)
      .property(SUSPENSION_STATE_PROPERTY)
      .singleResult();

    assertThat(suspendDefinitionEntry).isNotNull();
    assertThat(suspendDefinitionEntry.getProcessDefinitionId()).isNull();
    assertThat(suspendDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(suspendDefinitionEntry.getDeploymentId()).isNull();

    assertThat(suspendDefinitionEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(suspendDefinitionEntry.getNewValue()).isEqualTo("suspended");
    assertThat(suspendDefinitionEntry.getOrgValue()).isNull();
    assertThat(suspendDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry activateDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionKey("oneTaskProcess")
      .operationType(OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION)
      .property(SUSPENSION_STATE_PROPERTY)
      .singleResult();

    assertThat(activateDefinitionEntry).isNotNull();
    assertThat(activateDefinitionEntry.getProcessDefinitionId()).isNull();
    assertThat(activateDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(activateDefinitionEntry.getDeploymentId()).isNull();

    assertThat(activateDefinitionEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(activateDefinitionEntry.getNewValue()).isEqualTo("active");
    assertThat(activateDefinitionEntry.getOrgValue()).isNull();
    assertThat(activateDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry deleteDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionKey("oneTaskProcess")
      .operationType(OPERATION_TYPE_DELETE)
      .singleResult();

    assertThat(deleteDefinitionEntry).isNotNull();
    assertThat(deleteDefinitionEntry.getProcessDefinitionId()).isNotNull();
    assertThat(deleteDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(deleteDefinitionEntry.getDeploymentId()).isNotNull();

    assertThat(deleteDefinitionEntry.getProperty()).isEqualTo("cascade");
    assertThat(deleteDefinitionEntry.getNewValue()).isEqualTo("true");
    assertThat(deleteDefinitionEntry.getOrgValue()).isNotNull();
    assertThat(deleteDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
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
    assertThat(query().entityType(JOB_DEFINITION).count()).isEqualTo(2);
    assertThat(query().entityType(JOB).count()).isEqualTo(2);

    // active job definition
    UserOperationLogEntry activeJobDefinitionEntry = query()
      .entityType(JOB_DEFINITION)
      .processDefinitionId(process.getProcessDefinitionId())
      .operationType(OPERATION_TYPE_ACTIVATE_JOB_DEFINITION)
      .singleResult();

    assertThat(activeJobDefinitionEntry).isNotNull();
    assertThat(activeJobDefinitionEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());

    assertThat(activeJobDefinitionEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(activeJobDefinitionEntry.getNewValue()).isEqualTo("active");
    assertThat(activeJobDefinitionEntry.getOrgValue()).isNull();
    assertThat(activeJobDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    // active job
    UserOperationLogEntry activateJobIdEntry = query()
      .entityType(JOB)
      .processInstanceId(process.getProcessInstanceId())
      .operationType(OPERATION_TYPE_ACTIVATE_JOB)
      .singleResult();

    assertThat(activateJobIdEntry).isNotNull();
    assertThat(activateJobIdEntry.getProcessInstanceId()).isEqualTo(process.getProcessInstanceId());

    assertThat(activateJobIdEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(activateJobIdEntry.getNewValue()).isEqualTo("active");
    assertThat(activateJobIdEntry.getOrgValue()).isNull();
    assertThat(activateJobIdEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    // suspended job definition
    UserOperationLogEntry suspendJobDefinitionEntry = query()
      .entityType(JOB_DEFINITION)
      .processDefinitionId(process.getProcessDefinitionId())
      .operationType(OPERATION_TYPE_SUSPEND_JOB_DEFINITION)
      .singleResult();

    assertThat(suspendJobDefinitionEntry).isNotNull();
    assertThat(suspendJobDefinitionEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());

    assertThat(suspendJobDefinitionEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(suspendJobDefinitionEntry.getNewValue()).isEqualTo("suspended");
    assertThat(suspendJobDefinitionEntry.getOrgValue()).isNull();
    assertThat(suspendJobDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    // suspended job
    UserOperationLogEntry suspendedJobEntry = query()
      .entityType(JOB)
      .processInstanceId(process.getProcessInstanceId())
      .operationType(OPERATION_TYPE_SUSPEND_JOB)
      .singleResult();

    assertThat(suspendedJobEntry).isNotNull();
    assertThat(suspendedJobEntry.getProcessInstanceId()).isEqualTo(process.getProcessInstanceId());

    assertThat(suspendedJobEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(suspendedJobEntry.getNewValue()).isEqualTo("suspended");
    assertThat(suspendedJobEntry.getOrgValue()).isNull();
    assertThat(suspendedJobEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedServiceTask.bpmn20.xml" })
  void testQueryJobRetryOperationsById() {
    // given
    process = runtimeService.startProcessInstanceByKey("failedServiceTask");
    Job job = managementService.createJobQuery().processInstanceId(process.getProcessInstanceId()).singleResult();

    managementService.setJobRetries(job.getId(), 10);

    // then
    assertThat(query().entityType(JOB).operationType(OPERATION_TYPE_SET_JOB_RETRIES).count()).isEqualTo(1);

    UserOperationLogEntry jobRetryEntry = query()
      .entityType(JOB)
      .jobId(job.getId())
      .operationType(OPERATION_TYPE_SET_JOB_RETRIES)
      .singleResult();

    assertThat(jobRetryEntry).isNotNull();
    assertThat(jobRetryEntry.getJobId()).isEqualTo(job.getId());

    assertThat(jobRetryEntry.getOrgValue()).isEqualTo("3");
    assertThat(jobRetryEntry.getNewValue()).isEqualTo("10");
    assertThat(jobRetryEntry.getProperty()).isEqualTo("retries");
    assertThat(jobRetryEntry.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(jobRetryEntry.getProcessInstanceId()).isEqualTo(job.getProcessInstanceId());
    assertThat(jobRetryEntry.getProcessDefinitionKey()).isEqualTo(job.getProcessDefinitionKey());
    assertThat(jobRetryEntry.getProcessDefinitionId()).isEqualTo(job.getProcessDefinitionId());
    assertThat(jobRetryEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  // ----- PROCESS INSTANCE MODIFICATION -----

  @Test
  @Deployment(resources = { ONE_TASK_PROCESS })
  void testQueryProcessInstanceModificationOperation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().singleResult();

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("theTask")
      .execute();

    UserOperationLogQuery logQuery = query()
      .entityType(EntityTypes.PROCESS_INSTANCE)
      .operationType(UserOperationLogEntry.OPERATION_TYPE_MODIFY_PROCESS_INSTANCE);

    assertThat(logQuery.count()).isEqualTo(1);
    UserOperationLogEntry logEntry = logQuery.singleResult();

    assertThat(logEntry.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(logEntry.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(logEntry.getProcessDefinitionKey()).isEqualTo(definition.getKey());
    assertThat(logEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_MODIFY_PROCESS_INSTANCE);
    assertThat(logEntry.getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);
    assertThat(logEntry.getProperty()).isNull();
    assertThat(logEntry.getOrgValue()).isNull();
    assertThat(logEntry.getNewValue()).isNull();
    assertThat(logEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  // ----- ADD VARIABLES -----

  @Test
  @Deployment(resources = { ONE_TASK_PROCESS })
  void testQueryAddExecutionVariableOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.setVariable(process.getId(), "testVariable1", "THIS IS TESTVARIABLE!!!");

    // then
    verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE, UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Test
  @Deployment(resources = { ONE_TASK_PROCESS })
  void testQueryAddTaskVariablesSingleAndMapOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setVariable(processTaskId, "testVariable3", "foo");
    taskService.setVariables(processTaskId, createMapForVariableAddition());
    taskService.setVariable(processTaskId, "testVariable4", "bar");

    // then
    verifyVariableOperationAsserts(3, UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE, UserOperationLogEntry.CATEGORY_TASK_WORKER);
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
   verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_MODIFY_VARIABLE, UserOperationLogEntry.CATEGORY_OPERATOR);
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
    verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_MODIFY_VARIABLE, UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  // ----- REMOVE VARIABLES -----


  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryRemoveExecutionVariablesMapOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.removeVariables(process.getId(), createCollectionForVariableDeletion());

    // then
    verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_REMOVE_VARIABLE, UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryRemoveTaskVariableOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.removeVariable(processTaskId, "testVariable1");

    // then
    verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_REMOVE_VARIABLE, UserOperationLogEntry.CATEGORY_TASK_WORKER);
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

    verifyQueryResults(query, 2);
  }

  @Test
  @Deployment(resources = {ONE_TASK_PROCESS})
  void testQueryByCategories() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setAssignee(processTaskId, "foo");
    taskService.setVariable(processTaskId, "foo", "bar");

    // then
    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .categoryIn(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    verifyQueryResults(query, 2);
  }
  // --------------- CMMN --------------------

  @Test
  @Deployment(resources={ONE_TASK_CASE})
  void testQueryByCaseExecutionId() {
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

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    Task task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();

    // when
    taskService.setAssignee(task.getId(), "demo");

    // then

    UserOperationLogQuery query = historyService
      .createUserOperationLogQuery()
      .caseExecutionId(caseExecutionId);

    verifyQueryResults(query, 1);
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
    verifyQueryResults(query, 1);

    repositoryService.deleteDeployment(deploymentId, true);
  }

  @Test
  @Deployment(resources = { ONE_TASK_PROCESS })
  void testUserOperationLogDeletion() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariable(process.getId(), "testVariable1", "THIS IS TESTVARIABLE!!!");

    // assume
    verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE, UserOperationLogEntry.CATEGORY_OPERATOR);
    UserOperationLogQuery query = query().entityType(EntityTypes.VARIABLE).operationType(UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE);
    assertThat(query.count()).isEqualTo(1);

    // when
    historyService.deleteUserOperationLogEntry(query.singleResult().getId());

    // then
    assertThat(query.count()).isZero();
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

  protected void verifyVariableOperationAsserts(int countAssertValue, String operationType, String category) {
    UserOperationLogQuery logQuery = query().entityType(EntityTypes.VARIABLE).operationType(operationType);
    assertThat(logQuery.count()).isEqualTo(countAssertValue);

    if(countAssertValue > 1) {
      List<UserOperationLogEntry> logEntryList = logQuery.list();

      for (UserOperationLogEntry logEntry : logEntryList) {
        assertThat(logEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
        assertThat(logEntry.getProcessInstanceId()).isEqualTo(process.getProcessInstanceId());
        assertThat(logEntry.getCategory()).isEqualTo(category);
      }
    } else {
      UserOperationLogEntry logEntry = logQuery.singleResult();
      assertThat(logEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
      assertThat(logEntry.getProcessInstanceId()).isEqualTo(process.getProcessInstanceId());
      assertThat(logEntry.getCategory()).isEqualTo(category);
    }
  }

  protected UserOperationLogQuery query() {
    return historyService.createUserOperationLogQuery();
  }

}
