/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.cmmn.cmd;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import org.operaton.bpm.engine.exception.cmmn.CmmnModelInstanceNotFoundException;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionEntity;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;

/**
 * @author Daniel Meyer
 * @author Roman Smirnov
 *
 */
public class GetDeploymentCmmnModelInstanceCmd implements Command<CmmnModelInstance> {

  protected String caseDefinitionId;

  public GetDeploymentCmmnModelInstanceCmd(String caseDefinitionId) {
    this.caseDefinitionId = caseDefinitionId;
  }

  @Override
  public CmmnModelInstance execute(CommandContext commandContext) {
    ensureNotNull("caseDefinitionId", caseDefinitionId);

    ProcessEngineConfigurationImpl configuration = Context.getProcessEngineConfiguration();
    final DeploymentCache deploymentCache = configuration.getDeploymentCache();

    CaseDefinitionEntity caseDefinition = deploymentCache.findDeployedCaseDefinitionById(caseDefinitionId);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadCaseDefinition(caseDefinition);
    }

    CmmnModelInstance modelInstance = Context
        .getProcessEngineConfiguration()
        .getDeploymentCache()
        .findCmmnModelInstanceForCaseDefinition(caseDefinitionId);

    ensureNotNull(CmmnModelInstanceNotFoundException.class, "No CMMN model instance found for case definition id " + caseDefinitionId, "modelInstance", modelInstance);
    return modelInstance;
  }

}
