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
package org.operaton.bpm.engine.impl.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ActivityInstanceImpl;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.IncidentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TransitionInstanceImpl;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.CompensationBehavior;
import org.operaton.bpm.engine.impl.pvm.runtime.LegacyBehavior;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.commons.utils.CollectionUtil;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * <p>Creates an activity instance tree according to the following strategy:
 *
 * <ul>
 *   <li> Event scope executions are not considered at all
 *   <li> For every leaf execution, generate an activity/transition instance;
 *   the activity instance id is set in the leaf execution and the parent instance id is set in the parent execution
 *   <li> For every non-leaf scope execution, generate an activity instance;
 *   the activity instance id is always set in the parent execution and the parent activity
 *   instance id is always set in the parent's parent (because of tree compactation, we ensure
 *   that an activity instance id for a scope activity is always stored in the corresponding scope execution's parent,
 *   unless the execution is a leaf)
 *   <li> Compensation is an exception to the above procedure: A compensation throw event is not a scope, however the compensating executions
 *   are added as child executions of the (probably non-scope) execution executing the throw event. Logically, the compensating executions
 *   are children of the scope execution the throwing event is executed in. Due to this oddity, the activity instance id are stored on different
 *   executions
 * </ul>
 *
 * @author Thorben Lindhauer
 *
 */
public class GetActivityInstanceCmd implements Command<ActivityInstance> {
  private static final ExecutionIdComparator EXECUTION_ID_COMPARATOR = new ExecutionIdComparator();

  protected String processInstanceId;

  public GetActivityInstanceCmd(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  @Override
  public ActivityInstance execute(CommandContext commandContext) {
    ensureNotNull("processInstanceId", processInstanceId);

    List<ExecutionEntity> executionList = loadProcessInstance(processInstanceId, commandContext);
    if (executionList.isEmpty()) {
      return null;
    }

    checkGetActivityInstance(processInstanceId, commandContext);
    List<ExecutionEntity> leaves = getLeaves(executionList);

    ExecutionEntity processInstance = filterProcessInstance(executionList);
    if (processInstance.isEnded()) {
      return null;
    }

    Map<String, List<Incident>> incidents = groupIncidentIdsByExecutionId(commandContext);
    // create act instance for process instance
    ActivityInstanceImpl processActInst = createActivityInstance(
      processInstance,
      processInstance.getProcessDefinition(),
      processInstanceId,
      null,
      incidents);

    Map<String, ActivityInstanceImpl> activityInstances = new HashMap<>();
    activityInstances.put(processInstanceId, processActInst);
    Map<String, TransitionInstanceImpl> transitionInstances = new HashMap<>();
    for (ExecutionEntity leaf : leaves) {
      processLeaf(leaf, activityInstances, transitionInstances, incidents);
    }

    LegacyBehavior.repairParentRelationships(activityInstances.values(), processInstanceId);
    populateChildInstances(activityInstances, transitionInstances);

    return processActInst;
  }

  private List<ExecutionEntity> getLeaves(List<ExecutionEntity> executionList) {
    List<ExecutionEntity> nonEventScopeExecutions = filterNonEventScopeExecutions(executionList);
    List<ExecutionEntity> leaves = filterLeaves(nonEventScopeExecutions);
    // Leaves must be ordered in a predictable way (e.g. by ID)
    // in order to return a stable execution tree with every repeated invocation of this command.
    // For legacy process instances, there may miss scope executions for activities that are now a scope.
    // In this situation, there may be multiple scope candidates for the same instance id; which one
    // can depend on the order the leaves are iterated.
    orderById(leaves);
    return leaves;
  }

  private void processLeaf(ExecutionEntity leaf, Map<String, ActivityInstanceImpl> activityInstances, Map<String, TransitionInstanceImpl> transitionInstances, Map<String, List<Incident>> incidents) {
    // skip leafs without activity, e.g. if only the process instance exists after cancellation
    // it will not have an activity set
    if (leaf.getActivity() == null) {
      return;
    }

    Map<ScopeImpl, PvmExecutionImpl> activityExecutionMapping = leaf.createActivityExecutionMapping();
    Map<ScopeImpl, PvmExecutionImpl> scopeInstancesToCreate = new HashMap<>(activityExecutionMapping);

    if (leaf.getActivityInstanceId() != null) {
      createLeafInstance(leaf, activityExecutionMapping, scopeInstancesToCreate, activityInstances, incidents);
    }
    else {
      createLeafTransitionInstance(leaf, scopeInstancesToCreate, transitionInstances, incidents);
    }

    createScopeInstances(leaf, activityExecutionMapping, scopeInstancesToCreate, activityInstances, incidents);
  }

  private void createLeafInstance(ExecutionEntity leaf, Map<ScopeImpl, PvmExecutionImpl> activityExecutionMapping, Map<ScopeImpl, PvmExecutionImpl> scopeInstancesToCreate, Map<String, ActivityInstanceImpl> activityInstances, Map<String, List<Incident>> incidents) {
    // create an activity/transition instance for each leaf that executes a non-scope activity
    // and does not throw compensation
    if (!CompensationBehavior.isCompensationThrowing(leaf) || LegacyBehavior.isCompensationThrowing(leaf, activityExecutionMapping)) {
      String parentActivityInstanceId = activityExecutionMapping
          .get(leaf.getActivity().getFlowScope())
          .getParentActivityInstanceId();

      ActivityInstanceImpl leafInstance = createActivityInstance(leaf,
          leaf.getActivity(),
          leaf.getActivityInstanceId(),
          parentActivityInstanceId,
          incidents);
      activityInstances.put(leafInstance.getId(), leafInstance);

      scopeInstancesToCreate.remove(leaf.getActivity());
    }
  }

  private void createLeafTransitionInstance(ExecutionEntity leaf, Map<ScopeImpl, PvmExecutionImpl> scopeInstancesToCreate, Map<String, TransitionInstanceImpl> transitionInstances, Map<String, List<Incident>> incidents) {
    TransitionInstanceImpl transitionInstance = createTransitionInstance(leaf, incidents);
    transitionInstances.put(transitionInstance.getId(), transitionInstance);

    scopeInstancesToCreate.remove(leaf.getActivity());
  }

  private void createScopeInstances(ExecutionEntity leaf, Map<ScopeImpl, PvmExecutionImpl> activityExecutionMapping, Map<ScopeImpl, PvmExecutionImpl> scopeInstancesToCreate, Map<String, ActivityInstanceImpl> activityInstances, Map<String, List<Incident>> incidents) {
    LegacyBehavior.removeLegacyNonScopesFromMapping(scopeInstancesToCreate);
    scopeInstancesToCreate.remove(leaf.getProcessDefinition());

    // create an activity instance for each scope (including compensation throwing executions)
    for (Map.Entry<ScopeImpl, PvmExecutionImpl> scopeExecutionEntry : scopeInstancesToCreate.entrySet()) {
      ScopeImpl scope = scopeExecutionEntry.getKey();
      PvmExecutionImpl scopeExecution = scopeExecutionEntry.getValue();

      String activityInstanceId = scopeExecution.getParentActivityInstanceId();
      String parentActivityInstanceId = activityExecutionMapping
          .get(scope.getFlowScope())
          .getParentActivityInstanceId();

      activityInstances.computeIfAbsent(activityInstanceId, id ->
          createActivityInstance(
              scopeExecution,
              scope,
              id,
              parentActivityInstanceId,
              incidents));
    }
  }

  protected void checkGetActivityInstance(String processInstanceId, CommandContext commandContext) {
    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadProcessInstance(processInstanceId);
    }
  }

  protected void orderById(List<ExecutionEntity> leaves) {
    leaves.sort(EXECUTION_ID_COMPARATOR);
  }

  protected ActivityInstanceImpl createActivityInstance(PvmExecutionImpl scopeExecution, ScopeImpl scope,
      String activityInstanceId, String parentActivityInstanceId,
      Map<String, List<Incident>> incidentsByExecution) {
    ActivityInstanceImpl actInst = new ActivityInstanceImpl();

    actInst.setId(activityInstanceId);
    actInst.setParentActivityInstanceId(parentActivityInstanceId);
    actInst.setProcessInstanceId(scopeExecution.getProcessInstanceId());
    actInst.setProcessDefinitionId(scopeExecution.getProcessDefinitionId());
    actInst.setBusinessKey(scopeExecution.getBusinessKey());
    actInst.setActivityId(scope.getId());
    PvmExecutionImpl subProcessInstance = scopeExecution.getSubProcessInstance();
    if (subProcessInstance != null) {
      actInst.setSubProcessInstanceId(subProcessInstance.getId());
    }

    String name = scope.getName();
    if (name == null) {
      name = (String) scope.getProperty("name");
    }
    actInst.setActivityName(name);

    if (scope.getId().equals(scopeExecution.getProcessDefinition().getId())) {
      actInst.setActivityType("processDefinition");
    }
    else {
      actInst.setActivityType((String) scope.getProperty("type"));
    }

    List<String> executionIds = new ArrayList<>();
    List<String> incidentIds = new ArrayList<>();
    List<Incident> incidents = new ArrayList<>();

    executionIds.add(scopeExecution.getId());

    ActivityImpl executionActivity = scopeExecution.getActivity();

    // do not collect incidents if scopeExecution is a compacted subtree
    // and we currently create the scope activity instance
    if (executionActivity == null || executionActivity == scope) {
      incidentIds.addAll(getIncidentIds(incidentsByExecution, scopeExecution));
      incidents.addAll(getIncidents(incidentsByExecution, scopeExecution));
    }

    for (PvmExecutionImpl childExecution : scopeExecution.getNonEventScopeExecutions()) {
      // add all concurrent children that are not in an activity
      if (childExecution.isConcurrent() && childExecution.getActivityId() == null) {
        executionIds.add(childExecution.getId());
        incidentIds.addAll(getIncidentIds(incidentsByExecution, childExecution));
        incidents.addAll(getIncidents(incidentsByExecution, childExecution));
      }
    }

    actInst.setExecutionIds(executionIds.toArray(new String[executionIds.size()]));
    actInst.setIncidentIds(incidentIds.toArray(new String[incidentIds.size()]));
    actInst.setIncidents(incidents.toArray(new Incident[0]));

    return actInst;
  }

  protected TransitionInstanceImpl createTransitionInstance(PvmExecutionImpl execution,
      Map<String, List<Incident>> incidentsByExecution) {
    TransitionInstanceImpl transitionInstance = new TransitionInstanceImpl();

    // can use execution id as persistent ID for transition as an execution
    // can execute as most one transition at a time.
    transitionInstance.setId(execution.getId());
    transitionInstance.setParentActivityInstanceId(execution.getParentActivityInstanceId());
    transitionInstance.setProcessInstanceId(execution.getProcessInstanceId());
    transitionInstance.setProcessDefinitionId(execution.getProcessDefinitionId());
    transitionInstance.setExecutionId(execution.getId());
    transitionInstance.setActivityId(execution.getActivityId());
    PvmExecutionImpl subProcessInstance = execution.getSubProcessInstance();
    if (subProcessInstance != null) {
      transitionInstance.setSubProcessInstanceId(subProcessInstance.getId());
    }

    ActivityImpl activity = execution.getActivity();
    if (activity != null) {
      String name = activity.getName();
      if (name == null) {
        name = (String) activity.getProperty("name");
      }
      transitionInstance.setActivityName(name);
      transitionInstance.setActivityType((String) activity.getProperty("type"));
    }

    List<String> incidentIdList = getIncidentIds(incidentsByExecution, execution);
    List<Incident> incidents = getIncidents(incidentsByExecution, execution);
    transitionInstance.setIncidentIds(incidentIdList.toArray(new String[0]));
    transitionInstance.setIncidents(incidents.toArray(new Incident[0]));

    return transitionInstance;
  }

  protected void populateChildInstances(Map<String, ActivityInstanceImpl> activityInstances,
      Map<String, TransitionInstanceImpl> transitionInstances) {
    Map<ActivityInstanceImpl, List<ActivityInstanceImpl>> childActivityInstances
      = new HashMap<>();
    Map<ActivityInstanceImpl, List<TransitionInstanceImpl>> childTransitionInstances
      = new HashMap<>();

    populateActivityInstances(activityInstances, childActivityInstances);
    populateTransitionInstances(activityInstances, transitionInstances, childTransitionInstances);
    populateChildActivityInstances(childActivityInstances);
    populateChildTransitionInstances(childTransitionInstances);
  }

  private static void populateChildTransitionInstances(Map<ActivityInstanceImpl, List<TransitionInstanceImpl>> childTransitionInstances) {
    for (Map.Entry<ActivityInstanceImpl, List<TransitionInstanceImpl>> entry :
      childTransitionInstances.entrySet()) {
      ActivityInstanceImpl instance = entry.getKey();
      List<TransitionInstanceImpl> childInstances = entry.getValue();
      instance.setChildTransitionInstances(childInstances.toArray(new TransitionInstanceImpl[childInstances.size()]));
    }
  }

  private static void populateChildActivityInstances(Map<ActivityInstanceImpl, List<ActivityInstanceImpl>> childActivityInstances) {
    for (Map.Entry<ActivityInstanceImpl, List<ActivityInstanceImpl>> entry :
        childActivityInstances.entrySet()) {
      ActivityInstanceImpl instance = entry.getKey();
      List<ActivityInstanceImpl> childInstances = entry.getValue();
      if (childInstances != null) {
        instance.setChildActivityInstances(childInstances.toArray(new ActivityInstanceImpl[childInstances.size()]));
      }
    }
  }

  private void populateTransitionInstances(Map<String, ActivityInstanceImpl> activityInstances,
                         Map<String, TransitionInstanceImpl> transitionInstances,
                         Map<ActivityInstanceImpl, List<TransitionInstanceImpl>> childTransitionInstances) {
    for (TransitionInstanceImpl instance : transitionInstances.values()) {
      if (instance.getParentActivityInstanceId() != null) {
        ActivityInstanceImpl parentInstance = activityInstances.get(instance.getParentActivityInstanceId());
        if (parentInstance == null) {
          throw new ProcessEngineException("No parent activity instance with id %s generated".formatted(instance.getParentActivityInstanceId()));
        }
        putListElement(childTransitionInstances, parentInstance, instance);
      }
    }
  }

  private void populateActivityInstances(Map<String, ActivityInstanceImpl> activityInstances,
                         Map<ActivityInstanceImpl, List<ActivityInstanceImpl>> childActivityInstances) {
    for (ActivityInstanceImpl instance : activityInstances.values()) {
      if (instance.getParentActivityInstanceId() != null) {
        ActivityInstanceImpl parentInstance = activityInstances.get(instance.getParentActivityInstanceId());
        if (parentInstance == null) {
          throw new ProcessEngineException("No parent activity instance with id %s generated".formatted(instance.getParentActivityInstanceId()));
        }
        putListElement(childActivityInstances, parentInstance, instance);
      }
    }
  }

  protected <S, T> void putListElement(Map<S, List<T>> mapOfLists, S key, T listElement) {
    mapOfLists
      .computeIfAbsent(key, k -> new ArrayList<>())
      .add(listElement);
  }

  protected ExecutionEntity filterProcessInstance(List<ExecutionEntity> executionList) {
    for (ExecutionEntity execution : executionList) {
      if (execution.isProcessInstanceExecution()) {
        return execution;
      }
    }

    throw new ProcessEngineException("Could not determine process instance execution");
  }

  protected List<ExecutionEntity> filterLeaves(List<ExecutionEntity> executionList) {
    List<ExecutionEntity> leaves = new ArrayList<>();
    for (ExecutionEntity execution : executionList) {
      // although executions executing throwing compensation events are not leaves in the tree,
      // they are treated as leaves since their child executions are logical children of their parent scope execution
      if (execution.getNonEventScopeExecutions().isEmpty() || CompensationBehavior.isCompensationThrowing(execution)) {
        leaves.add(execution);
      }
    }
    return leaves;
  }

  protected List<ExecutionEntity> filterNonEventScopeExecutions(List<ExecutionEntity> executionList) {
    List<ExecutionEntity> nonEventScopeExecutions = new ArrayList<>();
    for (ExecutionEntity execution : executionList) {
      if (!execution.isEventScope()) {
        nonEventScopeExecutions.add(execution);
      }
    }
    return nonEventScopeExecutions;
  }

  protected List<ExecutionEntity> loadProcessInstance(String processInstanceId, CommandContext commandContext) {

    List<ExecutionEntity> result = null;

    // first try to load from cache
    // check whether the process instance is already (partially) loaded in command context
    List<ExecutionEntity> cachedExecutions = commandContext.getDbEntityManager().getCachedEntitiesByType(ExecutionEntity.class);
    for (ExecutionEntity executionEntity : cachedExecutions) {
      if(processInstanceId.equals(executionEntity.getProcessInstanceId())) {
        // found one execution from process instance
        result = new ArrayList<>();
        ExecutionEntity processInstance = executionEntity.getProcessInstance();
        // add process instance
        result.add(processInstance);
        loadChildExecutionsFromCache(processInstance, result);
        break;
      }
    }

    if(result == null) {
      // if the process instance could not be found in cache, load from database
      result = loadFromDb(processInstanceId, commandContext);
    }

    return result;
  }

  protected List<ExecutionEntity> loadFromDb(final String processInstanceId, final CommandContext commandContext) {

    List<ExecutionEntity> executions = commandContext.getExecutionManager().findExecutionsByProcessInstanceId(processInstanceId);
    ExecutionEntity processInstance = commandContext.getExecutionManager().findExecutionById(processInstanceId);

    // initialize parent/child sets
    if (processInstance != null) {
      processInstance.restoreProcessInstance(executions, null, null, null, null, null, null);
    }

    return executions;
  }

  /**
   * Loads all executions that are part of this process instance tree from the dbSqlSession cache.
   * (optionally querying the db if a child is not already loaded.)
   *
   * @param execution the current root execution (already contained in childExecutions)
   * @param childExecutions the list in which all child executions should be collected
   */
  protected void loadChildExecutionsFromCache(ExecutionEntity execution, List<ExecutionEntity> childExecutions) {
    List<ExecutionEntity> childrenOfThisExecution = execution.getExecutions();
    if(childrenOfThisExecution != null) {
      childExecutions.addAll(childrenOfThisExecution);
      for (ExecutionEntity child : childrenOfThisExecution) {
        loadChildExecutionsFromCache(child, childExecutions);
      }
    }
  }

  protected Map<String, List<Incident>> groupIncidentIdsByExecutionId(CommandContext commandContext) {
    List<IncidentEntity> incidents = commandContext.getIncidentManager().findIncidentsByProcessInstance(processInstanceId);
    Map<String, List<Incident>> result = new HashMap<>();
    for (IncidentEntity incidentEntity : incidents) {
      CollectionUtil.addToMapOfLists(result, incidentEntity.getExecutionId(), incidentEntity);
    }
    return result;
  }

  protected List<String> getIncidentIds(Map<String, List<Incident>> incidents,
      PvmExecutionImpl execution) {
    List<String> incidentIds = new ArrayList<>();
    List<Incident> incidentList = incidents.get(execution.getId());
    if (incidentList != null) {
      for (Incident incident : incidentList) {
        incidentIds.add(incident.getId());
      }

      return incidentIds;
    } else {
      return Collections.emptyList();
    }
  }

  protected List<Incident> getIncidents(Map<String, List<Incident>> incidents,
      PvmExecutionImpl execution) {
    List<Incident> incidentList = incidents.get(execution.getId());
    return Objects.requireNonNullElse(incidentList, Collections.emptyList());
  }

  public static class ExecutionIdComparator implements Comparator<ExecutionEntity> {

    @Override
    public int compare(ExecutionEntity o1, ExecutionEntity o2) {
      return o1.getId().compareTo(o2.getId());
    }

  }



}
