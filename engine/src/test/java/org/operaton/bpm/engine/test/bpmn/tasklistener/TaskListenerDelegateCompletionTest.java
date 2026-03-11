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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Askar Akhmerov
 */
class TaskListenerDelegateCompletionTest {

  protected static final String COMPLETE_LISTENER = "org.operaton.bpm.engine.test.bpmn.tasklistener.util.CompletingTaskListener";
  protected static final String TASK_LISTENER_PROCESS = "taskListenerProcess";
  protected static final String ACTIVITY_ID = "UT";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;

  @AfterEach
  void cleanUp() {
    if (runtimeService.createProcessInstanceQuery().count() > 0) {
      runtimeService.deleteProcessInstance(runtimeService.createProcessInstanceQuery().singleResult().getId(),null,true);
    }
  }


  protected static BpmnModelInstance setupProcess(String eventName) {
    return Bpmn.createExecutableProcess(TASK_LISTENER_PROCESS)
        .startEvent()
          .userTask(ACTIVITY_ID)
          .operatonTaskListenerClass(eventName,COMPLETE_LISTENER)
        .endEvent()
        .done();
  }

  @Test
  void testCompletionIsPossibleOnCreation() {
    //given
    createProcessWithListener(TaskListener.EVENTNAME_CREATE);

    //when
    runtimeService.startProcessInstanceByKey(TASK_LISTENER_PROCESS);

    //then
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNull();
  }

  @Test
  void testCompletionIsPossibleOnAssignment() {
    //given
    createProcessWithListener(TaskListener.EVENTNAME_ASSIGNMENT);

    //when
    runtimeService.startProcessInstanceByKey(TASK_LISTENER_PROCESS);
    Task task = taskService.createTaskQuery().singleResult();
    taskService.setAssignee(task.getId(),"test assignee");

    //then
    task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNull();
  }

  @Test
  void testCompletionIsPossibleAfterAssignmentUpdate() {
    //given
    createProcessWithListener(TaskListener.EVENTNAME_UPDATE);

    //when
    runtimeService.startProcessInstanceByKey(TASK_LISTENER_PROCESS);
    Task task = taskService.createTaskQuery().singleResult();
    taskService.setAssignee(task.getId(),"test assignee");

    //then
    task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNull();
  }

  @Test
  void testCompletionIsPossibleAfterPropertyUpdate() {
    //given
    createProcessWithListener(TaskListener.EVENTNAME_UPDATE);

    //when
    runtimeService.startProcessInstanceByKey(TASK_LISTENER_PROCESS);
    Task task = taskService.createTaskQuery().singleResult();
    taskService.setOwner(task.getId(),"ownerId");

    //then
    task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNull();
  }

  @Test
  @Deployment
  void testCompletionIsPossibleOnTimeout() {
    TaskQuery taskQuery = taskService.createTaskQuery();

    // given
    runtimeService.startProcessInstanceByKey("process");

    // assume
    assertThat(taskQuery.count()).isOne();

    // when
    ClockUtil.offset(TimeUnit.MINUTES.toMillis(70L));
    testHelper.waitForJobExecutorToProcessAllJobs(5000L);

    // then
    assertThat(taskQuery.count()).isZero();
  }

  @Test
  void testCompletionIsNotPossibleOnComplete() {
    //given
    createProcessWithListener(TaskListener.EVENTNAME_COMPLETE);

    runtimeService.startProcessInstanceByKey(TASK_LISTENER_PROCESS);
    Task task = taskService.createTaskQuery().singleResult();
    String taskId = task.getId();

    // when/then
    assertThatThrownBy(() -> taskService.complete(taskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("invalid task state");
  }

  @Test
  void testCompletionIsNotPossibleOnDelete() {
    //given
    createProcessWithListener(TaskListener.EVENTNAME_DELETE);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(TASK_LISTENER_PROCESS);
    String processInstanceId = processInstance.getId();

    // when/then
    assertThatThrownBy(() -> runtimeService.deleteProcessInstance(processInstanceId,"test reason"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("invalid task state");
  }

  protected void createProcessWithListener(String eventName) {
    BpmnModelInstance bpmnModelInstance = setupProcess(eventName);
    testHelper.deploy(bpmnModelInstance);
  }

}
