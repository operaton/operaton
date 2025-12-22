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
import java.util.Date;

import org.operaton.bpm.engine.history.HistoricVariableUpdate;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.DbEntityLifecycleAware;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.db.entitymanager.DbEntityManager;
import org.operaton.bpm.engine.impl.history.event.HistoricVariableUpdateEventEntity;
import org.operaton.bpm.engine.impl.persistence.entity.util.ByteArrayField;
import org.operaton.bpm.engine.impl.persistence.entity.util.TypedValueField;
import org.operaton.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.ValueFields;
import org.operaton.bpm.engine.repository.ResourceTypes;
import org.operaton.bpm.engine.variable.value.TypedValue;


/**
 * @author Tom Baeyens
 */
public class HistoricDetailVariableInstanceUpdateEntity extends HistoricVariableUpdateEventEntity implements ValueFields, HistoricVariableUpdate, DbEntityLifecycleAware {

  @Serial private static final long serialVersionUID = 1L;
  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  protected TypedValueField typedValueField = new TypedValueField(this, false);

  protected ByteArrayField byteArrayField = new ByteArrayField(this, ResourceTypes.HISTORY);

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

  @Override
  public void delete() {

    DbEntityManager dbEntityManger = Context
        .getCommandContext()
        .getDbEntityManager();

    dbEntityManger.delete(this);

    byteArrayField.deleteByteArrayValue();
  }

  public TypedValueSerializer<?> getSerializer() {
    return typedValueField.getSerializer();
  }

  @Override
  public String getErrorMessage() {
    return typedValueField.getErrorMessage();
  }

  @Override
  public void setByteArrayId(String id) {
    byteArrayField.setByteArrayId(id);
  }

  @Override
  public String getSerializerName() {
    return typedValueField.getSerializerName();
  }
  @Override
  public void setSerializerName(String serializerName) {
    typedValueField.setSerializerName(serializerName);
  }

  public String getByteArrayValueId() {
    return byteArrayField.getByteArrayId();
  }

  @Override
  public byte[] getByteArrayValue() {
    return byteArrayField.getByteArrayValue();
  }

  @Override
  public void setByteArrayValue(byte[] bytes) {
    byteArrayField.setByteArrayValue(bytes);
  }

  @Override
  public String getName() {
    return getVariableName();
  }

  // entity lifecycle /////////////////////////////////////////////////////////

  @Override
  public void postLoad() {
    // make sure the serializer is initialized
    typedValueField.postLoad();
  }

  // getters and setters //////////////////////////////////////////////////////

  @Override
  public String getTypeName() {
    return typedValueField.getTypeName();
  }

  @Override
  public String getVariableTypeName() {
    return getTypeName();
  }

  @Override
  public Date getTime() {
    return timestamp;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
           + "[variableName=" + variableName
           + ", variableInstanceId=" + variableInstanceId
           + ", revision=" + revision
           + ", serializerName=" + serializerName
           + ", longValue=" + longValue
           + ", doubleValue=" + doubleValue
           + ", textValue=" + textValue
           + ", textValue2=" + textValue2
           + ", byteArrayId=" + byteArrayId
           + ", activityInstanceId=" + activityInstanceId
           + ", eventType=" + eventType
           + ", executionId=" + executionId
           + ", id=" + id
           + ", processDefinitionId=" + processDefinitionId
           + ", processInstanceId=" + processInstanceId
           + ", taskId=" + taskId
           + ", timestamp=" + timestamp
           + "]";
  }

}
