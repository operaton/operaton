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

import java.io.InputStream;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.bpmn.diagram.ProcessDiagramLayoutFactory;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.repository.DiagramLayout;

/**
 * Provides positions and dimensions of elements in a process diagram as
 * provided by {@link GetDeploymentProcessDiagramCmd}.
 * <p>
 * This command requires a process model and a diagram image to be deployed.
 * </p>
 * @author Falko Menge
 */
public class GetDeploymentProcessDiagramLayoutCmd implements Command<DiagramLayout> {
  protected String processDefinitionId;

  public GetDeploymentProcessDiagramLayoutCmd(String processDefinitionId) {
    if (processDefinitionId == null || processDefinitionId.isEmpty()) {
      throw new ProcessEngineException("The process definition id is mandatory, but '%s' has been provided.".formatted(processDefinitionId));
    }
    this.processDefinitionId = processDefinitionId;
  }

  @Override
  public DiagramLayout execute(final CommandContext commandContext) {
    ProcessDefinitionEntity processDefinition = Context
        .getProcessEngineConfiguration()
        .getDeploymentCache()
        .findDeployedProcessDefinitionById(processDefinitionId);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadProcessDefinition(processDefinition);
    }

    InputStream processModelStream = commandContext.runWithoutAuthorization(
        new GetDeploymentProcessModelCmd(processDefinitionId));

    InputStream processDiagramStream = commandContext.runWithoutAuthorization(
        new GetDeploymentProcessDiagramCmd(processDefinitionId));

    return new ProcessDiagramLayoutFactory().getProcessDiagramLayout(processModelStream, processDiagramStream);
  }

}
