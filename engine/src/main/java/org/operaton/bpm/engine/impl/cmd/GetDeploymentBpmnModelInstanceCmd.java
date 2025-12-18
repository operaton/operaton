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

import java.io.Serial;
import java.io.Serializable;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * Gives access to a deployed BPMN model instance which can be accessed by
 * the BPMN model API.
 *
 * @author Sebastian Menski
 */
public class GetDeploymentBpmnModelInstanceCmd implements Command<BpmnModelInstance>, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;
  protected String processDefinitionId;

  public GetDeploymentBpmnModelInstanceCmd(String processDefinitionId) {
    if (processDefinitionId == null || processDefinitionId.isEmpty()) {
      throw new ProcessEngineException("The process definition id is mandatory, but '%s' has been provided.".formatted(processDefinitionId));
    }
    this.processDefinitionId = processDefinitionId;
  }

  @Override
  public BpmnModelInstance execute(CommandContext commandContext) {
    ProcessEngineConfigurationImpl configuration = Context.getProcessEngineConfiguration();
    final DeploymentCache deploymentCache = configuration.getDeploymentCache();

    ProcessDefinitionEntity processDefinition = deploymentCache.findDeployedProcessDefinitionById(processDefinitionId);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadProcessDefinition(processDefinition);
    }

    BpmnModelInstance modelInstance = deploymentCache.findBpmnModelInstanceForProcessDefinition(processDefinitionId);

    ensureNotNull("no BPMN model instance found for process definition id " + processDefinitionId, "modelInstance", modelInstance);
    return modelInstance;
  }
}
