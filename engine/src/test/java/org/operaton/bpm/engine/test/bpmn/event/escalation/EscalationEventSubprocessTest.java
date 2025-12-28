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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philipp Ossler
 */
@ExtendWith(ProcessEngineExtension.class)
@SuppressWarnings({"java:S4144", "java:S5976"})
class EscalationEventSubprocessTest {

  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;
  HistoryService historyService;

  @Deployment
  @Test
  void testCatchEscalationEventInsideSubprocess() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting event subprocess inside the subprocess should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  /** CAM-9220 (https://app.camunda.com/jira/browse/CAM-9220) */
  @Deployment
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testThrowEscalationEventFromEventSubprocess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("embeddedEventSubprocess");

    Job job = managementService.createJobQuery().singleResult();
    managementService.executeJob(job.getId());

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskName("task in subprocess").count()).isZero();
    assertThat(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskName("task in process").count()).isOne();

    // second timer job shouldn't be available
    job = managementService.createJobQuery().singleResult();
    assertThat(job).isNull();

    // there should only be one completed Escalation Catch Boundary Event
    assertThat(historyService.createHistoricActivityInstanceQuery()
      .processInstanceId(processInstance.getId())
      .activityId("EscalationCatchBoundaryEvent")
      .finished()
      .count()).isOne();
  }

  @Deployment
  @Test
  void testCatchEscalationEventFromEmbeddedSubprocess() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting event subprocess outside the subprocess should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventSubprocessTest.testCatchEscalationEventFromCallActivity.bpmn20.xml"})
  @Test
  void testCatchEscalationEventFromCallActivity() {
    runtimeService.startProcessInstanceByKey("catchEscalationProcess");
    // when throw an escalation event on called process

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting event subprocess should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and continue the called process
    assertThat(taskService.createTaskQuery().taskName("task after thrown escalation").count()).isOne();
  }

  @Deployment
  @Test
  void testCatchEscalationEventFromTopLevelProcess() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event from top level process

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting event subprocess on the top level process should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and continue the process
    assertThat(taskService.createTaskQuery().taskName("task after thrown escalation").count()).isOne();
  }

  @Deployment
  @Test
  void testCatchEscalationEventFromMultiInstanceSubprocess() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside a multi-instance subprocess

    assertThat(taskService.createTaskQuery().count()).isEqualTo(10);
    // the non-interrupting event subprocess outside the subprocess should catch every escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isEqualTo(5);
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isEqualTo(5);
  }

  @Deployment
  @Test
  void testPreferEscalationEventSubprocessToBoundaryEvent() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting event subprocess inside the subprocess should catch the escalation event
    // (the boundary event on the subprocess should not catch the escalation event since the event subprocess consume this event)
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation inside subprocess").count()).isOne();
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment
  @Test
  void testEscalationEventSubprocessWithEscalationCode() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess with escalationCode=1

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting event subprocess with escalationCode=1 should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation 1").count()).isOne();
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment
  @Test
  void testEscalationEventSubprocessWithoutEscalationCode() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting event subprocess without escalationCode should catch the escalation event (and all other escalation events)
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment
  @Test
  void testInterruptionEscalationEventSubprocess() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    // the interrupting event subprocess inside the subprocess should catch the escalation event event and cancel the subprocess
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventSubprocessTest.testInterruptingEscalationEventSubprocessWithCallActivity.bpmn20.xml"})
  @Test
  void testInterruptingEscalationEventSubprocessWithCallActivity() {
    runtimeService.startProcessInstanceByKey("catchEscalationProcess");
    // when throw an escalation event on called process

    // the interrupting event subprocess should catch the escalation event and cancel the called process
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
  }

  @Deployment
  @Test
  void testInterruptionEscalationEventSubprocessWithMultiInstanceSubprocess() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the multi-instance subprocess

    // the interrupting event subprocess outside the subprocess should catch the first escalation event and cancel all instances of the subprocess
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
  }

  @Deployment
  @Test
  void testReThrowEscalationEventToBoundaryEvent() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    // the non-interrupting event subprocess inside the subprocess should catch the escalation event
    Task task = taskService.createTaskQuery().taskName("task after catched escalation inside subprocess").singleResult();
    assertThat(task).isNotNull();

    // when re-throw the escalation event from the escalation event subprocess
    taskService.complete(task.getId());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting boundary event on subprocess should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation on boundary event").count()).isOne();
    // and continue the process
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment
  @Test
  void testReThrowEscalationEventToBoundaryEventWithoutEscalationCode() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    // the non-interrupting event subprocess inside the subprocess should catch the escalation event
    Task task = taskService.createTaskQuery().taskName("task after catched escalation inside subprocess").singleResult();
    assertThat(task).isNotNull();

    // when re-throw the escalation event from the escalation event subprocess
    taskService.complete(task.getId());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting boundary event on subprocess without escalationCode should catch the escalation event (and all other escalation events)
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation on boundary event").count()).isOne();
    // and continue the process
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment
  @Test
  void testReThrowEscalationEventToEventSubprocess() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    // the non-interrupting event subprocess inside the subprocess should catch the escalation event
    Task task = taskService.createTaskQuery().taskName("task after catched escalation inside subprocess").singleResult();
    assertThat(task).isNotNull();

    // when re-throw the escalation event from the escalation event subprocess
    taskService.complete(task.getId());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the non-interrupting event subprocess on process level should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation on process level").count()).isOne();
    // and continue the process
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment
  @Test
  void testReThrowEscalationEventIsNotCatched() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    // the non-interrupting event subprocess inside the subprocess should catch the escalation event
    Task task = taskService.createTaskQuery().taskName("task after catched escalation inside subprocess").singleResult();
    assertThat(task).isNotNull();

    // when re-throw the escalation event from the escalation event subprocess
    taskService.complete(task.getId());

    // continue the subprocess, no activity should catch the re-thrown escalation event
    assertThat(taskService.createTaskQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment
  @Test
  void testThrowEscalationEventToEventSubprocess() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the first non-interrupting event subprocess inside the subprocess should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation inside subprocess1").count()).isOne();
    // and continue the subprocess
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();

    // when throw a second escalation event from the first event subprocess
    String taskId = taskService.createTaskQuery().taskName("task after catched escalation inside subprocess1").singleResult().getId();
    taskService.complete(taskId);

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    // the second non-interrupting event subprocess inside the subprocess should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation inside subprocess2").count()).isOne();
    assertThat(taskService.createTaskQuery().taskName("task in subprocess").count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventSubprocessTest.testPropagateOutputVariablesWhileCatchEscalationOnCallActivity.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesWhileCatchEscalationOnCallActivity() {
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("catchEscalationProcess", variables).getId();
    // when throw an escalation event on called process

    // the non-interrupting event subprocess should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and set the output variable of the called process to the process
    assertThat(runtimeService.getVariable(processInstanceId, "output")).isEqualTo(42);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventSubprocessTest.testPropagateOutputVariablesWhileCatchEscalationOnCallActivity.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesTwoTimes() {
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("catchEscalationProcess", variables).getId();
    // when throw an escalation event on called process

    // (1) the variables has been passed for the first time (from sub process to super process)
    Task taskInSuperProcess = taskService.createTaskQuery().taskDefinitionKey("taskAfterCatchedEscalation").singleResult();
    assertThat(taskInSuperProcess).isNotNull();
    assertThat(runtimeService.getVariable(processInstanceId, "output")).isEqualTo(42);

    // change variable "input" in sub process
    Task taskInSubProcess = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    runtimeService.setVariable(taskInSubProcess.getProcessInstanceId(), "input", 999);
    taskService.complete(taskInSubProcess.getId());

    // (2) the variables has been passed for the second time (from sub process to super process)
    assertThat(runtimeService.getVariable(processInstanceId, "output")).isEqualTo(999);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventSubprocessTest.testPropagateOutputVariablesWhileCatchInterruptingEscalationOnCallActivity.bpmn20.xml"})
  @Test
  void testPropagateOutputVariablesWhileCatchInterruptingEscalationOnCallActivity() {
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("catchEscalationProcess", variables).getId();
    // when throw an escalation event on called process

    // the interrupting event subprocess should catch the escalation event
    assertThat(taskService.createTaskQuery().taskName("task after catched escalation").count()).isOne();
    // and set the output variable of the called process to the process
    assertThat(runtimeService.getVariable(processInstanceId, "output")).isEqualTo(42);
  }

  @Deployment
  @Test
  void testRetrieveEscalationCodeVariableOnEventSubprocess() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    // the event subprocess should catch the escalation event
    Task task = taskService.createTaskQuery().taskName("task after catched escalation").singleResult();
    assertThat(task).isNotNull();

    // and set the escalationCode of the escalation event to the declared variable
    assertThat(runtimeService.getVariable(task.getExecutionId(), "escalationCodeVar")).isEqualTo("escalationCode");
  }

  @Deployment
  @Test
  void testRetrieveEscalationCodeVariableOnEventSubprocessWithoutEscalationCode() {
    runtimeService.startProcessInstanceByKey("escalationProcess");
    // when throw an escalation event inside the subprocess

    // the event subprocess without escalationCode should catch the escalation event
    Task task = taskService.createTaskQuery().taskName("task after catched escalation").singleResult();
    assertThat(task).isNotNull();

    // and set the escalationCode of the escalation event to the declared variable
    assertThat(runtimeService.getVariable(task.getExecutionId(), "escalationCodeVar")).isEqualTo("escalationCode");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventTest.throwEscalationEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventSubprocessTest.testInterruptingRetrieveEscalationCodeInSuperProcess.bpmn20.xml"})
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
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventSubprocessTest.testInterruptingRetrieveEscalationCodeInSuperProcessWithoutEscalationCode.bpmn20.xml"})
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
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventSubprocessTest.testNonInterruptingRetrieveEscalationCodeInSuperProcess.bpmn20.xml"})
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
      "org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventSubprocessTest.testNonInterruptingRetrieveEscalationCodeInSuperProcessWithoutEscalationCode.bpmn20.xml"})
  @Test
  void testNonInterruptingRetrieveEscalationCodeInSuperProcessWithoutEscalationCode() {
    runtimeService.startProcessInstanceByKey("catchEscalationProcess");

    // the event subprocess without escalationCode should catch the escalation event
    Task task = taskService.createTaskQuery().taskDefinitionKey("taskAfterCatchedEscalation").singleResult();
    assertThat(task).isNotNull();

    // and set the escalationCode of the escalation event to the declared variable
    assertThat(runtimeService.getVariable(task.getExecutionId(), "escalationCodeVar")).isEqualTo("escalationCode");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventSubprocessTest.testNonInterruptingEscalationTriggeredTwice.bpmn20.xml"})
  @Test
  void testNonInterruptingEscalationTriggeredTwiceWithMainTaskCompletedFirst() {

    // given
    runtimeService.startProcessInstanceByKey("escalationProcess");
    Task taskInMainprocess = taskService.createTaskQuery().taskDefinitionKey("TaskInMainprocess").singleResult();

    // when
    taskService.complete(taskInMainprocess.getId());

    // then
    assertThat(taskService.createTaskQuery().taskDefinitionKey("TaskInSubprocess").count()).isEqualTo(2);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventSubprocessTest.testNonInterruptingEscalationTriggeredTwice.bpmn20.xml"})
  @Test
  void testNonInterruptingEscalationTriggeredTwiceWithSubprocessTaskCompletedFirst() {

    // given
    runtimeService.startProcessInstanceByKey("escalationProcess");
    Task taskInMainprocess = taskService.createTaskQuery().taskDefinitionKey("TaskInMainprocess").singleResult();
    Task taskInSubprocess = taskService.createTaskQuery().taskDefinitionKey("TaskInSubprocess").singleResult();

    // when
    taskService.complete(taskInSubprocess.getId());
    taskService.complete(taskInMainprocess.getId());

    // then
    assertThat(taskService.createTaskQuery().taskDefinitionKey("TaskInSubprocess").count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventSubprocessTest.testNonInterruptingEscalationTriggeredTwiceByIntermediateEvent.bpmn20.xml"})
  @Test
  void testNonInterruptingEscalationTriggeredTwiceByIntermediateEventWithMainTaskCompletedFirst() {

    // given
    runtimeService.startProcessInstanceByKey("escalationProcess");
    Task taskInMainprocess = taskService.createTaskQuery().taskDefinitionKey("FirstTaskInMainprocess").singleResult();

    // when
    taskService.complete(taskInMainprocess.getId());

    // then
    assertThat(taskService.createTaskQuery().taskDefinitionKey("TaskInSubprocess").count()).isEqualTo(2);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("SecondTaskInMainprocess").count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/escalation/EscalationEventSubprocessTest.testNonInterruptingEscalationTriggeredTwiceByIntermediateEvent.bpmn20.xml"})
  @Test
  void testNonInterruptingEscalationTriggeredTwiceByIntermediateEventWithSubprocessTaskCompletedFirst() {

    // given
    runtimeService.startProcessInstanceByKey("escalationProcess");
    Task taskInMainprocess = taskService.createTaskQuery().taskDefinitionKey("FirstTaskInMainprocess").singleResult();
    Task taskInSubprocess = taskService.createTaskQuery().taskDefinitionKey("TaskInSubprocess").singleResult();

    // when
    taskService.complete(taskInSubprocess.getId());
    taskService.complete(taskInMainprocess.getId());

    // then
    assertThat(taskService.createTaskQuery().taskDefinitionKey("TaskInSubprocess").count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("SecondTaskInMainprocess").count()).isOne();
  }

}
