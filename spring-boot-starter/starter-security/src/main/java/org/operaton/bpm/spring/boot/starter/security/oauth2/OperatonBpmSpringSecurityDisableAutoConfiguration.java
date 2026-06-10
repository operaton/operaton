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
package org.operaton.bpm.spring.boot.starter.security.oauth2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.security.oauth2.impl.ClientsNotConfiguredCondition;

@Conditional(ClientsNotConfiguredCondition.class)
@ConditionalOnBean(OperatonBpmProperties.class)
@Configuration(proxyBeanMethods = false)
public class OperatonBpmSpringSecurityDisableAutoConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(OperatonBpmSpringSecurityDisableAutoConfiguration.class);
  private final String webappPath;

  public OperatonBpmSpringSecurityDisableAutoConfiguration(OperatonBpmProperties properties) {
    this.webappPath = properties.getWebapp().getApplicationPath();
  }

  @Bean
  public SecurityFilterChain filterChainWebappPermitAll(HttpSecurity http) throws Exception {
    logger.info("Disabling Operaton Spring Security oauth2 integration for webapps");

    // @formatter:off
    http.securityMatcher(request -> {
          String fullPath = request.getServletPath() + (request.getPathInfo() != null ? request.getPathInfo() : "");
          return fullPath.startsWith(webappPath + "/app/") || fullPath.startsWith(webappPath + "/api/");
        })
        .authorizeHttpRequests(customizer -> customizer.anyRequest().permitAll())
        // disable anonymous access to avoid accessing the OAuth2IdentityProvider
        .anonymous(AbstractHttpConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable);
    // @formatter:on
    return http.build();
  }

}
