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
package org.operaton.bpm.container.impl.deployment.jobexecutor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.operaton.bpm.container.impl.RuntimeContainerDelegateImpl;
import org.operaton.bpm.container.impl.deployment.Attachments;
import org.operaton.bpm.container.impl.jmx.services.JmxManagedThreadPool;
import org.operaton.bpm.container.impl.metadata.spi.BpmPlatformXml;
import org.operaton.bpm.container.impl.metadata.spi.JobExecutorXml;
import org.operaton.bpm.container.impl.spi.PlatformServiceContainer;
import org.operaton.bpm.container.impl.spi.DeploymentOperation;
import org.operaton.bpm.container.impl.spi.DeploymentOperationStep;
import org.operaton.bpm.container.impl.spi.ServiceTypes;

/**
 * <p>
 * Deployment operation step responsible for deploying a thread pool for the
 * JobExecutor
 * </p>
 *
 * @author Daniel Meyer
 *
 */
public class StartManagedThreadPoolStep extends DeploymentOperationStep {

  private static final int DEFAULT_CORE_POOL_SIZE = 3;
  private static final int DEFAULT_MAX_POOL_SIZE = 10;
  private static final long DEFAULT_KEEP_ALIVE_TIME_MS = 0L;
  private static final int DEFAULT_QUEUE_SIZE = 3;

  @Override
  public String getName() {
    return "Deploy Job Executor Thread Pool";
  }

  @Override
  public void performOperationStep(DeploymentOperation operationContext) {

    final PlatformServiceContainer serviceContainer = operationContext.getServiceContainer();

    JobExecutorXml jobExecutorXml = getJobExecutorXml(operationContext);

    int queueSize = getQueueSize(jobExecutorXml);
    int corePoolSize = getCorePoolSize(jobExecutorXml);
    int maxPoolSize = getMaxPoolSize(jobExecutorXml);
    long keepAliveTime = getKeepAliveTime(jobExecutorXml);

    // initialize Queue & Executor services
    BlockingQueue<Runnable> threadPoolQueue = new ArrayBlockingQueue<>(queueSize);

    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, threadPoolQueue);
    threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

    // construct the service for the thread pool
    JmxManagedThreadPool managedThreadPool = new JmxManagedThreadPool(threadPoolQueue, threadPoolExecutor);

    // install the service into the container
    serviceContainer.startService(ServiceTypes.BPM_PLATFORM, RuntimeContainerDelegateImpl.SERVICE_NAME_EXECUTOR, managedThreadPool);

  }

  private JobExecutorXml getJobExecutorXml(DeploymentOperation operationContext) {
    BpmPlatformXml bpmPlatformXml = operationContext.getAttachment(Attachments.BPM_PLATFORM_XML);
    JobExecutorXml jobExecutorXml = bpmPlatformXml.getJobExecutor();
    return jobExecutorXml;
  }

  private int getQueueSize(JobExecutorXml jobExecutorXml) {
    String queueSize = jobExecutorXml.getProperties().get(JobExecutorXml.QUEUE_SIZE);
    if (queueSize == null) {
      return DEFAULT_QUEUE_SIZE;
    }
    return Integer.parseInt(queueSize);
  }

  private long getKeepAliveTime(JobExecutorXml jobExecutorXml) {
    String keepAliveTime = jobExecutorXml.getProperties().get(JobExecutorXml.KEEP_ALIVE_TIME);
    if (keepAliveTime == null) {
      return DEFAULT_KEEP_ALIVE_TIME_MS;
    }
    return Long.parseLong(keepAliveTime);
  }

  private int getMaxPoolSize(JobExecutorXml jobExecutorXml) {
    String maxPoolSize = jobExecutorXml.getProperties().get(JobExecutorXml.MAX_POOL_SIZE);
    if (maxPoolSize == null) {
      return DEFAULT_MAX_POOL_SIZE;
    }
    return Integer.parseInt(maxPoolSize);
  }

  private int getCorePoolSize(JobExecutorXml jobExecutorXml) {
    String corePoolSize = jobExecutorXml.getProperties().get(JobExecutorXml.CORE_POOL_SIZE);
    if (corePoolSize == null) {
      return DEFAULT_CORE_POOL_SIZE;
    }
    return Integer.parseInt(corePoolSize);
  }
}
