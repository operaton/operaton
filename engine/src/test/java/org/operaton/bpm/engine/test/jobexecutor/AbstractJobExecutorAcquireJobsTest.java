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
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.util.ClockTestUtil;

public abstract class AbstractJobExecutorAcquireJobsTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();

  protected ManagementService managementService;
  protected RuntimeService runtimeService;

  protected ProcessEngineConfigurationImpl configuration;

  private boolean jobExecutorAcquireByDueDate;
  private boolean jobExecutorAcquireByPriority;
  private boolean jobExecutorAcquireWithSkipLocked;
  private boolean jobExecutorPreferTimerJobs;
  private boolean jobEnsureDueDateSet;
  private Long jobExecutorPriorityRangeMin;
  private Long jobExecutorPriorityRangeMax;

  @BeforeEach
  public void saveProcessEngineConfiguration() {
    jobExecutorAcquireByDueDate = configuration.isJobExecutorAcquireByDueDate();
    jobExecutorAcquireByPriority = configuration.isJobExecutorAcquireByPriority();
    jobExecutorAcquireWithSkipLocked = configuration.isJobExecutorAcquireWithSkipLocked();
    jobExecutorPreferTimerJobs = configuration.isJobExecutorPreferTimerJobs();
    jobEnsureDueDateSet = configuration.isEnsureJobDueDateNotNull();
    jobExecutorPriorityRangeMin = configuration.getJobExecutorPriorityRangeMin();
    jobExecutorPriorityRangeMax = configuration.getJobExecutorPriorityRangeMax();
  }

  @BeforeEach
  public void setClock() {
    ClockTestUtil.setClockToDateWithoutMilliseconds();
  }

  @AfterEach
  public void restoreProcessEngineConfiguration() {
    configuration.setJobExecutorAcquireByDueDate(jobExecutorAcquireByDueDate);
    configuration.setJobExecutorAcquireByPriority(jobExecutorAcquireByPriority);
    configuration.setJobExecutorAcquireWithSkipLocked(jobExecutorAcquireWithSkipLocked);
    configuration.setJobExecutorPreferTimerJobs(jobExecutorPreferTimerJobs);
    configuration.setEnsureJobDueDateNotNull(jobEnsureDueDateSet);
    configuration.setJobExecutorPriorityRangeMin(jobExecutorPriorityRangeMin);
    configuration.setJobExecutorPriorityRangeMax(jobExecutorPriorityRangeMax);
  }

  @AfterEach
  public void resetClock() {
    ClockUtil.reset();
  }

  protected List<AcquirableJobEntity> findAcquirableJobs() {
    return configuration.getCommandExecutorTxRequired()
            .execute(commandContext -> commandContext
            .getJobManager()
            .findNextJobsToExecute(new Page(0, 100)));
  }

  protected String startProcess(String processDefinitionKey, String activity) {
    return runtimeService
      .createProcessInstanceByKey(processDefinitionKey)
      .startBeforeActivity(activity)
      .execute().getId();
  }

  protected void startProcess(String processDefinitionKey, String activity, int times) {
    for (int i = 0; i < times; i++) {
      startProcess(processDefinitionKey, activity);
    }
  }

  protected Job findJobById(String id) {
    return managementService.createJobQuery().jobId(id).singleResult();
  }

}
