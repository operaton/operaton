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
package org.operaton.bpm.engine.test.api.externaltask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.ExternalTaskQuery;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.AbstractAsyncOperationsTest;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SetExternalTasksRetriesTest extends AbstractAsyncOperationsTest {

  protected static final int RETRIES = 5;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  private static final String PROCESS_DEFINITION_KEY = "oneExternalTaskProcess";
  private static final String PROCESS_DEFINITION_KEY_2 = "twoExternalTaskWithPriorityProcess";

  protected ExternalTaskService externalTaskService;

  protected List<String> processInstanceIds;

  @BeforeEach
  void setup() {
    initDefaults(engineRule);
    externalTaskService = engineRule.getExternalTaskService();
    deployTestProcesses();
  }

  protected void deployTestProcesses() {
    org.operaton.bpm.engine.repository.Deployment deployment = engineRule.getRepositoryService().createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
      .addClasspathResource("org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityExpression.bpmn20.xml")
      .deploy();

    engineRule.manageDeployment(deployment);

    RuntimeService runtimeService = engineRule.getRuntimeService();
    if (processInstanceIds == null) {
      processInstanceIds = new ArrayList<>();
    }
    for (int i = 0; i < 4; i++) {
      processInstanceIds.add(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, i + "").getId());
    }
    processInstanceIds.add(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY_2).getId());
  }

  @AfterEach
  void tearDown() {
    processInstanceIds = null;
  }

  @Test
  void shouldSetExternalTaskRetriesSync() {

    List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery().list();
    ArrayList<String> externalTaskIds = new ArrayList<>();
    for (ExternalTask task : externalTasks) {
      externalTaskIds.add(task.getId());
    }
    // when
    externalTaskService.setRetries(externalTaskIds, 10);

    // then
    externalTasks = externalTaskService.createExternalTaskQuery().list();
    for (ExternalTask task : externalTasks) {
      assertThat((int) task.getRetries()).isEqualTo(10);
    }
  }

  @Test
  void shouldFailForNonExistingExternalTaskIdSync() {

    List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery().list();
    ArrayList<String> externalTaskIds = new ArrayList<>();
    for (ExternalTask task : externalTasks) {
      externalTaskIds.add(task.getId());
    }

    externalTaskIds.add("nonExistingExternalTaskId");

    assertThatThrownBy(() -> externalTaskService.setRetries(externalTaskIds, 10))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Cannot find external task with id nonExistingExternalTaskId");

  }

  @Test
  void shouldFailForNullExternalTaskIdSync() {

    List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery().list();
    ArrayList<String> externalTaskIds = new ArrayList<>();
    for (ExternalTask task : externalTasks) {
      externalTaskIds.add(task.getId());
    }

    externalTaskIds.add(null);

    assertThatThrownBy(() -> externalTaskService.setRetries(externalTaskIds, 10))
        .isInstanceOf(BadUserRequestException.class)
        .hasMessageContaining("External task id cannot be null");

  }

  @Test
  void shouldFailForNullExternalTaskIdsSync() {
    assertThatThrownBy(() -> externalTaskService.setRetries((List<String>) null, 10))
        .isInstanceOf(BadUserRequestException.class)
        .hasMessageContaining("externalTaskIds is empty");

  }

  @Test
  void shouldFailForNonExistingExternalTaskIdAsync() {

    List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery().list();
    ArrayList<String> externalTaskIds = new ArrayList<>();
    for (ExternalTask task : externalTasks) {
      externalTaskIds.add(task.getId());
    }

    externalTaskIds.add("nonExistingExternalTaskId");
    Batch batch = externalTaskService.setRetriesAsync(externalTaskIds, null, 10);

    assertThatThrownBy(() -> executeSeedAndBatchJobs(batch))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Cannot find external task with id nonExistingExternalTaskId");

  }

  @Test
  void shouldFailForNullExternalTaskIdAsync() {

    List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery().list();
    ArrayList<String> externalTaskIds = new ArrayList<>();
    for (ExternalTask task : externalTasks) {
      externalTaskIds.add(task.getId());
    }

    externalTaskIds.add(null);

    assertThatThrownBy(() -> externalTaskService.setRetriesAsync(externalTaskIds, null, 10))
        .isInstanceOf(BadUserRequestException.class)
        .hasMessageContaining("External task id cannot be null");

  }

  @Test
  void shouldFailForNullExternalTaskIdsAsync() {
    assertThatThrownBy(() -> externalTaskService.setRetriesAsync((List<String>) null, null, 10))
        .isInstanceOf(BadUserRequestException.class)
        .hasMessageContaining("externalTaskIds is empty");
  }

  @Test
  void shouldFailForNegativeRetriesSync() {

    List<String> externalTaskIds = List.of("externalTaskId");

    assertThatThrownBy(() -> externalTaskService.setRetries(externalTaskIds, -10))
        .isInstanceOf(BadUserRequestException.class)
        .hasMessageContaining("The number of retries cannot be negative");

  }

  @Test
  void shouldFailForNegativeRetriesAsync() {

    List<String> externalTaskIds = List.of("externalTaskId");

    Batch batch = externalTaskService.setRetriesAsync(externalTaskIds, null, -10);

    assertThatThrownBy(() -> executeSeedAndBatchJobs(batch))
        .isInstanceOf(BadUserRequestException.class)
        .hasMessageContaining("The number of retries cannot be negative");

  }

  @Test
  void shouldSetExternalTaskRetriesWithQueryAsync() {

    ExternalTaskQuery externalTaskQuery = engineRule.getExternalTaskService().createExternalTaskQuery();

    // when
    Batch batch = externalTaskService.setRetriesAsync(null, externalTaskQuery, RETRIES);

    // then
    executeSeedAndBatchJobs(batch);

    for (ExternalTask task : externalTaskQuery.list()) {
      assertThat((int) task.getRetries()).isEqualTo(RETRIES);
    }
  }

  @Test
  void shouldSetExternalTaskRetriesWithListAsync() {

    List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery().list();
    ArrayList<String> externalTaskIds = new ArrayList<>();
    for (ExternalTask task : externalTasks) {
      externalTaskIds.add(task.getId());
    }
    // when
    Batch batch = externalTaskService.setRetriesAsync(externalTaskIds, null, RETRIES);

    // then
    executeSeedAndBatchJobs(batch);

    externalTasks = externalTaskService.createExternalTaskQuery().list();
    for (ExternalTask task : externalTasks) {
      assertThat((int) task.getRetries()).isEqualTo(RETRIES);
    }
  }

  @Test
  void shouldSetExternalTaskRetriesWithListAsyncInDifferentDeployments() {
    // given multiple deployments
    deployTestProcesses();
    ProcessDefinitionQuery definitionQuery = engineRule.getRepositoryService().createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY);
    String firstDeploymentId = definitionQuery.processDefinitionVersion(1).singleResult().getDeploymentId();
    String secondDeploymentId = definitionQuery.processDefinitionVersion(2).singleResult().getDeploymentId();

    List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery().list();
    ArrayList<String> externalTaskIds = new ArrayList<>();
    for (ExternalTask task : externalTasks) {
      externalTaskIds.add(task.getId());
    }

    engineRule.getProcessEngineConfiguration().setInvocationsPerBatchJob(6);

    // when
    Batch batch = externalTaskService.setRetriesAsync(externalTaskIds, null, RETRIES);
    executeSeedJobs(batch, 2);
    // then batch jobs with different deployment ids exist
    List<Job> batchJobs = managementService.createJobQuery().jobDefinitionId(batch.getBatchJobDefinitionId()).list();
    assertThat(batchJobs).hasSize(2);
    assertThat(batchJobs.get(0).getDeploymentId()).isIn(firstDeploymentId, secondDeploymentId);
    assertThat(batchJobs.get(1).getDeploymentId()).isIn(firstDeploymentId, secondDeploymentId);
    assertThat(batchJobs.get(0).getDeploymentId()).isNotEqualTo(batchJobs.get(1).getDeploymentId());

    // when the batch jobs for the first deployment are executed
    assertThat(getTaskCountWithUnchangedRetries()).isEqualTo(12L);
    getJobIdsByDeployment(batchJobs, firstDeploymentId).forEach(managementService::executeJob);
    // then the retries for jobs from process instances related to the first deployment should be changed
    assertThat(getTaskCountWithUnchangedRetries()).isEqualTo(6L);

    // when the remaining batch jobs are executed
    getJobIdsByDeployment(batchJobs, secondDeploymentId).forEach(managementService::executeJob);
    // then
    externalTasks = externalTaskService.createExternalTaskQuery().list();
    for (ExternalTask task : externalTasks) {
      assertThat((int) task.getRetries()).isEqualTo(RETRIES);
    }
  }

  @Test
  void shouldSetExternalTaskRetriesWithListAndQueryAsync() {

    ExternalTaskQuery externalTaskQuery = externalTaskService.createExternalTaskQuery();
    List<ExternalTask> externalTasks = externalTaskQuery.list();

    ArrayList<String> externalTaskIds = new ArrayList<>();
    for (ExternalTask task : externalTasks) {
      externalTaskIds.add(task.getId());
    }
    // when
    Batch batch = externalTaskService.setRetriesAsync(externalTaskIds, externalTaskQuery, RETRIES);

    // then
    executeSeedAndBatchJobs(batch);

    externalTasks = externalTaskService.createExternalTaskQuery().list();
    for (ExternalTask task : externalTasks) {
      assertThat((int) task.getRetries()).isEqualTo(RETRIES);
    }
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldSetExternalTaskRetriesWithLargeList() {
    // given
    engineRule.getProcessEngineConfiguration().setBatchJobsPerSeed(1010);
    List<String> processIds = startProcessInstance(PROCESS_DEFINITION_KEY, 1100);

    HistoricProcessInstanceQuery processInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    Batch batch = externalTaskService.updateRetries()
        .historicProcessInstanceQuery(processInstanceQuery)
        .setAsync(3);

    executeSeedJobs(batch, 2);
    executeBatchJobs(batch);

    // then no error is thrown
    assertHistoricBatchExists();

    // cleanup
    if (!testHelper.isHistoryLevelNone()) {
      batch = historyService.deleteHistoricProcessInstancesAsync(processIds, null);
      executeSeedJobs(batch, 2);
      executeBatchJobs(batch);
    }
  }

  @Test
  void shouldSetExternalTaskRetriesWithDifferentListAndQueryAsync() {
    // given
    ExternalTaskQuery externalTaskQuery = externalTaskService.createExternalTaskQuery().processInstanceId(processInstanceIds.get(0));
    List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery().processInstanceId(processInstanceIds.get(processInstanceIds.size()-1)).list();
    ArrayList<String> externalTaskIds = new ArrayList<>();
    for (ExternalTask task : externalTasks) {
      externalTaskIds.add(task.getId());
    }

    // when
    Batch batch = externalTaskService.setRetriesAsync(externalTaskIds, externalTaskQuery, 8);
    executeSeedAndBatchJobs(batch);

    // then
    ExternalTask task = externalTaskService.createExternalTaskQuery().processInstanceId(processInstanceIds.get(0)).singleResult();
    assertThat((int) task.getRetries()).isEqualTo(8);
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().processInstanceId(processInstanceIds.get(processInstanceIds.size()-1)).list();
    for (ExternalTask t : tasks) {
      assertThat((int) t.getRetries()).isEqualTo(8);
    }
  }

  @Test
  void shouldUpdateRetriesByExternalTaskIds() {
    // given
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    List<String> externalTaskIds = List.of(
        tasks.get(0).getId(),
        tasks.get(1).getId(),
        tasks.get(2).getId(),
        tasks.get(3).getId(),
        tasks.get(4).getId(),
        tasks.get(5).getId());

    // when
    Batch batch = externalTaskService.updateRetries().externalTaskIds(externalTaskIds).setAsync(RETRIES);
    executeSeedAndBatchJobs(batch);

    // then
    tasks = externalTaskService.createExternalTaskQuery().list();
    assertThat(tasks).hasSize(6);

    for (ExternalTask task : tasks) {
      assertThat((int) task.getRetries()).isEqualTo(RETRIES);
    }
  }

  @Test
  void shouldUpdateRetriesByExternalTaskIdArray() {
    // given
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    List<String> externalTaskIds = List.of(
        tasks.get(0).getId(),
        tasks.get(1).getId(),
        tasks.get(2).getId(),
        tasks.get(3).getId(),
        tasks.get(4).getId(),
        tasks.get(5).getId());

    // when
    Batch batch = externalTaskService.updateRetries().externalTaskIds(externalTaskIds.toArray(new String[0])).setAsync(RETRIES);
    executeSeedAndBatchJobs(batch);

    // then
    tasks = externalTaskService.createExternalTaskQuery().list();
    assertThat(tasks).hasSize(6);

    for (ExternalTask task : tasks) {
      assertThat((int) task.getRetries()).isEqualTo(RETRIES);
    }
  }

  @Test
  void shouldUpdateRetriesByProcessInstanceIds() {
    // when
    Batch batch = externalTaskService.updateRetries().processInstanceIds(processInstanceIds).setAsync(RETRIES);
    executeSeedAndBatchJobs(batch);

    // then
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    assertThat(tasks).hasSize(6);

    for (ExternalTask task : tasks) {
      assertThat((int) task.getRetries()).isEqualTo(RETRIES);
    }
  }

  @Test
  void shouldUpdateRetriesByProcessInstanceIdArray() {
    // given

    // when
    Batch batch = externalTaskService.updateRetries().processInstanceIds(processInstanceIds.toArray(new String[0])).setAsync(RETRIES);
    executeSeedAndBatchJobs(batch);

    // then
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    assertThat(tasks).hasSize(6);

    for (ExternalTask task : tasks) {
      assertThat((int) task.getRetries()).isEqualTo(RETRIES);
    }
  }

  @Test
  void shouldUpdateRetriesByExternalTaskQuery() {
    // given
    ExternalTaskQuery query = externalTaskService.createExternalTaskQuery();

    // when
    Batch batch = externalTaskService.updateRetries().externalTaskQuery(query).setAsync(RETRIES);
    executeSeedAndBatchJobs(batch);

    // then
    List<ExternalTask> tasks = query.list();
    assertThat(tasks).hasSize(6);

    for (ExternalTask task : tasks) {
      assertThat((int) task.getRetries()).isEqualTo(RETRIES);
    }
  }

  @Test
  void shouldUpdateRetriesByProcessInstanceQuery() {
    // given
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery();

    // when
    Batch batch = externalTaskService.updateRetries().processInstanceQuery(processInstanceQuery).setAsync(RETRIES);
    executeSeedAndBatchJobs(batch);

    // then
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    assertThat(tasks).hasSize(6);

    for (ExternalTask task : tasks) {
      assertThat((int) task.getRetries()).isEqualTo(RETRIES);
    }
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void shouldUpdateRetriesByHistoricProcessInstanceQuery() {
    // given
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // when
    Batch batch = externalTaskService.updateRetries().historicProcessInstanceQuery(query).setAsync(RETRIES);
    executeSeedAndBatchJobs(batch);

    // then
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    assertThat(tasks).hasSize(6);

    for (ExternalTask task : tasks) {
      assertThat((int) task.getRetries()).isEqualTo(RETRIES);
    }
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void shouldUpdateRetriesByAllParameters() {
    // given
    ExternalTask externalTask = externalTaskService
        .createExternalTaskQuery()
        .processInstanceId(processInstanceIds.get(0))
        .singleResult();

    ExternalTaskQuery externalTaskQuery = externalTaskService
        .createExternalTaskQuery()
        .processInstanceId(processInstanceIds.get(1));

    ProcessInstanceQuery processInstanceQuery = runtimeService
        .createProcessInstanceQuery()
        .processInstanceId(processInstanceIds.get(2));


    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceId(processInstanceIds.get(3));

    // when
    Batch batch = externalTaskService.updateRetries()
      .externalTaskIds(externalTask.getId())
      .externalTaskQuery(externalTaskQuery)
      .processInstanceQuery(processInstanceQuery)
      .historicProcessInstanceQuery(historicProcessInstanceQuery)
      .processInstanceIds(processInstanceIds.get(4))
      .setAsync(RETRIES);
    executeSeedAndBatchJobs(batch);

    // then
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    assertThat(tasks).hasSize(6);

    for (ExternalTask task : tasks) {
      assertThat(task.getRetries()).isEqualTo(Integer.valueOf(RETRIES));
    }
  }

  protected void executeSeedAndBatchJobs(Batch batch) {
    completeSeedJobs(batch);

    for (Job pending : managementService.createJobQuery().jobDefinitionId(batch.getBatchJobDefinitionId()).list()) {
      managementService.executeJob(pending.getId());
    }
  }

  protected void assertHistoricBatchExists() {
    if (testHelper.isHistoryLevelFull()) {
      assertThat(historyService.createHistoricBatchQuery().count()).isOne();
    }
  }

  protected void startTestProcesses() {
    RuntimeService runtimeService = engineRule.getRuntimeService();
    for (int i = 4; i < 1000; i++) {
      processInstanceIds.add(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, i + "").getId());
    }

  }

  protected List<String> startProcessInstance(String key, int instances) {
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < instances; i++) {
      ids.add(runtimeService.startProcessInstanceByKey(key, String.valueOf(i)).getId());
    }
    processInstanceIds.addAll(ids);
    return ids;
  }

  @Test
  void shouldSetInvocationsPerBatchType() {
    // given
    engineRule.getProcessEngineConfiguration()
        .getInvocationsPerBatchJobByBatchType()
        .put(Batch.TYPE_SET_EXTERNAL_TASK_RETRIES, 42);

    ExternalTaskQuery externalTaskQuery = engineRule.getExternalTaskService()
        .createExternalTaskQuery();

    // when
    Batch batch = externalTaskService.setRetriesAsync(null, externalTaskQuery, RETRIES);

    // then
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(42);

    // clear
    engineRule.getProcessEngineConfiguration()
        .setInvocationsPerBatchJobByBatchType(new HashMap<>());
  }

  protected long getTaskCountWithUnchangedRetries() {
    return externalTaskService.createExternalTaskQuery().list().stream().filter(et -> !Integer.valueOf(RETRIES).equals(et.getRetries())).count();
  }
}
