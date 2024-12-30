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
package org.operaton.bpm.spring.boot.starter.webapp.filter.headersec.it.properties;

import org.operaton.bpm.spring.boot.starter.webapp.filter.util.FilterTestApp;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.HttpClientExtension;

import org.junit.Before;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { FilterTestApp.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
  "operaton.bpm.webapp.headerSecurity.xssProtectionDisabled=true",
  "operaton.bpm.webapp.headerSecurity.contentSecurityPolicyDisabled=true",
  "operaton.bpm.webapp.headerSecurity.contentTypeOptionsDisabled=true",
  "operaton.bpm.webapp.headerSecurity.hstsDisabled=true"
})
@DirtiesContext
public class HttpHeaderSecurityAbsenceIT {

  @RegisterExtension
  HttpClientExtension httpClientExtension;

  @LocalServerPort
  public int port;

  @Before
  public void assignRule() {
    httpClientExtension = new HttpClientExtension(port);
  }

  @Test
  public void shouldCheckAbsenceOfXssProtectionHeader() {
    // given

    // when
    httpClientExtension.performRequest();

    // then
    assertThat(httpClientExtension.headerExists("X-XSS-Protection")).isFalse();
  }

  @Test
  public void shouldCheckAbsenceOfContentSecurityPolicyHeader() {
    // given

    // when
    httpClientExtension.performRequest();

    // then
    assertThat(httpClientExtension.headerExists("Content-Security-Policy")).isFalse();
  }

  @Test
  public void shouldCheckAbsenceOfContentTypeOptions() {
    // given

    // when
    httpClientExtension.performRequest();

    // then
    assertThat(httpClientExtension.headerExists("X-Content-Type-Options")).isFalse();
  }

  @Test
  public void shouldCheckAbsenceOfHsts() {
    // given

    // when
    httpClientExtension.performRequest();

    // then
    assertThat(httpClientExtension.headerExists("Strict-Transport-Security")).isFalse();
  }

}
