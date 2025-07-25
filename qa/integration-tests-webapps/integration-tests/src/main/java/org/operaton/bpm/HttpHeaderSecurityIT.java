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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public class HttpHeaderSecurityIT extends AbstractWebIntegrationTest {

  public static final String CSP_VALUE = "base-uri 'self';script-src 'nonce-([-_a-zA-Z\\d]*)' 'strict-dynamic' 'unsafe-eval' https: 'self' 'unsafe-inline';style-src 'unsafe-inline' 'self';default-src 'self';img-src 'self' data:;block-all-mixed-content;form-action 'self';frame-ancestors 'none';object-src 'none';sandbox allow-forms allow-scripts allow-same-origin allow-popups allow-downloads";

  @BeforeEach
  public void createClient() throws Exception {
    preventRaceConditions();
    createClient(getWebappCtxPath());
  }

  @Test @Timeout(value=10000, unit=TimeUnit.MILLISECONDS)
  public void shouldCheckPresenceOfXssProtectionHeader() {
    // given
    target = client.target(appBasePath + TASKLIST_PATH);

    // when
    response = target.request().get();

    // then
    assertEquals(200, response.getStatus());
    assertHeaderPresent("X-XSS-Protection", "1; mode=block", response);
  }

  @Test @Timeout(value=10000, unit=TimeUnit.MILLISECONDS)
  public void shouldCheckPresenceOfContentSecurityPolicyHeader() {
    // given
    target = client.target(appBasePath + TASKLIST_PATH);

    // when
    response = target.request().get();

    // then
    assertEquals(200, response.getStatus());
    assertHeaderPresent("Content-Security-Policy", CSP_VALUE, response);
  }

  @Test @Timeout(value=10000, unit=TimeUnit.MILLISECONDS)
  public void shouldCheckPresenceOfContentTypeOptions() {
    // given
    target = client.target(appBasePath + TASKLIST_PATH);

    // when
    response = target.request().get();

    // then
    assertEquals(200, response.getStatus());
    assertHeaderPresent("X-Content-Type-Options", "nosniff", response);
  }

  @Test @Timeout(value=10000, unit=TimeUnit.MILLISECONDS)
  public void shouldCheckAbsenceOfHsts() {
    // given
    target = client.target(appBasePath + TASKLIST_PATH);

    // when
    response = target.request().get();

    // then
    assertEquals(200, response.getStatus());
    MultivaluedMap<String, Object> headers = response.getHeaders();
    List<Object> values = headers.get("Strict-Transport-Security");
    assertNull(values);
  }

  protected void assertHeaderPresent(String expectedName, String expectedValue, Response response) {
    MultivaluedMap<String, Object> headers = response.getHeaders();

    List<Object> values = headers.get(expectedName);
    for (Object value : values) {
      if (value.toString().matches(expectedValue)) {
        return;
      }
    }

    Assertions.fail(String.format("Header '%s' didn't match.\nExpected:\t%s \nActual:\t%s", expectedName, expectedValue, values));
  }

}
