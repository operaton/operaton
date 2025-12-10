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
package org.operaton.bpm.engine.test.api.optimize;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.OptimizeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.SuspensionState;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Attachment;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.BatchSuspensionHelper;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.history.UserOperationLogEntry.CATEGORY_OPERATOR;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.OPERATION_TYPE_ACTIVATE;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.OPERATION_TYPE_ACTIVATE_JOB;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.OPERATION_TYPE_SUSPEND;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.OPERATION_TYPE_SUSPEND_JOB;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION;
import static org.assertj.core.api.Assertions.assertThat;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class GetHistoricOperationLogsForOptimizeTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);
  BatchSuspensionHelper helper = new BatchSuspensionHelper(engineRule);

  OptimizeService optimizeService;

  String userId = "test";

  IdentityService identityService;
  RuntimeService runtimeService;
  TaskService taskService;
  RepositoryService repositoryService;

  @BeforeEach
  void init() {
    ProcessEngineConfigurationImpl config =
      engineRule.getProcessEngineConfiguration();
    optimizeService = config.getOptimizeService();

    createUser(userId);
    identityService.setAuthenticatedUserId(userId);
    deploySimpleDefinition();
  }

  @AfterEach
  void cleanUp() {
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    ClockUtil.reset();
    identityService.clearAuthentication();
    helper.removeAllRunningAndHistoricBatches();
  }

  @Test
  void getHistoricUserOperationLogs_suspendProcessInstanceByProcessInstanceId() {
    // given
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    runtimeService.suspendProcessInstanceById(processInstance.getProcessInstanceId());
    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    runtimeService.activateProcessInstanceById(processInstance.getProcessInstanceId());

    // when
    List<UserOperationLogEntry> userOperationsLog =
      optimizeService.getHistoricUserOperationLogs(pastDate(), null, 10);

    // then
    assertThat(userOperationsLog)
      .hasSize(2)
      .allSatisfy(
        entry -> {
          assertThat(entry).isNotNull();
          assertThat(entry.getId()).isNotNull();
          assertThat(entry.getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);
          assertThat(entry.getOrgValue()).isNull();
          assertThat(entry.getTimestamp()).isNotNull();
          assertThat(entry.getProcessDefinitionKey()).isEqualTo("process");
          assertThat(entry.getProcessDefinitionId()).isNotNull();
          assertThat(entry.getProcessInstanceId()).isEqualTo(processInstance.getId());
          assertThat(entry.getCategory()).isEqualTo(CATEGORY_OPERATOR);
        });

    assertThat(userOperationsLog.get(0).getOperationType()).isEqualTo(OPERATION_TYPE_SUSPEND);
    assertThat(userOperationsLog.get(0).getNewValue()).isEqualTo(SuspensionState.SUSPENDED.getName());

    assertThat(userOperationsLog.get(1).getOperationType()).isEqualTo(OPERATION_TYPE_ACTIVATE);
    assertThat(userOperationsLog.get(1).getNewValue()).isEqualTo(SuspensionState.ACTIVE.getName());
  }

  @Test
  void getHistoricUserOperationLogs_suspendProcessInstanceByProcessDefinitionId() {
    // given
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processInstance.getProcessDefinitionId());
    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    runtimeService.activateProcessInstanceByProcessDefinitionId(processInstance.getProcessDefinitionId());

    // when
    List<UserOperationLogEntry> userOperationsLog =
      optimizeService.getHistoricUserOperationLogs(pastDate(), null, 10);

    // then
    assertThat(userOperationsLog)
      .hasSize(2)
      .allSatisfy(entry -> {
        assertThat(entry).isNotNull();
        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);
        assertThat(entry.getOrgValue()).isNull();
        assertThat(entry.getTimestamp()).isNotNull();
        assertThat(entry.getProcessDefinitionKey()).isEqualTo("process");
        assertThat(entry.getProcessDefinitionId()).isNotNull();
        assertThat(entry.getProcessInstanceId()).isNull();
        assertThat(entry.getCategory()).isEqualTo(CATEGORY_OPERATOR);
      });
    assertThat(userOperationsLog.get(0).getOperationType()).isEqualTo(OPERATION_TYPE_SUSPEND);
    assertThat(userOperationsLog.get(0).getNewValue()).isEqualTo(SuspensionState.SUSPENDED.getName());
  }

  @Test
  void getHistoricUserOperationLogs_suspendProcessInstanceByProcessDefinitionKey() {
    // given
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    runtimeService.startProcessInstanceByKey("process");

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    runtimeService.suspendProcessInstanceByProcessDefinitionKey("process");
    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    runtimeService.activateProcessInstanceByProcessDefinitionKey("process");

    // when
    List<UserOperationLogEntry> userOperationsLog =
      optimizeService.getHistoricUserOperationLogs(pastDate(), null, 10);

    // then
    assertThat(userOperationsLog)
      .hasSize(2)
      .allSatisfy(entry -> {
        assertThat(entry).isNotNull();
        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);
        assertThat(entry.getOrgValue()).isNull();
        assertThat(entry.getTimestamp()).isNotNull();
        assertThat(entry.getProcessDefinitionKey()).isEqualTo("process");
        assertThat(entry.getProcessDefinitionId()).isNull();
        assertThat(entry.getProcessInstanceId()).isNull();
        assertThat(entry.getCategory()).isEqualTo(CATEGORY_OPERATOR);
      });
    assertThat(userOperationsLog.get(0).getOperationType()).isEqualTo(OPERATION_TYPE_SUSPEND);
    assertThat(userOperationsLog.get(0).getNewValue()).isEqualTo(SuspensionState.SUSPENDED.getName());

    assertThat(userOperationsLog.get(1).getOperationType()).isEqualTo(OPERATION_TYPE_ACTIVATE);
    assertThat(userOperationsLog.get(1).getNewValue()).isEqualTo(SuspensionState.ACTIVE.getName());
  }

  @Test
  void getHistoricUserOperationLogs_suspendProcessDefinitionById() {
    // given
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    repositoryService.suspendProcessDefinitionById(processInstance.getProcessDefinitionId(), true, null);
    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    repositoryService.activateProcessDefinitionById(processInstance.getProcessDefinitionId(), true, null);

    // when
    List<UserOperationLogEntry> userOperationsLog =
      optimizeService.getHistoricUserOperationLogs(pastDate(), null, 10);

    // then
    assertThat(userOperationsLog)
      .hasSize(4)
      .allSatisfy(
        entry -> {
          assertThat(entry.getEntityType()).isEqualTo(EntityTypes.PROCESS_DEFINITION);
          assertThat(entry.getOrgValue()).isNull();
          assertThat(entry.getTimestamp()).isNotNull();
          assertThat(entry.getProcessDefinitionKey()).isEqualTo("process");
          assertThat(entry.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
          assertThat(entry.getProcessInstanceId()).isNull();
          assertThat(entry.getCategory()).isEqualTo(CATEGORY_OPERATOR);
        }
    );
    // Verify suspend operation entries (order within same timestamp may vary by database)
    assertThat(userOperationsLog.subList(0, 2))
      .extracting(UserOperationLogEntry::getOperationType)
      .containsOnly(OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION);
    assertThat(userOperationsLog.subList(0, 2))
      .extracting(UserOperationLogEntry::getNewValue)
      .containsExactlyInAnyOrder(SuspensionState.SUSPENDED.getName(), "true");

    // Verify activate operation entries (order within same timestamp may vary by database)
    assertThat(userOperationsLog.subList(2, 4))
      .extracting(UserOperationLogEntry::getOperationType)
      .containsOnly(OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION);
    assertThat(userOperationsLog.subList(2, 4))
      .extracting(UserOperationLogEntry::getNewValue)
      .containsExactlyInAnyOrder(SuspensionState.ACTIVE.getName(), "true");
  }

  @Test
  void getHistoricUserOperationLogs_suspendProcessDefinitionByKey() {
    // given
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    runtimeService.startProcessInstanceByKey("process");

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    repositoryService.suspendProcessDefinitionByKey("process", true, null);
    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    repositoryService.activateProcessDefinitionByKey("process", true, null);

    // when
    List<UserOperationLogEntry> userOperationsLog =
      optimizeService.getHistoricUserOperationLogs(pastDate(), null, 10);

    // then
    assertThat(userOperationsLog)
      .hasSize(4)
      .allSatisfy(
        entry -> {
          assertThat(entry.getEntityType()).isEqualTo(EntityTypes.PROCESS_DEFINITION);
          assertThat(entry.getOrgValue()).isNull();
          assertThat(entry.getTimestamp()).isNotNull();
          assertThat(entry.getProcessDefinitionKey()).isEqualTo("process");
          assertThat(entry.getProcessDefinitionId()).isNull();
          assertThat(entry.getProcessInstanceId()).isNull();
          assertThat(entry.getCategory()).isEqualTo(CATEGORY_OPERATOR);
        }
    );
    // Verify suspend operation entries (order within same timestamp may vary by database)
    assertThat(userOperationsLog.subList(0, 2))
      .extracting(UserOperationLogEntry::getOperationType)
      .containsOnly(OPERATION_TYPE_SUSPEND_PROCESS_DEFINITION);
    assertThat(userOperationsLog.subList(0, 2))
      .extracting(UserOperationLogEntry::getNewValue)
      .containsExactlyInAnyOrder(SuspensionState.SUSPENDED.getName(), "true");

    // Verify activate operation entries (order within same timestamp may vary by database)
    assertThat(userOperationsLog.subList(2, 4))
      .extracting(UserOperationLogEntry::getOperationType)
      .containsOnly(OPERATION_TYPE_ACTIVATE_PROCESS_DEFINITION);
    assertThat(userOperationsLog.subList(2, 4))
      .extracting(UserOperationLogEntry::getNewValue)
      .containsExactlyInAnyOrder(SuspensionState.ACTIVE.getName(), "true");
  }

  @Test
  void getHistoricUserOperationLogs_suspendByBatchJobAndProcessInstanceId() {
    // given
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    Batch suspendProcess = runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds(Collections.singletonList(processInstance.getProcessInstanceId()))
      .suspendAsync();
    helper.completeSeedJobs(suspendProcess);
    helper.executeJobs(suspendProcess);
    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    Batch resumeProcess = runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds(Collections.singletonList(processInstance.getProcessInstanceId()))
      .activateAsync();
    helper.completeSeedJobs(resumeProcess);
    helper.executeJobs(resumeProcess);

    // when
    List<UserOperationLogEntry> userOperationsLog =
      optimizeService.getHistoricUserOperationLogs(pastDate(), null, 10);

    // then
    assertThat(userOperationsLog).hasSize(4);
    assertThat(userOperationsLog.get(0).getOperationType()).isEqualTo(OPERATION_TYPE_SUSPEND_JOB);
    assertThat(userOperationsLog.get(0).getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);
    assertThat(userOperationsLog.get(0).getProcessDefinitionKey()).isNull();
    assertThat(userOperationsLog.get(0).getProcessDefinitionId()).isNull();
    assertThat(userOperationsLog.get(0).getProcessInstanceId()).isNull();

    // creates two suspend jobs, one for number of process instances affected and one for the async operation
    assertThat(userOperationsLog.get(1).getOperationType()).isEqualTo(OPERATION_TYPE_SUSPEND_JOB);
    assertThat(userOperationsLog.get(1).getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);

    assertThat(userOperationsLog.get(2)).isNotNull();
    assertThat(userOperationsLog.get(2).getOperationType()).isEqualTo(OPERATION_TYPE_ACTIVATE_JOB);
    assertThat(userOperationsLog.get(2).getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);
    assertThat(userOperationsLog.get(2).getOrgValue()).isNull();
    assertThat(userOperationsLog.get(2).getNewValue()).isNotNull();
    assertThat(userOperationsLog.get(2).getTimestamp()).isNotNull();
    assertThat(userOperationsLog.get(2).getProcessDefinitionKey()).isNull();
    assertThat(userOperationsLog.get(2).getProcessDefinitionId()).isNull();
    assertThat(userOperationsLog.get(2).getProcessInstanceId()).isNull();
    assertThat(userOperationsLog.get(2).getCategory()).isEqualTo(CATEGORY_OPERATOR);

    // creates two activate jobs, one for number of process instances affected and one for the async operation
    assertThat(userOperationsLog.get(3).getOperationType()).isEqualTo(OPERATION_TYPE_ACTIVATE_JOB);
    assertThat(userOperationsLog.get(3).getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);
  }

  @Test
  void getHistoricUserOperationLogs_suspendByBatchJobAndQuery() {
    // given
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    runtimeService.startProcessInstanceByKey("process");

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    Batch suspendprocess = runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceQuery(runtimeService.createProcessInstanceQuery().active())
      .suspendAsync();
    helper.completeSeedJobs(suspendprocess);
    helper.executeJobs(suspendprocess);
    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    Batch resumeProcess = runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceQuery(runtimeService.createProcessInstanceQuery().suspended())
      .activateAsync();
    helper.completeSeedJobs(resumeProcess);
    helper.executeJobs(resumeProcess);

    // when
    List<UserOperationLogEntry> userOperationsLog =
      optimizeService.getHistoricUserOperationLogs(pastDate(), null, 10);

    // then
    assertThat(userOperationsLog).hasSize(4);
    assertThat(userOperationsLog.get(0).getOperationType()).isEqualTo(OPERATION_TYPE_SUSPEND_JOB);
    assertThat(userOperationsLog.get(0).getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);
    assertThat(userOperationsLog.get(0).getProcessDefinitionKey()).isNull();
    assertThat(userOperationsLog.get(0).getProcessDefinitionId()).isNull();
    assertThat(userOperationsLog.get(0).getProcessInstanceId()).isNull();

    // creates two suspend jobs, one for number of process instances affected and one for the async operation
    assertThat(userOperationsLog.get(1).getOperationType()).isEqualTo(OPERATION_TYPE_SUSPEND_JOB);
    assertThat(userOperationsLog.get(1).getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);

    assertThat(userOperationsLog.get(2)).isNotNull();
    assertThat(userOperationsLog.get(2).getOperationType()).isEqualTo(OPERATION_TYPE_ACTIVATE_JOB);
    assertThat(userOperationsLog.get(2).getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);
    assertThat(userOperationsLog.get(2).getOrgValue()).isNull();
    assertThat(userOperationsLog.get(2).getNewValue()).isNotNull();
    assertThat(userOperationsLog.get(2).getTimestamp()).isNotNull();
    assertThat(userOperationsLog.get(2).getProcessDefinitionKey()).isNull();
    assertThat(userOperationsLog.get(2).getProcessDefinitionId()).isNull();
    assertThat(userOperationsLog.get(2).getProcessInstanceId()).isNull();
    assertThat(userOperationsLog.get(2).getCategory()).isEqualTo(CATEGORY_OPERATOR);

    // creates two activate jobs, one for number of process instances affected and one for the async operation
    assertThat(userOperationsLog.get(3).getOperationType()).isEqualTo(OPERATION_TYPE_ACTIVATE_JOB);
    assertThat(userOperationsLog.get(3).getEntityType()).isEqualTo(EntityTypes.PROCESS_INSTANCE);
  }

  @Test
  void occurredAfterParameterWorks() {
    // given
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    final ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("process");
    runtimeService.suspendProcessInstanceById(processInstance.getProcessInstanceId());

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    runtimeService.activateProcessInstanceById(processInstance.getProcessInstanceId());

    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    runtimeService.suspendProcessInstanceById(processInstance.getProcessInstanceId());

    // when
    List<UserOperationLogEntry> userOperationsLog =
      optimizeService.getHistoricUserOperationLogs(now, null, 10);

    // then
    Set<String> allowedOperationsTypes = new HashSet<>(Arrays.asList(OPERATION_TYPE_SUSPEND, OPERATION_TYPE_ACTIVATE));
    assertThat(userOperationsLog).hasSize(2);
    assertThat(allowedOperationsTypes)
      .contains(userOperationsLog.get(0).getOperationType())
      .contains(userOperationsLog.get(1).getOperationType());
  }

  @Test
  void occurredAtParameterWorks() {
    // given
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    final ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("process");
    runtimeService.suspendProcessInstanceById(processInstance.getProcessInstanceId());

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    runtimeService.activateProcessInstanceById(processInstance.getProcessInstanceId());

    // when
    List<UserOperationLogEntry> userOperationsLog =
      optimizeService.getHistoricUserOperationLogs(null, now, 10);

    // then
    assertThat(userOperationsLog).hasSize(1);
    assertThat(userOperationsLog.get(0).getOperationType()).isEqualTo(OPERATION_TYPE_SUSPEND);
  }

  @Test
  void occurredAfterAndOccurredAtParameterWorks() {
    // given
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    final ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("process");
    runtimeService.suspendProcessInstanceById(processInstance.getProcessInstanceId());

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    runtimeService.activateProcessInstanceById(processInstance.getProcessInstanceId());

    // when
    List<UserOperationLogEntry> userOperationsLog =
      optimizeService.getHistoricUserOperationLogs(now, now, 10);

    // then
    assertThat(userOperationsLog).isEmpty();
  }

  @Test
  void maxResultsParameterWorks() {
     // given
    final ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("process");
    runtimeService.suspendProcessInstanceById(processInstance.getProcessInstanceId());
    runtimeService.activateProcessInstanceById(processInstance.getProcessInstanceId());
    runtimeService.suspendProcessInstanceById(processInstance.getProcessInstanceId());
    runtimeService.activateProcessInstanceById(processInstance.getProcessInstanceId());

    // when
    List<UserOperationLogEntry> userOperationsLog =
      optimizeService.getHistoricUserOperationLogs(pastDate(), null, 3);

    // then
    assertThat(userOperationsLog).hasSize(3);
  }

  @Test
  void resultIsSortedByTimestamp() {
    // given
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    final ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("process");
    runtimeService.suspendProcessInstanceById(processInstance.getProcessInstanceId());

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    runtimeService.activateProcessInstanceById(processInstance.getProcessInstanceId());

    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    runtimeService.suspendProcessInstanceById(processInstance.getProcessInstanceId());

    // when
    List<UserOperationLogEntry> userOperationsLog =
      optimizeService.getHistoricUserOperationLogs(pastDate(), null, 4);

    // then
    assertThat(userOperationsLog).hasSize(3);
    assertThat(userOperationsLog.get(0).getOperationType()).isEqualTo(OPERATION_TYPE_SUSPEND);
    assertThat(userOperationsLog.get(1).getOperationType()).isEqualTo(OPERATION_TYPE_ACTIVATE);
    assertThat(userOperationsLog.get(2).getOperationType()).isEqualTo(OPERATION_TYPE_SUSPEND);
  }

  @Test
  void fetchOnlyProcessInstanceSuspensionStateBasedLogEntries() {
    // given
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("process");
    createLogEntriesThatShouldNotBeReturned(processInstance.getId());
    assertThat(engineRule.getHistoryService().createUserOperationLogQuery().count()).isPositive();

    // when
    List<UserOperationLogEntry> userOperationsLog =
      optimizeService.getHistoricUserOperationLogs(pastDate(), null, 10);

    // then
    assertThat(userOperationsLog).isEmpty();
  }

  private void createLogEntriesThatShouldNotBeReturned(String processInstanceId) {
    ClockUtil.setCurrentTime(new Date());

    String processTaskId = taskService.createTaskQuery().singleResult().getId();

    // create and remove some links
    taskService.addCandidateUser(processTaskId, "er");
    taskService.deleteCandidateUser(processTaskId, "er");
    taskService.addCandidateGroup(processTaskId, "wir");
    taskService.deleteCandidateGroup(processTaskId, "wir");

    // assign and reassign the owner
    taskService.setOwner(processTaskId, "icke");

    // change priority of task
    taskService.setPriority(processTaskId, 10);

    // add and delete an attachment
    Attachment attachment = taskService.createAttachment(
      "image/ico",
      processTaskId,
      processInstanceId,
      "favicon.ico",
      "favicon",
      "http://operaton.com/favicon.ico"
    );
    taskService.deleteAttachment(attachment.getId());
    runtimeService.deleteProcessInstance(processInstanceId, "that's why");

    // create a standalone userTask
    Task userTask = taskService.newTask();
    userTask.setName("to do");
    taskService.saveTask(userTask);

    // change some properties manually to create an update event
    ClockUtil.setCurrentTime(new Date());
    userTask.setDescription("desc");
    userTask.setOwner("icke");
    userTask.setAssignee("er");
    userTask.setDueDate(new Date());
    taskService.saveTask(userTask);

    taskService.deleteTask(userTask.getId(), true);
  }

  protected void createUser(String userId) {
    User user = identityService.newUser(userId);
    identityService.saveUser(user);
  }

  private Date pastDate() {
    return new Date(2L);
  }

  private void deploySimpleDefinition() {
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask")
      .name("task")
      .endEvent("endEvent")
      .done();
    testHelper.deploy(simpleDefinition);
  }

}
