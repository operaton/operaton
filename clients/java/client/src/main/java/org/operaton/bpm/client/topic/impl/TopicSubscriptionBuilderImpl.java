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
package org.operaton.bpm.client.topic.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.client.impl.ExternalTaskClientLogger;
import org.operaton.bpm.client.task.ExternalTaskHandler;
import org.operaton.bpm.client.topic.TopicSubscription;
import org.operaton.bpm.client.topic.TopicSubscriptionBuilder;

import static java.util.Objects.requireNonNull;

/**
 * @author Tassilo Weidner
 */
public @NullMarked class TopicSubscriptionBuilderImpl implements TopicSubscriptionBuilder {

  protected static final ExternalTaskClientLogger LOG = ExternalTaskClientLogger.CLIENT_LOGGER;

  protected String topicName;
  protected @Nullable Long lockDuration;
  protected @Nullable List<String> variableNames;
  protected boolean localVariables;
  protected @Nullable String businessKey;
  protected @Nullable String processDefinitionId;
  protected @Nullable List<String> processDefinitionIds;
  protected @Nullable String processDefinitionKey;
  protected @Nullable List<String> processDefinitionKeys;
  protected @Nullable String processDefinitionVersionTag;
  protected @Nullable Map<String, Object> processVariables;
  protected boolean withoutTenantId;
  protected @Nullable List<String> tenantIds;
  protected @Nullable ExternalTaskHandler externalTaskHandler;
  protected TopicSubscriptionManager topicSubscriptionManager;
  protected boolean includeExtensionProperties;


  public TopicSubscriptionBuilderImpl(String topicName, TopicSubscriptionManager topicSubscriptionManager) {
    this.topicName = topicName;
    this.variableNames = null; // if not null, no variables are retrieved by default
    this.lockDuration = null;
    this.topicSubscriptionManager = topicSubscriptionManager;
  }

  @Override
  public TopicSubscriptionBuilder lockDuration(long lockDuration) {
    this.lockDuration = lockDuration;
    return this;
  }

  @Override
  public TopicSubscriptionBuilder handler(ExternalTaskHandler externalTaskHandler) {
    this.externalTaskHandler = externalTaskHandler;
    return this;
  }

  @Override
  public TopicSubscriptionBuilder variables(@Nullable String @Nullable... variableNames) {
    ensureNotNull(variableNames, "variableNames");
    this.variableNames = Stream.of(requireNonNull(variableNames)).filter(Objects::nonNull).toList();
    return this;
  }

  @Override
  public TopicSubscriptionBuilder localVariables(boolean localVariables) {
    this.localVariables = localVariables;
    return this;
  }

  @Override
  public TopicSubscriptionBuilder businessKey(String businessKey) {
    this.businessKey = businessKey;
    return this;
  }

  @Override
  public TopicSubscriptionBuilder processDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
    return this;
  }

  @Override
  public TopicSubscriptionBuilder processDefinitionIdIn(@Nullable String @Nullable... processDefinitionIds) {
    ensureNotNull(processDefinitionIds, "processDefinitionIds");
    this.processDefinitionIds = Stream.of(requireNonNull(processDefinitionIds)).filter(Objects::nonNull).toList();
    return this;
  }

  @Override
  public TopicSubscriptionBuilder processDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
    return this;
  }

  @Override
  public TopicSubscriptionBuilder processDefinitionKeyIn(@Nullable String @Nullable... processDefinitionKeys) {
    ensureNotNull(processDefinitionKeys, "processDefinitionKeys");
    this.processDefinitionKeys = Stream.of(requireNonNull(processDefinitionKeys)).filter(Objects::nonNull).toList();
    return this;
  }

  @Override
  public TopicSubscriptionBuilder processDefinitionVersionTag(String processDefinitionVersionTag) {
    ensureNotNull(processDefinitionVersionTag, "processDefinitionVersionTag");
    this.processDefinitionVersionTag = processDefinitionVersionTag;
    return this;
  }

  @Override
  public TopicSubscriptionBuilder processVariablesEqualsIn(@Nullable Map<String, Object> processVariables) {
    ensureNotNull(processVariables, "processVariables");
    requireNonNull(processVariables);
    if (this.processVariables == null) {
      this.processVariables = new HashMap<>();
    }
    for (Map.Entry<String, Object> processVariable : processVariables.entrySet()) {
      ensureNotNull(processVariable.getKey(), "processVariableName");
      this.processVariables.put(processVariable.getKey(), processVariable.getValue());
    }
    return this;
  }

  @Override
  public TopicSubscriptionBuilder processVariableEquals(String name, Object value) {
    ensureNotNull(name, "processVariableName");
    if (this.processVariables == null) {
      this.processVariables = new HashMap<>();
    }
    this.processVariables.put(name, value);
    return this;
  }

  @Override
  public TopicSubscriptionBuilder withoutTenantId() {
    withoutTenantId = true;
    return this;
  }

  @Override
  public TopicSubscriptionBuilder tenantIdIn(@Nullable String @Nullable... tenantIds) {
    ensureNotNull(tenantIds, "tenantIds");
    this.tenantIds = Stream.of(requireNonNull(tenantIds)).filter(Objects::nonNull).toList();
    return this;
  }

  @Override
  public TopicSubscriptionBuilder includeExtensionProperties(boolean includeExtensionProperties) {
    this.includeExtensionProperties = includeExtensionProperties;
    return this;
  }

  @Override
  public TopicSubscription open() {
    if (lockDuration != null && lockDuration <= 0L) {
      throw LOG.lockDurationIsNotGreaterThanZeroException(lockDuration);
    }

    if (externalTaskHandler == null) {
      throw LOG.externalTaskHandlerNullException();
    }

    TopicSubscriptionImpl subscription = new TopicSubscriptionImpl(topicName, lockDuration, externalTaskHandler, topicSubscriptionManager, variableNames, businessKey);
    if (processDefinitionId != null) {
      subscription.setProcessDefinitionId(processDefinitionId);
    }
    if (processDefinitionIds != null) {
      subscription.setProcessDefinitionIdIn(processDefinitionIds);
    }
    if (processDefinitionKey != null) {
      subscription.setProcessDefinitionKey(processDefinitionKey);
    }
    if (processDefinitionKeys != null) {
      subscription.setProcessDefinitionKeyIn(processDefinitionKeys);
    }
    if (withoutTenantId) {
      subscription.setWithoutTenantId(withoutTenantId);
    }
    if (tenantIds != null) {
      subscription.setTenantIdIn(tenantIds);
    }
    if(processDefinitionVersionTag != null) {
      subscription.setProcessDefinitionVersionTag(processDefinitionVersionTag);
    }
    if (processVariables != null) {
      subscription.setProcessVariables(processVariables);
    }
    if (localVariables) {
      subscription.setLocalVariables(localVariables);
    }
    if(includeExtensionProperties) {
      subscription.setIncludeExtensionProperties(includeExtensionProperties);
    }
    topicSubscriptionManager.subscribe(subscription);

    return subscription;
  }

  protected void ensureNotNull(@Nullable Object tenantIds, String parameterName) {
    if (tenantIds == null) {
      throw LOG.passNullValueParameter(parameterName);
    }
  }

}
