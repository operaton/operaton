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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.webapp.impl.util.HeaderRule;

import static org.operaton.bpm.webapp.impl.security.filter.headersec.provider.impl.StrictTransportSecurityProvider.HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

class StrictTransportSecurityTest {

  @RegisterExtension
  HeaderRule headerRule = new HeaderRule();

  @Test
  void shouldConfigureDisabledByDefault() {
    // given
    headerRule.startServer("web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getHeader(HEADER_NAME)).isNull();
  }

  @Test
  void shouldConfigureEnabled() {
    // given
    headerRule.startServer("hsts/enabled_web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.headerExists(HEADER_NAME)).isTrue();
  }

  @ParameterizedTest(name = "Should configure: {0}")
  @CsvSource({
      "aCustomValue,hsts/custom_value_web.xml,aCustomValue",
      "max-age,hsts/max_age_web.xml,max-age=47",
      "includeSubDomains,hsts/include_subdomains_web.xml,max-age=31536000; includeSubDomains",
      "max-age & includeSubDomains ,hsts/include_subdomains_max_age_web.xml,max-age=47; includeSubDomains",
  })
  void shouldConfigureHeader(String name,  String webXmlFile, String expectedHeaderValue) {
    // given
    headerRule.startServer(webXmlFile, "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getHeader(HEADER_NAME)).isEqualTo(expectedHeaderValue);
  }

  @Test
  void shouldConfigureEnabledIgnoreCase() {
    // given
    headerRule.startServer("hsts/enabled_ignore_case_web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.headerExists(HEADER_NAME)).isTrue();
  }

  @Test
  void shouldThrowExceptionWhenConfiguringCustomValueAndMaxAge() {
    // given
    headerRule.startServer("hsts/max_age_and_value_web.xml", "headersec");

    // when
    headerRule.performRequest();

    Throwable expectedException = headerRule.getException();

    // then
    assertThat(expectedException)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("StrictTransportSecurityProvider: cannot set hstsValue " +
        "in conjunction with hstsMaxAge or hstsIncludeSubdomainsDisabled.");
  }

}
