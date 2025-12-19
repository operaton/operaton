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

import java.util.Collection;

import java.util.List;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
@SuppressWarnings("java:S5960")
public class PluginsRootResourceIT extends AbstractWebIntegrationTest {

  @Parameter(0)
  public String assetName;

  @Parameter(1)
  public boolean assetAllowed;

  @BeforeEach
  void createClient() {
    createClient(getWebappCtxPath());
  }

  @Parameters(name = "Asset: {0}, Allowed: {1}")
  public static Collection<Object[]> getAssets() {
    return List.of(
      new Object[]{ "app/plugin.js", true },
      new Object[]{ "app/plugin.css", true },
      new Object[]{ "app/asset.js", false },
      new Object[]{ "../..", false },
      new Object[]{ "../../annotations-api.jar", false }
    );
  }

  @Test
  void shouldGetAssetIfAllowed() {
    // when
    HttpResponse<String> response = Unirest.get(appBasePath + "api/admin/plugin/adminPlugins/static/" + assetName).asString();

    // then
    assertResponse(assetName, response);
  }

  protected void assertResponse(String asset, HttpResponse<String> response) {
    if (assetAllowed) {
      assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
    } else {
      assertThat(response.getStatus()).isEqualTo(FORBIDDEN.getStatusCode());
      assertThat(response.getHeaders().getFirst("Content-Type")).startsWith("application/json");
      String responseEntity = response.getBody();
      assertThat(responseEntity)
              .contains("\"type\":\"RestException\"")
              .contains("\"message\":\"Not allowed to load the following file '" + asset + "'.\"");
    }
  }

}
