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
package org.operaton.bpm.spring.boot.starter.webapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestRestTemplate
@SpringBootTest(
  classes = WebappExampleApplication.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@SuppressWarnings("unused")
class WebappTest {

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  void testNeoAppAvailable() {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/app-neo/", String.class);

    // First request gets 302 from CsrfPreventionFilter (sets XSRF-TOKEN cookie)
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
    assertThat(response.getHeaders().getLocation()).isNotNull();
    assertThat(response.getHeaders().getLocation().getPath()).isEqualTo("/app-neo/");
  }

}
