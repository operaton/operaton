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
package org.operaton.bpm.engine.impl.cmmn.behavior;

import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.ACTIVE;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.FAILED;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_IS_BLOCKING;

import org.operaton.bpm.engine.impl.cmmn.execution.CmmnActivityExecution;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;

/**
 * @author Roman Smirnov
 *
 */
public class TaskActivityBehavior extends StageOrTaskActivityBehavior {

  @Override
  public void onReactivation(CmmnActivityExecution execution) {
    ensureTransitionAllowed(execution, FAILED, ACTIVE, "re-activate");
  }

  @Override
  protected void performStart(CmmnActivityExecution execution) {
    execution.complete();
  }

  @Override
  public void fireExitCriteria(CmmnActivityExecution execution) {
    execution.exit();
  }

  protected boolean isBlocking(CmmnActivityExecution execution) {
    CmmnActivity activity = execution.getActivity();
    Object isBlockingProperty = activity.getProperty(PROPERTY_IS_BLOCKING);
    if (isBlockingProperty != null && isBlockingProperty instanceof Boolean b) {
      return b;
    }
    return false;
  }

  @Override
  protected String getTypeName() {
    return "task";
  }


}
