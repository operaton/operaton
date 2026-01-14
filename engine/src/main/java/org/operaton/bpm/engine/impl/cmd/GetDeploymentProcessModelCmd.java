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
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;

/**
 * Gives access to a deployed process model, e.g., a BPMN 2.0 XML file, through
 * a stream of bytes.
 *
 * @author Falko Menge
 */
public class GetDeploymentProcessModelCmd implements Command<InputStream> {
  protected String processDefinitionId;

  public GetDeploymentProcessModelCmd(String processDefinitionId) {
    if (processDefinitionId == null || processDefinitionId.isEmpty()) {
      throw new ProcessEngineException("The process definition id is mandatory, but '%s' has been provided.".formatted(processDefinitionId));
    }
    this.processDefinitionId = processDefinitionId;
  }

  @Override
  public InputStream execute(final CommandContext commandContext) {
    ProcessDefinitionEntity processDefinition = Context
            .getProcessEngineConfiguration()
            .getDeploymentCache()
            .findDeployedProcessDefinitionById(processDefinitionId);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadProcessDefinition(processDefinition);
    }

    final String deploymentId = processDefinition.getDeploymentId();
    final String resourceName = processDefinition.getResourceName();

    return commandContext.runWithoutAuthorization(
        new GetDeploymentResourceCmd(deploymentId, resourceName));
  }

}
