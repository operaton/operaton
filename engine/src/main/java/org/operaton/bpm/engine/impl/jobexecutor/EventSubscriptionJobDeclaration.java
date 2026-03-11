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
package org.operaton.bpm.engine.impl.jobexecutor;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.bpmn.parser.EventSubscriptionDeclaration;
import org.operaton.bpm.engine.impl.jobexecutor.ProcessEventJobHandler.EventSubscriptionJobConfiguration;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.MessageEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.commons.utils.EnsureUtil;

/**
 * <p>Describes and creates jobs for handling an event asynchronously.
 * These jobs are created in the context of an {@link EventSubscriptionEntity} and are of type {@link MessageEntity}.</p>
 *
 * @author Thorben Lindhauer
 */
public class EventSubscriptionJobDeclaration extends JobDeclaration<EventSubscriptionEntity, MessageEntity> {

  @Serial private static final long serialVersionUID = 1L;

  protected EventSubscriptionDeclaration eventSubscriptionDeclaration;

  public EventSubscriptionJobDeclaration(EventSubscriptionDeclaration eventSubscriptionDeclaration) {
    super(ProcessEventJobHandler.TYPE);
    EnsureUtil.ensureNotNull("eventSubscriptionDeclaration", eventSubscriptionDeclaration);
    this.eventSubscriptionDeclaration = eventSubscriptionDeclaration;
  }


  @Override
  protected MessageEntity newJobInstance(EventSubscriptionEntity eventSubscription) {

    MessageEntity message = new MessageEntity();

    // initialize job
    message.setActivityId(eventSubscription.getActivityId());
    message.setExecutionId(eventSubscription.getExecutionId());
    message.setProcessInstanceId(eventSubscription.getProcessInstanceId());

    ProcessDefinitionEntity processDefinition = eventSubscription.getProcessDefinition();

    if (processDefinition != null) {
      message.setProcessDefinitionId(processDefinition.getId());
      message.setProcessDefinitionKey(processDefinition.getKey());
    }

    return message;
  }

  public String getEventType() {
    return eventSubscriptionDeclaration.getEventType();
  }

  public String getEventName() {
    return eventSubscriptionDeclaration.getUnresolvedEventName();
  }

  @Override
  public String getActivityId() {
    return eventSubscriptionDeclaration.getActivityId();
  }

  @Override
  protected ExecutionEntity resolveExecution(EventSubscriptionEntity context) {
    return context.getExecution();
  }

  @Override
  protected JobHandlerConfiguration resolveJobHandlerConfiguration(EventSubscriptionEntity context) {
    return new EventSubscriptionJobConfiguration(context.getId());
  }

  @SuppressWarnings("unchecked")
  public static List<EventSubscriptionJobDeclaration> getDeclarationsForActivity(PvmActivity activity) {
    Object result = activity.getProperty(BpmnParse.PROPERTYNAME_EVENT_SUBSCRIPTION_JOB_DECLARATION);
    if (result != null) {
      return (List<EventSubscriptionJobDeclaration>) result;
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * Assumes that an activity has at most one declaration of a certain eventType.
   */
  public static EventSubscriptionJobDeclaration findDeclarationForSubscription(EventSubscriptionEntity eventSubscription) {
    List<EventSubscriptionJobDeclaration> declarations = getDeclarationsForActivity(eventSubscription.getActivity());

    for (EventSubscriptionJobDeclaration declaration : declarations) {
      if (declaration.getEventType().equals(eventSubscription.getEventType())) {
        return declaration;
      }
    }

    return null;
  }


}
