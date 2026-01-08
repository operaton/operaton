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
package org.operaton.bpm.engine.test.bpmn.event.signal;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SignalEventReceivedBuilderTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;

  protected BpmnModelInstance signalStartProcess(String processId) {
    return Bpmn.createExecutableProcess(processId)
      .startEvent()
        .signal("signal")
      .userTask()
      .endEvent()
      .done();
  }

  protected BpmnModelInstance signalCatchProcess(String processId) {
    return Bpmn.createExecutableProcess(processId)
      .startEvent()
      .intermediateCatchEvent()
        .signal("signal")
      .userTask()
      .endEvent()
      .done();
  }

  @Test
  void testSendSignalToStartEvent() {
    testRule.deploy(signalStartProcess("signalStart"));

    runtimeService.createSignalEvent("signal").send();

    assertThat(taskService.createTaskQuery().count()).isOne();
  }

  @Test
  void testSendSignalToIntermediateCatchEvent() {
    testRule.deploy(signalCatchProcess("signalCatch"));

    runtimeService.startProcessInstanceByKey("signalCatch");

    runtimeService.createSignalEvent("signal").send();

    assertThat(taskService.createTaskQuery().count()).isOne();
  }

  @Test
  void testSendSignalToStartAndIntermediateCatchEvent() {
    testRule.deploy(signalStartProcess("signalStart"), signalCatchProcess("signalCatch"));

    runtimeService.startProcessInstanceByKey("signalCatch");

    runtimeService.createSignalEvent("signal").send();

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2L);
  }

  @Test
  void testSendSignalToMultipleStartEvents() {
    testRule.deploy(signalStartProcess("signalStart"), signalStartProcess("signalStart2"));

    runtimeService.createSignalEvent("signal").send();

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2L);
  }

  @Test
  void testSendSignalToMultipleIntermediateCatchEvents() {
    testRule.deploy(signalCatchProcess("signalCatch"), signalCatchProcess("signalCatch2"));

    runtimeService.startProcessInstanceByKey("signalCatch");
    runtimeService.startProcessInstanceByKey("signalCatch2");

    runtimeService.createSignalEvent("signal").send();

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2L);
  }

  @Test
  void testSendSignalWithExecutionId() {
    testRule.deploy(signalCatchProcess("signalCatch"), signalCatchProcess("signalCatch2"));

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalCatch");
    runtimeService.startProcessInstanceByKey("signalCatch2");

    EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
    String executionId = eventSubscription.getExecutionId();

    runtimeService.createSignalEvent("signal").executionId(executionId).send();

    assertThat(taskService.createTaskQuery().count()).isOne();
  }

  @Test
  void testSendSignalToStartEventWithVariables() {
    testRule.deploy(signalStartProcess("signalStart"));

    Map<String, Object> variables = Variables.createVariables()
        .putValue("var1", "a")
        .putValue("var2", "b");

    runtimeService.createSignalEvent("signal").setVariables(variables).send();

    Execution execution = runtimeService.createExecutionQuery().singleResult();
    assertThat(runtimeService.getVariables(execution.getId())).isEqualTo(variables);
  }

  @Test
  void testSendSignalToIntermediateCatchEventWithVariables() {
    testRule.deploy(signalCatchProcess("signalCatch"));

    runtimeService.startProcessInstanceByKey("signalCatch");

    Map<String, Object> variables = Variables.createVariables()
        .putValue("var1", "a")
        .putValue("var2", "b");

    runtimeService.createSignalEvent("signal").setVariables(variables).send();

    Execution execution = runtimeService.createExecutionQuery().singleResult();
    assertThat(runtimeService.getVariables(execution.getId())).isEqualTo(variables);
  }

  @Test
  void testNoSignalEventSubscription() {
    // assert that no exception is thrown
    assertThatCode(() -> runtimeService.createSignalEvent("signal").send())
        .doesNotThrowAnyException();
  }

  @Test
  void testNonExistingExecutionId() {

    try {
      runtimeService.createSignalEvent("signal").executionId("nonExisting").send();

    } catch (NullValueException e) {
      assertThat(e.getMessage()).contains("Cannot find execution with id 'nonExisting'");
    }
  }

  @Test
  void testNoSignalEventSubscriptionWithExecutionId() {
    testRule.deploy(Bpmn.createExecutableProcess("noSignal")
        .startEvent()
        .userTask()
        .endEvent()
        .done());

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("noSignal");
    String executionId = processInstance.getId();

    try {
      runtimeService.createSignalEvent("signal").executionId(executionId).send();

    } catch (NotFoundException e) {
      assertThat(e.getMessage()).contains("Execution '%s' has not subscribed to a signal event with name 'signal'".formatted(executionId));
    }
  }

}
