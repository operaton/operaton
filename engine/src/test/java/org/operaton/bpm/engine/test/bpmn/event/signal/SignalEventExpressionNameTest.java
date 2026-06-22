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

import java.util.Collections;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ExecutionQuery;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Johannes Heinemann
 */
@ExtendWith(ProcessEngineExtension.class)
class SignalEventExpressionNameTest {

  RuntimeService runtimeService;
  TaskService taskService;
  RepositoryService repositoryService;
  ManagementService managementService;

  @Deployment
  @Test
  void testSignalCatchIntermediate() {

    // given
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("var", "TestVar");

    // when
    runtimeService.startProcessInstanceByKey("catchSignal", variables);

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert-TestVar").count()).isOne();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventExpressionNameTest.testSignalCatchIntermediate.bpmn20.xml"})
  @Test
  void testSignalCatchIntermediateActsOnEventReceive() {

    // given
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("var", "TestVar");

    // when
    runtimeService.startProcessInstanceByKey("catchSignal", variables);
    runtimeService.signalEventReceived("alert-TestVar");

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert-TestVar").count()).isZero();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventExpressionNameTest.testSignalCatchIntermediate.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventExpressionNameTest.testSignalThrowIntermediate.bpmn20.xml"})
  @Test
  void testSignalThrowCatchIntermediate() {

    // given
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("var", "TestVar");

    // when
    runtimeService.startProcessInstanceByKey("catchSignal", variables);
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert-TestVar").count()).isOne();
    runtimeService.startProcessInstanceByKey("throwSignal", variables);

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert-${var}").count()).isZero();
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert-TestVar").count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventExpressionNameTest.testSignalCatchIntermediate.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventExpressionNameTest.testSignalThrowEnd.bpmn20.xml"})
  @Test
  void testSignalThrowEndCatchIntermediate() {

    // given
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("var", "TestVar");

    // when
    runtimeService.startProcessInstanceByKey("catchSignal", variables);
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert-TestVar").count()).isOne();
    runtimeService.startProcessInstanceByKey("throwEndSignal", variables);

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert-${var}").count()).isZero();
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert-TestVar").count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }


  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventExpressionNameTest.testSignalCatchBoundary.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventExpressionNameTest.testSignalThrowIntermediate.bpmn20.xml"})
  @Test
  void testSignalCatchBoundary() {

    // given
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("var", "TestVar");
    runtimeService.startProcessInstanceByKey("catchSignal", variables);
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert-TestVar").count()).isOne();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    // when
    runtimeService.startProcessInstanceByKey("throwSignal", variables);

    // then
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert-TestVar").count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventExpressionNameTest.testSignalStartEvent.bpmn20.xml"})
  @Test
  void testSignalStartEvent() {

    // given
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert-foo").count()).isOne();
    assertThat(taskService.createTaskQuery().count()).isZero();

    // when
    runtimeService.signalEventReceived("alert-foo");

    // then
    // the signal should start a new process instance
    assertThat(taskService.createTaskQuery().count()).isOne();
  }

  @Deployment
  @Test
  void testSignalStartEventInEventSubProcess() {

    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalStartEventInEventSubProcess");
    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isOne();
    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    assertThat(taskQuery.count()).isOne();

    // when
    runtimeService.signalEventReceived("alert-foo");

    // then
    assertThat(DummyServiceTask.wasExecuted).isTrue();
    // check if user task doesn't exist because signal start event is interrupting
    taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    assertThat(taskQuery.count()).isZero();
    // check if execution doesn't exist because signal start event is interrupting
    executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isZero();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventExpressionNameTest.testSignalStartEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventExpressionNameTest.throwAlertSignalAsync.bpmn20.xml"})
  @Test
  void testAsyncSignalStartEvent() {
    ProcessDefinition catchingProcessDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("startBySignal")
        .singleResult();

    // given a process instance that throws a signal asynchronously
    runtimeService.startProcessInstanceByKey("throwSignalAsync");
    // with an async job to trigger the signal event
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // when the job is executed
    managementService.executeJob(job.getId());

    // then there is a process instance
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance).isNotNull();
    assertThat(processInstance.getProcessDefinitionId()).isEqualTo(catchingProcessDefinition.getId());

    // and a task
    assertThat(taskService.createTaskQuery().count()).isOne();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventExpressionNameTest.testSignalCatchIntermediate.bpmn20.xml"})
  @Test
  void testSignalExpressionErrorHandling() {
    // given an empty variable mapping
    var variables = Collections.<String,Object>emptyMap();

    // when/then - the expression cannot be resolved and no signal should be available
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("catchSignal", variables))
      .isInstanceOf(ProcessEngineException.class);

    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").count()).isZero();
  }

}
