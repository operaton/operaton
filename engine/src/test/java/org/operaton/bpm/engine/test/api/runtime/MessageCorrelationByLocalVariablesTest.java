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
package org.operaton.bpm.engine.test.api.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.MismatchingMessageCorrelationException;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.MessageCorrelationResult;
import org.operaton.bpm.engine.runtime.MessageCorrelationResultType;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Svetlana Dorokhova
 */
public class MessageCorrelationByLocalVariablesTest {

  public static final String TEST_MESSAGE_NAME = "TEST_MSG";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  @Test
  void testReceiveTaskMessageCorrelation() {
    //given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
          .subProcess("SubProcess_1").embeddedSubProcess()
          .startEvent()
            .receiveTask("MessageReceiver_1").message(TEST_MESSAGE_NAME)
              .operatonInputParameter("localVar", "${loopVar}")
              .operatonInputParameter("constVar", "someValue")   //to test array of parameters
            .userTask("UserTask_1")
          .endEvent()
          .subProcessDone()
          .multiInstance().operatonCollection("${vars}").operatonElementVariable("loopVar").multiInstanceDone()
        .endEvent().done();

    testHelper.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("vars", List.of(1, 2, 3));
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    //when correlated by local variables
    String messageName = TEST_MESSAGE_NAME;
    Map<String, Object> correlationKeys = new HashMap<>();
    int correlationKey = 1;
    correlationKeys.put("localVar", correlationKey);
    correlationKeys.put("constVar", "someValue");

    MessageCorrelationResult messageCorrelationResult = engineRule.getRuntimeService().createMessageCorrelation(messageName)
        .localVariablesEqual(correlationKeys).setVariables(Variables.createVariables().putValue("newVar", "newValue")).correlateWithResult();

    //then one message is correlated, two other continue waiting
    checkExecutionMessageCorrelationResult(messageCorrelationResult, processInstance, "MessageReceiver_1");

    //uncorrelated executions
    List<Execution> uncorrelatedExecutions = engineRule.getRuntimeService().createExecutionQuery().activityId("MessageReceiver_1").list();
    assertThat(uncorrelatedExecutions).hasSize(2);

  }

  @Test
  void testIntermediateCatchEventMessageCorrelation() {
    //given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
          .subProcess("SubProcess_1").embeddedSubProcess()
          .startEvent()
            .intermediateCatchEvent("MessageReceiver_1").message(TEST_MESSAGE_NAME)
              .operatonInputParameter("localVar", "${loopVar}")
            .userTask("UserTask_1")
          .endEvent()
          .subProcessDone()
          .multiInstance().operatonCollection("${vars}").operatonElementVariable("loopVar").multiInstanceDone()
        .endEvent().done();

    testHelper.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("vars", List.of(1, 2, 3));
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    //when correlated by local variables
    String messageName = TEST_MESSAGE_NAME;
    int correlationKey = 1;

    MessageCorrelationResult messageCorrelationResult = engineRule.getRuntimeService().createMessageCorrelation(messageName)
        .localVariableEquals("localVar", correlationKey).setVariables(Variables.createVariables().putValue("newVar", "newValue")).correlateWithResult();

    //then one message is correlated, two others continue waiting
    checkExecutionMessageCorrelationResult(messageCorrelationResult, processInstance, "MessageReceiver_1");

    //uncorrelated executions
    List<Execution> uncorrelatedExecutions = engineRule.getRuntimeService().createExecutionQuery().activityId("MessageReceiver_1").list();
    assertThat(uncorrelatedExecutions).hasSize(2);

  }

  @Test
  void testMessageBoundaryEventMessageCorrelation() {
    //given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
          .subProcess("SubProcess_1").embeddedSubProcess()
          .startEvent()
            .userTask("UserTask_1")
              .operatonInputParameter("localVar", "${loopVar}")
              .operatonInputParameter("constVar", "someValue")   //to test array of parameters
              .boundaryEvent("MessageReceiver_1").message(TEST_MESSAGE_NAME)
            .userTask("UserTask_2")
          .endEvent()
          .subProcessDone()
          .multiInstance().operatonCollection("${vars}").operatonElementVariable("loopVar").multiInstanceDone()
        .endEvent().done();

    testHelper.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("vars", List.of(1, 2, 3));
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    //when correlated by local variables
    String messageName = TEST_MESSAGE_NAME;
    Map<String, Object> correlationKeys = new HashMap<>();
    int correlationKey = 1;
    correlationKeys.put("localVar", correlationKey);
    correlationKeys.put("constVar", "someValue");
    Map<String, Object> messagePayload = new HashMap<>();
    messagePayload.put("newVar", "newValue");

    MessageCorrelationResult messageCorrelationResult = engineRule.getRuntimeService().createMessageCorrelation(messageName)
        .localVariablesEqual(correlationKeys).setVariables(messagePayload).correlateWithResult();

    //then one message is correlated, two others continue waiting
    checkExecutionMessageCorrelationResult(messageCorrelationResult, processInstance, "UserTask_1");

    //uncorrelated executions
    List<Execution> uncorrelatedExecutions = engineRule.getRuntimeService().createExecutionQuery().activityId("UserTask_1").list();
    assertThat(uncorrelatedExecutions).hasSize(2);

  }

  @Test
  void testBothInstanceAndLocalVariableMessageCorrelation() {
    //given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
          .subProcess("SubProcess_1").embeddedSubProcess()
          .startEvent()
            .receiveTask("MessageReceiver_1").message(TEST_MESSAGE_NAME)
            .userTask("UserTask_1")
          .endEvent()
          .subProcessDone()
          .multiInstance().operatonCollection("${vars}").operatonElementVariable("loopVar").multiInstanceDone()
        .endEvent().done();

    model = modify(model).activityBuilder("MessageReceiver_1")
        .operatonInputParameter("localVar", "${loopVar}")
        .operatonInputParameter("constVar", "someValue")   //to test array of parameters
        .done();

    testHelper.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("vars", List.of(1, 2, 3));
    variables.put("processInstanceVar", "processInstanceVarValue");
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    //second process instance with another process instance variable value
    variables = new HashMap<>();
    variables.put("vars", List.of(1, 2, 3));
    variables.put("processInstanceVar", "anotherProcessInstanceVarValue");
    engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    //when correlated by local variables
    String messageName = TEST_MESSAGE_NAME;
    Map<String, Object> correlationKeys = new HashMap<>();
    int correlationKey = 1;
    correlationKeys.put("localVar", correlationKey);
    correlationKeys.put("constVar", "someValue");
    Map<String, Object> processInstanceKeys = new HashMap<>();
    String processInstanceVarValue = "processInstanceVarValue";
    processInstanceKeys.put("processInstanceVar", processInstanceVarValue);
    Map<String, Object> messagePayload = new HashMap<>();
    messagePayload.put("newVar", "newValue");

    MessageCorrelationResult messageCorrelationResult = engineRule.getRuntimeService().createMessageCorrelation(messageName)
        .processInstanceVariablesEqual(processInstanceKeys).localVariablesEqual(correlationKeys).setVariables(messagePayload).correlateWithResult();

    //then exactly one message is correlated = one receive task is passed by, two + three others continue waiting
    checkExecutionMessageCorrelationResult(messageCorrelationResult, processInstance, "MessageReceiver_1");

    //uncorrelated executions
    List<Execution> uncorrelatedExecutions = engineRule.getRuntimeService().createExecutionQuery().activityId("MessageReceiver_1").list();
    assertThat(uncorrelatedExecutions).hasSize(5);

  }

  @Test
  void testReceiveTaskMessageCorrelationFail() {
    //given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
          .subProcess("SubProcess_1").embeddedSubProcess()
          .startEvent()
            .receiveTask("MessageReceiver_1").message(TEST_MESSAGE_NAME)
              .operatonInputParameter("localVar", "${loopVar}")
              .operatonInputParameter("constVar", "someValue")   //to test array of parameters
            .userTask("UserTask_1")
          .endEvent()
          .subProcessDone()
          .multiInstance().operatonCollection("${vars}").operatonElementVariable("loopVar").multiInstanceDone()
        .endEvent().done();

    testHelper.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("vars", List.of(1, 2, 1));
    engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    //when correlated by local variables
    String messageName = TEST_MESSAGE_NAME;
    Map<String, Object> correlationKeys = new HashMap<>();
    int correlationKey = 1;
    correlationKeys.put("localVar", correlationKey);
    correlationKeys.put("constVar", "someValue");

    var messageCorrelationBuilder = engineRule.getRuntimeService()
      .createMessageCorrelation(messageName)
      .localVariablesEqual(correlationKeys)
      .setVariables(Variables.createVariables().putValue("newVar", "newValue"));

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlateWithResult)
      .isInstanceOf(MismatchingMessageCorrelationException.class)
      .hasMessageContaining("Cannot correlate a message with name '%s' to a single execution".formatted(TEST_MESSAGE_NAME));

  }

  @Test
  void testReceiveTaskMessageCorrelationAll() {
    //given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
          .subProcess("SubProcess_1").embeddedSubProcess()
          .startEvent()
            .receiveTask("MessageReceiver_1").message(TEST_MESSAGE_NAME)
              .operatonInputParameter("localVar", "${loopVar}")
              .operatonInputParameter("constVar", "someValue")   //to test array of parameters
            .userTask("UserTask_1")
          .endEvent()
          .subProcessDone()
          .multiInstance().operatonCollection("${vars}").operatonElementVariable("loopVar").multiInstanceDone()
        .endEvent().done();

    testHelper.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("vars", List.of(1, 2, 1));
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    //when correlated ALL by local variables
    String messageName = TEST_MESSAGE_NAME;
    Map<String, Object> correlationKeys = new HashMap<>();
    int correlationKey = 1;
    correlationKeys.put("localVar", correlationKey);
    correlationKeys.put("constVar", "someValue");

    List<MessageCorrelationResult> messageCorrelationResults = engineRule.getRuntimeService().createMessageCorrelation(messageName)
        .localVariablesEqual(correlationKeys).setVariables(Variables.createVariables().putValue("newVar", "newValue")).correlateAllWithResult();

    //then two messages correlated, one message task is still waiting
    for (MessageCorrelationResult result: messageCorrelationResults) {
      checkExecutionMessageCorrelationResult(result, processInstance, "MessageReceiver_1");
    }

    //uncorrelated executions
    List<Execution> uncorrelatedExecutions = engineRule.getRuntimeService().createExecutionQuery().activityId("MessageReceiver_1").list();
    assertThat(uncorrelatedExecutions).hasSize(1);

  }

  protected void checkExecutionMessageCorrelationResult(MessageCorrelationResult result, ProcessInstance processInstance, String activityId) {
    assertThat(result).isNotNull();
    assertThat(result.getResultType()).isEqualTo(MessageCorrelationResultType.Execution);
    assertThat(result.getExecution().getProcessInstanceId()).isEqualTo(processInstance.getId());
    ExecutionEntity entity = (ExecutionEntity) result.getExecution();
    assertThat(entity.getActivityId()).isEqualTo(activityId);
  }

}
