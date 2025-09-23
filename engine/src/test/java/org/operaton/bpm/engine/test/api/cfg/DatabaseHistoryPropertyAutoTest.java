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
package org.operaton.bpm.engine.test.api.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.HistoryLevelSetupCommand;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.history.HistoryLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseHistoryPropertyAutoTest {

  protected List<ProcessEngineImpl> processEngines = new ArrayList<>();

  private static ProcessEngineConfigurationImpl config(final String historyLevel) {

    return config("false", historyLevel);
  }

  private static ProcessEngineConfigurationImpl config(final String schemaUpdate, final String historyLevel) {
    StandaloneInMemProcessEngineConfiguration engineConfiguration = new StandaloneInMemProcessEngineConfiguration();
    engineConfiguration.setProcessEngineName(UUID.randomUUID().toString());
    engineConfiguration.setDatabaseSchemaUpdate(schemaUpdate);
    engineConfiguration.setHistory(historyLevel);
    engineConfiguration.setDbMetricsReporterActivate(false);
    engineConfiguration.setJdbcUrl("jdbc:h2:mem:DatabaseHistoryPropertyAutoTest");

    return engineConfiguration;
  }


  @Test
  void failWhenSecondEngineDoesNotHaveTheSameHistoryLevel() {
    buildEngine(config("true", ProcessEngineConfiguration.HISTORY_FULL));
    ProcessEngineConfigurationImpl config = config(ProcessEngineConfiguration.HISTORY_AUDIT);

    assertThatThrownBy(() -> buildEngine(config))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("historyLevel mismatch: configuration says HistoryLevelAudit(name=audit, id=2) and database says HistoryLevelFull(name=full, id=3)");
  }

  @Test
  void secondEngineCopiesHistoryLevelFromFirst() {
    // given
    buildEngine(config("true", ProcessEngineConfiguration.HISTORY_FULL));

    // when
    ProcessEngineImpl processEngineTwo = buildEngine(config("true", ProcessEngineConfiguration.HISTORY_AUTO));

    // then
    assertThat(processEngineTwo.getProcessEngineConfiguration().getHistory()).isSameAs(ProcessEngineConfiguration.HISTORY_AUTO);
    assertThat(processEngineTwo.getProcessEngineConfiguration().getHistoryLevel()).isSameAs(HistoryLevel.HISTORY_LEVEL_FULL);

  }

  @Test
  void usesDefaultValueAuditWhenNoValueIsConfigured() {
    final ProcessEngineConfigurationImpl config = config("true", ProcessEngineConfiguration.HISTORY_AUTO);
    ProcessEngineImpl processEngine = buildEngine(config);

    final Integer level = config.getCommandExecutorSchemaOperations().execute(HistoryLevelSetupCommand::databaseHistoryLevel);

    assertThat(level).isEqualTo(HistoryLevel.HISTORY_LEVEL_AUDIT.getId());

    assertThat(processEngine.getProcessEngineConfiguration().getHistoryLevel()).isEqualTo(HistoryLevel.HISTORY_LEVEL_AUDIT);
  }

  @AfterEach
  void after() {
    for (ProcessEngineImpl engine : processEngines) {
      // no need to drop schema when testing with h2
      engine.close();
    }

    processEngines.clear();
  }

  protected ProcessEngineImpl buildEngine(ProcessEngineConfigurationImpl engineConfiguration) {
    ProcessEngineImpl engine = (ProcessEngineImpl) engineConfiguration.buildProcessEngine();
    processEngines.add(engine);

    return engine;
  }

}
