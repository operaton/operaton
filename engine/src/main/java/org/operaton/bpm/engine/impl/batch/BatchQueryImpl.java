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

import java.io.Serial;
import java.util.List;

import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.BatchQuery;
import org.operaton.bpm.engine.impl.AbstractQuery;
import org.operaton.bpm.engine.impl.BatchQueryProperty;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.SuspensionState;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

public class BatchQueryImpl extends AbstractQuery<BatchQuery, Batch> implements BatchQuery {

  @Serial private static final long serialVersionUID = 1L;

  protected String batchId;
  protected String type;
  protected boolean isTenantIdSet;
  protected String[] tenantIds;
  protected SuspensionState suspensionState;

  public BatchQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public BatchQuery batchId(String batchId) {
    ensureNotNull("Batch id", batchId);
    this.batchId = batchId;
    return this;
  }

  public String getBatchId() {
    return batchId;
  }

  @Override
  public BatchQuery type(String type) {
    ensureNotNull("Type", type);
    this.type = type;
    return this;
  }

  public String getType() {
    return type;
  }

  @Override
  public BatchQuery tenantIdIn(String... tenantIds) {
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
  public BatchQuery withoutTenantId() {
    this.tenantIds = null;
    isTenantIdSet = true;
    return this;
  }

  @Override
  public BatchQuery active() {
    this.suspensionState = SuspensionState.ACTIVE;
    return this;
  }

  @Override
  public BatchQuery suspended() {
    this.suspensionState = SuspensionState.SUSPENDED;
    return this;
  }

  public SuspensionState getSuspensionState() {
    return suspensionState;
  }

  @Override
  public BatchQuery orderById() {
    return orderBy(BatchQueryProperty.ID);
  }

  @Override
  public BatchQuery orderByTenantId() {
    return orderBy(BatchQueryProperty.TENANT_ID);
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return commandContext.getBatchManager()
      .findBatchCountByQueryCriteria(this);
  }

  @Override
  public List<Batch> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    return commandContext.getBatchManager()
      .findBatchesByQueryCriteria(this, page);
  }

}
