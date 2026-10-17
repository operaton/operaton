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

import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocStartability;
import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.migration.instance.MigratingAdHocState;
import org.operaton.bpm.engine.impl.migration.instance.MigratingProcessInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingTransitionInstance;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;

/** Enabled tokens have not entered their target activity and do not require an async job. */
public class AdHocEnabledActivityValidator implements MigratingTransitionInstanceValidator {

  @Override
  public void validate(MigratingTransitionInstance instance, MigratingProcessInstance processInstance,
      MigratingTransitionInstanceValidationReportImpl report) {
    if (instance.getTargetScope() == null) {
      return;
    }
    if (!instance.isAdHocEnabledActivity()) {
      if (MigratingAdHocState.hasIncomingEnabledMarkerCollision(instance.getSourceScope(), instance.getTargetScope(),
          instance.getMigratingDependentInstances())) {
        report.addFailure("Cannot move ordinary transition state into an ad-hoc context with reserved variable 'adHocEnabledActivity'");
      }
      return;
    }
    ActivityImpl target = (ActivityImpl) instance.getTargetScope();
    ScopeImpl owner = target.getFlowScope();
    while (owner != null && !(owner.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior)) {
      owner = owner.getFlowScope();
    }
    var sourceOwner = instance.getParent();
    while (sourceOwner != null
        && !(sourceOwner.getSourceScope().getActivityBehavior() instanceof AdHocSubProcessActivityBehavior)) {
      sourceOwner = sourceOwner.getParent();
    }
    if (sourceOwner == null || sourceOwner.getTargetScope() != owner) {
      report.addFailure("An enabled ad-hoc activity must retain its original mapped activation owner");
      return;
    }
    ActivityImpl modelActivity = AdHocStartability.INSTANCE.getModelActivity(target);
    if (owner == null || modelActivity.isCompensationHandler() || modelActivity.isTriggeredByEvent()
        || !((AdHocSubProcessActivityBehavior) owner.getActivityBehavior()).isCompletableActivity(target)) {
      report.addFailure("An enabled ad-hoc activity must retain an ad-hoc activation owner and map to an eligible "
          + "activity or multi-instance body");
    }
  }
}
