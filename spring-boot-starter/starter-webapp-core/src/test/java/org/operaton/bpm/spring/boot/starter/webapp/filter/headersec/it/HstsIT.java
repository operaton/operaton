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

import org.junit.Before;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { FilterTestApp.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "operaton.bpm.webapp.headerSecurity.hstsDisabled=false",
    "operaton.bpm.webapp.headerSecurity.hstsMaxAge=8",
    "operaton.bpm.webapp.headerSecurity.hstsIncludeSubdomainsDisabled=false"
})
public class HstsIT {

  @RegisterExtension
  HttpClientExtension httpClientExtension;

  @LocalServerPort
  public int port;

  @Before
  public void assignRule() {
    httpClientExtension = new HttpClientExtension(port);
  }

  @Test
  public void shouldConfigureHsts() {
    // given

    // when
    httpClientExtension.performRequest();

    // then
    assertThat(httpClientExtension.getHeader("Strict-Transport-Security"))
        .isEqualTo("max-age=8; includeSubDomains");
  }

}
