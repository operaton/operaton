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
package org.operaton.bpm.engine.test.bpmn.sequenceflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests things that BPMN describes as 'uncontrolled flow':
 * Activities with more than one incoming sequence flow or with more than one
 * outgoing flow.
 *
 * @author Thorben Lindhauer
 */
class UncontrolledFlowTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;

  @Deployment
  @Test
  void testSubProcessTwoOutgoingFlowsCorrelateMessage() {
    // given a process instance
    runtimeService.startProcessInstanceByKey("process");

    // that leaves the sub process via two outgoing sequence flows
    Task innerTask = taskService.createTaskQuery().singleResult();
    taskService.complete(innerTask.getId());

    // then there are two tasks after the sub process
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("outerTask1").count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("outerTask2").count()).isOne();

    // when/then the message for the event subprocess cannot be delivered
    assertThatThrownBy(() -> runtimeService.correlateMessage("Message1"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot correlate message 'Message1'");
  }

  @Deployment
  @Test
  void testSubProcessTwoOutgoingFlowsEndProcess() {
    // given a process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    Task innerTask = taskService.createTaskQuery().singleResult();

    // when the subprocess completes and is left via two outgoing sequence
    // flows that point to end events
    taskService.complete(innerTask.getId());

    // then the process instance is finished
    testRule.assertProcessEnded(processInstance.getId());
  }
}
