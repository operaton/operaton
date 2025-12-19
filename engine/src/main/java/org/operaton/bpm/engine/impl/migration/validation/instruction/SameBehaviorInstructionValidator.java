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
package org.operaton.bpm.engine.impl.migration.validation.instruction;

import java.util.*;

import org.operaton.bpm.engine.impl.bpmn.behavior.CallActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.CaseCallActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.EventSubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.SubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.commons.utils.CollectionUtil;

public class SameBehaviorInstructionValidator implements MigrationInstructionValidator {

  public static final List<Set<Class<?>>> EQUIVALENT_BEHAVIORS =
      new ArrayList<>();

  static {
    EQUIVALENT_BEHAVIORS.add(CollectionUtil.<Class<?>>asHashSet(
      CallActivityBehavior.class, CaseCallActivityBehavior.class
    ));

    EQUIVALENT_BEHAVIORS.add(CollectionUtil.<Class<?>>asHashSet(
      SubProcessActivityBehavior.class, EventSubProcessActivityBehavior.class
    ));
  }

  protected Map<Class<?>, Set<Class<?>>> equivalentBehaviors = new HashMap<>();

  public SameBehaviorInstructionValidator() {
    this(EQUIVALENT_BEHAVIORS);
  }

  public SameBehaviorInstructionValidator(List<Set<Class<?>>> equivalentBehaviors) {
    for (Set<Class<?>> equivalenceClass : equivalentBehaviors) {
      for (Class<?> clazz : equivalenceClass) {
        this.equivalentBehaviors.put(clazz, equivalenceClass);
      }
    }
  }

  @Override
  public void validate(ValidatingMigrationInstruction instruction, ValidatingMigrationInstructions instructions, MigrationInstructionValidationReportImpl report) {
    ActivityImpl sourceActivity = instruction.getSourceActivity();
    ActivityImpl targetActivity = instruction.getTargetActivity();

    Class<?> sourceBehaviorClass = sourceActivity.getActivityBehavior().getClass();
    Class<?> targetBehaviorClass = targetActivity.getActivityBehavior().getClass();

    if (!sameBehavior(sourceBehaviorClass, targetBehaviorClass)) {
      report.addFailure("Activities have incompatible types "
          + "(%s is not compatible with ".formatted(sourceBehaviorClass.getSimpleName()) + targetBehaviorClass.getSimpleName() + ")");
    }
  }

  protected boolean sameBehavior(Class<?> sourceBehavior, Class<?> targetBehavior) {

    if (sourceBehavior == targetBehavior) {
      return true;
    }
    else {
      Set<Class<?>> behaviors = this.equivalentBehaviors.get(sourceBehavior);
      if (behaviors != null) {
        return behaviors.contains(targetBehavior);
      }
      else {
        return false;
      }
    }
  }

}
