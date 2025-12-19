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

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.HasDbRevision;
import org.operaton.bpm.engine.impl.db.HistoricEntity;
import org.operaton.bpm.engine.task.Attachment;


/**
 * @author Tom Baeyens
 */
public class AttachmentEntity implements Attachment, DbEntity, HasDbRevision, HistoricEntity, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  protected String id;
  protected int revision;
  protected String name;
  protected String description;
  protected String type;
  protected String taskId;
  protected String processInstanceId;
  protected String url;
  protected String contentId;
  protected ByteArrayEntity content;
  protected String tenantId;
  protected Date createTime;
  protected String rootProcessInstanceId;
  protected Date removalTime;

  @Override
  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<>();
    persistentState.put("name", name);
    persistentState.put("description", description);
    return persistentState;
  }

  @Override
  public int getRevisionNext() {
    return revision+1;
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
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  @Override
  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  @Override
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getContentId() {
    return contentId;
  }

  public void setContentId(String contentId) {
    this.contentId = contentId;
  }

  public ByteArrayEntity getContent() {
    return content;
  }

  public void setContent(ByteArrayEntity content) {
    this.content = content;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  @Override
  public String getRootProcessInstanceId() {
    return rootProcessInstanceId;
  }

  @Override
  public Date getRemovalTime() {
    return removalTime;
  }

  public void setRemovalTime(Date removalTime) {
    this.removalTime = removalTime;
  }

  public void setRootProcessInstanceId(String rootProcessInstanceId) {
    this.rootProcessInstanceId = rootProcessInstanceId;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
           + "[id=%s, revision=%s, name=%s, description=%s, type=%s, taskId=%s, processInstanceId=%s, rootProcessInstanceId=".formatted(id, revision, name, description).formatted(type, taskId, processInstanceId) + rootProcessInstanceId
           + ", removalTime=%s, url=%s, contentId=%s, content=%s, tenantId=%s, createTime=%s]".formatted(removalTime, url, contentId, content).formatted(tenantId, createTime);
  }
}
