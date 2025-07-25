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

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureEquals;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotEmpty;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.HistoricCaseInstanceQueryImpl;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;

public class DeleteHistoricCaseInstancesBulkCmd implements Command<Void>, Serializable {

  private static final long serialVersionUID = 1L;

  protected final List<String> caseInstanceIds;

  public DeleteHistoricCaseInstancesBulkCmd(List<String> caseInstanceIds) {
    this.caseInstanceIds = caseInstanceIds;
  }

  @Override
  public Void execute(CommandContext commandContext) {
    ensureNotEmpty(BadUserRequestException.class, "caseInstanceIds", caseInstanceIds);

    // Check if case instances are all closed
    commandContext.runWithoutAuthorization((Callable<Void>) () -> {
      ensureEquals(BadUserRequestException.class, "ClosedCaseInstanceIds",
          new HistoricCaseInstanceQueryImpl().closed().caseInstanceIds(new HashSet<>(caseInstanceIds)).count(), caseInstanceIds.size());
      return null;
    });

    commandContext.getOperationLogManager().logCaseInstanceOperation(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY,
        null, null, Collections.singletonList(new PropertyChange("nrOfInstances", null, caseInstanceIds.size())));

    commandContext.getHistoricCaseInstanceManager().deleteHistoricCaseInstancesByIds(caseInstanceIds);

    return null;
  }

}
