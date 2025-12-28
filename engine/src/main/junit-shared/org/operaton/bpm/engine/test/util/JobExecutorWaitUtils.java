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
package org.operaton.bpm.engine.test.util;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.ProcessEngineService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.ThreadPoolJobExecutor;

import static org.awaitility.Awaitility.await;

/**
 * Utility class for managing and ensuring the processing of jobs by the {@link JobExecutor}
 * in a BPM execution environment. This class provides methods for waiting until
 * all jobs are processed, querying the availability of jobs, and waiting for jobs
 * running in the thread pool to finish execution.
 * <p>
 * The methods offered by this class facilitate coordination of job processing
 * during tasks such as testing, ensuring proper job execution, and system
 * synchronization.
 * </p>
 * <p>
 * Note that this class includes mechanisms for controlling the active state of the
 * JobExecutor and employs configurable time intervals and timeouts for various
 * operations. All methods are statically available since the class is not intended
 * for instantiation.
 * </p>
 */
public final class JobExecutorWaitUtils {

  public static final long CHECK_INTERVAL_MS = 250L;
  public static final long JOBS_WAIT_TIMEOUT_MS = 20_000L;
  private static final int JOB_EXECUTOR_WAIT_MULTIPLIER = 2;
  private static final int THREAD_POOL_ACTIVE_COUNT_ZERO = 0;

  private JobExecutorWaitUtils() {
    // Private constructor to prevent instantiation
  }

  /**
   * Waits for the {@link JobExecutor} to process all jobs within the {@link ProcessEngine process engine}'s
   * default configuration using the default maximum wait time. This method internally delegates to
   * {@link #waitForJobExecutorToProcessAllJobs(long)} using a predefined timeout.
   */
  public static void waitForJobExecutorToProcessAllJobs() {
    waitForJobExecutorToProcessAllJobs(JOBS_WAIT_TIMEOUT_MS);
  }

  /**
   * Waits for the {@link JobExecutor} to process all jobs within the {@link ProcessEngine process engine}'s
   * default configuration for the specified maximum time. It retrieves the process engine configuration and delegates
   * the waiting logic to an internal implementation.
   *
   * @param maxMillisToWait the maximum time in milliseconds to wait for all jobs to be processed
   */
  public static void waitForJobExecutorToProcessAllJobs(long maxMillisToWait) {
    // Get the process engine instance (previously done before each test run)
    ProcessEngine processEngine = getProcessEngine();

    // Retrieve the job executor from the process engine configuration
    waitForJobExecutorToProcessAllJobs(processEngine.getProcessEngineConfiguration(), maxMillisToWait);
  }

  /**
   * Waits for the {@link JobExecutor} to process all jobs within the process engine configuration
   * for the specified maximum time. The method ensures that no jobs remain unprocessed within
   * the given time frame by invoking an internal implementation with a default check interval.
   *
   * @param maxMillisToWait            the maximum time in milliseconds to wait for all jobs to be processed
   * @param processEngineConfiguration the process engine configuration providing access to the {@link JobExecutor}
   *                                   and {@link ManagementService}
   */
  public static void waitForJobExecutorToProcessAllJobs(ProcessEngineConfiguration processEngineConfiguration,
                                                        long maxMillisToWait) {
    // Check interval configuration (deprecated and unused prior to migration)
    waitForJobExecutorToProcessAllJobs(processEngineConfiguration, maxMillisToWait, CHECK_INTERVAL_MS);
  }

  /**
   * Calculates the maximum wait time based on the provided maximum milliseconds and the wait time
   * derived from the given {@link JobExecutor}.
   * If the wait time required by the {@link JobExecutor} exceeds the specified maximum wait time,
   * the method returns the greater of the two values.
   *
   * @param maxMillisToWait the maximum time in milliseconds to wait for execution
   * @param jobExecutor     the {@link JobExecutor} instance used to determine its required wait time
   * @return the calculated maximum wait time in milliseconds
   */
  private static long calculateMaxWaitTime(JobExecutor jobExecutor, long maxMillisToWait) {
    int jobExecutorWaitTime = jobExecutor.getWaitTimeInMillis() * JOB_EXECUTOR_WAIT_MULTIPLIER;
    if (maxMillisToWait < jobExecutorWaitTime) {
      maxMillisToWait = jobExecutorWaitTime;
    }
    return maxMillisToWait;
  }

  /**
   * Waits for the {@link JobExecutor} to process all jobs within a specified maximum time, checking
   * the job completion status at regular intervals. If the jobs are not processed within the
   * specified time limit, a {@link ProcessEngineException} is thrown. Optionally shuts down
   * the job executor after processing jobs.
   *
   * @param processEngineConfiguration the process engine configuration providing access to the {@link JobExecutor}
   *                                   and {@link ManagementService}
   * @param maxMillisToWait   the maximum time in milliseconds to wait for all jobs to be processed
   * @param checkInterval     the time interval in milliseconds to check for job completion
   */
  public static void waitForJobExecutorToProcessAllJobs(ProcessEngineConfiguration processEngineConfiguration,
                                                        long maxMillisToWait,
                                                        long checkInterval) {
    JobExecutor jobExecutor = ((ProcessEngineConfigurationImpl)processEngineConfiguration).getJobExecutor();
    ManagementService managementService = ((ProcessEngineConfigurationImpl)processEngineConfiguration).getManagementService();
    waitForJobExecutorToProcessAllJobs(maxMillisToWait, checkInterval, jobExecutor, managementService);
  }

  /**
   * Waits for the {@link JobExecutor} to process all jobs within a specified maximum time, checking
   * the job completion status at regular intervals. If the jobs are not processed within the
   * specified time limit, a {@link ProcessEngineException} is thrown. Optionally shuts down
   * the job executor after processing jobs.
   *
   * @param maxMillisToWait   the maximum time in milliseconds to wait for all jobs to be processed
   * @param checkInterval     the time interval in milliseconds to check for job completion
   * @param jobExecutor       the {@link JobExecutor} responsible for managing job execution
   * @param managementService the {@link ManagementService} used to query and manage jobs
   */
  public static void waitForJobExecutorToProcessAllJobs(long maxMillisToWait,
                                                        long checkInterval,
                                                        JobExecutor jobExecutor,
                                                        ManagementService managementService) {
    waitForJobExecutorToProcessAllJobs(maxMillisToWait, checkInterval, jobExecutor, managementService, () -> !hasPendingJobs(managementService));
  }

  /**
   * Waits for the {@link JobExecutor} to process all jobs associated with a specific process instance.
   * This method repeatedly checks for job completion at specified intervals within a defined maximum wait time.
   * Throws a {@link ProcessEngineException} in case the timeout is exceeded.
   * Optionally shuts down the {@link JobExecutor} after processing if specified.
   *
   * @param processInstanceId the ID of the process instance for which jobs need to be processed
   * @param maxMillisToWait   the maximum time in milliseconds to wait for all jobs to be processed
   * @param checkInterval     the interval in milliseconds to check for job completion
   * @param jobExecutor       the {@link JobExecutor} responsible for processing the jobs
   * @param managementService the {@link ManagementService} used to interact with jobs management
   */
  public static void waitForJobExecutorToProcessAllJobs(String processInstanceId,
                                                        long maxMillisToWait,
                                                        long checkInterval,
                                                        JobExecutor jobExecutor,
                                                        ManagementService managementService) {
    waitForJobExecutorToProcessAllJobs(maxMillisToWait, checkInterval, jobExecutor, managementService, () -> !hasPendingJobsByPiId(processInstanceId, managementService));
  }

  /**
   * Waits for the {@link JobExecutor} to process all jobs within a specified maximum time, checking
   * the job completion status at regular intervals. If the jobs are not processed within the
   * specified time limit, a {@link ProcessEngineException} is thrown. Optionally shuts down
   * the job executor after processing jobs.
   *
   * @param maxMillisToWait   the maximum time in milliseconds to wait for all jobs to be processed
   * @param checkInterval     the time interval in milliseconds to check for job completion
   * @param jobExecutor       the {@link JobExecutor} responsible for managing job execution
   * @param managementService the {@link ManagementService} used to query and manage jobs
   */
  private static void waitForJobExecutorToProcessAllJobs(long maxMillisToWait,
                                                        long checkInterval,
                                                        JobExecutor jobExecutor,
                                                        ManagementService managementService,
                                                        Callable<Boolean> waitForCondition) {
    boolean shutdown = false;
    maxMillisToWait = calculateMaxWaitTime(jobExecutor, maxMillisToWait);
    try {
      shutdown = activateJobExecutor(jobExecutor);
      waitForCondition(waitForCondition, maxMillisToWait, checkInterval);
    } catch (Exception e) {
      throw new ProcessEngineException(
        "Time limit of %d was exceeded (still %d jobs available)".formatted(maxMillisToWait, numberOfJobsAvailable(managementService)), e);
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
      throw new ProcessEngineException("Time limit of %d was exceeded.".formatted(maxMillisToWait));
    }
  }

  /**
   * Activates the specified {@link JobExecutor} by starting it if it is not already active.
   * This method checks the current state of the {@link JobExecutor} and ensures that it
   * transitions to an active state if it is inactive.
   *
   * @param jobExecutor the {@link JobExecutor} instance to be activated
   * @return {@code true} if the {@link JobExecutor} was successfully activated, {@code false} if it was already active
   */
  private static boolean activateJobExecutor(JobExecutor jobExecutor) {
    // Ensure the job executor is inactive before proceeding
    if (jobExecutor.isActive()) {
      return false;
    }
    jobExecutor.start();
    return true;
  }

  /**
   * Determines if there are jobs available for execution associated with a specific process instance ID.
   * The method checks for executable, active jobs with retries left through the provided ManagementService.
   *
   * @param processInstanceId the ID of the process instance for which jobs should be checked
   * @param managementService the ManagementService used to query and manage jobs
   * @return true if jobs are available for the specified process instance, false otherwise
   */
  private static boolean hasPendingJobsByPiId(String processInstanceId, ManagementService managementService) {
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
  private static boolean hasPendingJobs(ManagementService managementService) {
    return numberOfJobsAvailable(managementService) > 0;      // Check if there are any matching jobs
  }

  /**
   * Counts the number of jobs available for execution using the provided ManagementService.
   * Only jobs that are executable, active, and have retries left are counted.
   *
   * @param managementService the ManagementService used to query for available jobs
   * @return the total number of jobs available for execution
   */
  private static long numberOfJobsAvailable(ManagementService managementService) {
    // Directly count the number of jobs that are available using the JobQuery interface
    return managementService.createJobQuery().withRetriesLeft() // Select jobs with retries > 0
        .executable()      // Ensure jobs are due (null or past due date)
        .active()          // Ensure jobs are not suspended
        .count();          // Efficiently count the jobs in the database
  }

  /**
   * Retrieves the default process engine from the process engine service.
   *
   * @return the default {@link ProcessEngine} instance as configured in the BPM platform.
   */
  static ProcessEngine getProcessEngine() {
    ProcessEngineService processEngineService = BpmPlatform.getProcessEngineService();
    return processEngineService.getDefaultProcessEngine();
  }

}
