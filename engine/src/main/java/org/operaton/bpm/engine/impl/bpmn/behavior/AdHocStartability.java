/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.operaton.bpm.engine.ActivityTypes;
import org.operaton.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.pvm.PvmTransition;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;

/**
 * Central startability support for ad-hoc subprocess activities.
 *
 * <p>Task-like activities, call activities, subprocesses, and transactions are
 * selectable unless they are compensation handlers or event subprocesses.
 * Activities with incoming sequence flows require a persisted enabled token;
 * no-incoming-flow activities are reusable starters. Discovery and activation
 * share the same runtime ordering checks.
 */
public class AdHocStartability {

  public static final AdHocStartability INSTANCE = new AdHocStartability();

  public static final String AD_HOC_ENABLED_ACTIVITY = "adHocEnabledActivity";

  public static final String ORDERING_SEQUENTIAL = "Sequential";

  private static final Set<String> STARTABLE_ACTIVITY_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
      ActivityTypes.TASK,
      ActivityTypes.TASK_SCRIPT,
      ActivityTypes.TASK_SERVICE,
      ActivityTypes.TASK_BUSINESS_RULE,
      ActivityTypes.TASK_MANUAL_TASK,
      ActivityTypes.TASK_USER_TASK,
      ActivityTypes.TASK_SEND_TASK,
      ActivityTypes.TASK_RECEIVE_TASK,
      ActivityTypes.CALL_ACTIVITY,
      ActivityTypes.SUB_PROCESS,
      ActivityTypes.SUB_PROCESS_AD_HOC,
      ActivityTypes.TRANSACTION)));

  protected AdHocStartability() {
  }

  /**
   * Returns activities that are potentially startable based on model-level
   * ad-hoc rules, independent of current runtime activity instances.
   */
  public List<ActivityImpl> getPotentiallyStartableActivities(ActivityImpl adHocScope) {
    if (adHocScope == null) {
      return Collections.emptyList();
    }

    return adHocScope.getActivities().stream()
        .map(this::getModelActivity)
        .filter(activity -> isPotentiallyStartableActivity(adHocScope, activity))
        .collect(Collectors.toList());
  }

  /**
   * Returns activities that are currently startable in the given ad-hoc scope execution.
   */
  public List<ActivityImpl> getStartableActivities(ActivityExecution adHocScopeExecution) {
    ActivityImpl adHocScope = getAdHocScope(adHocScopeExecution);
    if (isCompletionRequested(adHocScopeExecution)) {
      return Collections.emptyList();
    }

    Map<String, ActivityImpl> activities = new LinkedHashMap<>();
    for (ActivityImpl starter : getPotentiallyStartableActivities(adHocScope)) {
      activities.put(starter.getId(), starter);
    }
    for (ActivityExecution enabled : getEnabledExecutions(adHocScopeExecution)) {
      ActivityImpl activity = getModelActivity((ActivityImpl) enabled.getActivity());
      activities.putIfAbsent(activity.getId(), activity);
    }
    return activities.values().stream().filter(activity -> canActivate(adHocScopeExecution, activity))
        .collect(Collectors.toList());
  }

  /**
   * Checks if an activity can be started in the current ad-hoc scope execution.
   */
  public boolean isStartableActivity(ActivityExecution adHocScopeExecution, ActivityImpl activity) {
    ActivityImpl adHocScope = getAdHocScope(adHocScopeExecution);
    if (isCompletionRequested(adHocScopeExecution) || !isAvailableActivity(adHocScopeExecution, activity)) {
      return false;
    }

    return canActivate(adHocScopeExecution, activity);
  }

  /**
   * Checks if an activity is startable according to static ad-hoc model rules.
   */
  public boolean isPotentiallyStartableActivity(ActivityImpl adHocScope, ActivityImpl activity) {
    if (adHocScope == null || activity == null || getExecutionActivity(activity).getFlowScope() != adHocScope
        || activity.isCompensationHandler()
        || activity.isTriggeredByEvent()) {
      return false;
    }

    String type = (String) activity.getProperty(BpmnProperties.TYPE.name());
    if (!isStartableActivityType(type)) {
      return false;
    }

    return !hasIncomingTransitionFromAdHocScope(adHocScope, getExecutionActivity(activity));
  }

  /**
   * Checks if an activity type is allowed to be started in an ad-hoc subprocess.
   */
  public boolean isStartableActivityType(String type) {
    return type != null && STARTABLE_ACTIVITY_TYPES.contains(type);
  }

  /**
   * Checks if the ad-hoc scope uses BPMN sequential ordering.
   */
  public boolean isSequentialOrdering(ActivityImpl adHocScope) {
    return adHocScope != null
        && ORDERING_SEQUENTIAL.equals(adHocScope.getProperty(BpmnParse.PROPERTYNAME_AD_HOC_ORDERING));
  }

  /**
   * Checks if any child execution still represents running ad-hoc work.
   */
  public boolean hasActiveChildExecutions(ActivityExecution adHocScopeExecution) {
    return hasOpenChildExecutions(adHocScopeExecution);
  }

  /**
   * Checks if any open child execution exists in the ad-hoc scope.
   *
   * <p>This includes enabled tokens and routing waits as well as selected
   * activities. Open work prevents auto-completion; sequential activation uses
   * the narrower running-activity check.
   */
  public boolean hasOpenChildExecutions(ActivityExecution adHocScopeExecution) {
    return adHocScopeExecution != null
        && !adHocScopeExecution.getNonEventScopeExecutions().isEmpty();
  }

  /**
   * Checks if an activity has an incoming transition from within the ad-hoc scope.
   */
  public boolean hasIncomingTransitionFromAdHocScope(ActivityImpl adHocScope, ActivityImpl activity) {
    if (adHocScope == null || activity == null) {
      return false;
    }

    for (PvmTransition incomingTransition : activity.getIncomingTransitions()) {
      if (incomingTransition.getSource() instanceof ActivityImpl) {
        ActivityImpl source = (ActivityImpl) incomingTransition.getSource();
        if (adHocScope.findActivity(source.getId()) != null) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks if an activity already has an active child execution in the ad-hoc scope.
   */
  public boolean isActivityAlreadyActiveInScope(ActivityExecution adHocScopeExecution, String activityId) {
    return adHocScopeExecution != null && activityId != null
        && adHocScopeExecution.getExecutions().stream()
        .anyMatch(child -> child.getActivity() != null
            && activityId.equals(child.getActivity().getId())
            && child.isActive());
  }

  public boolean isEnabledExecution(ActivityExecution execution) {
    return execution != null && Boolean.TRUE.equals(execution.getVariableLocal(AD_HOC_ENABLED_ACTIVITY));
  }

  public List<ActivityExecution> getEnabledExecutions(ActivityExecution scope) {
    if (scope == null) {
      return Collections.emptyList();
    }
    List<ActivityExecution> enabled = new ArrayList<>();
    for (ActivityExecution child : scope.getNonEventScopeExecutions()) {
      collectEnabledExecutions(child, enabled);
    }
    enabled.sort(Comparator.comparing(ActivityExecution::getId));
    return enabled;
  }

  protected void collectEnabledExecutions(ActivityExecution execution, List<ActivityExecution> enabled) {
    if (isEnabledExecution(execution)) {
      enabled.add(execution);
      return;
    }
    if (execution.getActivity() instanceof ActivityImpl activity
        && activity.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior) {
      return;
    }
    for (ActivityExecution child : execution.getNonEventScopeExecutions()) {
      collectEnabledExecutions(child, enabled);
    }
  }

  public List<ActivityExecution> getEnabledExecutions(ActivityExecution scope, ActivityImpl activity) {
    ActivityImpl executionActivity = getExecutionActivity(activity);
    return getEnabledExecutions(scope).stream()
        .filter(execution -> execution.getActivity() == executionActivity)
        .collect(Collectors.toList());
  }

  /** Select deterministically among tokens whose outer activity may currently run. */
  public ActivityExecution getEnabledExecutionForActivation(ActivityExecution scope, ActivityImpl activity) {
    List<ActivityExecution> enabled = getEnabledExecutions(scope, activity);
    if (isSequentialOrdering(getAdHocScope(scope)) && hasRunningActivities(scope)) {
      return enabled.stream().filter(token -> isRunningActivity(getDirectChild(scope, token)))
          .findFirst().orElse(null);
    }
    return enabled.isEmpty() ? null : enabled.get(0);
  }

  public boolean isAvailableActivity(ActivityExecution scope, ActivityImpl activity) {
    return activity != null && activity == getModelActivity(activity)
        && (isPotentiallyStartableActivity(getAdHocScope(scope), activity)
        || !getEnabledExecutions(scope, activity).isEmpty());
  }

  /** Ready tokens and waiting routing events do not consume a Sequential activity slot. */
  public boolean hasRunningActivities(ActivityExecution scope) {
    return scope != null && scope.getNonEventScopeExecutions().stream().anyMatch(this::isRunningActivity);
  }

  public boolean isRunningActivity(ActivityExecution execution) {
    if (hasOnlyEnabledActivities(execution)) {
      return false;
    }
    if (execution.getActivity() instanceof ActivityImpl activity
        && isStartableActivityType((String) getModelActivity(activity).getProperty(BpmnProperties.TYPE.name()))) {
      return true;
    }
    return execution.getNonEventScopeExecutions().stream().anyMatch(this::isRunningActivity);
  }

  /** A migrated ordinary wrapper may have entered without any selected inner work. */
  public boolean hasOnlyEnabledActivities(ActivityExecution execution) {
    if (isEnabledExecution(execution)) {
      return true;
    }
    if (execution.getActivity() instanceof ActivityImpl activity
        && activity.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior) {
      return false;
    }
    List<? extends ActivityExecution> children = execution.getNonEventScopeExecutions();
    return !children.isEmpty() && children.stream().allMatch(this::hasOnlyEnabledActivities);
  }

  public ActivityExecution getDirectChild(ActivityExecution scope, ActivityExecution execution) {
    ActivityExecution child = execution;
    while (child.getParent() != null && child.getParent() != scope) {
      child = child.getParent();
    }
    return child;
  }

  /** Existing ordinary wrappers may contain enabled tokens following migration. */
  public boolean canActivate(ActivityExecution scope, ActivityImpl activity) {
    if (!isSequentialOrdering(getAdHocScope(scope))) {
      return true;
    }
    List<? extends ActivityExecution> running = scope.getNonEventScopeExecutions().stream()
        .filter(this::isRunningActivity).toList();
    if (running.isEmpty()) {
      return true;
    }
    return running.size() == 1 && getEnabledExecutions(scope, activity).stream()
        .anyMatch(token -> getDirectChild(scope, token) == running.get(0));
  }

  /** Resolve a model activity to its generated multi-instance body when present. */
  public ActivityImpl getExecutionActivity(ActivityImpl activity) {
    if (activity.getFlowScope() instanceof ActivityImpl flowScope
        && flowScope.getActivityBehavior() instanceof MultiInstanceActivityBehavior) {
      return flowScope;
    }
    return activity;
  }

  /** Expose the BPMN activity ID, never the generated multi-instance body ID. */
  public ActivityImpl getModelActivity(ActivityImpl activity) {
    if (activity.getActivityBehavior() instanceof MultiInstanceActivityBehavior
        && activity.getActivities().size() == 1) {
      return activity.getActivities().get(0);
    }
    return activity;
  }

  public boolean isCompletionRequested(ActivityExecution execution) {
    return execution != null && Boolean.TRUE.equals(execution.getVariableLocal(
        AdHocSubProcessActivityBehavior.AD_HOC_COMPLETION_CONDITION_SATISFIED));
  }

  protected ActivityImpl getAdHocScope(ActivityExecution adHocScopeExecution) {
    if (adHocScopeExecution == null || !(adHocScopeExecution.getActivity() instanceof ActivityImpl)) {
      return null;
    }
    return (ActivityImpl) adHocScopeExecution.getActivity();
  }
}
