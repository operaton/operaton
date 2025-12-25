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
package org.operaton.bpm.engine.rest.sub.batch.impl;

import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.rest.dto.SuspensionStateDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.batch.BatchResource;

public class BatchResourceImpl implements BatchResource {

  protected ProcessEngine processEngine;
  protected String batchId;

  public BatchResourceImpl(ProcessEngine processEngine, String batchId) {
    this.processEngine = processEngine;
    this.batchId = batchId;
  }

  @Override
  public BatchDto getBatch() {
    Batch batch = processEngine.getManagementService()
      .createBatchQuery()
      .batchId(batchId)
      .singleResult();

    if (batch == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Batch with id '%s' does not exist".formatted(batchId));
    }

    return BatchDto.fromBatch(batch);
  }

  @Override
  public void updateSuspensionState(SuspensionStateDto suspensionState) {
    if (suspensionState.getSuspended()) {
      suspendBatch();
    }
    else {
      activateBatch();
    }
  }

  protected void suspendBatch() {
    try {
      processEngine.getManagementService().suspendBatchById(batchId);
    }
    catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, "Unable to suspend batch with id '" + batchId + "'");
    }
  }

  protected void activateBatch() {
    try {
      processEngine.getManagementService().activateBatchById(batchId);
    }
    catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, "Unable to activate batch with id '" + batchId + "'");
    }
  }

  @Override
  public void deleteBatch(boolean cascade) {
    try {
      processEngine.getManagementService()
        .deleteBatch(batchId, cascade);
    }
    catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, "Unable to delete batch with id '" + batchId + "'");
    }
  }

}
