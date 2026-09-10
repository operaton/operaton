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

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.servlet.resource.ResourceTransformerChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpaIndexTransformerTest {

  protected static final String SHELL = """
      <!doctype html>
      <html lang="en">
          <head>
              <base href="/" />
              <link rel="icon" href="favicon.png" />
              <title>Operaton</title>
          </head>
          <body id="app"></body>
      </html>
      """;

  /** A pass-through chain: this transformer is the only one under test. */
  protected ResourceTransformerChain chain() throws Exception {
    ResourceTransformerChain chain = mock(ResourceTransformerChain.class);
    when(chain.transform(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
    return chain;
  }

  protected Resource resource(String filename, String content) {
    return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
      @Override
      public String getFilename() {
        return filename;
      }

      /** TransformedResource carries the original's timestamp forward. */
      @Override
      public long lastModified() {
        return 0L;
      }
    };
  }

  protected String transform(String applicationPath, String contextPath, Resource resource)
      throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContextPath(contextPath);

    Resource transformed = new SpaIndexTransformer(applicationPath)
        .transform(request, resource, chain());

    return new String(FileCopyUtils.copyToByteArray(transformed.getInputStream()),
        StandardCharsets.UTF_8);
  }

  @Test
  void shouldDeclareTheServerRootWhenNoApplicationPathIsConfigured() throws Exception {
    // when
    String content = transform("", "", resource("index.html", SHELL));

    // then
    assertThat(content).contains("<base href=\"/\">");
  }

  @Test
  void shouldDeclareTheApplicationPath() throws Exception {
    // when
    String content = transform("/app-neo", "", resource("index.html", SHELL));

    // then
    assertThat(content).contains("<base href=\"/app-neo/\">");
  }

  @Test
  void shouldIncludeTheContextPath() throws Exception {
    // when
    String content = transform("/app-neo", "/operaton", resource("index.html", SHELL));

    // then
    assertThat(content).contains("<base href=\"/operaton/app-neo/\">");
  }

  @Test
  void shouldReplaceTheBaseTagRatherThanAddOne() throws Exception {
    // when
    String content = transform("/app-neo", "", resource("index.html", SHELL));

    // then
    // Two base tags and the first one wins, silently.
    assertThat(content).containsOnlyOnce("<base");
    // everything else survives untouched
    assertThat(content).contains("<title>Operaton</title>")
        .contains("<link rel=\"icon\" href=\"favicon.png\" />");
  }

  @Test
  void shouldLeaveOtherResourcesAlone() throws Exception {
    // given
    Resource asset = resource("index-abc123.js", "const base = \"<base href=\\\"/\\\">\";");

    // when
    MockHttpServletRequest request = new MockHttpServletRequest();
    Resource transformed = new SpaIndexTransformer("/app-neo")
        .transform(request, asset, chain());

    // then
    assertThat(transformed).isSameAs(asset);
  }

  @Test
  void shouldLeaveAShellWithoutABaseTagAlone() throws Exception {
    // given
    Resource shell = resource("index.html", "<html><body>Hello World!</body></html>");

    // when
    MockHttpServletRequest request = new MockHttpServletRequest();
    Resource transformed = new SpaIndexTransformer("/app-neo")
        .transform(request, shell, chain());

    // then
    assertThat(transformed).isSameAs(shell);
  }

}
