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
package org.operaton.bpm.engine.impl.cmd;

import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.batch.BatchEntity;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.management.UpdateJobDefinitionSuspensionStateBuilderImpl;
import org.operaton.bpm.engine.impl.persistence.entity.SuspensionState;

public class ActivateBatchCmd extends AbstractSetBatchStateCmd {

  public ActivateBatchCmd(String batchId) {
    super(batchId);
  }

  @Override
  protected SuspensionState getNewSuspensionState() {
    return SuspensionState.ACTIVE;
  }

  @Override
  protected void checkAccess(CommandChecker checker, BatchEntity batch) {
    checker.checkActivateBatch(batch);
  }

  @Override
  protected AbstractSetJobDefinitionStateCmd createSetJobDefinitionStateCommand(UpdateJobDefinitionSuspensionStateBuilderImpl builder) {
    return new ActivateJobDefinitionCmd(builder);
  }

  @Override
  protected String getUserOperationType() {
    return UserOperationLogEntry.OPERATION_TYPE_ACTIVATE_BATCH;
  }

}
