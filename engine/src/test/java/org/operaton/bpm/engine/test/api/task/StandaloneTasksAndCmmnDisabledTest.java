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
package org.operaton.bpm.engine.test.api.task;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.authorization.TaskPermissions;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

class StandaloneTasksAndCmmnDisabledTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .randomEngineName().closeEngineAfterAllTests()
      .configurator(config -> config.setCmmnEnabled(false)).build();
  @RegisterExtension
  ProcessEngineTestExtension engineTestRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  IdentityService identityService;
  AuthorizationService authorizationService;


  @AfterEach
  void tearDown() {
    identityService.clearAuthentication();
    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(false);
    engineTestRule.deleteAllAuthorizations();
    engineTestRule.deleteAllStandaloneTasks();
  }

  /**
   * In this scenario (cmmn and standalone tasks) we want to perform an INNER JOIN
   * on the process definition table. While the test cannot assert this, it makes sure
   * that the query generally works in this case.
   */
  @Test
  void testTaskQueryAuthorization() {
    // given
    engineTestRule.deploy("org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml");
    engineTestRule.deploy("org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml");

    // a process instance task with read authorization on the process
    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task instance1Task = taskService.createTaskQuery().processInstanceId(instance1.getId()).singleResult();

    Authorization processInstanceAuthorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    processInstanceAuthorization.setResource(Resources.PROCESS_DEFINITION);
    processInstanceAuthorization.setResourceId("oneTaskProcess");
    processInstanceAuthorization.addPermission(ProcessDefinitionPermissions.READ_TASK);
    processInstanceAuthorization.setUserId("user");
    authorizationService.saveAuthorization(processInstanceAuthorization);

    // a process instance task with read authorization on the task
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("twoTasksProcess");
    Task instance2Task = taskService.createTaskQuery().processInstanceId(instance2.getId()).singleResult();

    Authorization standaloneTaskAuthorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    standaloneTaskAuthorization.setResource(Resources.TASK);
    standaloneTaskAuthorization.setResourceId(instance2Task.getId());
    standaloneTaskAuthorization.addPermission(TaskPermissions.READ);
    standaloneTaskAuthorization.setUserId("user");
    authorizationService.saveAuthorization(standaloneTaskAuthorization);

    // a third task for which we have no authorization
    runtimeService.startProcessInstanceByKey("twoTasksProcess");

    identityService.setAuthenticatedUserId("user");
    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(true);

    // when
    List<Task> tasks = taskService.createTaskQuery().list();

    // then
    assertThat(tasks).extracting("id").containsExactlyInAnyOrder(instance1Task.getId(), instance2Task.getId());
  }

  /**
   * CAM-12410 implements a single join for the process definition query filters
   * and the authorization check. This test assures that the query works when
   * both are used.
   */
  @Test
  void testTaskQueryAuthorizationWithProcessDefinitionFilter() {
    // given
    engineTestRule.deploy("org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml");
    engineTestRule.deploy("org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml");

    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task instance1Task = taskService.createTaskQuery().processInstanceId(instance1.getId()).singleResult();

    Authorization oneTaskAuthorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    oneTaskAuthorization.setResource(Resources.PROCESS_DEFINITION);
    oneTaskAuthorization.setResourceId("oneTaskProcess");
    oneTaskAuthorization.addPermission(ProcessDefinitionPermissions.READ_TASK);
    oneTaskAuthorization.setUserId("user");
    authorizationService.saveAuthorization(oneTaskAuthorization);

    runtimeService.startProcessInstanceByKey("twoTasksProcess");

    identityService.setAuthenticatedUserId("user");
    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(true);

    // when
    List<Task> tasks = taskService.createTaskQuery().processDefinitionKey("oneTaskProcess").list();

    // then
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getId()).isEqualTo(instance1Task.getId());
  }

  @Test
  void testTaskQueryAuthorizationWithProcessDefinitionFilterInOrQuery() {
    // given
    engineTestRule.deploy("org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml");
    engineTestRule.deploy("org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml");

    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task instance1Task = taskService.createTaskQuery().processInstanceId(instance1.getId()).singleResult();

    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("twoTasksProcess");
    Task instance2Task = taskService.createTaskQuery().processInstanceId(instance2.getId()).singleResult();
    runtimeService.setVariable(instance2.getId(), "markerVar", "markerValue");

    ProcessInstance instance3 = runtimeService.startProcessInstanceByKey("twoTasksProcess");
    taskService.createTaskQuery().processInstanceId(instance3.getId()).singleResult();

    Authorization oneTaskAuthorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    oneTaskAuthorization.setResource(Resources.PROCESS_DEFINITION);
    oneTaskAuthorization.setResourceId("oneTaskProcess");
    oneTaskAuthorization.addPermission(ProcessDefinitionPermissions.READ_TASK);
    oneTaskAuthorization.setUserId("user");
    authorizationService.saveAuthorization(oneTaskAuthorization);

    Authorization twoTasksAuthorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    twoTasksAuthorization.setResource(Resources.PROCESS_DEFINITION);
    twoTasksAuthorization.setResourceId("twoTasksProcess");
    twoTasksAuthorization.addPermission(ProcessDefinitionPermissions.READ_TASK);
    twoTasksAuthorization.setUserId("user");
    authorizationService.saveAuthorization(twoTasksAuthorization);

    identityService.setAuthenticatedUserId("user");
    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(true);

    // when
    List<Task> tasks = taskService.createTaskQuery()
        .or()
          .processDefinitionKey("oneTaskProcess")
          .processVariableValueEquals("markerVar", "markerValue")
        .endOr()
        .list();

    // then
    assertThat(tasks).extracting("id").containsExactlyInAnyOrder(instance1Task.getId(), instance2Task.getId());
  }

}
