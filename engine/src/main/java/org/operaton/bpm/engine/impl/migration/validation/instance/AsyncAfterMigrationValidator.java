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
package org.operaton.bpm.engine.impl.migration.validation.instance;

import java.util.HashSet;
import java.util.Set;

import org.operaton.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler.AsyncContinuationConfiguration;
import org.operaton.bpm.engine.impl.migration.instance.MigratingAsyncJobInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingJobInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingProcessInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingTransitionInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingVariableInstance;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.TransitionImpl;

public class AsyncAfterMigrationValidator implements MigratingTransitionInstanceValidator {

  @Override
  public void validate(MigratingTransitionInstance migratingInstance, MigratingProcessInstance migratingProcessInstance,
      MigratingTransitionInstanceValidationReportImpl instanceReport) {
    ActivityImpl targetActivity = (ActivityImpl) migratingInstance.getTargetScope();

    if (targetActivity != null && migratingInstance.isAsyncAfter()) {
      MigratingJobInstance jobInstance = migratingInstance.getJobInstance();
      AsyncContinuationConfiguration config = (AsyncContinuationConfiguration) jobInstance.getJobEntity().getJobHandlerConfiguration();
      String sourceTransitionId = config.getTransitionId();

      if (migratingInstance.isPendingScopedActivityEnd() && !targetActivity.isScope()) {
        Set<String> names = new HashSet<>();
        for (MigratingInstance dependent : migratingInstance.getMigratingDependentInstances()) {
          if (dependent instanceof MigratingVariableInstance variable && !names.add(variable.getVariableName())) {
            instanceReport.addFailure("The variable '%s' exists in both, this scope and concurrent local in the parent scope. Migrating to a non-scope activity would overwrite one of them."
                .formatted(variable.getVariableName()));
          }
        }
      }

      if (targetActivity.getOutgoingTransitions().size() > 1
          && !((MigratingAsyncJobInstance) jobInstance).isDeferredActivityEnd()
          && !((MigratingAsyncJobInstance) jobInstance).isRetiringActivityEnd()) {
        if (sourceTransitionId == null) {
          instanceReport.addFailure("Transition instance is assigned to no sequence flow"
              + " and target activity has more than one outgoing sequence flow");
        }
        else {
          TransitionImpl matchingOutgoingTransition = targetActivity.findOutgoingTransition(sourceTransitionId);
          if (matchingOutgoingTransition == null) {
            instanceReport.addFailure("Transition instance is assigned to a sequence flow"
              + " that cannot be matched in the target activity");
          }
        }
      }
    }

  }

}
