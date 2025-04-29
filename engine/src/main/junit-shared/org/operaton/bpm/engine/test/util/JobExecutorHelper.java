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
package org.operaton.bpm.engine.test.util;

import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.ProcessEngineService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.ThreadPoolJobExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * Utility class for managing and ensuring the processing of jobs by the JobExecutor
 * in a BPM execution environment. This class provides methods for waiting until
 * all jobs are processed, querying the availability of jobs, and waiting for jobs
 * running in the thread pool to finish execution.
 * The methods offered by this class facilitate coordination of job processing
 * during tasks such as testing, ensuring proper job execution, and system
 * synchronization.
 * Note that this class includes mechanisms for controlling the active state of the
 * JobExecutor and employs configurable time intervals and timeouts for various
 * operations. All methods are statically available since the class is not intended
 * for instantiation.
 */
public class JobExecutorHelper {

  public static final long CHECK_INTERVAL_MS = 500L;
  public static final long JOBS_WAIT_TIMEOUT_MS = 20_000L;
  private static final int JOB_EXECUTOR_WAIT_MULTIPLIER = 2;
  private static final int THREAD_POOL_ACTIVE_COUNT_ZERO = 0;

  private JobExecutorHelper() {
    // Private constructor to prevent instantiation
  }

  /**
   * Waits for the job executor to process all jobs within the process engine's default configuration
   * using the default maximum wait time. This method internally delegates to
   * {@link #waitForJobExecutorToProcessAllJobs(long)} using a predefined timeout.
   */
  public static void waitForJobExecutorToProcessAllJobs() {
    waitForJobExecutorToProcessAllJobs(JOBS_WAIT_TIMEOUT_MS);
  }

  /**
   * Waits for the job executor to process all jobs within the process engine's default configuration
   * for the specified maximum time. It retrieves the process engine configuration and delegates
   * the waiting logic to an internal implementation.
   *
   * @param maxMillisToWait the maximum time in milliseconds to wait for all jobs to be processed
   */
  public static void waitForJobExecutorToProcessAllJobs(long maxMillisToWait) {
    // Get the process engine instance (previously done before each test run)
    ProcessEngine processEngine = getProcessEngine();

    // Retrieve the job executor from the process engine configuration
    ProcessEngineConfigurationImpl processEngineConfiguration = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration();
    waitForJobExecutorToProcessAllJobs(processEngineConfiguration, maxMillisToWait);
  }

  /**
   * Waits for the job executor to process all jobs within the process engine configuration
   * for the specified maximum time. The method ensures that no jobs remain unprocessed within
   * the given time frame by invoking an internal implementation with a default check interval.
   *
   * @param processEngineConfiguration the process engine configuration providing access to the job executor
   *                                   and management service
   * @param maxMillisToWait            the maximum time in milliseconds to wait for all jobs to be processed
   */
  public static void waitForJobExecutorToProcessAllJobs(ProcessEngineConfigurationImpl processEngineConfiguration,
                                                        long maxMillisToWait) {
    // Check interval configuration (deprecated and unused prior to migration)
    waitForJobExecutorToProcessAllJobs(processEngineConfiguration, maxMillisToWait, CHECK_INTERVAL_MS);
  }

  /**
   * Waits for the job executor to process all jobs within the process engine configuration,
   * ensuring that no jobs remain unprocessed within the specified time. The method repeatedly
   * checks for job completion at the given intervals until the maximum time to wait is reached.
   * If the job executor is not active, it will be started. The method will throw an exception
   * if the jobs are not processed within the given time.
   *
   * @param processEngineConfiguration the process engine configuration providing access to the job executor
   *                                   and management service
   * @param maxMillisToWait            the maximum time in milliseconds to wait for all jobs to be processed
   * @param checkInterval              the interval in milliseconds to check for job completion
   */
  public static void waitForJobExecutorToProcessAllJobs(ProcessEngineConfigurationImpl processEngineConfiguration,
                                                        long maxMillisToWait,
                                                        long checkInterval) {
    JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
    ManagementService managementService = getManagementService(processEngineConfiguration);
    // Ensure the job executor is inactive before proceeding
    activateJobExecutor(jobExecutor);
    maxMillisToWait = calculateMaxWaitTime(maxMillisToWait, jobExecutor);
    boolean shutdown = true;
    waitForJobExecutorToProcessAllJobs(maxMillisToWait, checkInterval, jobExecutor, managementService, shutdown);
  }

  /**
   * Calculates the maximum wait time based on the provided maximum milliseconds and the wait time
   * derived from the given job executor. If the wait time required by the job executor exceeds the
   * specified maximum wait time, the method returns the greater of the two values.
   *
   * @param maxMillisToWait the maximum time in milliseconds to wait for execution
   * @param jobExecutor     the {@link JobExecutor} instance used to determine its required wait time
   * @return the calculated maximum wait time in milliseconds
   */
  private static long calculateMaxWaitTime(long maxMillisToWait, JobExecutor jobExecutor) {
    int jobExecutorWaitTime = jobExecutor.getWaitTimeInMillis() * JOB_EXECUTOR_WAIT_MULTIPLIER;
    if (maxMillisToWait < jobExecutorWaitTime) {
      maxMillisToWait = jobExecutorWaitTime;
    }
    return maxMillisToWait;
  }

  /**
   * Waits for the job executor to process all jobs within a specified maximum time, checking
   * the job completion status at regular intervals. If the jobs are not processed within the
   * specified time limit, a {@link ProcessEngineException} is thrown. Optionally shuts down
   * the job executor after processing jobs.
   *
   * @param maxMillisToWait   the maximum time in milliseconds to wait for all jobs to be processed
   * @param checkInterval     the time interval in milliseconds to check for job completion
   * @param jobExecutor       the {@link JobExecutor} responsible for managing job execution
   * @param managementService the {@link ManagementService} used to query and manage jobs
   * @param shutdown          a boolean flag indicating whether to shut down the job executor after processing
   */
  public static void waitForJobExecutorToProcessAllJobs(long maxMillisToWait,
                                                        long checkInterval,
                                                        JobExecutor jobExecutor,
                                                        ManagementService managementService,
                                                        boolean shutdown) {
    try {
      Callable<Boolean> condition = () -> !areJobsAvailable(managementService);
      waitForCondition(condition, maxMillisToWait, checkInterval);
    } catch (Exception e) {
      throw new ProcessEngineException(
          "Time limit of " + maxMillisToWait + " was exceeded (still " + numberOfJobsAvailable(managementService)
              + " jobs available)", e);
    } finally {
      if (shutdown) {
        jobExecutor.shutdown();
      }
    }
  }

  /**
   * Waits for all job execution runnables to finish within the specified maximum time, checking the status at regular intervals.
   *
   * @param maxMillisToWait the maximum time in milliseconds to wait for the job execution runnables to complete
   * @param intervalMillis  the interval in milliseconds at which to check whether the job execution runnables have finished
   * @param jobExecutor     the {@link JobExecutor} responsible for managing the job execution runnables
   */
  public static void waitForJobExecutionRunnablesToFinish(long maxMillisToWait,
                                                          long intervalMillis,
                                                          JobExecutor jobExecutor) {
    waitForCondition(() -> ((ThreadPoolJobExecutor) jobExecutor).getThreadPoolExecutor().getActiveCount()
        == THREAD_POOL_ACTIVE_COUNT_ZERO, maxMillisToWait, intervalMillis);
  }

  /**
   * Waits for a specified condition to be met within a given time period, repeatedly checking the condition
   * at specified intervals. If the condition is not met within the time limit, a {@link ProcessEngineException}
   * is thrown.
   *
   * @param condition       A {@link Callable} returning a {@link Boolean}, representing the condition to be evaluated.
   *                        The method will wait until this condition evaluates to {@code true}.
   * @param maxMillisToWait The maximum amount of time in milliseconds to wait for the condition to be satisfied.
   * @param checkInterval   The interval in milliseconds at which the condition will be re-evaluated.
   *                        If this value is greater than {@code maxMillisToWait}, it will be adjusted to match {@code maxMillisToWait}.
   */
  public static void waitForCondition(Callable<Boolean> condition, long maxMillisToWait, long checkInterval) {
    if (maxMillisToWait < checkInterval) {
      checkInterval = maxMillisToWait;
    }
    try {
      await().atMost(maxMillisToWait, TimeUnit.MILLISECONDS)
          .pollInterval(checkInterval, TimeUnit.MILLISECONDS)
          .ignoreExceptions() // In case condition throws an exception during polling
          .until(condition);
    } catch (Exception e) {
      throw new ProcessEngineException("Time limit of " + maxMillisToWait + " was exceeded.");
    }
  }

  /**
   * Waits for the job executor to process all jobs associated with a specific process instance.
   * This method repeatedly checks for job completion at specified intervals within a defined maximum wait time.
   * Throws a {@link ProcessEngineException} in case the timeout is exceeded.
   * Optionally shuts down the job executor after processing if specified.
   *
   * @param processInstanceId the ID of the process instance for which jobs need to be processed
   * @param maxMillisToWait   the maximum time in milliseconds to wait for all jobs to be processed
   * @param checkInterval     the interval in milliseconds to check for job completion
   * @param jobExecutor       the {@link JobExecutor} responsible for processing the jobs
   * @param managementService the {@link ManagementService} used to interact with jobs management
   * @param shutdown          a flag indicating whether to shut down the job executor after processing
   */
  public static void waitForJobExecutorToProcessAllJobs(String processInstanceId,
                                                        long maxMillisToWait,
                                                        long checkInterval,
                                                        JobExecutor jobExecutor,
                                                        ManagementService managementService,
                                                        boolean shutdown) {
    activateJobExecutor(jobExecutor);
    maxMillisToWait = calculateMaxWaitTime(maxMillisToWait, jobExecutor);
    try {
      Callable<Boolean> condition = () -> !areJobsAvailableByPiId(processInstanceId, managementService);
      waitForCondition(condition, maxMillisToWait, checkInterval);
    } catch (Exception e) {
      throw new ProcessEngineException("Time limit of " + maxMillisToWait + " was exceeded.", e);
    } finally {
      if (shutdown) {
        jobExecutor.shutdown();
      }
    }
  }

  /**
   * Activates the specified job executor by starting it if it is not already active.
   * This method checks the current state of the job executor and ensures that it
   * transitions to an active state if it is inactive.
   *
   * @param jobExecutor the {@link JobExecutor} instance to be activated
   */
  private static void activateJobExecutor(JobExecutor jobExecutor) {
    // Ensure the job executor is inactive before proceeding
    if (jobExecutor.isActive()) {
      return;
    }
    jobExecutor.start();
  }

  /**
   * Determines if there are jobs available for execution associated with a specific process instance ID.
   * The method checks for executable, active jobs with retries left through the provided ManagementService.
   *
   * @param processInstanceId the ID of the process instance for which jobs should be checked
   * @param managementService the ManagementService used to query and manage jobs
   * @return true if jobs are available for the specified process instance, false otherwise
   */
  private static boolean areJobsAvailableByPiId(String processInstanceId, ManagementService managementService) {
    return managementService.createJobQuery()
        .withRetriesLeft()
        .executable()
        .active()
        .processInstanceId(processInstanceId)
        .count() > 0;

  }

  /**
   * Checks if there are jobs available for processing using the provided ManagementService.
   *
   * @param managementService the ManagementService used to query the available jobs
   * @return {@code true} if jobs are available for processing, {@code false} otherwise
   */
  public static boolean areJobsAvailable(ManagementService managementService) {
    return numberOfJobsAvailable(managementService) > 0;      // Check if there are any matching jobs
  }

  /**
   * Counts the number of jobs available for execution using the provided ManagementService.
   * Only jobs that are executable, active, and have retries left are counted.
   *
   * @param managementService the ManagementService used to query for available jobs
   * @return the total number of jobs available for execution
   */
  public static long numberOfJobsAvailable(ManagementService managementService) {
    // Directly count the number of jobs that are available using the JobQuery interface
    return managementService.createJobQuery().withRetriesLeft() // Select jobs with retries > 0
        .executable()      // Ensure jobs are due (null or past due date)
        .active()          // Ensure jobs are not suspended
        .count();          // Efficiently count the jobs in the database
  }

  /**
   * Retrieves the {@link ManagementService} from the given {@link ProcessEngineConfigurationImpl}.
   *
   * @param processEngineConfiguration the process engine configuration from which to retrieve the management service
   * @return the {@link ManagementService} instance associated with the specified process engine configuration
   */
  private static ManagementService getManagementService(ProcessEngineConfigurationImpl processEngineConfiguration) {
    return processEngineConfiguration.getManagementService();
  }

  /**
   * Retrieves the default process engine from the process engine service.
   *
   * @return the default {@link ProcessEngine} instance as configured in the BPM platform.
   */
  private static ProcessEngine getProcessEngine() {
    ProcessEngineService processEngineService = BpmPlatform.getProcessEngineService();
    return processEngineService.getDefaultProcessEngine();
  }

}
