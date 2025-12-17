/*
 * Copyright 2025 the Operaton contributors.
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
package org.operaton.bpm.webapp.impl.filter;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateFilterTest {
  static class TestTemplateFilter extends AbstractTemplateFilter {

    @Override
    protected void applyFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
      // No implementation needed for this test
    }
  }

  @Mock
  FilterConfig filterConfig;

  @Mock
  ServletContext servletContext;

  @InjectMocks
  TestTemplateFilter filter;

  @Test
  void getWebResourceContents_shouldReturnContentsOfWebResource() throws Exception {
    // given
    when(filterConfig.getServletContext()).thenReturn(servletContext);
    when(servletContext.getResourceAsStream("web.xml"))
      .thenReturn(getClass().getResourceAsStream("/WEB-INF/session/web.xml"));

    // when
    String contents = filter.getWebResourceContents("web.xml");

    // then
    assertThat(contents)
      .isNotEmpty()
      .isEqualToNormalizingNewlines("""
        <?xml version="1.0" encoding="UTF-8"?>
        <web-app version="6.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xmlns="https://jakarta.ee/xml/ns/jakartaee" xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd">

          <display-name>Operaton webapp</display-name>

          <filter>
            <filter-name>SessionCookieFilter</filter-name>
            <filter-class>org.operaton.bpm.webapp.impl.security.filter.SessionCookieFilter</filter-class>
          </filter>
          <filter-mapping>
            <filter-name>SessionCookieFilter</filter-name>
            <url-pattern>/*</url-pattern>
          </filter-mapping>

        </web-app>
        """);
  }

  @Test
  void getWebResourceContents_shouldThrowExceptionWhenResourceNotFound() {
    // given
    when(filterConfig.getServletContext()).thenReturn(servletContext);

    // when / then
    assertThatThrownBy(() -> filter.getWebResourceContents("nonexistent.xml"))
      .isInstanceOf(IOException.class)
      .hasMessageContaining("Resource not found");
  }
}
