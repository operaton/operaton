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

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmmn.CaseControlRule;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnActivityExecution;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;

import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.ACTIVE;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.AVAILABLE;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.COMPLETED;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.DISABLED;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.ENABLED;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.SUSPENDED;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.TERMINATED;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE;

/**
 * @author Roman Smirnov
 *
 */
public abstract class StageOrTaskActivityBehavior extends PlanItemDefinitionActivityBehavior {

  protected static final CmmnBehaviorLogger LOG = ProcessEngineLogger.CMNN_BEHAVIOR_LOGGER;
  private static final String TRANSITION_COMPLETE = "complete";
  private static final String TRANSITION_DISABLE = "disable";
  private static final String TRANSITION_ENABLE = "enable";
  private static final String TRANSITION_EXIT = "exit";
  private static final String TRANSITION_PARENT_RESUME = "parentResume";
  private static final String TRANSITION_PARENT_SUSPEND = "parentSuspend";
  private static final String TRANSITION_PARENT_SUSPENSION = "parentSuspension";
  private static final String TRANSITION_PARENT_TERMINATE = "parentTerminate";
  private static final String TRANSITION_RESUME = "resume";
  private static final String TRANSITION_RE_ENABLE = "re-enable";
  private static final String TRANSITION_START = "start";
  private static final String TRANSITION_SUSPEND = "suspend";
  private static final String TRANSITION_TERMINATE = "terminate";
  private static final String TRANSITION_MANUAL_START = "manualStart";

  // creation /////////////////////////////////////////////////////////

  @Override
  protected void creating(CmmnActivityExecution execution) {
    evaluateRequiredRule(execution);
  }

  @Override
  public void created(CmmnActivityExecution execution) {
    if (execution.isAvailable() && isAtLeastOneEntryCriterionSatisfied(execution)) {
      fireEntryCriteria(execution);
    }
  }

  // enable ////////////////////////////////////////////////////////////

  @Override
  public void onEnable(CmmnActivityExecution execution) {
    ensureNotCaseInstance(execution, TRANSITION_ENABLE);
    ensureTransitionAllowed(execution, AVAILABLE, ENABLED, TRANSITION_ENABLE);
  }

  // re-enable /////////////////////////////////////////////////////////

  @Override
  public void onReenable(CmmnActivityExecution execution) {
    ensureNotCaseInstance(execution, TRANSITION_RE_ENABLE);
    ensureTransitionAllowed(execution, DISABLED, ENABLED, TRANSITION_RE_ENABLE);
  }

  // disable ///////////////////////////////////////////////////////////

  @Override
  public void onDisable(CmmnActivityExecution execution) {
    ensureNotCaseInstance(execution, TRANSITION_DISABLE);
    ensureTransitionAllowed(execution, ENABLED, DISABLED, TRANSITION_DISABLE);
  }

  // start /////////////////////////////////////////////////////////////

  @Override
  public void onStart(CmmnActivityExecution execution) {
    ensureNotCaseInstance(execution, TRANSITION_START);
    ensureTransitionAllowed(execution, AVAILABLE, ACTIVE, TRANSITION_START);
  }

  @Override
  public void onManualStart(CmmnActivityExecution execution) {
    ensureNotCaseInstance(execution, TRANSITION_MANUAL_START);
    ensureTransitionAllowed(execution, ENABLED, ACTIVE, TRANSITION_START);
  }

  @Override
  public void started(CmmnActivityExecution execution) {
    // only perform start behavior, when this case execution is
    // still active.
    // it can happen that a exit sentry will be triggered, so that
    // the given case execution will be terminated, in that case we
    // do not need to perform the start behavior
    if (execution.isActive()) {
      performStart(execution);
    }
  }

  protected abstract void performStart(CmmnActivityExecution execution);

  // completion ////////////////////////////////////////////////////////

  @Override
  public void onCompletion(CmmnActivityExecution execution) {
    ensureTransitionAllowed(execution, ACTIVE, COMPLETED, TRANSITION_COMPLETE);
    completing(execution);
  }

  @Override
  public void onManualCompletion(CmmnActivityExecution execution) {
    ensureTransitionAllowed(execution, ACTIVE, COMPLETED, TRANSITION_COMPLETE);
    manualCompleting(execution);
  }

  // termination //////////////////////////////////////////////////////

  @Override
  public void onTermination(CmmnActivityExecution execution) {
    ensureTransitionAllowed(execution, ACTIVE, TERMINATED, TRANSITION_TERMINATE);
    performTerminate(execution);
  }

  @Override
  public void onParentTermination(CmmnActivityExecution execution) {
    String id = execution.getId();
    throw LOG.illegalStateTransitionException(TRANSITION_PARENT_TERMINATE, id, getTypeName());
  }

  @Override
  public void onExit(CmmnActivityExecution execution) {
    String id = execution.getId();

    if (execution.isTerminated()) {
      throw LOG.alreadyTerminatedException(TRANSITION_EXIT, id);
    }

    if (execution.isCompleted()) {
      throw LOG.wrongCaseStateException(TRANSITION_EXIT, id, "[available|enabled|disabled|active|failed|suspended]", "completed");
    }

    performExit(execution);
  }

  // suspension ///////////////////////////////////////////////////////////

  @Override
  public void onSuspension(CmmnActivityExecution execution) {
    ensureTransitionAllowed(execution, ACTIVE, SUSPENDED, TRANSITION_SUSPEND);
    performSuspension(execution);
  }

  @Override
  public void onParentSuspension(CmmnActivityExecution execution) {
    ensureNotCaseInstance(execution, TRANSITION_PARENT_SUSPENSION);

    String id = execution.getId();

    if (execution.isSuspended()) {
      throw LOG.alreadySuspendedException(TRANSITION_PARENT_SUSPEND, id);
    }

    if (execution.isCompleted() || execution.isTerminated()) {
      throw LOG.wrongCaseStateException(TRANSITION_PARENT_SUSPEND, id, TRANSITION_SUSPEND, "[available|enabled|disabled|active]",
        execution.getCurrentState().toString());
    }

    performParentSuspension(execution);
  }

  // resume /////////////////////////////////////////////////////////////////

  @Override
  public void onResume(CmmnActivityExecution execution) {
    ensureNotCaseInstance(execution, TRANSITION_RESUME);
    ensureTransitionAllowed(execution, SUSPENDED, ACTIVE, TRANSITION_RESUME);

    CmmnActivityExecution parent = execution.getParent();
    if (parent != null && !parent.isActive()) {
      String id = execution.getId();
      throw LOG.resumeInactiveCaseException(TRANSITION_RESUME, id);
    }

    resuming(execution);

  }

  @Override
  public void onParentResume(CmmnActivityExecution execution) {
    ensureNotCaseInstance(execution, TRANSITION_PARENT_RESUME);
    String id = execution.getId();

    if (!execution.isSuspended()) {
      throw LOG.wrongCaseStateException(TRANSITION_PARENT_RESUME, id, TRANSITION_RESUME, "suspended", execution.getCurrentState().toString());
    }

    CmmnActivityExecution parent = execution.getParent();
    if (parent != null && !parent.isActive()) {
      throw LOG.resumeInactiveCaseException(TRANSITION_PARENT_RESUME, id);
    }

    resuming(execution);

  }

  // occur ////////////////////////////////////////////////////////

  @Override
  public void onOccur(CmmnActivityExecution execution) {
    String id = execution.getId();
    throw LOG.illegalStateTransitionException("occur", id, getTypeName());
  }

  // sentry ///////////////////////////////////////////////////////////////

  @Override
  public void fireEntryCriteria(CmmnActivityExecution execution) {
    boolean manualActivation = evaluateManualActivationRule(execution);
    if (manualActivation) {
      execution.enable();

    } else {
      execution.start();
    }
  }

  // manual activation rule //////////////////////////////////////////////

  protected boolean evaluateManualActivationRule(CmmnActivityExecution execution) {
    boolean manualActivation = false;
    CmmnActivity activity = execution.getActivity();
    Object manualActivationRule = activity.getProperty(PROPERTY_MANUAL_ACTIVATION_RULE);
    if (manualActivationRule != null) {
      CaseControlRule rule = (CaseControlRule) manualActivationRule;
      manualActivation = rule.evaluate(execution);
    }
    return manualActivation;
  }

  // helper ///////////////////////////////////////////////////////////

  protected abstract String getTypeName();
}
