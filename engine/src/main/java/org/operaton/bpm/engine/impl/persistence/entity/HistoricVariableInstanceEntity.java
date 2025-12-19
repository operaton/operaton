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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.*;
import org.operaton.bpm.engine.impl.history.event.HistoricVariableUpdateEventEntity;
import org.operaton.bpm.engine.impl.persistence.entity.util.ByteArrayField;
import org.operaton.bpm.engine.impl.persistence.entity.util.TypedValueField;
import org.operaton.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.ValueFields;
import org.operaton.bpm.engine.repository.ResourceTypes;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * @author Christian Lipphardt (Camunda)
 */
public class HistoricVariableInstanceEntity implements ValueFields, HistoricVariableInstance, DbEntity, HasDbRevision, HistoricEntity, Serializable, DbEntityLifecycleAware {

  @Serial private static final long serialVersionUID = 1L;
  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  protected String id;

  protected String processDefinitionKey;
  protected String processDefinitionId;
  protected String rootProcessInstanceId;
  protected String processInstanceId;

  protected String taskId;
  protected String executionId;
  protected String activityInstanceId;
  protected String tenantId;

  protected String caseDefinitionKey;
  protected String caseDefinitionId;
  protected String caseInstanceId;
  protected String caseExecutionId;

  protected String name;
  protected int revision;
  protected Date createTime;

  protected Long longValue;
  protected Double doubleValue;
  protected String textValue;
  protected String textValue2;

  protected String state = "CREATED";

  protected Date removalTime;

  protected ByteArrayField byteArrayField = new ByteArrayField(this, ResourceTypes.HISTORY);

  protected TypedValueField typedValueField = new TypedValueField(this, false);

  public HistoricVariableInstanceEntity() {
  }

  public HistoricVariableInstanceEntity(HistoricVariableUpdateEventEntity historyEvent) {
    updateFromEvent(historyEvent);
  }

  public void updateFromEvent(HistoricVariableUpdateEventEntity historyEvent) {
    this.id = historyEvent.getVariableInstanceId();
    this.processDefinitionKey = historyEvent.getProcessDefinitionKey();
    this.processDefinitionId = historyEvent.getProcessDefinitionId();
    this.processInstanceId = historyEvent.getProcessInstanceId();
    this.taskId = historyEvent.getTaskId();
    this.executionId = historyEvent.getExecutionId();
    this.activityInstanceId = historyEvent.getScopeActivityInstanceId();
    this.tenantId = historyEvent.getTenantId();
    this.caseDefinitionKey = historyEvent.getCaseDefinitionKey();
    this.caseDefinitionId = historyEvent.getCaseDefinitionId();
    this.caseInstanceId = historyEvent.getCaseInstanceId();
    this.caseExecutionId = historyEvent.getCaseExecutionId();
    this.name = historyEvent.getVariableName();
    this.longValue = historyEvent.getLongValue();
    this.doubleValue = historyEvent.getDoubleValue();
    this.textValue = historyEvent.getTextValue();
    this.textValue2 = historyEvent.getTextValue2();
    this.createTime = historyEvent.getTimestamp();
    this.rootProcessInstanceId = historyEvent.getRootProcessInstanceId();
    this.removalTime = historyEvent.getRemovalTime();

    setSerializerName(historyEvent.getSerializerName());

    byteArrayField.deleteByteArrayValue();

    if(historyEvent.getByteValue() != null) {
      byteArrayField.setRootProcessInstanceId(rootProcessInstanceId);
      byteArrayField.setRemovalTime(removalTime);
      setByteArrayValue(historyEvent.getByteValue());
    }

  }

  public void delete() {
    byteArrayField.deleteByteArrayValue();

    Context
      .getCommandContext()
      .getDbEntityManager()
      .delete(this);
  }

  @Override
  public Object getPersistentState() {
    List<Object> result = new ArrayList<>(8);
    result.add(getSerializerName());
    result.add(textValue);
    result.add(textValue2);
    result.add(this.state);
    result.add(doubleValue);
    result.add(longValue);
    result.add(processDefinitionId);
    result.add(processDefinitionKey);
    result.add(getByteArrayId());
    return result;
  }

  @Override
  public int getRevisionNext() {
    return revision+1;
  }

  @Override
  public Object getValue() {
    return typedValueField.getValue();
  }

  @Override
  public TypedValue getTypedValue() {
    return typedValueField.getTypedValue(false);
  }

  public TypedValue getTypedValue(boolean deserializeValue) {
    return typedValueField.getTypedValue(deserializeValue, false);
  }

  public TypedValueSerializer<?> getSerializer() {
    return typedValueField.getSerializer();
  }

  public String getByteArrayValueId() {
    return byteArrayField.getByteArrayId();
  }

  public String getByteArrayId() {
    return byteArrayField.getByteArrayId();
  }

  public void setByteArrayId(String byteArrayId) {
    byteArrayField.setByteArrayId(byteArrayId);
  }

  @Override
  public byte[] getByteArrayValue() {
    return byteArrayField.getByteArrayValue();
  }

  @Override
  public void setByteArrayValue(byte[] bytes) {
    byteArrayField.setByteArrayValue(bytes);
  }

  // entity lifecycle /////////////////////////////////////////////////////////

  @Override
  public void postLoad() {
    // make sure the serializer is initialized
    typedValueField.postLoad();
  }

  // getters and setters //////////////////////////////////////////////////////

  public String getSerializerName() {
    return typedValueField.getSerializerName();
  }

  public void setSerializerName(String serializerName) {
    typedValueField.setSerializerName(serializerName);
  }

  @Override
  public String getTypeName() {
    return typedValueField.getTypeName();
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

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Long getLongValue() {
    return longValue;
  }

  @Override
  public void setLongValue(Long longValue) {
    this.longValue = longValue;
  }

  @Override
  public Double getDoubleValue() {
    return doubleValue;
  }

  @Override
  public void setDoubleValue(Double doubleValue) {
    this.doubleValue = doubleValue;
  }

  @Override
  public String getTextValue() {
    return textValue;
  }

  @Override
  public void setTextValue(String textValue) {
    this.textValue = textValue;
  }

  @Override
  public String getTextValue2() {
    return textValue2;
  }

  @Override
  public void setTextValue2(String textValue2) {
    this.textValue2 = textValue2;
  }

  public void setByteArrayValue(ByteArrayEntity byteArrayValue) {
    byteArrayField.setByteArrayValue(byteArrayValue);
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
  public String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  public void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  @Override
  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  @Override
  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  @Override
  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  @Override
  public String getExecutionId() {
    return executionId;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  @Override
  public String getActivityInstanceId() {
    return activityInstanceId;
  }

  public void setActivityInstanceId(String activityInstanceId) {
    this.activityInstanceId = activityInstanceId;
  }

  @Override
  public String getCaseDefinitionKey() {
    return caseDefinitionKey;
  }

  public void setCaseDefinitionKey(String caseDefinitionKey) {
    this.caseDefinitionKey = caseDefinitionKey;
  }

  @Override
  public String getCaseDefinitionId() {
    return caseDefinitionId;
  }

  public void setCaseDefinitionId(String caseDefinitionId) {
    this.caseDefinitionId = caseDefinitionId;
  }

  @Override
  public String getCaseInstanceId() {
    return caseInstanceId;
  }

  public void setCaseInstanceId(String caseInstanceId) {
    this.caseInstanceId = caseInstanceId;
  }

  @Override
  public String getCaseExecutionId() {
    return caseExecutionId;
  }

  public void setCaseExecutionId(String caseExecutionId) {
    this.caseExecutionId = caseExecutionId;
  }

  @Override
  public String getErrorMessage() {
    return typedValueField.getErrorMessage();
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
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

  public void setRootProcessInstanceId(String rootProcessInstanceId) {
    this.rootProcessInstanceId = rootProcessInstanceId;
  }

  @Override
  public Date getRemovalTime() {
    return removalTime;
  }

  public void setRemovalTime(Date removalTime) {
    this.removalTime = removalTime;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
      + "[id=%s, processDefinitionKey=%s, processDefinitionId=%s, rootProcessInstanceId=".formatted(id, processDefinitionKey).formatted(processDefinitionId) + rootProcessInstanceId
      + ", removalTime=%s, processInstanceId=%s, taskId=%s, executionId=".formatted(removalTime, processInstanceId).formatted(taskId) + executionId
      + ", tenantId=%s, activityInstanceId=%s, caseDefinitionKey=%s, caseDefinitionId=".formatted(tenantId, activityInstanceId).formatted(caseDefinitionKey) + caseDefinitionId
      + ", caseInstanceId=%s, caseExecutionId=%s, name=%s, createTime=".formatted(caseInstanceId, caseExecutionId).formatted(name) + createTime
      + ", revision=%s, serializerName=%s, longValue=%s, doubleValue=".formatted(revision, getSerializerName()).formatted(longValue) + doubleValue
      + ", textValue=%s, textValue2=%s, state=%s, byteArrayId=".formatted(textValue, textValue2).formatted(state) + getByteArrayId()
      + "]";
  }

}
