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
package org.operaton.bpm.admin.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.admin.AdminRuntimeDelegate;
import org.operaton.bpm.admin.plugin.spi.AdminPlugin;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.webapp.db.CommandExecutor;
import org.operaton.bpm.webapp.db.QueryService;
import org.operaton.bpm.webapp.impl.AbstractAppRuntimeDelegate;
import org.operaton.bpm.webapp.impl.db.CommandExecutorImpl;
import org.operaton.bpm.webapp.impl.db.QueryServiceImpl;

/**
 * @author Daniel Meyer
 *
 */
public class DefaultAdminRuntimeDelegate extends AbstractAppRuntimeDelegate<AdminPlugin> implements AdminRuntimeDelegate {

  private static final List<String> MAPPING_FILES = List.of(
      "org/operaton/bpm/admin/plugin/base/queries/metrics.xml"
  );

  protected final Map<String, CommandExecutor> commandExecutors;

  public DefaultAdminRuntimeDelegate() {
    super(AdminPlugin.class);
    this.commandExecutors = new HashMap<>();
  }

  @Override
  public QueryService getQueryService(String processEngineName) {
    CommandExecutor commandExecutor = getCommandExecutor(processEngineName);
    return new QueryServiceImpl(commandExecutor);
  }

  public CommandExecutor getCommandExecutor(String processEngineName) {
    return commandExecutors.computeIfAbsent(processEngineName, this::createCommandExecutor);
  }

  /**
   * Create command executor for the engine with the given name
   *
   * @param processEngineName the process engine name
   * @return the command executor
   */
  protected CommandExecutor createCommandExecutor(String processEngineName) {
    ProcessEngine processEngine = getProcessEngine(processEngineName);
    if (processEngine == null) {
      throw new ProcessEngineException("No process engine with name %s found.".formatted(processEngineName));
    }

    ProcessEngineConfigurationImpl processEngineConfiguration = ((ProcessEngineImpl) processEngine)
        .getProcessEngineConfiguration();

    return new CommandExecutorImpl(processEngineConfiguration, MAPPING_FILES);
  }

}
