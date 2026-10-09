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
package org.operaton.bpm.spring.boot.starter.webapp.filter.headersec.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import org.operaton.bpm.spring.boot.starter.webapp.filter.util.FilterTestApp;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.HttpClientExtension;

import static org.operaton.bpm.webapp.neo.impl.security.filter.headersec.provider.impl.ContentSecurityPolicyProvider.HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security headers on the SPA shell when the application path is the server root.
 *
 * <p>The root is the shipped default, but every other integration test in this module
 * runs with {@code application-path: /app-neo}. That mattered: the filter chain is
 * mapped to {@code basePath + "/*"} only when there is a sub-path, so at the root the
 * shell HTML was served with no security headers at all and nothing noticed.</p>
 */
@SpringBootTest(classes = {FilterTestApp.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"operaton.bpm.webapp.neo.application-path="})
@DirtiesContext
class RootAppPathHeaderSecurityIT {

  @RegisterExtension
  HttpClientExtension httpClientExtension = new HttpClientExtension();

  @LocalServerPort
  public int port;

  @BeforeEach
  void assignPort() {
    httpClientExtension.setPort(port);
  }

  @Test
  void shouldSendSecurityHeadersWithTheSpaShellAtTheRoot() {
    // given the SPA served from "/" rather than a sub-path

    // when
    httpClientExtension.performRequest("http://localhost:" + port + "/");

    // then the document response carries the same headers the sub-path case gets
    assertThat(httpClientExtension.getHeader(HEADER_NAME))
        .as("Content-Security-Policy on the SPA shell")
        .isNotNull();
    assertThat(httpClientExtension.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(httpClientExtension.getHeader("X-XSS-Protection")).isEqualTo("1; mode=block");
  }

  @Test
  void shouldSendSecurityHeadersOnClientSideRoutes() {
    // given a deep link that the SPA resolves client-side, served as index.html

    // when
    httpClientExtension.performRequest("http://localhost:" + port + "/processes");

    // then it is still a document response and still needs the headers
    assertThat(httpClientExtension.getHeader(HEADER_NAME))
        .as("Content-Security-Policy on a client-side route")
        .isNotNull();
    assertThat(httpClientExtension.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
  }

}
