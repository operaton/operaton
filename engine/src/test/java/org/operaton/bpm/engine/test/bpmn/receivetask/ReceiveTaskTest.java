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
package org.operaton.bpm.engine.test.bpmn.receivetask;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.operaton.bpm.engine.MismatchingMessageCorrelationException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.event.EventType;
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
 * see https://app.camunda.com/jira/browse/CAM-1612
 *
 * @author Daniel Meyer
 * @author Danny Gräf
 * @author Falko Menge
 */
class ReceiveTaskTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;

  private List<EventSubscription> getEventSubscriptionList() {
    return runtimeService.createEventSubscriptionQuery()
        .eventType(EventType.MESSAGE.name()).list();
  }

  private List<EventSubscription> getEventSubscriptionList(String activityId) {
    return runtimeService.createEventSubscriptionQuery()
        .eventType(EventType.MESSAGE.name()).activityId(activityId).list();
  }

  private String getExecutionId(String processInstanceId, String activityId) {
    return runtimeService.createExecutionQuery()
        .processInstanceId(processInstanceId).activityId(activityId).singleResult().getId();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.simpleReceiveTask.bpmn20.xml")
  @Test
  void testReceiveTaskWithoutMessageReference() {

    // given: a process instance waiting in the receive task
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there is no message event subscription created for a receive task without a message reference
    assertThat(getEventSubscriptionList()).isEmpty();

    // then: we can signal the waiting receive task
    runtimeService.signal(processInstance.getId());

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.singleReceiveTask.bpmn20.xml")
  @Test
  void testSupportsLegacySignalingOnSingleReceiveTask() {

    // given: a process instance waiting in the receive task
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there is a message event subscription for the task
    assertThat(getEventSubscriptionList()).hasSize(1);

    // then: we can signal the waiting receive task
    runtimeService.signal(getExecutionId(processInstance.getId(), "waitState"));

    // expect: subscription is removed
    assertThat(getEventSubscriptionList()).isEmpty();

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.singleReceiveTask.bpmn20.xml")
  @Test
  void testSupportsMessageEventReceivedOnSingleReceiveTask() {

    // given: a process instance waiting in the receive task
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there is a message event subscription for the task
    List<EventSubscription> subscriptionList = getEventSubscriptionList();
    assertThat(subscriptionList).hasSize(1);
    EventSubscription subscription = subscriptionList.get(0);

    // then: we can trigger the event subscription
    runtimeService.messageEventReceived(subscription.getEventName(), subscription.getExecutionId());

    // expect: subscription is removed
    assertThat(getEventSubscriptionList()).isEmpty();

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.singleReceiveTask.bpmn20.xml")
  @Test
  void testSupportsCorrelateMessageOnSingleReceiveTask() {

    // given: a process instance waiting in the receive task
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there is a message event subscription for the task
    List<EventSubscription> subscriptionList = getEventSubscriptionList();
    assertThat(subscriptionList).hasSize(1);
    EventSubscription subscription = subscriptionList.get(0);

    // then: we can correlate the event subscription
    runtimeService.correlateMessage(subscription.getEventName());

    // expect: subscription is removed
    assertThat(getEventSubscriptionList()).isEmpty();

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.singleReceiveTask.bpmn20.xml")
  @Test
  void testSupportsCorrelateMessageByBusinessKeyOnSingleReceiveTask() {

    // given: a process instance with business key 23 waiting in the receive task
    ProcessInstance processInstance23 = runtimeService.startProcessInstanceByKey("testProcess", "23");

    // given: a 2nd process instance with business key 42 waiting in the receive task
    ProcessInstance processInstance42 = runtimeService.startProcessInstanceByKey("testProcess", "42");

    // expect: there is two message event subscriptions for the tasks
    List<EventSubscription> subscriptionList = getEventSubscriptionList();
    assertThat(subscriptionList).hasSize(2);

    // then: we can correlate the event subscription to one of the process instances
    runtimeService.correlateMessage("newInvoiceMessage", "23");

    // expect: one subscription is removed
    assertThat(getEventSubscriptionList()).hasSize(1);

    // expect: this ends the process instance with business key 23
    testRule.assertProcessEnded(processInstance23.getId());

    // expect: other process instance is still running
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance42.getId()).count()).isOne();

    // then: we can correlate the event subscription to the other process instance
    runtimeService.correlateMessage("newInvoiceMessage", "42");

    // expect: subscription is removed
    assertThat(getEventSubscriptionList()).isEmpty();

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance42.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.multiSequentialReceiveTask.bpmn20.xml")
  @Test
  void testSupportsLegacySignalingOnSequentialMultiReceiveTask() {

    // given: a process instance waiting in the first receive tasks
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there is a message event subscription for the first task
    List<EventSubscription> subscriptionList = getEventSubscriptionList();
    assertThat(subscriptionList).hasSize(1);
    EventSubscription subscription = subscriptionList.get(0);
    String firstSubscriptionId = subscription.getId();

    // then: we can signal the waiting receive task
    runtimeService.signal(getExecutionId(processInstance.getId(), "waitState"));

    // expect: there is a new subscription created for the second receive task instance
    subscriptionList = getEventSubscriptionList();
    assertThat(subscriptionList).hasSize(1);
    subscription = subscriptionList.get(0);
    assertThat(subscription.getId()).isNotEqualTo(firstSubscriptionId);

    // then: we can signal the second waiting receive task
    runtimeService.signal(getExecutionId(processInstance.getId(), "waitState"));

    // expect: no event subscription left
    assertThat(getEventSubscriptionList()).isEmpty();

    // expect: one user task is created
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.multiSequentialReceiveTask.bpmn20.xml")
  @Test
  void testSupportsMessageEventReceivedOnSequentialMultiReceiveTask() {

    // given: a process instance waiting in the first receive tasks
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there is a message event subscription for the first task
    List<EventSubscription> subscriptionList = getEventSubscriptionList();
    assertThat(subscriptionList).hasSize(1);
    EventSubscription subscription = subscriptionList.get(0);
    String firstSubscriptionId = subscription.getId();

    // then: we can trigger the event subscription
    runtimeService.messageEventReceived(subscription.getEventName(), subscription.getExecutionId());

    // expect: there is a new subscription created for the second receive task instance
    subscriptionList = getEventSubscriptionList();
    assertThat(subscriptionList).hasSize(1);
    subscription = subscriptionList.get(0);
    assertThat(subscription.getId()).isNotEqualTo(firstSubscriptionId);

    // then: we can trigger the second event subscription
    runtimeService.messageEventReceived(subscription.getEventName(), subscription.getExecutionId());

    // expect: no event subscription left
    assertThat(getEventSubscriptionList()).isEmpty();

    // expect: one user task is created
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.multiSequentialReceiveTask.bpmn20.xml")
  @Test
  void testSupportsCorrelateMessageOnSequentialMultiReceiveTask() {

    // given: a process instance waiting in the first receive tasks
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there is a message event subscription for the first task
    List<EventSubscription> subscriptionList = getEventSubscriptionList();
    assertThat(subscriptionList).hasSize(1);
    EventSubscription subscription = subscriptionList.get(0);
    String firstSubscriptionId = subscription.getId();

    // then: we can trigger the event subscription
    runtimeService.correlateMessage(subscription.getEventName());

    // expect: there is a new subscription created for the second receive task instance
    subscriptionList = getEventSubscriptionList();
    assertThat(subscriptionList).hasSize(1);
    subscription = subscriptionList.get(0);
    assertThat(subscription.getId()).isNotEqualTo(firstSubscriptionId);

    // then: we can trigger the second event subscription
    runtimeService.correlateMessage(subscription.getEventName());

    // expect: no event subscription left
    assertThat(getEventSubscriptionList()).isEmpty();

    // expect: one user task is created
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.multiParallelReceiveTask.bpmn20.xml")
  @Test
  void testSupportsLegacySignalingOnParallelMultiReceiveTask() {

    // given: a process instance waiting in two receive tasks
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there are two message event subscriptions
    List<EventSubscription> subscriptions = getEventSubscriptionList();
    assertThat(subscriptions).hasSize(2);

    // expect: there are two executions
    List<Execution> executions = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId()).activityId("waitState")
        .messageEventSubscriptionName("newInvoiceMessage").list();
    assertThat(executions).hasSize(2);

    // then: we can signal both waiting receive task
    runtimeService.signal(executions.get(0).getId());
    runtimeService.signal(executions.get(1).getId());

    // expect: both event subscriptions are removed
    assertThat(getEventSubscriptionList()).isEmpty();

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance.getId());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.multiParallelReceiveTask.bpmn20.xml",
    "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.multiSubProcessReceiveTask.bpmn20.xml",
    "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.parallelGatewayReceiveTask.bpmn20.xml"
  })
  void testSupportsMessageEventReceived (String bpmnResource) {
    testRule.deploy(bpmnResource);

    // given: a process instance waiting in two receive tasks
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there are two message event subscriptions
    List<EventSubscription> subscriptions = getEventSubscriptionList();
    assertThat(subscriptions).hasSize(2);

    // then: we can trigger both event subscriptions
    runtimeService.messageEventReceived(subscriptions.get(0).getEventName(), subscriptions.get(0).getExecutionId());
    runtimeService.messageEventReceived(subscriptions.get(1).getEventName(), subscriptions.get(1).getExecutionId());

    // expect: both event subscriptions are removed
    assertThat(getEventSubscriptionList()).isEmpty();

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.multiParallelReceiveTask.bpmn20.xml")
  @Test
  void testNotSupportsCorrelateMessageOnParallelMultiReceiveTask() {

    // given: a process instance waiting in two receive tasks
    runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there are two message event subscriptions
    List<EventSubscription> subscriptions = getEventSubscriptionList();
    assertThat(subscriptions).hasSize(2);
    var eventSubscription = subscriptions.get(0).getEventName();

    // then: we can not correlate an event
    assertThatThrownBy(() -> runtimeService.correlateMessage(eventSubscription)).isInstanceOf(MismatchingMessageCorrelationException.class);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.multiParallelReceiveTaskCompensate.bpmn20.xml")
  @Test
  void testSupportsMessageEventReceivedOnParallelMultiReceiveTaskWithCompensation() {

    // given: a process instance waiting in two receive tasks
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there are two message event subscriptions
    List<EventSubscription> subscriptions = getEventSubscriptionList();
    assertThat(subscriptions).hasSize(2);

    // then: we can trigger the first event subscription
    runtimeService.messageEventReceived(subscriptions.get(0).getEventName(), subscriptions.get(0).getExecutionId());

    // expect: after completing the first receive task there is one event subscription for compensation
    assertThat(runtimeService.createEventSubscriptionQuery()
      .eventType(EventType.COMPENSATE.name()).count()).isOne();

    // then: we can trigger the second event subscription
    runtimeService.messageEventReceived(subscriptions.get(1).getEventName(), subscriptions.get(1).getExecutionId());

    // expect: there are three event subscriptions for compensation (two subscriptions for tasks and one for miBody)
    assertThat(runtimeService.createEventSubscriptionQuery()
        .eventType(EventType.COMPENSATE.name()).count()).isEqualTo(3);

    // expect: one user task is created
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.multiParallelReceiveTaskBoundary.bpmn20.xml")
  @Test
  void testSupportsMessageEventReceivedOnParallelMultiInstanceWithBoundary() {

    // given: a process instance waiting in two receive tasks
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there are three message event subscriptions
    assertThat(getEventSubscriptionList()).hasSize(3);

    // expect: there are two message event subscriptions for the receive tasks
    List<EventSubscription> subscriptions = getEventSubscriptionList("waitState");
    assertThat(subscriptions).hasSize(2);

    // then: we can trigger both receive task event subscriptions
    runtimeService.messageEventReceived(subscriptions.get(0).getEventName(), subscriptions.get(0).getExecutionId());
    runtimeService.messageEventReceived(subscriptions.get(1).getEventName(), subscriptions.get(1).getExecutionId());

    // expect: all subscriptions are removed (boundary subscription is removed too)
    assertThat(getEventSubscriptionList()).isEmpty();

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.multiParallelReceiveTaskBoundary.bpmn20.xml")
  @Test
  void testSupportsMessageEventReceivedOnParallelMultiInstanceWithBoundaryEventReceived() {

    // given: a process instance waiting in two receive tasks
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there are three message event subscriptions
    assertThat(getEventSubscriptionList()).hasSize(3);

    // expect: there is one message event subscription for the boundary event
    List<EventSubscription> subscriptions = getEventSubscriptionList("cancel");
    assertThat(subscriptions).hasSize(1);
    EventSubscription subscription = subscriptions.get(0);

    // then: we can trigger the boundary subscription to cancel both tasks
    runtimeService.messageEventReceived(subscription.getEventName(), subscription.getExecutionId());

    // expect: all subscriptions are removed (receive task subscriptions too)
    assertThat(getEventSubscriptionList()).isEmpty();

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.subProcessReceiveTask.bpmn20.xml")
  @Test
  void testSupportsMessageEventReceivedOnSubProcessReceiveTask() {

    // given: a process instance waiting in the sub-process receive task
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there is a message event subscription for the task
    List<EventSubscription> subscriptionList = getEventSubscriptionList();
    assertThat(subscriptionList).hasSize(1);
    EventSubscription subscription = subscriptionList.get(0);

    // then: we can trigger the event subscription
    runtimeService.messageEventReceived(subscription.getEventName(), subscription.getExecutionId());

    // expect: subscription is removed
    assertThat(getEventSubscriptionList()).isEmpty();

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.parallelGatewayReceiveTask.bpmn20.xml")
  @Test
  void testSupportsCorrelateMessageOnReceiveTaskBehindParallelGateway() {

    // given: a process instance waiting in two receive tasks
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // expect: there are two message event subscriptions
    List<EventSubscription> subscriptions = getEventSubscriptionList();
    assertThat(subscriptions).hasSize(2);

    // then: we can trigger both receive task event subscriptions
    runtimeService.correlateMessage(subscriptions.get(0).getEventName());
    runtimeService.correlateMessage(subscriptions.get(1).getEventName());

    // expect: subscriptions are removed
    assertThat(getEventSubscriptionList()).isEmpty();

    // expect: this ends the process instance
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  void testWaitStateBehavior() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("receiveTask");
    Execution execution = runtimeService.createExecutionQuery()
      .processInstanceId(pi.getId())
      .activityId("waitState")
      .singleResult();
    assertThat(execution).isNotNull();

    runtimeService.signal(execution.getId());
    testRule.assertProcessEnded(pi.getId());
  }
}
