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
import org.operaton.bpm.engine.impl.pvm.delegate.CompositeActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.runtime.CompensationBehavior;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;


/**
 * <p>Base Atomic operation for implementing atomic operations which mark the creation
 * of a new activity instance.</p>
 *
 * <p>The new activity instance is created *before* the START listeners are invoked
 * on the execution.</p>
 *
 * @author Daniel Meyer
 *
 */
public abstract class PvmAtomicOperationActivityInstanceStart extends AbstractPvmEventAtomicOperation {

  @Override
  protected PvmExecutionImpl eventNotificationsStarted(PvmExecutionImpl execution) {
    execution.incrementSequenceCounter();
    execution.activityInstanceStarting();
    execution.enterActivityInstance();
    execution.setTransition(null);

    return execution;
  }

  protected void eventNotificationsCompleted(PvmExecutionImpl execution) {

    // hack around execution tree structure not being in sync with activity instance concept:
    // if we start a scope activity, remember current activity instance in parent
    PvmExecutionImpl parent = execution.getParent();
    PvmActivity activity = execution.getActivity();
    if(parent != null && execution.isScope() && activity.isScope() && canHaveChildScopes(execution)) {
      parent.setActivityInstanceId(execution.getActivityInstanceId());
    }

  }

  protected boolean canHaveChildScopes(PvmExecutionImpl execution) {
    PvmActivity activity = execution.getActivity();
    return activity.getActivityBehavior() instanceof CompositeActivityBehavior
        || CompensationBehavior.isCompensationThrowing(execution);
  }

}
