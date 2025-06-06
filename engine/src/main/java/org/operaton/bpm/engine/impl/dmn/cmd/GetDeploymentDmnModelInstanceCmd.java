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
package org.operaton.bpm.engine.impl.dmn.cmd;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import org.operaton.bpm.engine.exception.dmn.DmnModelInstanceNotFoundException;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionEntity;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.model.dmn.DmnModelInstance;

/**
 * Gives access to a deployed DMN model instance which can be accessed by the
 * DMN model API.
 */
public class GetDeploymentDmnModelInstanceCmd implements Command<DmnModelInstance> {

  protected String decisionDefinitionId;

  public GetDeploymentDmnModelInstanceCmd(String decisionDefinitionId) {
    this.decisionDefinitionId = decisionDefinitionId;
  }

  @Override
  public DmnModelInstance execute(CommandContext commandContext) {
    ensureNotNull("decisionDefinitionId", decisionDefinitionId);

    DeploymentCache deploymentCache = Context.getProcessEngineConfiguration().getDeploymentCache();

    DecisionDefinitionEntity decisionDefinition = deploymentCache.findDeployedDecisionDefinitionById(decisionDefinitionId);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadDecisionDefinition(decisionDefinition);
    }

    DmnModelInstance modelInstance = deploymentCache.findDmnModelInstanceForDecisionDefinition(decisionDefinitionId);

    ensureNotNull(DmnModelInstanceNotFoundException.class, "No DMN model instance found for decision definition id " + decisionDefinitionId, "modelInstance",
        modelInstance);
    return modelInstance;
  }

}
