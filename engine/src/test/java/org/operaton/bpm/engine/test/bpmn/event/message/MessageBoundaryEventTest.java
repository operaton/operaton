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
package org.operaton.bpm.engine.test.bpmn.event.message;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.Problem;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Daniel Meyer (operaton)
 * @author Kristin Polenz (operaton)
 * @author Christian Lipphardt (Camunda)
 */
class MessageBoundaryEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  TaskService taskService;
  RepositoryService repositoryService;
  HistoryService historyService;

  @Deployment
  @Test
  void testSingleBoundaryMessageEvent() {
    runtimeService.startProcessInstanceByKey("process");

    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(2);

    Task userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();

    Execution execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName")
        .singleResult();
    assertThat(execution).isNotNull();

    // 1. case: message received cancels the task

    runtimeService.messageEventReceived("messageName", execution.getId());

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterMessage");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    // 2nd. case: complete the user task cancels the message subscription

    runtimeService.startProcessInstanceByKey("process");

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    taskService.complete(userTask.getId());

    execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName")
        .singleResult();
    assertThat(execution).isNull();

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterTask");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

  }

  @Test
  void testDoubleBoundaryMessageEventSameMessageId() {
    // given
    var deploymentBuilder = repositoryService
          .createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/message/MessageBoundaryEventTest.testDoubleBoundaryMessageEventSameMessageId.bpmn20.xml");

    // when/then
    // deployment fails when two boundary message events have the same messageId
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ParseException.class)
      .hasMessageContaining("Cannot have more than one message event subscription with name 'messageName' for scope 'task'")
      .satisfies(e -> {
        ParseException pe = (ParseException) e;
        assertThat(repositoryService.createDeploymentQuery().count()).isZero();
        List<Problem> errors = pe.getResourceReports().get(0).getErrors();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getMainElementId()).isEqualTo("messageBoundary_2");
      });
  }

  @Deployment
  @Test
  void testDoubleBoundaryMessageEvent() {
    runtimeService.startProcessInstanceByKey("process");

    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(2);

    Task userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();

    // the executions for both messageEventSubscriptionNames are the same
    Execution execution1 = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName_1")
        .singleResult();
    assertThat(execution1).isNotNull();

    Execution execution2 = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName_2")
        .singleResult();
    assertThat(execution2).isNotNull();
    var execution2Id = execution2.getId();

    assertThat(execution2.getId()).isEqualTo(execution1.getId());

    // /////////////////////////////////////////////////////////////////////////////////
    // 1. first message received cancels the task and the execution and both subscriptions
    runtimeService.messageEventReceived("messageName_1", execution1.getId());

    // when/then
    // this should then throw an exception because execution2 no longer exists
    assertThatThrownBy(() -> runtimeService.messageEventReceived("messageName_2", execution2Id))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("does not have a subscription to a message event with name 'messageName_2'");

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterMessage_1");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    // ///////////////////////////////////////////////////////////////////
    // 2. complete the user task cancels the message subscriptions

    runtimeService.startProcessInstanceByKey("process");

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    taskService.complete(userTask.getId());

    execution1 = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName_1")
        .singleResult();
    assertThat(execution1).isNull();
    execution2 = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName_2")
        .singleResult();
    assertThat(execution2).isNull();

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterTask");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment
  @Test
  void testDoubleBoundaryMessageEventMultiInstance() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    // assume we have 7 executions
    // one process instance
    // one execution for scope created for boundary message event
    // five execution because we have loop cardinality 5
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(7);

    assertThat(taskService.createTaskQuery().count()).isEqualTo(5);

    Execution execution1 = runtimeService.createExecutionQuery().messageEventSubscriptionName("messageName_1").singleResult();
    Execution execution2 = runtimeService.createExecutionQuery().messageEventSubscriptionName("messageName_2").singleResult();
    // both executions are the same
    assertThat(execution2.getId()).isEqualTo(execution1.getId());
    var execution2Id = execution2.getId();

    // /////////////////////////////////////////////////////////////////////////////////
    // 1. first message received cancels all tasks and the executions and both subscriptions
    runtimeService.messageEventReceived("messageName_1", execution1.getId());

    // when/then
    // this should then throw an exception because execution2 no longer exists
    assertThatThrownBy(() -> runtimeService.messageEventReceived("messageName_2", execution2Id))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("does not have a subscription to a message event with name 'messageName_2'");

    // only process instance left
    assertThat(runtimeService.createExecutionQuery().count()).isOne();

    Task userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterMessage_1");
    taskService.complete(userTask.getId());
    testRule.assertProcessEnded(processInstance.getId());


    // /////////////////////////////////////////////////////////////////////////////////
    // 2. complete the user task cancels the message subscriptions

    processInstance = runtimeService.startProcessInstanceByKey("process");
    // assume we have 7 executions
    // one process instance
    // one execution for scope created for boundary message event
    // five execution because we have loop cardinality 5
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(7);

    assertThat(taskService.createTaskQuery().count()).isEqualTo(5);

    execution1 = runtimeService.createExecutionQuery().messageEventSubscriptionName("messageName_1").singleResult();
    execution2 = runtimeService.createExecutionQuery().messageEventSubscriptionName("messageName_2").singleResult();
    // both executions are the same
    assertThat(execution2.getId()).isEqualTo(execution1.getId());

    List<Task> userTasks = taskService.createTaskQuery().list();
    assertThat(userTasks)
            .isNotNull()
            .hasSize(5);

    // as long as tasks exists, the message subscriptions exist
    for (int i = 0; i < userTasks.size() - 1; i++) {
      Task task = userTasks.get(i);
      taskService.complete(task.getId());

      execution1 = runtimeService.createExecutionQuery()
          .messageEventSubscriptionName("messageName_1")
          .singleResult();
      assertThat(execution1).isNotNull();
      execution2 = runtimeService.createExecutionQuery()
          .messageEventSubscriptionName("messageName_2")
          .singleResult();
      assertThat(execution2).isNotNull();
    }

    // only one task left
    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    taskService.complete(userTask.getId());

    // after last task is completed, no message subscriptions left
    execution1 = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName_1")
        .singleResult();
    assertThat(execution1).isNull();
    execution2 = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName_2")
        .singleResult();
    assertThat(execution2).isNull();

    // complete last task to end process
    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterTask");
    taskService.complete(userTask.getId());
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  void testBoundaryMessageEventInsideSubprocess() {

    // this time the boundary events are placed on a user task that is contained inside a sub process

    runtimeService.startProcessInstanceByKey("process");

    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(3);

    Task userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();

    Execution execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName")
        .singleResult();
    assertThat(execution).isNotNull();

    // /////////////////////////////////////////////////
    // 1. case: message received cancels the task

    runtimeService.messageEventReceived("messageName", execution.getId());

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterMessage");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    // /////////////////////////////////////////////////
    // 2nd. case: complete the user task cancels the message subscription

    runtimeService.startProcessInstanceByKey("process");

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    taskService.complete(userTask.getId());

    execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName")
        .singleResult();
    assertThat(execution).isNull();

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterTask");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment
  @Test
  void testBoundaryMessageEventOnSubprocessAndInsideSubprocess() {

    // this time the boundary events are placed on a user task that is contained inside a sub process
    // and on the subprocess itself

    runtimeService.startProcessInstanceByKey("process");

    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(3);

    Task userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();

    Execution execution1 = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName")
        .singleResult();
    assertThat(execution1).isNotNull();

    Execution execution2 = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName2")
        .singleResult();
    assertThat(execution2).isNotNull();

    assertThat(execution2.getId()).isNotSameAs(execution1.getId());

    // ///////////////////////////////////////////////////////////
    // first case: we complete the inner usertask.

    taskService.complete(userTask.getId());

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterTask");

    // the inner subscription is cancelled
    Execution execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName")
        .singleResult();
    assertThat(execution).isNull();

    // the outer subscription still exists
    execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName2")
        .singleResult();
    assertThat(execution).isNotNull();

    // now complete the second usertask
    taskService.complete(userTask.getId());

    // now the outer event subscription is cancelled as well
    execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName2")
        .singleResult();
    assertThat(execution).isNull();

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterSubprocess");

    // now complete the outer usertask
    taskService.complete(userTask.getId());

    // ///////////////////////////////////////////////////////////
    // second case: we signal the inner message event

    runtimeService.startProcessInstanceByKey("process");

    execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName")
        .singleResult();
    runtimeService.messageEventReceived("messageName", execution.getId());

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterMessage");

    // the inner subscription is removed
    execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName")
        .singleResult();
    assertThat(execution).isNull();

    // the outer subscription still exists
    execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName2")
        .singleResult();
    assertThat(execution).isNotNull();

    // now complete the second usertask
    taskService.complete(userTask.getId());

    // now the outer event subscription is cancelled as well
    execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName2")
        .singleResult();
    assertThat(execution).isNull();

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterSubprocess");

    // now complete the outer usertask
    taskService.complete(userTask.getId());

    // ///////////////////////////////////////////////////////////
    // third case: we signal the outer message event

    runtimeService.startProcessInstanceByKey("process");

    execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName2")
        .singleResult();
    runtimeService.messageEventReceived("messageName2", execution.getId());

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterOuterMessageBoundary");

    // the inner subscription is removed
    execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName")
        .singleResult();
    assertThat(execution).isNull();

    // the outer subscription is removed
    execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName2")
        .singleResult();
    assertThat(execution).isNull();

    // now complete the second usertask
    taskService.complete(userTask.getId());

    // and we are done

  }


  @Deployment
  @Test
  void testBoundaryMessageEventOnSubprocess() {
    runtimeService.startProcessInstanceByKey("process");

    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(2);

    Task userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();

    // 1. case: message one received cancels the task

    Execution executionMessageOne = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName_one")
        .singleResult();
    assertThat(executionMessageOne).isNotNull();

    runtimeService.messageEventReceived("messageName_one", executionMessageOne.getId());

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterMessage_one");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    // 2nd. case: message two received cancels the task

    runtimeService.startProcessInstanceByKey("process");

    Execution executionMessageTwo = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName_two")
        .singleResult();
    assertThat(executionMessageTwo).isNotNull();

    runtimeService.messageEventReceived("messageName_two", executionMessageTwo.getId());

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterMessage_two");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();


    // 3rd. case: complete the user task cancels the message subscription

    runtimeService.startProcessInstanceByKey("process");

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    taskService.complete(userTask.getId());

    executionMessageOne = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName_one")
        .singleResult();
    assertThat(executionMessageOne).isNull();

    executionMessageTwo = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName_two")
        .singleResult();
    assertThat(executionMessageTwo).isNull();

    userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterSubProcess");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

  }

  @Deployment
  @Test
  void testBoundaryMessageEventOnSubprocessWithIntermediateMessageCatch() {

    // given
    // a process instance waiting inside the intermediate message catch inside the subprocess
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // when
    // I cancel the subprocess
    runtimeService.correlateMessage("cancelMessage");

    // then
    // the process instance is ended
    testRule.assertProcessEnded(processInstance.getId());

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      // and all activity instances in history have an end time set
      List<HistoricActivityInstance> hais = historyService.createHistoricActivityInstanceQuery().list();
      for (HistoricActivityInstance historicActivityInstance : hais) {
        assertThat(historicActivityInstance.getEndTime()).isNotNull();
      }
    }
  }

  @Deployment
  @Test
  void testBoundaryMessageEventOnSubprocessAndInsideSubprocessMultiInstance() {

    // this time the boundary events are placed on a user task that is contained inside a sub process
    // and on the subprocess itself

    runtimeService.startProcessInstanceByKey("process");

    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(17);

    // 5 user tasks
    List<Task> userTasks = taskService.createTaskQuery().list();
    assertThat(userTasks)
            .isNotNull()
            .hasSize(5);

    // there are 5 event subscriptions to the event on the inner user task
    List<Execution> executions = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName")
        .list();
    assertThat(executions)
            .isNotNull()
            .hasSize(5);

    // there is a single event subscription for the event on the subprocess
    executions = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName2")
        .list();
    assertThat(executions)
            .isNotNull()
            .hasSize(1);

    // if we complete the outer message event, all inner executions are removed
    Execution outerScopeExecution = executions.get(0);
    runtimeService.messageEventReceived("messageName2", outerScopeExecution.getId());

    executions = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName")
        .list();
    assertThat(executions).isEmpty();

    Task userTask = taskService.createTaskQuery()
        .singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterOuterMessageBoundary");

    taskService.complete(userTask.getId());

    // and we are done

  }

  /**
   * Triggering one boundary event should not remove the event subscription
   * of a boundary event for a concurrent task
   */
  @Deployment
  @Test
  void testBoundaryMessageEventConcurrent() {
    runtimeService.startProcessInstanceByKey("boundaryEvent");

    EventSubscription eventSubscriptionTask1 = runtimeService.createEventSubscriptionQuery().activityId("messageBoundary1").singleResult();
    assertThat(eventSubscriptionTask1).isNotNull();

    EventSubscription eventSubscriptionTask2 = runtimeService.createEventSubscriptionQuery().activityId("messageBoundary2").singleResult();
    assertThat(eventSubscriptionTask2).isNotNull();

    // when I trigger the boundary event for task1
    runtimeService.correlateMessage("task1Message");

    // then the event subscription for task2 still exists
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isOne();
    assertThat(runtimeService.createEventSubscriptionQuery().activityId("messageBoundary2").singleResult()).isNotNull();

  }

  @Deployment
  @Test
  void testExpressionInBoundaryMessageEventName() {

    // given a process instance with its variables
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("foo", "bar");
    runtimeService.startProcessInstanceByKey("process", variables);


    // when message is received
    Execution execution = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("messageName-bar")
        .singleResult();
    assertThat(execution).isNotNull();
    runtimeService.messageEventReceived("messageName-bar", execution.getId());

    // then then a task should be completed
    Task userTask = taskService.createTaskQuery().singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterMessage");
    taskService.complete(userTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

  }

}
