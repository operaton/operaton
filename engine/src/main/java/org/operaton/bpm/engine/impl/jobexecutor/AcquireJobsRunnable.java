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
package org.operaton.bpm.engine.impl.jobexecutor;

import java.util.concurrent.atomic.AtomicBoolean;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;

/**
 * @author Thorben Lindhauer
 *
 */
public abstract class AcquireJobsRunnable implements Runnable {

  private static final JobExecutorLogger LOG = ProcessEngineLogger.JOB_EXECUTOR_LOGGER;

  protected final JobExecutor jobExecutor;

  protected volatile boolean isInterrupted;
  protected volatile boolean isJobAdded;
  protected final Object monitor = new Object();
  protected final AtomicBoolean isWaiting = new AtomicBoolean(false);

  protected AcquireJobsRunnable(JobExecutor jobExecutor) {
    this.jobExecutor = jobExecutor;
  }

  protected void suspendAcquisition(long millis) {
    if (millis <= 0) {
      return;
    }

    try {
      LOG.debugJobAcquisitionThreadSleeping(millis);
      synchronized (monitor) {
        if(!isInterrupted) {
          isWaiting.set(true);
          monitor.wait(millis);
        }
      }
      LOG.jobExecutorThreadWokeUp();
    }
    catch (InterruptedException e) {
      LOG.jobExecutionWaitInterrupted();
      Thread.currentThread().interrupt();
    }
    finally {
      isWaiting.set(false);
    }
  }

  public void stop() {
    synchronized (monitor) {
      isInterrupted = true;
      if(isWaiting.compareAndSet(true, false)) {
        monitor.notifyAll();
      }
    }
  }

  public void jobWasAdded() {
    isJobAdded = true;
    if(isWaiting.compareAndSet(true, false)) {
      // ensures we only notify once
      // I am OK with the race condition
      synchronized (monitor) {
        monitor.notifyAll();
      }
    }
  }

  protected void clearJobAddedNotification() {
    isJobAdded = false;
  }

  public boolean isJobAdded() {
    return isJobAdded;
  }
}
