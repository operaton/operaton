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
package org.operaton.bpm.engine.test.bpmn.job;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class JobPrioritizationBpmnConstantValueTest {

  protected static final long EXPECTED_DEFAULT_PRIORITY = 0;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  RepositoryService repositoryService;
  ManagementService managementService;

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/oneTaskProcess.bpmn20.xml")
  @Test
  void testDefaultPrioritizationAsyncBefore() {
    // when
    runtimeService
      .createProcessInstanceByKey("oneTaskProcess")
      .startBeforeActivity("task1")
      .execute();

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getPriority()).isEqualTo(EXPECTED_DEFAULT_PRIORITY);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/oneTaskProcess.bpmn20.xml")
  @Test
  void testDefaultPrioritizationAsyncAfter() {
    // given
    runtimeService
      .createProcessInstanceByKey("oneTaskProcess")
      .startBeforeActivity("task1")
      .execute();

    // when
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getPriority()).isEqualTo(EXPECTED_DEFAULT_PRIORITY);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/oneTimerProcess.bpmn20.xml")
  @Test
  void testDefaultPrioritizationTimer() {
    // when
    runtimeService
      .createProcessInstanceByKey("oneTimerProcess")
      .startBeforeActivity("timer1")
      .execute();

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getPriority()).isEqualTo(EXPECTED_DEFAULT_PRIORITY);
  }

  @ParameterizedTest
  @CsvSource({
    "org/operaton/bpm/engine/test/bpmn/job/jobPrioProcess.bpmn20.xml, jobPrioProcess, task1, 10",
    "org/operaton/bpm/engine/test/bpmn/job/intermediateTimerJobPrioProcess.bpmn20.xml, intermediateTimerJobPrioProcess, timer1, 8",
    "org/operaton/bpm/engine/test/bpmn/job/jobPrioProcess.bpmn20.xml, jobPrioProcess, task2, 5",
    "org/operaton/bpm/engine/test/bpmn/job/intermediateTimerJobPrioProcess.bpmn20.xml, intermediateTimerJobPrioProcess, timer2, 4"
  })
  void testJobPrioritization(String bpmnResource, String processDefinitionKey, String startBeforeActivity, int expectedPriority) {
    // given
    testRule.deploy(bpmnResource);

    // when
    runtimeService
      .createProcessInstanceByKey(processDefinitionKey)
      .startBeforeActivity(startBeforeActivity)
      .execute();

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getPriority()).isEqualTo(expectedPriority);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/jobPrioProcess.bpmn20.xml")
  @Test
  void testProcessDefinitionPrioritizationAsyncAfter() {
    // given
    runtimeService
      .createProcessInstanceByKey("jobPrioProcess")
      .startBeforeActivity("task1")
      .execute();

    // when
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getPriority()).isEqualTo(10);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/jobPrioProcess.bpmn20.xml")
  @Test
  void testActivityPrioritizationAsyncAfter() {
    // given
    runtimeService
      .createProcessInstanceByKey("jobPrioProcess")
      .startBeforeActivity("task2")
      .execute();

    // when
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getPriority()).isEqualTo(5);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/subProcessJobPrioProcess.bpmn20.xml")
  @Test
  void testSubProcessPriorityIsNotDefaultForContainedActivities() {
    // when starting an activity contained in the sub process where the
    // sub process has job priority 20
    runtimeService
      .createProcessInstanceByKey("subProcessJobPrioProcess")
      .startBeforeActivity("task1")
      .execute();

    // then the job for that activity has priority 10 which is the process definition's
    // priority; the sub process priority is not inherited
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job.getPriority()).isEqualTo(10);
  }

  @Test
  void testFailOnMalformedInput() {
    // given
    var deploymentBuilder = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/job/invalidPrioProcess.bpmn20.xml");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ParseException.class)
      .satisfies(e -> {
        ParseException parseException = (ParseException) e;
        assertThat(parseException.getMessage()).containsIgnoringCase("value 'thisIsNotANumber' for attribute 'jobPriority' "
            + "is not a valid number");
        assertThat(parseException.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("task2");
      });
  }

  @Test
  void testParsePriorityOnNonAsyncActivity() {
    // deploying a process definition where the activity
    // has a priority but defines no jobs succeeds
    var deploymentBuilder = repositoryService.createDeployment().addClasspathResource("org/operaton/bpm/engine/test/bpmn/job/JobPrioritizationBpmnTest.testParsePriorityOnNonAsyncActivity.bpmn20.xml");
    assertThatCode(() -> engineRule.manageDeployment(deploymentBuilder.deploy())).doesNotThrowAnyException();
  }

  @Test
  void testTimerStartEventPriorityOnProcessDefinition() {
    // given a timer start job
    var deployment = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/job/JobPrioritizationBpmnConstantValueTest.testTimerStartEventPriorityOnProcessDefinition.bpmn20.xml")
        .deploy();

    Job job = managementService.createJobQuery().singleResult();

    // then the timer start job has the priority defined in the process definition
    assertThat(job.getPriority()).isEqualTo(8);

    // cleanup
    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Test
  void testTimerStartEventPriorityOnActivity() {
    // given a timer start job
    var deployment = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/job/JobPrioritizationBpmnConstantValueTest.testTimerStartEventPriorityOnActivity.bpmn20.xml")
        .deploy();

    Job job = managementService.createJobQuery().singleResult();

    // then the timer start job has the priority defined in the process definition
    assertThat(job.getPriority()).isEqualTo(1515);

    // cleanup
    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/boundaryTimerJobPrioProcess.bpmn20.xml")
  @Test
  void testBoundaryTimerEventPriority() {
    // given an active boundary event timer
    runtimeService.startProcessInstanceByKey("boundaryTimerJobPrioProcess");

    Job job = managementService.createJobQuery().singleResult();

    // then the job has the priority specified in the BPMN XML
    assertThat(job.getPriority()).isEqualTo(20);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/eventSubprocessTimerJobPrioProcess.bpmn20.xml")
  @Test
  void testEventSubprocessTimerPriority() {
    // given an active event subprocess timer
    runtimeService.startProcessInstanceByKey("eventSubprocessTimerJobPrioProcess");

    Job job = managementService.createJobQuery().singleResult();

    // then the job has the priority specified in the BPMN XML
    assertThat(job.getPriority()).isEqualTo(25);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/job/intermediateSignalAsyncProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/job/intermediateSignalCatchJobPrioProcess.bpmn20.xml"})
  @Test
  void testAsyncSignalThrowingEventActivityPriority() {
    // given a receiving process instance with two subscriptions
    runtimeService.startProcessInstanceByKey("intermediateSignalCatchJobPrioProcess");

    // and a process instance that executes an async signal throwing event
    runtimeService.startProcessInstanceByKey("intermediateSignalJobPrioProcess");

    Execution signal1Execution = runtimeService.createExecutionQuery().activityId("signal1").singleResult();
    Job signal1Job = managementService.createJobQuery().executionId(signal1Execution.getId()).singleResult();

    Execution signal2Execution = runtimeService.createExecutionQuery().activityId("signal2").singleResult();
    Job signal2Job = managementService.createJobQuery().executionId(signal2Execution.getId()).singleResult();

    // then the jobs have the priority as specified for the receiving events, not the throwing
    assertThat(signal1Job.getPriority()).isEqualTo(8);
    assertThat(signal2Job.getPriority()).isEqualTo(4);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/job/intermediateSignalAsyncProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/job/signalStartJobPrioProcess.bpmn20.xml"})
  @Test
  void testAsyncSignalThrowingEventSignalStartActivityPriority() {
    // given a process instance that executes an async signal throwing event
    runtimeService.startProcessInstanceByKey("intermediateSignalJobPrioProcess");

    // then there is an async job for the signal start event with the priority defined in the BPMN XML
    assertThat(managementService.createJobQuery().count()).isOne();
    Job signalStartJob = managementService.createJobQuery().singleResult();
    assertThat(signalStartJob).isNotNull();
    assertThat(signalStartJob.getPriority()).isEqualTo(4);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/miBodyAsyncProcess.bpmn20.xml")
  @Test
  @Disabled("Fixme: Expected priority does not match")
  void testMultiInstanceBodyActivityPriority() {
    // given a process instance that executes an async mi body
    runtimeService.startProcessInstanceByKey("miBodyAsyncPriorityProcess");

    // then there is a job that has the priority as defined on the activity
    assertThat(managementService.createJobQuery().count()).isOne();
    Job miBodyJob = managementService.createJobQuery().singleResult();
    assertThat(miBodyJob).isNotNull();
    assertThat(miBodyJob.getPriority()).isEqualTo(5);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/miInnerAsyncProcess.bpmn20.xml")
  @Test
  void testMultiInstanceInnerActivityPriority() {
    // given a process instance that executes an async mi inner activity
    runtimeService.startProcessInstanceByKey("miBodyAsyncPriorityProcess");

    // then there are three jobs that have the priority as defined on the activity
    List<Job> jobs = managementService.createJobQuery().list();

    assertThat(jobs).hasSize(3);
    for (Job job : jobs) {
      assertThat(job).isNotNull();
      assertThat(job.getPriority()).isEqualTo(5);
    }
  }
}
