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

import static org.operaton.bpm.engine.delegate.CaseExecutionListener.DISABLE;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.DISABLED;
import static org.operaton.bpm.engine.impl.util.ActivityBehaviorUtil.getActivityBehavior;

import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnCompositeActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnExecution;

/**
 * @author Roman Smirnov
 *
 */
public class AtomicOperationCaseExecutionDisable extends AbstractCmmnEventAtomicOperation {

  @Override
  public String getCanonicalName() {
    return "case-execution-disable";
  }

  protected String getEventName() {
    return DISABLE;
  }

  @Override
  protected CmmnExecution eventNotificationsStarted(CmmnExecution execution) {
    CmmnActivityBehavior behavior = getActivityBehavior(execution);
    behavior.onDisable(execution);

    execution.setCurrentState(DISABLED);

    return execution;
  }

  @Override
  protected void preTransitionNotification(CmmnExecution execution) {
    CmmnExecution parent = execution.getParent();
    if (parent != null) {
      CmmnActivityBehavior behavior = getActivityBehavior(parent);
      if (behavior instanceof CmmnCompositeActivityBehavior compositeBehavior) {
        compositeBehavior.handleChildDisabled(parent, execution);
      }
    }
  }

}
