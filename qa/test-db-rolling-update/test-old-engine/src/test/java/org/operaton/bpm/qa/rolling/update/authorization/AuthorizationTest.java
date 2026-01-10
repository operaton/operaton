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
package org.operaton.bpm.qa.rolling.update.authorization;

import java.util.Arrays;
import java.util.List;

import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.qa.rolling.update.RollingUpdateTest;
import org.operaton.bpm.qa.rolling.update.AbstractRollingUpdateTestCase;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@ScenarioUnderTest("AuthorizationScenario")
class AuthorizationTest extends AbstractRollingUpdateTestCase {

  public static final String PROCESS_DEF_KEY = "oneTaskProcess";
  protected static final String USER_ID = "user";
  protected static final String GROUP_ID = "group";

  protected IdentityService identityService;
  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected HistoryService historyService;
  protected FormService formService;

  private void initializeServices() {
    identityService = rule.getIdentityService();
    repositoryService = rule.getRepositoryService();
    runtimeService = rule.getRuntimeService();
    taskService = rule.getTaskService();
    historyService = rule.getHistoryService();
    formService = rule.getFormService();
  }

  private void authenticateUser() {
    identityService.clearAuthentication();
    identityService.setAuthentication(USER_ID + rule.getBusinessKey(), Arrays.asList(GROUP_ID + rule.getBusinessKey()));
  }

  private void clearAuthentication() {
    identityService.clearAuthentication();
  }

  @RollingUpdateTest
  @ScenarioUnderTest("startProcessInstance.1")
  void testAuthorization() {
    initializeServices();
    authenticateUser();

    try {
      //test access process related
      testGetDeployment();
      testGetProcessDefinition();
      testGetProcessInstance();
      testGetExecution();
      testGetTask();

      //test access historic
      testGetHistoricProcessInstance();
      testGetHistoricActivityInstance();
      testGetHistoricTaskInstance();

      //test process modification
      testSetVariable();
      testSubmitStartForm();
      testStartProcessInstance();
      testCompleteTaskInstance();
      testSubmitTaskForm();
    } finally {
      clearAuthentication();
    }
  }


  public void testGetDeployment() {
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    assertThat(deployments).isNotEmpty();
  }

  public void testGetProcessDefinition() {
    List<ProcessDefinition> definitions = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEF_KEY)
        .list();
    assertThat(definitions).isNotEmpty();
  }

  public void testGetProcessInstance() {
    List<ProcessInstance> instances = runtimeService
        .createProcessInstanceQuery()
        .processInstanceBusinessKey(rule.getBusinessKey())
        .processDefinitionKey(PROCESS_DEF_KEY)
        .list();
    assertThat(instances).isNotEmpty();
  }

  public void testGetExecution() {
    List<Execution> executions = runtimeService
        .createExecutionQuery()
        .processInstanceBusinessKey(rule.getBusinessKey())
        .processDefinitionKey(PROCESS_DEF_KEY)
        .list();
    assertThat(executions).isNotEmpty();
  }

  public void testGetTask() {
    List<Task> tasks = taskService
        .createTaskQuery()
        .processInstanceBusinessKey(rule.getBusinessKey())
        .processDefinitionKey(PROCESS_DEF_KEY)
        .list();
    assertThat(tasks).isNotEmpty();
  }

  public void testGetHistoricProcessInstance() {
    List<HistoricProcessInstance> instances= historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceBusinessKey(rule.getBusinessKey())
        .processDefinitionKey(PROCESS_DEF_KEY)
        .list();
    assertThat(instances).isNotEmpty();
  }

  public void testGetHistoricActivityInstance() {
    List<HistoricActivityInstance> instances= historyService
        .createHistoricActivityInstanceQuery()
        .list();
    assertThat(instances).isNotEmpty();
  }

  public void testGetHistoricTaskInstance() {
    List<HistoricTaskInstance> instances= historyService
        .createHistoricTaskInstanceQuery()
        .processDefinitionKey(PROCESS_DEF_KEY)
        .list();
    assertThat(instances).isNotEmpty();
  }

  public void testStartProcessInstance() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY, rule.getBusinessKey());
    assertThat(instance).isNotNull();
  }

  public void testSubmitStartForm() {
    ProcessInstance instance = formService.submitStartForm(rule.processInstance().getProcessDefinitionId(), rule.getBusinessKey(), null);
    assertThat(instance).isNotNull();
  }

  public void testCompleteTaskInstance() {
    String taskId = taskService
        .createTaskQuery()
        .processDefinitionKey(PROCESS_DEF_KEY)
        .processInstanceBusinessKey(rule.getBusinessKey())
        .listPage(0, 1)
        .get(0)
        .getId();
    taskService.complete(taskId);
  }

  public void testSubmitTaskForm() {
    String taskId = taskService
        .createTaskQuery()
        .processDefinitionKey(PROCESS_DEF_KEY)
        .processInstanceBusinessKey(rule.getBusinessKey())
        .listPage(0, 1)
        .get(0)
        .getId();
    formService.submitTaskForm(taskId, null);
  }

  public void testSetVariable() {
    String processInstanceId = runtimeService
        .createProcessInstanceQuery()
        .processDefinitionKey(PROCESS_DEF_KEY)
        .listPage(0, 1)
        .get(0)
        .getId();
    runtimeService.setVariable(processInstanceId, "abc", "def");
  }
}
