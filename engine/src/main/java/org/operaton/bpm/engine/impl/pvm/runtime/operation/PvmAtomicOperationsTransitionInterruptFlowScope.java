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

import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

/**
 * In BPMN this is used for terminate end events
 *
 * @author Daniel Meyer
 *
 */
public class PvmAtomicOperationsTransitionInterruptFlowScope extends PvmAtomicOperationInterruptScope {

  @Override
  public String getCanonicalName() {
    return "transition-interrupt-scope";
  }

  @Override
  protected void scopeInterrupted(PvmExecutionImpl execution) {
    execution.dispatchDelayedEventsAndPerformOperation(TRANSITION_CREATE_SCOPE);
  }

  @Override
  protected PvmActivity getInterruptingActivity(PvmExecutionImpl execution) {
    return execution.getTransition().getDestination();
  }

}
