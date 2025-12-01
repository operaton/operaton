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

import java.lang.reflect.Field;
import jakarta.servlet.Filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.operaton.bpm.engine.rest.security.auth.AuthenticationResult;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.spring.boot.starter.security.oauth2.impl.AuthorizeTokenFilter;
import org.operaton.bpm.spring.boot.starter.security.oauth2.impl.OAuth2AuthenticationProvider;
import org.operaton.bpm.webapp.impl.security.auth.ContainerBasedAuthenticationFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@AutoConfigureMockMvc
@TestPropertySource("/oauth2-mock.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OperatonBpmSecurityAutoConfigOauth2ApplicationIT extends AbstractSpringSecurityIT {

  protected static final String UNAUTHORIZED_USER = "mary";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private FilterRegistrationBean<Filter> filterRegistrationBean;

  @Autowired
  private ClientRegistrationRepository registrations;

  @MockitoBean
  private OAuth2AuthorizedClientService authorizedClientService;

  @RegisterExtension
  ProcessEngineLoggingExtension logger = new ProcessEngineLoggingExtension().watch(
    AuthorizeTokenFilter.class.getCanonicalName());

  private OAuth2AuthenticationProvider spiedAuthenticationProvider;

  @BeforeEach
  void init() throws Exception {
    spyAuthenticationProvider();
  }

  @Test
  void testSpringSecurityAutoConfigurationCorrectlySet() {
    // given oauth2 client configured
    // when retrieving config beans then only OAuth2AutoConfiguration is present
    assertThat(getBeanForClass(OperatonSpringSecurityOAuth2AutoConfiguration.class,
      mockMvc.getDispatcherServlet().getWebApplicationContext())).isNotNull();
    assertThat(getBeanForClass(OperatonBpmSpringSecurityDisableAutoConfiguration.class,
      mockMvc.getDispatcherServlet().getWebApplicationContext())).isNull();
  }

  @Test
  void testWebappWithoutAuthentication() throws Exception {
    // given no authentication

    // when
    mockMvc.perform(MockMvcRequestBuilders.get(baseUrl + "/operaton/api/engine/engine/default/user")
        .accept(MediaType.APPLICATION_JSON))
      .andDo(MockMvcResultHandlers.print())
      // then oauth2 redirection occurs
      .andExpect(MockMvcResultMatchers.status().isFound())
      .andExpect(MockMvcResultMatchers.header().exists(LOCATION))
      .andExpect(MockMvcResultMatchers.header().string(LOCATION, "/oauth2/authorization/" + PROVIDER));
  }

  @Test
  void testWebappApiWithAuthorizedUser() throws Exception {
    // given authorized oauth2 authentication token
    OAuth2AuthenticationToken authenticationToken = createToken(AUTHORIZED_USER);
    createAuthorizedClient(authenticationToken, registrations, authorizedClientService);

    // when
    mockMvc.perform(MockMvcRequestBuilders.get(baseUrl + "/operaton/api/engine/engine/default/user")
        .accept(MediaType.APPLICATION_JSON)
        .with(authentication(authenticationToken)))
      // then call is successful
      .andDo(MockMvcResultHandlers.print())
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andExpect(MockMvcResultMatchers.content().json(EXPECTED_NAME_DEFAULT));
  }

  @Test
  void testWebappWithUnauthorizedUser() throws Exception {
    // given unauthorized oauth2 authentication token
    OAuth2AuthenticationToken authenticationToken = createToken(UNAUTHORIZED_USER);
    createAuthorizedClient(authenticationToken, registrations, authorizedClientService);

    // when
    mockMvc.perform(MockMvcRequestBuilders.get(baseUrl + "/operaton/api/engine/engine/default/user")
        .accept(MediaType.APPLICATION_JSON)
        .with(authentication(authenticationToken)))
      // then authorization fails and redirection occurs
      .andExpect(MockMvcResultMatchers.status().isFound())
      .andExpect(MockMvcResultMatchers.header().exists(LOCATION))
      .andExpect(MockMvcResultMatchers.header().string(LOCATION, "/oauth2/authorization/" + PROVIDER));

    String expectedWarn = "Authorize failed for '" + UNAUTHORIZED_USER + "'";
    assertThat(logger.getFilteredLog(expectedWarn)).hasSize(1);
    verifyNoInteractions(spiedAuthenticationProvider);
  }

  @Test
  void testOauth2AuthenticationProvider() throws Exception {
    // given authorized oauth2 authentication token
    ResultCaptor<AuthenticationResult> resultCaptor = new ResultCaptor<>();
    doAnswer(resultCaptor).when(spiedAuthenticationProvider).extractAuthenticatedUser(any(), any());
    OAuth2AuthenticationToken authenticationToken = createToken(AUTHORIZED_USER);
    createAuthorizedClient(authenticationToken, registrations, authorizedClientService);

    // when
    mockMvc.perform(MockMvcRequestBuilders.get(baseUrl + "/operaton/api/engine/engine/default/user")
        .accept(MediaType.APPLICATION_JSON)
        .with(authentication(authenticationToken)))
      // then call is successful
      .andDo(MockMvcResultHandlers.print())
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andExpect(MockMvcResultMatchers.content().json(EXPECTED_NAME_DEFAULT));

    // and authentication provider was called and returned expected authentication result
    verify(spiedAuthenticationProvider).extractAuthenticatedUser(any(), any());
    AuthenticationResult authenticationResult = resultCaptor.result;
    assertThat(authenticationResult.isAuthenticated()).isTrue();
    assertThat(authenticationResult.getAuthenticatedUser()).isEqualTo(AUTHORIZED_USER);
  }

  private void spyAuthenticationProvider() throws NoSuchFieldException, IllegalAccessException {
    ContainerBasedAuthenticationFilter filter = (ContainerBasedAuthenticationFilter) filterRegistrationBean.getFilter();
    Field authProviderField = ContainerBasedAuthenticationFilter.class.getDeclaredField("authenticationProvider");
    authProviderField.setAccessible(true);
    Object realAuthenticationProvider = authProviderField.get(filter);
    spiedAuthenticationProvider = (OAuth2AuthenticationProvider) spy(realAuthenticationProvider);
    authProviderField.set(filter, spiedAuthenticationProvider);
  }
}
