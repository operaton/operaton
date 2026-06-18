/*
 * Copyright 2026 FINOS
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
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ActivityTypes;
import org.operaton.bpm.engine.impl.Condition;
import org.operaton.bpm.engine.impl.bpmn.helper.CompensationUtil;
import org.operaton.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.el.Expression;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.pvm.delegate.CompositeActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;

/**
 * Implementation of the BPMN 2.0 Ad-Hoc Sub-Process.
 *
 * <p>An Ad-Hoc Sub-Process is a specialized type of Sub-Process that has a set
 * of Activities that can be performed in any order, and some of which may not
 * be performed at all. Initial activities are activated from the
 * {@code activeTasksCollection} extension property and additional starter activities may be
 * activated via {@code RuntimeService#triggerAdHocActivities(String, Collection, Map)}.
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

  /**
   * On entry into an ad-hoc subprocess, only starter activities named in the
    * optional {@code activeTasksCollection} extension property are activated in parallel.
   */
  @Override
  public void execute(ActivityExecution execution) throws Exception {
    List<ActivityImpl> starterActivities = getInitiallyActivatableChildActivities(execution);
    List<String> configuredActiveTaskIds = getConfiguredActiveTaskIds(execution);
    validateConfiguredActiveTaskIds(execution, starterActivities, configuredActiveTaskIds);
    List<ActivityImpl> adHocActivities = filterStarterActivities(starterActivities, configuredActiveTaskIds);
    boolean adHocActivityStarted = !adHocActivities.isEmpty();

    for (ActivityImpl activity : adHocActivities) {
      if (!isActivityAlreadyActiveInScope(execution, activity.getId())) {
        startAdHocActivity(execution, activity);
      }
    }

    evaluateCompletionCondition(execution, adHocActivityStarted);
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
    endedExecution.remove();
    scopeExecution.forceUpdate();

    // Evaluate before pruning so ad-hoc scope metadata and active children are still intact.
    evaluateCompletionCondition(scopeExecution, adHocScopeActivity, true);

    // If completion handling moved execution out of the ad-hoc scope, stop here.
    if (scopeExecution.getActivity() != adHocScopeActivity) {
      return;
    }

    scopeExecution.tryPruneLastConcurrentChild();
    scopeExecution.forceUpdate();
  }

  /**
   * Called by the PVM when all concurrent executions inside the scope have
   * finished. For ad-hoc, this is the last chance to evaluate the completion
   * condition. If the condition is met (or not defined) the subprocess
   * proceeds; otherwise the scope execution stays open for further triggers.
   */
  @Override
  public void complete(ActivityExecution scopeExecution) {
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

    Condition completionCondition = (Condition) scopeActivity
        .getProperty(BpmnParse.PROPERTYNAME_AD_HOC_COMPLETION_CONDITION);

    boolean conditionMet;
    if (completionCondition == null) {
      // No condition: complete only if auto-complete is enabled and at least one
      // ad-hoc activity has started with no active children left.
      conditionMet = isAutoCompleteEnabled(scopeActivity)
          && adHocActivityStarted
          && isAdHocScopeActivity(scopeActivity)
          && !hasActiveChildExecutions(scopeExecution);
    } else {
      conditionMet = completionCondition.evaluate(scopeExecution, scopeExecution);
    }

    if (!conditionMet) {
      return;
    }

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
    List<ActivityExecution> children = new ArrayList<>(scopeExecution.getExecutions());
    for (ActivityExecution child : children) {
      child.interrupt("adHocCompletionConditionMet");
    }
  }

  protected boolean hasActiveChildExecutions(ActivityExecution scopeExecution) {
    return scopeExecution.getExecutions().stream().anyMatch(ActivityExecution::isActive);
  }

  protected boolean isAutoCompleteEnabled(ActivityImpl scopeActivity) {
    Object autoComplete = scopeActivity.getProperty(BpmnParse.PROPERTYNAME_AD_HOC_AUTO_COMPLETE);
    return autoComplete == null || Boolean.TRUE.equals(autoComplete);
  }

  protected List<ActivityImpl> getInitiallyActivatableChildActivities(ActivityExecution scopeExecution) {
    ActivityImpl adHocSubProcess = (ActivityImpl) scopeExecution.getActivity();
    return adHocSubProcess.getActivities().stream()
        .filter(this::isAutoActivatable)
        .filter(activity -> !hasIncomingTransitionFromAdHocScope(adHocSubProcess, activity))
        .collect(Collectors.toList());
  }

  protected boolean hasIncomingTransitionFromAdHocScope(ActivityImpl adHocScope, ActivityImpl activity) {
    return AdHocSubProcessValidationHelper.hasIncomingTransitionFromAdHocScope(adHocScope, activity);
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

  protected boolean isActivityAlreadyActiveInScope(ActivityExecution scopeExecution, String activityId) {
    return scopeExecution.getExecutions().stream()
        .anyMatch(child -> child.getActivity() != null
            && activityId.equals(child.getActivity().getId())
            && child.isActive());
  }

  protected boolean isAutoActivatable(ActivityImpl activity) {
    return AdHocSubProcessValidationHelper.isStartableActivityInAdHocScope(
        (ActivityImpl) activity.getFlowScope(), activity);
  }



  protected void startAdHocActivity(ActivityExecution scopeExecution, ActivityImpl targetActivity) {
    ActivityExecution childExecution = scopeExecution.createExecution();
    scopeExecution.forceUpdate();
    childExecution.setConcurrent(true);
    childExecution.setScope(false);
    childExecution.executeActivity(targetActivity);
  }

  @Override
  public void doLeave(ActivityExecution execution) {
    CompensationUtil.createEventScopeExecution((ExecutionEntity) execution);
    super.doLeave(execution);
  }

}
