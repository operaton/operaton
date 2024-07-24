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
package org.operaton.bpm.run;

import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.impl.cfg.CompositeProcessEnginePlugin;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.engine.impl.plugin.AdministratorAuthorizationPlugin;
import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin;
import org.operaton.bpm.run.property.OperatonBpmRunAdministratorAuthorizationProperties;
import org.operaton.bpm.run.property.OperatonBpmRunLdapProperties;
import org.operaton.bpm.run.property.OperatonBpmRunProcessEnginePluginProperty;
import org.operaton.bpm.run.property.OperatonBpmRunProperties;
import org.operaton.bpm.run.utils.OperatonBpmRunProcessEnginePluginHelper;
import org.operaton.bpm.spring.boot.starter.OperatonBpmAutoConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonDeploymentConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(OperatonBpmRunProperties.class)
@Configuration
@AutoConfigureAfter({ OperatonBpmAutoConfiguration.class })
public class OperatonBpmRunConfiguration {

  @Autowired
  OperatonBpmRunProperties operatonBpmRunProperties;

  @Bean
  @ConditionalOnProperty(name = "enabled", havingValue = "true", prefix = OperatonBpmRunLdapProperties.PREFIX)
  public LdapIdentityProviderPlugin ldapIdentityProviderPlugin() {
    return operatonBpmRunProperties.getLdap();
  }

  @Bean
  @ConditionalOnProperty(name = "enabled", havingValue = "true", prefix = OperatonBpmRunAdministratorAuthorizationProperties.PREFIX)
  public AdministratorAuthorizationPlugin administratorAuthorizationPlugin() {
    return operatonBpmRunProperties.getAdminAuth();
  }

  @Bean
  public ProcessEngineConfigurationImpl processEngineConfigurationImpl(List<ProcessEnginePlugin> processEnginePlugins) {
    final SpringProcessEngineConfiguration configuration = new OperatonBpmRunProcessEngineConfiguration();

    // register process engine plugins defined in yaml
    List<OperatonBpmRunProcessEnginePluginProperty> yamlPluginsInfo = operatonBpmRunProperties.getProcessEnginePlugins();
    OperatonBpmRunProcessEnginePluginHelper.registerYamlPlugins(processEnginePlugins, yamlPluginsInfo);

    configuration.getProcessEnginePlugins().add(new CompositeProcessEnginePlugin(processEnginePlugins));
    return configuration;
  }

  @Bean
  public static OperatonDeploymentConfiguration operatonDeploymentConfiguration() {
    return new OperatonBpmRunDeploymentConfiguration();
  }

}
