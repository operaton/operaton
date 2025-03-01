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
package org.operaton.bpm;

import org.operaton.bpm.engine.rest.mapper.JacksonConfigurator;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import jakarta.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
    // when
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + SCHEMA_LOG_PATH))
      .header("Accept", MediaType.APPLICATION_JSON)
      .header(X_XSRF_TOKEN_HEADER, csrfToken)
      .header(COOKIE_HEADER, createCookieHeader())
      .GET()
      .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    // then
    assertEquals(200, response.statusCode());
    JSONObject logElement = new JSONArray(response.body()).getJSONObject(0);
    String timestamp = logElement.getString("timestamp");
    try {
      new SimpleDateFormat(JacksonConfigurator.DEFAULT_DATE_FORMAT).parse(timestamp);
    } catch (ParseException pex) {
      fail("Couldn't parse timestamp from schema log: " + timestamp);
    }
  }

}
