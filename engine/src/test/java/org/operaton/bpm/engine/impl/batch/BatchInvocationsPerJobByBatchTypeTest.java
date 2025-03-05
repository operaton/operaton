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
package org.operaton.bpm.engine.impl.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

@ExtendWith(ProcessEngineExtension.class)
public class BatchInvocationsPerJobByBatchTypeTest {

  ManagementService managementService;
  RuntimeService runtimeService;
  HistoryService historyService;
  ProcessEngineConfigurationImpl engineConfiguration;

  @AfterEach
  public void tearDown() {
    int defaultInvocationsPerJob =
        ProcessEngineConfigurationImpl.DEFAULT_INVOCATIONS_PER_BATCH_JOB;

    engineConfiguration
        .setInvocationsPerBatchJobByBatchType(new HashMap<>())
        .setInvocationsPerBatchJob(defaultInvocationsPerJob);

    managementService.createBatchQuery()
        .list()
        .forEach(this::cascadeDeleteBatch);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void shouldFallbackToDefaultConfigurationWhenBatchTypeIsNotConfigured() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery();

    // when
    Batch batchOne = runtimeService.deleteProcessInstancesAsync(processInstanceQuery, "");

    // then
    assertThat(batchOne.getInvocationsPerBatchJob()).isEqualTo(1);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  public void shouldFallbackToGlobalConfigurationWhenBatchTypeIsNotConfigured() {
    // given
    Map<String, Integer> invocationsPerBatchJobByBatchType =
        Collections.singletonMap(Batch.TYPE_PROCESS_INSTANCE_DELETION, 10);

    engineConfiguration
        .setInvocationsPerBatchJobByBatchType(invocationsPerBatchJobByBatchType)
        .setInvocationsPerBatchJob(42);

    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery();

    Batch batchOne =
        runtimeService.deleteProcessInstancesAsync(processInstanceQuery, "");

    HistoricProcessInstanceQuery historicProcessInstanceQuery =
        historyService.createHistoricProcessInstanceQuery();

    // when
    Batch batchTwo =
        historyService.deleteHistoricProcessInstancesAsync(historicProcessInstanceQuery, "");

    // then
    assertThat(batchOne.getInvocationsPerBatchJob()).isEqualTo(10);
    assertThat(batchTwo.getInvocationsPerBatchJob()).isEqualTo(42);
  }

  // helper ////////////////////////////////////////////////////////////////////////////////////////

  protected void cascadeDeleteBatch(Batch batch) {
    managementService.deleteBatch(batch.getId(), true);
  }

}
