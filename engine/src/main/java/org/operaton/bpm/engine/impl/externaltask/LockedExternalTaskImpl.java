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
package org.operaton.bpm.engine.impl.externaltask;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ExternalTaskEntity;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.impl.VariableMapImpl;

/**
 * @author Thorben Lindhauer
 *
 */
public class LockedExternalTaskImpl implements LockedExternalTask {

  protected String id;
  protected String topicName;
  protected String workerId;
  protected Date lockExpirationTime;
  protected Date createTime;
  protected Integer retries;
  protected String errorMessage;
  protected String errorDetails;
  protected String processInstanceId;
  protected String executionId;
  protected String activityId;
  protected String activityInstanceId;
  protected String processDefinitionId;
  protected String processDefinitionKey;
  protected String processDefinitionVersionTag;
  protected String tenantId;
  protected long priority;
  protected VariableMapImpl variables;
  protected String businessKey;
  protected Map<String, String> extensionProperties;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getTopicName() {
    return topicName;
  }

  @Override
  public String getWorkerId() {
    return workerId;
  }

  @Override
  public Date getLockExpirationTime() {
    return lockExpirationTime;
  }

  @Override
  public Date getCreateTime() {
    return createTime;
  }

  @Override
  public Integer getRetries() {
    return retries;
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public String getProcessInstanceId() {
    return processInstanceId;
  }

  @Override
  public String getExecutionId() {
    return executionId;
  }

  @Override
  public String getActivityId() {
    return activityId;
  }

  @Override
  public String getActivityInstanceId() {
    return activityInstanceId;
  }

  @Override
  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  @Override
  public String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  @Override
  public String getProcessDefinitionVersionTag() {
    return processDefinitionVersionTag;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  @Override
  public VariableMap getVariables() {
    return variables;
  }

  @Override
  public String getErrorDetails() {
    return errorDetails;
  }

  @Override
  public long getPriority() {
    return priority;
  }

  @Override
  public String getBusinessKey() {
    return businessKey;
  }


  @Override
  public Map<String, String> getExtensionProperties() {
    return extensionProperties;
  }

  /**
   * Construct representation of locked ExternalTask from corresponding entity.
   * During mapping variables will be collected,during collection variables will not be deserialized
   * and scope will not be set to local.
   *
   * @see {@link org.operaton.bpm.engine.impl.core.variable.scope.AbstractVariableScope#collectVariables(VariableMapImpl, Collection, boolean, boolean)}
   *
   * @param externalTaskEntity - source persistent entity to use for fields
   * @param variablesToFetch - list of variable names to fetch, if null then all variables will be fetched
   * @param isLocal - if true only local variables will be collected
   *
   * @return object with all fields copied from the ExternalTaskEntity, error details fetched from the
   * database and variables attached
   */
  public static LockedExternalTaskImpl fromEntity(ExternalTaskEntity externalTaskEntity, List<String> variablesToFetch, boolean isLocal, boolean deserializeVariables, boolean includeExtensionProperties) {
    LockedExternalTaskImpl result = new LockedExternalTaskImpl();
    result.id = externalTaskEntity.getId();
    result.topicName = externalTaskEntity.getTopicName();
    result.workerId = externalTaskEntity.getWorkerId();
    result.lockExpirationTime = externalTaskEntity.getLockExpirationTime();
    result.createTime = externalTaskEntity.getCreateTime();
    result.retries = externalTaskEntity.getRetries();
    result.errorMessage = externalTaskEntity.getErrorMessage();
    result.errorDetails = externalTaskEntity.getErrorDetails();

    result.processInstanceId = externalTaskEntity.getProcessInstanceId();
    result.executionId = externalTaskEntity.getExecutionId();
    result.activityId = externalTaskEntity.getActivityId();
    result.activityInstanceId = externalTaskEntity.getActivityInstanceId();
    result.processDefinitionId = externalTaskEntity.getProcessDefinitionId();
    result.processDefinitionKey = externalTaskEntity.getProcessDefinitionKey();
    result.processDefinitionVersionTag = externalTaskEntity.getProcessDefinitionVersionTag();
    result.tenantId = externalTaskEntity.getTenantId();
    result.priority = externalTaskEntity.getPriority();
    result.businessKey = externalTaskEntity.getBusinessKey();

    ExecutionEntity execution = externalTaskEntity.getExecution();
    result.variables = new VariableMapImpl();
    execution.collectVariables(result.variables, variablesToFetch, isLocal, deserializeVariables);

    if(includeExtensionProperties) {
      result.extensionProperties = (Map<String, String>) execution.getActivity().getProperty(BpmnProperties.EXTENSION_PROPERTIES.getName());
    }
    if(result.extensionProperties == null) {
      result.extensionProperties = Collections.emptyMap();
    }

    return result;
  }
}
