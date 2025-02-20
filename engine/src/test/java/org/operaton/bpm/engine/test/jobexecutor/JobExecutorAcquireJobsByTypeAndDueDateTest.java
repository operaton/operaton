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
package org.operaton.bpm.engine.test.jobexecutor;

import static org.operaton.bpm.engine.test.util.ClockTestUtil.incrementClock;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.junit.Before;
import org.junit.Test;

public class JobExecutorAcquireJobsByTypeAndDueDateTest extends AbstractJobExecutorAcquireJobsTest {

  @Before
  public void prepareProcessEngineConfiguration() {
    configuration.setJobExecutorPreferTimerJobs(true);
    configuration.setJobExecutorAcquireByDueDate(true);
  }

  @Test
  public void testProcessEngineConfiguration() {
    assertThat(configuration.isJobExecutorPreferTimerJobs()).isTrue();
    assertThat(configuration.isJobExecutorAcquireByDueDate()).isTrue();
    assertThat(configuration.isJobExecutorAcquireByPriority()).isFalse();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  public void testMessageJobHasDueDateSet() {
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job.getDuedate()).isNotNull();
    assertThat(job.getDuedate()).isEqualTo(ClockUtil.getCurrentTime());
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/jobexecutor/processWithTimerCatch.bpmn20.xml"
  })
  public void testTimerAndOldJobsArePreferred() {
    // first start process with timer job
    ProcessInstance timerProcess1 = runtimeService.startProcessInstanceByKey("testProcess");
    // then start process with async task
    incrementClock(1);
    ProcessInstance asyncProcess1 = runtimeService.startProcessInstanceByKey("simpleAsyncProcess");
    // then start process with timer job
    incrementClock(1);
    ProcessInstance timerProcess2 = runtimeService.startProcessInstanceByKey("testProcess");
    // and another process with async task after the timers are acquirable
    incrementClock(61);
    ProcessInstance asyncProcess2 = runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    Job timerJob1 = managementService.createJobQuery().processInstanceId(timerProcess1.getId()).singleResult();
    Job timerJob2 = managementService.createJobQuery().processInstanceId(timerProcess2.getId()).singleResult();
    Job messageJob1 = managementService.createJobQuery().processInstanceId(asyncProcess1.getId()).singleResult();
    Job messageJob2 = managementService.createJobQuery().processInstanceId(asyncProcess2.getId()).singleResult();

    assertThat(timerJob1.getDuedate()).isNotNull();
    assertThat(timerJob2.getDuedate()).isNotNull();
    assertThat(messageJob1.getDuedate()).isNotNull();
    assertThat(messageJob2.getDuedate()).isNotNull();

    assertThat(messageJob1.getDuedate().before(timerJob1.getDuedate())).isTrue();
    assertThat(timerJob1.getDuedate().before(timerJob2.getDuedate())).isTrue();
    assertThat(timerJob2.getDuedate().before(messageJob2.getDuedate())).isTrue();

    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();
    assertThat(acquirableJobs).hasSize(4);
    assertThat(acquirableJobs.get(0).getId()).isEqualTo(timerJob1.getId());
    assertThat(acquirableJobs.get(1).getId()).isEqualTo(timerJob2.getId());
    assertThat(acquirableJobs.get(2).getId()).isEqualTo(messageJob1.getId());
    assertThat(acquirableJobs.get(3).getId()).isEqualTo(messageJob2.getId());
  }
}
