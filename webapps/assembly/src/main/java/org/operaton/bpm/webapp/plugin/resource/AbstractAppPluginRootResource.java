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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;

import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.webapp.AppRuntimeDelegate;
import org.operaton.bpm.webapp.plugin.spi.AppPlugin;

/**
 * A resource class that provides a plugins restful API.
 *
 * <p>
 * Subclasses of this class may provide subresources using annotated getters
 * in order to be multi-engine aware.
 * </p>
 *
 * <p>
 * Subresources must properly initialize the subresources via
 * {@link AbstractAppPluginRootResource#subResource(AbstractAppPluginResource) }.
 *
 * <pre>
 * @Path("myplugin")
 * public class MyPluginRootResource extends AbstractAppPluginRootResource {
 *
 *   @Path("{engine}/my-resource")
 *   public FooResource getFooResource(@PathParam("engine") String engine) {
 *     return subResource(new FooResource(engine), engine);
 *   }
 * }
 * </pre>
 * </p>
 *
 * @author nico.rehwaldt
 * @author Daniel Meyer
 */
public class AbstractAppPluginRootResource<T extends AppPlugin> {

  public static final String MIME_TYPE_TEXT_PLAIN = "text/plain";
  public static final String MIME_TYPE_TEXT_HTML = "text/html";
  public static final String MIME_TYPE_TEXT_CSS = "text/css";
  public static final String MIME_TYPE_TEXT_JAVASCRIPT = "text/javascript";

  @Context
  protected ServletContext servletContext;

  @Context
  protected HttpHeaders headers;

  @Context
  protected UriInfo uriInfo;

  private final String pluginName;

  protected AppRuntimeDelegate<T> runtimeDelegate;
  protected List<String> allowedAssets;

  public AbstractAppPluginRootResource(String pluginName, AppRuntimeDelegate<T> runtimeDelegate) {
    this.pluginName = pluginName;
    this.runtimeDelegate = runtimeDelegate;
    this.allowedAssets = getAllowedAssets();
  }

  /**
   * <p>Returns the list of allowed assets to be served by the {@link #getAsset(String)} method.</p>
   * <p>The default implementation includes: <code>["app/plugin.js","app/plugin.css"]</code></p>
   *
   * @return list of allowed assets
   */
  protected List<String> getAllowedAssets() {
    List<String> assets = new ArrayList<>();
    assets.add("app/plugin.js");
    assets.add("app/plugin.css");
    return assets;
  }

  /**
   *
   * @param <T>
   * @param subResource
   * @return
   */
  protected <S extends AbstractAppPluginResource<T>> S subResource(S subResource) {
    return subResource;
  }

  /**
   * <p>Provides a plugins asset files via <code>$PLUGIN_ROOT_PATH/static</code>.</p>
   * <p>Assets must be explicitly declared in the {@link #getAllowedAssets()} method.</p>
   *
   * @param file
   * @return
   */
  @GET
  @Path("/static/{file:.*}")
  public Response getAsset(@PathParam("file") String file) {

    if (!allowedAssets.contains(file)) {
      throw new RestException(Status.FORBIDDEN, "Not allowed to load the following file '" + file + "'.");
    }

    AppPlugin plugin = runtimeDelegate.getAppPluginRegistry().getPlugin(pluginName);

    if (plugin != null) {
      InputStream assetStream = getPluginAssetAsStream(plugin, file);
      final InputStream filteredStream = applyResourceOverrides(assetStream);

      if (assetStream != null) {
        String contentType = getContentType(file);
        return Response.ok((StreamingOutput) out -> {

          try {
            byte[] buff = new byte[16 * 1000];
            int read = 0;
            while ((read = filteredStream.read(buff)) > 0) {
              out.write(buff, 0, read);
            }
          }
          finally {
            IoUtil.closeSilently(filteredStream);
            IoUtil.closeSilently(out);
          }

        }, contentType).build();
      }
    }

    // no asset found
    throw new RestException(Status.NOT_FOUND, "It was not able to load the following file '" + file + "'.");
  }

  /**
   * @param assetStream
   */
  protected InputStream applyResourceOverrides(InputStream assetStream) {
    // use a copy of the list cause it could be modified during iteration
    List<PluginResourceOverride> resourceOverrides = new ArrayList<>(runtimeDelegate.getResourceOverrides());
    for (PluginResourceOverride pluginResourceOverride : resourceOverrides) {
      assetStream = pluginResourceOverride.filterResource(assetStream, new RequestInfo(headers, servletContext, uriInfo));
    }
    return assetStream;
  }

  protected String getContentType(String file) {
    if (file.endsWith(".js")) {
      return MIME_TYPE_TEXT_JAVASCRIPT;
    } else
    if (file.endsWith(".html")) {
      return MIME_TYPE_TEXT_HTML;
    } else
    if (file.endsWith(".css")) {
      return MIME_TYPE_TEXT_CSS;
    } else {
      return MIME_TYPE_TEXT_PLAIN;
    }
  }

  /**
   * Returns an input stream for a given resource
   *
   * @param resourceName
   * @return
   */
  protected InputStream getPluginAssetAsStream(AppPlugin plugin, String fileName) {

    String assetDirectory = plugin.getAssetDirectory();

    if (assetDirectory == null) {
      return null;
    }

    InputStream result = getWebResourceAsStream(assetDirectory, fileName);

    if (result == null) {
      result = getClasspathResourceAsStream(plugin, assetDirectory, fileName);
    }
    return result;
  }

  protected InputStream getWebResourceAsStream(String assetDirectory, String fileName) {
    String resourceName = "/%s/%s".formatted(assetDirectory, fileName);

    return servletContext.getResourceAsStream(resourceName);
  }

  protected InputStream getClasspathResourceAsStream(AppPlugin plugin, String assetDirectory, String fileName) {
    String resourceName = "%s/%s".formatted(assetDirectory, fileName);
    return plugin.getClass().getClassLoader().getResourceAsStream(resourceName);
  }

}
