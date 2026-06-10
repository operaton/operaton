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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Nullable;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jersey.autoconfigure.JerseyApplicationPath;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import org.operaton.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.rest.OperatonBpmRestInitializer;
import org.operaton.bpm.spring.boot.starter.security.oauth2.impl.OAuth2AuthenticationProvider;
import org.operaton.bpm.spring.boot.starter.security.oauth2.impl.ResourceServerConfiguredCondition;

@AutoConfigureOrder(OperatonSpringSecurityOAuth2CommonAutoConfiguration.OPERATON_OAUTH2_ORDER + 1)
@AutoConfigureAfter(OperatonSpringSecurityOAuth2CommonAutoConfiguration.class)
@ConditionalOnBean(OperatonBpmProperties.class)
@ConditionalOnClass(OperatonBpmRestInitializer.class)
@Conditional(ResourceServerConfiguredCondition.class)
@EnableConfigurationProperties({OAuth2Properties.class, OAuth2ResourceServerProperties.class})
@Configuration(proxyBeanMethods = false)
public class OperatonSpringSecurityOAuth2EngineAutoConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(OperatonSpringSecurityOAuth2EngineAutoConfiguration.class);

  private final OAuth2Properties oAuth2Properties;
  private final OAuth2ResourceServerProperties oAuth2ResourceServerProperties;

  public OperatonSpringSecurityOAuth2EngineAutoConfiguration(OAuth2Properties oAuth2Properties,
                                                            OAuth2ResourceServerProperties oAuth2ResourceServerProperties) {
    this.oAuth2Properties = oAuth2Properties;
    this.oAuth2ResourceServerProperties = oAuth2ResourceServerProperties;
  }

  @Bean
  public FilterRegistrationBean<Filter> engineRestAuthenticationFilter(JerseyApplicationPath applicationPath) {
    FilterRegistrationBean<Filter> filterRegistration = new FilterRegistrationBean<>();
    filterRegistration.setName("Container Based Authentication Filter for engine-rest");
    filterRegistration.setFilter(new ProcessEngineAuthenticationFilter());
    filterRegistration.setInitParameters(Map.of(
        ProcessEngineAuthenticationFilter.AUTHENTICATION_PROVIDER_PARAM, OAuth2AuthenticationProvider.class.getName()));
    // make sure the filter is registered after the Spring Security Filter Chain
    filterRegistration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER + 1);
    filterRegistration.addUrlPatterns(applicationPath.getPath() + "/*");
    filterRegistration.setDispatcherTypes(DispatcherType.REQUEST);
    return filterRegistration;
  }

  @Bean
  @ConditionalOnProperty(name = "identity-provider.group-name-attribute", prefix = OAuth2Properties.PREFIX)
  protected JwtAuthenticationConverter oauth2JwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter((Converter<Jwt, Collection<GrantedAuthority>>) jwt -> {
      var identityProviderProperties = oAuth2Properties.getIdentityProvider();
      var groupNameAttribute = identityProviderProperties.getGroupNameAttribute();

      List<String> groups = null;
      try {
        groups = jwt.getClaimAsStringList(groupNameAttribute);
      } catch (IllegalArgumentException e) {
        logger.debug("Claim {} is not a list of strings, trying to parse as single string", groupNameAttribute);
        String groupsAttribute = jwt.getClaimAsString(groupNameAttribute);
        if (groupsAttribute != null) {
          groups = List.of(groupsAttribute.split(identityProviderProperties.getGroupNameDelimiter()));
        }
      }
      if (groups == null) {
        logger.debug("Claim {} is not available", groupNameAttribute);
        return List.of();
      }
      return groups.stream()
          .map(group -> (GrantedAuthority) new SimpleGrantedAuthority(group))
          .toList();
    });

    String principalClaimName = Optional.ofNullable(oAuth2ResourceServerProperties.getJwt())
        .map(OAuth2ResourceServerProperties.Jwt::getPrincipalClaimName)
        .filter(s -> !s.isBlank())
        .orElse("preferred_username");
    converter.setPrincipalClaimName(principalClaimName);

    return converter;
  }

  @Bean
  @Order(1)
  public SecurityFilterChain engineRestSecurityFilterChain(HttpSecurity http,
                                                          JerseyApplicationPath applicationPath,
                                                          @Nullable JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
    logger.info("Enabling Operaton Spring Security oauth2 integration for engine-rest");
    String engineRestPath = applicationPath.getPath();

    // @formatter:off
    http.securityMatcher(request -> {
          String fullPath = request.getServletPath() + (request.getPathInfo() != null ? request.getPathInfo() : "");
          String requestUrl = fullPath.startsWith(engineRestPath) ? fullPath.substring(engineRestPath.length()) : fullPath;
          return fullPath.startsWith(engineRestPath)
              && ProcessEngineAuthenticationFilter.requiresEngineAuthentication(requestUrl);
        })
        .authorizeHttpRequests(c -> c
          .requestMatchers(engineRestPath + "/**").authenticated())
        .oauth2ResourceServer(oauth2 -> {
          if (jwtAuthenticationConverter != null) {
            oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter));
          }
        });
    // @formatter:on

    return http.build();
  }
}
