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
package org.operaton.bpm.engine.impl.batch;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Thorben Lindhauer
 *
 */
public class DeleteBatchCmd implements Command<Void> {

  protected boolean cascadeToHistory;
  protected String batchId;

  public DeleteBatchCmd(String batchId, boolean cascadeToHistory) {
    this.batchId = batchId;
    this.cascadeToHistory = cascadeToHistory;
  }

  @Override
  public Void execute(CommandContext commandContext) {
    ensureNotNull(BadUserRequestException.class, "Batch id must not be null", "batch id", batchId);

    BatchEntity batchEntity = commandContext.getBatchManager().findBatchById(batchId);
    ensureNotNull(BadUserRequestException.class, "Batch for id '%s' cannot be found".formatted(batchId), "batch", batchEntity);

    checkAccess(commandContext, batchEntity);
    writeUserOperationLog(commandContext, batchEntity.getTenantId());
    batchEntity.delete(cascadeToHistory, true);

    return null;
  }

  protected void checkAccess(CommandContext commandContext, BatchEntity batch) {
    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkDeleteBatch(batch);
    }
  }

  protected void writeUserOperationLog(CommandContext commandContext, String tenantId) {
    commandContext.getOperationLogManager()
      .logBatchOperation(UserOperationLogEntry.OPERATION_TYPE_DELETE,
        batchId,
        tenantId,
        new PropertyChange("cascadeToHistory", null, cascadeToHistory));
  }
}
