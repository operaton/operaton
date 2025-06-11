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

import org.operaton.bpm.engine.delegate.BaseDelegateExecution;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.core.delegate.CoreActivityBehavior;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.history.event.HistoryEventProcessor;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.history.producer.HistoryEventProducer;
import org.operaton.bpm.engine.impl.migration.MigrationLogger;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.delegate.CompositeActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.delegate.MigrationObserverBehavior;
import org.operaton.bpm.engine.impl.pvm.delegate.ModificationObserverBehavior;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.migration.MigrationInstruction;
import org.operaton.bpm.engine.runtime.ActivityInstance;

import java.util.*;

/**
 * @author Thorben Lindhauer
 *
 */
public class MigratingActivityInstance extends MigratingScopeInstance implements MigratingInstance {

  public static final MigrationLogger MIGRATION_LOGGER = ProcessEngineLogger.MIGRATION_LOGGER;

  protected ActivityInstance activityInstance;
  // scope execution for actual scopes,
  // concurrent execution in case of non-scope activity with expanded tree
  protected ExecutionEntity representativeExecution;
  protected boolean activeState;

  protected List<RemovingInstance> removingDependentInstances = new ArrayList<>();
  protected List<MigratingInstance> migratingDependentInstances = new ArrayList<>();
  protected List<EmergingInstance> emergingDependentInstances = new ArrayList<>();

  protected Set<MigratingActivityInstance> childActivityInstances = new HashSet<>();
  protected Set<MigratingTransitionInstance> childTransitionInstances = new HashSet<>();
  protected Set<MigratingEventScopeInstance> childCompensationInstances = new HashSet<>();
  protected Set<MigratingCompensationEventSubscriptionInstance> childCompensationSubscriptionInstances = new HashSet<>();

  // behaves differently if the current activity is scope or not
  protected MigratingActivityInstanceBehavior instanceBehavior;

  /**
   * Creates a migrating activity instances
   */
  public MigratingActivityInstance(ActivityInstance activityInstance,
      MigrationInstruction migrationInstruction,
      ScopeImpl sourceScope,
      ScopeImpl targetScope,
      ExecutionEntity scopeExecution) {

    this.activityInstance = activityInstance;
    this.migrationInstruction = migrationInstruction;
    this.sourceScope = sourceScope;
    this.currentScope = sourceScope;
    this.targetScope = targetScope;
    this.representativeExecution = scopeExecution;
    this.instanceBehavior = determineBehavior(sourceScope);

    if (activityInstance.getChildActivityInstances().length == 0
      && activityInstance.getChildTransitionInstances().length == 0) {
      // active state is only relevant for child activity instances;
      // for all other instances, their respective executions are always inactive
      activeState = representativeExecution.isActive();
    }
  }

  /**
   * Creates an emerged activity instance
   */
  public MigratingActivityInstance(ScopeImpl targetScope, ExecutionEntity scopeExecution) {

    this.targetScope = targetScope;
    this.currentScope = targetScope;
    this.representativeExecution = scopeExecution;
    this.instanceBehavior = determineBehavior(targetScope);
  }


  protected MigratingActivityInstanceBehavior determineBehavior(ScopeImpl scope) {
    if (scope.isScope()) {
      return new MigratingScopeActivityInstanceBehavior();
    }
    else {
      return new MigratingNonScopeActivityInstanceBehavior();
    }
  }

  @Override
  public void detachChildren() {
    Set<MigratingActivityInstance> childrenCopy = new HashSet<>(childActivityInstances);
    // First detach all dependent entities, only then detach the activity instances.
    // This is because detaching activity instances may trigger execution tree compaction which in turn
    // may overwrite certain dependent entities (e.g. variables)
    for (MigratingActivityInstance child : childrenCopy) {
      child.detachDependentInstances();
    }

    for (MigratingActivityInstance child : childrenCopy) {
      child.detachState();
    }

    Set<MigratingTransitionInstance> transitionChildrenCopy = new HashSet<>(childTransitionInstances);
    for (MigratingTransitionInstance child : transitionChildrenCopy) {
      child.detachState();
    }

    Set<MigratingEventScopeInstance> compensationChildrenCopy = new HashSet<>(childCompensationInstances);
    for (MigratingEventScopeInstance child : compensationChildrenCopy) {
      child.detachState();
    }

    Set<MigratingCompensationEventSubscriptionInstance> compensationSubscriptionsChildrenCopy = new HashSet<>(childCompensationSubscriptionInstances);
    for (MigratingCompensationEventSubscriptionInstance child : compensationSubscriptionsChildrenCopy) {
      child.detachState();
    }
  }

  public void detachDependentInstances() {
    for (MigratingInstance dependentInstance : migratingDependentInstances) {
      if (!dependentInstance.isDetached()) {
        dependentInstance.detachState();
      }
    }
  }

  @Override
  public boolean isDetached() {
    return instanceBehavior.isDetached();
  }

  @Override
  public void detachState() {

    detachDependentInstances();

    instanceBehavior.detachState();

    setParent(null);
  }

  @Override
  public void attachState(MigratingScopeInstance activityInstance) {

    this.setParent(activityInstance);
    instanceBehavior.attachState();

    for (MigratingInstance dependentInstance : migratingDependentInstances) {
      dependentInstance.attachState(this);
    }
  }

  @Override
  public void attachState(MigratingTransitionInstance targetTransitionInstance) {
    throw MIGRATION_LOGGER.cannotAttachToTransitionInstance(this);
  }

  @Override
  public void migrateDependentEntities() {
    for (MigratingInstance migratingInstance : migratingDependentInstances) {
      migratingInstance.migrateState();
      migratingInstance.migrateDependentEntities();
    }

    ExecutionEntity representativeExec = resolveRepresentativeExecution();
    for (EmergingInstance emergingInstance : emergingDependentInstances) {
      emergingInstance.create(representativeExec);
    }
  }

  @Override
  public ExecutionEntity resolveRepresentativeExecution() {
    return instanceBehavior.resolveRepresentativeExecution();
  }

  @Override
  public void addMigratingDependentInstance(MigratingInstance migratingInstance) {
    migratingDependentInstances.add(migratingInstance);
  }

  public List<MigratingInstance> getMigratingDependentInstances() {
    return migratingDependentInstances;
  }

  public void addRemovingDependentInstance(RemovingInstance removingInstance) {
    removingDependentInstances.add(removingInstance);
  }

  public void addEmergingDependentInstance(EmergingInstance emergingInstance) {
    emergingDependentInstances.add(emergingInstance);
  }

  public void addChild(MigratingTransitionInstance transitionInstance) {
    this.childTransitionInstances.add(transitionInstance);
  }

  public void removeChild(MigratingTransitionInstance transitionInstance) {
    this.childTransitionInstances.remove(transitionInstance);
  }

  public void addChild(MigratingActivityInstance activityInstance) {
    this.childActivityInstances.add(activityInstance);
  }

  public void removeChild(MigratingActivityInstance activityInstance) {
    this.childActivityInstances.remove(activityInstance);
  }

  @Override
  public void addChild(MigratingScopeInstance migratingActivityInstance) {
    if (migratingActivityInstance instanceof MigratingActivityInstance instance) {
      addChild(instance);
    }
    else if (migratingActivityInstance instanceof MigratingEventScopeInstance instance) {
      addChild(instance);
    }
    else {
      throw MIGRATION_LOGGER.cannotHandleChild(this, migratingActivityInstance);
    }
  }

  @Override
  public void removeChild(MigratingScopeInstance child) {
    if (child instanceof MigratingActivityInstance instance) {
      removeChild(instance);
    }
    else if (child instanceof MigratingEventScopeInstance instance) {
      removeChild(instance);
    }
    else {
      throw MIGRATION_LOGGER.cannotHandleChild(this, child);
    }
  }

  public void addChild(MigratingEventScopeInstance compensationInstance) {
    this.childCompensationInstances.add(compensationInstance);
  }

  public void removeChild(MigratingEventScopeInstance compensationInstance) {
    this.childCompensationInstances.remove(compensationInstance);
  }

  @Override
  public void addChild(MigratingCompensationEventSubscriptionInstance migratingEventSubscription) {
    this.childCompensationSubscriptionInstances.add(migratingEventSubscription);
  }

  @Override
  public void removeChild(MigratingCompensationEventSubscriptionInstance migratingEventSubscription) {
    this.childCompensationSubscriptionInstances.remove(migratingEventSubscription);
  }

  public ActivityInstance getActivityInstance() {
    return activityInstance;
  }

  public String getActivityInstanceId() {
    if (activityInstance != null) {
      return activityInstance.getId();
    }
    else {
      // - this branch is only executed for emerging activity instances
      // - emerging activity instances are never leaf activities
      // - therefore it is fine to always look up the activity instance id on the parent
      ExecutionEntity execution = resolveRepresentativeExecution();
      return execution.getParentActivityInstanceId();
    }
  }

  @Override
  public MigratingActivityInstance getParent() {
    return (MigratingActivityInstance) super.getParent();
  }

  /**
   * Returns a copy of all children, modifying the returned set does not have any further effect.
   */
  @Override
  public Set<MigratingProcessElementInstance> getChildren() {
    Set<MigratingProcessElementInstance> childInstances = new HashSet<>();
    childInstances.addAll(childActivityInstances);
    childInstances.addAll(childTransitionInstances);
    childInstances.addAll(childCompensationInstances);
    childInstances.addAll(childCompensationSubscriptionInstances);
    return childInstances;
  }

  @Override
  public Collection<MigratingScopeInstance> getChildScopeInstances() {
    Set<MigratingScopeInstance> childInstances = new HashSet<>();
    childInstances.addAll(childActivityInstances);
    childInstances.addAll(childCompensationInstances);
    return childInstances;
  }

  public Set<MigratingActivityInstance> getChildActivityInstances() {
    return childActivityInstances;
  }

  public Set<MigratingTransitionInstance> getChildTransitionInstances() {
    return childTransitionInstances;
  }

  public Set<MigratingEventScopeInstance> getChildCompensationInstances() {
    return childCompensationInstances;
  }

  @Override
  public boolean migrates() {
    return targetScope != null;
  }

  @Override
  public void removeUnmappedDependentInstances() {
    for (RemovingInstance removingInstance : removingDependentInstances) {
      removingInstance.remove();
    }
  }

  @Override
  public void remove(boolean skipCustomListeners, boolean skipIoMappings) {
    instanceBehavior.remove(skipCustomListeners, skipIoMappings);
  }

  @Override
  public void migrateState() {
    instanceBehavior.migrateState();
  }

  protected void migrateHistory(DelegateExecution execution) {
    if (activityInstance.getId().equals(activityInstance.getProcessInstanceId())) {
      migrateProcessInstanceHistory(execution);
    }
    else {
      migrateActivityInstanceHistory(execution);
    }
  }

  protected void migrateProcessInstanceHistory(final DelegateExecution execution) {
    HistoryLevel historyLevel = Context.getProcessEngineConfiguration().getHistoryLevel();
    if (!historyLevel.isHistoryEventProduced(HistoryEventTypes.PROCESS_INSTANCE_MIGRATE, this)) {
      return;
    }

    HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
      @Override
      public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
        return producer.createProcessInstanceMigrateEvt(execution);
      }
    });
  }

  @SuppressWarnings("unused")
  protected void migrateActivityInstanceHistory(final DelegateExecution execution) {
    HistoryLevel historyLevel = Context.getProcessEngineConfiguration().getHistoryLevel();
    if (!historyLevel.isHistoryEventProduced(HistoryEventTypes.ACTIVITY_INSTANCE_MIGRATE, this)) {
      return;
    }

    HistoryEventProcessor.processHistoryEvents(new HistoryEventProcessor.HistoryEventCreator() {
      @Override
      public HistoryEvent createHistoryEvent(HistoryEventProducer producer) {
        return producer.createActivityInstanceMigrateEvt(MigratingActivityInstance.this);
      }
    });
  }

  public ExecutionEntity createAttachableExecution() {
    return instanceBehavior.createAttachableExecution();
  }

  public void destroyAttachableExecution(ExecutionEntity execution) {
    instanceBehavior.destroyAttachableExecution(execution);
  }

  @Override
  public void setParent(MigratingScopeInstance parentInstance) {
    if (this.parentInstance != null) {
      this.parentInstance.removeChild(this);
    }

    this.parentInstance = parentInstance;

    if (parentInstance != null) {
      parentInstance.addChild(this);
    }
  }


  protected interface MigratingActivityInstanceBehavior {

    boolean isDetached();

    void detachState();

    void attachState();

    void migrateState();

    void remove(boolean skipCustomListeners, boolean skipIoMappings);

    ExecutionEntity resolveRepresentativeExecution();

    ExecutionEntity createAttachableExecution();

    void destroyAttachableExecution(ExecutionEntity execution);
  }

  protected class MigratingNonScopeActivityInstanceBehavior implements MigratingActivityInstanceBehavior {

    @Override
    public boolean isDetached() {
      return resolveRepresentativeExecution().getActivity() == null;
    }

    @Override
    public void detachState() {
      ExecutionEntity currentExecution = resolveRepresentativeExecution();

      currentExecution.setActivity(null);
      currentExecution.leaveActivityInstance();
      currentExecution.setActive(false);

      getParent().destroyAttachableExecution(currentExecution);
    }

    @Override
    public void attachState() {

      representativeExecution = getParent().createAttachableExecution();

      representativeExecution.setActivity((PvmActivity) sourceScope);
      representativeExecution.setActivityInstanceId(activityInstance.getId());
      representativeExecution.setActive(activeState);

    }

    @Override
    public void migrateState() {
      ExecutionEntity currentExecution = resolveRepresentativeExecution();
      currentExecution.setProcessDefinition(targetScope.getProcessDefinition());
      currentExecution.setActivity((PvmActivity) targetScope);

      currentScope = targetScope;

      if (targetScope.isScope()) {
        becomeScope();
      }

      migrateHistory(currentExecution);
    }

    protected void becomeScope() {
      for (MigratingInstance dependentInstance : migratingDependentInstances) {
        dependentInstance.detachState();
      }

      ExecutionEntity currentExecution = resolveRepresentativeExecution();

      currentExecution = currentExecution.createExecution();
      ExecutionEntity parent = currentExecution.getParent();
      parent.setActivity(null);

      if (!parent.isConcurrent()) {
        parent.leaveActivityInstance();
      }

      representativeExecution = currentExecution;
      for (MigratingInstance dependentInstance : migratingDependentInstances) {
        dependentInstance.attachState(MigratingActivityInstance.this);
      }

      instanceBehavior = new MigratingScopeActivityInstanceBehavior();
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
    public void remove(boolean skipCustomListeners, boolean skipIoMappings) {
      // nothing to do; we don't remove non-scope instances
    }

    @Override
    public ExecutionEntity createAttachableExecution() {
      throw MIGRATION_LOGGER.cannotBecomeSubordinateInNonScope(MigratingActivityInstance.this);
    }

    @Override
    public void destroyAttachableExecution(ExecutionEntity execution) {
      throw MIGRATION_LOGGER.cannotDestroySubordinateInNonScope(MigratingActivityInstance.this);
    }
  }

  protected class MigratingScopeActivityInstanceBehavior implements MigratingActivityInstanceBehavior {

    @Override
    public boolean isDetached() {
      ExecutionEntity representativeExec = resolveRepresentativeExecution();
      return representativeExec != representativeExec.getProcessInstance()
        && representativeExec.getParent() == null;
    }

    @Override
    public void detachState() {
      ExecutionEntity currentScopeExecution = resolveRepresentativeExecution();

      ExecutionEntity parentExecution = currentScopeExecution.getParent();
      currentScopeExecution.setParent(null);

      if (sourceScope.getActivityBehavior() instanceof CompositeActivityBehavior) {
        parentExecution.leaveActivityInstance();
      }

      getParent().destroyAttachableExecution(parentExecution);
    }

    @Override
    public void attachState() {
      ExecutionEntity newParentExecution = getParent().createAttachableExecution();

      ExecutionEntity currentScopeExecution = resolveRepresentativeExecution();
      currentScopeExecution.setParent(newParentExecution);

      if (sourceScope.getActivityBehavior() instanceof CompositeActivityBehavior) {
        newParentExecution.setActivityInstanceId(activityInstance.getId());
      }

    }

    @Override
    public void migrateState() {
      ExecutionEntity currentScopeExecution = resolveRepresentativeExecution();
      currentScopeExecution.setProcessDefinition(targetScope.getProcessDefinition());

      ExecutionEntity parentExecution = currentScopeExecution.getParent();

      if (parentExecution != null && parentExecution.isConcurrent()) {
        parentExecution.setProcessDefinition(targetScope.getProcessDefinition());
      }

      currentScope = targetScope;

      if (!targetScope.isScope()) {
        becomeNonScope();
        currentScopeExecution = resolveRepresentativeExecution();
      }

      if (isLeafActivity(targetScope)) {
        currentScopeExecution.setActivity((PvmActivity) targetScope);
      }

      CoreActivityBehavior<? extends BaseDelegateExecution> activityBehavior = sourceScope.getActivityBehavior();
      if (activityBehavior instanceof MigrationObserverBehavior migrationBehavior) {
        migrationBehavior.migrateScope(currentScopeExecution);
      }

      migrateHistory(currentScopeExecution);
    }

    protected void becomeNonScope() {
      for (MigratingInstance dependentInstance : migratingDependentInstances) {
        dependentInstance.detachState();
      }

      ExecutionEntity parentExecution = representativeExecution.getParent();

      parentExecution.setActivity(representativeExecution.getActivity());
      parentExecution.setActivityInstanceId(representativeExecution.getActivityInstanceId());
      parentExecution.setActive(representativeExecution.isActive());

      representativeExecution.remove();
      representativeExecution = parentExecution;

      for (MigratingInstance dependentInstance : migratingDependentInstances) {
        dependentInstance.attachState(MigratingActivityInstance.this);
      }

      instanceBehavior = new MigratingNonScopeActivityInstanceBehavior();
    }

    protected boolean isLeafActivity(ScopeImpl scope) {
      return scope.getActivities().isEmpty();
    }

    @Override
    public ExecutionEntity resolveRepresentativeExecution() {
      return representativeExecution;
    }

    @Override
    public void remove(boolean skipCustomListeners, boolean skipIoMappings) {

      ExecutionEntity currentExecution = resolveRepresentativeExecution();
      ExecutionEntity parentExecution = currentExecution.getParent();

      currentExecution.setActivity((PvmActivity) sourceScope);
      currentExecution.setActivityInstanceId(activityInstance.getId());

      currentExecution.deleteCascade("migration", skipCustomListeners, skipIoMappings);

      getParent().destroyAttachableExecution(parentExecution);

      setParent(null);
      for (MigratingTransitionInstance child : childTransitionInstances) {
        child.setParent(null);
      }
      for (MigratingActivityInstance child : childActivityInstances) {
        child.setParent(null);
      }
      for (MigratingEventScopeInstance child : childCompensationInstances) {
        child.setParent(null);
      }
    }

    @Override
    public ExecutionEntity createAttachableExecution() {
      ExecutionEntity scopeExecution = resolveRepresentativeExecution();
      ExecutionEntity attachableExecution = scopeExecution;


      CoreActivityBehavior<? extends BaseDelegateExecution> activityBehavior = currentScope.getActivityBehavior();
      if (activityBehavior instanceof ModificationObserverBehavior behavior) {
        attachableExecution = (ExecutionEntity) behavior.createInnerInstance(scopeExecution);
      }
      else {
        if (!scopeExecution.getNonEventScopeExecutions().isEmpty() || scopeExecution.getActivity() != null) {
          attachableExecution = (ExecutionEntity) scopeExecution.createConcurrentExecution();
          attachableExecution.setActive(false);
          scopeExecution.forceUpdate();
        }
      }

      return attachableExecution;
    }

    @Override
    public void destroyAttachableExecution(ExecutionEntity execution) {
      CoreActivityBehavior<? extends BaseDelegateExecution> activityBehavior = currentScope.getActivityBehavior();
      if (activityBehavior instanceof ModificationObserverBehavior behavior) {
        behavior.destroyInnerInstance(execution);
      }
      else {
        if (execution.isConcurrent()) {
          execution.remove();
          execution.getParent().tryPruneLastConcurrentChild();
          execution.getParent().forceUpdate();
        }
      }
    }
  }
}
