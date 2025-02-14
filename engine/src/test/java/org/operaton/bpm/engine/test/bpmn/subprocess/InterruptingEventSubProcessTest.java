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
package org.operaton.bpm.engine.test.bpmn.subprocess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.fail;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.EventSubscriptionQuery;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 * @author Roman Smirnov
 */
public class InterruptingEventSubProcessTest extends PluggableProcessEngineTest {

  @Deployment(resources="org/operaton/bpm/engine/test/bpmn/subprocess/InterruptingEventSubProcessTest.testCancelEventSubscriptions.bpmn")
  @Test
  public void testCancelEventSubscriptionsWhenReceivingAMessage() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    TaskQuery taskQuery = taskService.createTaskQuery();
    EventSubscriptionQuery eventSubscriptionQuery = runtimeService.createEventSubscriptionQuery();

    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskBeforeInterruptingEventSuprocess");

    List<EventSubscription> eventSubscriptions = eventSubscriptionQuery.list();
    assertThat(eventSubscriptions).hasSize(2);

    runtimeService.messageEventReceived("newMessage", pi.getId());

    task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterMessageStartEvent");

    assertThat(eventSubscriptionQuery.count()).isEqualTo(0);
    var processInstanceId = pi.getId();

    try {
      runtimeService.signalEventReceived("newSignal", processInstanceId);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected exception;
    }

    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources="org/operaton/bpm/engine/test/bpmn/subprocess/InterruptingEventSubProcessTest.testCancelEventSubscriptions.bpmn")
  @Test
  public void testCancelEventSubscriptionsWhenReceivingASignal() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    TaskQuery taskQuery = taskService.createTaskQuery();
    EventSubscriptionQuery eventSubscriptionQuery = runtimeService.createEventSubscriptionQuery();

    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskBeforeInterruptingEventSuprocess");

    List<EventSubscription> eventSubscriptions = eventSubscriptionQuery.list();
    assertThat(eventSubscriptions).hasSize(2);

    runtimeService.signalEventReceived("newSignal", pi.getId());

    task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("tastAfterSignalStartEvent");

    assertThat(eventSubscriptionQuery.count()).isEqualTo(0);
    var processInstanceId = pi.getId();

    try {
      runtimeService.messageEventReceived("newMessage", processInstanceId);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected exception;
    }

    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testCancelTimer() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    TaskQuery taskQuery = taskService.createTaskQuery();
    JobQuery jobQuery = managementService.createJobQuery().timers();

    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskBeforeInterruptingEventSuprocess");

    Job timer = jobQuery.singleResult();
    assertThat(timer).isNotNull();

    runtimeService.messageEventReceived("newMessage", pi.getId());

    task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterMessageStartEvent");

    assertThat(jobQuery.count()).isEqualTo(0);

    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testKeepCompensation() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    TaskQuery taskQuery = taskService.createTaskQuery();
    EventSubscriptionQuery eventSubscriptionQuery = runtimeService.createEventSubscriptionQuery();

    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskBeforeInterruptingEventSuprocess");

    List<EventSubscription> eventSubscriptions = eventSubscriptionQuery.list();
    assertThat(eventSubscriptions).hasSize(2);

    runtimeService.messageEventReceived("newMessage", pi.getId());

    task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterMessageStartEvent");

    assertThat(eventSubscriptionQuery.count()).isEqualTo(1);

    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testTimeCycle() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    EventSubscriptionQuery eventSubscriptionQuery = runtimeService.createEventSubscriptionQuery();
    assertThat(eventSubscriptionQuery.count()).isEqualTo(0);

    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.count()).isEqualTo(1);
    Task task = taskQuery.singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("task");

    JobQuery jobQuery = managementService.createJobQuery().timers();
    assertThat(jobQuery.count()).isEqualTo(1);

    String jobId = jobQuery.singleResult().getId();
    managementService.executeJob(jobId);

    assertThat(jobQuery.count()).isEqualTo(0);

    assertThat(taskQuery.count()).isEqualTo(1);
    task = taskQuery.singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("eventSubProcessTask");

    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstanceId);
  }

}
