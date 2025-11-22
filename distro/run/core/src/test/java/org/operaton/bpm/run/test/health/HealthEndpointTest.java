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
package org.operaton.bpm.run.test.health;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.run.test.AbstractRestTest;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

public class HealthEndpointTest extends AbstractRestTest {

  @Test
  void shouldReturnHealthStatus() {
    ResponseEntity<JsonNode> entity = testRestTemplate.getForEntity("/health", JsonNode.class);
    assertThat(entity.getStatusCode().value()).isEqualTo(200);
    JsonNode body = entity.getBody();
    assertThat(body).isNotNull();
    assertThat(body.has("status")).isTrue();
    assertThat(body.get("status").isTextual()).isTrue();
    assertThat(body.has("timestamp")).isTrue();
    assertThat(body.get("timestamp").isTextual()).isTrue();
    assertThat(body.has("details")).isTrue();
  }
}
