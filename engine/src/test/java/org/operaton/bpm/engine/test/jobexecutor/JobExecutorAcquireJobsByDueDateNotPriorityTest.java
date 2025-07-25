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
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;

class JobExecutorAcquireJobsByDueDateNotPriorityTest extends AbstractJobExecutorAcquireJobsTest {

  @BeforeEach
  void prepareProcessEngineConfiguration() {
    configuration.setJobExecutorAcquireByDueDate(true);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/jobPrioProcess.bpmn20.xml")
  void testJobPriorityIsNotConsidered() {
    // prio 5
    String instance1 = startProcess("jobPrioProcess", "task2");

    // prio 10
    incrementClock(1);
    String instance2 = startProcess("jobPrioProcess", "task1");

    // prio 5
    incrementClock(1);
    String instance3 = startProcess("jobPrioProcess", "task2");

    // prio 10
    incrementClock(1);
    String instance4 = startProcess("jobPrioProcess", "task1");

    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();
    assertThat(acquirableJobs).hasSize(4);

    assertThat((int) findJobById(acquirableJobs.get(0).getId()).getPriority()).isEqualTo(5);
    assertThat(acquirableJobs.get(0).getProcessInstanceId()).isEqualTo(instance1);
    assertThat((int) findJobById(acquirableJobs.get(1).getId()).getPriority()).isEqualTo(10);
    assertThat(acquirableJobs.get(1).getProcessInstanceId()).isEqualTo(instance2);
    assertThat((int) findJobById(acquirableJobs.get(2).getId()).getPriority()).isEqualTo(5);
    assertThat(acquirableJobs.get(2).getProcessInstanceId()).isEqualTo(instance3);
    assertThat((int) findJobById(acquirableJobs.get(3).getId()).getPriority()).isEqualTo(10);
    assertThat(acquirableJobs.get(3).getProcessInstanceId()).isEqualTo(instance4);
  }


  @Override
  protected Job findJobById(String id) {
    return managementService.createJobQuery().jobId(id).singleResult();
  }

}
