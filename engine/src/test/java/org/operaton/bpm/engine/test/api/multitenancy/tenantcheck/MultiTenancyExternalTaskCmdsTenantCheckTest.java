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
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

class MultiTenancyExternalTaskCmdsTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String PROCESS_DEFINITION_KEY = "twoExternalTaskProcess";
  protected static final String PROCESS_DEFINITION_KEY_ONE = "oneExternalTaskProcess";
  private static final String ERROR_DETAILS = "anErrorDetail";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected static final String WORKER_ID = "aWorkerId";

  protected static final long LOCK_TIME = 10000L;

  protected static final String TOPIC_NAME = "externalTaskTopic";

  protected static final String ERROR_MESSAGE = "errorMessage";

  protected ExternalTaskService externalTaskService;

  protected TaskService taskService;

  protected String processInstanceId;

  protected IdentityService identityService;

  @BeforeEach
  void init() {
    testRule.deployForTenant(TENANT_ONE,
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml");

    processInstanceId = engineRule.getRuntimeService().startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();

  }

  // fetch and lock test cases
  @Test
  void testFetchAndLockWithAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // then
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    assertThat(externalTasks).hasSize(1);

  }

  @Test
  void testFetchAndLockWithNoAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null);

    // then external task cannot be fetched due to the absence of tenant Id authentication
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    assertThat(externalTasks).isEmpty();

  }

  @Test
  void testFetchAndLockWithDifferentTenant() {

    identityService.setAuthentication("aUserId", null, List.of("tenantTwo"));

    // then external task cannot be fetched due to the absence of 'tenant1' authentication
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    assertThat(externalTasks).isEmpty();

  }

  @Test
  void testFetchAndLockWithDisabledTenantCheck() {

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    // then
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    assertThat(externalTasks).hasSize(1);

  }

  @Test
  void testFetchAndLockWithoutTenantId() {
    // given
    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .withoutTenantId()
      .execute();

    // then
    assertThat(externalTasks).isEmpty();
  }

  @Test
  void testFetchAndLockWithTenantId() {
    // given
    testRule.deploy("org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml");
    engineRule.getRuntimeService().startProcessInstanceByKey(PROCESS_DEFINITION_KEY_ONE).getId();
    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .tenantIdIn(TENANT_ONE)
      .execute();

    // then
    assertThat(externalTasks).hasSize(1);
  }

  @Test
  void testFetchAndLockWithTenantIdIn() {

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .tenantIdIn(TENANT_ONE, TENANT_TWO)
      .execute();

    // then
    assertThat(externalTasks).hasSize(1);
  }

  @Test
  void testFetchAndLockWithTenantIdInTwoTenants() {
    // given
    testRule.deploy("org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskWithPriorityProcess.bpmn20.xml");
    engineRule.getRuntimeService().startProcessInstanceByKey("twoExternalTaskWithPriorityProcess").getId();
    testRule.deployForTenant(TENANT_TWO,
        "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml");
    String instanceId = engineRule.getRuntimeService().startProcessInstanceByKey(PROCESS_DEFINITION_KEY_ONE).getId();

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(2, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .tenantIdIn(TENANT_ONE, TENANT_TWO)
      .execute();

    // then
    assertThat(externalTasks).hasSize(2);

    for (LockedExternalTask externalTask : externalTasks) {
      if (externalTask.getProcessInstanceId().equals(processInstanceId)) {
        assertThat(externalTask.getTenantId()).isEqualTo(TENANT_ONE);
      } else if (externalTask.getProcessInstanceId().equals(instanceId)) {
        assertThat(externalTask.getTenantId()).isEqualTo(TENANT_TWO);
      } else {
        fail("No other external tasks should be available!");
      }
    }
  }

  // complete external task test cases
  @Test
  void testCompleteWithAuthenticatedTenant() {

    String externalTaskId = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    assertThat(externalTaskService.createExternalTaskQuery().active().count()).isOne();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    externalTaskService.complete(externalTaskId, WORKER_ID);

    assertThat(externalTaskService.createExternalTaskQuery().active().count()).isZero();

  }

  @Test
  void testCompleteWithNoAuthenticatedTenant() {

    String externalTaskId = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    assertThat(externalTaskService.createExternalTaskQuery().active().count()).isOne();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> externalTaskService.complete(externalTaskId, WORKER_ID))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testCompleteWithDisableTenantCheck() {

    String externalTaskId = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    assertThat(externalTaskService.createExternalTaskQuery().active().count()).isOne();

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    externalTaskService.complete(externalTaskId, WORKER_ID);
    // then
    assertThat(externalTaskService.createExternalTaskQuery().active().count()).isZero();
  }

  // handle failure test cases
  @Test
  void testHandleFailureWithAuthenticatedTenant() {

    LockedExternalTask task = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0);

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, 1, 0);

    // then
    assertThat(externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute()
        .get(0)
        .getErrorMessage()).isEqualTo(ERROR_MESSAGE);

  }

  @Test
  void testHandleFailureWithNoAuthenticatedTenant() {

    LockedExternalTask task = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0);
    String taskId = task.getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> externalTaskService.handleFailure(taskId, WORKER_ID, ERROR_MESSAGE, 1, 0))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void testHandleFailureWithDisabledTenantCheck() {

    String taskId = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    externalTaskService.handleFailure(taskId, WORKER_ID, ERROR_MESSAGE, 1, 0);
    // then
    assertThat(externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute()
        .get(0)
        .getErrorMessage()).isEqualTo(ERROR_MESSAGE);
  }

  // handle BPMN error
  @Test
  void testHandleBPMNErrorWithAuthenticatedTenant() {

    String taskId = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // when
    externalTaskService.handleBpmnError(taskId, WORKER_ID, "ERROR-OCCURED");

    // then
    assertThat(taskService.createTaskQuery().singleResult().getTaskDefinitionKey()).isEqualTo("afterBpmnError");
  }

  @Test
  void testHandleBPMNErrorWithNoAuthenticatedTenant() {

    String taskId = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> externalTaskService.handleBpmnError(taskId, WORKER_ID, "ERROR-OCCURED"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testHandleBPMNErrorWithDisabledTenantCheck() {

    String taskId = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // when
    externalTaskService.handleBpmnError(taskId, WORKER_ID, "ERROR-OCCURED");

    // then
    assertThat(taskService.createTaskQuery().singleResult().getTaskDefinitionKey()).isEqualTo("afterBpmnError");

  }

  // setRetries test
  @Test
  void testSetRetriesWithAuthenticatedTenant() {
    // given
    String externalTaskId = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // when
    externalTaskService.setRetries(externalTaskId, 5);

    // then
    assertThat((int) externalTaskService.createExternalTaskQuery().singleResult().getRetries()).isEqualTo(5);
  }

  @Test
  void testSetRetriesWithNoAuthenticatedTenant() {
    // given
    String externalTaskId = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> externalTaskService.setRetries(externalTaskId, 5))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testSetRetriesWithDisabledTenantCheck() {
    // given
    String externalTaskId = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // when
    externalTaskService.setRetries(externalTaskId, 5);

    // then
    assertThat((int) externalTaskService.createExternalTaskQuery().singleResult().getRetries()).isEqualTo(5);

  }

  // set priority test cases
  @Test
  void testSetPriorityWithAuthenticatedTenant() {
    // given
    String externalTaskId = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // when
    externalTaskService.setPriority(externalTaskId, 1);

    // then
    assertThat((int) externalTaskService.createExternalTaskQuery().singleResult().getPriority()).isEqualTo(1);
  }

  @Test
  void testSetPriorityWithNoAuthenticatedTenant() {
    // given
    String externalTaskId = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> externalTaskService.setPriority(externalTaskId, 1))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testSetPriorityWithDisabledTenantCheck() {
    // given
    String externalTaskId = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // when
    externalTaskService.setPriority(externalTaskId, 1);

    // then
    assertThat((int) externalTaskService.createExternalTaskQuery().singleResult().getPriority()).isEqualTo(1);
  }

  // unlock test cases
  @Test
  void testUnlockWithAuthenticatedTenant() {
    // given
    String externalTaskId = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    assertThat(externalTaskService.createExternalTaskQuery().locked().count()).isOne();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // when
    externalTaskService.unlock(externalTaskId);

    // then
    assertThat(externalTaskService.createExternalTaskQuery().locked().count()).isZero();
  }

  @Test
  void testUnlockWithNoAuthenticatedTenant() {
    // given
    String externalTaskId = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> externalTaskService.unlock(externalTaskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void testUnlockWithDisabledTenantCheck() {
    // given
    String externalTaskId = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    externalTaskService.unlock(externalTaskId);
    // then
    assertThat(externalTaskService.createExternalTaskQuery().locked().count()).isZero();
  }

  // get error details tests
  @Test
  void testGetErrorDetailsWithAuthenticatedTenant() {
    // given
    String externalTaskId = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute()
      .get(0)
      .getId();

    externalTaskService.handleFailure(externalTaskId,WORKER_ID,ERROR_MESSAGE,ERROR_DETAILS,1,1000L);

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    // when then
    assertThat(externalTaskService.getExternalTaskErrorDetails(externalTaskId)).isEqualTo(ERROR_DETAILS);
  }

  @Test
  void testGetErrorDetailsWithNoAuthenticatedTenant() {
    // given
    String externalTaskId = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute()
        .get(0)
        .getId();

    externalTaskService.handleFailure(externalTaskId,WORKER_ID,ERROR_MESSAGE,ERROR_DETAILS,1,1000L);

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> externalTaskService.getExternalTaskErrorDetails(externalTaskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the process instance '"
          + processInstanceId +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void testGetErrorDetailsWithDisabledTenantCheck() {
    // given
    String externalTaskId = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute()
        .get(0)
        .getId();

    externalTaskService.handleFailure(externalTaskId,WORKER_ID,ERROR_MESSAGE,ERROR_DETAILS,1,1000L);

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    assertThat(externalTaskService.getExternalTaskErrorDetails(externalTaskId)).isEqualTo(ERROR_DETAILS);
  }
}
