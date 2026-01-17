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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.cmd.AcquireJobsCmd;
import org.operaton.bpm.engine.impl.db.entitymanager.DbEntityManager;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.jobexecutor.AcquiredJobs;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AcquireJobCmdUnitTest {

  protected static final String PROCESS_INSTANCE_ID_1 = "pi_1";
  protected static final String PROCESS_INSTANCE_ID_2 = "pi_2";

  protected static final String JOB_ID_1 = "job_1";
  protected static final String JOB_ID_2 = "job_2";

  protected AcquireJobsCmd acquireJobsCmd;
  protected JobManager jobManager;
  protected CommandContext commandContext;

  @BeforeEach
  void initCommand() {
    JobExecutor jobExecutor = mock(JobExecutor.class);
    when(jobExecutor.getMaxJobsPerAcquisition()).thenReturn(3);
    when(jobExecutor.getLockOwner()).thenReturn("test");
    when(jobExecutor.getLockTimeInMillis()).thenReturn(5 * 60 * 1000);

    acquireJobsCmd = new AcquireJobsCmd(jobExecutor);

    commandContext = mock(CommandContext.class);

    DbEntityManager dbEntityManager = mock(DbEntityManager.class);
    when(commandContext.getDbEntityManager()).thenReturn(dbEntityManager);

    jobManager = mock(JobManager.class);
    when(commandContext.getJobManager()).thenReturn(jobManager);
  }

  @Test
  void nonExclusiveJobsSameInstance() {
    // given: two non-exclusive jobs for a different process instance
    AcquirableJobEntity job1 = createNonExclusiveJob(JOB_ID_1, PROCESS_INSTANCE_ID_1);
    AcquirableJobEntity job2 = createNonExclusiveJob(JOB_ID_2, PROCESS_INSTANCE_ID_1);

    // when the job executor acquire new jobs
    when(jobManager.findNextJobsToExecute(any(Page.class))).thenReturn(List.of(job1, job2));

    // then the job executor should acquire job1 and job 2 in different batches
    checkThatAcquiredJobsInDifferentBatches();
  }

  @Test
  void nonExclusiveDifferentInstance() {
    // given: two non-exclusive jobs for the same process instance
    AcquirableJobEntity job1 = createNonExclusiveJob(JOB_ID_1, PROCESS_INSTANCE_ID_1);
    AcquirableJobEntity job2 = createNonExclusiveJob(JOB_ID_2, PROCESS_INSTANCE_ID_2);

    // when the job executor acquire new jobs
    when(jobManager.findNextJobsToExecute(any(Page.class))).thenReturn(List.of(job1, job2));

    // then the job executor should acquire job1 and job 2 in different batches
    checkThatAcquiredJobsInDifferentBatches();
  }

  @Test
  void exclusiveJobsSameInstance() {
    // given: two exclusive jobs for the same process instance
    AcquirableJobEntity job1 = createExclusiveJob(JOB_ID_1, PROCESS_INSTANCE_ID_1);
    AcquirableJobEntity job2 = createExclusiveJob(JOB_ID_2, PROCESS_INSTANCE_ID_1);

    // when the job executor acquire new jobs
    when(jobManager.findNextJobsToExecute(any(Page.class))).thenReturn(List.of(job1, job2));

    // then the job executor should acquire job1 and job 2 in one batch
    AcquiredJobs acquiredJobs = acquireJobsCmd.execute(commandContext);

    List<List<String>> jobIdBatches = acquiredJobs.getJobIdBatches();
    assertThat(jobIdBatches).hasSize(1);
    assertThat(jobIdBatches.get(0)).hasSize(2);
    assertThat(jobIdBatches.get(0)).containsExactlyInAnyOrder(JOB_ID_1, JOB_ID_2);
  }

  @Test
  void exclusiveJobsDifferentInstance() {
    // given: two exclusive jobs for a different process instance
    AcquirableJobEntity job1 = createExclusiveJob(JOB_ID_1, PROCESS_INSTANCE_ID_1);
    AcquirableJobEntity job2 = createExclusiveJob(JOB_ID_2, PROCESS_INSTANCE_ID_2);

    // when the job executor acquire new jobs
    when(jobManager.findNextJobsToExecute(any(Page.class))).thenReturn(List.of(job1, job2));

    // then the job executor should acquire job1 and job 2 in different batches
    checkThatAcquiredJobsInDifferentBatches();
  }

  protected AcquirableJobEntity createExclusiveJob(String id, String processInstanceId) {
    AcquirableJobEntity job = createNonExclusiveJob(id, processInstanceId);
    when(job.isExclusive()).thenReturn(true);
    return job;
  }

  protected AcquirableJobEntity createNonExclusiveJob(String id, String processInstanceId) {
    AcquirableJobEntity job = mock(AcquirableJobEntity.class);
    when(job.getId()).thenReturn(id);
    when(job.getProcessInstanceId()).thenReturn(processInstanceId);
    return job;
  }

  protected void checkThatAcquiredJobsInDifferentBatches() {
    AcquiredJobs acquiredJobs = acquireJobsCmd.execute(commandContext);

    List<List<String>> jobIdBatches = acquiredJobs.getJobIdBatches();
    assertThat(jobIdBatches).hasSize(2);
    assertThat(jobIdBatches.get(0)).hasSize(1);
    assertThat(jobIdBatches.get(1)).hasSize(1);
  }

}
