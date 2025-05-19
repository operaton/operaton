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
package org.operaton.bpm.engine.test.api.history;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.AbstractAsyncOperationsTest;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * @author Askar Akhmerov
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
class HistoryServiceAsyncOperationsTest extends AbstractAsyncOperationsTest {

  protected static final String TEST_REASON = "test reason";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected TaskService taskService;
  protected List<String> historicProcessInstances;

  @BeforeEach
  void setup() {
    initDefaults(engineRule);
    taskService = engineRule.getTaskService();

    prepareData();
  }

  protected void prepareData() {
    testRule.deploy("org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml");
    startTestProcesses(2);

    for (Task activeTask : taskService.createTaskQuery().list()) {
      taskService.complete(activeTask.getId());
    }

    historicProcessInstances = new ArrayList<>();
    for (HistoricProcessInstance pi : historyService.createHistoricProcessInstanceQuery().list()) {
      historicProcessInstances.add(pi.getId());
    }
  }

  @Test
  void testDeleteHistoryProcessInstancesAsyncWithList() {
    //when
    Batch batch = historyService.deleteHistoricProcessInstancesAsync(historicProcessInstances, TEST_REASON);

    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    assertNoHistoryForTasks();
    assertHistoricBatchExists(testRule);
    assertAllHistoricProcessInstancesAreDeleted();
  }

  @Test
  void testDeleteHistoryProcessInstancesAsyncWithListForDeletedDeployment() {
    // given a second deployment
    prepareData();
    ProcessDefinitionQuery definitionQuery = engineRule.getRepositoryService().createProcessDefinitionQuery();
    String firstDeploymentId = definitionQuery.processDefinitionVersion(1).singleResult().getDeploymentId();
    String secondDeploymentId = definitionQuery.processDefinitionVersion(2).singleResult().getDeploymentId();
    engineRule.getRepositoryService().deleteDeployment(secondDeploymentId);

    engineRule.getProcessEngineConfiguration().setInvocationsPerBatchJob(2);

    // when
    Batch batch = historyService.deleteHistoricProcessInstancesAsync(historicProcessInstances, TEST_REASON);
    // then a seed job with the lowest deployment id exist
    Job seedJob = getSeedJob(batch);
    assertThat(seedJob.getDeploymentId()).isEqualTo(firstDeploymentId);
    // when
    executeSeedJob(batch);
    // then
    seedJob = getSeedJob(batch);
    assertThat(seedJob.getDeploymentId()).isEqualTo(firstDeploymentId);
    // when
    executeSeedJob(batch);
    // then batch jobs with different deployment ids exist
    JobQuery batchJobQuery = managementService.createJobQuery().jobDefinitionId(batch.getBatchJobDefinitionId());
    List<Job> batchJobs = batchJobQuery.list();
    assertThat(batchJobs).hasSize(2);
    batchJobs.stream().forEach(job -> assertThat(job.getDeploymentId())
        .satisfiesAnyOf(
            arg -> assertThat(arg).isEqualTo(firstDeploymentId),
            arg -> assertThat(arg).isNull()
        ));
    assertThat(batchJobs.get(0).getDeploymentId()).isNotEqualTo(batchJobs.get(1).getDeploymentId());
    assertThat(historicProcessInstances).hasSize(4);
    assertThat(getHistoricProcessInstanceCountByDeploymentId(firstDeploymentId)).isEqualTo(2L);

    // when the batch jobs for the first deployment are executed
    getJobIdsByDeployment(batchJobs, firstDeploymentId).forEach(managementService::executeJob);
    // then the historic process instances related to the first deployment should be deleted
    assertThat(getHistoricProcessInstanceCountByDeploymentId(firstDeploymentId)).isZero();
    // and historic process instances related to the second deployment should not be deleted
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(2L);

    // when the remaining batch jobs are executed
    batchJobQuery.list().forEach(j -> managementService.executeJob(j.getId()));
    // then
    assertNoHistoryForTasks();
    assertHistoricBatchExists(testRule);
    assertAllHistoricProcessInstancesAreDeleted();
  }

  @Test
  void testDeleteHistoryProcessInstancesAsyncWithListInDifferentDeployments() {
    // given a second deployment
    prepareData();
    ProcessDefinitionQuery definitionQuery = engineRule.getRepositoryService().createProcessDefinitionQuery();
    String firstDeploymentId = definitionQuery.processDefinitionVersion(1).singleResult().getDeploymentId();
    String secondDeploymentId = definitionQuery.processDefinitionVersion(2).singleResult().getDeploymentId();

    engineRule.getProcessEngineConfiguration().setInvocationsPerBatchJob(2);

    // when
    Batch batch = historyService.deleteHistoricProcessInstancesAsync(historicProcessInstances, TEST_REASON);
    executeSeedJobs(batch, 2);
    // then batch jobs with different deployment ids exist
    List<Job> batchJobs = managementService.createJobQuery().jobDefinitionId(batch.getBatchJobDefinitionId()).list();
    assertThat(batchJobs).hasSize(2);
    assertThat(batchJobs.get(0).getDeploymentId()).isIn(firstDeploymentId, secondDeploymentId);
    assertThat(batchJobs.get(1).getDeploymentId()).isIn(firstDeploymentId, secondDeploymentId);
    assertThat(batchJobs.get(0).getDeploymentId()).isNotEqualTo(batchJobs.get(1).getDeploymentId());
    assertThat(historicProcessInstances).hasSize(4);
    assertThat(getHistoricProcessInstanceCountByDeploymentId(firstDeploymentId)).isEqualTo(2L);
    assertThat(getHistoricProcessInstanceCountByDeploymentId(secondDeploymentId)).isEqualTo(2L);

    // when the batch jobs for the first deployment are executed
    getJobIdsByDeployment(batchJobs, firstDeploymentId).forEach(managementService::executeJob);
    // then the historic process instances related to the first deployment should be deleted
    assertThat(getHistoricProcessInstanceCountByDeploymentId(firstDeploymentId)).isZero();
    // and historic process instances related to the second deployment should not be deleted
    assertThat(getHistoricProcessInstanceCountByDeploymentId(secondDeploymentId)).isEqualTo(2L);

    // when the remaining batch jobs are executed
    getJobIdsByDeployment(batchJobs, secondDeploymentId).forEach(managementService::executeJob);
    // then
    assertNoHistoryForTasks();
    assertHistoricBatchExists(testRule);
    assertAllHistoricProcessInstancesAreDeleted();
  }

  @Test
  void testDeleteHistoryProcessInstancesAsyncWithEmptyList() {
    // given
    List<String> processInstanceIds = emptyList();
    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricProcessInstancesAsync(processInstanceIds, TEST_REASON))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testDeleteHistoryProcessInstancesAsyncWithFake() {
    //given
    ArrayList<String> processInstanceIds = new ArrayList<>();
    processInstanceIds.add(historicProcessInstances.get(0));
    processInstanceIds.add("aFakeId");

    //when
    Batch batch = historyService.deleteHistoricProcessInstancesAsync(processInstanceIds, TEST_REASON);
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    //then
    assertThat(exceptions).isEmpty();
    assertHistoricBatchExists(testRule);
  }

  @Test
  void testDeleteHistoryProcessInstancesAsyncWithQueryAndList() {
    //given
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(historicProcessInstances.get(0));
    Batch batch = historyService.deleteHistoricProcessInstancesAsync(
        historicProcessInstances.subList(1, historicProcessInstances.size()), query, TEST_REASON);
    completeSeedJobs(batch);

    //when
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    assertNoHistoryForTasks();
    assertHistoricBatchExists(testRule);
    assertAllHistoricProcessInstancesAreDeleted();
  }

  @Test
  void testDeleteHistoryProcessInstancesAsyncWithQuery() {
    //given
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
        .processInstanceIds(new HashSet<>(historicProcessInstances));
    Batch batch = historyService.deleteHistoricProcessInstancesAsync(query, TEST_REASON);
    completeSeedJobs(batch);

    //when
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();
    assertNoHistoryForTasks();
    assertHistoricBatchExists(testRule);
    assertAllHistoricProcessInstancesAreDeleted();
  }

  @Test
  void testDeleteHistoryProcessInstancesAsyncWithEmptyQuery() {
    //given
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().unfinished();

    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricProcessInstancesAsync(query, TEST_REASON))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testDeleteHistoryProcessInstancesAsyncWithNonExistingIDAsQuery() {
    //given
    ArrayList<String> processInstanceIds = new ArrayList<>();
    processInstanceIds.add(historicProcessInstances.get(0));
    processInstanceIds.add("aFakeId");
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
        .processInstanceIds(new HashSet<>(processInstanceIds));

    //when
    Batch batch = historyService.deleteHistoricProcessInstancesAsync(query, TEST_REASON);
    completeSeedJobs(batch);
    executeBatchJobs(batch);

    //then
    assertHistoricBatchExists(testRule);
  }

  @Test
  void testDeleteHistoryProcessInstancesAsyncWithoutDeleteReason() {
    //when
    Batch batch = historyService.deleteHistoricProcessInstancesAsync(historicProcessInstances, null);
    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    //then
    assertThat(exceptions).isEmpty();
    assertNoHistoryForTasks();
    assertHistoricBatchExists(testRule);
    assertAllHistoricProcessInstancesAreDeleted();
  }

  @Test
  void testDeleteHistoryProcessInstancesAsyncWithNullList() {
    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricProcessInstancesAsync((List<String>) null, TEST_REASON))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testDeleteHistoryProcessInstancesAsyncWithNullQuery() {
    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricProcessInstancesAsync((HistoricProcessInstanceQuery) null, TEST_REASON))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void shouldSetInvocationsPerBatchType() {
    // given
    engineRule.getProcessEngineConfiguration()
        .getInvocationsPerBatchJobByBatchType()
        .put(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION, 42);

    //when
    Batch batch = historyService.deleteHistoricProcessInstancesAsync(historicProcessInstances, TEST_REASON);

    // then
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(42);

    // clear
    engineRule.getProcessEngineConfiguration()
        .setInvocationsPerBatchJobByBatchType(new HashMap<>());
  }

  protected long getHistoricProcessInstanceCountByDeploymentId(String deploymentId) {
    // fetch process definitions of the deployment
    Set<String> processDefinitionIds = engineRule.getRepositoryService().createProcessDefinitionQuery()
        .deploymentId(deploymentId).list().stream()
        .map(ProcessDefinition::getId)
        .collect(Collectors.toSet());
    // return historic instances of the deployed definitions
    return historyService.createHistoricProcessInstanceQuery().list().stream()
        .filter(hpi -> processDefinitionIds.contains(hpi.getProcessDefinitionId()))
        .map(HistoricProcessInstance::getId)
        .count();
  }

  protected void assertNoHistoryForTasks() {
    if (!testRule.isHistoryLevelNone()) {
      assertThat(historyService.createHistoricTaskInstanceQuery().count()).isZero();
    }
  }

  protected void assertAllHistoricProcessInstancesAreDeleted() {
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
  }

}
