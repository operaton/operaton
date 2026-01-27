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
package org.operaton.bpm.engine.test.api.history.removaltime;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.AuthorizationQuery;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInputInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionOutputInstance;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricExternalTaskLog;
import org.operaton.bpm.engine.history.HistoricIdentityLinkLog;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.history.DefaultHistoryRemovalTimeProvider;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionInputInstanceEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionOutputInstanceEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricExternalTaskLogEntity;
import org.operaton.bpm.engine.impl.persistence.entity.AttachmentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricJobLogEventEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.operaton.bpm.engine.impl.test.RequiredDatabase;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Attachment;
import org.operaton.bpm.engine.task.Comment;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.dmn.businessruletask.TestPojo;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_REMOVAL_TIME_STRATEGY_END;
import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobIgnoringException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Tassilo Weidner
 */
class RemovalTimeStrategyEndTest extends AbstractRemovalTimeTest {

  @BeforeEach
  void setUp() {
    processEngineConfiguration
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_END)
      .setHistoryRemovalTimeProvider(new DefaultHistoryRemovalTimeProvider())
      .initHistoryRemovalTime();
  }

  @AfterEach
  void clearDatabase() {
    clearAuthorization();
  }

  static final String CALLED_PROCESS_KEY = "calledProcess";

  static final BpmnModelInstance CALLED_PROCESS = Bpmn.createExecutableProcess(CALLED_PROCESS_KEY)
      .operatonHistoryTimeToLive(180)
      .startEvent()
      .userTask("userTask")
      .name("userTask")
      .operatonCandidateUsers("foo")
      .endEvent()
      .done();

  static final String CALLING_PROCESS_KEY = "callingProcess";
  static final BpmnModelInstance CALLING_PROCESS = Bpmn.createExecutableProcess(CALLING_PROCESS_KEY)
    .operatonHistoryTimeToLive(5)
    .startEvent()
      .callActivity()
        .calledElement(CALLED_PROCESS_KEY)
    .endEvent().done();

  static final Date START_DATE = new Date(1363607000000L);
  static final Date END_DATE = new GregorianCalendar(2013, Calendar.MARCH, 18, 13, 0, 0).getTime();

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldResolveHistoricDecisionInstance() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("process")
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .businessRuleTask()
          .operatonAsyncAfter()
          .operatonDecisionRef("dish-decision")
      .endEvent().done());

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey("process",
      Variables.createVariables()
        .putValue("temperature", 32)
        .putValue("dayType", "Weekend"));

    ClockUtil.setCurrentTime(END_DATE);

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // assume
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isNull();
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isNull();

    String jobId = managementService.createJobQuery().singleResult().getId();

    // when
    managementService.executeJob(jobId);

    historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldResolveHistoricDecisionInputInstance() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("process")
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .businessRuleTask()
          .operatonAsyncAfter()
          .operatonDecisionRef("dish-decision")
      .endEvent().done());

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey("process",
      Variables.createVariables()
        .putValue("temperature", 32)
        .putValue("dayType", "Weekend"));

    ClockUtil.setCurrentTime(END_DATE);

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeInputs()
      .singleResult();

    List<HistoricDecisionInputInstance> historicDecisionInputInstances = historicDecisionInstance.getInputs();

    // assume
    assertThat(historicDecisionInputInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInputInstances.get(1).getRemovalTime()).isNull();

    String jobId = managementService.createJobQuery().singleResult().getId();

    // when
    managementService.executeJob(jobId);

    historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeInputs()
      .singleResult();

    historicDecisionInputInstances = historicDecisionInstance.getInputs();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(historicDecisionInputInstances.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicDecisionInputInstances.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldResolveHistoricDecisionOutputInstance() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("process")
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .businessRuleTask()
          .operatonAsyncAfter()
          .operatonDecisionRef("dish-decision")
      .endEvent().done());

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey("process",
      Variables.createVariables()
        .putValue("temperature", 32)
        .putValue("dayType", "Weekend"));

    ClockUtil.setCurrentTime(END_DATE);

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeOutputs()
      .singleResult();

    List<HistoricDecisionOutputInstance> historicDecisionOutputInstances = historicDecisionInstance.getOutputs();

    // assume
    assertThat(historicDecisionOutputInstances.get(0).getRemovalTime()).isNull();

    String jobId = managementService.createJobQuery().singleResult().getId();

    // when
    managementService.executeJob(jobId);

    historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeOutputs()
      .singleResult();

    historicDecisionOutputInstances = historicDecisionInstance.getOutputs();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(historicDecisionOutputInstances.get(0).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveHistoricProcessInstance() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
      .activeActivityIdIn("userTask")
      .singleResult();

    // assume
    assertThat(historicProcessInstance.getRemovalTime()).isNull();

    String taskId = historyService.createHistoricTaskInstanceQuery().singleResult().getId();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
      .processDefinitionKey(CALLED_PROCESS_KEY)
      .singleResult();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(historicProcessInstance.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveHistoricActivityInstance() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = historyService.createHistoricTaskInstanceQuery().singleResult().getId();

    ClockUtil.setCurrentTime(END_DATE);

    HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
      .activityId("userTask")
      .singleResult();

    // assume
    assertThat(historicActivityInstance.getRemovalTime()).isNull();

    // when
    taskService.complete(taskId);

    historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
      .activityId("userTask")
      .singleResult();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(historicActivityInstance.getRemovalTime()).isEqualTo(removalTime);
  }

  /**
   * Test excluded on MariaDB and MySQL for now since it is failing randomly there.
   * See CAM-13291 for details.
   */
  @Test
  @RequiredDatabase(excludes = {DbSqlSessionFactory.MARIADB, DbSqlSessionFactory.MYSQL})
  void shouldResolveHistoricActivityInstanceInConcurrentEnvironment() {
    // given
    int degreeOfParallelism = 30;

    testRule.deploy(Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(5)
        .startEvent()
        .serviceTask().operatonExpression("${true}")
        .endEvent()
        .done());

    ClockUtil.setCurrentTime(START_DATE);

    // when/then
    assertThatCode(() -> IntStream.range(0, degreeOfParallelism).parallel()
        .forEach(i -> runtimeService.startProcessInstanceByKey("process")))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldResolveHistoricTaskInstance() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();

    // assume
    assertThat(historicTaskInstance.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = historyService.createHistoricTaskInstanceQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(historicTaskInstance.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveHistoricTaskAuthorization_HistoricTaskInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    Authorization authorization =
        authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);

    authorization.setUserId("myUserId");
    authorization.setResource(Resources.HISTORIC_TASK);
    authorization.setResourceId(taskId);

    // when
    authorizationService.saveAuthorization(authorization);

    authorization = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_TASK)
        .singleResult();

    // assume
    assertThat(authorization.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    Date removalTime = addDays(END_DATE, 5);

    authorization = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_TASK)
        .singleResult();

    // then
    assertThat(authorization.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResetAuthorizationAfterUpdate_HistoricTaskInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    enabledAuth();
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);
    disableAuth();

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    Authorization authorization = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_TASK)
        .singleResult();

    Date removalTime = addDays(END_DATE, 5);

    // assume
    assertThat(authorization.getRootProcessInstanceId()).isEqualTo(processInstance.getProcessInstanceId());
    assertThat(authorization.getRemovalTime()).isEqualTo(removalTime);

    authorization.setResourceId("*");

    // when
    authorizationService.saveAuthorization(authorization);

    authorization = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_TASK)
        .singleResult();

    // then
    assertThat(authorization.getRootProcessInstanceId()).isNull();
    assertThat(authorization.getRemovalTime()).isNull();
  }

  @Test
  void shouldResolveAuthorizationAfterUpdate_HistoricTaskInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    authorization.setResource(Resources.HISTORIC_TASK);
    authorization.setResourceId("*");
    authorization.setUserId("foo");

    authorizationService.saveAuthorization(authorization);

    authorization = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_TASK)
        .singleResult();

    // assume
    assertThat(authorization.getRootProcessInstanceId()).isNull();
    assertThat(authorization.getRemovalTime()).isNull();

    taskId = historyService.createHistoricTaskInstanceQuery().singleResult().getId();

    authorization.setResourceId(taskId);

    // when
    authorizationService.saveAuthorization(authorization);

    authorization = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_TASK)
        .singleResult();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(authorization.getRootProcessInstanceId()).isEqualTo(processInstance.getRootProcessInstanceId());
    assertThat(authorization.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveHistoricTaskAuthorization_HistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    String rootProcessInstanceId = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY)
        .getProcessInstanceId();

    Authorization authorization =
        authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);

    authorization.setUserId("myUserId");
    authorization.setResource(Resources.HISTORIC_PROCESS_INSTANCE);

    String processInstanceId = historyService.createHistoricProcessInstanceQuery()
        .activeActivityIdIn("userTask")
        .singleResult()
        .getId();

    authorization.setResourceId(processInstanceId);

    authorizationService.saveAuthorization(authorization);

    // assume
    AuthorizationQuery authQuery = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_PROCESS_INSTANCE);

    assertThat(authQuery.list())
        .extracting("removalTime", "resourceId", "rootProcessInstanceId")
        .containsExactly(tuple(null, processInstanceId, rootProcessInstanceId));

    // when
    String taskId = taskService.createTaskQuery().singleResult().getId();

    ClockUtil.setCurrentTime(END_DATE);

    taskService.complete(taskId);

    // then
    Date removalTime = addDays(END_DATE, 5);

    authQuery = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_PROCESS_INSTANCE);

    assertThat(authQuery.list())
        .extracting("removalTime", "resourceId", "rootProcessInstanceId")
        .containsExactly(tuple(removalTime, processInstanceId, rootProcessInstanceId));
  }

  @Test
  void shouldResetAuthorizationAfterUpdate_HistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    enabledAuth();
    String rootProcessInstanceId = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY)
        .getProcessInstanceId();
    disableAuth();

    Authorization authorization =
        authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);

    authorization.setUserId("myUserId");
    authorization.setResource(Resources.HISTORIC_PROCESS_INSTANCE);

    String processInstanceId = historyService.createHistoricProcessInstanceQuery()
        .activeActivityIdIn("userTask")
        .singleResult()
        .getId();

    authorization.setResourceId(processInstanceId);

    authorizationService.saveAuthorization(authorization);

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    // assume
    AuthorizationQuery authQuery = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_PROCESS_INSTANCE);

    Date removalTime = addDays(END_DATE, 5);
    assertThat(authQuery.list())
        .extracting("removalTime", "resourceId", "rootProcessInstanceId")
        .containsExactly(tuple(removalTime, processInstanceId, rootProcessInstanceId));

    // when
    authorization.setResourceId("*");
    authorizationService.saveAuthorization(authorization);

    // then
    authQuery = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_PROCESS_INSTANCE);

    assertThat(authQuery.list())
        .extracting("removalTime", "resourceId", "rootProcessInstanceId")
        .containsExactly(tuple(null, "*", null));
  }

  @Test
  void shouldResolveAuthorizationAfterUpdate_HistoricProcessInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    ProcessInstance rootProcessInstance =
        runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.complete(taskId);

    Authorization authorization =
        authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    authorization.setResource(Resources.HISTORIC_PROCESS_INSTANCE);
    authorization.setResourceId("*");
    authorization.setUserId("foo");

    authorizationService.saveAuthorization(authorization);

    // assume
    AuthorizationQuery authQuery = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_PROCESS_INSTANCE);

    assertThat(authQuery.list())
        .extracting("removalTime", "resourceId", "rootProcessInstanceId")
        .containsExactly(tuple(null, "*", null));

    // when
    String processInstanceId = historyService.createHistoricProcessInstanceQuery()
        .executedActivityIdIn("userTask")
        .singleResult()
        .getId();

    authorization.setResourceId(processInstanceId);

    authorizationService.saveAuthorization(authorization);

    // then
    authQuery = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_PROCESS_INSTANCE);

    Date removalTime = addDays(END_DATE, 5);
    String rootProcessInstanceId = rootProcessInstance.getRootProcessInstanceId();
    assertThat(authQuery.list())
        .extracting("removalTime", "resourceId", "rootProcessInstanceId")
        .containsExactly(tuple(removalTime, processInstanceId, rootProcessInstanceId));
  }

  @Test
  void shouldWriteHistoryAndResolveHistoricTaskAuthorizationInDifferentTransactions() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    enabledAuth();
    taskService.setAssignee(taskId, "myUserId");
    disableAuth();

    Authorization authorization = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_TASK)
        .singleResult();

    // assume
    assertThat(authorization.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    authorization = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_TASK)
        .singleResult();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(authorization.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldWriteHistoryAndResolveHistoricTaskAuthorizationInSameTransaction() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    enabledAuth();
    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);
    disableAuth();

    String taskId = taskService.createTaskQuery().singleResult().getId();

    Authorization authorization = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_TASK)
        .singleResult();

    // assume
    assertThat(authorization.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    authorization = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_TASK)
        .singleResult();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(authorization.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveVariableInstance() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY,
      Variables.createVariables()
        .putValue("aVariableName", Variables.stringValue("aVariableValue")));

    runtimeService.setVariable(processInstance.getId(), "aVariableName", Variables.stringValue("anotherVariableValue"));

    HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();

    // assume
    assertThat(historicVariableInstance.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = historyService.createHistoricTaskInstanceQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    historicVariableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(historicVariableInstance.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveHistoricDetailByVariableInstanceUpdate() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY,
      Variables.createVariables()
        .putValue("aVariableName", Variables.stringValue("aVariableValue")));

    runtimeService.setVariable(processInstance.getId(), "aVariableName", Variables.stringValue("anotherVariableValue"));

    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery()
      .variableUpdates()
      .list();

    // assume
    assertThat(historicDetails.get(0).getRemovalTime()).isNull();
    assertThat(historicDetails.get(1).getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = historyService.createHistoricTaskInstanceQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    historicDetails = historyService.createHistoricDetailQuery()
      .variableUpdates()
      .list();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(historicDetails.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicDetails.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveHistoricDetailByFormProperty() {
    // given
    DeploymentWithDefinitions deployment = testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    String processDefinitionId = deployment.getDeployedProcessDefinitions().get(0).getId();
    Map<String, Object> properties = new HashMap<>();
    properties.put("aFormProperty", "aFormPropertyValue");

    ClockUtil.setCurrentTime(START_DATE);

    formService.submitStartForm(processDefinitionId, properties);

    HistoricDetail historicDetail = historyService.createHistoricDetailQuery().formFields().singleResult();

    // assume
    assertThat(historicDetail.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = historyService.createHistoricTaskInstanceQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    historicDetail = historyService.createHistoricDetailQuery().formFields().singleResult();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(historicDetail.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveIncident() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(Bpmn.createExecutableProcess(CALLED_PROCESS_KEY)
    .startEvent()
      .scriptTask()
        .operatonAsyncBefore()
        .scriptFormat("groovy")
        .scriptText("if(execution.getIncidents().size() == 0) throw new RuntimeException()")
      .userTask()
    .endEvent().done());

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String jobId = managementService.createJobQuery().singleResult().getId();

    managementService.setJobRetries(jobId, 0);

    executeJobIgnoringException(managementService, jobId);

    List<HistoricIncident> historicIncidents = historyService.createHistoricIncidentQuery().list();

    // assume
    assertThat(historicIncidents.get(0).getRemovalTime()).isNull();
    assertThat(historicIncidents.get(1).getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = historyService.createHistoricTaskInstanceQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    historicIncidents = historyService.createHistoricIncidentQuery().list();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(historicIncidents.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicIncidents.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  /**
   * See https://app.camunda.com/jira/browse/CAM-9505
   */
  @Test
  void shouldResolveIncidentWithPreservedCreateTime() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(Bpmn.createExecutableProcess(CALLED_PROCESS_KEY)
    .startEvent()
      .scriptTask()
        .operatonAsyncBefore()
        .scriptFormat("groovy")
        .scriptText("if(execution.getIncidents().size() == 0) throw new RuntimeException()")
      .userTask()
    .endEvent().done());

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String jobId = managementService.createJobQuery().singleResult().getId();

    managementService.setJobRetries(jobId, 0);

    executeJobIgnoringException(managementService, jobId);

    String taskId = historyService.createHistoricTaskInstanceQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    List<HistoricIncident> historicIncidents = historyService.createHistoricIncidentQuery().list();

    // then
    assertThat(historicIncidents.get(0).getCreateTime()).isEqualTo(START_DATE);
    assertThat(historicIncidents.get(1).getCreateTime()).isEqualTo(START_DATE);
  }

  @Test
  void shouldNotResolveStandaloneIncident() {
    // given
    ClockUtil.setCurrentTime(END_DATE);

    testRule.deploy(CALLED_PROCESS);

    repositoryService.suspendProcessDefinitionByKey(CALLED_PROCESS_KEY, true, new Date());

    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    managementService.setJobRetries(jobId, 0);

    // when
    managementService.executeJob(jobId);

    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();

    // assume
    assertThat(historicIncident).isNotNull();

    // then
    assertThat(historicIncident.getRemovalTime()).isNull();

    // cleanup
    clearJobLog(jobId);
    clearHistoricIncident(historicIncident);
  }

  @Test
  void shouldResolveExternalTaskLog() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("calledProcess")
      .startEvent()
        .serviceTask().operatonExternalTask("anExternalTaskTopic")
      .endEvent().done());

    testRule.deploy(Bpmn.createExecutableProcess("callingProcess")
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .callActivity()
          .calledElement("calledProcess")
      .endEvent().done());

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey("callingProcess");

    LockedExternalTask externalTask = externalTaskService.fetchAndLock(1, "aWorkerId")
      .topic("anExternalTaskTopic", 3000)
      .execute()
      .get(0);

    HistoricExternalTaskLog externalTaskLog = historyService.createHistoricExternalTaskLogQuery().singleResult();

    // assume
    assertThat(externalTaskLog.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    externalTaskService.complete(externalTask.getId(), "aWorkerId");

    Date removalTime = addDays(END_DATE, 5);

    List<HistoricExternalTaskLog> externalTaskLogs = historyService.createHistoricExternalTaskLogQuery().list();

    // then
    assertThat(externalTaskLogs.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(externalTaskLogs.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  /**
   * See https://app.camunda.com/jira/browse/CAM-9505
   */
  @Test
  void shouldResolveExternalTaskLogWithTimestampPreserved() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("calledProcess")
      .startEvent()
        .serviceTask().operatonExternalTask("anExternalTaskTopic")
      .endEvent().done());

    testRule.deploy(Bpmn.createExecutableProcess("callingProcess")
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .callActivity()
          .calledElement("calledProcess")
      .endEvent().done());

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey("callingProcess");

    LockedExternalTask externalTask = externalTaskService.fetchAndLock(1, "aWorkerId")
      .topic("anExternalTaskTopic", 3000)
      .execute()
      .get(0);

    // when
    externalTaskService.complete(externalTask.getId(), "aWorkerId");

    List<HistoricExternalTaskLog> externalTaskLogs = historyService.createHistoricExternalTaskLogQuery().list();

    // then
    assertThat(externalTaskLogs.get(0).getTimestamp()).isEqualTo(START_DATE);
    assertThat(externalTaskLogs.get(1).getTimestamp()).isEqualTo(START_DATE);
  }

  @Test
  void shouldResolveJobLog() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(Bpmn.createExecutableProcess(CALLED_PROCESS_KEY)
      .startEvent().operatonAsyncBefore()
        .userTask("userTask").name("userTask")
      .endEvent().done());

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    executeJobIgnoringException(managementService, jobId);

    List<HistoricJobLog> jobLog = historyService.createHistoricJobLogQuery().list();

    // assume
    assertThat(jobLog.get(0).getRemovalTime()).isNull();
    assertThat(jobLog.get(1).getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    jobLog = historyService.createHistoricJobLogQuery().list();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(jobLog.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(jobLog.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  /**
   * See https://app.camunda.com/jira/browse/CAM-9505
   */
  @Test
  void shouldResolveJobLogWithTimestampPreserved() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(Bpmn.createExecutableProcess(CALLED_PROCESS_KEY)
      .startEvent().operatonAsyncBefore()
        .userTask("userTask").name("userTask")
      .endEvent().done());

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    executeJobIgnoringException(managementService, jobId);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    List<HistoricJobLog> jobLog = historyService.createHistoricJobLogQuery().list();

    // then
    assertThat(jobLog.get(0).getTimestamp()).isEqualTo(START_DATE);
    assertThat(jobLog.get(1).getTimestamp()).isEqualTo(START_DATE);
  }

  @Test
  void shouldResolveUserOperationLog_SetJobRetries() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(Bpmn.createExecutableProcess(CALLED_PROCESS_KEY)
      .startEvent().operatonAsyncBefore()
        .userTask("userTask").name("userTask")
      .endEvent().done());

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    identityService.setAuthenticatedUserId("aUserId");
    managementService.setJobRetries(jobId, 65);
    identityService.clearAuthentication();

    UserOperationLogEntry userOperationLog = historyService.createUserOperationLogQuery().singleResult();

    // assume
    assertThat(userOperationLog.getRemovalTime()).isNull();

    managementService.executeJob(jobId);

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    userOperationLog = historyService.createUserOperationLogQuery().singleResult();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(userOperationLog.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveUserOperationLog_SetExternalTaskRetries() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("calledProcess")
      .startEvent()
        .serviceTask().operatonExternalTask("anExternalTaskTopic")
      .endEvent().done());

    testRule.deploy(Bpmn.createExecutableProcess("callingProcess")
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .callActivity()
          .calledElement("calledProcess")
      .endEvent().done());

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey("callingProcess");

    String externalTaskId = externalTaskService.createExternalTaskQuery().singleResult().getId();

    identityService.setAuthenticatedUserId("aUserId");
    externalTaskService.setRetries(externalTaskId, 65);
    identityService.clearAuthentication();

    UserOperationLogEntry userOperationLog = historyService.createUserOperationLogQuery().singleResult();

    // assume
    assertThat(userOperationLog.getRemovalTime()).isNull();

    LockedExternalTask externalTask = externalTaskService.fetchAndLock(1, "aWorkerId")
      .topic("anExternalTaskTopic", 2000)
      .execute()
      .get(0);

    ClockUtil.setCurrentTime(END_DATE);

    // when
    externalTaskService.complete(externalTask.getId(), "aWorkerId");

    Date removalTime = addDays(END_DATE, 5);

    userOperationLog = historyService.createUserOperationLogQuery().singleResult();

    // then
    assertThat(userOperationLog.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveUserOperationLog_ClaimTask() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    identityService.setAuthenticatedUserId("aUserId");
    taskService.claim(taskId, "aUserId");
    identityService.clearAuthentication();

    UserOperationLogEntry userOperationLog = historyService.createUserOperationLogQuery().singleResult();

    // assume
    assertThat(userOperationLog.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    userOperationLog = historyService.createUserOperationLogQuery().singleResult();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(userOperationLog.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveUserOperationLog_CreateAttachment() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String processInstanceId = runtimeService.createProcessInstanceQuery().activityIdIn("userTask").singleResult().getId();

    identityService.setAuthenticatedUserId("aUserId");
    taskService.createAttachment(null, null, processInstanceId, null, null, "http://operaton.com");
    identityService.clearAuthentication();

    UserOperationLogEntry userOperationLog = historyService.createUserOperationLogQuery().singleResult();

    // assume
    assertThat(userOperationLog.getRemovalTime()).isNull();

    String taskId = taskService.createTaskQuery().singleResult().getId();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    userOperationLog = historyService.createUserOperationLogQuery().singleResult();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(userOperationLog.getRemovalTime()).isEqualTo(removalTime);
  }

  /**
   * See https://app.camunda.com/jira/browse/CAM-9505
   */
  @Test
  void shouldResolveUserOperationLogWithTimestampPreserved() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String processInstanceId = runtimeService.createProcessInstanceQuery().activityIdIn("userTask").singleResult().getId();

    identityService.setAuthenticatedUserId("aUserId");
    taskService.createAttachment(null, null, processInstanceId, null, null, "http://operaton.com");
    identityService.clearAuthentication();

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    UserOperationLogEntry userOperationLog = historyService.createUserOperationLogQuery().singleResult();

    // then
    assertThat(userOperationLog.getTimestamp()).isEqualTo(START_DATE);
  }

  @Test
  void shouldResolveIdentityLink_AddCandidateUser() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.addCandidateUser(taskId, "aUserId");

    HistoricIdentityLinkLog historicIdentityLinkLog =
        historyService.createHistoricIdentityLinkLogQuery()
            .userId("aUserId")
            .singleResult();

    // assume
    assertThat(historicIdentityLinkLog.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    historicIdentityLinkLog = historyService.createHistoricIdentityLinkLogQuery()
        .userId("aUserId")
        .singleResult();

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(historicIdentityLinkLog.getRemovalTime()).isEqualTo(removalTime);
  }

  /**
   * See https://app.camunda.com/jira/browse/CAM-9505
   */
  @Test
  void shouldResolveIdentityLinkWithTimePreserved() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.addCandidateUser(taskId, "aUserId");

    // when
    taskService.complete(taskId);

    HistoricIdentityLinkLog historicIdentityLinkLog =
        historyService.createHistoricIdentityLinkLogQuery()
            .userId("aUserId")
            .singleResult();

    // then
    assertThat(historicIdentityLinkLog.getTime()).isEqualTo(START_DATE);
  }

  @Test
  void shouldNotResolveIdentityLink_AddCandidateUser() {
    // given
    ClockUtil.setCurrentTime(END_DATE);

    Task aTask = taskService.newTask();
    taskService.saveTask(aTask);

    // when
    taskService.addCandidateUser(aTask.getId(), "aUserId");

    HistoricIdentityLinkLog historicIdentityLinkLog = historyService.createHistoricIdentityLinkLogQuery().singleResult();

    // assume
    assertThat(historicIdentityLinkLog).isNotNull();

    // then
    assertThat(historicIdentityLinkLog.getRemovalTime()).isNull();

    // cleanup
    taskService.complete(aTask.getId());
    clearHistoricTaskInst(aTask.getId());
  }

  @Test
  void shouldResolveCommentByProcessInstanceId() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String processInstanceId = runtimeService.createProcessInstanceQuery()
      .activityIdIn("userTask")
      .singleResult()
      .getId();

    taskService.createComment(null, processInstanceId, "aMessage");

    Comment comment = taskService.getProcessInstanceComments(processInstanceId).get(0);

    // assume
    assertThat(comment.getRemovalTime()).isNull();

    String taskId = taskService.createTaskQuery().singleResult().getId();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    comment = taskService.getProcessInstanceComments(processInstanceId).get(0);

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(comment.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveCommentByTaskId() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.createComment(taskId, null, "aMessage");

    Comment comment = taskService.getTaskComments(taskId).get(0);

    // assume
    assertThat(comment.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    comment = taskService.getTaskComments(taskId).get(0);

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(comment.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldNotResolveCommentByWrongTaskIdAndProcessInstanceId() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    ClockUtil.setCurrentTime(START_DATE);

    String processInstanceId = runtimeService.createProcessInstanceQuery()
      .activityIdIn("userTask")
      .singleResult()
      .getId();

    taskService.createComment("aNonExistentTaskId", processInstanceId, "aMessage");

    Comment comment = taskService.getProcessInstanceComments(processInstanceId).get(0);

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    // then
    assertThat(comment.getRemovalTime()).isNull();
  }

  @Test
  void shouldResolveCommentByTaskIdAndWrongProcessInstanceId() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.createComment(taskId, "aNonExistentProcessInstanceId", "aMessage");

    Comment comment = taskService.getTaskComments(taskId).get(0);

    // assume
    assertThat(comment.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    comment = taskService.getTaskComments(taskId).get(0);

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(comment.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveAttachmentByProcessInstanceId() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String processInstanceId = runtimeService.createProcessInstanceQuery()
      .activityIdIn("userTask")
      .singleResult()
      .getId();

    String attachmentId = taskService.createAttachment(null, null, processInstanceId, null, null, "http://operaton.com").getId();

    Attachment attachment = taskService.getAttachment(attachmentId);

    // assume
    assertThat(attachment.getRemovalTime()).isNull();

    String taskId = taskService.createTaskQuery().singleResult().getId();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    Date removalTime = addDays(END_DATE, 5);

    attachment = taskService.getAttachment(attachmentId);

    // then
    assertThat(attachment.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveAttachmentByTaskId() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    String attachmentId = taskService.createAttachment(null, taskId, null, null, null, "http://operaton.com").getId();

    Attachment attachment = taskService.getAttachment(attachmentId);

    // assume
    assertThat(attachment.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    attachment = taskService.getAttachment(attachmentId);

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(attachment.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldNotResolveAttachmentByWrongTaskIdAndProcessInstanceId() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String processInstanceId = runtimeService.createProcessInstanceQuery()
      .activityIdIn("userTask")
      .singleResult()
      .getId();

    String attachmentId = taskService.createAttachment(null, "aWrongTaskId", processInstanceId, null, null, "http://operaton.com").getId();

    Attachment attachment = taskService.getAttachment(attachmentId);

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    // then
    assertThat(attachment.getRemovalTime()).isNull();
  }

  @Test
  void shouldResolveAttachmentByTaskIdAndWrongProcessInstanceId() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery()
      .singleResult()
      .getId();

    String attachmentId = taskService.createAttachment(null, taskId, "aWrongProcessInstanceId", null, null, "http://operaton.com").getId();

    Attachment attachment = taskService.getAttachment(attachmentId);

    // assume
    assertThat(attachment.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    attachment = taskService.getAttachment(attachmentId);

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(attachment.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveByteArray_CreateAttachmentByTask() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    AttachmentEntity attachment = (AttachmentEntity) taskService.createAttachment(null, taskId, null, null, null, new ByteArrayInputStream("hello world".getBytes()));

    ByteArrayEntity byteArray = findByteArrayById(attachment.getContentId());

    // assume
    assertThat(byteArray.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    byteArray = findByteArrayById(attachment.getContentId());

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(byteArray.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveByteArray_CreateAttachmentByProcessInstance() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String calledProcessInstanceId = runtimeService.createProcessInstanceQuery()
      .activityIdIn("userTask")
      .singleResult()
      .getId();

    AttachmentEntity attachment = (AttachmentEntity) taskService.createAttachment(null, null, calledProcessInstanceId, null, null, new ByteArrayInputStream("hello world".getBytes()));

    ByteArrayEntity byteArray = findByteArrayById(attachment.getContentId());

    // assume
    assertThat(byteArray.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    byteArray = findByteArrayById(attachment.getContentId());

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(byteArray.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveByteArray_SetVariable() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    runtimeService.setVariable(processInstance.getId(), "aVariableName", new ByteArrayInputStream("hello world".getBytes()));

    HistoricVariableInstanceEntity historicVariableInstance = (HistoricVariableInstanceEntity) historyService.createHistoricVariableInstanceQuery().singleResult();

    ByteArrayEntity byteArray = findByteArrayById(historicVariableInstance.getByteArrayId());

    // assume
    assertThat(byteArray.getRemovalTime()).isNull();

    String taskId = taskService.createTaskQuery().singleResult().getId();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    byteArray = findByteArrayById(historicVariableInstance.getByteArrayId());

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(byteArray.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveByteArray_UpdateVariable() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY,
      Variables.createVariables()
        .putValue("aVariableName", Variables.stringValue("aVariableValue")));

    runtimeService.setVariable(processInstance.getId(), "aVariableName", new ByteArrayInputStream("hello world".getBytes()));

    HistoricDetailVariableInstanceUpdateEntity historicDetails = (HistoricDetailVariableInstanceUpdateEntity) historyService.createHistoricDetailQuery()
      .variableUpdates()
      .variableTypeIn("Bytes")
      .singleResult();

    ByteArrayEntity byteArray = findByteArrayById(historicDetails.getByteArrayValueId());

    // assume
    assertThat(byteArray.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    byteArray = findByteArrayById(historicDetails.getByteArrayValueId());

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(byteArray.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveByteArray_JobLog() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(Bpmn.createExecutableProcess(CALLED_PROCESS_KEY)
      .startEvent()
        .scriptTask()
          .operatonAsyncBefore()
          .scriptFormat("groovy")
          .scriptText("if(execution.getIncidents().size() == 0) throw new RuntimeException(\"I'm supposed to fail!\")")
      .endEvent().done());

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    executeJobIgnoringException(managementService, jobId);

    HistoricJobLogEventEntity jobLog = (HistoricJobLogEventEntity) historyService.createHistoricJobLogQuery()
      .failureLog()
      .singleResult();

    ByteArrayEntity byteArray = findByteArrayById(jobLog.getExceptionByteArrayId());

    // assume
    assertThat(byteArray.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    managementService.setJobRetries(jobId, 0);

    // when
    executeJobIgnoringException(managementService, jobId);

    Date removalTime = addDays(END_DATE, 5);

    byteArray = findByteArrayById(jobLog.getExceptionByteArrayId());

    // then
    assertThat(byteArray.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveByteArray_ExternalTaskLog() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("calledProcess")
      .startEvent()
        .serviceTask().operatonExternalTask("aTopicName")
      .endEvent().done());

    testRule.deploy(Bpmn.createExecutableProcess("callingProcess")
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .callActivity()
          .calledElement("calledProcess")
      .endEvent().done());

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey("callingProcess");

    String taskId = externalTaskService.fetchAndLock(5, "aWorkerId")
      .topic("aTopicName", Integer.MAX_VALUE)
      .execute()
      .get(0)
      .getId();

    externalTaskService.handleFailure(taskId, "aWorkerId", null, "errorDetails", 5, 3000L);

    HistoricExternalTaskLogEntity externalTaskLog = (HistoricExternalTaskLogEntity) historyService.createHistoricExternalTaskLogQuery()
      .failureLog()
      .singleResult();

    ByteArrayEntity byteArrayEntity = findByteArrayById(externalTaskLog.getErrorDetailsByteArrayId());

    // assume
    assertThat(byteArrayEntity.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    externalTaskService.complete(taskId, "aWorkerId");

    byteArrayEntity = findByteArrayById(externalTaskLog.getErrorDetailsByteArrayId());

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(byteArrayEntity.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml"
  })
  void shouldResolveByteArray_DecisionInput() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess(CALLING_PROCESS_KEY)
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .businessRuleTask().operatonDecisionRef("testDecision")
        .userTask()
      .endEvent().done());

    ClockUtil.setCurrentTime(START_DATE);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY,
      Variables.createVariables()
        .putValue("pojo", new TestPojo("okay", 13.37)));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeInputs()
      .singleResult();

    HistoricDecisionInputInstanceEntity historicDecisionInputInstanceEntity = (HistoricDecisionInputInstanceEntity) historicDecisionInstance.getInputs().get(0);

    ByteArrayEntity byteArrayEntity = findByteArrayById(historicDecisionInputInstanceEntity.getByteArrayValueId());

    // assume
    assertThat(byteArrayEntity.getRemovalTime()).isNull();

    String taskId = taskService.createTaskQuery().singleResult().getId();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    byteArrayEntity = findByteArrayById(historicDecisionInputInstanceEntity.getByteArrayValueId());

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(byteArrayEntity.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml"
  })
  void shouldResolveByteArray_DecisionOutput() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(Bpmn.createExecutableProcess(CALLING_PROCESS_KEY)
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .businessRuleTask().operatonDecisionRef("testDecision")
        .userTask()
      .endEvent().done());

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY,
      Variables.createVariables()
        .putValue("pojo", new TestPojo("okay", 13.37)));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeOutputs()
      .singleResult();

    HistoricDecisionOutputInstanceEntity historicDecisionOutputInstanceEntity = (HistoricDecisionOutputInstanceEntity) historicDecisionInstance.getOutputs().get(0);

    ByteArrayEntity byteArrayEntity = findByteArrayById(historicDecisionOutputInstanceEntity.getByteArrayValueId());

    // assume
    assertThat(byteArrayEntity.getRemovalTime()).isNull();

    ClockUtil.setCurrentTime(END_DATE);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.complete(taskId);

    byteArrayEntity = findByteArrayById(historicDecisionOutputInstanceEntity.getByteArrayValueId());

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(byteArrayEntity.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/history/removaltime/HistoricRootProcessInstanceTest.shouldResolveByteArray_DecisionOutputLiteralExpression.dmn"
  })
  void shouldResolveByteArray_DecisionOutputLiteralExpression() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(Bpmn.createExecutableProcess(CALLING_PROCESS_KEY)
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .businessRuleTask().operatonDecisionRef("testDecision")
        .userTask()
      .endEvent().done());

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY,
      Variables.createVariables()
        .putValue("pojo", new TestPojo("okay", 13.37)));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeOutputs()
      .singleResult();

    HistoricDecisionOutputInstanceEntity historicDecisionOutputInstanceEntity = (HistoricDecisionOutputInstanceEntity) historicDecisionInstance.getOutputs().get(0);

    ByteArrayEntity byteArrayEntity = findByteArrayById(historicDecisionOutputInstanceEntity.getByteArrayValueId());

    // assume
    assertThat(byteArrayEntity.getRemovalTime()).isNull();

    String taskId = taskService.createTaskQuery().singleResult().getId();

    ClockUtil.setCurrentTime(END_DATE);

    // when
    taskService.complete(taskId);

    byteArrayEntity = findByteArrayById(historicDecisionOutputInstanceEntity.getByteArrayValueId());

    Date removalTime = addDays(END_DATE, 5);

    // then
    assertThat(byteArrayEntity.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveBatch() {
    // given
    processEngineConfiguration.setBatchOperationHistoryTimeToLive("P5D");
    processEngineConfiguration.initHistoryCleanup();

    testRule.deploy(CALLED_PROCESS);

    testRule.deploy(CALLING_PROCESS);

    String processInstanceId = runtimeService.startProcessInstanceByKey(CALLED_PROCESS_KEY).getId();

    Batch batch = runtimeService.deleteProcessInstancesAsync(Collections.singletonList(processInstanceId), "aDeleteReason");

    ClockUtil.setCurrentTime(END_DATE);

    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.executeJob(jobId);

    List<Job> jobs = managementService.createJobQuery().list();
    for (Job job : jobs) {
      managementService.executeJob(job.getId());
    }

    HistoricBatch historicBatch = historyService.createHistoricBatchQuery().singleResult();

    // then
    assertThat(historicBatch.getRemovalTime()).isEqualTo(addDays(END_DATE, 5));

    // cleanup
    historyService.deleteHistoricBatch(batch.getId());
  }

  @Test
  void shouldResolveBatchJobLog() {
    // given
    processEngineConfiguration.setBatchOperationHistoryTimeToLive("P5D");
    processEngineConfiguration.initHistoryCleanup();

    testRule.deploy(CALLED_PROCESS);

    testRule.deploy(CALLING_PROCESS);

    String processInstanceId = runtimeService.startProcessInstanceByKey(CALLED_PROCESS_KEY).getId();

    Batch batch = runtimeService.deleteProcessInstancesAsync(Collections.singletonList(processInstanceId), "aDeleteReason");

    ClockUtil.setCurrentTime(END_DATE);

    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.executeJob(jobId);

    List<Job> jobs = managementService.createJobQuery().list();
    for (Job job : jobs) {
      managementService.executeJob(job.getId());
    }

    List<HistoricJobLog> jobLogs = historyService.createHistoricJobLogQuery().list();

    // then
    assertThat(jobLogs.get(0).getRemovalTime()).isEqualTo(addDays(END_DATE, 5));
    assertThat(jobLogs.get(1).getRemovalTime()).isEqualTo(addDays(END_DATE, 5));
    assertThat(jobLogs.get(2).getRemovalTime()).isEqualTo(addDays(END_DATE, 5));

    // cleanup
    historyService.deleteHistoricBatch(batch.getId());
  }

  @Test
  void shouldResolveBatchJobLog_ByteArray() {
    // given
    processEngineConfiguration.setBatchOperationHistoryTimeToLive("P5D");
    processEngineConfiguration.initHistoryCleanup();

    FailingExecutionListener.shouldFail = true;

    testRule.deploy(Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
        .operatonExecutionListenerClass("end", FailingExecutionListener.class)
      .endEvent()
      .done());

    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    Batch batch = runtimeService.deleteProcessInstancesAsync(Collections.singletonList(processInstanceId), "aDeleteReason");

    ClockUtil.setCurrentTime(END_DATE);

    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    managementService.executeJob(jobId);

    List<Job> jobs = managementService.createJobQuery().list();
    for (Job job : jobs) {
      executeJobIgnoringException(managementService, job.getId());
    }

    jobs = managementService.createJobQuery().list();
    for (Job job : jobs) {
      managementService.executeJob(job.getId());
    }

    HistoricJobLogEventEntity jobLog = (HistoricJobLogEventEntity)historyService.createHistoricJobLogQuery()
      .failureLog()
      .singleResult();

    String byteArrayId = jobLog.getExceptionByteArrayId();

    ByteArrayEntity byteArray = findByteArrayById(byteArrayId);

    // then
    assertThat(byteArray.getRemovalTime()).isEqualTo(addDays(END_DATE, 5));

    // cleanup
    historyService.deleteHistoricBatch(batch.getId());
    FailingExecutionListener.shouldFail = false;
  }

  /**
   * See https://app.camunda.com/jira/browse/CAM-9505
   */
  @Test
  void shouldResolveBatchJobLogWithTimestampPreserved() {
    // given
    processEngineConfiguration.setBatchOperationHistoryTimeToLive("P5D");
    processEngineConfiguration.initHistoryCleanup();

    testRule.deploy(CALLED_PROCESS);

    testRule.deploy(CALLING_PROCESS);

    ClockUtil.setCurrentTime(START_DATE);

    String processInstanceId = runtimeService.startProcessInstanceByKey(CALLED_PROCESS_KEY).getId();

    Batch batch = runtimeService.deleteProcessInstancesAsync(Collections.singletonList(processInstanceId), "aDeleteReason");

    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.executeJob(jobId);

    List<Job> jobs = managementService.createJobQuery().list();

    managementService.executeJob(jobs.get(0).getId());

    // when
    managementService.executeJob(jobs.get(1).getId());

    List<HistoricJobLog> jobLogs = historyService.createHistoricJobLogQuery().list();

    // then
    assertThat(jobLogs.get(0).getTimestamp()).isEqualTo(START_DATE);
    assertThat(jobLogs.get(1).getTimestamp()).isEqualTo(START_DATE);
    assertThat(jobLogs.get(2).getTimestamp()).isEqualTo(START_DATE);

    // cleanup
    historyService.deleteHistoricBatch(batch.getId());
  }

  @Test
  void shouldResolveBatchIncident_SeedJob() {
    // given
    processEngineConfiguration.setBatchOperationHistoryTimeToLive("P5D");
    processEngineConfiguration.initHistoryCleanup();

    testRule.deploy(CALLED_PROCESS);
    testRule.deploy(CALLING_PROCESS);

    String processInstanceId = runtimeService.startProcessInstanceByKey(CALLED_PROCESS_KEY).getId();

    ClockUtil.setCurrentTime(START_DATE);

    Batch batch = runtimeService.deleteProcessInstancesAsync(Collections.singletonList(processInstanceId), "aDeleteReason");

    HistoricJobLog jobLog = historyService.createHistoricJobLogQuery().singleResult();

    // assume
    assertThat(jobLog.getRemovalTime()).isNull();

    managementService.setJobRetries(jobLog.getJobId(), 0);

    managementService.executeJob(jobLog.getJobId());

    List<Job> jobs = managementService.createJobQuery().list();
    managementService.executeJob(jobs.get(0).getId());
    managementService.executeJob(jobs.get(1).getId());

    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();

    // then
    assertThat(historicIncident.getRemovalTime()).isEqualTo(addDays(START_DATE, 5));

    // cleanup
    historyService.deleteHistoricBatch(batch.getId());
  }

  @Test
  void shouldResolveBatchIncident_BatchJob() {
    // given
    processEngineConfiguration.setBatchOperationHistoryTimeToLive("P5D");
    processEngineConfiguration.initHistoryCleanup();

    testRule.deploy(CALLED_PROCESS);
    testRule.deploy(CALLING_PROCESS);

    String processInstanceId = runtimeService.startProcessInstanceByKey(CALLED_PROCESS_KEY).getId();

    ClockUtil.setCurrentTime(START_DATE);

    Batch batch = runtimeService.deleteProcessInstancesAsync(Collections.singletonList(processInstanceId), "aDeleteReason");

    HistoricJobLog jobLog = historyService.createHistoricJobLogQuery().singleResult();

    // assume
    assertThat(jobLog.getRemovalTime()).isNull();

    // when
    runtimeService.deleteProcessInstance(processInstanceId, "aDeleteReason");

    managementService.executeJob(jobLog.getJobId());

    String jobId = managementService.createJobQuery()
      .jobDefinitionId(batch.getBatchJobDefinitionId())
      .singleResult()
      .getId();

    managementService.setJobRetries(jobId, 0);

    managementService.deleteJob(jobId);

    jobId = managementService.createJobQuery()
      .jobDefinitionId(batch.getMonitorJobDefinitionId())
      .singleResult()
      .getId();

    managementService.executeJob(jobId);

    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();

    // then
    assertThat(historicIncident.getRemovalTime()).isEqualTo(addDays(START_DATE, 5));

    // cleanup
    historyService.deleteHistoricBatch(batch.getId());
  }

  @Test
  void shouldResolveBatchIncident_MonitorJob() {
    // given
    processEngineConfiguration.setBatchOperationHistoryTimeToLive("P5D");
    processEngineConfiguration.initHistoryCleanup();

    testRule.deploy(CALLED_PROCESS);
    testRule.deploy(CALLING_PROCESS);

    String processInstanceId = runtimeService.startProcessInstanceByKey(CALLED_PROCESS_KEY).getId();

    ClockUtil.setCurrentTime(START_DATE);

    Batch batch = runtimeService.deleteProcessInstancesAsync(Collections.singletonList(processInstanceId), "aDeleteReason");

    HistoricJobLog jobLog = historyService.createHistoricJobLogQuery().singleResult();

    // assume
    assertThat(jobLog.getRemovalTime()).isNull();

    managementService.executeJob(jobLog.getJobId());

    String jobId = managementService.createJobQuery()
      .jobDefinitionId(batch.getBatchJobDefinitionId())
      .singleResult()
      .getId();
    managementService.executeJob(jobId);

    jobId = managementService.createJobQuery()
      .jobDefinitionId(batch.getMonitorJobDefinitionId())
      .singleResult()
      .getId();
    managementService.setJobRetries(jobId, 0);
    managementService.executeJob(jobId);

    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();

    // then
    assertThat(historicIncident.getRemovalTime()).isEqualTo(addDays(START_DATE, 5));

    // cleanup
    historyService.deleteHistoricBatch(batch.getId());
  }

  /**
   * See https://app.camunda.com/jira/browse/CAM-9505
   */
  @Test
  void shouldNotUpdateCreateTimeForIncidentRelatedToBatch() {
    // given
    processEngineConfiguration.setBatchOperationHistoryTimeToLive("P5D");
    processEngineConfiguration.initHistoryCleanup();

    testRule.deploy(CALLED_PROCESS);
    testRule.deploy(CALLING_PROCESS);

    String processInstanceId = runtimeService.startProcessInstanceByKey(CALLED_PROCESS_KEY).getId();

    ClockUtil.setCurrentTime(START_DATE);

    Batch batch = runtimeService.deleteProcessInstancesAsync(Collections.singletonList(processInstanceId), "aDeleteReason");

    HistoricJobLog jobLog = historyService.createHistoricJobLogQuery().singleResult();

    // assume
    assertThat(jobLog.getRemovalTime()).isNull();

    managementService.setJobRetries(jobLog.getJobId(), 0);

    managementService.executeJob(jobLog.getJobId());

    List<Job> jobs = managementService.createJobQuery().list();
    managementService.executeJob(jobs.get(0).getId());
    managementService.executeJob(jobs.get(1).getId());

    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();

    // then
    assertThat(historicIncident.getCreateTime()).isEqualTo(START_DATE);

    // cleanup
    historyService.deleteHistoricBatch(batch.getId());
  }

}
