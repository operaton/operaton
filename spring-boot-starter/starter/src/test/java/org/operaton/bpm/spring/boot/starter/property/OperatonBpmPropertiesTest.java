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
package org.operaton.bpm.spring.boot.starter.property;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
@ActiveProfiles("test")
class OperatonBpmPropertiesTest extends AbstractOperatonBpmPropertiesTest<OperatonBpmProperties> {

  @Test
  void initResourcePatterns() {
    final String[] patterns = OperatonBpmProperties.initDeploymentResourcePattern();

    assertThat(patterns)
            .hasSize(7)
            .containsOnly("classpath*:**/*.bpmn", "classpath*:**/*.bpmn20.xml", "classpath*:**/*.dmn", "classpath*:**/*.dmn11.xml",
      "classpath*:**/*.cmmn", "classpath*:**/*.cmmn10.xml", "classpath*:**/*.cmmn11.xml");
  }

  @Test
  void restrict_allowed_values_for_dbUpdate() {
    new OperatonBpmProperties().getDatabase().setSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
    new OperatonBpmProperties().getDatabase().setSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);
    new OperatonBpmProperties().getDatabase().setSchemaUpdate(ProcessEngineConfigurationImpl.DB_SCHEMA_UPDATE_CREATE);
    new OperatonBpmProperties().getDatabase().setSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP);
    new OperatonBpmProperties().getDatabase().setSchemaUpdate(ProcessEngineConfigurationImpl.DB_SCHEMA_UPDATE_DROP_CREATE);

    DatabaseProperty databaseProperty = new OperatonBpmProperties().getDatabase();
    assertThatIllegalArgumentException().isThrownBy(() -> databaseProperty.setSchemaUpdate("foo"));
  }

  @Test
  void shouldBindPreviewFeaturesEnabledProperty() {
    // given
    TestPropertyValues.of("operaton.bpm.preview-features-enabled=true")
      .applyTo(environment);

    binder.bind(OperatonBpmProperties.PREFIX, Bindable.ofInstance(properties));

    // then
    assertThat(properties.getPreviewFeaturesEnabled()).isTrue();
  }

}
