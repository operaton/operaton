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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.annotation.Nullable;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.PvmLogger;
import org.operaton.bpm.engine.impl.pvm.PvmTransition;
import org.operaton.bpm.engine.impl.pvm.runtime.LegacyBehavior;
import org.operaton.bpm.engine.impl.pvm.runtime.OutgoingExecution;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 * @author Thorben Lindhauer
 */
public class PvmAtomicOperationTransitionDestroyScope implements PvmAtomicOperation {

  private static final PvmLogger LOG = ProcessEngineLogger.PVM_LOGGER;

  @Override
  public boolean isAsync(PvmExecutionImpl instance) {
    return false;
  }

  @Override
  public boolean isAsyncCapable() {
    return false;
  }

  @Override
  public void execute(PvmExecutionImpl execution) {

    // calculate the propagating execution
    PvmExecutionImpl propagatingExecution = execution;

    PvmActivity activity = execution.getActivity();
    List<PvmTransition> transitionsToTake = execution.getTransitionsToTake();
    execution.setTransitionsToTake(null);

    // check whether the current scope needs to be destroyed
    if (execution.isScope() && activity.isScope() && !LegacyBehavior.destroySecondNonScope(execution)) {
      if (execution.isConcurrent()) {
        // legacy behavior
        LegacyBehavior.destroyConcurrentScope(execution);
      }
      else {
        propagatingExecution = execution.getParent();
        LOG.debugDestroyScope(execution, propagatingExecution);
        execution.destroy();
        propagatingExecution.setActivity(execution.getActivity());
        propagatingExecution.setTransition(execution.getTransition());
        propagatingExecution.setActive(true);
        execution.remove();
      }
    }

    // take the specified transitions
    if (transitionsToTake.isEmpty()) {
      throw new ProcessEngineException("%s: No outgoing transitions from activity %s".formatted(execution.toString(), activity));
    }
    else if (transitionsToTake.size() == 1) {
      propagatingExecution.setTransition(transitionsToTake.get(0));
      propagatingExecution.take();
    }
    else {
      propagatingExecution.inactivate();

      List<OutgoingExecution> outgoingExecutions = collectOutgoingExecutions(transitionsToTake, propagatingExecution);

      // start executions in reverse order (order will be reversed again in command context with the effect that they are
      // actually be started in correct order :) )
      Collections.reverse(outgoingExecutions);

      for (OutgoingExecution outgoingExecution : outgoingExecutions) {
        outgoingExecution.take();
      }
    }

  }

  private static List<OutgoingExecution> collectOutgoingExecutions(List<PvmTransition> transitionsToTake,
      PvmExecutionImpl propagatingExecution) {
    List<OutgoingExecution> outgoingExecutions = new ArrayList<>();

    for (int i = 0; i < transitionsToTake.size(); i++) {
      PvmTransition transition = transitionsToTake.get(i);

      PvmExecutionImpl scopeExecution = propagatingExecution.isScope() ?
          propagatingExecution : propagatingExecution.getParent();

      // reuse concurrent, propagating execution for first transition
      PvmExecutionImpl concurrentExecution = getPvmExecution(transitionsToTake, propagatingExecution, i, scopeExecution,
          outgoingExecutions);

      outgoingExecutions.add(new OutgoingExecution(concurrentExecution, transition));
    }
    return outgoingExecutions;
  }

  private static PvmExecutionImpl getPvmExecution(List<PvmTransition> transitionsToTake,
                                                  PvmExecutionImpl propagatingExecution,
                                                  int i,
                                                  PvmExecutionImpl scopeExecution,
                                                  List<OutgoingExecution> outgoingExecutions) {
    PvmExecutionImpl concurrentExecution = null;
    if (i == 0) {
      concurrentExecution = propagatingExecution;
    }
    else {
      concurrentExecution = scopeExecution.createConcurrentExecution();

      if (i == 1 && !propagatingExecution.isConcurrent()) {
        outgoingExecutions.remove(0);
        // get a hold of the concurrent execution that replaced the scope propagating execution
        PvmExecutionImpl replacingExecution = getReplacingExecution(propagatingExecution, scopeExecution);
        if (replacingExecution != null) {
        outgoingExecutions.add(new OutgoingExecution(replacingExecution, transitionsToTake.get(0)));
        }
      }
    }
    return concurrentExecution;
  }

  @Nullable
  private static PvmExecutionImpl getReplacingExecution(PvmExecutionImpl propagatingExecution, PvmExecutionImpl scopeExecution) {
    PvmExecutionImpl replacingExecution = null;
    for (PvmExecutionImpl concurrentChild : scopeExecution.getNonEventScopeExecutions())  {
      if (concurrentChild != propagatingExecution) {
        replacingExecution = concurrentChild;
        break;
      }
    }
    return replacingExecution;
  }

  @Override
  public String getCanonicalName() {
    return "transition-destroy-scope";
  }
}
