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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.runtime.Job;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryCleanupJobPriorityRangeTest extends AbstractJobExecutorAcquireJobsTest {

  HistoryService historyService;
  long defaultHistoryCleanupJobPriority;
  boolean defaultIsJobExecutorAcquireByPriority;

  @BeforeEach
  void setup() {
    defaultHistoryCleanupJobPriority = configuration.getHistoryCleanupJobPriority();
    defaultIsJobExecutorAcquireByPriority = configuration.isJobExecutorAcquireByPriority();
  }

  @AfterEach
  void tearDown() {
    configuration.setHistoryCleanupJobPriority(defaultHistoryCleanupJobPriority);
    configuration.setJobExecutorAcquireByPriority(defaultIsJobExecutorAcquireByPriority);
    resetDatabase();
  }

  private void resetDatabase() {
    configuration.getCommandExecutorTxRequired().execute(commandContext -> {
      List<Job> jobs = historyService.findHistoryCleanupJobs();

      for (Job job : jobs) {
        commandContext.getJobManager().deleteJob((JobEntity) job);
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(job.getId());
      }
      commandContext.getMeterLogManager().deleteAll();

      return null;
    });
  }

  @Test
  void shouldSetConfiguredPriorityOnHistoryCleanupJob() {
    // given
    configuration.setHistoryCleanupJobPriority(10L);

    // when
    historyService.cleanUpHistoryAsync(true);
    List<Job> historyCleanupJobs = historyService.findHistoryCleanupJobs();

    // then
    assertThat(historyCleanupJobs).hasSize(1);
    assertThat(historyCleanupJobs.get(0).getPriority()).isEqualTo(10L);
  }

  @Test
  void shouldAcquireHistoryCleanupJobInPriorityRange() {
    // given
    configuration.setJobExecutorPriorityRangeMin(5L);
    configuration.setJobExecutorPriorityRangeMax(15L);
    configuration.setHistoryCleanupJobPriority(10L);

    // when
    historyService.cleanUpHistoryAsync(true);

    // then
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();
    assertThat(acquirableJobs).hasSize(1);
    assertThat(findJobById(acquirableJobs.get(0).getId()).getPriority()).isEqualTo(10L);
  }

  @Test
  void shouldNotAcquireHistoryCleanupJobOutsidePriorityRange() {
    // given
    configuration.setJobExecutorAcquireByPriority(true);
    configuration.setJobExecutorPriorityRangeMin(5L);
    configuration.setJobExecutorPriorityRangeMax(15L);
    configuration.setHistoryCleanupJobPriority(20L);

    // when
    historyService.cleanUpHistoryAsync(true);

    // then
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();
    assertThat(acquirableJobs).isEmpty();
    List<Job> historyCleanupJobs = historyService.findHistoryCleanupJobs();
    assertThat(historyCleanupJobs).hasSize(1);
    assertThat(historyCleanupJobs.get(0).getPriority()).isEqualTo(20L);
  }
}
