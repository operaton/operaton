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
package org.operaton.bpm.identity.impl.scim.plugin;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.identity.impl.scim.ScimConfiguration;
import org.operaton.bpm.identity.impl.scim.ScimIdentityProviderFactory;
import org.operaton.bpm.identity.impl.scim.util.ScimPluginLogger;

/**
 * Process Engine Plugin for SCIM Identity Provider.
 * 
 * This class extends ScimConfiguration so that configuration properties
 * can be set directly on this class via the properties element
 * in bpm-platform.xml / processes.xml
 */
public class ScimIdentityProviderPlugin extends ScimConfiguration implements ProcessEnginePlugin {

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    ScimPluginLogger.INSTANCE.pluginActivated(getClass().getSimpleName(), 
        processEngineConfiguration.getProcessEngineName());

    if (isAcceptUntrustedCertificates()) {
      ScimPluginLogger.INSTANCE.acceptingUntrustedCertificates();
    }

    ScimIdentityProviderFactory scimIdentityProviderFactory = new ScimIdentityProviderFactory();
    scimIdentityProviderFactory.setScimConfiguration(this);
    processEngineConfiguration.setIdentityProviderSessionFactory(scimIdentityProviderFactory);
  }

  @Override
  public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    // nothing to do
  }

  @Override
  public void postProcessEngineBuild(ProcessEngine processEngine) {
    // nothing to do
  }
}
