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
  "operaton.bpm.webapp.headerSecurity.xssProtectionOption=anOption"
})
class XssProtectionOptionTest extends ParsePropertiesHelper {

  @Test
  void shouldCheckXssProtectionOptionTest() {
    HeaderSecurityProperties properties = webapp.getHeaderSecurity();

    assertThat(properties.getXssProtectionOption()).isEqualTo("anOption");
    assertThat(properties.getInitParams()).containsEntry("xssProtectionOption", "anOption");
  }

}
