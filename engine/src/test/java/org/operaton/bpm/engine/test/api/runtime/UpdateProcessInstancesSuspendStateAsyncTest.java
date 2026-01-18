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

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateProcessInstancesSuspendStateAsyncTest {

  protected static final Date TEST_DATE = new Date(1457326800000L);

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  BatchSuspensionHelper helper = new BatchSuspensionHelper(engineRule);

  RuntimeService runtimeService;
  HistoryService historyService;

  @AfterEach
  void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }


  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  void testBatchSuspensionById() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when
    Batch suspendprocess = runtimeService.updateProcessInstanceSuspensionState().byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId())).suspendAsync();
    helper.completeSeedJobs(suspendprocess);
    helper.executeJobs(suspendprocess);


    // then
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertThat(p1c.isSuspended()).isTrue();
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertThat(p2c.isSuspended()).isTrue();

  }

  @Test
  void testBatchSuspensionByIdsInDifferentDeployments() {
    // given
    String deploymentId1 = testRule.deployAndGetDefinition("org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml").getDeploymentId();
    String deploymentId2 = testRule.deployAndGetDefinition("org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml").getDeploymentId();
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when
    Batch suspendprocess = runtimeService.updateProcessInstanceSuspensionState().byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId())).suspendAsync();

    // then
    Job seedJob = helper.getSeedJob(suspendprocess);
    assertThat(seedJob.getDeploymentId()).isIn(deploymentId1, deploymentId2);

    // when
    helper.completeSeedJobs(suspendprocess);

    // then
    List<Job> batchJobs = helper.getExecutionJobs(suspendprocess);
    assertThat(batchJobs).hasSize(2);
    assertThat(batchJobs.get(0).getDeploymentId()).isIn(deploymentId1, deploymentId2);
    assertThat(batchJobs.get(1).getDeploymentId()).isIn(deploymentId1, deploymentId2);
    assertThat(batchJobs.get(0).getDeploymentId()).isNotEqualTo(batchJobs.get(1).getDeploymentId());

    // when
    helper.completeExecutionJobs(suspendprocess);

    // then
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertThat(p1c.isSuspended()).isTrue();
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertThat(p2c.isSuspended()).isTrue();

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  void shouldSetInvocationsPerBatchTypeOnSuspension() {
    // given
    engineRule.getProcessEngineConfiguration()
        .getInvocationsPerBatchJobByBatchType()
        .put(Batch.TYPE_PROCESS_INSTANCE_UPDATE_SUSPENSION_STATE, 42);

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when
    Batch batch = runtimeService.updateProcessInstanceSuspensionState()
        .byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId()))
        .suspendAsync();

    // then
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(42);

    // clear
    engineRule.getProcessEngineConfiguration()
        .setInvocationsPerBatchJobByBatchType(new HashMap<>());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  void shouldSetInvocationsPerBatchTypeOnActivation() {
    // given
    engineRule.getProcessEngineConfiguration()
        .getInvocationsPerBatchJobByBatchType()
        .put(Batch.TYPE_PROCESS_INSTANCE_UPDATE_SUSPENSION_STATE, 42);

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when
    Batch batch = runtimeService.updateProcessInstanceSuspensionState()
        .byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId()))
        .activateAsync();

    // then
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(42);

    // clear
    engineRule.getProcessEngineConfiguration()
        .setInvocationsPerBatchJobByBatchType(new HashMap<>());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  void testBatchActivationById() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when
    Batch suspendprocess = runtimeService.updateProcessInstanceSuspensionState().byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId())).suspendAsync();
    helper.completeSeedJobs(suspendprocess);
    helper.executeJobs(suspendprocess);
    Batch activateprocess = runtimeService.updateProcessInstanceSuspensionState().byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId())).activateAsync();
    helper.completeSeedJobs(activateprocess);
    helper.executeJobs(activateprocess);


    // then
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertThat(p1c.isSuspended()).isFalse();
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertThat(p2c.isSuspended()).isFalse();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  void testBatchSuspensionByProcessInstanceQuery() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when
    Batch suspendprocess = runtimeService.updateProcessInstanceSuspensionState().byProcessInstanceQuery(runtimeService.createProcessInstanceQuery().active()).suspendAsync();
    helper.completeSeedJobs(suspendprocess);
    helper.executeJobs(suspendprocess);


    // then
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertThat(p1c.isSuspended()).isTrue();
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertThat(p2c.isSuspended()).isTrue();

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  void testBatchActivationByProcessInstanceQuery() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when
    Batch suspendprocess = runtimeService.updateProcessInstanceSuspensionState().byProcessInstanceQuery(runtimeService.createProcessInstanceQuery().active()).suspendAsync();
    helper.completeSeedJobs(suspendprocess);
    helper.executeJobs(suspendprocess);
    Batch activateprocess = runtimeService.updateProcessInstanceSuspensionState().byProcessInstanceQuery(runtimeService.createProcessInstanceQuery().suspended()).activateAsync();
    helper.completeSeedJobs(activateprocess);
    helper.executeJobs(activateprocess);


    // then
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertThat(p1c.isSuspended()).isFalse();
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertThat(p2c.isSuspended()).isFalse();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void testBatchSuspensionByHistoricProcessInstanceQuery() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when
    Batch suspendprocess = runtimeService.updateProcessInstanceSuspensionState().byHistoricProcessInstanceQuery(historyService.createHistoricProcessInstanceQuery().processInstanceIds(Set.of(processInstance1.getId(), processInstance2.getId()))).suspendAsync();
    helper.completeSeedJobs(suspendprocess);
    helper.executeJobs(suspendprocess);


    // then
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertThat(p1c.isSuspended()).isTrue();
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertThat(p2c.isSuspended()).isTrue();

  }


  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void testBatchActivationByHistoricProcessInstanceQuery() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when
    Batch suspendprocess = runtimeService.updateProcessInstanceSuspensionState().byHistoricProcessInstanceQuery(historyService.createHistoricProcessInstanceQuery().processInstanceIds(Set.of(processInstance1.getId(), processInstance2.getId()))).suspendAsync();
    helper.completeSeedJobs(suspendprocess);
    helper.executeJobs(suspendprocess);
    Batch activateprocess = runtimeService.updateProcessInstanceSuspensionState().byHistoricProcessInstanceQuery(historyService.createHistoricProcessInstanceQuery().processInstanceIds(Set.of(processInstance1.getId(), processInstance2.getId()))).activateAsync();
    helper.completeSeedJobs(activateprocess);
    helper.executeJobs(activateprocess);


    // then
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertThat(p1c.isSuspended()).isFalse();
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertThat(p2c.isSuspended()).isFalse();
  }

  @Test
  void testEmptyProcessInstanceListSuspendAsync() {
    // given
    var updateProcessInstancesSuspensionStateBuilder = runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds();

    // when/then
    assertThatThrownBy(updateProcessInstancesSuspensionStateBuilder::suspendAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("No process instance ids given");
  }

  @Test
  void testEmptyProcessInstanceListActivateAsync() {
    // given
    var updateProcessInstancesSuspensionStateBuilder = runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds();

    // when/then
    assertThatThrownBy(updateProcessInstancesSuspensionStateBuilder::activateAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("No process instance ids given");
  }


  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  void testNullProcessInstanceListActivateAsync() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");
    var updateProcessInstancesSuspensionStateBuilder = runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId(), null));

    // when/then
    assertThatThrownBy(updateProcessInstancesSuspensionStateBuilder::activateAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot be null");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  void testNullProcessInstanceListSuspendAsync() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");
    var updateProcessInstancesSuspensionStateBuilder = runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId(), null));

    // when/then
    assertThatThrownBy(updateProcessInstancesSuspensionStateBuilder::suspendAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot be null");

  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldSetExecutionStartTimeInBatchAndHistory() {
    // given
    ClockUtil.setCurrentTime(TEST_DATE);
    testRule.deployAndGetDefinition("org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
        .getDeploymentId();
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    Batch batch = runtimeService.updateProcessInstanceSuspensionState()
        .byProcessInstanceIds(processInstance1.getId())
        .suspendAsync();
    helper.executeSeedJob(batch);
    List<Job> executionJobs = helper.getExecutionJobs(batch);

    // when
    helper.executeJob(executionJobs.get(0));

    // then
    HistoricBatch historicBatch = historyService.createHistoricBatchQuery().singleResult();
    batch = engineRule.getManagementService().createBatchQuery().singleResult();

    assertThat(batch.getExecutionStartTime()).isCloseTo(TEST_DATE, 1000);
    assertThat(historicBatch.getExecutionStartTime()).isCloseTo(TEST_DATE, 1000);
  }

}
