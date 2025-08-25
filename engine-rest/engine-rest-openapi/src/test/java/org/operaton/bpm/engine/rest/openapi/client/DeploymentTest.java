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
package org.operaton.bpm.engine.rest.openapi.client;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.DeploymentApi;
import org.openapitools.client.model.DeploymentWithDefinitionsDto;

import java.io.File;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class DeploymentTest {

  private static final String ENGINE_REST_DEPLOYMENT = "/engine-rest/deployment";

  //DeploymentApi with new ApiClient. The new ApiClient is required or all Api share the same default
  private final DeploymentApi api = new DeploymentApi(new ApiClient());

  @RegisterExtension
  static WireMockExtension wireMock = WireMockExtension.newInstance()
          .options(WireMockConfiguration.options().dynamicPort())
          .build();

  @BeforeEach
  void setUp() {
    api.setCustomBaseUrl(api.getApiClient()
            .getBasePath()
            .replace("8080", String.valueOf(wireMock.getPort())));
  }

  @Test
  void shouldCreateDeployment() throws Exception {
    // given
    String deploymentSource = "test-source";
    String deploymentName = "deployment-test-name";
    wireMock.stubFor(post(urlEqualTo(ENGINE_REST_DEPLOYMENT + "/create")).willReturn(
            aResponse().withStatus(200).withBody("""
                        {
                        "links": [
                            {
                                "method": "GET",
                                "href": "http://localhost:%d/rest-test/deployment/aDeploymentId",
                                "rel": "self"
                            }
                        ],
                        "id": "aDeploymentId",
                        "name": "%s",
                        "source": "%s",
                        "deploymentTime": "2013-01-23T13:59:43.000+0200",
                        "tenantId": null,
                        "deployedProcessDefinitions": {
                            "aProcDefId": {
                                "id": "aProcDefId",
                                "key": "aKey",
                                "category": "aCategory",
                                "description": "aDescription",
                                "name": "aName",
                                "version": 42,
                                "resource": "aResourceName",
                                "deploymentId": "aDeploymentId",
                                "diagram": "aResourceName.png",
                                "suspended": true,
                                "tenantId": null,
                                "versionTag": null
                            }
                        },
                        "deployedCaseDefinitions": null,
                        "deployedDecisionDefinitions": null,
                        "deployedDecisionRequirementsDefinitions": null
                    }""".formatted(
                    wireMock.getPort(),
                    deploymentName,
                    deploymentSource
            ))));

    DeploymentWithDefinitionsDto deployment;

    // when
    deployment = api.createDeployment(null,
            deploymentSource,
            false,
            false,
            deploymentName,
            null,
            new File("src/test/resources/one.bpmn")
    );

    // then
    assertThat(deployment.getId()).isEqualTo("aDeploymentId");
    assertThat(deployment.getName()).isEqualTo(deploymentName);
    assertThat(deployment.getSource()).isEqualTo(deploymentSource);
    assertThat(deployment.getDeployedProcessDefinitions()).containsKey("aProcDefId");
    assertThat(deployment.getDeployedProcessDefinitions().get("aProcDefId").getId()).isEqualTo(
            "aProcDefId");
    assertThat(deployment.getDeployedProcessDefinitions().get("aProcDefId").getKey()).isEqualTo(
            "aKey");
    assertThat(deployment.getDeployedProcessDefinitions().get("aProcDefId").getName()).isEqualTo(
            "aName");

    wireMock.verify(postRequestedFor(urlEqualTo(ENGINE_REST_DEPLOYMENT + "/create")).withRequestBody(
                    containing("Content-Disposition: form-data; name=\"deployment-name\""))
            .withRequestBody(containing("deployment-test-name"))
            .withRequestBody(containing("Content-Disposition: form-data; name=\"deployment-source\""))
            .withRequestBody(containing("test-source"))
            .withRequestBody(containing(
                    "Content-Disposition: form-data; name=\"data\"; filename=\"one.bpmn\""))
            .withRequestBody(containing("Content-Type: application/octet-stream"))
            .withHeader("Content-Type", containing("multipart/form-data")));

  }
}
