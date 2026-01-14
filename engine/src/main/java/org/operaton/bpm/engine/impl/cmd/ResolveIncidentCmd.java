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

import java.util.Collections;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;
import org.operaton.bpm.engine.impl.util.EnsureUtil;
import org.operaton.bpm.engine.runtime.Incident;

/**
 *
 * @author Anna Pazola
 *
 */
public class ResolveIncidentCmd implements Command<Void> {

  protected String incidentId;

  public ResolveIncidentCmd(String incidentId) {
    EnsureUtil.ensureNotNull(BadUserRequestException.class, "", "incidentId", incidentId);
    this.incidentId = incidentId;
  }

  @Override
  public Void execute(CommandContext commandContext) {
    final Incident incident = commandContext.getIncidentManager().findIncidentById(incidentId);

    EnsureUtil.ensureNotNull(NotFoundException.class, "Cannot find an incident with id '%s'".formatted(incidentId),
        "incident", incident);

    if ("failedJob".equals(incident.getIncidentType()) || "failedExternalTask".equals(incident.getIncidentType())) {
      throw new BadUserRequestException("Cannot resolve an incident of type %s".formatted(incident.getIncidentType()));
    }

    EnsureUtil.ensureNotNull(BadUserRequestException.class, "", "executionId", incident.getExecutionId());
    ExecutionEntity execution = commandContext.getExecutionManager().findExecutionById(incident.getExecutionId());

    EnsureUtil.ensureNotNull(BadUserRequestException.class,
        "Cannot find an execution for an incident with id '%s'".formatted(incidentId), "execution", execution);

    for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkUpdateProcessInstance(execution);
    }

    commandContext.getOperationLogManager().logProcessInstanceOperation(UserOperationLogEntry.OPERATION_TYPE_RESOLVE, execution.getProcessInstanceId(),
        execution.getProcessDefinitionId(), null, Collections.singletonList(new PropertyChange("incidentId", null, incidentId)));

    execution.resolveIncident(incidentId);
    return null;
  }
}
