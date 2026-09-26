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
package org.operaton.bpm.engine.impl.cmmn.execution;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineServices;
import org.operaton.bpm.engine.delegate.CmmnModelExecutionContext;
import org.operaton.bpm.engine.delegate.ProcessEngineServicesAware;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnBehaviorLogger;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.engine.impl.core.variable.CoreVariableInstance;
import org.operaton.bpm.engine.impl.core.variable.scope.SimpleVariableInstance;
import org.operaton.bpm.engine.impl.core.variable.scope.SimpleVariableInstance.SimpleVariableInstanceFactory;
import org.operaton.bpm.engine.impl.core.variable.scope.VariableInstanceFactory;
import org.operaton.bpm.engine.impl.core.variable.scope.VariableInstanceLifecycleListener;
import org.operaton.bpm.engine.impl.core.variable.scope.VariableStore;
import org.operaton.bpm.engine.impl.pvm.PvmProcessDefinition;
import org.operaton.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.cmmn.instance.CmmnElement;

import static java.util.Objects.requireNonNull;

/**
 * @author Roman Smirnov
 *
 */
public @NullMarked class CaseExecutionImpl extends CmmnExecution implements Serializable {

  protected static final CmmnBehaviorLogger LOG = ProcessEngineLogger.CMNN_BEHAVIOR_LOGGER;
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static final VariableInstanceFactory<CoreVariableInstance> VARIABLE_INSTANCE_FACTORY = (VariableInstanceFactory) new SimpleVariableInstanceFactory();

  @Serial private static final long serialVersionUID = 1L;

  // current position /////////////////////////////////////////////////////////

  private @Nullable List<CaseExecutionImpl> caseExecutions;

  private @Nullable List<CaseSentryPartImpl> caseSentryParts;

  protected @Nullable CaseExecutionImpl caseInstance;

  protected @Nullable CaseExecutionImpl parent;

  protected @Nullable ExecutionImpl subProcessInstance;

  protected @Nullable ExecutionImpl superExecution;

  protected @Nullable CaseExecutionImpl subCaseInstance;

  protected @Nullable CaseExecutionImpl superCaseExecution;

  // variables ////////////////////////////////////////////////////////////////

  protected transient VariableStore<SimpleVariableInstance> variableStore = new VariableStore<>();

  // case definition id ///////////////////////////////////////////////////////

  @Override
  public @Nullable String getCaseDefinitionId() {
    return getCaseDefinition().getId();
  }

  // parent ////////////////////////////////////////////////////////////////////

  @Override
  public @Nullable CaseExecutionImpl getParent() {
    return parent;
  }

  @Override
  public void setParent(CmmnExecution parent) {
    this.parent = (CaseExecutionImpl) parent;
  }

  @Override
  public @Nullable String getParentId() {
    return getParent() != null ? getParent().getId() : null;
  }

  // activity //////////////////////////////////////////////////////////////////

  @Override
  public @Nullable String getActivityId() {
    return getActivity() != null ? getActivity().getId() : null;
  }

  @Override
  public @Nullable String getActivityName() {
    return getActivity() != null ? getActivity().getName() : null;
  }

  // case executions ////////////////////////////////////////////////////////////////

  @Override
  public List<CaseExecutionImpl> getCaseExecutions() {
    return new ArrayList<>(getCaseExecutionsInternal());
  }

  @Override
  protected List<CaseExecutionImpl> getCaseExecutionsInternal() {
    if (caseExecutions == null) {
      caseExecutions = new ArrayList<>();
    }
    return caseExecutions;
  }

  // case instance /////////////////////////////////////////////////////////////

  @Override
  public @Nullable CaseExecutionImpl getCaseInstance() {
    return caseInstance;
  }

  @Override
  public void setCaseInstance(@Nullable CmmnExecution caseInstance) {
    this.caseInstance = (CaseExecutionImpl) caseInstance;
  }

  // super execution /////////////////////////////////////////////////////////////

  @Override
  public @Nullable ExecutionImpl getSuperExecution() {
    return superExecution;
  }

  @Override
  public void setSuperExecution(@Nullable PvmExecutionImpl superExecution) {
    this.superExecution = (ExecutionImpl) superExecution;
  }

  // sub process instance ////////////////////////////////////////////////////////

  @Override
  public @Nullable ExecutionImpl getSubProcessInstance() {
    return subProcessInstance;
  }

  @Override
  public void setSubProcessInstance(@Nullable PvmExecutionImpl subProcessInstance) {
    this.subProcessInstance = (ExecutionImpl) subProcessInstance;
  }

  @Override
  public PvmExecutionImpl createSubProcessInstance(PvmProcessDefinition processDefinition) {
    return createSubProcessInstance(processDefinition, null);
  }

  @Override
  public PvmExecutionImpl createSubProcessInstance(PvmProcessDefinition processDefinition, @Nullable String businessKey) {
    return createSubProcessInstance(processDefinition, businessKey, getCaseInstanceId());
  }

  @Override
  public PvmExecutionImpl createSubProcessInstance(PvmProcessDefinition processDefinition, @Nullable String businessKey, @Nullable String caseInstanceId) {
    ExecutionImpl subProcess = (ExecutionImpl) processDefinition.createProcessInstance(businessKey, caseInstanceId);

    // manage bidirectional super-subprocess relation
    subProcess.setSuperCaseExecution(this);
    setSubProcessInstance(subProcess);

    return subProcess;
  }

  // sub-/super- case instance ////////////////////////////////////////////////////

  @Override
  public @Nullable CaseExecutionImpl getSubCaseInstance() {
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
  public CaseExecutionImpl createSubCaseInstance(CmmnCaseDefinition caseDefinition, @Nullable String businessKey) {
    CaseExecutionImpl result = (CaseExecutionImpl) caseDefinition.createCaseInstance(businessKey);

    // manage bidirectional super-sub-case-instances relation
    requireNonNull(subCaseInstance).setSuperCaseExecution(this);
    setSubCaseInstance(subCaseInstance);

    return result;
  }

  @Override
  public @Nullable CaseExecutionImpl getSuperCaseExecution() {
    return superCaseExecution;
  }

  @Override
  public void setSuperCaseExecution(CmmnExecution superCaseExecution) {
    this.superCaseExecution = (CaseExecutionImpl) superCaseExecution;
  }

  // sentry /////////////////////////////////////////////////////////////////////////

  @Override
  public List<CaseSentryPartImpl> getCaseSentryParts() {
    if (caseSentryParts == null) {
      caseSentryParts = new ArrayList<>();
    }
    return caseSentryParts;
  }

  @Override
  protected Map<String, List<CmmnSentryPart>> getSentries() {
    Map<String, List<CmmnSentryPart>> sentries = new HashMap<>();

    for (CaseSentryPartImpl sentryPart : getCaseSentryParts()) {
      String sentryId = sentryPart.getSentryId();
      sentries
        .computeIfAbsent(sentryId, k -> new ArrayList<>())
        .add(sentryPart);
    }

    return sentries;
  }

  @Override
  protected List<CaseSentryPartImpl> findSentry(String sentryId) {
    List<CaseSentryPartImpl> result = new ArrayList<>();

    for (CaseSentryPartImpl sentryPart : getCaseSentryParts()) {
      if (sentryPart.getSentryId().equals(sentryId)) {
        result.add(sentryPart);
      }
    }

    return result;
  }

  @Override
  protected void addSentryPart(CmmnSentryPart sentryPart) {
    getCaseSentryParts().add((CaseSentryPartImpl) sentryPart);
  }

  @Override
  protected CmmnSentryPart newSentryPart() {
    return new CaseSentryPartImpl();
  }

  // new case executions ////////////////////////////////////////////////////////////

  @Override
  protected CaseExecutionImpl createCaseExecution(CmmnActivity activity) {
    CaseExecutionImpl child = newCaseExecution();

    // set activity to execute
    child.setActivity(activity);

    // handle child/parent-relation
    child.setParent(this);
    getCaseExecutionsInternal().add(child);

    // set case instance
    child.setCaseInstance(getCaseInstance());

    // set case definition
    child.setCaseDefinition(getCaseDefinition());

    return child;
  }

  @Override
  protected CaseExecutionImpl newCaseExecution() {
    return new CaseExecutionImpl();
  }

  // variables //////////////////////////////////////////////////////////////

  @SuppressWarnings({"unchecked","rawtypes"})
  protected VariableStore<CoreVariableInstance> getVariableStore() {
    return (VariableStore) variableStore;
  }

  @Override
  protected VariableInstanceFactory<CoreVariableInstance> getVariableInstanceFactory() {
    return VARIABLE_INSTANCE_FACTORY;
  }

  @Override
  protected List<VariableInstanceLifecycleListener<CoreVariableInstance>> getVariableInstanceLifecycleListeners() {
    return Collections.emptyList();
  }

  // toString /////////////////////////////////////////////////////////////////

  @Override
  public String toString() {
    if (isCaseInstanceExecution()) {
      return "CaseInstance[%s]".formatted(getToStringIdentity());
    } else {
      return "CmmnExecution[%s]".formatted(getToStringIdentity());
    }
  }

  @Override
  protected String getToStringIdentity() {
    return Integer.toString(System.identityHashCode(this));
  }

  @Override
  public String getId() {
    return String.valueOf(System.identityHashCode(this));
  }

  @Override
  public ProcessEngineServices getProcessEngineServices() {
    throw LOG.unsupportedTransientOperationException(ProcessEngineServicesAware.class.getName());
  }

  @Override
  public ProcessEngine getProcessEngine() {
    throw LOG.unsupportedTransientOperationException(ProcessEngineServicesAware.class.getName());
  }

  @Override
  public CmmnElement getCmmnModelElementInstance() {
    throw LOG.unsupportedTransientOperationException(CmmnModelExecutionContext.class.getName());
  }

  @Override
  public CmmnModelInstance getCmmnModelInstance() {
    throw LOG.unsupportedTransientOperationException(CmmnModelExecutionContext.class.getName());
  }
}
