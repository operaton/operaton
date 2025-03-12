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
import org.operaton.bpm.webapp.impl.util.HeaderExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentTypeOptionsTest {

  public static final String HEADER_NAME = "X-Content-Type-Options";
  public static final String HEADER_DEFAULT_VALUE = "nosniff";

  @RegisterExtension
  HeaderExtension headerExtension = new HeaderExtension();

  @Test
  void shouldConfigureEnabledByDefault() {
    // given
    headerExtension.startServer("web.xml", "headersec");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getHeader(HEADER_NAME)).isEqualTo(HEADER_DEFAULT_VALUE);
  }

  @Test
  void shouldConfigureDisabled() {
    // given
    headerExtension.startServer("cto/disabled_web.xml", "headersec");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.headerExists(HEADER_NAME)).isFalse();
  }

  @Test
  void shouldConfigureDisabledIgnoreCase() {
    // given
    headerExtension.startServer("cto/disabled_ignore_case_web.xml", "headersec");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.headerExists(HEADER_NAME)).isFalse();
  }

  @Test
  void shouldConfigureCustomValue() {
    // given
    headerExtension.startServer("cto/custom_value_web.xml", "headersec");

    // when
    headerExtension.performRequest();

    // then
    assertThat(headerExtension.getHeader(HEADER_NAME)).isEqualTo("aCustomValue");
  }

}
