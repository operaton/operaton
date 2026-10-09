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

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler.AsyncContinuationConfiguration;
import org.operaton.bpm.engine.impl.migration.MigrationLogger;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.util.EnsureUtil;
import org.operaton.bpm.engine.migration.MigrationInstruction;
import org.operaton.bpm.engine.runtime.TransitionInstance;

import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocStartability.AD_HOC_ENABLED_ACTIVITY;
import static java.util.Objects.requireNonNull;

/**
 * @author Thorben Lindhauer
 *
 */
public @NullMarked class MigratingTransitionInstance extends MigratingProcessElementInstance implements MigratingInstance {

  public static final MigrationLogger MIGRATION_LOGGER = ProcessEngineLogger.MIGRATION_LOGGER;

  protected ExecutionEntity representativeExecution;

  protected TransitionInstance transitionInstance;
  protected @Nullable MigratingAsyncJobInstance jobInstance;
  protected List<MigratingInstance> migratingDependentInstances = new ArrayList<>();
  protected boolean activeState;
  protected final boolean adHocEnabledActivity;
  protected boolean pendingActivityEnd;
  protected boolean pendingScopedActivityEnd;


  public MigratingTransitionInstance(
      TransitionInstance transitionInstance,
      MigrationInstruction migrationInstruction,
      ScopeImpl sourceScope,
      ScopeImpl targetScope,
      ExecutionEntity asyncExecution) {
    this.transitionInstance = transitionInstance;
    this.migrationInstruction = migrationInstruction;
    this.sourceScope = sourceScope;
    this.targetScope = targetScope;
    this.currentScope = sourceScope;
    this.representativeExecution = asyncExecution;
    this.activeState = representativeExecution.isActive();
    this.adHocEnabledActivity = isAdHocEnabledActivity(sourceScope, asyncExecution);
  }

  @Override
  public boolean isDetached() {
    return adHocEnabledActivity ? getParent() == null : getJobInstance().isDetached();
  }

  @Override
  public @Nullable MigratingActivityInstance getParent() {
    return (MigratingActivityInstance) super.getParent();
  }

  @Override
  public void detachState() {
    if (!adHocEnabledActivity) {
      getJobInstance().detachState();
    }
    for (MigratingInstance dependentInstance : migratingDependentInstances) {
      dependentInstance.detachState();
    }

    ExecutionEntity execution = resolveRepresentativeExecution();
    execution.setActive(false);
    MigratingActivityInstance parent = getParent();
    if (pendingScopedActivityEnd) {
      ExecutionEntity carrier = requireNonNull(execution.getParent());
      execution.setParent(null);
      if (parent != null) {
        parent.destroyAttachableExecution(carrier);
      }
    } else if (parent != null) {
      parent.destroyAttachableExecution(execution);
    }

    setParent(null);
  }

  @Override
  public void attachState(MigratingScopeInstance scopeInstance) {
    if (!(scopeInstance instanceof MigratingActivityInstance)) {
      throw MIGRATION_LOGGER.cannotHandleChild(scopeInstance, this);
    }

    MigratingActivityInstance activityInstance = (MigratingActivityInstance) scopeInstance;

    setParent(activityInstance);

    ExecutionEntity carrier = activityInstance.createAttachableExecution();
    if (pendingScopedActivityEnd) {
      resolveRepresentativeExecution().setParent(carrier);
    } else {
      representativeExecution = carrier;
    }
    representativeExecution.setActivityInstanceId(null);
    representativeExecution.setActive(activeState);

    if (!adHocEnabledActivity) {
      getJobInstance().attachState(this);
    }

    for (MigratingInstance dependentInstance : migratingDependentInstances) {
      dependentInstance.attachState(this);
    }
  }

  @Override
  public ExecutionEntity resolveRepresentativeExecution() {
    if (representativeExecution.getReplacedBy() != null) {
      return representativeExecution.resolveReplacedBy();
    }
    else {
      return representativeExecution;
    }
  }

  @Override
  public void attachState(MigratingTransitionInstance targetTransitionInstance) {
    throw MIGRATION_LOGGER.cannotAttachToTransitionInstance(this);
  }

  public void setDependentJobInstance(MigratingAsyncJobInstance jobInstance) {
    this.jobInstance = jobInstance;
    pendingActivityEnd = MigratingAsyncJobInstance.isActivityEnd(
        (AsyncContinuationConfiguration) jobInstance.getJobEntity().getJobHandlerConfiguration());
    pendingScopedActivityEnd = pendingActivityEnd && sourceScope != null && sourceScope.isScope()
        && representativeExecution.isScope();
  }

  @Override
  public void addMigratingDependentInstance(MigratingInstance migratingInstance) {
    migratingDependentInstances.add(migratingInstance);
  }

  public List<MigratingInstance> getMigratingDependentInstances() {
    return migratingDependentInstances;
  }

  /** An activity-end continuation can retain its undisposed activity scope and output mappings. */
  public boolean isPendingScopedActivityEnd() {
    return pendingScopedActivityEnd;
  }

  public boolean isPendingActivityEnd() {
    return pendingActivityEnd;
  }

  @Override
  public void migrateState() {
    ExecutionEntity representativeExec = resolveRepresentativeExecution();

    requireNonNull(targetScope);
    representativeExec.setProcessDefinition(targetScope.getProcessDefinition());
    representativeExec.setActivity((PvmActivity) targetScope);
    currentScope = targetScope;
    if (pendingActivityEnd && pendingScopedActivityEnd != targetScope.isScope()) {
      changeActivityScope();
      representativeExec = resolveRepresentativeExecution();
    }
    if (isPendingScopedActivityEnd()) {
      ExecutionEntity parent = representativeExec.getParent();
      if (parent != null && parent.isConcurrent()) {
        parent.setProcessDefinition(targetScope.getProcessDefinition());
      }
    }
  }

  /** Apply the ordinary mapped-activity scope conversion policy without replaying start or input mappings. */
  protected void changeActivityScope() {
    for (MigratingInstance dependent : migratingDependentInstances) {
      if (dependent instanceof MigratingActivityEndScope retainedScope) {
        retainedScope.removeUnmappedDependentInstances();
      }
    }
    getJobInstance().detachState();
    for (MigratingInstance dependent : migratingDependentInstances) {
      dependent.detachState();
    }

    ExecutionEntity execution = resolveRepresentativeExecution();
    if (pendingScopedActivityEnd) {
      ExecutionEntity carrier = requireNonNull(execution.getParent());
      carrier.setProcessDefinition(requireNonNull(targetScope).getProcessDefinition());
      carrier.setActivity(execution.getActivity());
      carrier.setActivityInstanceId(null);
      carrier.setActive(execution.isActive());
      carrier.setEnded(execution.isEnded());
      execution.remove();
      representativeExecution = carrier;
      pendingScopedActivityEnd = false;
    } else {
      ExecutionEntity scope = execution.createExecution();
      scope.setScope(true);
      scope.setConcurrent(false);
      scope.setActivityInstanceId(null);
      scope.setActive(execution.isActive());
      scope.setEnded(execution.isEnded());
      execution.setActivity(null);
      if (!execution.isConcurrent()) {
        execution.leaveActivityInstance();
      }
      representativeExecution = scope;
      pendingScopedActivityEnd = true;
    }

    getJobInstance().attachState(this);
    for (MigratingInstance dependent : migratingDependentInstances) {
      dependent.attachState(this);
    }
  }

  @Override
  public void migrateDependentEntities() {
    if (!adHocEnabledActivity) {
      getJobInstance().migrateState();
      getJobInstance().migrateDependentEntities();
    }

    for (MigratingInstance dependentInstance : migratingDependentInstances) {
      dependentInstance.migrateState();
      dependentInstance.migrateDependentEntities();
    }
  }

  public TransitionInstance getTransitionInstance() {
    return transitionInstance;
  }

  /**
   * Else asyncBefore
   */
  public boolean isAsyncAfter() {
    return !adHocEnabledActivity && ((MigratingAsyncJobInstance) getJobInstance()).isAsyncAfter();
  }

  public boolean isAsyncBefore() {
    return !adHocEnabledActivity && ((MigratingAsyncJobInstance) getJobInstance()).isAsyncBefore();
  }

  public boolean isAdHocEnabledActivity() {
    return adHocEnabledActivity;
  }

  public static boolean isAdHocEnabledActivity(@Nullable ScopeImpl activity, ExecutionEntity execution) {
    if (!Boolean.TRUE.equals(execution.getVariableLocal(AD_HOC_ENABLED_ACTIVITY))) {
      return false;
    }
    ScopeImpl scope = activity == null ? null : activity.getFlowScope();
    while (scope != null && !(scope.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior)) {
      scope = scope.getFlowScope();
    }
    return scope != null;
  }

  public MigratingJobInstance getJobInstance() {
    EnsureUtil.ensureNotNull("jobInstance", jobInstance);
    requireNonNull(jobInstance);
    return jobInstance;
  }

  @Override
  public void setParent(@Nullable MigratingScopeInstance parentInstance) {
    if (parentInstance != null && !(parentInstance instanceof MigratingActivityInstance)) {
      throw MIGRATION_LOGGER.cannotHandleChild(parentInstance, this);
    }

    MigratingActivityInstance parentActivityInstance = (MigratingActivityInstance) parentInstance;

    if (this.parentInstance != null) {
      ((MigratingActivityInstance) this.parentInstance).removeChild(this);
    }

    this.parentInstance = parentActivityInstance;

    if (parentInstance != null) {
      parentActivityInstance.addChild(this);
    }
  }

}
