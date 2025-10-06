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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.operaton.bpm.client.spring.annotation.ExternalTaskSubscription;

import static org.operaton.bpm.client.spring.annotation.ExternalTaskSubscription.LONG_NULL_VALUE;
import static org.operaton.bpm.client.spring.annotation.ExternalTaskSubscription.ProcessVariable;
import static org.operaton.bpm.client.spring.annotation.ExternalTaskSubscription.STRING_NULL_VALUE;

public class SubscriptionConfiguration {

  protected Boolean autoOpen;

  protected String topicName;
  protected Long lockDuration;
  protected List<String> variableNames;
  protected Boolean localVariables;
  protected String businessKey;
  protected String processDefinitionId;
  protected List<String> processDefinitionIdIn;
  protected String processDefinitionKey;
  protected List<String> processDefinitionKeyIn;
  protected String processDefinitionVersionTag;
  protected Map<String, Object> processVariables = new HashMap<>();
  protected Boolean withoutTenantId;
  protected List<String> tenantIdIn;
  protected Boolean includeExtensionProperties;

  public Boolean getAutoOpen() {
    return autoOpen;
  }

  public void setAutoOpen(Boolean autoOpen) {
    this.autoOpen = autoOpen;
  }

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public Long getLockDuration() {
    return lockDuration;
  }

  public void setLockDuration(Long lockDuration) {
    this.lockDuration = lockDuration;
  }

  public List<String> getVariableNames() {
    return variableNames;
  }

  public void setVariableNames(List<String> variableNames) {
    this.variableNames = variableNames;
  }

  public Boolean getLocalVariables() {
    return localVariables;
  }

  public void setLocalVariables(Boolean localVariables) {
    this.localVariables = localVariables;
  }

  public String getBusinessKey() {
    return businessKey;
  }

  public void setBusinessKey(String businessKey) {
    this.businessKey = businessKey;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  public List<String> getProcessDefinitionIdIn() {
    return processDefinitionIdIn;
  }

  public void setProcessDefinitionIdIn(List<String> processDefinitionIdIn) {
    this.processDefinitionIdIn = processDefinitionIdIn;
  }

  public String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  public void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  public List<String> getProcessDefinitionKeyIn() {
    return processDefinitionKeyIn;
  }

  public void setProcessDefinitionKeyIn(List<String> processDefinitionKeyIn) {
    this.processDefinitionKeyIn = processDefinitionKeyIn;
  }

  public String getProcessDefinitionVersionTag() {
    return processDefinitionVersionTag;
  }

  public void setProcessDefinitionVersionTag(String processDefinitionVersionTag) {
    this.processDefinitionVersionTag = processDefinitionVersionTag;
  }

  public Map<String, Object> getProcessVariables() {
    return processVariables;
  }

  public void setProcessVariables(Map<String, Object> processVariables) {
    this.processVariables = processVariables;
  }

  public Boolean getWithoutTenantId() {
    return withoutTenantId;
  }

  public void setWithoutTenantId(Boolean withoutTenantId) {
    this.withoutTenantId = withoutTenantId;
  }

  public List<String> getTenantIdIn() {
    return tenantIdIn;
  }

  public void setTenantIdIn(List<String> tenantIdIn) {
    this.tenantIdIn = tenantIdIn;
  }

  public Boolean getIncludeExtensionProperties() {
    return includeExtensionProperties;
  }

  public void setIncludeExtensionProperties(Boolean includeExtensionProperties) {
    this.includeExtensionProperties = includeExtensionProperties;
  }

  public void fromAnnotation(ExternalTaskSubscription config) {
    setAutoOpen(config.autoOpen());

    String configuredTopicName = config.topicName();
    setTopicName(isNull(configuredTopicName) ? null : configuredTopicName);

    long configuredLockDuration = config.lockDuration();
    setLockDuration(isNull(configuredLockDuration) ? null : configuredLockDuration);

    String[] configuredVariableNames = config.variableNames();
    setVariableNames(isNull(configuredVariableNames) ? null : Arrays.asList(configuredVariableNames));

    setLocalVariables(config.localVariables());

    String configuredBusinessKey = config.businessKey();
    setBusinessKey(isNull(configuredBusinessKey) ? null : configuredBusinessKey);

    String configuredProcessDefinitionId = config.processDefinitionId();
    setProcessDefinitionId(isNull(configuredProcessDefinitionId) ? null : configuredProcessDefinitionId);

    String[] configuredProcessDefinitionIdIn = config.processDefinitionIdIn();
    setProcessDefinitionIdIn(isNull(configuredProcessDefinitionIdIn) ? null :
        Arrays.asList(configuredProcessDefinitionIdIn));

    String configuredProcessDefinitionKey = config.processDefinitionKey();
    setProcessDefinitionKey(isNull(configuredProcessDefinitionKey) ? null : configuredProcessDefinitionKey);

    String[] configuredProcessDefinitionKeyIn = config.processDefinitionKeyIn();
    setProcessDefinitionKeyIn(isNull(configuredProcessDefinitionKeyIn) ? null :
        Arrays.asList(configuredProcessDefinitionKeyIn));

    String configuredProcessDefinitionVersionTag = config.processDefinitionVersionTag();
    setProcessDefinitionVersionTag(isNull(configuredProcessDefinitionVersionTag) ? null :
        configuredProcessDefinitionVersionTag);

    ProcessVariable[] configuredProcessVariables = config.processVariables();
    setProcessVariables(isNull(configuredProcessVariables) ? null : Arrays.stream(configuredProcessVariables)
        .collect(Collectors.toMap(ProcessVariable::name, ProcessVariable::value)));

    setWithoutTenantId(config.withoutTenantId());

    String[] configuredTenantIdIn = config.tenantIdIn();
    setTenantIdIn(isNull(configuredTenantIdIn) ? null : Arrays.asList(configuredTenantIdIn));

    setIncludeExtensionProperties(config.includeExtensionProperties());
  }

  protected static boolean isNull(String[] values) {
    return values.length == 1 && STRING_NULL_VALUE.equals(values[0]);
  }

  protected static boolean isNull(String value) {
    return STRING_NULL_VALUE.equals(value);
  }

  protected static boolean isNull(long value) {
    return LONG_NULL_VALUE == value;
  }

  protected static boolean isNull(ProcessVariable[] values) {
    return values.length == 1 && STRING_NULL_VALUE.equals(values[0].name()) &&
        STRING_NULL_VALUE.equals(values[0].value());
  }

}
