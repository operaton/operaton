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
package org.operaton.bpm.engine.test.api.authorization.history;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.history.HistoricCaseInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.metrics.Meter;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricIncidentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;
import org.operaton.bpm.engine.test.dmn.businessruletask.TestPojo;
import org.operaton.bpm.engine.variable.Variables;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_CLEANUP_STRATEGY_END_TIME_BASED;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED;

import java.util.*;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class HistoryCleanupAuthorizationTest extends AuthorizationTest {

  @Before
  @Override
  public void setUp() {
    processEngineConfiguration.setHistoryCleanupStrategy(HISTORY_CLEANUP_STRATEGY_END_TIME_BASED);
    super.setUp();
  }

  @After
  @Override
  public void tearDown() {
    processEngineConfiguration.setHistoryCleanupStrategy(HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED);

    super.tearDown();
    clearDatabase();
    clearMetrics();
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRef.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml", "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn" })
  public void testHistoryCleanupWithAuthorization() {
    // given
    prepareInstances(5, 5, 5);

    ClockUtil.setCurrentTime(new Date());
    // when
    identityService.setAuthentication("user", Collections.singletonList(Groups.OPERATON_ADMIN), null);

    String jobId = historyService.cleanUpHistoryAsync(true).getId();

    managementService.executeJob(jobId);

    // then
    assertResult(0);
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRef.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml", "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn" })
  public void testHistoryCleanupWithoutAuthorization() {
    // given
    prepareInstances(5, 5, 5);

    ClockUtil.setCurrentTime(new Date());

    assertThatThrownBy(() -> historyService.cleanUpHistoryAsync(true))
        .withFailMessage("Exception expected: It should not be possible to execute the history cleanup")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining("ENGINE-03029 Required admin authenticated group or user.");
  }

  protected void prepareInstances(Integer processInstanceTimeToLive, Integer decisionTimeToLive, Integer caseTimeToLive) {
    // update time to live
    disableAuthorization();
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().processDefinitionKey("testProcess").list();
    assertThat(processDefinitions).hasSize(1);
    repositoryService.updateProcessDefinitionHistoryTimeToLive(processDefinitions.get(0).getId(), processInstanceTimeToLive);

    final List<DecisionDefinition> decisionDefinitions = repositoryService.createDecisionDefinitionQuery().decisionDefinitionKey("testDecision").list();
    assertThat(decisionDefinitions).hasSize(1);
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinitions.get(0).getId(), decisionTimeToLive);

    List<CaseDefinition> caseDefinitions = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase").list();
    assertThat(caseDefinitions).hasSize(1);
    repositoryService.updateCaseDefinitionHistoryTimeToLive(caseDefinitions.get(0).getId(), caseTimeToLive);

    Date oldCurrentTime = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(oldCurrentTime, -6));

    // create 3 process instances
    List<String> processInstanceIds = new ArrayList<>();
    Map<String, Object> variables = Variables.createVariables().putValue("pojo", new TestPojo("okay", 13.37));
    for (int i = 0; i < 3; i++) {
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess", variables);
      processInstanceIds.add(processInstance.getId());
    }
    runtimeService.deleteProcessInstances(processInstanceIds, null, true, true);

    // +10 standalone decisions
    for (int i = 0; i < 10; i++) {
      decisionService.evaluateDecisionByKey("testDecision").variables(variables).evaluate();
    }

    // create 4 case instances
    for (int i = 0; i < 4; i++) {
      CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase",
          Variables.createVariables().putValue("pojo", new TestPojo("okay", 13.37 + i)));
      caseService.terminateCaseExecution(caseInstance.getId());
      caseService.closeCaseInstance(caseInstance.getId());
    }

    ClockUtil.setCurrentTime(oldCurrentTime);
    enableAuthorization();

  }

  protected void assertResult(long expectedInstanceCount) {
    long count = historyService.createHistoricProcessInstanceQuery().count() + historyService.createHistoricDecisionInstanceQuery().count()
        + historyService.createHistoricCaseInstanceQuery().count();
    assertThat(count).isEqualTo(expectedInstanceCount);
  }

  protected void clearDatabase() {
    // reset configuration changes
    String defaultStartTime = processEngineConfiguration.getHistoryCleanupBatchWindowStartTime();
    String defaultEndTime = processEngineConfiguration.getHistoryCleanupBatchWindowEndTime();
    int defaultBatchSize = processEngineConfiguration.getHistoryCleanupBatchSize();

    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime(defaultStartTime);
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime(defaultEndTime);
    processEngineConfiguration.setHistoryCleanupBatchSize(defaultBatchSize);

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      List<Job> jobs = managementService.createJobQuery().list();
      if (!jobs.isEmpty()) {
        assertThat(jobs).hasSize(1);
        String jobId = jobs.get(0).getId();
        commandContext.getJobManager().deleteJob((JobEntity) jobs.get(0));
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(jobId);
      }

      List<HistoricIncident> historicIncidents = historyService.createHistoricIncidentQuery().list();
      for (HistoricIncident historicIncident : historicIncidents) {
        commandContext.getDbEntityManager().delete((HistoricIncidentEntity) historicIncident);
      }

      commandContext.getMeterLogManager().deleteAll();

      return null;
    });

    List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();
    for (HistoricProcessInstance historicProcessInstance : historicProcessInstances) {
      historyService.deleteHistoricProcessInstance(historicProcessInstance.getId());
    }

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();
    for (HistoricDecisionInstance historicDecisionInstance : historicDecisionInstances) {
      historyService.deleteHistoricDecisionInstanceByInstanceId(historicDecisionInstance.getId());
    }

    List<HistoricCaseInstance> historicCaseInstances = historyService.createHistoricCaseInstanceQuery().list();
    for (HistoricCaseInstance historicCaseInstance : historicCaseInstances) {
      historyService.deleteHistoricCaseInstance(historicCaseInstance.getId());
    }
  }

  protected void clearMetrics() {
    Collection<Meter> meters = processEngineConfiguration.getMetricsRegistry().getDbMeters().values();
    for (Meter meter : meters) {
      meter.getAndClear();
    }
    managementService.deleteMetrics(null);
  }
}
