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
package org.operaton.bpm.engine.test.api.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

public class HistoryCleanupDisabledOnBootstrapTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .closeEngineAfterAllTests()
    .randomEngineName()
    .configurator(configuration -> {
      configuration.setJdbcUrl("jdbc:h2:mem:" + HistoryCleanupDisabledOnBootstrapTest.class.getSimpleName());
      configuration.setHistoryCleanupEnabled(false);
      configuration.setHistoryCleanupBatchWindowStartTime("12:00");
      configuration.setDatabaseSchemaUpdate(DB_SCHEMA_UPDATE_CREATE_DROP);
    }).build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  public HistoryService historyService;

  public static ProcessEngineConfigurationImpl engineConfiguration;

  @AfterEach
  void resetConfig() {
    engineConfiguration.setHistoryCleanupEnabled(true);
  }

  @Test
  void shouldNotCreateJobs() {
    // given

    // when
    List<Job> historyCleanupJobs = historyService.findHistoryCleanupJobs();

    // then
    assertThat(historyCleanupJobs).isEmpty();
  }

}
