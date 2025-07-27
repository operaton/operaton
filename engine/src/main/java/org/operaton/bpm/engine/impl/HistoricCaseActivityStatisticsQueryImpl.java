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
package org.operaton.bpm.engine.impl;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serial;
import java.util.List;

import org.operaton.bpm.engine.history.HistoricCaseActivityStatistics;
import org.operaton.bpm.engine.history.HistoricCaseActivityStatisticsQuery;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

/**
 * @author smirnov
 *
 */
public class HistoricCaseActivityStatisticsQueryImpl extends AbstractQuery<HistoricCaseActivityStatisticsQuery, HistoricCaseActivityStatistics> implements
    HistoricCaseActivityStatisticsQuery {

  @Serial private static final long serialVersionUID = 1L;

  protected String caseDefinitionId;

  public HistoricCaseActivityStatisticsQueryImpl(String caseDefinitionId, CommandExecutor commandExecutor) {
    super(commandExecutor);
    this.caseDefinitionId = caseDefinitionId;
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return
      commandContext
        .getHistoricStatisticsManager()
        .getHistoricStatisticsCountGroupedByCaseActivity(this);
  }

  @Override
  public List<HistoricCaseActivityStatistics> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    return
      commandContext
        .getHistoricStatisticsManager()
        .getHistoricStatisticsGroupedByCaseActivity(this, page);
  }

  @Override
  protected void checkQueryOk() {
    super.checkQueryOk();
    ensureNotNull("No valid case definition id supplied", "caseDefinitionId", caseDefinitionId);
  }

  public String getCaseDefinitionId() {
    return caseDefinitionId;
  }

}
