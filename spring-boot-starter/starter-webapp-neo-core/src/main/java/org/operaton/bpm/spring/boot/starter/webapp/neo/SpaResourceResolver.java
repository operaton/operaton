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
package org.operaton.bpm.spring.boot.starter.webapp.neo;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Resolves static resources for the webapps-neo SPA and falls back to
 * {@code index.html} for client-side routes (deep links) so the single-page
 * application can take over routing.
 *
 * <p>The fallback only applies to paths that look like application routes:
 * paths that resolve to an actual file are served directly, requests for the
 * API namespaces are left to 404, and requests that carry a file extension
 * (i.e. a missing static asset) are not masked by the HTML shell.</p>
 */
public class SpaResourceResolver extends PathResourceResolver {

  protected static final String INDEX_HTML = "index.html";

  /** Request prefixes that must never be answered with the SPA shell. */
  protected static final List<String> RESERVED_PREFIXES = List.of("api/", "engine-rest", "operaton");

  @Override
  protected Resource getResource(String resourcePath, Resource location) throws IOException {
    Resource resource = super.getResource(resourcePath, location);
    if (resource != null) {
      return resource;
    }
    if (isClientRoute(resourcePath)) {
      Resource index = location.createRelative(INDEX_HTML);
      return index.exists() && index.isReadable() ? index : null;
    }
    return null;
  }

  protected boolean isClientRoute(String resourcePath) {
    if (resourcePath == null || resourcePath.isEmpty()) {
      return true; // the application root
    }
    for (String prefix : RESERVED_PREFIXES) {
      if (resourcePath.equals(prefix) || resourcePath.startsWith(prefix)) {
        return false;
      }
    }
    // a last segment carrying a file extension is a (missing) static asset, not a route
    String lastSegment = resourcePath;
    int slash = lastSegment.lastIndexOf('/');
    if (slash >= 0) {
      lastSegment = lastSegment.substring(slash + 1);
    }
    return !lastSegment.contains(".");
  }
}
