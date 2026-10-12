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
package org.operaton.bpm.engine.impl.migration.instance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.migration.instance.parser.MigratingInstanceParseContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.migration.MigrationInstruction;

import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocStartability.AD_HOC_ENABLED_ACTIVITY;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.AD_HOC_COMPLETED_ACTIVITY_IDS;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.AD_HOC_COMPLETION_CONDITION_SATISFIED;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.AD_HOC_LAST_COMPLETED_ACTIVITY_ID;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES;

/**
 * Rewrites the engine-owned activity references after the scope and its variables have migrated.
 * Counts, duplicate completions, the completion latch and application variables are left intact.
 */
public class MigratingAdHocState implements MigratingInstance {

  public static final String RETIRED_CONTEXT = "adHocRetiredContext";
  public static final String RETIRED_CONTEXT_VALUE = "operaton:ad-hoc-retired-context:v1";

  public static final List<String> STATE_VARIABLES = List.of(
      "adHocActiveActivityIds", "nrOfActiveAdHocActivities", AD_HOC_COMPLETED_ACTIVITY_IDS,
      NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES, AD_HOC_LAST_COMPLETED_ACTIVITY_ID, AD_HOC_COMPLETION_CONDITION_SATISFIED,
      "adHocEnabledActivityIds", "nrOfEnabledAdHocActivities", "adHocEnabledActivity", RETIRED_CONTEXT);

  /** Initialize a fresh owner or resume identified retired state without replaying initial activities. */
  public static void initializeScopeContext(ExecutionEntity execution) {
    if (hasRetiredContext(execution)) {
      execution.removeVariableLocal(RETIRED_CONTEXT);
      initializeMissingCompletionState(execution);
      return;
    }
    execution.setVariableLocal(AD_HOC_COMPLETED_ACTIVITY_IDS, new ArrayList<String>());
    execution.setVariableLocal(NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES, 0);
    execution.setVariableLocal(AD_HOC_LAST_COMPLETED_ACTIVITY_ID, null);
    execution.setVariableLocal(AD_HOC_COMPLETION_CONDITION_SATISFIED, false);
  }


  protected static void initializeMissingCompletionState(ExecutionEntity execution) {
    if (!execution.hasVariableLocal(AD_HOC_COMPLETED_ACTIVITY_IDS)) {
      execution.setVariableLocal(AD_HOC_COMPLETED_ACTIVITY_IDS, new ArrayList<String>());
    }
    if (!execution.hasVariableLocal(NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)) {
      execution.setVariableLocal(NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES, 0);
    }
    if (!execution.hasVariableLocal(AD_HOC_LAST_COMPLETED_ACTIVITY_ID)) {
      execution.setVariableLocal(AD_HOC_LAST_COMPLETED_ACTIVITY_ID, null);
    }
    if (!execution.hasVariableLocal(AD_HOC_COMPLETION_CONDITION_SATISFIED)) {
      execution.setVariableLocal(AD_HOC_COMPLETION_CONDITION_SATISFIED, false);
    }
  }

  public static boolean hasRetiredContext(ExecutionEntity execution) {
    return RETIRED_CONTEXT_VALUE.equals(execution.getVariableLocal(RETIRED_CONTEXT));
  }

  /** Only transferred locals can collide; inherited variables outside the new context stay ordinary data. */
  public static boolean hasIncomingEnabledMarkerCollision(ScopeImpl source, ScopeImpl target,
      List<MigratingInstance> dependentInstances) {
    return !isInAdHocContext(source == null ? null : source.getFlowScope())
        && isInAdHocContext(target == null ? null : target.getFlowScope())
        && dependentInstances.stream().anyMatch(dependent -> dependent instanceof MigratingVariableInstance variable
            && AD_HOC_ENABLED_ACTIVITY.equals(variable.getVariableName()));
  }

  protected static boolean isInAdHocContext(ScopeImpl scope) {
    while (scope != null) {
      if (scope.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior) {
        return true;
      }
      scope = scope.getFlowScope();
    }
    return false;
  }

  protected final MigratingActivityInstance owningInstance;
  protected final Map<String, String> activityIds = new HashMap<>();

  public MigratingAdHocState(MigratingActivityInstance owningInstance, MigratingInstanceParseContext parseContext) {
    this.owningInstance = owningInstance;
    collectActivityIds(owningInstance.getSourceScope().getActivities(), parseContext);
  }

  protected void collectActivityIds(List<ActivityImpl> activities, MigratingInstanceParseContext parseContext) {
    for (ActivityImpl activity : activities) {
      MigrationInstruction instruction = parseContext.getInstructionFor(activity.getId());
      if (instruction != null) {
        activityIds.put(activity.getId(), instruction.getTargetActivityId());
      }
      collectActivityIds(activity.getActivities(), parseContext);
    }
  }

  @Override
  public void migrateState() {
    ExecutionEntity execution = owningInstance.resolveRepresentativeExecution();
    migrateActivityIds(execution, AD_HOC_COMPLETED_ACTIVITY_IDS);
    Object lastCompleted = execution.getVariableLocal(AD_HOC_LAST_COMPLETED_ACTIVITY_ID);
    if (lastCompleted instanceof String id && activityIds.containsKey(id) && !id.equals(activityIds.get(id))) {
      execution.setVariableLocal(AD_HOC_LAST_COMPLETED_ACTIVITY_ID, activityIds.get(id));
    }
  }

  protected void migrateActivityIds(ExecutionEntity execution, String variableName) {
    Object value = execution.getVariableLocal(variableName);
    if (value instanceof Collection<?> ids) {
      List<Object> migratedIds = ids.stream()
          .map(id -> id instanceof String activityId ? activityIds.getOrDefault(activityId, activityId) : id)
          .collect(Collectors.toCollection(ArrayList::new));
      // An unmapped, already completed activity remains a historical source ID.
      if (!migratedIds.equals(new ArrayList<>(ids))) {
        execution.setVariableLocal(variableName, migratedIds);
      }
    }
  }

  /** Refresh live context after every child has been reattached, using the same rules as normal execution. */
  public static void refreshActiveState(MigratingActivityInstance instance) {
    if (instance.getTargetScope() != null
        && instance.getTargetScope().getActivityBehavior() instanceof AdHocSubProcessActivityBehavior behavior) {
      behavior.activityContinued(instance.resolveRepresentativeExecution());
    }
  }

  @Override
  public boolean isDetached() {
    return false;
  }

  @Override
  public void detachState() {
    // The regular variable instances own persistence and attachment.
  }

  @Override
  public void attachState(MigratingScopeInstance targetActivityInstance) {
    // The owning activity instance resolves the current execution after reattachment.
  }

  @Override
  public void attachState(MigratingTransitionInstance targetTransitionInstance) {
    // This state belongs only to an entered ad-hoc activity instance.
  }

  @Override
  public void migrateDependentEntities() {
    // No additional dependent entities.
  }
}
