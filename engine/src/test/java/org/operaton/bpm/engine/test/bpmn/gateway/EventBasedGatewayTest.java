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
package org.operaton.bpm.engine.test.bpmn.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.fail;

import java.util.Date;

import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.EventSubscriptionQuery;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;


/**
 * @author Daniel Meyer
 */
public class EventBasedGatewayTest extends PluggableProcessEngineTest {

  @Deployment(resources={
          "org/operaton/bpm/engine/test/bpmn/gateway/EventBasedGatewayTest.testCatchAlertAndTimer.bpmn20.xml",
          "org/operaton/bpm/engine/test/bpmn/gateway/EventBasedGatewayTest.throwAlertSignal.bpmn20.xml"})
  @Test
  public void testCatchSignalCancelsTimer() {

    runtimeService.startProcessInstanceByKey("catchSignal");

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    runtimeService.startProcessInstanceByKey("throwSignal");

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(0);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);

    Task task = taskService.createTaskQuery()
      .taskName("afterSignal")
      .singleResult();

    assertThat(task).isNotNull();

    taskService.complete(task.getId());

  }

  @Deployment(resources={
          "org/operaton/bpm/engine/test/bpmn/gateway/EventBasedGatewayTest.testCatchAlertAndTimer.bpmn20.xml"
          })
  @Test
  public void testCatchTimerCancelsSignal() {

    runtimeService.startProcessInstanceByKey("catchSignal");

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() +10000));
    try {
      // wait for timer to fire
      testRule.waitForJobExecutorToProcessAllJobs(10000);

      assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(0);
      assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
      assertThat(managementService.createJobQuery().count()).isEqualTo(0);

      Task task = taskService.createTaskQuery()
        .taskName("afterTimer")
        .singleResult();

      assertThat(task).isNotNull();

      taskService.complete(task.getId());
    }finally{
      ClockUtil.setCurrentTime(new Date());
    }
  }

  @Deployment
  @Test
  public void testCatchSignalAndMessageAndTimer() {

    runtimeService.startProcessInstanceByKey("catchSignal");

    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(2);
    EventSubscriptionQuery messageEventSubscriptionQuery = runtimeService.createEventSubscriptionQuery().eventType("message");
    assertThat(messageEventSubscriptionQuery.count()).isEqualTo(1);
    assertThat(runtimeService.createEventSubscriptionQuery().eventType("signal").count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    // we can query for an execution with has both a signal AND message subscription
    Execution execution = runtimeService.createExecutionQuery()
      .messageEventSubscriptionName("newInvoice")
      .signalEventSubscriptionName("alert")
      .singleResult();
    assertThat(execution).isNotNull();

    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() +10000));
    try {

      EventSubscription messageEventSubscription = messageEventSubscriptionQuery.singleResult();
      runtimeService.messageEventReceived(messageEventSubscription.getEventName(), messageEventSubscription.getExecutionId());

      assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(0);
      assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
      assertThat(managementService.createJobQuery().count()).isEqualTo(0);

      Task task = taskService.createTaskQuery()
        .taskName("afterMessage")
        .singleResult();

      assertThat(task).isNotNull();

      taskService.complete(task.getId());
    }finally{
      ClockUtil.setCurrentTime(new Date());
    }
  }

  @Test
  public void testConnectedToActitity() {
    var deploymentBuilder = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/gateway/EventBasedGatewayTest.testConnectedToActivity.bpmn20.xml");

    try {
      deploymentBuilder.deploy();
      fail("exception expected");
    } catch (ParseException e) {
      assertThat(e.getMessage()).contains("Event based gateway can only be connected to elements of type intermediateCatchEvent");
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("gw1");
    }

  }

  @Test
  public void testInvalidSequenceFlow() {
    var deploymentBuilder = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/gateway/EventBasedGatewayTest.testEventInvalidSequenceFlow.bpmn20.xml");

    try {
      deploymentBuilder.deploy();
      fail("exception expected");
    } catch (ParseException e) {
      assertThat(e.getMessage()).contains("Invalid incoming sequenceflow for intermediateCatchEvent");
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("invalidFlow");
    }

  }

  @Deployment
  @Test
  public void testTimeCycle() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    String jobId = jobQuery.singleResult().getId();
    managementService.executeJob(jobId);

    assertThat(jobQuery.count()).isEqualTo(0);

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    testRule.assertProcessEnded(processInstanceId);
  }

}
