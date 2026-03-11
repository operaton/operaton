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
package org.operaton.bpm.engine.spring.test.servicetask;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import org.operaton.bpm.engine.spring.test.SpringProcessEngineTestCase;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;

import static org.operaton.bpm.engine.impl.test.ProcessEngineAssert.assertProcessEnded;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see <a href="http://jira.codehaus.org/browse/ACT-1166">ACT-1166</a>
 * @author Angel López Cima
 * @author Falko Menge
 */
@ContextConfiguration("classpath:org/operaton/bpm/engine/spring/test/servicetask/servicetaskSpringTestCatchError-context.xml")
class BoundaryErrorEventSpringTest extends SpringProcessEngineTestCase {

  @Deployment
  @Test
  void catchErrorThrownByJavaDelegateOnServiceTask() {
    String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByExpressionDelegateOnServiceTask").getId();
    assertThatErrorHasBeenCaught(procId);
  }

  private void assertThatErrorHasBeenCaught(String procId) {
    // The service task will throw an error event,
    // which is caught on the service task boundary
    assertThat(taskService.createTaskQuery().count()).as("No tasks found in task list.").isOne();
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Escalated Task");

    // Completing the task will end the process instance
    taskService.complete(task.getId());
    assertProcessEnded(processEngine, procId);
  }
}
