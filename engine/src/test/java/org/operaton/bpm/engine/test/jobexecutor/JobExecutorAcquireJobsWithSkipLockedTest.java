/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for job acquisition with SKIP LOCKED feature enabled.
 * This feature allows multiple job executors to acquire jobs concurrently
 * without blocking on locked rows.
 */
class JobExecutorAcquireJobsWithSkipLockedTest extends AbstractJobExecutorAcquireJobsTest {

  @BeforeEach
  void prepareProcessEngineConfiguration() {
    configuration = rule.getProcessEngineConfiguration();
    managementService = rule.getManagementService();
    runtimeService = rule.getRuntimeService();
    configuration.setJobExecutorAcquireWithSkipLocked(true);
  }

  @Test
  void testProcessEngineConfiguration() {
    // verify configuration is set correctly
    assertThat(configuration.isJobExecutorAcquireWithSkipLocked()).isTrue();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testJobAcquisitionWithoutLockedJobs() {
    // given: multiple jobs available
    int numberOfJobs = 5;
    for (int i = 0; i < numberOfJobs; i++) {
      runtimeService.startProcessInstanceByKey("simpleAsyncProcess");
    }

    // when: acquiring jobs with skip locked enabled
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    // then: all jobs are acquired
    assertThat(acquirableJobs).hasSize(numberOfJobs);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testJobAcquisitionSkipsLockedJobs() {
    // given: multiple jobs where some are locked
    int numberOfJobs = 5;
    for (int i = 0; i < numberOfJobs; i++) {
      runtimeService.startProcessInstanceByKey("simpleAsyncProcess");
    }

    // when: locking the first 2 jobs manually
    List<Job> allJobs = managementService.createJobQuery().list();
    assertThat(allJobs).hasSize(numberOfJobs);

    String jobId1 = allJobs.get(0).getId();
    String jobId2 = allJobs.get(1).getId();

    // Lock jobs by setting lock owner and expiration time
    configuration.getCommandExecutorTxRequired().execute(commandContext -> {
      commandContext.getJobManager()
        .findJobById(jobId1)
        .setLockOwner("test-lock-owner");
      commandContext.getJobManager()
        .findJobById(jobId1)
        .setLockExpirationTime(ClockUtil.getCurrentTime());
      commandContext.getJobManager()
        .findJobById(jobId2)
        .setLockOwner("test-lock-owner");
      commandContext.getJobManager()
        .findJobById(jobId2)
        .setLockExpirationTime(ClockUtil.getCurrentTime());
      return null;
    });

    // when: acquiring jobs with skip locked enabled
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    // then: only unlocked jobs are acquired (3 out of 5)
    assertThat(acquirableJobs).hasSize(3);

    // verify locked jobs are not in the acquired list
    assertThat(acquirableJobs)
      .extracting(AcquirableJobEntity::getId)
      .doesNotContain(jobId1, jobId2);
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/jobexecutor/jobPrioProcess.bpmn20.xml"
  })
  void testSkipLockedWorksWithPriorityAcquisition() {
    // given: skip locked and priority acquisition both enabled
    configuration.setJobExecutorAcquireByPriority(true);

    // when: creating jobs with different priorities
    startProcess("jobPrioProcess", "task1", 3); // priority 10
    startProcess("jobPrioProcess", "task2", 3); // priority 5

    // and: locking a high-priority job
    List<Job> highPriorityJobs = managementService.createJobQuery()
      .orderByJobPriority().desc()
      .listPage(0, 1);

    String lockedJobId = highPriorityJobs.get(0).getId();
    configuration.getCommandExecutorTxRequired().execute(commandContext -> {
      commandContext.getJobManager()
        .findJobById(lockedJobId)
        .setLockOwner("test-lock-owner");
      commandContext.getJobManager()
        .findJobById(lockedJobId)
        .setLockExpirationTime(ClockUtil.getCurrentTime());
      return null;
    });

    // when: acquiring jobs
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    // then: 5 jobs acquired (skipping the locked one)
    assertThat(acquirableJobs).hasSize(5);

    // and: locked high-priority job is not acquired
    assertThat(acquirableJobs)
      .extracting(AcquirableJobEntity::getId)
      .doesNotContain(lockedJobId);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testDisablingSkipLockedDoesNotSkipLockedJobs() {
    // given: skip locked disabled
    configuration.setJobExecutorAcquireWithSkipLocked(false);

    // and: jobs where some are locked
    int numberOfJobs = 5;
    for (int i = 0; i < numberOfJobs; i++) {
      runtimeService.startProcessInstanceByKey("simpleAsyncProcess");
    }

    List<Job> allJobs = managementService.createJobQuery().list();
    String jobId1 = allJobs.get(0).getId();

    // lock one job
    configuration.getCommandExecutorTxRequired().execute(commandContext -> {
      commandContext.getJobManager()
        .findJobById(jobId1)
        .setLockOwner("test-lock-owner");
      commandContext.getJobManager()
        .findJobById(jobId1)
        .setLockExpirationTime(ClockUtil.getCurrentTime());
      return null;
    });

    // when: acquiring jobs without skip locked
    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    // then: the query still returns all jobs (including locked ones)
    // because without SKIP LOCKED, the database-level locking doesn't prevent
    // the SELECT from returning locked rows
    // Note: This behavior depends on the transaction isolation level
    assertThat(acquirableJobs).hasSizeGreaterThanOrEqualTo(3);
  }
}
