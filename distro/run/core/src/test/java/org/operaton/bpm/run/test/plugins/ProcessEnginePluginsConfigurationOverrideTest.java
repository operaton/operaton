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
import org.operaton.bpm.spring.boot.starter.spin.SpringBootSpinProcessEnginePlugin;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = {OperatonBpmRun.class})
@ActiveProfiles(profiles = {"test-new-plugins", "test-plugins-config-override"})
class ProcessEnginePluginsConfigurationOverrideTest {

  @Autowired
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  // IDEs can't autowire the Spin plugin bean since it's conditionally
  // created in our SB starter when the Spin plugin dependency is present.
  // This dependency is present in Operaton Run by default, so we can suppress this warning.
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  protected SpringBootSpinProcessEnginePlugin spinPlugin;

  protected List<ProcessEnginePlugin> plugins;

  @BeforeEach
  void setUp() {
    this.plugins = processEngineConfiguration.getProcessEnginePlugins();
}

  @Test
  void shouldOverrideDefaultPluginConfiguration() {
    // given
    List<ProcessEnginePlugin> registeredPlugins =
        ((CompositeProcessEnginePlugin) plugins.get(0)).getPlugins();

    // then
    // the Spin plugin properties are correctly applied
    SpringBootSpinProcessEnginePlugin overriddenSpinPlugin = (SpringBootSpinProcessEnginePlugin) registeredPlugins.stream()
        .filter(SpringBootSpinProcessEnginePlugin.class::isInstance).findFirst().orElseThrow();
    assertThat(overriddenSpinPlugin).isSameAs(spinPlugin);
    assertThat(overriddenSpinPlugin.isEnableXxeProcessing()).isTrue();
    assertThat(overriddenSpinPlugin.isEnableSecureXmlProcessing()).isFalse();
  }
}