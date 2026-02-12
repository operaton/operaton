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
import org.operaton.bpm.engine.impl.history.DefaultHistoryRemovalTimeProvider;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionInputInstanceEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionOutputInstanceEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricExternalTaskLogEntity;
import org.operaton.bpm.engine.impl.persistence.entity.AttachmentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricJobLogEventEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Attachment;
import org.operaton.bpm.engine.task.Comment;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.async.FailingDelegate;
import org.operaton.bpm.engine.test.dmn.businessruletask.TestPojo;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_REMOVAL_TIME_STRATEGY_START;
import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobIgnoringException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Tassilo Weidner
 */
class RemovalTimeStrategyStartTest extends AbstractRemovalTimeTest {

  @BeforeEach
  void setUp() {
    processEngineConfiguration
      .setHistoryRemovalTimeStrategy(HISTORY_REMOVAL_TIME_STRATEGY_START)
      .setHistoryRemovalTimeProvider(new DefaultHistoryRemovalTimeProvider())
      .initHistoryRemovalTime();
  }

  @AfterEach
  void clearDatabase() {
    clearAuthorization();
  }

  protected static final String CALLED_PROCESS_KEY = "calledProcess";

  protected static final BpmnModelInstance CALLED_PROCESS = Bpmn.createExecutableProcess(CALLED_PROCESS_KEY)
      .operatonHistoryTimeToLive(180)
      .startEvent()
      .userTask("userTask")
      .name("userTask")
      .operatonCandidateUsers("foo")
      .serviceTask()
      .operatonAsyncBefore()
      .operatonClass(FailingDelegate.class.getName())
      .endEvent()
      .done();

  protected static final String CALLING_PROCESS_KEY = "callingProcess";
  protected static final BpmnModelInstance CALLING_PROCESS = Bpmn.createExecutableProcess(CALLING_PROCESS_KEY)
    .operatonHistoryTimeToLive(5)
    .startEvent()
      .callActivity()
        .calledElement(CALLED_PROCESS_KEY)
    .endEvent().done();

  protected static final Date START_DATE = new GregorianCalendar(2013, Calendar.MARCH, 18, 13, 0, 0).getTime();

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldResolveHistoricDecisionInstance() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(Bpmn.createExecutableProcess(CALLING_PROCESS_KEY)
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("dish-decision")
      .endEvent().done());

    // when
    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY,
      Variables.createVariables()
        .putValue("temperature", 32)
        .putValue("dayType", "Weekend"));

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // assume
    assertThat(historicDecisionInstances).hasSize(3);

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(historicDecisionInstances.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicDecisionInstances.get(1).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicDecisionInstances.get(2).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldResolveStandaloneHistoricDecisionInstance() {
    // given
    ClockUtil.setCurrentTime(START_DATE);
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery()
      .decisionDefinitionKey("dish-decision")
      .singleResult();
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinition.getId(), 5);

    // when
    decisionService.evaluateDecisionTableByKey("dish-decision", Variables.createVariables()
      .putValue("temperature", 32)
      .putValue("dayType", "Weekend"));

    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().list();

    // assume
    assertThat(historicDecisionInstances).hasSize(3);

    Date removalTime = addDays(START_DATE, 5);

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
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(Bpmn.createExecutableProcess(CALLING_PROCESS_KEY)
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("dish-decision")
      .endEvent().done());

    // when
    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY,
      Variables.createVariables()
        .putValue("temperature", 32)
        .putValue("dayType", "Weekend"));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeInputs()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance).isNotNull();

    List<HistoricDecisionInputInstance> historicDecisionInputInstances = historicDecisionInstance.getInputs();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(historicDecisionInputInstances.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicDecisionInputInstances.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldResolveStandaloneHistoricDecisionInputInstance() {
    // given
    ClockUtil.setCurrentTime(START_DATE);
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery()
      .decisionDefinitionKey("dish-decision")
      .singleResult();
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinition.getId(), 5);

    // when
    decisionService.evaluateDecisionTableByKey("dish-decision", Variables.createVariables()
      .putValue("temperature", 32)
      .putValue("dayType", "Weekend"));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeInputs()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance).isNotNull();

    List<HistoricDecisionInputInstance> historicDecisionInputInstances = historicDecisionInstance.getInputs();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(historicDecisionInputInstances.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicDecisionInputInstances.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldNotResolveHistoricDecisionInputInstance() {
    // given

    // when
    decisionService.evaluateDecisionTableByKey("dish-decision", Variables.createVariables()
      .putValue("temperature", 32)
      .putValue("dayType", "Weekend"));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeInputs()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance).isNotNull();

    List<HistoricDecisionInputInstance> historicDecisionInputInstances = historicDecisionInstance.getInputs();

    // then
    assertThat(historicDecisionInputInstances.get(0).getRemovalTime()).isNull();
    assertThat(historicDecisionInputInstances.get(1).getRemovalTime()).isNull();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldResolveHistoricDecisionOutputInstance() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(Bpmn.createExecutableProcess(CALLING_PROCESS_KEY)
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("dish-decision")
      .endEvent().done());

    // when
    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY,
      Variables.createVariables()
        .putValue("temperature", 32)
        .putValue("dayType", "Weekend"));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeOutputs()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance).isNotNull();

    List<HistoricDecisionOutputInstance> historicDecisionOutputInstances = historicDecisionInstance.getOutputs();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(historicDecisionOutputInstances.get(0).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldResolveStandaloneHistoricDecisionOutputInstance() {
    // given
    ClockUtil.setCurrentTime(START_DATE);
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery()
        .decisionDefinitionKey("dish-decision")
        .singleResult();
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinition.getId(), 5);

    // when
    decisionService.evaluateDecisionTableByKey("dish-decision", Variables.createVariables()
        .putValue("temperature", 32)
        .putValue("dayType", "Weekend"));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
        .rootDecisionInstancesOnly()
        .includeOutputs()
        .singleResult();

    // assume
    assertThat(historicDecisionInstance).isNotNull();

    List<HistoricDecisionOutputInstance> historicDecisionOutputInstances = historicDecisionInstance.getOutputs();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(historicDecisionOutputInstances.get(0).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  void shouldNotResolveHistoricDecisionOutputInstance() {
    // given

    // when
    decisionService.evaluateDecisionTableByKey("dish-decision", Variables.createVariables()
      .putValue("temperature", 32)
      .putValue("dayType", "Weekend"));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeOutputs()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance).isNotNull();

    List<HistoricDecisionOutputInstance> historicDecisionOutputInstances = historicDecisionInstance.getOutputs();

    // then
    assertThat(historicDecisionOutputInstances.get(0).getRemovalTime()).isNull();
  }

  @Test
  void shouldResolveHistoricProcessInstance() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    // when
    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
      .activeActivityIdIn("userTask")
      .singleResult();

    // assume
    assertThat(historicProcessInstance).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(historicProcessInstance.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveHistoricActivityInstance() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    // when
    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
      .activityId("userTask")
      .singleResult();

    // assume
    assertThat(historicActivityInstance).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(historicActivityInstance.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveHistoricTaskInstance() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    // when
    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery()
      .taskName("userTask")
      .singleResult();

    // assume
    assertThat(historicTaskInstance).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(historicTaskInstance.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveHistoricTaskAuthorization_HistoricTaskInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

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
    assertThat(authorization).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(authorization.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResetAuthorizationAfterUpdate_HistoricTaskInstance() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    enabledAuth();
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);
    disableAuth();

    Authorization authorization = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_TASK)
        .singleResult();

    Date removalTime = addDays(START_DATE, 5);

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

    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

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

    String taskId = historyService.createHistoricTaskInstanceQuery().singleResult().getId();

    authorization.setResourceId(taskId);

    // when
    authorizationService.saveAuthorization(authorization);

    authorization = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_TASK)
        .singleResult();

    Date removalTime = addDays(START_DATE, 5);

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

    ProcessInstance rootProcessInstance = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    // when
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

    // then
    AuthorizationQuery authQuery = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_PROCESS_INSTANCE);

    String rootProcessInstanceId = rootProcessInstance.getRootProcessInstanceId();
    Date removalTime = addDays(START_DATE, 5);
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
    ProcessInstance rootProcessInstance = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);
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

    // assume
    AuthorizationQuery authQuery = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_PROCESS_INSTANCE);

    String rootProcessInstanceId = rootProcessInstance.getRootProcessInstanceId();
    Date removalTime = addDays(START_DATE, 5);
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

    ProcessInstance rootProcessInstance = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

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
        .activeActivityIdIn("userTask")
        .singleResult()
        .getId();

    authorization.setResourceId(processInstanceId);

    authorizationService.saveAuthorization(authorization);

    // then
    authQuery = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_PROCESS_INSTANCE);

    Date removalTime = addDays(START_DATE, 5);
    String rootProcessInstanceId = rootProcessInstance.getRootProcessInstanceId();
    assertThat(authQuery.list())
        .extracting("removalTime", "resourceId", "rootProcessInstanceId")
        .containsExactly(tuple(removalTime, processInstanceId, rootProcessInstanceId));
  }

  @Test
  void shouldWriteHistoryAndResolveHistoricTaskAuthorizationInDifferentTransactions() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    enabledAuth();

    // when
    taskService.setAssignee(taskId, "myUserId");

    disableAuth();

    Authorization authorization = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_TASK)
        .singleResult();

    // assume
    assertThat(authorization).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(authorization.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldWriteHistoryAndResolveHistoricTaskAuthorizationInSameTransaction() {
    // given
    processEngineConfiguration.setEnableHistoricInstancePermissions(true);

    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    enabledAuth();
    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);
    disableAuth();

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setAssignee(taskId, "myUserId");

    Authorization authorization = authorizationService.createAuthorizationQuery()
        .resourceType(Resources.HISTORIC_TASK)
        .singleResult();

    // assume
    assertThat(authorization).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(authorization.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldNotResolveHistoricTaskInstance() {
    // given
    Task task = taskService.newTask();

    // when
    taskService.saveTask(task);

    HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();

    // assume
    assertThat(historicTaskInstance).isNotNull();

    // then
    assertThat(historicTaskInstance.getRemovalTime()).isNull();

    // cleanup
    taskService.deleteTask(task.getId(), true);
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

    // when
    runtimeService.setVariable(processInstance.getId(), "aVariableName", Variables.stringValue("anotherVariableValue"));

    HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(historicVariableInstance.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveHistoricDetailByVariableInstanceUpdate() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY,
      Variables.createVariables()
        .putValue("aVariableName", Variables.stringValue("aVariableValue")));

    // when
    runtimeService.setVariable(processInstance.getId(), "aVariableName", Variables.stringValue("anotherVariableValue"));

    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery()
      .variableUpdates()
      .list();

    // assume
    assertThat(historicDetails).hasSize(2);

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(historicDetails.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicDetails.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveHistoricDetailByFormProperty() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    DeploymentWithDefinitions deployment = testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    String processDefinitionId = deployment.getDeployedProcessDefinitions().get(0).getId();
    Map<String, Object> properties = new HashMap<>();
    properties.put("aFormProperty", "aFormPropertyValue");

    // when
    formService.submitStartForm(processDefinitionId, properties);

    HistoricDetail historicDetail = historyService.createHistoricDetailQuery().formFields().singleResult();

    // assume
    assertThat(historicDetail).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(historicDetail.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveIncident() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);
    taskService.complete(taskService.createTaskQuery().singleResult().getId());

    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    managementService.setJobRetries(jobId, 0);

    // when
    executeJobIgnoringException(managementService, jobId);

    List<HistoricIncident> historicIncidents = historyService.createHistoricIncidentQuery().list();

    // assume
    assertThat(historicIncidents).hasSize(2);

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(historicIncidents.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(historicIncidents.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldNotResolveStandaloneIncident() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLED_PROCESS);

    repositoryService.suspendProcessDefinitionByKey(CALLED_PROCESS_KEY, true, new GregorianCalendar(2013, Calendar.MARCH, 18, 13, 0, 0).getTime());

    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    managementService.setJobRetries(jobId, 0);

    // when
    executeJobIgnoringException(managementService, jobId);

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
    ClockUtil.setCurrentTime(START_DATE);

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

    // when
    runtimeService.startProcessInstanceByKey("callingProcess");

    HistoricExternalTaskLog externalTaskLog = historyService.createHistoricExternalTaskLogQuery().singleResult();

    // assume
    assertThat(externalTaskLog).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(externalTaskLog.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveJobLog() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);
    taskService.complete(taskService.createTaskQuery().singleResult().getId());

    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    // when
    executeJobIgnoringException(managementService, jobId);

    List<HistoricJobLog> jobLog = historyService.createHistoricJobLogQuery().list();

    // assume
    assertThat(jobLog).hasSize(2);

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(jobLog.get(0).getRemovalTime()).isEqualTo(removalTime);
    assertThat(jobLog.get(1).getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldNotResolveJobLog() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLED_PROCESS);

    repositoryService.suspendProcessDefinitionByKey(CALLED_PROCESS_KEY, true, new GregorianCalendar(2013, Calendar.MARCH, 18, 13, 0, 0).getTime());

    // when
    HistoricJobLog jobLog = historyService.createHistoricJobLogQuery().singleResult();

    // assume
    assertThat(jobLog).isNotNull();

    // then
    assertThat(jobLog.getRemovalTime()).isNull();

    // cleanup
    managementService.deleteJob(jobLog.getJobId());
    clearJobLog(jobLog.getJobId());
  }

  @Test
  void shouldResolveUserOperationLog_SetJobRetries() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);
    taskService.complete(taskService.createTaskQuery().singleResult().getId());

    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    // when
    identityService.setAuthenticatedUserId("aUserId");
    managementService.setJobRetries(jobId, 65);
    identityService.clearAuthentication();

    UserOperationLogEntry userOperationLog = historyService.createUserOperationLogQuery().singleResult();

    // assume
    assertThat(userOperationLog).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(userOperationLog.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveUserOperationLog_SetExternalTaskRetries() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

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

    runtimeService.startProcessInstanceByKey("callingProcess");

    // when
    identityService.setAuthenticatedUserId("aUserId");
    externalTaskService.setRetries(externalTaskService.createExternalTaskQuery().singleResult().getId(), 65);
    identityService.clearAuthentication();

    UserOperationLogEntry userOperationLog = historyService.createUserOperationLogQuery().singleResult();

    // assume
    assertThat(userOperationLog).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(userOperationLog.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveUserOperationLog_ClaimTask() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    // when
    identityService.setAuthenticatedUserId("aUserId");
    taskService.claim(taskService.createTaskQuery().singleResult().getId(), "aUserId");
    identityService.clearAuthentication();

    UserOperationLogEntry userOperationLog = historyService.createUserOperationLogQuery().singleResult();

    // assume
    assertThat(userOperationLog).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(userOperationLog.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveUserOperationLog_CreateAttachment() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    // when
    identityService.setAuthenticatedUserId("aUserId");
    taskService.createAttachment(null, null, runtimeService.createProcessInstanceQuery().activityIdIn("userTask").singleResult().getId(), null, null, "http://operaton.com");
    identityService.clearAuthentication();

    UserOperationLogEntry userOperationLog = historyService.createUserOperationLogQuery().singleResult();

    // assume
    assertThat(userOperationLog).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(userOperationLog.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveIdentityLink_AddCandidateUser() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.addCandidateUser(taskId, "aUserId");

    HistoricIdentityLinkLog historicIdentityLinkLog = historyService.createHistoricIdentityLinkLogQuery()
        .userId("aUserId")
        .singleResult();

    // assume
    assertThat(historicIdentityLinkLog).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(historicIdentityLinkLog.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldNotResolveIdentityLink_AddCandidateUser() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

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
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String processInstanceId = runtimeService.createProcessInstanceQuery()
      .activityIdIn("userTask")
      .singleResult()
      .getId();

    // when
    taskService.createComment(null, processInstanceId, "aMessage");

    Comment comment = taskService.getProcessInstanceComments(processInstanceId).get(0);

    // assume
    assertThat(comment).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(comment.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveCommentByTaskId() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.createComment(taskId, null, "aMessage");

    Comment comment = taskService.getTaskComments(taskId).get(0);

    // assume
    assertThat(comment).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(comment.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldNotResolveCommentByWrongTaskIdAndProcessInstanceId() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String processInstanceId = runtimeService.createProcessInstanceQuery()
      .activityIdIn("userTask")
      .singleResult()
      .getId();

    // when
    taskService.createComment("aNonExistentTaskId", processInstanceId, "aMessage");

    Comment comment = taskService.getProcessInstanceComments(processInstanceId).get(0);

    // assume
    assertThat(comment).isNotNull();

    // then
    assertThat(comment.getRemovalTime()).isNull();
  }

  @Test
  void shouldResolveCommentByTaskIdAndWrongProcessInstanceId() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.createComment(taskId, "aNonExistentProcessInstanceId", "aMessage");

    Comment comment = taskService.getTaskComments(taskId).get(0);

    // assume
    assertThat(comment).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(comment.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldNotResolveCommentByWrongProcessInstanceId() {
    // given

    // when
    taskService.createComment(null, "aNonExistentProcessInstanceId", "aMessage");

    Comment comment = taskService.getProcessInstanceComments("aNonExistentProcessInstanceId").get(0);

    // assume
    assertThat(comment).isNotNull();

    // then
    assertThat(comment.getRemovalTime()).isNull();

    // cleanup
    clearCommentByProcessInstanceId("aNonExistentProcessInstanceId");
  }

  @Test
  void shouldNotResolveCommentByWrongTaskId() {
    // given

    // when
    taskService.createComment("aNonExistentTaskId", null, "aMessage");

    Comment comment = taskService.getTaskComments("aNonExistentTaskId").get(0);

    // assume
    assertThat(comment).isNotNull();

    // then
    assertThat(comment.getRemovalTime()).isNull();

    // cleanup
    clearCommentByTaskId("aNonExistentTaskId");
  }

  @Test
  void shouldResolveAttachmentByProcessInstanceId() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String processInstanceId = runtimeService.createProcessInstanceQuery()
      .activityIdIn("userTask")
      .singleResult()
      .getId();

    // when
    String attachmentId = taskService.createAttachment(null, null, processInstanceId, null, null, "http://operaton.com").getId();

    Attachment attachment = taskService.getAttachment(attachmentId);

    // assume
    assertThat(attachment).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(attachment.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveAttachmentByTaskId() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    String attachmentId = taskService.createAttachment(null, taskId, null, null, null, "http://operaton.com").getId();

    Attachment attachment = taskService.getAttachment(attachmentId);

    // assume
    assertThat(attachment).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(attachment.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldNotResolveAttachmentByWrongTaskIdAndProcessInstanceId() {
    // given
    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String processInstanceId = runtimeService.createProcessInstanceQuery()
      .activityIdIn("userTask")
      .singleResult()
      .getId();

    // when
    String attachmentId = taskService.createAttachment(null, "aWrongTaskId", processInstanceId, null, null, "http://operaton.com").getId();

    Attachment attachment = taskService.getAttachment(attachmentId);

    // assume
    assertThat(attachment).isNotNull();

    // then
    assertThat(attachment.getRemovalTime()).isNull();
  }

  @Test
  void shouldResolveAttachmentByTaskIdAndWrongProcessInstanceId() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery()
      .singleResult()
      .getId();

    // when
    String attachmentId = taskService.createAttachment(null, taskId, "aWrongProcessInstanceId", null, null, "http://operaton.com").getId();

    Attachment attachment = taskService.getAttachment(attachmentId);

    // assume
    assertThat(attachment).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(attachment.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldNotResolveAttachmentByWrongTaskId() {
    // given

    // when
    String attachmentId = taskService.createAttachment(null, "aWrongTaskId", null, null, null, "http://operaton.com").getId();

    Attachment attachment = taskService.getAttachment(attachmentId);

    // assume
    assertThat(attachment).isNotNull();

    // then
    assertThat(attachment.getRemovalTime()).isNull();

    // cleanup
    clearAttachment(attachment);
  }

  @Test
  void shouldResolveByteArray_CreateAttachmentByTask() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    AttachmentEntity attachment = (AttachmentEntity) taskService.createAttachment(null, taskId, null, null, null, new ByteArrayInputStream("hello world".getBytes()));

    ByteArrayEntity byteArray = findByteArrayById(attachment.getContentId());

    // assume
    assertThat(byteArray).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(byteArray.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveByteArray_CreateAttachmentByProcessInstance() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    String calledProcessInstanceId = runtimeService.createProcessInstanceQuery()
      .activityIdIn("userTask")
      .singleResult()
      .getId();

    // when
    AttachmentEntity attachment = (AttachmentEntity) taskService.createAttachment(null, null, calledProcessInstanceId, null, null, new ByteArrayInputStream("hello world".getBytes()));

    ByteArrayEntity byteArray = findByteArrayById(attachment.getContentId());

    // assume
    assertThat(byteArray).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(byteArray.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveByteArray_SetVariable() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    // when
    runtimeService.setVariable(processInstance.getId(), "aVariableName", new ByteArrayInputStream("hello world".getBytes()));

    HistoricVariableInstanceEntity historicVariableInstance = (HistoricVariableInstanceEntity) historyService.createHistoricVariableInstanceQuery().singleResult();

    ByteArrayEntity byteArray = findByteArrayById(historicVariableInstance.getByteArrayId());

    // assume
    assertThat(byteArray).isNotNull();

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(byteArray.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveByteArray_UpdateVariable() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY,
      Variables.createVariables()
        .putValue("aVariableName", Variables.stringValue("aVariableValue")));

    // when
    runtimeService.setVariable(processInstance.getId(), "aVariableName", new ByteArrayInputStream("hello world".getBytes()));

    HistoricDetailVariableInstanceUpdateEntity historicDetails = (HistoricDetailVariableInstanceUpdateEntity) historyService.createHistoricDetailQuery()
      .variableUpdates()
      .variableTypeIn("Bytes")
      .singleResult();

    // assume
    ByteArrayEntity byteArray = findByteArrayById(historicDetails.getByteArrayValueId());

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(byteArray.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveByteArray_JobLog() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(CALLING_PROCESS);

    testRule.deploy(CALLED_PROCESS);

    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    taskService.complete(taskService.createTaskQuery().singleResult().getId());

    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    // when
    executeJobIgnoringException(managementService, jobId);

    HistoricJobLogEventEntity jobLog = (HistoricJobLogEventEntity) historyService.createHistoricJobLogQuery()
      .jobExceptionMessage("I'm supposed to fail!")
      .singleResult();

    // assume
    assertThat(jobLog).isNotNull();

    ByteArrayEntity byteArray = findByteArrayById(jobLog.getExceptionByteArrayId());

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(byteArray.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  void shouldResolveByteArray_ExternalTaskLog() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

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

    runtimeService.startProcessInstanceByKey("callingProcess");

    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, "aWorkerId")
      .topic("aTopicName", Integer.MAX_VALUE)
      .execute();

    // when
    externalTaskService.handleFailure(tasks.get(0).getId(), "aWorkerId", null, "errorDetails", 5, 3000L);

    HistoricExternalTaskLogEntity externalTaskLog = (HistoricExternalTaskLogEntity) historyService.createHistoricExternalTaskLogQuery()
      .failureLog()
      .singleResult();

    // assume
    assertThat(externalTaskLog).isNotNull();

    ByteArrayEntity byteArrayEntity = findByteArrayById(externalTaskLog.getErrorDetailsByteArrayId());

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(byteArrayEntity.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml"
  })
  void shouldResolveByteArray_DecisionInput() {
    // given
    ClockUtil.setCurrentTime(START_DATE);

    testRule.deploy(Bpmn.createExecutableProcess(CALLING_PROCESS_KEY)
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .businessRuleTask().operatonDecisionRef("testDecision")
      .endEvent().done());

    // when
    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY,
      Variables.createVariables()
        .putValue("pojo", new TestPojo("okay", 13.37)));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeInputs()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance).isNotNull();

    HistoricDecisionInputInstanceEntity historicDecisionInputInstanceEntity = (HistoricDecisionInputInstanceEntity) historicDecisionInstance.getInputs().get(0);

    ByteArrayEntity byteArrayEntity = findByteArrayById(historicDecisionInputInstanceEntity.getByteArrayValueId());

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(byteArrayEntity.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml"
  })
  void shouldResolveByteArray_StandaloneDecisionInput() {
    // given
    ClockUtil.setCurrentTime(START_DATE);
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery()
      .decisionDefinitionKey("testDecision")
      .singleResult();
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinition.getId(), 5);

    // when
    decisionService.evaluateDecisionTableByKey("testDecision", Variables.createVariables()
      .putValue("pojo", new TestPojo("okay", 13.37)));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeInputs()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance).isNotNull();

    HistoricDecisionInputInstanceEntity historicDecisionInputInstanceEntity = (HistoricDecisionInputInstanceEntity) historicDecisionInstance.getInputs().get(0);

    ByteArrayEntity byteArrayEntity = findByteArrayById(historicDecisionInputInstanceEntity.getByteArrayValueId());

    Date removalTime = addDays(START_DATE, 5);

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
      .endEvent().done());

    // when
    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY,
      Variables.createVariables()
        .putValue("pojo", new TestPojo("okay", 13.37)));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeOutputs()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance).isNotNull();

    HistoricDecisionOutputInstanceEntity historicDecisionOutputInstanceEntity = (HistoricDecisionOutputInstanceEntity) historicDecisionInstance.getOutputs().get(0);

    ByteArrayEntity byteArrayEntity = findByteArrayById(historicDecisionOutputInstanceEntity.getByteArrayValueId());

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(byteArrayEntity.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml"
  })
  void shouldResolveByteArray_StandaloneDecisionOutput() {
    // given
    ClockUtil.setCurrentTime(START_DATE);
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery()
      .decisionDefinitionKey("testDecision")
      .singleResult();
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinition.getId(), 5);

    // when
    decisionService.evaluateDecisionTableByKey("testDecision", Variables.createVariables()
      .putValue("pojo", new TestPojo("okay", 13.37)));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeOutputs()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance).isNotNull();

    HistoricDecisionOutputInstanceEntity historicDecisionOutputInstanceEntity = (HistoricDecisionOutputInstanceEntity) historicDecisionInstance.getOutputs().get(0);

    ByteArrayEntity byteArrayEntity = findByteArrayById(historicDecisionOutputInstanceEntity.getByteArrayValueId());

    Date removalTime = addDays(START_DATE, 5);

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
      .endEvent().done());

    // when
    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY,
      Variables.createVariables()
        .putValue("pojo", new TestPojo("okay", 13.37)));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeOutputs()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance).isNotNull();

    HistoricDecisionOutputInstanceEntity historicDecisionOutputInstanceEntity = (HistoricDecisionOutputInstanceEntity) historicDecisionInstance.getOutputs().get(0);

    ByteArrayEntity byteArrayEntity = findByteArrayById(historicDecisionOutputInstanceEntity.getByteArrayValueId());

    Date removalTime = addDays(START_DATE, 5);

    // then
    assertThat(byteArrayEntity.getRemovalTime()).isEqualTo(removalTime);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/history/removaltime/HistoricRootProcessInstanceTest.shouldResolveByteArray_DecisionOutputLiteralExpression.dmn"
  })
  void shouldResolveByteArray_StandaloneDecisionOutputLiteralExpression() {
    // given
    ClockUtil.setCurrentTime(START_DATE);
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery()
      .decisionDefinitionKey("testDecision")
      .singleResult();
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinition.getId(), 5);

    // when
    decisionService.evaluateDecisionTableByKey("testDecision", Variables.createVariables()
      .putValue("pojo", new TestPojo("okay", 13.37)));

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery()
      .rootDecisionInstancesOnly()
      .includeOutputs()
      .singleResult();

    // assume
    assertThat(historicDecisionInstance).isNotNull();

    HistoricDecisionOutputInstanceEntity historicDecisionOutputInstanceEntity = (HistoricDecisionOutputInstanceEntity) historicDecisionInstance.getOutputs().get(0);

    ByteArrayEntity byteArrayEntity = findByteArrayById(historicDecisionOutputInstanceEntity.getByteArrayValueId());

    Date removalTime = addDays(START_DATE, 5);

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

    ClockUtil.setCurrentTime(START_DATE);

    // when batch is started
    Batch batch = runtimeService.deleteProcessInstancesAsync(Collections.singletonList(processInstanceId), "aDeleteReason");

    HistoricBatch historicBatch = historyService.createHistoricBatchQuery().singleResult();

    // then removal time is set
    assertThat(historicBatch.getRemovalTime()).isEqualTo(addDays(START_DATE, 5));

    String seedJobId = managementService.createJobQuery().singleResult().getId();
    managementService.executeJob(seedJobId);

    String jobId = managementService.createJobQuery().list().get(0).getId();
    managementService.executeJob(jobId);

    String monitorJobId = managementService.createJobQuery().singleResult().getId();

    // when batch is ended
    managementService.executeJob(monitorJobId);

    historicBatch = historyService.createHistoricBatchQuery().singleResult();

    // then removal time is still set
    assertThat(historicBatch.getRemovalTime()).isEqualTo(addDays(START_DATE, 5));

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

    ClockUtil.setCurrentTime(START_DATE);

    Batch batch = runtimeService.deleteProcessInstancesAsync(Collections.singletonList(processInstanceId), "aDeleteReason");

    HistoricJobLog jobLog = historyService.createHistoricJobLogQuery().singleResult();

    // assume
    assertThat(jobLog.getRemovalTime()).isEqualTo(addDays(START_DATE, 5));

    // when
    managementService.executeJob(jobLog.getJobId());

    List<HistoricJobLog> jobLogs = historyService.createHistoricJobLogQuery().list();

    // then
    assertThat(jobLogs.get(0).getRemovalTime()).isEqualTo(addDays(START_DATE, 5));
    assertThat(jobLogs.get(1).getRemovalTime()).isEqualTo(addDays(START_DATE, 5));

    // cleanup
    managementService.deleteBatch(batch.getId(), true);
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

    ClockUtil.setCurrentTime(START_DATE);

    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();
    Batch batch = runtimeService.deleteProcessInstancesAsync(Collections.singletonList(processInstanceId), "aDeleteReason");

    String jobId = managementService.createJobQuery()
      .singleResult()
      .getId();

    managementService.executeJob(jobId);

    List<Job> jobs = managementService.createJobQuery().list();
    for (Job job : jobs) {
      executeJobIgnoringException(managementService, job.getId());
    }

    HistoricJobLogEventEntity jobLog = (HistoricJobLogEventEntity)historyService.createHistoricJobLogQuery()
      .failureLog()
      .singleResult();

    String byteArrayId = jobLog.getExceptionByteArrayId();

    ByteArrayEntity byteArray = findByteArrayById(byteArrayId);

    // then
    assertThat(byteArray.getRemovalTime()).isEqualTo(addDays(START_DATE, 5));

    // cleanup
    managementService.deleteBatch(batch.getId(), true);
    FailingExecutionListener.shouldFail = false;
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
    assertThat(jobLog.getRemovalTime()).isEqualTo(addDays(START_DATE, 5));

    // when
    managementService.setJobRetries(jobLog.getJobId(), 0);

    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();

    // then
    assertThat(historicIncident.getRemovalTime()).isEqualTo(addDays(START_DATE, 5));

    // cleanup
    managementService.deleteBatch(batch.getId(), true);
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
    assertThat(jobLog.getRemovalTime()).isEqualTo(addDays(START_DATE, 5));

    runtimeService.deleteProcessInstance(processInstanceId, "aDeleteReason");

    managementService.executeJob(jobLog.getJobId());

    String jobId = managementService.createJobQuery()
      .jobDefinitionId(batch.getBatchJobDefinitionId())
      .singleResult()
      .getId();

    // when
    managementService.setJobRetries(jobId, 0);

    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();

    // then
    assertThat(historicIncident.getRemovalTime()).isEqualTo(addDays(START_DATE, 5));

    // cleanup
    managementService.deleteBatch(batch.getId(), true);
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
    assertThat(jobLog.getRemovalTime()).isEqualTo(addDays(START_DATE, 5));

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

    // when
    managementService.setJobRetries(jobId, 0);

    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();

    // then
    assertThat(historicIncident.getRemovalTime()).isEqualTo(addDays(START_DATE, 5));

    // cleanup
    managementService.deleteBatch(batch.getId(), true);
  }

}
