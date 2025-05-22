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
package org.operaton.bpm.integrationtest.functional.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import org.operaton.bpm.application.InvocationContext;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.util.JobExecutorWaitUtils;
import org.operaton.bpm.integrationtest.functional.context.beans.NoOpJavaDelegate;
import org.operaton.bpm.integrationtest.functional.context.beans.SignalableTask;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Checks if the process application is invoked with an invocation context.
 */
@RunWith(Arquillian.class)
public class InvocationContextTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment(name = "app")
  public static WebArchive createDeployment() {
    return ShrinkWrap.create(WebArchive.class, "app.war")
        .addAsResource("META-INF/processes.xml")
        .addAsLibraries(DeploymentHelper.getTestingLibs())
        .addClass(AbstractFoxPlatformIntegrationTest.class)
        .addClass(ProcessApplicationWithInvocationContext.class)
        .addClass(NoOpJavaDelegate.class)
        .addClass(SignalableTask.class)
        .addClass(JobExecutorWaitUtils.class)
        .addAsResource("org/operaton/bpm/integrationtest/functional/context/InvocationContextTest-timer.bpmn")
        .addAsResource("org/operaton/bpm/integrationtest/functional/context/InvocationContextTest-message.bpmn")
        .addAsResource("org/operaton/bpm/integrationtest/functional/context/InvocationContextTest-signalTask.bpmn");
  }

  @After
  public void cleanUp() {
    ClockUtil.reset();
  }

  @Test
  @OperateOnDeployment("app")
  public void testInvokeProcessApplicationWithContextOnStart() {

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("messageProcess");

    InvocationContext invocationContext = ProcessApplicationWithInvocationContext.getInvocationContext();
    assertThat(invocationContext).isNotNull();
    assertThat(invocationContext.getExecution()).isNotNull();
    assertThat(invocationContext.getExecution().getId()).isEqualTo(pi.getId());
  }

  @Test
  @OperateOnDeployment("app")
  public void testInvokeProcessApplicationWithContextOnAsyncExecution() {

    runtimeService.startProcessInstanceByKey("timerProcess");
    ProcessApplicationWithInvocationContext.clearInvocationContext();

    Job timer = managementService.createJobQuery().timers().singleResult();
    assertThat(timer).isNotNull();

    long dueDate = timer.getDuedate().getTime();
    Date afterDueDate = new Date(dueDate + 1000 * 60);

    ClockUtil.setCurrentTime(afterDueDate);
    waitForJobExecutorToProcessAllJobs();

    InvocationContext invocationContext = ProcessApplicationWithInvocationContext.getInvocationContext();
    assertThat(invocationContext).isNotNull();
    assertThat(invocationContext.getExecution()).isNotNull();
    assertThat(invocationContext.getExecution().getId()).isEqualTo(timer.getExecutionId());
  }

  @Test
  @OperateOnDeployment("app")
  public void testInvokeProcessApplicationWithContextOnMessageReceived() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("messageProcess");
    ProcessApplicationWithInvocationContext.clearInvocationContext();

    EventSubscription messageSubscription = runtimeService.createEventSubscriptionQuery().eventType("message").processInstanceId(processInstance.getId()).singleResult();
    assertThat(messageSubscription).isNotNull();

    runtimeService.messageEventReceived(messageSubscription.getEventName(), messageSubscription.getExecutionId());

    InvocationContext invocationContext = ProcessApplicationWithInvocationContext.getInvocationContext();
    assertThat(invocationContext).isNotNull();
    assertThat(invocationContext.getExecution()).isNotNull();
    assertThat(invocationContext.getExecution().getId()).isEqualTo(messageSubscription.getExecutionId());
  }

  @Test
  @OperateOnDeployment("app")
  public void testInvokeProcessApplicationWithContextOnSignalTask() {

    runtimeService.startProcessInstanceByKey("signalableProcess");
    ProcessApplicationWithInvocationContext.clearInvocationContext();

    Execution execution = runtimeService.createExecutionQuery().activityId("waitingTask").singleResult();
    assertThat(execution).isNotNull();

    runtimeService.signal(execution.getId());

    InvocationContext invocationContext = ProcessApplicationWithInvocationContext.getInvocationContext();
    assertThat(invocationContext).isNotNull();
    assertThat(invocationContext.getExecution()).isNotNull();
    assertThat(invocationContext.getExecution().getId()).isEqualTo(execution.getId());
  }

}
