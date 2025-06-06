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
package org.operaton.bpm.engine.test.bpmn.tasklistener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.delegate.TaskListener.EVENTNAME_ASSIGNMENT;
import static org.operaton.bpm.engine.delegate.TaskListener.EVENTNAME_COMPLETE;
import static org.operaton.bpm.engine.delegate.TaskListener.EVENTNAME_CREATE;
import static org.operaton.bpm.engine.delegate.TaskListener.EVENTNAME_UPDATE;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.AssigneeAssignment;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.CandidateUserAssignment;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.CompletingTaskListener;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.RecorderTaskListener;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

class TaskListenerEventLifecycleTest extends AbstractTaskListenerTest {
  /*
  Testing Task Event chains to validate event lifecycle order

  Note: completing tasks inside TaskListeners breaks the global task event lifecycle. However,
  task events are still fired in the right order inside the listener "scope".
   */

  protected static final String[] TRACKED_EVENTS = {
      EVENTNAME_CREATE,
      EVENTNAME_UPDATE,
      EVENTNAME_ASSIGNMENT,
      EVENTNAME_COMPLETE,
      TaskListener.EVENTNAME_DELETE
  };

  // CREATE phase

  @Test
  void shouldOnlyFireCreateAndAssignmentEventsWhenTaskIsCreated() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTaskWithAssignee("kermit", TRACKED_EVENTS);

    // when
    runtimeService.startProcessInstanceByKey("process");

    // then
    List<String> orderedEvents = RecorderTaskListener.getOrderedEvents();
    assertThat(orderedEvents)
      .hasSize(2)
      .containsExactly(EVENTNAME_CREATE, EVENTNAME_ASSIGNMENT);
  }

  @Test
  void shouldFireCompleteEventOnTaskCompletedInCreateListener() {
    // given
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  EVENTNAME_CREATE,
                                                                                  CompletingTaskListener.class);
    testRule.deploy(model);

    // when
    runtimeService.startProcessInstanceByKey("process");

    // then
    List<String> orderedEvents = RecorderTaskListener.getOrderedEvents();
    assertThat(orderedEvents)
      .hasSize(2)
      .containsExactly(EVENTNAME_CREATE, EVENTNAME_COMPLETE);
  }

  @Test
  void shouldFireCompleteEventOnTaskCompletedInAssignmentListenerWhenTaskCreated() {
    // given
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  EVENTNAME_ASSIGNMENT,
                                                                                  CompletingTaskListener.class);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.setAssignee(task.getId(), "gonzo");

    // then
    List<String> orderedEvents = RecorderTaskListener.getOrderedEvents();
    assertThat(orderedEvents)
      .hasSize(4)
      .containsExactly(EVENTNAME_CREATE, EVENTNAME_UPDATE, EVENTNAME_ASSIGNMENT, EVENTNAME_COMPLETE);
  }

  @Test
  void shouldFireCreateEventBeforeTimeoutEventWhenTaskCreated() {
    // given
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    Calendar now = Calendar.getInstance();
    now.add(Calendar.HOUR, -1);
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
        .startEvent()
          .userTask("task")
            .operatonTaskListenerClass(EVENTNAME_CREATE, RecorderTaskListener.class)
            .operatonTaskListenerClassTimeoutWithDate(TaskListener.EVENTNAME_TIMEOUT,
                                                     RecorderTaskListener.class,
                                                     sdf.format(now.getTime()))
        .endEvent()
        .done();
    testRule.deploy(model);

    // when
    runtimeService.startProcessInstanceByKey("process");
    testRule.waitForJobExecutorToProcessAllJobs(0L);

    // then
    List<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    // the TIMEOUT event will always fire after the CREATE event, since the Timer Job can't be
    // picked up by the JobExecutor before it's committed. And it is committed in the same
    // transaction as the task creation phase.
    assertThat(orderedEvents)
      .hasSize(2)
      .containsExactly(EVENTNAME_CREATE, TaskListener.EVENTNAME_TIMEOUT);
  }

  @Test
  void shouldCancelTimeoutTaskListenerWhenTaskCompleted() {
    // given
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    Calendar now = Calendar.getInstance();
    now.add(Calendar.MINUTE, 10);
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
                                  .startEvent()
                                  .userTask("task")
                                  .operatonTaskListenerClass(EVENTNAME_CREATE, RecorderTaskListener.class)
                                  .operatonTaskListenerClass(EVENTNAME_COMPLETE, RecorderTaskListener.class)
                                  .operatonTaskListenerClassTimeoutWithDate(TaskListener.EVENTNAME_TIMEOUT,
                                                                           RecorderTaskListener.class,
                                                                           sdf.format(now.getTime()))
                                  .endEvent()
                                  .done();
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    long runningJobCount = managementService.createJobQuery().count();
    taskService.complete(task.getId());
    long completedJobCount = managementService.createJobQuery().count();

    // then
    LinkedList<String> orderedEvents = RecorderTaskListener.getOrderedEvents();
    assertThat(runningJobCount).isOne();
    assertThat(completedJobCount).isZero();
    assertThat(orderedEvents).hasSize(2);
    assertThat(orderedEvents.getLast()).isEqualToIgnoringCase(EVENTNAME_COMPLETE);
  }

  // UPDATE phase

  @Test
  void shouldFireUpdateEventOnPropertyChangeWhenTaskUpdated() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TRACKED_EVENTS);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.setOwner(task.getId(), "gonzo");

    // then
    List<String> orderedEvents = RecorderTaskListener.getOrderedEvents();
    // create event fired on task creation
    assertThat(orderedEvents)
      .hasSize(2)
      .containsExactly(EVENTNAME_CREATE, EVENTNAME_UPDATE);
  }

  @Test
  void shouldFireUpdateEventBeforeAssignmentEventOnSetAssigneeWhenTaskUpdated() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TRACKED_EVENTS);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.setAssignee(task.getId(), "gonzo");

    // then
    List<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    assertThat(orderedEvents)
      .hasSize(3)
      .containsExactly(EVENTNAME_CREATE, EVENTNAME_UPDATE, EVENTNAME_ASSIGNMENT);
  }

  @Test
  void shouldFireCompleteEventOnTaskCompletedInUpdateListener() {
    // given
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  EVENTNAME_UPDATE,
                                                                                  CompletingTaskListener.class);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.setPriority(task.getId(), 3000);

    // then
    List<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    // assignment event should not be processed
    assertThat(orderedEvents)
      .hasSize(3)
      .containsExactly(EVENTNAME_CREATE, EVENTNAME_UPDATE, EVENTNAME_COMPLETE);
  }

  @Test
  void shouldFireCompleteEventOnTaskCompletedInAssignmentListenerWhenTaskUpdated() {
    // given
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  EVENTNAME_ASSIGNMENT,
                                                                                  CompletingTaskListener.class);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.setAssignee(task.getId(), "kermit");

    // then
    List<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    assertThat(orderedEvents)
      .hasSize(4)
      .containsExactly(EVENTNAME_CREATE, EVENTNAME_UPDATE, EVENTNAME_ASSIGNMENT, EVENTNAME_COMPLETE);
  }

  @Test
  void shouldNotFireUpdateEventAfterCreateTaskListenerUpdatesProperties() {
    // given
    BpmnModelInstance process = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  EVENTNAME_CREATE,
                                                                                  ModifyingTaskListener.class);
    testRule.deploy(process);

    // when
    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // then
    List<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    // ASSIGNMENT Event is fired, since the ModifyingTaskListener sets an assignee, and the
    // ASSIGNMENT Event evaluation happens after the CREATE Event evaluation
    assertThat(orderedEvents)
      .hasSize(2)
      .containsExactly(EVENTNAME_CREATE, EVENTNAME_ASSIGNMENT);
  }

  @Test
  void shouldNotFireUpdateEventAfterUpdateTaskListenerUpdatesProperties() {
    // given
    BpmnModelInstance process = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                    null,
                                                                                    EVENTNAME_UPDATE,
                                                                                    ModifyingTaskListener.class);
    testRule.deploy(process);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    Task task = engineRule.getTaskService().createTaskQuery().singleResult();

    // when
    taskService.setPriority(task.getId(), 3000);

    // then
    // only the initial, first update event is expected
    List<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    // ASSIGNMENT Event is fired, since the ModifyingTaskListener sets an assignee, and the
    // ASSIGNMENT Event evaluation happens after the UPDATE Event evaluation
    assertThat(orderedEvents)
      .hasSize(3)
      .containsExactly(EVENTNAME_CREATE, EVENTNAME_UPDATE, EVENTNAME_ASSIGNMENT);
  }

  @Test
  void shouldNotFireUpdateEventAfterAssignmentTaskListenerUpdatesProperties() {
    // given
    BpmnModelInstance process = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                    null,
                                                                                    EVENTNAME_ASSIGNMENT,
                                                                                    ModifyingTaskListener.class);
    testRule.deploy(process);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    Task task = engineRule.getTaskService().createTaskQuery().singleResult();

    // when
    taskService.setAssignee(task.getId(), "john");

    // then
    // only one update event is expected, from the initial assignment
    List<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    assertThat(orderedEvents)
      .hasSize(3)
      .containsExactly(EVENTNAME_CREATE, EVENTNAME_UPDATE, EVENTNAME_ASSIGNMENT);
  }

  @Test
  void shouldNotFireUpdateEventAfterCompleteTaskListenerUpdatesProperties() {
    // given
    BpmnModelInstance process = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                    null,
                                                                                    EVENTNAME_COMPLETE,
                                                                                    ModifyingTaskListener.class);
    testRule.deploy(process);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    Task task = engineRule.getTaskService().createTaskQuery().singleResult();

    // when
    taskService.complete(task.getId());

    // then
    List<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    assertThat(orderedEvents)
      .hasSize(2)
      .containsExactly(EVENTNAME_CREATE, EVENTNAME_COMPLETE);
  }

  @Test
  void shouldNotFireUpdateEventAfterDeleteTaskListenerUpdatesProperties() {
    // given
    BpmnModelInstance process = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                    null,
                                                                                    TaskListener.EVENTNAME_DELETE,
                                                                                    ModifyingTaskListener.class);
    testRule.deploy(process);
    ProcessInstance processInstance = engineRule.getRuntimeService()
                                                .startProcessInstanceByKey("process");

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), "Trigger Delete Event");

    // then
    List<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    assertThat(orderedEvents)
        .hasSize(2)
        .containsExactly(EVENTNAME_CREATE, TaskListener.EVENTNAME_DELETE);
  }

  // COMPLETE phase

  @Test
  void shouldFireCompleteEventLastWhenTaskCompleted() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TRACKED_EVENTS);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.complete(task.getId());

    // then
    LinkedList<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    assertThat(orderedEvents).hasSize(2);
    assertThat(orderedEvents.getLast()).isEqualToIgnoringCase(EVENTNAME_COMPLETE);
  }

  @Test
  void shouldNotFireUpdateEventOnPropertyChangesInCompleteListener() {
    // given
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  EVENTNAME_COMPLETE,
                                                                                  CandidateUserAssignment.class);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.complete(task.getId());

    // then
    LinkedList<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    assertThat(orderedEvents).hasSize(2);
    assertThat(orderedEvents.getLast()).isEqualToIgnoringCase(EVENTNAME_COMPLETE);
  }

  @Test
  void shouldNotFireAssignmentEventOnAssigneeChangesInCompleteListener() {
    // given
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  EVENTNAME_COMPLETE,
                                                                                  AssigneeAssignment.class);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.complete(task.getId());

    // then
    LinkedList<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    assertThat(orderedEvents).hasSize(2);
    assertThat(orderedEvents.getLast()).isEqualToIgnoringCase(EVENTNAME_COMPLETE);
  }

  @Test
  void shouldNotFireDeleteEventOnTaskDeletedInCompleteListener() {
    // given
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  EVENTNAME_COMPLETE,
                                                                                  TaskDeleteTaskListener.class);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    assertThatThrownBy(() -> taskService.complete(taskId))
    // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The task cannot be deleted because is part of a running process");

    LinkedList<String> orderedEvents = RecorderTaskListener.getOrderedEvents();
    assertThat(orderedEvents).hasSize(2);
    assertThat(orderedEvents.getLast()).isEqualToIgnoringCase(EVENTNAME_COMPLETE);
  }

  @Test
  void shouldFireDeleteEventOnProcessInstanceDeletedInCompleteListener() {
    // given
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  EVENTNAME_COMPLETE,
                                                                                  ProcessInstanceDeleteTaskListener.class);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.complete(task.getId());

    // then
    LinkedList<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    assertThat(orderedEvents)
      .hasSize(3).containsExactly(EVENTNAME_CREATE,
        EVENTNAME_COMPLETE,
        TaskListener.EVENTNAME_DELETE);
  }

  @Test
  void shouldNotFireCompleteEventOnTaskCompletedInCompleteListener() {
    // given
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  EVENTNAME_COMPLETE,
                                                                                  CompletingTaskListener.class);
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();
    String taskId = task.getId();

    // when
    assertThatThrownBy(() -> taskService.complete(taskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("invalid task state");
    // then
    LinkedList<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    assertThat(orderedEvents).hasSize(2);
    assertThat(orderedEvents.getFirst()).isEqualToIgnoringCase(EVENTNAME_CREATE);
    assertThat(orderedEvents.getLast()).isEqualToIgnoringCase(EVENTNAME_COMPLETE);
  }

  // DELETE phase

  @Test
  void shouldFireDeleteEventLastWhenProcessDeleted() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TRACKED_EVENTS);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), "Canceled!");

    // then
    LinkedList<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    assertThat(orderedEvents).hasSize(2);
    assertThat(orderedEvents.getFirst()).isEqualToIgnoringCase(EVENTNAME_CREATE);
    assertThat(orderedEvents.getLast()).isEqualToIgnoringCase(TaskListener.EVENTNAME_DELETE);
  }

  @Test
  void shouldNotFireUpdateEventOnPropertyChangesInDeleteListener() {
    // given
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  TaskListener.EVENTNAME_DELETE,
                                                                                  CandidateUserAssignment.class);
    testRule.deploy(model);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), "Canceled!");

    // then
    LinkedList<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    assertThat(orderedEvents).hasSize(2);
    assertThat(orderedEvents.getFirst()).isEqualToIgnoringCase(EVENTNAME_CREATE);
    assertThat(orderedEvents.getLast()).isEqualToIgnoringCase(TaskListener.EVENTNAME_DELETE);
  }

  @Test
  void shouldNotFireAssignmentEventOnAssigneeChangesInDeleteListener() {
    // given
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  TaskListener.EVENTNAME_DELETE,
                                                                                  AssigneeAssignment.class);
    testRule.deploy(model);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), "Canceled!");

    // then
    LinkedList<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    assertThat(orderedEvents).hasSize(2);
    assertThat(orderedEvents.getFirst()).isEqualToIgnoringCase(EVENTNAME_CREATE);
    assertThat(orderedEvents.getLast()).isEqualToIgnoringCase(TaskListener.EVENTNAME_DELETE);
  }

  @Test
  void shouldNotFireCompleteEventOnCompleteAttemptInDeleteListener() {
    // given
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  TaskListener.EVENTNAME_DELETE,
                                                                                  CompletingTaskListener.class);
    testRule.deploy(model);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    // when
    assertThatThrownBy(() -> runtimeService.deleteProcessInstance(processInstanceId, "Canceled!"))
    // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("invalid task state");

    LinkedList<String> orderedEvents = RecorderTaskListener.getOrderedEvents();
    assertThat(orderedEvents).hasSize(2);
    assertThat(orderedEvents.getFirst()).isEqualToIgnoringCase(EVENTNAME_CREATE);
    assertThat(orderedEvents.getLast()).isEqualToIgnoringCase(TaskListener.EVENTNAME_DELETE);
  }

  @Test
  void shouldNotFireDeleteEventOnTaskDeleteAttemptInDeleteListener() {
    // given
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  TaskListener.EVENTNAME_DELETE,
                                                                                  TaskDeleteTaskListener.class);
    testRule.deploy(model);
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when
    assertThatThrownBy(() -> runtimeService.deleteProcessInstance(processInstanceId, "Canceled!"))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The task cannot be deleted because is part of a running process");

    LinkedList<String> orderedEvents = RecorderTaskListener.getOrderedEvents();
    assertThat(orderedEvents).hasSize(2);
    assertThat(orderedEvents.getFirst()).isEqualToIgnoringCase(EVENTNAME_CREATE);
    assertThat(orderedEvents.getLast()).isEqualToIgnoringCase(TaskListener.EVENTNAME_DELETE);
  }

  @Test
  void shouldNotFireDeleteEventOnProcessDeleteAttemptInDeleteListener() {
    // given
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(TRACKED_EVENTS,
                                                                                  null,
                                                                                  TaskListener.EVENTNAME_DELETE,
                                                                                  ProcessInstanceDeleteTaskListener.class);
    testRule.deploy(model);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), "Canceled!");

    // then
    LinkedList<String> orderedEvents = RecorderTaskListener.getOrderedEvents();

    assertThat(orderedEvents).hasSize(2);
    assertThat(orderedEvents.getFirst()).isEqualToIgnoringCase(EVENTNAME_CREATE);
    assertThat(orderedEvents.getLast()).isEqualToIgnoringCase(TaskListener.EVENTNAME_DELETE);
  }

  // HELPER methods and classes

  public static class ModifyingTaskListener implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {
      delegateTask.setAssignee("demo");
      delegateTask.setOwner("john");
      delegateTask.setDueDate(new Date());
    }
  }

  public static class TaskDeleteTaskListener implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {
      delegateTask.getProcessEngineServices().getTaskService().deleteTask(delegateTask.getId());
    }
  }

  public static class ProcessInstanceDeleteTaskListener implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {
      delegateTask.getProcessEngineServices().getRuntimeService()
                  .deleteProcessInstance(delegateTask.getProcessInstanceId(), "Trigger a Task Delete event.");
    }
  }
}
