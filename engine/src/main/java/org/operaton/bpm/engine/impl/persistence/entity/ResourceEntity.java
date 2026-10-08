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

import java.util.Date;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.repository.Resource;

/**
 * @author Tom Baeyens
 */
public @NullMarked class ResourceEntity implements DbEntity, Resource {

  protected @Nullable String id;
  protected @Nullable String name;
  protected byte@Nullable[] bytes;
  protected @Nullable String deploymentId;
  protected boolean generated;
  protected @Nullable String tenantId;
  protected @Nullable Integer type;
  protected @Nullable Date createTime;

  @Override
  public @Nullable String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public @Nullable String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  @Override
  public byte@Nullable[] getBytes() {
    return bytes;
  }

  public void setBytes(byte@Nullable[] bytes) {
    this.bytes = bytes;
  }

  @Override
  public @Nullable String getDeploymentId() {
    return deploymentId;
  }

  public void setDeploymentId(@Nullable String deploymentId) {
    this.deploymentId = deploymentId;
  }

  @Override
  public Object getPersistentState() {
    return ResourceEntity.class;
  }

  public void setGenerated(boolean generated) {
    this.generated = generated;
  }

  /**
   * Indicated whether or not the resource has been generated while deploying rather than
   * being actual part of the deployment.
   */
  public boolean isGenerated() {
    return generated;
  }

  public @Nullable String getTenantId() {
    return tenantId;
  }

  public void setTenantId(@Nullable String tenantId) {
    this.tenantId = tenantId;
  }

  public @Nullable Integer getType() {
    return type;
  }

  public void setType(@Nullable Integer type) {
    this.type = type;
  }

  public @Nullable Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(@Nullable Date createTime) {
    this.createTime = createTime;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
           + "[id=" + id
           + ", name=" + name
           + ", deploymentId=" + deploymentId
           + ", generated=" + generated
           + ", tenantId=" + tenantId
           + ", type=" + type
           + ", createTime=" + createTime
           + "]";
  }

}
