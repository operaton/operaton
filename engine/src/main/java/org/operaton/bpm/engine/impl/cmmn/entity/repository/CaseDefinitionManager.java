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
package org.operaton.bpm.engine.impl.cmmn.entity.repository;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.persistence.AbstractManager;
import org.operaton.bpm.engine.impl.persistence.AbstractResourceDefinitionManager;
import org.operaton.bpm.engine.repository.CaseDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Smirnov
 *
 */
public class CaseDefinitionManager extends AbstractManager implements AbstractResourceDefinitionManager<CaseDefinitionEntity> {

  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;
  private static final String PARAM_CASE_DEFINITION_KEY = "caseDefinitionKey";
  private static final String PARAM_TENANT_ID = "tenantId";
  private static final String PARAM_CASE_DEFINITION_VERSION = "caseDefinitionVersion";
  private static final String PARAM_DEPLOYMENT_ID = "deploymentId";
  private static final String PARAM_KEY = "key";
  private static final String PARAM_VERSION = "version";

  public void insertCaseDefinition(CaseDefinitionEntity caseDefinition) {
    getDbEntityManager().insert(caseDefinition);
  }

  public void deleteCaseDefinitionsByDeploymentId(String deploymentId) {
    getDbEntityManager().delete(CaseDefinitionEntity.class, "deleteCaseDefinitionsByDeploymentId", deploymentId);
  }

  public CaseDefinitionEntity findCaseDefinitionById(String caseDefinitionId) {
    return getDbEntityManager().selectById(CaseDefinitionEntity.class, caseDefinitionId);
  }

  /**
   * @return the latest version of the case definition with the given key (from any tenant)
   *
   * @throws ProcessEngineException if more than one tenant has a case definition with the given key
   *
   * @see #findLatestCaseDefinitionByKeyAndTenantId(String, String)
   */
  public CaseDefinitionEntity findLatestCaseDefinitionByKey(String caseDefinitionKey) {
    @SuppressWarnings("unchecked")
    List<CaseDefinitionEntity> caseDefinitions = getDbEntityManager().selectList("selectLatestCaseDefinitionByKey", configureParameterizedQuery(caseDefinitionKey));

    if (caseDefinitions.isEmpty()) {
      return null;

    } else if (caseDefinitions.size() == 1) {
      return caseDefinitions.iterator().next();

    } else {
      throw LOG.multipleTenantsForCaseDefinitionKeyException(caseDefinitionKey);
    }
  }

  /**
   * @return the latest version of the case definition with the given key and tenant id
   *
   * @see #findLatestCaseDefinitionByKeyAndTenantId(String, String)
   */
  public CaseDefinitionEntity findLatestCaseDefinitionByKeyAndTenantId(String caseDefinitionKey, String tenantId) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(PARAM_CASE_DEFINITION_KEY, caseDefinitionKey);
    parameters.put(PARAM_TENANT_ID, tenantId);

    if (tenantId == null) {
      return (CaseDefinitionEntity) getDbEntityManager().selectOne("selectLatestCaseDefinitionByKeyWithoutTenantId", parameters);
    } else {
      return (CaseDefinitionEntity) getDbEntityManager().selectOne("selectLatestCaseDefinitionByKeyAndTenantId", parameters);
    }
  }

  public CaseDefinitionEntity findCaseDefinitionByKeyVersionAndTenantId(String caseDefinitionKey, Integer caseDefinitionVersion, String tenantId) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PARAM_CASE_DEFINITION_VERSION, caseDefinitionVersion);
    parameters.put(PARAM_CASE_DEFINITION_KEY, caseDefinitionKey);
    parameters.put(PARAM_TENANT_ID, tenantId);
    return (CaseDefinitionEntity) getDbEntityManager().selectOne("selectCaseDefinitionByKeyVersionAndTenantId", parameters);
  }

  public CaseDefinitionEntity findCaseDefinitionByDeploymentAndKey(String deploymentId, String caseDefinitionKey) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(PARAM_DEPLOYMENT_ID, deploymentId);
    parameters.put(PARAM_CASE_DEFINITION_KEY, caseDefinitionKey);
    return (CaseDefinitionEntity) getDbEntityManager().selectOne("selectCaseDefinitionByDeploymentAndKey", parameters);
  }

  public String findPreviousCaseDefinitionId(String caseDefinitionKey, Integer version, String tenantId) {
    Map<String, Object> params = new HashMap<>();
    params.put(PARAM_KEY, caseDefinitionKey);
    params.put(PARAM_VERSION, version);
    params.put(PARAM_TENANT_ID, tenantId);
    return (String) getDbEntityManager().selectOne("selectPreviousCaseDefinitionId", params);
  }

  @SuppressWarnings("unchecked")
  public List<CaseDefinition> findCaseDefinitionsByQueryCriteria(CaseDefinitionQueryImpl caseDefinitionQuery, Page page) {
    configureCaseDefinitionQuery(caseDefinitionQuery);
    return getDbEntityManager().selectList("selectCaseDefinitionsByQueryCriteria", caseDefinitionQuery, page);
  }

  public long findCaseDefinitionCountByQueryCriteria(CaseDefinitionQueryImpl caseDefinitionQuery) {
    configureCaseDefinitionQuery(caseDefinitionQuery);
    return (Long) getDbEntityManager().selectOne("selectCaseDefinitionCountByQueryCriteria", caseDefinitionQuery);
  }

  @SuppressWarnings("unchecked")
  public List<CaseDefinition> findCaseDefinitionByDeploymentId(String deploymentId) {
    return getDbEntityManager().selectList("selectCaseDefinitionByDeploymentId", deploymentId);
  }

  protected void configureCaseDefinitionQuery(CaseDefinitionQueryImpl query) {
    getTenantManager().configureQuery(query);
  }

  protected ListQueryParameterObject configureParameterizedQuery(Object parameter) {
    return getTenantManager().configureQuery(parameter);
  }

  @Override
  public CaseDefinitionEntity findLatestDefinitionByKey(String key) {
    return findLatestCaseDefinitionByKey(key);
  }

  @Override
  public CaseDefinitionEntity findLatestDefinitionById(String id) {
    return findCaseDefinitionById(id);
  }

  @Override
  public CaseDefinitionEntity getCachedResourceDefinitionEntity(String definitionId) {
    return getDbEntityManager().getCachedEntity(CaseDefinitionEntity.class, definitionId);
  }

  @Override
  public CaseDefinitionEntity findLatestDefinitionByKeyAndTenantId(String definitionKey, String tenantId) {
    return findLatestCaseDefinitionByKeyAndTenantId(definitionKey, tenantId);
  }

  @Override
  public CaseDefinitionEntity findDefinitionByKeyVersionTagAndTenantId(String definitionKey, String definitionVersionTag, String tenantId) {
    throw new UnsupportedOperationException("Currently finding case definition by version tag and tenant is not implemented.");
  }

  @Override
  public CaseDefinitionEntity findDefinitionByKeyVersionAndTenantId(String definitionKey, Integer definitionVersion, String tenantId) {
    return findCaseDefinitionByKeyVersionAndTenantId(definitionKey, definitionVersion, tenantId);
  }

  @Override
  public CaseDefinitionEntity findDefinitionByDeploymentAndKey(String deploymentId, String definitionKey) {
    return findCaseDefinitionByDeploymentAndKey(deploymentId, definitionKey);
  }
}
