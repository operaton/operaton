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
package org.operaton.bpm.engine.impl.variable.listener;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineServices;
import org.operaton.bpm.engine.delegate.DelegateCaseExecution;
import org.operaton.bpm.engine.delegate.DelegateCaseVariableInstance;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.variable.value.TypedValue;
/**
 * @author Thorben Lindhauer
 *
 */
public class DelegateCaseVariableInstanceImpl implements DelegateCaseVariableInstance {

  protected String eventName;
  protected DelegateCaseExecution sourceExecution;
  protected DelegateCaseExecution scopeExecution;

  // fields copied from variable instance
  protected String variableId;
  protected String processDefinitionId;
  protected String processInstanceId;
  protected String executionId;
  protected String caseInstanceId;
  protected String caseExecutionId;
  protected String taskId;
  protected String activityInstanceId;
  protected String tenantId;
  protected String errorMessage;
  protected String name;
  protected TypedValue value;

  @Override
  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  @Override
  public DelegateCaseExecution getSourceExecution() {
    return sourceExecution;
  }

  public void setSourceExecution(DelegateCaseExecution sourceExecution) {
    this.sourceExecution = sourceExecution;
  }

  /**
   * Currently not part of public interface.
   */
  public DelegateCaseExecution getScopeExecution() {
    return scopeExecution;
  }

  public void setScopeExecution(DelegateCaseExecution scopeExecution) {
    this.scopeExecution = scopeExecution;
  }

  //// methods delegated to wrapped variable ////

  @Override
  public String getId() {
    return variableId;
  }

  @Override
  public String getProcessDefinitionId() {
    return processDefinitionId;
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
  public String getCaseInstanceId() {
    return caseInstanceId;
  }

  @Override
  public String getCaseExecutionId() {
    return caseExecutionId;
  }

  @Override
  public String getTaskId() {
    return taskId;
  }

  @Override
  public String getBatchId() {
    return null;
  }

  @Override
  public String getActivityInstanceId() {
    return activityInstanceId;
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public String getTypeName() {
    if(value != null) {
      return value.getType().getName();
    }
    else {
      return null;
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Object getValue() {
    if(value != null) {
      return value.getValue();
    }
    else {
      return null;
    }
  }

  @Override
  public TypedValue getTypedValue() {
    return value;
  }

  @Override
  public ProcessEngineServices getProcessEngineServices() {
    return Context.getProcessEngineConfiguration().getProcessEngine();
  }

  @Override
  public ProcessEngine getProcessEngine() {
    return Context.getProcessEngineConfiguration().getProcessEngine();
  }

  public static DelegateCaseVariableInstanceImpl fromVariableInstance(VariableInstance variableInstance) {
    DelegateCaseVariableInstanceImpl delegateInstance = new DelegateCaseVariableInstanceImpl();
    delegateInstance.variableId = variableInstance.getId();
    delegateInstance.processDefinitionId = variableInstance.getProcessDefinitionId();
    delegateInstance.processInstanceId = variableInstance.getProcessInstanceId();
    delegateInstance.executionId = variableInstance.getExecutionId();
    delegateInstance.caseExecutionId = variableInstance.getCaseExecutionId();
    delegateInstance.caseInstanceId = variableInstance.getCaseInstanceId();
    delegateInstance.taskId = variableInstance.getTaskId();
    delegateInstance.activityInstanceId = variableInstance.getActivityInstanceId();
    delegateInstance.tenantId = variableInstance.getTenantId();
    delegateInstance.errorMessage = variableInstance.getErrorMessage();
    delegateInstance.name = variableInstance.getName();
    delegateInstance.value = variableInstance.getTypedValue();

    return delegateInstance;
  }

}
