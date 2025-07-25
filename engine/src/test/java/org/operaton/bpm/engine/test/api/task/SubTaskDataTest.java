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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@ExtendWith(ProcessEngineExtension.class)
class SubTaskDataTest {

  RepositoryService repositoryService;
  RuntimeService runtimeService;
  TaskService taskService;

  @Test
  @Deployment
  void testSubTaskData() {
    //given simple process with user task
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subTaskTest");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // when set variable to user task
    taskService.setVariable(task.getId(), "testVariable", "testValue");

    // then variable is set in the scope of execution
    assertThat(runtimeService.getVariable(task.getExecutionId(), "testVariable")).isEqualTo("testValue");

    // when sub task is created create subtask for user task
    Task subTask = taskService.newTask("123456789");
    subTask.setParentTaskId(task.getId());
    subTask.setName("Test Subtask");
    taskService.saveTask(subTask);

    // and variable is update
    taskService.setVariable(subTask.getId(), "testVariable", "newTestValue");

    //then variable is also updated in the scope execution
    assertThat(runtimeService.getVariable(task.getExecutionId(), "testVariable")).isEqualTo("newTestValue");
  }
}
