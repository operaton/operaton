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
package org.operaton.bpm.client.spring.impl.subscription;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import org.operaton.bpm.client.ExternalTaskClient;
import org.operaton.bpm.client.spring.SpringTopicSubscription;
import org.operaton.bpm.client.spring.event.SubscriptionInitializedEvent;
import org.operaton.bpm.client.spring.impl.subscription.util.SubscriptionLoggerUtil;
import org.operaton.bpm.client.spring.impl.util.LoggerUtil;
import org.operaton.bpm.client.task.ExternalTaskHandler;
import org.operaton.bpm.client.topic.TopicSubscription;
import org.operaton.bpm.client.topic.TopicSubscriptionBuilder;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;

public class SpringTopicSubscriptionImpl
  implements SpringTopicSubscription, InitializingBean {

  protected static final SubscriptionLoggerUtil LOG = LoggerUtil.SUBSCRIPTION_LOGGER;

  protected SubscriptionConfiguration subscriptionConfiguration;
  protected ExternalTaskHandler externalTaskHandler;

  protected TopicSubscriptionBuilder topicSubscriptionBuilder;
  protected TopicSubscription topicSubscription;

  protected ExternalTaskClient client;

  protected ApplicationEventPublisher applicationEventPublisher;

  public SpringTopicSubscriptionImpl(ExternalTaskClient client, ApplicationEventPublisher applicationEventPublisher) {
    this.client = client;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  protected Predicate<ApplicationEvent> isEventThatCanStartSubscription() {
    return ContextRefreshedEvent.class::isInstance;
  }

  @EventListener
  public void start(ApplicationEvent event) {
    if (isEventThatCanStartSubscription().test(event)) {
      initialize();
    }
  }

  public void initialize() {
    String topicName = subscriptionConfiguration.getTopicName();
    topicSubscriptionBuilder = client.subscribe(topicName)
        .handler(externalTaskHandler);

    ofNullable(subscriptionConfiguration.getVariableNames())
        .map(this::toArray)
        .ifPresent(topicSubscriptionBuilder::variables);

    ofNullable(subscriptionConfiguration.getLockDuration())
        .ifPresent(topicSubscriptionBuilder::lockDuration);

    if (TRUE.equals(subscriptionConfiguration.getLocalVariables())) {
      topicSubscriptionBuilder.localVariables(true);
    }

    ofNullable(subscriptionConfiguration.getBusinessKey())
        .ifPresent(topicSubscriptionBuilder::businessKey);

    ofNullable(subscriptionConfiguration.getProcessDefinitionId())
        .ifPresent(topicSubscriptionBuilder::processDefinitionId);

    ofNullable(subscriptionConfiguration.getProcessDefinitionIdIn())
        .map(this::toArray)
        .ifPresent(topicSubscriptionBuilder::processDefinitionIdIn);

    ofNullable(subscriptionConfiguration.getProcessDefinitionKey())
        .ifPresent(topicSubscriptionBuilder::processDefinitionKey);

    ofNullable(subscriptionConfiguration.getProcessDefinitionKeyIn())
        .map(this::toArray)
        .ifPresent(topicSubscriptionBuilder::processDefinitionKeyIn);

    ofNullable(subscriptionConfiguration.getProcessDefinitionVersionTag())
        .ifPresent(topicSubscriptionBuilder::processDefinitionVersionTag);

    ofNullable(subscriptionConfiguration.getProcessVariables())
        .ifPresent(topicSubscriptionBuilder::processVariablesEqualsIn);

    if (TRUE.equals(subscriptionConfiguration.getWithoutTenantId())) {
      topicSubscriptionBuilder.withoutTenantId();
    }

    ofNullable(subscriptionConfiguration.getTenantIdIn())
        .map(this::toArray)
        .ifPresent(topicSubscriptionBuilder::tenantIdIn);

    if (TRUE.equals(subscriptionConfiguration.getIncludeExtensionProperties())) {
      topicSubscriptionBuilder.includeExtensionProperties(true);
    }
    if(isAutoOpen()) {
      open();
    }
    publishInitializedEvent(topicName);
  }

  protected void publishInitializedEvent(String topicName) {
    SubscriptionInitializedEvent event = new SubscriptionInitializedEvent(this);
    applicationEventPublisher.publishEvent(event);

    LOG.initialized(topicName);
  }

  @Override
  public void open() {
    String topicName = subscriptionConfiguration.getTopicName();

    if (topicSubscriptionBuilder != null) {
      topicSubscription = topicSubscriptionBuilder.open();
      LOG.opened(topicName);
    } else {
      throw LOG.notInitializedException(topicName);
    }
  }

  @Override
  public boolean isOpen() {
    return topicSubscription != null;
  }

  public void closeInternally() {
    if (topicSubscription != null) {
      topicSubscription.close();
      topicSubscription = null;

      String topicName = subscriptionConfiguration.getTopicName();
      LOG.closed(topicName);
    }
  }

  @Override
  public void close() {
    String topicName = subscriptionConfiguration.getTopicName();
    if (topicSubscriptionBuilder == null) {
      throw LOG.notInitializedException(topicName);
    }

    if (topicSubscription != null) {
      closeInternally();
    } else {
      throw LOG.notOpenedException(topicName);
    }
  }

  @Override
  public boolean isAutoOpen() {
    return subscriptionConfiguration.getAutoOpen();
  }

  public void setExternalTaskHandler(ExternalTaskHandler externalTaskHandler) {
    this.externalTaskHandler = externalTaskHandler;
  }

  public SubscriptionConfiguration getSubscriptionConfiguration() {
    return subscriptionConfiguration;
  }

  public void setSubscriptionConfiguration(SubscriptionConfiguration subscriptionConfiguration) {
    this.subscriptionConfiguration = subscriptionConfiguration;
  }

  @Override
  public String getTopicName() {
    return subscriptionConfiguration.getTopicName();
  }

  @Override
  public Long getLockDuration() {
    return subscriptionConfiguration.getLockDuration();
  }

  @Override
  public ExternalTaskHandler getExternalTaskHandler() {
    return externalTaskHandler;
  }

  @Override
  public List<String> getVariableNames() {
    return subscriptionConfiguration.getVariableNames();
  }

  @Override
  public boolean isLocalVariables() {
    return subscriptionConfiguration.getLocalVariables();
  }

  @Override
  public String getBusinessKey() {
    return subscriptionConfiguration.getBusinessKey();
  }

  @Override
  public String getProcessDefinitionId() {
    return subscriptionConfiguration.getProcessDefinitionId();
  }

  @Override
  public List<String> getProcessDefinitionIdIn() {
    return subscriptionConfiguration.getProcessDefinitionIdIn();
  }

  @Override
  public String getProcessDefinitionKey() {
    return subscriptionConfiguration.getProcessDefinitionKey();
  }

  @Override
  public List<String> getProcessDefinitionKeyIn() {
    return subscriptionConfiguration.getProcessDefinitionKeyIn();
  }

  @Override
  public String getProcessDefinitionVersionTag() {
    return subscriptionConfiguration.getProcessDefinitionVersionTag();
  }

  @Override
  public Map<String, Object> getProcessVariables() {
    return subscriptionConfiguration.getProcessVariables();
  }

  @Override
  public boolean isWithoutTenantId() {
    return subscriptionConfiguration.getWithoutTenantId();
  }

  @Override
  public List<String> getTenantIdIn() {
    return subscriptionConfiguration.getTenantIdIn();
  }

  @Override
  public boolean isIncludeExtensionProperties() {
    return subscriptionConfiguration.getIncludeExtensionProperties();
  }

  protected String[] toArray(List<String> list) {
    return list.toArray(new String[0]);
  }

  @Override
  public void afterPropertiesSet() {
    // no-op
  }

}
