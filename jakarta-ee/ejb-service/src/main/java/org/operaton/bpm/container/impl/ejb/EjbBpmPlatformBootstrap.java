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
package org.operaton.bpm.container.impl.ejb;

import org.operaton.bpm.ProcessApplicationService;
import org.operaton.bpm.ProcessEngineService;
import org.operaton.bpm.container.ExecutorService;
import org.operaton.bpm.container.RuntimeContainerDelegate;
import org.operaton.bpm.container.impl.RuntimeContainerDelegateImpl;
import org.operaton.bpm.container.impl.deployment.DiscoverBpmPlatformPluginsStep;
import org.operaton.bpm.container.impl.deployment.PlatformXmlStartProcessEnginesStep;
import org.operaton.bpm.container.impl.deployment.StopProcessApplicationsStep;
import org.operaton.bpm.container.impl.deployment.StopProcessEnginesStep;
import org.operaton.bpm.container.impl.deployment.UnregisterBpmPlatformPluginsStep;
import org.operaton.bpm.container.impl.deployment.jobexecutor.StartJobExecutorStep;
import org.operaton.bpm.container.impl.deployment.jobexecutor.StopJobExecutorStep;
import org.operaton.bpm.container.impl.ejb.deployment.EjbJarParsePlatformXmlStep;
import org.operaton.bpm.container.impl.ejb.deployment.StartJcaExecutorServiceStep;
import org.operaton.bpm.container.impl.ejb.deployment.StopJcaExecutorServiceStep;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <p>Bootstrap for the Operaton using a singleton EJB</p>
 *
 * @author Daniel Meyer
 */
@Startup
@Singleton(name="BpmPlatformBootstrap")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class EjbBpmPlatformBootstrap {

  private static final Logger LOGGER = Logger.getLogger(EjbBpmPlatformBootstrap.class.getName());

  @EJB
  protected ExecutorService executorServiceBean;

  protected ProcessEngineService processEngineService;
  protected ProcessApplicationService processApplicationService;

  @PostConstruct
  protected void start() {

    final RuntimeContainerDelegateImpl containerDelegate = getContainerDelegate();

    containerDelegate.getServiceContainer().createDeploymentOperation("deploying Operaton")
      .addStep(new EjbJarParsePlatformXmlStep())
      .addStep(new DiscoverBpmPlatformPluginsStep())
      .addStep(new StartJcaExecutorServiceStep(executorServiceBean))
      .addStep(new StartJobExecutorStep())
      .addStep(new PlatformXmlStartProcessEnginesStep())
      .execute();

    processEngineService = containerDelegate.getProcessEngineService();
    processApplicationService = containerDelegate.getProcessApplicationService();

    LOGGER.log(Level.INFO, "Operaton started successfully.");
  }

  @PreDestroy
  protected void stop() {

    final RuntimeContainerDelegateImpl containerDelegate = getContainerDelegate();

    containerDelegate.getServiceContainer().createUndeploymentOperation("undeploying Operaton")
      .addStep(new StopProcessApplicationsStep())
      .addStep(new StopProcessEnginesStep())
      .addStep(new StopJobExecutorStep())
      .addStep(new StopJcaExecutorServiceStep())
      .addStep(new UnregisterBpmPlatformPluginsStep())
      .execute();

    LOGGER.log(Level.INFO, "Operaton stopped.");

  }

  protected RuntimeContainerDelegateImpl getContainerDelegate() {
    return (RuntimeContainerDelegateImpl) RuntimeContainerDelegate.INSTANCE.get();
  }

  // getters //////////////////////////////////////////////

  public ProcessEngineService getProcessEngineService() {
    return processEngineService;
  }

  public ProcessApplicationService getProcessApplicationService() {
    return processApplicationService;
  }

}
