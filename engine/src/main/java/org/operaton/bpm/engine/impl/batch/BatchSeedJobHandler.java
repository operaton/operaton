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

import org.operaton.bpm.engine.impl.batch.BatchSeedJobHandler.BatchSeedJobConfiguration;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.jobexecutor.JobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.JobHandlerConfiguration;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * The batch seed job handler is responsible to
 * create all jobs to be executed by the batch.
 * <p>
 * If all jobs are created a seed monitor job is
 * created to oversee the completion of the batch
 * (see {@link BatchMonitorJobHandler}).
 * </p>
 */
public class BatchSeedJobHandler implements JobHandler<BatchSeedJobConfiguration> {

  public static final String TYPE = "batch-seed-job";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public void execute(BatchSeedJobConfiguration configuration, ExecutionEntity execution, CommandContext commandContext, String tenantId) {

    String batchId = configuration.getBatchId();
    BatchEntity batch = commandContext.getBatchManager().findBatchById(batchId);
    ensureNotNull("Batch with id '%s' cannot be found".formatted(batchId), "batch", batch);

    BatchJobHandler<?> batchJobHandler = commandContext
        .getProcessEngineConfiguration()
        .getBatchHandlers()
        .get(batch.getType());

    boolean done = batchJobHandler.createJobs(batch);

    if (!done) {
      batch.createSeedJob();
    }
    else {
      // create monitor job initially without due date to
      // enable rapid completion of simple batches
      batch.createMonitorJob(false);
    }
  }

  @Override
  public BatchSeedJobConfiguration newConfiguration(String canonicalString) {
    return new BatchSeedJobConfiguration(canonicalString);
  }

  public static class BatchSeedJobConfiguration implements JobHandlerConfiguration {
    protected String batchId;

    public BatchSeedJobConfiguration(String batchId) {
      this.batchId = batchId;
    }

    public String getBatchId() {
      return batchId;
    }

    @Override
    public String toCanonicalString() {
      return batchId;
    }
  }

 }
