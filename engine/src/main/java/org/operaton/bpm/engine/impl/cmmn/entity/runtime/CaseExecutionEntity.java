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
package org.operaton.bpm.engine.impl.cmmn.entity.runtime;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.ProcessEngineServices;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProvider;
import org.operaton.bpm.engine.impl.cfg.multitenancy.TenantIdProviderCaseInstanceContext;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionEntity;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnExecution;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnSentryPart;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.engine.impl.cmmn.operation.CmmnAtomicOperation;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.core.instance.CoreExecution;
import org.operaton.bpm.engine.impl.core.operation.CoreAtomicOperation;
import org.operaton.bpm.engine.impl.core.variable.CoreVariableInstance;
import org.operaton.bpm.engine.impl.core.variable.scope.*;
import org.operaton.bpm.engine.impl.core.variable.scope.VariableStore.VariablesProvider;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.HasDbReferences;
import org.operaton.bpm.engine.impl.db.HasDbRevision;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.operaton.bpm.engine.impl.history.producer.CmmnHistoryEventProducer;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.*;
import org.operaton.bpm.engine.impl.pvm.PvmProcessDefinition;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;
import org.operaton.bpm.engine.impl.task.TaskDecorator;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.type.ModelElementType;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_ACTIVITY_DESCRIPTION;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_ACTIVITY_TYPE;

import java.util.*;

/**
 * @author Roman Smirnov
 *
 */
public class CaseExecutionEntity extends CmmnExecution implements CaseExecution, CaseInstance, DbEntity, HasDbRevision, HasDbReferences, VariablesProvider<VariableInstanceEntity> {

  private static final long serialVersionUID = 1L;

  // current position /////////////////////////////////////////////////////////

  /** the case instance.  this is the root of the execution tree.
   * the caseInstance of a case instance is a self reference. */
  protected transient CaseExecutionEntity caseInstance;

  /** the parent execution */
  protected transient CaseExecutionEntity parent;

  /** nested executions */
  protected List<CaseExecutionEntity> caseExecutions;

  /** nested case sentry parts */
  protected List<CaseSentryPartEntity> caseSentryParts;
  protected Map<String, List<CmmnSentryPart>> sentries;

  /** reference to a sub process instance, not-null if currently subprocess is started from this execution */
  protected transient ExecutionEntity subProcessInstance;

  protected transient ExecutionEntity superExecution;

  protected transient CaseExecutionEntity subCaseInstance;

  protected transient CaseExecutionEntity superCaseExecution;

  // associated entities /////////////////////////////////////////////////////

  @SuppressWarnings({ "unchecked" })
  protected VariableStore<VariableInstanceEntity> variableStore = new VariableStore<>(
      this, new CaseExecutionEntityReferencer(this));

  // Persistence //////////////////////////////////////////////////////////////

  protected int revision = 1;
  protected String caseDefinitionId;
  protected String activityId;
  protected String caseInstanceId;
  protected String parentId;
  protected String superCaseExecutionId;
  protected String superExecutionId;

  // activity properites //////////////////////////////////////////////////////

  protected String activityName;
  protected String activityType;
  protected String activityDescription;

  // case definition ///////////////////////////////////////////////////////////

  @Override
  public String getCaseDefinitionId() {
    return caseDefinitionId;
  }

  /** ensures initialization and returns the case definition. */
  @Override
  public CmmnCaseDefinition getCaseDefinition() {
    ensureCaseDefinitionInitialized();
    return caseDefinition;
  }

  @Override
  public void setCaseDefinition(CmmnCaseDefinition caseDefinition) {
    super.setCaseDefinition(caseDefinition);

    caseDefinitionId = null;
    if (caseDefinition != null) {
      caseDefinitionId = caseDefinition.getId();
    }

  }

  protected void ensureCaseDefinitionInitialized() {
    if ((caseDefinition == null) && (caseDefinitionId != null)) {

      CaseDefinitionEntity deployedCaseDefinition = Context
        .getProcessEngineConfiguration()
        .getDeploymentCache()
        .getCaseDefinitionById(caseDefinitionId);

      setCaseDefinition(deployedCaseDefinition);
    }
  }

  // parent ////////////////////////////////////////////////////////////////////

  @Override
  public CaseExecutionEntity getParent() {
    ensureParentInitialized();
    return parent;
  }

  @Override
  public void setParent(CmmnExecution parent) {
    this.parent = (CaseExecutionEntity) parent;

    if (parent != null) {
      this.parentId = parent.getId();
    } else {
      this.parentId = null;
    }
  }

  protected void ensureParentInitialized() {
    if (parent == null && parentId != null) {
      if(isExecutionTreePrefetchEnabled()) {
        ensureCaseExecutionTreeInitialized();

      } else {
        parent = Context
            .getCommandContext()
            .getCaseExecutionManager()
            .findCaseExecutionById(parentId);

      }
    }
  }

  /**
   * @see ExecutionEntity#ensureExecutionTreeInitialized
   */
  protected void ensureCaseExecutionTreeInitialized() {
    List<CaseExecutionEntity> executions = Context.getCommandContext()
      .getCaseExecutionManager()
      .findChildCaseExecutionsByCaseInstanceId(caseInstanceId);

    CaseExecutionEntity caseExecutionInstance = null;

    Map<String, CaseExecutionEntity> executionMap = new HashMap<>();
    for (CaseExecutionEntity execution : executions) {
      execution.caseExecutions = new ArrayList<>();
      executionMap.put(execution.getId(), execution);
      if(execution.isCaseInstanceExecution()) {
        caseExecutionInstance = execution;
      }
    }

    for (CaseExecutionEntity execution : executions) {
      CaseExecutionEntity parent = executionMap.get(execution.getParentId());
      if(!execution.isCaseInstanceExecution()) {
        execution.caseInstance = caseExecutionInstance;
        execution.parent = parent;
        parent.caseExecutions.add(execution);
      } else {
        execution.caseInstance = execution;
      }
    }
  }

  /**
   * @return true if execution tree prefetching is enabled
   */
  protected boolean isExecutionTreePrefetchEnabled() {
    return Context.getProcessEngineConfiguration()
      .isExecutionTreePrefetchEnabled();
  }

  @Override
  public String getParentId() {
    return parentId;
  }

  // activity //////////////////////////////////////////////////////////////////

  @Override
  public CmmnActivity getActivity() {
    ensureActivityInitialized();
    return super.getActivity();
  }

  @Override
  public void setActivity(CmmnActivity activity) {
    super.setActivity(activity);
    if (activity != null) {
      this.activityId = activity.getId();
      this.activityName = activity.getName();
      this.activityType = getActivityProperty(activity, PROPERTY_ACTIVITY_TYPE);
      this.activityDescription = getActivityProperty(activity, PROPERTY_ACTIVITY_DESCRIPTION);
    } else {
      this.activityId = null;
      this.activityName = null;
      this.activityType = null;
      this.activityDescription = null;
    }
  }

  protected void ensureActivityInitialized() {
    if ((activity == null) && (activityId != null)) {
      setActivity(getCaseDefinition().findActivity(activityId));
    }
  }

  protected String getActivityProperty(CmmnActivity activity, String property) {
    String result = null;

    if (activity != null) {
      Object value = activity.getProperty(property);
      if (value != null && value instanceof String string) {
        result = string;
      }
    }

    return result;
  }

  // activity properties //////////////////////////////////////////////////////

  @Override
  public String getActivityId() {
    return activityId;
  }

  @Override
  public String getActivityName() {
    return activityName;
  }

  @Override
  public String getActivityType() {
    return activityType;
  }

  @Override
  public String getActivityDescription() {
    return activityDescription;
  }

  // case executions ////////////////////////////////////////////////////////////////

  @Override
  public List<CaseExecutionEntity> getCaseExecutions() {
    return new ArrayList<>(getCaseExecutionsInternal());
  }

  protected List<CaseExecutionEntity> getCaseExecutionsInternal() {
    ensureCaseExecutionsInitialized();
    return caseExecutions;
  }

  protected void ensureCaseExecutionsInitialized() {
    if (caseExecutions == null) {
      this.caseExecutions = Context
        .getCommandContext()
        .getCaseExecutionManager()
        .findChildCaseExecutionsByParentCaseExecutionId(id);
    }
  }

  // task ///////////////////////////////////////////////////////////////////

  @Override
  public TaskEntity getTask() {
    ensureTaskInitialized();
    return task;
  }

  protected void ensureTaskInitialized() {
    if (task == null) {
      task = Context
        .getCommandContext()
        .getTaskManager()
        .findTaskByCaseExecutionId(id);
    }
  }

  @Override
  public TaskEntity createTask(TaskDecorator taskDecorator) {
    TaskEntity task = super.createTask(taskDecorator);
    fireHistoricCaseActivityInstanceUpdate();
    return task;
  }

  // case instance /////////////////////////////////////////////////////////

  @Override
  public String getCaseInstanceId() {
    return caseInstanceId;
  }

  @Override
  public CaseExecutionEntity getCaseInstance() {
    ensureCaseInstanceInitialized();
    return caseInstance;
  }

  @Override
  public void setCaseInstance(CmmnExecution caseInstance) {
    this.caseInstance = (CaseExecutionEntity) caseInstance;

    if (caseInstance != null) {
      this.caseInstanceId = this.caseInstance.getId();
    }
  }

  protected void ensureCaseInstanceInitialized() {
    if ((caseInstance == null) && (caseInstanceId != null)) {

      caseInstance =  Context
        .getCommandContext()
        .getCaseExecutionManager()
        .findCaseExecutionById(caseInstanceId);

    }
  }

  @Override
  public boolean isCaseInstanceExecution() {
    return parentId == null;
  }

  @Override
  public void create(Map<String, Object> variables) {
    // determine tenant Id if null
    if(tenantId == null) {
      provideTenantId(variables);
    }
    super.create(variables);
  }

  protected void provideTenantId(Map<String, Object> variables) {
    TenantIdProvider tenantIdProvider = Context.getProcessEngineConfiguration().getTenantIdProvider();

    if(tenantIdProvider != null) {
      VariableMap variableMap = Variables.fromMap(variables);
      CaseDefinition caseDefinition = (CaseDefinition) getCaseDefinition();

      TenantIdProviderCaseInstanceContext ctx = null;

      if(superExecutionId != null) {
        ctx = new TenantIdProviderCaseInstanceContext(caseDefinition, variableMap, getSuperExecution());
      }
      else if(superCaseExecutionId != null) {
        ctx = new TenantIdProviderCaseInstanceContext(caseDefinition, variableMap, getSuperCaseExecution());
      }
      else {
        ctx = new TenantIdProviderCaseInstanceContext(caseDefinition, variableMap);
      }

      tenantId = tenantIdProvider.provideTenantIdForCaseInstance(ctx);
    }
  }

  protected CaseExecutionEntity createCaseExecution(CmmnActivity activity) {
    CaseExecutionEntity child = newCaseExecution();

    // set activity to execute
    child.setActivity(activity);

    // handle child/parent-relation
    child.setParent(this);
    getCaseExecutionsInternal().add(child);

    // set case instance
    child.setCaseInstance(getCaseInstance());

    // set case definition
    child.setCaseDefinition(getCaseDefinition());

    // inherit the tenant id from parent case execution
    if(tenantId != null) {
      child.setTenantId(tenantId);
    }

    return child;
  }

  protected CaseExecutionEntity newCaseExecution() {
    CaseExecutionEntity newCaseExecution = new CaseExecutionEntity();

    Context
      .getCommandContext()
      .getCaseExecutionManager()
      .insertCaseExecution(newCaseExecution);

    return newCaseExecution;
  }

  // super execution //////////////////////////////////////////////////////

  public String getSuperExecutionId() {
    return superExecutionId;
  }

  public void setSuperExecutionId(String superProcessExecutionId) {
    this.superExecutionId = superProcessExecutionId;
  }

  @Override
  public ExecutionEntity getSuperExecution() {
    ensureSuperExecutionInstanceInitialized();
    return superExecution;
  }

  @Override
  public void setSuperExecution(PvmExecutionImpl superExecution) {
    if (this.superExecutionId != null) {
      ensureSuperExecutionInstanceInitialized();
      this.superExecution.setSubCaseInstance(null);
    }

    this.superExecution = (ExecutionEntity) superExecution;

    if (superExecution != null) {
      this.superExecutionId = superExecution.getId();
      this.superExecution.setSubCaseInstance(this);
    } else {
      this.superExecutionId = null;
    }
  }

  protected void ensureSuperExecutionInstanceInitialized() {
    if (superExecution == null && superExecutionId != null) {
      superExecution = Context
        .getCommandContext()
        .getExecutionManager()
        .findExecutionById(superExecutionId);
    }
  }

  // sub process instance ///////////////////////////////////////////////////

  @Override
  public ExecutionEntity getSubProcessInstance() {
    ensureSubProcessInstanceInitialized();
    return subProcessInstance;
  }

  @Override
  public void setSubProcessInstance(PvmExecutionImpl subProcessInstance) {
    this.subProcessInstance = (ExecutionEntity) subProcessInstance;
  }

  @Override
  public ExecutionEntity createSubProcessInstance(PvmProcessDefinition processDefinition) {
    return createSubProcessInstance(processDefinition, null);
  }

  @Override
  public ExecutionEntity createSubProcessInstance(PvmProcessDefinition processDefinition, String businessKey) {
    return createSubProcessInstance(processDefinition, businessKey, getCaseInstanceId());
  }

  @Override
  public ExecutionEntity createSubProcessInstance(PvmProcessDefinition processDefinition, String businessKey, String caseInstanceId) {
    ExecutionEntity subProcess = (ExecutionEntity) processDefinition.createProcessInstance(businessKey, caseInstanceId);

    // inherit the tenant-id from the process definition
    String tenantId = ((ProcessDefinitionEntity) processDefinition).getTenantId();
    if (tenantId != null) {
      subProcess.setTenantId(tenantId);
    }
    else {
      // if process definition has no tenant id, inherit this case instance's tenant id
      subProcess.setTenantId(this.tenantId);
    }

    // manage bidirectional super-subprocess relation
    subProcess.setSuperCaseExecution(this);
    setSubProcessInstance(subProcess);

    fireHistoricCaseActivityInstanceUpdate();

    return subProcess;
  }

  protected void ensureSubProcessInstanceInitialized() {
    if (subProcessInstance == null) {
      subProcessInstance = Context
        .getCommandContext()
        .getExecutionManager()
        .findSubProcessInstanceBySuperCaseExecutionId(id);
    }
  }

  // sub-/super- case instance ////////////////////////////////////////////////////

  @Override
  public CaseExecutionEntity getSubCaseInstance() {
    ensureSubCaseInstanceInitialized();
    return subCaseInstance;
  }

  @Override
  public void setSubCaseInstance(CmmnExecution subCaseInstance) {
    this.subCaseInstance = (CaseExecutionEntity) subCaseInstance;
  }

  @Override
  public CaseExecutionEntity createSubCaseInstance(CmmnCaseDefinition caseDefinition) {
    return createSubCaseInstance(caseDefinition, null);
  }

  @Override
  public CaseExecutionEntity createSubCaseInstance(CmmnCaseDefinition caseDefinition, String businessKey) {
    CaseExecutionEntity subCase = (CaseExecutionEntity) caseDefinition.createCaseInstance(businessKey);

    // inherit the tenant-id from the case definition
    String tenantId = ((CaseDefinitionEntity) caseDefinition).getTenantId();
    if (tenantId != null) {
      subCase.setTenantId(tenantId);
    }
    else {
      // if case definition has no tenant id, inherit this case instance's tenant id
      subCase.setTenantId(this.tenantId);
    }

    // manage bidirectional super-sub-case-instances relation
    subCase.setSuperCaseExecution(this);
    setSubCaseInstance(subCase);

    fireHistoricCaseActivityInstanceUpdate();

    return subCase;
  }

  public void fireHistoricCaseActivityInstanceUpdate() {
    ProcessEngineConfigurationImpl configuration = Context.getProcessEngineConfiguration();
    HistoryLevel historyLevel = configuration.getHistoryLevel();
    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.CASE_ACTIVITY_INSTANCE_UPDATE, this)) {
      CmmnHistoryEventProducer eventProducer = configuration.getCmmnHistoryEventProducer();
      HistoryEventHandler eventHandler = configuration.getHistoryEventHandler();

      HistoryEvent event = eventProducer.createCaseActivityInstanceUpdateEvt(this);
      eventHandler.handleEvent(event);
    }
  }

  protected void ensureSubCaseInstanceInitialized() {
    if (subCaseInstance == null) {
      subCaseInstance = Context
        .getCommandContext()
        .getCaseExecutionManager()
        .findSubCaseInstanceBySuperCaseExecutionId(id);
    }
  }

  public String getSuperCaseExecutionId() {
    return superCaseExecutionId;
  }

  public void setSuperCaseExecutionId(String superCaseExecutionId) {
    this.superCaseExecutionId = superCaseExecutionId;
  }

  @Override
  public CmmnExecution getSuperCaseExecution() {
    ensureSuperCaseExecutionInitialized();
    return superCaseExecution;
  }

  @Override
  public void setSuperCaseExecution(CmmnExecution superCaseExecution) {
    this.superCaseExecution = (CaseExecutionEntity) superCaseExecution;

    if (superCaseExecution != null) {
      this.superCaseExecutionId = superCaseExecution.getId();
    } else {
      this.superCaseExecutionId = null;
    }
  }

  protected void ensureSuperCaseExecutionInitialized() {
    if (superCaseExecution == null && superCaseExecutionId != null) {
      superCaseExecution = Context
        .getCommandContext()
        .getCaseExecutionManager()
        .findCaseExecutionById(superCaseExecutionId);
    }
  }

  // sentry /////////////////////////////////////////////////////////////////////////

  @Override
  public List<CaseSentryPartEntity> getCaseSentryParts() {
    ensureCaseSentryPartsInitialized();
    return caseSentryParts;
  }

  protected void ensureCaseSentryPartsInitialized() {
    if (caseSentryParts == null) {

      caseSentryParts = Context
        .getCommandContext()
        .getCaseSentryPartManager()
        .findCaseSentryPartsByCaseExecutionId(id);

      // create a map sentries: sentryId -> caseSentryParts
      // for simple select to get all parts for one sentry
      sentries = new HashMap<>();

      for (CaseSentryPartEntity sentryPart : caseSentryParts) {
        String sentryId = sentryPart.getSentryId();
        sentries.computeIfAbsent(sentryId, k -> new ArrayList<>())
          .add(sentryPart);
      }
    }
  }

  protected void addSentryPart(CmmnSentryPart sentryPart) {
    CaseSentryPartEntity entity = (CaseSentryPartEntity) sentryPart;

    getCaseSentryParts().add(entity);

    String sentryId = sentryPart.getSentryId();
    List<CmmnSentryPart> parts = sentries.computeIfAbsent(sentryId, k -> new ArrayList<>());

    parts.add(entity);
  }

  protected Map<String, List<CmmnSentryPart>> getSentries() {
    ensureCaseSentryPartsInitialized();
    return sentries;
  }

  protected List<CmmnSentryPart> findSentry(String sentryId) {
    ensureCaseSentryPartsInitialized();
    return sentries.get(sentryId);
  }

  protected CaseSentryPartEntity newSentryPart() {
    CaseSentryPartEntity caseSentryPart = new CaseSentryPartEntity();

    Context
      .getCommandContext()
      .getCaseSentryPartManager()
      .insertCaseSentryPart(caseSentryPart);

    return caseSentryPart;
  }

  // variables //////////////////////////////////////////////////////////////

  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected VariableStore<CoreVariableInstance> getVariableStore() {
    return (VariableStore) variableStore;
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected VariableInstanceFactory<CoreVariableInstance> getVariableInstanceFactory() {
    return (VariableInstanceFactory) VariableInstanceEntityFactory.INSTANCE;
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected List<VariableInstanceLifecycleListener<CoreVariableInstance>> getVariableInstanceLifecycleListeners() {
    return Arrays.<VariableInstanceLifecycleListener<CoreVariableInstance>>asList((VariableInstanceLifecycleListener) VariableInstanceEntityPersistenceListener.INSTANCE,
        (VariableInstanceLifecycleListener) VariableInstanceSequenceCounterListener.INSTANCE,
        (VariableInstanceLifecycleListener) VariableInstanceHistoryListener.INSTANCE,
        (VariableInstanceLifecycleListener) CmmnVariableInvocationListener.INSTANCE,
        (VariableInstanceLifecycleListener) new VariableOnPartListener(this)
      );

  }

  @Override
  public Collection<VariableInstanceEntity> provideVariables() {
    return Context
      .getCommandContext()
      .getVariableInstanceManager()
      .findVariableInstancesByCaseExecutionId(id);
  }

  @Override
  public Collection<VariableInstanceEntity> provideVariables(Collection<String> variableNames) {
    return Context
      .getCommandContext()
      .getVariableInstanceManager()
      .findVariableInstancesByCaseExecutionIdAndVariableNames(id, variableNames);
  }

  // toString /////////////////////////////////////////////////////////////

  @Override
  public String toString() {
    if (isCaseInstanceExecution()) {
      return "CaseInstance["+getToStringIdentity()+"]";
    } else {
      return "CaseExecution["+getToStringIdentity()+"]";
    }
  }

  @Override
  protected String getToStringIdentity() {
    return id;
  }

  // delete/remove ///////////////////////////////////////////////////////

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void remove() {
    super.remove();

    for (VariableInstanceEntity variableInstance : variableStore.getVariables()) {
      invokeVariableLifecycleListenersDelete(variableInstance, this,
          Arrays.<VariableInstanceLifecycleListener<CoreVariableInstance>>asList((VariableInstanceLifecycleListener) VariableInstanceEntityPersistenceListener.INSTANCE));
      variableStore.removeVariable(variableInstance.getName());
    }

    CommandContext commandContext = Context.getCommandContext();

    for (CaseSentryPartEntity sentryPart : getCaseSentryParts()) {
      commandContext
        .getCaseSentryPartManager()
        .deleteSentryPart(sentryPart);
    }

    // finally delete this execution
    commandContext
      .getCaseExecutionManager()
      .deleteCaseExecution(this);
  }

  // persistence /////////////////////////////////////////////////////////

  @Override
  public int getRevision() {
    return revision;
  }

  @Override
  public void setRevision(int revision) {
    this.revision = revision;
  }

  @Override
  public int getRevisionNext() {
    return revision + 1;
  }

  public void forceUpdate() {
    Context.getCommandContext()
      .getDbEntityManager()
      .forceUpdate(this);
  }

  @Override
  public Set<String> getReferencedEntityIds() {
    Set<String> referenceIds = new HashSet<>();

    if (parentId != null) {
      referenceIds.add(parentId);
    }
    if (superCaseExecutionId != null) {
      referenceIds.add(superCaseExecutionId);
    }

    return referenceIds;
  }

  @Override
  public Map<String, Class> getReferencedEntitiesIdAndClass() {
    Map<String, Class> referenceIdAndClass = new HashMap<>();

    if (parentId != null) {
      referenceIdAndClass.put(parentId, CaseExecutionEntity.class);
    }
    if (superCaseExecutionId != null) {
      referenceIdAndClass.put(superCaseExecutionId, CaseExecutionEntity.class);
    }
    if (caseDefinitionId != null) {
      referenceIdAndClass.put(caseDefinitionId, CmmnCaseDefinition.class);
    }

    return referenceIdAndClass;
  }

  @Override
  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<>();
    persistentState.put("caseDefinitionId", caseDefinitionId);
    persistentState.put("businessKey", businessKey);
    persistentState.put("activityId", activityId);
    persistentState.put("parentId", parentId);
    persistentState.put("currentState", currentState);
    persistentState.put("previousState", previousState);
    persistentState.put("superExecutionId", superExecutionId);
    return persistentState;
  }

  @Override
  public CmmnModelInstance getCmmnModelInstance() {
    if(caseDefinitionId != null) {

      return Context.getProcessEngineConfiguration()
        .getDeploymentCache()
        .findCmmnModelInstanceForCaseDefinition(caseDefinitionId);

    } else {
      return null;

    }
  }

  @Override
  public CmmnElement getCmmnModelElementInstance() {
    CmmnModelInstance cmmnModelInstance = getCmmnModelInstance();
    if(cmmnModelInstance != null) {

      ModelElementInstance modelElementInstance = cmmnModelInstance.getModelElementById(activityId);

      try {
        return (CmmnElement) modelElementInstance;

      } catch(ClassCastException e) {
        ModelElementType elementType = modelElementInstance.getElementType();
        throw new ProcessEngineException("Cannot cast "+modelElementInstance+" to CmmnElement. "
            + "Is of type "+elementType.getTypeName() + " Namespace "
            + elementType.getTypeNamespace(), e);
      }

    } else {
      return null;
    }
  }

  @Override
  public ProcessEngineServices getProcessEngineServices() {
    return Context
        .getProcessEngineConfiguration()
        .getProcessEngine();
  }

  @Override
  public ProcessEngine getProcessEngine() {
    return Context.getProcessEngineConfiguration().getProcessEngine();
  }

  public String getCaseDefinitionTenantId() {
    CaseDefinitionEntity caseDefinition = (CaseDefinitionEntity) getCaseDefinition();
    return caseDefinition.getTenantId();
  }

  @Override
  public <T extends CoreExecution> void performOperation(CoreAtomicOperation<T> operation) {
    Context.getCommandContext()
      .performOperation((CmmnAtomicOperation) operation, this);
  }

  @Override
  public <T extends CoreExecution> void performOperationSync(CoreAtomicOperation<T> operation) {
    Context.getCommandContext()
      .performOperation((CmmnAtomicOperation) operation, this);
  }
}
