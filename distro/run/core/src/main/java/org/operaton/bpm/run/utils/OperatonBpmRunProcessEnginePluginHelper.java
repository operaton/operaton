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
package org.operaton.bpm.run.utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.run.property.OperatonBpmRunProcessEnginePluginProperty;
import org.operaton.bpm.spring.boot.starter.util.SpringBootStarterException;
import org.operaton.bpm.spring.boot.starter.util.SpringBootStarterPropertyHelper;

public final class OperatonBpmRunProcessEnginePluginHelper {

  protected static final OperatonBpmRunLogger LOG = OperatonBpmRunLogger.LOG;

  private OperatonBpmRunProcessEnginePluginHelper() {
  }

  public static void registerYamlPlugins(List<ProcessEnginePlugin> processEnginePlugins,
                                         List<OperatonBpmRunProcessEnginePluginProperty> pluginsInfo) {

    for (OperatonBpmRunProcessEnginePluginProperty pluginInfo : pluginsInfo) {
      String className = pluginInfo.getPluginClass();
      ProcessEnginePlugin plugin = getOrCreatePluginInstance(processEnginePlugins, className);

      Map<String, Object> pluginParameters = pluginInfo.getPluginParameters();
      populatePluginInstance(plugin, pluginParameters);

      LOG.processEnginePluginRegistered(className);
    }
  }

  protected static ProcessEnginePlugin getOrCreatePluginInstance(
      List<ProcessEnginePlugin> processEnginePlugins,
      String className) {

    try {
      // find class on classpath
      Class<? extends ProcessEnginePlugin> pluginClass = ReflectUtil
          .loadClass(className, null);

      // check if an instance of the process engine plugin is already present
      Optional<ProcessEnginePlugin> plugin = processEnginePlugins.stream()
          .filter(pluginClass::isInstance).findFirst();

      // get existing plugin instance or create a new one and add it to the list
      return plugin.orElseGet(() -> {

        ProcessEnginePlugin newPlugin = ReflectUtil.createInstance(pluginClass);
        processEnginePlugins.add(newPlugin);

        return newPlugin;
      });

    } catch (ClassNotFoundException | ClassCastException | ProcessEngineException e) {
      throw LOG.failedProcessEnginePluginInstantiation(className, e);
    }
  }

  protected static void populatePluginInstance(ProcessEnginePlugin plugin,
                                               Map<String, Object> properties) {
    try {
      SpringBootStarterPropertyHelper.applyProperties(plugin, properties, false);
    } catch (SpringBootStarterException e) {
      throw LOG.pluginPropertyNotFound(plugin.getClass().getCanonicalName(), e);
    }
  }

}
