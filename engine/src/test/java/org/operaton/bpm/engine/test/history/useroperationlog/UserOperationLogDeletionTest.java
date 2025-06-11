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
package org.operaton.bpm.engine.test.history.useroperationlog;

import static org.operaton.bpm.engine.history.UserOperationLogEntry.CATEGORY_OPERATOR;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.ResetDmnConfigUtil;

/**
 * @author Roman Smirnov
 *
 */
public class UserOperationLogDeletionTest extends AbstractUserOperationLogTest {

  public static final String DECISION_SINGLE_OUTPUT_DMN = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.decisionSingleOutput.dmn11.xml";
  public static final String DECISION_DEFINITION_KEY = "testDecision";

  protected static final String PROCESS_PATH = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";
  protected static final String PROCESS_KEY = "oneTaskProcess";

  @BeforeEach
  void setUp() {
    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        processEngineConfiguration.getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(true)
        .init();


  }

  @AfterEach
  void tearDown() {
    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        processEngineConfiguration.getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(false)
        .init();


  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testDeleteProcessTaskKeepTaskOperationLog() {
    // given
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.setAssignee(taskId, "demo");
    taskService.complete(taskId);

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .taskId(taskId);
    assertThat(query.count()).isEqualTo(2);

    // when
    historyService.deleteHistoricTaskInstance(taskId);

    // then
    assertThat(query.count()).isEqualTo(4);

    UserOperationLogEntry entry = historyService.createUserOperationLogQuery()
      .operationType(OPERATION_TYPE_DELETE_HISTORY)
      .taskId(taskId)
      .property("nrOfInstances")
      .singleResult();
    assertThat(entry.getCategory()).isEqualTo(CATEGORY_OPERATOR);
  }

  @Test
  void testDeleteStandaloneTaskKeepUserOperationLog() {
    // given
    String taskId = "my-task";
    Task task = taskService.newTask(taskId);
    taskService.saveTask(task);

    taskService.setAssignee(taskId, "demo");
    taskService.complete(taskId);

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .taskId(taskId);
    assertThat(query.count()).isEqualTo(3);

    // when
    historyService.deleteHistoricTaskInstance(taskId);

    // then
    assertThat(query.count()).isEqualTo(5);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testDeleteCaseTaskKeepUserOperationLog() {
    // given
    caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .create();

    caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.setAssignee(taskId, "demo");
    taskService.complete(taskId);

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .taskId(taskId);
    assertThat(query.count()).isEqualTo(2);

    // when
    historyService.deleteHistoricTaskInstance(taskId);

    // then
    assertThat(query.count()).isEqualTo(4);
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testDeleteProcessInstanceKeepUserOperationLog() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    runtimeService.suspendProcessInstanceById(processInstanceId);
    runtimeService.activateProcessInstanceById(processInstanceId);

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .processInstanceId(processInstanceId);
    assertThat(query.count()).isEqualTo(4);

    // when
    historyService.deleteHistoricProcessInstance(processInstanceId);

    // then
    assertThat(query.count()).isEqualTo(4);

    UserOperationLogEntry entry = historyService.createUserOperationLogQuery()
      .operationType(OPERATION_TYPE_DELETE_HISTORY)
      .property("nrOfInstances")
      .singleResult();

    assertThat(entry).isNotNull();
    assertThat(entry.getCategory()).isEqualTo(CATEGORY_OPERATOR);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testDeleteCaseInstanceKeepUserOperationLog() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .create()
        .getId();

    caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    caseService.closeCaseInstance(caseInstanceId);

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .caseInstanceId(caseInstanceId)
        .entityType(EntityTypes.TASK);
    assertThat(query.count()).isEqualTo(1);

    // when
    historyService.deleteHistoricCaseInstance(caseInstanceId);

    // then
    assertThat(query.count()).isEqualTo(1);

    UserOperationLogEntry entry = historyService.createUserOperationLogQuery()
        .operationType(OPERATION_TYPE_DELETE_HISTORY)
        .singleResult();

    assertThat(entry).isNotNull();
    assertThat(entry.getCategory()).isEqualTo(CATEGORY_OPERATOR);
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testDeleteProcessDefinitionKeepUserOperationLog() {
    // given
    String processDefinitionId = repositoryService
        .createProcessDefinitionQuery()
        .singleResult()
        .getId();

    String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    runtimeService.suspendProcessInstanceById(processInstanceId);

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .processInstanceId(processInstanceId);
    assertThat(query.count()).isEqualTo(2);

    // when
    repositoryService.deleteProcessDefinition(processDefinitionId, true);

    // then new log is created and old stays
    assertThat(query.count()).isEqualTo(2);
  }

  @Test
  void testDeleteProcessDefinitionsByKey() {
    // given
    for (int i = 0; i < 3; i++) {
      testRule.deploy(repositoryService.createDeployment()
        .addClasspathResource(PROCESS_PATH));
    }

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey(PROCESS_KEY)
      .withoutTenantId()
      .delete();

    // then
    assertUserOperationLogs();
  }

  @Test
  void testDeleteProcessDefinitionsByKeyCascading() {
    // given
    for (int i = 0; i < 3; i++) {
      testRule.deploy(repositoryService.createDeployment()
                          .addClasspathResource(PROCESS_PATH));
    }

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey(PROCESS_KEY)
      .withoutTenantId()
      .cascade()
      .delete();

    // then
    assertUserOperationLogs();
  }

  @Test
  void testDeleteProcessDefinitionsByIds() {
    // given
    for (int i = 0; i < 3; i++) {
      testRule.deploy(repositoryService.createDeployment()
                          .addClasspathResource(PROCESS_PATH));
    }

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(findProcessDefinitionIdsByKey(PROCESS_KEY))
      .delete();

    // then
    assertUserOperationLogs();
  }

  @Test
  void testDeleteProcessDefinitionsByIdsCascading() {
    // given
    for (int i = 0; i < 3; i++) {
      testRule.deploy(repositoryService.createDeployment()
                          .addClasspathResource(PROCESS_PATH));
    }

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(findProcessDefinitionIdsByKey(PROCESS_KEY))
      .cascade()
      .delete();

    // then
    assertUserOperationLogs();
  }

  @Deployment(resources = PROCESS_PATH)
  @Test
  void testDeleteDeploymentKeepUserOperationLog() {
    // given
    String deploymentId = repositoryService
        .createDeploymentQuery()
        .singleResult()
        .getId();

    String processDefinitionId = repositoryService
        .createProcessDefinitionQuery()
        .singleResult()
        .getId();

    repositoryService.suspendProcessDefinitionById(processDefinitionId);

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .processDefinitionId(processDefinitionId);
    assertThat(query.count()).isEqualTo(2);

    // when
    repositoryService.deleteDeployment(deploymentId, true);

    // then
    assertThat(query.count()).isEqualTo(2);
  }

  @Deployment(resources = {DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testDeleteDecisionInstanceByDecisionDefinition() {

    Map<String, Object> variables = new HashMap<>();
    variables.put("input1", "test");
    decisionService.evaluateDecisionTableByKey(DECISION_DEFINITION_KEY, variables);

    String decisionDefinitionId = repositoryService.createDecisionDefinitionQuery().decisionDefinitionKey(DECISION_DEFINITION_KEY).singleResult().getId();
    historyService.deleteHistoricDecisionInstanceByDefinitionId(decisionDefinitionId);

    List<UserOperationLogEntry> userOperationLogEntries = historyService.createUserOperationLogQuery()
      .operationType(OPERATION_TYPE_DELETE_HISTORY)
      .property("nrOfInstances")
      .list();

    assertThat(userOperationLogEntries).hasSize(1);

    UserOperationLogEntry entry = userOperationLogEntries.get(0);
    assertThat(entry.getNewValue()).isEqualTo("1");
    assertThat(entry.getCategory()).isEqualTo(CATEGORY_OPERATOR);
  }

  @Deployment(resources = {DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testDeleteDecisionInstanceById() {

    Map<String, Object> variables = new HashMap<>();
    variables.put("input1", "test");
    decisionService.evaluateDecisionTableByKey(DECISION_DEFINITION_KEY, variables);

    HistoricDecisionInstance historicDecisionInstance = historyService.createHistoricDecisionInstanceQuery().singleResult();
    historyService.deleteHistoricDecisionInstanceByInstanceId(historicDecisionInstance.getId());

    List<UserOperationLogEntry> userOperationLogEntries = historyService.createUserOperationLogQuery()
      .operationType(OPERATION_TYPE_DELETE_HISTORY)
      .property("nrOfInstances")
      .list();

    assertThat(userOperationLogEntries).hasSize(1);

    UserOperationLogEntry entry = userOperationLogEntries.get(0);
    assertThat(entry.getNewValue()).isEqualTo("1");
    assertThat(entry.getCategory()).isEqualTo(CATEGORY_OPERATOR);
  }

  public void assertUserOperationLogs() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();

    UserOperationLogQuery userOperationLogQuery = historyService
      .createUserOperationLogQuery()
      .operationType(UserOperationLogEntry.OPERATION_TYPE_DELETE);

    List<UserOperationLogEntry> userOperationLogs = userOperationLogQuery.list();

    assertThat(userOperationLogs).hasSize(3);

    for (ProcessDefinition processDefinition: processDefinitions) {
      UserOperationLogEntry userOperationLogEntry = userOperationLogQuery
        .deploymentId(processDefinition.getDeploymentId()).singleResult();

      assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.PROCESS_DEFINITION);
      assertThat(userOperationLogEntry.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
      assertThat(userOperationLogEntry.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
      assertThat(userOperationLogEntry.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());

      assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);

      assertThat(userOperationLogEntry.getProperty()).isEqualTo("cascade");
      assertThat(Boolean.parseBoolean(userOperationLogEntry.getOrgValue())).isFalse();
      assertThat(Boolean.parseBoolean(userOperationLogEntry.getNewValue())).isTrue();

      assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

      assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);

      assertThat(userOperationLogEntry.getJobDefinitionId()).isNull();
      assertThat(userOperationLogEntry.getProcessInstanceId()).isNull();
      assertThat(userOperationLogEntry.getCaseInstanceId()).isNull();
      assertThat(userOperationLogEntry.getCaseDefinitionId()).isNull();
    }

    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(6);
  }

  private String[] findProcessDefinitionIdsByKey(String processDefinitionKey) {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey(processDefinitionKey).list();
    List<String> processDefinitionIds = new ArrayList<>();
    for (ProcessDefinition processDefinition: processDefinitions) {
      processDefinitionIds.add(processDefinition.getId());
    }

    return processDefinitionIds.toArray(new String[0]);
  }

}
