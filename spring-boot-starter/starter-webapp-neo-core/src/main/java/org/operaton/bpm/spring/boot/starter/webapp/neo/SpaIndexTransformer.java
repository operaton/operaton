/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

/**
 * Rewrites the {@code <base href>} of the SPA shell to the application root, so the
 * bundle can be served from a sub-path.
 *
 * <p>Nothing in the bundle can know its own prefix at build time: it is the sum of the
 * servlet context path (known per request) and
 * {@code operaton.bpm.webapp.neo.application-path}. The one place a browser reads a
 * prefix from is {@code <base href>}, so the server states it here and everything the
 * SPA emits — assets, locales, routes — hangs off it.</p>
 *
 * <p>Only {@code index.html} is transformed; every other resource passes through
 * untouched.</p>
 */
public class SpaIndexTransformer implements ResourceTransformer {

  protected static final String INDEX_HTML = "index.html";

  /** The {@code <base href="...">} the frontend build emits (see {@code index.html}). */
  protected static final Pattern BASE_TAG = Pattern.compile("<base\\s+href=\"[^\"]*\"\\s*/?>",
      Pattern.CASE_INSENSITIVE);

  protected final String applicationPath;

  public SpaIndexTransformer(String applicationPath) {
    this.applicationPath = applicationPath;
  }

  @Override
  public Resource transform(HttpServletRequest request,
                            Resource resource,
                            ResourceTransformerChain transformerChain) throws IOException {
    Resource transformed = transformerChain.transform(request, resource);
    if (!INDEX_HTML.equals(transformed.getFilename())) {
      return transformed;
    }

    String content = new String(FileCopyUtils.copyToByteArray(transformed.getInputStream()),
        StandardCharsets.UTF_8);
    Matcher matcher = BASE_TAG.matcher(content);
    if (!matcher.find()) {
      return transformed;
    }

    // Replace the existing tag rather than inserting a second one: with two base
    // tags the first wins, silently.
    String rewritten = content.substring(0, matcher.start())
        + "<base href=\"" + appRoot(request) + "\">"
        + content.substring(matcher.end());

    return new TransformedResource(transformed, rewritten.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * The application root as the browser must see it: context path + application path,
   * with a trailing slash so relative URLs resolve inside the app rather than beside it.
   */
  protected String appRoot(HttpServletRequest request) {
    return request.getContextPath() + applicationPath + "/";
  }

}
