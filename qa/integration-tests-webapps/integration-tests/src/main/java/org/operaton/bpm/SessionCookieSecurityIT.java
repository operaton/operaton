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

import java.util.List;
import java.util.concurrent.TimeUnit;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SessionCookieSecurityIT extends AbstractWebIntegrationTest {

  @BeforeEach
  public void createClient() throws Exception {
    preventRaceConditions();
    createClient(getWebappCtxPath());
  }

  @Test @Timeout(value=10000, unit=TimeUnit.MILLISECONDS)
  public void shouldCheckPresenceOfProperties() {
    // given

    // when
    target = client.target(appBasePath + TASKLIST_PATH);

    // Send GET request and return the Response
    response = target.request().get(Response.class);

    // then
    assertEquals(200, response.getStatus());
    assertTrue(isCookieHeaderValuePresent("HttpOnly", response));
    assertFalse(isCookieHeaderValuePresent("Secure", response));
  }

  protected boolean isCookieHeaderValuePresent(String expectedHeaderValue, Response response) {
    MultivaluedMap<String, Object> headers = response.getHeaders();

    List<Object> values = headers.get("Set-Cookie");
    for (Object value : values) {
      if (value.toString().startsWith("JSESSIONID=")) {
        return value.toString().contains(expectedHeaderValue);
      }
    }

    return false;
  }

}
