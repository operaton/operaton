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

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
@Ignore("""
  Fails with com.sun.jersey.api.client.ClientHandlerException: A message body reader for Java class jakarta.ws.rs.core.Response, and Java type class jakarta.ws.rs.core.Response, and MIME media type text/javascript was not found
  Revisit after upgrading to a newer version of Jersey
  """)
public class PluginsRootResourceIT extends AbstractWebIntegrationTest {

  @Parameter(0)
  public String assetName;

  @Parameter(1)
  public boolean assetAllowed;

  @Before
  public void createClient() throws Exception {
    createClient(getWebappCtxPath());
  }

  @Parameters(name = "Test instance: {index}. Asset: {0}, Allowed: {1}")
  public static Collection<Object[]> getAssets() {
    return Arrays.asList(new Object[][]{
        {"app/plugin.js", true},
        {"app/plugin.css", true},
        {"app/asset.js", false},
        {"../..", false},
        {"../../annotations-api.jar", false},
    });
  }

  @Test
  public void shouldGetAssetIfAllowed() {
    // when
    Response response = getAsset("api/admin/plugin/adminPlugins/static/" + assetName);

    // then
    assertResponse(assetName, response);

    // cleanup
    response.close();
  }

  protected Response getAsset(String path) {
    WebTarget target = client.target(appBasePath + path);

    // Send GET request and return the Response
    return target.request().get(Response.class);
  }

  protected void assertResponse(String asset, Response response) {
    if (assetAllowed) {
      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    } else {
      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
      assertTrue(response.getMediaType().toString().startsWith(MediaType.APPLICATION_JSON));
      String responseEntity = response.getEntity().toString();
      assertTrue(responseEntity.contains("\"type\":\"RestException\""));
      assertTrue(responseEntity.contains("\"message\":\"Not allowed to load the following file '" + asset + "'.\""));
    }
  }

}
