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
package org.operaton.bpm.engine.spring.test.taskListener;

import static org.assertj.core.api.Assertions.assertThat;


import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.spring.test.SpringProcessEngineTestCase;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.springframework.test.context.ContextConfiguration;


/**
 * @author Joram Barrez
 */
@ContextConfiguration("classpath:org/operaton/bpm/engine/spring/test/taskListener/TaskListenerDelegateExpressionTest-context.xml")
class TaskListenerSpringTest extends SpringProcessEngineTestCase {

  @Deployment
  @Test
  void taskListenerDelegateExpression() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskListenerDelegateExpression");
    
    // Completing first task will set variable on process instance
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    assertThat(runtimeService.getVariable(processInstance.getId(), "calledInExpression")).isEqualTo("task1-complete");
    
    // Completing second task will set variable on process instance
    task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    assertThat(runtimeService.getVariable(processInstance.getId(), "calledThroughNotify")).isEqualTo("task2-notify");
  }

}
