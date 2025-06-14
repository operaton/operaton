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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;

import jakarta.ws.rs.core.Response;
import org.operaton.bpm.engine.rest.mapper.JacksonConfigurator;
import org.junit.Before;
import org.junit.Test;

public class DateSerializationIT extends AbstractWebIntegrationTest {

  private static final String SCHEMA_LOG_PATH = "api/engine/engine/default/schema/log";

  @Before
  public void createClient() throws Exception {
    preventRaceConditions();
    createClient(getWebappCtxPath());
    getTokens();
  }

  @Test
  public void shouldSerializeDateWithDefinedFormat() throws Exception {
    // given
    target = client.target(appBasePath + SCHEMA_LOG_PATH);

    // when
    response = target.request()
            .accept(MediaType.APPLICATION_JSON)
            .header("X-XSRF-TOKEN", csrfToken)  // Replace with your actual CSRF token header name
            .header("Cookie", createCookieHeader())  // Replace with actual cookie header method
            .get(Response.class);

    // then
    assertEquals(200, response.getStatus());

    // Read the response body and parse it into JsonNode
    String responseBody = response.readEntity(String.class);

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode logElement = objectMapper.readTree(responseBody).get(0); // Get the first element in the array

    // Extract the "timestamp" field from the JsonNode
    String timestamp = logElement.get("timestamp").asText();

    // Try parsing the timestamp using the predefined format
    try {
      new SimpleDateFormat(JacksonConfigurator.DEFAULT_DATE_FORMAT).parse(timestamp);
    } catch (ParseException pex) {
      fail("Couldn't parse timestamp from schema log: " + timestamp);
    }
  }

}
