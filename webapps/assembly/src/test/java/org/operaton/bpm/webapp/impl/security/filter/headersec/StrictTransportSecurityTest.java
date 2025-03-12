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
package org.operaton.bpm.webapp.impl.security.filter.headersec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.webapp.impl.util.HeaderExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.webapp.impl.security.filter.headersec.provider.impl.StrictTransportSecurityProvider.HEADER_NAME;

class StrictTransportSecurityTest {

  @RegisterExtension
  HeaderExtension headerExtension = new HeaderExtension();

  @Test
  void shouldConfigureDisabledByDefault() {
    // given
    headerExtension.startServer("web.xml", "headersec");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getHeader(HEADER_NAME)).isNull();
  }

  @Test
  void shouldConfigureEnabled() {
    // given
    headerExtension.startServer("hsts/enabled_web.xml", "headersec");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.headerExists(HEADER_NAME)).isTrue();
  }

  @Test
  void shouldConfigureEnabledIgnoreCase() {
    // given
    headerExtension.startServer("hsts/enabled_ignore_case_web.xml", "headersec");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.headerExists(HEADER_NAME)).isTrue();
  }

  @Test
  void shouldConfigureCustomValue() {
    // given
    headerExtension.startServer("hsts/custom_value_web.xml", "headersec");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getHeader(HEADER_NAME)).isEqualTo("aCustomValue");
  }

  @Test
  void shouldThrowExceptionWhenConfiguringCustomValueAndMaxAge() {
    // given
    headerExtension.startServer("hsts/max_age_and_value_web.xml", "headersec");

    // when
    headerExtension.performRequest();

    Throwable expectedException = headerExtension.getException();

    // then
    assertThat(expectedException)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("StrictTransportSecurityProvider: cannot set hstsValue " +
        "in conjunction with hstsMaxAge or hstsIncludeSubdomainsDisabled.");
  }

  @Test
  void shouldConfigureIncludeSubdomains() {
    // given
    headerExtension.startServer("hsts/include_subdomains_web.xml", "headersec");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getHeader(HEADER_NAME)).isEqualTo("max-age=31536000; includeSubDomains");
  }

  @Test
  void shouldConfigureIncludeSubdomainsAndMaxAge() {
    // given
    headerExtension.startServer("hsts/include_subdomains_max_age_web.xml", "headersec");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getHeader(HEADER_NAME)).isEqualTo("max-age=47; includeSubDomains");
  }

  @Test
  void shouldConfigureMaxAge() {
    // given
    headerExtension.startServer("hsts/max_age_web.xml", "headersec");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getHeader(HEADER_NAME)).isEqualTo("max-age=47");
  }

}
