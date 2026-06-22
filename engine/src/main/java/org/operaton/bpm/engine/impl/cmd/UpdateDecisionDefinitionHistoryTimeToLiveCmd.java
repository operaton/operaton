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

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.HistoryTimeToLiveParser;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionEntity;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureGreaterThanOrEqual;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Svetlana Dorokhova
 */
public class UpdateDecisionDefinitionHistoryTimeToLiveCmd implements Command<Void> {
  protected String decisionDefinitionId;
  protected Integer historyTimeToLive;

  public UpdateDecisionDefinitionHistoryTimeToLiveCmd(String decisionDefinitionId, Integer historyTimeToLive) {
    this.decisionDefinitionId = decisionDefinitionId;
    this.historyTimeToLive = historyTimeToLive;
  }

  @Override
  public Void execute(CommandContext context) {
    checkAuthorization(context);

    ensureNotNull(BadUserRequestException.class, "decisionDefinitionId", decisionDefinitionId);

    if (historyTimeToLive != null) {
      ensureGreaterThanOrEqual(BadUserRequestException.class, "", "historyTimeToLive", historyTimeToLive, 0);
    }

    validate(historyTimeToLive, context);

    DecisionDefinitionEntity decisionDefinitionEntity = context.getDecisionDefinitionManager().findDecisionDefinitionById(decisionDefinitionId);
    logUserOperation(context, decisionDefinitionEntity);
    decisionDefinitionEntity.setHistoryTimeToLive(historyTimeToLive);

    return null;
  }

  protected void checkAuthorization(CommandContext commandContext) {
    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
        checker.checkUpdateDecisionDefinitionById(decisionDefinitionId);
    }
  }

  protected void logUserOperation(CommandContext commandContext, DecisionDefinitionEntity decisionDefinitionEntity) {
    List<PropertyChange> propertyChanges = new ArrayList<>();
    propertyChanges.add(new PropertyChange("historyTimeToLive", decisionDefinitionEntity.getHistoryTimeToLive(), historyTimeToLive));
    propertyChanges.add(new PropertyChange("decisionDefinitionId", null, decisionDefinitionId));
    propertyChanges.add(new PropertyChange("decisionDefinitionKey", null, decisionDefinitionEntity.getKey()));

    commandContext.getOperationLogManager()
      .logDecisionDefinitionOperation(UserOperationLogEntry.OPERATION_TYPE_UPDATE_HISTORY_TIME_TO_LIVE, decisionDefinitionEntity.getTenantId(), propertyChanges);
  }

  protected void validate(Integer historyTimeToLive, CommandContext context) {
    HistoryTimeToLiveParser parser = HistoryTimeToLiveParser.create(context);
    parser.validate(historyTimeToLive);
  }
}
