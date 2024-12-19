/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.batch.history;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.util.List;

import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.batch.history.HistoricBatchQuery;
import org.operaton.bpm.engine.impl.AbstractQuery;
import org.operaton.bpm.engine.impl.HistoricBatchQueryProperty;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

public class HistoricBatchQueryImpl extends AbstractQuery<HistoricBatchQuery, HistoricBatch> implements HistoricBatchQuery {

  private static final long serialVersionUID = 1L;

  protected String batchId;
  protected String type;
  protected Boolean completed;
  protected boolean isTenantIdSet = false;
  protected String[] tenantIds;

  public HistoricBatchQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public HistoricBatchQuery batchId(String batchId) {
    ensureNotNull("Batch id", batchId);
    this.batchId = batchId;
    return this;
  }

  public String getBatchId() {
    return batchId;
  }

  @Override
  public HistoricBatchQuery type(String type) {
    ensureNotNull("Type", type);
    this.type = type;
    return this;
  }

  @Override
  public HistoricBatchQuery completed(boolean completed) {
    this.completed = completed;
    return this;
  }

  @Override
  public HistoricBatchQuery tenantIdIn(String... tenantIds) {
    ensureNotNull("tenantIds", (Object[]) tenantIds);
    this.tenantIds = tenantIds;
    isTenantIdSet = true;
    return this;
  }

  public String[] getTenantIds() {
    return tenantIds;
  }

  public boolean isTenantIdSet() {
    return isTenantIdSet;
  }

  @Override
  public HistoricBatchQuery withoutTenantId() {
    this.tenantIds = null;
    isTenantIdSet = true;
    return this;
  }

  public String getType() {
    return type;
  }

  @Override
  public HistoricBatchQuery orderById() {
    return orderBy(HistoricBatchQueryProperty.ID);
  }

  @Override
  public HistoricBatchQuery orderByStartTime() {
    return orderBy(HistoricBatchQueryProperty.START_TIME);
  }

  @Override
  public HistoricBatchQuery orderByEndTime() {
    return orderBy(HistoricBatchQueryProperty.END_TIME);
  }

  @Override
  public HistoricBatchQuery orderByTenantId() {
    return orderBy(HistoricBatchQueryProperty.TENANT_ID);
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return commandContext
      .getHistoricBatchManager()
    .findBatchCountByQueryCriteria(this);
  }


  @Override
  public List<HistoricBatch> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    return commandContext
      .getHistoricBatchManager()
      .findBatchesByQueryCriteria(this, page);
  }
}
