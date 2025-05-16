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
package org.operaton.bpm.engine.test.api.multitenancy.suspensionstate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.externaltask.ExternalTaskQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

class MultiTenancyProcessInstanceSuspensionStateTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String PROCESS_DEFINITION_KEY = "testProcess";

  protected static final BpmnModelInstance PROCESS = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
      .startEvent()
      .parallelGateway("fork")
        .userTask()
      .moveToLastGateway()
        .sendTask()
          .operatonType("external")
          .operatonTopic("test")
        .boundaryEvent()
          .timerWithDuration("PT1M")
      .done();

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @BeforeEach
  void setUp() {

    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);
    testRule.deploy(PROCESS);

    engineRule.getRuntimeService().createProcessInstanceByKey(PROCESS_DEFINITION_KEY).processDefinitionTenantId(TENANT_ONE).execute();
    engineRule.getRuntimeService().createProcessInstanceByKey(PROCESS_DEFINITION_KEY).processDefinitionTenantId(TENANT_TWO).execute();
    engineRule.getRuntimeService().createProcessInstanceByKey(PROCESS_DEFINITION_KEY).processDefinitionWithoutTenantId().execute();
  }

  @Test
  void suspendAndActivateProcessInstancesForAllTenants() {
    // given activated process instances
    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // first suspend
    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    // then activate
    engineRule.getRuntimeService()
    .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .activate();

    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();
  }

  @Test
  void suspendProcessInstanceForTenant() {
    // given activated process instances
    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void suspendProcessInstanceForNonTenant() {
    // given activated process instances
    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void activateProcessInstanceForTenant() {
    // given suspended process instances
    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void activateProcessInstanceForNonTenant() {
    // given suspended process instances
    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void suspendAndActivateProcessInstancesIncludingUserTasksForAllTenants() {
    // given activated user tasks
    TaskQuery query = engineRule.getTaskService().createTaskQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // first suspend
    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    // then activate
    engineRule.getRuntimeService()
    .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .activate();

    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();
  }

  @Test
  void suspendProcessInstanceIncludingUserTaskForTenant() {
    // given activated user tasks
    TaskQuery query = engineRule.getTaskService().createTaskQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void suspendProcessInstanceIncludingUserTaskForNonTenant() {
    // given activated user tasks
    TaskQuery query = engineRule.getTaskService().createTaskQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void activateProcessInstanceIncludingUserTaskForTenant() {
    // given suspended user tasks
    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    TaskQuery query = engineRule.getTaskService().createTaskQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void activateProcessInstanceIncludingUserTaskForNonTenant() {
    // given suspended user tasks
    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    TaskQuery query = engineRule.getTaskService().createTaskQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void suspendAndActivateProcessInstancesIncludingExternalTasksForAllTenants() {
    // given activated external tasks
    ExternalTaskQuery query = engineRule.getExternalTaskService().createExternalTaskQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // first suspend
    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    // then activate
    engineRule.getRuntimeService()
    .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .activate();

    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();
  }

  @Test
  void suspendProcessInstanceIncludingExternalTaskForTenant() {
    // given activated external tasks
    ExternalTaskQuery query = engineRule.getExternalTaskService().createExternalTaskQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void suspendProcessInstanceIncludingExternalTaskForNonTenant() {
    // given activated external tasks
    ExternalTaskQuery query = engineRule.getExternalTaskService().createExternalTaskQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().singleResult().getTenantId()).isNull();
  }

  @Test
  void activateProcessInstanceIncludingExternalTaskForTenant() {
    // given suspended external tasks
    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    ExternalTaskQuery query = engineRule.getExternalTaskService().createExternalTaskQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void activateProcessInstanceIncludingExternalTaskForNonTenant() {
    // given suspended external tasks
    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    ExternalTaskQuery query = engineRule.getExternalTaskService().createExternalTaskQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().singleResult().getTenantId()).isNull();
  }

  @Test
  void suspendAndActivateProcessInstancesIncludingJobsForAllTenants() {
    // given activated jobs
    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // first suspend
    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    // then activate
    engineRule.getRuntimeService()
    .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .activate();

    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();
  }

  @Test
  void suspendProcessInstanceIncludingJobForTenant() {
    // given activated jobs
    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void suspendProcessInstanceIncludingJobForNonTenant() {
    // given activated jobs
    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().singleResult().getTenantId()).isNull();
  }

  @Test
  void activateProcessInstanceIncludingJobForTenant() {
    // given suspended job
    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void activateProcessInstanceIncludingJobForNonTenant() {
    // given suspended jobs
    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().singleResult().getTenantId()).isNull();
  }

  @Test
  void suspendProcessInstanceNoAuthenticatedTenants() {
    // given activated process instances
    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getIdentityService().setAuthentication("user", null, null);

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    engineRule.getIdentityService().clearAuthentication();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void failToSuspendProcessInstanceByProcessDefinitionIdNoAuthenticatedTenants() {
    ProcessDefinition processDefinition = engineRule.getRepositoryService().createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY).tenantIdIn(TENANT_ONE).singleResult();

    engineRule.getIdentityService().setAuthentication("user", null, null);
    var updateProcessInstanceSuspensionStateBuilder = engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionId(processDefinition.getId());

    // when/then
    assertThatThrownBy(updateProcessInstanceSuspensionStateBuilder::suspend)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process definition '"
          + processDefinition.getId() +"' because it belongs to no authenticated tenant");
  }

  @Test
  void suspendProcessInstanceWithAuthenticatedTenant() {
    // given activated process instances
    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getIdentityService().setAuthentication("user", null, List.of(TENANT_ONE));

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    engineRule.getIdentityService().clearAuthentication();

    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().tenantIdIn(TENANT_TWO).count()).isEqualTo(1L);
    assertThat(query.suspended().withoutTenantId().count()).isEqualTo(1L);
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void suspendProcessInstanceDisabledTenantCheck() {
    // given activated process instances
    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    engineRule.getIdentityService().setAuthentication("user", null, null);

    engineRule.getRuntimeService()
      .updateProcessInstanceSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.suspended().tenantIdIn(TENANT_ONE, TENANT_TWO).count()).isEqualTo(2L);
    assertThat(query.suspended().withoutTenantId().count()).isEqualTo(1L);
  }
}
