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

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.util.ClockTestUtil.incrementClock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.MessageEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TimerEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;

class JobExecutorAcquireJobsByTypeTest extends AbstractJobExecutorAcquireJobsTest {

  @BeforeEach
  void prepareProcessEngineConfiguration() {
    configuration.setJobExecutorPreferTimerJobs(true);
  }

  @Test
  void testProcessEngineConfiguration() {
    assertThat(configuration.isJobExecutorPreferTimerJobs()).isTrue();
    assertThat(configuration.isJobExecutorAcquireByDueDate()).isFalse();
    assertThat(configuration.isJobExecutorAcquireByPriority()).isFalse();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testMessageJobHasNoDueDateSet() {
    configuration.setEnsureJobDueDateNotNull(false);

    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job.getDuedate()).isNull();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testMessageJobHasDueDateSet() {
    configuration.setEnsureJobDueDateNotNull(true);

    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    Job job = managementService.createJobQuery().singleResult();

    // time is fixed for the purposes of the test
    assertThat(job.getDuedate()).isEqualTo(ClockUtil.getCurrentTime());
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/jobexecutor/processWithTimerCatch.bpmn20.xml"
  })
  void testTimerJobsArePreferred() {
    // first start process with timer job
    runtimeService.startProcessInstanceByKey("testProcess");
    // then start process with async task
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");
    // then start process with timer job
    runtimeService.startProcessInstanceByKey("testProcess");
    // and another process with async task
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    // increment clock so that timer events are acquirable
    incrementClock(70);

    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();
    assertThat(acquirableJobs).hasSize(4);
    assertThat(findJobById(acquirableJobs.get(0).getId())).isInstanceOf(TimerEntity.class);
    assertThat(findJobById(acquirableJobs.get(1).getId())).isInstanceOf(TimerEntity.class);
    assertThat(findJobById(acquirableJobs.get(2).getId())).isInstanceOf(MessageEntity.class);
    assertThat(findJobById(acquirableJobs.get(3).getId())).isInstanceOf(MessageEntity.class);
  }

  @Override
  protected Job findJobById(String id) {
    return managementService.createJobQuery().jobId(id).singleResult();
  }

}
