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

import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceStatistics;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceStatisticsQuery;

import java.util.List;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Askar Akhmerov
 */
public class HistoricDecisionInstanceStatisticsQueryImpl extends
    AbstractQuery<HistoricDecisionInstanceStatisticsQuery, HistoricDecisionInstanceStatistics> implements HistoricDecisionInstanceStatisticsQuery {

  protected final String decisionRequirementsDefinitionId;
  protected String decisionInstanceId;

  public HistoricDecisionInstanceStatisticsQueryImpl(String decisionRequirementsDefinitionId, CommandExecutor commandExecutor) {
    super(commandExecutor);
    this.decisionRequirementsDefinitionId = decisionRequirementsDefinitionId;
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();

    return commandContext
        .getStatisticsManager()
        .getStatisticsCountGroupedByDecisionRequirementsDefinition(this);
  }

  @Override
  public List<HistoricDecisionInstanceStatistics> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();

    return commandContext
        .getStatisticsManager()
        .getStatisticsGroupedByDecisionRequirementsDefinition(this, page);
  }

  @Override
  protected void checkQueryOk() {
    super.checkQueryOk();
    ensureNotNull("decisionRequirementsDefinitionId", decisionRequirementsDefinitionId);
  }

  public String getDecisionRequirementsDefinitionId() {
    return decisionRequirementsDefinitionId;
  }

  @Override
  public HistoricDecisionInstanceStatisticsQuery decisionInstanceId(String decisionInstanceId) {
    ensureNotNull(NotValidException.class, "decisionInstanceId", decisionInstanceId);
    this.decisionInstanceId = decisionInstanceId;
    return this;
  }

  public String getDecisionInstanceId() {
    return decisionInstanceId;
  }

  public void setDecisionInstanceId(String decisionInstanceId) {
    ensureNotNull(NotValidException.class, "decisionInstanceId", decisionInstanceId);
    this.decisionInstanceId = decisionInstanceId;
  }

}
