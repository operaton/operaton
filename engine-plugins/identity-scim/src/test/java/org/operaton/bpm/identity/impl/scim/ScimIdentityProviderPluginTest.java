/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
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
package org.operaton.bpm.identity.impl.scim;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.identity.impl.scim.plugin.ScimIdentityProviderPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for SCIM Identity Provider Plugin.
 */
public class ScimIdentityProviderPluginTest {

  private ScimIdentityProviderPlugin plugin;
  private ProcessEngineConfigurationImpl processEngineConfiguration;

  @BeforeEach
  public void setup() {
    plugin = new ScimIdentityProviderPlugin();
    processEngineConfiguration = mock(ProcessEngineConfigurationImpl.class);
    when(processEngineConfiguration.getProcessEngineName()).thenReturn("default");
  }

  @Test
  public void testPluginExtendsConfiguration() {
    assertThat(plugin).isInstanceOf(ScimConfiguration.class);
  }

  @Test
  public void testPreInit() {
    plugin.setServerUrl("https://scim.example.com");
    plugin.setBearerToken("test-token");

    plugin.preInit(processEngineConfiguration);

    verify(processEngineConfiguration).setIdentityProviderSessionFactory(
        org.mockito.ArgumentMatchers.any(ScimIdentityProviderFactory.class));
  }

  @Test
  public void testPostInitDoesNothing() {
    // Should not throw any exceptions
    plugin.postInit(processEngineConfiguration);
  }

  @Test
  public void testPostProcessEngineBuildDoesNothing() {
    // Should not throw any exceptions
    plugin.postProcessEngineBuild(null);
  }
}
