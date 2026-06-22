/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.spring.boot.starter.webapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.spring.boot.starter.OperatonBpmAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SessionCookiePathEnforcementAutoConfigurationTest {
  private WebApplicationContextRunner contextRunner;

  @BeforeEach
  void setUp() {
    AutoConfigurations autoConfigurationsUnderTest = AutoConfigurations.of(OperatonBpmAutoConfiguration.class, OperatonBpmWebappAutoConfiguration.class);
    AutoConfigurations additionalAutoConfigurations = AutoConfigurations.of(DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class);
    contextRunner = new WebApplicationContextRunner().withConfiguration(autoConfigurationsUnderTest).withConfiguration(additionalAutoConfigurations);
  }

  @Test
  void sessionCookiePathEnforcement_enabled_should_register_filter_with_correct_params() {
    contextRunner.withPropertyValues(
            "operaton.bpm.webapp.session-cookie-path-enforcement=true",
            "operaton.bpm.webapp.application-path=/my-app",
            "server.servlet.session.cookie.name=CUSTOM_ID"
    ).run(context -> {
      assertThat(context)
          .hasNotFailed()
          .hasBean("sessionCookiePathFilter");

      org.springframework.boot.web.servlet.FilterRegistrationBean<?> filterBean =
          context.getBean("sessionCookiePathFilter", org.springframework.boot.web.servlet.FilterRegistrationBean.class);

      assertThat(filterBean.getUrlPatterns()).contains("/my-app/*");
      assertThat(filterBean.getInitParameters())
          .containsEntry(org.operaton.bpm.spring.boot.starter.webapp.filter.SessionCookiePathFilter.PARAM_COOKIE_PATH, "/my-app")
          .containsEntry(org.operaton.bpm.spring.boot.starter.webapp.filter.SessionCookiePathFilter.PARAM_SESSION_COOKIE_NAME, "CUSTOM_ID");
    });
  }

  @Test
  void sessionCookiePathEnforcement_disabled_should_not_register_filter() {
    contextRunner.withPropertyValues(
        "operaton.bpm.webapp.session-cookie-path-enforcement=false"
    ).run(context -> {
      assertThat(context)
          .hasNotFailed()
          .doesNotHaveBean("sessionCookiePathFilter");
    });
  }

  @Test
  void sessionCookiePathEnforcement_absent_should_not_register_filter() {
    contextRunner.run(context -> {
      assertThat(context)
              .hasNotFailed()
              .doesNotHaveBean("sessionCookiePathFilter");
    });
  }
}