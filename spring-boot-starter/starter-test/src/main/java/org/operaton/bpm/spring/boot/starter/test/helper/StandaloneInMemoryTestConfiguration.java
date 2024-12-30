/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.spring.boot.starter.test.helper;

import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.mock.MockExpressionManager;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class StandaloneInMemoryTestConfiguration extends StandaloneInMemProcessEngineConfiguration {

  public StandaloneInMemoryTestConfiguration(ProcessEnginePlugin... plugins) {
    this(Optional.ofNullable(plugins)
      .map(Arrays::asList)
      .orElse(emptyList())
    );
  }

  public StandaloneInMemoryTestConfiguration(List<ProcessEnginePlugin> plugins) {
    jobExecutorActivate = false;
    expressionManager = new MockExpressionManager();
    databaseSchemaUpdate = DB_SCHEMA_UPDATE_DROP_CREATE;
    isDbMetricsReporterActivate = false;
    historyLevel = HistoryLevel.HISTORY_LEVEL_FULL;

    getProcessEnginePlugins().addAll(plugins);
  }

  public ProcessEngineExtension extension() {
    var extension = new ProcessEngineExtension();
    var processEngine = buildProcessEngine();
    extension.setProcessEngine(processEngine);

    return extension;
  }
}
