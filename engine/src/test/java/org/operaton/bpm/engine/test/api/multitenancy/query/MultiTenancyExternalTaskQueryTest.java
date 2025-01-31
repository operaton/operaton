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
package org.operaton.bpm.engine.test.api.multitenancy.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.ExternalTaskQuery;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Before;
import org.junit.Test;

public class MultiTenancyExternalTaskQueryTest extends PluggableProcessEngineTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @Before
  public void setUp() {
    BpmnModelInstance process = Bpmn.createExecutableProcess()
      .startEvent()
      .serviceTask()
        .operatonType("external")
        .operatonTopic("test")
      .endEvent()
    .done();

    testRule.deployForTenant(TENANT_ONE, process);
    testRule.deployForTenant(TENANT_TWO, process);

    startProcessInstance(TENANT_ONE);
    startProcessInstance(TENANT_TWO);
  }

  @Test
  public void testQueryWithoutTenantId() {
    ExternalTaskQuery query = externalTaskService
        .createExternalTaskQuery();

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  public void testQueryByTenantId() {
    ExternalTaskQuery query = externalTaskService
        .createExternalTaskQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isEqualTo(1L);

    query = externalTaskService
        .createExternalTaskQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isEqualTo(1L);
  }

  @Test
  public void testQueryByTenantIds() {
    ExternalTaskQuery query = externalTaskService
        .createExternalTaskQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  public void testQueryByNonExistingTenantId() {
    ExternalTaskQuery query = externalTaskService
        .createExternalTaskQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  public void testFailQueryByTenantIdNull() {
    var externalTaskQuery = externalTaskService.createExternalTaskQuery();
    try {
      externalTaskQuery.tenantIdIn((String) null);

      fail("expected exception");
    } catch (NullValueException e) {
    }
  }

  @Test
  public void testQuerySortingAsc() {
    List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery()
        .orderByTenantId()
        .asc()
        .list();

    assertThat(externalTasks).hasSize(2);
    assertThat(externalTasks.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(externalTasks.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  public void testQuerySortingDesc() {
    List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery()
        .orderByTenantId()
        .desc()
        .list();

    assertThat(externalTasks).hasSize(2);
    assertThat(externalTasks.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(externalTasks.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  public void testQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    ExternalTaskQuery query = externalTaskService.createExternalTaskQuery();
    assertThat(query.count()).isZero();
  }

  @Test
  public void testQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE));

    ExternalTaskQuery query = externalTaskService.createExternalTaskQuery();

    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).count()).isEqualTo(1L);
  }

  @Test
  public void testQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE, TENANT_TWO));

    ExternalTaskQuery query = externalTaskService.createExternalTaskQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isEqualTo(1L);
  }

  @Test
  public void testQueryDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    ExternalTaskQuery query = externalTaskService.createExternalTaskQuery();
    assertThat(query.count()).isEqualTo(2L);
  }

  protected void startProcessInstance(String tenant) {
    String processDefinitionId = repositoryService
      .createProcessDefinitionQuery()
      .tenantIdIn(tenant)
      .singleResult()
      .getId();

    runtimeService.startProcessInstanceById(processDefinitionId);
  }

}
