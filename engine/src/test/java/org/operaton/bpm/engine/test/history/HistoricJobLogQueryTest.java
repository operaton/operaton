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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.history.HistoricJobLogQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.MessageJobDeclaration;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.FailingDelegate;
import org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil;
import org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.NullTolerantComparator;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.*;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoricJobLogQueryTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  ManagementService managementService;
  HistoryService historyService;

  protected String defaultHostname;

  @BeforeEach
  void init() {
    defaultHostname = processEngineConfiguration.getHostname();
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setHostname(defaultHostname);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQuery() {
    runtimeService.startProcessInstanceByKey("process");
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByLogId() {
    runtimeService.startProcessInstanceByKey("process");
    String logId = historyService.createHistoricJobLogQuery().singleResult().getId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().logId(logId);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidLogId() {
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().logId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.logId(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("historicJobLogId is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByJobId() {
    runtimeService.startProcessInstanceByKey("process");
    String jobId = managementService.createJobQuery().singleResult().getId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobId(jobId);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidJobId() {
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.jobId(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("jobId is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByJobExceptionMessage() {
    runtimeService.startProcessInstanceByKey("process");
    String jobId = managementService.createJobQuery().singleResult().getId();

    assertThatThrownBy(() -> managementService.executeJob(jobId))
      .isInstanceOf(ProcessEngineException.class)
      .hasStackTraceContaining(FailingDelegate.EXCEPTION_MESSAGE);

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobExceptionMessage(FailingDelegate.EXCEPTION_MESSAGE);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidJobExceptionMessage() {
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobExceptionMessage("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.jobExceptionMessage(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("jobExceptionMessage is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByFailedActivityId() {
    runtimeService.startProcessInstanceByKey("process");
    String jobId = managementService.createJobQuery().singleResult().getId();

    assertThatThrownBy(() -> managementService.executeJob(jobId))
      .isInstanceOf(ProcessEngineException.class)
      .hasStackTraceContaining(FailingDelegate.EXCEPTION_MESSAGE);

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().failedActivityIdIn("serviceTask");

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidFailedActivityId() {
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().failedActivityIdIn("invalid");

    verifyQueryResults(query, 0);

    String[] nullValue = null;

    assertThatThrownBy(() -> query.failedActivityIdIn(nullValue))
      .isInstanceOf(NotValidException.class)
      .hasMessage("activityIds is null");

    String[] activityIdsContainsNull = {"a", null, "b"};

    assertThatThrownBy(() -> query.failedActivityIdIn(activityIdsContainsNull))
      .isInstanceOf(NotValidException.class)
      .hasMessage("activityIds contains null value");

    String[] activityIdsContainsEmptyString = {"a", "", "b"};

    assertThatThrownBy(() -> query.failedActivityIdIn(activityIdsContainsEmptyString))
      .isInstanceOf(NotValidException.class)
      .hasMessage("activityIds contains empty string");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByJobDefinitionId() {
    runtimeService.startProcessInstanceByKey("process");
    String jobDefinitionId = managementService.createJobQuery().singleResult().getJobDefinitionId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobDefinitionId(jobDefinitionId);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidJobDefinitionId() {
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobDefinitionId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.jobDefinitionId(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("jobDefinitionId is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByJobDefinitionType() {
    runtimeService.startProcessInstanceByKey("process");

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobDefinitionType(AsyncContinuationJobHandler.TYPE);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidJobDefinitionType() {
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobDefinitionType("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.jobDefinitionType(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("jobDefinitionType is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByJobDefinitionConfiguration() {
    runtimeService.startProcessInstanceByKey("process");

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobDefinitionConfiguration(MessageJobDeclaration.ASYNC_BEFORE);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidJobDefinitionConfiguration() {
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().jobDefinitionConfiguration("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.jobDefinitionConfiguration(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("jobDefinitionConfiguration is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByActivityId() {
    runtimeService.startProcessInstanceByKey("process");

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().activityIdIn("serviceTask");

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidActivityId() {
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().activityIdIn("invalid");

    verifyQueryResults(query, 0);

    String[] nullValue = null;

    assertThatThrownBy(() -> query.activityIdIn(nullValue))
      .isInstanceOf(NotValidException.class)
      .hasMessage("activityIds is null");

    String[] activityIdsContainsNull = {"a", null, "b"};

    assertThatThrownBy(() -> query.activityIdIn(activityIdsContainsNull))
      .isInstanceOf(NotValidException.class)
      .hasMessage("activityIds contains null value");

    String[] activityIdsContainsEmptyString = {"a", "", "b"};

    assertThatThrownBy(() -> query.activityIdIn(activityIdsContainsEmptyString))
      .isInstanceOf(NotValidException.class)
      .hasMessage("activityIds contains empty string");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByExecutionId() {
    runtimeService.startProcessInstanceByKey("process");
    String executionId = managementService.createJobQuery().singleResult().getExecutionId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().executionIdIn(executionId);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidExecutionId() {
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().executionIdIn("invalid");

    verifyQueryResults(query, 0);

    String[] nullValue = null;

    assertThatThrownBy(() -> query.executionIdIn(nullValue))
      .isInstanceOf(NotValidException.class)
      .hasMessage("executionIds is null");

    String[] executionIdsContainsNull = {"a", null, "b"};

    assertThatThrownBy(() -> query.executionIdIn(executionIdsContainsNull))
      .isInstanceOf(NotValidException.class)
      .hasMessage("executionIds contains null value");

    String[] executionIdsContainsEmptyString = {"a", "", "b"};

    assertThatThrownBy(() -> query.executionIdIn(executionIdsContainsEmptyString))
      .isInstanceOf(NotValidException.class)
      .hasMessage("executionIds contains empty string");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByProcessInstanceId() {
    runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = managementService.createJobQuery().singleResult().getProcessInstanceId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().processInstanceId(processInstanceId);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidProcessInstanceId() {
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().processInstanceId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.processInstanceId(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("processInstanceId is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByProcessDefinitionId() {
    runtimeService.startProcessInstanceByKey("process");
    String processDefinitionId = managementService.createJobQuery().singleResult().getProcessDefinitionId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().processDefinitionId(processDefinitionId);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidProcessDefinitionId() {
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().processDefinitionId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.processDefinitionId(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("processDefinitionId is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByProcessDefinitionKey() {
    runtimeService.startProcessInstanceByKey("process");
    String processDefinitionKey = managementService.createJobQuery().singleResult().getProcessDefinitionKey();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().processDefinitionKey(processDefinitionKey);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidProcessDefinitionKey() {
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().processDefinitionKey("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.processDefinitionKey(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("processDefinitionKey is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByDeploymentId() {
    runtimeService.startProcessInstanceByKey("process");
    String deploymentId = managementService.createJobQuery().singleResult().getDeploymentId();

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().deploymentId(deploymentId);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void shouldQueryCreateLogByHostname() {
    // given
    String testHostname1 = "HOST_1";
    processEngineConfiguration.setHostname(testHostname1);
    startProcessInstanceWithJob(false);

    String testHostname2 = "HOST_2";
    processEngineConfiguration.setHostname(testHostname2);
    startProcessInstanceWithJob(false);

    // when
    HistoricJobLogQuery query1 = historyService.createHistoricJobLogQuery()
                                               .creationLog()
                                               .hostname(testHostname1);
    HistoricJobLogQuery query2 = historyService.createHistoricJobLogQuery()
                                               .creationLog()
                                               .hostname(testHostname2);

    // then
    verifyQueryResults(query1, 1);
    verifyQueryResults(query2, 1);
    assertThat(query1.singleResult().getHostname())
        .isNotEqualToIgnoringCase(query2.singleResult().getHostname());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void shouldQuerySuccessLogByHostname() {
    // given
    String testHostname1 = "HOST_1";
    processEngineConfiguration.setHostname(testHostname1);
    startProcessInstanceWithJobAndCompleteJob(false);

    String testHostname2 = "HOST_2";
    processEngineConfiguration.setHostname(testHostname2);
    startProcessInstanceWithJobAndCompleteJob(false);

    // when
    HistoricJobLogQuery query1 = historyService.createHistoricJobLogQuery()
                                               .successLog()
                                               .hostname(testHostname1);
    HistoricJobLogQuery query2 = historyService.createHistoricJobLogQuery()
                                               .successLog()
                                               .hostname(testHostname2);

    // then
    verifyQueryResults(query1, 1);
    verifyQueryResults(query2, 1);
    assertThat(query1.singleResult().getHostname())
        .isNotEqualToIgnoringCase(query2.singleResult().getHostname());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void shouldQueryFailureLogByHostname() {
    // given
    String testHostname1 = "HOST_1";
    processEngineConfiguration.setHostname(testHostname1);

    // when/then
    assertThatThrownBy(() -> startProcessInstanceWithJobAndCompleteJob(true))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Expected_exception");

    String testHostname2 = "HOST_2";
    processEngineConfiguration.setHostname(testHostname2);

    // when/then
    assertThatThrownBy(() -> startProcessInstanceWithJobAndCompleteJob(true))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Expected_exception");

    // when
    HistoricJobLogQuery query1 = historyService.createHistoricJobLogQuery()
                                               .failureLog()
                                               .hostname(testHostname1);
    HistoricJobLogQuery query2 = historyService.createHistoricJobLogQuery()
                                               .failureLog()
                                               .hostname(testHostname2);

    // then
    verifyQueryResults(query1, 1);
    verifyQueryResults(query2, 1);
    assertThat(query1.singleResult().getHostname())
        .isNotEqualToIgnoringCase(query2.singleResult().getHostname());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void shouldQueryDeletionLogByHostname() {
    // given
    String testHostname1 = "HOST_1";
    processEngineConfiguration.setHostname(testHostname1);
    String pId1 = startProcessInstanceWithJob(false);
    runtimeService.deleteProcessInstance(pId1, "delete job log");

    String testHostname2 = "HOST_2";
    processEngineConfiguration.setHostname(testHostname2);
    String pId2 = startProcessInstanceWithJob(false);
    runtimeService.deleteProcessInstance(pId2, "delete job log");

    // when
    HistoricJobLogQuery query1 = historyService.createHistoricJobLogQuery()
                                               .deletionLog()
                                               .hostname(testHostname1);
    HistoricJobLogQuery query2 = historyService.createHistoricJobLogQuery()
                                               .deletionLog()
                                               .hostname(testHostname2);

    // then
    verifyQueryResults(query1, 1);
    verifyQueryResults(query2, 1);
    assertThat(query1.singleResult().getHostname())
        .isNotEqualToIgnoringCase(query2.singleResult().getHostname());
  }

  @Test
  void testQueryByInvalidDeploymentId() {
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().deploymentId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.deploymentId(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("deploymentId is null");
  }

  @Deployment
  @Test
  void testQueryByJobPriority() {
    // given 5 process instances with 5 jobs
    for (int i = 0; i < 5; i++) {
      runtimeService.startProcessInstanceByKey("process",
          Variables.createVariables().putValue("priority", i));
    }

    // then the creation logs can be filtered by priority of the jobs
    // (1) lower than or equal a priority
    List<HistoricJobLog> jobLogs = historyService.createHistoricJobLogQuery()
        .jobPriorityLowerThanOrEquals(2L)
        .orderByJobPriority()
        .asc()
        .list();

    assertThat(jobLogs).hasSize(3);
    for (HistoricJobLog log : jobLogs) {
      assertThat(log.getJobPriority()).isLessThanOrEqualTo(2);
    }

    // (2) higher than or equal a given priorty
    jobLogs = historyService.createHistoricJobLogQuery()
        .jobPriorityHigherThanOrEquals(3L)
        .orderByJobPriority()
        .asc()
        .list();

    assertThat(jobLogs).hasSize(2);
    for (HistoricJobLog log : jobLogs) {
      assertThat(log.getJobPriority()).isGreaterThanOrEqualTo(3);
    }

    // (3) lower and higher than or equal
    jobLogs = historyService.createHistoricJobLogQuery()
        .jobPriorityHigherThanOrEquals(1L)
        .jobPriorityLowerThanOrEquals(3L)
        .orderByJobPriority()
        .asc()
        .list();

    assertThat(jobLogs).hasSize(3);
    for (HistoricJobLog log : jobLogs) {
      assertThat(log.getJobPriority() >= 1 && log.getJobPriority() <= 3).isTrue();
    }

    // (4) lower and higher than or equal are disjunctive
    jobLogs = historyService.createHistoricJobLogQuery()
        .jobPriorityHigherThanOrEquals(3)
        .jobPriorityLowerThanOrEquals(1)
        .orderByJobPriority()
        .asc()
        .list();
    assertThat(jobLogs).isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByCreationLog() {
    runtimeService.startProcessInstanceByKey("process");

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().creationLog();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByFailureLog() {
    runtimeService.startProcessInstanceByKey("process");
    String jobId = managementService.createJobQuery().singleResult().getId();

    assertThatThrownBy(() -> managementService.executeJob(jobId))
      .isInstanceOf(ProcessEngineException.class)
      .hasStackTraceContaining(FailingDelegate.EXCEPTION_MESSAGE);

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().failureLog();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryBySuccessLog() {
    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("fail", false));
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.executeJob(jobId);

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().successLog();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryByDeletionLog() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();
    runtimeService.deleteProcessInstance(processInstanceId, null);

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery().deletionLog();

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQuerySorting() {
    for (int i = 0; i < 10; i++) {
      runtimeService.startProcessInstanceByKey("process");
    }

    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // asc
    query
      .orderByTimestamp()
      .asc();
    verifyQueryWithOrdering(query, 10, historicJobLogByTimestamp());

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByJobId()
      .asc();
    verifyQueryWithOrdering(query, 10, historicJobLogByJobId());

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByJobDefinitionId()
      .asc();
    verifyQueryWithOrdering(query, 10, historicJobLogByJobDefinitionId());

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByJobDueDate()
      .asc();
    verifyQueryWithOrdering(query, 10, historicJobLogByJobDueDate());

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByJobRetries()
      .asc();
    verifyQueryWithOrdering(query, 10, historicJobLogByJobRetries());

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByActivityId()
      .asc();
    verifyQueryWithOrdering(query, 10, historicJobLogByActivityId());

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByExecutionId()
      .asc();
    verifyQueryWithOrdering(query, 10, historicJobLogByExecutionId());

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByProcessInstanceId()
      .asc();
    verifyQueryWithOrdering(query, 10, historicJobLogByProcessInstanceId());

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByProcessDefinitionId()
      .asc();
    verifyQueryWithOrdering(query, 10, historicJobLogByProcessDefinitionId());

    query = historyService.createHistoricJobLogQuery();

    ProcessEngine processEngine = engineRule.getProcessEngine();
    query
      .orderByProcessDefinitionKey()
      .asc();
    verifyQueryWithOrdering(query, 10, historicJobLogByProcessDefinitionKey(processEngine));

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByDeploymentId()
      .asc();
    verifyQueryWithOrdering(query, 10, historicJobLogByDeploymentId());

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByJobPriority()
      .asc();

    verifyQueryWithOrdering(query, 10, historicJobLogByJobPriority());

    // desc
    query
      .orderByTimestamp()
      .desc();
    verifyQueryWithOrdering(query, 10, inverted(historicJobLogByTimestamp()));

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByJobId()
      .desc();
    verifyQueryWithOrdering(query, 10, inverted(historicJobLogByJobId()));

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByJobDefinitionId()
      .asc();
    verifyQueryWithOrdering(query, 10, inverted(historicJobLogByJobDefinitionId()));

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByJobDueDate()
      .desc();
    verifyQueryWithOrdering(query, 10, inverted(historicJobLogByJobDueDate()));

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByJobRetries()
      .desc();
    verifyQueryWithOrdering(query, 10, inverted(historicJobLogByJobRetries()));

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByActivityId()
      .desc();
    verifyQueryWithOrdering(query, 10, inverted(historicJobLogByActivityId()));

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByExecutionId()
      .desc();
    verifyQueryWithOrdering(query, 10, inverted(historicJobLogByExecutionId()));

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByProcessInstanceId()
      .desc();
    verifyQueryWithOrdering(query, 10, inverted(historicJobLogByProcessInstanceId()));

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByProcessDefinitionId()
      .desc();
    verifyQueryWithOrdering(query, 10, inverted(historicJobLogByProcessDefinitionId()));

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByProcessDefinitionKey()
      .desc();
    verifyQueryWithOrdering(query, 10, inverted(historicJobLogByProcessDefinitionKey(processEngine)));

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByDeploymentId()
      .desc();
    verifyQueryWithOrdering(query, 10, inverted(historicJobLogByDeploymentId()));

    query = historyService.createHistoricJobLogQuery();

    query
      .orderByJobPriority()
      .desc();

  verifyQueryWithOrdering(query, 10, inverted(historicJobLogByJobPriority()));
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQuerySortingPartiallyByOccurrence() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();
    String jobId = managementService.createJobQuery().singleResult().getId();

    testRule.executeAvailableJobs();
    runtimeService.setVariable(processInstanceId, "fail", false);
    managementService.executeJob(jobId);

    // asc
    HistoricJobLogQuery query = historyService
      .createHistoricJobLogQuery()
      .jobId(jobId)
      .orderPartiallyByOccurrence()
      .asc();

    verifyQueryWithOrdering(query, 5, historicJobLogPartiallyByOccurence());

    // desc
    query = historyService
        .createHistoricJobLogQuery()
        .jobId(jobId)
        .orderPartiallyByOccurrence()
        .desc();

    verifyQueryWithOrdering(query, 5, inverted(historicJobLogPartiallyByOccurence()));

    runtimeService.deleteProcessInstance(processInstanceId, null);

    // delete job /////////////////////////////////////////////////////////

    runtimeService.startProcessInstanceByKey("process").getId();
    jobId = managementService.createJobQuery().singleResult().getId();

    testRule.executeAvailableJobs();
    managementService.deleteJob(jobId);

    // asc
    query = historyService
      .createHistoricJobLogQuery()
      .jobId(jobId)
      .orderPartiallyByOccurrence()
      .asc();

    verifyQueryWithOrdering(query, 5, historicJobLogPartiallyByOccurence());

    // desc
    query = historyService
        .createHistoricJobLogQuery()
        .jobId(jobId)
        .orderPartiallyByOccurrence()
        .desc();

    verifyQueryWithOrdering(query, 5, inverted(historicJobLogPartiallyByOccurence()));
  }

  /**
   * Results in the creation of a historic job log.
   * @param failJob controlls if the job will fail or not.
   * @return the ProcessInstance ID
   */
  protected String startProcessInstanceWithJob(boolean failJob) {
    return runtimeService.startProcessInstanceByKey("process",
                                                    Variables.createVariables()
                                                             .putValue("fail", failJob)
                                                   ).getId();
  }

  protected void startProcessInstanceWithJobAndCompleteJob(boolean failJob) {
    String pId = startProcessInstanceWithJob(failJob);
    String jobId = managementService.createJobQuery().processInstanceId(pId).singleResult().getId();
    managementService.executeJob(jobId);
  }

  protected void verifyQueryWithOrdering(HistoricJobLogQuery query, int countExpected, NullTolerantComparator<HistoricJobLog> expectedOrdering) {
    verifyQueryResults(query, countExpected);
    TestOrderingUtil.verifySorting(query.list(), expectedOrdering);
  }

}
