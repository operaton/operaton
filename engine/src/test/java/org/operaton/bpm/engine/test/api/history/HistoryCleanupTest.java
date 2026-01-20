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
package org.operaton.bpm.engine.test.api.history;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricCaseInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.cfg.BatchWindowConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.HistoryCleanupCmd;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.HistoryCleanupHelper;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.HistoryCleanupJobHandlerConfiguration;
import org.operaton.bpm.engine.impl.metrics.Meter;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.impl.util.ExceptionUtil;
import org.operaton.bpm.engine.impl.util.JsonUtil;
import org.operaton.bpm.engine.impl.util.ParseUtil;
import org.operaton.bpm.engine.management.MetricIntervalValue;
import org.operaton.bpm.engine.management.Metrics;
import org.operaton.bpm.engine.management.MetricsQuery;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.dmn.businessruletask.TestPojo;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.Removable;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_CLEANUP_STRATEGY_END_TIME_BASED;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.CATEGORY_OPERATOR;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.OPERATION_TYPE_CREATE_HISTORY_CLEANUP_JOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Svetlana Dorokhova
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoryCleanupTest {
  private static final int PROCESS_INSTANCES_COUNT = 3;
  private static final int DECISIONS_IN_PROCESS_INSTANCES = 3;
  private static final int DECISION_INSTANCES_COUNT = 10;
  private static final int CASE_INSTANCES_COUNT = 4;
  private static final int HISTORY_TIME_TO_LIVE = 5;
  private static final int DAYS_IN_THE_PAST = -6;

  protected static final String ONE_TASK_PROCESS = "oneTaskProcess";
  protected static final String DECISION = "decision";
  protected static final String ONE_TASK_CASE = "case";

  private static final int NUMBER_OF_THREADS = 3;
  private static final String USER_ID = "demo";

  private static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
  private static final Date targetDate = new Date(Instant.parse("2025-01-01T00:00:00Z").toEpochMilli());

  private static Date parseDate(String dateString) {
    try {
      LocalDateTime localDateTime = LocalDateTime.parse(dateString, sdf);
      return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    } catch (DateTimeParseException e) {
      throw new RuntimeException("Failed to parse date: " + dateString, e);
    }
  }

  private static String formatTime(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).format(timeFormatter);
  }

  protected String defaultStartTime;
  protected String defaultEndTime;
  protected int defaultBatchSize;

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .closeEngineAfterAllTests()
    .randomEngineName()
    .configurator(configuration -> {
      configuration.setHistoryCleanupBatchSize(20);
      configuration.setHistoryCleanupBatchThreshold(10);
      configuration.setDefaultNumberOfRetries(5);
      configuration.setHistoryCleanupDegreeOfParallelism(NUMBER_OF_THREADS);
    }).build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected Removable removable;

  private final Random random = new Random();

  private HistoryService historyService;
  private RuntimeService runtimeService;
  private ManagementService managementService;
  private CaseService caseService;
  private RepositoryService repositoryService;
  private IdentityService identityService;
  private ProcessEngineConfigurationImpl processEngineConfiguration;


  @BeforeEach
  void init() {
    testRule.deploy("org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml", "org/operaton/bpm/engine/test/api/dmn/Example.dmn", "org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithHistoryTimeToLive.cmmn");
    defaultStartTime = processEngineConfiguration.getHistoryCleanupBatchWindowStartTime();
    defaultEndTime = processEngineConfiguration.getHistoryCleanupBatchWindowEndTime();
    defaultBatchSize = processEngineConfiguration.getHistoryCleanupBatchSize();
    processEngineConfiguration.setHistoryCleanupStrategy(HISTORY_CLEANUP_STRATEGY_END_TIME_BASED);

    identityService.setAuthenticatedUserId(USER_ID);
    removable = Removable.of(testRule.getProcessEngine());

  }

  @AfterEach
  void clearDatabase() {
    //reset configuration changes
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime(defaultStartTime);
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime(defaultEndTime);
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTimeAsDate(null);
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTimeAsDate(null);
    processEngineConfiguration.setHistoryCleanupBatchSize(defaultBatchSize);
    processEngineConfiguration.setHistoryCleanupStrategy(HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED);
    processEngineConfiguration.setHistoryCleanupEnabled(true);
    processEngineConfiguration.setHistoryCleanupDefaultNumberOfRetries(Integer.MIN_VALUE);
    processEngineConfiguration.setHistoryTimeToLive(null);
    processEngineConfiguration.setDefaultNumberOfRetries(5);
    processEngineConfiguration.setHistoryCleanupDegreeOfParallelism(NUMBER_OF_THREADS);
    processEngineConfiguration.setHistoryCleanupBatchThreshold(10);

    removable.removeAll();
    clearMetrics();
    identityService.clearAuthentication();
  }

  protected void clearMetrics() {
    Collection<Meter> meters = processEngineConfiguration.getMetricsRegistry().getDbMeters().values();
    for (Meter meter : meters) {
      meter.getAndClear();
    }
    managementService.deleteMetrics(null);
  }

  @Test
  void testHistoryCleanupManualRun() {
    //given
    prepareData(15);

    ClockUtil.setCurrentTime(targetDate);
    //when
    runHistoryCleanup(true);

    //then
    assertResult(0);


    List<UserOperationLogEntry> userOperationLogEntries = historyService
      .createUserOperationLogQuery()
      .operationType(OPERATION_TYPE_CREATE_HISTORY_CLEANUP_JOB)
      .list();

    assertThat(userOperationLogEntries).hasSize(1);

    UserOperationLogEntry entry = userOperationLogEntries.get(0);
    assertThat(entry.getCategory()).isEqualTo(CATEGORY_OPERATOR);
  }

  @Test
  void shouldThrowExceptionWhenCleanupDisabled_1() {
    // given
    processEngineConfiguration.setHistoryCleanupEnabled(false);

    // when/then
    assertThatThrownBy(() -> historyService.cleanUpHistoryAsync())
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("History cleanup is disabled for this engine");
  }

  @Test
  void shouldThrowExceptionWhenCleanupDisabled_2() {
    // given
    processEngineConfiguration.setHistoryCleanupEnabled(false);

    // when/then
    assertThatThrownBy(() -> historyService.cleanUpHistoryAsync(true))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("History cleanup is disabled for this engine");
  }

  @Test
  void testDataSplitBetweenThreads() {
    //given
    prepareData(15);

    ClockUtil.setCurrentTime(targetDate);

    //when
    historyService.cleanUpHistoryAsync(true).getId();
    for (Job job : historyService.findHistoryCleanupJobs()) {
      managementService.executeJob(job.getId());
      //assert that the corresponding data was removed
      final HistoryCleanupJobHandlerConfiguration jobHandlerConfiguration = getHistoryCleanupJobHandlerConfiguration(job);
      final int minuteFrom = jobHandlerConfiguration.getMinuteFrom();
      final int minuteTo = jobHandlerConfiguration.getMinuteTo();

      final List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();
      for (HistoricProcessInstance historicProcessInstance: historicProcessInstances) {
        if (historicProcessInstance.getEndTime() != null) {
          Calendar calendar = Calendar.getInstance();
          calendar.setTime(historicProcessInstance.getEndTime());
          assertThat(minuteFrom > calendar.get(Calendar.MINUTE) || calendar.get(Calendar.MINUTE) > minuteTo).isTrue();
        }
      }

      final List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();
      for (HistoricDecisionInstance historicDecisionInstance: historicDecisionInstances) {
        if (historicDecisionInstance.getEvaluationTime() != null) {
          Calendar calendar = Calendar.getInstance();
          calendar.setTime(historicDecisionInstance.getEvaluationTime());
          assertThat(minuteFrom > calendar.get(Calendar.MINUTE) || calendar.get(Calendar.MINUTE) > minuteTo).isTrue();
        }
      }

      final List<HistoricCaseInstance> historicCaseInstances = historyService.createHistoricCaseInstanceQuery().list();
      for (HistoricCaseInstance historicCaseInstance: historicCaseInstances) {
        if (historicCaseInstance.getCloseTime() != null) {
          Calendar calendar = Calendar.getInstance();
          calendar.setTime(historicCaseInstance.getCloseTime());
          assertThat(minuteFrom > calendar.get(Calendar.MINUTE) || calendar.get(Calendar.MINUTE) > minuteTo).isTrue();
        }
      }

    }

    assertResult(0);
  }

  private HistoryCleanupJobHandlerConfiguration getHistoryCleanupJobHandlerConfiguration(Job job) {
    return HistoryCleanupJobHandlerConfiguration
          .fromJson(JsonUtil.asObject(((JobEntity) job).getJobHandlerConfigurationRaw()));
  }

  private void runHistoryCleanup() {
    runHistoryCleanup(false);
  }

  private void runHistoryCleanup(boolean manualRun) {
    historyService.cleanUpHistoryAsync(manualRun);

    for (Job job : historyService.findHistoryCleanupJobs()) {
      managementService.executeJob(job.getId());
    }
  }

  @Test
  void testHistoryCleanupMetrics() {
    //given
    processEngineConfiguration.setHistoryCleanupMetricsEnabled(true);
    prepareData(15);

    ClockUtil.setCurrentTime(targetDate);
    //when
    runHistoryCleanup(true);

    //then
    final long removedProcessInstances = managementService.createMetricsQuery().name(Metrics.HISTORY_CLEANUP_REMOVED_PROCESS_INSTANCES).sum();
    final long removedDecisionInstances = managementService.createMetricsQuery().name(Metrics.HISTORY_CLEANUP_REMOVED_DECISION_INSTANCES).sum();
    final long removedCaseInstances = managementService.createMetricsQuery().name(Metrics.HISTORY_CLEANUP_REMOVED_CASE_INSTANCES).sum();

    assertThat(removedProcessInstances).isPositive();
    assertThat(removedDecisionInstances).isPositive();
    assertThat(removedCaseInstances).isPositive();

    assertThat(removedProcessInstances + removedCaseInstances + removedDecisionInstances).isEqualTo(15);
  }


  @Test
  void testHistoryCleanupMetricsExtend() {
    Date currentDate = targetDate;
    // given
    processEngineConfiguration.setHistoryCleanupMetricsEnabled(true);
    prepareData(15);

    ClockUtil.setCurrentTime(currentDate);
    // when
    runHistoryCleanup(true);

    // assume
    assertResult(0);

    // then
    MetricsQuery processMetricsQuery = managementService.createMetricsQuery().name(Metrics.HISTORY_CLEANUP_REMOVED_PROCESS_INSTANCES);
    long removedProcessInstances = processMetricsQuery.startDate(DateUtils.addDays(currentDate, DAYS_IN_THE_PAST)).endDate(DateUtils.addHours(currentDate, 1)).sum();
    assertThat(removedProcessInstances).isEqualTo(5);
    MetricsQuery decisionMetricsQuery = managementService.createMetricsQuery().name(Metrics.HISTORY_CLEANUP_REMOVED_DECISION_INSTANCES);
    long removedDecisionInstances = decisionMetricsQuery.startDate(DateUtils.addDays(currentDate, DAYS_IN_THE_PAST)).endDate(DateUtils.addHours(currentDate, 1)).sum();
    assertThat(removedDecisionInstances).isEqualTo(5);
    MetricsQuery caseMetricsQuery = managementService.createMetricsQuery().name(Metrics.HISTORY_CLEANUP_REMOVED_CASE_INSTANCES);
    long removedCaseInstances = caseMetricsQuery.startDate(DateUtils.addDays(currentDate, DAYS_IN_THE_PAST)).endDate(DateUtils.addHours(currentDate, 1)).sum();
    assertThat(removedCaseInstances).isEqualTo(5);

    long noneProcessInstances = processMetricsQuery.startDate(DateUtils.addHours(currentDate, 1)).limit(1).sum();
    assertThat(noneProcessInstances).isZero();
    long noneDecisionInstances = decisionMetricsQuery.startDate(DateUtils.addHours(currentDate, 1)).limit(1).sum();
    assertThat(noneDecisionInstances).isZero();
    long noneCaseInstances = caseMetricsQuery.startDate(DateUtils.addHours(currentDate, 1)).limit(1).sum();
    assertThat(noneCaseInstances).isZero();

    List<MetricIntervalValue> piList = processMetricsQuery.startDate(currentDate).interval(900);
    assertThat(piList).hasSize(1);
    assertThat(piList.get(0).getValue()).isEqualTo(5);
    List<MetricIntervalValue> diList = decisionMetricsQuery.startDate(DateUtils.addDays(currentDate, DAYS_IN_THE_PAST)).interval(900);
    assertThat(diList).hasSize(1);
    assertThat(diList.get(0).getValue()).isEqualTo(5);
    List<MetricIntervalValue> ciList = caseMetricsQuery.startDate(DateUtils.addDays(currentDate, DAYS_IN_THE_PAST)).interval(900);
    assertThat(ciList).hasSize(1);
    assertThat(ciList.get(0).getValue()).isEqualTo(5);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRef.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml", "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn"})
  void testHistoryCleanupOnlyDecisionInstancesRemoved() {
    // given
    prepareInstances(null, HISTORY_TIME_TO_LIVE, null);

    ClockUtil.setCurrentTime(targetDate);
    // when
    runHistoryCleanup(true);

    // then
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(PROCESS_INSTANCES_COUNT);
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isZero();
    assertThat(historyService.createHistoricCaseInstanceQuery().count()).isEqualTo(CASE_INSTANCES_COUNT);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRef.bpmn20.xml", "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml", "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn"})
  void testHistoryCleanupOnlyProcessInstancesRemoved() {
    // given
    prepareInstances(HISTORY_TIME_TO_LIVE, null, null);

    ClockUtil.setCurrentTime(targetDate);
    // when
    runHistoryCleanup(true);

    // then
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isEqualTo(DECISION_INSTANCES_COUNT + DECISIONS_IN_PROCESS_INSTANCES);
    assertThat(historyService.createHistoricCaseInstanceQuery().count()).isEqualTo(CASE_INSTANCES_COUNT);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRef.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml", "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn"})
  void testHistoryCleanupOnlyCaseInstancesRemoved() {
    // given
    prepareInstances(null, null, HISTORY_TIME_TO_LIVE);

    ClockUtil.setCurrentTime(targetDate);

    // when
    runHistoryCleanup(true);

    // then
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(PROCESS_INSTANCES_COUNT);
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isEqualTo(DECISION_INSTANCES_COUNT + DECISIONS_IN_PROCESS_INSTANCES);
    assertThat(historyService.createHistoricCaseInstanceQuery().count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRef.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml", "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn"})
  void testHistoryCleanupOnlyDecisionInstancesNotRemoved() {
    // given
    prepareInstances(HISTORY_TIME_TO_LIVE, null, HISTORY_TIME_TO_LIVE);

    ClockUtil.setCurrentTime(targetDate);
    // when
    runHistoryCleanup(true);

    // then
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isEqualTo(DECISION_INSTANCES_COUNT + DECISIONS_IN_PROCESS_INSTANCES);
    assertThat(historyService.createHistoricCaseInstanceQuery().count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRef.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml", "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn"})
  void testHistoryCleanupOnlyProcessInstancesNotRemoved() {
    // given
    prepareInstances(null, HISTORY_TIME_TO_LIVE, HISTORY_TIME_TO_LIVE);

    ClockUtil.setCurrentTime(targetDate);
    // when
    runHistoryCleanup(true);

    // then
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(PROCESS_INSTANCES_COUNT);
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isZero();
    assertThat(historyService.createHistoricCaseInstanceQuery().count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRef.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml", "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn"})
  void testHistoryCleanupOnlyCaseInstancesNotRemoved() {
    // given
    prepareInstances(HISTORY_TIME_TO_LIVE, HISTORY_TIME_TO_LIVE, null);

    ClockUtil.setCurrentTime(targetDate);

    // when
    runHistoryCleanup(true);

    // then
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isZero();
    assertThat(historyService.createHistoricCaseInstanceQuery().count()).isEqualTo(CASE_INSTANCES_COUNT);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRef.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml", "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn"})
  void testHistoryCleanupEverythingRemoved() {
    // given
    prepareInstances(HISTORY_TIME_TO_LIVE, HISTORY_TIME_TO_LIVE, HISTORY_TIME_TO_LIVE);

    ClockUtil.setCurrentTime(targetDate);
    // when
    runHistoryCleanup(true);

    // then
    assertResult(0);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRef.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml", "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn"})
  void testHistoryCleanupNothingRemoved() {
    // given
    prepareInstances(null, null, null);

    ClockUtil.setCurrentTime(targetDate);
    // when
    runHistoryCleanup(true);

    // then
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(PROCESS_INSTANCES_COUNT);
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isEqualTo(DECISION_INSTANCES_COUNT + DECISIONS_IN_PROCESS_INSTANCES);
    assertThat(historyService.createHistoricCaseInstanceQuery().count()).isEqualTo(CASE_INSTANCES_COUNT);
  }

  private void prepareInstances(Integer processInstanceTimeToLive, Integer decisionTimeToLive, Integer caseTimeToLive) {
    //update time to live
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
    ClockUtil.setCurrentTime(DateUtils.addDays(targetDate, DAYS_IN_THE_PAST));

    //create 3 process instances
    List<String> processInstanceIds = new ArrayList<>();
    Map<String, Object> variables = Variables.createVariables().putValue("pojo", new TestPojo("okay", 13.37));
    for (int i = 0; i < PROCESS_INSTANCES_COUNT; i++) {
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess", variables);
      processInstanceIds.add(processInstance.getId());
    }
    runtimeService.deleteProcessInstances(processInstanceIds, null, true, true);

    //+10 standalone decisions
    for (int i = 0; i < DECISION_INSTANCES_COUNT; i++) {
      engineRule.getDecisionService().evaluateDecisionByKey("testDecision").variables(variables).evaluate();
    }

    // create 4 case instances
    for (int i = 0; i < CASE_INSTANCES_COUNT; i++) {
      CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase",
          Variables.createVariables().putValue("pojo", new TestPojo("okay", 13.37 + i)));
      caseService.terminateCaseExecution(caseInstance.getId());
      caseService.closeCaseInstance(caseInstance.getId());
    }

    ClockUtil.setCurrentTime(oldCurrentTime);

  }

  @Test
  void testHistoryCleanupWithinBatchWindow() {
    //given
    prepareData(15);

    //we're within batch window
    Date now = targetDate;
    ClockUtil.setCurrentTime(now);
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime(formatTime(now));
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime(formatTime(DateUtils.addHours(now, HISTORY_TIME_TO_LIVE)));
    processEngineConfiguration.initHistoryCleanup();

    //when
    runHistoryCleanup();

    //then
    assertResult(0);
  }

  @Test
  void testHistoryCleanupJobNullTTL() {
    //given
    removeHistoryTimeToLive();

    prepareData(15);

    ClockUtil.setCurrentTime(targetDate);
    //when
    runHistoryCleanup(true);

    //then
    assertResult(15);
  }

  private void removeHistoryTimeToLive() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().processDefinitionKey(ONE_TASK_PROCESS).list();
    assertThat(processDefinitions).hasSize(1);
    repositoryService.updateProcessDefinitionHistoryTimeToLive(processDefinitions.get(0).getId(), null);

    final List<DecisionDefinition> decisionDefinitions = repositoryService.createDecisionDefinitionQuery().decisionDefinitionKey(DECISION).list();
    assertThat(decisionDefinitions).hasSize(1);
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinitions.get(0).getId(), null);

    final List<CaseDefinition> caseDefinitions = repositoryService.createCaseDefinitionQuery().caseDefinitionKey(ONE_TASK_CASE).list();
    assertThat(caseDefinitions).hasSize(1);
    repositoryService.updateCaseDefinitionHistoryTimeToLive(caseDefinitions.get(0).getId(), null);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml"})
  void testHistoryCleanupJobDefaultTTL() {
    //given
    prepareBPMNData(15, "twoTasksProcess");

    ClockUtil.setCurrentTime(targetDate);
    //when
    runHistoryCleanup(true);

    //then
    assertResult(15);
  }

  @Test
  void testFindHistoryCleanupJob() {
    //given
    historyService.cleanUpHistoryAsync(true).getId();

    //when
    final List<Job> historyCleanupJobs = historyService.findHistoryCleanupJobs();

    //then
    assertThat(historyCleanupJobs).hasSize(NUMBER_OF_THREADS);
  }

  @Test
  void testRescheduleForNever() {
    //given

    //force creation of job
    historyService.cleanUpHistoryAsync(true);
    List<Job> historyCleanupJobs = historyService.findHistoryCleanupJobs();
    assertThat(historyCleanupJobs).isNotEmpty();
    for (Job job : historyCleanupJobs) {
      assertThat(job.getDuedate()).isNotNull();
    }

    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime(null);
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime(null);
    processEngineConfiguration.initHistoryCleanup();

    ClockUtil.setCurrentTime(targetDate);

    //when
    historyService.cleanUpHistoryAsync(false);

    //then
    historyCleanupJobs = historyService.findHistoryCleanupJobs();
    for (Job job : historyCleanupJobs) {
      assertThat(job.isSuspended()).isTrue();
      assertThat(job.getDuedate()).isNull();
    }

  }

  @Test
  void shouldResolveIncidentAndApplyHistoryCleanupDefaultRetriesConfig() {
    //given
    processEngineConfiguration.setHistoryCleanupDefaultNumberOfRetries(10);
    String jobId = historyService.cleanUpHistoryAsync(true).getId();
    imitateFailedJob(jobId);

    //when
    jobId = historyService.cleanUpHistoryAsync(true).getId();

    //then
    JobEntity jobEntity = getJobEntity(jobId);

    assertThat(jobEntity.getExceptionByteArrayId()).isNull();
    assertThat(jobEntity.getExceptionMessage()).isNull();
    assertThat(jobEntity.getRetries()).isEqualTo(10);
  }

  @Test
  void shouldResolveIncidentAndApplyDefaultRetriesConfig() {
    //given
    String jobId = historyService.cleanUpHistoryAsync(true).getId();
    imitateFailedJob(jobId);

    //when
    jobId = historyService.cleanUpHistoryAsync(true).getId();

    //then
    JobEntity jobEntity = getJobEntity(jobId);

    assertThat(jobEntity.getExceptionByteArrayId()).isNull();
    assertThat(jobEntity.getExceptionMessage()).isNull();
    assertThat(jobEntity.getRetries()).isEqualTo(5);
  }

  private void imitateFailedJob(final String jobId) {
    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      JobEntity jobEntity = getJobEntity(jobId);
      jobEntity.setRetries(0);
      jobEntity.setExceptionMessage("Something bad happened");
      jobEntity.setExceptionStacktrace(ExceptionUtil.getExceptionStacktrace(new RuntimeException("Something bad happened")));
      return null;
    });
  }

  @Test
  void testLessThanThresholdManualRun() {
    //given
    prepareData(5);

    ClockUtil.setCurrentTime(targetDate);
    //when
    runHistoryCleanup(true);

    //then
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();

    for (Job job : historyService.findHistoryCleanupJobs()) {
      assertThat(job.isSuspended()).isTrue();
    }
  }

  @Test
  void testNotEnoughTimeToDeleteEverything() {
    //given
    //we have something to clean up
    prepareData(80);
    //we call history cleanup within batch window
    Date now = targetDate;
    ClockUtil.setCurrentTime(now);
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime(formatTime(now));
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime(formatTime(DateUtils.addHours(now, HISTORY_TIME_TO_LIVE)));
    processEngineConfiguration.initHistoryCleanup();
    //job is executed once within batch window
    //we run the job in 3 threads, so not more than 60 instances can be removed in one run
    runHistoryCleanup();

    //when
    //time passed -> outside batch window
    ClockUtil.setCurrentTime(DateUtils.addHours(now, 6));
    //the job is called for the second time
    for (Job job : historyService.findHistoryCleanupJobs()) {
      managementService.executeJob(job.getId());
    }

    //then
    //second execution was not able to delete rest data
    assertResultNotLess(20);
  }

  @Test
  void testManualRunDoesNotRespectBatchWindow() {
    //given
    //we have something to clean up
    int processInstanceCount = 40;
    prepareData(processInstanceCount);

    //we call history cleanup outside batch window
    Date now = targetDate;
    ClockUtil.setCurrentTime(now);
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime(formatTime(DateUtils.addHours(now, 1))); //now + 1 hour
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime(formatTime(DateUtils.addHours(now, HISTORY_TIME_TO_LIVE)));   //now + 5 hours
    processEngineConfiguration.initHistoryCleanup();

    //when
    //job is executed before batch window start
    runHistoryCleanup(true);

    //the job is called for the second time after batch window end
    ClockUtil.setCurrentTime(DateUtils.addHours(now, 6)); //now + 6 hours
    for (Job job : historyService.findHistoryCleanupJobs()) {
      managementService.executeJob(job.getId());
    }

    //then
    assertResult(0);
  }


  @Test
  void testLessThanThresholdWithinBatchWindow() {
    //given
    prepareData(5);

    //we're within batch window
    Date now = targetDate;
    ClockUtil.setCurrentTime(now);
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime(formatTime(now));
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime(formatTime(DateUtils.addHours(now, HISTORY_TIME_TO_LIVE)));
    processEngineConfiguration.initHistoryCleanup();

    //when
    runHistoryCleanup();

    //then
    final List<Job> historyCleanupJobs = historyService.findHistoryCleanupJobs();
    for (Job job : historyCleanupJobs) {
      JobEntity jobEntity = (JobEntity) job;
      HistoryCleanupJobHandlerConfiguration configuration = getConfiguration(jobEntity);

      //job rescheduled till current time + delay
      Date nextRun = getNextRunWithDelay(ClockUtil.getCurrentTime(), 0);
      assertThat(jobEntity.getDuedate().equals(nextRun) || jobEntity.getDuedate().after(nextRun)).isTrue();
      Date nextRunMax = DateUtils.addSeconds(ClockUtil.getCurrentTime(), HistoryCleanupJobHandlerConfiguration.MAX_DELAY);
      assertThat(jobEntity.getDuedate().before(nextRunMax)).isTrue();

      //countEmptyRuns incremented
      assertThat(configuration.getCountEmptyRuns()).isEqualTo(1);
    }

    //data is still removed
    assertResult(0);
  }

  private Date getNextRunWithDelay(Date date, int countEmptyRuns) {
    //ignore milliseconds because MySQL does not support them, and it's not important for test
    return DateUtils.setMilliseconds(DateUtils.addSeconds(date, Math.min((int)(Math.pow(2., countEmptyRuns) * HistoryCleanupJobHandlerConfiguration.START_DELAY),
        HistoryCleanupJobHandlerConfiguration.MAX_DELAY)), 0);
  }

  private JobEntity getJobEntity(String jobId) {
    return (JobEntity)managementService.createJobQuery().jobId(jobId).list().get(0);
  }

  @Test
  void testLessThanThresholdWithinBatchWindowAgain() {
    //given
    prepareData(5);

    //we're within batch window
    Date now = targetDate;
    ClockUtil.setCurrentTime(now);
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime(formatTime(now));
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime(formatTime(DateUtils.addHours(now, 1)));
    processEngineConfiguration.initHistoryCleanup();

    //when
    historyService.cleanUpHistoryAsync();
    List<Job> historyCleanupJobs = historyService.findHistoryCleanupJobs();
    for (int i = 1; i <= 6; i++) {
      for (Job job : historyCleanupJobs) {
        managementService.executeJob(job.getId());
      }
    }

    //then
    historyCleanupJobs = historyService.findHistoryCleanupJobs();
    for (Job job : historyCleanupJobs) {
      JobEntity jobEntity = (JobEntity) job;
      HistoryCleanupJobHandlerConfiguration configuration = getConfiguration(jobEntity);

      //job rescheduled till current time + (2 power count)*delay
      Date nextRun = getNextRunWithDelay(ClockUtil.getCurrentTime(), HISTORY_TIME_TO_LIVE);
      assertThat(jobEntity.getDuedate().equals(nextRun) || jobEntity.getDuedate().after(nextRun)).isTrue();
      Date nextRunMax = DateUtils.addSeconds(ClockUtil.getCurrentTime(), HistoryCleanupJobHandlerConfiguration.MAX_DELAY);
      assertThat(jobEntity.getDuedate().before(nextRunMax)).isTrue();

      //countEmptyRuns incremented
      assertThat(configuration.getCountEmptyRuns()).isEqualTo(6);
    }

    //data is still removed
    assertResult(0);
  }

  @Test
  void testLessThanThresholdWithinBatchWindowMaxDelayReached() {
    //given
    prepareData(5);

    //we're within batch window
    Date now = targetDate;
    ClockUtil.setCurrentTime(now);
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime(formatTime(now));
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime(formatTime(DateUtils.addHours(now, 2)));
    processEngineConfiguration.initHistoryCleanup();

    //when
    historyService.cleanUpHistoryAsync();
    List<Job> historyCleanupJobs = historyService.findHistoryCleanupJobs();
    for (int i = 1; i <= 11; i++) {
      for (Job job : historyCleanupJobs) {
        managementService.executeJob(job.getId());
      }
    }

    //then
    historyCleanupJobs = historyService.findHistoryCleanupJobs();
    for (Job job : historyCleanupJobs) {
      JobEntity jobEntity = (JobEntity) job;
      HistoryCleanupJobHandlerConfiguration configuration = getConfiguration(jobEntity);

      //job rescheduled till current time + max delay
      Date nextRun = getNextRunWithDelay(ClockUtil.getCurrentTime(), 10);
      assertThat(jobEntity.getDuedate().equals(nextRun) || jobEntity.getDuedate().after(nextRun)).isTrue();
      assertThat(jobEntity.getDuedate().before(getNextRunWithinBatchWindow(now))).isTrue();

      //countEmptyRuns incremented
      assertThat(configuration.getCountEmptyRuns()).isEqualTo(11);
    }

    //data is still removed
    assertResult(0);
  }

  @Test
  void testLessThanThresholdCloseToBatchWindowEndTime() {
    //given
    prepareData(5);

    //we're within batch window
    Date now = targetDate;
    ClockUtil.setCurrentTime(now);
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime(formatTime(now));
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime(formatTime(DateUtils.addMinutes(now, 30)));
    processEngineConfiguration.initHistoryCleanup();

    //when
    historyService.cleanUpHistoryAsync();
    List<Job> historyCleanupJobs = historyService.findHistoryCleanupJobs();
    for (int i = 1; i <= 9; i++) {
      for (Job job : historyCleanupJobs) {
        managementService.executeJob(job.getId());
      }
    }

    //then
    historyCleanupJobs = historyService.findHistoryCleanupJobs();
    for (Job job: historyCleanupJobs) {
      JobEntity jobEntity = (JobEntity)job;
      HistoryCleanupJobHandlerConfiguration configuration = getConfiguration(jobEntity);

      //job rescheduled till next batch window start time
      Date nextRun = getNextRunWithinBatchWindow(ClockUtil.getCurrentTime());
      assertThat(nextRun).isEqualTo(jobEntity.getDuedate());

      //countEmptyRuns canceled
      assertThat(configuration.getCountEmptyRuns()).isZero();
    }

    //data is still removed
    assertResult(0);
  }

  @Test
  void testLessThanThresholdOutsideBatchWindow() {
    //given
    prepareData(5);

    //we're outside batch window
    Date twoHoursAgo = targetDate;
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime(formatTime(twoHoursAgo));
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime(formatTime(DateUtils.addHours(twoHoursAgo, 1)));
    processEngineConfiguration.initHistoryCleanup();
    ClockUtil.setCurrentTime(DateUtils.addHours(twoHoursAgo, 2));

    //when
    for (int i = 1; i <= 3; i++) {
      runHistoryCleanup();
    }

    //then
    final List<Job> historyCleanupJobs = historyService.findHistoryCleanupJobs();
    for (Job job : historyCleanupJobs) {
      JobEntity jobEntity = (JobEntity) job;
      HistoryCleanupJobHandlerConfiguration configuration = getConfiguration(jobEntity);

      //job rescheduled till next batch window start
      Date nextRun = getNextRunWithinBatchWindow(ClockUtil.getCurrentTime());
      assertThat(nextRun).isEqualTo(jobEntity.getDuedate());

      //countEmptyRuns canceled
      assertThat(configuration.getCountEmptyRuns()).isZero();
    }

    //nothing was removed
    assertResult(5);
  }

  @Test
  void testLessThanThresholdOutsideBatchWindowAfterMidnight() {
    //given
    prepareData(5);

    //we're outside batch window, batch window passes midnight
    Date date = addDays(targetDate, 1);
    ClockUtil.setCurrentTime(DateUtils.setMinutes(DateUtils.setHours(date, 1), 10));  // 01:10 tomorrow
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23:00");
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime("01:00");
    processEngineConfiguration.initHistoryCleanup();

    //when
    String jobId = historyService.cleanUpHistoryAsync().getId();
    managementService.executeJob(jobId);

    //then
    JobEntity jobEntity = getJobEntity(jobId);
    HistoryCleanupJobHandlerConfiguration configuration = getConfiguration(jobEntity);

    //job rescheduled till next batch window start
    Date nextRun = getNextRunWithinBatchWindow(ClockUtil.getCurrentTime());
    assertThat(nextRun).isEqualTo(jobEntity.getDuedate());
    assertThat(nextRun.after(ClockUtil.getCurrentTime())).isTrue();

    //countEmptyRuns canceled
    assertThat(configuration.getCountEmptyRuns()).isZero();

    //nothing was removed
    assertResult(5);
  }

  @Test
  void testLessThanThresholdOutsideBatchWindowBeforeMidnight() {
    //given
    prepareData(5);

    //we're outside batch window, batch window passes midnight
    ClockUtil.setCurrentTime(DateUtils.setMinutes(DateUtils.setHours(targetDate, 22), 10));  //22:10
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23:00");
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime("01:00");
    processEngineConfiguration.initHistoryCleanup();

    //when
    String jobId = historyService.cleanUpHistoryAsync().getId();
    managementService.executeJob(jobId);

    //then
    JobEntity jobEntity = getJobEntity(jobId);
    HistoryCleanupJobHandlerConfiguration configuration = getConfiguration(jobEntity);

    //job rescheduled till next batch window start
    Date nextRun = getNextRunWithinBatchWindow(ClockUtil.getCurrentTime());
    assertThat(nextRun).isEqualTo(jobEntity.getDuedate());
    assertThat(nextRun.after(ClockUtil.getCurrentTime())).isTrue();

    //countEmptyRuns cancelled
    assertThat(configuration.getCountEmptyRuns()).isZero();

    //nothing was removed
    assertResult(5);
  }

  @Test
  void testLessThanThresholdWithinBatchWindowBeforeMidnight() {
    //given
    prepareData(5);

    //we're within batch window, but batch window passes midnight
    ClockUtil.setCurrentTime(DateUtils.setMinutes(DateUtils.setHours(targetDate, 23), 10));  //23:10
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23:00");
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime("01:00");
    processEngineConfiguration.initHistoryCleanup();

    //when
    runHistoryCleanup();

    //then
    final List<Job> historyCleanupJobs = historyService.findHistoryCleanupJobs();
    for (Job job : historyCleanupJobs) {
      JobEntity jobEntity = (JobEntity) job;
      HistoryCleanupJobHandlerConfiguration configuration = getConfiguration(jobEntity);

      //job rescheduled till current time + delay
      Date nextRun = getNextRunWithDelay(ClockUtil.getCurrentTime(), 0);
      assertThat(jobEntity.getDuedate().equals(nextRun) || jobEntity.getDuedate().after(nextRun)).isTrue();
      Date nextRunMax = DateUtils.addSeconds(ClockUtil.getCurrentTime(), HistoryCleanupJobHandlerConfiguration.MAX_DELAY);
      assertThat(jobEntity.getDuedate().before(nextRunMax)).isTrue();

      //countEmptyRuns incremented
      assertThat(configuration.getCountEmptyRuns()).isEqualTo(1);
    }

    //data is still removed
    assertResult(0);
  }

  @Test
  void testLessThanThresholdWithinBatchWindowAfterMidnight() {
    //given
    prepareData(5);

    //we're within batch window, but batch window passes midnight
    Date date = addDays(targetDate, 1);
    ClockUtil.setCurrentTime(DateUtils.setMinutes(DateUtils.setHours(date, 0), 10));  // 00:10 tomorrow
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23:00");
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime("01:00");
    processEngineConfiguration.initHistoryCleanup();

    //when
    runHistoryCleanup(false);

    //then
    final List<Job> historyCleanupJobs = historyService.findHistoryCleanupJobs();
    for (Job job : historyCleanupJobs) {
      JobEntity jobEntity = (JobEntity) job;
      HistoryCleanupJobHandlerConfiguration configuration = getConfiguration(jobEntity);

      //job rescheduled till current time + delay
      Date nextRun = getNextRunWithDelay(ClockUtil.getCurrentTime(), 0);
      assertThat(jobEntity.getDuedate().equals(nextRun) || jobEntity.getDuedate().after(nextRun)).isTrue();
      Date nextRunMax = DateUtils.addSeconds(ClockUtil.getCurrentTime(), HistoryCleanupJobHandlerConfiguration.MAX_DELAY);
      assertThat(jobEntity.getDuedate().before(nextRunMax)).isTrue();

      //countEmptyRuns incremented
      assertThat(configuration.getCountEmptyRuns()).isEqualTo(1);
    }

    //data is still removed
    assertResult(0);
  }

  /*I can't find the corresponding ticket. On my local machine (Windows) it works, but not in github actions
  It fails because in line 1135 the timestamps are different:
  Locally the results of both are:
  Tue May 28 2019 21:00:00 GMT+0000

  On github:
    Mon May 27 2019 23:10:10 GMT+0000
    and
    Tue May 28 2019 22:00:00 GMT+0000
   */

  @Test
  @Disabled("CAM-10055")
  void testLessThanThresholdOutsideBatchWindowAfterMidnightDaylightSaving() {
    //given
    prepareData(5);

    //we're outside batch window, batch window passes midnight
    ClockUtil.setCurrentTime(parseDate("2019-05-28T01:10:00"));  // 01:10
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23:00CET");
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime("01:00CET");
    processEngineConfiguration.initHistoryCleanup();

    //when
    String jobId = historyService.cleanUpHistoryAsync().getId();
    managementService.executeJob(jobId);

    //then
    JobEntity jobEntity = getJobEntity(jobId);
    HistoryCleanupJobHandlerConfiguration configuration = getConfiguration(jobEntity);

    //job rescheduled till next batch window start
    Date nextRun = getNextRunWithinBatchWindow(ClockUtil.getCurrentTime());

    assertThat(nextRun).isEqualTo(jobEntity.getDuedate());
    assertThat(nextRun.after(ClockUtil.getCurrentTime())).isTrue();

    //countEmptyRuns canceled
    assertThat(configuration.getCountEmptyRuns()).isZero();

    //nothing was removed
    assertResult(5);
  }

  @Test
  @Disabled("CAM-10055")
  void testLessThanThresholdWithinBatchWindowAfterMidnightDaylightSaving() {
    //given
    prepareData(5);

    //we're within batch window, but batch window passes midnight
    ClockUtil.setCurrentTime(parseDate("2018-05-14T00:10:00"));  // 00:10
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23:00CET");
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime("01:00CET");
    processEngineConfiguration.initHistoryCleanup();

    //when
    runHistoryCleanup(false);

    //then
    final List<Job> historyCleanupJobs = historyService.findHistoryCleanupJobs();
    for (Job job : historyCleanupJobs) {
      JobEntity jobEntity = (JobEntity) job;
      HistoryCleanupJobHandlerConfiguration configuration = getConfiguration(jobEntity);

      //job rescheduled till current time + delay
      Date nextRun = getNextRunWithDelay(ClockUtil.getCurrentTime(), 0);
      assertThat(jobEntity.getDuedate().equals(nextRun) || jobEntity.getDuedate().after(nextRun)).isTrue();
      Date nextRunMax = DateUtils.addSeconds(ClockUtil.getCurrentTime(), HistoryCleanupJobHandlerConfiguration.MAX_DELAY);
      assertThat(jobEntity.getDuedate().before(nextRunMax)).isTrue();

      //countEmptyRuns incremented
      assertThat(configuration.getCountEmptyRuns()).isEqualTo(1);
    }

    //data is still removed
    assertResult(0);
  }

  @Test
  void testConfiguration() {
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23:00+0200");
    processEngineConfiguration.initHistoryCleanup();
    Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+2:00"));
    Date startTime = processEngineConfiguration.getHistoryCleanupBatchWindowStartTimeAsDate();
    c.setTime(startTime);
    assertThat(c.get(Calendar.HOUR_OF_DAY)).isEqualTo(23);
    assertThat(c.get(Calendar.MINUTE)).isZero();
    assertThat(c.get(Calendar.SECOND)).isZero();

    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23:00");
    processEngineConfiguration.initHistoryCleanup();
    c = Calendar.getInstance();
    startTime = processEngineConfiguration.getHistoryCleanupBatchWindowStartTimeAsDate();
    c.setTime(startTime);
    assertThat(c.get(Calendar.HOUR_OF_DAY)).isEqualTo(23);
    assertThat(c.get(Calendar.MINUTE)).isZero();
    assertThat(c.get(Calendar.SECOND)).isZero();

    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime("01:35-0800");
    processEngineConfiguration.initHistoryCleanup();
    c = Calendar.getInstance(TimeZone.getTimeZone("GMT-8:00"));
    Date endTime = processEngineConfiguration.getHistoryCleanupBatchWindowEndTimeAsDate();
    c.setTime(endTime);
    assertThat(c.get(Calendar.HOUR_OF_DAY)).isEqualTo(1);
    assertThat(c.get(Calendar.MINUTE)).isEqualTo(35);
    assertThat(c.get(Calendar.SECOND)).isZero();

    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime("01:35");
    processEngineConfiguration.initHistoryCleanup();
    c = Calendar.getInstance();
    endTime = processEngineConfiguration.getHistoryCleanupBatchWindowEndTimeAsDate();
    c.setTime(endTime);
    assertThat(c.get(Calendar.HOUR_OF_DAY)).isEqualTo(1);
    assertThat(c.get(Calendar.MINUTE)).isEqualTo(35);
    assertThat(c.get(Calendar.SECOND)).isZero();

    processEngineConfiguration.setHistoryCleanupBatchSize(500);
    processEngineConfiguration.initHistoryCleanup();
    assertThat(processEngineConfiguration.getHistoryCleanupBatchSize()).isEqualTo(500);

    processEngineConfiguration.setHistoryTimeToLive("5");
    processEngineConfiguration.initHistoryCleanup();
    assertThat(ParseUtil.parseHistoryTimeToLive(processEngineConfiguration.getHistoryTimeToLive()).intValue()).isEqualTo(5);

    processEngineConfiguration.setHistoryTimeToLive("P6D");
    processEngineConfiguration.initHistoryCleanup();
    assertThat(ParseUtil.parseHistoryTimeToLive(processEngineConfiguration.getHistoryTimeToLive()).intValue()).isEqualTo(6);
  }

  @Test
  void testHistoryCleanupHelper() {
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime("22:00+0100");
    processEngineConfiguration.initHistoryCleanup();

    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    Date date = Date.from(Instant.from(dateFormat.parse("2017-09-06T22:15:00+0100")));

    assertThat(HistoryCleanupHelper.isWithinBatchWindow(date, processEngineConfiguration)).isTrue();

    date = Date.from(Instant.from(dateFormat.parse("2017-09-06T22:15:00+0200")));
    assertThat(HistoryCleanupHelper.isWithinBatchWindow(date, processEngineConfiguration)).isFalse();
  }

  @Test
  void testConfigurationFailureWrongStartTime() {
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23");
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime("01:00");

    // when/then
    assertThatThrownBy(() -> processEngineConfiguration.initHistoryCleanup())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("historyCleanupBatchWindowStartTime");
  }

  @Test
  void testConfigurationFailureWrongDayOfTheWeekStartTime() {
    // when/then
    assertThatThrownBy(() -> new BatchWindowConfiguration("23", "01:00"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("startTime");
  }

  @Test
  void testConfigurationFailureWrongDayOfTheWeekEndTime() {
    // when/then
    assertThatThrownBy(() -> new BatchWindowConfiguration("23:00", "01"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("endTime");
  }

  @Test
  void testConfigurationFailureWrongDegreeOfParallelism() {
    processEngineConfiguration.setHistoryCleanupDegreeOfParallelism(0);

    // when/then
    assertThatThrownBy(() -> processEngineConfiguration.initHistoryCleanup())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("historyCleanupDegreeOfParallelism");

    // and
    processEngineConfiguration.setHistoryCleanupDegreeOfParallelism(HistoryCleanupCmd.MAX_THREADS_NUMBER + 1);

    // when/then
    assertThatThrownBy(() -> processEngineConfiguration.initHistoryCleanup())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("historyCleanupDegreeOfParallelism");
  }

  @Test
  void testConfigurationFailureWrongEndTime() {
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23:00");
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime("wrongValue");

    // when/then
    assertThatThrownBy(() -> processEngineConfiguration.initHistoryCleanup())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("historyCleanupBatchWindowEndTime");
  }

  @Test
  void testConfigurationFailureWrongBatchSize() {
    processEngineConfiguration.setHistoryCleanupBatchSize(501);

    // when/then
    assertThatThrownBy(() -> processEngineConfiguration.initHistoryCleanup())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("historyCleanupBatchSize");
  }

  @Test
  void testConfigurationFailureWrongBatchSize2() {
    processEngineConfiguration.setHistoryCleanupBatchSize(-5);

    // when/then
    assertThatThrownBy(() -> processEngineConfiguration.initHistoryCleanup())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("historyCleanupBatchSize");
  }

  @Test
  void testConfigurationFailureWrongBatchThreshold() {
    processEngineConfiguration.setHistoryCleanupBatchThreshold(-1);

    // when/then
    assertThatThrownBy(() -> processEngineConfiguration.initHistoryCleanup())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("historyCleanupBatchThreshold");
  }

  @ParameterizedTest(name = "{index} - {0}")
  @CsvSource({
    "Malformed value, PP5555DDDD",
    "Invalid value, invalidValue",
    "Negative value, -6"
  })
  void testConfigurationFailure (@SuppressWarnings("unused") String testName, String historyTimeToLive) {
    processEngineConfiguration.setHistoryTimeToLive(historyTimeToLive);

    // when/then
    assertThatThrownBy(() -> processEngineConfiguration.initHistoryCleanup())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("historyTimeToLive");
  }

  @Test
  void shouldApplyGlobalJobRetries() {
    // given
    engineRule.getProcessEngineConfiguration().setDefaultNumberOfRetries(7);

    // when
    Job cleanupJob = historyService.cleanUpHistoryAsync(true);

    // then
    assertThat(cleanupJob.getRetries()).isEqualTo(7);
  }

  @Test
  void shouldApplyLocalJobRetries() {
    // given
    engineRule.getProcessEngineConfiguration().setDefaultNumberOfRetries(7);
    engineRule.getProcessEngineConfiguration().setHistoryCleanupDefaultNumberOfRetries(1);

    // when
    Job cleanupJob = historyService.cleanUpHistoryAsync(true);

    // then
    assertThat(cleanupJob.getRetries()).isEqualTo(1);
  }

  @Test
  void shouldApplyCleanupJobRetries() {
    // given
    engineRule.getProcessEngineConfiguration().setHistoryCleanupDefaultNumberOfRetries(22);

    // when
    Job cleanupJob = historyService.cleanUpHistoryAsync(true);

    // then
    assertThat(cleanupJob.getRetries()).isEqualTo(22);
  }

  @Test
  void shouldDisableRetriesOnCleanupJob() {
    //given
    ProcessEngineConfigurationImpl configuration = engineRule.getProcessEngineConfiguration();
    configuration.setHistoryCleanupDefaultNumberOfRetries(0);

    //when
    Job cleanupJob = historyService.cleanUpHistoryAsync(true);

    //then
    assertThat(cleanupJob.getRetries()).isZero();
  }


  private Date getNextRunWithinBatchWindow(Date currentTime) {
    return processEngineConfiguration.getBatchWindowManager().getNextBatchWindow(currentTime, processEngineConfiguration).getStart();
  }

  private HistoryCleanupJobHandlerConfiguration getConfiguration(JobEntity jobEntity) {
    String jobHandlerConfigurationRaw = jobEntity.getJobHandlerConfigurationRaw();
    return HistoryCleanupJobHandlerConfiguration.fromJson(JsonUtil.asObject(jobHandlerConfigurationRaw));
  }

  private void prepareData(int instanceCount) {
    int createdInstances = instanceCount / 3;
    prepareBPMNData(createdInstances, ONE_TASK_PROCESS);
    prepareDMNData(createdInstances);
    prepareCMMNData(instanceCount - 2 * createdInstances);
  }

  private void prepareBPMNData(int instanceCount, String definitionKey) {
    Date oldCurrentTime = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(targetDate, DAYS_IN_THE_PAST));
    final List<String> ids = prepareHistoricProcesses(definitionKey, getVariables(), instanceCount);
    deleteProcessInstances(ids);
    ClockUtil.setCurrentTime(oldCurrentTime);
  }

  private void deleteProcessInstances(List<String> ids) {
    final Date currentTime = ClockUtil.getCurrentTime();
    for (String id : ids) {
      //spread end_time between different "minutes"
      ClockUtil.setCurrentTime(DateUtils.setMinutes(currentTime, random.nextInt(60)));
      runtimeService.deleteProcessInstance(id, null, true, true);
    }
  }

  private void prepareDMNData(int instanceCount) {
    Date oldCurrentTime = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(targetDate, DAYS_IN_THE_PAST));
    for (int i = 0; i < instanceCount; i++) {
      //spread end_time between different "minutes"
      ClockUtil.setCurrentTime(DateUtils.setMinutes(ClockUtil.getCurrentTime(), random.nextInt(60)));
      engineRule.getDecisionService().evaluateDecisionByKey(DECISION).variables(getDMNVariables()).evaluate();
    }
    ClockUtil.setCurrentTime(oldCurrentTime);
  }

  private void prepareCMMNData(int instanceCount) {
    Date oldCurrentTime = ClockUtil.getCurrentTime();
    ClockUtil.setCurrentTime(DateUtils.addDays(targetDate, DAYS_IN_THE_PAST));

    for (int i = 0; i < instanceCount; i++) {
      CaseInstance caseInstance = caseService.createCaseInstanceByKey(ONE_TASK_CASE);
      //spread end_time between different "minutes"
      ClockUtil.setCurrentTime(DateUtils.setMinutes(ClockUtil.getCurrentTime(), random.nextInt(60)));
      caseService.terminateCaseExecution(caseInstance.getId());
      caseService.closeCaseInstance(caseInstance.getId());
    }
    ClockUtil.setCurrentTime(oldCurrentTime);
  }

  private List<String> prepareHistoricProcesses(String definitionKey, VariableMap variables, Integer processInstanceCount) {
    List<String> processInstanceIds = new ArrayList<>();

    for (int i = 0; i < processInstanceCount; i++) {
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(definitionKey, variables);
      processInstanceIds.add(processInstance.getId());
    }

    return processInstanceIds;
  }

  private VariableMap getVariables() {
    return Variables.createVariables().putValue("aVariableName", "aVariableValue").putValue("anotherVariableName", "anotherVariableValue");
  }

  protected VariableMap getDMNVariables() {
    return Variables.createVariables().putValue("status", "silver").putValue("sum", 723);
  }

  private void assertResult(long expectedInstanceCount) {
    long count = historyService.createHistoricProcessInstanceQuery().count()
        + historyService.createHistoricDecisionInstanceQuery().count()
        + historyService.createHistoricCaseInstanceQuery().count();
    assertThat(count).isEqualTo(expectedInstanceCount);
  }

  private void assertResultNotLess(long expectedInstanceCount) {
    long count = historyService.createHistoricProcessInstanceQuery().count()
      + historyService.createHistoricDecisionInstanceQuery().count()
      + historyService.createHistoricCaseInstanceQuery().count();
    assertThat(expectedInstanceCount).isLessThanOrEqualTo(count);
  }

  protected static Date addDays(Date date, int days) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.add(Calendar.DATE, days);
    return calendar.getTime();
  }

}
