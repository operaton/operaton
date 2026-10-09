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
package org.operaton.bpm.engine.impl.migration.validation.instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocStartability;
import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.migration.instance.MigratingActivityInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingAdHocState;
import org.operaton.bpm.engine.impl.migration.instance.MigratingProcessElementInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingProcessInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingScopeInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingTransitionInstance;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;

/** Validates live state without rejecting empty or active ad-hoc scopes categorically. */
public class AdHocActivityInstanceValidator implements MigratingActivityInstanceValidator {

  @Override
  public void validate(MigratingActivityInstance instance, MigratingProcessInstance processInstance,
      MigratingActivityInstanceValidationReportImpl report) {
    boolean sourceAdHoc = instance.getSourceScope().getActivityBehavior() instanceof AdHocSubProcessActivityBehavior;
    boolean targetAdHoc = instance.getTargetScope() != null
        && instance.getTargetScope().getActivityBehavior() instanceof AdHocSubProcessActivityBehavior;
    if (sourceAdHoc && !targetAdHoc) {
      var execution = instance.resolveRepresentativeExecution();
      if (instance.getTargetScope() != null && execution.hasVariableLocal(MigratingAdHocState.RETIRED_CONTEXT)) {
        report.addFailure("Cannot retire an ad-hoc owner over reserved variable '" + MigratingAdHocState.RETIRED_CONTEXT + "'");
      }
      if (Boolean.TRUE.equals(execution.getVariableLocal("adHocCompletionConditionSatisfied"))) {
        report.addFailure("Cannot remove or convert an ad-hoc owner with a latched completion decision");
      }
      if (!AdHocStartability.INSTANCE.getEnabledExecutions(execution).isEmpty()) {
        report.addFailure("Cannot remove or convert an ad-hoc owner with enabled activities");
      }
      if (!AdHocStartability.INSTANCE.hasOpenChildExecutions(execution)) {
        report.addFailure("An idle entered ad-hoc subprocess must retain its mapped ad-hoc owner");
      }
    }
    if (!sourceAdHoc && targetAdHoc && !MigratingAdHocState.hasRetiredContext(instance.resolveRepresentativeExecution())) {
      for (String reserved : MigratingAdHocState.STATE_VARIABLES) {
        if (instance.resolveRepresentativeExecution().hasVariableLocal(reserved)) {
          report.addFailure("Cannot initialize a fresh ad-hoc owner over reserved variable '" + reserved + "'");
        }
      }
    }
    if (MigratingAdHocState.hasIncomingEnabledMarkerCollision(instance.getSourceScope(), instance.getTargetScope(),
        instance.getMigratingDependentInstances())) {
      report.addFailure("Cannot move ordinary activity state into an ad-hoc context with reserved variable 'adHocEnabledActivity'");
    }
    if (instance.getParent() == null) {
      validateTargetOrdering(processInstance, report);
    }
  }

  protected void validateTargetOrdering(MigratingProcessInstance processInstance,
      MigratingActivityInstanceValidationReportImpl report) {
    List<MigratingProcessElementInstance> candidates = new ArrayList<>(processInstance.getMigratingActivityInstances());
    candidates.addAll(processInstance.getMigratingTransitionInstances());
    Set<TargetOwner> owners = new HashSet<>();
    for (MigratingProcessElementInstance candidate : candidates) {
      for (ScopeImpl scope = candidate.getTargetScope(); scope != null; scope = scope.getFlowScope()) {
        if (scope instanceof ActivityImpl activity
            && activity.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior
            && AdHocStartability.INSTANCE.isSequentialOrdering(activity)) {
          MigratingProcessElementInstance anchor = candidate;
          while (anchor != null && !isTargetAncestor(anchor.getTargetScope(), scope)) {
            anchor = anchor.getParent();
          }
          if (anchor instanceof MigratingActivityInstance activityInstance) {
            owners.add(new TargetOwner(activity, activityInstance));
          }
        }
      }
    }
    for (TargetOwner owner : owners) {
      if (countTargetChildren(owner.anchor(), owner.scope(), processInstance) > 1) {
        report.addFailure("Cannot migrate to a sequential ad-hoc subprocess with more than one open child activity: "
            + owner.scope().getId());
      }
    }
  }

  protected boolean isTargetAncestor(ScopeImpl candidate, ScopeImpl scope) {
    while (scope != null && scope != candidate) {
      scope = scope.getFlowScope();
    }
    return candidate != null && scope == candidate;
  }

  protected record TargetOwner(ActivityImpl scope, MigratingActivityInstance anchor) { }

  protected int countTargetChildren(MigratingActivityInstance instance, ScopeImpl targetOwner,
      MigratingProcessInstance processInstance) {
    Set<Object> children = new HashSet<>();
    for (MigratingActivityInstance candidate : processInstance.getMigratingActivityInstances()) {
      if (AdHocStartability.INSTANCE.isRunningActivity(candidate.resolveRepresentativeExecution())) {
        collectTargetChild(instance, targetOwner, candidate, children);
      }
    }
    for (MigratingTransitionInstance candidate : processInstance.getMigratingTransitionInstances()) {
      // Neither ready tokens nor ordinary wrappers containing only ready tokens consume
      // a running activity slot. Gateway/event waits follow the same runtime rule.
      if (!candidate.isAdHocEnabledActivity()
          && AdHocStartability.INSTANCE.isRunningActivity(candidate.resolveRepresentativeExecution())) {
        collectTargetChild(instance, targetOwner, candidate, children);
      }
    }
    return children.size();
  }

  protected void collectTargetChild(MigratingActivityInstance instance, ScopeImpl targetOwner,
      MigratingProcessElementInstance candidate, Set<Object> children) {
    if (!isDescendant(instance, candidate)) {
      return;
    }
    ScopeImpl target = candidate.getTargetScope();
    if (target == null) {
      return;
    }
    ScopeImpl directChild = target;
    while (directChild != null && directChild.getFlowScope() != targetOwner) {
      directChild = directChild.getFlowScope();
    }
    if (directChild == null) {
      return;
    }
    // Preserve instance identity for repeated activations and MI bodies. Descendants of the
    // same mapped direct child count only once; an emerging ordinary scope also counts once.
    MigratingProcessElementInstance ancestor = candidate;
    while (ancestor != instance && ancestor != null) {
      if (ancestor.getTargetScope() == directChild) {
        children.add(ancestor);
        return;
      }
      ancestor = ancestor.getParent();
    }
    children.add(directChild);
  }

  protected boolean isDescendant(MigratingActivityInstance ancestor, MigratingProcessElementInstance candidate) {
    MigratingScopeInstance parent = candidate.getParent();
    while (parent != null && parent != ancestor) {
      parent = parent.getParent();
    }
    return parent == ancestor;
  }
}
