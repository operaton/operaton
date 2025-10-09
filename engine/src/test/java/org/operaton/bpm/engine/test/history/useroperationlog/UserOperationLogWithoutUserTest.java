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
package org.operaton.bpm.engine.test.history.useroperationlog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class UserOperationLogWithoutUserTest {

  protected static final String PROCESS_PATH = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";
  protected static final String PROCESS_KEY = "oneTaskProcess";

  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;
  RepositoryService repositoryService;
  HistoryService historyService;
  CaseService caseService;

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testCompleteTask() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testAssignTask() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setAssignee(taskId, "demo");

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testClaimTask() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.claim(taskId, "demo");

    // then
    verifyNoUserOperationLogged();
  }

  @Test
  void testCreateTask() {
    // when
    Task task = taskService.newTask("a-task-id");
    taskService.saveTask(task);

    // then
    verifyNoUserOperationLogged();

    taskService.deleteTask("a-task-id", true);
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testDelegateTask() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.delegateTask(taskId, "demo");

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testResolveTask() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.resolveTask(taskId);

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testSetOwnerTask() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setOwner(taskId, "demo");

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testSetPriorityTask() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setPriority(taskId, 60);

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testUpdateTask() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().singleResult();
    task.setCaseInstanceId("a-case-instance-id");

    // when
    taskService.saveTask(task);

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testActivateProcessInstance() {
    // given
    String id = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    // when
    runtimeService.activateProcessInstanceById(id);

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testSuspendProcessInstance() {
    // given
    String id = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    // when
    runtimeService.suspendProcessInstanceById(id);

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml")
  @Test
  void testActivateJobDefinition() {
    // given
    runtimeService.startProcessInstanceByKey("oneFailingServiceTaskProcess");
    String id = managementService.createJobDefinitionQuery().singleResult().getId();

    // when
    managementService.activateJobByJobDefinitionId(id);

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml")
  @Test
  void testSuspendJobDefinition() {
    // given
    runtimeService.startProcessInstanceByKey("oneFailingServiceTaskProcess");
    String id = managementService.createJobDefinitionQuery().singleResult().getId();

    // when
    managementService.suspendJobByJobDefinitionId(id);

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml")
  @Test
  void testActivateJob() {
    // given
    runtimeService.startProcessInstanceByKey("oneFailingServiceTaskProcess");
    String id = managementService.createJobQuery().singleResult().getId();

    // when
    managementService.activateJobById(id);

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml")
  @Test
  void testSuspendJob() {
    // given
    runtimeService.startProcessInstanceByKey("oneFailingServiceTaskProcess");
    String id = managementService.createJobQuery().singleResult().getId();

    // when
    managementService.suspendJobById(id);

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml")
  @Test
  void testSetJobRetries() {
    // given
    runtimeService.startProcessInstanceByKey("oneFailingServiceTaskProcess");
    String id = managementService.createJobQuery().singleResult().getId();

    // when
    managementService.setJobRetries(id, 5);

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testActivateProcessDefinition() {
    // when
    repositoryService.activateProcessDefinitionByKey(PROCESS_KEY);

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testSuspendProcessDefinition() {
    // when
    repositoryService.suspendProcessDefinitionByKey(PROCESS_KEY);

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testModifyProcessInstance() {
    // given
    String id = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    // when
    runtimeService
      .createProcessInstanceModification(id)
      .cancelAllForActivity("theTask")
      .execute();

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testSetVariable() {
    // given
    String id = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    // when
    runtimeService.setVariable(id, "aVariable", "aValue");

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testRemoveVariable() {
    // given
    String id = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    runtimeService.setVariable(id, "aVariable", "aValue");

    // when
    runtimeService.removeVariable(id, "aVariable");

    // then
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testDeleteHistoricVariable() {
    // given
    String id = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    runtimeService.setVariable(id, "aVariable", "aValue");
    runtimeService.deleteProcessInstance(id, "none");
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isOne();
    String historicVariableId = historyService.createHistoricVariableInstanceQuery().singleResult().getId();

    // when
    historyService.deleteHistoricVariableInstance(historicVariableId);

    // then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isZero();
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = PROCESS_PATH)
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testDeleteAllHistoricVariables() {
    // given
    String id = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();
    runtimeService.setVariable(id, "aVariable", "aValue");
    runtimeService.deleteProcessInstance(id, "none");
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isOne();

    // when
    historyService.deleteHistoricVariableInstancesByProcessInstanceId(id);

    // then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isZero();
    verifyNoUserOperationLogged();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
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
    verifyNoUserOperationLogged();
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
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
    verifyNoUserOperationLogged();

    taskService.deleteTask(task.getId(), true);
  }

  protected void verifyNoUserOperationLogged() {
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();
    assertThat(query.count()).isZero();
  }

}
