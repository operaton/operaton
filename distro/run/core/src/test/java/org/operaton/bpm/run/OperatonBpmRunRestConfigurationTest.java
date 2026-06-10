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
package org.operaton.bpm.run;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jersey.autoconfigure.JerseyApplicationPath;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import org.operaton.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.operaton.bpm.engine.rest.security.auth.impl.CompositeAuthenticationProvider;
import org.operaton.bpm.engine.rest.security.auth.impl.HttpBasicAuthenticationProvider;
import org.operaton.bpm.engine.rest.security.auth.impl.PseudoAuthenticationProvider;
import org.operaton.bpm.run.property.OperatonBpmRunAuthenticationProperties;
import org.operaton.bpm.run.property.OperatonBpmRunProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperatonBpmRunRestConfigurationTest {

  @Test
  void shouldUseHttpBasicAuthenticationProviderByDefault() {
    FilterRegistrationBean<?> registration = createAuthenticationFilterRegistration(null);

    assertThat(registration.getInitParameters())
        .containsEntry(ProcessEngineAuthenticationFilter.AUTHENTICATION_PROVIDER_PARAM,
            HttpBasicAuthenticationProvider.class.getName());
    assertThat(registration.getUrlPatterns()).containsExactly("/engine-rest/*");
    assertThat(registration.isAsyncSupported()).isTrue();
  }

  @Test
  void shouldUseCompositeAuthenticationProviderWhenConfigured() {
    FilterRegistrationBean<?> registration = createAuthenticationFilterRegistration(
        OperatonBpmRunAuthenticationProperties.COMPOSITE_AUTH);

    assertThat(registration.getInitParameters())
        .containsEntry(ProcessEngineAuthenticationFilter.AUTHENTICATION_PROVIDER_PARAM,
            CompositeAuthenticationProvider.class.getName());
  }

  @Test
  void shouldUsePseudoAuthenticationProviderWhenConfigured() {
    FilterRegistrationBean<?> registration = createAuthenticationFilterRegistration(
        OperatonBpmRunAuthenticationProperties.PSEUDO_AUTH);

    assertThat(registration.getInitParameters())
        .containsEntry(ProcessEngineAuthenticationFilter.AUTHENTICATION_PROVIDER_PARAM,
            PseudoAuthenticationProvider.class.getName());
  }

  private static FilterRegistrationBean<?> createAuthenticationFilterRegistration(String authentication) {
    OperatonBpmRunProperties properties = new OperatonBpmRunProperties();
    properties.getAuth().setAuthentication(authentication);
    OperatonBpmRunRestConfiguration configuration = new OperatonBpmRunRestConfiguration(properties);
    JerseyApplicationPath applicationPath = mock(JerseyApplicationPath.class);
    when(applicationPath.getUrlMapping()).thenReturn("/engine-rest/*");
    return configuration.processEngineAuthenticationFilter(applicationPath);
  }

}
