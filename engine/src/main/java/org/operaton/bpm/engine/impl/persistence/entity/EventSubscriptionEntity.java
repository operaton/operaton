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
import java.util.*;

import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.HasDbReferences;
import org.operaton.bpm.engine.impl.db.HasDbRevision;
import org.operaton.bpm.engine.impl.event.EventHandler;
import org.operaton.bpm.engine.impl.event.EventType;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.jobexecutor.EventSubscriptionJobDeclaration;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.EventSubscription;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Daniel Meyer
 */
public class EventSubscriptionEntity implements EventSubscription, DbEntity, HasDbRevision, HasDbReferences, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  // persistent state ///////////////////////////
  protected String id;
  protected int revision = 1;
  protected String eventType;
  protected String eventName;

  protected String executionId;
  protected String processInstanceId;
  protected String activityId;
  protected String configuration;
  protected Date created;
  protected String tenantId;

  // runtime state /////////////////////////////
  protected ExecutionEntity execution;
  protected ActivityImpl activity;
  protected EventSubscriptionJobDeclaration jobDeclaration;

  // ///////////////////////////////////////////

  //only for mybatis
  public EventSubscriptionEntity() {
  }

  public EventSubscriptionEntity(EventType eventType) {
    this.created = ClockUtil.getCurrentTime();
    this.eventType = eventType.name();
  }

  public EventSubscriptionEntity(ExecutionEntity executionEntity, EventType eventType) {
    this(eventType);
    setExecution(executionEntity);
    setActivity(execution.getActivity());
    this.processInstanceId = executionEntity.getProcessInstanceId();
    this.tenantId = executionEntity.getTenantId();
  }

  // processing /////////////////////////////
  public void eventReceived(Object payload, boolean processASync) {
    eventReceived(payload, null, null, null, processASync);
  }

  public void eventReceived(Object payload, Object payloadLocal, Object payloadToTriggeredScope, String businessKey, boolean processASync) {
    if(processASync) {
      scheduleEventAsync(payload, payloadLocal, payloadToTriggeredScope, businessKey);
    } else {
      processEventSync(payload, payloadLocal, payloadToTriggeredScope, businessKey);
    }
  }

  protected void processEventSync(Object payload) {
    this.processEventSync(payload, null, null, null);
  }

  protected void processEventSync(Object payload, Object payloadLocal, Object payloadToTriggeredScope, String businessKey) {
    EventHandler eventHandler = Context.getProcessEngineConfiguration().getEventHandler(eventType);
    ensureNotNull("Could not find eventhandler for event of type '%s'".formatted(eventType), "eventHandler", eventHandler);
    eventHandler.handleEvent(this, payload, payloadLocal, payloadToTriggeredScope, businessKey, Context.getCommandContext());
  }

  protected void scheduleEventAsync(Object payload, Object payloadLocal, Object payloadToTriggeredScope, String businessKey) {

    EventSubscriptionJobDeclaration asyncDeclaration = getJobDeclaration();

    if (asyncDeclaration == null) {
      // fallback to sync if we couldn't find a job declaration
      processEventSync(payload, payloadLocal, payloadToTriggeredScope, businessKey);
    }
    else {
      MessageEntity message = asyncDeclaration.createJobInstance(this);
      CommandContext commandContext = Context.getCommandContext();
      commandContext.getJobManager().send(message);
    }
  }

  // persistence behavior /////////////////////

  public void delete() {
    Context.getCommandContext()
      .getEventSubscriptionManager()
      .deleteEventSubscription(this);
    removeFromExecution();
  }

  public void insert() {
    Context.getCommandContext()
      .getEventSubscriptionManager()
      .insert(this);
    addToExecution();
  }


  public static EventSubscriptionEntity createAndInsert(ExecutionEntity executionEntity, EventType eventType, ActivityImpl activity) {
    return createAndInsert(executionEntity, eventType, activity, null);
  }

  public static EventSubscriptionEntity createAndInsert(ExecutionEntity executionEntity, EventType eventType, ActivityImpl activity, String configuration) {
    EventSubscriptionEntity eventSubscription = new EventSubscriptionEntity(executionEntity, eventType);
    eventSubscription.setActivity(activity);
    eventSubscription.setTenantId(executionEntity.getTenantId());
    eventSubscription.setConfiguration(configuration);
    eventSubscription.insert();
    return eventSubscription;
  }

 // referential integrity -> ExecutionEntity ////////////////////////////////////

  protected void addToExecution() {
    // add reference in execution
    ExecutionEntity exec = getExecution();
    if(exec != null) {
      exec.addEventSubscription(this);
    }
  }

  protected void removeFromExecution() {
    // remove reference in execution
    ExecutionEntity exec = getExecution();
    if(exec != null) {
      exec.removeEventSubscription(this);
    }
  }

  @Override
  public Object getPersistentState() {
    HashMap<String, Object> persistentState = new HashMap<>();
    persistentState.put("executionId", executionId);
    persistentState.put("configuration", configuration);
    persistentState.put("activityId", activityId);
    persistentState.put("eventName", eventName);
    return persistentState;
  }

  // getters & setters ////////////////////////////

  public ExecutionEntity getExecution() {
    if(execution == null && executionId != null) {
      execution = Context.getCommandContext()
              .getExecutionManager()
              .findExecutionById(executionId);
    }
    return execution;
  }

  public void setExecution(ExecutionEntity execution) {
    if(execution != null) {
      this.execution = execution;
      this.executionId = execution.getId();
      addToExecution();
    }
    else {
      removeFromExecution();
      this.executionId = null;
      this.execution = null;
    }
  }

  public ActivityImpl getActivity() {
    if(activity == null && activityId != null) {
      ProcessDefinitionImpl processDefinition = getProcessDefinition();
      activity = processDefinition.findActivity(activityId);
    }
    return activity;
  }

  public ProcessDefinitionEntity getProcessDefinition() {
    if (executionId != null) {
      return getExecution().getProcessDefinition();
    }
    else {
      // this assumes that start event subscriptions have the process definition id
      // as their configuration (which holds for message and signal start events)
      String processDefinitionId = getConfiguration();
      return Context.getProcessEngineConfiguration()
        .getDeploymentCache()
        .findDeployedProcessDefinitionById(processDefinitionId);
    }
  }

  public void setActivity(ActivityImpl activity) {
    this.activity = activity;
    if(activity != null) {
      this.activityId = activity.getId();
    }
  }

  public EventSubscriptionJobDeclaration getJobDeclaration() {
    if (jobDeclaration == null) {
      jobDeclaration = EventSubscriptionJobDeclaration.findDeclarationForSubscription(this);
    }

    return jobDeclaration;
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
    return revision +1;
  }

  public boolean isSubscriptionForEventType(EventType eventType) {
    return this.eventType.equals(eventType.name());
  }

  @Override
  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  @Override
  public String getEventName() {
    return this.eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  @Override
  public String getExecutionId() {
    return executionId;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  @Override
  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public String getConfiguration() {
    return configuration;
  }

  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }

  @Override
  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
    this.activity = null;
  }

  @Override
  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    return prime * result + (id == null ? 0 : id.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    EventSubscriptionEntity other = (EventSubscriptionEntity) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

  @Override
  public Map<String, Class<?>> getReferencedEntitiesIdAndClass() {
    Map<String, Class<?>> referenceIdAndClass = new HashMap<>();

    if (executionId != null) {
      referenceIdAndClass.put(executionId, ExecutionEntity.class);
    }

    return referenceIdAndClass;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
           + "[id=" + id
           + ", eventType=" + eventType
           + ", eventName=" + eventName
           + ", executionId=" + executionId
           + ", processInstanceId=" + processInstanceId
           + ", activityId=" + activityId
           + ", tenantId=" + tenantId
           + ", configuration=" + configuration
           + ", revision=" + revision
           + ", created=" + created
           + "]";
  }
}
