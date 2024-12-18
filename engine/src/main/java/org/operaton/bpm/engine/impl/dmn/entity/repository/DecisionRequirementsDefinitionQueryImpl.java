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
package org.operaton.bpm.engine.impl.dmn.entity.repository;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensurePositive;

import java.util.Collections;
import java.util.List;

import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.AbstractQuery;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinitionQuery;

public class DecisionRequirementsDefinitionQueryImpl extends AbstractQuery<DecisionRequirementsDefinitionQuery, DecisionRequirementsDefinition> implements DecisionRequirementsDefinitionQuery {

  private static final long serialVersionUID = 1L;

  protected String id;
  protected String[] ids;
  protected String category;
  protected String categoryLike;
  protected String name;
  protected String nameLike;
  protected String deploymentId;
  protected String key;
  protected String keyLike;
  protected String resourceName;
  protected String resourceNameLike;
  protected Integer version;
  protected boolean latest = false;

  protected boolean isTenantIdSet = false;
  protected String[] tenantIds;
  protected boolean includeDefinitionsWithoutTenantId = false;

  public DecisionRequirementsDefinitionQueryImpl() {
  }

  public DecisionRequirementsDefinitionQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  // Query parameter //////////////////////////////////////////////////////////////

  @Override
  public DecisionRequirementsDefinitionQuery decisionRequirementsDefinitionId(String id) {
    ensureNotNull(NotValidException.class, "id", id);
    this.id = id;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery decisionRequirementsDefinitionIdIn(String... ids) {
    this.ids = ids;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery decisionRequirementsDefinitionCategory(String category) {
    ensureNotNull(NotValidException.class, "category", category);
    this.category = category;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery decisionRequirementsDefinitionCategoryLike(String categoryLike) {
    ensureNotNull(NotValidException.class, "categoryLike", categoryLike);
    this.categoryLike = categoryLike;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery decisionRequirementsDefinitionName(String name) {
    ensureNotNull(NotValidException.class, "name", name);
    this.name = name;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery decisionRequirementsDefinitionNameLike(String nameLike) {
    ensureNotNull(NotValidException.class, "nameLike", nameLike);
    this.nameLike = nameLike;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery decisionRequirementsDefinitionKey(String key) {
    ensureNotNull(NotValidException.class, "key", key);
    this.key = key;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery decisionRequirementsDefinitionKeyLike(String keyLike) {
    ensureNotNull(NotValidException.class, "keyLike", keyLike);
    this.keyLike = keyLike;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery deploymentId(String deploymentId) {
    ensureNotNull(NotValidException.class, "deploymentId", deploymentId);
    this.deploymentId = deploymentId;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery decisionRequirementsDefinitionVersion(Integer version) {
    ensureNotNull(NotValidException.class, "version", version);
    ensurePositive(NotValidException.class, "version", version.longValue());
    this.version = version;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery latestVersion() {
    this.latest = true;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery decisionRequirementsDefinitionResourceName(String resourceName) {
    ensureNotNull(NotValidException.class, "resourceName", resourceName);
    this.resourceName = resourceName;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery decisionRequirementsDefinitionResourceNameLike(String resourceNameLike) {
    ensureNotNull(NotValidException.class, "resourceNameLike", resourceNameLike);
    this.resourceNameLike = resourceNameLike;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery tenantIdIn(String... tenantIds) {
    ensureNotNull("tenantIds", (Object[]) tenantIds);
    this.tenantIds = tenantIds;
    isTenantIdSet = true;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery withoutTenantId() {
    isTenantIdSet = true;
    this.tenantIds = null;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery includeDecisionRequirementsDefinitionsWithoutTenantId() {
    this.includeDefinitionsWithoutTenantId  = true;
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery orderByDecisionRequirementsDefinitionCategory() {
    orderBy(DecisionRequirementsDefinitionQueryProperty.DECISION_REQUIREMENTS_DEFINITION_CATEGORY);
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery orderByDecisionRequirementsDefinitionKey() {
    orderBy(DecisionRequirementsDefinitionQueryProperty.DECISION_REQUIREMENTS_DEFINITION_KEY);
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery orderByDecisionRequirementsDefinitionId() {
    orderBy(DecisionRequirementsDefinitionQueryProperty.DECISION_REQUIREMENTS_DEFINITION_ID);
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery orderByDecisionRequirementsDefinitionVersion() {
    orderBy(DecisionRequirementsDefinitionQueryProperty.DECISION_REQUIREMENTS_DEFINITION_VERSION);
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery orderByDecisionRequirementsDefinitionName() {
    orderBy(DecisionRequirementsDefinitionQueryProperty.DECISION_REQUIREMENTS_DEFINITION_NAME);
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery orderByDeploymentId() {
    orderBy(DecisionRequirementsDefinitionQueryProperty.DEPLOYMENT_ID);
    return this;
  }

  @Override
  public DecisionRequirementsDefinitionQuery orderByTenantId() {
    return orderBy(DecisionRequirementsDefinitionQueryProperty.TENANT_ID);
  }

  //results ////////////////////////////////////////////

  @Override
  public long executeCount(CommandContext commandContext) {
    if (commandContext.getProcessEngineConfiguration().isDmnEnabled()) {
      checkQueryOk();
      return commandContext
              .getDecisionRequirementsDefinitionManager()
              .findDecisionRequirementsDefinitionCountByQueryCriteria(this);
    }
    return 0;
  }

  @Override
  public List<DecisionRequirementsDefinition> executeList(CommandContext commandContext, Page page) {
    if (commandContext.getProcessEngineConfiguration().isDmnEnabled()) {
      checkQueryOk();
      return commandContext
              .getDecisionRequirementsDefinitionManager()
              .findDecisionRequirementsDefinitionsByQueryCriteria(this, page);
    }
    return Collections.emptyList();
  }

  @Override
  public void checkQueryOk() {
    super.checkQueryOk();

    // latest() makes only sense when used with key() or keyLike()
    if (latest && ( (id != null) || (name != null) || (nameLike != null) || (version != null) || (deploymentId != null) ) ){
      throw new NotValidException("Calling latest() can only be used in combination with key(String) and keyLike(String)");
    }
  }

  // getters ////////////////////////////////////////////

  public String getId() {
    return id;
  }

  public String[] getIds() {
    return ids;
  }

  public String getCategory() {
    return category;
  }

  public String getCategoryLike() {
    return categoryLike;
  }

  public String getName() {
    return name;
  }

  public String getNameLike() {
    return nameLike;
  }

  public String getDeploymentId() {
    return deploymentId;
  }

  public String getKey() {
    return key;
  }

  public String getKeyLike() {
    return keyLike;
  }

  public String getResourceName() {
    return resourceName;
  }

  public String getResourceNameLike() {
    return resourceNameLike;
  }

  public Integer getVersion() {
    return version;
  }

  public boolean isLatest() {
    return latest;
  }
}
