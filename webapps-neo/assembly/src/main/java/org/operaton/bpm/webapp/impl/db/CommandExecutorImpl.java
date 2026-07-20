/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements.
 * Modifications Copyright the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.webapp.impl.db;

import java.util.List;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.webapp.db.CommandExecutor;

public class CommandExecutorImpl implements CommandExecutor {

  private QuerySessionFactory sessionFactory;

  public CommandExecutorImpl() { }

  public CommandExecutorImpl(ProcessEngineConfigurationImpl processEngineConfiguration, List<String> mappingFiles) {
    sessionFactory = new QuerySessionFactory();
    sessionFactory.initFromProcessEngineConfiguration(processEngineConfiguration, mappingFiles);
  }

  @Override
  public <T> T executeCommand(Command<T> command) {
    return sessionFactory.getCommandExecutorTxRequired().execute(command);
  }
}

