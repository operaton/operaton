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
package org.operaton.bpm.engine.impl.pvm.runtime.operation;

import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocStartability;
import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.BpmnActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.PvmScope;
import org.operaton.bpm.engine.impl.pvm.PvmTransition;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.delegate.CompositeActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.LegacyBehavior;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 * @author Thorben Lindhauer
 */
public class PvmAtomicOperationActivityEnd implements PvmAtomicOperation {

  protected PvmScope getScope(PvmExecutionImpl execution) {
    return execution.getActivity();
  }

  @Override
  public boolean isAsync(PvmExecutionImpl execution) {
    return execution.getActivity().isAsyncAfter();
  }

  @Override
  public boolean isAsyncCapable() {
    return false;
  }

  @Override
  public void execute(PvmExecutionImpl execution) {
    // restore activity instance id
    if (execution.getActivityInstanceId() == null) {
      execution.setActivityInstanceId(execution.getParentActivityInstanceId());
    }

    PvmActivity activity = execution.getActivity();
    Map<ScopeImpl, PvmExecutionImpl> activityExecutionMapping = execution.createActivityExecutionMapping();

    if (activity.getFlowScope() instanceof ActivityImpl adHocScope
        && adHocScope.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior adHocBehavior
        && adHocBehavior.isCompletableActivity((ActivityImpl) activity)
        && (adHocBehavior.shouldHandleChildCompletion(activityExecutionMapping.get(adHocScope),
            (ActivityImpl) activity) || !activity.getOutgoingTransitions().isEmpty())) {
      completeAdHocActivity(execution, activityExecutionMapping.get(adHocScope), adHocBehavior);
      return;
    }

    ActivityImpl modelActivity = AdHocStartability.INSTANCE.getModelActivity((ActivityImpl) activity);
    if (!activity.getOutgoingTransitions().isEmpty()
        && AdHocStartability.INSTANCE.isStartableActivityType(
            (String) modelActivity.getProperty(BpmnProperties.TYPE.name()))) {
      // Gateway/event END operations can retire joined tokens and must not take their flows again.
      // Migration may move a deferred scoped activity end into an ordinary flow scope.
      // Its end listeners already ran; output mappings and flow selection are still pending.
      boolean destroyScope = execution.isScope() && activity.isScope()
          && !LegacyBehavior.destroySecondNonScope(execution);
      if (destroyScope && execution.getActivity().getIoMapping() != null && !execution.isSkipIoMappings()) {
        execution.getActivity().getIoMapping().executeOutputParameters(execution);
      }
      List<PvmTransition> transitions = new BpmnActivityBehavior().selectOutgoingTransitions(execution);
      PvmExecutionImpl propagatingExecution = execution;
      if (destroyScope) {
        execution.destroy(true);
        if (!execution.isConcurrent()) {
          propagatingExecution = execution.getParent();
          propagatingExecution.setActivity(activity);
          execution.remove();
        }
      }
      propagatingExecution.setEnded(false);
      propagatingExecution.setActive(true);
      PvmAtomicOperationTransitionDestroyScope.takeTransitions(propagatingExecution, transitions, true);
      return;
    }

    PvmExecutionImpl propagatingExecution = execution;

    if((execution.isScope() && activity.isScope()) && !LegacyBehavior.destroySecondNonScope(execution)) {
      execution.destroy();
      if(!execution.isConcurrent()) {
        execution.remove();
        propagatingExecution = execution.getParent();
        propagatingExecution.setActivity(execution.getActivity());
      }
    }

    propagatingExecution = LegacyBehavior.determinePropagatingExecutionOnEnd(propagatingExecution, activityExecutionMapping);
    PvmScope flowScope = activity.getFlowScope();

    if(flowScope == activity.getProcessDefinition()) {
      executeProcessDefinition(propagatingExecution);
    } else {
      executeNonProcessDefinition((PvmActivity) flowScope, propagatingExecution, activity);
    }
  }

  /**
   * An ad-hoc child takes the normal activity-end path even when it has outgoing
   * flows. End listeners and the async-after boundary have already run. Apply
   * output mappings before evaluating completion, keeping local variables alive
   * until any outgoing conditions have been selected.
   */
  private static void completeAdHocActivity(PvmExecutionImpl execution, PvmExecutionImpl scopeExecution,
      AdHocSubProcessActivityBehavior behavior) {
    ActivityImpl activity = execution.getActivity();
    PvmExecutionImpl completingChild = execution;
    while (completingChild.getParent() != scopeExecution) {
      completingChild = completingChild.getParent();
    }

    boolean destroyScope = execution.isScope() && activity.isScope()
        && !LegacyBehavior.destroySecondNonScope(execution);
    if (destroyScope && activity.getIoMapping() != null && !execution.isSkipIoMappings()) {
      activity.getIoMapping().executeOutputParameters(execution);
    }

    boolean completionRequested = behavior.activityCompleted(scopeExecution, completingChild, activity);
    List<PvmTransition> transitions = completionRequested ? List.of()
        : new BpmnActivityBehavior().selectOutgoingTransitions(execution);

    if (destroyScope) {
      // Output mappings have already run, including their side effects.
      execution.destroy(true);
      if (execution != completingChild) {
        execution.remove();
        completingChild.setActivity(activity);
      }
    }

    if (transitions.isEmpty()) {
      behavior.completedActivityExecutionEnded(scopeExecution, completingChild);
    } else {
      completingChild.setEnded(false);
      completingChild.setActive(true);
      behavior.activityContinued(scopeExecution);
      PvmAtomicOperationTransitionDestroyScope.takeTransitions(completingChild, transitions, true);
    }
  }

  private static void executeProcessDefinition(PvmExecutionImpl propagatingExecution) {
    // 1. flow scope = Process Definition
    // 1.1 concurrent execution => end + tryPrune()
    if(propagatingExecution.isConcurrent()) {
      propagatingExecution.remove();
      propagatingExecution.getParent().tryPruneLastConcurrentChild();
      propagatingExecution.getParent().forceUpdate();
    }
    else {
      // 1.2 Process End
      propagatingExecution.setEnded(true);
      if (!propagatingExecution.isPreserveScope()) {
        propagatingExecution.performOperation(PROCESS_END);
      }
    }
  }

  private static void executeNonProcessDefinition(PvmActivity flowScope, PvmExecutionImpl propagatingExecution, PvmActivity activity) {
    // 2. flowScope != process definition
    PvmActivity flowScopeActivity = flowScope;

    ActivityBehavior activityBehavior = flowScopeActivity.getActivityBehavior();
    if (activityBehavior instanceof CompositeActivityBehavior compositeActivityBehavior) {
      // 2.1 Concurrent execution => composite behavior.concurrentExecutionEnded()
      if(propagatingExecution.isConcurrent() && !LegacyBehavior.isConcurrentScope(propagatingExecution)) {
        compositeActivityBehavior.concurrentChildExecutionEnded(propagatingExecution.getParent(), propagatingExecution);
      }
      else {
        // 2.2 Scope Execution => composite behavior.complete()
        propagatingExecution.setActivity(flowScopeActivity);
        compositeActivityBehavior.complete(propagatingExecution);
      }

    }
    else {
      // activity behavior is not composite => this is unexpected
      throw new ProcessEngineException("Expected behavior of composite scope %s to be a CompositeActivityBehavior but got %s"
          .formatted(activity, activityBehavior));
    }
  }

  @Override
  public String getCanonicalName() {
    return "activity-end";
  }

}
