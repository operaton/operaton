/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements.
 * Modifications Copyright 2026 the Operaton contributors.
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
package org.operaton.bpm.identity.scim.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.identity.impl.scim.plugin.ScimIdentityProviderPlugin;

/**
 * JUnit 5 extension for managing the SCIM test server lifecycle.
 */
public class ScimTestEnvironmentExtension implements BeforeAllCallback, AfterAllCallback {

  protected ScimTestEnvironment scimTestEnvironment;

  public ScimTestEnvironmentExtension() {
    this.scimTestEnvironment = new ScimTestEnvironment(0);
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    scimTestEnvironment.init();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    scimTestEnvironment.shutdown();
  }

  public void injectScimUrlIntoProcessEngineConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
    configureScimPlugin(processEngineConfiguration, "id");
  }

  public void injectScimUrlWithExternalGroupIds(ProcessEngineConfigurationImpl processEngineConfiguration) {
    configureScimPlugin(processEngineConfiguration, "externalId");
  }

  public ScimTestEnvironment getScimTestEnvironment() {
    return scimTestEnvironment;
  }

  protected void configureScimPlugin(ProcessEngineConfigurationImpl processEngineConfiguration, String groupIdAttribute) {
    ScimIdentityProviderPlugin scimPlugin = findOrCreateScimPlugin(processEngineConfiguration);
    scimPlugin.setServerUrl(scimTestEnvironment.getServerUrl());
    scimPlugin.setAuthenticationType("bearer");
    scimPlugin.setBearerToken("test-token");
    scimPlugin.setUserIdAttribute("userName");
    scimPlugin.setUserFirstnameAttribute("name.givenName");
    scimPlugin.setUserLastnameAttribute("name.familyName");
    scimPlugin.setGroupIdAttribute(groupIdAttribute);
    scimPlugin.setGroupNameAttribute("displayName");
    scimPlugin.setAuthorizationCheckEnabled(false);
  }

  protected ScimIdentityProviderPlugin findOrCreateScimPlugin(ProcessEngineConfigurationImpl processEngineConfiguration) {
    List<ProcessEnginePlugin> plugins = processEngineConfiguration.getProcessEnginePlugins();
    if (plugins == null) {
      plugins = new ArrayList<>();
      processEngineConfiguration.setProcessEnginePlugins(plugins);
    }
    final List<ProcessEnginePlugin> processEnginePlugins = plugins;

    return processEnginePlugins.stream()
        .filter(ScimIdentityProviderPlugin.class::isInstance)
        .map(ScimIdentityProviderPlugin.class::cast)
        .findFirst()
        .orElseGet(() -> {
          ScimIdentityProviderPlugin plugin = new ScimIdentityProviderPlugin();
          processEnginePlugins.add(plugin);
          return plugin;
        });
  }
}
