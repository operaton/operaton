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
package org.operaton.bpm.engine.test.bpmn.event.escalation;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philipp Ossler
 */
@ExtendWith(ProcessEngineExtension.class)
@SuppressWarnings({"java:S4144", "java:S5976"})
class EscalationEventTest {

  RuntimeService runtimeService;
  TaskService taskService;

  @Deployment
  @Test
  void testThrowEscalationEventFromEmbeddedSubprocess() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting boundary event should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment
  @Test
  void testThrowEscalationEventHierarchical() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting boundary event inside the subprocess should catch the escalation event (and not the boundary event on process)
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation inside subprocess").count()).isOne();
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.nonInterruptingEscalationBoundaryEventOnCallActivity.bpmn20.xml"})
  @Test
  void testThrowEscalationEventFromCallActivity() {
    runtimeService.startProcessInstanceByKey("catchEscalationProcess");
    // when throw an escalation event on called process

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting boundary event on call activity should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and continue the called process
    assertThat(taskService.createTaskQuery().taskName("task after thrown escalation").count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml")
  @Test
  void testThrowEscalationEventNotCaught() {
    runtimeService.startProcessInstanceByKey("throwEscalationProcess");
    // when throw an escalation event

    // continue the process instance, no activity should catch the escalation event
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().taskName("task after thrown escalation").count()).isOne();
  }

  @Deployment
  @Test
  void testBoundaryEventWithEscalationCode() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess with escalationCode=1

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting boundary event with escalationCode=1 should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation 1").count()).isOne();
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment
  @Test
  void testBoundaryEventWithoutEscalationCode() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting boundary event without escalationCode should catch the escalation event (and all other escalation events)
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment
  @Test
  void testBoundaryEventWithEmptyEscalationCode() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting boundary event with empty escalationCode should catch the escalation event (and all other escalation events)
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment
  @Test
  void testBoundaryEventWithoutEscalationRef() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting boundary event without escalationRef should catch the escalation event (and all other escalation events)
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment
  @Test
  void testInterruptingEscalationBoundaryEventOnMultiInstanceSubprocess() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the multi-instance subprocess

    // the interrupting boundary event should catch the first escalation event and cancel all instances of the subprocess
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
  }

  @Deployment
  @Test
  void testNonInterruptingEscalationBoundaryEventOnMultiInstanceSubprocess() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the multi-instance subprocess

    assertThat(taskService.createTaskQuery().count()).isEqualTo(10);
    // the non-interrupting boundary event should catch every escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isEqualTo(5);
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isEqualTo(5);
  }

  /**
   * current bug: default value of 'cancelActivity' is 'true'
   */
  @Deployment
  @Disabled("CAM-4403")
  @Test
  void testImplicitNonInterruptingEscalationBoundaryEvent() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the implicit non-interrupting boundary event ('cancelActivity' is not defined) should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment
  @Test
  void testInterruptingEscalationBoundaryEvent(){
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    // the interrupting boundary should catch the escalation event event and cancel the subprocess
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.interruptingEscalationBoundaryEventOnCallActivity.bpmn20.xml"})
  @Test
  void testInterruptingEscalationBoundaryEventOnCallActivity(){
    runtimeService.startProcessInstanceByKey("catchEscalationProcess");
    // when throw an escalation event on called process

    // the interrupting boundary event on call activity should catch the escalation event and cancel the called process
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
  }

  @Deployment
  @Test
  void testParallelEscalationEndEvent() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation end event inside the subprocess

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting boundary event should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and continue the parallel flow in subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment
  @Test
  void testEscalationEndEvent() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation end event inside the subprocess

    // the subprocess should end and
    // the non-interrupting boundary event should catch the escalation event
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.testPropagateOutputVariablesWhileCatchEscalationOnCallActivity.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesWhileCatchEscalationOnCallActivity() {
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("catchEscalationProcess", variables).getId();
    // when throw an escalation event on called process

    // the non-interrupting boundary event on call activity should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and set the output variable of the called process to the process
    assertThat(runtimeService.getVariable(processInstanceId, "output")).isEqualTo(42);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.testPropagateOutputVariablesWhileCatchEscalationOnCallActivity.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesTwoTimes() {
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("catchEscalationProcess", variables).getId();
    // when throw an escalation event on called process

    Task taskInSuperProcess = taskService.createTaskQuery().taskDefinitionKey("taskAfterCatchedEscalation").singleResult();
    assertThat(taskInSuperProcess).isNotNull();

    // (1) the variables has been passed for the first time (from sub process to super process)
    assertThat(runtimeService.getVariable(processInstanceId, "output")).isEqualTo(42);

    // change variable "input" in sub process
    Task taskInSubProcess = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    runtimeService.setVariable(taskInSubProcess.getProcessInstanceId(), "input", 999);
    taskService.complete(taskInSubProcess.getId());

    // (2) the variables has been passed for the second time (from sub process to super process)
    assertThat(runtimeService.getVariable(processInstanceId, "output")).isEqualTo(999);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.testPropagateOutputVariablesWhileCatchInterruptingEscalationOnCallActivity.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesWhileCatchInterruptingEscalationOnCallActivity() {
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("catchEscalationProcess", variables).getId();
    // when throw an escalation event on called process

    // the interrupting boundary event on call activity should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and set the output variable of the called process to the process
    assertThat(runtimeService.getVariable(processInstanceId, "output")).isEqualTo(42);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.testPropagateOutputVariablesWithoutCatchEscalation.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesWithoutCatchEscalation() {
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("catchEscalationProcess", variables).getId();
    // when throw an escalation event on called process

    // then the output variable of the called process should be set to the process
    // also if the escalation is not caught by the process
    assertThat(runtimeService.getVariable(processInstanceId, "output")).isEqualTo(42);
  }

  @Deployment
  @Test
  void testRetrieveEscalationCodeVariableOnBoundaryEvent() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    // the boundary event should catch the escalation event
    Task task = taskService.createTaskQuery().taskName("task after catched escalation").singleResult();
    assertThat(task).isNotNull();

    // and set the escalationCode of the escalation event to the declared variable
    assertThat(runtimeService.getVariable(task.getExecutionId(), "escalationCodeVar")).isEqualTo("escalationCode");
  }

  @Deployment
  @Test
  void testRetrieveEscalationCodeVariableOnBoundaryEventWithoutEscalationCode() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    // the boundary event without escalationCode should catch the escalation event
    Task task = taskService.createTaskQuery().taskName("task after catched escalation").singleResult();
    assertThat(task).isNotNull();

    // and set the escalationCode of the escalation event to the declared variable
    assertThat(runtimeService.getVariable(task.getExecutionId(), "escalationCodeVar")).isEqualTo("escalationCode");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.testInterruptingRetrieveEscalationCodeInSuperProcess.bpmn20.xml"})
  @Test
  void testInterruptingRetrieveEscalationCodeInSuperProcess() {
    runtimeService.startProcessInstanceByKey("catchEscalationProcess");

    // the event subprocess without escalationCode should catch the escalation event
    Task task = taskService.createTaskQuery().taskDefinitionKey("taskAfterCatchedEscalation").singleResult();
    assertThat(task).isNotNull();

    // and set the escalationCode of the escalation event to the declared variable
    assertThat(runtimeService.getVariable(task.getExecutionId(), "escalationCodeVar")).isEqualTo("escalationCode");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.testInterruptingRetrieveEscalationCodeInSuperProcessWithoutEscalationCode.bpmn20.xml"})
  @Test
  void testInterruptingRetrieveEscalationCodeInSuperProcessWithoutEscalationCode() {
    runtimeService.startProcessInstanceByKey("catchEscalationProcess");

    // the event subprocess without escalationCode should catch the escalation event
    Task task = taskService.createTaskQuery().taskDefinitionKey("taskAfterCatchedEscalation").singleResult();
    assertThat(task).isNotNull();

    // and set the escalationCode of the escalation event to the declared variable
    assertThat(runtimeService.getVariable(task.getExecutionId(), "escalationCodeVar")).isEqualTo("escalationCode");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.testNonInterruptingRetrieveEscalationCodeInSuperProcess.bpmn20.xml"})
  @Test
  void testNonInterruptingRetrieveEscalationCodeInSuperProcess() {
    runtimeService.startProcessInstanceByKey("catchEscalationProcess");

    // the event subprocess without escalationCode should catch the escalation event
    Task task = taskService.createTaskQuery().taskDefinitionKey("taskAfterCatchedEscalation").singleResult();
    assertThat(task).isNotNull();

    // and set the escalationCode of the escalation event to the declared variable
    assertThat(runtimeService.getVariable(task.getExecutionId(), "escalationCodeVar")).isEqualTo("escalationCode");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.testNonInterruptingRetrieveEscalationCodeInSuperProcessWithoutEscalationCode.bpmn20.xml"})
  @Test
  void testNonInterruptingRetrieveEscalationCodeInSuperProcessWithoutEscalationCode() {
    runtimeService.startProcessInstanceByKey("catchEscalationProcess");

    // the event subprocess without escalationCode should catch the escalation event
    Task task = taskService.createTaskQuery().taskDefinitionKey("taskAfterCatchedEscalation").singleResult();
    assertThat(task).isNotNull();

    // and set the escalationCode of the escalation event to the declared variable
    assertThat(runtimeService.getVariable(task.getExecutionId(), "escalationCodeVar")).isEqualTo("escalationCode");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/testOutputVariablesWhileThrowEscalation.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.escalationParent.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesWhileThrowEscalation() {
    // given
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("EscalationParentProcess", variables).getId();

    // when throw an escalation event on called process
    String id = taskService.createTaskQuery().taskName("ut2").singleResult().getId();
    taskService.complete(id);

    // then
    checkOutput(processInstanceId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/testOutputVariablesWhileThrowEscalationTwoLevels.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.escalationParent.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesWhileThrowEscalationTwoLevels() {
    // given
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("EscalationParentProcess", variables).getId();

    // when throw an escalation event on called process
    String id = taskService.createTaskQuery().taskName("ut2").singleResult().getId();
    taskService.complete(id);

    // then
    checkOutput(processInstanceId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/testOutputVariablesWhileThrowEscalationThreeLevels.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.escalationParent.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesWhileThrowEscalationThreeLevels() {
    // given
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("EscalationParentProcess", variables).getId();

    // when throw an escalation event on called process
    String id = taskService.createTaskQuery().taskName("ut2").singleResult().getId();
    taskService.complete(id);

    // then
    checkOutput(processInstanceId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/testOutputVariablesWhileThrowEscalationInSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.escalationParent.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesWhileThrowEscalationInSubProcess() {
    // given
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("EscalationParentProcess", variables).getId();

    // when throw an escalation event on called process
    String id = taskService.createTaskQuery().taskName("ut2").singleResult().getId();
    taskService.complete(id);

    // then
    checkOutput(processInstanceId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/testOutputVariablesWhileThrowEscalationInSubProcessThreeLevels.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.escalationParent.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesWhileThrowEscalationInSubProcessThreeLevels() {
    // given
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("EscalationParentProcess", variables).getId();

    // when throw an escalation event on called process
    String id = taskService.createTaskQuery().taskName("ut2").singleResult().getId();
    taskService.complete(id);

    // then
    checkOutput(processInstanceId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/testOutputVariablesWhileThrowEscalation2.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.escalationParent.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesWhileThrowEscalation2() {
    // given
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("EscalationParentProcess", variables).getId();

    // when throw an escalation event on called process
    String id = taskService.createTaskQuery().taskName("inside subprocess").singleResult().getId();
    taskService.complete(id);

    // then
    checkOutput(processInstanceId);
  }

  protected void checkOutput(String processInstanceId) {
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and set the output variable of the called process to the process
    assertThat(runtimeService.getVariable(processInstanceId, "cancelReason")).isNotNull();
    assertThat(runtimeService.getVariable(processInstanceId, "output")).isEqualTo(42);
  }
}
