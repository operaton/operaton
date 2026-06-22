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
package org.operaton.bpm.engine.rest.sub.history.impl;

import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.rest.dto.history.batch.HistoricBatchDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.history.HistoricBatchResource;

public class HistoricBatchResourceImpl implements HistoricBatchResource {

  protected ProcessEngine processEngine;
  protected String batchId;

  public HistoricBatchResourceImpl(ProcessEngine processEngine, String batchId) {
    this.processEngine = processEngine;
    this.batchId = batchId;
  }

  @Override
  public HistoricBatchDto getHistoricBatch() {
    HistoricBatch batch = processEngine.getHistoryService()
      .createHistoricBatchQuery()
      .batchId(batchId)
      .singleResult();

    if (batch == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Historic batch with id '%s' does not exist".formatted(batchId));
    }

    return HistoricBatchDto.fromBatch(batch);
  }

  @Override
  public void deleteHistoricBatch() {
    try {
      processEngine.getHistoryService()
        .deleteHistoricBatch(batchId);
    }
    catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, "Unable to delete historic batch with id '%s'".formatted(batchId));
    }
  }

}
