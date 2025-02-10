/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.pvm.runtime.operation;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.ScopeInstantiationContext;
import org.operaton.bpm.engine.impl.pvm.runtime.InstantiationStack;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

/**
 * @author Thorben Lindhauer
 *
 */
public class PvmAtomicOperationActivityInitStackNotifyListenerReturn extends PvmAtomicOperationActivityInstanceStart {

  @Override
  public String getCanonicalName() {
    return "activity-init-stack-notify-listener-return";
  }

  @Override
  protected ScopeImpl getScope(PvmExecutionImpl execution) {
    ActivityImpl activity = execution.getActivity();

    if (activity!=null) {
      return activity;
    } else {
      PvmExecutionImpl parent = execution.getParent();
      if (parent != null) {
        return getScope(execution.getParent());
      }
      return execution.getProcessDefinition();
    }
  }

  protected String getEventName() {
    return ExecutionListener.EVENTNAME_START;
  }

  @Override
  protected void eventNotificationsCompleted(PvmExecutionImpl execution) {
    super.eventNotificationsCompleted(execution);

    ScopeInstantiationContext startContext = execution.getScopeInstantiationContext();
    InstantiationStack instantiationStack = startContext.getInstantiationStack();

    // if the stack has been instantiated
    if (instantiationStack.getActivities().isEmpty()) {
      // done
      execution.disposeScopeInstantiationContext();
    }
    else {
      // else instantiate the activity stack further
      execution.setActivity(null);
      execution.performOperation(ACTIVITY_INIT_STACK_AND_RETURN);
    }
  }

}
