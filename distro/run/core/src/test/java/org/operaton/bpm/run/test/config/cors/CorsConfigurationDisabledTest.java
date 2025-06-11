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
package org.operaton.bpm.run.test.config.cors;

import org.operaton.bpm.run.property.OperatonBpmRunCorsProperty;
import org.operaton.bpm.run.test.AbstractRestTest;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Note: To run this test via an IDE you must set the system property
 * {@code sun.net.http.allowRestrictedHeaders} to {@code true}.
 * (e.g. System.setProperty("sun.net.http.allowRestrictedHeaders", "true");)
 *
 * @see <a href="https://jira.camunda.com/browse/CAM-11290">CAM-11290</a>
 */
@ActiveProfiles(profiles = {"test-cors-enabled"})
@TestPropertySource(properties = {OperatonBpmRunCorsProperty.PREFIX + ".enabled=false"})
class CorsConfigurationDisabledTest extends AbstractRestTest {

  @Test
  void shouldPassSameOriginRequest() {
    // given
    // same origin
    String origin = "http://localhost:" + localPort;

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.ORIGIN, origin);

    // when
    var response = testRestTemplate.exchange(CONTEXT_PATH + "/task", HttpMethod.GET, new HttpEntity<>(headers), List.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
  }

  @Test
  @Disabled("""
    TestRestTemplate does not follow same origin policy. With CORS disabled a cross-origin request
    should not be allowed by the calling client (i.e. browser or TestRestTemplate). Testing this
    manually in a browser should work.
  """)
  void shouldFailCrossOriginRequest() {
    // given
    // cross-origin
    String origin = "http://other.origin";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Origin", origin);

    // when
    var response = testRestTemplate.exchange(CONTEXT_PATH + "/task", HttpMethod.GET, new HttpEntity<>(headers), List.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
  }
}
