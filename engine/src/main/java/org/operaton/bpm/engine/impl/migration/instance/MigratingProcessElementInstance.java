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
package org.operaton.bpm.engine.impl.migration.instance;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.migration.MigrationInstruction;

/**
 * @author Thorben Lindhauer
 *
 */
public @NullMarked abstract class MigratingProcessElementInstance implements MigratingInstance {

  protected @Nullable MigrationInstruction migrationInstruction;

  protected @Nullable ScopeImpl sourceScope;
  protected @Nullable ScopeImpl targetScope;
  // changes from source to target scope during migration
  protected @Nullable ScopeImpl currentScope;

  protected @Nullable MigratingScopeInstance parentInstance;

  public @Nullable ScopeImpl getSourceScope() {
    return sourceScope;
  }

  public @Nullable ScopeImpl getTargetScope() {
    return targetScope;
  }

  public @Nullable ScopeImpl getCurrentScope() {
    return currentScope;
  }

  public @Nullable MigrationInstruction getMigrationInstruction() {
    return migrationInstruction;
  }

  public @Nullable MigratingScopeInstance getParent() {
    return parentInstance;
  }

  public boolean migratesTo(ScopeImpl other) {
    return other == targetScope;
  }

  public abstract void setParent(@Nullable MigratingScopeInstance parentInstance);

  public abstract void addMigratingDependentInstance(MigratingInstance migratingInstance);

  public abstract ExecutionEntity resolveRepresentativeExecution();

  public @Nullable MigratingActivityInstance getClosestAncestorActivityInstance() {
    MigratingScopeInstance ancestorInstance = parentInstance;

    while (ancestorInstance != null && !(ancestorInstance instanceof MigratingActivityInstance)) {
      ancestorInstance = ancestorInstance.getParent();
    }

    return (MigratingActivityInstance) ancestorInstance;
  }

}
