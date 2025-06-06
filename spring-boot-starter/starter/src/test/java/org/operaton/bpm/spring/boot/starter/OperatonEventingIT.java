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
package org.operaton.bpm.spring.boot.starter;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.history.event.HistoricIdentityLinkLogEventEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricTaskInstanceEventEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricVariableUpdateEventEntity;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.spring.boot.starter.event.TaskEvent;
import org.operaton.bpm.spring.boot.starter.test.nonpa.BoundaryEventServiceTask;
import org.operaton.bpm.spring.boot.starter.test.nonpa.TestApplication;
import org.operaton.bpm.spring.boot.starter.test.nonpa.TestEventCaptor;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Date;

import org.assertj.core.util.DateUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(
  classes = {TestApplication.class},
  webEnvironment = WebEnvironment.NONE
)
@ActiveProfiles("eventing")
class OperatonEventingIT extends AbstractOperatonAutoConfigurationIT {

  private final RuntimeService runtime;
  private final TaskService taskService;
  private final TestEventCaptor eventCaptor;

  @Autowired
  public OperatonEventingIT(RuntimeService runtime, TaskService taskService, TestEventCaptor eventCaptor) {
      this.runtime = runtime;
      this.taskService = taskService;
      this.eventCaptor = eventCaptor;
  }

  private ProcessInstance instance;

  @BeforeEach
  void init() {
    eventCaptor.clear();
  }

  @AfterEach
  void stop() {
    if (instance != null) {
      // update stale instance
      instance = runtime.createProcessInstanceQuery().processInstanceId(instance.getProcessInstanceId()).active().singleResult();
      if (instance != null) {
        runtime.deleteProcessInstance(instance.getProcessInstanceId(), "eventing shutdown");
      }
    }
    for (HistoricVariableInstance historicVariableInstance : historyService.createHistoricVariableInstanceQuery().list()) {
      historyService.deleteHistoricVariableInstance(historicVariableInstance.getId());
    }
  }

  @Test
  final void shouldEventTaskCreation() {
    // when
    startEventingInstance();

    // then
    Task task = taskService.createTaskQuery().active().singleResult();
    assertTaskEvents(task, TaskListener.EVENTNAME_CREATE);
  }

  @Test
  final void shouldEventTaskCreationWithAssignment() {
    // when
    instance = runtime.startProcessInstanceByKey("eventingWithAssignment");

    // then
    Task task = taskService.createTaskQuery().active().singleResult();
    // two events fired ('create' and then 'assignment')
    assertTaskEvents(task, 2, TaskListener.EVENTNAME_ASSIGNMENT, TaskListener.EVENTNAME_CREATE);
  }

  @Test
  final void shouldEventTaskUpdate() {
    // given
    startEventingInstance();
    Task task = taskService.createTaskQuery().active().singleResult();
    eventCaptor.clear();

    // when
    taskService.setOwner(task.getId(), "newUser");

    // then
    assertTaskEvents(task, TaskListener.EVENTNAME_UPDATE);
  }

  @Test
  final void shouldEventTaskAssignment() {
    // given
    startEventingInstance();
    Task task = taskService.createTaskQuery().active().singleResult();
    eventCaptor.clear();

    // when
    taskService.setAssignee(task.getId(), "kermit");

    // then
    // two events fired ('update' and then 'assignment')
    assertTaskEvents(task, 2, TaskListener.EVENTNAME_ASSIGNMENT, TaskListener.EVENTNAME_UPDATE);
  }

  @Test
  final void shouldEventTaskComplete() {
    // given
    startEventingInstance();
    Task task = taskService.createTaskQuery().active().singleResult();
    eventCaptor.clear();

    // when
    taskService.complete(task.getId());

    // then
    assertTaskEvents(task, TaskListener.EVENTNAME_COMPLETE);
  }

  @Test
  final void shouldEventTaskDelete() {
    // given
    startEventingInstance();
    Task task = taskService.createTaskQuery().active().singleResult();
    eventCaptor.clear();

    // when
    runtimeService.deleteProcessInstance(instance.getProcessInstanceId(), "no need");

    // then
    assertTaskEvents(task, TaskListener.EVENTNAME_DELETE);
  }

  @Test
  final void shouldNotInvokeHandlerWhenSkippingCustomListeners() {
    // given
    startEventingInstance();
    eventCaptor.clear();

    // when
    runtimeService.deleteProcessInstance(instance.getProcessInstanceId(), "no need", true);

    // then
    assertThat(eventCaptor.taskEvents).isEmpty();
  }

  @Test
  final void shouldEventExecution() {
    // given
    startEventingInstance();
    Task task = taskService.createTaskQuery().active().singleResult();
    eventCaptor.clear();

    // when
    taskService.complete(task.getId());

    // then 8
    // 2 for user task (take, end)
    // 3 for service task (start, take, end)
    // 2 for end event (start, end)
    // 1 for process (end)
    int expectedCount = 2 + 3 + 2 + 1;
    assertThat(eventCaptor.executionEvents).hasSize(expectedCount);
    assertThat(eventCaptor.immutableExecutionEvents).hasSize(expectedCount);
    assertThat(eventCaptor.transactionExecutionEvents).hasSize(expectedCount);
    assertThat(eventCaptor.transactionImmutableExecutionEvents).hasSize(expectedCount);
  }

  @Test
  final void shouldEventExecutionWhenIntermediateCatchExists() {
    // given
    eventCaptor.clear();
    instance = runtime.startProcessInstanceByKey("eventingWithIntermediateCatch");
    // then 13
    // 1 for process (start)
    // 3 for start event (start, take, end)
    // 1 for timer event (start)
    int expectedCount = 1 + 3 +  1;
    assertThat(eventCaptor.executionEvents).hasSize(expectedCount);
    assertThat(eventCaptor.immutableExecutionEvents).hasSize(expectedCount);
    assertThat(eventCaptor.transactionExecutionEvents).hasSize(expectedCount);
    assertThat(eventCaptor.transactionImmutableExecutionEvents).hasSize(expectedCount);
  }

  @Test
  final void shouldEventExecutionWhenBoundaryEventExists() {
    // given
    eventCaptor.clear();
    instance = runtime.startProcessInstanceByKey("eventingWithBoundaryEvent");
    runtime.signalEventReceived("countSignal");
    // then 13
    // 1 for process (start)
    // 3 for start event (start, take, end)
    // 3 for signal event (start, take, end)
    // 3 for service task (start, take, end)
    // 2 for end event (start, end)
    // 1 for process (end)
    int expectedCount = 1 + 3 + 3 + 3 + 2 + 1;
    assertThat(eventCaptor.executionEvents).hasSize(expectedCount);
    assertThat(eventCaptor.immutableExecutionEvents).hasSize(expectedCount);
    assertThat(eventCaptor.transactionExecutionEvents).hasSize(expectedCount);
    assertThat(eventCaptor.transactionImmutableExecutionEvents).hasSize(expectedCount);
  }

  @Test
  final void shouldEventExecutionWhenBoundaryEventWithErrorExists() {
    // given
    eventCaptor.clear();
    instance = runtime.startProcessInstanceByKey("eventingWithBoundaryEvent",
            Collections.singletonMap(BoundaryEventServiceTask.ERROR_NAME, "testError"));
    runtime.signalEventReceived("countSignal");
    // then 15
    // 1 for process (start)
    // 3 for start event (start, take, end)
    // 3 for signal event (start, take, end)
    // 2 for service task (start, end)
    // 3 for boundary error event (start, take, end)
    // 2 for end event (start, end)
    // 1 for process (end)
    int expectedCount = 1 + 3 + 3 + 2 + 3 + 2 + 1;
    assertThat(eventCaptor.executionEvents).hasSize(expectedCount);
    assertThat(eventCaptor.immutableExecutionEvents).hasSize(expectedCount);
    assertThat(eventCaptor.transactionExecutionEvents).hasSize(expectedCount);
    assertThat(eventCaptor.transactionImmutableExecutionEvents).hasSize(expectedCount);
  }

  @Test
  final void shouldEventHistoryTaskAssignmentChanges() {
    // given
    startEventingInstance();
    Task task = taskService.createTaskQuery().active().singleResult();
    eventCaptor.clear();

    // when
    taskService.addCandidateUser(task.getId(), "userId");
    taskService.addCandidateGroup(task.getId(), "groupId");
    taskService.deleteCandidateUser(task.getId(), "userId");
    taskService.deleteCandidateGroup(task.getId(), "groupId");

    // then in reverse order

    // update task event
    HistoryEvent historyTaskEvent = eventCaptor.historyEvents.pop();
    assertThat(historyTaskEvent.getEventType()).isEqualTo("update");
    assertThat(historyTaskEvent).isInstanceOf(HistoricTaskInstanceEventEntity.class);

    // Remove candidate group
    HistoryEvent candidateGroupEvent = eventCaptor.historyEvents.pop();
    assertThat(candidateGroupEvent.getEventType()).isEqualTo("delete-identity-link");
    if (candidateGroupEvent instanceof HistoricIdentityLinkLogEventEntity entity) {
      assertThat(entity.getType()).isEqualTo("candidate");
      assertThat(entity.getOperationType()).isEqualTo("delete");
      assertThat(entity.getGroupId()).isEqualTo("groupId");
    } else {
      fail("Expected identity link log event");
    }

    // update task event
    historyTaskEvent = eventCaptor.historyEvents.pop();
    assertThat(historyTaskEvent.getEventType()).isEqualTo("update");
    assertThat(historyTaskEvent).isInstanceOf(HistoricTaskInstanceEventEntity.class);

    // Remove candidate user
    HistoryEvent candidateUserEvent = eventCaptor.historyEvents.pop();
    assertThat(candidateUserEvent.getEventType()).isEqualTo("delete-identity-link");
    if (candidateUserEvent instanceof HistoricIdentityLinkLogEventEntity entity) {
      assertThat(entity.getType()).isEqualTo("candidate");
      assertThat(entity.getOperationType()).isEqualTo("delete");
      assertThat(entity.getUserId()).isEqualTo("userId");
    } else {
      fail("Expected identity link log event");
    }

    // update task event
    historyTaskEvent = eventCaptor.historyEvents.pop();
    assertThat(historyTaskEvent.getEventType()).isEqualTo("update");
    assertThat(historyTaskEvent).isInstanceOf(HistoricTaskInstanceEventEntity.class);

    // Add candidate group
    candidateGroupEvent = eventCaptor.historyEvents.pop();
    assertThat(candidateGroupEvent.getEventType()).isEqualTo("add-identity-link");
    if (candidateGroupEvent instanceof HistoricIdentityLinkLogEventEntity entity) {
      assertThat(entity.getType()).isEqualTo("candidate");
      assertThat(entity.getOperationType()).isEqualTo("add");
      assertThat(entity.getGroupId()).isEqualTo("groupId");
    } else {
      fail("Expected identity link log event");
    }

    // update task event
    historyTaskEvent = eventCaptor.historyEvents.pop();
    assertThat(historyTaskEvent.getEventType()).isEqualTo("update");
    assertThat(historyTaskEvent).isInstanceOf(HistoricTaskInstanceEventEntity.class);

    // Add candidate user
    candidateUserEvent = eventCaptor.historyEvents.pop();
    assertThat(candidateUserEvent.getEventType()).isEqualTo("add-identity-link");
    if (candidateUserEvent instanceof HistoricIdentityLinkLogEventEntity entity) {
      assertThat(entity.getType()).isEqualTo("candidate");
      assertThat(entity.getOperationType()).isEqualTo("add");
      assertThat(entity.getUserId()).isEqualTo("userId");
    } else {
      fail("Expected identity link log event");
    }

    assertThat(eventCaptor.historyEvents).isEmpty();
  }

  @Test
  void shouldEventHistoryTaskAttributeChanges() {
    // given
    startEventingInstance();
    Task task = taskService.createTaskQuery().active().singleResult();
    eventCaptor.clear();

    // when
    task.setName("new Name");
    taskService.saveTask(task);

    // then
    HistoryEvent taskChangeEvent = eventCaptor.historyEvents.pop();
    assertThat(taskChangeEvent.getEventType()).isEqualTo("update");
    if (taskChangeEvent instanceof HistoricTaskInstanceEventEntity entity) {
      assertThat(entity.getName()).isEqualTo("new Name");
    } else {
      fail("Expected task instance change event");
    }

  }

  @Test
  void shouldEventHistoryTaskMultipleAssignmentChanges() {
    // given
    startEventingInstance();
    Task task = taskService.createTaskQuery().active().singleResult();
    eventCaptor.clear();

    // when
    taskService.addCandidateUser(task.getId(), "user1");
    taskService.addCandidateUser(task.getId(), "user2");

    // then in reverse order

    // update task event
    HistoryEvent historyTaskEvent = eventCaptor.historyEvents.pop();
    assertThat(historyTaskEvent.getEventType()).isEqualTo("update");
    assertThat(historyTaskEvent).isInstanceOf(HistoricTaskInstanceEventEntity.class);

    // Add candidate user
    HistoryEvent candidateUserEvent = eventCaptor.historyEvents.pop();
    assertThat(candidateUserEvent.getEventType()).isEqualTo("add-identity-link");
    if (candidateUserEvent instanceof HistoricIdentityLinkLogEventEntity entity) {
      assertThat(entity.getType()).isEqualTo("candidate");
      assertThat(entity.getOperationType()).isEqualTo("add");
      assertThat(entity.getUserId()).isEqualTo("user2");
    } else {
      fail("Expected identity link log event");
    }

    // update task event
    historyTaskEvent = eventCaptor.historyEvents.pop();
    assertThat(historyTaskEvent.getEventType()).isEqualTo("update");
    assertThat(historyTaskEvent).isInstanceOf(HistoricTaskInstanceEventEntity.class);

    // Add candidate user
    candidateUserEvent = eventCaptor.historyEvents.pop();
    assertThat(candidateUserEvent.getEventType()).isEqualTo("add-identity-link");
    if (candidateUserEvent instanceof HistoricIdentityLinkLogEventEntity entity) {
      assertThat(entity.getType()).isEqualTo("candidate");
      assertThat(entity.getOperationType()).isEqualTo("add");
      assertThat(entity.getUserId()).isEqualTo("user1");
    } else {
      fail("Expected identity link log event");
    }

    assertThat(eventCaptor.historyEvents).isEmpty();
  }

  @Test
  void shouldEventHistoryTaskFollowUpDateChanges() {
    // given
    startEventingInstance();
    Task task = taskService.createTaskQuery().active().singleResult();
    eventCaptor.clear();

    Date now = DateUtil.now();

    // when
    task.setFollowUpDate(now);
    taskService.saveTask(task);

    // then
    HistoryEvent taskChangeEvent = eventCaptor.historyEvents.pop();
    assertThat(taskChangeEvent.getEventType()).isEqualTo("update");
    if (taskChangeEvent instanceof HistoricTaskInstanceEventEntity entity) {
      assertThat(entity.getFollowUpDate()).isEqualTo(now);
    } else {
      fail("Expected task instance change event");
    }

    assertThat(eventCaptor.executionEvents).isEmpty();
  }

  @Test
  final void shouldNotInvokeHandlerOnModificationWhenSkippingCustomListeners() {
    // given
    startEventingInstance();
    final TaskEntity task = (TaskEntity)taskService.createTaskQuery().active().singleResult();
    eventCaptor.clear();

    // when
    runtimeService
        .createProcessInstanceModification(instance.getProcessInstanceId())
        .cancelAllForActivity(task.getTaskDefinitionKey())
        .startBeforeActivity("service_task", instance.getId())
        .execute(true, false);

    // then
    assertThat(eventCaptor.executionEvents).isEmpty();
//    assertThat(eventCaptor.taskEvents).isEmpty(); => https://github.com/camunda/camunda-bpm-platform/issues/3103
  }

  @Test
  void shouldEventHistoryTaskVariableChange() {
    // given
    startEventingInstance();
    Task task = taskService.createTaskQuery().active().singleResult();
    eventCaptor.clear();

    // when
    taskService.setVariable(task.getId(), "VARIABLE", "VALUE");
    taskService.saveTask(task);

    // then

    // task update
    TaskEvent taskEvent = eventCaptor.taskEvents.pop();
    assertThat(taskEvent.getEventName()).isEqualTo(TaskListener.EVENTNAME_UPDATE);
    assertThat(taskEvent.getLastUpdated()).isNotNull();

    assertThat(eventCaptor.taskEvents).isEmpty();

    // historic task update
    HistoryEvent historicUpdateTaskEvent = eventCaptor.historyEvents.pop();
    assertThat(historicUpdateTaskEvent.getEventType()).isEqualTo("update");
    assertThat(historicUpdateTaskEvent).isInstanceOf(HistoricTaskInstanceEventEntity.class);

    // add variable
    HistoryEvent historicVariableEvent = eventCaptor.historyEvents.pop();
    assertThat(historicVariableEvent.getEventType()).isEqualTo("create");
    assertThat(historicVariableEvent).isInstanceOf(HistoricVariableUpdateEventEntity.class);

    assertThat(eventCaptor.historyEvents).isEmpty();
  }

  @Test
  void shouldEventHistoryTaskPriorityChange() {
    // given
    startEventingInstance();
    Task task = taskService.createTaskQuery().active().singleResult();
    eventCaptor.clear();

    // when
    taskService.setPriority(task.getId(), 14);

    // then

    // task update
    TaskEvent taskEvent = eventCaptor.taskEvents.pop();
    assertThat(taskEvent.getEventName()).isEqualTo(TaskListener.EVENTNAME_UPDATE);
    assertThat(taskEvent.getLastUpdated()).isNotNull();

    assertThat(eventCaptor.taskEvents).isEmpty();

    // historic task update
    HistoryEvent historicUpdateTaskEvent = eventCaptor.historyEvents.pop();
    assertThat(historicUpdateTaskEvent.getEventType()).isEqualTo("update");
    assertThat(historicUpdateTaskEvent).isInstanceOf(HistoricTaskInstanceEventEntity.class);

    assertThat(eventCaptor.historyEvents).isEmpty();
  }

  @Test
  void shouldEventHistoryTaskAddDeleteAttachment() {
    // given
    startEventingInstance();
    Task task = taskService.createTaskQuery().active().singleResult();
    eventCaptor.clear();

    // when
    String attachmentId = taskService.createAttachment(null, task.getId(), null, null, null,
        new ByteArrayInputStream("hello world".getBytes())).getId();
    taskService.deleteAttachment(attachmentId);

    // then

    // task update for delete
    TaskEvent taskEvent = eventCaptor.taskEvents.pop();
    assertThat(taskEvent.getEventName()).isEqualTo(TaskListener.EVENTNAME_UPDATE);
    assertThat(taskEvent.getLastUpdated()).isNotNull();

    // task update for create
    taskEvent = eventCaptor.taskEvents.pop();
    assertThat(taskEvent.getEventName()).isEqualTo(TaskListener.EVENTNAME_UPDATE);
    assertThat(taskEvent.getLastUpdated()).isNotNull();

    assertThat(eventCaptor.taskEvents).isEmpty();

    // task update for delete attachment
    HistoryEvent historicUpdateTaskEvent = eventCaptor.historyEvents.pop();
    assertThat(historicUpdateTaskEvent.getEventType()).isEqualTo("update");
    assertThat(historicUpdateTaskEvent).isInstanceOf(HistoricTaskInstanceEventEntity.class);

    // task update for add attachment
    historicUpdateTaskEvent = eventCaptor.historyEvents.pop();
    assertThat(historicUpdateTaskEvent.getEventType()).isEqualTo("update");
    assertThat(historicUpdateTaskEvent).isInstanceOf(HistoricTaskInstanceEventEntity.class);

    assertThat(eventCaptor.historyEvents).isEmpty();
  }

  protected void assertTaskEvents(Task task, String event) {
    assertTaskEvents(task, 1, event);
  }

  protected void assertTaskEvents(Task task, int numberOfEvents, String...events) {
    assertThat(eventCaptor.taskEvents).hasSize(numberOfEvents);
    assertThat(eventCaptor.immutableTaskEvents).hasSize(numberOfEvents);
    assertThat(eventCaptor.transactionTaskEvents).hasSize(numberOfEvents);
    assertThat(eventCaptor.transactionImmutableTaskEvents).hasSize(numberOfEvents);

    for (int i = 0; i < numberOfEvents; i++) {
      /*
       * oldest event happened before latest does not work for mutable transaction
       * listener events since the delegate task was altered to assignment when the
       * event is dispatched to the listener
       */
      assertTaskEvent(task, eventCaptor.taskEvents.pop(), events[i]);
      assertTaskEvent(task, eventCaptor.immutableTaskEvents.pop(), events[i]);
      assertTaskEvent(task, eventCaptor.transactionTaskEvents.pop(), events[0]);
      assertTaskEvent(task, eventCaptor.transactionImmutableTaskEvents.pop(), events[i]);
    }
  }

  protected void assertTaskEvent(Task task, TaskEvent taskEvent, String event) {
    assertThat(taskEvent.getEventName()).isEqualTo(event);
    assertThat(taskEvent.getId()).isEqualTo(task.getId());
    assertThat(taskEvent.getProcessInstanceId()).isEqualTo(task.getProcessInstanceId());
  }

  protected void startEventingInstance() {
    instance = runtime.startProcessInstanceByKey("eventing");
  }
}
