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
package org.operaton.bpm.webapp.impl.security.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.webapp.impl.util.HeaderExtension;

import static org.assertj.core.api.Assertions.assertThat;

class SessionCookieTest {

  @RegisterExtension
  HeaderExtension headerExtension = new HeaderExtension();

  @Test
  void shouldConfigureDefault() {
    // given
    headerExtension.startServer("web.xml", "session");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getCookieHeader()).matches(headerExtension.getSessionCookieRegex("operaton", "Lax", false));
  }

  @Test
  void shouldConfigureRootContextPath() {
    // given
    headerExtension.startServer("web.xml", "session", "/");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getCookieHeader()).matches(headerExtension.getSessionCookieRegex(null, "Lax", false));
  }

  @Test
  void shouldConfigureSecureEnabled() {
    // given
    headerExtension.startServer("secure_enabled_web.xml", "session");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getCookieHeader()).matches(headerExtension.getSessionCookieRegex("operaton", "Lax", true));
  }

  @Test
  void shouldConfigureSameSiteDisabled() {
    // given
    headerExtension.startServer("same_site_disabled_web.xml", "session");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getCookieHeader()).matches(headerExtension.getSessionCookieRegex("operaton", null, false));
  }

  @Test
  void shouldConfigureSameSiteOptionStrict() {
    // given
    headerExtension.startServer("same_site_option_strict_web.xml", "session");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getCookieHeader()).matches(headerExtension.getSessionCookieRegex("operaton", "Strict", false));
  }

  @Test
  void shouldConfigureSameSiteOptionLax() {
    // given
    headerExtension.startServer("same_site_option_lax_web.xml", "session");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getCookieHeader()).matches(headerExtension.getSessionCookieRegex("operaton", "Lax", false));
  }

  @Test
  void shouldConfigureSameSiteCustomValue() {
    // given
    headerExtension.startServer("same_site_custom_value_web.xml", "session");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getCookieHeader()).matches(headerExtension.getSessionCookieRegex("operaton", "aCustomValue", false));
  }

  @Test
  void shouldThrowExceptionWhenConfiguringBothSameSiteOptionAndValue() {
    // given
    headerExtension.startServer("same_site_option_value_web.xml", "session");

    // when
    headerExtension.performRequest();

    Throwable expectedException = headerExtension.getException();

    // then
    assertThat(expectedException)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Please either configure sameSiteCookieOption or sameSiteCookieValue.");
  }

  @Test
  void shouldThrowExceptionWhenConfiguringUnknownSameSiteOption() {
    // given
    headerExtension.startServer("same_site_option_unknown_web.xml", "session");

    // when
    headerExtension.performRequest();

    Throwable expectedException = headerExtension.getException();

    // then
    assertThat(expectedException)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("For sameSiteCookieOption param, please configure one of the following options: [LAX, STRICT]");
  }

  @Test
  void shouldIgnoreCaseOfParamValues() {
    // given
    headerExtension.startServer("ignore_case_web.xml", "session");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getCookieHeader()).matches(headerExtension.getSessionCookieRegex("operaton", "Lax", true));
  }

  @Test
  void shouldConfigureCookieName() {
    // given
    headerExtension.startServer("changed_cookie_name_web.xml", "session");

    // when
    headerExtension.performRequest();
    String cookieHeader = headerExtension.getCookieHeader();

    // then
    assertThat(cookieHeader).matches(
            headerExtension.getSessionCookieRegex("operaton", "MYCOOKIENAME", "Lax", false));
  }

  @Test
  void shouldConfigureWhenCookieIsSent() {
    // given
    headerExtension.startServer("web.xml", "session");

    // when
    headerExtension.performRequestWithHeader("Cookie", "JSESSIONID=aToken");

    // then
    assertThat(headerExtension.getCookieHeader()).matches(headerExtension.getSessionCookieRegex("operaton", "Lax", false));
  }

}
