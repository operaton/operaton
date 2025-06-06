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

import static org.operaton.bpm.engine.test.util.ClockTestUtil.incrementClock;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.test.Deployment;
import org.junit.Before;
import org.junit.Test;

public class JobExecutorAcquireJobsByPriorityAndDueDateTest extends AbstractJobExecutorAcquireJobsTest {

  @Before
  public void prepareProcessEngineConfiguration() {
    configuration.setJobExecutorAcquireByPriority(true);
    configuration.setJobExecutorAcquireByDueDate(true);
  }

  @Test
  public void testProcessEngineConfiguration() {
    assertThat(configuration.isJobExecutorPreferTimerJobs()).isFalse();
    assertThat(configuration.isJobExecutorAcquireByDueDate()).isTrue();
    assertThat(configuration.isJobExecutorAcquireByPriority()).isTrue();
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/jobexecutor/jobPrioProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/jobexecutor/timerJobPrioProcess.bpmn20.xml"
  })
  public void testAcquisitionByPriorityAndDueDate() {
    // job with priority 10
    String instance1 = startProcess("jobPrioProcess", "task1");

    // job with priority 5
    incrementClock(1);
    String instance2 = startProcess("jobPrioProcess", "task2");

    // job with priority 10
    incrementClock(1);
    String instance3 = startProcess("jobPrioProcess", "task1");

    // job with priority 5
    incrementClock(1);
    String instance4 = startProcess("jobPrioProcess", "task2");

    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();
    assertThat(acquirableJobs).hasSize(4);
    assertThat(acquirableJobs.get(0).getProcessInstanceId()).isEqualTo(instance1);
    assertThat(acquirableJobs.get(1).getProcessInstanceId()).isEqualTo(instance3);
    assertThat(acquirableJobs.get(2).getProcessInstanceId()).isEqualTo(instance2);
    assertThat(acquirableJobs.get(3).getProcessInstanceId()).isEqualTo(instance4);
  }

}
