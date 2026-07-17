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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
 * potentially startable unless they are compensation handlers, event
 * subprocesses, or downstream activities with an incoming sequence flow from
 * inside the ad-hoc scope.
 * Runtime ordering checks are kept here as well so future discovery APIs and
 * scheduler integration can reuse the same decision point.
 */
public class AdHocStartability {

  public static final AdHocStartability INSTANCE = new AdHocStartability();

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
        .filter(activity -> isPotentiallyStartableActivity(adHocScope, activity))
        .collect(Collectors.toList());
  }

  /**
   * Returns activities that are currently startable in the given ad-hoc scope execution.
   */
  public List<ActivityImpl> getStartableActivities(ActivityExecution adHocScopeExecution) {
    ActivityImpl adHocScope = getAdHocScope(adHocScopeExecution);
    if (isSequentialOrdering(adHocScope) && hasOpenChildExecutions(adHocScopeExecution)) {
      return Collections.emptyList();
    }

    return getPotentiallyStartableActivities(adHocScope);
  }

  /**
   * Checks if an activity can be started in the current ad-hoc scope execution.
   */
  public boolean isStartableActivity(ActivityExecution adHocScopeExecution, ActivityImpl activity) {
    ActivityImpl adHocScope = getAdHocScope(adHocScopeExecution);
    if (!isPotentiallyStartableActivity(adHocScope, activity)) {
      return false;
    }

    return !isSequentialOrdering(adHocScope) || !hasOpenChildExecutions(adHocScopeExecution);
  }

  /**
   * Checks if an activity is startable according to static ad-hoc model rules.
   */
  public boolean isPotentiallyStartableActivity(ActivityImpl adHocScope, ActivityImpl activity) {
    if (adHocScope == null || activity == null || activity.isCompensationHandler()
        || activity.isTriggeredByEvent()) {
      return false;
    }

    String type = (String) activity.getProperty(BpmnProperties.TYPE.name());
    if (!isStartableActivityType(type)) {
      return false;
    }

    return !hasIncomingTransitionFromAdHocScope(adHocScope, activity);
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
   * <p>Async continuations and scope activities may leave child executions that
   * are not currently active but still represent running ad-hoc work. Those
   * executions must still block sequential triggers and auto-completion.
   */
  public boolean hasOpenChildExecutions(ActivityExecution adHocScopeExecution) {
    return adHocScopeExecution != null
        && adHocScopeExecution.getExecutions().stream().anyMatch(child -> !child.isEnded());
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

  protected ActivityImpl getAdHocScope(ActivityExecution adHocScopeExecution) {
    if (adHocScopeExecution == null || !(adHocScopeExecution.getActivity() instanceof ActivityImpl)) {
      return null;
    }
    return (ActivityImpl) adHocScopeExecution.getActivity();
  }
}
