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

import java.util.concurrent.TimeUnit;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.assertj.core.api.Assertions.assertThat;

public class CsrfPreventionIT extends AbstractWebIntegrationTest {

  @BeforeEach
  public void createClient() throws Exception {
    preventRaceConditions();
    createClient(getWebappCtxPath());
  }

  @Test @Timeout(value=10000, unit = TimeUnit.MILLISECONDS)
  public void shouldCheckPresenceOfCsrfPreventionCookie() {
      // given
      target = client.target(appBasePath + TASKLIST_PATH);

      // when
      response = target.request().get(Response.class);

      // then
      assertThat(response.getStatus()).isEqualTo(200);
      String xsrfTokenHeader = getXsrfTokenHeader(response);
      String xsrfCookieValue = getXsrfCookieValue(response);
      response.close();

      assertThat(xsrfTokenHeader).isNotNull();
      assertThat(xsrfTokenHeader.length()).isEqualTo(32);
      assertThat(xsrfCookieValue).isNotNull();
      assertThat(xsrfCookieValue.contains(";SameSite=Lax")).isTrue();
  }

  @Test @Timeout(value=10000, unit = TimeUnit.MILLISECONDS)
  public void shouldRejectModifyingRequest() {
      // given
      String baseUrl = testProperties.getApplicationPath("/" + getWebappCtxPath());
      String modifyingRequestPath = "api/admin/auth/user/default/login/welcome";
      target = client.target(baseUrl + modifyingRequestPath);

      // when
      response = target.request()
              .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED)
              .post(null, Response.class);

      // then
      assertThat(response.getStatus()).isEqualTo(403);
      assertThat("Required".equals(getXsrfTokenHeader(response))).isTrue();
  }

}
