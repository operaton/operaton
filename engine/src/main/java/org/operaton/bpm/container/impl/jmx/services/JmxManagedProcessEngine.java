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

import java.util.Set;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.container.impl.jmx.MBeanServiceContainer;
import org.operaton.bpm.container.impl.spi.PlatformService;
import org.operaton.bpm.container.impl.spi.PlatformServiceContainer;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;

import static java.util.Objects.requireNonNull;

/**
 * <p>Represents a process engine managed by the {@link MBeanServiceContainer}</p>
 *
 * @author Daniel Meyer
 *
 */
public @NullMarked class JmxManagedProcessEngine implements PlatformService<ProcessEngine>, JmxManagedProcessEngineMBean {

  protected @Nullable ProcessEngine processEngine;

  // for subclasses
  protected JmxManagedProcessEngine() {
  }

  public JmxManagedProcessEngine(ProcessEngine processEngine) {
    this.processEngine = processEngine;
  }

  @Override
  public void start(PlatformServiceContainer container) {
    // this one has no lifecycle support
  }

  @Override
  public void stop(PlatformServiceContainer container) {
    // this one has no lifecycle support
  }

  @Override
  public String getName() {
    return getProcessEngine().getName();
  }

  public ProcessEngine getProcessEngine() {
    return requireNonNull(processEngine);
  }

  @Override
  public ProcessEngine getValue() {
    return getProcessEngine();
  }

  @Override
  public Set<String> getRegisteredDeployments() {
    return getManagementService().getRegisteredDeployments();
  }

  @Override
  public void registerDeployment(String deploymentId) {
    getManagementService().registerDeploymentForJobExecutor(deploymentId);
  }

  @Override
  public void unregisterDeployment(String deploymentId) {
    getManagementService().unregisterDeploymentForJobExecutor(deploymentId);
  }

  @Override
  public void reportDbMetrics() {
    getManagementService().reportDbMetricsNow();
  }

  private ManagementService getManagementService() {
    ManagementService managementService = getProcessEngine().getManagementService();
    return requireNonNull(managementService);
  }

}
