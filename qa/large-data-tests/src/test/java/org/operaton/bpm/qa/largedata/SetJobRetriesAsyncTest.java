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
package org.operaton.bpm.qa.largedata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.qa.largedata.util.BatchModificationJobHelper;
import org.operaton.bpm.qa.largedata.util.EngineDataGenerator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SetJobRetriesAsyncTest {

  protected static final String DATA_PREFIX = SetJobRetriesAsyncTest.class.getSimpleName();
  protected static final int GENERATE_PROCESS_INSTANCES_COUNT = 3000;

  @RegisterExtension
  static ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder().build();
  protected ProcessEngine processEngine;
  protected BatchModificationJobHelper helper = new BatchModificationJobHelper(() -> processEngine);

  protected EngineDataGenerator generator;
  protected RuntimeService runtimeService;
  protected ManagementService managementService;

  @BeforeEach
  void setUp() {
    // generate data
    generator = new EngineDataGenerator(processEngine, GENERATE_PROCESS_INSTANCES_COUNT, DATA_PREFIX);
    generator.deployDefinitions();
    generator.generateAsyncTaskProcessInstanceData();
  }

  @AfterEach
  void tearDown() {
    helper.removeAllRunningAndHistoricBatches();
  }

  /* See https://jira.camunda.com/browse/CAM-12852 for more details */
  @Test
  void shouldModifyJobRetriesAsync() {
    // given
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey(generator.getAsyncTaskProcessKey())
        .list();
    List<String> processInstanceIds = processInstances.stream()
        .map(ProcessInstance::getId)
        .toList();
    int newJobRetriesNumber = 10;
    Batch jobRetriesBatch = managementService
        .setJobRetriesAsync(processInstanceIds, (ProcessInstanceQuery) null, newJobRetriesNumber);

    // when
    helper.completeBatch(jobRetriesBatch);

    // then
    List<Job> jobs = managementService.createJobQuery()
        .processDefinitionKey(generator.getAsyncTaskProcessKey())
        .list();
    Set<Integer> retries = jobs.stream().map(Job::getRetries).collect(Collectors.toSet());
    // all the jobs have been updated to the same retry value and that value is 10
    assertThat(retries).hasSize(1).contains(10);
  }

}
