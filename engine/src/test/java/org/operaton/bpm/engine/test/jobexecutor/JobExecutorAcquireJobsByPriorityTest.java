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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.test.Deployment;
import org.junit.Before;
import org.junit.Test;

public class JobExecutorAcquireJobsByPriorityTest extends AbstractJobExecutorAcquireJobsTest {

  @Before
  public void prepareProcessEngineConfiguration() {
    configuration.setJobExecutorAcquireByPriority(true);
  }

  @Test
  public void testProcessEngineConfiguration() {
    assertFalse(configuration.isJobExecutorPreferTimerJobs());
    assertFalse(configuration.isJobExecutorAcquireByDueDate());
    assertTrue(configuration.isJobExecutorAcquireByPriority());
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/jobexecutor/jobPrioProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/jobexecutor/timerJobPrioProcess.bpmn20.xml"
  })
  public void testAcquisitionByPriority() {
    // jobs with priority 10
    startProcess("jobPrioProcess", "task1", 5);

    // jobs with priority 5
    startProcess("jobPrioProcess", "task2", 5);

    // jobs with priority 8
    startProcess("timerJobPrioProcess", "timer1", 5);

    // jobs with priority 4
    startProcess("timerJobPrioProcess", "timer2", 5);

    // make timers due
    incrementClock(61);

    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();
    assertThat(acquirableJobs).hasSize(20);
    for (int i = 0; i < 5; i++) {
      assertThat(findJobById(acquirableJobs.get(i).getId()).getPriority()).isEqualTo(10);
    }

    for (int i = 5; i < 10; i++) {
      assertThat(findJobById(acquirableJobs.get(i).getId()).getPriority()).isEqualTo(8);
    }

    for (int i = 10; i < 15; i++) {
      assertThat(findJobById(acquirableJobs.get(i).getId()).getPriority()).isEqualTo(5);
    }

    for (int i = 15; i < 20; i++) {
      assertThat(findJobById(acquirableJobs.get(i).getId()).getPriority()).isEqualTo(4);
    }
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/jobPrioProcess.bpmn20.xml")
  public void testMixedPriorityAcquisition() {
    // jobs with priority 10
    startProcess("jobPrioProcess", "task1", 5);

    // jobs with priority 5
    startProcess("jobPrioProcess", "task2", 5);

    // set some job priorities to NULL indicating that they were produced without priorities
  }

}
