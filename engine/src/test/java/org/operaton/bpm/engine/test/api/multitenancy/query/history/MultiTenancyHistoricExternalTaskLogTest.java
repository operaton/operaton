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
package org.operaton.bpm.engine.test.api.multitenancy.query.history;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.history.HistoricExternalTaskLog;
import org.operaton.bpm.engine.history.HistoricExternalTaskLogQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.operaton.bpm.engine.test.api.runtime.migration.models.ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.builder.DefaultExternalTaskModelBuilder.DEFAULT_PROCESS_KEY;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.builder.DefaultExternalTaskModelBuilder.DEFAULT_TOPIC;
import static org.assertj.core.api.Assertions.*;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class MultiTenancyHistoricExternalTaskLogTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected HistoryService historyService;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected ExternalTaskService externalTaskService;

  static final String TENANT_NULL = null;
  static final String TENANT_ONE = "tenant1";
  static final String TENANT_TWO = "tenant2";

  static final String WORKER_ID = "aWorkerId";
  static final String ERROR_DETAILS = "These are the error details!";
  static final long LOCK_DURATION = 5 * 60L * 1000L;


  @BeforeEach
  void setUp() {
    testRule.deployForTenant(TENANT_NULL, ONE_EXTERNAL_TASK_PROCESS);
    testRule.deployForTenant(TENANT_ONE, ONE_EXTERNAL_TASK_PROCESS);
    testRule.deployForTenant(TENANT_TWO, ONE_EXTERNAL_TASK_PROCESS);

    startProcessInstanceAndFailExternalTask(TENANT_ONE);
    startProcessInstanceFailAndCompleteExternalTask(TENANT_TWO);
  }

  @Test
  void shouldQueryWithoutTenantId() {

    //given two process with different tenants

    // when
    HistoricExternalTaskLogQuery query = historyService.
      createHistoricExternalTaskLogQuery();

    // then
    assertThat(query.count()).isEqualTo(5L);
  }

  @Test
  void shouldQueryFilterWithoutTenantId() {
    // given
    startProcessInstanceAndFailExternalTask(TENANT_NULL);

    // when
    HistoricExternalTaskLogQuery query = historyService
        .createHistoricExternalTaskLogQuery()
        .withoutTenantId();

    // then
    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryByTenantId() {

    // given two process with different tenants

    // when
    HistoricExternalTaskLogQuery queryTenant1 = historyService
      .createHistoricExternalTaskLogQuery()
      .tenantIdIn(TENANT_ONE);
    HistoricExternalTaskLogQuery queryTenant2 = historyService
      .createHistoricExternalTaskLogQuery()
      .tenantIdIn(TENANT_TWO);

    // then
    assertThat(queryTenant1.count()).isEqualTo(2L);
    assertThat(queryTenant2.count()).isEqualTo(3L);
  }

  @Test
  void shouldQueryByTenantIds() {

    //given two process with different tenants

    // when
    HistoricExternalTaskLogQuery query = historyService
      .createHistoricExternalTaskLogQuery()
      .tenantIdIn(TENANT_ONE, TENANT_TWO);

    // then
    assertThat(query.count()).isEqualTo(5L);
  }

  @Test
  void shouldQueryByNonExistingTenantId() {

    //given two process with different tenants

    // when
    HistoricExternalTaskLogQuery query = historyService
      .createHistoricExternalTaskLogQuery()
      .tenantIdIn("nonExisting");

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldFailQueryByTenantIdNull() {
    var historicExternalTaskLogQuery = historyService.createHistoricExternalTaskLogQuery();
    assertThatThrownBy(() -> historicExternalTaskLogQuery.tenantIdIn((String) null)).isInstanceOf(NullValueException.class);
  }

  @Test
  void shouldQuerySortingAsc() {

    //given two process with different tenants

    // when
    List<HistoricExternalTaskLog> historicExternalTaskLogs = historyService.createHistoricExternalTaskLogQuery()
      .orderByTenantId()
      .asc()
      .list();

    // then
    assertThat(historicExternalTaskLogs).hasSize(5);
    assertThat(historicExternalTaskLogs.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(historicExternalTaskLogs.get(1).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(historicExternalTaskLogs.get(2).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(historicExternalTaskLogs.get(3).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(historicExternalTaskLogs.get(4).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void shouldQuerySortingDesc() {

    //given two process with different tenants

    // when
    List<HistoricExternalTaskLog> historicExternalTaskLogs = historyService.createHistoricExternalTaskLogQuery()
      .orderByTenantId()
      .desc()
      .list();

    // then
    assertThat(historicExternalTaskLogs).hasSize(5);
    assertThat(historicExternalTaskLogs.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(historicExternalTaskLogs.get(1).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(historicExternalTaskLogs.get(2).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(historicExternalTaskLogs.get(3).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(historicExternalTaskLogs.get(4).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void shouldQueryNoAuthenticatedTenants() {

    // given
    identityService.setAuthentication("user", null, null);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  void shouldQueryAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_ONE));

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    assertThat(query.count()).isEqualTo(5L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isEqualTo(3L);
  }

  @Test
  void shouldQueryDisabledTenantCheck() {
    // given
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    // when
    HistoricExternalTaskLogQuery query = historyService.createHistoricExternalTaskLogQuery();

    // then
    assertThat(query.count()).isEqualTo(5L);
  }

  @Test
  void shouldGetErrorDetailsNoAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_ONE));

    String failedHistoricExternalTaskLogId = historyService
      .createHistoricExternalTaskLogQuery()
      .failureLog()
      .tenantIdIn(TENANT_ONE)
      .singleResult()
      .getId();
    identityService.clearAuthentication();
    identityService.setAuthentication("user", null, null);


    try {
      // when
      historyService.getHistoricExternalTaskLogErrorDetails(failedHistoricExternalTaskLogId);
      fail("Exception expected: It should not be possible to retrieve the error details");
    } catch (ProcessEngineException e) {
      // then
      String errorMessage = e.getMessage();
      assertThat(errorMessage)
              .contains("Cannot get the historic external task log ")
              .contains(failedHistoricExternalTaskLogId)
              .contains("because it belongs to no authenticated tenant.");
    }
  }

  @Test
  void shouldGetErrorDetailsAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_ONE));

    String failedHistoricExternalTaskLogId = historyService
      .createHistoricExternalTaskLogQuery()
      .failureLog()
      .tenantIdIn(TENANT_ONE)
      .singleResult()
      .getId();

    // when
    String stacktrace = historyService.getHistoricExternalTaskLogErrorDetails(failedHistoricExternalTaskLogId);

    // then
    assertThat(stacktrace).isEqualTo(ERROR_DETAILS);
  }

  @Test
  void shouldGetErrorDetailsAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    String logIdTenant1 = historyService
      .createHistoricExternalTaskLogQuery()
      .failureLog()
      .tenantIdIn(TENANT_ONE)
      .singleResult()
      .getId();

    String logIdTenant2 = historyService
      .createHistoricExternalTaskLogQuery()
      .failureLog()
      .tenantIdIn(TENANT_ONE)
      .singleResult()
      .getId();

    // when
    String stacktrace1 = historyService.getHistoricExternalTaskLogErrorDetails(logIdTenant1);
    String stacktrace2 = historyService.getHistoricExternalTaskLogErrorDetails(logIdTenant2);

    // then
    assertThat(stacktrace1).isEqualTo(ERROR_DETAILS);
    assertThat(stacktrace2).isEqualTo(ERROR_DETAILS);
  }

  // helper methods

  protected void completeExternalTask(String externalTaskId) {
    List<LockedExternalTask> list = externalTaskService.fetchAndLock(100, WORKER_ID, true)
      .topic(DEFAULT_TOPIC, LOCK_DURATION)
      .execute();
    externalTaskService.complete(externalTaskId, WORKER_ID);
    // unlock the remaining tasks
    for (LockedExternalTask lockedExternalTask : list) {
      if (!lockedExternalTask.getId().equals(externalTaskId)) {
        externalTaskService.unlock(lockedExternalTask.getId());
      }
    }
  }

  protected ExternalTask startProcessInstanceAndFailExternalTask(String tenant) {
    ProcessInstance pi = runtimeService.createProcessInstanceByKey(DEFAULT_PROCESS_KEY).processDefinitionTenantId(tenant).execute();
    ExternalTask externalTask = externalTaskService
      .createExternalTaskQuery()
      .processInstanceId(pi.getId())
      .singleResult();
    reportExternalTaskFailure(externalTask.getId());
    return externalTask;
  }

  protected void startProcessInstanceFailAndCompleteExternalTask(String tenant) {
    ExternalTask task = startProcessInstanceAndFailExternalTask(tenant);
    completeExternalTask(task.getId());
  }

  protected void reportExternalTaskFailure(String externalTaskId) {
    reportExternalTaskFailure(externalTaskId, DEFAULT_TOPIC, WORKER_ID, 1, false, "This is an error!");
  }

  protected void reportExternalTaskFailure(String externalTaskId, String topic, String workerId, Integer retries,
                                           boolean usePriority, String errorMessage) {
    List<LockedExternalTask> list = externalTaskService.fetchAndLock(100, workerId, usePriority)
      .topic(topic, LOCK_DURATION)
      .execute();
    externalTaskService.handleFailure(externalTaskId, workerId, errorMessage, ERROR_DETAILS, retries, 0L);

    for (LockedExternalTask lockedExternalTask : list) {
      externalTaskService.unlock(lockedExternalTask.getId());
    }
  }
}
