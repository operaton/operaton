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
package org.operaton.bpm.engine.test.api.history.removaltime.batch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.batch.history.HistoricBatchQuery;
import org.operaton.bpm.engine.history.CleanableHistoricBatchReportResult;
import org.operaton.bpm.engine.history.CleanableHistoricDecisionInstanceReportResult;
import org.operaton.bpm.engine.history.CleanableHistoricProcessInstanceReportResult;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.history.SetRemovalTimeSelectModeForHistoricBatchesBuilder;
import org.operaton.bpm.engine.history.SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder;
import org.operaton.bpm.engine.history.SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder;
import org.operaton.bpm.engine.history.SetRemovalTimeToHistoricBatchesBuilder;
import org.operaton.bpm.engine.history.SetRemovalTimeToHistoricDecisionInstancesBuilder;
import org.operaton.bpm.engine.history.SetRemovalTimeToHistoricProcessInstancesBuilder;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.history.removaltime.batch.helper.BatchSetRemovalTimeExtension;
import org.operaton.bpm.engine.test.api.runtime.BatchHelper;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_FULL;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_REMOVAL_TIME_STRATEGY_END;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_REMOVAL_TIME_STRATEGY_NONE;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_REMOVAL_TIME_STRATEGY_START;
import static org.operaton.bpm.engine.test.api.history.removaltime.batch.helper.BatchSetRemovalTimeRule.addDays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Tassilo Weidner
 */

@RequiredHistoryLevel(HISTORY_FULL)
class BatchSetRemovalTimeTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension engineTestRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  BatchSetRemovalTimeExtension testRule = new BatchSetRemovalTimeExtension(engineRule, engineTestRule);
  BatchHelper helper = new BatchHelper(engineRule);

  protected final Date currentDate = testRule.CURRENT_DATE;
  protected final Date removalTime = testRule.REMOVAL_TIME;

  protected RuntimeService runtimeService;
  protected DecisionService decisionService;
  protected HistoryService historyService;
  protected ManagementService managementService;

  @AfterEach
  void tearDown() {
    ClockUtil.reset();
    managementService.createBatchQuery().list().forEach(b -> managementService.deleteBatch(b.getId(), true));
    historyService.createHistoricBatchQuery().list().forEach(b -> historyService.deleteHistoricBatch(b.getId()));
    engineRule.getProcessEngineConfiguration().setInvocationsPerBatchJobByBatchType(new HashMap<>());
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldNotSetRemovalTime_DmnDisabled() {
    // given
    testRule.getProcessEngineConfiguration()
      .setDmnEnabled(false);

    testRule.process().ruleTask("dish-decision").deploy().startWithVariables(
      Variables.createVariables()
        .putValue("temperature", 32)
        .putValue("dayType", "Weekend")
    );

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isNull();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .absoluteRemovalTime(removalTime)
        .byQuery(query)
        .executeAsync()
    );

    historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isNull();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldNotSetRemovalTimeInHierarchy_DmnDisabled() {
    // given
    testRule.getProcessEngineConfiguration()
      .setDmnEnabled(false);

    testRule.process()
      .call()
        .passVars("temperature", "dayType")
      .ruleTask("dish-decision")
      .deploy()
      .startWithVariables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      );

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isNull();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().rootProcessInstances();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isNull();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldNotSetRemovalTimeForStandaloneDecision_DmnDisabled() {
    // given
    testRule.getProcessEngineConfiguration()
      .setDmnEnabled(false);

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isNull();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .absoluteRemovalTime(removalTime)
        .byQuery(query)
        .executeAsync()
    );

    historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isNull();
  }

  @Test
  void shouldCreateDeploymentAwareBatchJobs_ProcessInstances() {
    // given
    testRule.getProcessEngineConfiguration()
      .setInvocationsPerBatchJob(2);

    testRule.process().userTask().deploy().start();
    testRule.process().userTask().deploy().start();

    List<String> deploymentIds = engineRule.getRepositoryService().createDeploymentQuery()
        .list().stream()
        .map(org.operaton.bpm.engine.repository.Deployment::getId)
        .toList();

    // when
    Batch batch = historyService.setRemovalTimeToHistoricProcessInstances()
        .absoluteRemovalTime(removalTime)
        .byQuery(historyService.createHistoricProcessInstanceQuery())
        .executeAsync();
    testRule.executeSeedJobs(batch);

    // then
    List<Job> executionJobs = testRule.getExecutionJobs(batch);
    assertThat(executionJobs).hasSize(2);
    assertThat(executionJobs.get(0).getDeploymentId()).isIn(deploymentIds);
    assertThat(executionJobs.get(1).getDeploymentId()).isIn(deploymentIds);
    assertThat(executionJobs.get(0).getDeploymentId()).isNotEqualTo(executionJobs.get(1).getDeploymentId());
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldCreateDeploymentAwareBatchJobs_StandaloneDecision() {
    // given
    testRule.getProcessEngineConfiguration()
      .setInvocationsPerBatchJob(3);

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    // ... and a second DMN deployment and its evaluation
    engineTestRule.deploy("org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml");
    decisionService.evaluateDecisionByKey("dish-decision")
    .variables(
      Variables.createVariables()
        .putValue("temperature", 32)
        .putValue("dayType", "Weekend")
    ).evaluate();

    List<String> deploymentIds = engineRule.getRepositoryService().createDeploymentQuery()
        .list().stream()
        .map(org.operaton.bpm.engine.repository.Deployment::getId)
        .toList();

    // when
    Batch batch = historyService.setRemovalTimeToHistoricDecisionInstances()
        .absoluteRemovalTime(removalTime)
        .byQuery(historyService.createHistoricDecisionInstanceQuery())
        .executeAsync();
    testRule.executeSeedJobs(batch);

    // then
    List<Job> executionJobs = testRule.getExecutionJobs(batch);
    assertThat(executionJobs).hasSize(2);
    assertThat(executionJobs.get(0).getDeploymentId()).isIn(deploymentIds);
    assertThat(executionJobs.get(1).getDeploymentId()).isIn(deploymentIds);
    assertThat(executionJobs.get(0).getDeploymentId()).isNotEqualTo(executionJobs.get(1).getDeploymentId());
  }

  @Test
  void shouldSetRemovalTime_MultipleInvocationsPerBatchJob() {
    // given
    testRule.getProcessEngineConfiguration()
      .setInvocationsPerBatchJob(2);

    testRule.process().userTask().deploy().start();
    testRule.process().userTask().deploy().start();

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // assume
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isNull();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .absoluteRemovalTime(removalTime)
        .byQuery(query)
        .executeAsync()
    );

    historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // then
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetRemovalTimeForStandaloneDecision_MultipleInvocationsPerBatchJob() {
    // given
    testRule.getProcessEngineConfiguration()
      .setInvocationsPerBatchJob(2);

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // assume
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isNull();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .absoluteRemovalTime(removalTime)
        .byQuery(query)
        .executeAsync()
    );

    historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldSetRemovalTimeForBatch_MultipleInvocationsPerBatchJob() {
    // given
    testRule.getProcessEngineConfiguration()
      .setInvocationsPerBatchJob(2);

    String processInstanceIdOne = testRule.process().userTask().deploy().start();
    historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceIdOne), "");

    String processInstanceIdTwo = testRule.process().userTask().deploy().start();
    historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceIdTwo), "");

    List<HistoricBatch> historicBatches = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .list();

    // assume
    assertThat(historicBatches.get(0).getRemovalTime()).isNull();
    assertThat(historicBatches.get(1).getRemovalTime()).isNull();

    HistoricBatchQuery query = historyService.createHistoricBatchQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .absoluteRemovalTime(removalTime)
        .byQuery(query)
        .executeAsync()
    );

    historicBatches = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .list();

    // then
    assertThat(historicBatches.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicBatches.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldSetRemovalTime_SingleInvocationPerBatchJob() {
    // given
    testRule.getProcessEngineConfiguration()
      .setInvocationsPerBatchJob(1);

    testRule.process().userTask().deploy().start();
    testRule.process().userTask().deploy().start();

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // assume
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isNull();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .absoluteRemovalTime(removalTime)
        .byQuery(query)
        .executeAsync()
    );

    historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // then
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetRemovalTimeForStandaloneDecision_SingleInvocationPerBatchJob() {
    // given
    testRule.getProcessEngineConfiguration()
      .setInvocationsPerBatchJob(1);

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // assume
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isNull();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .absoluteRemovalTime(removalTime)
        .byQuery(query)
        .executeAsync()
    );

    historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldSetRemovalTimeForBatch_SingleInvocationPerBatchJob() {
    // given
    testRule.getProcessEngineConfiguration()
      .setInvocationsPerBatchJob(1);

    String processInstanceIdOne = testRule.process().userTask().deploy().start();
    historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceIdOne), "");

    String processInstanceIdTwo = testRule.process().userTask().deploy().start();
    historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceIdTwo), "");

    List<HistoricBatch> historicBatches = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .list();

    // assume
    assertThat(historicBatches.get(0).getRemovalTime()).isNull();
    assertThat(historicBatches.get(1).getRemovalTime()).isNull();

    HistoricBatchQuery query = historyService.createHistoricBatchQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .absoluteRemovalTime(removalTime)
        .byQuery(query)
        .executeAsync()
    );

    historicBatches = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .list();

    // then
    assertThat(historicBatches.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicBatches.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldNotSetRemovalTime_BaseTimeNone() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_NONE)
      .initHistoryRemovalTime();

    testRule.process().ttl(5).serviceTask().deploy().start();

    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // assume
    assertThat(historicProcessInstance.getRemovalTime()).isNull();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // then
    assertThat(historicProcessInstance.getRemovalTime()).isNull();
  }

  @Test
  void shouldClearRemovalTime_BaseTimeNone() {
    // given
    testRule.process().ttl(5).serviceTask().deploy().start();

    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // assume
    assertThat(historicProcessInstance.getRemovalTime()).isNotNull();

    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_NONE)
      .initHistoryRemovalTime();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // then
    assertThat(historicProcessInstance.getRemovalTime()).isNull();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldNotSetRemovalTimeForStandaloneDecision_BaseTimeNone() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_NONE)
      .initHistoryRemovalTime();

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    testRule.updateHistoryTimeToLiveDmn("dish-decision", 5);

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // assume
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isNull();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isNull();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldClearRemovalTimeForStandaloneDecision_BaseTimeNone() {
    testRule.updateHistoryTimeToLiveDmn("dish-decision", 5);

    // given
    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // assume
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNotNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNotNull();
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isNotNull();

    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_NONE)
      .initHistoryRemovalTime();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isNull();
  }

  @Test
  void shouldNotSetRemovalTimeInHierarchy_BaseTimeNone() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_NONE)
      .initHistoryRemovalTime();

    testRule.process().ttl(5).call().serviceTask().deploy().start();

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // assume
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isNull();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().rootProcessInstances();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // then
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isNull();
  }

  @Test
  void shouldClearRemovalTimeInHierarchy_BaseTimeNone() {
    // given
    testRule.process().ttl(5).call().serviceTask().deploy().start();

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // assume
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNotNull();
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isNotNull();

    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_NONE)
      .initHistoryRemovalTime();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().rootProcessInstances();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // then
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isNull();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldNotSetRemovalTimeForStandaloneDecisionInHierarchy_BaseTimeNone() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_NONE)
      .initHistoryRemovalTime();

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    testRule.updateHistoryTimeToLiveDmn("dish-decision", 5);

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult();

    // assume
    assertThat(historicDecisionInstance.getRemovalTime()).isNull();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().rootDecisionInstancesOnly();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult();

    // then
    assertThat(historicDecisionInstance.getRemovalTime()).isNull();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldClearRemovalTimeForStandaloneDecisionInHierarchy_BaseTimeNone() {
    // given
    testRule.updateHistoryTimeToLiveDmn("dish-decision", 5);

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult();

    // assume
    assertThat(historicDecisionInstance.getRemovalTime()).isNotNull();

    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_NONE)
      .initHistoryRemovalTime();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().rootDecisionInstancesOnly();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult();

    // then
    assertThat(historicDecisionInstance.getRemovalTime()).isNull();
  }

  @Test
  void shouldNotSetRemovalTimeForBatch_BaseTimeNone() {
    // given
    ProcessEngineConfigurationImpl configuration = testRule.getProcessEngineConfiguration();
    configuration.setHistoryCleanupStrategy("endTimeBased");
    configuration.setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_NONE);

    configuration.setBatchOperationHistoryTimeToLive("P5D");
    configuration.initHistoryCleanup();

    String processInstanceIdOne = testRule.process().serviceTask().deploy().start();
    Batch batchOne = historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceIdOne), "");
    testRule.syncExec(batchOne);

    String processInstanceIdTwo = testRule.process().serviceTask().deploy().start();
    Batch batchTwo = historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceIdTwo), "");
    testRule.syncExec(batchTwo);

    List<HistoricBatch> historicBatches = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .list();

    // assume
    assertThat(historicBatches.get(0).getRemovalTime()).isNull();
    assertThat(historicBatches.get(1).getRemovalTime()).isNull();

    HistoricBatchQuery query = historyService.createHistoricBatchQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicBatches = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .list();

    // then
    assertThat(historicBatches.get(0).getRemovalTime()).isNull();
    assertThat(historicBatches.get(1).getRemovalTime()).isNull();
  }

  @Test
  void shouldClearRemovalTimeForBatch_BaseTimeNone() {
    // given
    ProcessEngineConfigurationImpl configuration = testRule.getProcessEngineConfiguration();

    configuration.setBatchOperationHistoryTimeToLive("P5D");
    configuration.initHistoryCleanup();

    String processInstanceIdOne = testRule.process().serviceTask().deploy().start();
    Batch batchOne = historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceIdOne), "");
    testRule.syncExec(batchOne);

    String processInstanceIdTwo = testRule.process().serviceTask().deploy().start();
    Batch batchTwo = historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceIdTwo), "");
    testRule.syncExec(batchTwo);

    List<HistoricBatch> historicBatches = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .list();

    // assume
    assertThat(historicBatches.get(0).getRemovalTime()).isNotNull();
    assertThat(historicBatches.get(1).getRemovalTime()).isNotNull();

    configuration.setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_NONE);

    HistoricBatchQuery query = historyService.createHistoricBatchQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicBatches = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .list();

    // then
    assertThat(historicBatches.get(0).getRemovalTime()).isNull();
    assertThat(historicBatches.get(1).getRemovalTime()).isNull();
  }

  @Test
  void shouldSetRemovalTime_BaseTimeStart() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_START)
      .initHistoryRemovalTime();

    testRule.process().userTask().deploy().start();

    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // assume
    assertThat(historicProcessInstance.getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLive("process", 5);

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // then
    assertThat(historicProcessInstance.getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetRemovalTimeForStandaloneDecision_BaseTimeStart() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_START)
      .initHistoryRemovalTime();

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance.getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLiveDmn("dish-decision", 5);

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .singleResult();

    // then
    assertThat(historicDecisionInstance.getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  void shouldSetRemovalTimeForBatch_BaseTimeStart() {
    // given
    String processInstanceId = testRule.process().serviceTask().deploy().start();
    historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");

    HistoricBatch historicBatch = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .singleResult();

    // assume
    assertThat(historicBatch.getRemovalTime()).isNull();

    ProcessEngineConfigurationImpl configuration = testRule.getProcessEngineConfiguration();
    configuration.setBatchOperationHistoryTimeToLive("P5D");
    configuration.initHistoryCleanup();

    HistoricBatchQuery query = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION);

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicBatch = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .singleResult();

    // then
    assertThat(historicBatch.getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  void shouldSetRemovalTimeInHierarchy_BaseTimeStart() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_START)
      .initHistoryRemovalTime();

    testRule.process().call().userTask().deploy().start();

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // assume
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLive("rootProcess", 5);

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().rootProcessInstances();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // then
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetRemovalTimeInHierarchyForStandaloneDecision_BaseTimeStart() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_START)
      .initHistoryRemovalTime();

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult();

    // assume
    assertThat(historicDecisionInstance.getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLiveDmn("dish-decision", 5);

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult();

    // then
    assertThat(historicDecisionInstance.getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  void shouldNotSetRemovalTime_BaseTimeEnd() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END)
      .initHistoryRemovalTime();

    testRule.process().ttl(5).userTask().deploy().start();

    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // assume
    assertThat(historicProcessInstance.getRemovalTime()).isNull();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // then
    assertThat(historicProcessInstance.getRemovalTime()).isNull();
  }

  @Test
  void shouldClearRemovalTime_BaseTimeEnd() {
    // given
    testRule.process().ttl(5).userTask().deploy().start();

    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // assume
    assertThat(historicProcessInstance.getRemovalTime()).isNotNull();

    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END)
      .initHistoryRemovalTime();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // then
    assertThat(historicProcessInstance.getRemovalTime()).isNull();
  }

  @Test
  void shouldNotSetRemovalTimeForBatch_BaseTimeEnd() {
    // given
    ProcessEngineConfigurationImpl configuration = testRule.getProcessEngineConfiguration();

    configuration
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END);

    String processInstanceId = testRule.process().serviceTask().deploy().start();
    historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");

    HistoricBatch historicBatch = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .singleResult();

    // assume
    assertThat(historicBatch.getRemovalTime()).isNull();

    configuration.setBatchOperationHistoryTimeToLive("P5D");
    configuration.initHistoryCleanup();

    HistoricBatchQuery query = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION);

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicBatch = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .singleResult();

    // then
    assertThat(historicBatch.getRemovalTime()).isNull();
  }

  @Test
  void shouldClearRemovalTimeForBatch_BaseTimeEnd() {
    // given
    ProcessEngineConfigurationImpl configuration = testRule.getProcessEngineConfiguration();
    configuration.setBatchOperationHistoryTimeToLive("P5D");
    configuration.initHistoryCleanup();

    String processInstanceId = testRule.process().serviceTask().deploy().start();
    historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");

    HistoricBatch historicBatch = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .singleResult();

    // assume
    assertThat(historicBatch.getRemovalTime()).isNotNull();

    configuration.setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END);

    HistoricBatchQuery query = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION);

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicBatch = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .singleResult();

    // then
    assertThat(historicBatch.getRemovalTime()).isNull();
  }

  @Test
  void shouldNotSetRemovalTimeInHierarchy_BaseTimeEnd() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END)
      .initHistoryRemovalTime();

    testRule.process().call().ttl(5).userTask().deploy().start();

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // assume
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isNull();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().rootProcessInstances();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // then
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isNull();
  }

  @Test
  void shouldClearRemovalTimeInHierarchy_BaseTimeEnd() {
    // given
    testRule.process().call().ttl(5).userTask().deploy().start();

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // assume
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNotNull();
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isNotNull();

    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END)
      .initHistoryRemovalTime();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().rootProcessInstances();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // then
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isNull();
  }

  @Test
  void shouldSetRemovalTime_BaseTimeEnd() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END)
      .initHistoryRemovalTime();

    testRule.process().serviceTask().deploy().start();

    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // assume
    assertThat(historicProcessInstance.getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLive("process", 5);

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // then
    assertThat(historicProcessInstance.getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetRemovalTimeForStandaloneDecision_BaseTimeEnd() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END)
      .initHistoryRemovalTime();

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance.getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLiveDmn("dish-decision", 5);

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .singleResult();

    // then
    assertThat(historicDecisionInstance.getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  void shouldSetRemovalTimeForBatch_BaseTimeEnd() {
    // given
    ProcessEngineConfigurationImpl configuration = testRule.getProcessEngineConfiguration();

    configuration
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END);

    String processInstanceId = testRule.process().serviceTask().deploy().start();
    Batch batch = historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");

    ClockUtil.setCurrentTime(addDays(currentDate, 1));

    testRule.syncExec(batch);

    HistoricBatch historicBatch = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .singleResult();

    // assume
    assertThat(historicBatch.getRemovalTime()).isNull();

    configuration.setBatchOperationHistoryTimeToLive("P5D");
    configuration.initHistoryCleanup();

    HistoricBatchQuery query = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION);

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicBatch = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .singleResult();

    // then
    assertThat(historicBatch.getRemovalTime()).isEqualTo(addDays(currentDate, 5+1));
  }

  @Test
  void shouldSetRemovalTimeInHierarchy_BaseTimeEnd() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END)
      .initHistoryRemovalTime();

    testRule.process().call().serviceTask().deploy().start();

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // assume
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLive("rootProcess", 5);

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().rootProcessInstances();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // then
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetRemovalTimeInHierarchyForStandaloneDecision_BaseTimeEnd() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END)
      .initHistoryRemovalTime();

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult();

    // assume
    assertThat(historicDecisionInstance.getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLiveDmn("dish-decision", 5);

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().rootDecisionInstancesOnly();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult();

    // then
    assertThat(historicDecisionInstance.getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  void shouldSetRemovalTime_Null() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_START)
      .initHistoryRemovalTime();

    testRule.process().ttl(5).userTask().deploy().start();

    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // assume
    assertThat(historicProcessInstance.getRemovalTime()).isEqualTo(addDays(currentDate, 5));

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .clearedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // then
    assertThat(historicProcessInstance.getRemovalTime()).isNull();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetRemovalTimeForStandaloneDecision_Null() {
    // given
    testRule.updateHistoryTimeToLiveDmn("dish-decision", 5);

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance.getRemovalTime()).isEqualTo(addDays(currentDate, 5));

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .clearedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .singleResult();

    // then
    assertThat(historicDecisionInstance.getRemovalTime()).isNull();
  }

  @Test
  void shouldSetRemovalTimeForBatch_Null() {
    // given
    ProcessEngineConfigurationImpl configuration = testRule.getProcessEngineConfiguration();

    configuration.setBatchOperationHistoryTimeToLive("P5D");
    configuration.initHistoryCleanup();

    String processInstanceId = testRule.process().serviceTask().deploy().start();
    historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");

    HistoricBatch historicBatch = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .singleResult();

    // assume
    assertThat(historicBatch.getRemovalTime()).isNotNull();

    HistoricBatchQuery query = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION);

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .clearedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    historicBatch = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .singleResult();

    // then
    assertThat(historicBatch.getRemovalTime()).isNull();
  }

  @Test
  void shouldSetRemovalTimeInHierarchy_Null() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_START)
      .initHistoryRemovalTime();

    testRule.process().call().ttl(5).userTask().deploy().start();

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // assume
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isEqualTo(addDays(currentDate, 5));

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().rootProcessInstances();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .clearedRemovalTime()
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // then
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isNull();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetRemovalTimeInHierarchyForStandaloneDecision_Null() {
    // given
    testRule.updateHistoryTimeToLiveDmn("dish-decision", 5);

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult();

    // assume
    assertThat(historicDecisionInstance.getRemovalTime()).isEqualTo(addDays(currentDate, 5));

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().rootDecisionInstancesOnly();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .clearedRemovalTime()
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult();

    // then
    assertThat(historicDecisionInstance.getRemovalTime()).isNull();
  }

  @Test
  void shouldSetRemovalTime_Absolute() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_START)
      .initHistoryRemovalTime();

    testRule.process().ttl(5).userTask().deploy().start();

    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // assume
    assertThat(historicProcessInstance.getRemovalTime()).isEqualTo(addDays(currentDate, 5));

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .absoluteRemovalTime(removalTime)
        .byQuery(query)
        .executeAsync()
    );

    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();

    // then
    assertThat(historicProcessInstance.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetRemovalTimeForStandaloneDecision_Absolute() {
    // given
    testRule.updateHistoryTimeToLiveDmn("dish-decision", 5);

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance.getRemovalTime()).isEqualTo(addDays(currentDate, 5));

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .absoluteRemovalTime(removalTime)
        .byQuery(query)
        .executeAsync()
    );

    historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .singleResult();

    // then
    assertThat(historicDecisionInstance.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldSetRemovalTimeForBatch_Absolute() {
    // given
    String processInstanceId = testRule.process().serviceTask().deploy().start();
    historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");

    HistoricBatch historicBatch = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .singleResult();

    // assume
    assertThat(historicBatch.getRemovalTime()).isNull();

    HistoricBatchQuery query = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION);

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .absoluteRemovalTime(removalTime)
        .byQuery(query)
        .executeAsync()
    );

    historicBatch = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .singleResult();

    // then
    assertThat(historicBatch.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldSetRemovalTimeInHierarchy_Absolute() {
    // given
    testRule.getProcessEngineConfiguration()
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_START)
      .initHistoryRemovalTime();

    testRule.process().call().ttl(5).userTask().deploy().start();

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // assume
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isEqualTo(addDays(currentDate, 5));

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().rootProcessInstances();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .absoluteRemovalTime(removalTime)
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // then
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetRemovalTimeInHierarchyForStandaloneDecision_Absolute() {
    // given
    testRule.updateHistoryTimeToLiveDmn("dish-decision", 5);

    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult();

    // assume
    assertThat(historicDecisionInstance.getRemovalTime()).isEqualTo(addDays(currentDate, 5));

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().rootDecisionInstancesOnly();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .absoluteRemovalTime(removalTime)
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult();

    // then
    assertThat(historicDecisionInstance.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldSetRemovalTimeInHierarchy_ByChildInstance() {
    // given
    String rootProcessInstance = testRule.process().call().ttl(5).userTask().deploy().start();

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // assume
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isEqualTo(addDays(currentDate, 5));

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
      .superProcessInstanceId(rootProcessInstance);

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .absoluteRemovalTime(removalTime)
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // then
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetRemovalTimeInHierarchyForStandaloneDecision_ByChildInstance() {
    // given
    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult();

    // assume
    assertThat(historicDecisionInstance.getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLiveDmn("dish-decision", 5);

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery()
      .decisionInstanceId(historicDecisionInstance.getId());

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .hierarchical()
        .executeAsync()
    );

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  void shouldSetRemovalTime_ByIds() {
    // given
    testRule.process().call().userTask().deploy().start();

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // assume
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLive(5, "process", "rootProcess");

    List<String> ids = new ArrayList<>();
    for (HistoricProcessInstance historicProcessInstance : historicProcessInstances) {
      ids.add(historicProcessInstance.getId());
    }

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byIds(ids.toArray(new String[0]))
        .executeAsync()
    );

    historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // then
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  void shouldThrowBadUserRequestException_NotExistingIds() {
    // given
    var setRemovalTimeToHistoricProcessInstancesBuilder = historyService.setRemovalTimeToHistoricProcessInstances()
      .absoluteRemovalTime(removalTime)
      .byIds("aNotExistingId", "anotherNotExistingId");

    // when/then
    assertThatThrownBy(setRemovalTimeToHistoricProcessInstancesBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("historicProcessInstances is empty");
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetRemovalTimeForStandaloneDecision_ByIds() {
    // given
    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // assume
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLiveDmn(5, "dish-decision", "season", "guestCount");

    List<String> ids = new ArrayList<>();
    for (HistoricDecisionInstance historicDecisionInstance : historicDecisionInstances) {
      ids.add(historicDecisionInstance.getId());
    }

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .calculatedRemovalTime()
        .byIds(ids.toArray(new String[0]))
        .executeAsync()
    );

    historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  void shouldThrowBadUserRequestExceptionForStandaloneDecision_NotExistingIds() {
    // given
    var setRemovalTimeToHistoricDecisionInstancesBuilder = historyService.setRemovalTimeToHistoricDecisionInstances()
      .absoluteRemovalTime(removalTime)
      .byIds("aNotExistingId", "anotherNotExistingId");

    // when/then
    assertThatThrownBy(setRemovalTimeToHistoricDecisionInstancesBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("historicDecisionInstances is empty");
  }

  @Test
  void shouldSetRemovalTimeForBatch_ByIds() {
    // given
    String processInstanceId = testRule.process().serviceTask().deploy().start();

    historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");
    historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");

    List<HistoricBatch> historicBatches = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .list();

    // assume
    assertThat(historicBatches.get(0).getRemovalTime()).isNull();
    assertThat(historicBatches.get(1).getRemovalTime()).isNull();

    List<String> ids = new ArrayList<>();
    for (HistoricBatch historicBatch : historicBatches) {
      ids.add(historicBatch.getId());
    }

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .absoluteRemovalTime(removalTime)
        .byIds(ids.toArray(new String[0]))
        .executeAsync()
    );

    historicBatches = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .list();

    // then
    assertThat(historicBatches.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicBatches.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldThrowBadUserRequestExceptionForBatch_NotExistingIds() {
    // given
    var setRemovalTimeToHistoricBatchesBuilder = historyService.setRemovalTimeToHistoricBatches()
      .absoluteRemovalTime(removalTime)
      .byIds("aNotExistingId", "anotherNotExistingId");

    // when/then
    assertThatThrownBy(setRemovalTimeToHistoricBatchesBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("historicBatches is empty");
  }

  @Test
  void shouldThrowBadUserRequestException() {
    // given
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
    var setRemovalTimeToHistoricProcessInstancesBuilder = historyService.setRemovalTimeToHistoricProcessInstances()
      .absoluteRemovalTime(removalTime)
      .byQuery(query);

    // when/then
    assertThatThrownBy(setRemovalTimeToHistoricProcessInstancesBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("historicProcessInstances is empty");
  }

  @Test
  void shouldThrowBadUserRequestExceptionForStandaloneDecision() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();
    var setRemovalTimeToHistoricDecisionInstancesBuilder = historyService.setRemovalTimeToHistoricDecisionInstances()
      .absoluteRemovalTime(removalTime)
      .byQuery(query);

    // when/then
    assertThatThrownBy(setRemovalTimeToHistoricDecisionInstancesBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("historicDecisionInstances is empty");
  }

  @Test
  void shouldThrowBadUserRequestExceptionForBatch() {
    // given
    HistoricBatchQuery query = historyService.createHistoricBatchQuery();
    var setRemovalTimeToHistoricBatchesBuilder = historyService.setRemovalTimeToHistoricBatches()
      .absoluteRemovalTime(removalTime)
      .byQuery(query);

    // when/then
    assertThatThrownBy(setRemovalTimeToHistoricBatchesBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("historicBatches is empty");
  }

  @Test
  void shouldProduceHistory() {
    // given
    testRule.process().serviceTask().deploy().start();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    HistoricBatch historicBatch = historyService.createHistoricBatchQuery().singleResult();

    // then
    assertThat(historicBatch.getType()).isEqualTo("process-set-removal-time");
    assertThat(historicBatch.getStartTime()).isEqualTo(currentDate);
    assertThat(historicBatch.getEndTime()).isEqualTo(currentDate);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldProduceHistoryForStandaloneDecision() {
    // given
    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    HistoricBatch historicBatch = historyService.createHistoricBatchQuery().singleResult();

    // then
    assertThat(historicBatch.getType()).isEqualTo("decision-set-removal-time");
    assertThat(historicBatch.getStartTime()).isEqualTo(currentDate);
    assertThat(historicBatch.getEndTime()).isEqualTo(currentDate);
  }

  @Test
  void shouldProduceHistoryForBatch() {
    // given
    String processInstanceId = testRule.process().serviceTask().deploy().start();
    Batch batch = historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");

    testRule.syncExec(batch);

    HistoricBatchQuery query = historyService.createHistoricBatchQuery();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .calculatedRemovalTime()
        .byQuery(query)
        .executeAsync()
    );

    HistoricBatch historicBatch = historyService.createHistoricBatchQuery()
      .type("batch-set-removal-time")
      .singleResult();

    // then
    assertThat(historicBatch.getStartTime()).isEqualTo(currentDate);
    assertThat(historicBatch.getEndTime()).isEqualTo(currentDate);
  }

  @Test
  void shouldThrowExceptionIfNoRemovalTimeSettingDefined()
  {
    // given
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

    SetRemovalTimeToHistoricProcessInstancesBuilder batchBuilder = historyService.setRemovalTimeToHistoricProcessInstances()
      .byQuery(query);

    // when/then
    assertThatThrownBy(batchBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("removalTime is null");
  }

  @Test
  void shouldThrowExceptionIfNoRemovalTimeSettingDefinedForStandaloneDecision() {
    // given
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    SetRemovalTimeToHistoricDecisionInstancesBuilder batchBuilder = historyService.setRemovalTimeToHistoricDecisionInstances()
      .byQuery(query);

    // when/then
    assertThatThrownBy(batchBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("removalTime is null");
  }

  @Test
  void shouldThrowExceptionIfNoRemovalTimeSettingDefinedForBatch() {
    // given
    HistoricBatchQuery query = historyService.createHistoricBatchQuery();

    SetRemovalTimeToHistoricBatchesBuilder batchBuilder = historyService.setRemovalTimeToHistoricBatches()
      .byQuery(query);

    // when/then
    assertThatThrownBy(batchBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("removalTime is null");
  }

  @Test
  void shouldThrowExceptionIfNoQueryAndNoIdsDefined()
  {
    // given
    SetRemovalTimeToHistoricProcessInstancesBuilder batchBuilder = historyService.setRemovalTimeToHistoricProcessInstances()
      .absoluteRemovalTime(new Date());

    // when/then
    assertThatThrownBy(batchBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Neither query nor ids provided.");
  }

  @Test
  void shouldThrowExceptionIfNoQueryAndNoIdsDefinedForStandaloneDecision()
  {
    // given
    SetRemovalTimeToHistoricDecisionInstancesBuilder batchBuilder = historyService.setRemovalTimeToHistoricDecisionInstances()
      .absoluteRemovalTime(new Date());

    // when/then
    assertThatThrownBy(batchBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Neither query nor ids provided.");
  }

  @Test
  void shouldThrowExceptionIfNoQueryAndNoIdsDefinedForBatch()
  {
    // given
    SetRemovalTimeToHistoricBatchesBuilder batchBuilder = historyService.setRemovalTimeToHistoricBatches()
      .absoluteRemovalTime(new Date());

    // when/then
    assertThatThrownBy(batchBuilder::executeAsync)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Neither query nor ids provided.");
  }

  @Test
  void shouldSetRemovalTime_BothQueryAndIdsDefined() {
    // given
    String rootProcessInstanceId = testRule.process().call().userTask().deploy().start();

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // assume
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLive(5, "rootProcess", "process");

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
      .superProcessInstanceId(rootProcessInstanceId);

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .byIds(rootProcessInstanceId)
        .executeAsync()
    );

    historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // then
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
    assertThat(historicProcessInstances.get(1).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetRemovalTimeForStandaloneDecision_BothQueryAndIdsDefined() {
    // given
    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKeyIn("season", "dish-decision")
      .list();

    // assume
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLiveDmn( 5, "dish-decision", "season");

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("dish-decision");

    String id = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult()
      .getId();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .calculatedRemovalTime()
        .byQuery(query)
        .byIds(id)
        .executeAsync()
    );

    historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKeyIn("season", "dish-decision")
      .list();

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  void shouldSetRemovalTimeForBatch_BothQueryAndIdsDefined() {
    // given
    String processInstanceId = testRule.process().serviceTask().deploy().start();
    Batch batchOne = historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");
    Batch batchTwo = historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");

    List<HistoricBatch> historicBatches = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .list();

    // assume
    assertThat(historicBatches.get(0).getRemovalTime()).isNull();
    assertThat(historicBatches.get(1).getRemovalTime()).isNull();

    ProcessEngineConfigurationImpl configuration = testRule.getProcessEngineConfiguration();
    configuration.setBatchOperationHistoryTimeToLive("P5D");
    configuration.initHistoryCleanup();

    HistoricBatchQuery query = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .batchId(batchOne.getId());

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .calculatedRemovalTime()
        .byQuery(query)
        .byIds(batchTwo.getId())
        .executeAsync()
    );

    historicBatches = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .list();

    // then
    assertThat(historicBatches.get(0).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
    assertThat(historicBatches.get(1).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  void shouldSetRemovalTime_ExistingAndNotExistingId() {
    // given
    String processInstanceId = testRule.process().userTask().deploy().start();

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // assume
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLive(5, "process");

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .calculatedRemovalTime()
        .byIds("notExistingId", processInstanceId)
        .executeAsync()
    );

    historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();

    // then
    assertThat(historicProcessInstances.get(0).getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetRemovalTimeForStandaloneDecision_ExistingAndNotExistingId() {
    // given
    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKeyIn("season")
      .singleResult();

    // assume
    assertThat(historicDecisionInstance.getRemovalTime()).isNull();

    testRule.updateHistoryTimeToLiveDmn( 5, "season");

    String id = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("season")
      .singleResult()
      .getId();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .calculatedRemovalTime()
        .byIds("notExistingId", id)
        .executeAsync()
    );

    historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKeyIn("season")
      .singleResult();

    // then
    assertThat(historicDecisionInstance.getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  void shouldSetRemovalTimeForBatch_ExistingAndNotExistingId() {
    // given
    String processInstanceId = testRule.process().serviceTask().deploy().start();
    Batch batchOne = historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");

    HistoricBatch historicBatch = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .singleResult();

    // assume
    assertThat(historicBatch.getRemovalTime()).isNull();

    ProcessEngineConfigurationImpl configuration = testRule.getProcessEngineConfiguration();
    configuration.setBatchOperationHistoryTimeToLive("P5D");
    configuration.initHistoryCleanup();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .calculatedRemovalTime()
        .byIds("notExistingId", batchOne.getId())
        .executeAsync()
    );

    historicBatch = historyService.createHistoricBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .singleResult();

    // then
    assertThat(historicBatch.getRemovalTime()).isEqualTo(addDays(currentDate, 5));
  }

  @Test
  void ThrowBadUserRequestException_SelectMultipleModes_ModeCleared() {
    // given
    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builder = historyService.setRemovalTimeToHistoricProcessInstances();
    builder.calculatedRemovalTime();

    // when/then
    assertThatThrownBy(builder::clearedRemovalTime)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("The removal time modes are mutually exclusive: mode is not null");
  }

  @Test
  void ThrowBadUserRequestException_SelectMultipleModes_ModeAbsolute() {
    // given
    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builder = historyService.setRemovalTimeToHistoricProcessInstances();
    builder.calculatedRemovalTime();
    Date absoluteRemovalTime = new Date();

    // when/then
    assertThatThrownBy(() -> builder.absoluteRemovalTime(absoluteRemovalTime))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("The removal time modes are mutually exclusive: mode is not null");
  }

  @Test
  void ThrowBadUserRequestExceptionForStandaloneDecision_SelectMultipleModes_ModeCleared() {
    // given
    SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder builder = historyService.setRemovalTimeToHistoricDecisionInstances();
    builder.calculatedRemovalTime();

    // when/then
    assertThatThrownBy(builder::clearedRemovalTime)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("The removal time modes are mutually exclusive: mode is not null");
  }

  @Test
  void ThrowBadUserRequestExceptionForStandaloneDecision_SelectMultipleModes_ModeAbsolute() {
    // given
    SetRemovalTimeSelectModeForHistoricDecisionInstancesBuilder builder = historyService.setRemovalTimeToHistoricDecisionInstances();
    builder.calculatedRemovalTime();
    Date absoluteRemovalTime = new Date();

    // when/then
    assertThatThrownBy(() -> builder.absoluteRemovalTime(absoluteRemovalTime))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("The removal time modes are mutually exclusive: mode is not null");
  }

  @Test
  void ThrowBadUserRequestExceptionForBatch_SelectMultipleModes_ModeCleared() {
    // given
    SetRemovalTimeSelectModeForHistoricBatchesBuilder builder = historyService.setRemovalTimeToHistoricBatches();
    builder.calculatedRemovalTime();

    // when/then
    assertThatThrownBy(builder::clearedRemovalTime)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("The removal time modes are mutually exclusive: mode is not null");
  }

  @Test
  void ThrowBadUserRequestExceptionForBatch_SelectMultipleModes_ModeAbsolute() {
    // given
    SetRemovalTimeSelectModeForHistoricBatchesBuilder builder = historyService.setRemovalTimeToHistoricBatches();
    builder.calculatedRemovalTime();
    Date absoluteRemovalTime = new Date();

    // when/then
    assertThatThrownBy(() -> builder.absoluteRemovalTime(absoluteRemovalTime))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("The removal time modes are mutually exclusive: mode is not null");
  }

  @Test
  void shouldSeeCleanableButNotFinishedProcessInstanceInReport() {
    // given
    String processInstanceId = testRule.process().userTask().deploy().start();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .absoluteRemovalTime(currentDate)
        .byIds(processInstanceId)
        .executeAsync()
    );

    CleanableHistoricProcessInstanceReportResult report = historyService.createCleanableHistoricProcessInstanceReport().singleResult();

    // then
    assertThat(report.getFinishedProcessInstanceCount()).isZero();
    assertThat(report.getCleanableProcessInstanceCount()).isOne();
    assertThat(report.getHistoryTimeToLive()).isNull();
  }

  @Test
  void shouldSeeCleanableAndFinishedProcessInstanceInReport() {
    // given
    String processInstanceId = testRule.process().serviceTask().deploy().start();

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricProcessInstances()
        .absoluteRemovalTime(currentDate)
        .byIds(processInstanceId)
        .executeAsync()
    );

    CleanableHistoricProcessInstanceReportResult report = historyService.createCleanableHistoricProcessInstanceReport().singleResult();

    // then
    assertThat(report.getFinishedProcessInstanceCount()).isOne();
    assertThat(report.getCleanableProcessInstanceCount()).isOne();
    assertThat(report.getHistoryTimeToLive()).isNull();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSeeCleanableAndFinishedDecisionInstanceInReport() {
    // given
    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery()
      .decisionDefinitionKey("dish-decision");

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricDecisionInstances()
        .absoluteRemovalTime(currentDate)
        .byQuery(query)
        .executeAsync()
    );

    CleanableHistoricDecisionInstanceReportResult report = historyService.createCleanableHistoricDecisionInstanceReport()
      .decisionDefinitionKeyIn("dish-decision")
      .singleResult();

    // then
    assertThat(report.getFinishedDecisionInstanceCount()).isOne();
    assertThat(report.getCleanableDecisionInstanceCount()).isOne();
    assertThat(report.getHistoryTimeToLive()).isNull();
  }

  @Test
  void shouldSeeCleanableButNotFinishedBatchInReport() {
    // given
    String processInstanceId = testRule.process().serviceTask().deploy().start();
    Batch batchOne = historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .absoluteRemovalTime(currentDate)
        .byIds(batchOne.getId())
        .executeAsync()
    );

    testRule.clearDatabase();

    CleanableHistoricBatchReportResult report = historyService.createCleanableHistoricBatchReport().singleResult();

    // then
    assertThat(report.getFinishedBatchesCount()).isZero();
    assertThat(report.getCleanableBatchesCount()).isOne();
    assertThat(report.getHistoryTimeToLive()).isNull();
  }

  @Test
  void shouldSeeCleanableAndFinishedBatchInReport() {
    // given
    String processInstanceId = testRule.process().serviceTask().deploy().start();
    Batch batchOne = historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");

    testRule.syncExec(batchOne, false);

    // when
    testRule.syncExec(
      historyService.setRemovalTimeToHistoricBatches()
        .absoluteRemovalTime(currentDate)
        .byIds(batchOne.getId())
        .executeAsync()
    );

    testRule.clearDatabase();

    CleanableHistoricBatchReportResult report = historyService.createCleanableHistoricBatchReport().singleResult();

    // then
    assertThat(report.getFinishedBatchesCount()).isOne();
    assertThat(report.getCleanableBatchesCount()).isOne();
    assertThat(report.getHistoryTimeToLive()).isNull();
  }

  @Test
  void shouldSetInvocationsPerBatchTypeForProcesses() {
    // given
    engineRule.getProcessEngineConfiguration()
        .getInvocationsPerBatchJobByBatchType()
        .put(Batch.TYPE_PROCESS_SET_REMOVAL_TIME, 42);

    testRule.process().serviceTask().deploy().start();

    // when
    Batch batch = historyService.setRemovalTimeToHistoricProcessInstances()
        .absoluteRemovalTime(currentDate)
        .byQuery(historyService.createHistoricProcessInstanceQuery())
        .executeAsync();

    // then
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(42);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldSetInvocationsPerBatchTypeForDecisions() {
    // given
    engineRule.getProcessEngineConfiguration()
        .getInvocationsPerBatchJobByBatchType()
        .put(Batch.TYPE_DECISION_SET_REMOVAL_TIME, 42);

    decisionService.evaluateDecisionByKey("dish-decision")
        .variables(
            Variables.createVariables()
                .putValue("temperature", 32)
                .putValue("dayType", "Weekend")
        ).evaluate();

    // when
    Batch batch = historyService.setRemovalTimeToHistoricDecisionInstances()
        .absoluteRemovalTime(currentDate)
        .byQuery(historyService.createHistoricDecisionInstanceQuery())
        .executeAsync();

    // then
    assertThat(batch.getInvocationsPerBatchJob()).isEqualTo(42);
  }

  @Test
  void shouldSetInvocationsPerBatchTypeForBatches() {
    // given
    engineRule.getProcessEngineConfiguration()
        .getInvocationsPerBatchJobByBatchType()
        .put(Batch.TYPE_BATCH_SET_REMOVAL_TIME, 42);

    String processInstanceId = testRule.process().serviceTask().deploy().start();
    Batch batchOne = historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");

    testRule.syncExec(batchOne, false);

    // when
    Batch batchTwo = historyService.setRemovalTimeToHistoricBatches()
        .absoluteRemovalTime(currentDate)
        .byQuery(historyService.createHistoricBatchQuery())
        .executeAsync();

    // then
    assertThat(batchTwo.getInvocationsPerBatchJob()).isEqualTo(42);
  }

  @Test
  void shouldSetExecutionStartTimeInBatchAndHistoryForBatches() {
    // given
    ClockUtil.setCurrentTime(currentDate);
    String processInstanceId = testRule.process().serviceTask().deploy().start();
    Batch batchDelete = historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "");
    testRule.syncExec(batchDelete, false);
    Batch batch = historyService.setRemovalTimeToHistoricBatches()
        .absoluteRemovalTime(currentDate)
        .byQuery(historyService.createHistoricBatchQuery())
        .executeAsync();
    helper.executeSeedJob(batch);
    List<Job> executionJobs = helper.getExecutionJobs(batch, Batch.TYPE_BATCH_SET_REMOVAL_TIME);
    historyService.deleteHistoricBatch(batchDelete.getId());

    // when
    helper.executeJob(executionJobs.get(0));

    // then
    HistoricBatch historicBatch = historyService.createHistoricBatchQuery().singleResult();
    batch = managementService.createBatchQuery().singleResult();

    assertThat(batch.getExecutionStartTime()).isCloseTo(currentDate, 1000);
    assertThat(historicBatch.getExecutionStartTime()).isCloseTo(currentDate, 1000);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml")
  void shouldSetExecutionStartTimeInBatchAndHistoryForDecisions() {
    // given
    ClockUtil.setCurrentTime(currentDate);
    decisionService.evaluateDecisionByKey("dish-decision")
        .variables(
            Variables.createVariables()
                .putValue("temperature", 32)
                .putValue("dayType", "Weekend")
        ).evaluate();
    Batch batch = historyService.setRemovalTimeToHistoricDecisionInstances()
        .absoluteRemovalTime(currentDate)
        .byQuery(historyService.createHistoricDecisionInstanceQuery())
        .executeAsync();
    helper.executeSeedJob(batch);
    List<Job> executionJobs = helper.getExecutionJobs(batch, Batch.TYPE_DECISION_SET_REMOVAL_TIME);

    // when
    helper.executeJob(executionJobs.get(0));

    // then
    HistoricBatch historicBatch = historyService.createHistoricBatchQuery().singleResult();
    batch = managementService.createBatchQuery().singleResult();

    assertThat(batch.getExecutionStartTime()).isCloseTo(currentDate, 1000);
    assertThat(historicBatch.getExecutionStartTime()).isCloseTo(currentDate, 1000);
  }

}
