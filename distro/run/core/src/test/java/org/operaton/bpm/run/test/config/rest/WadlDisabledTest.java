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
package org.operaton.bpm.run.test.config.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.operaton.bpm.run.property.OperatonBpmRunRestProperties;
import org.operaton.bpm.run.test.AbstractRestTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {OperatonBpmRunRestProperties.PREFIX + ".disable-wadl=true"})
@Disabled("application.wadl is not available")
class WadlDisabledTest extends AbstractRestTest {

  @Test
  void shouldReturn404() {
    // given

    // when
    ResponseEntity<JsonNode> response = testRestTemplate.getForEntity(CONTEXT_PATH + "/application.wadl", JsonNode.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("message").textValue()).isEqualTo("HTTP 404 Not Found");
    assertThat(response.getBody().get("type").textValue()).isEqualTo("NotFoundException");
    assertThat(response.getBody().get("code").getNodeType()).isEqualTo(JsonNodeType.NULL);
  }
}
