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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.impl.RuntimeServiceImpl;
import org.operaton.bpm.engine.impl.TaskServiceImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.TimerActivateJobDefinitionHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerSuspendProcessDefinitionHandler;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Attachment;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;

import static org.operaton.bpm.engine.EntityTypes.ATTACHMENT;
import static org.operaton.bpm.engine.EntityTypes.IDENTITY_LINK;
import static org.operaton.bpm.engine.EntityTypes.JOB;
import static org.operaton.bpm.engine.EntityTypes.JOB_DEFINITION;
import static org.operaton.bpm.engine.EntityTypes.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.EntityTypes.PROCESS_INSTANCE;
import static org.operaton.bpm.engine.EntityTypes.TASK;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.*;
import static org.operaton.bpm.engine.impl.cmd.AbstractSetBatchStateCmd.SUSPENSION_STATE_PROPERTY;
import static org.operaton.bpm.engine.impl.cmd.AbstractSetProcessDefinitionStateCmd.INCLUDE_PROCESS_INSTANCES_PROPERTY;
import static org.operaton.bpm.engine.impl.persistence.entity.TaskEntity.ASSIGNEE;
import static org.operaton.bpm.engine.impl.persistence.entity.TaskEntity.OWNER;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Danny Gräf
 */
class UserOperationLogQueryTest extends AbstractUserOperationLogTest {

  protected static final String ONE_TASK_PROCESS = "org/operaton/bpm/engine/test/history/oneTaskProcess.bpmn20.xml";
  protected static final String ONE_TASK_CASE = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn";
  protected static final String ONE_EXTERNAL_TASK_PROCESS = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml";

  private ProcessInstance process;
  private Task userTask;
  private Execution execution;
  private String processTaskId;

  private final Date today = new Date(Instant.parse("2025-01-01T12:00:00Z").toEpochMilli());
  private final Date tomorrow = new Date(Instant.parse("2025-01-02T12:00:00Z").toEpochMilli());
  private final Date yesterday = new Date(Instant.parse("2024-12-31T12:00:00Z").toEpochMilli());

  @AfterEach
  void tearDown() {
    if (userTask != null) {
      historyService.deleteHistoricTaskInstance(userTask.getId());
    }
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQuery() {
    createLogEntries();

    // expect: all entries can be fetched
    assertThat(query().count()).isEqualTo(18);

    // entity type
    assertThat(query().entityType(TASK).count()).isEqualTo(11);
    assertThat(query().entityType(IDENTITY_LINK).count()).isEqualTo(4);
    assertThat(query().entityType(ATTACHMENT).count()).isEqualTo(2);
    assertThat(query().entityType(EntityTypes.PROCESS_INSTANCE).count()).isOne();
    assertThat(query().entityType("unknown entity type").count()).isZero();

    // operation type
    assertThat(query().operationType(OPERATION_TYPE_CREATE).count()).isEqualTo(2);
    assertThat(query().operationType(OPERATION_TYPE_SET_PRIORITY).count()).isOne();
    assertThat(query().operationType(OPERATION_TYPE_UPDATE).count()).isEqualTo(4);
    assertThat(query().operationType(OPERATION_TYPE_ADD_USER_LINK).count()).isOne();
    assertThat(query().operationType(OPERATION_TYPE_DELETE_USER_LINK).count()).isOne();
    assertThat(query().operationType(OPERATION_TYPE_ADD_GROUP_LINK).count()).isOne();
    assertThat(query().operationType(OPERATION_TYPE_DELETE_GROUP_LINK).count()).isOne();
    assertThat(query().operationType(OPERATION_TYPE_ADD_ATTACHMENT).count()).isOne();
    assertThat(query().operationType(OPERATION_TYPE_DELETE_ATTACHMENT).count()).isOne();

    // category
    assertThat(query().categoryIn(UserOperationLogEntry.CATEGORY_TASK_WORKER).count()).isEqualTo(17);
    assertThat(query().categoryIn(UserOperationLogEntry.CATEGORY_OPERATOR).count()).isOne();// start process instance

    // process and execution reference
    assertThat(query().processDefinitionId(process.getProcessDefinitionId()).count()).isEqualTo(12);
    assertThat(query().processInstanceId(process.getId()).count()).isEqualTo(12);
    assertThat(query().executionId(execution.getId()).count()).isEqualTo(11);

    // task reference
    assertThat(query().taskId(processTaskId).count()).isEqualTo(11);
    assertThat(query().taskId(userTask.getId()).count()).isEqualTo(6);

    // user reference
    assertThat(query().userId("icke").count()).isEqualTo(11); // not includes the create operation called by the process
    assertThat(query().userId("er").count()).isEqualTo(6);

    // operation ID
    UserOperationLogQuery updates = query().operationType(OPERATION_TYPE_UPDATE);
    String updateOperationId = updates.list().get(0).getOperationId();
    assertThat(query().operationId(updateOperationId).count()).isEqualTo(updates.count());

    // changed properties
    assertThat(query().property(ASSIGNEE).count()).isEqualTo(3);
    assertThat(query().property(OWNER).count()).isEqualTo(2);

    // ascending order results by time
    List<UserOperationLogEntry> ascLog = query().orderByTimestamp().asc().list();
    for (int i = 0; i < 5; i++) {
      assertThat(ascLog.get(i).getTimestamp()).isAfterOrEqualTo(yesterday);
    }
    for (int i = 5; i < 13; i++) {
      assertThat(ascLog.get(i).getTimestamp()).isAfterOrEqualTo(today);
    }
    for (int i = 13; i < 18; i++) {
      assertThat(ascLog.get(i).getTimestamp()).isAfterOrEqualTo(tomorrow);
    }

    // descending order results by time
    List<UserOperationLogEntry> descLog = query().orderByTimestamp().desc().list();
    for (int i = 0; i < 4; i++) {
      assertThat(descLog.get(i).getTimestamp()).isAfterOrEqualTo(tomorrow);
    }
    for (int i = 4; i < 11; i++) {
      assertThat(descLog.get(i).getTimestamp()).isAfterOrEqualTo(today);
    }
    for (int i = 11; i < 18; i++) {
      assertThat(descLog.get(i).getTimestamp()).isAfterOrEqualTo(yesterday);
    }

    // filter by time, created yesterday
    assertThat(query().beforeTimestamp(today).count()).isEqualTo(5);
    // filter by time, created today and before
    assertThat(query().beforeTimestamp(tomorrow).count()).isEqualTo(13);
    // filter by time, created today and later
    assertThat(query().afterTimestamp(yesterday).count()).isEqualTo(13);
    // filter by time, created tomorrow
    assertThat(query().afterTimestamp(today).count()).isEqualTo(5);
    assertThat(query().afterTimestamp(today).beforeTimestamp(yesterday).count()).isZero();
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryWithBackwardCompatibility() {
    createLogEntries();

    // expect: all entries can be fetched
    assertThat(query().count()).isEqualTo(18);

    // entity type
    assertThat(query().entityType(TASK).count()).isEqualTo(11);
    assertThat(query().entityType(IDENTITY_LINK).count()).isEqualTo(4);
    assertThat(query().entityType(ATTACHMENT).count()).isEqualTo(2);
    assertThat(query().entityType("unknown entity type").count()).isZero();
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryProcessInstanceOperationsById() {
    // given
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.suspendProcessInstanceById(process.getId());
    runtimeService.activateProcessInstanceById(process.getId());

    runtimeService.deleteProcessInstance(process.getId(), "a delete reason");

    // then
    assertThat(query().entityType(PROCESS_INSTANCE).count()).isEqualTo(4);

    UserOperationLogEntry deleteEntry = query()
        .entityType(PROCESS_INSTANCE)
        .processInstanceId(process.getId())
        .operationType(OPERATION_TYPE_DELETE)
        .singleResult();

    assertThat(deleteEntry).isNotNull();
    assertThat(deleteEntry.getProcessInstanceId()).isEqualTo(process.getId());
    assertThat(deleteEntry.getProcessDefinitionId()).isNotNull();
    assertThat(deleteEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(deleteEntry.getDeploymentId()).isEqualTo(deploymentId);
    assertThat(deleteEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry suspendEntry = query()
      .entityType(PROCESS_INSTANCE)
      .processInstanceId(process.getId())
      .operationType(OPERATION_TYPE_SUSPEND)
      .singleResult();

    assertThat(suspendEntry).isNotNull();
    assertThat(suspendEntry.getProcessInstanceId()).isEqualTo(process.getId());
    assertThat(suspendEntry.getProcessDefinitionId()).isNotNull();
    assertThat(suspendEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");

    assertThat(suspendEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(suspendEntry.getNewValue()).isEqualTo("suspended");
    assertThat(suspendEntry.getOrgValue()).isNull();
    assertThat(suspendEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry activateEntry = query()
        .entityType(PROCESS_INSTANCE)
        .processInstanceId(process.getId())
        .operationType(OPERATION_TYPE_ACTIVATE)
        .singleResult();

    assertThat(activateEntry).isNotNull();
    assertThat(activateEntry.getProcessInstanceId()).isEqualTo(process.getId());
    assertThat(activateEntry.getProcessDefinitionId()).isNotNull();
    assertThat(activateEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(activateEntry.getDeploymentId()).isEqualTo(deploymentId);

    assertThat(activateEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(activateEntry.getNewValue()).isEqualTo("active");
    assertThat(activateEntry.getOrgValue()).isNull();
    assertThat(activateEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryProcessInstanceOperationsByProcessDefinitionId() {
    // given
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.suspendProcessInstanceByProcessDefinitionId(process.getProcessDefinitionId());
    runtimeService.activateProcessInstanceByProcessDefinitionId(process.getProcessDefinitionId());

    // then
    assertThat(query().entityType(PROCESS_INSTANCE).count()).isEqualTo(3);

    UserOperationLogEntry suspendEntry = query()
        .entityType(PROCESS_INSTANCE)
        .processDefinitionId(process.getProcessDefinitionId())
        .operationType(OPERATION_TYPE_SUSPEND)
        .singleResult();

    assertThat(suspendEntry).isNotNull();
    assertThat(suspendEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
    assertThat(suspendEntry.getProcessInstanceId()).isNull();
    assertThat(suspendEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(suspendEntry.getDeploymentId()).isEqualTo(deploymentId);

    assertThat(suspendEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(suspendEntry.getNewValue()).isEqualTo("suspended");
    assertThat(suspendEntry.getOrgValue()).isNull();
    assertThat(suspendEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry activateEntry = query()
        .entityType(PROCESS_INSTANCE)
        .processDefinitionId(process.getProcessDefinitionId())
        .operationType(OPERATION_TYPE_ACTIVATE)
        .singleResult();

    assertThat(activateEntry).isNotNull();
    assertThat(activateEntry.getProcessInstanceId()).isNull();
    assertThat(activateEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(activateEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
    assertThat(activateEntry.getDeploymentId()).isEqualTo(deploymentId);

    assertThat(activateEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(activateEntry.getNewValue()).isEqualTo("active");
    assertThat(activateEntry.getOrgValue()).isNull();
    assertThat(activateEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryProcessInstanceOperationsByProcessDefinitionKey() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.suspendProcessInstanceByProcessDefinitionKey("oneTaskProcess");
    runtimeService.activateProcessInstanceByProcessDefinitionKey("oneTaskProcess");

    // then
    assertThat(query().entityType(PROCESS_INSTANCE).count()).isEqualTo(3);

    UserOperationLogEntry suspendEntry = query()
        .entityType(PROCESS_INSTANCE)
        .processDefinitionKey("oneTaskProcess")
        .operationType(OPERATION_TYPE_SUSPEND)
        .singleResult();

    assertThat(suspendEntry).isNotNull();
    assertThat(suspendEntry.getProcessInstanceId()).isNull();
    assertThat(suspendEntry.getProcessDefinitionId()).isNull();
    assertThat(suspendEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(suspendEntry.getDeploymentId()).isNull();

    assertThat(suspendEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(suspendEntry.getNewValue()).isEqualTo("suspended");
    assertThat(suspendEntry.getOrgValue()).isNull();
    assertThat(suspendEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry activateEntry = query()
        .entityType(PROCESS_INSTANCE)
        .processDefinitionKey("oneTaskProcess")
        .operationType(OPERATION_TYPE_ACTIVATE)
        .singleResult();

    assertThat(activateEntry).isNotNull();
    assertThat(activateEntry.getProcessInstanceId()).isNull();
    assertThat(activateEntry.getProcessDefinitionId()).isNull();
    assertThat(activateEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(activateEntry.getDeploymentId()).isNull();

    assertThat(activateEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(activateEntry.getNewValue()).isEqualTo("active");
    assertThat(activateEntry.getOrgValue()).isNull();
    assertThat(activateEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  /**
   * CAM-1930: add assertions for additional op log entries here
   */
  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryProcessDefinitionOperationsById() {
    // given
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    repositoryService.suspendProcessDefinitionById(process.getProcessDefinitionId(), true, null);
    repositoryService.activateProcessDefinitionById(process.getProcessDefinitionId(), true, null);

    // then
    assertThat(query().entityType(PROCESS_DEFINITION).property(SUSPENSION_STATE_PROPERTY).count()).isEqualTo(2);

    // Process Definition Suspension
    UserOperationLogEntry suspendDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionId(process.getProcessDefinitionId())
      .operationType(OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION)
      .property(SUSPENSION_STATE_PROPERTY)
      .singleResult();

    assertThat(suspendDefinitionEntry).isNotNull();
    assertThat(suspendDefinitionEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
    assertThat(suspendDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(suspendDefinitionEntry.getDeploymentId()).isEqualTo(deploymentId);

    assertThat(suspendDefinitionEntry.getProperty()).isEqualTo(SUSPENSION_STATE_PROPERTY);
    assertThat(suspendDefinitionEntry.getNewValue()).isEqualTo("suspended");
    assertThat(suspendDefinitionEntry.getOrgValue()).isNull();
    assertThat(suspendDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry activateDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionId(process.getProcessDefinitionId())
      .operationType(OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION)
      .property(SUSPENSION_STATE_PROPERTY)
      .singleResult();

    assertThat(activateDefinitionEntry).isNotNull();
    assertThat(activateDefinitionEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
    assertThat(activateDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(activateDefinitionEntry.getDeploymentId()).isEqualTo(deploymentId);

    assertThat(activateDefinitionEntry.getProperty()).isEqualTo(SUSPENSION_STATE_PROPERTY);
    assertThat(activateDefinitionEntry.getNewValue()).isEqualTo("active");
    assertThat(activateDefinitionEntry.getOrgValue()).isNull();
    assertThat(activateDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

  }

  /**
   * CAM-1930: add assertions for additional op log entries here
   */
  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryProcessDefinitionOperationsByKey() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    repositoryService.suspendProcessDefinitionByKey("oneTaskProcess", true, null);
    repositoryService.activateProcessDefinitionByKey("oneTaskProcess", true, null);

    // then
    assertThat(query().entityType(PROCESS_DEFINITION).property(SUSPENSION_STATE_PROPERTY).count()).isEqualTo(2);

    UserOperationLogEntry suspendDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionKey("oneTaskProcess")
      .operationType(OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION)
      .property(SUSPENSION_STATE_PROPERTY)
      .singleResult();

    assertThat(suspendDefinitionEntry).isNotNull();
    assertThat(suspendDefinitionEntry.getProcessDefinitionId()).isNull();
    assertThat(suspendDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(suspendDefinitionEntry.getDeploymentId()).isNull();

    assertThat(suspendDefinitionEntry.getProperty()).isEqualTo(SUSPENSION_STATE_PROPERTY);
    assertThat(suspendDefinitionEntry.getNewValue()).isEqualTo("suspended");
    assertThat(suspendDefinitionEntry.getOrgValue()).isNull();
    assertThat(suspendDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry activateDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionKey("oneTaskProcess")
      .operationType(OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION)
      .property(SUSPENSION_STATE_PROPERTY)
      .singleResult();

    assertThat(activateDefinitionEntry).isNotNull();
    assertThat(activateDefinitionEntry.getProcessDefinitionId()).isNull();
    assertThat(activateDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(activateDefinitionEntry.getDeploymentId()).isNull();

    assertThat(activateDefinitionEntry.getProperty()).isEqualTo(SUSPENSION_STATE_PROPERTY);
    assertThat(activateDefinitionEntry.getNewValue()).isEqualTo("active");
    assertThat(activateDefinitionEntry.getOrgValue()).isNull();
    assertThat(activateDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryProcessDefinitionOperationsById_createsTwoLogEntries() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    repositoryService.suspendProcessDefinitionById(process.getProcessDefinitionId(), true, null);
    repositoryService.activateProcessDefinitionById(process.getProcessDefinitionId(), true, null);

    // then
    assertThat(query().entityType(PROCESS_DEFINITION).operationType(OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION).count()).isEqualTo(2);
    assertThat(query().entityType(PROCESS_DEFINITION).operationType(OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION).count()).isEqualTo(2);

    // Process Definition Suspension
    Set<String> actualProperties = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionId(process.getProcessDefinitionId())
      .operationType(OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION)
      .list()
      .stream()
      .map(UserOperationLogEntry::getProperty)
      .collect(Collectors.toSet());

    assertThat(new HashSet<>(List.of(INCLUDE_PROCESS_INSTANCES_PROPERTY, SUSPENSION_STATE_PROPERTY))).isEqualTo(actualProperties);

    // Process Definition activation
    actualProperties = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionId(process.getProcessDefinitionId())
      .operationType(OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION)
      .list()
      .stream()
      .map(UserOperationLogEntry::getProperty)
      .collect(Collectors.toSet());

    assertThat(new HashSet<>(List.of(INCLUDE_PROCESS_INSTANCES_PROPERTY, SUSPENSION_STATE_PROPERTY))).isEqualTo(actualProperties);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryProcessDefinitionOperationsById_includeProcessInstancesEntries() {
    // given
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    repositoryService.suspendProcessDefinitionById(process.getProcessDefinitionId(), true, null);
    repositoryService.activateProcessDefinitionById(process.getProcessDefinitionId(), true, null);

    // then
    assertThat(query().entityType(PROCESS_DEFINITION).property(INCLUDE_PROCESS_INSTANCES_PROPERTY).count()).isEqualTo(2);

    // Process Definition Suspension
    UserOperationLogEntry suspendDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionId(process.getProcessDefinitionId())
      .operationType(OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION)
      .property(INCLUDE_PROCESS_INSTANCES_PROPERTY)
      .singleResult();

    assertThat(suspendDefinitionEntry).isNotNull();
    assertThat(suspendDefinitionEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
    assertThat(suspendDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(suspendDefinitionEntry.getDeploymentId()).isEqualTo(deploymentId);

    assertThat(suspendDefinitionEntry.getProperty()).isEqualTo(INCLUDE_PROCESS_INSTANCES_PROPERTY);
    assertThat(suspendDefinitionEntry.getNewValue()).isEqualTo("true");
    assertThat(suspendDefinitionEntry.getOrgValue()).isNull();
    assertThat(suspendDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    // Process Definition Activation
    UserOperationLogEntry activeDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionId(process.getProcessDefinitionId())
      .operationType(OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION)
      .property(INCLUDE_PROCESS_INSTANCES_PROPERTY)
      .singleResult();

    assertThat(activeDefinitionEntry).isNotNull();
    assertThat(activeDefinitionEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
    assertThat(activeDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(activeDefinitionEntry.getDeploymentId()).isEqualTo(deploymentId);

    assertThat(activeDefinitionEntry.getProperty()).isEqualTo(INCLUDE_PROCESS_INSTANCES_PROPERTY);
    assertThat(activeDefinitionEntry.getNewValue()).isEqualTo("true");
    assertThat(activeDefinitionEntry.getOrgValue()).isNull();
    assertThat(activeDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryProcessDefinitionOperationsById_excludeProcessInstancesEntries() {
    // given
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    repositoryService.suspendProcessDefinitionById(process.getProcessDefinitionId(), false, null);
    repositoryService.activateProcessDefinitionById(process.getProcessDefinitionId(), true, null);

    // then
    assertThat(query().entityType(PROCESS_DEFINITION).property(INCLUDE_PROCESS_INSTANCES_PROPERTY).count()).isEqualTo(2);

    // Process Definition Suspension
    UserOperationLogEntry suspendDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionId(process.getProcessDefinitionId())
      .operationType(OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION)
      .property(INCLUDE_PROCESS_INSTANCES_PROPERTY)
      .singleResult();

    assertThat(suspendDefinitionEntry).isNotNull();
    assertThat(suspendDefinitionEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
    assertThat(suspendDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(suspendDefinitionEntry.getDeploymentId()).isEqualTo(deploymentId);

    assertThat(suspendDefinitionEntry.getProperty()).isEqualTo(INCLUDE_PROCESS_INSTANCES_PROPERTY);
    assertThat(suspendDefinitionEntry.getNewValue()).isEqualTo("false");
    assertThat(suspendDefinitionEntry.getOrgValue()).isNull();
    assertThat(suspendDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    // Process Definition Activation
    UserOperationLogEntry activeDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionId(process.getProcessDefinitionId())
      .operationType(OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION)
      .property(INCLUDE_PROCESS_INSTANCES_PROPERTY)
      .singleResult();

    assertThat(activeDefinitionEntry).isNotNull();
    assertThat(activeDefinitionEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
    assertThat(activeDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(activeDefinitionEntry.getDeploymentId()).isEqualTo(deploymentId);

    assertThat(activeDefinitionEntry.getProperty()).isEqualTo(INCLUDE_PROCESS_INSTANCES_PROPERTY);
    assertThat(activeDefinitionEntry.getNewValue()).isEqualTo("true");
    assertThat(activeDefinitionEntry.getOrgValue()).isNull();
    assertThat(activeDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryProcessDefinitionOperationsByKey_createsTwoLogEntries() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    repositoryService.suspendProcessDefinitionByKey("oneTaskProcess", true, null);
    repositoryService.activateProcessDefinitionByKey("oneTaskProcess", true, null);

    // then
    assertThat(query().entityType(PROCESS_DEFINITION).operationType(OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION).count()).isEqualTo(2);
    assertThat(query().entityType(PROCESS_DEFINITION).operationType(OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION).count()).isEqualTo(2);

    // Process Definition Suspension
    Set<String> actualProperties = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionKey("oneTaskProcess")
      .operationType(OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION)
      .list()
      .stream()
      .map(UserOperationLogEntry::getProperty)
      .collect(Collectors.toSet());

    assertThat(new HashSet<>(List.of(INCLUDE_PROCESS_INSTANCES_PROPERTY, SUSPENSION_STATE_PROPERTY))).isEqualTo(actualProperties);

    // Process Definition activation
    actualProperties = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionKey("oneTaskProcess")
      .operationType(OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION)
      .list()
      .stream()
      .map(UserOperationLogEntry::getProperty)
      .collect(Collectors.toSet());

    assertThat(new HashSet<>(List.of(INCLUDE_PROCESS_INSTANCES_PROPERTY, SUSPENSION_STATE_PROPERTY))).isEqualTo(actualProperties);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryProcessDefinitionOperationsByKey_includeProcessInstancesEntries() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    repositoryService.suspendProcessDefinitionByKey("oneTaskProcess", true, null);
    repositoryService.activateProcessDefinitionByKey("oneTaskProcess", true, null);

    // then
    assertThat(query().entityType(PROCESS_DEFINITION).property(INCLUDE_PROCESS_INSTANCES_PROPERTY).count()).isEqualTo(2);

    // Process Definition Suspension
    UserOperationLogEntry suspendDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionKey("oneTaskProcess")
      .operationType(OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION)
      .property(INCLUDE_PROCESS_INSTANCES_PROPERTY)
      .singleResult();

    assertThat(suspendDefinitionEntry).isNotNull();
    assertThat(suspendDefinitionEntry.getProcessDefinitionId()).isNull();
    assertThat(suspendDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(suspendDefinitionEntry.getDeploymentId()).isNull();

    assertThat(suspendDefinitionEntry.getProperty()).isEqualTo(INCLUDE_PROCESS_INSTANCES_PROPERTY);
    assertThat(suspendDefinitionEntry.getNewValue()).isEqualTo("true");
    assertThat(suspendDefinitionEntry.getOrgValue()).isNull();
    assertThat(suspendDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    // Process Definition Activation
    UserOperationLogEntry activeDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionKey("oneTaskProcess")
      .operationType(OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION)
      .property(INCLUDE_PROCESS_INSTANCES_PROPERTY)
      .singleResult();

    assertThat(activeDefinitionEntry).isNotNull();
    assertThat(activeDefinitionEntry.getProcessDefinitionId()).isNull();
    assertThat(activeDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(activeDefinitionEntry.getDeploymentId()).isNull();

    assertThat(activeDefinitionEntry.getProperty()).isEqualTo(INCLUDE_PROCESS_INSTANCES_PROPERTY);
    assertThat(activeDefinitionEntry.getNewValue()).isEqualTo("true");
    assertThat(activeDefinitionEntry.getOrgValue()).isNull();
    assertThat(activeDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryProcessDefinitionOperationsByKey_excludeProcessInstancesEntries() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    repositoryService.suspendProcessDefinitionByKey("oneTaskProcess", false, null);
    repositoryService.activateProcessDefinitionByKey("oneTaskProcess", false, null);

    // then
    assertThat(query().entityType(PROCESS_DEFINITION).property(INCLUDE_PROCESS_INSTANCES_PROPERTY).count()).isEqualTo(2);

    // Process Definition Suspension
    UserOperationLogEntry suspendDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionKey("oneTaskProcess")
      .operationType(OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION)
      .property(INCLUDE_PROCESS_INSTANCES_PROPERTY)
      .singleResult();

    assertThat(suspendDefinitionEntry).isNotNull();
    assertThat(suspendDefinitionEntry.getProcessDefinitionId()).isNull();
    assertThat(suspendDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(suspendDefinitionEntry.getDeploymentId()).isNull();

    assertThat(suspendDefinitionEntry.getProperty()).isEqualTo(INCLUDE_PROCESS_INSTANCES_PROPERTY);
    assertThat(suspendDefinitionEntry.getNewValue()).isEqualTo("false");
    assertThat(suspendDefinitionEntry.getOrgValue()).isNull();
    assertThat(suspendDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    // Process Definition Activation
    UserOperationLogEntry activeDefinitionEntry = query()
      .entityType(PROCESS_DEFINITION)
      .processDefinitionKey("oneTaskProcess")
      .operationType(OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION)
      .property(INCLUDE_PROCESS_INSTANCES_PROPERTY)
      .singleResult();

    assertThat(activeDefinitionEntry).isNotNull();
    assertThat(activeDefinitionEntry.getProcessDefinitionId()).isNull();
    assertThat(activeDefinitionEntry.getProcessDefinitionKey()).isEqualTo("oneTaskProcess");
    assertThat(activeDefinitionEntry.getDeploymentId()).isNull();

    assertThat(activeDefinitionEntry.getProperty()).isEqualTo(INCLUDE_PROCESS_INSTANCES_PROPERTY);
    assertThat(activeDefinitionEntry.getNewValue()).isEqualTo("false");
    assertThat(activeDefinitionEntry.getOrgValue()).isNull();
    assertThat(activeDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricJobLogTest.testAsyncContinuation.bpmn20.xml"})
  @Test
  void testQueryJobOperations() {
    // given
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    process = runtimeService.startProcessInstanceByKey("process");

    // when
    managementService.suspendJobDefinitionByProcessDefinitionId(process.getProcessDefinitionId());
    managementService.activateJobDefinitionByProcessDefinitionId(process.getProcessDefinitionId());
    managementService.suspendJobByProcessInstanceId(process.getId());
    managementService.activateJobByProcessInstanceId(process.getId());

    // then
    assertThat(query().entityType(JOB_DEFINITION).count()).isEqualTo(2);
    assertThat(query().entityType(JOB).count()).isEqualTo(2);

    // active job definition
    UserOperationLogEntry activeJobDefinitionEntry = query()
      .entityType(JOB_DEFINITION)
      .processDefinitionId(process.getProcessDefinitionId())
      .operationType(OPERATION_TYPE_ACTIVATE_JOB_DEFINITION)
      .singleResult();

    assertThat(activeJobDefinitionEntry).isNotNull();
    assertThat(activeJobDefinitionEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
    assertThat(activeJobDefinitionEntry.getDeploymentId()).isEqualTo(deploymentId);

    assertThat(activeJobDefinitionEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(activeJobDefinitionEntry.getNewValue()).isEqualTo("active");
    assertThat(activeJobDefinitionEntry.getOrgValue()).isNull();
    assertThat(activeJobDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    // active job
    UserOperationLogEntry activateJobIdEntry = query()
      .entityType(JOB)
      .processInstanceId(process.getProcessInstanceId())
      .operationType(OPERATION_TYPE_ACTIVATE_JOB)
      .singleResult();

    assertThat(activateJobIdEntry).isNotNull();
    assertThat(activateJobIdEntry.getProcessInstanceId()).isEqualTo(process.getProcessInstanceId());
    assertThat(activateJobIdEntry.getDeploymentId()).isEqualTo(deploymentId);

    assertThat(activateJobIdEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(activateJobIdEntry.getNewValue()).isEqualTo("active");
    assertThat(activateJobIdEntry.getOrgValue()).isNull();
    assertThat(activateJobIdEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    // suspended job definition
    UserOperationLogEntry suspendJobDefinitionEntry = query()
      .entityType(JOB_DEFINITION)
      .processDefinitionId(process.getProcessDefinitionId())
      .operationType(OPERATION_TYPE_SUSPEND_JOB_DEFINITION)
      .singleResult();

    assertThat(suspendJobDefinitionEntry).isNotNull();
    assertThat(suspendJobDefinitionEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
    assertThat(suspendJobDefinitionEntry.getDeploymentId()).isEqualTo(deploymentId);

    assertThat(suspendJobDefinitionEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(suspendJobDefinitionEntry.getNewValue()).isEqualTo("suspended");
    assertThat(suspendJobDefinitionEntry.getOrgValue()).isNull();
    assertThat(suspendJobDefinitionEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    // suspended job
    UserOperationLogEntry suspendedJobEntry = query()
      .entityType(JOB)
      .processInstanceId(process.getProcessInstanceId())
      .operationType(OPERATION_TYPE_SUSPEND_JOB)
      .singleResult();

    assertThat(suspendedJobEntry).isNotNull();
    assertThat(suspendedJobEntry.getProcessInstanceId()).isEqualTo(process.getProcessInstanceId());
    assertThat(suspendedJobEntry.getDeploymentId()).isEqualTo(deploymentId);

    assertThat(suspendedJobEntry.getProperty()).isEqualTo("suspensionState");
    assertThat(suspendedJobEntry.getNewValue()).isEqualTo("suspended");
    assertThat(suspendedJobEntry.getOrgValue()).isNull();
    assertThat(suspendedJobEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/FoxJobRetryCmdTest.testFailedServiceTask.bpmn20.xml"})
  @Test
  void testQueryJobRetryOperationsById() {
    // given
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    process = runtimeService.startProcessInstanceByKey("failedServiceTask");
    Job job = managementService.createJobQuery().processInstanceId(process.getProcessInstanceId()).singleResult();

    managementService.setJobRetries(job.getId(), 10);

    // then
    assertThat(query().entityType(JOB).operationType(OPERATION_TYPE_SET_JOB_RETRIES).count()).isOne();

    UserOperationLogEntry jobRetryEntry = query()
      .entityType(JOB)
      .jobId(job.getId())
      .operationType(OPERATION_TYPE_SET_JOB_RETRIES)
      .singleResult();

    assertThat(jobRetryEntry).isNotNull();
    assertThat(jobRetryEntry.getJobId()).isEqualTo(job.getId());

    assertThat(jobRetryEntry.getOrgValue()).isEqualTo("5");
    assertThat(jobRetryEntry.getNewValue()).isEqualTo("10");
    assertThat(jobRetryEntry.getProperty()).isEqualTo("retries");
    assertThat(jobRetryEntry.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(jobRetryEntry.getProcessInstanceId()).isEqualTo(job.getProcessInstanceId());
    assertThat(jobRetryEntry.getProcessDefinitionKey()).isEqualTo(job.getProcessDefinitionKey());
    assertThat(jobRetryEntry.getProcessDefinitionId()).isEqualTo(job.getProcessDefinitionId());
    assertThat(jobRetryEntry.getDeploymentId()).isEqualTo(deploymentId);
    assertThat(jobRetryEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryJobDefinitionOperationWithDelayedJobDefinition() {
    // given
    // a running process instance
    ProcessInstance oneTaskProcess = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // with a process definition id
    String processDefinitionId = oneTaskProcess.getProcessDefinitionId();

    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionId(processDefinitionId, true);

    // one week from now
    ClockUtil.setCurrentTime(today);
    long oneWeekFromStartTime = today.getTime() + (7 * 24 * 60 * 60 * 1000);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionId(processDefinitionId, false, new Date(oneWeekFromStartTime));

    // then
    // there is a user log entry for the activation
    long jobDefinitionEntryCount = query()
      .entityType(JOB_DEFINITION)
      .operationType(UserOperationLogEntry.OPERATION_TYPE_ACTIVATE_JOB_DEFINITION)
      .processDefinitionId(processDefinitionId)
      .category(UserOperationLogEntry.CATEGORY_OPERATOR)
      .count();

    assertThat(jobDefinitionEntryCount).isOne();

    // there exists a job for the delayed activation execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedActivationJob = jobQuery.timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    jobDefinitionEntryCount = query()
      .entityType(JOB_DEFINITION)
      .operationType(UserOperationLogEntry.OPERATION_TYPE_ACTIVATE_JOB_DEFINITION)
      .processDefinitionId(processDefinitionId)
      .category(UserOperationLogEntry.CATEGORY_OPERATOR)
      .count();

    assertThat(jobDefinitionEntryCount).isOne();

    // Clean up db
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerActivateJobDefinitionHandler.TYPE);
      return null;
    });
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testQueryProcessDefinitionOperationWithDelayedProcessDefinition() {
    // given
    ClockUtil.setCurrentTime(today);
    final long hourInMs = 60 * 60 * 1000;

    String key = "oneFailingServiceTaskProcess";

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey(key, params);

    // when
    // the process definition will be suspended
    repositoryService.suspendProcessDefinitionByKey(key, false, new Date(today.getTime() + (2 * hourInMs)));

    // then
    // there exists a timer job to suspend the process definition delayed
    Job timerToSuspendProcessDefinition = managementService.createJobQuery().timers().singleResult();
    assertThat(timerToSuspendProcessDefinition).isNotNull();

    // there is a user log entry for the activation
    long processDefinitionEntryCount = query()
      .entityType(PROCESS_DEFINITION)
      .operationType(UserOperationLogEntry.OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION)
      .processDefinitionKey(key)
      .category(UserOperationLogEntry.CATEGORY_OPERATOR)
      .property(SUSPENSION_STATE_PROPERTY)
      .count();

    assertThat(processDefinitionEntryCount).isOne();

    // when
    // execute job
    managementService.executeJob(timerToSuspendProcessDefinition.getId());

    // then
    // there is a user log entry for the activation
    processDefinitionEntryCount = query()
      .entityType(PROCESS_DEFINITION)
      .operationType(UserOperationLogEntry.OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION)
      .processDefinitionKey(key)
      .category(UserOperationLogEntry.CATEGORY_OPERATOR)
      .property(SUSPENSION_STATE_PROPERTY)
      .count();

    assertThat(processDefinitionEntryCount).isOne();

    // clean up op log
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerSuspendProcessDefinitionHandler.TYPE);
      return null;
    });
  }

  // ----- PROCESS INSTANCE MODIFICATION -----

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryProcessInstanceModificationOperation() {
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().singleResult();

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("theTask")
      .execute();

    UserOperationLogQuery logQuery = query()
      .entityType(EntityTypes.PROCESS_INSTANCE)
      .operationType(UserOperationLogEntry.OPERATION_TYPE_MODIFY_PROCESS_INSTANCE);

    assertThat(logQuery.count()).isOne();
    UserOperationLogEntry logEntry = logQuery.singleResult();

    assertThat(logEntry.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(logEntry.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(logEntry.getProcessDefinitionKey()).isEqualTo(definition.getKey());
    assertThat(logEntry.getDeploymentId()).isEqualTo(deploymentId);
    assertThat(logEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_MODIFY_PROCESS_INSTANCE);
    assertThat(logEntry.getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);
    assertThat(logEntry.getProperty()).isNull();
    assertThat(logEntry.getOrgValue()).isNull();
    assertThat(logEntry.getNewValue()).isNull();
    assertThat(logEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  // ----- ADD VARIABLES -----

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryAddExecutionVariableOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.setVariable(process.getId(), "testVariable1", "THIS IS TESTVARIABLE!!!");

    // then
    verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE, UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryAddExecutionVariablesMapOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.setVariables(process.getId(), createMapForVariableAddition());

    // then
    verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE, UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryAddExecutionVariablesSingleAndMapOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.setVariable(process.getId(), "testVariable3", "foo");
    runtimeService.setVariables(process.getId(), createMapForVariableAddition());
    runtimeService.setVariable(process.getId(), "testVariable4", "bar");

    // then
    verifyVariableOperationAsserts(3, UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE, UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryAddTaskVariableOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setVariable(processTaskId, "testVariable1", "THIS IS TESTVARIABLE!!!");

    // then
    verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE, UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryAddTaskVariablesMapOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setVariables(processTaskId, createMapForVariableAddition());

    // then
    verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE, UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryAddTaskVariablesSingleAndMapOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setVariable(processTaskId, "testVariable3", "foo");
    taskService.setVariables(processTaskId, createMapForVariableAddition());
    taskService.setVariable(processTaskId, "testVariable4", "bar");

    // then
    verifyVariableOperationAsserts(3, UserOperationLogEntry.OPERATION_TYPE_SET_VARIABLE, UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  // ----- PATCH VARIABLES -----

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryPatchExecutionVariablesOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    ((RuntimeServiceImpl) runtimeService)
      .updateVariables(process.getId(), createMapForVariableAddition(), createCollectionForVariableDeletion());

    // then
   verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_MODIFY_VARIABLE, UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryPatchTaskVariablesOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    ((TaskServiceImpl) taskService)
      .updateVariablesLocal(processTaskId, createMapForVariableAddition(), createCollectionForVariableDeletion());

    // then
    verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_MODIFY_VARIABLE, UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  // ----- REMOVE VARIABLES -----

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryRemoveExecutionVariableOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.removeVariable(process.getId(), "testVariable1");

    // then
    verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_REMOVE_VARIABLE, UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryRemoveExecutionVariablesMapOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.removeVariables(process.getId(), createCollectionForVariableDeletion());

    // then
    verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_REMOVE_VARIABLE, UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryRemoveExecutionVariablesSingleAndMapOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    runtimeService.removeVariable(process.getId(), "testVariable1");
    runtimeService.removeVariables(process.getId(), createCollectionForVariableDeletion());
    runtimeService.removeVariable(process.getId(), "testVariable2");

    // then
    verifyVariableOperationAsserts(3, UserOperationLogEntry.OPERATION_TYPE_REMOVE_VARIABLE, UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryRemoveTaskVariableOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.removeVariable(processTaskId, "testVariable1");

    // then
    verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_REMOVE_VARIABLE, UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryRemoveTaskVariablesMapOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.removeVariables(processTaskId, createCollectionForVariableDeletion());

    // then
    verifyVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_REMOVE_VARIABLE, UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryRemoveTaskVariablesSingleAndMapOperation() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.removeVariable(processTaskId, "testVariable3");
    taskService.removeVariables(processTaskId, createCollectionForVariableDeletion());
    taskService.removeVariable(processTaskId, "testVariable4");

    // then
    verifyVariableOperationAsserts(3, UserOperationLogEntry.OPERATION_TYPE_REMOVE_VARIABLE, UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryByEntityTypes() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setAssignee(processTaskId, "foo");
    taskService.setVariable(processTaskId, "foo", "bar");

    // then
    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .entityTypeIn(TASK, EntityTypes.VARIABLE);

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByInvalidEntityTypes() {
    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .entityTypeIn("foo");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.entityTypeIn((String[]) null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> query.entityTypeIn(TASK, null, EntityTypes.VARIABLE)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryByCategories() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setAssignee(processTaskId, "foo");
    taskService.setVariable(processTaskId, "foo", "bar");

    // then
    UserOperationLogQuery query = historyService
      .createUserOperationLogQuery()
      .categoryIn(UserOperationLogEntry.CATEGORY_TASK_WORKER, UserOperationLogEntry.CATEGORY_OPERATOR);

    verifyQueryResults(query, 3);

    // and
    query = historyService
        .createUserOperationLogQuery()
        .categoryIn(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    verifyQueryResults(query, 2);

    // and
    query = historyService
      .createUserOperationLogQuery()
      .category(UserOperationLogEntry.CATEGORY_OPERATOR);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidCategories() {
    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .categoryIn("foo");

    verifyQueryResults(query, 0);

    query = historyService
        .createUserOperationLogQuery()
        .category("foo");

    verifyQueryResults(query, 0);

    var query2 = query;
    assertThatThrownBy(() -> query2.category(null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> query2.categoryIn((String[]) null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> query2.categoryIn(UserOperationLogEntry.CATEGORY_ADMIN, null, UserOperationLogEntry.CATEGORY_TASK_WORKER)).isInstanceOf(ProcessEngineException.class);
  }

  // ----- DELETE VARIABLE HISTORY -----

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryDeleteVariableHistoryOperationOnRunningInstance() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariable(process.getId(), "testVariable", "test");
    runtimeService.setVariable(process.getId(), "testVariable", "test2");
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().singleResult().getId();

    // when
    historyService.deleteHistoricVariableInstance(variableInstanceId);

    // then
    verifyHistoricVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    verifySingleVariableOperationPropertyChange("name", "testVariable", UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryDeleteVariableHistoryOperationOnHistoricInstance() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariable(process.getId(), "testVariable", "test");
    runtimeService.deleteProcessInstance(process.getId(), "none");
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().singleResult().getId();

    // when
    historyService.deleteHistoricVariableInstance(variableInstanceId);

    // then
    verifyHistoricVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    verifySingleVariableOperationPropertyChange("name", "testVariable", UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryDeleteVariableHistoryOperationOnTaskOfRunningInstance() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();
    taskService.setVariable(processTaskId, "testVariable", "test");
    taskService.setVariable(processTaskId, "testVariable", "test2");
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().singleResult().getId();

    // when
    historyService.deleteHistoricVariableInstance(variableInstanceId);

    // then
    verifyHistoricVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    verifySingleVariableOperationPropertyChange("name", "testVariable", UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryDeleteVariableHistoryOperationOnTaskOfHistoricInstance() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    processTaskId = taskService.createTaskQuery().singleResult().getId();
    taskService.setVariable(processTaskId, "testVariable", "test");
    runtimeService.deleteProcessInstance(process.getId(), "none");
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().singleResult().getId();

    // when
    historyService.deleteHistoricVariableInstance(variableInstanceId);

    // then
    verifyHistoricVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    verifySingleVariableOperationPropertyChange("name", "testVariable", UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryDeleteVariableHistoryOperationOnCase() {
    // given
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase");
    caseService.setVariable(caseInstance.getId(), "myVariable", 1);
    caseService.setVariable(caseInstance.getId(), "myVariable", 2);
    caseService.setVariable(caseInstance.getId(), "myVariable", 3);
    HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();

    // when
    historyService.deleteHistoricVariableInstance(variableInstance.getId());

    // then
    verifySingleCaseVariableOperationAsserts(caseInstance);
    verifySingleVariableOperationPropertyChange("name", "myVariable", UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryDeleteVariableHistoryOperationOnTaskOfCase() {
    // given
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase");
    processTaskId = taskService.createTaskQuery().singleResult().getId();
    taskService.setVariable(processTaskId, "myVariable", "1");
    taskService.setVariable(processTaskId, "myVariable", "2");
    taskService.setVariable(processTaskId, "myVariable", "3");
    HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();

    // when
    historyService.deleteHistoricVariableInstance(variableInstance.getId());

    verifySingleCaseVariableOperationAsserts(caseInstance);
    verifySingleVariableOperationPropertyChange("name", "myVariable", UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Test
  void testQueryDeleteVariableHistoryOperationOnStandaloneTask() {
    // given
    Task task = taskService.newTask();
    taskService.saveTask(task);
    taskService.setVariable(task.getId(), "testVariable", "testValue");
    taskService.setVariable(task.getId(), "testVariable", "testValue2");
    HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();

    // when
    historyService.deleteHistoricVariableInstance(variableInstance.getId());

    // then
    UserOperationLogQuery logQuery = query().entityType(EntityTypes.VARIABLE).operationType(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    assertThat(logQuery.count()).isOne();

    UserOperationLogEntry logEntry = logQuery.singleResult();
    assertThat(logEntry.getTaskId()).isEqualTo(task.getId());
    assertThat(logEntry.getDeploymentId()).isNull();
    verifySingleVariableOperationPropertyChange("name", "testVariable", UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);

    taskService.deleteTask(task.getId(), true);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryDeleteVariablesHistoryOperationOnRunningInstance() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariable(process.getId(), "testVariable", "test");
    runtimeService.setVariable(process.getId(), "testVariable", "test2");
    runtimeService.setVariable(process.getId(), "testVariable2", "test");
    runtimeService.setVariable(process.getId(), "testVariable2", "test2");
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(2);

    // when
    historyService.deleteHistoricVariableInstancesByProcessInstanceId(process.getId());

    // then
    verifyHistoricVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryDeleteVariablesHistoryOperationOnHistoryInstance() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariable(process.getId(), "testVariable", "test");
    runtimeService.setVariable(process.getId(), "testVariable2", "test");
    runtimeService.deleteProcessInstance(process.getId(), "none");
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(2);

    // when
    historyService.deleteHistoricVariableInstancesByProcessInstanceId(process.getId());

    // then
    verifyHistoricVariableOperationAsserts(1, UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryDeleteVariableAndVariablesHistoryOperationOnRunningInstance() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariable(process.getId(), "testVariable", "test");
    runtimeService.setVariable(process.getId(), "testVariable", "test2");
    runtimeService.setVariable(process.getId(), "testVariable2", "test");
    runtimeService.setVariable(process.getId(), "testVariable2", "test2");
    runtimeService.setVariable(process.getId(), "testVariable3", "test");
    runtimeService.setVariable(process.getId(), "testVariable3", "test2");
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().variableName("testVariable").singleResult().getId();

    // when
    historyService.deleteHistoricVariableInstance(variableInstanceId);
    historyService.deleteHistoricVariableInstancesByProcessInstanceId(process.getId());

    // then
    verifyHistoricVariableOperationAsserts(2, UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  @Deployment(resources = {ONE_TASK_PROCESS})
  @Test
  void testQueryDeleteVariableAndVariablesHistoryOperationOnHistoryInstance() {
    // given
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariable(process.getId(), "testVariable", "test");
    runtimeService.setVariable(process.getId(), "testVariable2", "test");
    runtimeService.setVariable(process.getId(), "testVariable3", "test");
    runtimeService.deleteProcessInstance(process.getId(), "none");
    String variableInstanceId = historyService.createHistoricVariableInstanceQuery().variableName("testVariable").singleResult().getId();

    // when
    historyService.deleteHistoricVariableInstance(variableInstanceId);
    historyService.deleteHistoricVariableInstancesByProcessInstanceId(process.getId());

    // then
    verifyHistoricVariableOperationAsserts(2, UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
  }

  // --------------- CMMN --------------------

  @Deployment(resources = {ONE_TASK_CASE})
  @Test
  void testQueryByCaseDefinitionId() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    Task task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();

    // when
    taskService.setAssignee(task.getId(), "demo");

    // then

    UserOperationLogQuery query = historyService
      .createUserOperationLogQuery()
      .caseDefinitionId(caseDefinitionId)
      .category(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {ONE_TASK_CASE})
  @Test
  void testQueryByCaseInstanceId() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    Task task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();

    // when
    taskService.setAssignee(task.getId(), "demo");

    // then

    UserOperationLogQuery query = historyService
      .createUserOperationLogQuery()
      .caseInstanceId(caseInstanceId)
      .category(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {ONE_TASK_CASE})
  @Test
  void testQueryByCaseExecutionId() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    Task task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();

    // when
    taskService.setAssignee(task.getId(), "demo");

    // then

    UserOperationLogQuery query = historyService
      .createUserOperationLogQuery()
      .caseExecutionId(caseExecutionId)
      .category(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {ONE_EXTERNAL_TASK_PROCESS})
  @Test
  void testQueryByExternalTaskId() {
    // given:
    // an active process instance
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    ExternalTask task = externalTaskService.createExternalTaskQuery().singleResult();
    assertThat(task).isNotNull();

    // when
    externalTaskService.setRetries(task.getId(), 5);

    // then
    UserOperationLogQuery query = historyService
      .createUserOperationLogQuery()
      .externalTaskId(task.getId());

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByDeploymentId() {
    // given
    String deploymentId = repositoryService
        .createDeployment()
        .addClasspathResource(ONE_TASK_PROCESS)
        .deploy()
        .getId();

    // when
    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .deploymentId(deploymentId)
        .category(UserOperationLogEntry.CATEGORY_OPERATOR);

    // then
    verifyQueryResults(query, 1);

    repositoryService.deleteDeployment(deploymentId, true);
  }

  @Test
  void testQueryByInvalidDeploymentId() {
    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .deploymentId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.deploymentId(null)).isInstanceOf(ProcessEngineException.class);
  }

  private Map<String, Object> createMapForVariableAddition() {
    Map<String, Object> variables =  new HashMap<>();
    variables.put("testVariable1", "THIS IS TESTVARIABLE!!!");
    variables.put("testVariable2", "OVER 9000!");

    return variables;
  }

  private Collection<String> createCollectionForVariableDeletion() {
    Collection<String> variables = new ArrayList<>();
    variables.add("testVariable3");
    variables.add("testVariable4");

    return variables;
  }

  private void verifyVariableOperationAsserts(int countAssertValue, String operationType, String category) {
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    UserOperationLogQuery logQuery = query().entityType(EntityTypes.VARIABLE).operationType(operationType);
    assertThat(logQuery.count()).isEqualTo(countAssertValue);

    if(countAssertValue > 1) {
      List<UserOperationLogEntry> logEntryList = logQuery.list();

      for (UserOperationLogEntry logEntry : logEntryList) {
        assertThat(logEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
        assertThat(logEntry.getProcessInstanceId()).isEqualTo(process.getProcessInstanceId());
        assertThat(logEntry.getDeploymentId()).isEqualTo(deploymentId);
        assertThat(logEntry.getCategory()).isEqualTo(category);
      }
    } else {
      UserOperationLogEntry logEntry = logQuery.singleResult();
      assertThat(logEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
      assertThat(logEntry.getProcessInstanceId()).isEqualTo(process.getProcessInstanceId());
      assertThat(logEntry.getDeploymentId()).isEqualTo(deploymentId);
      assertThat(logEntry.getCategory()).isEqualTo(category);
    }
  }

  private void verifyHistoricVariableOperationAsserts(int countAssertValue, String operationType) {
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    UserOperationLogQuery logQuery = query().entityType(EntityTypes.VARIABLE).operationType(operationType);
    assertThat(logQuery.count()).isEqualTo(countAssertValue);

    if(countAssertValue > 1) {
      List<UserOperationLogEntry> logEntryList = logQuery.list();

      for (UserOperationLogEntry logEntry : logEntryList) {
        assertThat(logEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
        assertThat(logEntry.getProcessInstanceId()).isEqualTo(process.getProcessInstanceId());
        assertThat(logEntry.getDeploymentId()).isEqualTo(deploymentId);
        assertThat(logEntry.getTaskId()).isNull();
        assertThat(logEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
      }
    } else {
      UserOperationLogEntry logEntry = logQuery.singleResult();
      assertThat(logEntry.getProcessDefinitionId()).isEqualTo(process.getProcessDefinitionId());
      assertThat(logEntry.getProcessInstanceId()).isEqualTo(process.getProcessInstanceId());
      assertThat(logEntry.getDeploymentId()).isEqualTo(deploymentId);
      assertThat(logEntry.getTaskId()).isNull();
      assertThat(logEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    }
  }

  private void verifySingleVariableOperationPropertyChange(String property, String newValue, String operationType) {
    UserOperationLogQuery logQuery = query().entityType(EntityTypes.VARIABLE).operationType(operationType);
    assertThat(logQuery.count()).isOne();
    UserOperationLogEntry logEntry = logQuery.singleResult();
    assertThat(logEntry.getProperty()).isEqualTo(property);
    assertThat(logEntry.getNewValue()).isEqualTo(newValue);
    assertThat(logEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  private void verifySingleCaseVariableOperationAsserts(CaseInstance caseInstance) {
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    UserOperationLogQuery logQuery = query().entityType(EntityTypes.VARIABLE).operationType(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
    assertThat(logQuery.count()).isOne();

    UserOperationLogEntry logEntry = logQuery.singleResult();
    assertThat(logEntry.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
    assertThat(logEntry.getCaseInstanceId()).isEqualTo(caseInstance.getCaseInstanceId());
    assertThat(logEntry.getDeploymentId()).isEqualTo(deploymentId);
    assertThat(logEntry.getTaskId()).isNull();
    assertThat(logEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  private UserOperationLogQuery query() {
    return historyService.createUserOperationLogQuery();
  }

  /**
   * start process and operate on userTask to create some log entries for the query tests
   */
  private void createLogEntries() {
    ClockUtil.setCurrentTime(yesterday);

    // create a process with a userTask and work with it
    process = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    execution = processEngine.getRuntimeService().createExecutionQuery().processInstanceId(process.getId()).singleResult();
    processTaskId = taskService.createTaskQuery().singleResult().getId();

    // user "icke" works on the process userTask
    identityService.setAuthenticatedUserId("icke");

    // create and remove some links
    taskService.addCandidateUser(processTaskId, "er");
    taskService.deleteCandidateUser(processTaskId, "er");
    taskService.addCandidateGroup(processTaskId, "wir");
    taskService.deleteCandidateGroup(processTaskId, "wir");

    // assign and reassign the userTask
    ClockUtil.setCurrentTime(today);
    taskService.setOwner(processTaskId, "icke");
    taskService.claim(processTaskId, "icke");
    taskService.setAssignee(processTaskId, "er");

    // change priority of task
    taskService.setPriority(processTaskId, 10);

    // add and delete an attachment
    Attachment attachment = taskService.createAttachment("image/ico", processTaskId, process.getId(), "favicon.ico", "favicon", "http://operaton.com/favicon.ico");
    taskService.deleteAttachment(attachment.getId());

    // complete the userTask to finish the process
    taskService.complete(processTaskId);
    testRule.assertProcessEnded(process.getId());

    // user "er" works on the process userTask
    identityService.setAuthenticatedUserId("er");

    // create a standalone userTask
    userTask = taskService.newTask();
    userTask.setName("to do");
    taskService.saveTask(userTask);

    // change some properties manually to create an update event
    ClockUtil.setCurrentTime(tomorrow);
    userTask.setDescription("desc");
    userTask.setOwner("icke");
    userTask.setAssignee("er");
    userTask.setDueDate(today);
    taskService.saveTask(userTask);

    // complete the userTask
    taskService.complete(userTask.getId());
  }

}
