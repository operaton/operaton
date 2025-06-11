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
package org.operaton.bpm.engine.impl.cmmn.behavior;

import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.AVAILABLE;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.COMPLETED;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.SUSPENDED;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.TERMINATED;

import org.operaton.bpm.engine.exception.cmmn.CaseIllegalStateTransitionException;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnActivityExecution;

/**
 * @author Roman Smirnov
 *
 */
public abstract class EventListenerOrMilestoneActivityBehavior extends PlanItemDefinitionActivityBehavior {

  protected static final CmmnBehaviorLogger LOG = ProcessEngineLogger.CMNN_BEHAVIOR_LOGGER;

  // enable /////////////////////////////////////////////////////////////

  @Override
  public void onEnable(CmmnActivityExecution execution) {
    throw createIllegalStateTransitionException("enable", execution);
  }

  // re-enable //////////////////////////////////////////////////////////

  @Override
  public void onReenable(CmmnActivityExecution execution) {
    throw createIllegalStateTransitionException("reenable", execution);
  }

  // disable ///////////////////////////////////////////////////////////

  @Override
  public void onDisable(CmmnActivityExecution execution) {
    throw createIllegalStateTransitionException("disable", execution);
  }

  // start /////////////////////////////////////////////////////////////

  @Override
  public void onStart(CmmnActivityExecution execution) {
    throw createIllegalStateTransitionException("start", execution);
  }

  @Override
  public void onManualStart(CmmnActivityExecution execution) {
    throw createIllegalStateTransitionException("manualStart", execution);
  }

  // completion /////////////////////////////////////////////////////////

  @Override
  public void onCompletion(CmmnActivityExecution execution) {
    throw createIllegalStateTransitionException("complete", execution);
  }

  @Override
  public void onManualCompletion(CmmnActivityExecution execution) {
    throw createIllegalStateTransitionException("complete", execution);
  }

  // termination ////////////////////////////////////////////////////////

  @Override
  public void onTermination(CmmnActivityExecution execution) {
    ensureTransitionAllowed(execution, AVAILABLE, TERMINATED, "terminate");
    performTerminate(execution);
  }

  @Override
  public void onParentTermination(CmmnActivityExecution execution) {

    if (execution.isCompleted()) {
      String id = execution.getId();
      throw LOG.executionAlreadyCompletedException("parentTerminate", id);
    }

    performParentTerminate(execution);
  }

  @Override
  public void onExit(CmmnActivityExecution execution) {
    throw createIllegalStateTransitionException("exit", execution);
  }

  // occur /////////////////////////////////////////////////////////////////

  @Override
  public void onOccur(CmmnActivityExecution execution) {
    ensureTransitionAllowed(execution, AVAILABLE, COMPLETED, "occur");
  }

  // suspension ////////////////////////////////////////////////////////////

  @Override
  public void onSuspension(CmmnActivityExecution execution) {
    ensureTransitionAllowed(execution, AVAILABLE, SUSPENDED, "suspend");
    performSuspension(execution);
  }

  @Override
  public void onParentSuspension(CmmnActivityExecution execution) {
    throw createIllegalStateTransitionException("parentSuspend", execution);
  }

  // resume ////////////////////////////////////////////////////////////////

  @Override
  public void onResume(CmmnActivityExecution execution) {
    ensureTransitionAllowed(execution, SUSPENDED, AVAILABLE, "resume");

    CmmnActivityExecution parent = execution.getParent();
    if (parent != null && !parent.isActive()) {
      String id = execution.getId();
      throw LOG.resumeInactiveCaseException("resume", id);
    }

    resuming(execution);
  }

  @Override
  public void onParentResume(CmmnActivityExecution execution) {
    throw createIllegalStateTransitionException("parentResume", execution);
  }

  // re-activation ////////////////////////////////////////////////////////

  @Override
  public void onReactivation(CmmnActivityExecution execution) {
    throw createIllegalStateTransitionException("reactivate", execution);
  }

  // sentry ///////////////////////////////////////////////////////////////

  @SuppressWarnings("unused")
  protected boolean isAtLeastOneExitCriterionSatisfied(CmmnActivityExecution execution) {
    return false;
  }

  @Override
  public void fireExitCriteria(CmmnActivityExecution execution) {
    throw LOG.criteriaNotAllowedForEventListenerOrMilestonesException("exit", execution.getId());
  }

  // helper ////////////////////////////////////////////////////////////////

  protected CaseIllegalStateTransitionException createIllegalStateTransitionException(String transition, CmmnActivityExecution execution) {
    String id = execution.getId();
    return LOG.illegalStateTransitionException(transition, id, getTypeName());
  }

  protected abstract String getTypeName();

}
