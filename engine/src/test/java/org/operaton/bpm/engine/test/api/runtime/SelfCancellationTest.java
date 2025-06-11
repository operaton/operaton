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

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.pvm.delegate.SignallableActivityBehavior;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.EndEvent;
import org.operaton.bpm.model.bpmn.instance.TerminateEventDefinition;

/**
 * Tests for when delegate code synchronously cancels the activity instance it belongs to.
 *
 * @author Thorben Lindhauer
 */
public class SelfCancellationTest {

  protected static final String MESSAGE = "Message";

  @RegisterExtension
  static ProcessEngineExtension processEngineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(processEngineRule);

  //========================================================================================================================
  //=======================================================MODELS===========================================================
  //========================================================================================================================

  public static final BpmnModelInstance PROCESS_WITH_CANCELING_RECEIVE_TASK = Bpmn.createExecutableProcess("process")
      .startEvent()
      .parallelGateway("fork")
      .userTask()
      .sendTask("sendTask")
        .operatonClass(SendMessageDelegate.class.getName())
        .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_END, RecorderExecutionListener.class.getName())
      .endEvent("endEvent")
        .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
      .moveToLastGateway()
      .receiveTask("receiveTask")
        .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_END, RecorderExecutionListener.class.getName())
        .message(MESSAGE)
      .endEvent("terminateEnd")
        .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_END, RecorderExecutionListener.class.getName())
      .done();

  public static final BpmnModelInstance PROCESS_WITH_CANCELING_RECEIVE_TASK_AND_USER_TASK_AFTER_SEND =
    modify(PROCESS_WITH_CANCELING_RECEIVE_TASK)
      .removeFlowNode("endEvent")
      .activityBuilder("sendTask")
      .userTask("userTask")
        .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
      .endEvent()
      .done();

  public static final BpmnModelInstance PROCESS_WITH_CANCELING_RECEIVE_TASK_WITHOUT_END_AFTER_SEND =
      modify(PROCESS_WITH_CANCELING_RECEIVE_TASK)
        .removeFlowNode("endEvent");

  public static final BpmnModelInstance PROCESS_WITH_CANCELING_RECEIVE_TASK_WITH_SEND_AS_SCOPE =
      modify(PROCESS_WITH_CANCELING_RECEIVE_TASK)
        .activityBuilder("sendTask")
        .boundaryEvent("boundary")
          .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
          .timerWithDuration("PT5S")
        .endEvent("endEventBoundary")
          .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
        .done();


  public static final BpmnModelInstance PROCESS_WITH_CANCELING_RECEIVE_TASK_WITH_SEND_AS_SCOPE_WITHOUT_END =
      modify(PROCESS_WITH_CANCELING_RECEIVE_TASK_WITH_SEND_AS_SCOPE)
        .removeFlowNode("endEvent");

  public static final BpmnModelInstance PROCESS_WITH_SUBPROCESS_AND_DELEGATE_MSG_SEND = modify(Bpmn.createExecutableProcess("process")
        .startEvent()
        .subProcess()
          .embeddedSubProcess()
            .startEvent()
            .userTask()
            .serviceTask("sendTask")
              .operatonClass(SendMessageDelegate.class.getName())
              .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_END, RecorderExecutionListener.class.getName())
            .endEvent("endEventSubProc")
              .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
            .subProcessDone()
        .endEvent()
          .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
        .done())
      .addSubProcessTo("process")
        .triggerByEvent()
        .embeddedSubProcess()
          .startEvent("startSubEvent")
            .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_END, RecorderExecutionListener.class.getName())
            .message(MESSAGE)
          .endEvent("endEventSubEvent")
            .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_END, RecorderExecutionListener.class.getName())
      .done();

  public static final BpmnModelInstance PROCESS_WITH_PARALLEL_SEND_TASK_AND_BOUNDARY_EVENT = Bpmn.createExecutableProcess("process")
      .startEvent()
      .parallelGateway("fork")
      .userTask()
      .endEvent()
        .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
      .moveToLastGateway()
      .sendTask("sendTask")
        .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_END, RecorderExecutionListener.class.getName())
        .operatonClass(SignalDelegate.class.getName())
      .boundaryEvent("boundary")
        .message(MESSAGE)
        .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_END, RecorderExecutionListener.class.getName())
      .endEvent("endEventBoundary")
        .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_END, RecorderExecutionListener.class.getName())
      .moveToNode("sendTask")
      .endEvent("endEvent")
        .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
      .done();


  public static final BpmnModelInstance PROCESS_WITH_SEND_TASK_AND_BOUNDARY_EVENT = Bpmn.createExecutableProcess("process")
      .startEvent()
      .sendTask("sendTask")
        .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_END, RecorderExecutionListener.class.getName())
        .operatonClass(SignalDelegate.class.getName())
      .boundaryEvent("boundary")
        .message(MESSAGE)
        .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_END, RecorderExecutionListener.class.getName())
      .endEvent("endEventBoundary")
        .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_END, RecorderExecutionListener.class.getName())
      .moveToNode("sendTask")
      .endEvent("endEvent")
        .operatonExecutionListenerClass(RecorderExecutionListener.EVENTNAME_START, RecorderExecutionListener.class.getName())
      .done();


  //========================================================================================================================
  //=========================================================INIT===========================================================
  //========================================================================================================================

  static {
    initEndEvent(PROCESS_WITH_CANCELING_RECEIVE_TASK, "terminateEnd");
    initEndEvent(PROCESS_WITH_CANCELING_RECEIVE_TASK_AND_USER_TASK_AFTER_SEND, "terminateEnd");
    initEndEvent(PROCESS_WITH_CANCELING_RECEIVE_TASK_WITH_SEND_AS_SCOPE, "terminateEnd");
    initEndEvent(PROCESS_WITH_CANCELING_RECEIVE_TASK_WITHOUT_END_AFTER_SEND, "terminateEnd");
    initEndEvent(PROCESS_WITH_CANCELING_RECEIVE_TASK_WITH_SEND_AS_SCOPE_WITHOUT_END, "terminateEnd");
  }

  public static void initEndEvent(BpmnModelInstance modelInstance, String endEventId) {
    EndEvent endEvent = modelInstance.getModelElementById(endEventId);
    TerminateEventDefinition terminateDefinition = modelInstance.newInstance(TerminateEventDefinition.class);
    endEvent.addChildElement(terminateDefinition);
  }

  //========================================================================================================================
  //=======================================================TESTS============================================================
  //========================================================================================================================


  protected RuntimeService runtimeService;
  protected TaskService taskService;

  @BeforeEach
  void clearRecorderListener()
  {
    RecorderExecutionListener.clear();
  }

  private void checkRecordedEvents(String ...activityIds) {
    List<RecorderExecutionListener.RecordedEvent> recordedEvents = RecorderExecutionListener.getRecordedEvents();
    assertThat(recordedEvents).hasSize(activityIds.length);

    for (int i = 0; i < activityIds.length; i++) {
      assertThat(recordedEvents.get(i).getActivityId()).isEqualTo(activityIds[i]);
    }
  }

  private void testParallelTerminationWithSend(BpmnModelInstance modelInstance) {
    // given
    testHelper.deploy(modelInstance);
    runtimeService.startProcessInstanceByKey("process");

    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.complete(task.getId());

    // then
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    checkRecordedEvents("receiveTask", "sendTask", "terminateEnd");
  }

  @Test
  void testTriggerParallelTerminateEndEvent() {
    testParallelTerminationWithSend(PROCESS_WITH_CANCELING_RECEIVE_TASK);
  }

  @Test
  void testTriggerParallelTerminateEndEventWithUserTask() {
    testParallelTerminationWithSend(PROCESS_WITH_CANCELING_RECEIVE_TASK_AND_USER_TASK_AFTER_SEND);
  }

  @Test
  void testTriggerParallelTerminateEndEventWithoutEndAfterSend() {
    testParallelTerminationWithSend(PROCESS_WITH_CANCELING_RECEIVE_TASK_WITHOUT_END_AFTER_SEND);
  }

  @Test
  void testTriggerParallelTerminateEndEventWithSendAsScope() {
    testParallelTerminationWithSend(PROCESS_WITH_CANCELING_RECEIVE_TASK_WITH_SEND_AS_SCOPE);
  }

  @Test
  void testTriggerParallelTerminateEndEventWithSendAsScopeWithoutEnd() {
    testParallelTerminationWithSend(PROCESS_WITH_CANCELING_RECEIVE_TASK_WITH_SEND_AS_SCOPE_WITHOUT_END);
  }

  @Test
  void testSendMessageInSubProcess() {
    // given
    testHelper.deploy(PROCESS_WITH_SUBPROCESS_AND_DELEGATE_MSG_SEND);
    runtimeService.startProcessInstanceByKey("process");

    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.complete(task.getId());

    // then
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    checkRecordedEvents("sendTask", "startSubEvent", "endEventSubEvent");
  }

  @Test
  void testParallelSendTaskWithBoundaryRecieveTask() {
    // given
    testHelper.deploy(PROCESS_WITH_PARALLEL_SEND_TASK_AND_BOUNDARY_EVENT);
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey("process");

    Execution activity = runtimeService.createExecutionQuery().activityId("sendTask").singleResult();
    runtimeService.signal(activity.getId());

    // then
    List<String> activities = runtimeService.getActiveActivityIds(procInst.getId());
    assertThat(activities)
            .isNotNull()
            .hasSize(1);
    checkRecordedEvents("sendTask", "boundary", "endEventBoundary");
  }

  @Test
  void testSendTaskWithBoundaryEvent() {
    // given
    testHelper.deploy(PROCESS_WITH_SEND_TASK_AND_BOUNDARY_EVENT);
    runtimeService.startProcessInstanceByKey("process");

    Execution activity = runtimeService.createExecutionQuery().activityId("sendTask").singleResult();
    runtimeService.signal(activity.getId());

    // then
    checkRecordedEvents("sendTask", "boundary", "endEventBoundary");
  }

  //========================================================================================================================
  //===================================================STATIC CLASSES=======================================================
  //========================================================================================================================
  public static class SendMessageDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
      RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService();
      runtimeService.correlateMessage(MESSAGE);
    }
  }

  public static class SignalDelegate implements SignallableActivityBehavior {

    @Override
    public void execute(ActivityExecution execution) throws Exception {
    }

    @Override
    public void signal(ActivityExecution execution, String signalEvent, Object signalData) throws Exception {
      RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService();
      runtimeService.correlateMessage(MESSAGE);
    }
  }
}
