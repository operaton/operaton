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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.ActivityExecutionTreeMapping;
import org.operaton.bpm.engine.impl.bpmn.behavior.SequentialMultiInstanceActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.operaton.bpm.engine.impl.core.delegate.CoreActivityBehavior;
import org.operaton.bpm.engine.impl.core.model.CoreModelElement;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.PvmScope;
import org.operaton.bpm.engine.impl.pvm.PvmTransition;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ActivityStartBehavior;
import org.operaton.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.pvm.process.TransitionImpl;
import org.operaton.bpm.engine.impl.tree.ActivityStackCollector;
import org.operaton.bpm.engine.impl.tree.FlowScopeWalker;
import org.operaton.bpm.engine.impl.util.EnsureUtil;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.impl.VariableMapImpl;

/**
 * @author Thorben Lindhauer
 *
 */
public abstract class AbstractInstantiationCmd extends AbstractProcessInstanceModificationCommand {

  protected VariableMap variables;
  protected VariableMap variablesLocal;
  protected String ancestorActivityInstanceId;

  protected AbstractInstantiationCmd(String processInstanceId, String ancestorActivityInstanceId) {
    super(processInstanceId);
    this.ancestorActivityInstanceId = ancestorActivityInstanceId;
    this.variables = new VariableMapImpl();
    this.variablesLocal = new VariableMapImpl();
  }

  public void addVariable(String name, Object value) {
    this.variables.put(name, value);
  }

  public void addVariableLocal(String name, Object value) {
    this.variablesLocal.put(name, value);
  }

  public void addVariables(Map<String, Object> variables) {
    this.variables.putAll(variables);
  }

  public void addVariablesLocal(Map<String, Object> variables) {
    this.variablesLocal.putAll(variables);
  }

  public VariableMap getVariables() {
    return variables;
  }

  public VariableMap getVariablesLocal() {
    return variablesLocal;
  }

  public String getAncestorActivityInstanceId() {
    return ancestorActivityInstanceId;
  }

  public void setAncestorActivityInstanceId(String ancestorActivityInstanceId) {
    this.ancestorActivityInstanceId = ancestorActivityInstanceId;
  }

  @Override
  public Void execute(final CommandContext commandContext) {
    ExecutionEntity processInstance = commandContext.getExecutionManager().findExecutionById(processInstanceId);
    final ProcessDefinitionImpl processDefinition = processInstance.getProcessDefinition();

    CoreModelElement elementToInstantiate = getTargetElement(processDefinition);
    EnsureUtil.ensureNotNull(NotValidException.class,
        describeFailure(
            "Element '" + getTargetElementId() + "' does not exist in process '" + processDefinition.getId() + "'"),
        "element", elementToInstantiate);

    // rebuild the mapping because the execution tree changes with every
    // iteration
    final ActivityExecutionTreeMapping mapping = new ActivityExecutionTreeMapping(commandContext, processInstanceId);

    // before instantiating an activity, two things have to be determined:
    //
    // activityStack:
    // For the activity to instantiate, we build a stack of parent flow scopes
    // for which no executions exist yet and that have to be instantiated
    //
    // scopeExecution:
    // This is typically the execution under which a new sub tree has to be
    // created.
    // if an explicit ancestor activity instance is set:
    // - this is the scope execution for that ancestor activity instance
    // - throws exception if that scope execution is not in the parent hierarchy
    // of the activity to be started
    // if no explicit ancestor activity instance is set:
    // - this is the execution of the first parent/ancestor flow scope that has
    // an execution
    // - throws an exception if there is more than one such execution

    ScopeImpl targetFlowScope = getTargetFlowScope(processDefinition);

    // prepare to walk up the flow scope hierarchy and collect the flow scope
    // activities
    ActivityStackCollector stackCollector = new ActivityStackCollector();
    FlowScopeWalker walker = new FlowScopeWalker(targetFlowScope);
    walker.addPreVisitor(stackCollector);

    ExecutionEntity scopeExecution = determineScopeExecution(commandContext, processInstance, processDefinition,
        elementToInstantiate, mapping, targetFlowScope, walker);

    List<PvmActivity> activitiesToInstantiate = stackCollector.getActivityStack();
    Collections.reverse(activitiesToInstantiate);

    // We have to make a distinction between
    // - "regular" activities for which the activity stack can be instantiated
    // and started
    // right away
    // - interrupting or cancelling activities for which we have to ensure that
    // the interruption and cancellation takes place before we instantiate the
    // activity stack
    ActivityImpl topMostActivity = determineTopMostActivity(activitiesToInstantiate, elementToInstantiate);
    ScopeImpl flowScope = determineFlowScope(activitiesToInstantiate, elementToInstantiate, topMostActivity);

    throwIfNoConcurrentInstantiationPossible(flowScope);

    ActivityStartBehavior startBehavior = determineStartBehavior(elementToInstantiate, activitiesToInstantiate,
        topMostActivity);

    executeInstantiation(elementToInstantiate, mapping, scopeExecution, activitiesToInstantiate, topMostActivity,
        startBehavior);

    return null;
  }

  private ExecutionEntity determineScopeExecution(final CommandContext commandContext, ExecutionEntity processInstance,
      final ProcessDefinitionImpl processDefinition, CoreModelElement elementToInstantiate,
      final ActivityExecutionTreeMapping mapping, ScopeImpl targetFlowScope, FlowScopeWalker walker) {
    return ancestorActivityInstanceId == null
        ? determineScopeExecutionWithoutAncestorActivity(processDefinition, mapping, targetFlowScope, walker)
        : determineScopeExecutionWithAncestorActivity(commandContext, processInstance, processDefinition,
            elementToInstantiate, mapping, walker);
  }

  private ExecutionEntity determineScopeExecutionWithoutAncestorActivity(final ProcessDefinitionImpl processDefinition,
      final ActivityExecutionTreeMapping mapping, ScopeImpl targetFlowScope, FlowScopeWalker walker) {
    // walk until a scope is reached for which executions exist
    walker.walkWhile(element -> !mapping.getExecutions(element).isEmpty() || element == processDefinition);

    Set<ExecutionEntity> flowScopeExecutions = mapping.getExecutions(walker.getCurrentElement());

    if (flowScopeExecutions.size() > 1) {
      throw new ProcessEngineException("Ancestor activity execution is ambiguous for activity " + targetFlowScope);
    }

    return flowScopeExecutions.iterator().next();
  }

  private ExecutionEntity determineScopeExecutionWithAncestorActivity(final CommandContext commandContext,
      ExecutionEntity processInstance, final ProcessDefinitionImpl processDefinition,
      CoreModelElement elementToInstantiate, final ActivityExecutionTreeMapping mapping, FlowScopeWalker walker) {
    ActivityInstance tree = commandContext.runWithoutAuthorization(new GetActivityInstanceCmd(processInstanceId));

    ActivityInstance ancestorInstance = findActivityInstance(tree, ancestorActivityInstanceId);
    EnsureUtil.ensureNotNull(NotValidException.class,
        describeFailure("Ancestor activity instance '" + ancestorActivityInstanceId + "' does not exist"),
        "ancestorInstance", ancestorInstance);

    // determine ancestor activity scope execution and activity
    final ExecutionEntity ancestorScopeExecution = getScopeExecutionForActivityInstance(processInstance, mapping,
        ancestorInstance);
    final PvmScope ancestorScope = getScopeForActivityInstance(processDefinition, ancestorInstance);

    // walk until the scope of the ancestor scope execution is reached
    walker.walkWhile(
        element -> (mapping.getExecutions(element).contains(ancestorScopeExecution) && element == ancestorScope)
            || element == processDefinition);

    Set<ExecutionEntity> flowScopeExecutions = mapping.getExecutions(walker.getCurrentElement());

    if (!flowScopeExecutions.contains(ancestorScopeExecution)) {
      throw new NotValidException(describeFailure("Scope execution for '" + ancestorActivityInstanceId
          + "' cannot be found in parent hierarchy of flow element '" + elementToInstantiate.getId() + "'"));
    }

    return ancestorScopeExecution;
  }

  private ActivityImpl determineTopMostActivity(List<PvmActivity> activitiesToInstantiate,
      CoreModelElement elementToInstantiate) {
    if (!activitiesToInstantiate.isEmpty()) {
      return (ActivityImpl) activitiesToInstantiate.get(0);
    } else if (ActivityImpl.class.isAssignableFrom(elementToInstantiate.getClass())) {
      return (ActivityImpl) elementToInstantiate;
    }

    return null;
  }

  private ScopeImpl determineFlowScope(List<PvmActivity> activitiesToInstantiate, CoreModelElement elementToInstantiate,
      ActivityImpl topMostActivity) {
    if (!activitiesToInstantiate.isEmpty() || ActivityImpl.class.isAssignableFrom(elementToInstantiate.getClass())) {
      return topMostActivity.getFlowScope();
    } else if (TransitionImpl.class.isAssignableFrom(elementToInstantiate.getClass())) {
      TransitionImpl transitionToInstantiate = (TransitionImpl) elementToInstantiate;
      return transitionToInstantiate.getSource().getFlowScope();
    }

    return null;
  }

  private void throwIfNoConcurrentInstantiationPossible(ScopeImpl flowScope) {
    if (!supportsConcurrentChildInstantiation(flowScope)) {
      throw new ProcessEngineException(
          "Concurrent instantiation not possible for " + "activities in scope " + flowScope.getId());
    }
  }

  private ActivityStartBehavior determineStartBehavior(CoreModelElement elementToInstantiate,
      List<PvmActivity> activitiesToInstantiate, ActivityImpl topMostActivity) {
    ActivityStartBehavior startBehavior = ActivityStartBehavior.CONCURRENT_IN_FLOW_SCOPE;
    if (topMostActivity != null) {
      startBehavior = topMostActivity.getActivityStartBehavior();

      if (!activitiesToInstantiate.isEmpty()) {
        // this is in BPMN relevant if there is an interrupting event sub
        // process.
        // we have to distinguish between instantiation of the start event and
        // any other activity.
        // instantiation of the start event means interrupting behavior;
        // instantiation
        // of any other task means no interruption.
        PvmActivity initialActivity = topMostActivity.getProperties().get(BpmnProperties.INITIAL_ACTIVITY);
        PvmActivity secondTopMostActivity = null;
        if (activitiesToInstantiate.size() > 1) {
          secondTopMostActivity = activitiesToInstantiate.get(1);
        } else if (ActivityImpl.class.isAssignableFrom(elementToInstantiate.getClass())) {
          secondTopMostActivity = (PvmActivity) elementToInstantiate;
        }

        if (initialActivity != secondTopMostActivity) {
          startBehavior = ActivityStartBehavior.CONCURRENT_IN_FLOW_SCOPE;
        }
      }
    }
    return startBehavior;
  }

  private void executeInstantiation(CoreModelElement elementToInstantiate, final ActivityExecutionTreeMapping mapping,
      ExecutionEntity scopeExecution, List<PvmActivity> activitiesToInstantiate, ActivityImpl topMostActivity,
      ActivityStartBehavior startBehavior) {
    switch (startBehavior) {
    case CANCEL_EVENT_SCOPE: {
      ScopeImpl scopeToCancel = topMostActivity.getEventScope();
      ExecutionEntity executionToCancel = getSingleExecutionForScope(mapping, scopeToCancel);
      if (executionToCancel != null) {
        executionToCancel.deleteCascade("Cancelling activity " + topMostActivity + " executed.", skipCustomListeners,
            skipIoMappings);
        instantiate(executionToCancel.getParent(), activitiesToInstantiate, elementToInstantiate);
      } else {
        ExecutionEntity flowScopeExecution = getSingleExecutionForScope(mapping, topMostActivity.getFlowScope());
        instantiateConcurrent(flowScopeExecution, activitiesToInstantiate, elementToInstantiate);
      }
      break;
    }
    case INTERRUPT_EVENT_SCOPE: {
      ScopeImpl scopeToCancel = topMostActivity.getEventScope();
      ExecutionEntity executionToCancel = getSingleExecutionForScope(mapping, scopeToCancel);
      executionToCancel.interrupt("Interrupting activity " + topMostActivity + " executed.", skipCustomListeners,
          skipIoMappings, false);
      executionToCancel.setActivity(null);
      executionToCancel.leaveActivityInstance();
      instantiate(executionToCancel, activitiesToInstantiate, elementToInstantiate);
      break;
    }
    case INTERRUPT_FLOW_SCOPE: {
      ScopeImpl scopeToCancel = topMostActivity.getFlowScope();
      ExecutionEntity executionToCancel = getSingleExecutionForScope(mapping, scopeToCancel);
      executionToCancel.interrupt("Interrupting activity " + topMostActivity + " executed.", skipCustomListeners,
          skipIoMappings, false);
      executionToCancel.setActivity(null);
      executionToCancel.leaveActivityInstance();
      instantiate(executionToCancel, activitiesToInstantiate, elementToInstantiate);
      break;
    }
    default: {
      // if all child executions have been cancelled
      // or this execution has ended executing its scope, it can be reused
      if (!scopeExecution.hasChildren() && (scopeExecution.getActivity() == null || scopeExecution.isEnded())) {
        // reuse the scope execution
        instantiate(scopeExecution, activitiesToInstantiate, elementToInstantiate);
      } else {
        // if the activity is not cancelling/interrupting, it can simply be
        // instantiated as
        // a concurrent child of the scopeExecution
        instantiateConcurrent(scopeExecution, activitiesToInstantiate, elementToInstantiate);
      }
      break;
    }
    }
  }

  /**
   * Cannot create more than inner instance in a sequential MI construct
   */
  protected boolean supportsConcurrentChildInstantiation(ScopeImpl flowScope) {
    CoreActivityBehavior<?> behavior = flowScope.getActivityBehavior();
    return !(behavior instanceof SequentialMultiInstanceActivityBehavior);
  }

  protected ExecutionEntity getSingleExecutionForScope(ActivityExecutionTreeMapping mapping, ScopeImpl scope) {
    Set<ExecutionEntity> executions = mapping.getExecutions(scope);

    if (!executions.isEmpty()) {
      if (executions.size() > 1) {
        throw new ProcessEngineException("Executions for activity " + scope + " ambiguous");
      }

      return executions.iterator().next();
    } else {
      return null;
    }
  }

  protected boolean isConcurrentStart(ActivityStartBehavior startBehavior) {
    return startBehavior == ActivityStartBehavior.DEFAULT
        || startBehavior == ActivityStartBehavior.CONCURRENT_IN_FLOW_SCOPE;
  }

  protected void instantiate(ExecutionEntity ancestorScopeExecution, List<PvmActivity> parentFlowScopes,
      CoreModelElement targetElement) {
    if (PvmTransition.class.isAssignableFrom(targetElement.getClass())) {
      ancestorScopeExecution.executeActivities(parentFlowScopes, null, (PvmTransition) targetElement, variables,
          variablesLocal, skipCustomListeners, skipIoMappings);
    } else if (PvmActivity.class.isAssignableFrom(targetElement.getClass())) {
      ancestorScopeExecution.executeActivities(parentFlowScopes, (PvmActivity) targetElement, null, variables,
          variablesLocal, skipCustomListeners, skipIoMappings);

    } else {
      throw new ProcessEngineException("Cannot instantiate element " + targetElement);
    }
  }

  protected void instantiateConcurrent(ExecutionEntity ancestorScopeExecution, List<PvmActivity> parentFlowScopes,
      CoreModelElement targetElement) {
    if (PvmTransition.class.isAssignableFrom(targetElement.getClass())) {
      ancestorScopeExecution.executeActivitiesConcurrent(parentFlowScopes, null, (PvmTransition) targetElement,
          variables, variablesLocal, skipCustomListeners, skipIoMappings);
    } else if (PvmActivity.class.isAssignableFrom(targetElement.getClass())) {
      ancestorScopeExecution.executeActivitiesConcurrent(parentFlowScopes, (PvmActivity) targetElement, null, variables,
          variablesLocal, skipCustomListeners, skipIoMappings);

    } else {
      throw new ProcessEngineException("Cannot instantiate element " + targetElement);
    }
  }

  protected abstract ScopeImpl getTargetFlowScope(ProcessDefinitionImpl processDefinition);

  protected abstract CoreModelElement getTargetElement(ProcessDefinitionImpl processDefinition);

  protected abstract String getTargetElementId();

}
