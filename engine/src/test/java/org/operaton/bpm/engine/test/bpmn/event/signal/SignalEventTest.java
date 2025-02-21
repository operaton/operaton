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
package org.operaton.bpm.engine.test.bpmn.event.signal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.EventSubscriptionQueryImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.digest._apacheCommonsCodec.Base64;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ExecutionQuery;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.variables.FailingJavaSerializable;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener;
import org.operaton.bpm.engine.test.util.ProcessEngineBootstrapRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.Variables.SerializationDataFormats;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author Daniel Meyer
 */
public class SignalEventTest {

  @ClassRule
  public static ProcessEngineBootstrapRule bootstrapRule = new ProcessEngineBootstrapRule(configuration ->
      configuration.setJavaSerializationFormatEnabled(true));
  protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule(bootstrapRule);
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected RepositoryService repositoryService;
  protected ManagementService managementService;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  protected boolean defaultEnsureJobDueDateSet;

  @Before
  public void init() {
    runtimeService = engineRule.getRuntimeService();
    taskService = engineRule.getTaskService();
    repositoryService = engineRule.getRepositoryService();
    managementService = engineRule.getManagementService();
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    defaultEnsureJobDueDateSet = processEngineConfiguration.isEnsureJobDueDateNotNull();
  }

  @After
  public void resetConfiguration() {
    processEngineConfiguration.setEnsureJobDueDateNotNull(defaultEnsureJobDueDateSet);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignal.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml"})
  @Test
  public void testSignalCatchIntermediate() {

    runtimeService.startProcessInstanceByKey("catchSignal");

    assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    runtimeService.startProcessInstanceByKey("throwSignal");

    assertThat(createEventSubscriptionQuery().count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignalBoundary.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml"})
  @Test
  public void testSignalCatchBoundary() {
    runtimeService.startProcessInstanceByKey("catchSignal");

    assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    runtimeService.startProcessInstanceByKey("throwSignal");

    assertThat(createEventSubscriptionQuery().count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignalBoundaryWithReceiveTask.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml"})
  @Test
  public void testSignalCatchBoundaryWithVariables() {
    HashMap<String, Object> variables1 = new HashMap<>();
    variables1.put("processName", "catchSignal");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("catchSignal", variables1);

    HashMap<String, Object> variables2 = new HashMap<>();
    variables2.put("processName", "throwSignal");
    runtimeService.startProcessInstanceByKey("throwSignal", variables2);

    assertThat(runtimeService.getVariable(pi.getId(), "processName")).isEqualTo("catchSignal");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignal.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignalAsynch.bpmn20.xml"})
  @Test
  public void testSignalCatchIntermediateAsynch() {

    runtimeService.startProcessInstanceByKey("catchSignal");

    assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    runtimeService.startProcessInstanceByKey("throwSignal");

    assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    // there is a job:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    try {
      ClockUtil.setCurrentTime(new Date(System.currentTimeMillis() + 1000));
      testRule.waitForJobExecutorToProcessAllJobs(10000);

      assertThat(createEventSubscriptionQuery().count()).isZero();
      assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
      assertThat(managementService.createJobQuery().count()).isZero();
    } finally {
      ClockUtil.setCurrentTime(new Date());
    }

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.catchMultipleSignals.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.throwAbortSignal.bpmn20.xml"})
  @Test
  public void testSignalCatchDifferentSignals() {

    runtimeService.startProcessInstanceByKey("catchSignal");

    assertThat(createEventSubscriptionQuery().count()).isEqualTo(2);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    runtimeService.startProcessInstanceByKey("throwAbort");

    assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    Task taskAfterAbort = taskService.createTaskQuery().taskAssignee("gonzo").singleResult();
    assertThat(taskAfterAbort).isNotNull();
    taskService.complete(taskAfterAbort.getId());

    runtimeService.startProcessInstanceByKey("throwSignal");

    assertThat(createEventSubscriptionQuery().count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  /**
   * Verifies the solution of https://jira.codehaus.org/browse/ACT-1309
   */
  @Deployment
  @Test
  public void testSignalBoundaryOnSubProcess() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("signalEventOnSubprocess");
    runtimeService.signalEventReceived("stopSignal");
    testRule.assertProcessEnded(pi.getProcessInstanceId());
  }

  private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
    return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutorTxRequired());
  }

  /**
   * TestCase to reproduce Issue ACT-1344
   */
  @Deployment
  @Test
  public void testNonInterruptingSignal() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingSignalEvent");

    List<Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
    assertThat(tasks).hasSize(1);
    Task currentTask = tasks.get(0);
    assertThat(currentTask.getName()).isEqualTo("My User Task");

    runtimeService.signalEventReceived("alert");

    tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
    assertThat(tasks).hasSize(2);

    for (Task task : tasks) {
      if (!task.getName().equals("My User Task") && !task.getName().equals("My Second User Task")) {
        fail("Expected: <My User Task> or <My Second User Task> but was <" + task.getName() + ">.");
      }
    }

    taskService.complete(taskService.createTaskQuery().taskName("My User Task").singleResult().getId());

    tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
    assertThat(tasks).hasSize(1);
    currentTask = tasks.get(0);
    assertThat(currentTask.getName()).isEqualTo("My Second User Task");
  }


  /**
   * TestCase to reproduce Issue ACT-1344
   */
  @Deployment
  @Test
  public void testNonInterruptingSignalWithSubProcess() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingSignalWithSubProcess");
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
    assertThat(tasks).hasSize(1);

    Task currentTask = tasks.get(0);
    assertThat(currentTask.getName()).isEqualTo("Approve");

    runtimeService.signalEventReceived("alert");

    tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
    assertThat(tasks).hasSize(2);

    for (Task task : tasks) {
      if (!task.getName().equals("Approve") && !task.getName().equals("Review")) {
        fail("Expected: <Approve> or <Review> but was <" + task.getName() + ">.");
      }
    }

    taskService.complete(taskService.createTaskQuery().taskName("Approve").singleResult().getId());

    tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
    assertThat(tasks).hasSize(1);

    currentTask = tasks.get(0);
    assertThat(currentTask.getName()).isEqualTo("Review");

    taskService.complete(taskService.createTaskQuery().taskName("Review").singleResult().getId());

    tasks = taskService.createTaskQuery().processInstanceId(pi.getProcessInstanceId()).list();
    assertThat(tasks).hasSize(1);

  }

  @Deployment
  @Test
  public void testSignalStartEventInEventSubProcess() {
    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("signalStartEventInEventSubProcess");

    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(1);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    assertThat(taskQuery.count()).isEqualTo(1);

    // send interrupting signal to event sub process
    runtimeService.signalEventReceived("alert");

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // check if user task doesn't exist because signal start event is interrupting
    taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    assertThat(taskQuery.count()).isZero();

    // check if execution doesn't exist because signal start event is interrupting
    executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isZero();
  }

  @Deployment
  @Test
  public void testNonInterruptingSignalStartEventInEventSubProcess() {
    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nonInterruptingSignalStartEventInEventSubProcess");

    // check if execution exists
    ExecutionQuery executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(1);

    // check if user task exists
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    assertThat(taskQuery.count()).isEqualTo(1);

    // send non interrupting signal to event sub process
    runtimeService.signalEventReceived("alert");

    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // check if user task still exists because signal start event is non interrupting
    taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    assertThat(taskQuery.count()).isEqualTo(1);

    // check if execution still exists because signal start event is non interrupting
    executionQuery = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId());
    assertThat(executionQuery.count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTest.signalStartEvent.bpmn20.xml"})
  @Test
  public void testSignalStartEvent() {
    // event subscription for signal start event
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert").count()).isEqualTo(1);

    runtimeService.signalEventReceived("alert");
    // the signal should start a new process instance
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTest.signalStartEvent.bpmn20.xml"})
  @Test
  public void testSuspendedProcessWithSignalStartEvent() {
    // event subscription for signal start event
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert").count()).isEqualTo(1);

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    repositoryService.suspendProcessDefinitionById(processDefinition.getId());

    runtimeService.signalEventReceived("alert");
    // the signal should not start a process instance for the suspended process definition
    assertThat(taskService.createTaskQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTest.signalStartEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTest.testOtherSignalStartEvent.bpmn20.xml"})
  @Test
  public void testMultipleProcessesWithSameSignalStartEvent() {
    // event subscriptions for signal start event
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert").count()).isEqualTo(2);

    runtimeService.signalEventReceived("alert");
    // the signal should start new process instances for both process definitions
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTest.signalStartEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml"})
  @Test
  public void testStartProcessInstanceBySignalFromIntermediateThrowingSignalEvent() {
    // start a process instance to throw a signal
    runtimeService.startProcessInstanceByKey("throwSignal");
    // the signal should start a new process instance
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTest.signalStartEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml"})
  @Test
  public void testIntermediateThrowingSignalEventWithSuspendedSignalStartEvent() {
    // event subscription for signal start event
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert").count()).isEqualTo(1);

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("startBySignal").singleResult();
    repositoryService.suspendProcessDefinitionById(processDefinition.getId());

    // start a process instance to throw a signal
    runtimeService.startProcessInstanceByKey("throwSignal");
    // the signal should not start a new process instance of the suspended process definition
    assertThat(taskService.createTaskQuery().count()).isZero();
  }

  @Deployment
  @Test
  public void testProcessesWithMultipleSignalStartEvents() {
    // event subscriptions for signal start event
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").count()).isEqualTo(2);

    runtimeService.signalEventReceived("alert");
    // the signal should start new process instances for both process definitions
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.catchAlertTwiceAndTerminate.bpmn20.xml"})
  @Test
  public void testThrowSignalMultipleCancellingReceivers() {
    RecorderExecutionListener.clear();

    runtimeService.startProcessInstanceByKey("catchAlertTwiceAndTerminate");

    // event subscription for intermediate signal events
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert").count()).isEqualTo(2);

    // try to send 'alert' signal to both executions
    runtimeService.signalEventReceived("alert");

    // then only one terminate end event was executed
    assertThat(RecorderExecutionListener.getRecordedEvents()).hasSize(1);

    // and instances ended successfully
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.catchAlertTwiceAndTerminate.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignal.bpmn20.xml"})
  @Test
  public void testIntermediateThrowSignalMultipleCancellingReceivers() {
    RecorderExecutionListener.clear();

    runtimeService.startProcessInstanceByKey("catchAlertTwiceAndTerminate");

    // event subscriptions for intermediate events
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").eventName("alert").count()).isEqualTo(2);

    // started process instance try to send 'alert' signal to both executions
    runtimeService.startProcessInstanceByKey("throwSignal");

    // then only one terminate end event was executed
    assertThat(RecorderExecutionListener.getRecordedEvents()).hasSize(1);

    // and both instances ended successfully
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTest.signalStartEvent.bpmn20.xml",
    "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignalAsync.bpmn20.xml"})
  @Test
  public void testAsyncSignalStartEventJobProperties() {
    processEngineConfiguration.setEnsureJobDueDateNotNull(false);

    ProcessDefinition catchingProcessDefinition = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionKey("startBySignal")
      .singleResult();

    // given a process instance that throws a signal asynchronously
    runtimeService.startProcessInstanceByKey("throwSignalAsync");
    // where the throwing instance ends immediately

    // then there is not yet a catching process instance
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    // but there is a job for the asynchronous continuation
    Job asyncJob = managementService.createJobQuery().singleResult();
    assertThat(asyncJob.getProcessDefinitionId()).isEqualTo(catchingProcessDefinition.getId());
    assertThat(asyncJob.getProcessDefinitionKey()).isEqualTo(catchingProcessDefinition.getKey());
    assertThat(asyncJob.getExceptionMessage()).isNull();
    assertThat(asyncJob.getExecutionId()).isNull();
    assertThat(asyncJob.getJobDefinitionId()).isNull();
    assertThat(asyncJob.getPriority()).isZero();
    assertThat(asyncJob.getProcessInstanceId()).isNull();
    assertThat(asyncJob.getRetries()).isEqualTo(3);
    assertThat(asyncJob.getDuedate()).isNull();
    assertThat(asyncJob.getDeploymentId()).isNull();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTest.signalStartEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignalAsync.bpmn20.xml"})
  @Test
  public void testAsyncSignalStartEventJobPropertiesDueDateSet() {
    Date testTime = new Date(1457326800000L);
    ClockUtil.setCurrentTime(testTime);
    processEngineConfiguration.setEnsureJobDueDateNotNull(true);

    ProcessDefinition catchingProcessDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("startBySignal")
        .singleResult();

    // given a process instance that throws a signal asynchronously
    runtimeService.startProcessInstanceByKey("throwSignalAsync");
    // where the throwing instance ends immediately

    // then there is not yet a catching process instance
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    // but there is a job for the asynchronous continuation
    Job asyncJob = managementService.createJobQuery().singleResult();
    assertThat(asyncJob.getProcessDefinitionId()).isEqualTo(catchingProcessDefinition.getId());
    assertThat(asyncJob.getProcessDefinitionKey()).isEqualTo(catchingProcessDefinition.getKey());
    assertThat(asyncJob.getExceptionMessage()).isNull();
    assertThat(asyncJob.getExecutionId()).isNull();
    assertThat(asyncJob.getJobDefinitionId()).isNull();
    assertThat(asyncJob.getPriority()).isZero();
    assertThat(asyncJob.getProcessInstanceId()).isNull();
    assertThat(asyncJob.getRetries()).isEqualTo(3);
    assertThat(asyncJob.getDuedate()).isEqualTo(testTime);
    assertThat(asyncJob.getDeploymentId()).isNull();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTest.signalStartEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignalAsync.bpmn20.xml"})
  @Test
  public void testAsyncSignalStartEvent() {
    ProcessDefinition catchingProcessDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("startBySignal")
        .singleResult();

    // given a process instance that throws a signal asynchronously
    runtimeService.startProcessInstanceByKey("throwSignalAsync");

    // with an async job to trigger the signal event
    Job job = managementService.createJobQuery().singleResult();

    // when the job is executed
    managementService.executeJob(job.getId());

    // then there is a process instance
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance).isNotNull();
    assertThat(processInstance.getProcessDefinitionId()).isEqualTo(catchingProcessDefinition.getId());

    // and a task
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
  }

  /**
   * CAM-4527
   */
  @Deployment
  @Test
  public void testNoContinuationWhenSignalInterruptsThrowingActivity() {

    // given a process instance
    runtimeService.startProcessInstanceByKey("signalEventSubProcess");

    // when throwing a signal in the sub process that interrupts the subprocess
    Task subProcessTask = taskService.createTaskQuery().singleResult();
    taskService.complete(subProcessTask.getId());

    // then execution should not have been continued after the subprocess
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("afterSubProcessTask").count()).isZero();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").count()).isEqualTo(1);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTest.signalStartEvent.bpmn20.xml")
  @Test
  public void testSetSerializedVariableValues() throws IOException, ClassNotFoundException {

    // when
    FailingJavaSerializable javaSerializable = new FailingJavaSerializable("foo");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    String serializedObject = StringUtil.fromBytes(Base64.encodeBase64(baos.toByteArray()), engineRule.getProcessEngine());

    // then it is not possible to deserialize the object
    try {
      new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
    } catch (RuntimeException e) {
      testRule.assertTextPresent("Exception while deserializing object.", e.getMessage());
    }

    // but it can be set as a variable when delivering a message:
    runtimeService
        .signalEventReceived(
            "alert",
            Variables.createVariables().putValueTyped("var",
                Variables
                    .serializedObjectValue(serializedObject)
                    .objectTypeName(FailingJavaSerializable.class.getName())
                    .serializationDataFormat(SerializationDataFormats.JAVA)
                    .create()));

    // then
    ProcessInstance startedInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(startedInstance).isNotNull();

    ObjectValue variableTyped = runtimeService.getVariableTyped(startedInstance.getId(), "var", false);
    assertThat(variableTyped).isNotNull();
    assertThat(variableTyped.isDeserialized()).isFalse();
    assertThat(variableTyped.getValueSerialized()).isEqualTo(serializedObject);
    assertThat(variableTyped.getObjectTypeName()).isEqualTo(FailingJavaSerializable.class.getName());
    assertThat(variableTyped.getSerializationDataFormat()).isEqualTo(SerializationDataFormats.JAVA.getName());
  }

  /**
   * CAM-6807
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.catchAlertSignalBoundary.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignalAsync.bpmn20.xml"})
  @Test
  @Ignore("CAM-6807")
  public void testAsyncSignalBoundary() {
    runtimeService.startProcessInstanceByKey("catchSignal");

    // given a process instance that throws a signal asynchronously
    runtimeService.startProcessInstanceByKey("throwSignalAsync");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();  // Throws Exception!

    // when the job is executed
    managementService.executeJob(job.getId());

    // then there is a process instance
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance).isNotNull();
//    assertEquals(catchingProcessDefinition.getId(), processInstance.getProcessDefinitionId());

    // and a task
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
  }

  @Test
  @Deployment
  public void testThrownSignalInEventSubprocessInSubprocess() {
    runtimeService.startProcessInstanceByKey("embeddedEventSubprocess");

    Task taskBefore = taskService.createTaskQuery().singleResult();
    assertThat(taskBefore).isNotNull();
    assertThat(taskBefore.getName()).isEqualTo("task in subprocess");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    //when job is executed task is created
    managementService.executeJob(job.getId());

    Task taskAfter = taskService.createTaskQuery().singleResult();
    assertThat(taskAfter).isNotNull();
    assertThat(taskAfter.getName()).isEqualTo("after catch");

    Job jobAfter = managementService.createJobQuery().singleResult();
    assertThat(jobAfter).isNull();
  }

}
