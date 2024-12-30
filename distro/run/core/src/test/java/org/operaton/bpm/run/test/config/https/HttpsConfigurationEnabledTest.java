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
package org.operaton.bpm.run.test.config.https;

import org.operaton.bpm.run.OperatonBpmRun;
import org.operaton.bpm.run.test.AbstractRestTest;
import org.operaton.bpm.run.test.util.TestUtils;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {OperatonBpmRun.class}, webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles(profiles = {"test-https-enabled"})
class HttpsConfigurationEnabledTest extends AbstractRestTest {

  @BeforeEach
  void init() throws Exception {
    TestUtils.trustSelfSignedSSL();
  }

  @Test
  void shouldConnectWithHttps() {
    // given
    String url = "https://localhost:" + localPort + CONTEXT_PATH + "/task";

    // when
    var response = testRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null), List.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldNotRedirect() {
    // given
    String url = "http://localhost:" + 8080 + CONTEXT_PATH + "/task";
    // when
    HttpEntity<Object> requestEntity = new HttpEntity<>(null);
    Throwable exception = assertThrows(ResourceAccessException.class, () ->
        testRestTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class));
    assertThat(exception.getMessage()).contains("Connection refused");
  }
}
