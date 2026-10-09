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

import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;

/**
 * Adapts the retained scope of an activity-end transition to the existing scope dependency parsers.
 * The activity already ended, so this is not another migrating activity instance and must not
 * create history or replay listeners. Its execution and dependencies move together intact.
 */
public class MigratingActivityEndScope extends MigratingActivityInstance {

  protected final MigratingTransitionInstance transitionInstance;

  public MigratingActivityEndScope(MigratingTransitionInstance transitionInstance) {
    super(transitionInstance.getSourceScope(), transitionInstance.resolveRepresentativeExecution());
    this.transitionInstance = transitionInstance;
    sourceScope = transitionInstance.getSourceScope();
    targetScope = transitionInstance.getTargetScope();
  }

  @Override
  public ExecutionEntity resolveRepresentativeExecution() {
    return transitionInstance.resolveRepresentativeExecution();
  }

  @Override
  public void removeUnmappedDependentInstances() {
    super.removeUnmappedDependentInstances();
    removingDependentInstances.clear();
  }

  @Override
  public void detachState() {
    // Retain subscriptions and timers on the scope execution that is itself reattached.
  }

  @Override
  public void attachState(MigratingTransitionInstance transitionInstance) {
    // The owning transition reattaches the retained scope execution.
  }

  @Override
  public void migrateState() {
    currentScope = targetScope;
    removeUnmappedDependentInstances();
  }
}
