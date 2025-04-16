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
package org.operaton.bpm.engine.rest.openapi.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.ProcessInstanceApi;
import org.openapitools.client.model.ProcessInstanceDto;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

public class BasicAuthenticationTest {

  private static final String ENGINE_REST_PROCESS_INSTANCE = "/engine-rest/process-instance";

  private static final String USERNAME = "mike";
  private static final String PASSWORD = "secret";

  ProcessInstanceApi api;

  @RegisterExtension
  static WireMockExtension wireMockExtension = WireMockExtension.newInstance().options(
          WireMockConfiguration.options().dynamicPort()).build();

  @BeforeEach
  public void clientWithValidCredentials() {
    ApiClient apiClient = new ApiClient();

    apiClient.setUsername(USERNAME);
    apiClient.setPassword(PASSWORD);
    apiClient.setBasePath(apiClient.getBasePath()
            .replace("8080", String.valueOf(wireMockExtension.getPort())));

    api = new ProcessInstanceApi(apiClient);
    WireMock.configureFor(wireMockExtension.getPort());
    System.out.printf("%s: Port before: %d%n", getClass(), wireMockExtension.getPort());
  }

  @org.junit.jupiter.api.Test
  public void shouldUseBasicAuth() throws ApiException {
    System.out.printf("%s: Port test: %d%n", getClass(), wireMockExtension.getPort());
    // given
    stubFor(get(urlEqualTo(ENGINE_REST_PROCESS_INSTANCE + "/1")).willReturn(aResponse().withStatus(
            200).withBody("{ \"id\": 1 }")));

    // when
    ProcessInstanceDto processInstance = api.getProcessInstance("1");

    // then
    assertThat(processInstance.getId()).isEqualTo("1");
    verify(getRequestedFor(urlEqualTo(ENGINE_REST_PROCESS_INSTANCE + "/1")).withHeader("Authorization",
            equalTo("Basic bWlrZTpzZWNyZXQ=")
    ));
  }
}
