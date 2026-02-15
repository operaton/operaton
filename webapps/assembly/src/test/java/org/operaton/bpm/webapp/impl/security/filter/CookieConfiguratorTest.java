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
 *
 */
package org.operaton.bpm.webapp.impl.security.filter;

import jakarta.servlet.FilterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.webapp.impl.security.filter.util.CookieConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.operaton.bpm.webapp.impl.security.filter.CookieConfigurator.getSameSiteCookieValueInitValue;

@ExtendWith(MockitoExtension.class)
class CookieConfiguratorTest {
    @Mock
    FilterConfig filterConfig;

    @InjectMocks
    CookieConfigurator cookieConfigurator;

    @BeforeEach
    void setUp() {
        when(filterConfig.getInitParameter(anyString())).thenReturn(null);
        when(filterConfig.getInitParameter(CookieConfigurator.SAME_SITE_OPTION_PARAM)).thenReturn("Lax");

        // Initialize the CookieConfigurator with default values
        cookieConfigurator.parseParams(filterConfig);
    }

    @Test
    void getCookieName_shouldReturnDefaultCookieName_whenNotConfigured() {
        // given
        String expectedCookieName = "MYCOOKIE"; // Default cookie name

        // when
        String actualCookieName = cookieConfigurator.getCookieName("MYCOOKIE");

        // then
        assertThat(actualCookieName).isEqualTo(expectedCookieName);
    }

    @Test
    void getCookieName_shouldReturnCustomCookieName_whenConfiguredByInitParam() {
        // given
        String customCookieName = "CUSTOMCOOKIE";

        // when
        String actualCookieName = cookieConfigurator.getCookieName(customCookieName);

        // then
        assertThat(actualCookieName).isEqualTo(customCookieName);
    }

    @Test
    void getSameSiteCookieValueInitValue_shouldThrow_whenBothParamsAreNonEmpty() {
        // given
        String sameSiteCookieValueInitParam = "value1";
        String sameSiteCookieOptionInitParam = "value2";

        // when & then
        assertThatThrownBy(() -> getSameSiteCookieValueInitValue(sameSiteCookieValueInitParam, sameSiteCookieOptionInitParam))
                .isInstanceOf(ProcessEngineException.class)
                .hasMessageContaining("Please either configure sameSiteCookieOption or sameSiteCookieValue.");
    }

    @ParameterizedTest
    @CsvSource({
            // sameSiteCookieValueInitParam, sameSiteCookieOptionInitParam, expectedResult
            "SomeValue,,SomeValue",
            ",Lax,Lax",
            ",Strict,Strict"
    })
    void getSameSiteCookieValueInitValue_shouldReturnExpectedValue(
            String sameSiteCookieValueInitParam,
            String sameSiteCookieOptionInitParam,
            String expectedResult) {
        // when
        String result = getSameSiteCookieValueInitValue(sameSiteCookieValueInitParam, sameSiteCookieOptionInitParam);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void getSameSiteCookieValueInitValue_shouldThrow_whenInvalidSameSiteOption() {
        // given
        String sameSiteCookieValueInitParam = null;
        String sameSiteCookieOptionInitParam = "InvalidOption";

        // when & then
        assertThatThrownBy(() -> getSameSiteCookieValueInitValue(sameSiteCookieValueInitParam, sameSiteCookieOptionInitParam))
                .isInstanceOf(ProcessEngineException.class)
                .hasMessageContaining("For sameSiteCookieOption param, please configure one of the following options: [Lax, Strict]");
    }

    @Test
    void shouldReturnDefaultSameSiteValue_whenNoParamsSet() {
        // given
        cookieConfigurator.parseParams(filterConfig);

        // when
        String result = cookieConfigurator.getConfig();

        // then
        assertThat(result).contains(CookieConstants.SAME_SITE_FIELD_NAME + "Lax");
    }
}
