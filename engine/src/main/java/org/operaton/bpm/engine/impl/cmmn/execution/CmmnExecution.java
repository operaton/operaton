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
import java.util.*;

import org.operaton.bpm.engine.delegate.CaseVariableListener;
import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.delegate.VariableListener;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnBehaviorLogger;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseSentryPartEntity;
import org.operaton.bpm.engine.impl.cmmn.model.*;
import org.operaton.bpm.engine.impl.cmmn.operation.CmmnAtomicOperation;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.core.instance.CoreExecution;
import org.operaton.bpm.engine.impl.core.variable.event.VariableEvent;
import org.operaton.bpm.engine.impl.core.variable.scope.AbstractVariableScope;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity.TaskState;
import org.operaton.bpm.engine.impl.pvm.PvmException;
import org.operaton.bpm.engine.impl.pvm.PvmProcessDefinition;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;
import org.operaton.bpm.engine.impl.task.TaskDecorator;
import org.operaton.bpm.engine.impl.variable.listener.CaseVariableListenerInvocation;
import org.operaton.bpm.engine.impl.variable.listener.DelegateCaseVariableInstanceImpl;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.*;
import static org.operaton.bpm.engine.impl.cmmn.model.CmmnSentryDeclaration.IF_PART;
import static org.operaton.bpm.engine.impl.cmmn.model.CmmnSentryDeclaration.PLAN_ITEM_ON_PART;
import static org.operaton.bpm.engine.impl.cmmn.model.CmmnSentryDeclaration.VARIABLE_ON_PART;
import static org.operaton.bpm.engine.impl.cmmn.operation.CmmnAtomicOperation.*;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureInstanceOf;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Roman Smirnov
 *
 */
public abstract class CmmnExecution extends CoreExecution implements CmmnCaseInstance {

  protected static final CmmnBehaviorLogger LOG = ProcessEngineLogger.CMNN_BEHAVIOR_LOGGER;

  @Serial private static final long serialVersionUID = 1L;

  protected transient CmmnCaseDefinition caseDefinition;

  // current position //////////////////////////////////////

  /** current activity */
  protected transient CmmnActivity activity;

  protected boolean required;

  protected int previousState;

  protected int currentState = NEW.getStateCode();

  protected transient Queue<VariableEvent> variableEventsQueue;

  protected transient TaskEntity task;

  /**
   * This property will be used if <code>this</code>
   * {@link CmmnExecution} is in state {@link CaseExecutionState#NEW}
   * to note that an entry criterion is satisfied.
   */
  protected boolean entryCriterionSatisfied;

  protected CmmnExecution() {
  }

  // plan items ///////////////////////////////////////////////////////////////

  protected abstract List<? extends CmmnExecution> getCaseExecutionsInternal();

  @Override
  public CmmnExecution findCaseExecution(String activityId) {
    if ((getActivity()!=null) && (getActivity().getId().equals(activityId))) {
     return this;
   }
   for (CmmnExecution nestedExecution : getCaseExecutions()) {
     CmmnExecution result = nestedExecution.findCaseExecution(activityId);
     if (result != null) {
       return result;
     }
   }
   return null;
  }

  // task /////////////////////////////////////////////////////////////////////

  public TaskEntity getTask() {
    return this.task;
  }

  public void setTask(Task task) {
    this.task = (TaskEntity) task;
  }

  @Override
  public TaskEntity createTask(TaskDecorator taskDecorator) {
    TaskEntity taskEntity = new TaskEntity((CaseExecutionEntity) this);
    taskEntity.insert();

    setTask(taskEntity);
    taskDecorator.decorate(taskEntity, this);

    // task decoration is part of the initialization of the task,
    // so we transition to CREATED only afterwards
    taskEntity.transitionTo(TaskState.STATE_CREATED);

    return taskEntity;
  }

  // super execution  ////////////////////////////////////////////////////////

  public abstract PvmExecutionImpl getSuperExecution();

  public abstract void setSuperExecution(PvmExecutionImpl superExecution);

  // sub process instance ////////////////////////////////////////////////////

  public abstract PvmExecutionImpl getSubProcessInstance();

  public abstract void setSubProcessInstance(PvmExecutionImpl subProcessInstance);

  @Override
  public abstract PvmExecutionImpl createSubProcessInstance(PvmProcessDefinition processDefinition);

  @Override
  public abstract PvmExecutionImpl createSubProcessInstance(PvmProcessDefinition processDefinition, String businessKey);

  @Override
  public abstract PvmExecutionImpl createSubProcessInstance(PvmProcessDefinition processDefinition, String businessKey, String caseInstanceId);

  // sub-/super- case instance ////////////////////////////////////////////////////

  public abstract CmmnExecution getSubCaseInstance();

  public abstract void setSubCaseInstance(CmmnExecution subCaseInstance);

  @Override
  public abstract CmmnExecution createSubCaseInstance(CmmnCaseDefinition caseDefinition);

  @Override
  public abstract CmmnExecution createSubCaseInstance(CmmnCaseDefinition caseDefinition, String businessKey);

  public abstract CmmnExecution getSuperCaseExecution();

  public abstract void setSuperCaseExecution(CmmnExecution superCaseExecution);

  // sentry //////////////////////////////////////////////////////////////////

  // sentry: (1) create and initialize sentry parts

  protected abstract CmmnSentryPart newSentryPart();

  protected abstract void addSentryPart(CmmnSentryPart sentryPart);

  @Override
  public void createSentryParts() {
    CmmnActivity cmmnActivity = getActivity();
    ensureNotNull("Case execution '%s': has no current activity".formatted(id), "activity", cmmnActivity);

    List<CmmnSentryDeclaration> sentries = cmmnActivity.getSentries();

    if (sentries != null && !sentries.isEmpty()) {

      for (CmmnSentryDeclaration sentryDeclaration : sentries) {

        CmmnIfPartDeclaration ifPartDeclaration = sentryDeclaration.getIfPart();
        if (ifPartDeclaration != null) {
          CmmnSentryPart ifPart = createIfPart(sentryDeclaration, ifPartDeclaration);
          addSentryPart(ifPart);
        }

        List<CmmnOnPartDeclaration> onPartDeclarations = sentryDeclaration.getOnParts();

        for (CmmnOnPartDeclaration onPartDeclaration : onPartDeclarations) {
          CmmnSentryPart onPart = createOnPart(sentryDeclaration, onPartDeclaration);
          addSentryPart(onPart);
        }

        List<CmmnVariableOnPartDeclaration> variableOnPartDeclarations = sentryDeclaration.getVariableOnParts();
        for(CmmnVariableOnPartDeclaration variableOnPartDeclaration: variableOnPartDeclarations) {
          CmmnSentryPart variableOnPart = createVariableOnPart(sentryDeclaration, variableOnPartDeclaration);
          addSentryPart(variableOnPart);
        }

      }
    }
  }

  protected CmmnSentryPart createOnPart(CmmnSentryDeclaration sentryDeclaration, CmmnOnPartDeclaration onPartDeclaration) {
    CmmnSentryPart sentryPart = createSentryPart(sentryDeclaration, PLAN_ITEM_ON_PART);

    // set the standard event
    String standardEvent = onPartDeclaration.getStandardEvent();
    sentryPart.setStandardEvent(standardEvent);

    // set source case execution
    CmmnActivity source = onPartDeclaration.getSource();
    ensureNotNull("The source of sentry '%s' is null.".formatted(sentryDeclaration.getId()), "source", source);

    String sourceActivityId = source.getId();
    sentryPart.setSource(sourceActivityId);

    // TODO: handle also sentryRef!!! (currently not implemented on purpose)

    return sentryPart;
  }

  @SuppressWarnings("unused")
  protected CmmnSentryPart createIfPart(CmmnSentryDeclaration sentryDeclaration, CmmnIfPartDeclaration ifPartDeclaration) {
    return createSentryPart(sentryDeclaration, IF_PART);
  }

  protected CmmnSentryPart createVariableOnPart(CmmnSentryDeclaration sentryDeclaration, CmmnVariableOnPartDeclaration variableOnPartDeclaration) {
    CmmnSentryPart sentryPart = createSentryPart(sentryDeclaration, VARIABLE_ON_PART);

    // set the variable event
    String variableEvent = variableOnPartDeclaration.getVariableEvent();
    sentryPart.setVariableEvent(variableEvent);

    // set the variable name
    String variableName = variableOnPartDeclaration.getVariableName();
    sentryPart.setVariableName(variableName);

    return sentryPart;
  }

  protected CmmnSentryPart createSentryPart(CmmnSentryDeclaration sentryDeclaration, String type) {
    CmmnSentryPart newSentryPart = newSentryPart();

    // set the type
    newSentryPart.setType(type);

    // set the case instance and case execution
    newSentryPart.setCaseInstance(getCaseInstance());
    newSentryPart.setCaseExecution(this);

    // set sentry id
    String sentryId = sentryDeclaration.getId();
    newSentryPart.setSentryId(sentryId);

    return newSentryPart;
  }

  // sentry: (2) handle transitions

  public void handleChildTransition(CmmnExecution child, String transition) {
    // Step 1: collect all affected sentries
    List<String> affectedSentries = collectAffectedSentries(child, transition);

    // Step 2: fire force update on all case sentry part
    // contained by an affected sentry to provoke an
    // OptimisticLockingException
    forceUpdateOnSentries(affectedSentries);

    // Step 3: check each affected sentry whether it is satisfied.
    // the returned list contains all satisfied sentries
    List<String> satisfiedSentries = getSatisfiedSentries(affectedSentries);

    // Step 4: reset sentries -> satisfied == false
    resetSentries(satisfiedSentries);

    // Step 5: fire satisfied sentries
    fireSentries(satisfiedSentries);

  }

  @Override
  public void fireIfOnlySentryParts() {
    // the following steps are a workaround, because setVariable()
    // does not check nor fire a sentry!!!
    Set<String> affectedSentries = new HashSet<>();
    List<CmmnSentryPart> sentryParts = collectSentryParts(getSentries());
    for (CmmnSentryPart sentryPart : sentryParts) {
      if (isNotSatisfiedIfPartOnly(sentryPart)) {
        affectedSentries.add(sentryPart.getSentryId());
      }
    }

    // Step 7: check each not affected sentry whether it is satisfied
    List<String> satisfiedSentries = getSatisfiedSentries(new ArrayList<>(affectedSentries));

    // Step 8: reset sentries -> satisfied == false
    resetSentries(satisfiedSentries);

    // Step 9: fire satisfied sentries
    fireSentries(satisfiedSentries);
  }

  public void handleVariableTransition(String variableName, String transition) {
    Map<String, List<CmmnSentryPart>> sentries = collectAllSentries();

    List<CmmnSentryPart> sentryParts = collectSentryParts(sentries);

    List<String> affectedSentries = collectAffectedSentriesWithVariableOnParts(variableName, transition, sentryParts);

    List<CmmnSentryPart> affectedSentryParts = getAffectedSentryParts(sentries,affectedSentries);
    forceUpdateOnCaseSentryParts(affectedSentryParts);

    List<String> allSentries = new ArrayList<>(sentries.keySet());

    List<String> satisfiedSentries = getSatisfiedSentriesInExecutionTree(allSentries, sentries);

    List<CmmnSentryPart> satisfiedSentryParts = getAffectedSentryParts(sentries, satisfiedSentries);
    resetSentryParts(satisfiedSentryParts);

    fireSentries(satisfiedSentries);

  }

  protected List<String> collectAffectedSentries(CmmnExecution child, String transition) {
    List<? extends CmmnSentryPart> sentryParts = getCaseSentryParts();

    List<String> affectedSentries = new ArrayList<>();

    for (CmmnSentryPart sentryPart : sentryParts) {

      // necessary for backward compatibility
      String sourceCaseExecutionId = sentryPart.getSourceCaseExecutionId();
      String sourceRef = sentryPart.getSource();
      if (child.getActivityId().equals(sourceRef) || child.getId().equals(sourceCaseExecutionId)) {

        String standardEvent = sentryPart.getStandardEvent();
        if (transition.equals(standardEvent)) {
          addIdIfNotSatisfied(affectedSentries, sentryPart);
        }
      }
    }

    return affectedSentries;
  }

  protected boolean isNotSatisfiedIfPartOnly(CmmnSentryPart sentryPart) {
    return IF_PART.equals(sentryPart.getType())
        && getSentries().get(sentryPart.getSentryId()).size() == 1
        && !sentryPart.isSatisfied();
  }

  protected void addIdIfNotSatisfied(List<String> affectedSentries, CmmnSentryPart sentryPart) {
    if (!sentryPart.isSatisfied()) {
      // if it is not already satisfied, then set the
      // current case sentry part to satisfied (=true).
      String sentryId = sentryPart.getSentryId();
      sentryPart.setSatisfied(true);

      // collect the id of affected sentry.
      if (!affectedSentries.contains(sentryId)) {
        affectedSentries.add(sentryId);
      }
    }
  }

  protected List<String> collectAffectedSentriesWithVariableOnParts(String variableName, String variableEvent, List<CmmnSentryPart> sentryParts) {

    List<String> affectedSentries = new ArrayList<>();

    for (CmmnSentryPart sentryPart : sentryParts) {

      String sentryVariableName = sentryPart.getVariableName();
      String sentryVariableEvent = sentryPart.getVariableEvent();
      CmmnExecution execution = sentryPart.getCaseExecution();
      if (VARIABLE_ON_PART.equals(sentryPart.getType()) && sentryVariableName.equals(variableName)
        && sentryVariableEvent.equals(variableEvent)
        && !hasVariableWithSameNameInParent(execution, sentryVariableName)) {

        addIdIfNotSatisfied(affectedSentries, sentryPart);
      }
    }

    return affectedSentries;
  }

  protected boolean hasVariableWithSameNameInParent(CmmnExecution execution, String variableName) {
    while(execution != null) {
      if (execution.getId().equals(getId())) {
        return false;
      }
      TypedValue variableTypedValue = execution.getVariableLocalTyped(variableName);
      if (variableTypedValue != null) {
        return true;
      }
      execution = execution.getParent();
    }
    return false;
  }

  protected Map<String,List<CmmnSentryPart>> collectAllSentries() {
    Map<String,List<CmmnSentryPart>> sentries = new HashMap<>();
    List<? extends CmmnExecution> caseExecutions = getCaseExecutions();
    for(CmmnExecution caseExecution: caseExecutions) {
      sentries.putAll(caseExecution.collectAllSentries());
    }
    sentries.putAll(getSentries());
    return sentries;
  }

  protected List<CmmnSentryPart> getAffectedSentryParts(Map<String,List<CmmnSentryPart>> allSentries, List<String> affectedSentries) {
    List<CmmnSentryPart> affectedSentryParts = new ArrayList<>();
    for(String affectedSentryId: affectedSentries) {
      affectedSentryParts.addAll(allSentries.get(affectedSentryId));
    }
    return affectedSentryParts;
  }

  protected List<CmmnSentryPart> collectSentryParts(Map<String,List<CmmnSentryPart>> sentries) {
   return sentries.values().stream()
           .flatMap(Collection::stream)
           .toList();
  }

  protected void forceUpdateOnCaseSentryParts(List<CmmnSentryPart> sentryParts) {
    // set for each case sentry part forceUpdate flag to true to provoke
    // an OptimisticLockingException if different case sentry parts of the
    // same sentry has been satisfied concurrently.
    for (CmmnSentryPart sentryPart : sentryParts) {
      if (sentryPart instanceof CaseSentryPartEntity sentryPartEntity) {
        sentryPartEntity.forceUpdate();
      }
    }
  }

  /**
   * Checks for each given sentry id whether the corresponding
   * sentry is satisfied.
   */
  protected List<String> getSatisfiedSentries(List<String> sentryIds) {
    List<String> result = new ArrayList<>();

    if (sentryIds != null) {

      for (String sentryId : sentryIds) {

        if (isSentrySatisfied(sentryId)) {
          result.add(sentryId);
        }
      }
    }

    return result;
  }

  /**
   * Checks for each given sentry id in the execution tree whether the corresponding
   * sentry is satisfied.
   */
  protected List<String> getSatisfiedSentriesInExecutionTree(List<String> sentryIds, Map<String, List<CmmnSentryPart>> allSentries) {
    List<String> result = new ArrayList<>();

    if (sentryIds != null) {

      for (String sentryId : sentryIds) {
        List<CmmnSentryPart> sentryParts = allSentries.get(sentryId);
        if (isSentryPartsSatisfied(sentryId, sentryParts)) {
          result.add(sentryId);
        }
      }
    }

    return result;
  }

  protected void forceUpdateOnSentries(List<String> sentryIds) {
    for (String sentryId : sentryIds) {
      List<? extends CmmnSentryPart> sentryParts = findSentry(sentryId);
      // set for each case sentry part forceUpdate flag to true to provoke
      // an OptimisticLockingException if different case sentry parts of the
      // same sentry has been satisfied concurrently.
      for (CmmnSentryPart sentryPart : sentryParts) {
        if (sentryPart instanceof CaseSentryPartEntity sentryPartEntity) {
          sentryPartEntity.forceUpdate();
        }
      }
    }
  }

  protected void resetSentries(List<String> sentries) {
    for (String sentry : sentries) {
      List<CmmnSentryPart> parts = getSentries().get(sentry);
      for (CmmnSentryPart part : parts) {
        part.setSatisfied(false);
      }
    }
  }

  protected void resetSentryParts(List<CmmnSentryPart> parts) {
    for (CmmnSentryPart part : parts) {
      part.setSatisfied(false);
    }
  }

  protected void fireSentries(List<String> satisfiedSentries) {
    if (satisfiedSentries != null && !satisfiedSentries.isEmpty()) {
      // if there are satisfied sentries, trigger the associated
      // case executions

      // 1. propagate to all child case executions ///////////////////////////////////////////

      // collect the execution tree.
      ArrayList<CmmnExecution> children = new ArrayList<>();
      collectCaseExecutionsInExecutionTree(children);

      for (CmmnExecution currentChild : children) {

        // check and fire first exitCriteria
        currentChild.checkAndFireExitCriteria(satisfiedSentries);

        // then trigger entryCriteria
        currentChild.checkAndFireEntryCriteria(satisfiedSentries);
      }

      // 2. check exit criteria of the case instance //////////////////////////////////////////

      if (isCaseInstanceExecution() && isActive()) {
        checkAndFireExitCriteria(satisfiedSentries);
      }

    }
  }

  protected void collectCaseExecutionsInExecutionTree(List<CmmnExecution> children) {
    for(CmmnExecution child: getCaseExecutions()) {
      child.collectCaseExecutionsInExecutionTree(children);
    }
    children.addAll(getCaseExecutions());
  }

  protected void checkAndFireExitCriteria(List<String> satisfiedSentries) {
    if (isActive()) {
      CmmnActivity cmmnActivity = getActivity();
      ensureNotNull(PvmException.class, "Case execution '%s': has no current activity.".formatted(getId()), "activity", cmmnActivity);

      // trigger first exitCriteria
      List<CmmnSentryDeclaration> exitCriteria = cmmnActivity.getExitCriteria();
      for (CmmnSentryDeclaration sentryDeclaration : exitCriteria) {

        if (sentryDeclaration != null && satisfiedSentries.contains(sentryDeclaration.getId())) {
          fireExitCriteria();
          break;
        }
      }
    }
  }

  protected void checkAndFireEntryCriteria(List<String> satisfiedSentries) {
    if (isAvailable() || isNew()) {
      // do that only, when this child case execution
      // is available

      CmmnActivity cmmnActivity = getActivity();
      ensureNotNull(PvmException.class, "Case execution '%s': has no current activity.".formatted(getId()), "activity", cmmnActivity);

      List<CmmnSentryDeclaration> criteria = cmmnActivity.getEntryCriteria();
      for (CmmnSentryDeclaration sentryDeclaration : criteria) {
        if (sentryDeclaration != null && satisfiedSentries.contains(sentryDeclaration.getId())) {
          if (isAvailable()) {
            fireEntryCriteria();
          }
          else {
            entryCriterionSatisfied = true;
          }
          break;
        }
      }
    }
  }

  public void fireExitCriteria() {
    performOperation(CASE_EXECUTION_FIRE_EXIT_CRITERIA);
  }

  public void fireEntryCriteria() {
    performOperation(CASE_EXECUTION_FIRE_ENTRY_CRITERIA);
  }

  // sentry: (3) helper

  public abstract List<? extends CmmnSentryPart> getCaseSentryParts();

  protected abstract List<? extends CmmnSentryPart> findSentry(String sentryId);

  protected abstract Map<String, List<CmmnSentryPart>> getSentries();

  @Override
  public boolean isSentrySatisfied(String sentryId) {
    List<? extends CmmnSentryPart> sentryParts = findSentry(sentryId);
    return isSentryPartsSatisfied(sentryId, sentryParts);

  }

  protected boolean isSentryPartsSatisfied(String sentryId, List<? extends CmmnSentryPart> sentryParts) {
    if (sentryParts == null || sentryParts.isEmpty()) {
      return true;
    }

    // if part will be evaluated in the end
    CmmnSentryPart ifPart = null;
    for (CmmnSentryPart sentryPart : sentryParts) {
      if (PLAN_ITEM_ON_PART.equals(sentryPart.getType())) {
        if (!sentryPart.isSatisfied()) {
          return false;
        }
      } else if (VARIABLE_ON_PART.equals(sentryPart.getType())) {
        if (!sentryPart.isSatisfied()) {
          return false;
        }
      } else { /* IF_PART.equals(sentryPart.getType) == true */
        ifPart = sentryPart;
        // once the ifPart has been satisfied the whole sentry is satisfied
        if (ifPart.isSatisfied()) {
          return true;
        }
      }
    }

    // if all onParts are satisfied and there is no
    // ifPart then the whole sentry is satisfied.
    if (ifPart == null) {
      return true;
    }

    // therefore evaluate the ifPart
    Boolean booleanResult = isIfPartSatisfied(sentryId, ifPart);
    ifPart.setSatisfied(booleanResult);
    return booleanResult;
  }

  private Boolean isIfPartSatisfied(String sentryId, CmmnSentryPart ifPart) {
    CmmnExecution execution = ifPart.getCaseExecution();
    ensureNotNull("Case execution of sentry '%s': is null".formatted(ifPart.getSentryId()), execution);

    CmmnActivity cmmnActivity = ifPart.getCaseExecution().getActivity();
    ensureNotNull("Case execution '%s': has no current activity".formatted(id), "activity", cmmnActivity);

    CmmnSentryDeclaration sentryDeclaration = cmmnActivity.getSentry(sentryId);
    ensureNotNull("Case execution '%s': has no declaration for sentry '%s'".formatted(id, sentryId), "sentryDeclaration", sentryDeclaration);

    CmmnIfPartDeclaration ifPartDeclaration = sentryDeclaration.getIfPart();
    ensureNotNull("Sentry declaration '%s' has no defined ifPart, but there should be one defined for case execution '%s'.".formatted(
      sentryId, id), "ifPartDeclaration", ifPartDeclaration);

    Expression condition = ifPartDeclaration.getCondition();
    ensureNotNull("A condition was expected for ifPart of Sentry declaration '%s' for case execution '%s'.".formatted(
      sentryId, id), "condition", condition);

    Object result = condition.getValue(this);
    ensureInstanceOf("condition expression returns non-Boolean", "result", result, Boolean.class);

    return (Boolean) result;
  }

  protected boolean containsIfPartAndExecutionActive(String sentryId, Map<String,List<CmmnSentryPart>> sentries) {
    List<? extends CmmnSentryPart> sentryParts = sentries.get(sentryId);

    for (CmmnSentryPart part : sentryParts) {
      CmmnExecution caseExecution = part.getCaseExecution();
      if (IF_PART.equals(part.getType()) && caseExecution != null
          && caseExecution.isActive()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean isEntryCriterionSatisfied() {
    return entryCriterionSatisfied;
  }

  // business key ////////////////////////////////////////////////////////////

  @Override
  public String getCaseBusinessKey() {
    return getCaseInstance().getBusinessKey();
  }

  @Override
  public String getBusinessKey() {
    if (this.isCaseInstanceExecution()) {
      return businessKey;
    } else {
      return getCaseBusinessKey();
    }
  }

  // case definition ///////////////////////////////////////////////////////

  public CmmnCaseDefinition getCaseDefinition() {
    return caseDefinition;
  }

  public void setCaseDefinition(CmmnCaseDefinition caseDefinition) {
    this.caseDefinition = caseDefinition;
  }

  // case instance /////////////////////////////////////////////////////////

  /** ensures initialization and returns the process instance. */
  public abstract CmmnExecution getCaseInstance();

  public abstract void setCaseInstance(CmmnExecution caseInstance);

  @Override
  public boolean isCaseInstanceExecution() {
    return getParent() == null;
  }

  // case instance id /////////////////////////////////////////////////////////

  /** ensures initialization and returns the process instance. */
  @Override
  public String getCaseInstanceId() {
    return getCaseInstance().getId();
  }

  // parent ///////////////////////////////////////////////////////////////////

  /** ensures initialization and returns the parent */
  @Override
  public abstract CmmnExecution getParent();

  public abstract void setParent(CmmnExecution parent);

  // activity /////////////////////////////////////////////////////////////////

  /** ensures initialization and returns the activity */
  @Override
  public CmmnActivity getActivity() {
    return activity;
  }

  public void setActivity(CmmnActivity activity) {
    this.activity = activity;
  }

  // variables ////////////////////////////////////////////

  @Override
  public String getVariableScopeKey() {
    return "caseExecution";
  }

  @Override
  public AbstractVariableScope getParentVariableScope() {
    return getParent();
  }

  //delete/remove /////////////////////////////////////////////////////

  public void deleteCascade() {
   performOperation(CASE_EXECUTION_DELETE_CASCADE);
  }

  @Override
  public void remove() {
   CmmnExecution parent = getParent();
   if (parent!=null) {
     parent.getCaseExecutionsInternal().remove(this);
   }
  }

  // required //////////////////////////////////////////////////

  @Override
  public boolean isRequired() {
    return required;
  }

  @Override
  public void setRequired(boolean required) {
    this.required = required;
  }

  // state /////////////////////////////////////////////////////

  @Override
  public CaseExecutionState getCurrentState() {
    return CaseExecutionState.CASE_EXECUTION_STATES.get(getState());
  }

  @Override
  public void setCurrentState(CaseExecutionState currentState) {
    if (!isSuspending() && !isTerminating()) {
      // do not reset the previous state, if this case execution
      // is currently terminating or suspending. otherwise the
      // "real" previous state is lost.
      previousState = this.currentState;
    }
    this.currentState = currentState.getStateCode();
  }

  public int getState() {
    return currentState;
  }

  public void setState(int state) {
    this.currentState = state;
  }

  @Override
  public boolean isNew() {
    return currentState == NEW.getStateCode();
  }

  @Override
  public boolean isAvailable() {
    return currentState == AVAILABLE.getStateCode();
  }

  @Override
  public boolean isEnabled() {
    return currentState == ENABLED.getStateCode();
  }

  @Override
  public boolean isDisabled() {
    return currentState == DISABLED.getStateCode();
  }

  @Override
  public boolean isActive() {
    return currentState == ACTIVE.getStateCode();
  }

  @Override
  public boolean isCompleted() {
    return currentState == COMPLETED.getStateCode();
  }

  @Override
  public boolean isSuspended() {
    return currentState == SUSPENDED.getStateCode();
  }

  @Override
  public boolean isSuspending() {
    return currentState == SUSPENDING_ON_SUSPENSION.getStateCode()
        || currentState == SUSPENDING_ON_PARENT_SUSPENSION.getStateCode();
  }

  @Override
  public boolean isTerminated() {
    return currentState == TERMINATED.getStateCode();
  }

  @Override
  public boolean isTerminating() {
    return currentState == TERMINATING_ON_TERMINATION.getStateCode()
        || currentState == TERMINATING_ON_PARENT_TERMINATION.getStateCode()
        || currentState == TERMINATING_ON_EXIT.getStateCode();
  }

  @Override
  public boolean isFailed() {
    return currentState == FAILED.getStateCode();
  }

  @Override
  public boolean isClosed() {
    return currentState == CLOSED.getStateCode();
  }

  // previous state /////////////////////////////////////////////

  @Override
  public CaseExecutionState getPreviousState() {
    return CaseExecutionState.CASE_EXECUTION_STATES.get(getPrevious());
  }

  public int getPrevious() {
    return previousState;
  }

  public void setPrevious(int previous) {
    this.previousState = previous;
  }

  // state transition ///////////////////////////////////////////

  @Override
  public void create() {
    create(null);
  }

  @Override
  public void create(Map<String, Object> variables) {
    if(variables != null) {
      setVariables(variables);
    }

    performOperation(CASE_INSTANCE_CREATE);
  }

  @Override
  public List<CmmnExecution> createChildExecutions(List<CmmnActivity> activities) {
    List<CmmnExecution> children = new ArrayList<>();

    // first create new child case executions
    for (CmmnActivity currentActivity : activities) {
      CmmnExecution child = createCaseExecution(currentActivity);
      children.add(child);
    }

    return children;
  }


  @Override
  public void triggerChildExecutionsLifecycle(List<CmmnExecution> children) {
    // then notify create listener for each created
    // child case execution
    for (CmmnExecution child : children) {

      if (isActive()) {
        if (child.isNew()) {
          child.performOperation(CASE_EXECUTION_CREATE);
        }
      } else {
        // if this case execution is not active anymore,
        // then stop notifying create listener and executing
        // of each child case execution
        break;
      }
    }
  }

  protected abstract CmmnExecution createCaseExecution(CmmnActivity activity);

  protected abstract CmmnExecution newCaseExecution();

  @Override
  public void enable() {
    performOperation(CASE_EXECUTION_ENABLE);
  }

  @Override
  public void disable() {
    performOperation(CASE_EXECUTION_DISABLE);
  }

  @Override
  public void reenable() {
    performOperation(CASE_EXECUTION_RE_ENABLE);
  }

  @Override
  public void manualStart() {
    performOperation(CASE_EXECUTION_MANUAL_START);
  }

  @Override
  public void start() {
    performOperation(CASE_EXECUTION_START);
  }

  @Override
  public void complete() {
    performOperation(CASE_EXECUTION_COMPLETE);
  }

  @Override
  public void manualComplete() {
    performOperation(CASE_EXECUTION_MANUAL_COMPLETE);
  }

  @Override
  public void occur() {
    performOperation(CASE_EXECUTION_OCCUR);
  }

  @Override
  public void terminate() {
    performOperation(CASE_EXECUTION_TERMINATING_ON_TERMINATION);
  }

  @Override
  public void performTerminate() {
    performOperation(CASE_EXECUTION_TERMINATE);
  }

  @Override
  public void parentTerminate() {
    performOperation(CASE_EXECUTION_TERMINATING_ON_PARENT_TERMINATION);
  }

  @Override
  public void performParentTerminate() {
    performOperation(CASE_EXECUTION_PARENT_TERMINATE);
  }

  @Override
  public void exit() {
    performOperation(CASE_EXECUTION_TERMINATING_ON_EXIT);
  }

  @Override
  public void parentComplete() {
    performOperation(CmmnAtomicOperation.CASE_EXECUTION_PARENT_COMPLETE);
  }

  @Override
  public void performExit() {
    performOperation(CASE_EXECUTION_EXIT);
  }

  @Override
  public void suspend() {
    performOperation(CASE_EXECUTION_SUSPENDING_ON_SUSPENSION);
  }

  @Override
  public void performSuspension() {
    performOperation(CASE_EXECUTION_SUSPEND);
  }

  @Override
  public void parentSuspend() {
    performOperation(CASE_EXECUTION_SUSPENDING_ON_PARENT_SUSPENSION);
  }

  @Override
  public void performParentSuspension() {
    performOperation(CASE_EXECUTION_PARENT_SUSPEND);
  }

  @Override
  public void resume() {
    performOperation(CASE_EXECUTION_RESUME);
  }

  @Override
  public void parentResume() {
    performOperation(CASE_EXECUTION_PARENT_RESUME);
  }

  @Override
  public void reactivate() {
    performOperation(CASE_EXECUTION_RE_ACTIVATE);
  }

  @Override
  public void close() {
    performOperation(CASE_INSTANCE_CLOSE);
  }

  // variable listeners
  @Override
  public void dispatchEvent(VariableEvent variableEvent) {
    boolean invokeCustomListeners =
        Context
          .getProcessEngineConfiguration()
          .isInvokeCustomVariableListeners();

    Map<String, List<VariableListener<?>>> listeners = getActivity()
        .getVariableListeners(variableEvent.getEventName(), invokeCustomListeners);

    // only attempt to invoke listeners if there are any (as this involves resolving the upwards execution hierarchy)
    if (!listeners.isEmpty()) {
      getCaseInstance().queueVariableEvent(variableEvent, invokeCustomListeners);
    }
  }

  protected void queueVariableEvent(VariableEvent variableEvent, boolean includeCustomerListeners) {

    Queue<VariableEvent> queue = getVariableEventQueue();

    queue.add(variableEvent);

    // if this is the first event added, trigger listener invocation
    if (queue.size() == 1) {
      invokeVariableListeners(includeCustomerListeners);
    }
  }

  protected void invokeVariableListeners(boolean includeCustomerListeners) {
    Queue<VariableEvent> eventQueue = getVariableEventQueue();

    while (!eventQueue.isEmpty()) {
      // do not remove the event yet, as otherwise new events will immediately be dispatched
      VariableEvent nextEvent = eventQueue.peek();

      CmmnExecution sourceExecution = (CmmnExecution) nextEvent.getSourceScope();

      DelegateCaseVariableInstanceImpl delegateVariable =
          DelegateCaseVariableInstanceImpl.fromVariableInstance(nextEvent.getVariableInstance());
      delegateVariable.setEventName(nextEvent.getEventName());
      delegateVariable.setSourceExecution(sourceExecution);

      Map<String, List<VariableListener<?>>> listenersByActivity =
          sourceExecution.getActivity().getVariableListeners(delegateVariable.getEventName(), includeCustomerListeners);

      CmmnExecution currentExecution = sourceExecution;

      while (currentExecution != null) {
        String activityId = currentExecution.getActivityId();
        List<VariableListener<?>> listeners = activityId != null ? listenersByActivity.get(activityId) : null;

        if (listeners != null) {
          delegateVariable.setScopeExecution(currentExecution);
          for (VariableListener<?> listener : listeners) {
            tryHandleInvocation((CaseVariableListener) listener, delegateVariable, currentExecution);
          }
        }

        currentExecution = currentExecution.getParent();
      }

      // finally remove the event from the queue
      eventQueue.remove();
    }
  }

  private static void tryHandleInvocation(CaseVariableListener caseVariableListener,
                                DelegateCaseVariableInstanceImpl delegateVariable,
                                CmmnExecution currentExecution) {
    try {
      CaseVariableListenerInvocation invocation = new CaseVariableListenerInvocation(caseVariableListener,
          delegateVariable, currentExecution);
      Context.getProcessEngineConfiguration()
        .getDelegateInterceptor()
        .handleInvocation(invocation);
    } catch (Exception e) {
      throw LOG.invokeVariableListenerException(e);
    }
  }

  protected Queue<VariableEvent> getVariableEventQueue() {
    if (variableEventsQueue == null) {
      variableEventsQueue = new LinkedList<>();
    }

    return variableEventsQueue;
  }

  // toString() //////////////////////////////////////  ///////////

  @Override
  public String toString() {
    if (isCaseInstanceExecution()) {
      return "CaseInstance[%s]".formatted(getToStringIdentity());
    } else {
      return "CmmnExecution[%s]".formatted(getToStringIdentity());
    }
  }

  protected String getToStringIdentity() {
    return id;
  }

}
