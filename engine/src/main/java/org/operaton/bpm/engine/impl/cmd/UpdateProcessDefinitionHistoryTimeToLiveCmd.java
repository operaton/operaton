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

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureGreaterThanOrEqual;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serializable;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.HistoryTimeToLiveParser;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;

/**
 * @author Svetlana Dorokhova
 */
public class UpdateProcessDefinitionHistoryTimeToLiveCmd implements Command<Void>, Serializable {

  private static final long serialVersionUID = 1L;

  protected String processDefinitionId;
  protected Integer historyTimeToLive;

  public UpdateProcessDefinitionHistoryTimeToLiveCmd(String processDefinitionId, Integer historyTimeToLive) {
    this.processDefinitionId = processDefinitionId;
    this.historyTimeToLive = historyTimeToLive;
  }

  @Override
  public Void execute(CommandContext context) {
    checkAuthorization(context);

    ensureNotNull(BadUserRequestException.class, "processDefinitionId", processDefinitionId);

    if (historyTimeToLive != null) {
      ensureGreaterThanOrEqual(BadUserRequestException.class, "", "historyTimeToLive", historyTimeToLive, 0);
    }

    HistoryTimeToLiveParser parser = HistoryTimeToLiveParser.create(context);
    parser.validate(historyTimeToLive);

    ProcessDefinitionEntity processDefinitionEntity = context.getProcessDefinitionManager().findLatestProcessDefinitionById(processDefinitionId);
    logUserOperation(context, processDefinitionEntity);
    processDefinitionEntity.setHistoryTimeToLive(historyTimeToLive);

    return null;
  }

  protected void checkAuthorization(CommandContext commandContext) {
    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
        checker.checkUpdateProcessDefinitionById(processDefinitionId);
    }
  }

  protected void logUserOperation(CommandContext commandContext, ProcessDefinitionEntity processDefinitionEntity) {
    PropertyChange propertyChange = new PropertyChange("historyTimeToLive", processDefinitionEntity.getHistoryTimeToLive(), historyTimeToLive);
    commandContext.getOperationLogManager()
        .logProcessDefinitionOperation(UserOperationLogEntry.OPERATION_TYPE_UPDATE_HISTORY_TIME_TO_LIVE, processDefinitionId, processDefinitionEntity.getKey(),
            propertyChange);
  }

}
