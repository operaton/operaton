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
package org.operaton.bpm.engine.impl.cmmn.cmd;

import java.util.Map;

import org.operaton.bpm.engine.exception.cmmn.CaseDefinitionNotFoundException;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.cmmn.CaseInstanceBuilderImpl;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionEntity;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.runtime.CaseInstance;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureAtLeastOneNotNull;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Roman Smirnov
 *
 */
public class CreateCaseInstanceCmd implements Command<CaseInstance> {
  protected String caseDefinitionKey;
  protected String caseDefinitionId;
  protected Map<String, Object> variables;
  protected String businessKey;

  protected String caseDefinitionTenantId;
  protected boolean isTenantIdSet;

  public CreateCaseInstanceCmd(CaseInstanceBuilderImpl builder) {
    this.caseDefinitionKey = builder.getCaseDefinitionKey();
    this.caseDefinitionId = builder.getCaseDefinitionId();
    this.businessKey = builder.getBusinessKey();
    this.variables = builder.getVariables();
    this.caseDefinitionTenantId = builder.getCaseDefinitionTenantId();
    this.isTenantIdSet = builder.isTenantIdSet();
  }

  @Override
  public CaseInstance execute(CommandContext commandContext) {
    ensureAtLeastOneNotNull("caseDefinitionId and caseDefinitionKey are null", caseDefinitionId, caseDefinitionKey);

    CaseDefinitionEntity caseDefinition = find(commandContext);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkCreateCaseInstance(caseDefinition);
    }

    // Start the case instance
    CaseExecutionEntity caseInstance = (CaseExecutionEntity) caseDefinition.createCaseInstance(businessKey);
    caseInstance.create(variables);
    return caseInstance;
  }

  protected CaseDefinitionEntity find(CommandContext commandContext) {
    DeploymentCache deploymentCache = commandContext.getProcessEngineConfiguration().getDeploymentCache();

    // Find the case definition
    CaseDefinitionEntity caseDefinition = null;

    if (caseDefinitionId!=null) {
      caseDefinition =  findById(deploymentCache, caseDefinitionId);

      ensureNotNull(CaseDefinitionNotFoundException.class, "No case definition found for id = '%s'".formatted(caseDefinitionId), "caseDefinition", caseDefinition);

    } else {
      caseDefinition = findByKey(deploymentCache, caseDefinitionKey);

      ensureNotNull(CaseDefinitionNotFoundException.class, "No case definition found for key '%s'".formatted(caseDefinitionKey), "caseDefinition", caseDefinition);
    }

    return caseDefinition;
  }

  protected CaseDefinitionEntity findById(DeploymentCache deploymentCache, String caseDefinitionId) {
    return deploymentCache.findDeployedCaseDefinitionById(caseDefinitionId);
  }

  protected CaseDefinitionEntity findByKey(DeploymentCache deploymentCache, String caseDefinitionKey) {
    if (isTenantIdSet) {
      return deploymentCache.findDeployedLatestCaseDefinitionByKeyAndTenantId(caseDefinitionKey, caseDefinitionTenantId);

    } else {
      return deploymentCache.findDeployedLatestCaseDefinitionByKey(caseDefinitionKey);
    }
  }

}
