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
package org.operaton.bpm.cockpit;

import org.operaton.bpm.cockpit.db.CommandExecutor;
import org.operaton.bpm.cockpit.db.QueryService;
import org.operaton.bpm.cockpit.plugin.PluginRegistry;
import org.operaton.bpm.cockpit.plugin.spi.CockpitPlugin;
import org.operaton.bpm.webapp.AppRuntimeDelegate;

/**
 * <p>The {@link CockpitRuntimeDelegate} is a delegate to provide
 * the operaton cockpit plugin service.</p>
 *
 * @author roman.smirnov
 */
public interface CockpitRuntimeDelegate extends AppRuntimeDelegate<CockpitPlugin> {

  /**
   * Returns a configured {@link QueryService} to execute custom
   * statements to the corresponding process engine.
   * @param processEngineName
   * @return a {@link QueryService}
   */
  QueryService getQueryService(String processEngineName);

  /**
   * Returns a configured {@link CommandExecutor} to execute
   * commands to the corresponding process engine.
   * @param processEngineName
   * @return a {@link CommandExecutor}
   */
  CommandExecutor getCommandExecutor(String processEngineName);

  /**
   * A registry that provides access to the plugins registered
   * in the application.
   *
   * @return
   */
  @Deprecated(since = "1.0")
  PluginRegistry getPluginRegistry();

}
