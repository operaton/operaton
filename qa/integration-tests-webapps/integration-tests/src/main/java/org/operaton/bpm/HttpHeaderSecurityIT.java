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

import org.glassfish.jersey.client.ClientResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.core.MultivaluedMap;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HttpHeaderSecurityIT extends AbstractWebIntegrationTest {

  public static final String CSP_VALUE = "base-uri 'self';script-src 'nonce-([-_a-zA-Z\\d]*)' 'strict-dynamic' 'unsafe-eval' https: 'self' 'unsafe-inline';style-src 'unsafe-inline' 'self';default-src 'self';img-src 'self' data:;block-all-mixed-content;form-action 'self';frame-ancestors 'none';object-src 'none';sandbox allow-forms allow-scripts allow-same-origin allow-popups allow-downloads";

  @Before
  public void createClient() throws Exception {
    preventRaceConditions();
    createClient(getWebappCtxPath());
  }

  @Test(timeout=10000)
  public void shouldCheckPresenceOfXssProtectionHeader() {
    // given

    // when
    var response = getTasklistResponse();

    // then
    assertEquals(200, response.statusCode());
    assertHeaderPresent("X-XSS-Protection", "1; mode=block", response);
  }

  @Test(timeout=10000)
  public void shouldCheckPresenceOfContentSecurityPolicyHeader() throws Exception {
    // given

    // when
    HttpResponse<String> response = getTasklistResponse();

    // then
    assertEquals(200, response.statusCode());
    assertHeaderPresent("Content-Security-Policy", CSP_VALUE, response);
  }

  @Test(timeout=10000)
  public void shouldCheckPresenceOfContentTypeOptions() throws Exception {
    // given

    // when
    HttpResponse<String> response = getTasklistResponse();

    // then
    assertEquals(200, response.statusCode());
    assertHeaderPresent("X-Content-Type-Options", "nosniff", response);

    // cleanup
  }

  @Test(timeout=10000)
  public void shouldCheckAbsenceOfHsts() {
    // given

    // when
    HttpResponse<String> response = getTasklistResponse();

    // then
    assertEquals(200, response.statusCode());
    List<String> values = response.headers().allValues("Strict-Transport-Security");
    assertNull(values);
  }

  protected <T> void assertHeaderPresent(String expectedName, String expectedValue, HttpResponse<T> response) {
    var headers = response.headers();

    List<String> values = headers.allValues(expectedName);
    for (String value : values) {
      if (value.matches(expectedValue)) {
        return;
      }
    }

    Assert.fail(String.format("Header '%s' didn't match.\nExpected:\t%s \nActual:\t%s", expectedName, expectedValue, values));
  }

}
