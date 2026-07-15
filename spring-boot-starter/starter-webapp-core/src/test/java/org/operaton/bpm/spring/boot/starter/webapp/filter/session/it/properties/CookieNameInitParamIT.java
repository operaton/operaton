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
package org.operaton.bpm.spring.boot.starter.webapp.filter.session.it.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import org.operaton.bpm.spring.boot.starter.webapp.filter.util.FilterTestApp;
import org.operaton.bpm.spring.boot.starter.webapp.filter.util.HttpClientExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the behaviour of the {@code cookieName} init-param of the {@code SessionCookieFilter}
 * (see {@code operaton.bpm.webapp.session-cookie.cookieName}), mirroring the
 * {@code cookieName} init-param configured in {@code changed_cookie_name_web.xml}.
 * <p>
 * The filter applies its {@code SameSite}/{@code Secure} attributes only to the {@code Set-Cookie}
 * header whose name matches the configured cookie name. {@code CookieConfigurator} resolves that
 * name from the {@code cookieName} init-param first, and only falls back to the servlet session
 * cookie config ({@code server.servlet.session.cookie.name}) when the init-param is absent.
 * <p>
 * Here the init-param ({@code fromInitParam}) intentionally differs from the actually emitted
 * session cookie name ({@code fromServletConfig}). Because the init-param takes precedence, the
 * filter looks for {@code fromInitParam=}, does not find it, and therefore leaves the emitted
 * {@code fromServletConfig} cookie untouched (no {@code SameSite}). If the init-param handling were
 * ever dropped, the servlet-config fallback would kick in and wrongly apply {@code SameSite} to
 * {@code fromServletConfig}, failing this test.
 */
@SpringBootTest(classes = {FilterTestApp.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "operaton.bpm.webapp.session-cookie.cookieName=fromInitParam",
        "server.servlet.session.cookie.name=fromServletConfig"
})
@DirtiesContext
class CookieNameInitParamIT {

    @RegisterExtension
    HttpClientExtension httpClientExtension = new HttpClientExtension();

    @LocalServerPort
    public int port;

    @BeforeEach
    void assignPort() {
        httpClientExtension.setPort(port);
    }

    @Test
    void shouldRespectCookieNameInitParamOverServletConfigFallback() {
        // given

        // when
        httpClientExtension.performRequest("http://localhost:" + port + "/operaton/app/tasklist/default");

        String servletConfigCookie = httpClientExtension.getCookie("fromServletConfig");

        // then
        // the container emits the session cookie under the servlet-config name ...
        assertThat(servletConfigCookie).startsWith("fromServletConfig=")
            // ... but the filter targeted the init-param name, so no SameSite is applied to it
            .doesNotContain("SameSite");
        // and no cookie is emitted under the init-param name
        assertThat(httpClientExtension.getCookie("fromInitParam")).isEmpty();
    }

}
