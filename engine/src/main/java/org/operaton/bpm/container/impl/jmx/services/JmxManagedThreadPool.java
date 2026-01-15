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
package org.operaton.bpm.container.impl.jmx.services;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.operaton.bpm.container.impl.ContainerIntegrationLogger;
import org.operaton.bpm.container.impl.spi.PlatformService;
import org.operaton.bpm.container.impl.spi.PlatformServiceContainer;
import org.operaton.bpm.container.impl.threading.se.SeExecutorService;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;

/**
 * @author Daniel Meyer
 *
 */
public class JmxManagedThreadPool extends SeExecutorService implements JmxManagedThreadPoolMBean, PlatformService<JmxManagedThreadPool> {

  private static final ContainerIntegrationLogger LOG = ProcessEngineLogger.CONTAINER_INTEGRATION_LOGGER;

  protected final BlockingQueue<Runnable> threadPoolQueue;

  public JmxManagedThreadPool(BlockingQueue<Runnable> queue, ThreadPoolExecutor executor) {
    super(executor);
    threadPoolQueue = queue;
  }

  @Override
  public void start(PlatformServiceContainer mBeanServiceContainer) {
    // nothing to do
  }

  @Override
  public void stop(PlatformServiceContainer mBeanServiceContainer) {

    // clear the queue
    threadPoolQueue.clear();

    // Ask the thread pool to finish and exit
    threadPoolExecutor.shutdown();

    // Waits for 1 minute to finish all currently executing jobs
    try {
      if(!threadPoolExecutor.awaitTermination(60L, TimeUnit.SECONDS)) {
        LOG.timeoutDuringShutdownOfThreadPool(60, TimeUnit.SECONDS);
      }
    }
    catch (InterruptedException e) {
      LOG.interruptedWhileShuttingDownThreadPool(e);
      Thread.currentThread().interrupt();
    }

  }

  @Override
  public JmxManagedThreadPool getValue() {
    return this;
  }

  @Override
  public void setCorePoolSize(int corePoolSize) {
    threadPoolExecutor.setCorePoolSize(corePoolSize);
  }

  @Override
  public void setMaximumPoolSize(int maximumPoolSize) {
    threadPoolExecutor.setMaximumPoolSize(maximumPoolSize);
  }

  @Override
  public int getMaximumPoolSize() {
    return threadPoolExecutor.getMaximumPoolSize();
  }

  public void setKeepAliveTime(long time, TimeUnit unit) {
    threadPoolExecutor.setKeepAliveTime(time, unit);
  }

  @Override
  public void purgeThreadPool() {
    threadPoolExecutor.purge();
  }

  @Override
  public int getPoolSize() {
    return threadPoolExecutor.getPoolSize();
  }

  @Override
  public int getActiveCount() {
    return threadPoolExecutor.getActiveCount();
  }

  @Override
  public int getLargestPoolSize() {
    return threadPoolExecutor.getLargestPoolSize();
  }

  @Override
  public long getTaskCount() {
    return threadPoolExecutor.getTaskCount();
  }

  @Override
  public long getCompletedTaskCount() {
    return threadPoolExecutor.getCompletedTaskCount();
  }

  @Override
  public int getQueueCount() {
    return threadPoolQueue.size();
  }

  public int getQueueAddlCapacity() {
    return threadPoolQueue.remainingCapacity();
  }

  public ThreadPoolExecutor getThreadPoolExecutor() {
    return threadPoolExecutor;
  }
}
