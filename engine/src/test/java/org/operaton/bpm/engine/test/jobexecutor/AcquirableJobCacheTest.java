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
package org.operaton.bpm.engine.test.jobexecutor;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobManager;
import org.operaton.bpm.engine.impl.persistence.entity.TimerEntity;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcquirableJobCacheTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ManagementService managementService;
  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/metrics/asyncServiceTaskProcess.bpmn20.xml")
  void testFetchJobEntityWhenAcquirableJobIsCached() {
    // given
    runtimeService.startProcessInstanceByKey("asyncServiceTaskProcess");

    // when/then
    assertThatThrownBy(this::fetchJobAfterCachedAcquirableJob)
      .isInstanceOf(ProcessEngineException.class)
      .satisfies(e -> assertThat(e.getMessage())
          .contains("Could not lookup entity of type")
          .contains(AcquirableJobEntity.class.getSimpleName())
          .contains(JobEntity.class.getSimpleName()));
  }

  @Test
  void testFetchTimerEntityWhenAcquirableJobIsCached() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("startTimer")
        .startEvent()
        .userTask("userTask")
          .boundaryEvent()
          .timerWithDate("2016-02-11T12:13:14Z")
        .done();
    testRule.deploy(process);
    runtimeService.startProcessInstanceByKey("startTimer");
    Execution execution = runtimeService.createExecutionQuery().activityId("userTask").singleResult();
    var executionId = execution.getId();

    // when/then
    assertThatThrownBy(() -> fetchTimerJobAfterCachedAcquirableJob(executionId))
      .isInstanceOf(ProcessEngineException.class)
      .satisfies(e -> assertThat(e.getMessage())
          .contains("Could not lookup entity of type")
          .contains(TimerEntity.class.getSimpleName())
          .contains(AcquirableJobEntity.class.getSimpleName()));
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/metrics/asyncServiceTaskProcess.bpmn20.xml")
  void testFetchAcquirableJobWhenJobEntityIsCached() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncServiceTaskProcess");

    // when
    AcquirableJobEntity job = fetchAcquirableJobAfterCachedJob(processInstance.getId());

    // then
    assertThat(job).isNotNull();
    assertThat(job.getProcessInstanceId()).isEqualTo(processInstance.getId());
  }

  @Test
  void testFetchAcquirableJobWhenTimerEntityIsCached() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("timer")
      .startEvent()
      .userTask("userTask")
        .boundaryEvent()
        .timerWithDate("2016-02-11T12:13:14Z")
      .done();
    testRule.deploy(process);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timer");
    Execution execution = runtimeService.createExecutionQuery().activityId("userTask").singleResult();

    // when
    AcquirableJobEntity job = fetchAcquirableJobAfterCachedTimerEntity(execution.getId());

    // then
    assertThat(job).isNotNull();
    assertThat(job.getProcessInstanceId()).isEqualTo(processInstance.getId());
  }

  protected JobEntity fetchJobAfterCachedAcquirableJob() {
    return processEngineConfiguration.getCommandExecutorTxRequiresNew().execute(commandContext -> {
      JobManager jobManager = commandContext.getJobManager();
      List<AcquirableJobEntity> acquirableJobs = jobManager.findNextJobsToExecute(new Page(0, 100));
      return jobManager.findJobById(acquirableJobs.get(0).getId());
    });
  }

  protected TimerEntity fetchTimerJobAfterCachedAcquirableJob(final String executionId) {
    return processEngineConfiguration.getCommandExecutorTxRequiresNew().execute(commandContext -> {
      JobManager jobManager = commandContext.getJobManager();
      jobManager.findNextJobsToExecute(new Page(0, 100));
      List<TimerEntity> timerJobs = jobManager.findTimersByExecutionId(executionId);
      return timerJobs.get(0);
    });
  }

  protected AcquirableJobEntity fetchAcquirableJobAfterCachedTimerEntity(final String executionId) {
    return processEngineConfiguration.getCommandExecutorTxRequiresNew().execute(commandContext -> {
      JobManager jobManager = commandContext.getJobManager();
      jobManager.findTimersByExecutionId(executionId);
      List<AcquirableJobEntity> acquirableJob = jobManager.findNextJobsToExecute(new Page(0, 100));
      return acquirableJob.get(0);
    });
  }

  protected AcquirableJobEntity fetchAcquirableJobAfterCachedJob(final String processInstanceId) {
    return processEngineConfiguration.getCommandExecutorTxRequiresNew().execute(commandContext -> {
      JobManager jobManager = commandContext.getJobManager();
      jobManager.findJobsByProcessInstanceId(processInstanceId);
      List<AcquirableJobEntity> acquirableJobs = jobManager.findNextJobsToExecute(new Page(0, 100));
      return acquirableJobs.get(0);
    });
  }
}
