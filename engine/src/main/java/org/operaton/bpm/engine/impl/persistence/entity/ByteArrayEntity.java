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
import org.operaton.bpm.engine.impl.db.HasDbRevision;
import org.operaton.bpm.engine.repository.ResourceType;

/**
 * @author Tom Baeyens
 */
@NullMarked
public class ByteArrayEntity implements DbEntity, HasDbRevision {

  private static final Object PERSISTENTSTATE_NULL = new Object();

  protected @Nullable String id;
  protected int revision;
  protected @Nullable String name;
  protected byte@Nullable[] bytes;
  protected @Nullable String deploymentId;
  protected @Nullable String tenantId;
  protected @Nullable Integer type;
  protected @Nullable Date createTime;
  protected @Nullable String rootProcessInstanceId;
  protected @Nullable Date removalTime;

  public ByteArrayEntity() {
  }

  public ByteArrayEntity(String name, byte[] bytes, ResourceType type, String rootProcessInstanceId, Date removalTime) {
    this(name, bytes, type);
    this.rootProcessInstanceId = rootProcessInstanceId;
    this.removalTime = removalTime;
  }

  public ByteArrayEntity(String name, byte[] bytes, ResourceType type) {
    this(name, bytes);
    this.type = type.getValue();
  }

  public ByteArrayEntity(String name, byte[] bytes) {
    this.name = name;
    this.bytes = bytes;
  }

  public ByteArrayEntity(byte[] bytes, ResourceType type) {
    this.bytes = bytes;
    this.type = type.getValue();
  }

  public byte@Nullable[] getBytes() {
    return bytes;
  }

  @Override
  public Object getPersistentState() {
    return bytes != null ? bytes : PERSISTENTSTATE_NULL;
  }

  @Override
  public int getRevisionNext() {
    return revision+1;
  }

  // getters and setters //////////////////////////////////////////////////////

  @Override
  public @Nullable String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public @Nullable String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public @Nullable String getDeploymentId() {
    return deploymentId;
  }

  public void setDeploymentId(@Nullable String deploymentId) {
    this.deploymentId = deploymentId;
  }

  public void setBytes(byte@Nullable[] bytes) {
    this.bytes = bytes;
  }

  @Override
  public int getRevision() {
    return revision;
  }

  @Override
  public void setRevision(int revision) {
    this.revision = revision;
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

  public void setType(Integer type) {
    this.type = type;
  }

  public @Nullable Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(@Nullable Date createTime) {
    this.createTime = createTime;
  }

  public @Nullable String getRootProcessInstanceId() {
    return rootProcessInstanceId;
  }

  public void setRootProcessInstanceId(@Nullable String rootProcessInstanceId) {
    this.rootProcessInstanceId = rootProcessInstanceId;
  }

  public @Nullable Date getRemovalTime() {
    return removalTime;
  }

  public void setRemovalTime(@Nullable Date removalTime) {
    this.removalTime = removalTime;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
           + "[id=" + id
           + ", revision=" + revision
           + ", name=" + name
           + ", deploymentId=" + deploymentId
           + ", tenantId=" + tenantId
           + ", type=" + type
           + ", createTime=" + createTime
           + ", rootProcessInstanceId=" + rootProcessInstanceId
           + ", removalTime=" + removalTime
           + "]";
  }

}
