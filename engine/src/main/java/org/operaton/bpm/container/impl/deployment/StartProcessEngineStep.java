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
package org.operaton.bpm.container.impl.deployment;

import java.util.Map;

import org.operaton.bpm.application.AbstractProcessApplication;
import org.operaton.bpm.container.impl.ContainerIntegrationLogger;
import org.operaton.bpm.container.impl.jmx.services.JmxManagedProcessEngine;
import org.operaton.bpm.container.impl.jmx.services.JmxManagedProcessEngineController;
import org.operaton.bpm.container.impl.metadata.PropertyHelper;
import org.operaton.bpm.container.impl.metadata.spi.ProcessEnginePluginXml;
import org.operaton.bpm.container.impl.metadata.spi.ProcessEngineXml;
import org.operaton.bpm.container.impl.spi.DeploymentOperation;
import org.operaton.bpm.container.impl.spi.DeploymentOperationStep;
import org.operaton.bpm.container.impl.spi.PlatformServiceContainer;
import org.operaton.bpm.container.impl.spi.ServiceTypes;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.persistence.StrongUuidGenerator;
import org.operaton.bpm.engine.impl.util.ReflectUtil;

import static org.operaton.bpm.container.impl.deployment.Attachments.PROCESS_APPLICATION;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * <p>Deployment operation step responsible for starting a managed process engine
 * inside the runtime container.</p>
 *
 * @author Daniel Meyer
 *
 */
public class StartProcessEngineStep extends DeploymentOperationStep {

  private static final ContainerIntegrationLogger LOG = ProcessEngineLogger.CONTAINER_INTEGRATION_LOGGER;

  /** the process engine Xml configuration passed in as a parameter to the operation step */
  protected final ProcessEngineXml processEngineXml;

  public StartProcessEngineStep(ProcessEngineXml processEngineXml) {
    this.processEngineXml = processEngineXml;
  }

  @Override
  public String getName() {
    return "Start process engine " + processEngineXml.getName();
  }

  @Override
  public void performOperationStep(DeploymentOperation operationContext) {

    final PlatformServiceContainer serviceContainer = operationContext.getServiceContainer();
    final AbstractProcessApplication processApplication = operationContext.getAttachment(PROCESS_APPLICATION);

    ClassLoader classLoader = null;

    if(processApplication != null) {
      classLoader = processApplication.getProcessApplicationClassloader();
    }

    String configurationClassName = processEngineXml.getConfigurationClass();

    if(configurationClassName == null || configurationClassName.isEmpty()) {
      configurationClassName = StandaloneProcessEngineConfiguration.class.getName();
    }

    // create & instantiate configuration class
    Class<? extends ProcessEngineConfigurationImpl> configurationClass = loadClass(configurationClassName, classLoader, ProcessEngineConfigurationImpl.class);
    ProcessEngineConfigurationImpl configuration = ReflectUtil.createInstance(configurationClass);

    // set UUid generator
    // TODO: move this to configuration and use as default?
    ProcessEngineConfigurationImpl configurationImpl = configuration;
    configurationImpl.setIdGenerator(new StrongUuidGenerator());

    // set configuration values
    String name = processEngineXml.getName();
    configuration.setProcessEngineName(name);

    String datasourceJndiName = processEngineXml.getDatasource();
    configuration.setDataSourceJndiName(datasourceJndiName);

    // apply properties
    Map<String, String> properties = processEngineXml.getProperties();
    setJobExecutorActivate(configuration, properties);
    PropertyHelper.applyProperties(configuration, properties);

    // instantiate plugins:
    configurePlugins(configuration, processEngineXml, classLoader);
    addAdditionalPlugins(configuration);

    if(processEngineXml.getJobAcquisitionName() != null && !processEngineXml.getJobAcquisitionName().isEmpty()) {
      JobExecutor jobExecutor = getJobExecutorService(serviceContainer);
      ensureNotNull("Cannot find referenced job executor with name '" + processEngineXml.getJobAcquisitionName() + "'", "jobExecutor", jobExecutor);

      // set JobExecutor on process engine
      configurationImpl.setJobExecutor(jobExecutor);
    }

    additionalConfiguration(configuration);

    // start the process engine inside the container.
    JmxManagedProcessEngine managedProcessEngineService = createProcessEngineControllerInstance(configuration);
    serviceContainer.startService(ServiceTypes.PROCESS_ENGINE, configuration.getProcessEngineName(), managedProcessEngineService);

  }

  @SuppressWarnings("unused")
  protected void setJobExecutorActivate(ProcessEngineConfigurationImpl configuration, Map<String, String> properties) {
    // override job executor auto activate: set to true in shared engine scenario
    // if it is not specified (see #CAM-4817)
    configuration.setJobExecutorActivate(true);
  }

  protected JmxManagedProcessEngineController createProcessEngineControllerInstance(ProcessEngineConfigurationImpl configuration) {
    return new JmxManagedProcessEngineController(configuration);
  }

  /**
   * <p>Instantiates and applies all {@link ProcessEnginePlugin}s defined in the processEngineXml
   */
  protected void configurePlugins(ProcessEngineConfigurationImpl configuration, ProcessEngineXml processEngineXml, ClassLoader classLoader) {

    for (ProcessEnginePluginXml pluginXml : processEngineXml.getPlugins()) {
      // create plugin instance
      Class<? extends ProcessEnginePlugin> pluginClass = loadClass(pluginXml.getPluginClass(), classLoader, ProcessEnginePlugin.class);
      ProcessEnginePlugin plugin = ReflectUtil.createInstance(pluginClass);

      // apply configured properties
      Map<String, String> properties = pluginXml.getProperties();
      PropertyHelper.applyProperties(plugin, properties);

      // add to configuration
      configuration.getProcessEnginePlugins().add(plugin);
    }

  }


  protected JobExecutor getJobExecutorService(final PlatformServiceContainer serviceContainer) {
    // lookup container managed job executor
    String jobAcquisitionName = processEngineXml.getJobAcquisitionName();
    return serviceContainer.getServiceValue(ServiceTypes.JOB_EXECUTOR, jobAcquisitionName);
  }

  @SuppressWarnings("unchecked")
  protected <T> Class<? extends T> loadClass(String className, ClassLoader customClassloader, Class<T> clazz) {
    try {
      return ReflectUtil.loadClass(className, customClassloader, clazz);
    }
    catch (ClassNotFoundException e) {
      throw LOG.cannotLoadConfigurationClass(className, e);
    }
    catch (ClassCastException e) {
      throw LOG.configurationClassHasWrongType(className, clazz, e);
    }
  }

  /**
   * Add additional plugins that are not declared in the process engine xml.
   */
  protected void addAdditionalPlugins(ProcessEngineConfigurationImpl configuration) {
    // do nothing
  }

  protected void additionalConfiguration(ProcessEngineConfigurationImpl configuration) {
    // do nothing
  }

}
