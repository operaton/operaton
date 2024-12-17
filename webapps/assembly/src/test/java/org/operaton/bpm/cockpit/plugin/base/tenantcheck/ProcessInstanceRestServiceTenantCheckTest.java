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
package org.operaton.bpm.cockpit.plugin.base.tenantcheck;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.operaton.bpm.cockpit.impl.plugin.base.dto.IncidentStatisticsDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessInstanceDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.query.ProcessInstanceQueryDto;
import org.operaton.bpm.cockpit.impl.plugin.resources.ProcessInstanceRestService;
import org.operaton.bpm.cockpit.plugin.test.AbstractCockpitPluginTest;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProcessInstanceRestServiceTenantCheckTest extends AbstractCockpitPluginTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";
  protected static final String ADMIN_GROUP = "adminGroup";
  protected static final String ADMIN_USER = "adminUser";

  private ProcessEngine processEngine;
  private ProcessEngineConfigurationImpl processEngineConfiguration;
  private RuntimeService runtimeService;
  private IdentityService identityService;

  private ProcessInstanceRestService resource;
  private ProcessInstanceQueryDto queryParameter;

  @Before
  public void init() {

    processEngine = getProcessEngine();
    processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();

    runtimeService = processEngine.getRuntimeService();
    identityService = processEngine.getIdentityService();

    processEngineConfiguration.getAdminGroups().add(ADMIN_GROUP);
    processEngineConfiguration.getAdminUsers().add(ADMIN_USER);

    resource = new ProcessInstanceRestService(processEngine.getName());

    deployForTenant(TENANT_ONE, "processes/failing-process.bpmn");
    deployForTenant(TENANT_TWO, "processes/failing-process.bpmn");

    startProcessInstancesWithTenantId("FailingProcess", TENANT_ONE);
    startProcessInstancesWithTenantId("FailingProcess", TENANT_TWO);

    queryParameter = new ProcessInstanceQueryDto();
    queryParameter.setActivityIdIn(new String[] { "ServiceTask_1" });
  }

  @After
  public void tearDown() {
    processEngineConfiguration.getAdminGroups().remove(ADMIN_GROUP);
    processEngineConfiguration.getAdminUsers().remove(ADMIN_USER);
  }

  @Test
  public void queryCountNoAuthenticatedTenants() {

    identityService.setAuthentication("user", null, null);

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);
    assertThat(result).isNotNull();
    assertThat(result.getCount()).isZero();
  }

  @Test
  public void queryCountWithAuthenticatedTenant() {

    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE));

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);
    assertThat(result).isNotNull();
    assertThat(result.getCount()).isEqualTo(1);
  }

  @Test
  public void queryCountDisabledTenantCheck() {

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);
    assertThat(result).isNotNull();
    assertThat(result.getCount()).isEqualTo(2);
  }

  @Test
  public void queryCountWithOperatonAdmin() {

    identityService.setAuthentication("user", Collections.singletonList(Groups.OPERATON_ADMIN), null);

    CountResultDto result = resource.queryProcessInstancesCount(queryParameter);
    assertThat(result).isNotNull();
    assertThat(result.getCount()).isEqualTo(2);
  }

  @Test
  public void queryWithContainingIncidentsNoAuthenticatedTenants() {

    identityService.setAuthentication("user", null, null);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isEmpty();
  }

  @Test
  public void queryWithContainingIncidentsWithAuthenticatedTenant() {

    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE));

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty();
    assertThat(result).hasSize(1);

    List<IncidentStatisticsDto> incidents = result.get(0).getIncidents();
    assertThat(incidents).isNotEmpty();
    assertThat(incidents).hasSize(1);
  }

  @Test
  public void queryWithContainingIncidentsDisabledTenantCheck() {

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty();
    assertThat(result).hasSize(2);

    List<IncidentStatisticsDto> incidents = result.get(0).getIncidents();
    assertThat(incidents).isNotEmpty();
    assertThat(incidents).hasSize(1);

    incidents = result.get(1).getIncidents();
    assertThat(incidents).isNotEmpty();
    assertThat(incidents).hasSize(1);
  }

  @Test
  public void queryWithContainingIncidentsWithOperatonAdmin() {

    identityService.setAuthentication("user", Collections.singletonList(Groups.OPERATON_ADMIN), null);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty();
    assertThat(result).hasSize(2);

    List<IncidentStatisticsDto> incidents = result.get(0).getIncidents();
    assertThat(incidents).isNotEmpty();
    assertThat(incidents).hasSize(1);

    incidents = result.get(1).getIncidents();
    assertThat(incidents).isNotEmpty();
    assertThat(incidents).hasSize(1);
  }

  @Test
  public void queryWithContainingIncidentsWithAdminGroups() {

    identityService.setAuthentication("user", Collections.singletonList(ADMIN_GROUP), null);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty();
    assertThat(result).hasSize(2);

    List<IncidentStatisticsDto> incidents = result.get(0).getIncidents();
    assertThat(incidents).isNotEmpty();
    assertThat(incidents).hasSize(1);

    incidents = result.get(1).getIncidents();
    assertThat(incidents).isNotEmpty();
    assertThat(incidents).hasSize(1);
  }

  @Test
  public void queryWithContainingIncidentsWithAdminUsers() {

    identityService.setAuthentication("adminUser", null, null);

    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, null, null);
    assertThat(result).isNotEmpty();
    assertThat(result).hasSize(2);

    List<IncidentStatisticsDto> incidents = result.get(0).getIncidents();
    assertThat(incidents).isNotEmpty();
    assertThat(incidents).hasSize(1);

    incidents = result.get(1).getIncidents();
    assertThat(incidents).isNotEmpty();
    assertThat(incidents).hasSize(1);
  }

  private void startProcessInstancesWithTenantId(String processDefinitionKey, String tenantId) {

    runtimeService
      .createProcessInstanceByKey(processDefinitionKey)
      .processDefinitionTenantId(tenantId)
      .execute();

    executeAvailableJobs();
  }

}
