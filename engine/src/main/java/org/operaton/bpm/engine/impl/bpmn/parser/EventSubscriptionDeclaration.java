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
package org.operaton.bpm.engine.impl.bpmn.parser;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.operaton.bpm.engine.delegate.BaseDelegateExecution;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.operaton.bpm.engine.impl.core.model.CallableElement;
import org.operaton.bpm.engine.impl.el.Expression;
import org.operaton.bpm.engine.impl.el.StartProcessVariableScope;
import org.operaton.bpm.engine.impl.event.EventType;
import org.operaton.bpm.engine.impl.jobexecutor.EventSubscriptionJobDeclaration;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.pvm.PvmScope;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.LegacyBehavior;

/**
 * @author Daniel Meyer
 * @author Falko Menge
 * @author Danny Gräf
 */
public class EventSubscriptionDeclaration implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  protected final EventType eventType;
  protected final Expression eventName;
  protected final CallableElement eventPayload;

  protected boolean async;
  protected String activityId;
  protected String eventScopeActivityId;
  protected boolean isStartEvent;

  protected EventSubscriptionJobDeclaration jobDeclaration;

  public EventSubscriptionDeclaration(Expression eventExpression, EventType eventType) {
    this.eventName = eventExpression;
    this.eventType = eventType;
    this.eventPayload = null;
  }

  public EventSubscriptionDeclaration(Expression eventExpression, EventType eventType, CallableElement eventPayload) {
    this.eventType = eventType;
    this.eventName = eventExpression;
    this.eventPayload = eventPayload;
  }

  public static Map<String, EventSubscriptionDeclaration> getDeclarationsForScope(PvmScope scope) {
    if (scope == null) {
      return Collections.emptyMap();
    }

    return scope.getProperties().get(BpmnProperties.EVENT_SUBSCRIPTION_DECLARATIONS);
  }

  /**
   * Returns the name of the event without evaluating the possible expression that it might contain.
   */
  public String getUnresolvedEventName() {
      return eventName.getExpressionText();
  }

  public boolean hasEventName() {
    return !( eventName == null || "".equalsIgnoreCase(getUnresolvedEventName().trim()) );
  }

  public boolean isEventNameLiteralText() {
    return eventName.isLiteralText();
  }

  public boolean isAsync() {
    return async;
  }

  public void setAsync(boolean async) {
    this.async = async;
  }

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public String getEventScopeActivityId() {
    return eventScopeActivityId;
  }

  public void setEventScopeActivityId(String eventScopeActivityId) {
    this.eventScopeActivityId = eventScopeActivityId;
  }

  public boolean isStartEvent() {
    return isStartEvent;
  }

  public void setStartEvent(boolean isStartEvent) {
    this.isStartEvent = isStartEvent;
  }

  public String getEventType() {
    return eventType.name();
  }

  public CallableElement getEventPayload() {
    return eventPayload;
  }

  public void setJobDeclaration(EventSubscriptionJobDeclaration jobDeclaration) {
    this.jobDeclaration = jobDeclaration;
  }

  public EventSubscriptionEntity createSubscriptionForStartEvent(ProcessDefinitionEntity processDefinition) {
    EventSubscriptionEntity eventSubscriptionEntity = new EventSubscriptionEntity(eventType);

    VariableScope scopeForExpression = StartProcessVariableScope.getSharedInstance();
    String event = resolveExpressionOfEventName(scopeForExpression);
    eventSubscriptionEntity.setEventName(event);
    eventSubscriptionEntity.setActivityId(activityId);
    eventSubscriptionEntity.setConfiguration(processDefinition.getId());
    eventSubscriptionEntity.setTenantId(processDefinition.getTenantId());
    return eventSubscriptionEntity;
  }

  /**
   * Creates and inserts a subscription entity depending on the message type of this declaration.
   */
  public EventSubscriptionEntity createSubscriptionForExecution(ExecutionEntity execution) {
    EventSubscriptionEntity eventSubscriptionEntity = new EventSubscriptionEntity(execution, eventType);

    String event = resolveExpressionOfEventName(execution);
    eventSubscriptionEntity.setEventName(event);
    if (activityId != null) {
      ActivityImpl activity = execution.getProcessDefinition().findActivity(activityId);
      eventSubscriptionEntity.setActivity(activity);
    }

    eventSubscriptionEntity.insert();
    LegacyBehavior.removeLegacySubscriptionOnParent(execution, eventSubscriptionEntity);

    return eventSubscriptionEntity;
  }

  /**
   * Resolves the event name within the given scope.
   */
  public String resolveExpressionOfEventName(VariableScope scope) {
    if (isExpressionAvailable()) {
      if(scope instanceof BaseDelegateExecution execution) {
        // the variable scope execution is also the current context execution
        // during expression evaluation the current context is updated with the scope execution
        return (String) eventName.getValue(scope, execution);
      } else {
        return (String) eventName.getValue(scope);
      }
    } else {
      return null;
    }
  }

  protected boolean isExpressionAvailable() {
    return eventName != null;
  }

  public void updateSubscription(EventSubscriptionEntity eventSubscription) {
    String event = resolveExpressionOfEventName(eventSubscription.getExecution());
    eventSubscription.setEventName(event);
    eventSubscription.setActivityId(activityId);
  }

}
