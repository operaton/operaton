/*
 * Copyright 2026 FINOS
 * Modified in 2026 by the Operaton contributors for lifecycle-safe completion.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.operaton.bpm.engine.impl.bpmn.behavior;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.operaton.bpm.engine.ActivityTypes;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.Condition;
import org.operaton.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.operaton.bpm.engine.impl.bpmn.helper.CompensationUtil;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.el.Expression;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.pvm.delegate.CompositeActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

/**
 * Implementation of the BPMN 2.0 Ad-Hoc Sub-Process.
 *
 * <p>An Ad-Hoc Sub-Process is a specialized type of Sub-Process that has a set
 * of Activities that can be performed in any order, and some of which may not
 * be performed at all. Initial activities are activated from the
 * {@code activeTasksCollection} extension property and additional starter activities may be
 * activated via {@code RuntimeService#triggerAdHocActivities(String, Collection, Map)}. BPMN
 * {@code ordering} determines whether child activities may run in parallel or only one at a time.
 *
 * <p>The subprocess completes when the {@code completionCondition} evaluates to
 * {@code true} after any inner activity completes. If no completion condition
 * is defined, default auto-complete semantics apply: once at least one ad-hoc
 * activity has been started and no child activities remain active, the scope
 * is completed. This behavior can be disabled by setting extension attribute
 * {@code autoComplete} to {@code false}, in which case explicit completion is
 * required.
 *
 */
public class AdHocSubProcessActivityBehavior extends AbstractBpmnActivityBehavior implements CompositeActivityBehavior {

  public static final String NUMBER_OF_ACTIVE_AD_HOC_ACTIVITIES = "nrOfActiveAdHocActivities";
  public static final String NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES = "nrOfCompletedAdHocActivities";
  public static final String NUMBER_OF_ENABLED_AD_HOC_ACTIVITIES = "nrOfEnabledAdHocActivities";
  public static final String AD_HOC_ENABLED_ACTIVITY_IDS = "adHocEnabledActivityIds";
  public static final String AD_HOC_ACTIVE_ACTIVITY_IDS = "adHocActiveActivityIds";
  public static final String AD_HOC_COMPLETED_ACTIVITY_IDS = "adHocCompletedActivityIds";
  public static final String AD_HOC_LAST_COMPLETED_ACTIVITY_ID = "adHocLastCompletedActivityId";
  public static final String AD_HOC_COMPLETION_CONDITION_SATISFIED = "adHocCompletionConditionSatisfied";

  protected final AdHocStartability startability = AdHocStartability.INSTANCE;

  /**
   * On entry into an ad-hoc subprocess, only starter activities named in the
   * optional {@code activeTasksCollection} extension property are activated.
   */
  @Override
  public void execute(ActivityExecution execution) throws Exception {
    List<ActivityImpl> starterActivities = getInitiallyActivatableChildActivities(execution);
    List<String> configuredActiveTaskIds = getConfiguredActiveTaskIds(execution);
    validateConfiguredActiveTaskIds(execution, starterActivities, configuredActiveTaskIds);
    List<ActivityImpl> adHocActivities = filterStarterActivities(starterActivities, configuredActiveTaskIds);
    validateOrderingAllowsInitialActivities(execution, adHocActivities);
    activateActivities(execution, adHocActivities, null);
    if (isAdHocScopeExecution(execution)) {
      evaluateCompletionCondition(execution, !adHocActivities.isEmpty());
    }
  }

  /**
   * Called by the PVM each time a concurrent child execution within this
   * ad-hoc scope completes. Removes the ended execution, re-evaluates the
   * completion condition and — if met — cancels any remaining running
   * activities and leaves the subprocess.
   */
  @Override
  public void concurrentChildExecutionEnded(ActivityExecution scopeExecution, ActivityExecution endedExecution) {
    ActivityImpl adHocScopeActivity = (ActivityImpl) scopeExecution.getActivity();
    String completedActivityId = getCompletedActivityId(endedExecution);
    endedExecution.remove();
    scopeExecution.forceUpdate();
    recordCompletedAdHocActivity(scopeExecution, adHocScopeActivity, completedActivityId);

    ((PvmExecutionImpl) scopeExecution).dispatchDelayedEventsAndPerformOperation(resumedScope -> {
      // Routing events do not introduce a new Activity completion decision. They can
      // still finish an already-latched drain or the no-condition auto-complete path.
      ActivityImpl completedActivity = adHocScopeActivity.findActivity(completedActivityId);
      if (completedActivity != null && isCompletableActivity(completedActivity)
          || getCompletionCondition(adHocScopeActivity) == null
          || isAdHocCompletionConditionSatisfied(resumedScope)) {
        evaluateCompletionCondition(resumedScope, adHocScopeActivity, true);
      } else if (hasCompletionContext(resumedScope, adHocScopeActivity)) {
        updateActiveAdHocActivityContext(resumedScope);
      }
      if (resumedScope.getActivity() == adHocScopeActivity) {
        // Keep the owner addressable instead of folding its last child into it.
        resumedScope.forceUpdate();
      }
      return null;
    });
  }

  /**
   * Called by the PVM when all concurrent executions inside the scope have
   * finished. For ad-hoc, this is the last chance to evaluate the completion
   * condition. If the condition is met (or not defined) the subprocess
   * proceeds; otherwise the scope execution stays open for further triggers.
   */
  @Override
  public void complete(ActivityExecution scopeExecution) {
    ActivityImpl scopeActivity = (ActivityImpl) scopeExecution.getActivity();
    recordPreviouslyActiveAdHocActivities(scopeExecution, scopeActivity);
    evaluateCompletionCondition(scopeExecution, true);
  }

  /**
   * Evaluates the optional completion condition. Exits the subprocess if the
   * condition is {@code true} or if no condition is configured. If the
   * condition is present but evaluates to {@code false} the scope execution
   * simply remains open.
   */
  protected void evaluateCompletionCondition(ActivityExecution scopeExecution, boolean adHocActivityStarted) {
    evaluateCompletionCondition(scopeExecution, (ActivityImpl) scopeExecution.getActivity(), adHocActivityStarted);
  }

  protected void evaluateCompletionCondition(ActivityExecution scopeExecution, ActivityImpl scopeActivity,
      boolean adHocActivityStarted) {
    if (scopeExecution == null || scopeActivity == null) {
      return;
    }

    Condition completionCondition = getCompletionCondition(scopeActivity);

    boolean conditionMet;
    if (isAdHocCompletionConditionSatisfied(scopeExecution)) {
      conditionMet = true;
    } else if (completionCondition == null) {
      // No condition: complete only if auto-complete is enabled and at least one
      // ad-hoc activity has started with no active children left.
      conditionMet = isAutoCompleteEnabled(scopeActivity)
          && adHocActivityStarted
          && isAdHocScopeActivity(scopeActivity)
          && !hasActiveChildExecutions(scopeExecution);
    } else {
      updateAdHocCompletionContext(scopeExecution);
      conditionMet = isAdHocCompletionConditionSatisfied(scopeExecution)
          || completionCondition.evaluate(scopeExecution, scopeExecution);
      if (conditionMet) {
        scopeExecution.setVariableLocal(AD_HOC_COMPLETION_CONDITION_SATISFIED, true);
      }
    }

    if (!conditionMet) {
      return;
    }

    discardEnabledActivities(scopeExecution);

    boolean cancelRemaining = Boolean.TRUE.equals(
      scopeActivity.getProperty(BpmnParse.PROPERTYNAME_AD_HOC_CANCEL_REMAINING));

    if (cancelRemaining) {
      cancelAllActiveChildren(scopeExecution);
      leave(scopeExecution);
      return;
    }

    // When remaining instances are preserved, only leave once no active children are left.
    if (!hasActiveChildExecutions(scopeExecution)) {
      leave(scopeExecution);
    }
  }

  /** Whether child completion must precede selecting its outgoing sequence flows. */
  public boolean shouldHandleChildCompletion(ActivityExecution scopeExecution, ActivityImpl activity) {
    return isCompletableActivity(activity)
        && (getCompletionCondition((ActivityImpl) activity.getFlowScope()) != null
            || isAdHocCompletionConditionSatisfied(scopeExecution));
  }

  /** Gateways and events are flow nodes, but do not count as completed activities. */
  public boolean isCompletableActivity(ActivityImpl activity) {
    ActivityImpl modelActivity = startability.getModelActivity(activity);
    return startability.isStartableActivityType((String) modelActivity.getProperty(BpmnProperties.TYPE.name()));
  }

  /**
   * Record a completed activity once, after its end listeners and output mappings.
   * The completing token is excluded while evaluating the completion condition,
   * although it remains available for selecting an outgoing flow using local variables.
   */
  public boolean activityCompleted(ActivityExecution scopeExecution, ActivityExecution completingChild,
      ActivityImpl activity) {
    ActivityImpl scopeActivity = (ActivityImpl) activity.getFlowScope();
    scopeExecution.forceUpdate();
    recordCompletedAdHocActivity(scopeExecution, scopeActivity, startability.getModelActivity(activity).getId());
    if (hasCompletionContext(scopeExecution, scopeActivity)) {
      updateAdHocCompletionContext(scopeExecution);
      updateActiveAdHocActivityContext(scopeExecution, completingChild);
    }
    Condition condition = getCompletionCondition(scopeActivity);
    boolean completionRequested = isAdHocCompletionConditionSatisfied(scopeExecution)
        || condition != null && condition.evaluate(scopeExecution, scopeExecution);
    if (completionRequested) {
      scopeExecution.setVariableLocal(AD_HOC_COMPLETION_CONDITION_SATISFIED, true);
    }
    return completionRequested;
  }

  /** Finish a token whose completion has already been recorded. */
  public void completedActivityExecutionEnded(ActivityExecution scopeExecution, ActivityExecution endedExecution) {
    endedExecution.remove();
    scopeExecution.forceUpdate();
    activityContinued(scopeExecution);
    // Output mappings and completion-context updates happen after the activity's
    // earlier end-listener dispatch. Deliver their events before closing/draining
    // the owner; an interrupting conditional event may replace that continuation.
    ((PvmExecutionImpl) scopeExecution).dispatchDelayedEventsAndPerformOperation(resumedScope -> {
      if (isAdHocCompletionConditionSatisfied(resumedScope)) {
        evaluateCompletionCondition(resumedScope, true);
      }
      return null;
    });
  }

  /** Refresh the context after a completed activity continues along sequence flows. */
  public void activityContinued(ActivityExecution scopeExecution) {
    if (hasCompletionContext(scopeExecution, (ActivityImpl) scopeExecution.getActivity())) {
      updateActiveAdHocActivityContext(scopeExecution);
    }
  }

  protected boolean hasCompletionContext(ActivityExecution scopeExecution, ActivityImpl scopeActivity) {
    return getCompletionCondition(scopeActivity) != null
        || scopeExecution.hasVariableLocal(NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)
        || scopeExecution.hasVariableLocal(AD_HOC_COMPLETED_ACTIVITY_IDS);
  }

  /** Persist a ready token without starting listeners, history, input mapping or async-before. */
  public void enableActivity(ActivityExecution scope, ActivityExecution token, ActivityImpl activity) {
    token.setActivity(activity);
    token.setActivityInstanceId(null);
    token.setActive(false);
    token.setEnded(false);
    token.setVariableLocal(AdHocStartability.AD_HOC_ENABLED_ACTIVITY, true);
    scope.forceUpdate();
    if (hasCompletionContext(scope, (ActivityImpl) scope.getActivity())) {
      updateActiveAdHocActivityContext(scope);
    }
  }

  /** Enabled work has not started and is discarded when completion is decided. */
  public void discardEnabledActivities(ActivityExecution scope) {
    discardEnabledChildren(scope);
    scope.forceUpdate();
    if (hasCompletionContext(scope, (ActivityImpl) scope.getActivity())) {
      updateActiveAdHocActivityContext(scope);
    }
  }

  protected void discardEnabledChildren(ActivityExecution parent) {
    for (ActivityExecution child : new ArrayList<>(parent.getNonEventScopeExecutions())) {
      if (startability.hasOnlyEnabledActivities(child)) {
        // Generic cancellation also closes entered wrapper scopes and their subscriptions.
        // Enabled leaves have no activity-instance ID, so no unstarted activity is ended.
        normalizeEnabledScopeExecutions(child);
        ((PvmExecutionImpl) child).deleteCascade("adHocEnabledActivityDiscarded");
      } else if (!(child.getActivity() instanceof ActivityImpl activity
          && activity.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior)) {
        discardEnabledChildren(child);
      }
    }
    if (parent.isScope() && !(parent.getActivity() instanceof ActivityImpl activity
        && activity.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior)) {
      // Ordinary scopes rely on their final running token being compacted before it ends.
      // Otherwise its concurrent-end callback removes that token but leaves the wrapper waiting.
      parent.tryPruneLastConcurrentChild();
    }
  }

  /** Restore structural wrapper scopes before cancellation can mistake a parked composite for an entered activity. */
  protected void normalizeEnabledScopeExecutions(ActivityExecution execution) {
    for (ActivityExecution child : execution.getNonEventScopeExecutions()) {
      normalizeEnabledScopeExecutions(child);
    }
    if (execution.isScope() && startability.isEnabledExecution(execution)) {
      ActivityImpl pendingActivity = (ActivityImpl) execution.getActivity();
      if (!(pendingActivity.getFlowScope() instanceof ActivityImpl wrapper)
          || wrapper.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior) {
        throw new ProcessEngineException(
            "Cannot discard an enabled scope token without an ordinary flow-scope owner");
      }
      PvmExecutionImpl scopeExecution = (PvmExecutionImpl) execution;
      if (scopeExecution.createActivityExecutionMapping(wrapper).get(wrapper) != scopeExecution) {
        throw new ProcessEngineException(
            "Enabled scope token does not represent its enclosing flow scope");
      }
      execution.setActivity(wrapper);
      execution.removeVariableLocal(AdHocStartability.AD_HOC_ENABLED_ACTIVITY);
    }
  }

  protected boolean isAdHocScopeExecution(ActivityExecution execution) {
    if (execution == null || execution.getActivity() == null) {
      return false;
    }

    Object type = execution.getActivity().getProperty(BpmnProperties.TYPE.name());
    return ActivityTypes.SUB_PROCESS_AD_HOC.equals(type);
  }

  protected boolean isAdHocScopeActivity(ActivityImpl activity) {
    if (activity == null) {
      return false;
    }

    Object type = activity.getProperty(BpmnProperties.TYPE.name());
    return ActivityTypes.SUB_PROCESS_AD_HOC.equals(type);
  }

  /**
   * Cancels all active child (concurrent) executions within the ad-hoc scope.
   */
  protected void cancelAllActiveChildren(ActivityExecution scopeExecution) {
    List<ActivityExecution> children = new ArrayList<>(scopeExecution.getNonEventScopeExecutions());
    for (ActivityExecution child : children) {
      ((PvmExecutionImpl) child).deleteCascade("adHocCompletionConditionMet");
    }
  }

  protected boolean hasActiveChildExecutions(ActivityExecution scopeExecution) {
    return startability.hasActiveChildExecutions(scopeExecution);
  }

  protected Condition getCompletionCondition(ActivityImpl scopeActivity) {
    return scopeActivity != null
        ? (Condition) scopeActivity.getProperty(BpmnParse.PROPERTYNAME_AD_HOC_COMPLETION_CONDITION)
        : null;
  }

  protected void recordCompletedAdHocActivity(ActivityExecution scopeExecution, ActivityImpl scopeActivity,
      String completedActivityId) {
    if (!hasCompletionContext(scopeExecution, scopeActivity) || completedActivityId == null) {
      return;
    }
    ActivityImpl completedActivity = scopeActivity.findActivity(completedActivityId);
    if (completedActivity == null || !isCompletableActivity(completedActivity)) {
      return;
    }

    List<String> completedActivityIds = getCompletedAdHocActivityIds(scopeExecution);
    completedActivityIds.add(completedActivityId);
    scopeExecution.setVariableLocal(AD_HOC_COMPLETED_ACTIVITY_IDS, completedActivityIds);
    scopeExecution.setVariableLocal(NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES, completedActivityIds.size());
    scopeExecution.setVariableLocal(AD_HOC_LAST_COMPLETED_ACTIVITY_ID, completedActivityId);
    updateActiveAdHocActivityContext(scopeExecution);
  }

  protected void recordPreviouslyActiveAdHocActivities(ActivityExecution scopeExecution, ActivityImpl scopeActivity) {
    if (!hasCompletionContext(scopeExecution, scopeActivity) || hasActiveChildExecutions(scopeExecution)) {
      return;
    }

    List<String> activeActivityIds = getAdHocActivityIds(scopeExecution, AD_HOC_ACTIVE_ACTIVITY_IDS);
    if (activeActivityIds.isEmpty()) {
      return;
    }

    List<String> completedActivityIds = getCompletedAdHocActivityIds(scopeExecution);
    for (String activeActivityId : activeActivityIds) {
      ActivityImpl activeActivity = scopeActivity.findActivity(activeActivityId);
      if (activeActivity == null || !isCompletableActivity(activeActivity)) {
        continue;
      }
      completedActivityIds.add(activeActivityId);
      scopeExecution.setVariableLocal(AD_HOC_LAST_COMPLETED_ACTIVITY_ID, activeActivityId);
    }

    scopeExecution.setVariableLocal(AD_HOC_COMPLETED_ACTIVITY_IDS, completedActivityIds);
    scopeExecution.setVariableLocal(NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES, completedActivityIds.size());
    updateActiveAdHocActivityContext(scopeExecution);
  }

  protected List<String> getCompletedAdHocActivityIds(ActivityExecution scopeExecution) {
    return getAdHocActivityIds(scopeExecution, AD_HOC_COMPLETED_ACTIVITY_IDS);
  }

  protected List<String> getAdHocActivityIds(ActivityExecution scopeExecution, String variableName) {
    Object activityIds = scopeExecution.getVariableLocal(variableName);
    if (activityIds instanceof Collection<?>) {
      List<String> result = new ArrayList<>();
      for (Object activityId : (Collection<?>) activityIds) {
        if (activityId != null) {
          result.add(String.valueOf(activityId));
        }
      }
      return result;
    }
    return new ArrayList<>();
  }

  protected void updateAdHocCompletionContext(ActivityExecution scopeExecution) {
    if (!scopeExecution.hasVariableLocal(AD_HOC_ACTIVE_ACTIVITY_IDS)) {
      scopeExecution.setVariableLocal(AD_HOC_ACTIVE_ACTIVITY_IDS, new ArrayList<String>());
    }
    if (!scopeExecution.hasVariableLocal(AD_HOC_COMPLETED_ACTIVITY_IDS)) {
      scopeExecution.setVariableLocal(AD_HOC_COMPLETED_ACTIVITY_IDS, new ArrayList<String>());
    }
    if (!scopeExecution.hasVariableLocal(NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)) {
      scopeExecution.setVariableLocal(NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES, 0);
    }
    if (!scopeExecution.hasVariableLocal(AD_HOC_LAST_COMPLETED_ACTIVITY_ID)) {
      scopeExecution.setVariableLocal(AD_HOC_LAST_COMPLETED_ACTIVITY_ID, null);
    }
    if (!scopeExecution.hasVariableLocal(AD_HOC_COMPLETION_CONDITION_SATISFIED)) {
      scopeExecution.setVariableLocal(AD_HOC_COMPLETION_CONDITION_SATISFIED, false);
    }

    updateActiveAdHocActivityContext(scopeExecution);
  }

  protected boolean isAdHocCompletionConditionSatisfied(ActivityExecution scopeExecution) {
    return Boolean.TRUE.equals(scopeExecution.getVariableLocal(AD_HOC_COMPLETION_CONDITION_SATISFIED));
  }

  protected void updateActiveAdHocActivityContext(ActivityExecution scopeExecution) {
    updateActiveAdHocActivityContext(scopeExecution, null);
  }

  protected void updateActiveAdHocActivityContext(ActivityExecution scopeExecution, ActivityExecution excludedChild) {
    List<? extends ActivityExecution> children = scopeExecution.getNonEventScopeExecutions().stream()
        .filter(child -> child != excludedChild && startability.isRunningActivity(child))
        .toList();
    ActivityImpl scopeActivity = (ActivityImpl) scopeExecution.getActivity();
    List<String> activityIds = children.stream()
        .map(child -> getActiveAdHocActivityId(child, scopeActivity))
        .filter(activityId -> activityId != null)
        .collect(Collectors.toCollection(ArrayList::new));
    scopeExecution.setVariableLocal(AD_HOC_ACTIVE_ACTIVITY_IDS, activityIds);
    scopeExecution.setVariableLocal(NUMBER_OF_ACTIVE_AD_HOC_ACTIVITIES, children.size());
    List<String> enabledIds = startability.getEnabledExecutions(scopeExecution).stream()
        .map(child -> startability.getModelActivity((ActivityImpl) child.getActivity()).getId())
        .collect(Collectors.toCollection(ArrayList::new));
    scopeExecution.setVariableLocal(AD_HOC_ENABLED_ACTIVITY_IDS, enabledIds);
    scopeExecution.setVariableLocal(NUMBER_OF_ENABLED_AD_HOC_ACTIVITIES, enabledIds.size());
  }

  protected String getActiveAdHocActivityId(ActivityExecution execution, ActivityImpl scopeActivity) {
    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    if (activity != null) {
      while (activity.getFlowScope() != scopeActivity
          && activity.getFlowScope() instanceof ActivityImpl flowScope) {
        activity = flowScope;
      }
      return startability.getModelActivity(activity).getId();
    }
    for (ActivityExecution child : execution.getNonEventScopeExecutions()) {
      String activityId = getActiveAdHocActivityId(child, scopeActivity);
      if (activityId != null) {
        return activityId;
      }
    }
    return null;
  }

  protected String getCompletedActivityId(ActivityExecution endedExecution) {
    if (endedExecution == null) {
      return null;
    }
    if (endedExecution.getActivity() instanceof ActivityImpl activity) {
      return startability.getModelActivity(activity).getId();
    }
    if (endedExecution.getCurrentActivityId() != null) {
      return endedExecution.getCurrentActivityId();
    }
    return endedExecution.getActivity() != null ? endedExecution.getActivity().getId() : null;
  }

  protected boolean isAutoCompleteEnabled(ActivityImpl scopeActivity) {
    Object autoComplete = scopeActivity.getProperty(BpmnParse.PROPERTYNAME_AD_HOC_AUTO_COMPLETE);
    return autoComplete == null || Boolean.TRUE.equals(autoComplete);
  }

  protected List<ActivityImpl> getInitiallyActivatableChildActivities(ActivityExecution scopeExecution) {
    ActivityImpl adHocSubProcess = (ActivityImpl) scopeExecution.getActivity();
    return startability.getPotentiallyStartableActivities(adHocSubProcess);
  }

  protected List<String> getConfiguredActiveTaskIds(ActivityExecution scopeExecution) {
    Expression activeTasksCollection = (Expression) scopeExecution.getActivity()
        .getProperty(BpmnParse.PROPERTYNAME_AD_HOC_ACTIVE_TASKS_COLLECTION);

    if (activeTasksCollection == null) {
      return new ArrayList<>();
    }

    Object activeTasks = activeTasksCollection.getValue(scopeExecution);

    if (activeTasks == null) {
      return new ArrayList<>();
    }

    if (activeTasks instanceof Collection<?>) {
      List<String> activityIds = new ArrayList<>();
      for (Object value : (Collection<?>) activeTasks) {
        if (value != null) {
          String activityId = String.valueOf(value).trim();
          if (!activityId.isEmpty()) {
            activityIds.add(activityId);
          }
        }
      }
      return activityIds;
    }

    if (activeTasks instanceof String) {
      String activityIdsText = ((String) activeTasks).trim();
      if (activityIdsText.isEmpty()) {
        return new ArrayList<>();
      }

      List<String> activityIds = new ArrayList<>();
      for (String activityId : activityIdsText.split(",")) {
        String trimmedActivityId = activityId.trim();
        if (!trimmedActivityId.isEmpty()) {
          activityIds.add(trimmedActivityId);
        }
      }
      return activityIds;
    }

    throw new BadUserRequestException(
        "activeTasksCollection for adHocSubProcess '" + scopeExecution.getActivity().getId()
            + "' must resolve to a String or Collection");
  }

  protected void validateConfiguredActiveTaskIds(ActivityExecution scopeExecution,
      List<ActivityImpl> starterActivities,
      List<String> configuredActiveTaskIds) {
    if (configuredActiveTaskIds.isEmpty()) {
      return;
    }

    Set<String> starterActivityIds = starterActivities.stream()
        .map(ActivityImpl::getId)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    List<String> invalidActivityIds = configuredActiveTaskIds.stream()
        .filter(activityId -> !starterActivityIds.contains(activityId))
        .distinct()
        .collect(Collectors.toList());

    if (!invalidActivityIds.isEmpty()) {
      throw new BadUserRequestException(
          "activeTasksCollection contains non-startable activities in adHocSubProcess '"
              + scopeExecution.getActivity().getId() + "': " + invalidActivityIds);
    }
  }

  protected List<ActivityImpl> filterStarterActivities(List<ActivityImpl> starterActivities,
      List<String> configuredActiveTaskIds) {
    Set<String> configuredIds = new LinkedHashSet<>(configuredActiveTaskIds);
    return starterActivities.stream()
        .filter(activity -> configuredIds.contains(activity.getId()))
        .collect(Collectors.toList());
  }

  protected void validateOrderingAllowsInitialActivities(ActivityExecution scopeExecution,
      List<ActivityImpl> adHocActivities) {
    ActivityImpl adHocActivity = (ActivityImpl) scopeExecution.getActivity();
    if (startability.isSequentialOrdering(adHocActivity) && adHocActivities.size() > 1) {
      throw new BadUserRequestException(
          "Sequential adHocSubProcess '" + adHocActivity.getId()
              + "' can activate only one activity from activeTasksCollection");
    }
  }

  /**
   * Reserve the entire batch before running synchronous activities. Otherwise a
   * synchronous first member could complete the scope before later members exist.
   */
  public void activateActivities(ActivityExecution scopeExecution, List<ActivityImpl> activities,
      java.util.Map<String, java.util.Map<String, Object>> activityVariables) {
    List<ActivityExecution> children = new ArrayList<>();
    List<Boolean> enabledTokens = new ArrayList<>();
    for (ActivityImpl activity : activities) {
      ActivityExecution ready = startability.getEnabledExecutionForActivation(scopeExecution, activity);
      boolean enabledToken = ready != null;
      ActivityExecution child;
      if (enabledToken) {
        child = ready;
        child.removeVariableLocal(AdHocStartability.AD_HOC_ENABLED_ACTIVITY);
      } else {
        child = scopeExecution.createExecution();
        child.setConcurrent(true);
        child.setScope(false);
        child.setActivity(startability.getExecutionActivity(activity));
        child.setActivityInstanceId(null);
      }
      child.setActive(true);
      if (activityVariables != null && activityVariables.get(activity.getId()) != null) {
        child.setVariablesLocal(activityVariables.get(activity.getId()));
      }
      children.add(child);
      enabledTokens.add(enabledToken);
    }
    scopeExecution.forceUpdate();
    for (int i = 0; i < children.size(); i++) {
      ActivityExecution child = children.get(i);
      if (!child.isEnded() && !((PvmExecutionImpl) child).isRemoved()
          && startability.getDirectChild(scopeExecution, child).getParent() == scopeExecution) {
        if (enabledTokens.get(i)) {
          ((PvmExecutionImpl) child).performOperation(
              org.operaton.bpm.engine.impl.pvm.runtime.operation.PvmAtomicOperation.TRANSITION_CREATE_SCOPE);
        } else {
          child.executeActivity(startability.getExecutionActivity(activities.get(i)));
        }
      }
    }
  }

  @Override
  public void doLeave(ActivityExecution execution) {
    CompensationUtil.createEventScopeExecution((ExecutionEntity) execution);
    super.doLeave(execution);
  }

}
