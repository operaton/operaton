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
package org.operaton.bpm.engine.impl.persistence.entity;

import org.operaton.bpm.engine.history.CleanableHistoricBatchReportResult;

public class CleanableHistoricBatchesReportResultEntity implements CleanableHistoricBatchReportResult {

  protected String batchType;
  protected Integer historyTimeToLive;
  protected long finishedBatchesCount;
  protected long cleanableBatchesCount;

  @Override
  public String getBatchType() {
    return batchType;
  }

  public void setBatchType(String batchType) {
    this.batchType = batchType;
  }

  @Override
  public Integer getHistoryTimeToLive() {
    return historyTimeToLive;
  }

  public void setHistoryTimeToLive(Integer historyTimeToLive) {
    this.historyTimeToLive = historyTimeToLive;
  }

  @Override
  public long getFinishedBatchesCount() {
    return finishedBatchesCount;
  }

  public void setFinishedBatchesCount(long finishedBatchCount) {
    this.finishedBatchesCount = finishedBatchCount;
  }

  @Override
  public long getCleanableBatchesCount() {
    return cleanableBatchesCount;
  }

  public void setCleanableBatchesCount(long cleanableBatchCount) {
    this.cleanableBatchesCount = cleanableBatchCount;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
        + "[batchType = " + batchType
        + ", historyTimeToLive = " + historyTimeToLive
        + ", finishedBatchesCount = " + finishedBatchesCount
        + ", cleanableBatchesCount = " + cleanableBatchesCount
        + "]";
  }
}
