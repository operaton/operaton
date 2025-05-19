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
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.ProcessInstanceApi;
import org.openapitools.client.model.CountResultDto;
import org.openapitools.client.model.ProcessInstanceQueryDto;
import org.openapitools.client.model.SuspensionStateDto;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessInstanceTest {

  private static final String ENGINE_REST_PROCESS_INSTANCE = "/engine-rest/process-instance";

  // Create new ApiClient for ProcessInstanceApi to avoid the default client.
  final ProcessInstanceApi api = new ProcessInstanceApi(new ApiClient());

  @RegisterExtension
  static WireMockExtension wireMock = WireMockExtension.newInstance()
          .options(WireMockConfiguration.options().dynamicPort())
          .build();

  @BeforeEach
  void setUp() {
    api.setCustomBaseUrl(api.getApiClient()
            .getBasePath()
            .replace("8080", String.valueOf(wireMock.getPort())));
    // Dynamically set the basePath for the API to match WireMock's port
    var basePath = URI.create(api.getApiClient().getBasePath());
    String newBasePath = String.format("%s://%s:%d%s",
            basePath.getScheme(), basePath.getHost(), wireMock.getPort(), basePath.getPath());
    api.getApiClient().setBasePath(newBasePath);

    WireMock.configureFor(wireMock.getPort());
  }

  @Test
  void shouldQueryProcessInstancesCount() throws ApiException {
    // given
    stubFor(post(urlEqualTo(
            ENGINE_REST_PROCESS_INSTANCE + "/count")).willReturn(aResponse().withStatus(200)
            .withBody("{ \"count\": 3 }")));

    // when
    ProcessInstanceQueryDto processInstanceQueryDto = new ProcessInstanceQueryDto();
    processInstanceQueryDto.setActive(true);
    CountResultDto count = api.queryProcessInstancesCount(processInstanceQueryDto);

    // then
    assertThat(count.getCount()).isEqualTo(3);
    verify(postRequestedFor(urlEqualTo(ENGINE_REST_PROCESS_INSTANCE + "/count")).withRequestBody(
                    equalToJson("{ \"active\": true }"))
            .withHeader("Content-Type", equalTo("application/json; charset=UTF-8")));
  }

  @Test
  void shouldUpdateSuspensionStateById() throws ApiException {
    // given
    String id = "anProcessInstanceId";
    stubFor(put(urlEqualTo(ENGINE_REST_PROCESS_INSTANCE + "/" + id + "/suspended")).willReturn(
            aResponse().withStatus(204)));

    // when
    SuspensionStateDto dto = new SuspensionStateDto();
    dto.setSuspended(true);
    api.updateSuspensionStateById(id, dto);

    // then no error
    verify(putRequestedFor(urlEqualTo(
            ENGINE_REST_PROCESS_INSTANCE + "/" + id + "/suspended")).withRequestBody(equalToJson(
            "{ \"suspended\": true }")));
  }
}
