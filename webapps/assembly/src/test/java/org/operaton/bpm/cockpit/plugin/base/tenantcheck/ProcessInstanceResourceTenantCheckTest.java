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
package org.operaton.bpm.cockpit.plugin.base.tenantcheck;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.cockpit.impl.plugin.base.dto.CalledProcessInstanceDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.query.CalledProcessInstanceQueryDto;
import org.operaton.bpm.cockpit.impl.plugin.base.sub.resources.ProcessInstanceResource;
import org.operaton.bpm.cockpit.plugin.test.AbstractCockpitPluginTest;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.runtime.ProcessInstance;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessInstanceResourceTenantCheckTest extends AbstractCockpitPluginTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";
  protected static final String ADMIN_GROUP = "adminGroup";
  protected static final String ADMIN_USER = "adminUser";

  private ProcessInstanceResource resource;
  private CalledProcessInstanceQueryDto queryParameter;

  @BeforeEach
  void init() {
    processEngineConfiguration.getAdminGroups().add(ADMIN_GROUP);
    processEngineConfiguration.getAdminUsers().add(ADMIN_USER);

    deploy("processes/multi-tenancy-call-activity.bpmn");
    deployForTenant(TENANT_ONE, "processes/user-task-process.bpmn");
    deployForTenant(TENANT_TWO, "processes/user-task-process.bpmn");

    ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("multiTenancyCallActivity").execute();

    resource = new ProcessInstanceResource(getProcessEngine().getName(), processInstance.getId());

    queryParameter = new CalledProcessInstanceQueryDto();
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.getAdminGroups().remove(ADMIN_GROUP);
    processEngineConfiguration.getAdminUsers().remove(ADMIN_USER);
  }

  @Test
  void getCalledProcessInstancesByParentProcessInstanceIdNoAuthenticatedTenants() {

    identityService.setAuthentication("user", null, null);

    List<CalledProcessInstanceDto> result = resource.queryCalledProcessInstances(queryParameter);
    assertThat(result).isEmpty();
  }

  @Test
  void getCalledProcessInstancesByParentProcessInstanceIdWithAuthenticatedTenant() {

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    List<CalledProcessInstanceDto> result = resource.queryCalledProcessInstances(queryParameter);
    assertThat(result).isNotEmpty().hasSize(1);

    CalledProcessInstanceDto dto = result.get(0);
    assertThat(dto.getCallActivityId()).isEqualTo("CallActivity_Tenant1");
  }

  @Test
  void getCalledProcessInstancesByParentProcessInstanceIdDisabledTenantCheck() {

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    List<CalledProcessInstanceDto> result = resource.queryCalledProcessInstances(queryParameter);
    assertThat(result).isNotEmpty().hasSize(2);

    assertThat(getCalledActivityIds(result)).contains("CallActivity_Tenant1", "CallActivity_Tenant2");
  }

  @Test
  void getCalledProcessInstancesByParentProcessInstanceIdWithOperatonAdmin() {

    identityService.setAuthentication("user", Collections.singletonList(Groups.OPERATON_ADMIN), null);

    List<CalledProcessInstanceDto> result = resource.queryCalledProcessInstances(queryParameter);
    assertThat(result).isNotEmpty().hasSize(2);

    assertThat(getCalledActivityIds(result)).contains("CallActivity_Tenant1", "CallActivity_Tenant2");
  }

  @Test
  void getCalledProcessInstancesByParentProcessInstanceIdWithAdminGroups() {

    identityService.setAuthentication("user", Collections.singletonList(ADMIN_GROUP), null);

    List<CalledProcessInstanceDto> result = resource.queryCalledProcessInstances(queryParameter);
    assertThat(result).isNotEmpty().hasSize(2);

    assertThat(getCalledActivityIds(result)).contains("CallActivity_Tenant1", "CallActivity_Tenant2");
  }

  @Test
  void getCalledProcessInstancesByParentProcessInstanceIdWithAdminUsers() {

    identityService.setAuthentication("adminUser", null, null);

    List<CalledProcessInstanceDto> result = resource.queryCalledProcessInstances(queryParameter);
    assertThat(result).isNotEmpty().hasSize(2);

    assertThat(getCalledActivityIds(result)).contains("CallActivity_Tenant1", "CallActivity_Tenant2");
  }

  private Set<String> getCalledActivityIds(List<CalledProcessInstanceDto> result) {
    Set<String> callActivityIds = new HashSet<>();
    for (CalledProcessInstanceDto calledProcessInstanceDto : result) {
       callActivityIds.add(calledProcessInstanceDto.getCallActivityId());
    }
    return callActivityIds;
  }

}
