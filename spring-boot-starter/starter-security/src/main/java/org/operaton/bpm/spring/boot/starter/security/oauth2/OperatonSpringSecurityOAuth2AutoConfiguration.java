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
package org.operaton.bpm.spring.boot.starter.security.oauth2;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import org.operaton.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.operaton.bpm.engine.spring.SpringProcessEngineServicesConfiguration;
import org.operaton.bpm.spring.boot.starter.OperatonBpmAutoConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.property.WebappProperty;
import org.operaton.bpm.spring.boot.starter.security.oauth2.impl.AuthorizeTokenFilter;
import org.operaton.bpm.spring.boot.starter.security.oauth2.impl.OAuth2AuthenticationProvider;
import org.operaton.bpm.spring.boot.starter.security.oauth2.impl.OAuth2GrantedAuthoritiesMapper;
import org.operaton.bpm.spring.boot.starter.security.oauth2.impl.OAuth2IdentityProviderPlugin;
import org.operaton.bpm.webapp.impl.security.auth.ContainerBasedAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Map;

@AutoConfigureOrder(OperatonSpringSecurityOAuth2AutoConfiguration.CAMUNDA_OAUTH2_ORDER)
@AutoConfigureAfter({ OperatonBpmAutoConfiguration.class, SpringProcessEngineServicesConfiguration.class })
@ConditionalOnBean(OperatonBpmProperties.class)
@Conditional(ClientsConfiguredCondition.class)
@EnableConfigurationProperties(OAuth2Properties.class)
public class OperatonSpringSecurityOAuth2AutoConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(OperatonSpringSecurityOAuth2AutoConfiguration.class);
  public static final int CAMUNDA_OAUTH2_ORDER = Ordered.HIGHEST_PRECEDENCE + 100;
  private final OAuth2Properties oAuth2Properties;
  private final String webappPath;

  public OperatonSpringSecurityOAuth2AutoConfiguration(OperatonBpmProperties properties,
                                                      OAuth2Properties oAuth2Properties) {
    this.oAuth2Properties = oAuth2Properties;
    WebappProperty webapp = properties.getWebapp();
    this.webappPath = webapp.getApplicationPath();
  }

  @Bean
  public FilterRegistrationBean<?> webappAuthenticationFilter() {
    FilterRegistrationBean<Filter> filterRegistration = new FilterRegistrationBean<>();
    filterRegistration.setName("Container Based Authentication Filter");
    filterRegistration.setFilter(new ContainerBasedAuthenticationFilter());
    filterRegistration.setInitParameters(Map.of(
        ProcessEngineAuthenticationFilter.AUTHENTICATION_PROVIDER_PARAM, OAuth2AuthenticationProvider.class.getName()));
    // make sure the filter is registered after the Spring Security Filter Chain
    filterRegistration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER + 1);
    filterRegistration.addUrlPatterns(webappPath + "/app/*", webappPath + "/api/*");
    filterRegistration.setDispatcherTypes(DispatcherType.REQUEST);
    return filterRegistration;
  }

  @Bean
  @ConditionalOnProperty(name = "identity-provider.enabled", prefix = OAuth2Properties.PREFIX)
  public OAuth2IdentityProviderPlugin identityProviderPlugin() {
    logger.debug("Registering OAuth2IdentityProviderPlugin");
    return new OAuth2IdentityProviderPlugin();
  }

  @Bean
  @ConditionalOnProperty(name = "identity-provider.group-name-attribute", prefix = OAuth2Properties.PREFIX)
  protected GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
    logger.debug("Registering OAuth2GrantedAuthoritiesMapper");
    return new OAuth2GrantedAuthoritiesMapper(oAuth2Properties);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, OAuth2AuthorizedClientManager clientManager)
      throws Exception {
    logger.info("Enabling Operaton Spring Security oauth2 integration");

    var validateTokenFilter = new AuthorizeTokenFilter(clientManager);

    // @formatter:off
    http.authorizeHttpRequests(c -> c
            .requestMatchers(webappPath + "/app/**").authenticated()
            .requestMatchers(webappPath + "/api/**").authenticated()
            .anyRequest().permitAll()
        )
        .addFilterAfter(validateTokenFilter, OAuth2AuthorizationRequestRedirectFilter.class)
        .anonymous(AbstractHttpConfigurer::disable)
        .oauth2Login(Customizer.withDefaults())
        .oidcLogout(Customizer.withDefaults())
        .oauth2Client(Customizer.withDefaults())
        .cors(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable);
    // @formatter:on

    return http.build();
  }

}
