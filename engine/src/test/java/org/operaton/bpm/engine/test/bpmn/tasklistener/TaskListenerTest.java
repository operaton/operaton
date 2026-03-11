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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Attachment;
import org.operaton.bpm.engine.task.IdentityLinkType;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.CompletingTaskListener;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.RecorderTaskListener;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.RecorderTaskListener.RecordedTaskEvent;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.TaskDeleteListener;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.commons.utils.IoUtil;

import static org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Joram Barrez
 */
class TaskListenerTest extends AbstractTaskListenerTest {
  /*
  Testing use-cases when Task Events are thrown and caught by Task Listeners
   */

  @BeforeEach
  void resetListenerCounters() {
    VariablesCollectingListener.reset();
  }

  // CREATE Task Listener tests

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/tasklistener/TaskListenerTest.bpmn20.xml"})
  void testTaskCreateListener() {
    runtimeService.startProcessInstanceByKey("taskListenerProcess");
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Schedule meeting");
    assertThat(task.getDescription()).isEqualTo("TaskCreateListener is listening!");
  }

  @Test
  void testCompleteTaskInCreateEventTaskListener() {
    // given process with user task and task create listener
    BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess("startToEnd")
            .startEvent()
            .userTask()
            .operatonTaskListenerClass(TaskListener.EVENTNAME_CREATE, CompletingTaskListener.class.getName())
            .name("userTask")
            .endEvent().done();

    testRule.deploy(modelInstance);

    // when process is started and user task completed in task create listener
    runtimeService.startProcessInstanceByKey("startToEnd");

    // then task is successfully completed without an exception
    assertThat(taskService.createTaskQuery().singleResult()).isNull();
  }

  @Test
  void testCompleteTaskInCreateEventTaskListenerWithIdentityLinks() {
    // given process with user task, identity links and task create listener
    BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess("startToEnd")
            .startEvent()
            .userTask()
            .operatonTaskListenerClass(TaskListener.EVENTNAME_CREATE, CompletingTaskListener.class.getName())
            .name("userTask")
            .operatonCandidateUsers(List.of("users1", "user2"))
            .operatonCandidateGroups(List.of("group1", "group2"))
            .endEvent().done();

    testRule.deploy(modelInstance);

    // when process is started and user task completed in task create listener
    runtimeService.startProcessInstanceByKey("startToEnd");

    // then task is successfully completed without an exception
    assertThat(taskService.createTaskQuery().singleResult()).isNull();
  }

  @Test
  void testCompleteTaskInCreateEventListenerWithFollowingCallActivity() {
    final BpmnModelInstance subProcess = Bpmn.createExecutableProcess("subProc")
                                             .startEvent()
                                             .userTask("calledTask")
                                             .endEvent()
                                             .done();

    final BpmnModelInstance instance = Bpmn.createExecutableProcess("mainProc")
                                           .startEvent()
                                           .userTask("mainTask")
                                           .operatonTaskListenerClass(TaskListener.EVENTNAME_CREATE, CompletingTaskListener.class.getName())
                                           .callActivity().calledElement("subProc")
                                           .endEvent()
                                           .done();

    testRule.deploy(subProcess, instance);

    runtimeService.startProcessInstanceByKey("mainProc");
    Task task = taskService.createTaskQuery().singleResult();

    assertThat(task.getTaskDefinitionKey()).isEqualTo("calledTask");
  }

  // COMPLETE Task Listener tests

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/tasklistener/TaskListenerTest.bpmn20.xml"})
  void testTaskCompleteListener() {
    TaskDeleteListener.clear();
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskListenerProcess");
    assertThat(runtimeService.getVariable(processInstance.getId(), "greeting")).isNull();
    assertThat(runtimeService.getVariable(processInstance.getId(), "expressionValue")).isNull();

    // Completing first task will change the description
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    // Check that the completion did not execute the delete listener
    assertThat(TaskDeleteListener.eventCounter).isZero();
    assertThat(TaskDeleteListener.lastTaskDefinitionKey).isNull();
    assertThat(TaskDeleteListener.lastDeleteReason).isNull();

    assertThat(runtimeService.getVariable(processInstance.getId(), "greeting")).isEqualTo("Hello from The Process");
    assertThat(runtimeService.getVariable(processInstance.getId(), "shortName")).isEqualTo("Act");
  }

  // DELETE Task Listener tests

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/tasklistener/TaskListenerTest.bpmn20.xml"})
  void testTaskDeleteListenerByProcessDeletion() {
    TaskDeleteListener.clear();
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskListenerProcess");

    assertThat(TaskDeleteListener.eventCounter).isZero();
    assertThat(TaskDeleteListener.lastTaskDefinitionKey).isNull();
    assertThat(TaskDeleteListener.lastDeleteReason).isNull();

    // delete process instance to delete task
    Task task = taskService.createTaskQuery().singleResult();
    runtimeService.deleteProcessInstance(processInstance.getProcessInstanceId(), "test delete task listener");

    assertThat(TaskDeleteListener.eventCounter).isEqualTo(1);
    assertThat(TaskDeleteListener.lastTaskDefinitionKey).isEqualTo(task.getTaskDefinitionKey());
    assertThat(TaskDeleteListener.lastDeleteReason).isEqualTo("test delete task listener");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/tasklistener/TaskListenerTest.bpmn20.xml"})
  void testTaskDeleteListenerByBoundaryEvent() {
    TaskDeleteListener.clear();
    runtimeService.startProcessInstanceByKey("taskListenerProcess");

    assertThat(TaskDeleteListener.eventCounter).isZero();
    assertThat(TaskDeleteListener.lastTaskDefinitionKey).isNull();
    assertThat(TaskDeleteListener.lastDeleteReason).isNull();

    // correlate message to delete task
    Task task = taskService.createTaskQuery().singleResult();
    runtimeService.correlateMessage("message");

    assertThat(TaskDeleteListener.eventCounter).isEqualTo(1);
    assertThat(TaskDeleteListener.lastTaskDefinitionKey).isEqualTo(task.getTaskDefinitionKey());
    assertThat(TaskDeleteListener.lastDeleteReason).isEqualTo("deleted");
  }

  @Test
  void testActivityInstanceIdOnDeleteInCalledProcess() {
    // given
    RecorderTaskListener.clear();

    BpmnModelInstance callActivityProcess = Bpmn.createExecutableProcess("calling")
                                                .startEvent()
                                                .callActivity()
                                                .calledElement("called")
                                                .endEvent()
                                                .done();

    BpmnModelInstance calledProcess = Bpmn.createExecutableProcess("called")
                                          .startEvent()
                                          .userTask()
                                          .operatonTaskListenerClass(TaskListener.EVENTNAME_CREATE, RecorderTaskListener.class.getName())
                                          .operatonTaskListenerClass(TaskListener.EVENTNAME_DELETE, RecorderTaskListener.class.getName())
                                          .endEvent()
                                          .done();

    testRule.deploy(callActivityProcess, calledProcess);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("calling");

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // then
    List<RecordedTaskEvent> recordedEvents = RecorderTaskListener.getRecordedEvents();
    assertThat(recordedEvents).hasSize(2);
    String createActivityInstanceId = recordedEvents.get(0).getActivityInstanceId();
    String deleteActivityInstanceId = recordedEvents.get(1).getActivityInstanceId();

    assertThat(deleteActivityInstanceId).isEqualTo(createActivityInstanceId);
  }

  @Test
  void testVariableAccessOnDeleteInCalledProcess() {
    // given
    VariablesCollectingListener.reset();

    BpmnModelInstance callActivityProcess = Bpmn.createExecutableProcess("calling")
                                                .startEvent()
                                                .callActivity()
                                                .operatonIn("foo", "foo")
                                                .calledElement("called")
                                                .endEvent()
                                                .done();

    BpmnModelInstance calledProcess = Bpmn.createExecutableProcess("called")
                                          .startEvent()
                                          .userTask()
                                          .operatonTaskListenerClass(TaskListener.EVENTNAME_DELETE, VariablesCollectingListener.class.getName())
                                          .endEvent()
                                          .done();

    testRule.deploy(callActivityProcess, calledProcess);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("calling",
                                                                               Variables.createVariables().putValue("foo", "bar"));

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // then
    VariableMap collectedVariables = VariablesCollectingListener.getCollectedVariables();
    assertThat(collectedVariables)
      .isNotNull()
      .hasSize(1)
      .containsEntry("foo", "bar");
  }

  // Expression & Scripts Task Listener tests

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/tasklistener/TaskListenerTest.bpmn20.xml"})
  void testTaskListenerWithExpression() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskListenerProcess");
    assertThat(runtimeService.getVariable(processInstance.getId(), "greeting2")).isNull();

    // Completing first task will change the description
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    assertThat(runtimeService.getVariable(processInstance.getId(), "greeting2")).isEqualTo("Write meeting notes");
  }

  @Test
  @Deployment
  void testScriptListener() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    assertThat((Boolean) runtimeService.getVariable(processInstance.getId(), "create")).isTrue();

    taskService.setAssignee(task.getId(), "test");
    assertThat((Boolean) runtimeService.getVariable(processInstance.getId(), "assignment")).isTrue();

    taskService.complete(task.getId());
    assertThat((Boolean) runtimeService.getVariable(processInstance.getId(), "complete")).isTrue();

    task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    if (processEngineConfiguration.getHistoryLevel().getId() >= HISTORYLEVEL_AUDIT) {
      HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().variableName("delete").singleResult();
      assertThat(variable).isNotNull();
      assertThat((Boolean) variable.getValue()).isTrue();
    }
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/tasklistener/TaskListenerTest.testScriptResourceListener.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/tasklistener/taskListener.groovy"
  })
  void testScriptResourceListener() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    assertThat((Boolean) runtimeService.getVariable(processInstance.getId(), "create")).isTrue();

    taskService.setAssignee(task.getId(), "test");
    assertThat((Boolean) runtimeService.getVariable(processInstance.getId(), "assignment")).isTrue();

    taskService.complete(task.getId());
    assertThat((Boolean) runtimeService.getVariable(processInstance.getId(), "complete")).isTrue();

    task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    if (processEngineConfiguration.getHistoryLevel().getId() >= HISTORYLEVEL_AUDIT) {
      HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().variableName("delete").singleResult();
      assertThat(variable).isNotNull();
      assertThat((Boolean) variable.getValue()).isTrue();
    }
  }

  // UPDATE Task Listener tests

  @Test
  void testUpdateTaskListenerOnAssign() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.setAssignee(task.getId(), "gonzo");
    taskService.setAssignee(task.getId(), "leelo");

    // then
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(2);
  }

  @Test
  void testUpdateTaskListenerOnOwnerSet() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.setOwner(task.getId(), "gonzo");

    // then
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(1);
  }

  @Test
  void testUpdateTaskListenerOnUserIdLinkAdd() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.addUserIdentityLink(task.getId(), "gonzo", IdentityLinkType.CANDIDATE);

    // then
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(1);
  }

  @Test
  void testUpdateTaskListenerOnUserIdLinkDelete() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();
    taskService.addUserIdentityLink(task.getId(), "gonzo", IdentityLinkType.CANDIDATE);

    // when
    taskService.deleteUserIdentityLink(task.getId(), "gonzo", IdentityLinkType.CANDIDATE);

    // then
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(2);
  }

  @Test
  void testUpdateTaskListenerOnGroupIdLinkAdd() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.addGroupIdentityLink(task.getId(), "admins", IdentityLinkType.CANDIDATE);

    // then
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(1);
  }

  @Test
  void testUpdateTaskListenerOnGroupIdLinkDelete() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();
    taskService.addGroupIdentityLink(task.getId(), "admins", IdentityLinkType.CANDIDATE);

    // when
    taskService.deleteGroupIdentityLink(task.getId(), "admins", IdentityLinkType.CANDIDATE);

    // then
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(2);
  }

  @Test
  void testUpdateTaskListenerOnTaskResolve() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.resolveTask(task.getId());

    // then
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(1);
  }

  @Test
  void testUpdateTaskListenerOnDelegate() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.delegateTask(task.getId(), "gonzo");

    // then
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(1);
  }

  @Test
  void testUpdateTaskListenerOnClaim() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.claim(task.getId(), "test");

    // then
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(1);
  }

  @Test
  void testUpdateTaskListenerOnPrioritySet() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.setPriority(task.getId(), 3000);

    // then
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(1);
  }

  @Test
  void testUpdateTaskListenerOnTaskFormSubmit() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    Task task = engineRule.getTaskService().createTaskQuery().singleResult();

    // when
    taskService.delegateTask(task.getId(), "john");
    processEngineConfiguration.getFormService().submitTaskForm(task.getId(), null);

    // then
    // first update event comes from delegating the task,
    // setting its delegation state to PENDING
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(2);
  }

  @Test
  void testUpdateTaskListenerOnPropertyUpdate() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    task.setDueDate(new Date());
    taskService.saveTask(task);

    // then
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(1);
  }

  @Test
  void testUpdateTaskListenerOnPropertyUpdateOnlyOnce() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    task.setAssignee("test");
    task.setDueDate(new Date());
    task.setOwner("test");
    taskService.saveTask(task);

    // then
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(1);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void testUpdateTaskListenerOnCommentCreate() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.createComment(task.getId(), null, "new comment");

    // then
    assertThat(RecorderTaskListener.getTotalEventCount()).isEqualTo(1);
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(1);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void testUpdateTaskListenerOnCommentAdd() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.addComment(task.getId(), null, "new comment");

    // then
    assertThat(RecorderTaskListener.getTotalEventCount()).isEqualTo(1);
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(1);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void testUpdateTaskListenerOnAttachmentCreate() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.createAttachment("foo", task.getId(), null, "bar", "baz", IoUtil.stringAsInputStream("foo"));

    // then
    assertThat(RecorderTaskListener.getTotalEventCount()).isEqualTo(1);
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(1);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void testUpdateTaskListenerOnAttachmentUpdate() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    Attachment attachment = taskService.createAttachment("foo", task.getId(), null, "bar", "baz", IoUtil.stringAsInputStream("foo"));
    attachment.setDescription("bla");
    attachment.setName("foo");

    // when
    taskService.saveAttachment(attachment);

    // then
    assertThat(RecorderTaskListener.getTotalEventCount()).isEqualTo(2);
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(2); // create and update attachment
  }

  @Test
  void testUpdateTaskListenerOnAttachmentDelete() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    Attachment attachment = taskService.createAttachment("foo", task.getId(), null, "bar", "baz", IoUtil.stringAsInputStream("foo"));

    // when
    taskService.deleteAttachment(attachment.getId());

    // then
    assertThat(RecorderTaskListener.getTotalEventCount()).isEqualTo(2);
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(2); // create and delete attachment
  }

  @Test
  void testUpdateTaskListenerOnAttachmentDeleteWithTaskId() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    Attachment attachment = taskService.createAttachment("foo", task.getId(), null, "bar", "baz", IoUtil.stringAsInputStream("foo"));

    // when
    taskService.deleteTaskAttachment(task.getId(), attachment.getId());

    // then
    assertThat(RecorderTaskListener.getTotalEventCount()).isEqualTo(2);
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(2); // create and delete attachment
  }

  @Test
  void testUpdateTaskListenerOnSetLocalVariable() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.setVariableLocal(task.getId(), "foo", "bar");

    // then
    assertThat(RecorderTaskListener.getTotalEventCount()).isEqualTo(1);
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(1);
  }

  @Test
  void testUpdateTaskListenerOnSetLocalVariables() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    VariableMap variables = Variables.createVariables()
        .putValue("var1", "val1")
        .putValue("var2", "val2");

    // when
    taskService.setVariablesLocal(task.getId(), variables);

    // then
    // only a single invocation of the listener is triggered
    assertThat(RecorderTaskListener.getTotalEventCount()).isEqualTo(1);
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(1);
  }

  @Test
  void testUpdateTaskListenerOnSetVariableInTaskScope() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();
    taskService.setVariableLocal(task.getId(), "foo", "bar");

    // when
    taskService.setVariable(task.getId(), "foo", "bar");

    // then
    assertThat(RecorderTaskListener.getTotalEventCount()).isEqualTo(2);
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isEqualTo(2); // local and non-local
  }

  @Test
  void testUpdateTaskListenerOnSetVariableInHigherScope() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_UPDATE);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.setVariable(task.getId(), "foo", "bar");

    // then
    assertThat(RecorderTaskListener.getTotalEventCount()).isZero();
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_UPDATE)).isZero();
  }

  @Test
  void testUpdateTaskListenerInvokedBeforeConditionalEventsOnSetVariable() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask("task")
        .operatonTaskListenerClass(TaskListener.EVENTNAME_UPDATE, RecorderTaskListener.class)
      .boundaryEvent()
        .condition("${triggerBoundaryEvent}")
      .userTask("afterBoundaryEvent")
        .operatonTaskListenerClass(TaskListener.EVENTNAME_CREATE, RecorderTaskListener.class)
      .endEvent()
      .moveToActivity("task")
      .endEvent()
      .done();

    testRule.deploy(modelInstance);

    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();
    taskService.setVariableLocal(task.getId(), "taskLocalVariable", "bar");
    RecorderTaskListener.clear();

    VariableMap variables = Variables.createVariables().putValue("triggerBoundaryEvent", true).putValue("taskLocalVariable", "baz");

    // when
    taskService.setVariables(task.getId(), variables);

    // then
    assertThat(RecorderTaskListener.getOrderedEvents()).containsExactly(TaskListener.EVENTNAME_UPDATE, TaskListener.EVENTNAME_CREATE);
  }

  @Test
  void testAssignmentTaskListenerWhenSavingTask() {
    // given
    createAndDeployModelWithTaskEventsRecorderOnUserTask(TaskListener.EVENTNAME_ASSIGNMENT);
    runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    task.setAssignee("gonzo");
    taskService.saveTask(task);

    // then
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_ASSIGNMENT)).isEqualTo(1);
  }

  // TIMEOUT listener tests

  @Test
  @Deployment
  void testTimeoutTaskListenerDuration() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");

    // when
    ClockUtil.offset(TimeUnit.MINUTES.toMillis(70L));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    // then
    assertThat(runtimeService.getVariable(instance.getId(), "timeout-status")).isEqualTo("fired");
  }

  @Test
  @Deployment
  void testTimeoutTaskListenerDate() throws Exception {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");

    // when
    ClockUtil.setCurrentTime(new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2019-09-09T13:00:00"));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    // then
    assertThat(runtimeService.getVariable(instance.getId(), "timeout-status")).isEqualTo("fired");
  }

  @Test
  @Deployment
  void testTimeoutTaskListenerCycle() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");

    // when
    ClockUtil.offset(TimeUnit.MINUTES.toMillis(70L));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);
    ClockUtil.offset(TimeUnit.MINUTES.toMillis(130L));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    // then
    assertThat(runtimeService.getVariable(instance.getId(), "timeout-status")).isEqualTo("fired2");
  }

  @Test
  @Deployment
  void testMultipleTimeoutTaskListeners() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");

    // assume
    assertThat(managementService.createJobQuery().count()).isEqualTo(2L);

    // when
    ClockUtil.offset(TimeUnit.MINUTES.toMillis(70L));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    // then
    assertThat(managementService.createJobQuery().count()).isOne();
    assertThat(runtimeService.getVariable(instance.getId(), "timeout-status")).isEqualTo("fired");
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/tasklistener/TaskListenerTest.testTimeoutTaskListenerDuration.bpmn20.xml")
  void testTimeoutTaskListenerNotCalledWhenTaskCompleted() {
    // given
    JobQuery jobQuery = managementService.createJobQuery();
    TaskQuery taskQuery = taskService.createTaskQuery();
    runtimeService.startProcessInstanceByKey("process");

    // assume
    assertThat(jobQuery.count()).isOne();

    // when
    taskService.complete(taskQuery.singleResult().getId());

    // then
    HistoricVariableInstanceQuery variableQuery = historyService.createHistoricVariableInstanceQuery().variableName("timeout-status");
    assertThat(variableQuery.count()).isZero();
    assertThat(jobQuery.count()).isZero();
  }

  @Test
  @Deployment
  void testTimeoutTaskListenerNotCalledWhenTaskCompletedByBoundaryEvent() {
    // given
    JobQuery jobQuery = managementService.createJobQuery();
    runtimeService.startProcessInstanceByKey("process");

    // assume
    assertThat(jobQuery.count()).isEqualTo(2L);

    // when the boundary event is triggered
    ClockUtil.offset(TimeUnit.MINUTES.toMillis(70L));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    // then
    HistoricVariableInstanceQuery variableQuery = historyService.createHistoricVariableInstanceQuery().variableName("timeout-status");
    assertThat(variableQuery.count()).isZero();
    assertThat(jobQuery.count()).isZero();
  }

  @Test
  @Deployment
  void testRecalculateTimeoutTaskListenerDuedateCreationDateBased() {
    // given
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process", Variables.putValue("duration", "PT1H"));

    JobQuery jobQuery = managementService.createJobQuery().processInstanceId(pi.getId());
    List<Job> jobs = jobQuery.list();
    assertThat(jobs).hasSize(1);
    Job job = jobs.get(0);
    Date oldDate = job.getDuedate();

    // when
    runtimeService.setVariable(pi.getId(), "duration", "PT15M");
    managementService.recalculateJobDuedate(job.getId(), true);

    // then
    Job jobUpdated = jobQuery.singleResult();
    assertThat(jobUpdated.getId()).isEqualTo(job.getId());
    assertThat(jobUpdated.getDuedate()).isNotEqualTo(oldDate);
    assertThat(oldDate.after(jobUpdated.getDuedate())).isTrue();
    assertThat(jobUpdated.getDuedate()).isEqualTo(LocalDateTime.fromDateFields(jobUpdated.getCreateTime()).plusMinutes(15).toDate());
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/tasklistener/TaskListenerTest.testRecalculateTimeoutTaskListenerDuedateCreationDateBased.bpmn20.xml")
  void testRecalculateTimeoutTaskListenerDuedateCurrentDateBased() {
    // given
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process", Variables.putValue("duration", "PT1H"));

    JobQuery jobQuery = managementService.createJobQuery().processInstanceId(pi.getId());
    List<Job> jobs = jobQuery.list();
    assertThat(jobs).hasSize(1);
    Job job = jobs.get(0);
    Date oldDate = job.getDuedate();
    ClockUtil.offset(2000L);

    // when
    managementService.recalculateJobDuedate(job.getId(), false);

    // then
    Job jobUpdated = jobQuery.singleResult();
    assertThat(jobUpdated.getId()).isEqualTo(job.getId());
    assertThat(jobUpdated.getDuedate()).isNotEqualTo(oldDate);
    assertThat(oldDate.before(jobUpdated.getDuedate())).isTrue();
  }

  @Test
  @Deployment
  void testRecalculateTimeoutTaskListenerDuedateCreationDateBasedWithDefinedBoundaryEvent() {
    // given
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process", Variables.putValue("duration", "PT1H"));

    JobQuery jobQuery = managementService.createJobQuery()
        .processInstanceId(pi.getId())
        .activityId("userTask");
    List<Job> jobs = jobQuery.list();
    assertThat(jobs).hasSize(1);
    Job job = jobs.get(0);
    Date oldDate = job.getDuedate();

    // when
    runtimeService.setVariable(pi.getId(), "duration", "PT15M");
    managementService.recalculateJobDuedate(job.getId(), true);

    // then
    Job jobUpdated = jobQuery.singleResult();
    assertThat(jobUpdated.getId()).isEqualTo(job.getId());
    assertThat(jobUpdated.getDuedate()).isNotEqualTo(oldDate);
    assertThat(oldDate.after(jobUpdated.getDuedate())).isTrue();
    assertThat(jobUpdated.getDuedate()).isEqualTo(LocalDateTime.fromDateFields(jobUpdated.getCreateTime()).plusMinutes(15).toDate());
  }

  // Helper methods


  public static class VariablesCollectingListener implements TaskListener {

    protected static VariableMap collectedVariables;

    @Override
    public void notify(DelegateTask delegateTask) {
      collectedVariables = delegateTask.getVariablesTyped();
    }

    public static VariableMap getCollectedVariables() {
      return collectedVariables;
    }

    public static void reset() {
      collectedVariables = null;
    }

  }
}
