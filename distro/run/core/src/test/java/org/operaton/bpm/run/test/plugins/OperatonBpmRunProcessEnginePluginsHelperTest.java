/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.run.test.plugins;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.run.property.OperatonBpmRunProcessEnginePluginProperty;
import org.operaton.bpm.run.utils.OperatonBpmRunProcessEnginePluginHelper;
import org.junit.Test;

public class OperatonBpmRunProcessEnginePluginsHelperTest {

  @Test
  public void shouldReportMissingPluginClass() {
    // given
    // a process engine plugins map with a class not on the classpath
    OperatonBpmRunProcessEnginePluginProperty pluginConfig = new OperatonBpmRunProcessEnginePluginProperty();
    pluginConfig.setPluginClass("org.operaton.bpm.run.test.plugins.TestThirdPlugin");
    pluginConfig.setPluginParameters(Collections.EMPTY_MAP);
    // a process engine plugins map with a plugin not configured properly
    List<OperatonBpmRunProcessEnginePluginProperty> plugins =
        Collections.singletonList(pluginConfig);
    List<ProcessEnginePlugin> pluginList = Collections.EMPTY_LIST;

    // when
    assertThatThrownBy(() -> OperatonBpmRunProcessEnginePluginHelper.registerYamlPlugins(pluginList,
                                                                                        plugins))
        // then
        // an exception is thrown with a user-friendly message asking to check the plugin class
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Unable to register the process engine plugin " +
                                  "'org.operaton.bpm.run.test.plugins.TestThirdPlugin'.");
  }

  @Test
  public void shouldReportWrongPluginClass() {
    // given
    // a process engine plugins map with a class not implementing the ProcessEnginePlugin interface
    OperatonBpmRunProcessEnginePluginProperty pluginConfig = new OperatonBpmRunProcessEnginePluginProperty();
    pluginConfig.setPluginClass("org.operaton.bpm.run.test.plugins.TestFalsePlugin");
    pluginConfig.setPluginParameters(Collections.EMPTY_MAP);
    // a process engine plugins map with a plugin not configured properly
    List<OperatonBpmRunProcessEnginePluginProperty> plugins =
        Collections.singletonList(pluginConfig);
    List<ProcessEnginePlugin> pluginList = Collections.EMPTY_LIST;

    // when
    assertThatThrownBy(() -> OperatonBpmRunProcessEnginePluginHelper.registerYamlPlugins(pluginList,
                                                                                        plugins))
        // then
        // an exception is thrown with a user-friendly message asking to check the plugin class
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("'org.operaton.bpm.run.test.plugins.TestFalsePlugin'. " +
                                  "Please ensure that the correct plugin class is configured");
  }

  @Test
  public void shouldReportMissingPluginConfigurationProperty() {
    // given
    OperatonBpmRunProcessEnginePluginProperty pluginConfig = new OperatonBpmRunProcessEnginePluginProperty();
    pluginConfig.setPluginClass("org.operaton.bpm.run.test.plugins.TestFirstPlugin");
    pluginConfig.setPluginParameters(Collections.singletonMap("wrongKey", "wrongValue"));
    // a process engine plugins map with a plugin not configured properly
    List<OperatonBpmRunProcessEnginePluginProperty> plugins =
        Collections.singletonList(pluginConfig);
    List<ProcessEnginePlugin> pluginList = new ArrayList<>();

    // when
    assertThatThrownBy(() -> OperatonBpmRunProcessEnginePluginHelper.registerYamlPlugins(pluginList,
                                                                                        plugins))
        // then
        // an exception is thrown with a user-friendly message asking to check the config options
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Please check the configuration options for plugin " +
                                  "'org.operaton.bpm.run.test.plugins.TestFirstPlugin'.");
  }

}