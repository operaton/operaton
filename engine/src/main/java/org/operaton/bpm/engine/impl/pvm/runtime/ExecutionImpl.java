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
package org.operaton.bpm.engine.impl.pvm.runtime;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineServices;
import org.operaton.bpm.engine.delegate.BpmnModelExecutionContext;
import org.operaton.bpm.engine.delegate.ProcessEngineServicesAware;
import org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionImpl;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnExecution;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.engine.impl.core.variable.CoreVariableInstance;
import org.operaton.bpm.engine.impl.core.variable.scope.SimpleVariableInstance.SimpleVariableInstanceFactory;
import org.operaton.bpm.engine.impl.core.variable.scope.VariableInstanceFactory;
import org.operaton.bpm.engine.impl.core.variable.scope.VariableInstanceLifecycleListener;
import org.operaton.bpm.engine.impl.core.variable.scope.VariableStore;
import org.operaton.bpm.engine.impl.pvm.PvmProcessInstance;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.FlowElement;


/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Daniel Meyer
 * @author Falko Menge
 */
public class ExecutionImpl extends PvmExecutionImpl implements
        Serializable,
        ActivityExecution,
        PvmProcessInstance {

  @Serial private static final long serialVersionUID = 1L;

  private static final AtomicInteger idGenerator = new AtomicInteger();

  // current position /////////////////////////////////////////////////////////

  /** the process instance.  this is the root of the execution tree.
   * the processInstance of a process instance is a self reference. */
  protected ExecutionImpl processInstance;

  /** the parent execution */
  protected ExecutionImpl parent;

  /** nested executions representing scopes or concurrent paths */
  protected List<ExecutionImpl> executions;

  /** super execution, not-null if this execution is part of a subprocess */
  protected ExecutionImpl superExecution;

  /** reference to a subprocessinstance, not-null if currently subprocess is started from this execution */
  protected ExecutionImpl subProcessInstance;

  /** super case execution, not-null if this execution is part of a case execution */
  protected CaseExecutionImpl superCaseExecution;

  /** reference to a subcaseinstance, not-null if currently subcase is started from this execution */
  protected CaseExecutionImpl subCaseInstance;

  // variables/////////////////////////////////////////////////////////////////

  protected VariableStore<CoreVariableInstance> variableStore = new VariableStore<>();

  // lifecycle methods ////////////////////////////////////////////////////////

  /** creates a new execution. properties processDefinition, processInstance and activity will be initialized. */
  @Override
  public ExecutionImpl createExecution() {
    // create the new child execution
    ExecutionImpl createdExecution = newExecution();

    // initialize sequence counter
    createdExecution.setSequenceCounter(getSequenceCounter());

    // manage the bidirectional parent-child relation
    createdExecution.setParent(this);

    // initialize the new execution
    createdExecution.setProcessDefinition(getProcessDefinition());
    createdExecution.setProcessInstance(getProcessInstance());
    createdExecution.setActivity(getActivity());

    // make created execution start in same activity instance
    createdExecution.activityInstanceId = activityInstanceId;

    // with the fix of CAM-9249 we presume that the parent and the child have the same startContext
    createdExecution.setStartContext(scopeInstantiationContext);

    createdExecution.skipCustomListeners = this.skipCustomListeners;
    createdExecution.skipIoMapping = this.skipIoMapping;

    return createdExecution;
  }

  /** instantiates a new execution.  can be overridden by subclasses */
  @Override
  protected ExecutionImpl newExecution() {
    return new ExecutionImpl();
  }

  @Override
  public void initialize() {
    // nothing to do
  }

  @Override
  public void initializeTimerDeclarations() {
    // nothing to do
  }

  // parent ///////////////////////////////////////////////////////////////////

  /** ensures initialization and returns the parent */
  @Override
  public ExecutionImpl getParent() {
    return parent;
  }

  @Override
  public void setParentExecution(PvmExecutionImpl parent) {
    this.parent = (ExecutionImpl) parent;
  }

  // executions ///////////////////////////////////////////////////////////////

  @Override
  public List<ExecutionImpl> getExecutionsAsCopy() {
    return new ArrayList<>(getExecutions());
  }

  /** ensures initialization and returns the non-null executions list */
  @Override
  public List<ExecutionImpl> getExecutions() {
    if(executions == null) {
      executions = new ArrayList<>();
    }
    return executions;
  }

  @Override
  public ExecutionImpl getSuperExecution() {
    return superExecution;
  }

  @Override
  public void setSuperExecution(PvmExecutionImpl superExecution) {
    this.superExecution = (ExecutionImpl) superExecution;
    if (superExecution != null) {
      superExecution.setSubProcessInstance(null);
    }
  }

  @Override
  public ExecutionImpl getSubProcessInstance() {
    return subProcessInstance;
  }

  @Override
  public void setSubProcessInstance(PvmExecutionImpl subProcessInstance) {
    this.subProcessInstance = (ExecutionImpl) subProcessInstance;
  }

  // super case execution /////////////////////////////////////////////////////

  @Override
  public CaseExecutionImpl getSuperCaseExecution() {
    return superCaseExecution;
  }

  @Override
  public void setSuperCaseExecution(CmmnExecution superCaseExecution) {
    this.superCaseExecution = (CaseExecutionImpl) superCaseExecution;
  }

  // sub case execution ////////////////////////////////////////////////////////

  @Override
  public CaseExecutionImpl getSubCaseInstance() {
    return subCaseInstance;
  }

  @Override
  public void setSubCaseInstance(CmmnExecution subCaseInstance) {
    this.subCaseInstance = (CaseExecutionImpl) subCaseInstance;
  }

  @Override
  public CaseExecutionImpl createSubCaseInstance(CmmnCaseDefinition caseDefinition) {
    return createSubCaseInstance(caseDefinition, null);
  }

  @Override
  public CaseExecutionImpl createSubCaseInstance(CmmnCaseDefinition caseDefinition, String businessKey) {
    CaseExecutionImpl caseInstance = (CaseExecutionImpl) caseDefinition.createCaseInstance(businessKey);

    // manage bidirectional super-process-sub-case-instances relation
    subCaseInstance.setSuperExecution(this);
    setSubCaseInstance(subCaseInstance);

    return caseInstance;
  }

  // process definition ///////////////////////////////////////////////////////

  @Override
  public String getProcessDefinitionId() {
    return processDefinition.getId();
  }

  // process instance /////////////////////////////////////////////////////////

  /** ensures initialization and returns the process instance. */
  @Override
  public ExecutionImpl getProcessInstance() {
    return processInstance;
  }

  @Override
  public String getProcessInstanceId() {
    return getProcessInstance().getId();
  }

  @Override
  public String getBusinessKey() {
    return getProcessInstance().getBusinessKey();
  }

  @Override
  public void setBusinessKey(String businessKey) {
    this.businessKey = businessKey;
  }

  @Override
  public String getProcessBusinessKey() {
    return getProcessInstance().getBusinessKey();
  }

  /** for setting the process instance, this setter must be used as subclasses can override */
  @Override
  public void setProcessInstance(PvmExecutionImpl processInstance) {
    this.processInstance = (ExecutionImpl) processInstance;
  }

  // activity /////////////////////////////////////////////////////////////////

  /**
   * generates an activity instance id
   */
  @Override
  protected String generateActivityInstanceId(String activityId) {
    int nextId = idGenerator.incrementAndGet();
    String compositeId = activityId+":"+nextId;
    if(compositeId.length()>64) {
      return String.valueOf(nextId);
    } else {
      return compositeId;
    }
  }

  // toString /////////////////////////////////////////////////////////////////

  @Override
  public String toString() {
    if (isProcessInstanceExecution()) {
      return "ProcessInstance["+getToStringIdentity()+"]";
    } else {
      return (isEventScope? "EventScope":"")+(isConcurrent? "Concurrent" : "")+(isScope() ? "Scope" : "")+"Execution["+getToStringIdentity()+"]";
    }
  }

  @Override
  protected String getToStringIdentity() {
    return Integer.toString(System.identityHashCode(this));
  }

  // allow for subclasses to expose a real id /////////////////////////////////

  @Override
  public String getId() {
    return String.valueOf(System.identityHashCode(this));
  }

  // getters and setters //////////////////////////////////////////////////////

  protected VariableStore<CoreVariableInstance> getVariableStore() {
    return variableStore;
  }

  @Override
  protected VariableInstanceFactory<CoreVariableInstance> getVariableInstanceFactory() {
    return (VariableInstanceFactory) SimpleVariableInstanceFactory.INSTANCE;
  }

  @Override
  protected List<VariableInstanceLifecycleListener<CoreVariableInstance>> getVariableInstanceLifecycleListeners() {
    return Collections.emptyList();
  }

  @Override
  public ExecutionImpl getReplacedBy() {
    return (ExecutionImpl) replacedBy;
  }

  public void setExecutions(List<ExecutionImpl> executions) {
    this.executions = executions;
  }

  @Override
  public String getCurrentActivityName() {
    String currentActivityName = null;
    if (this.activity != null) {
      currentActivityName = (String) activity.getProperty("name");
    }
    return currentActivityName;
  }

  @Override
  public FlowElement getBpmnModelElementInstance() {
    throw new UnsupportedOperationException(BpmnModelExecutionContext.class.getName() +" is unsupported in transient ExecutionImpl");
  }

  @Override
  public BpmnModelInstance getBpmnModelInstance() {
    throw new UnsupportedOperationException(BpmnModelExecutionContext.class.getName() +" is unsupported in transient ExecutionImpl");
  }

  @Override
  public ProcessEngineServices getProcessEngineServices() {
    throw new UnsupportedOperationException(ProcessEngineServicesAware.class.getName() +" is unsupported in transient ExecutionImpl");
  }

  @Override
  public ProcessEngine getProcessEngine() {
    throw new UnsupportedOperationException(ProcessEngineServicesAware.class.getName() +" is unsupported in transient ExecutionImpl");
  }

  @Override
  public void forceUpdate() {
    // nothing to do
  }

  @Override
  public void fireHistoricProcessStartEvent() {
    // do nothing
  }

  @Override
  protected void removeVariablesLocalInternal() {
    // do nothing
  }

}
