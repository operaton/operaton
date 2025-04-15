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

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

public class CsrfPreventionIT extends AbstractWebIntegrationTest {

  @Before
  public void createClient() throws Exception {
    preventRaceConditions();
    createClient(getWebappCtxPath());
  }

  @Test(timeout = 10000)
  public void shouldCheckPresenceOfCsrfPreventionCookie() throws Exception {
    // given
    target = client.target(appBasePath + TASKLIST_PATH);

    // when
    response = target.request().get(Response.class);

    // then
    assertEquals(200, response.statusCode());
    String xsrfTokenHeader = getXsrfTokenHeader(response);
    String xsrfCookieValue = getXsrfCookieValue(response);
    assertNotNull(xsrfTokenHeader);
    assertEquals(32, xsrfTokenHeader.length());
    assertNotNull(xsrfCookieValue);
    assertTrue(xsrfCookieValue.contains(";SameSite=Lax"));
  }

  @Test(timeout = 10000)
  public void shouldRejectModifyingRequest() throws Exception {
    // given
    String baseUrl = testProperties.getApplicationPath("/" + getWebappCtxPath());
    String modifyingRequestPath = "api/admin/auth/user/default/login/welcome";
    target = client.target(baseUrl + modifyingRequestPath);

    // when
    response = target.request()
            .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED)
            .post(null, Response.class);

    // then
    assertEquals(403, response.statusCode());
    assertTrue(getXsrfTokenHeader(response).equals("Required"));
  }

}
