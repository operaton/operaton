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
package org.operaton.bpm.engine.impl.form.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.persistence.AbstractManager;
import org.operaton.bpm.engine.impl.persistence.AbstractResourceDefinitionManager;
import org.operaton.bpm.engine.impl.persistence.entity.OperatonFormDefinitionEntity;

public class OperatonFormDefinitionManager extends AbstractManager
    implements AbstractResourceDefinitionManager<OperatonFormDefinitionEntity> {

  private static final String OPERATON_FORM_DEFINITION_KEY = "operatonFormDefinitionKey";
  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  @Override
  public OperatonFormDefinitionEntity findLatestDefinitionByKey(String key) {
    @SuppressWarnings("unchecked")
    List<OperatonFormDefinitionEntity> operatonFormDefinitions = getDbEntityManager()
        .selectList("selectLatestOperatonFormDefinitionByKey", configureParameterizedQuery(key));

    if (operatonFormDefinitions.isEmpty()) {
      return null;

    } else if (operatonFormDefinitions.size() == 1) {
      return operatonFormDefinitions.iterator().next();

    } else {
      throw LOG.multipleTenantsForOperatonFormDefinitionKeyException(key);
    }
  }

  @Override
  public OperatonFormDefinitionEntity findLatestDefinitionById(String id) {
    return getDbEntityManager().selectById(OperatonFormDefinitionEntity.class, id);
  }

  @Override
  public OperatonFormDefinitionEntity findLatestDefinitionByKeyAndTenantId(String definitionKey, String tenantId) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(OPERATON_FORM_DEFINITION_KEY, definitionKey);
    parameters.put(TENANT_ID, tenantId);

    if (tenantId == null) {
      return (OperatonFormDefinitionEntity) getDbEntityManager()
          .selectOne("selectLatestOperatonFormDefinitionByKeyWithoutTenantId", parameters);
    } else {
      return (OperatonFormDefinitionEntity) getDbEntityManager()
          .selectOne("selectLatestOperatonDefinitionByKeyAndTenantId", parameters);
    }
  }

  @Override
  public OperatonFormDefinitionEntity findDefinitionByKeyVersionAndTenantId(String definitionKey,
      Integer definitionVersion, String tenantId) {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("operatonFormDefinitionVersion", definitionVersion);
    parameters.put(OPERATON_FORM_DEFINITION_KEY, definitionKey);
    parameters.put(TENANT_ID, tenantId);
    if (tenantId == null) {
      return (OperatonFormDefinitionEntity) getDbEntityManager()
          .selectOne("selectOperatonFormDefinitionByKeyVersionWithoutTenantId", parameters);
    } else {
      return (OperatonFormDefinitionEntity) getDbEntityManager()
          .selectOne("selectOperatonFormDefinitionByKeyVersionAndTenantId", parameters);
    }
  }

  @Override
  public OperatonFormDefinitionEntity findDefinitionByDeploymentAndKey(String deploymentId, String definitionKey) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("deploymentId", deploymentId);
    parameters.put(OPERATON_FORM_DEFINITION_KEY, definitionKey);
    return (OperatonFormDefinitionEntity) getDbEntityManager().selectOne("selectOperatonFormDefinitionByDeploymentAndKey",
        parameters);
  }

  @SuppressWarnings("unchecked")
  public List<OperatonFormDefinitionEntity> findDefinitionsByDeploymentId(String deploymentId) {
    return getDbEntityManager().selectList("selectOperatonFormDefinitionByDeploymentId", deploymentId);
  }

  @Override
  public OperatonFormDefinitionEntity getCachedResourceDefinitionEntity(String definitionId) {
    return getDbEntityManager().getCachedEntity(OperatonFormDefinitionEntity.class, definitionId);
  }

  @Override
  public OperatonFormDefinitionEntity findDefinitionByKeyVersionTagAndTenantId(String definitionKey,
      String definitionVersionTag, String tenantId) {
    throw new UnsupportedOperationException(
        "Currently finding Operaton Form definition by version tag and tenant is not implemented.");
  }

  public void deleteOperatonFormDefinitionsByDeploymentId(String deploymentId) {
    getDbEntityManager().delete(OperatonFormDefinitionEntity.class, "deleteOperatonFormDefinitionsByDeploymentId",
        deploymentId);
  }

  protected ListQueryParameterObject configureParameterizedQuery(Object parameter) {
    return getTenantManager().configureQuery(parameter);
  }

}
