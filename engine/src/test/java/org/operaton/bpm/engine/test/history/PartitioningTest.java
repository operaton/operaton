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
package org.operaton.bpm.engine.test.history;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.impl.HistoricJobLogQueryImpl;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.batch.history.HistoricBatchEntity;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tassilo Weidner
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class PartitioningTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;
  ManagementService managementService;

  CommandExecutor commandExecutor;

  @BeforeEach
  void init() {
    commandExecutor = engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired();
  }

  private static final BpmnModelInstance PROCESS_WITH_USERTASK = Bpmn.createExecutableProcess("process")
    .startEvent()
      .userTask()
    .endEvent().done();

  @Test
  void shouldUpdateHistoricProcessInstance() {
    // given
    final String processInstanceId = deployAndStartProcess(PROCESS_WITH_USERTASK).getId();

    commandExecutor.execute(commandContext -> {

      HistoricProcessInstanceEntity historicProcessInstanceEntity =
          (HistoricProcessInstanceEntity) historyService.createHistoricProcessInstanceQuery().singleResult();

      commandContext.getDbEntityManager()
          .delete(historicProcessInstanceEntity);

      return null;
    });

    // assume
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();

    // when
    runtimeService.deleteProcessInstance(processInstanceId, "aDeleteReason");

    // then
    assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNull();

    // cleanup
    cleanUp(processInstanceId);
  }

  @Test
  void shouldUpdateHistoricTaskInstance() {
    // given
    deployAndStartProcess(PROCESS_WITH_USERTASK).getId();

    commandExecutor.execute(commandContext -> {

      HistoricTaskInstanceEntity historicTaskInstanceEntity =
          (HistoricTaskInstanceEntity) historyService.createHistoricTaskInstanceQuery().singleResult();

      commandContext.getDbEntityManager()
          .delete(historicTaskInstanceEntity);

      return null;
    });

    // assume
    assertThat(historyService.createHistoricTaskInstanceQuery().singleResult()).isNull();

    // when
    String taskId = taskService.createTaskQuery()
      .singleResult()
      .getId();

    taskService.complete(taskId);

    // then
    assertThat(taskService.createTaskQuery().singleResult()).isNull();
  }

  @Test
  void shouldUpdateHistoricActivityInstance() {
    // given
    final String processInstanceId = deployAndStartProcess(PROCESS_WITH_USERTASK).getId();

    commandExecutor.execute(commandContext -> {

      commandContext.getHistoricActivityInstanceManager()
          .deleteHistoricActivityInstancesByProcessInstanceIds(Collections.singletonList(processInstanceId));

      return null;
    });

    // assume
    assertThat(historyService.createHistoricActivityInstanceQuery().count()).isZero();

    // when
    String taskId = taskService.createTaskQuery()
      .singleResult()
      .getId();

    taskService.complete(taskId);

    // then
    assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(1L);
  }

  @Test
  void shouldUpdateHistoricIncident() {
    // given
    final String processInstanceId = deployAndStartProcess(PROCESS_WITH_USERTASK).getId();

    ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().singleResult();

    String incidentId = engineRule.getRuntimeService()
      .createIncident("foo", execution.getId(), execution.getActivityId(), "bar").getId();

    commandExecutor.execute(commandContext -> {

      commandContext.getHistoricIncidentManager()
          .deleteHistoricIncidentsByProcessInstanceIds(Collections.singletonList(processInstanceId));

      return null;
    });

    // assume
    assertThat(historyService.createHistoricIncidentQuery().count()).isZero();
    assertThat(runtimeService.createIncidentQuery().count()).isEqualTo(1L);

    // when
    runtimeService.resolveIncident(incidentId);

    // then
    assertThat(runtimeService.createIncidentQuery().count()).isZero();
    assertThat(historyService.createHistoricIncidentQuery().count()).isZero();
  }

  @Test
  void shouldUpdateHistoricBatch() {
    // given
    String processInstanceId = deployAndStartProcess(PROCESS_WITH_USERTASK).getId();

    final Batch batch = runtimeService.deleteProcessInstancesAsync(Collections.singletonList(processInstanceId), "aDeleteReason");

    // assume
    assertThat(historyService.createHistoricBatchQuery().count()).isEqualTo(1L);

    commandExecutor.execute(commandContext -> {

      HistoricBatchEntity historicBatchEntity = (HistoricBatchEntity) historyService.createHistoricBatchQuery()
          .singleResult();

      commandContext.getDbEntityManager()
          .delete(historicBatchEntity);

      return null;
    });

    // assume
    assertThat(historyService.createHistoricBatchQuery().count()).isZero();

    // when
    String seedJobDefinitionId = batch.getSeedJobDefinitionId();
    Job seedJob = managementService.createJobQuery().jobDefinitionId(seedJobDefinitionId).singleResult();
    managementService.executeJob(seedJob.getId());

    String batchJobDefinitionId = batch.getBatchJobDefinitionId();
    List<Job> batchJobs = managementService.createJobQuery().jobDefinitionId(batchJobDefinitionId).list();
    for (Job batchJob : batchJobs) {
      managementService.executeJob(batchJob.getId());
    }

    List<Job> monitorJobs = managementService.createJobQuery().jobDefinitionId(batch.getMonitorJobDefinitionId()).list();
    for (Job monitorJob : monitorJobs) {
      managementService.executeJob(monitorJob.getId());
    }

    // then
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    assertThat(managementService.createBatchQuery().count()).isZero();

    // cleanup
    cleanUp(processInstanceId);
  }

  protected ProcessInstance deployAndStartProcess(BpmnModelInstance bpmnModelInstance) {
    testHelper.deploy(bpmnModelInstance);

    String processDefinitionKey = bpmnModelInstance.getDefinitions().getRootElements().iterator().next().getId();
    return runtimeService.startProcessInstanceByKey(processDefinitionKey);
  }

  protected void cleanUp(final String processInstanceId) {
    commandExecutor.execute(commandContext -> {

      commandContext.getHistoricActivityInstanceManager()
          .deleteHistoricActivityInstancesByProcessInstanceIds(Collections.singletonList(processInstanceId));

      commandContext.getHistoricTaskInstanceManager()
          .deleteHistoricTaskInstancesByProcessInstanceIds(Collections.singletonList(processInstanceId), true);

      List<HistoricJobLog> historicJobLogs = commandContext.getHistoricJobLogManager()
          .findHistoricJobLogsByQueryCriteria(new HistoricJobLogQueryImpl(), new Page(0, 100));

      for (HistoricJobLog historicJobLog : historicJobLogs) {
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(historicJobLog.getJobId());
      }

      return null;
    });
  }

}
