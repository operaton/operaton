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
package org.operaton.bpm.spring.boot.starter.property.headersec;

import org.operaton.bpm.spring.boot.starter.property.HeaderSecurityProperties;
import org.operaton.bpm.spring.boot.starter.property.ParsePropertiesHelper;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
  "operaton.bpm.webapp.headerSecurity.xssProtectionValue=aValue",
  "operaton.bpm.webapp.headerSecurity.contentSecurityPolicyValue=aValue",
  "operaton.bpm.webapp.headerSecurity.contentTypeOptionsValue=aValue",
  "operaton.bpm.webapp.headerSecurity.hstsValue=aValue"
})
class HttpHeaderSecurityValueTest extends ParsePropertiesHelper {

  @Test
  void shouldCheckXssProtectionValue() {
    HeaderSecurityProperties properties = webapp.getHeaderSecurity();

    assertThat(properties.getXssProtectionValue()).isEqualTo("aValue");
    assertThat(properties.getInitParams()).containsEntry("xssProtectionValue", "aValue");
  }

  @Test
  void shouldCheckContentSecurityPolicyValue() {
    HeaderSecurityProperties properties = webapp.getHeaderSecurity();

    assertThat(properties.getContentSecurityPolicyValue()).isEqualTo("aValue");
    assertThat(properties.getInitParams()).containsEntry("contentSecurityPolicyValue", "aValue");
  }

  @Test
  void shouldCheckContentTypeOptionsValue() {
    HeaderSecurityProperties properties = webapp.getHeaderSecurity();

    assertThat(properties.getContentTypeOptionsValue()).isEqualTo("aValue");
    assertThat(properties.getInitParams()).containsEntry("contentTypeOptionsValue", "aValue");
  }

  @Test
  void shouldCheckHstsValue() {
    // given

    // when
    HeaderSecurityProperties properties = webapp.getHeaderSecurity();

    // then
    assertThat(properties.getHstsValue()).isEqualTo("aValue");
    assertThat(properties.getInitParams()).containsEntry("hstsValue", "aValue");
  }

}
