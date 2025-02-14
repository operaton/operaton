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
package org.operaton.bpm.engine.test.bpmn.event.message;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.operaton.bpm.engine.impl.EventSubscriptionQueryImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.ExecutionTree;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.engine.test.util.TestExecutionListener;
import org.junit.After;
import org.junit.Test;


/**
 * @author Daniel Meyer
 * @author Falko Menge
 * @author Danny Gräf
 */
public class MessageEventSubprocessTest extends PluggableProcessEngineTest {

  @After
  public void tearDown() {
    TestExecutionListener.reset();
  }

  @Deployment
  @Test
  public void testInterruptingUnderProcessDefinition() {
    testInterruptingUnderProcessDefinition(1);
  }

  /**
   * Checks if unused event subscriptions are properly deleted.
   */
  @Deployment
  @Test
  public void testTwoInterruptingUnderProcessDefinition() {
    testInterruptingUnderProcessDefinition(2);
  }

  private void testInterruptingUnderProcessDefinition(int expectedNumberOfEventSubscriptions) {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // the process instance must have a message event subscription:
    Execution execution = runtimeService.createExecutionQuery()
        .executionId(processInstance.getId())
        .messageEventSubscriptionName("newMessage")
        .singleResult();
    assertThat(execution).isNotNull();
    assertThat(createEventSubscriptionQuery().count()).isEqualTo(expectedNumberOfEventSubscriptions);
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(1);

    // if we trigger the usertask, the process terminates and the event subscription is removed:
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(createEventSubscriptionQuery().count()).isEqualTo(0);
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(0);

    // now we start a new instance but this time we trigger the event subprocess:
    processInstance = runtimeService.startProcessInstanceByKey("process");
    runtimeService.messageEventReceived("newMessage", processInstance.getId());

    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("eventSubProcessTask");
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(createEventSubscriptionQuery().count()).isEqualTo(0);
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testEventSubprocessListenersInvoked() {
    runtimeService.startProcessInstanceByKey("testProcess");

    runtimeService.correlateMessage("message");

    Task taskInEventSubProcess = taskService.createTaskQuery().singleResult();
    assertThat(taskInEventSubProcess.getTaskDefinitionKey()).isEqualTo("taskInEventSubProcess");

    taskService.complete(taskInEventSubProcess.getId());

    List<String> collectedEvents = TestExecutionListener.collectedEvents;

    assertThat(collectedEvents.get(0)).isEqualTo("taskInMainFlow-start");
    assertThat(collectedEvents.get(1)).isEqualTo("taskInMainFlow-end");
    assertThat(collectedEvents.get(2)).isEqualTo("eventSubProcess-start");
    assertThat(collectedEvents.get(3)).isEqualTo("startEventInSubProcess-start");
    assertThat(collectedEvents.get(4)).isEqualTo("startEventInSubProcess-end");
    assertThat(collectedEvents.get(5)).isEqualTo("taskInEventSubProcess-start");
    assertThat(collectedEvents.get(6)).isEqualTo("taskInEventSubProcess-end");
    assertThat(collectedEvents.get(7)).isEqualTo("eventSubProcess-end");

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("taskInMainFlow").canceled().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("startEventInSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("taskInEventSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("endEventInSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("eventSubProcess").finished().count()).isEqualTo(1);
    }

  }

  @Deployment
  @Test
  public void testNonInterruptingEventSubprocessListenersInvoked() {
    runtimeService.startProcessInstanceByKey("testProcess");

    runtimeService.correlateMessage("message");

    Task taskInMainFlow = taskService.createTaskQuery().taskDefinitionKey("taskInMainFlow").singleResult();
    assertThat(taskInMainFlow).isNotNull();

    Task taskInEventSubProcess = taskService.createTaskQuery().taskDefinitionKey("taskInEventSubProcess").singleResult();
    assertThat(taskInEventSubProcess).isNotNull();

    taskService.complete(taskInMainFlow.getId());
    taskService.complete(taskInEventSubProcess.getId());

    List<String> collectedEvents = TestExecutionListener.collectedEvents;

    assertThat(collectedEvents.get(0)).isEqualTo("taskInMainFlow-start");
    assertThat(collectedEvents.get(1)).isEqualTo("eventSubProcess-start");
    assertThat(collectedEvents.get(2)).isEqualTo("startEventInSubProcess-start");
    assertThat(collectedEvents.get(3)).isEqualTo("startEventInSubProcess-end");
    assertThat(collectedEvents.get(4)).isEqualTo("taskInEventSubProcess-start");
    assertThat(collectedEvents.get(5)).isEqualTo("taskInMainFlow-end");
    assertThat(collectedEvents.get(6)).isEqualTo("taskInEventSubProcess-end");
    assertThat(collectedEvents.get(7)).isEqualTo("eventSubProcess-end");

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("startEventInSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("taskInMainFlow").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("taskInEventSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("endEventInSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("eventSubProcess").finished().count()).isEqualTo(1);
    }
  }

  @Deployment
  @Test
  public void testNestedEventSubprocessListenersInvoked() {
    runtimeService.startProcessInstanceByKey("testProcess");

    runtimeService.correlateMessage("message");

    Task taskInEventSubProcess = taskService.createTaskQuery().singleResult();
    assertThat(taskInEventSubProcess.getTaskDefinitionKey()).isEqualTo("taskInEventSubProcess");

    taskService.complete(taskInEventSubProcess.getId());

    List<String> collectedEvents = TestExecutionListener.collectedEvents;

    assertThat(collectedEvents.get(0)).isEqualTo("taskInMainFlow-start");
    assertThat(collectedEvents.get(1)).isEqualTo("taskInMainFlow-end");
    assertThat(collectedEvents.get(2)).isEqualTo("eventSubProcess-start");
    assertThat(collectedEvents.get(3)).isEqualTo("startEventInSubProcess-start");
    assertThat(collectedEvents.get(4)).isEqualTo("startEventInSubProcess-end");
    assertThat(collectedEvents.get(5)).isEqualTo("taskInEventSubProcess-start");
    assertThat(collectedEvents.get(6)).isEqualTo("taskInEventSubProcess-end");
    assertThat(collectedEvents.get(7)).isEqualTo("eventSubProcess-end");

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("taskInMainFlow").canceled().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("startEventInSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("taskInEventSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("endEventInSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("eventSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("subProcess").finished().count()).isEqualTo(1);
    }

  }

  @Deployment
  @Test
  public void testNestedNonInterruptingEventSubprocessListenersInvoked() {
    runtimeService.startProcessInstanceByKey("testProcess");

    runtimeService.correlateMessage("message");

    Task taskInMainFlow = taskService.createTaskQuery().taskDefinitionKey("taskInMainFlow").singleResult();
    assertThat(taskInMainFlow).isNotNull();

    Task taskInEventSubProcess = taskService.createTaskQuery().taskDefinitionKey("taskInEventSubProcess").singleResult();
    assertThat(taskInEventSubProcess).isNotNull();

    taskService.complete(taskInMainFlow.getId());
    taskService.complete(taskInEventSubProcess.getId());

    List<String> collectedEvents = TestExecutionListener.collectedEvents;

    assertThat(collectedEvents.get(0)).isEqualTo("taskInMainFlow-start");
    assertThat(collectedEvents.get(1)).isEqualTo("eventSubProcess-start");
    assertThat(collectedEvents.get(2)).isEqualTo("startEventInSubProcess-start");
    assertThat(collectedEvents.get(3)).isEqualTo("startEventInSubProcess-end");
    assertThat(collectedEvents.get(4)).isEqualTo("taskInEventSubProcess-start");
    assertThat(collectedEvents.get(5)).isEqualTo("taskInMainFlow-end");
    assertThat(collectedEvents.get(6)).isEqualTo("taskInEventSubProcess-end");
    assertThat(collectedEvents.get(7)).isEqualTo("eventSubProcess-end");

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("taskInMainFlow").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("startEventInSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("taskInEventSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("endEventInSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("eventSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("subProcess").finished().count()).isEqualTo(1);
    }

  }

  @Deployment
  @Test
  public void testEventSubprocessBoundaryListenersInvoked() {
    runtimeService.startProcessInstanceByKey("testProcess");

    runtimeService.correlateMessage("message");

    Task taskInEventSubProcess = taskService.createTaskQuery().singleResult();
    assertThat(taskInEventSubProcess.getTaskDefinitionKey()).isEqualTo("taskInEventSubProcess");

    runtimeService.correlateMessage("message2");

    List<String> collectedEvents = TestExecutionListener.collectedEvents;


    assertThat(collectedEvents.get(0)).isEqualTo("taskInMainFlow-start");
    assertThat(collectedEvents.get(1)).isEqualTo("taskInMainFlow-end");
    assertThat(collectedEvents.get(2)).isEqualTo("eventSubProcess-start");
    assertThat(collectedEvents.get(3)).isEqualTo("startEventInSubProcess-start");
    assertThat(collectedEvents.get(4)).isEqualTo("startEventInSubProcess-end");
    assertThat(collectedEvents.get(5)).isEqualTo("taskInEventSubProcess-start");
    assertThat(collectedEvents.get(6)).isEqualTo("taskInEventSubProcess-end");
    assertThat(collectedEvents.get(7)).isEqualTo("eventSubProcess-end");

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("taskInMainFlow").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("taskInMainFlow").canceled().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("startEventInSubProcess").finished().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("taskInEventSubProcess").canceled().count()).isEqualTo(1);
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("eventSubProcess").finished().count()).isEqualTo(1);
    }

  }

  @Deployment
  @Test
  public void testNonInterruptingUnderProcessDefinition() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // the process instance must have a message event subscription:
    Execution execution = runtimeService.createExecutionQuery()
        .executionId(processInstance.getId())
        .messageEventSubscriptionName("newMessage")
        .singleResult();
    assertThat(execution).isNotNull();
    assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(1);

    // if we trigger the usertask, the process terminates and the event subscription is removed:
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(createEventSubscriptionQuery().count()).isEqualTo(0);
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(0);

    // ###################### now we start a new instance but this time we trigger the event subprocess:
    processInstance = runtimeService.startProcessInstanceByKey("process");
    runtimeService.messageEventReceived("newMessage", processInstance.getId());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // now let's first complete the task in the main flow:
    task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    taskService.complete(task.getId());
    // we still have 2 executions (one for process instance, one for event subprocess):
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(2);

    // now let's complete the task in the event subprocess
    task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
    taskService.complete(task.getId());
    // done!
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(0);

    // #################### again, the other way around:

    processInstance = runtimeService.startProcessInstanceByKey("process");
    runtimeService.messageEventReceived("newMessage", processInstance.getId());

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
    taskService.complete(task.getId());
    // we still have 1 execution:
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(1);

    task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    taskService.complete(task.getId());
    // done!
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testNonInterruptingUnderProcessDefinitionScope() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // the process instance must have a message event subscription:
    Execution execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("newMessage")
        .singleResult();
    assertThat(execution).isNotNull();
    assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(2);

    // if we trigger the usertask, the process terminates and the event subscription is removed:
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(createEventSubscriptionQuery().count()).isEqualTo(0);
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(0);

    // ###################### now we start a new instance but this time we trigger the event subprocess:
    processInstance = runtimeService.startProcessInstanceByKey("process");
    runtimeService.correlateMessage("newMessage");

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);

    // now let's first complete the task in the main flow:
    task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    taskService.complete(task.getId());
    // we still have 2 executions (one for process instance, one for subprocess scope):
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(2);

    // now let's complete the task in the event subprocess
    task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
    taskService.complete(task.getId());
    // done!
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(0);

    // #################### again, the other way around:

    processInstance = runtimeService.startProcessInstanceByKey("process");
    runtimeService.correlateMessage("newMessage");

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
    taskService.complete(task.getId());
    // we still have 2 executions (usertask in main flow is scope):
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(2);

    task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    taskService.complete(task.getId());
    // done!
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testNonInterruptingInEmbeddedSubprocess() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // the process instance must have a message event subscription:
    Execution execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("newMessage")
        .singleResult();
    assertThat(execution).isNotNull();
    assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);

    // if we trigger the usertask, the process terminates and the event subscription is removed:
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(createEventSubscriptionQuery().count()).isEqualTo(0);
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(0);

    // ###################### now we start a new instance but this time we trigger the event subprocess:
    processInstance = runtimeService.startProcessInstanceByKey("process");
    runtimeService.correlateMessage("newMessage");

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // now let's first complete the task in the main flow:
    task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    taskService.complete(task.getId());
    // we still have 3 executions:
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(3);

    // now let's complete the task in the event subprocess
    task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
    taskService.complete(task.getId());
    // done!
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(0);

    // #################### again, the other way around:

    processInstance = runtimeService.startProcessInstanceByKey("process");
    runtimeService.correlateMessage("newMessage");

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
    taskService.complete(task.getId());
    // we still have 2 executions:
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(2);

    task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    taskService.complete(task.getId());
    // done!
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testMultipleNonInterruptingInEmbeddedSubprocess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // the process instance must have a message event subscription:
    Execution subProcess = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("newMessage")
        .singleResult();
    assertThat(subProcess).isNotNull();
    assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);

    Task subProcessTask = taskService.createTaskQuery().taskDefinitionKey("subProcessTask").singleResult();
    assertThat(subProcessTask).isNotNull();

    // start event sub process multiple times
    for (int i = 1; i < 3; i++) {
      runtimeService.messageEventReceived("newMessage", subProcess.getId());

      // check that now i event sub process tasks exist
      List<Task> eventSubProcessTasks = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list();
      assertThat(eventSubProcessTasks).hasSize(i);
    }

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);

    // check that the parent execution of the event sub process task execution is the event
    // sub process execution
    assertThat(executionTree)
        .matches(
            describeExecutionTree(null).scope()
                .child(null).scope()
                  .child("subProcessTask").concurrent().noScope().up()
                  .child(null).concurrent().noScope()
                    .child("eventSubProcessTask").scope().up().up()
                  .child(null).concurrent().noScope()
                    .child("eventSubProcessTask").scope()
                .done());

    // complete sub process task
    taskService.complete(subProcessTask.getId());

    // after complete the sub process task all task should be deleted because of the terminating end event
    assertThat(taskService.createTaskQuery().count()).isEqualTo(0);

    // and the process instance should be ended
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
    return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutorTxRequired());
  }

  @Deployment
  @Test
  public void testNonInterruptingInMultiParallelEmbeddedSubprocess() {
    // #################### I. start process and only complete the tasks
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // assert execution tree: scope (process) > scope (subprocess) > 2 x subprocess + usertask
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(6);

    // expect: two subscriptions, one for each instance
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(2);

    // expect: two subprocess instances, i.e. two tasks created
    List<Task> tasks = taskService.createTaskQuery().list();
    // then: complete both tasks
    for (Task task : tasks) {
      assertThat(task.getTaskDefinitionKey()).isEqualTo("subUserTask");
      taskService.complete(task.getId());
    }

    // expect: the event subscriptions are removed
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(0);

    // then: complete the last task of the main process
    taskService.complete(taskService.createTaskQuery().singleResult().getId());
    testRule.assertProcessEnded(processInstance.getId());

    // #################### II. start process and correlate messages to trigger subprocesses instantiation
    processInstance = runtimeService.startProcessInstanceByKey("process");
    for (EventSubscription es : runtimeService.createEventSubscriptionQuery().list()) {
      runtimeService.messageEventReceived("message", es.getExecutionId()); // trigger
    }

    // expect: both subscriptions are remaining and they can be re-triggered as long as the subprocesses are active
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(2);

    // expect: two additional task, one for each triggered process
    tasks = taskService.createTaskQuery().taskName("Message User Task").list();
    assertThat(tasks).hasSize(2);
    for (Task task : tasks) { // complete both tasks
      taskService.complete(task.getId());
    }

    // then: complete one subprocess
    taskService.complete(taskService.createTaskQuery().taskName("Sub User Task").list().get(0).getId());

    // expect: only the subscription of the second subprocess instance is left
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(1);

    // then: trigger the second subprocess again
    runtimeService.messageEventReceived("message",
        runtimeService.createEventSubscriptionQuery().singleResult().getExecutionId());

    // expect: one message subprocess task exist
    assertThat(taskService.createTaskQuery().taskName("Message User Task").list()).hasSize(1);

    // then: complete all inner subprocess tasks
    tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    // expect: no subscription is left
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(0);

    // then: complete the last task of the main process
    taskService.complete(taskService.createTaskQuery().singleResult().getId());
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  public void testNonInterruptingInMultiSequentialEmbeddedSubprocess() {
    // start process and trigger the first message sub process
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    runtimeService.messageEventReceived("message", runtimeService.createEventSubscriptionQuery().singleResult().getExecutionId());

    // expect: one subscription is remaining for the first instance
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(1);

    // then: complete both tasks (subprocess and message subprocess)
    taskService.complete(taskService.createTaskQuery().taskName("Message User Task").singleResult().getId());
    taskService.complete(taskService.createTaskQuery().taskName("Sub User Task").list().get(0).getId());

    // expect: the second instance is started
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(1);

    // then: just complete this
    taskService.complete(taskService.createTaskQuery().taskName("Sub User Task").list().get(0).getId());

    // expect: no subscription is left
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(0);

    // then: complete the last task of the main process
    taskService.complete(taskService.createTaskQuery().singleResult().getId());
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  public void testNonInterruptingWithParallelForkInsideEmbeddedSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    runtimeService.messageEventReceived("newMessage", runtimeService.createEventSubscriptionQuery().singleResult().getExecutionId());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);

    assertThat(executionTree)
        .matches(
            describeExecutionTree(null).scope()
                .child(null).scope()
                .child("firstUserTask").concurrent().noScope().up()
                .child("secondUserTask").concurrent().noScope().up()
                .child(null).concurrent().noScope()
                    .child("eventSubProcessTask")
                .done());

    List<Task> tasks = taskService.createTaskQuery().list();

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment
  @Test
  public void testNonInterruptingWithReceiveTask() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("eventSubProcessTask")
        .singleResult();
    assertThat(task1).isNotNull();

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    // check that the parent execution of the event sub process task execution is the event
    // sub process execution
    assertThat(executionTree)
        .matches(
          describeExecutionTree(null).scope()
            .child(null).concurrent().noScope()
              .child("receiveTask").scope().up().up()
            .child(null).concurrent().noScope()
              .child("eventSubProcessTask").scope()
            .done());

    // when (2)
    runtimeService.correlateMessage("secondMessage");

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("eventSubProcessTask")
        .singleResult();
    assertThat(task1).isNotNull();

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("userTask")
        .singleResult();
    assertThat(task2).isNotNull();

    executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    // check that the parent execution of the event sub process task execution is the event
    // sub process execution
    assertThat(executionTree)
        .matches(
          describeExecutionTree(null).scope()
            .child("userTask").concurrent().noScope().up()
            .child(null).concurrent().noScope()
              .child("eventSubProcessTask").scope()
            .done());

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(1);

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

  /**
   * CAM-3655
   */
  @Deployment
  @Test
  public void testNonInterruptingWithAsyncConcurrentTask() {
    // given a process instance with an asyncBefore user task
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // and a triggered non-interrupting subprocess with a user task
    runtimeService.correlateMessage("message");

    // then triggering the async job should be successful
    Job asyncJob = managementService.createJobQuery().singleResult();
    assertThat(asyncJob).isNotNull();
    managementService.executeJob(asyncJob.getId());

    // and there should be two tasks now that can be completed successfully
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    Task processTask = taskService.createTaskQuery().taskDefinitionKey("userTask").singleResult();
    Task eventSubprocessTask = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();

    assertThat(processTask).isNotNull();
    assertThat(eventSubprocessTask).isNotNull();

    taskService.complete(processTask.getId());
    taskService.complete(eventSubprocessTask.getId());


    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  public void testNonInterruptingWithReceiveTaskInsideEmbeddedSubProcess() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("eventSubProcessTask")
        .singleResult();
    assertThat(task1).isNotNull();

    Execution task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("eventSubProcessTask")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isNotEqualTo(processInstanceId);

    // when (2)
    runtimeService.correlateMessage("secondMessage");

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("eventSubProcessTask")
        .singleResult();
    assertThat(task1).isNotNull();

    task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("eventSubProcessTask")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isNotEqualTo(processInstanceId);

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("userTask")
        .singleResult();
    assertThat(task2).isNotNull();

    Execution task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("eventSubProcessTask")
        .singleResult();

    assertThat(((ExecutionEntity) task2Execution).getParentId()).isNotEqualTo(processInstanceId);

    // both have the same parent (but it is not the process instance)
    assertThat(((ExecutionEntity) task2Execution).getParentId()).isEqualTo(((ExecutionEntity) task1Execution).getParentId());

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(1);

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  public void testNonInterruptingWithUserTaskAndBoundaryEventInsideEmbeddedSubProcess() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when
    runtimeService.correlateMessage("newMessage");

    // then
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("eventSubProcessTask")
        .singleResult();
    assertThat(task1).isNotNull();

    Execution task1Execution = runtimeService
        .createExecutionQuery()
        .activityId("eventSubProcessTask")
        .singleResult();

    assertThat(((ExecutionEntity) task1Execution).getParentId()).isNotEqualTo(processInstanceId);

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("task")
        .singleResult();
    assertThat(task2).isNotNull();

    Execution task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("eventSubProcessTask")
        .singleResult();

    assertThat(((ExecutionEntity) task2Execution).getParentId()).isNotEqualTo(processInstanceId);

    // both have the same parent (but it is not the process instance)
    assertThat(((ExecutionEntity) task2Execution).getParentId()).isEqualTo(((ExecutionEntity) task1Execution).getParentId());

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(1);

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  public void testNonInterruptingOutsideEmbeddedSubProcessWithReceiveTaskInsideEmbeddedSubProcess() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)
    runtimeService.correlateMessage("firstMessage");

    // then (1)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

    Task task1 = taskService.createTaskQuery()
        .taskDefinitionKey("eventSubProcessTask")
        .singleResult();
    assertThat(task1).isNotNull();

    // when (2)
    runtimeService.correlateMessage("secondMessage");

    // then (2)
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    task1 = taskService.createTaskQuery()
        .taskDefinitionKey("eventSubProcessTask")
        .singleResult();
    assertThat(task1).isNotNull();

    Task task2 = taskService.createTaskQuery()
        .taskDefinitionKey("userTask")
        .singleResult();
    assertThat(task2).isNotNull();

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(1);

    taskService.complete(task1.getId());
    taskService.complete(task2.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  public void testInterruptingActivityInstanceTree() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = instance.getId();

    // when
    runtimeService.correlateMessage("newMessage");

    // then
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(instance.getProcessDefinitionId())
            .beginScope("subProcess")
              .beginScope("eventSubProcess")
                .activity("eventSubProcessTask")
              .endScope()
            .endScope()
            .done());
  }

  @Deployment
  @Test
  public void testNonInterruptingActivityInstanceTree() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = instance.getId();

    // when
    runtimeService.correlateMessage("newMessage");

    // then
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(instance.getProcessDefinitionId())
            .beginScope("subProcess")
              .activity("innerTask")
              .beginScope("eventSubProcess")
                  .activity("eventSubProcessTask")
              .endScope()
            .endScope()
            .done());
  }

  @Deployment
  @Test
  public void testNonInterruptingWithTerminatingEndEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName(), is("Inner User Task"));
    runtimeService.correlateMessage("message");

    Task eventSubprocessTask = taskService.createTaskQuery().taskName("Event User Task").singleResult();
    assertThat(eventSubprocessTask, is(notNullValue()));
    taskService.complete(eventSubprocessTask.getId());

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("SubProcess_1")
            .activity("UserTask_1")
          .endScope()
        .endScope()
        .done()
    );
  }

  @Deployment
  @Test
  public void testExpressionInMessageNameInInterruptingSubProcessDefinition() {
    // given an process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // when receiving the message
    runtimeService.messageEventReceived("newMessage-foo", processInstance.getId());

    // the the subprocess is triggered and we can complete the task
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("eventSubProcessTask");
    taskService.complete(task.getId());
    testRule.assertProcessEnded(processInstance.getId());
  }

}
