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
package org.operaton.bpm.container.impl.jmx.services;

import org.operaton.bpm.container.impl.spi.PlatformService;
import org.operaton.bpm.container.impl.spi.PlatformServiceContainer;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;

/**
 * @author Daniel Meyer
 *
 */
public class JmxManagedJobExecutor implements PlatformService<JobExecutor>, JmxManagedJobExecutorMBean {

  protected final JobExecutor jobExecutor;

  public JmxManagedJobExecutor(JobExecutor jobExecutor) {
    this.jobExecutor = jobExecutor;
  }

  @Override
  public void start(PlatformServiceContainer mBeanServiceContainer) {
    // no-op:
    // job executor is lazy-started when first process engine is registered and jobExecutorActivate = true
    // See: #CAM-4817
  }

  @Override
  public void stop(PlatformServiceContainer mBeanServiceContainer) {
    shutdown();
  }

  @Override
  public void start() {
    jobExecutor.start();
  }

  @Override
  public void shutdown() {
    jobExecutor.shutdown();
  }

  @Override
  public int getWaitTimeInMillis() {
    return jobExecutor.getWaitTimeInMillis();
  }

  @Override
  public void setWaitTimeInMillis(int waitTimeInMillis) {
    jobExecutor.setWaitTimeInMillis(waitTimeInMillis);
  }

  @Override
  public int getLockTimeInMillis() {
    return jobExecutor.getLockTimeInMillis();
  }

  @Override
  public void setLockTimeInMillis(int lockTimeInMillis) {
    jobExecutor.setLockTimeInMillis(lockTimeInMillis);
  }

  @Override
  public String getLockOwner() {
    return jobExecutor.getLockOwner();
  }

  @Override
  public void setLockOwner(String lockOwner) {
    jobExecutor.setLockOwner(lockOwner);
  }

  @Override
  public int getMaxJobsPerAcquisition() {
    return jobExecutor.getMaxJobsPerAcquisition();
  }

  @Override
  public void setMaxJobsPerAcquisition(int maxJobsPerAcquisition) {
    jobExecutor.setMaxJobsPerAcquisition(maxJobsPerAcquisition);
  }

  @Override
  public String getName() {
    return jobExecutor.getName();
  }

  @Override
  public JobExecutor getValue() {
    return jobExecutor;
  }

  @Override
  public boolean isActive() {
    return jobExecutor.isActive();
  }
}
