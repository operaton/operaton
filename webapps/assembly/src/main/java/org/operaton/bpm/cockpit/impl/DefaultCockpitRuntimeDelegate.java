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
package org.operaton.bpm.cockpit.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import org.operaton.bpm.cockpit.CockpitRuntimeDelegate;
import org.operaton.bpm.cockpit.db.CommandExecutor;
import org.operaton.bpm.cockpit.db.QueryService;
import org.operaton.bpm.cockpit.impl.db.CommandExecutorImpl;
import org.operaton.bpm.cockpit.impl.db.QueryServiceImpl;
import org.operaton.bpm.cockpit.impl.plugin.DefaultPluginRegistry;
import org.operaton.bpm.cockpit.plugin.PluginRegistry;
import org.operaton.bpm.cockpit.plugin.spi.CockpitPlugin;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.webapp.impl.AbstractAppRuntimeDelegate;

/**
 * <p>This is the default {@link CockpitRuntimeDelegate} implementation that provides
 * the operaton cockpit plugin services (i.e. {@link QueryService} and
 * {@link CommandExecutor}).</p>
 *
 * @author roman.smirnov
 * @author nico.rehwaldt
 */
public class DefaultCockpitRuntimeDelegate extends AbstractAppRuntimeDelegate<CockpitPlugin> implements CockpitRuntimeDelegate {

  private final Map<String, CommandExecutor> commandExecutors;

  public DefaultCockpitRuntimeDelegate() {
    super(CockpitPlugin.class);
    this.commandExecutors = new ConcurrentHashMap<>();
  }

  @Override
  public QueryService getQueryService(String processEngineName) {
    CommandExecutor commandExecutor = getCommandExecutor(processEngineName);
    return new QueryServiceImpl(commandExecutor);
  }

  @Override
  public CommandExecutor getCommandExecutor(String processEngineName) {
    synchronized (commandExecutors) {
      return commandExecutors.computeIfAbsent(processEngineName, this::createCommandExecutor);
    }
  }

  /**
   * @deprecated Use {@link #getAppPluginRegistry()} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  @Override
  public PluginRegistry getPluginRegistry() {
    return new DefaultPluginRegistry(pluginRegistry);
  }

  /**
   * Returns the list of mapping files that should be used to create the
   * session factory for this runtime.
   *
   * @return
   */
  protected List<String> getMappingFiles() {
    List<CockpitPlugin> cockpitPlugins = pluginRegistry.getPlugins();

    List<String> mappingFiles = new ArrayList<>();
    for (CockpitPlugin plugin: cockpitPlugins) {
      mappingFiles.addAll(plugin.getMappingFiles());
    }

    return mappingFiles;
  }

  /**
   * Create command executor for the engine with the given name
   *
   * @param processEngineName
   * @return
   */
  protected CommandExecutor createCommandExecutor(String processEngineName) {

    ProcessEngine processEngine = getProcessEngine(processEngineName);
    if (processEngine == null) {
      throw new ProcessEngineException("No process engine with name %s found.".formatted(processEngineName));
    }

    ProcessEngineConfigurationImpl processEngineConfiguration = ((ProcessEngineImpl)processEngine).getProcessEngineConfiguration();
    List<String> mappingFiles = getMappingFiles();

    return new CommandExecutorImpl(processEngineConfiguration, mappingFiles);
  }

}
