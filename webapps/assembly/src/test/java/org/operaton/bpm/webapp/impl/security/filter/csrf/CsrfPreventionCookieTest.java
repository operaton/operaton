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
package org.operaton.bpm.webapp.impl.security.filter.csrf;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.webapp.impl.util.HeaderRule;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("temporarily disabled due to issues with the test setup")
class CsrfPreventionCookieTest {

  @RegisterExtension
  HeaderRule headerRule = new HeaderRule();

  @Test
  void shouldConfigureDefault() {
    // given
    headerRule.startServer("web.xml", "csrf");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getCookieHeader()).matches("XSRF-TOKEN=[A-Z0-9]{32};Path=/operaton;SameSite=Lax");
  }

  @Test
  void shouldConfigureRootContextPath() {
    // given
    headerRule.startServer("web.xml", "csrf", "/");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getCookieHeader()).matches("XSRF-TOKEN=[A-Z0-9]{32};Path=/;SameSite=Lax");
  }

  @Test
  void shouldConfigureSecureEnabled() {
    // given
    headerRule.startServer("secure_enabled_web.xml", "csrf");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getCookieHeader()).matches("XSRF-TOKEN=[A-Z0-9]{32};Path=/operaton;SameSite=Lax;Secure");
  }

  @Test
  void shouldConfigureSameSiteDisabled() {
    // given
    headerRule.startServer("same_site_disabled_web.xml", "csrf");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getCookieHeader()).matches("XSRF-TOKEN=[A-Z0-9]{32};Path=/operaton");
  }

  @Test
  void shouldConfigureSameSiteOptionStrict() {
    // given
    headerRule.startServer("same_site_option_strict_web.xml", "csrf");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getCookieHeader()).matches("XSRF-TOKEN=[A-Z0-9]{32};Path=/operaton;SameSite=Strict");
  }

  @Test
  void shouldConfigureSameSiteOptionLax() {
    // given
    headerRule.startServer("same_site_option_lax_web.xml", "csrf");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getCookieHeader()).matches("XSRF-TOKEN=[A-Z0-9]{32};Path=/operaton;SameSite=Lax");
  }

  @Test
  void shouldConfigureSameSiteCustomValue() {
    // given
    headerRule.startServer("same_site_custom_value_web.xml", "csrf");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getCookieHeader()).matches("XSRF-TOKEN=[A-Z0-9]{32};Path=/operaton;SameSite=aCustomValue");
  }

  @Test
  void shouldThrowExceptionWhenConfiguringBothSameSiteOptionAndValue() {
    // given
    headerRule.startServer("same_site_option_value_web.xml", "csrf");

    // when
    headerRule.performRequest();

    Throwable expectedException = headerRule.getException();

    // then
    assertThat(expectedException)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Please either configure sameSiteCookieOption or sameSiteCookieValue.");
  }

  @Test
  void shouldThrowExceptionWhenConfiguringUnknownSameSiteOption() {
    // given
    headerRule.startServer("same_site_option_unknown_web.xml", "csrf");

    // when
    headerRule.performRequest();

    Throwable expectedException = headerRule.getException();

    // then
    assertThat(expectedException)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("For sameSiteCookieOption param, please configure one of the following options: [LAX, STRICT]");
  }

  @Test
  void shouldIgnoreCaseOfParamValues() {
    // given
    headerRule.startServer("ignore_case_web.xml", "csrf");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getCookieHeader()).matches("XSRF-TOKEN=[A-Z0-9]{32};Path=/operaton;SameSite=Lax;Secure");
  }

  @Test
  void shouldConfigureWhenCookieIsSent() {
    // given
    headerRule.startServer("web.xml", "csrf");

    // when
    headerRule.performRequestWithHeader("Cookie", "XSRF-TOKEN=aToken");

    // then
    assertThat(headerRule.getCookieHeader()).matches("XSRF-TOKEN=[A-Z0-9]{32};Path=/operaton;SameSite=Lax");
  }

  @Test
  void shouldConfigureCookieName() {
    // given
    headerRule.startServer("changed_cookie_name_web.xml", "csrf");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getCookieHeader())
      .matches("myCookieName=[A-Z0-9]{32};Path=/operaton;SameSite=Lax");
  }

  @Test
  void shouldRejectModifyingRequest() {
    // given
    headerRule.startServer("web.xml", "csrf");

    // when
    headerRule.performPostRequest("/api/admin/auth/user/default/login/welcome");

    // then
    assertThat(headerRule.getResponseCode())
      .isEqualTo(HttpServletResponse.SC_FORBIDDEN);
  }

}
