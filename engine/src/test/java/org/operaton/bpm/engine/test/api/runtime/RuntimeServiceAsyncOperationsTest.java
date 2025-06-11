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
package org.operaton.bpm.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.AbstractAsyncOperationsTest;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.api.runtime.util.IncrementCounterListener;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;


/**
 * @author Askar Akhmerov
 */
class RuntimeServiceAsyncOperationsTest extends AbstractAsyncOperationsTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  protected MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);

  @BeforeEach
  void setup() {
    initDefaults(engineRule);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesAsyncWithList() {
    // given
    List<String> processIds = startTestProcesses(2);

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(processIds, null, TESTING_INSTANCE_DELETE);

    completeSeedJobs(batch);
    executeBatchJobs(batch);

    // then
    assertHistoricTaskDeletionPresent(processIds, TESTING_INSTANCE_DELETE, testRule);
    assertHistoricBatchExists(testRule);
    assertProcessInstancesAreDeleted();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesAsyncWithLargeList() {
    // given
    engineRule.getProcessEngineConfiguration().setBatchJobsPerSeed(1010);
    List<String> processIds = startTestProcesses(1100);

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(processIds, null, TESTING_INSTANCE_DELETE);

    executeSeedJobs(batch, 2);
    executeBatchJobs(batch);

    // then
    assertHistoricTaskDeletionPresent(processIds, TESTING_INSTANCE_DELETE, testRule);
    assertHistoricBatchExists(testRule);
    assertProcessInstancesAreDeleted();

    // cleanup
    if (!testRule.isHistoryLevelNone()) {
      batch = historyService.deleteHistoricProcessInstancesAsync(processIds, null);
      executeSeedJobs(batch, 2);
      executeBatchJobs(batch);
    }
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesAsyncWithListOnly() {
    // given
    List<String> processIds = startTestProcesses(2);

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(processIds, TESTING_INSTANCE_DELETE);

    completeSeedJobs(batch);
    executeBatchJobs(batch);

    // then
    assertHistoricTaskDeletionPresent(processIds, TESTING_INSTANCE_DELETE, testRule);
    assertHistoricBatchExists(testRule);
    assertProcessInstancesAreDeleted();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesAsyncWithFake() {
    // given
    List<String> processIds = startTestProcesses(2);
    processIds.add("aFake");

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(processIds, null, TESTING_INSTANCE_DELETE);

    completeSeedJobs(batch);
    List<Exception> exceptions = executeBatchJobs(batch);

    // then
    assertThat(exceptions).isEmpty();

    assertThat(managementService.createJobQuery().withException().list()).isEmpty();

    processIds.remove("aFake");
    assertHistoricTaskDeletionPresent(processIds, TESTING_INSTANCE_DELETE, testRule);
    assertHistoricBatchExists(testRule);
    assertProcessInstancesAreDeleted();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesAsyncWithNullList() {

    // when/then
    assertThatThrownBy(() -> runtimeService.deleteProcessInstancesAsync(null, null, TESTING_INSTANCE_DELETE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("processInstanceIds is empty");

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesAsyncWithEmptyList() {
    // given
    List<String> emptyProcessInstanceIds = new ArrayList<>();

    // when/then
    assertThatThrownBy(() -> runtimeService.deleteProcessInstancesAsync(emptyProcessInstanceIds, null, TESTING_INSTANCE_DELETE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("processInstanceIds is empty");

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesAsyncWithQuery() {
    // given
    List<String> processIds = startTestProcesses(2);
    ProcessInstanceQuery processInstanceQuery = runtimeService
        .createProcessInstanceQuery().processInstanceIds(new HashSet<>(processIds));

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(null, processInstanceQuery, TESTING_INSTANCE_DELETE);

    completeSeedJobs(batch);
    executeBatchJobs(batch);

    // then
    assertHistoricTaskDeletionPresent(processIds, TESTING_INSTANCE_DELETE, testRule);
    assertHistoricBatchExists(testRule);
    assertProcessInstancesAreDeleted();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesAsyncWithQueryOnly() {
    // given
    List<String> processIds = startTestProcesses(2);
    ProcessInstanceQuery processInstanceQuery = runtimeService
        .createProcessInstanceQuery().processInstanceIds(new HashSet<>(processIds));

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(processInstanceQuery, TESTING_INSTANCE_DELETE);

    completeSeedJobs(batch);
    executeBatchJobs(batch);

    // then
    assertHistoricTaskDeletionPresent(processIds, TESTING_INSTANCE_DELETE, testRule);
    assertHistoricBatchExists(testRule);
    assertProcessInstancesAreDeleted();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesAsyncWithQueryWithoutDeleteReason() {
    // given
    List<String> processIds = startTestProcesses(2);
    ProcessInstanceQuery processInstanceQuery = runtimeService
        .createProcessInstanceQuery().processInstanceIds(new HashSet<>(processIds));

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(null, processInstanceQuery, null);

    completeSeedJobs(batch);
    executeBatchJobs(batch);

    // then
    assertHistoricTaskDeletionPresent(processIds, "deleted", testRule);
    assertHistoricBatchExists(testRule);
    assertProcessInstancesAreDeleted();
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesAsyncWithHistoryQuery() {
    // given
    List<String> processIds = startTestProcesses(2);
    HistoricProcessInstanceQuery historicProcessInstanceQuery =
        historyService.createHistoricProcessInstanceQuery()
            .processInstanceIds(new HashSet<>(processIds));

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(null, null,
        historicProcessInstanceQuery, "", false, false, false);

    completeSeedJobs(batch);
    executeBatchJobs(batch);

    // then
    assertHistoricTaskDeletionPresent(processIds, "deleted", testRule);
    assertHistoricBatchExists(testRule);
    assertProcessInstancesAreDeleted();
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesAsyncWithRuntimeAndHistoryQuery() {
    // given
    List<String> processIds = startTestProcesses(2);
    HistoricProcessInstanceQuery historicProcessInstanceQuery =
        historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(processIds.get(0));

    ProcessInstanceQuery processInstanceQuery =
        runtimeService.createProcessInstanceQuery().processInstanceId(processIds.get(1));

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(null, processInstanceQuery,
        historicProcessInstanceQuery, "", false, false, false);

    completeSeedJobs(batch);
    executeBatchJobs(batch);

    // then
    assertHistoricTaskDeletionPresent(processIds, "deleted", testRule);
    assertHistoricBatchExists(testRule);
    assertProcessInstancesAreDeleted();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesAsyncWithNullQueryParameter() {

    // when/then
    assertThatThrownBy(() -> runtimeService.deleteProcessInstancesAsync(null, null, TESTING_INSTANCE_DELETE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("processInstanceIds is empty");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesAsyncWithInvalidQueryParameter() {
    // given
    startTestProcesses(2);
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
        .processInstanceBusinessKey("invalid");

    // when/then
    assertThatThrownBy(() -> runtimeService.deleteProcessInstancesAsync(null, query, TESTING_INSTANCE_DELETE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("processInstanceIds is empty");

  }

  protected void assertProcessInstancesAreDeleted() {
    assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
  }

  @Test
  void testDeleteProcessInstancesAsyncWithSkipCustomListeners() {

    // given
    IncrementCounterListener.counter = 0;

    BpmnModelInstance instance = ProcessModels.newModel(ONE_TASK_PROCESS)
        .startEvent()
        .userTask()
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, IncrementCounterListener.class.getName())
        .endEvent()
        .done();

    testRule.deploy(instance);
    List<String> processIds = startTestProcesses(1);

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(processIds, null, TESTING_INSTANCE_DELETE, true);
    completeSeedJobs(batch);
    executeBatchJobs(batch);

    // then
    assertThat(IncrementCounterListener.counter).isZero();
  }

  @Test
  void testDeleteProcessInstancesAsyncWithSkipSubprocesses() {

    // given
    BpmnModelInstance callingInstance = ProcessModels.newModel(ONE_TASK_PROCESS)
        .startEvent()
          .callActivity()
            .calledElement("called")
        .endEvent()
        .done();

    BpmnModelInstance calledInstance = ProcessModels.newModel("called")
        .startEvent()
        .userTask()
        .endEvent()
        .done();

    testRule.deploy(callingInstance, calledInstance);
    List<String> processIds = startTestProcesses(1);

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(processIds, null, TESTING_INSTANCE_DELETE, false, true);
    completeSeedJobs(batch);
    executeBatchJobs(batch);

    // then
    ProcessInstance superInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processIds.get(0)).singleResult();
    assertThat(superInstance).isNull();

    ProcessInstance subInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("called").singleResult();
    assertThat(subInstance).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcessWithIoMappings.bpmn20.xml")
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void shouldApplySkipIoMappingOnDeleteProcessInstancesAsync() {
    // given a process instance
    var instanceId1 = runtimeService.startProcessInstanceByKey("ioMappingProcess").getProcessInstanceId();
    var instanceId2 = runtimeService.startProcessInstanceByKey("ioMappingProcess").getProcessInstanceId();
    var instanceIds = List.of(instanceId1, instanceId2);

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(instanceIds, null, null, null, true, false, true);

    completeSeedJobs(batch);
    executeBatchJobs(batch);

    // then
    testRule.assertProcessEnded(instanceId1);
    testRule.assertProcessEnded(instanceId2);

    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(instanceId1).list().size()).isOne();
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(instanceId2).list().size()).isOne();
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("inputMappingExecuted").count()).isEqualTo(2);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcessWithIoMappings.bpmn20.xml")
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void shouldNotApplySkipIoMappingOnDeleteProcessInstancesAsyncFalse() {
    // given a process instance
    var instanceId1 = runtimeService.startProcessInstanceByKey("ioMappingProcess").getProcessInstanceId();
    var instanceId2 = runtimeService.startProcessInstanceByKey("ioMappingProcess").getProcessInstanceId();
    var instanceIds = List.of(instanceId1, instanceId2);

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(instanceIds, null, null, null, true, false, false);

    completeSeedJobs(batch);
    executeBatchJobs(batch);

    // then
    testRule.assertProcessEnded(instanceId1);
    testRule.assertProcessEnded(instanceId2);

    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(instanceId1).list()).hasSize(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(instanceId2).list()).hasSize(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("inputMappingExecuted").count()).isEqualTo(2);
  }

  @Test
  void testDeleteProcessInstancesAsyncWithoutSkipSubprocesses() {

    // given
    BpmnModelInstance callingInstance = ProcessModels.newModel(ONE_TASK_PROCESS)
        .startEvent()
          .callActivity()
            .calledElement("called")
        .endEvent()
        .done();

    BpmnModelInstance calledInstance = ProcessModels.newModel("called")
        .startEvent()
        .userTask()
        .endEvent()
        .done();

    testRule.deploy(callingInstance, calledInstance);
    List<String> processIds = startTestProcesses(1);

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(processIds, null, TESTING_INSTANCE_DELETE, false, false);
    completeSeedJobs(batch);
    executeBatchJobs(batch);

    // then
    ProcessInstance superInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processIds.get(0)).singleResult();
    assertThat(superInstance).isNull();

    ProcessInstance subInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("called").singleResult();
    assertThat(subInstance).isNull();
  }


  @Test
  void testInvokeListenersWhenDeletingProcessInstancesAsync() {

    // given
    IncrementCounterListener.counter = 0;

    BpmnModelInstance instance = ProcessModels.newModel(ONE_TASK_PROCESS)
        .startEvent()
        .userTask()
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, IncrementCounterListener.class.getName())
        .endEvent()
        .done();

    migrationRule.deploy(instance);
    List<String> processIds = startTestProcesses(1);

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(processIds, TESTING_INSTANCE_DELETE);
    completeSeedJobs(batch);
    executeBatchJobs(batch);

    // then
    assertThat(IncrementCounterListener.counter).isEqualTo(1);
  }

  @Test
  void testDeleteProcessInstancesAsyncWithListInDifferentDeployments() {
    // given
    ProcessDefinition sourceDefinition1 = testRule
        .deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS).changeElementId(ProcessModels.PROCESS_KEY, "ONE_TASK_PROCESS"));
    ProcessDefinition sourceDefinition2 = testRule
        .deployAndGetDefinition(modify(ProcessModels.TWO_TASKS_PROCESS).changeElementId(ProcessModels.PROCESS_KEY, "TWO_TASKS_PROCESS"));
    List<String> processInstanceIds = createProcessInstances(sourceDefinition1, sourceDefinition2, 15, 10);
    final String firstDeploymentId = sourceDefinition1.getDeploymentId();
    final String secondDeploymentId = sourceDefinition2.getDeploymentId();

    List<String> processInstanceIdsFromFirstDeployment = getProcessInstanceIdsByDeploymentId(firstDeploymentId);
    List<String> processInstanceIdsFromSecondDeployment = getProcessInstanceIdsByDeploymentId(secondDeploymentId);

    engineRule.getProcessEngineConfiguration().setInvocationsPerBatchJob(2);
    engineRule.getProcessEngineConfiguration().setBatchJobsPerSeed(3);

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(processInstanceIds, null, "test_reason");

    // seed jobs
    int expectedSeedJobsCount = 5;
    executeSeedJobs(batch, expectedSeedJobsCount);

    // then
    List<Job> jobs = managementService.createJobQuery().jobDefinitionId(batch.getBatchJobDefinitionId()).list();

    // execute jobs related to the first deployment
    List<String> jobIdsForFirstDeployment = getJobIdsByDeployment(jobs, firstDeploymentId);
    assertThat(jobIdsForFirstDeployment).isNotNull();
    for (String jobId : jobIdsForFirstDeployment) {
      managementService.executeJob(jobId);
    }

    // the process instances related to the first deployment should be deleted
    assertThat(runtimeService.createProcessInstanceQuery().deploymentId(firstDeploymentId).count()).isZero();
    assertHistoricTaskDeletionPresent(processInstanceIdsFromFirstDeployment, "test_reason", testRule);
    // and process instances related to the second deployment should not be deleted
    assertThat(runtimeService.createProcessInstanceQuery().deploymentId(secondDeploymentId).count()).isEqualTo(processInstanceIdsFromSecondDeployment.size());
    assertHistoricTaskDeletionPresent(processInstanceIdsFromSecondDeployment, null, testRule);

    // execute jobs related to the second deployment
    List<String> jobIdsForSecondDeployment = getJobIdsByDeployment(jobs, secondDeploymentId);
    assertThat(jobIdsForSecondDeployment).isNotNull();
    for (String jobId : jobIdsForSecondDeployment) {
      managementService.executeJob(jobId);
    }

    // all of the process instances should be deleted
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void shouldSetInvocationsPerBatchType() {
    // given
    engineRule.getProcessEngineConfiguration()
        .getInvocationsPerBatchJobByBatchType()
        .put(Batch.TYPE_PROCESS_INSTANCE_DELETION, 42);

    List<String> processIds = startTestProcesses(2);

    // when
    Batch batch = runtimeService.deleteProcessInstancesAsync(processIds, null, TESTING_INSTANCE_DELETE);

    // then
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(42);

    // clear
    engineRule.getProcessEngineConfiguration()
        .setInvocationsPerBatchJobByBatchType(new HashMap<>());
  }

  private List<String> createProcessInstances(ProcessDefinition sourceDefinition1, ProcessDefinition sourceDefinition2, int instanceCountDef1, int instanceCountDef2) {
    List<String> processInstanceIds = new ArrayList<>();
    for (int i = 0; i < instanceCountDef1; i++) {
      ProcessInstance processInstance1 = runtimeService.startProcessInstanceById(sourceDefinition1.getId());
      processInstanceIds.add(processInstance1.getId());
      if (i < instanceCountDef2) {
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceById(sourceDefinition2.getId());
        processInstanceIds.add(processInstance2.getId());
      }
    }
    return processInstanceIds;
  }

  private List<String> getProcessInstanceIdsByDeploymentId(final String deploymentId) {
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().deploymentId(deploymentId).list();
    List<String> processInstanceIds = new ArrayList<>();
    for (ProcessInstance processInstance : processInstances) {
      processInstanceIds.add(processInstance.getId());
    }
    return processInstanceIds;
  }
}
