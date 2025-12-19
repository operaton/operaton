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

import org.operaton.bpm.engine.exception.cmmn.CaseDefinitionNotFoundException;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionEntity;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.persistence.AbstractResourceDefinitionManager;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author: Johannes Heinemann
 */
public class CaseDefinitionCache extends ResourceDefinitionCache<CaseDefinitionEntity> {
  private static final String VAR_CASE_DEFINITION = "caseDefinition";
  private static final String VAR_CACHED_CASE_DEFINITION = "cachedCaseDefinition";

  public CaseDefinitionCache(CacheFactory factory, int cacheCapacity, CacheDeployer cacheDeployer) {
    super(factory, cacheCapacity, cacheDeployer);
  }

  public CaseDefinitionEntity getCaseDefinitionById(String caseDefinitionId) {
    checkInvalidDefinitionId(caseDefinitionId);
    CaseDefinitionEntity caseDefinition = getDefinition(caseDefinitionId);
    if (caseDefinition == null) {
      caseDefinition = findDeployedDefinitionById(caseDefinitionId);

    }
    return caseDefinition;
  }

  @Override
  protected AbstractResourceDefinitionManager<CaseDefinitionEntity> getManager() {
    return Context.getCommandContext().getCaseDefinitionManager();
  }

  @Override
  protected void checkInvalidDefinitionId(String definitionId) {
    ensureNotNull("Invalid case definition id", "caseDefinitionId", definitionId);
  }

  @Override
  protected void checkDefinitionFound(String definitionId, CaseDefinitionEntity definition) {
    ensureNotNull(CaseDefinitionNotFoundException.class, "no deployed case definition found with id '%s'".formatted(definitionId),
      VAR_CASE_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionByKey(String definitionKey, CaseDefinitionEntity definition) {
    ensureNotNull(CaseDefinitionNotFoundException.class, "no case definition deployed with key '%s'".formatted(definitionKey),
      VAR_CASE_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionByKeyAndTenantId(String definitionKey, String tenantId, CaseDefinitionEntity definition) {
    ensureNotNull(CaseDefinitionNotFoundException.class, "no case definition deployed with key '%s' and tenant-id '".formatted(definitionKey) + tenantId + "'",
      VAR_CASE_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionByKeyVersionAndTenantId(String definitionKey, Integer definitionVersion, String tenantId, CaseDefinitionEntity definition) {
    ensureNotNull(CaseDefinitionNotFoundException.class, "no case definition deployed with key = '%s', version = '".formatted(definitionKey) + definitionVersion + "'"
        + " and tenant-id = '%s'".formatted(tenantId), VAR_CASE_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionByKeyVersionTagAndTenantId(String definitionKey, String definitionVersionTag, String tenantId, CaseDefinitionEntity definition) {
    throw new UnsupportedOperationException("Version tag is not implemented in case definition.");  }

  @Override
  protected void checkInvalidDefinitionByDeploymentAndKey(String deploymentId, String definitionKey, CaseDefinitionEntity definition) {
    ensureNotNull(CaseDefinitionNotFoundException.class, "no case definition deployed with key = '%s' in deployment = '".formatted(definitionKey) + deploymentId + "'",
      VAR_CASE_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionWasCached(String deploymentId, String definitionId, CaseDefinitionEntity definition) {
    ensureNotNull("deployment '%s' didn't put case definition '".formatted(deploymentId) + definitionId + "' in the cache",
      VAR_CACHED_CASE_DEFINITION, definition);
  }
}
