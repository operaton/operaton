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

import org.operaton.bpm.engine.impl.cfg.CompositeProcessEnginePlugin;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.run.OperatonBpmRun;
import org.operaton.bpm.run.property.OperatonBpmRunProcessEnginePluginProperty;
import org.operaton.bpm.run.property.OperatonBpmRunProperties;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = {OperatonBpmRun.class})
@ActiveProfiles(profiles = {"test-new-plugins"})
class OperatonRunProcessEnginePluginsRegistrationTest {

  @Autowired
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  @Autowired
  protected OperatonBpmRunProperties properties;

  protected List<ProcessEnginePlugin> plugins;

  @BeforeEach
  void setUp() {
    this.plugins = processEngineConfiguration.getProcessEnginePlugins();
  }

  @Test
  void shouldPickUpAllPluginConfigurations() {
    // given a OperatonBpmRunProperties instance
    String pluginOne = "org.operaton.bpm.run.test.plugins.TestFirstPlugin";
    String pluginTwo = "org.operaton.bpm.run.test.plugins.TestSecondPlugin";
    String pluginThree = "org.operaton.bpm.run.test.plugins.TestDefaultValuesPlugin";
    List<OperatonBpmRunProcessEnginePluginProperty> pluginConfigs = properties
        .getProcessEnginePlugins();

    // then
    // assert that all plugin configuration properties were mapped properly
    assertThat(pluginConfigs).hasSize(3);
    List<String> pluginClasses = pluginConfigs.stream()
        .map(OperatonBpmRunProcessEnginePluginProperty::getPluginClass)
        .collect(Collectors.toList());
    assertThat(pluginClasses)
        .contains(pluginOne, pluginTwo, pluginThree);

    OperatonBpmRunProcessEnginePluginProperty firstPlugin = pluginConfigs.get(0);
    Map<String, Object> firstPluginParameters = firstPlugin.getPluginParameters();
    assertThat(firstPluginParameters).containsOnly(
        entry("parameterOne", "valueOne"),
        entry("parameterTwo", true));

    OperatonBpmRunProcessEnginePluginProperty secondPlugin = properties.getProcessEnginePlugins().get(1);
    Map<String, Object> secondPluginParameters = secondPlugin.getPluginParameters();
    assertThat(secondPluginParameters).containsOnly(
        entry("parameterOne", 1.222),
        entry("parameterTwo", false),
        entry("parameterThree", 123));
  }

  @Test
  void shouldRegisterYamlDefinedPluginsWithProcessEngine() {
    // given a yaml config file defining process engine plugins

    // then
    // there is a single composite plugin
    assertThat(plugins).hasSize(1).hasOnlyElementsOfType(CompositeProcessEnginePlugin.class);
    CompositeProcessEnginePlugin compositePlugin =
        (CompositeProcessEnginePlugin) plugins.get(0);

    // the composite plugin contains all of the registered plugins
    List<ProcessEnginePlugin> registeredPlugins = compositePlugin.getPlugins();
    List<Class> classList = registeredPlugins.stream().map(ProcessEnginePlugin::getClass)
        .collect(Collectors.toList());
    assertThat(classList).contains(TestFirstPlugin.class, TestSecondPlugin.class, TestDefaultValuesPlugin.class);
  }

  @Test
  void shouldInitializeRegisteredPlugins() {
    // given
    List<ProcessEnginePlugin> registeredPlugins =
        ((CompositeProcessEnginePlugin) plugins.get(0)).getPlugins();

    // then
    // the test plugins are correctly initialized
    TestFirstPlugin firstPlugin = (TestFirstPlugin) registeredPlugins.stream()
        .filter(plugin -> plugin instanceof TestFirstPlugin).findFirst().get();
    assertThat(firstPlugin.getParameterOne()).isEqualTo("valueOne");
    assertThat(firstPlugin.getParameterTwo()).isTrue();

    TestSecondPlugin secondPlugin = (TestSecondPlugin) registeredPlugins.stream()
        .filter(plugin -> plugin instanceof TestSecondPlugin).findFirst().get();
    assertThat(secondPlugin.getParameterOne()).isEqualTo(1.222);
    assertThat(secondPlugin.getParameterTwo()).isFalse();
    assertThat(secondPlugin.getParameterThree()).isEqualTo(123);
  }

}