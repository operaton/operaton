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
package org.operaton.bpm.engine.impl.migration.validation.instruction;

import java.util.List;

import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;

/** Preserves the relative hierarchy of retained ad-hoc contexts; live-state checks govern owner changes. */
public class AdHocScopeInstructionValidator implements MigrationInstructionValidator {

  @Override
  public void validate(ValidatingMigrationInstruction instruction, ValidatingMigrationInstructions instructions,
      MigrationInstructionValidationReportImpl report) {
    ScopeImpl source = nextAdHocScope(instruction.getSourceActivity().getFlowScope());
    ScopeImpl target = nextAdHocScope(instruction.getTargetActivity().getFlowScope());
    while (source != null) {
      List<ValidatingMigrationInstruction> mappings = instructions.getInstructionsBySourceScope(source);
      ScopeImpl mappedTarget = mappings.size() == 1 ? mappings.get(0).getTargetActivity() : null;
      if (mappedTarget != null && mappedTarget.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior) {
        while (target != null && target != mappedTarget) {
          target = nextAdHocScope(target.getFlowScope());
        }
        if (target == null) {
          report.addFailure("A retained ad-hoc ancestor must remain a mapped ancestor in the target");
          return;
        }
        target = nextAdHocScope(target.getFlowScope());
      }
      source = nextAdHocScope(source.getFlowScope());
    }
  }

  protected ScopeImpl nextAdHocScope(ScopeImpl scope) {
    while (scope != null && !(scope.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior)) {
      scope = scope.getFlowScope();
    }
    return scope;
  }
}
