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

import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.persistence.AbstractResourceDefinitionManager;
import org.operaton.bpm.engine.impl.persistence.entity.OperatonFormDefinitionEntity;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

public class OperatonFormDefinitionCache extends ResourceDefinitionCache<OperatonFormDefinitionEntity> {

  private static final String VAR_OPERATON_FORM_DEFINITION = "operatonFormDefinition";
  private static final String VAR_OPERATON_FORM_DEFINITION_ID = "operatonFormDefinitionId";
  private static final String VAR_CACHED_PROCESS_DEFINITION = "cachedProcessDefinition";

  public OperatonFormDefinitionCache(CacheFactory factory, int cacheCapacity, CacheDeployer cacheDeployer) {
    super(factory, cacheCapacity, cacheDeployer);
  }

  @Override
  protected AbstractResourceDefinitionManager<OperatonFormDefinitionEntity> getManager() {
    return Context.getCommandContext().getOperatonFormDefinitionManager();
  }

  @Override
  protected void checkInvalidDefinitionId(String definitionId) {
    ensureNotNull("Invalid operaton form definition id", VAR_OPERATON_FORM_DEFINITION_ID, definitionId);
  }

  @Override
  protected void checkDefinitionFound(String definitionId, OperatonFormDefinitionEntity definition) {
    ensureNotNull("no deployed operaton form definition found with id '%s'".formatted(definitionId),
      VAR_OPERATON_FORM_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionByKey(String definitionKey, OperatonFormDefinitionEntity definition) {
    ensureNotNull("no deployed operaton form definition found with key '%s'".formatted(definitionKey),
      VAR_OPERATON_FORM_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionByKeyAndTenantId(String definitionKey, String tenantId, OperatonFormDefinitionEntity definition) {
    ensureNotNull("no deployed operaton form definition found with key '%s' and tenant-id '%s'".formatted(definitionKey, tenantId),
      VAR_OPERATON_FORM_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionByKeyVersionAndTenantId(String definitionKey, Integer definitionVersion, String tenantId, OperatonFormDefinitionEntity definition) {
    ensureNotNull("no deployed operaton form definition found with key '%s', version '%s' and tenant-id '%s'".formatted(definitionKey, definitionVersion, tenantId), VAR_OPERATON_FORM_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionByKeyVersionTagAndTenantId(String definitionKey, String definitionVersionTag, String tenantId,
      OperatonFormDefinitionEntity definition) {
    // version tag is currently not supported for OperatonFormDefinition
  }

  @Override
  protected void checkInvalidDefinitionByDeploymentAndKey(String deploymentId, String definitionKey, OperatonFormDefinitionEntity definition) {
    ensureNotNull("no deployed operaton form definition found with key '%s' in deployment '%s'".formatted(definitionKey, deploymentId),
      VAR_OPERATON_FORM_DEFINITION, definition);
  }

  @Override
  protected void checkInvalidDefinitionWasCached(String deploymentId, String definitionId, OperatonFormDefinitionEntity definition) {
    ensureNotNull("deployment '%s' didn't put operaton form definition '%s' in the cache".formatted(deploymentId, definitionId),
      VAR_CACHED_PROCESS_DEFINITION, definition);
  }

}
