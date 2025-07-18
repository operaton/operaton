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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Thilo-Alexander Ginkel
 */
@ExtendWith(ProcessEngineExtension.class)
class TaskPriorityExtensionsTest {

  RuntimeService runtimeService;
  TaskService taskService;

  @Deployment
  @Test
  void testPriorityExtension() {
    testPriorityExtension(25);
    testPriorityExtension(75);
  }

  private void testPriorityExtension(int priority) {
    final Map<String, Object> variables = new HashMap<>();
    variables.put("taskPriority", priority);

    // Start process-instance, passing priority that should be used as task priority
    final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskPriorityExtension", variables);

    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    assertThat(task.getPriority()).isEqualTo(priority);
  }

  @Deployment
  @Test
  void testPriorityExtensionString() {
    final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskPriorityExtensionString");
    final Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getPriority()).isEqualTo(42);
  }
}
