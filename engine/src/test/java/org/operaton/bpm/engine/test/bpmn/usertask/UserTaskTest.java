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
package org.operaton.bpm.engine.test.bpmn.usertask;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joram Barrez
 */
class UserTaskTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  IdentityService identityService;
  TaskService taskService;

  @BeforeEach
  void setUp() {
    identityService.saveUser(identityService.newUser("fozzie"));
    identityService.saveUser(identityService.newUser("kermit"));

    identityService.saveGroup(identityService.newGroup("accountancy"));
    identityService.saveGroup(identityService.newGroup("management"));

    identityService.createMembership("fozzie", "accountancy");
    identityService.createMembership("kermit", "management");
  }

  @AfterEach
  void tearDown() {
    identityService.deleteUser("fozzie");
    identityService.deleteUser("kermit");
    identityService.deleteGroup("accountancy");
    identityService.deleteGroup("management");
  }

  @Deployment
  @Test
  @SuppressWarnings("deprecation")
  void testTaskPropertiesNotNull() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    runtimeService.getActiveActivityIds(processInstance.getId());

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getId()).isNotNull();
    assertThat(task.getName()).isEqualTo("my task");
    assertThat(task.getDescription()).isEqualTo("Very important");
    assertThat(task.getPriority()).isPositive();
    assertThat(task.getAssignee()).isEqualTo("kermit");
    assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(task.getExecutionId()).isEqualTo(processInstance.getId());
    assertThat(task.getProcessDefinitionId()).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isNotNull();
    assertThat(task.getCreateTime()).isNotNull();

    // the next test verifies that if an execution creates a task, that no events are created during creation of the task.
    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {
      assertThat(taskService.getTaskEvents(task.getId())).isEmpty();
    }
  }

  @Deployment
  @Test
  void testQuerySortingWithParameter() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
  }

  @Deployment
  @Test
  void testCompleteAfterParallelGateway() {
	  // related to http://jira.codehaus.org/browse/ACT-1054

	  // start the process
    runtimeService.startProcessInstanceByKey("ForkProcess");
    List<Task> taskList = taskService.createTaskQuery().list();
    assertThat(taskList)
            .isNotNull()
            .hasSize(2);

    // make sure user task exists
    Task task = taskService.createTaskQuery().taskDefinitionKey("SimpleUser").singleResult();
    assertThat(task).isNotNull();

  	// attempt to complete the task and get PersistenceException pointing to "referential integrity constraint violation"
  	taskService.complete(task.getId());
	}

  @Deployment
  @Test
  void testComplexScenarioWithSubprocessesAndParallelGateways() {
    runtimeService.startProcessInstanceByKey("processWithSubProcessesAndParallelGateways");

    List<Task> taskList = taskService.createTaskQuery().list();
    assertThat(taskList)
            .isNotNull()
            .hasSize(13);

  }

  @Deployment
  @Test
  void testSimpleProcess() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("financialReport");

    List<Task> tasks = taskService.createTaskQuery().taskCandidateUser("fozzie").list();
    assertThat(tasks).hasSize(1);
    Task task = tasks.get(0);
    assertThat(task.getName()).isEqualTo("Write monthly financial report");

    taskService.claim(task.getId(), "fozzie");
    tasks = taskService
      .createTaskQuery()
      .taskAssignee("fozzie")
      .list();

    assertThat(tasks).hasSize(1);
    taskService.complete(task.getId());

    tasks = taskService.createTaskQuery().taskCandidateUser("fozzie").list();
    assertThat(tasks).isEmpty();
    tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("Verify monthly financial report");
    taskService.complete(tasks.get(0).getId());

    testRule.assertProcessEnded(processInstance.getId());
  }
}
