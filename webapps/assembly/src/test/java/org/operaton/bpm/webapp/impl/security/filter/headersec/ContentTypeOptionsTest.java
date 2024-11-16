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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.operaton.bpm.webapp.impl.util.HeaderRule;

public class ContentTypeOptionsTest {

  public static final String HEADER_NAME = "X-Content-Type-Options";
  public static final String HEADER_DEFAULT_VALUE = "nosniff";

  @Rule
  public HeaderRule headerRule = new HeaderRule();

  @Test
  public void shouldConfigureEnabledByDefault() {
    // given
    headerRule.startServer("web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getHeader(HEADER_NAME)).isEqualTo(HEADER_DEFAULT_VALUE);
  }

  @Test
  public void shouldConfigureDisabled() {
    // given
    headerRule.startServer("cto/disabled_web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.headerExists(HEADER_NAME)).isFalse();
  }

  @Test
  public void shouldConfigureDisabledIgnoreCase() {
    // given
    headerRule.startServer("cto/disabled_ignore_case_web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.headerExists(HEADER_NAME)).isFalse();
  }

  @Test
  public void shouldConfigureCustomValue() {
    // given
    headerRule.startServer("cto/custom_value_web.xml", "headersec");

    // when
    headerRule.performRequest();

    // then
    assertThat(headerRule.getHeader(HEADER_NAME)).isEqualTo("aCustomValue");
  }

}
