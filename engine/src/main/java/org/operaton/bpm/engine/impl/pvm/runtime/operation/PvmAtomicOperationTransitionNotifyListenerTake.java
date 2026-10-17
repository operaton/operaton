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

import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocStartability;
import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;


/**
 * @author Tom Baeyens
 */
public class PvmAtomicOperationTransitionNotifyListenerTake extends AbstractPvmAtomicOperationTransitionNotifyListenerTake {

  @Override
  public void execute(PvmExecutionImpl execution) {
    if (execution.getActivity().getFlowScope() instanceof ActivityImpl flowScope
        && flowScope.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior adHocBehavior) {
      // A gateway/event can already be waiting at async-after when a different
      // child satisfies completion. Its continuation must not start new work.
      // Use the execution mapping because the source's scope may already have
      // been destroyed before reaching this transition continuation.
      PvmExecutionImpl scopeExecution = execution.createActivityExecutionMapping().get(flowScope);
      if (AdHocStartability.INSTANCE.isCompletionRequested(scopeExecution)) {
        adHocBehavior.completedActivityExecutionEnded(scopeExecution, execution);
        return;
      }
    }
    super.execute(execution);
  }

  @Override
  protected void eventNotificationsCompleted(PvmExecutionImpl execution) {
    ActivityImpl destination = (ActivityImpl) execution.getTransition().getDestination();
    if (destination.getFlowScope() instanceof ActivityImpl flowScope
        && flowScope.getActivityBehavior() instanceof AdHocSubProcessActivityBehavior behavior
        && behavior.isCompletableActivity(destination)) {
      execution.setActivity(destination);
      execution.dispatchDelayedEventsAndPerformOperation(resumedExecution -> {
        PvmExecutionImpl scope = resumedExecution.createActivityExecutionMapping().get(flowScope);
        resumedExecution.setTransition(null);
        behavior.enableActivity(scope, resumedExecution, destination);
        // Parking changes engine-owned readiness variables and can itself satisfy
        // conditional subscriptions. Flush those events without starting the target.
        resumedExecution.dispatchDelayedEventsAndPerformOperation(ignored -> null);
        return null;
      });
      return;
    }
    super.eventNotificationsCompleted(execution);
  }

  @Override
  public boolean isAsync(PvmExecutionImpl execution) {
    return execution.getActivity().isAsyncAfter();
  }

  @Override
  public String getCanonicalName() {
    return "transition-notify-listener-take";
  }

  @Override
  public boolean isAsyncCapable() {
    return true;
  }

  @Override
  public boolean shouldHandleFailureAsBpmnError() {
    return true;
  }

}
