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
package org.operaton.bpm.engine.impl.cmd;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.metrics.reporter.DbMetricsReporter;

/**
 * @author Thorben Lindhauer
 *
 */
public class ReportDbMetricsCmd implements Command<Void> {
  @Override
  public Void execute(CommandContext commandContext) {
    ProcessEngineConfigurationImpl engineConfiguration = Context.getProcessEngineConfiguration();

    if (!engineConfiguration.isMetricsEnabled()) {
      throw new ProcessEngineException("Metrics reporting is disabled");
    }

    if (!engineConfiguration.isDbMetricsReporterActivate()) {
      throw new ProcessEngineException("Metrics reporting to database is disabled");
    }

    DbMetricsReporter dbMetricsReporter = engineConfiguration.getDbMetricsReporter();
    dbMetricsReporter.reportNow();
    return null;
  }

}
