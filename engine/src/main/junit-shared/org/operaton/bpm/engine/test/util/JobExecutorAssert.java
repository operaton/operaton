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
package org.operaton.bpm.engine.test.util;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;

/**
 * Fluent assertion to wait for the {@link JobExecutor} to process jobs.
 * <p>
 * Defaults are aligned with {@link JobExecutorWaitUtils} so existing behaviour
 * can be used in a more expressive way.
 */
public final class JobExecutorAssert {

  private ProcessEngine processEngine;
  private ProcessEngineConfiguration processEngineConfiguration;
  private JobExecutor jobExecutor;
  private ManagementService managementService;
  private String processInstanceId;
  private long timeoutMillis = JobExecutorWaitUtils.JOBS_WAIT_TIMEOUT_MS;
  private long checkIntervalMillis = JobExecutorWaitUtils.CHECK_INTERVAL_MS;
  private boolean shutdownExecutorAfter = false;

  private JobExecutorAssert() {
  }

  public static JobExecutorAssert assertThatJobExecutor() {
    return new JobExecutorAssert();
  }

  public JobExecutorAssert withProcessEngine(ProcessEngine processEngine) {
    this.processEngine = processEngine;
    this.processEngineConfiguration = processEngine.getProcessEngineConfiguration();
    return this;
  }

  public JobExecutorAssert withProcessEngineConfiguration(ProcessEngineConfiguration configuration) {
    this.processEngineConfiguration = configuration;
    if (configuration instanceof ProcessEngineConfigurationImpl) {
      ProcessEngineConfigurationImpl impl = (ProcessEngineConfigurationImpl) configuration;
      this.jobExecutor = impl.getJobExecutor();
      this.managementService = impl.getManagementService();
    }
    return this;
  }

  public JobExecutorAssert withJobExecutor(JobExecutor jobExecutor) {
    this.jobExecutor = jobExecutor;
    return this;
  }

  public JobExecutorAssert withManagementService(ManagementService managementService) {
    this.managementService = managementService;
    return this;
  }

  public JobExecutorAssert withProcessInstance(String processInstanceId) {
    this.processInstanceId = processInstanceId;
    return this;
  }

  public JobExecutorAssert withTimeout(long timeoutMillis) {
    this.timeoutMillis = timeoutMillis;
    return this;
  }

  public JobExecutorAssert withCheckInterval(long checkIntervalMillis) {
    this.checkIntervalMillis = checkIntervalMillis;
    return this;
  }

  public JobExecutorAssert shutdownExecutorAfter(boolean shutdown) {
    this.shutdownExecutorAfter = shutdown;
    return this;
  }

  public void hasAllJobsProcessed() {
    resolveMissingConfiguration();

    boolean wasActive = jobExecutor.isActive();

    if (processInstanceId != null) {
      JobExecutorWaitUtils.waitForJobExecutorToProcessAllJobs(processInstanceId,
          timeoutMillis, checkIntervalMillis, jobExecutor, managementService);
    } else {
      JobExecutorWaitUtils.waitForJobExecutorToProcessAllJobs(timeoutMillis,
          checkIntervalMillis, jobExecutor, managementService);
    }

    if (shutdownExecutorAfter && jobExecutor.isActive()) {
      jobExecutor.shutdown();
    } else if (!wasActive && jobExecutor.isActive()) {
      // JobExecutorWaitUtils shuts down the executor if it was not active
      // before waiting. If the executor was active before, keep the state.
    }
  }

  protected void resolveMissingConfiguration() {
    if (jobExecutor != null && managementService != null) {
      return;
    }

    if (processEngineConfiguration == null) {
      if (processEngine != null) {
        processEngineConfiguration = processEngine.getProcessEngineConfiguration();
      } else {
        processEngine = JobExecutorWaitUtils.getProcessEngine();
        processEngineConfiguration = processEngine.getProcessEngineConfiguration();
      }
    }

    if (jobExecutor == null || managementService == null) {
      ProcessEngineConfigurationImpl impl = (ProcessEngineConfigurationImpl) processEngineConfiguration;
      if (jobExecutor == null) {
        jobExecutor = impl.getJobExecutor();
      }
      if (managementService == null) {
        managementService = impl.getManagementService();
      }
    }
  }
}

