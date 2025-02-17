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
package org.operaton.bpm.engine.test.bpmn.async;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.persistence.entity.MessageEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener.RecordedEvent;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.engine.variable.Variables;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Daniel Meyer
 * @author Stefan Hentschel
 */
public class AsyncTaskTest extends PluggableProcessEngineTest {

  public static boolean INVOCATION;
  public static int NUM_INVOCATIONS = 0;

  @Deployment
  @Test
  public void testAsyncServiceNoListeners() {
    INVOCATION = false;
    // start process
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncService");

    // now we have one transition instance below the process instance:
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(activityInstance.getChildTransitionInstances().length).isEqualTo(1);
    assertThat(activityInstance.getChildActivityInstances().length).isEqualTo(0);

    assertNotNull(activityInstance.getChildTransitionInstances()[0]);

    // now there should be one job in the database:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    // the service was not invoked:
    assertFalse(INVOCATION);

    testRule.executeAvailableJobs();

    // the service was invoked
    assertTrue(INVOCATION);
    // and the job is done
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testAsyncServiceListeners() {
    String pid = runtimeService.startProcessInstanceByKey("asyncService").getProcessInstanceId();
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    // the listener was not yet invoked:
    assertNull(runtimeService.getVariable(pid, "listener"));

    testRule.executeAvailableJobs();

    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testAsyncServiceConcurrent() {
    INVOCATION = false;
    // start process
    runtimeService.startProcessInstanceByKey("asyncService");
    // now there should be one job in the database:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    // the service was not invoked:
    assertFalse(INVOCATION);

    testRule.executeAvailableJobs();

    // the service was invoked
    assertTrue(INVOCATION);
    // and the job is done
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testAsyncSequentialMultiInstanceWithServiceTask() {
    NUM_INVOCATIONS = 0;
    // start process
    runtimeService.startProcessInstanceByKey("asyncService");

    // the service was not invoked:
    assertThat(NUM_INVOCATIONS).isEqualTo(0);

    // now there should be one job for the multi-instance body to execute:
    testRule.executeAvailableJobs(1);

    // the service was invoked
    assertThat(NUM_INVOCATIONS).isEqualTo(5);
    // and the job is done
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
  }


  @Deployment
  @Test
  public void testAsyncParallelMultiInstanceWithServiceTask() {
    NUM_INVOCATIONS = 0;
    // start process
    runtimeService.startProcessInstanceByKey("asyncService");

    // the service was not invoked:
    assertThat(NUM_INVOCATIONS).isEqualTo(0);

    // now there should be one job for the multi-instance body to execute:
    testRule.executeAvailableJobs(1);

    // the service was invoked
    assertThat(NUM_INVOCATIONS).isEqualTo(5);
    // and the job is done
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testAsyncServiceWrappedInSequentialMultiInstance() {
    NUM_INVOCATIONS = 0;
    // start process
    runtimeService.startProcessInstanceByKey("asyncService");

    // the service was not invoked:
    assertThat(NUM_INVOCATIONS).isEqualTo(0);

    // now there should be one job for the first service task wrapped in the multi-instance body:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    // execute all jobs - one for each service task:
    testRule.executeAvailableJobs(5);

    // the service was invoked
    assertThat(NUM_INVOCATIONS).isEqualTo(5);
    // and the job is done
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testAsyncServiceWrappedInParallelMultiInstance() {
    NUM_INVOCATIONS = 0;
    // start process
    runtimeService.startProcessInstanceByKey("asyncService");

    // the service was not invoked:
    assertThat(NUM_INVOCATIONS).isEqualTo(0);

    // now there should be one job for each service task wrapped in the multi-instance body:
    assertThat(managementService.createJobQuery().count()).isEqualTo(5);
    // execute all jobs:
    testRule.executeAvailableJobs(5);

    // the service was invoked
    assertThat(NUM_INVOCATIONS).isEqualTo(5);
    // and the job is done
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testAsyncBeforeAndAfterOfServiceWrappedInParallelMultiInstance() {
    NUM_INVOCATIONS = 0;
    // start process
    runtimeService.startProcessInstanceByKey("asyncService");

    // the service was not invoked:
    assertThat(NUM_INVOCATIONS).isEqualTo(0);

    // now there should be one job for each service task wrapped in the multi-instance body:
    assertThat(managementService.createJobQuery().count()).isEqualTo(5);
    // execute all jobs - one for asyncBefore and another for asyncAfter:
    testRule.executeAvailableJobs(5+5);

    // the service was invoked
    assertThat(NUM_INVOCATIONS).isEqualTo(5);
    // and the job is done
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testAsyncBeforeSequentialMultiInstanceWithAsyncAfterServiceWrappedInMultiInstance() {
    NUM_INVOCATIONS = 0;
    // start process
    runtimeService.startProcessInstanceByKey("asyncService");

    // the service was not invoked:
    assertThat(NUM_INVOCATIONS).isEqualTo(0);

    // now there should be one job for the multi-instance body:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    // execute all jobs - one for multi-instance body and one for each service task wrapped in the multi-instance body:
    testRule.executeAvailableJobs(1+5);

    // the service was invoked
    assertThat(NUM_INVOCATIONS).isEqualTo(5);
    // and the job is done
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
  }

  protected void assertTransitionInstances(String processInstanceId, String activityId, int numInstances) {
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    assertThat(tree.getTransitionInstances(activityId).length).isEqualTo(numInstances);
  }

  @Deployment
  @Test
  public void testAsyncBeforeAndAfterParallelMultiInstanceWithAsyncBeforeAndAfterServiceWrappedInMultiInstance() {
    NUM_INVOCATIONS = 0;
    // start process
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncService");

    // the service was not invoked:
    assertThat(NUM_INVOCATIONS).isEqualTo(0);

    // now there should be one job for the multi-instance body:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    assertTransitionInstances(processInstance.getId(), "service" + BpmnParse.MULTI_INSTANCE_BODY_ID_SUFFIX, 1);

    // when the mi body before job is executed
    Job job = managementService.createJobQuery().singleResult();
    managementService.executeJob(job.getId());

    // then there are five inner async before jobs
    List<Job> innerBeforeJobs = managementService.createJobQuery().list();
    assertThat(innerBeforeJobs.size()).isEqualTo(5);
    assertTransitionInstances(processInstance.getId(), "service", 5);
    assertThat(NUM_INVOCATIONS).isEqualTo(0);

    // when executing all inner jobs
    for (Job innerBeforeJob : innerBeforeJobs) {
      managementService.executeJob(innerBeforeJob.getId());
    }
    assertThat(NUM_INVOCATIONS).isEqualTo(5);

    // then there are five async after jobs
    List<Job> innerAfterJobs = managementService.createJobQuery().list();
    assertThat(innerAfterJobs.size()).isEqualTo(5);
    assertTransitionInstances(processInstance.getId(), "service", 5);

    // when executing all inner jobs
    for (Job innerAfterJob : innerAfterJobs) {
      managementService.executeJob(innerAfterJob.getId());
    }

    // then there is one mi body after job
    job = managementService.createJobQuery().singleResult();
    assertNotNull(job);
    assertTransitionInstances(processInstance.getId(), "service" + BpmnParse.MULTI_INSTANCE_BODY_ID_SUFFIX, 1);

    // when executing this job, the process ends
    managementService.executeJob(job.getId());
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/async/AsyncTaskTest.testAsyncServiceWrappedInParallelMultiInstance.bpmn20.xml")
  @Test
  public void testAsyncServiceWrappedInParallelMultiInstanceActivityInstance() {
    // given a process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncService");

    // when there are five jobs for the inner activity
    assertThat(managementService.createJobQuery().count()).isEqualTo(5);

    // then they are represented in the activity instance tree by transition instances
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("service#multiInstanceBody")
            .transition("service")
            .transition("service")
            .transition("service")
            .transition("service")
            .transition("service")
        .done());
  }

  @Deployment
  @Test
  public void testFailingAsyncServiceTimer() {
    // start process
    runtimeService.startProcessInstanceByKey("asyncService");
    // now there should be one job in the database, and it is a message
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    Job job = managementService.createJobQuery().singleResult();
    if(!(job instanceof MessageEntity)) {
      fail("the job must be a message");
    }

    testRule.executeAvailableJobs();

    // the service failed: the execution is still sitting in the service task:
    Execution execution = runtimeService.createExecutionQuery().singleResult();
    assertNotNull(execution);
    assertThat(runtimeService.getActiveActivityIds(execution.getId()).get(0)).isEqualTo("service");

    // there is still a single job because the timer was created in the same transaction as the
    // service was executed (which rolled back)
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    runtimeService.deleteProcessInstance(execution.getId(), "dead");
  }

  // TODO: Think about this:
  @Deployment
  @Ignore
  @Test
  public void testFailingAsyncServiceTimerWithMessageJob() {
    // start process
    runtimeService.startProcessInstanceByKey("asyncService");
    // now there are two jobs the message and a timer:
    assertThat(managementService.createJobQuery().count()).isEqualTo(2);

    // let 'max-retires' on the message be reached
    testRule.executeAvailableJobs();

    // the service failed: the execution is still sitting in the service task:
    Execution execution = runtimeService.createExecutionQuery().singleResult();
    assertNotNull(execution);
    assertThat(runtimeService.getActiveActivityIds(execution.getId()).get(0)).isEqualTo("service");

    // there are two jobs, the message and the timer (the message will not be retried anymore, max retires is reached.)
    assertThat(managementService.createJobQuery().count()).isEqualTo(2);

    // now the timer triggers:
    ClockUtil.setCurrentTime(new Date(System.currentTimeMillis()+10000));
    testRule.executeAvailableJobs();

    // and we are done:
    assertNull(runtimeService.createExecutionQuery().singleResult());
    // and there are no more jobs left:
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);

  }

  @Deployment
  @Test
  public void testAsyncServiceSubProcessTimer() {
    INVOCATION = false;
    // start process
    runtimeService.startProcessInstanceByKey("asyncService");
    // now there should be two jobs in the database:
    assertThat(managementService.createJobQuery().count()).isEqualTo(2);
    // the service was not invoked:
    assertFalse(INVOCATION);

    Job job = managementService.createJobQuery().messages().singleResult();
    managementService.executeJob(job.getId());

    // the service was invoked
    assertTrue(INVOCATION);
    // both the timer and the message are cancelled
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);

  }

  @Deployment
  @Test
  public void testAsyncServiceSubProcess() {
    // start process
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncService");

    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .transition("subProcess")
      .done());

    testRule.executeAvailableJobs();

    // both the timer and the message are cancelled
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);

  }

  @Deployment
  @Test
  public void testAsyncTask() {
    // start process
    runtimeService.startProcessInstanceByKey("asyncTask");
    // now there should be one job in the database:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    testRule.executeAvailableJobs();

    // the job is done
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testAsyncScript() {
    // start process
    runtimeService.startProcessInstanceByKey("asyncScript").getProcessInstanceId();
    // now there should be one job in the database:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    // the script was not invoked:
    String eid = runtimeService.createExecutionQuery().singleResult().getId();
    assertNull(runtimeService.getVariable(eid, "invoked"));

    testRule.executeAvailableJobs();

    // and the job is done
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);

    // the script was invoked
    assertThat(runtimeService.getVariable(eid, "invoked")).isEqualTo("true");

    runtimeService.signal(eid);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/bpmn/async/AsyncTaskTest.testAsyncCallActivity.bpmn20.xml",
          "org/operaton/bpm/engine/test/bpmn/async/AsyncTaskTest.testAsyncServiceNoListeners.bpmn20.xml"})
  @Test
  public void testAsyncCallActivity() {
    // start process
    runtimeService.startProcessInstanceByKey("asyncCallactivity");
    // now there should be one job in the database:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    testRule.executeAvailableJobs();

    assertThat(managementService.createJobQuery().count()).isEqualTo(0);

  }

  @Deployment
  @Test
  public void testAsyncUserTask() {
    // start process
    String pid = runtimeService.startProcessInstanceByKey("asyncUserTask").getProcessInstanceId();
    // now there should be one job in the database:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    // the listener was not yet invoked:
    assertNull(runtimeService.getVariable(pid, "listener"));
    // there is no usertask
    assertNull(taskService.createTaskQuery().singleResult());

    testRule.executeAvailableJobs();
    // the listener was now invoked:
    assertNotNull(runtimeService.getVariable(pid, "listener"));

    // there is a usertask
    assertNotNull(taskService.createTaskQuery().singleResult());
    // and no more job
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

  }

  @Deployment
  @Test
  public void testAsyncManualTask() {
    // start PI
    String pid = runtimeService.startProcessInstanceByKey("asyncManualTask").getProcessInstanceId();

    // now there should be one job in the database:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    // the listener was not yet invoked:
    assertNull(runtimeService.getVariable(pid, "listener"));
    // there is no manual Task
    assertNull(taskService.createTaskQuery().singleResult());

    testRule.executeAvailableJobs();

    // the listener was invoked now:
    assertNotNull(runtimeService.getVariable(pid, "listener"));
    // there isn't a job anymore:
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
    // now there is a userTask
    assertNotNull(taskService.createTaskQuery().singleResult());

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);
  }

  @Deployment
  @Test
  public void testAsyncIntermediateCatchEvent() {
    // start PI
    String pid = runtimeService.startProcessInstanceByKey("asyncIntermediateCatchEvent").getProcessInstanceId();

    // now there is 1 job in the database:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    // the listener was not invoked now:
    assertNull(runtimeService.getVariable(pid, "listener"));
    // there is no intermediate catch event:
    assertNull(taskService.createTaskQuery().singleResult());

    testRule.executeAvailableJobs();
    runtimeService.correlateMessage("testMessage1");

    // the listener was now invoked:
    assertNotNull(runtimeService.getVariable(pid, "listener"));
    // there isn't a job anymore
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
    // now there is a userTask
    assertNotNull(taskService.createTaskQuery().singleResult());

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

  }

  @Deployment
  @Test
  public void testAsyncIntermediateThrowEvent() {
    // start PI
    String pid = runtimeService.startProcessInstanceByKey("asyncIntermediateThrowEvent").getProcessInstanceId();

    // now there is 1 job in the database:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    // the listener was not invoked now:
    assertNull(runtimeService.getVariable(pid, "listener"));
    // there is no intermediate throw event:
    assertNull(taskService.createTaskQuery().singleResult());

    testRule.executeAvailableJobs();

    // the listener was now invoked:
    assertNotNull(runtimeService.getVariable(pid, "listener"));
    // there isn't a job anymore
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
    // now there is a userTask
    assertNotNull(taskService.createTaskQuery().singleResult());

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);
  }

  @Deployment
  @Test
  public void testAsyncExclusiveGateway() {
    // The test needs variables to work properly
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("flow", false);

    // start PI
    String pid = runtimeService.startProcessInstanceByKey("asyncExclusiveGateway", variables).getProcessInstanceId();

    // now there is 1 job in the database:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    // the listener was not invoked now:
    assertNull(runtimeService.getVariable(pid, "listener"));
    // there is no gateway:
    assertNull(taskService.createTaskQuery().singleResult());

    testRule.executeAvailableJobs();

    // the listener was now invoked:
    assertNotNull(runtimeService.getVariable(pid, "listener"));
    // there isn't a job anymore
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
    // now there is a userTask
    assertNotNull(taskService.createTaskQuery().singleResult());

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);
  }

  @Deployment
  @Test
  public void testAsyncInclusiveGateway() {
    // start PI
    String pid = runtimeService.startProcessInstanceByKey("asyncInclusiveGateway").getProcessInstanceId();

    // now there is 1 job in the database:
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    // the listener was not invoked now:
    assertNull(runtimeService.getVariable(pid, "listener"));
    // there is no gateway:
    assertNull(taskService.createTaskQuery().singleResult());

    testRule.executeAvailableJobs();

    // the listener was now invoked:
    assertNotNull(runtimeService.getVariable(pid, "listener"));
    // there isn't a job anymore
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);
    // now there are 2 user tasks
    List<Task> list = taskService.createTaskQuery().list();
    assertThat(list.size()).isEqualTo(2);

    // complete these tasks and finish the process instance
    for(Task task: list) {
      taskService.complete(task.getId());
    }
  }

  @Deployment
  @Test
  public void testAsyncEventGateway() {
    // start PI
    String pid = runtimeService.startProcessInstanceByKey("asyncEventGateway").getProcessInstanceId();

    // now there is a job in the database
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    // the listener was not invoked now:
    assertNull(runtimeService.getVariable(pid, "listener"));
    // there is no task:
    assertNull(taskService.createTaskQuery().singleResult());

    testRule.executeAvailableJobs();

    // the listener was now invoked:
    assertNotNull(runtimeService.getVariable(pid, "listener"));
    // there isn't a job anymore
    assertThat(managementService.createJobQuery().count()).isEqualTo(0);

    // correlate Message
    runtimeService.correlateMessage("testMessageDef1");

    // now there is a userTask
    assertNotNull(taskService.createTaskQuery().singleResult());

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);
  }

  /**
   * CAM-3707
   */
  @Deployment
  @Test
  public void testDeleteShouldNotInvokeListeners() {
    RecorderExecutionListener.clear();

    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("asyncListener",
        Variables.createVariables().putValue("listener", new RecorderExecutionListener()));
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    // when deleting the process instance
    runtimeService.deleteProcessInstance(instance.getId(), "");

    // then no listeners for the async activity should have been invoked because
    // it was not active yet
    assertThat(RecorderExecutionListener.getRecordedEvents().size()).isEqualTo(0);

    RecorderExecutionListener.clear();
  }

  /**
   * CAM-3707
   */
  @Deployment
  @Test
  public void testDeleteInScopeShouldNotInvokeListeners() {
    RecorderExecutionListener.clear();

    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("asyncListenerSubProcess",
        Variables.createVariables().putValue("listener", new RecorderExecutionListener()));
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    // when deleting the process instance
    runtimeService.deleteProcessInstance(instance.getId(), "");

    // then the async task end listener has not been executed but the listeners of the sub
    // process and the process

    List<RecordedEvent> recordedEvents = RecorderExecutionListener.getRecordedEvents();
    assertThat(recordedEvents.size()).isEqualTo(2);
    assertThat(recordedEvents.get(0).getActivityId()).isEqualTo("subProcess");
    assertNull(recordedEvents.get(1).getActivityId()); // process instance end event has no activity id

    RecorderExecutionListener.clear();
  }

  /**
   * CAM-3708
   */
  @Deployment
  @Test
  public void testDeleteShouldNotInvokeOutputMapping() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("asyncOutputMapping");
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    // when
    runtimeService.deleteProcessInstance(instance.getId(), "");

    // then the output mapping has not been executed because the
    // activity was not active yet
    if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_AUDIT.getId()) {
      assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(0);
    }

  }

  /**
   * CAM-3708
   */
  @Deployment
  @Test
  public void testDeleteInScopeShouldNotInvokeOutputMapping() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("asyncOutputMappingSubProcess");
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);

    // when
    runtimeService.deleteProcessInstance(instance.getId(), "");

    // then
    if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_AUDIT.getId()) {
      // the output mapping of the task has not been executed because the
      // activity was not active yet
      assertThat(historyService.createHistoricVariableInstanceQuery().variableName("taskOutputMappingExecuted").count()).isEqualTo(0);

      // but the containing sub process output mapping was executed
      assertThat(historyService.createHistoricVariableInstanceQuery().variableName("subProcessOutputMappingExecuted").count()).isEqualTo(1);
    }
  }

  @Test
  public void testDeployAndRemoveAsyncActivity() {
    Set<String> deployments = new HashSet<>();

    try {
      // given a deployment that contains a process called "process" with an async task "task"
      org.operaton.bpm.engine.repository.Deployment deployment1 = repositoryService
          .createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/bpmn/async/AsyncTaskTest.testDeployAndRemoveAsyncActivity.v1.bpmn20.xml")
          .deploy();
      deployments.add(deployment1.getId());

      // when redeploying the process where that task is not contained anymore
      org.operaton.bpm.engine.repository.Deployment deployment2 = repositoryService
          .createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/bpmn/async/AsyncTaskTest.testDeployAndRemoveAsyncActivity.v2.bpmn20.xml")
          .deploy();
      deployments.add(deployment2.getId());

      // and clearing the deployment cache (note that the equivalent of this in a real-world
      // scenario would be making the deployment with a different engine
      processEngineConfiguration.getDeploymentCache().discardProcessDefinitionCache();

      // then it should be possible to load the latest process definition
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
      assertNotNull(processInstance);

    } finally {
      for (String deploymentId : deployments) {
        repositoryService.deleteDeployment(deploymentId, true);
      }
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/bpmn/async/processWithGatewayAndTwoEndEvents.bpmn20.xml"})
  @Test
  public void testGatewayWithTwoEndEventsLastJobReAssignedToParentExe() {
    String processKey = repositoryService.createProcessDefinitionQuery().singleResult().getKey();
    String processInstanceId = runtimeService.startProcessInstanceByKey(processKey).getId();

    List<Job> jobList = managementService.createJobQuery().processInstanceId(processInstanceId).list();

    // There should be two jobs
    assertNotNull(jobList);
    assertThat(jobList.size()).isEqualTo(2);

    managementService.executeJob(jobList.get(0).getId());

    // There should be only one job left
    jobList = managementService.createJobQuery().list();
    assertThat(jobList.size()).isEqualTo(1);

    // There should only be 1 execution left - the root execution
    assertThat(runtimeService.createExecutionQuery().list().size()).isEqualTo(1);

    // root execution should be attached to the last job
    assertThat(jobList.get(0).getExecutionId()).isEqualTo(processInstanceId);

    managementService.executeJob(jobList.get(0).getId());

    // There should be no more jobs
    jobList = managementService.createJobQuery().list();
    assertThat(jobList.size()).isEqualTo(0);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/bpmn/async/processGatewayAndTwoEndEventsPlusTimer.bpmn20.xml"})
  @Test
  public void testGatewayWithTwoEndEventsLastTimerReAssignedToParentExe() {
    String processKey = repositoryService.createProcessDefinitionQuery().singleResult().getKey();
    String processInstanceId = runtimeService.startProcessInstanceByKey(processKey).getId();

    List<Job> jobList = managementService.createJobQuery().processInstanceId(processInstanceId).list();

    // There should be two jobs
    assertNotNull(jobList);
    assertThat(jobList.size()).isEqualTo(2);

    // execute timer first
    String timerId = managementService.createJobQuery().timers().singleResult().getId();
    managementService.executeJob(timerId);

    // There should be only one job left
    jobList = managementService.createJobQuery().list();
    assertThat(jobList.size()).isEqualTo(1);

    // There should only be 1 execution left - the root execution
    assertThat(runtimeService.createExecutionQuery().list().size()).isEqualTo(1);

    // root execution should be attached to the last job
    assertThat(jobList.get(0).getExecutionId()).isEqualTo(processInstanceId);

    // execute service task
    managementService.executeJob(jobList.get(0).getId());

    // There should be no more jobs
    jobList = managementService.createJobQuery().list();
    assertThat(jobList.size()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testLongProcessDefinitionKey() {
    String key = "myrealrealrealrealrealrealrealrealrealrealreallongprocessdefinitionkeyawesome";
    String processInstanceId = runtimeService.startProcessInstanceByKey(key).getId();

    Job job = managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();

    assertThat(job.getProcessDefinitionKey()).isEqualTo(key);
  }
}
