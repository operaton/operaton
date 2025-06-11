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
package org.operaton.bpm.cockpit.plugin;

import java.util.List;

import org.operaton.bpm.cockpit.plugin.spi.CockpitPlugin;

/**
 * The registry for cockpit plugins.
 *
 * @deprecated Use {@link AppPluginRegistry} instead.
 * @author nico.rehwaldt
 */
@Deprecated(forRemoval = true, since = "1.0")
public interface PluginRegistry {

  /**
   * @return all registered plugins
   */
  List<CockpitPlugin> getPlugins();

  /**
   * @param id
   * @return the registered plugin with the given name or
   * <code>null</code> if the plugin does not exist.
   */
  CockpitPlugin getPlugin(String id);
}
