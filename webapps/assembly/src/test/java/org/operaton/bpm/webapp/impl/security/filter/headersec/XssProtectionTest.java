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
package org.operaton.bpm.webapp.impl.security.filter.headersec;

import org.junit.Rule;
import org.junit.Test;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.webapp.impl.util.HeaderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.webapp.impl.security.filter.headersec.provider.impl.XssProtectionOption.BLOCK;
import static org.operaton.bpm.webapp.impl.security.filter.headersec.provider.impl.XssProtectionOption.SANITIZE;
import static org.operaton.bpm.webapp.impl.security.filter.headersec.provider.impl.XssProtectionProvider.HEADER_NAME;

/**
 * @author Tassilo Weidner
 */
public class XssProtectionTest {

  @Rule
  public HeaderRule headerRule = new HeaderRule();

  @Test
  public void shouldConfigureEnabledByDefault() {
    // given
    headerRule.startServer("web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getHeader(HEADER_NAME)).isEqualTo(BLOCK.getHeaderValue());
  }

  @Test
  public void shouldConfigureDisabled() {
    // given
    headerRule.startServer("xss/disabled_web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.headerExists(HEADER_NAME)).isFalse();
  }

  @Test
  public void shouldConfigureDisabledIgnoreCase() {
    // given
    headerRule.startServer("xss/disabled_ignore_case_web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.headerExists(HEADER_NAME)).isFalse();
  }

  @Test
  public void shouldConfigureCustomValue() {
    // given
    headerRule.startServer("xss/custom_value_web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getHeader(HEADER_NAME)).isEqualTo("aCustomValue");
  }

  @Test
  public void shouldConfigureOptionSanitize() {
    // given
    headerRule.startServer("xss/option_sanitize_web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getHeader(HEADER_NAME)).isEqualTo(SANITIZE.getHeaderValue());
  }

  @Test
  public void shouldConfigureOptionSanitizeIgnoreCase() {
    // given
    headerRule.startServer("xss/option_sanitize_ignore_case_web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getHeader(HEADER_NAME)).isEqualTo(SANITIZE.getHeaderValue());
  }

  @Test
  public void shouldConfigureOptionBlock() {
    // given
    headerRule.startServer("xss/option_block_web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getHeader(HEADER_NAME)).isEqualTo(BLOCK.getHeaderValue());
  }

  @Test
  public void shouldThrowExceptionOnSetBothParamsOptionAndValue() {
    // given
    headerRule.startServer("xss/option_and_value_web.xml", "headersec");

    // when
    headerRule.performRequest();

    Throwable expectedException = headerRule.getException();

    // then
    assertThat(expectedException).isInstanceOf(ProcessEngineException.class);
    assertThat(expectedException.getMessage()).isEqualTo("XssProtectionProvider: cannot set both xssProtectionValue and xssProtectionOption.");
  }

  @Test
  public void shouldThrowExceptionOnNonExistingOption() {
    // given
    headerRule.startServer("xss/option_non_existing_web.xml", "headersec");

    Throwable expectedException = headerRule.getException();

    // then
    assertThat(expectedException).isInstanceOf(ProcessEngineException.class);
    assertThat(expectedException.getMessage()).isEqualTo("XssProtectionProvider: cannot set non-existing option foo for xssProtectionOption.");
  }

}
