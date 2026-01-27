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

import java.util.Map;

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
import org.operaton.commons.utils.CollectionUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Joram Barrez
 * @author Falko Menge (operaton)
 */
class ConditionalSequenceFlowTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;

  @Deployment
  @Test
  void testUelExpression() {
    Map<String, Object> variables = CollectionUtil.singletonMap("input", "right");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("condSeqFlowUelExpr", variables);

    Task task = taskService
      .createTaskQuery()
      .processInstanceId(pi.getId())
      .singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo("task right");
  }

  @Deployment
  @Test
  void testValueAndMethodExpression() {
    // An order of price 150 is a standard order (goes through an UEL value expression)
    ConditionalSequenceFlowTestOrder order = new ConditionalSequenceFlowTestOrder(150);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("uelExpressions",
            CollectionUtil.singletonMap("order",  order));
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getName()).isEqualTo("Standard service");

    // While an order of 300, gives us a premium service (goes through an UEL method expression)
    order = new ConditionalSequenceFlowTestOrder(300);
    processInstance = runtimeService.startProcessInstanceByKey("uelExpressions",
            CollectionUtil.singletonMap("order",  order));
    task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getName()).isEqualTo("Premium service");

  }

  /**
   * Test that Conditional Sequence Flows throw an exception, if no condition
   * evaluates to true.
   *
   * <p>
   * BPMN 2.0.1 p. 427 (PDF 457):
   * "Multiple outgoing Sequence Flows with conditions behaves as an inclusive split."
   * </p>
   *
   * <p>
   * BPMN 2.0.1 p. 436 (PDF 466):
   * "The inclusive gateway throws an exception in case all conditions evaluate to false and a default flow has not been specified."
   * </p>
   *
   * @see <a href="https://app.camunda.com/jira/browse/CAM-1773">https://app.camunda.com/jira/browse/CAM-1773</a>
   */
  @Deployment
  @Test
  void testNoExpressionTrueThrowsException() {
    // given
    Map<String, Object> variables = CollectionUtil.singletonMap("input", "non-existing-value");

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("condSeqFlowUelExpr", variables))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No conditional sequence flow leaving the Flow Node 'theStart' could be selected for continuing the process");
  }

}
