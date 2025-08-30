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
package org.operaton.bpm.engine.test.junit5.batch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl.DEFAULT_INVOCATIONS_PER_BATCH_JOB;

public class BatchExtension implements TestWatcher, BeforeTestExecutionCallback, AfterTestExecutionCallback {

  public static final String SEED_JOB = "seed-job";
  public static final String MONITOR_JOB = "monitor-job";
  public static final String EXECUTION_JOBS = "execution-job";

  protected ProcessEngineExtension engineRule;
  protected ProcessEngineTestExtension engineTestRule;

  public BatchExtension(ProcessEngineExtension engineRule, ProcessEngineTestExtension engineTestRule) {
    this.engineRule = engineRule;
    this.engineTestRule = engineTestRule;
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) {
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    engineRule.getProcessEngineConfiguration()
        .setInvocationsPerBatchJob(DEFAULT_INVOCATIONS_PER_BATCH_JOB);
    ClockUtil.reset();
    clearDatabase();
  }

  protected List<String> batchIds = new ArrayList<>();

  public void clearDatabase() {
    if (!batchIds.isEmpty()) {
      for (String batchId : batchIds) {
        HistoricBatch historicBatch = engineRule.getHistoryService().createHistoricBatchQuery()
            .batchId(batchId)
            .singleResult();
        if (historicBatch != null) {
          engineRule.getHistoryService().deleteHistoricBatch(historicBatch.getId());
        }

        Batch batch = engineRule.getManagementService().createBatchQuery()
            .batchId(batchId)
            .singleResult();
        if (batch != null) {
          engineRule.getManagementService().deleteBatch(batchId, true);
        }
      }
    }
  }

  public Map<String, List<Job>> syncExec(Batch batch) {
    return syncExec(batch, true);
  }

  public Map<String, List<Job>> syncExec(Batch batch, boolean isClear) {
    Map<String, List<Job>> processedJobs = new HashMap<>();
    List<Job> processedSeedJobs = new ArrayList<>();
    if (isClear) {
      batchIds.add(batch.getId());
    }

    processedSeedJobs.addAll(executeSeedJobs(batch));
    processedJobs.put(SEED_JOB, processedSeedJobs);

    List<Job> processedExecutionJobs = new ArrayList<>();
    List<Job> jobs = getExecutionJobs(batch);
    while (!jobs.isEmpty()) {
      for (Job job : jobs) {
        engineRule.getManagementService().executeJob(job.getId());
        processedExecutionJobs.add(job);
      }
      jobs = getExecutionJobs(batch);
    }
    processedJobs.put(EXECUTION_JOBS, processedExecutionJobs);

    List<Job> processedMonitorJobs = new ArrayList<>();
    Job monitorJob = getJobForDefinition(batch.getMonitorJobDefinitionId());
    engineRule.getManagementService().executeJob(monitorJob.getId());
    processedMonitorJobs.add(monitorJob);
    processedJobs.put(MONITOR_JOB, processedMonitorJobs);

    return processedJobs;
  }

  public List<Job> executeSeedJobs(Batch batch) {
    return executeSeedJobs(batch, false);
  }

  public List<Job> executeSeedJobs(Batch batch, boolean cleanUp) {
    List<Job> processedJobs = new ArrayList<>();
    if (cleanUp) {
      batchIds.add(batch.getId());
    }
    while (getSeedJob(batch) != null) {
      Job seedJob = getSeedJob(batch);
      engineRule.getManagementService().executeJob(seedJob.getId());
      processedJobs.add(seedJob);
    }
    return processedJobs;
  }

  public Job getSeedJob(Batch batch) {
    return getJobForDefinition(batch.getSeedJobDefinitionId());
  }

  protected Job getJobForDefinition(String definitionId) {
    return engineRule.getManagementService().createJobQuery()
        .jobDefinitionId(definitionId)
        .singleResult();
  }

  public List<Job> getExecutionJobs(Batch batch) {
    return engineRule.getManagementService().createJobQuery()
        .jobDefinitionId(batch.getBatchJobDefinitionId())
        .list();
  }

}
