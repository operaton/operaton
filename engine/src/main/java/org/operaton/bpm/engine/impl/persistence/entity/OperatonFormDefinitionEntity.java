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
package org.operaton.bpm.engine.impl.persistence.entity;

import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.HasDbRevision;
import org.operaton.bpm.engine.impl.repository.ResourceDefinitionEntity;
import org.operaton.bpm.engine.repository.OperatonFormDefinition;

public class OperatonFormDefinitionEntity implements OperatonFormDefinition,
    ResourceDefinitionEntity<OperatonFormDefinitionEntity>, DbEntity, HasDbRevision {

  protected String id;
  protected int revision = 1;
  protected String key;
  protected int version;
  protected String deploymentId;
  protected String resourceName;
  protected String tenantId;

  public OperatonFormDefinitionEntity(String key, String deploymentId, String resourceName, String tenantId) {
    this.key = key;
    this.deploymentId = deploymentId;
    this.resourceName = resourceName;
    this.tenantId = tenantId;
  }

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
  public String getTenantId() {
    return tenantId;
  }

  @Override
  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public String getCategory() {
    throw new UnsupportedOperationException();
  }

  @Override
  public OperatonFormDefinitionEntity getPreviousDefinition() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setCategory(String category) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDiagramResourceName() {
    throw new UnsupportedOperationException("deployment of diagrams not supported for Operaton Forms");
  }

  @Override
  public void setDiagramResourceName(String diagramResourceName) {
    throw new UnsupportedOperationException("deployment of diagrams not supported for Operaton Forms");
  }

  @Override
  public Integer getHistoryTimeToLive() {
    throw new UnsupportedOperationException("history time to live not supported for Operaton Forms");
  }

  @Override
  public void setHistoryTimeToLive(Integer historyTimeToLive) {
    throw new UnsupportedOperationException("history time to live not supported for Operaton Forms");
  }

  @Override
  public Object getPersistentState() {
    // properties of this entity are immutable
    return OperatonFormDefinitionEntity.class;
  }

  @Override
  public void updateModifiableFieldsFromEntity(OperatonFormDefinitionEntity updatingDefinition) {
    throw new UnsupportedOperationException("properties of Operaton Form Definitions are immutable");
  }

  @Override
  public String getName() {
    throw new UnsupportedOperationException("name property not supported for Operaton Forms");
  }

  @Override
  public void setName(String name) {
    throw new UnsupportedOperationException("name property not supported for Operaton Forms");
  }

}
