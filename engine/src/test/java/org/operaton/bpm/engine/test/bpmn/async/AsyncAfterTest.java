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
package org.operaton.bpm.engine.test.bpmn.async;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.pvm.runtime.operation.PvmAtomicOperation;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.event.error.ThrowBpmnErrorDelegate;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobIgnoringException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Daniel Meyer
 * @author Stefan Hentschel
 *
 */
class AsyncAfterTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  RepositoryService repositoryService;
  ManagementService managementService;
  TaskService taskService;

  @Test
  void testTransitionIdRequired() {
    // given
    var deploymentBuilder = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/async/AsyncAfterTest.testTransitionIdRequired.bpmn20.xml");

    // when/then
    // if an outgoing sequence flow has no id, we cannot use it in asyncAfter
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ParseException.class)
      .hasMessageContaining("Sequence flow with sourceRef='service' must have an id, activity with id 'service' uses 'asyncAfter'.")
      .asInstanceOf(type(ParseException.class))
      .extracting(e -> e.getResourceReports().get(0).getErrors().get(0).getElementIds())
      .isEqualTo(List.of("service"));

  }

  @Deployment
  @Test
  void testAsyncAfterServiceTask() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // listeners should be fired by now
    assertListenerStartInvoked(pi);
    assertListenerEndInvoked(pi);

    // the process should wait *after* the catch event
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // if the waiting job is executed, the process instance should end
    managementService.executeJob(job.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testAsyncAfterMultiInstanceUserTask() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    List<Task> list = taskService.createTaskQuery().list();
    // multiinstance says three in the bpmn
    assertThat(list).hasSize(3);

    for (Task task : list) {
      taskService.complete(task.getId());
    }

    testRule.waitForJobExecutorToProcessAllJobs(TimeUnit.MILLISECONDS.convert(5L, TimeUnit.SECONDS));

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testAsyncAfterAndBeforeServiceTask() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // the service task is not yet invoked
    assertNotListenerStartInvoked(pi);
    assertNotBehaviorInvoked(pi);
    assertNotListenerEndInvoked(pi);

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // if the job is executed
    managementService.executeJob(job.getId());

    // the manual task is invoked
    assertListenerStartInvoked(pi);
    assertListenerEndInvoked(pi);

    // and now the process is waiting *after* the manual task
    job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // after executing the waiting job, the process instance will end
    managementService.executeJob(job.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testAsyncAfterServiceTaskMultipleTransitions() {

    // start process instance
    Map<String, Object> varMap = new HashMap<>();
    varMap.put("flowToTake", "flow2");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", varMap);

    // the service task is completely invoked
    assertListenerStartInvoked(pi);
    assertBehaviorInvoked(pi);
    assertListenerEndInvoked(pi);

    // and the execution is waiting *after* the service task
    Job continuationJob = managementService.createJobQuery().singleResult();
    assertThat(continuationJob).isNotNull();

    // if we execute the job, the process instance continues along the selected path
    managementService.executeJob(continuationJob.getId());

    assertThat(runtimeService.createExecutionQuery().activityId("taskAfterFlow2").singleResult()).isNotNull();
    assertThat(runtimeService.createExecutionQuery().activityId("taskAfterFlow3").singleResult()).isNull();

    // end the process
    runtimeService.signal(pi.getId());

    // ////////////////////////////////////////////////////////////

    // start process instance
    varMap = new HashMap<>();
    varMap.put("flowToTake", "flow3");
    pi = runtimeService.startProcessInstanceByKey("testProcess", varMap);

    // the service task is completely invoked
    assertListenerStartInvoked(pi);
    assertBehaviorInvoked(pi);
    assertListenerEndInvoked(pi);

    // and the execution is waiting *after* the service task
    continuationJob = managementService.createJobQuery().singleResult();
    assertThat(continuationJob).isNotNull();

    // if we execute the job, the process instance continues along the selected path
    managementService.executeJob(continuationJob.getId());

    assertThat(runtimeService.createExecutionQuery().activityId("taskAfterFlow2").singleResult()).isNull();
    assertThat(runtimeService.createExecutionQuery().activityId("taskAfterFlow3").singleResult()).isNotNull();

  }

  @Deployment
  @Test
  void testAsyncAfterServiceTaskMultipleTransitionsConcurrent() {

    // start process instance
    Map<String, Object> varMap = new HashMap<>();
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", varMap);

    // the service task is completely invoked
    assertListenerStartInvoked(pi);
    assertBehaviorInvoked(pi);
    assertListenerEndInvoked(pi);

    // there are two async jobs
    List<Job> jobs = managementService.createJobQuery().list();
    assertThat(jobs).hasSize(2);
    managementService.executeJob(jobs.get(0).getId());
    managementService.executeJob(jobs.get(1).getId());

    // both subsequent tasks are activated
    assertThat(runtimeService.createExecutionQuery().activityId("taskAfterFlow2").singleResult()).isNotNull();
    assertThat(runtimeService.createExecutionQuery().activityId("taskAfterFlow3").singleResult()).isNotNull();

  }

  @Deployment
  @Test
  void testAsyncAfterWithoutTransition() {

    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // the service task is completely invoked
    assertListenerStartInvoked(pi);
    assertBehaviorInvoked(pi);
    assertListenerEndInvoked(pi);

    // and the execution is waiting *after* the service task
    Job continuationJob = managementService.createJobQuery().singleResult();
    assertThat(continuationJob).isNotNull();

    // but the process end listeners have not been invoked yet
    assertThat(runtimeService.getVariable(pi.getId(), "process-listenerEndInvoked")).isNull();

    // if we execute the job, the process instance ends.
    managementService.executeJob(continuationJob.getId());
    testRule.assertProcessEnded(pi.getId());

  }

  @Deployment
  @Test
  void testAsyncAfterInNestedWithoutTransition() {

    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // the service task is completely invoked
    assertListenerStartInvoked(pi);
    assertBehaviorInvoked(pi);
    assertListenerEndInvoked(pi);

    // and the execution is waiting *after* the service task
    Job continuationJob = managementService.createJobQuery().singleResult();
    assertThat(continuationJob).isNotNull();

    // but the subprocess end listeners have not been invoked yet
    assertThat(runtimeService.getVariable(pi.getId(), "subprocess-listenerEndInvoked")).isNull();

    // if we execute the job, the listeners are invoked;
    managementService.executeJob(continuationJob.getId());
    assertThat((Boolean) runtimeService.getVariable(pi.getId(), "subprocess-listenerEndInvoked")).isTrue();

  }

  @Deployment
  @Test
  void testAsyncAfterManualTask() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testManualTask");

    // listeners should be fired by now
    assertListenerStartInvoked(pi);
    assertListenerEndInvoked(pi);

    // the process should wait *after* the catch event
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // if the waiting job is executed, the process instance should end
    managementService.executeJob(job.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testAsyncAfterAndBeforeManualTask() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testManualTask");

    // the service task is not yet invoked
    assertNotListenerStartInvoked(pi);
    assertNotListenerEndInvoked(pi);

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // if the job is executed
    managementService.executeJob(job.getId());

    // the manual task is invoked
    assertListenerStartInvoked(pi);
    assertListenerEndInvoked(pi);

    // and now the process is waiting *after* the manual task
    job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // after executing the waiting job, the process instance will end
    managementService.executeJob(job.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testAsyncAfterIntermediateCatchEvent() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testIntermediateCatchEvent");

    // the intermediate catch event is waiting for its message
    runtimeService.correlateMessage("testMessage1");

    // listeners should be fired by now
    assertListenerStartInvoked(pi);
    assertListenerEndInvoked(pi);

    // the process should wait *after* the catch event
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // if the waiting job is executed, the process instance should end
    managementService.executeJob(job.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testAsyncAfterAndBeforeIntermediateCatchEvent() {

    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testIntermediateCatchEvent");

    // check that no listener is invoked by now
    assertNotListenerStartInvoked(pi);
    assertNotListenerEndInvoked(pi);

    // the process is waiting before the message event
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // execute job to get to the message event
    testRule.executeAvailableJobs();

    // now we need to trigger the message to proceed
    runtimeService.correlateMessage("testMessage1");

    // now the listener should be invoked
    assertListenerStartInvoked(pi);
    assertListenerEndInvoked(pi);

    // and now the process is waiting *after* the intermediate catch event
    job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // after executing the waiting job, the process instance will end
    managementService.executeJob(job.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testAsyncAfterIntermediateThrowEvent() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testIntermediateThrowEvent");

    // listeners should be fired by now
    assertListenerStartInvoked(pi);
    assertListenerEndInvoked(pi);

    // the process should wait *after* the throw event
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // if the waiting job is executed, the process instance should end
    managementService.executeJob(job.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testAsyncAfterAndBeforeIntermediateThrowEvent() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testIntermediateThrowEvent");

    // the throw event is not yet invoked
    assertNotListenerStartInvoked(pi);
    assertNotListenerEndInvoked(pi);

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // if the job is executed
    managementService.executeJob(job.getId());

    // the listeners are invoked
    assertListenerStartInvoked(pi);
    assertListenerEndInvoked(pi);

    // and now the process is waiting *after* the throw event
    job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // after executing the waiting job, the process instance will end
    managementService.executeJob(job.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testAsyncAfterInclusiveGateway() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testInclusiveGateway");

    // listeners should be fired
    assertListenerStartInvoked(pi);
    assertListenerEndInvoked(pi);

    // the process should wait *after* the gateway
    assertThat(managementService.createJobQuery().active().count()).isEqualTo(2);

    testRule.executeAvailableJobs();

    // if the waiting job is executed there should be 2 user tasks
    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.active().count()).isEqualTo(2);

    // finish tasks
    List<Task> tasks = taskQuery.active().list();
    for(Task task : tasks) {
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(pi.getProcessInstanceId());

  }

  @Deployment
  @Test
  void testAsyncAfterAndBeforeInclusiveGateway() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testInclusiveGateway");

    // no listeners are fired:
    assertNotListenerStartInvoked(pi);
    assertNotListenerEndInvoked(pi);

    // we should wait *before* the gateway:
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // after executing the gateway:
    managementService.executeJob(job.getId());

    // the listeners are fired:
    assertListenerStartInvoked(pi);
    assertListenerEndInvoked(pi);

    // and we will wait *after* the gateway:
    List<Job> jobs = managementService.createJobQuery().active().list();
    assertThat(jobs).hasSize(2);
  }

  @Deployment
  @Test
  void testAsyncAfterExclusiveGateway() {
    // start process instance with variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("flow", false);

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testExclusiveGateway", variables);

    // listeners should be fired
    assertListenerStartInvoked(pi);
    assertListenerEndInvoked(pi);

    // the process should wait *after* the gateway
    assertThat(managementService.createJobQuery().active().count()).isOne();

    testRule.executeAvailableJobs();

    // if the waiting job is executed there should be 2 user tasks
    TaskQuery taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.active().count()).isOne();

    // finish tasks
    List<Task> tasks = taskQuery.active().list();
    for(Task task : tasks) {
      taskService.complete(task.getId());
    }

    testRule.assertProcessEnded(pi.getProcessInstanceId());
  }

  @Deployment
  @Test
  void testAsyncAfterAndBeforeExclusiveGateway() {
    // start process instance with variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("flow", false);

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testExclusiveGateway", variables);

    // no listeners are fired:
    assertNotListenerStartInvoked(pi);
    assertNotListenerEndInvoked(pi);

    // we should wait *before* the gateway:
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // after executing the gateway:
    managementService.executeJob(job.getId());

    // the listeners are fired:
    assertListenerStartInvoked(pi);
    assertListenerEndInvoked(pi);

    // and we will wait *after* the gateway:
    assertThat(managementService.createJobQuery().active().count()).isOne();
  }

  /**
   * Test for CAM-2518: Fixes an issue that creates an infinite loop when using
   * asyncAfter together with an execution listener on sequence flow event "take".
   * So the only required assertion here is that the process executes successfully.
   */
  @Deployment
  @Test
  void testAsyncAfterWithExecutionListener() {
    // given an async after job and an execution listener on that task
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    assertNotListenerTakeInvoked(processInstance);

    // when the job is executed
    managementService.executeJob(job.getId());

    // then the process should advance and not recreate the job
    job = managementService.createJobQuery().singleResult();
    assertThat(job).isNull();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    assertListenerTakeInvoked(processInstance);
  }

  @Deployment
  @Test
  void testAsyncAfterOnParallelGatewayFork() {
    String configuration = PvmAtomicOperation.TRANSITION_NOTIFY_LISTENER_TAKE.getCanonicalName();
    String config1 = configuration + "$afterForkFlow1";
    String config2 = configuration + "$afterForkFlow2";

    runtimeService.startProcessInstanceByKey("process");

    // there are two jobs
    List<Job> jobs = managementService.createJobQuery().list();
    assertThat(jobs).hasSize(2);
    Job jobToExecute = fetchFirstJobByHandlerConfiguration(jobs, config1);
    assertThat(jobToExecute).isNotNull();
    managementService.executeJob(jobToExecute.getId());

    Task task1 = taskService.createTaskQuery().taskDefinitionKey("theTask1").singleResult();
    assertThat(task1).isNotNull();

    // there is one left
    jobs = managementService.createJobQuery().list();
    assertThat(jobs).hasSize(1);
    jobToExecute = fetchFirstJobByHandlerConfiguration(jobs, config2);
    managementService.executeJob(jobToExecute.getId());

    Task task2 = taskService.createTaskQuery().taskDefinitionKey("theTask2").singleResult();
    assertThat(task2).isNotNull();

    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
  }

  @Deployment
  @Test
  void testAsyncAfterParallelMultiInstanceWithServiceTask() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // listeners and behavior should be invoked by now
    assertListenerStartInvoked(pi);
    assertBehaviorInvoked(pi, 5);
    assertListenerEndInvoked(pi);

    // the process should wait *after* execute all service tasks
    testRule.executeAvailableJobs(1);

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testAsyncAfterServiceWrappedInParallelMultiInstance(){
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // listeners and behavior should be invoked by now
    assertListenerStartInvoked(pi);
    assertBehaviorInvoked(pi, 5);
    assertListenerEndInvoked(pi);

    // the process should wait *after* execute each service task wrapped in the multi-instance body
    assertThat(managementService.createJobQuery().count()).isEqualTo(5L);
    // execute all jobs - one for each service task
    testRule.executeAvailableJobs(5);

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testAsyncAfterServiceWrappedInSequentialMultiInstance(){
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // listeners and behavior should be invoked by now
    assertListenerStartInvoked(pi);
    assertBehaviorInvoked(pi, 1);
    assertListenerEndInvoked(pi);

    // the process should wait *after* execute each service task step-by-step
    assertThat(managementService.createJobQuery().count()).isOne();
    // execute all jobs - one for each service task wrapped in the multi-instance body
    testRule.executeAvailableJobs(5);

    // behavior should be invoked for each service task
    assertBehaviorInvoked(pi, 5);

    // the process should wait on user task after execute all service tasks
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Disabled("Expected 3 jobs to be created, but only 1 was created")
  @Test
  void testAsyncAfterOnParallelGatewayJoin() {
    String configuration = PvmAtomicOperation.ACTIVITY_END.getCanonicalName();

    runtimeService.startProcessInstanceByKey("process");

    // there are three jobs
    List<Job> jobs = managementService.createJobQuery().list();
    assertThat(jobs).hasSize(3);
    Job jobToExecute = fetchFirstJobByHandlerConfiguration(jobs, configuration);
    assertThat(jobToExecute).isNotNull();
    managementService.executeJob(jobToExecute.getId());

    // there are two jobs left
    jobs = managementService.createJobQuery().list();
    assertThat(jobs).hasSize(2);
    jobToExecute = fetchFirstJobByHandlerConfiguration(jobs, configuration);
    managementService.executeJob(jobToExecute.getId());

    // there is one job left
    jobToExecute = managementService.createJobQuery().singleResult();
    assertThat(jobToExecute).isNotNull();
    managementService.executeJob(jobToExecute.getId());

    // the process should stay in the user task
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
  }

  @Deployment
  @Test
  void testAsyncAfterBoundaryEvent() {
    // given process instance
    runtimeService.startProcessInstanceByKey("Process");

    // assume
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    // when we trigger the event
    runtimeService.correlateMessage("foo");

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNull();
  }

  @Deployment
  @Test
  void testAsyncBeforeBoundaryEvent() {
    // given process instance
    runtimeService.startProcessInstanceByKey("Process");

    // assume
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    // when we trigger the event
    runtimeService.correlateMessage("foo");

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNull();
  }

  @Test
  void testAsyncAfterErrorEvent() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("process")
      .startEvent()
      .serviceTask("servTask")
        .operatonClass(ThrowBpmnErrorDelegate.class)
      .boundaryEvent()
        .operatonAsyncAfter(true)
        .operatonFailedJobRetryTimeCycle("R10/PT10S")
        .errorEventDefinition()
        .errorEventDefinitionDone()
      .serviceTask()
        .operatonClass("foo")
      .endEvent()
      .moveToActivity("servTask")
      .endEvent().done();
   testRule.deploy(instance);

    runtimeService.startProcessInstanceByKey("process");

    Job job = managementService.createJobQuery().singleResult();

   // when job fails
    executeJobIgnoringException(managementService, job.getId());

    // then
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(9);
  }

  protected Job fetchFirstJobByHandlerConfiguration(List<Job> jobs, String configuration) {
    for (Job job : jobs) {
      JobEntity jobEntity = (JobEntity) job;
      String jobConfig = jobEntity.getJobHandlerConfigurationRaw();
      if (configuration.equals(jobConfig)) {
        return job;
      }
    }

    return null;
  }

  protected void assertListenerStartInvoked(Execution e) {
    assertThat((Boolean) runtimeService.getVariable(e.getId(), "listenerStartInvoked")).isTrue();
  }

  protected void assertListenerTakeInvoked(Execution e) {
    assertThat((Boolean) runtimeService.getVariable(e.getId(), "listenerTakeInvoked")).isTrue();
  }

  protected void assertListenerEndInvoked(Execution e) {
    assertThat((Boolean) runtimeService.getVariable(e.getId(), "listenerEndInvoked")).isTrue();
  }

  protected void assertBehaviorInvoked(Execution e) {
    assertThat((Boolean) runtimeService.getVariable(e.getId(), "behaviorInvoked")).isTrue();
  }

  private void assertBehaviorInvoked(ProcessInstance pi, int times) {
    Long behaviorInvoked = (Long) runtimeService.getVariable(pi.getId(), "behaviorInvoked");
    assertThat(behaviorInvoked).as("behavior was not invoked").isNotNull();
    assertThat(behaviorInvoked.intValue()).isEqualTo(times);

  }

  protected void assertNotListenerStartInvoked(Execution e) {
    assertThat(runtimeService.getVariable(e.getId(), "listenerStartInvoked")).isNull();
  }

  protected void assertNotListenerTakeInvoked(Execution e) {
    assertThat(runtimeService.getVariable(e.getId(), "listenerTakeInvoked")).isNull();
  }

  protected void assertNotListenerEndInvoked(Execution e) {
    assertThat(runtimeService.getVariable(e.getId(), "listenerEndInvoked")).isNull();
  }

  protected void assertNotBehaviorInvoked(Execution e) {
    assertThat(runtimeService.getVariable(e.getId(), "behaviorInvoked")).isNull();
  }

}
