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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.impl.migration.instance.MigratingActivityInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingProcessInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingVariableInstance;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.commons.utils.CollectionUtil;

/**
 * Validates that when an activity instance has a variable with the same name twice (as a scope execution variable and
 * a concurrent variable parent execution variable), no situation occurs in which either one is overwritten.
 *
 * @author Thorben Lindhauer
 */
public class VariableConflictActivityInstanceValidator implements MigratingActivityInstanceValidator {

  @Override
  public void validate(MigratingActivityInstance migratingInstance, MigratingProcessInstance migratingProcessInstance,
      MigratingActivityInstanceValidationReportImpl instanceReport) {

    ScopeImpl sourceScope = migratingInstance.getSourceScope();
    ScopeImpl targetScope = migratingInstance.getTargetScope();

    if (migratingInstance.migrates()) {
      boolean becomesNonScope = sourceScope.isScope() && !targetScope.isScope();
      if (becomesNonScope) {
        Map<String, List<MigratingVariableInstance>> dependentVariablesByName = getMigratingVariableInstancesByName(migratingInstance);
        for (var entry : dependentVariablesByName.entrySet()) {
          String variableName = entry.getKey();
          if (entry.getValue().size() > 1) {
            instanceReport.addFailure("The variable '" + variableName + "' exists in both, this scope and "
                + "concurrent local in the parent scope. "
                + "Migrating to a non-scope activity would overwrite one of them.");
          }
        }
      }
    }
  }

  protected Map<String, List<MigratingVariableInstance>> getMigratingVariableInstancesByName(MigratingActivityInstance activityInstance) {
    Map<String, List<MigratingVariableInstance>> result = new HashMap<>();

    for (MigratingInstance migratingInstance : activityInstance.getMigratingDependentInstances()) {
      if (migratingInstance instanceof MigratingVariableInstance migratingVariableInstance) {
        CollectionUtil.addToMapOfLists(result, migratingVariableInstance.getVariableName(), migratingVariableInstance);
      }
    }

    return result;
  }

}
