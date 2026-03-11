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
package org.operaton.bpm.engine.test.standalone.initialization;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Tom Baeyens
 * @author Stefan Hentschel
 * @author Roman Smirnov
 */
class ProcessEngineInitializationTest {

  @Test
  void testNoTables() {
    // given
    var processEngineConfiguration = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/standalone/initialization/notables.operaton.cfg.xml");
    processEngineConfiguration.setProcessEngineName("testProcessEngine");

    // when/then
    assertThatThrownBy(processEngineConfiguration::buildProcessEngine)
      .isInstanceOf(Exception.class)
      .hasMessageContaining("ENGINE-03057 There are no Operaton tables in the database. " +
        "Hint: Set <property name=\"databaseSchemaUpdate\" to value=\"true\" or value=\"create-drop\" " +
        "(use create-drop for testing only!) in bean processEngineConfiguration " +
        "in operaton.cfg.xml for automatic schema creation");
  }

  @Test
  void testDefaultRetries() {
    ProcessEngineConfiguration configuration = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/standalone/initialization/defaultretries.operaton.cfg.xml");

    assertThat(configuration.getDefaultNumberOfRetries()).isEqualTo(JobEntity.DEFAULT_RETRIES);
  }

  @Test
  void testCustomDefaultRetries() {
    ProcessEngineConfiguration configuration = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/standalone/initialization/customdefaultretries.operaton.cfg.xml");

    assertThat(configuration.getDefaultNumberOfRetries()).isEqualTo(5);
  }

}
