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
package org.operaton.bpm.spring.boot.starter.webapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import org.operaton.bpm.spring.boot.starter.OperatonBpmAutoConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.property.WebappProperty;

import static org.assertj.core.api.Assertions.assertThat;

class OperatonBpmWebappAutoConfigurationIntegrationTest {

  private final String bpmEnabled = OperatonBpmProperties.PREFIX + ".enabled=true";

  private final String bpmDisabled = OperatonBpmProperties.PREFIX + ".enabled=false";

  private final String webappEnabled = WebappProperty.PREFIX + ".enabled=true";

  private final String webappDisabled = WebappProperty.PREFIX + ".enabled=false";

  private WebApplicationContextRunner contextRunner;

  @BeforeEach
  void setUp() {
    AutoConfigurations autoConfigurationsUnderTest = AutoConfigurations.of(OperatonBpmAutoConfiguration.class, OperatonBpmWebappAutoConfiguration.class);
    AutoConfigurations additionalAutoConfigurations = AutoConfigurations.of(DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class);
    contextRunner = new WebApplicationContextRunner().withConfiguration(autoConfigurationsUnderTest).withConfiguration(additionalAutoConfigurations);
  }

  @Test
  void bpm_is_not_disabled_and_webapp_is_not_disabled_should_init_webapp() {
    contextRunner.run(context ->
      assertThat(context)
        .hasNotFailed()
        .hasSingleBean(OperatonBpmWebappInitializer.class)
        .hasSingleBean(FaviconResourceResolver.class));
  }

  @Test
  void bpm_is_enabled_and_webapp_is_not_disabled_should_init_webapp() {
    contextRunner.withPropertyValues(bpmEnabled).run(context ->
      assertThat(context)
        .hasNotFailed()
        .hasSingleBean(OperatonBpmWebappInitializer.class)
        .hasSingleBean(FaviconResourceResolver.class));
  }

  @Test
  void bpm_is_disabled_and_webapp_is_not_disabled_should_not_init_webapp() {
    contextRunner.withPropertyValues(bpmDisabled).run(context ->
      assertThat(context)
        .hasNotFailed()
        .doesNotHaveBean(OperatonBpmWebappInitializer.class)
        .doesNotHaveBean(FaviconResourceResolver.class));
  }

  @Test
  void bpm_is_not_disabled_and_webapp_is_enabled_should_init_webapp() {
    contextRunner.withPropertyValues(webappEnabled).run(context ->
      assertThat(context)
        .hasNotFailed()
        .hasSingleBean(OperatonBpmWebappInitializer.class)
        .hasSingleBean(FaviconResourceResolver.class));
  }

  @Test
  void bpm_is_enabled_and_webapp_is_enabled_should_init_webapp() {
    contextRunner.withPropertyValues(bpmEnabled, webappEnabled).run(context ->
      assertThat(context)
        .hasNotFailed()
        .hasSingleBean(OperatonBpmWebappInitializer.class)
        .hasSingleBean(FaviconResourceResolver.class));
  }

  @Test
  void bpm_is_disabled_and_webapp_is_enabled_should_not_init_webapp() {
    contextRunner.withPropertyValues(bpmDisabled, webappEnabled).run(context ->
      assertThat(context)
        .hasNotFailed()
        .doesNotHaveBean(OperatonBpmWebappInitializer.class)
        .doesNotHaveBean(FaviconResourceResolver.class));
  }

  @Test
  void bpm_is_not_disabled_and_webapp_is_disabled_should_not_init_webapp() {
    contextRunner.withPropertyValues(webappDisabled).run(context ->
      assertThat(context)
        .hasNotFailed()
        .doesNotHaveBean(OperatonBpmWebappInitializer.class)
        .doesNotHaveBean(FaviconResourceResolver.class));
  }

  @Test
  void bpm_is_enabled_and_webapp_is_disabled_should_not_init_webapp() {
    contextRunner.withPropertyValues(bpmEnabled, webappDisabled).run(context ->
      assertThat(context)
        .hasNotFailed()
        .doesNotHaveBean(OperatonBpmWebappInitializer.class)
        .doesNotHaveBean(FaviconResourceResolver.class));
  }

  @Test
  void bpm_is_disabled_and_webapp_is_disabled_should_not_init_webapp() {
    contextRunner.withPropertyValues(bpmDisabled, webappDisabled).run(context ->
      assertThat(context)
        .hasNotFailed()
        .doesNotHaveBean(OperatonBpmWebappInitializer.class)
        .doesNotHaveBean(FaviconResourceResolver.class));
  }
}
