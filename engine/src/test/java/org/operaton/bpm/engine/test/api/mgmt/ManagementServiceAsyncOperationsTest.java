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
package org.operaton.bpm.engine.test.api.mgmt;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.AbstractAsyncOperationsTest;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * @author Askar Akhmerov
 */
class ManagementServiceAsyncOperationsTest extends AbstractAsyncOperationsTest {
  protected static final int RETRIES = 5;
  protected static final java.lang.String TEST_PROCESS = "exceptionInJobExecution";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected final Date TEST_DUE_DATE = new Date(1675752840000L);

  protected List<String> processInstanceIds;
  protected List<String> ids;

  boolean tearDownEnsureJobDueDateNotNull;

  @BeforeEach
  void setup() {
    initDefaults(engineRule);
    prepareData();
  }

  protected void prepareData() {
    testRule.deploy("org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml");
    if (processInstanceIds == null) {
      processInstanceIds = new ArrayList<>();
    }
    processInstanceIds.addAll(startTestProcesses(2));
    ids = getAllJobIds();
  }

  @AfterEach
  void tearDown() {
    processInstanceIds = null;
    if(tearDownEnsureJobDueDateNotNull) {
      engineConfiguration.setEnsureJobDueDateNotNull(false);
    }
  }

  @Test
  void testSetJobsRetryAsyncWithJobList() {
    //when
    Batch batch = managementService.setJobRetriesAsync(ids, RETRIES);
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    assertRetries(ids, RETRIES);
    assertHistoricBatchExists(testRule);
  }

  @Test
  void shouldSetInvocationsPerBatchTypeForJobsByJobIds() {
    // given
    engineRule.getProcessEngineConfiguration()
        .getInvocationsPerBatchJobByBatchType()
        .put(Batch.TYPE_SET_JOB_RETRIES, 42);

    //when
    Batch batch = managementService.setJobRetriesAsync(ids, RETRIES);

    // then
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(42);

    // clear
    engineRule.getProcessEngineConfiguration()
        .setInvocationsPerBatchJobByBatchType(new HashMap<>());
  }

  @Test
  void testSetJobsRetryAsyncWithProcessList() {
    //when
    Batch batch = managementService.setJobRetriesAsync(processInstanceIds, (ProcessInstanceQuery) null, RETRIES);
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    assertRetries(ids, RETRIES);
    assertHistoricBatchExists(testRule);
  }

  @Test
  void testSetJobsRetryAsyncWithProcessListInDifferentDeployments() {
    // given a second deployment
    prepareData();
    ProcessDefinitionQuery definitionQuery = engineRule.getRepositoryService().createProcessDefinitionQuery();
    String firstDeploymentId = definitionQuery.processDefinitionVersion(1).singleResult().getDeploymentId();
    String secondDeploymentId = definitionQuery.processDefinitionVersion(2).singleResult().getDeploymentId();

    engineRule.getProcessEngineConfiguration().setInvocationsPerBatchJob(2);

    // when
    Batch batch = managementService.setJobRetriesAsync(processInstanceIds, (ProcessInstanceQuery) null, RETRIES);
    executeSeedJobs(batch, 2);
    // then batch jobs with different deployment ids exist
    List<Job> batchJobs = managementService.createJobQuery().jobDefinitionId(batch.getBatchJobDefinitionId()).list();
    assertThat(batchJobs).hasSize(2);
    assertThat(batchJobs.get(0).getDeploymentId()).isIn(firstDeploymentId, secondDeploymentId);
    assertThat(batchJobs.get(1).getDeploymentId()).isIn(firstDeploymentId, secondDeploymentId);
    assertThat(batchJobs.get(0).getDeploymentId()).isNotEqualTo(batchJobs.get(1).getDeploymentId());

    // when the batch jobs for the first deployment are executed
    assertThat(getJobCountWithUnchangedRetries()).isEqualTo(4L);
    getJobIdsByDeployment(batchJobs, firstDeploymentId).forEach(managementService::executeJob);
    // then the retries for jobs from process instances related to the first deployment should be changed
    assertThat(getJobCountWithUnchangedRetries()).isEqualTo(2L);

    // when the remaining batch jobs are executed
    getJobIdsByDeployment(batchJobs, secondDeploymentId).forEach(managementService::executeJob);
    // then
    assertRetries(ids, RETRIES);
    assertHistoricBatchExists(testRule);
  }

  @Test
  void testSetJobsRetryAsyncWithEmptyJobList() {
    // given
    List<String> jobIds = emptyList();
    // when/then
    assertThatThrownBy(() -> managementService.setJobRetriesAsync(jobIds, RETRIES))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testSetJobsRetryAsyncWithEmptyProcessList() {
    // given
    List<String> jobIds = emptyList();
    // when/then
    assertThatThrownBy(() -> managementService.setJobRetriesAsync(jobIds, (ProcessInstanceQuery) null, RETRIES))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testSetJobsRetryAsyncWithNonExistingJobID() {
    //given
    ids.add("aFake");

    //when
    Batch batch = managementService.setJobRetriesAsync(ids, RETRIES);
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    //then
    assertThat(exceptions).hasSize(1);
    assertRetries(getAllJobIds(), RETRIES);
    assertHistoricBatchExists(testRule);
  }

  @Test
  void testSetJobsRetryAsyncWithNonExistingProcessID() {
    //given
    processInstanceIds.add("aFake");

    //when
    Batch batch = managementService.setJobRetriesAsync(processInstanceIds, (ProcessInstanceQuery) null, RETRIES);
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    //then
    assertThat(exceptions).isEmpty();
    assertRetries(getAllJobIds(), RETRIES);
    assertHistoricBatchExists(testRule);
  }

  @Test
  void testSetJobsRetryAsyncWithJobQueryAndList() {
    //given
    List<String> extraPi = startTestProcesses(1);
    JobQuery query = managementService.createJobQuery().processInstanceId(extraPi.get(0));

    //when
    Batch batch = managementService.setJobRetriesAsync(ids, query, RETRIES);
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    assertRetries(getAllJobIds(), RETRIES);
    assertHistoricBatchExists(testRule);
  }

  @Test
  void testSetJobsRetryAsyncWithProcessQueryAndList() {
    //given
    List<String> extraPi = startTestProcesses(1);
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processInstanceId(extraPi.get(0));

    //when
    Batch batch = managementService.setJobRetriesAsync(processInstanceIds, query, RETRIES);
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    assertRetries(getAllJobIds(), RETRIES);
    assertHistoricBatchExists(testRule);
  }

  @Test
  void testSetJobsRetryAsyncWithJobQuery() {
    //given
    JobQuery query = managementService.createJobQuery();

    //when
    Batch batch = managementService.setJobRetriesAsync(query, RETRIES);
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    assertRetries(ids, RETRIES);
    assertHistoricBatchExists(testRule);
  }

  @Test
  void testSetJobsRetryAsyncWithProcessQuery() {
    //given
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    //when
    Batch batch = managementService.setJobRetriesAsync(null, query, RETRIES);
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    assertRetries(ids, RETRIES);
    assertHistoricBatchExists(testRule);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void testSetJobsRetryAsyncWithHistoryProcessQuery() {
    //given
    HistoricProcessInstanceQuery historicProcessInstanceQuery =
        historyService.createHistoricProcessInstanceQuery();

    //when
    Batch batch = managementService.setJobRetriesAsync(null, null,
        historicProcessInstanceQuery, RETRIES);
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    assertRetries(ids, RETRIES);
    assertHistoricBatchExists(testRule);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void testSetJobsRetryAsyncWithRuntimeAndHistoryProcessQuery() {
    //given
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
        .processInstanceId(processInstanceIds.get(0));

    HistoricProcessInstanceQuery historicProcessInstanceQuery =
        historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(processInstanceIds.get(1));

    //when
    Batch batch = managementService.setJobRetriesAsync(null, query,
        historicProcessInstanceQuery, RETRIES);
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    assertRetries(ids, RETRIES);
    assertHistoricBatchExists(testRule);
  }

  @Test
  void testSetJobsRetryAsyncWithEmptyJobQuery() {
    //given
    JobQuery query = managementService.createJobQuery().suspended();

    // when/then
    assertThatThrownBy(() -> managementService.setJobRetriesAsync(query, RETRIES))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testSetJobsRetryAsyncWithEmptyProcessQuery() {
    //given
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().suspended();

    // when/then
    assertThatThrownBy(() -> managementService.setJobRetriesAsync(null, query, RETRIES))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testSetJobsRetryAsyncWithNonExistingIDAsJobQuery() {
    //given
    JobQuery query = managementService.createJobQuery().jobId("aFake");

    // when/then
    assertThatThrownBy(() -> managementService.setJobRetriesAsync(query, RETRIES))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testSetJobsRetryAsyncWithNonExistingIDAsProcessQuery() {
    //given
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processInstanceId("aFake");

    // when/then
    assertThatThrownBy(() -> managementService.setJobRetriesAsync(null, query, RETRIES))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testSetJobsRetryAsyncWithNullJobList() {

    // when/then
    assertThatThrownBy(() -> managementService.setJobRetriesAsync((List<String>) null, RETRIES))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testSetJobsRetryAsyncWithNullJobQuery() {
    // when/then
    assertThatThrownBy(() -> managementService.setJobRetriesAsync((JobQuery) null, RETRIES))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testSetJobsRetryAsyncWithNullProcessQuery() {
    // when/then
    assertThatThrownBy(() -> managementService.setJobRetriesAsync(null, (ProcessInstanceQuery) null, RETRIES))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testSetJobsRetryAsyncWithNegativeRetries() {
    //given
    JobQuery query = managementService.createJobQuery();

    // when/then
    assertThatThrownBy(() -> managementService.setJobRetriesAsync(query, -1))
      .isInstanceOf(ProcessEngineException.class);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldSetJobDueDateOnJobRetryAsyncByJobQuery() {
    //given
    JobQuery query = managementService.createJobQuery();

    //when
    Batch batch = managementService.setJobRetriesByJobsAsync(RETRIES)
        .jobQuery(query)
        .dueDate(TEST_DUE_DATE).executeAsync();
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    for (String id : ids) {
      Job job = managementService.createJobQuery().jobId(id).singleResult();
      assertThat(job.getRetries()).isEqualTo(RETRIES);
      assertThat(job.getDuedate()).isCloseTo(TEST_DUE_DATE, 1000);
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldSetJobDueDateOnJobRetryAsyncByProcessInstanceIds() {
    //given

    //when
    Batch batch = managementService.setJobRetriesByProcessAsync(RETRIES)
        .processInstanceIds(processInstanceIds)
        .dueDate(TEST_DUE_DATE).executeAsync();
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    for (String id : processInstanceIds) {
      Job job = managementService.createJobQuery().processInstanceId(id).singleResult();
      assertThat(job.getRetries()).isEqualTo(RETRIES);
      assertThat(job.getDuedate()).isCloseTo(TEST_DUE_DATE, 1000);
    }
  }

  @Test
  void shouldSetJobDueDateOnJobRetryAsyncByProcessInstanceQuery() {
    //given
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    //when
    Batch batch = managementService.setJobRetriesByProcessAsync(RETRIES).processInstanceQuery(query).dueDate(TEST_DUE_DATE).executeAsync();
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    for (String id : ids) {
      Job jobResult = managementService.createJobQuery().jobId(id).singleResult();
      assertThat(jobResult.getRetries()).isEqualTo(RETRIES);
      assertThat(jobResult.getDuedate()).isCloseTo(TEST_DUE_DATE, 1000);
    }
  }


  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldSetJobDueDateOnJobRetryAsyncByHistoricProcessInstanceQuery() {
    //given
    HistoricProcessInstanceQuery historicProcessInstanceQuery =
        historyService.createHistoricProcessInstanceQuery();

    //when
    Batch batch = managementService.setJobRetriesByProcessAsync(RETRIES)
        .historicProcessInstanceQuery(historicProcessInstanceQuery)
        .dueDate(TEST_DUE_DATE).executeAsync();
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    for (String id : ids) {
      Job jobResult = managementService.createJobQuery().jobId(id).singleResult();
      assertThat(jobResult.getRetries()).isEqualTo(RETRIES);
      assertThat(jobResult.getDuedate()).isCloseTo(TEST_DUE_DATE, 1000);
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldSetDueDateNull() {
    // given
    engineConfiguration.setEnsureJobDueDateNotNull(false);
    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // assume
    List<Job> job = managementService.createJobQuery().list();
    assertThat(job.get(0).getDuedate()).isNotNull();
    assertThat(job.get(1).getDuedate()).isNotNull();

    // when
    Batch batch = managementService.setJobRetriesByProcessAsync(RETRIES)
        .historicProcessInstanceQuery(historicProcessInstanceQuery)
        .dueDate(null)
        .executeAsync();
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    for (String id : ids) {
      Job jobResult = managementService.createJobQuery().jobId(id).singleResult();
      assertThat(jobResult.getRetries()).isEqualTo(RETRIES);
      assertThat(jobResult.getDuedate()).isNull();
    }
  }

  @Test
  void shouldSetJobDueDateOnJobRetryAsyncByJobIds() {
    //given

    //when
    Batch batch = managementService.setJobRetriesByJobsAsync(RETRIES).jobIds(ids).dueDate(TEST_DUE_DATE).executeAsync();
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    for (String id : ids) {
      Job jobResult = managementService.createJobQuery().jobId(id).singleResult();
      assertThat(jobResult.getRetries()).isEqualTo(RETRIES);
      assertThat(jobResult.getDuedate()).isCloseTo(TEST_DUE_DATE, 1000);
    }
  }

  @Test
  void shouldThrowErrorOnEmptySetRetryByJobsBuilderConfig() {
    // given
    var setJobRetriesByJobsAsyncBuilder = managementService.setJobRetriesByJobsAsync(RETRIES);

    // when/then
    assertThatThrownBy(setJobRetriesByJobsAsyncBuilder::executeAsync)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("050")
      .hasMessageContaining("You must specify at least one of jobIds or jobQuery.");
  }

  @Test
  void shouldThrowErrorOnEmptySetRetryByProcessBuilderConfig() {
    // given
    var setJobRetriesByProcessAsyncBuilder = managementService.setJobRetriesByProcessAsync(RETRIES);

    // when/then
    assertThatThrownBy(setJobRetriesByProcessAsyncBuilder::executeAsync)
    .isInstanceOf(ProcessEngineException.class)
    .hasMessageContaining("051")
    .hasMessageContaining("You must specify at least one of or one of processInstanceIds, processInstanceQuery, or historicProcessInstanceQuery.");
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldSetInvocationsPerBatchTypeForJobsByProcessInstanceIds() {
    // given
    engineRule.getProcessEngineConfiguration()
        .getInvocationsPerBatchJobByBatchType()
        .put(Batch.TYPE_SET_JOB_RETRIES, 42);

    HistoricProcessInstanceQuery historicProcessInstanceQuery =
        historyService.createHistoricProcessInstanceQuery();

    //when
    Batch batch = managementService.setJobRetriesAsync(null, null,
        historicProcessInstanceQuery, RETRIES);

    // then
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(42);

    // clear
    engineRule.getProcessEngineConfiguration()
        .setInvocationsPerBatchJobByBatchType(new HashMap<>());
  }

  protected List<String> getAllJobIds() {
    return getAllJobs().stream().map(Job::getId).collect(Collectors.toList());
  }

  protected List<Job> getAllJobs() {
    return managementService.createJobQuery().list().stream()
        .filter(j -> j.getProcessInstanceId() != null)
        .toList();
  }

  @Override
  protected List<String> startTestProcesses(int numberOfProcesses) {
    ArrayList<String> processIds = new ArrayList<>();

    for (int i = 0; i < numberOfProcesses; i++) {
      processIds.add(runtimeService.startProcessInstanceByKey(TEST_PROCESS).getProcessInstanceId());
    }

    return processIds;
  }

  protected void assertRetries(List<String> allJobIds, int i) {
    for (String id : allJobIds) {
      assertThat(managementService.createJobQuery().jobId(id).singleResult().getRetries()).isEqualTo(i);
    }
  }

  protected long getJobCountWithUnchangedRetries() {
    return getAllJobs().stream().filter(j -> j.getRetries() != RETRIES).count();
  }
}
