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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cmd.DetermineHistoryLevelCmd;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEventType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DetermineHistoryLevelCmdTest {

  private ProcessEngineImpl processEngineImpl;

  private static ProcessEngineConfigurationImpl config(final String schemaUpdate, final String historyLevel) {
    StandaloneInMemProcessEngineConfiguration engineConfiguration = new StandaloneInMemProcessEngineConfiguration();
    engineConfiguration.setProcessEngineName(UUID.randomUUID().toString());
    engineConfiguration.setDatabaseSchemaUpdate(schemaUpdate);
    engineConfiguration.setHistory(historyLevel);
    engineConfiguration.setDbMetricsReporterActivate(false);
    engineConfiguration.setJdbcUrl("jdbc:h2:mem:DetermineHistoryLevelCmdTest");

    return engineConfiguration;
  }


  @Test
  void readLevelFullFromDB() {
    final ProcessEngineConfigurationImpl config = config("true", ProcessEngineConfiguration.HISTORY_FULL);

    // init the db with level=full
    processEngineImpl = (ProcessEngineImpl) config.buildProcessEngine();

    HistoryLevel historyLevel = config.getCommandExecutorSchemaOperations().execute(new DetermineHistoryLevelCmd(config.getHistoryLevels()));

    assertThat(historyLevel).isEqualTo(HistoryLevel.HISTORY_LEVEL_FULL);
  }


  @Test
  void useDefaultLevelAudit() {
    ProcessEngineConfigurationImpl config = config("true", ProcessEngineConfiguration.HISTORY_AUTO);

    // init the db with level=auto -> audit
    processEngineImpl = (ProcessEngineImpl) config.buildProcessEngine();
    // the history Level has been overwritten with audit
    assertThat(config.getHistoryLevel()).isEqualTo(HistoryLevel.HISTORY_LEVEL_AUDIT);

    // and this is written to the database
    HistoryLevel databaseLevel =
        config.getCommandExecutorSchemaOperations().execute(new DetermineHistoryLevelCmd(config.getHistoryLevels()));
    assertThat(databaseLevel).isEqualTo(HistoryLevel.HISTORY_LEVEL_AUDIT);
  }

  @Test
  void failWhenExistingHistoryLevelIsNotRegistered() {
    // init the db with custom level
    HistoryLevel customLevel = new HistoryLevel() {
      @Override
      public int getId() {
        return 99;
      }

      @Override
      public String getName() {
        return "custom";
      }

      @Override
      public boolean isHistoryEventProduced(HistoryEventType eventType, Object entity) {
        return false;
      }
    };
    ProcessEngineConfigurationImpl config = config("true", "custom");
    config.setCustomHistoryLevels(List.of(customLevel));
    processEngineImpl = (ProcessEngineImpl) config.buildProcessEngine();
    var determineHistoryLevelCmd = new DetermineHistoryLevelCmd(Collections.emptyList());
    var commandExecutor = config.getCommandExecutorSchemaOperations();

    // when/then
    assertThatThrownBy(() -> commandExecutor.execute(determineHistoryLevelCmd))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The configured history level with id='99' is not registered in this config.");
  }

  @AfterEach
  void after() {
    processEngineImpl.close();
    processEngineImpl = null;
  }
}
