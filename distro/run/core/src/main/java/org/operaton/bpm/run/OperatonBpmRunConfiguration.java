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
package org.operaton.bpm.run;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.engine.impl.plugin.AdministratorAuthorizationPlugin;
import org.operaton.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin;
import org.operaton.bpm.run.property.OperatonBpmRunAdministratorAuthorizationProperties;
import org.operaton.bpm.run.property.OperatonBpmRunLdapProperties;
import org.operaton.bpm.run.property.OperatonBpmRunProperties;
import org.operaton.bpm.spring.boot.starter.OperatonBpmAutoConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@EnableConfigurationProperties(OperatonBpmRunProperties.class)
@Configuration
@AutoConfigureAfter({ OperatonBpmAutoConfiguration.class })
public class OperatonBpmRunConfiguration {

  @Bean
  @ConditionalOnProperty(name = "enabled", havingValue = "true", prefix = OperatonBpmRunLdapProperties.PREFIX)
  public LdapIdentityProviderPlugin ldapIdentityProviderPlugin(OperatonBpmRunProperties properties) {
    return properties.getLdap();
  }

  @Bean
  @ConditionalOnProperty(name = "enabled", havingValue = "true", prefix = OperatonBpmRunAdministratorAuthorizationProperties.PREFIX)
  public AdministratorAuthorizationPlugin administratorAuthorizationPlugin(OperatonBpmRunProperties properties) {
    return properties.getAdminAuth();
  }

  @Bean
  public ProcessEngineConfigurationImpl processEngineConfigurationImpl(List<ProcessEnginePlugin> processEnginePluginsFromContext,
                                                                       OperatonBpmRunProperties properties,
                                                                       OperatonBpmRunDeploymentConfiguration deploymentConfig) {
    String normalizedDeploymentDir = deploymentConfig.getNormalizedDeploymentDir();
    boolean deployChangedOnly = properties.getDeployment().isDeployChangedOnly();
    var processEnginePluginsFromYaml = properties.getProcessEnginePlugins();

    return new OperatonBpmRunProcessEngineConfiguration(normalizedDeploymentDir, deployChangedOnly,
        processEnginePluginsFromContext, processEnginePluginsFromYaml);
  }

  @Bean
  public OperatonBpmRunDeploymentConfiguration operatonDeploymentConfiguration(@Value(
      "${operaton" + ".deploymentDir:#{null}}") String deploymentDir, OperatonBpmProperties operatonBpmProperties) {
    return new OperatonBpmRunDeploymentConfiguration(deploymentDir, operatonBpmProperties);
  }

}
