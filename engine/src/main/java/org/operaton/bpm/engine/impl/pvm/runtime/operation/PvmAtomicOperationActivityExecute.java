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

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.pvm.PvmException;
import org.operaton.bpm.engine.impl.pvm.PvmLogger;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

import static org.operaton.bpm.engine.impl.util.ActivityBehaviorUtil.getActivityBehavior;

/**
 * @author Tom Baeyens
 */
public class PvmAtomicOperationActivityExecute implements PvmAtomicOperation {

  private static final PvmLogger LOG = ProcessEngineLogger.PVM_LOGGER;

  @Override
  public boolean isAsync(PvmExecutionImpl execution) {
    return false;
  }

  @Override
  public void execute(PvmExecutionImpl execution) {
    execution.activityInstanceStarted();

    execution.continueIfExecutionDoesNotAffectNextOperation(execution1 -> {
      if (execution1.getActivity().isScope()) {
        execution1.dispatchEvent(null);
      }
      return null;
    }, execution2 -> {

      ActivityBehavior activityBehavior = getActivityBehavior(execution2);

      ActivityImpl activity = execution2.getActivity();
      LOG.debugExecutesActivity(execution2, activity, activityBehavior.getClass().getName());

      try {
        activityBehavior.execute(execution2);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new PvmException("couldn't execute activity <%s id=\"%s\" ...>: %s".formatted(activity.getProperty("type"), activity.getId(), e.getMessage()), e);
      }
      return null;
    }, execution);
  }

  @Override
  public String getCanonicalName() {
    return "activity-execute";
  }

  @Override
  public boolean isAsyncCapable() {
    return false;
  }
}
