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
package org.operaton.bpm.engine.test.api.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.BatchRestartHelper;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.After;
import org.junit.Test;


public class MultiTenancyProcessInstantiationTest extends PluggableProcessEngineTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final BpmnModelInstance PROCESS = Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .userTask("userTask")
      .endEvent()
      .done();

  public BatchRestartHelper batchHelper = new BatchRestartHelper(this);

  @After
  public void tearDown() {

    authorizationService.createAuthorizationQuery();
    batchHelper.removeAllRunningAndHistoricBatches();
  }

  @Test
  public void testStartProcessInstanceByKeyAndTenantId() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionTenantId(TENANT_ONE)
      .execute();

    assertThat(runtimeService.createProcessInstanceQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  public void testStartProcessInstanceByKeyForAnyTenant() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .execute();

    assertThat(runtimeService.createProcessInstanceQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  public void testStartProcessInstanceByKeyWithoutTenantId() {
   testRule.deploy(PROCESS);
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionWithoutTenantId()
      .execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.singleResult().getTenantId()).isNull();
  }

  @Test
  public void testFailToStartProcessInstanceByKeyForOtherTenant() {
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
  public void testFailToStartProcessInstanceByKeyForMultipleTenants() {
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
  public void testFailToStartProcessInstanceByIdAndTenantId() {
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
  public void testFailToStartProcessInstanceByIdWithoutTenantId() {
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
  public void testStartProcessInstanceAtActivityByKeyAndTenantId() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionTenantId(TENANT_ONE)
      .startBeforeActivity("userTask")
      .execute();

    assertThat(runtimeService.createProcessInstanceQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  public void testStartProcessInstanceAtActivityByKeyForAnyTenant() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .startBeforeActivity("userTask")
      .execute();

    assertThat(runtimeService.createProcessInstanceQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  public void testStartProcessInstanceAtActivityByKeyWithoutTenantId() {
   testRule.deploy(PROCESS);
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionWithoutTenantId()
      .startBeforeActivity("userTask")
      .execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.singleResult().getTenantId()).isNull();
  }

  @Test
  public void testFailToStartProcessInstanceAtActivityByKeyForOtherTenant() {
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
  public void testFailToStartProcessInstanceAtActivityByKeyForMultipleTenants() {
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
  public void testFailToStartProcessInstanceAtActivityByIdAndTenantId() {
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
  public void testFailToStartProcessInstanceAtActivityByIdWithoutTenantId() {
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
  public void testStartProcessInstanceByKeyWithoutTenantIdNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

   testRule.deploy(PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionWithoutTenantId()
      .execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);
  }

  @Test
  public void testFailToStartProcessInstanceByKeyNoAuthenticatedTenants() {
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
  public void testFailToStartProcessInstanceByKeyWithTenantIdNoAuthenticatedTenants() {
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
  public void testFailToStartProcessInstanceByIdNoAuthenticatedTenants() {
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
  public void testStartProcessInstanceByKeyWithTenantIdAuthenticatedTenant() {
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE));

    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionTenantId(TENANT_ONE)
      .execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  public void testStartProcessInstanceByIdAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .singleResult();

    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE));

    runtimeService.createProcessInstanceById(processDefinition.getId())
      .execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  public void testStartProcessInstanceByKeyWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE));

    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess").execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  public void testStartProcessInstanceByKeyWithTenantIdDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    testRule.deployForTenant(TENANT_ONE, PROCESS);

    runtimeService.createProcessInstanceByKey("testProcess")
      .processDefinitionTenantId(TENANT_ONE)
      .execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  public void testRestartProcessInstanceSyncWithTenantId() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance(TENANT_ONE, PROCESS);

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
  public void testRestartProcessInstanceAsyncWithTenantId() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance(TENANT_ONE, PROCESS);

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
  public void testFailToRestartProcessInstanceSyncWithOtherTenantId() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance(TENANT_ONE, PROCESS);

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
  public void testFailToRestartProcessInstanceAsyncWithOtherTenantId() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance(TENANT_ONE, PROCESS);

    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_TWO));

    try {
      // when
      runtimeService.restartProcessInstances(processInstance.getProcessDefinitionId())
        .startBeforeActivity("userTask")
        .processInstanceIds(processInstance.getId())
        .executeAsync();
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Cannot restart process instances of process definition '" + processInstance.getProcessDefinitionId() + "' because it belongs to no authenticated tenant.");
    }

  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  public void testRestartProcessInstanceSyncWithTenantIdByHistoricProcessInstanceQuery() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance(TENANT_ONE, PROCESS);
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
  public void testRestartProcessInstanceAsyncWithTenantIdByHistoricProcessInstanceQuery() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance(TENANT_ONE, PROCESS);
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
  public void testFailToRestartProcessInstanceSyncWithOtherTenantIdByHistoricProcessInstanceQuery() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance(TENANT_ONE, PROCESS);
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
  public void testFailToRestartProcessInstanceAsyncWithOtherTenantIdByHistoricProcessInstanceQuery() {
    // given
    ProcessInstance processInstance = startAndDeleteProcessInstance(TENANT_ONE, PROCESS);
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


  public ProcessInstance startAndDeleteProcessInstance(String tenantId, BpmnModelInstance modelInstance) {
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
