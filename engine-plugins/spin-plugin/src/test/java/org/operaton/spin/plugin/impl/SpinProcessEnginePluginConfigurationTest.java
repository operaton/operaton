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
package org.operaton.spin.plugin.impl;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class SpinProcessEnginePluginConfigurationTest {

  @RegisterExtension
  static ProcessEngineExtension engineExtension = ProcessEngineExtension.builder().configurationResource("custom.operaton.cfg.xml").build();

  ProcessEngine engine;
  ProcessEngineConfiguration engineConfiguration;

  @Test
  void shouldSetCustomSpinPluginProperties() {

    // when
    List<ProcessEnginePlugin> pluginList = ((ProcessEngineConfigurationImpl)engineConfiguration).getProcessEnginePlugins();

    // then
    assertThat(pluginList).hasOnlyElementsOfType(SpinProcessEnginePlugin.class).hasSize(1);
    SpinProcessEnginePlugin spinProcessEnginePlugin = (SpinProcessEnginePlugin) pluginList.get(0);
    assertThat(spinProcessEnginePlugin.isEnableXxeProcessing()).isTrue();
    assertThat(spinProcessEnginePlugin.isEnableSecureXmlProcessing()).isFalse();

  }

}
