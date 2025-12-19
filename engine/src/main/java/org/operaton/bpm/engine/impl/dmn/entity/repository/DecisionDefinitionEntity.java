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
package org.operaton.bpm.engine.impl.dmn.entity.repository;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.dmn.engine.impl.DmnDecisionImpl;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.db.HasDbRevision;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.repository.ResourceDefinitionEntity;
import org.operaton.bpm.engine.repository.DecisionDefinition;

public class DecisionDefinitionEntity extends DmnDecisionImpl implements DecisionDefinition, ResourceDefinitionEntity<DecisionDefinitionEntity>, DbEntity, HasDbRevision, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  protected String id;
  protected int revision = 1;
  protected String category;
  protected int version;
  protected String deploymentId;
  protected String resourceName;
  protected String diagramResourceName;
  protected String tenantId;
  protected String decisionRequirementsDefinitionId;
  protected String decisionRequirementsDefinitionKey;

  // firstVersion is true, when version == 1 or when
  // this definition does not have any previous definitions
  protected boolean firstVersion;
  protected String previousDecisionDefinitionId;

  protected Integer historyTimeToLive;
  protected String versionTag;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public int getRevision() {
    return revision;
  }

  @Override
  public void setRevision(int revision) {
    this.revision = revision;
  }

  @Override
  public int getRevisionNext() {
    return revision + 1;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getCategory() {
    return category;
  }

  @Override
  public void setCategory(String category) {
    this.category = category;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public void setKey(String key) {
    this.key = key;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public void setVersion(int version) {
    this.version = version;
    this.firstVersion = this.version == 1;
  }

  @Override
  public String getDeploymentId() {
    return deploymentId;
  }

  @Override
  public void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  @Override
  public String getResourceName() {
    return resourceName;
  }

  @Override
  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  @Override
  public String getDiagramResourceName() {
    return diagramResourceName;
  }

  @Override
  public void setDiagramResourceName(String diagramResourceName) {
    this.diagramResourceName = diagramResourceName;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  @Override
  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public String getDecisionRequirementsDefinitionId() {
    return decisionRequirementsDefinitionId;
  }

  public void setDecisionRequirementsDefinitionId(String decisionRequirementsDefinitionId) {
    this.decisionRequirementsDefinitionId = decisionRequirementsDefinitionId;
  }

  @Override
  public String getDecisionRequirementsDefinitionKey() {
    return decisionRequirementsDefinitionKey;
  }

  public void setDecisionRequirementsDefinitionKey(String decisionRequirementsDefinitionKey) {
    this.decisionRequirementsDefinitionKey = decisionRequirementsDefinitionKey;
  }

  @Override
  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<>();
    persistentState.put("historyTimeToLive", this.historyTimeToLive);
    return persistentState;
  }

  /**
   * Updates all modifiable fields from another decision definition entity.
   *
   * @param updatingDecisionDefinition
   */
  @Override
  public void updateModifiableFieldsFromEntity(DecisionDefinitionEntity updatingDecisionDefinition) {
    if (this.key.equals(updatingDecisionDefinition.key) && this.deploymentId.equals(updatingDecisionDefinition.deploymentId)) {
      this.revision = updatingDecisionDefinition.revision;
      this.historyTimeToLive = updatingDecisionDefinition.historyTimeToLive;
    } else {
      LOG.logUpdateUnrelatedDecisionDefinitionEntity(this.key, updatingDecisionDefinition.key, this.deploymentId, updatingDecisionDefinition.deploymentId);
    }
  }

  // previous decision definition //////////////////////////////////////////////

  @Override
  public DecisionDefinitionEntity getPreviousDefinition() {
    DecisionDefinitionEntity previousDecisionDefinition = null;

    String previousDecisionDefId = getPreviousDecisionDefinitionId();
    if (previousDecisionDefId != null) {

      previousDecisionDefinition = loadDecisionDefinition(previousDecisionDefId);

      if (previousDecisionDefinition == null) {
        resetPreviousDecisionDefinitionId();
        previousDecisionDefId = getPreviousDecisionDefinitionId();

        if (previousDecisionDefId != null) {
          previousDecisionDefinition = loadDecisionDefinition(previousDecisionDefId);
        }
      }
    }

    return previousDecisionDefinition;
  }

  /**
   * Returns the cached version if exists; does not update the entity from the database in that case
   */
  protected DecisionDefinitionEntity loadDecisionDefinition(String decisionDefinitionId) {
    ProcessEngineConfigurationImpl configuration = Context.getProcessEngineConfiguration();
    DeploymentCache deploymentCache = configuration.getDeploymentCache();

    DecisionDefinitionEntity decisionDefinition = deploymentCache.findDecisionDefinitionFromCache(decisionDefinitionId);

    if (decisionDefinition == null) {
      CommandContext commandContext = Context.getCommandContext();
      DecisionDefinitionManager decisionDefinitionManager = commandContext.getDecisionDefinitionManager();
      decisionDefinition = decisionDefinitionManager.findDecisionDefinitionById(decisionDefinitionId);

      if (decisionDefinition != null) {
        decisionDefinition = deploymentCache.resolveDecisionDefinition(decisionDefinition);
      }
    }

    return decisionDefinition;

  }

  public String getPreviousDecisionDefinitionId() {
    ensurePreviousDecisionDefinitionIdInitialized();
    return previousDecisionDefinitionId;
  }

  public void setPreviousDecisionDefinitionId(String previousDecisionDefinitionId) {
    this.previousDecisionDefinitionId = previousDecisionDefinitionId;
  }

  protected void resetPreviousDecisionDefinitionId() {
    previousDecisionDefinitionId = null;
    ensurePreviousDecisionDefinitionIdInitialized();
  }

  protected void ensurePreviousDecisionDefinitionIdInitialized() {
    if (previousDecisionDefinitionId == null && !firstVersion) {
      previousDecisionDefinitionId = Context
          .getCommandContext()
          .getDecisionDefinitionManager()
          .findPreviousDecisionDefinitionId(key, version, tenantId);

      if (previousDecisionDefinitionId == null) {
        firstVersion = true;
      }
    }
  }

  @Override
  public Integer getHistoryTimeToLive() {
    return historyTimeToLive;
  }

  @Override
  public void setHistoryTimeToLive(Integer historyTimeToLive) {
    this.historyTimeToLive = historyTimeToLive;
  }

  @Override
  public String getVersionTag() {
    return versionTag;
  }

  public void setVersionTag(String versionTag) {
    this.versionTag = versionTag;
  }

  @Override
  public String toString() {
    return "DecisionDefinitionEntity{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", category='" + category + '\'' +
      ", key='" + key + '\'' +
      ", version=%s, versionTag=%s, decisionRequirementsDefinitionId='%s'".formatted(version, versionTag, decisionRequirementsDefinitionId) +
      ", decisionRequirementsDefinitionKey='" + decisionRequirementsDefinitionKey + '\'' +
      ", deploymentId='" + deploymentId + '\'' +
      ", tenantId='" + tenantId + '\'' +
      ", historyTimeToLive=" + historyTimeToLive +
      '}';
  }

}
