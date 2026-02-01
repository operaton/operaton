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
package org.operaton.bpm.webapp.plugin.resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.List;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.webapp.AppRuntimeDelegate;
import org.operaton.bpm.webapp.plugin.AppPluginRegistry;
import org.operaton.bpm.webapp.plugin.spi.AppPlugin;

import static org.operaton.bpm.webapp.plugin.resource.AbstractAppPluginRootResource.MIME_TYPE_TEXT_CSS;
import static org.operaton.bpm.webapp.plugin.resource.AbstractAppPluginRootResource.MIME_TYPE_TEXT_JAVASCRIPT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Parameterized
public class AbstractAppPluginRootResourceTest {

  public static final String PLUGIN_NAME = "test-plugin";
  public static final String ASSET_DIR = "plugin/asset-dir";
  public static final String ASSET_CONTENT = "content";

  @Parameter(0)
  String assetName;
  @Parameter(1)
  String assetMediaType;
  @Parameter(2)
  boolean assetAllowed;

  private AppRuntimeDelegate<AppPlugin> runtimeDelegate;
  private AppPluginRegistry<AppPlugin>  pluginRegistry;
  private AbstractAppPluginRootResource<AppPlugin> pluginRootResource;
  private ServletContext mockServletContext;

  public void initAbstractAppPluginRootResourceTest(String assetName, String assetMediaType, boolean assetAllowed) {
    this.assetName = assetName;
    this.assetMediaType = assetMediaType;
    this.assetAllowed = assetAllowed;
  }

  @Parameters
  public static Collection<Object[]> getAssets() {
    return List.of(new Object[][]{
        {"app/plugin.js", MIME_TYPE_TEXT_JAVASCRIPT, true},
        {"app/plugin.css", MIME_TYPE_TEXT_CSS, true},
        {"app/asset.js", MIME_TYPE_TEXT_JAVASCRIPT, true},
        {null, null, false},
        {"", null, false},
        {"app/plugin.cs", null, false},
        {"../..", null, false},
        {"../../annotations-api.jar", null, false},
    });
  }

  @BeforeEach
  void setup() {
    runtimeDelegate = Mockito.mock(AppRuntimeDelegate.class);
    pluginRegistry = Mockito.mock(AppPluginRegistry.class);
    AppPlugin plugin = Mockito.mock(AppPlugin.class);

    Mockito.doReturn(pluginRegistry).when(runtimeDelegate).getAppPluginRegistry();
    Mockito.doReturn(plugin).when(pluginRegistry).getPlugin(PLUGIN_NAME);
    Mockito.doReturn(ASSET_DIR).when(plugin).getAssetDirectory();

    pluginRootResource = new AbstractAppPluginRootResource<>(PLUGIN_NAME, runtimeDelegate);
    mockServletContext = Mockito.mock(ServletContext.class);
    pluginRootResource.servletContext = mockServletContext;
    pluginRootResource.allowedAssets.add("app/asset.js");
    pluginRootResource.allowedAssets.add("app/asset.css");
  }

  @MethodSource("getAssets")
  @ParameterizedTest
  void shouldGetAssetIfAllowed(String assetName, String assetMediaType, boolean assetAllowed) throws Exception {
    initAbstractAppPluginRootResourceTest(assetName, assetMediaType, assetAllowed);
    // given
    var resourceName = "/" + ASSET_DIR + "/" + assetName;
    var inputStream = new ByteArrayInputStream(ASSET_CONTENT.getBytes());
    Mockito.doReturn(inputStream).when(mockServletContext).getResourceAsStream(resourceName);

    // when/then
    if (assetAllowed) {
      assertThatCode(() -> {
        final Response actual = pluginRootResource.getAsset(assetName);
        
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ((StreamingOutput) actual.getEntity()).write(output);

        assertThat(output).hasToString(ASSET_CONTENT);
        assertThat(actual.getHeaders()).containsKey(HttpHeaders.CONTENT_TYPE).hasSize(1);
        assertThat(actual.getHeaders().get(HttpHeaders.CONTENT_TYPE)).hasSize(1);
        // In IDE it's String, with maven it's MediaType class
        assertThat(actual.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0)).hasToString(assetMediaType);

        Mockito.verify(runtimeDelegate).getAppPluginRegistry();
        Mockito.verify(pluginRegistry).getPlugin(PLUGIN_NAME);
        Mockito.verify(mockServletContext).getResourceAsStream(resourceName);
      }).doesNotThrowAnyException();
    } else {
      assertThatThrownBy(() -> pluginRootResource.getAsset(assetName))
        .isInstanceOf(RestException.class)
        .hasMessage("Not allowed to load the following file '%s'.".formatted(assetName));

      Mockito.verify(runtimeDelegate, Mockito.never()).getAppPluginRegistry();
      Mockito.verify(pluginRegistry, Mockito.never()).getPlugin(PLUGIN_NAME);
      Mockito.verify(mockServletContext, Mockito.never()).getResourceAsStream(assetName);
    }
  }

}
