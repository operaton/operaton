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

import java.util.List;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnActivityExecution;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnExecution;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;

import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.*;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_AUTO_COMPLETE;
import static org.operaton.bpm.engine.impl.util.ActivityBehaviorUtil.getActivityBehavior;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureInstanceOf;

/**
 * @author Roman Smirnov
 *
 */
public class StageActivityBehavior extends StageOrTaskActivityBehavior implements CmmnCompositeActivityBehavior {

  protected static final CmmnBehaviorLogger LOG = ProcessEngineLogger.CMNN_BEHAVIOR_LOGGER;
  private static final String TRANSITION_REACTIVATE = "reactivate";
  private static final String TRANSITION_COMPLETE = "complete";

  // start /////////////////////////////////////////////////////////////////////

  @Override
  protected void performStart(CmmnActivityExecution execution) {
    CmmnActivity activity = execution.getActivity();
    List<CmmnActivity> childActivities = activity.getActivities();

    if (childActivities != null && !childActivities.isEmpty()) {
      List<CmmnExecution> children = execution.createChildExecutions(childActivities);
      execution.createSentryParts();
      execution.triggerChildExecutionsLifecycle(children);


      if (execution.isActive()) {
        execution.fireIfOnlySentryParts();
        // if "autoComplete == true" and there are no
        // required nor active child activities,
        // then the stage will be completed.
        if (execution.isActive()) {
          checkAndCompleteCaseExecution(execution);
        }
      }

    } else {
      execution.complete();
    }
  }

  // re-activation ////////////////////////////////////////////////////////////

  @Override
  public void onReactivation(CmmnActivityExecution execution) {
    String id = execution.getId();

    if (execution.isActive()) {
      throw LOG.alreadyActiveException(TRANSITION_REACTIVATE, id);
    }

    if (execution.isCaseInstanceExecution()) {
      if (execution.isClosed()) {
        throw LOG.alreadyClosedCaseException(TRANSITION_REACTIVATE, id);
      }
    } else {
      ensureTransitionAllowed(execution, FAILED, ACTIVE, TRANSITION_REACTIVATE);
    }
  }

  @Override
  public void reactivated(CmmnActivityExecution execution) {
    if (execution.isCaseInstanceExecution()) {
      CaseExecutionState previousState = execution.getPreviousState();

      if (SUSPENDED.equals(previousState)) {
        resumed(execution);
      }
    }

    // at the moment it is not possible to re-activate a case execution
    // because the state "FAILED" is not implemented.
  }

  // completion //////////////////////////////////////////////////////////////

  @Override
  public void onCompletion(CmmnActivityExecution execution) {
    ensureTransitionAllowed(execution, ACTIVE, COMPLETED, TRANSITION_COMPLETE);
    canComplete(execution, true);
    completing(execution);
  }

  @Override
  public void onManualCompletion(CmmnActivityExecution execution) {
    ensureTransitionAllowed(execution, ACTIVE, COMPLETED, TRANSITION_COMPLETE);
    canComplete(execution, true, true);
    completing(execution);
  }

  @Override
  protected void completing(CmmnActivityExecution execution) {
    List<? extends CmmnExecution> children = execution.getCaseExecutions();
    for (CmmnExecution child : children) {
      if (!child.isDisabled()) {
        child.parentComplete();
      } else {
        child.remove();
      }
    }
  }

  protected boolean canComplete(CmmnActivityExecution execution) {
    return canComplete(execution, false);
  }

  protected boolean canComplete(CmmnActivityExecution execution, boolean throwException) {
    boolean autoComplete = evaluateAutoComplete(execution);
    return canComplete(execution, throwException, autoComplete);
  }

  protected boolean canComplete(CmmnActivityExecution execution, boolean throwException, boolean autoComplete) {
    String id = execution.getId();

    List<? extends CmmnExecution> children = execution.getCaseExecutions();

    if (children == null || children.isEmpty()) {
      // if the stage does not contain any child
      // then the stage can complete.
      return true;
    }

    // verify there are no STATE_ACTIVE children
    for (CmmnExecution child : children) {
      if (child.isNew() || child.isActive()) {

        if (throwException) {
          throw LOG.remainingChildException(TRANSITION_COMPLETE, id, child.getId(), CaseExecutionState.ACTIVE);
        }

        return false;
      }
    }

    if (autoComplete) {
      // ensure that all required children are DISABLED, STATE_COMPLETED and/or TERMINATED
      // available in the case execution tree.

      for (CmmnExecution child : children) {
        if (child.isRequired() && !child.isDisabled() && !child.isCompleted() && !child.isTerminated()) {

          if (throwException) {
            throw LOG.remainingChildException(TRANSITION_COMPLETE, id, child.getId(), child.getCurrentState());
          }

          return false;
        }
      }

    } else { /* autoComplete == false && manualCompletion == false */
      // ensure that ALL children are DISABLED, STATE_COMPLETED and/or TERMINATED

      for (CmmnExecution child : children) {
        if (!child.isDisabled() && !child.isCompleted() && !child.isTerminated()) {

          if (throwException) {
            throw LOG.wrongChildStateException(TRANSITION_COMPLETE, id, child.getId(), "[available|enabled|suspended]");
          }

          return false;
        }
      }

      // TODO: are there any DiscretionaryItems?
      // if yes, then it is not possible to complete
      // this stage (NOTE: manualCompletion == false)!

    }

    return true;
  }

  protected boolean evaluateAutoComplete(CmmnActivityExecution execution) {
    CmmnActivity activity = getActivity(execution);

    Object autoCompleteProperty = activity.getProperty(PROPERTY_AUTO_COMPLETE);
    if (autoCompleteProperty != null) {
      String message = "Property autoComplete expression returns non-Boolean: "+autoCompleteProperty+" ("+autoCompleteProperty.getClass().getName()+")";
      ensureInstanceOf(message, "autoComplete", autoCompleteProperty, Boolean.class);

      return (Boolean) autoCompleteProperty;
    }

    return false;
  }

  // termination //////////////////////////////////////////////////////////////

  protected boolean isAbleToTerminate(CmmnActivityExecution execution) {
    List<? extends CmmnExecution> children = execution.getCaseExecutions();

    if (children != null && !children.isEmpty()) {

      for (CmmnExecution child : children) {
        // the guard "!child.isCompleted()" is needed,
        // when an exitCriteria is triggered on a stage, and
        // the referenced sentry contains an onPart to a child
        // case execution which has defined as standardEvent "complete".
        // In that case the completed child case execution is still
        // in the list of child case execution of the parent case execution.
        if (!child.isTerminated() && !child.isCompleted()) {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  protected void performTerminate(CmmnActivityExecution execution) {
    if (!isAbleToTerminate(execution)) {
      terminateChildren(execution);

    } else {
      super.performTerminate(execution);
    }

  }

  @Override
  protected void performExit(CmmnActivityExecution execution) {
    if (!isAbleToTerminate(execution)) {
      terminateChildren(execution);

    } else {
      super.performExit(execution);
    }
  }

  protected void terminateChildren(CmmnActivityExecution execution) {
    List<? extends CmmnExecution> children = execution.getCaseExecutions();

    for (CmmnExecution child : children) {
      terminateChild(child);
    }
  }

  protected void terminateChild(CmmnExecution child) {
    CmmnActivityBehavior behavior = getActivityBehavior(child);

    // "child.isTerminated()": during resuming the children, it can
    // happen that a sentry will be satisfied, so that a child
    // will terminated. these terminated child cannot be resumed,
    // so ignore it.
    // "child.isCompleted()": in case that an exitCriteria on caseInstance
    // (ie. casePlanModel) has been fired, when a child inside has been
    // completed, so ignore it.
    if (!child.isTerminated() && !child.isCompleted()) {
      if (behavior instanceof StageOrTaskActivityBehavior) {
        child.exit();

      } else { /* behavior instanceof EventListenerOrMilestoneActivityBehavior */
        child.parentTerminate();
      }
    }
  }

  // suspension /////////////////////////////////////////////////////////////////

  @Override
  protected void performSuspension(CmmnActivityExecution execution) {
    if (!isAbleToSuspend(execution)) {
      suspendChildren(execution);

    } else {
      super.performSuspension(execution);
    }
  }


  @Override
  protected void performParentSuspension(CmmnActivityExecution execution) {
    if (!isAbleToSuspend(execution)) {
      suspendChildren(execution);

    } else {
      super.performParentSuspension(execution);
    }
  }

  protected void suspendChildren(CmmnActivityExecution execution) {
    List<? extends CmmnExecution> children = execution.getCaseExecutions();
    if (children != null && !children.isEmpty()) {

      for (CmmnExecution child : children) {

        CmmnActivityBehavior behavior = getActivityBehavior(child);

        // "child.isTerminated()": during resuming the children, it can
        // happen that a sentry will be satisfied, so that a child
        // will terminated. these terminated child cannot be resumed,
        // so ignore it.
        // "child.isSuspended()": maybe the child has been already
        // suspended, so ignore it.
        if (!child.isTerminated() && !child.isSuspended()) {
          if (behavior instanceof StageOrTaskActivityBehavior) {
            child.parentSuspend();

          } else { /* behavior instanceof EventListenerOrMilestoneActivityBehavior */
            child.suspend();
          }
        }
      }
    }
  }

  protected boolean isAbleToSuspend(CmmnActivityExecution execution) {
    List<? extends CmmnExecution> children = execution.getCaseExecutions();

    if (children != null && !children.isEmpty()) {

      for (CmmnExecution child : children) {
        if (!child.isSuspended()) {
          return false;
        }
      }
    }

    return true;
  }

  // resume /////////////////////////////////////////////////////////////////////////

  @Override
  public void resumed(CmmnActivityExecution execution) {
    if (execution.isAvailable()) {
      // trigger created() to check whether an exit- or
      // entryCriteria has been satisfied in the meantime.
      created(execution);

    } else if (execution.isActive()) {
      // if the given case execution is active after resuming,
      // then propagate it to the children.
      resumeChildren(execution);
    }
  }

  protected void resumeChildren(CmmnActivityExecution execution) {
    List<? extends CmmnExecution> children = execution.getCaseExecutions();

    if (children != null && !children.isEmpty()) {

      for (CmmnExecution child : children) {

        CmmnActivityBehavior behavior = getActivityBehavior(child);

        // during resuming the children, it can happen that a sentry
        // will be satisfied, so that a child will terminated. these
        // terminated child cannot be resumed, so ignore it.
        if (!child.isTerminated()) {
          if (behavior instanceof StageOrTaskActivityBehavior) {
            child.parentResume();

          } else { /* behavior instanceof EventListenerOrMilestoneActivityBehavior */
            child.resume();
          }
        }
      }
    }
  }

  // sentry ///////////////////////////////////////////////////////////////////////////////

  @Override
  protected boolean isAtLeastOneEntryCriterionSatisfied(CmmnActivityExecution execution) {
    if (!execution.isCaseInstanceExecution()) {
      return super.isAtLeastOneEntryCriterionSatisfied(execution);
    }

    return false;
  }

  @Override
  public void fireExitCriteria(CmmnActivityExecution execution) {
    if (!execution.isCaseInstanceExecution()) {
      execution.exit();
    } else {
      execution.terminate();
    }
  }

  @Override
  public void fireEntryCriteria(CmmnActivityExecution execution) {
    if (!execution.isCaseInstanceExecution()) {
      super.fireEntryCriteria(execution);
      return;
    }

    throw LOG.criteriaNotAllowedForCaseInstanceException("entry", execution.getId());
  }

  // handle child state changes ///////////////////////////////////////////////////////////

  @Override
  public void handleChildCompletion(CmmnActivityExecution execution, CmmnActivityExecution child) {
    fireForceUpdate(execution);

    if (execution.isActive()) {
      checkAndCompleteCaseExecution(execution);
    }
  }

  @Override
  public void handleChildDisabled(CmmnActivityExecution execution, CmmnActivityExecution child) {
    fireForceUpdate(execution);

    if (execution.isActive()) {
      checkAndCompleteCaseExecution(execution);
    }
  }

  @Override
  public void handleChildSuspension(CmmnActivityExecution execution, CmmnActivityExecution child) {
    // if the given execution is not suspending currently, then ignore this notification.
    if (execution.isSuspending() && isAbleToSuspend(execution)) {
      String id = execution.getId();
      CaseExecutionState currentState = execution.getCurrentState();

      if (SUSPENDING_ON_SUSPENSION.equals(currentState)) {
        execution.performSuspension();

      } else if (SUSPENDING_ON_PARENT_SUSPENSION.equals(currentState)) {
        execution.performParentSuspension();

      } else {
        throw LOG.suspendCaseException(id, currentState);
      }
    }
  }

  @Override
  public void handleChildTermination(CmmnActivityExecution execution, CmmnActivityExecution child) {
    fireForceUpdate(execution);

    if (execution.isActive()) {
      checkAndCompleteCaseExecution(execution);

    } else if (execution.isTerminating() && isAbleToTerminate(execution)) {
      String id = execution.getId();
      CaseExecutionState currentState = execution.getCurrentState();

      if (TERMINATING_ON_TERMINATION.equals(currentState)) {
        execution.performTerminate();

      } else if (TERMINATING_ON_EXIT.equals(currentState)) {
        execution.performExit();

      } else if (TERMINATING_ON_PARENT_TERMINATION.equals(currentState)) {
        throw LOG.illegalStateTransitionException("parentTerminate", id, getTypeName());

      } else {
        throw LOG.terminateCaseException(id, currentState);

      }
    }
  }

  protected void checkAndCompleteCaseExecution(CmmnActivityExecution execution) {
    if (canComplete(execution)) {
      execution.complete();
    }
  }

  protected void fireForceUpdate(CmmnActivityExecution execution) {
    if (execution instanceof CaseExecutionEntity entity) {
      entity.forceUpdate();
    }
  }

  @Override
  protected String getTypeName() {
    return "stage";
  }

}
