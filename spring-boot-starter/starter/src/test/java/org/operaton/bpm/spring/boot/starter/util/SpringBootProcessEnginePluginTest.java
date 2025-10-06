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
package org.operaton.bpm.spring.boot.starter.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SpringBootProcessEnginePluginTest {

  private static class DummySpringPlugin extends SpringBootProcessEnginePlugin {
    public boolean preInit;
    public boolean postInit;

    @Override
    public void postInit(SpringProcessEngineConfiguration processEngineConfiguration) {
      postInit = true;
    }

    @Override
    public void preInit(SpringProcessEngineConfiguration processEngineConfiguration) {
      preInit = true;
    }
  }

  @Test
  void delegate_for_springConfig() {
    ProcessEngineConfigurationImpl c = new SpringProcessEngineConfiguration();

    DummySpringPlugin plugin = new DummySpringPlugin();

    plugin.preInit(c);
    plugin.postInit(c);

    assertThat(plugin.preInit).isTrue();
    assertThat(plugin.postInit).isTrue();
  }

  @Test
  void no_delegate_for_standaloneConfig() {
    ProcessEngineConfigurationImpl c = new StandaloneInMemProcessEngineConfiguration();

    DummySpringPlugin plugin = new DummySpringPlugin();

    plugin.preInit(c);
    plugin.postInit(c);

    assertThat(plugin.preInit).isFalse();
    assertThat(plugin.postInit).isFalse();
  }


}
