/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0; you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.spring.boot.starter.security.oauth2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import org.operaton.bpm.engine.spring.SpringProcessEngineServicesConfiguration;
import org.operaton.bpm.spring.boot.starter.OperatonBpmAutoConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.security.oauth2.impl.ClientsOrResourceServerConfiguredCondition;
import org.operaton.bpm.spring.boot.starter.security.oauth2.impl.OAuth2IdentityProviderPlugin;

@AutoConfigureOrder(OperatonSpringSecurityOAuth2CommonAutoConfiguration.OPERATON_OAUTH2_ORDER)
@AutoConfigureAfter({OperatonBpmAutoConfiguration.class, SpringProcessEngineServicesConfiguration.class})
@ConditionalOnBean(OperatonBpmProperties.class)
@Conditional(ClientsOrResourceServerConfiguredCondition.class)
@EnableConfigurationProperties(OAuth2Properties.class)
@Configuration(proxyBeanMethods = false)
public class OperatonSpringSecurityOAuth2CommonAutoConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(OperatonSpringSecurityOAuth2CommonAutoConfiguration.class);
  public static final int OPERATON_OAUTH2_ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

  @Bean
  @ConditionalOnProperty(name = "identity-provider.enabled", havingValue = "true", prefix = OAuth2Properties.PREFIX, matchIfMissing = true)
  public OAuth2IdentityProviderPlugin identityProviderPlugin() {
    logger.debug("Registering OAuth2IdentityProviderPlugin");
    return new OAuth2IdentityProviderPlugin();
  }
}
