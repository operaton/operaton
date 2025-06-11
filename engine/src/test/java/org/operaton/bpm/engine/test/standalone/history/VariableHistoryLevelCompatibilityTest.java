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
package org.operaton.bpm.engine.test.standalone.history;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;


/**
 * This test ensures that if a user selects
 * {@link ProcessEngineConfiguration#HISTORY_VARIABLE}, the level is internally
 * mapped to {@link ProcessEngineConfigurationImpl#HISTORYLEVEL_ACTIVITY}.
 *
 * @author Daniel Meyer
 */
class VariableHistoryLevelCompatibilityTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .closeEngineAfterAllTests()
    .configurationResource("org/operaton/bpm/engine/test/standalone/history/variablehistory.operaton.cfg.xml")
    .build();

  ProcessEngineConfigurationImpl processEngineConfiguration;

  @Test
  void testCompatibilty() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    assertThat(historyLevel).isEqualTo(ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY);
  }

}
