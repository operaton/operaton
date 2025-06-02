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
package org.operaton.bpm.engine.test.api.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

class StandaloneTasksDisabledTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .randomEngineName().closeEngineAfterAllTests()
      .configurator(config -> config.setStandaloneTasksEnabled(false)).build();
  @RegisterExtension
  ProcessEngineTestExtension engineTestRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  IdentityService identityService;
  CaseService caseService;

  @AfterEach
  void tearDown() {
    identityService.clearAuthentication();
    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(false);
    engineTestRule.deleteAllAuthorizations();
    engineTestRule.deleteAllStandaloneTasks();
  }

  @Test
  void shouldNotCreateStandaloneTask() {
    // given
    Task task = taskService.newTask();

    // when/then
    assertThatThrownBy(() -> taskService.saveTask(task))
      .isInstanceOf(NotAllowedException.class)
      .hasMessageContaining("Cannot save standalone task. They are disabled in the process engine configuration.");
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void shouldAllowToUpdateProcessInstanceTask() {

    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().singleResult();

    task.setAssignee("newAssignee");

    // when
    taskService.saveTask(task);

    // then
    Task updatedTask = taskService.createTaskQuery().singleResult();
    assertThat(updatedTask.getAssignee()).isEqualTo("newAssignee");
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  void shouldAllowToUpdateCaseInstanceTask() {

    // given
    caseService.createCaseInstanceByKey("oneTaskCase").getId();
    Task task = taskService.createTaskQuery().singleResult();

    task.setAssignee("newAssignee");

    // when
    taskService.saveTask(task);

    // then
    Task updatedTask = taskService.createTaskQuery().singleResult();
    assertThat(updatedTask.getAssignee()).isEqualTo("newAssignee");
  }
}
