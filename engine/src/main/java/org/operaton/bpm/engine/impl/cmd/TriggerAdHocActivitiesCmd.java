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
package org.operaton.bpm.engine.impl.cmd;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocStartability;
import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;

/**
 * Triggers starter activities inside an active ad-hoc subprocess execution.
 */
public class TriggerAdHocActivitiesCmd implements Command<Void>, Serializable {

  private static final long serialVersionUID = 1L;

  protected final String executionId;
  protected final Collection<String> activityIds;
  protected final Map<String, Map<String, Object>> activityVariables;
  protected final AdHocStartability startability = AdHocStartability.INSTANCE;

  public TriggerAdHocActivitiesCmd(String executionId,
                                   Collection<String> activityIds,
                                   Map<String, Map<String, Object>> activityVariables) {
    this.executionId = executionId;
    this.activityIds = activityIds;
    this.activityVariables = activityVariables;
  }

  @Override
  public Void execute(CommandContext commandContext) {
    ensureNotNull(BadUserRequestException.class, "executionId is null", "executionId", executionId);
    ensureNotNull(BadUserRequestException.class, "activityIds is null", "activityIds", activityIds);

    if (activityIds.isEmpty()) {
      throw new BadUserRequestException("activityIds is empty");
    }

    ExecutionEntity execution = commandContext.getExecutionManager().findExecutionById(executionId);
    ensureNotNull(BadUserRequestException.class, "execution " + executionId + " doesn't exist", "execution", execution);

    for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkUpdateProcessInstance(execution);
    }

    ActivityImpl adHocActivity = execution.getActivity();
    ensureNotNull(BadUserRequestException.class, "execution " + executionId + " has no current activity", "activity", adHocActivity);

    if (!(adHocActivity.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior)) {
      throw new BadUserRequestException("execution " + executionId + " is not waiting in an adHocSubProcess");
    }

    Set<String> uniqueActivityIds = new HashSet<>();
    List<String> normalizedActivityIds = new ArrayList<>();
    for (String activityId : activityIds) {
      ensureNotNull(BadUserRequestException.class, "activityId is null", "activityId", activityId);
      if (!uniqueActivityIds.add(activityId)) {
        throw new BadUserRequestException("duplicate adHoc activity '" + activityId + "' in request");
      }
      normalizedActivityIds.add(activityId);
    }

    if (activityVariables != null) {
      for (String keyedActivityId : activityVariables.keySet()) {
        if (!uniqueActivityIds.contains(keyedActivityId)) {
          throw new BadUserRequestException("variables provided for non-requested adHoc activity '" + keyedActivityId + "'");
        }
      }
    }

    List<ActivityImpl> targetActivities = new ArrayList<>(normalizedActivityIds.size());
    for (String activityId : normalizedActivityIds) {
      ActivityImpl targetActivity = adHocActivity.findActivity(activityId);
      ensureNotNull(BadUserRequestException.class,
          "adHoc activity '" + activityId + "' does not exist in adHocSubProcess " + adHocActivity.getId(),
          "targetActivity",
          targetActivity);

      if (!isStartableInAdHocScope(adHocActivity, targetActivity)) {
        throw new BadUserRequestException(
            "adHoc activity '" + activityId + "' is not startable in adHocSubProcess " + adHocActivity.getId());
      }

      targetActivities.add(targetActivity);
    }

    ensureOrderingAllowsTrigger(execution, adHocActivity, targetActivities);

    for (ActivityImpl targetActivity : targetActivities) {
      ActivityExecution childExecution = execution.createExecution();
      ((ExecutionEntity) execution).forceUpdate();
      childExecution.setConcurrent(true);
      childExecution.setScope(false);
      if (activityVariables != null) {
        Map<String, Object> localVariables = activityVariables.get(targetActivity.getId());
        if (localVariables != null && !localVariables.isEmpty()) {
          childExecution.setVariablesLocal(localVariables);
        }
      }
      childExecution.executeActivity(targetActivity);
    }

    return null;
  }

  protected boolean isStartableInAdHocScope(ActivityImpl adHocScope, ActivityImpl activity) {
    return startability.isPotentiallyStartableActivity(adHocScope, activity);
  }

  protected void ensureOrderingAllowsTrigger(ActivityExecution execution,
      ActivityImpl adHocActivity,
      List<ActivityImpl> targetActivities) {
    if (!startability.isSequentialOrdering(adHocActivity)) {
      return;
    }

    if (targetActivities.size() > 1) {
      throw new BadUserRequestException(
          "Sequential adHocSubProcess '" + adHocActivity.getId() + "' can trigger only one activity per request");
    }

    if (startability.hasActiveChildExecutions(execution)) {
      throw new BadUserRequestException(
          "Sequential adHocSubProcess '" + adHocActivity.getId() + "' already has an active child activity");
    }
  }
}
