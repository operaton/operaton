/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements.
 * Modifications Copyright the Operaton contributors.
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
package org.operaton.bpm.webapp.neo.impl.security.filter.headersec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.webapp.neo.impl.util.HeaderRule;

import static org.operaton.bpm.webapp.neo.impl.security.filter.headersec.provider.impl.ContentSecurityPolicyProvider.HEADER_DEFAULT_VALUE;
import static org.operaton.bpm.webapp.neo.impl.security.filter.headersec.provider.impl.ContentSecurityPolicyProvider.HEADER_NAME;
import static org.operaton.bpm.webapp.neo.impl.security.filter.headersec.provider.impl.ContentSecurityPolicyProvider.HEADER_NONCE_PLACEHOLDER;
import static org.assertj.core.api.Assertions.assertThat;

class ContentSecurityPolicyTest {

  @RegisterExtension
  HeaderRule headerRule = new HeaderRule();

  @Test
  void shouldConfigureEnabledByDefault() {
    // given
    headerRule.startServer("web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    String expectedHeaderPattern = HEADER_DEFAULT_VALUE.replace(HEADER_NONCE_PLACEHOLDER, "'nonce-([-_a-zA-Z\\d]*)'");
    assertThat(headerRule.getHeader(HEADER_NAME)).matches(expectedHeaderPattern);
  }

  /**
   * The SPA is a static document whose single script tag carries no nonce. 'strict-dynamic' makes
   * browsers ignore 'self', so its presence in the default policy blocked the SPA's own bundle and
   * the page rendered blank. This pins the policy that actually lets the app load.
   */
  @Test
  void shouldServeADefaultPolicyThatPermitsTheSpaBundle() {
    // given
    headerRule.startServer("web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    String header = headerRule.getHeader(HEADER_NAME);
    assertThat(header)
        .as("the same-origin bundle must be allowed")
        .contains("script-src 'self'");
    assertThat(header)
        .as("'strict-dynamic' would disable the 'self' allowance and block the bundle")
        .doesNotContain("strict-dynamic");
    assertThat(header)
        .as("no nonce is issued when nothing substitutes one into the document")
        .doesNotContain("nonce-");
  }

  @Test
  void shouldConfigureDisabled() {
    // given
    headerRule.startServer("csp/disabled_web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.headerExists(HEADER_NAME)).isFalse();
  }

  @Test
  void shouldConfigureDisabledIgnoreCase() {
    // given
    headerRule.startServer("csp/disabled_ignore_case_web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.headerExists(HEADER_NAME)).isFalse();
  }

  @Test
  void shouldConfigureCustomValue() {
    // given
    headerRule.startServer("csp/custom_value_web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getHeader(HEADER_NAME))
      .isEqualTo("base-uri 'self'; default-src 'self' 'unsafe-inline'; img-src 'self' data:; block-all-mixed-content");
  }

}
