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

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SuppressWarnings("java:S5960")
public class HttpHeaderSecurityIT extends AbstractWebIntegrationTest {

  public static final String CSP_VALUE = "base-uri 'self';script-src 'nonce-([-_a-zA-Z\\d]*)' 'strict-dynamic' 'unsafe-eval' https: 'self' 'unsafe-inline';style-src 'unsafe-inline' 'self';default-src 'self';img-src 'self' data:;block-all-mixed-content;form-action 'self';frame-ancestors 'none';object-src 'none';sandbox allow-forms allow-scripts allow-same-origin allow-popups allow-downloads";

  @BeforeEach
  void createClient() {
    preventRaceConditions();
    createClient(getWebappCtxPath());
  }

  @Test
  @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
  void shouldCheckPresenceOfXssProtectionHeader() {
    // when
    HttpResponse<String> response = Unirest.get(appBasePath + TASKLIST_PATH).asString();

    // then
    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
    assertHeaderPresent("X-XSS-Protection", "1; mode=block", response);
  }

  @Test
  @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
  void shouldCheckPresenceOfContentSecurityPolicyHeader() {
    // when
    HttpResponse<String> response = Unirest.get(appBasePath + TASKLIST_PATH).asString();

    // then
    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
    assertHeaderPresent("Content-Security-Policy", CSP_VALUE, response);
  }

  @Test
  @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
  void shouldCheckPresenceOfContentTypeOptions() {
    // when
    HttpResponse<String> response = Unirest.get(appBasePath + TASKLIST_PATH).asString();

    // then
    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
    assertHeaderPresent("X-Content-Type-Options", "nosniff", response);
  }

  @Test
  @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
  void shouldCheckAbsenceOfHsts() {
    // when
    HttpResponse<String> response = Unirest.get(appBasePath + TASKLIST_PATH).asString();

    // then
    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
    List<String> values = response.getHeaders().get("Strict-Transport-Security");
    assertThat(values).isEmpty();
  }

  protected void assertHeaderPresent(String expectedName, String expectedValue, HttpResponse<String> response) {
    List<String> values = response.getHeaders().get(expectedName);

    if (values != null) {
      for (String value : values) {
        if (value.matches(expectedValue)) {
          return;
        }
      }
    }

    fail("Header '%s' didn't match.%nExpected:\t%s %nActual:\t%s".formatted(expectedName, expectedValue, values));
  }

}
