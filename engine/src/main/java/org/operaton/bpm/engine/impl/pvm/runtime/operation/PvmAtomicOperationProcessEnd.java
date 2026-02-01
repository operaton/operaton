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

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmmn.behavior.TransferVariablesActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnActivityExecution;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.PvmLogger;
import org.operaton.bpm.engine.impl.pvm.delegate.SubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;
import org.springframework.lang.NonNull;

/**
 * @author Tom Baeyens
 */
public class PvmAtomicOperationProcessEnd extends PvmAtomicOperationActivityInstanceEnd {

  private static final PvmLogger LOG = ProcessEngineLogger.PVM_LOGGER;

  @Override
  protected ScopeImpl getScope(PvmExecutionImpl execution) {
    return execution.getProcessDefinition();
  }

  protected String getEventName() {
    return ExecutionListener.EVENTNAME_END;
  }

  @Override
  protected void eventNotificationsCompleted(PvmExecutionImpl execution) {

    execution.leaveActivityInstance();

    PvmExecutionImpl superExecution = execution.getSuperExecution();
    CmmnActivityExecution superCaseExecution = execution.getSuperCaseExecution();

    SubProcessActivityBehavior subProcessActivityBehavior = null;

    // copy variables before destroying the ended sub process instance
    if (superExecution != null) {
      subProcessActivityBehavior = passOutputVariables(superExecution, execution);

      execution.destroy();
      execution.remove();

      // and trigger execution afterwards
      superExecution.setSubProcessInstance(null);
      try {
        subProcessActivityBehavior.completed(superExecution);
      } catch (RuntimeException e) {
        LOG.exceptionWhileCompletingSupProcess(execution, e);
        throw e;
      } catch (Exception e) {
        LOG.exceptionWhileCompletingSupProcess(execution, e);
        throw new ProcessEngineException("Error while completing sub process of execution %s".formatted(execution), e);
      }
    } else if (superCaseExecution != null) {
      transferVariables(superCaseExecution, execution);

      execution.destroy();
      execution.remove();

      // and trigger execution afterwards
      superCaseExecution.complete();
    }
  }

  @NonNull
  private static SubProcessActivityBehavior passOutputVariables(PvmExecutionImpl superExecution, PvmExecutionImpl execution) {
    PvmActivity activity = superExecution.getActivity();
    SubProcessActivityBehavior subProcessActivityBehavior = (SubProcessActivityBehavior) activity.getActivityBehavior();
    try {
      subProcessActivityBehavior.passOutputVariables(superExecution, execution);
    } catch (RuntimeException e) {
      LOG.exceptionWhileCompletingSupProcess(execution, e);
      throw e;
    } catch (Exception e) {
      LOG.exceptionWhileCompletingSupProcess(execution, e);
      throw new ProcessEngineException("Error while completing sub process of execution %s".formatted(execution), e);
    }
    return subProcessActivityBehavior;
  }

  private static void transferVariables(CmmnActivityExecution superCaseExecution, PvmExecutionImpl execution) {
    TransferVariablesActivityBehavior transferVariablesBehavior;
    CmmnActivity activity = superCaseExecution.getActivity();
    transferVariablesBehavior = (TransferVariablesActivityBehavior) activity.getActivityBehavior();
    try {
      transferVariablesBehavior.transferVariables(execution, superCaseExecution);
    } catch (RuntimeException e) {
      LOG.exceptionWhileCompletingSupProcess(execution, e);
      throw e;
    } catch (Exception e) {
      LOG.exceptionWhileCompletingSupProcess(execution, e);
      throw new ProcessEngineException("Error while completing sub process of execution %s".formatted(execution), e);
    }
  }


  @Override
  public String getCanonicalName() {
    return "process-end";
  }
}
