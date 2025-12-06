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
package org.operaton.bpm.webapp.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.Set;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.rest.spi.ProcessEngineProvider;
import org.operaton.bpm.webapp.AppRuntimeDelegate;
import org.operaton.bpm.webapp.plugin.AppPluginRegistry;
import org.operaton.bpm.webapp.plugin.impl.DefaultAppPluginRegistry;
import org.operaton.bpm.webapp.plugin.resource.PluginResourceOverride;
import org.operaton.bpm.webapp.plugin.spi.AppPlugin;
import org.operaton.commons.utils.ServiceLoaderUtil;

/**
 * @author Daniel Meyer
 *
 */
public abstract class AbstractAppRuntimeDelegate<T extends AppPlugin> implements AppRuntimeDelegate<T> {

  protected final AppPluginRegistry<T> pluginRegistry;
  protected final ProcessEngineProvider processEngineProvider;

  protected List<PluginResourceOverride> resourceOverrides;

  protected AbstractAppRuntimeDelegate(Class<T> pluginType) {
    pluginRegistry = new DefaultAppPluginRegistry<>(pluginType);
    processEngineProvider = loadProcessEngineProvider();
  }

  @Override
  public ProcessEngine getProcessEngine(String processEngineName) {
    try {
      return processEngineProvider.getProcessEngine(processEngineName);
    } catch (Exception e) {
      throw new ProcessEngineException("No process engine with name " + processEngineName + " found.", e);
    }
  }

  @Override
  public Set<String> getProcessEngineNames() {
    return processEngineProvider.getProcessEngineNames();
  }

  @Override
  public ProcessEngine getDefaultProcessEngine() {
    return processEngineProvider.getDefaultProcessEngine();
  }

  @Override
  public AppPluginRegistry<T> getAppPluginRegistry() {
    return pluginRegistry;
  }

  /**
   * Load the {@link ProcessEngineProvider} spi implementation.
   *
   * @return
   */
  protected ProcessEngineProvider loadProcessEngineProvider() {
    return ServiceLoaderUtil.loadSingleService(ProcessEngineProvider.class);
  }

  @Override
  public List<PluginResourceOverride> getResourceOverrides() {
    if(resourceOverrides == null) {
      initResourceOverrides();
    }
    return resourceOverrides;
  }

  protected synchronized void initResourceOverrides() {
    if(resourceOverrides == null) { // double-checked sync, do not remove
      resourceOverrides = new ArrayList<>();
      List<T> plugins = pluginRegistry.getPlugins();
      for (T p : plugins) {
        resourceOverrides.addAll(p.getResourceOverrides());
      }
    }
  }

}
