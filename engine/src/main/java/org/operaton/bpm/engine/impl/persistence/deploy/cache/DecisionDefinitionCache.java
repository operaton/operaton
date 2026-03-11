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
package org.operaton.bpm.engine.impl.persistence.deploy.cache;

import org.operaton.bpm.engine.exception.dmn.DecisionDefinitionNotFoundException;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionEntity;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionManager;
import org.operaton.bpm.engine.impl.persistence.AbstractResourceDefinitionManager;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author: Johannes Heinemann
 */
public class DecisionDefinitionCache extends ResourceDefinitionCache<DecisionDefinitionEntity> {

  private static final String VAR_DECISION_DEFINITION = "decisionDefinition";
  private static final String VAR_CACHED_CASE_DEFINITION = "cachedCaseDefinition";

  public DecisionDefinitionCache(CacheFactory factory, int cacheCapacity, CacheDeployer cacheDeployer) {
    super(factory, cacheCapacity, cacheDeployer);
  }

  public DecisionDefinitionEntity findDeployedDefinitionByKeyAndVersion(String definitionKey, Integer definitionVersion) {
    DecisionDefinitionEntity definition = ((DecisionDefinitionManager) getManager())
        .findDecisionDefinitionByKeyAndVersion(definitionKey, definitionVersion);

    checkInvalidDefinitionByKeyAndVersion(definitionKey, definitionVersion, definition);
    return resolveDefinition(definition);
  }

  @Override
  protected AbstractResourceDefinitionManager<DecisionDefinitionEntity> getManager() {
    return Context.getCommandContext().getDecisionDefinitionManager();
  }

  @Override
  protected void checkInvalidDefinitionId(String definitionId) {
    ensureNotNull("Invalid decision definition id", "decisionDefinitionId", definitionId);
  }

  @Override
  protected void checkDefinitionFound(String definitionId, DecisionDefinitionEntity definition) {
    ensureNotNull(DecisionDefinitionNotFoundException.class, "no deployed decision definition found with id '%s'".formatted(definitionId),
      VAR_DECISION_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionByKey(String definitionKey, DecisionDefinitionEntity definition) {
    ensureNotNull(DecisionDefinitionNotFoundException.class, "no decision definition deployed with key '%s'".formatted(definitionKey),
      VAR_DECISION_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionByKeyAndTenantId(String definitionKey, String tenantId, DecisionDefinitionEntity definition) {
    ensureNotNull(DecisionDefinitionNotFoundException.class, "no decision definition deployed with key '%s' and tenant-id '%s'".formatted(definitionKey, tenantId),
      VAR_DECISION_DEFINITION, definition);
  }

  protected void checkInvalidDefinitionByKeyAndVersion(String decisionDefinitionKey, Integer decisionDefinitionVersion, DecisionDefinitionEntity decisionDefinition) {
    ensureNotNull(DecisionDefinitionNotFoundException.class, "no decision definition deployed with key = '%s' and version = '%s'".formatted(decisionDefinitionKey, decisionDefinitionVersion),
      VAR_DECISION_DEFINITION, decisionDefinition);
  }

  @Override
  protected void checkInvalidDefinitionByKeyVersionAndTenantId(String definitionKey, Integer definitionVersion, String tenantId, DecisionDefinitionEntity definition) {
    ensureNotNull(DecisionDefinitionNotFoundException.class, "no decision definition deployed with key = '%s', version = '%s' and tenant-id = '%s'".formatted(definitionKey, definitionVersion, tenantId),
      VAR_DECISION_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionByKeyVersionTagAndTenantId(String definitionKey, String definitionVersionTag, String tenantId, DecisionDefinitionEntity definition) {
    ensureNotNull(DecisionDefinitionNotFoundException.class, "no decision definition deployed with key = '%s', versionTag = '%s' and tenant-id = '%s'".formatted(definitionKey, definitionVersionTag, tenantId),
      VAR_DECISION_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionByDeploymentAndKey(String deploymentId, String definitionKey, DecisionDefinitionEntity definition) {
    ensureNotNull(DecisionDefinitionNotFoundException.class, "no decision definition deployed with key = '%s' in deployment = '%s'".formatted(definitionKey, deploymentId),
      VAR_DECISION_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionWasCached(String deploymentId, String definitionId, DecisionDefinitionEntity definition) {
    ensureNotNull("deployment '%s' didn't put decision definition '%s' in the cache".formatted(deploymentId, definitionId), VAR_CACHED_CASE_DEFINITION, definition);
  }
}
