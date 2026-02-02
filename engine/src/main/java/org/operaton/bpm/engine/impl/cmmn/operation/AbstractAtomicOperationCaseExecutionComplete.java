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
package org.operaton.bpm.engine.impl.cmmn.operation;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnCompositeActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.behavior.TransferVariablesActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnExecution;
import org.operaton.bpm.engine.impl.pvm.delegate.SubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

import static org.operaton.bpm.engine.delegate.CaseExecutionListener.COMPLETE;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.COMPLETED;
import static org.operaton.bpm.engine.impl.util.ActivityBehaviorUtil.getActivityBehavior;

/**
 * @author Roman Smirnov
 *
 */
public abstract class AbstractAtomicOperationCaseExecutionComplete extends AbstractCmmnEventAtomicOperation {

  protected static final CmmnOperationLogger LOG = ProcessEngineLogger.CMMN_OPERATION_LOGGER;

  protected String getEventName() {
    return COMPLETE;
  }

  @Override
  protected CmmnExecution eventNotificationsStarted(CmmnExecution execution) {
    CmmnActivityBehavior behavior = getActivityBehavior(execution);
    triggerBehavior(behavior, execution);

    execution.setCurrentState(COMPLETED);

    return execution;
  }

  @Override
  protected void postTransitionNotification(CmmnExecution execution) {
    if (!execution.isCaseInstanceExecution()) {
      execution.remove();
    } else {
      handleCaseInstanceCompletion(execution);
    }

    handleParentCompletion(execution);
  }

  protected void handleCaseInstanceCompletion(CmmnExecution execution) {
    CmmnExecution superCaseExecution = execution.getSuperCaseExecution();
    PvmExecutionImpl superExecution = execution.getSuperExecution();

    if (superCaseExecution != null) {
      handleSuperCaseExecutionCompletion(execution, superCaseExecution);
    } else if (superExecution != null) {
      handleSuperExecutionCompletion(execution, superExecution);
    }

    execution.setSuperCaseExecution(null);
    execution.setSuperExecution(null);
  }

  protected void handleSuperCaseExecutionCompletion(CmmnExecution execution, CmmnExecution superCaseExecution) {
    TransferVariablesActivityBehavior behavior = (TransferVariablesActivityBehavior) getActivityBehavior(superCaseExecution);
    behavior.transferVariables(execution, superCaseExecution);
    superCaseExecution.complete();
  }

  protected void handleSuperExecutionCompletion(CmmnExecution execution, PvmExecutionImpl superExecution) {
    SubProcessActivityBehavior behavior = (SubProcessActivityBehavior) getActivityBehavior(superExecution);
    passOutputVariables(behavior, superExecution, execution);
    superExecution.setSubCaseInstance(null);
    completeSuperExecution(behavior, superExecution, execution);
  }

  protected void passOutputVariables(SubProcessActivityBehavior behavior, PvmExecutionImpl superExecution, CmmnExecution execution) {
    try {
      behavior.passOutputVariables(superExecution, execution);
    } catch (RuntimeException e) {
      LOG.completingSubCaseError(execution, e);
      throw e;
    } catch (Exception e) {
      LOG.completingSubCaseError(execution, e);
      throw LOG.completingSubCaseErrorException(execution, e);
    }
  }

  protected void completeSuperExecution(SubProcessActivityBehavior behavior, PvmExecutionImpl superExecution, CmmnExecution execution) {
    try {
      behavior.completed(superExecution);
    } catch (RuntimeException e) {
      LOG.completingSubCaseError(execution, e);
      throw e;
    } catch (Exception e) {
      LOG.completingSubCaseError(execution, e);
      throw LOG.completingSubCaseErrorException(execution, e);
    }
  }

  protected void handleParentCompletion(CmmnExecution execution) {
    CmmnExecution parent = execution.getParent();
    if (parent != null) {
      CmmnActivityBehavior behavior = getActivityBehavior(parent);
      if (behavior instanceof CmmnCompositeActivityBehavior compositeBehavior) {
        compositeBehavior.handleChildCompletion(parent, execution);
      }
    }
  }

  protected abstract void triggerBehavior(CmmnActivityBehavior behavior, CmmnExecution execution);

}
