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
package org.operaton.bpm.engine.test.api.multitenancy;
import java.util.List;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.BatchRestartHelper;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


class MultiTenancyProcessInstantiationTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final BpmnModelInstance PROCESS = Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .userTask("userTask")
      .endEvent()
      .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  public BatchRestartHelper batchHelper = new BatchRestartHelper(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected AuthorizationService authorizationService;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected HistoryService historyService;

  @AfterEach
  void tearDown() {
    authorizationService.createAuthorizationQuery();
    batchHelper.removeAllRunningAndHistoricBatches();
  }

  @Test
  void testStartProcessInstanceByKeyAndTenantId() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionTenantId(TENANT_ONE)
      .execute();

    assertThat(runtimeService.createProcessInstanceQuery().tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void testStartProcessInstanceByKeyForAnyTenant() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .execute();

    assertThat(runtimeService.createProcessInstanceQuery().tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void testStartProcessInstanceByKeyWithoutTenantId() {
   testRule.deploy(PROCESS);
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionWithoutTenantId()
      .execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isOne();
    assertThat(query.singleResult().getTenantId()).isNull();
  }

  @Test
  void testFailToStartProcessInstanceByKeyForOtherTenant() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("testProcess")
        .processDefinitionTenantId(TENANT_TWO);

    try {
      processInstantiationBuilder.execute();

      fail("expected exception");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("no processes deployed");
    }
  }

  @Test
  void testFailToStartProcessInstanceByKeyForMultipleTenants() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("testProcess");

    try {
      processInstantiationBuilder.execute();

      fail("expected exception");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("multiple tenants");
    }
  }

  @Test
  void testFailToStartProcessInstanceByIdAndTenantId() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    var processInstantiationBuilder = runtimeService.createProcessInstanceById(processDefinition.getId())
        .processDefinitionTenantId(TENANT_ONE);

    try {
      processInstantiationBuilder.execute();

      fail("expected exception");
    } catch (BadUserRequestException e) {
      assertThat(e.getMessage()).contains("Cannot specify a tenant-id");
    }
  }

  @Test
  void testFailToStartProcessInstanceByIdWithoutTenantId() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    var processInstantiationBuilder = runtimeService.createProcessInstanceById(processDefinition.getId())
        .processDefinitionWithoutTenantId();

    try {
      processInstantiationBuilder.execute();

      fail("expected exception");
    } catch (BadUserRequestException e) {
      assertThat(e.getMessage()).contains("Cannot specify a tenant-id");
    }
  }

  @Test
  void testStartProcessInstanceAtActivityByKeyAndTenantId() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionTenantId(TENANT_ONE)
      .startBeforeActivity("userTask")
      .execute();

    assertThat(runtimeService.createProcessInstanceQuery().tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void testStartProcessInstanceAtActivityByKeyForAnyTenant() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .startBeforeActivity("userTask")
      .execute();

    assertThat(runtimeService.createProcessInstanceQuery().tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void testStartProcessInstanceAtActivityByKeyWithoutTenantId() {
   testRule.deploy(PROCESS);
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionWithoutTenantId()
      .startBeforeActivity("userTask")
      .execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isOne();
    assertThat(query.singleResult().getTenantId()).isNull();
  }

  @Test
  void testFailToStartProcessInstanceAtActivityByKeyForOtherTenant() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("testProcess")
        .processDefinitionTenantId(TENANT_TWO)
        .startBeforeActivity("userTask");

    try {
      processInstantiationBuilder.execute();

      fail("expected exception");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("no processes deployed");
    }
  }

  @Test
  void testFailToStartProcessInstanceAtActivityByKeyForMultipleTenants() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("testProcess")
        .startBeforeActivity("userTask");

    try {
      processInstantiationBuilder.execute();

      fail("expected exception");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("multiple tenants");
    }
  }

  @Test
  void testFailToStartProcessInstanceAtActivityByIdAndTenantId() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    var processInstantiationBuilder = runtimeService.createProcessInstanceById(processDefinition.getId())
        .processDefinitionTenantId(TENANT_ONE)
        .startBeforeActivity("userTask");

    try {
      processInstantiationBuilder.execute();

      fail("expected exception");
    } catch (BadUserRequestException e) {
      assertThat(e.getMessage()).contains("Cannot specify a tenant-id");
    }
  }

  @Test
  void testFailToStartProcessInstanceAtActivityByIdWithoutTenantId() {
   testRule.deploy(PROCESS);

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    var processInstantiationBuilder = runtimeService.createProcessInstanceById(processDefinition.getId())
        .processDefinitionWithoutTenantId()
        .startBeforeActivity("userTask");

    try {
      processInstantiationBuilder.execute();

      fail("expected exception");
    } catch (BadUserRequestException e) {
      assertThat(e.getMessage()).contains("Cannot specify a tenant-id");
    }
  }

  @Test
  void testStartProcessInstanceByKeyWithoutTenantIdNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

   testRule.deploy(PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionWithoutTenantId()
      .execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isOne();
  }

  @Test
  void testFailToStartProcessInstanceByKeyNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    testRule.deployForTenant(TENANT_ONE, PROCESS);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("testProcess");

    try {
      processInstantiationBuilder.execute();

      fail("expected exception");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("no processes deployed with key 'testProcess'");
    }
  }

  @Test
  void testFailToStartProcessInstanceByKeyWithTenantIdNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    testRule.deployForTenant(TENANT_ONE, PROCESS);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("testProcess")
        .processDefinitionTenantId(TENANT_ONE);

    try {
      processInstantiationBuilder.execute();

      fail("expected exception");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Cannot create an instance of the process definition");
    }
  }

  @Test
  void testFailToStartProcessInstanceByIdNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    ProcessDefinition processDefinition = repositoryService
      .createProcessDefinitionQuery()
      .singleResult();

    identityService.setAuthentication("user", null, null);
    var processInstantiationBuilder = runtimeService.createProcessInstanceById(processDefinition.getId());

    try {
      processInstantiationBuilder.execute();

      fail("expected exception");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Cannot create an instance of the process definition");
    }
  }

  @Test
  void testStartProcessInstanceByKeyWithTenantIdAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionTenantId(TENANT_ONE)
      .execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void testStartProcessInstanceByIdAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .singleResult();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    runtimeService.createProcessInstanceById(processDefinition.getId())
      .execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void testStartProcessInstanceByKeyWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess").execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void testStartProcessInstanceByKeyWithTenantIdDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    testRule.deployForTenant(TENANT_ONE, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionTenantId(TENANT_ONE)
      .execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testRestartProcessInstanceSyncWithTenantId() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance();

    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_ONE));

    // when
    runtimeService.restartProcessInstances(processInstance.getProcessDefinitionId())
      .startBeforeActivity("userTask")
      .processInstanceIds(processInstance.getId())
      .execute();

    // then
    ProcessInstance restartedInstance = runtimeService.createProcessInstanceQuery().active()
        .processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();

    assertThat(restartedInstance).isNotNull();
    assertThat(restartedInstance.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testRestartProcessInstanceAsyncWithTenantId() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance();

    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_ONE));

    // when
    Batch batch = runtimeService.restartProcessInstances(processInstance.getProcessDefinitionId())
      .startBeforeActivity("userTask")
      .processInstanceIds(processInstance.getId())
      .executeAsync();

    batchHelper.completeBatch(batch);

    // then
    ProcessInstance restartedInstance = runtimeService.createProcessInstanceQuery().active()
      .processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();

    assertThat(restartedInstance).isNotNull();
    assertThat(restartedInstance.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testFailToRestartProcessInstanceSyncWithOtherTenantId() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance();

    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_TWO));
    var restartProcessInstanceBuilder = runtimeService.restartProcessInstances(processInstance.getProcessDefinitionId())
        .startBeforeActivity("userTask")
        .processInstanceIds(processInstance.getId());

    try {
      // when
      restartProcessInstanceBuilder.execute();

      fail("expected exception");
    } catch (BadUserRequestException e) {
      // then
      assertThat(e.getMessage()).contains("Historic process instance cannot be found: historicProcessInstanceId is null");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testFailToRestartProcessInstanceAsyncWithOtherTenantId() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance();

    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_TWO));

    try {
      // when
      runtimeService.restartProcessInstances(processInstance.getProcessDefinitionId())
        .startBeforeActivity("userTask")
        .processInstanceIds(processInstance.getId())
        .executeAsync();
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Cannot restart process instances of process definition '%s' because it belongs to no authenticated tenant.".formatted(processInstance.getProcessDefinitionId()));
    }

  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testRestartProcessInstanceSyncWithTenantIdByHistoricProcessInstanceQuery() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance();
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId());

    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_ONE));

    // when
    runtimeService.restartProcessInstances(processInstance.getProcessDefinitionId())
      .startBeforeActivity("userTask")
      .historicProcessInstanceQuery(query)
      .execute();

    // then
    ProcessInstance restartedInstance = runtimeService.createProcessInstanceQuery().active()
      .processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();

    assertThat(restartedInstance).isNotNull();
    assertThat(restartedInstance.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testRestartProcessInstanceAsyncWithTenantIdByHistoricProcessInstanceQuery() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance();
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId());

    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_ONE));

    // when
    Batch batch = runtimeService.restartProcessInstances(processInstance.getProcessDefinitionId())
      .startBeforeActivity("userTask")
      .historicProcessInstanceQuery(query)
      .executeAsync();

    batchHelper.completeBatch(batch);

    // then
    ProcessInstance restartedInstance = runtimeService.createProcessInstanceQuery().active()
      .processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();

    assertThat(restartedInstance).isNotNull();
    assertThat(restartedInstance.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testFailToRestartProcessInstanceSyncWithOtherTenantIdByHistoricProcessInstanceQuery() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance();
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId());

    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_TWO));
    var restartProcessInstanceBuilder = runtimeService.restartProcessInstances(processInstance.getProcessDefinitionId())
        .startBeforeActivity("userTask")
        .historicProcessInstanceQuery(query);

    try {
      // when
      restartProcessInstanceBuilder.execute();

      fail("expected exception");
    } catch (BadUserRequestException e) {
      // then
      assertThat(e.getMessage()).contains("processInstanceIds is empty");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testFailToRestartProcessInstanceAsyncWithOtherTenantIdByHistoricProcessInstanceQuery() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance();
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId());

    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_TWO));

    try {
      // when
      runtimeService.restartProcessInstances(processInstance.getProcessDefinitionId())
        .startBeforeActivity("userTask")
        .historicProcessInstanceQuery(query)
        .executeAsync();
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("processInstanceIds is empty");
    }

  }


  public ProcessInstance startAndDeleteProcessInstance() {
    Deployment deployment = testRule.deployForTenant(TENANT_ONE, PROCESS);
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .deploymentId(deployment.getId())
        .singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    return processInstance;
  }

}
