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
package org.operaton.bpm;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("java:S5960")
class ErrorPageIT extends AbstractWebIntegrationTest {

  @BeforeEach
  void createClient() {
    createClient(getWebappCtxPath());
  }

  @Test
  void shouldCheckNonFoundResponse() {
    // when
    HttpResponse<String> response = Unirest.get(appBasePath + "nonexisting").asString();

    // then
    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(response.getHeaders().get("Content-Type").get(0)).startsWith("text/html");
    String responseEntity = response.getBody();
    assertThat(responseEntity)
            .contains("Operaton")
            .contains("Not Found");
  }

}
