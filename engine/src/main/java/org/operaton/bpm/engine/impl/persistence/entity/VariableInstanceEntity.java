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

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.application.InvocationContext;
import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.context.ProcessApplicationContextUtil;
import org.operaton.bpm.engine.impl.core.variable.CoreVariableInstance;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.DbEntityLifecycleAware;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.db.HasDbReferences;
import org.operaton.bpm.engine.impl.db.HasDbRevision;
import org.operaton.bpm.engine.impl.persistence.entity.util.ByteArrayField;
import org.operaton.bpm.engine.impl.persistence.entity.util.TypedValueField;
import org.operaton.bpm.engine.impl.persistence.entity.util.TypedValueUpdateListener;
import org.operaton.bpm.engine.impl.pvm.runtime.LegacyBehavior;
import org.operaton.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.ValueFields;
import org.operaton.bpm.engine.impl.variable.serializer.VariableSerializerFactory;
import org.operaton.bpm.engine.repository.ResourceTypes;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.operaton.bpm.engine.impl.persistence.entity.util.TypedValueField.getSerializers;

/**
 * @author Tom Baeyens
 */
public class VariableInstanceEntity implements VariableInstance, CoreVariableInstance, ValueFields, DbEntity, DbEntityLifecycleAware, TypedValueUpdateListener, HasDbRevision,
  HasDbReferences, Serializable {

  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  private static final long serialVersionUID = 1L;

  protected String id;
  protected int revision;

  protected String name;

  protected String processDefinitionId;
  protected String processInstanceId;
  protected String executionId;
  protected String taskId;
  protected String batchId;
  protected String caseInstanceId;
  protected String caseExecutionId;
  protected String activityInstanceId;
  protected String tenantId;

  protected Long longValue;
  protected Double doubleValue;
  protected String textValue;
  protected String textValue2;
  protected String variableScopeId;

  protected ByteArrayField byteArrayField = new ByteArrayField(this, ResourceTypes.RUNTIME);

  protected TypedValueField typedValueField = new TypedValueField(this, true);

  boolean forcedUpdate;

  protected String configuration;

  protected long sequenceCounter = 1;

  /**
   * <p>Determines whether this variable is supposed to be a local variable
   * in case of concurrency in its scope. This affects
   * </p>
   *
   * <ul>
   * <li>tree expansion (not evaluated yet by the engine)
   * <li>activity instance IDs of variable instances: concurrentLocal
   *   variables always receive the activity instance id of their execution
   *   (which may not be the scope execution), while non-concurrentLocal variables
   *   always receive the activity instance id of their scope (which is set in the
   *   parent execution)
   * </ul>
   *
   * <p>
   *   In the future, this field could be used for restoring the variable distribution
   *   when the tree is expanded/compacted multiple times.
   *   On expansion, the goal would be to keep concurrentLocal variables always with
   *   their concurrent replacing executions while non-concurrentLocal variables
   *   stay in the scope execution
   * </p>
   */
  protected boolean isConcurrentLocal = false;

  /**
   * Determines whether this variable is stored in the data base.
   */
  protected boolean isTransient = false;

  // transient properties
  protected ExecutionEntity execution;

  // Default constructor for SQL mapping
  public VariableInstanceEntity() {
    typedValueField.addImplicitUpdateListener(this);
  }

  public VariableInstanceEntity(String name, TypedValue value, boolean isTransient) {
    this();
    this.name = name;
    this.isTransient = isTransient;
    typedValueField.setValue(value);
  }

  public static VariableInstanceEntity createAndInsert(String name, TypedValue value) {
    VariableInstanceEntity variableInstance = create(name, value, value.isTransient());
    insert(variableInstance);
    return variableInstance;
  }

  public static void insert(VariableInstanceEntity variableInstance) {
    if (!variableInstance.isTransient()) {
      Context
      .getCommandContext()
      .getDbEntityManager()
      .insert(variableInstance);
    }
  }

  public static VariableInstanceEntity create(String name, TypedValue value, boolean isTransient) {
    return new VariableInstanceEntity(name, value, isTransient);
  }

  public void delete() {

    if (!isTransient()) {
      typedValueField.notifyImplicitValueUpdateIfEnabled();
    }

    // clear value
    clearValueFields(true);

    if (!isTransient) {
      // delete variable
      Context.getCommandContext().getDbEntityManager().delete(this);
    }
  }

  @Override
  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<>();
    if (typedValueField.getSerializerName() != null) {
      persistentState.put("serializerName", typedValueField.getSerializerName());
    }
    if (longValue != null) {
      persistentState.put("longValue", longValue);
    }
    if (doubleValue != null) {
      persistentState.put("doubleValue", doubleValue);
    }
    if (textValue != null) {
      persistentState.put("textValue", textValue);
    }
    if (textValue2 != null) {
      persistentState.put("textValue2", textValue2);
    }
    if (byteArrayField.getByteArrayId() != null) {
      persistentState.put("byteArrayValueId", byteArrayField.getByteArrayId());
    }

    persistentState.put("sequenceCounter", getSequenceCounter());
    persistentState.put("concurrentLocal", isConcurrentLocal);
    persistentState.put("executionId", executionId);
    persistentState.put("taskId", taskId);
    persistentState.put("caseExecutionId", caseExecutionId);
    persistentState.put("caseInstanceId", caseInstanceId);
    persistentState.put("tenantId", tenantId);
    persistentState.put("processInstanceId", processInstanceId);
    persistentState.put("processDefinitionId", processDefinitionId);

    return persistentState;
  }

  @Override
  public int getRevisionNext() {
    return revision+1;
  }

  // lazy initialized relations ///////////////////////////////////////////////

  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  public void setCaseInstanceId(String caseInstanceId) {
    this.caseInstanceId = caseInstanceId;
  }

  public void setCaseExecutionId(String caseExecutionId) {
    this.caseExecutionId = caseExecutionId;
  }

  public void setCaseExecution(CaseExecutionEntity caseExecution) {
    if (caseExecution != null) {
      this.caseInstanceId = caseExecution.getCaseInstanceId();
      this.caseExecutionId = caseExecution.getId();
      this.tenantId = caseExecution.getTenantId();
    }
    else {
      this.caseInstanceId = null;
      this.caseExecutionId = null;
      this.tenantId = null;
    }
  }

  // byte array value /////////////////////////////////////////////////////////

  // i couldn't find a easy readable way to extract the common byte array value logic
  // into a common class.  therefor it's duplicated in VariableInstanceEntity,
  // HistoricVariableInstance and HistoricDetailVariableInstanceUpdateEntity

  public String getByteArrayValueId() {
    return byteArrayField.getByteArrayId();
  }

  public void setByteArrayValueId(String byteArrayValueId) {
    this.byteArrayField.setByteArrayId(byteArrayValueId);
  }

  @Override
  public byte[] getByteArrayValue() {
    return byteArrayField.getByteArrayValue();
  }

  @Override
  public void setByteArrayValue(byte[] bytes) {
    byteArrayField.setByteArrayValue(bytes, isTransient);
  }

  protected void deleteByteArrayValue() {
    byteArrayField.deleteByteArrayValue();
  }

  // type /////////////////////////////////////////////////////////////////////

  @Override
  public Object getValue() {
    return typedValueField.getValue();
  }

  @Override
  public TypedValue getTypedValue() {
    return typedValueField.getTypedValue(isTransient);
  }

  @Override
  public TypedValue getTypedValue(boolean deserializeValue) {
    return typedValueField.getTypedValue(deserializeValue, isTransient);
  }

  @Override
  public void setValue(TypedValue value) {
    TypedValue newValue = value;

    ValueType newType = null;
    if (newValue instanceof UntypedValueImpl impl) {
      newValue = getSerializers()
          .findSerializerForValue(newValue, getFallbackSerializerFactory())
          .convertToTypedValue(impl);
      newType = newValue.getType();
    }

    String oldTypeAsString = typedValueField.getTypeName();
    clearValueFields(!isObjectTyped(newType, oldTypeAsString));
    typedValueField.setValue(newValue);
  }

  /**
   * The engine should only remove byte arrays in case the type
   * changes but keep it when old and new types are object.
   *
   * @param newType of the variable overriding the old.
   * @param oldTypeAsString of the variable previously set.
   * @return {@code true} if old and new variable are both object typed.
   */
  protected boolean isObjectTyped(ValueType newType, String oldTypeAsString) {
    return ValueType.OBJECT.equals(newType) && newType.getName().equals(oldTypeAsString);
  }

  protected VariableSerializerFactory getFallbackSerializerFactory() {
    return Context.getProcessEngineConfiguration().getFallbackSerializerFactory();
  }

  public void clearValueFields(boolean deleteVariable) {
    this.longValue = null;
    this.doubleValue = null;
    this.textValue = null;
    this.textValue2 = null;
    typedValueField.clear();

    if (deleteVariable && byteArrayField.getByteArrayId() != null) {
      deleteByteArrayValue();
      setByteArrayValueId(null);
    }
  }

  @Override
  public String getTypeName() {
    return typedValueField.getTypeName();
  }

  // entity lifecycle /////////////////////////////////////////////////////////

  @Override
  public void postLoad() {
    // make sure the serializer is initialized
    typedValueField.postLoad();
  }

  // execution ////////////////////////////////////////////////////////////////

  protected void ensureExecutionInitialized() {
    if (execution == null && executionId != null) {
      execution = Context
          .getCommandContext()
          .getExecutionManager()
          .findExecutionById(executionId);
    }
  }

  public ExecutionEntity getExecution() {
    ensureExecutionInitialized();
    return execution;
  }

  public void setExecution(ExecutionEntity execution) {
    this.execution = execution;

    if (execution == null) {
      this.executionId = null;
      this.processInstanceId = null;
      this.processDefinitionId = null;
      this.tenantId = null;
    }
    else {
      setExecutionId(execution.getId());
      this.processDefinitionId = execution.getProcessDefinitionId();
      this.processInstanceId = execution.getProcessInstanceId();
      this.tenantId = execution.getTenantId();
    }

  }

  // case execution ///////////////////////////////////////////////////////////

  public CaseExecutionEntity getCaseExecution() {
    if (caseExecutionId != null) {
      return Context
          .getCommandContext()
          .getCaseExecutionManager()
          .findCaseExecutionById(caseExecutionId);
    }
    return null;
  }

  // getters and setters //////////////////////////////////////////////////////

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getTextValue() {
    return textValue;
  }

  @Override
  public String getProcessInstanceId() {
    return processInstanceId;
  }

  @Override
  public String getProcessDefinitionId() {
    return processDefinitionId;
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

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void setTextValue(String textValue) {
    this.textValue = textValue;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getRevision() {
    return revision;
  }

  @Override
  public void setRevision(int revision) {
    this.revision = revision;
  }

  public void setSerializer(TypedValueSerializer<?> serializer) {
    typedValueField.setSerializerName(serializer.getName());
  }

  public void setSerializerName(String type) {
    typedValueField.setSerializerName(type);
  }

  public TypedValueSerializer<?> getSerializer() {
    return typedValueField.getSerializer();
  }

  @Override
  public String getTextValue2() {
    return textValue2;
  }

  @Override
  public void setTextValue2(String textValue2) {
    this.textValue2 = textValue2;
  }

  @Override
  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  @Override
  public String getBatchId() {
    return batchId;
  }

  public void setBatchId(String batchId) {
    this.batchId = batchId;
  }

  public void setTask(TaskEntity task) {
    if (task != null) {
      this.taskId = task.getId();
      this.tenantId = task.getTenantId();

      if (task.getExecution() != null) {
        setExecution(task.getExecution());
      }
      if (task.getCaseExecution() != null) {
        setCaseExecution(task.getCaseExecution());
      }
    }
    else {
      this.taskId = null;
      this.tenantId = null;
      setExecution(null);
      setCaseExecution(null);
    }


  }

  @Override
  public String getActivityInstanceId() {
    return activityInstanceId;
  }

  public void setActivityInstanceId(String activityInstanceId) {
    this.activityInstanceId = activityInstanceId;
  }

  public String getSerializerName() {
    return typedValueField.getSerializerName();
  }

  @Override
  public String getErrorMessage() {
    return typedValueField.getErrorMessage();
  }

  public String getVariableScopeId() {
    if (variableScopeId != null) {
      return variableScopeId;
    }

    if (taskId != null) {
      return taskId;
    }

    if (executionId != null) {
      return executionId;
    }

    return caseExecutionId;
  }

  public void setVariableScopeId(String variableScopeId) {
    this.variableScopeId = variableScopeId;
  }

  protected VariableScope getVariableScope() {

    if (taskId != null) {
      return getTask();
    }
    else if (executionId != null) {
      return getExecution();
    }
    else if (caseExecutionId != null) {
      return getCaseExecution();
    }
    else {
      return null;
    }
  }

  protected TaskEntity getTask() {
    if (taskId != null) {
      return Context.getCommandContext().getTaskManager().findTaskById(taskId);
    }
    else {
      return null;
    }
  }

  //sequence counter ///////////////////////////////////////////////////////////

  public long getSequenceCounter() {
    return sequenceCounter;
  }

  public void setSequenceCounter(long sequenceCounter) {
    this.sequenceCounter = sequenceCounter;
  }

   public void incrementSequenceCounter() {
    sequenceCounter++;
  }


  public boolean isConcurrentLocal() {
    return isConcurrentLocal;
  }

  public void setConcurrentLocal(boolean isConcurrentLocal) {
    this.isConcurrentLocal = isConcurrentLocal;
  }

  @Override
  public void onImplicitValueUpdate(final TypedValue updatedValue) {
    // note: this implementation relies on the
    //   behavior that the variable scope
    //   of variable value can never become null

    ProcessApplicationReference targetProcessApplication = getContextProcessApplication();
    if (targetProcessApplication != null) {
      Context.executeWithinProcessApplication(() -> {
        getVariableScope().setVariableLocal(name, updatedValue);
        return null;
      }, targetProcessApplication, new InvocationContext(getExecution()));

    }
    else {
      if (!isTransient) {
        getVariableScope().setVariableLocal(name, updatedValue);
      }
    }
  }

  protected ProcessApplicationReference getContextProcessApplication() {
    if (taskId != null) {
      return ProcessApplicationContextUtil.getTargetProcessApplication(getTask());
    }
    else if (executionId != null) {
      return ProcessApplicationContextUtil.getTargetProcessApplication(getExecution());
    }
    else if (caseExecutionId != null) {
      return ProcessApplicationContextUtil.getTargetProcessApplication(getCaseExecution());
    }
    else {
      return null;
    }
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
      + "[id=" + id
      + ", revision=" + revision
      + ", name=" + name
      + ", processDefinitionId=" + processDefinitionId
      + ", processInstanceId=" + processInstanceId
      + ", executionId=" + executionId
      + ", caseInstanceId=" + caseInstanceId
      + ", caseExecutionId=" + caseExecutionId
      + ", taskId=" + taskId
      + ", activityInstanceId=" + activityInstanceId
      + ", tenantId=" + tenantId
      + ", longValue=" + longValue
      + ", doubleValue=" + doubleValue
      + ", textValue=" + textValue
      + ", textValue2=" + textValue2
      + ", byteArrayValueId=" + getByteArrayValueId()
      + ", configuration=" + configuration
      + ", isConcurrentLocal=" + isConcurrentLocal
      + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    VariableInstanceEntity other = (VariableInstanceEntity) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }

  /**
   * @param isTransient
   *          <code>true</code>, if the variable is not stored in the data base.
   *          Default is <code>false</code>.
   */
  public void setTransient(boolean isTransient) {
    this.isTransient = isTransient;
  }

  /**
   * @return <code>true</code>, if the variable is transient. A transient
   *         variable is not stored in the data base.
   */
  public boolean isTransient() {
    return isTransient;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public Set<String> getReferencedEntityIds() {
    return new HashSet<>();
  }

  @Override
  public Map<String, Class> getReferencedEntitiesIdAndClass() {
    Map<String, Class> referenceIdAndClass = new HashMap<>();

    if (processInstanceId != null){
      referenceIdAndClass.put(processInstanceId, ExecutionEntity.class);
    }
    if (executionId != null){
      referenceIdAndClass.put(executionId, ExecutionEntity.class);
    }
    if (caseInstanceId != null){
      referenceIdAndClass.put(caseInstanceId, CaseExecutionEntity.class);
    }
    if (caseExecutionId != null){
      referenceIdAndClass.put(caseExecutionId, CaseExecutionEntity.class);
    }
    if (getByteArrayValueId() != null){
      referenceIdAndClass.put(getByteArrayValueId(), ByteArrayEntity.class);
    }

    return referenceIdAndClass;
  }

  /**
   *
   * @return <code>true</code> <code>processDefinitionId</code> is introduced in 7.13,
   * the check is used to created missing history at {@link LegacyBehavior#createMissingHistoricVariables(org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl) LegacyBehavior#createMissingHistoricVariables}
   */
  public boolean wasCreatedBefore713() {
    return this.getProcessDefinitionId() == null;
  }
}
