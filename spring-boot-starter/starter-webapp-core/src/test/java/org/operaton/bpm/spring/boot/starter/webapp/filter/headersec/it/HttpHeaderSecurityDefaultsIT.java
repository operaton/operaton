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
package org.operaton.bpm.spring.boot.starter.webapp.filter.headersec.it;

import org.operaton.bpm.spring.boot.starter.webapp.filter.util.FilterTestApp;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.HttpClientExtension;
import static org.operaton.bpm.webapp.impl.security.filter.headersec.provider.impl.ContentSecurityPolicyProvider.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { FilterTestApp.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HttpHeaderSecurityDefaultsIT {

  @RegisterExtension
  HttpClientExtension httpClientExtension;

  @LocalServerPort
  public int port;

  @BeforeEach
  void assignRule() {
    httpClientExtension = new HttpClientExtension(port);
  }

  @Test
  void shouldCheckDefaultOfXssProtectionHeader() {
    // given

    // when
    httpClientExtension.performRequest();

    // then
    assertThat(httpClientExtension.getHeader("X-XSS-Protection")).isEqualTo("1; mode=block");
  }

  @Test
  void shouldCheckDefaultOfContentSecurityPolicyHeader() {
    // given

    // when
    httpClientExtension.performRequest();

    // then
    String expectedHeaderPattern = HEADER_DEFAULT_VALUE.replace(HEADER_NONCE_PLACEHOLDER, "'nonce-([-_a-zA-Z\\d]*)'");
    assertThat(httpClientExtension.getHeader(HEADER_NAME)).matches(expectedHeaderPattern);
  }

  @Test
  void shouldCheckDefaultOfContentTypeOptions() {
    // given

    // when
    httpClientExtension.performRequest();

    // then
    assertThat(httpClientExtension.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
  }

}
