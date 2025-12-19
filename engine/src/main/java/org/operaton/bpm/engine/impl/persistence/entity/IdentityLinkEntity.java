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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.db.HasDbReferences;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.history.event.HistoryEventProcessor;
import org.operaton.bpm.engine.impl.history.event.HistoryEventType;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.history.producer.HistoryEventProducer;
import org.operaton.bpm.engine.task.IdentityLink;


/**
 * @author Joram Barrez
 * @author Deivarayan Azhagappan
 */
public class IdentityLinkEntity implements Serializable, IdentityLink, DbEntity, HasDbReferences {

  @Serial private static final long serialVersionUID = 1L;
  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  protected String id;

  protected String type;

  protected String userId;

  protected String groupId;

  protected String taskId;

  protected String processDefId;

  protected String tenantId;

  protected TaskEntity task;

  protected ProcessDefinitionEntity processDef;

  @Override
  public Object getPersistentState() {
    return this.type;
  }

  public static IdentityLinkEntity createAndInsert() {
    IdentityLinkEntity identityLinkEntity = new IdentityLinkEntity();
    identityLinkEntity.insert();
    return identityLinkEntity;
  }

  public static IdentityLinkEntity newIdentityLink() {
    return new IdentityLinkEntity();
  }

  public void insert() {
    Context
      .getCommandContext()
      .getDbEntityManager()
      .insert(this);
    fireHistoricIdentityLinkEvent(HistoryEventTypes.IDENTITY_LINK_ADD);
  }

  public void delete() {
    delete(true);
  }

  public void delete(boolean withHistory) {
    Context
        .getCommandContext()
        .getDbEntityManager()
        .delete(this);
    if (withHistory) {
      fireHistoricIdentityLinkEvent(HistoryEventTypes.IDENTITY_LINK_DELETE);
    }
  }

  public boolean isUser() {
    return userId != null;
  }

  public boolean isGroup() {
    return groupId != null;
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
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    if (this.groupId != null && userId != null) {
      throw LOG.taskIsAlreadyAssignedException("userId", "groupId");
    }
    this.userId = userId;
  }

  @Override
  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    if (this.userId != null && groupId != null) {
      throw LOG.taskIsAlreadyAssignedException("groupId", "userId");
    }
    this.groupId = groupId;
  }

  @Override
  public String getTaskId() {
    return taskId;
  }

  void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  @Override
  public String getProcessDefId() {
    return processDefId;
  }

  public void setProcessDefId(String processDefId) {
    this.processDefId = processDefId;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public TaskEntity getTask() {
    if ( (task==null) && (taskId!=null) ) {
      this.task = Context
        .getCommandContext()
        .getTaskManager()
        .findTaskById(taskId);
    }
    return task;
  }

  public void setTask(TaskEntity task) {
    this.task = task;
    this.taskId = task.getId();
  }

  public ProcessDefinitionEntity getProcessDef() {
    if ((processDef == null) && (processDefId != null)) {
      this.processDef = Context
              .getCommandContext()
              .getProcessDefinitionManager()
              .findLatestProcessDefinitionById(processDefId);
    }
    return processDef;
  }

  public void setProcessDef(ProcessDefinitionEntity processDef) {
    this.processDef = processDef;
    this.processDefId = processDef.getId();
  }

  public void fireHistoricIdentityLinkEvent(final HistoryEventType eventType) {
    ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();

    HistoryLevel historyLevel = processEngineConfiguration.getHistoryLevel();
    if(historyLevel.isHistoryEventProduced(eventType, this)) {

      HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
        @Override
        public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
          HistoryEvent event = null;
          if (HistoryEvent.IDENTITY_LINK_ADD.equals(eventType.getEventName())) {
            event = producer.createHistoricIdentityLinkAddEvent(IdentityLinkEntity.this);
          } else if (HistoryEvent.IDENTITY_LINK_DELETE.equals(eventType.getEventName())) {
            event = producer.createHistoricIdentityLinkDeleteEvent(IdentityLinkEntity.this);
          }
          return event;
        }
      });

    }
  }

  @Override
  public Set<String> getReferencedEntityIds() {
    return new HashSet<>();
  }

  @Override
  public Map<String, Class> getReferencedEntitiesIdAndClass() {
    Map<String, Class> referenceIdAndClass = new HashMap<>();

    if (processDefId != null) {
      referenceIdAndClass.put(processDefId, ProcessDefinitionEntity.class);
    }
    if (taskId != null) {
      referenceIdAndClass.put(taskId, TaskEntity.class);
    }

    return referenceIdAndClass;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
           + "[id=%s, type=%s, userId=%s, groupId=%s, taskId=%s, processDefId=%s, task=%s, processDef=".formatted(id, type, userId, groupId).formatted(taskId, processDefId, task) + processDef
           + ", tenantId=%s]".formatted(tenantId);
  }
}
