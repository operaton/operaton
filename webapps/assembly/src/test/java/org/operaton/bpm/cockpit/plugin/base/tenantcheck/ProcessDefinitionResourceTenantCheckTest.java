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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessDefinitionDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.query.ProcessDefinitionQueryDto;
import org.operaton.bpm.cockpit.impl.plugin.base.sub.resources.ProcessDefinitionResource;
import org.operaton.bpm.cockpit.plugin.test.AbstractCockpitPluginTest;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.runtime.ProcessInstance;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessDefinitionResourceTenantCheckTest extends AbstractCockpitPluginTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";
  protected static final String ADMIN_GROUP = "adminGroup";
  protected static final String ADMIN_USER = "adminUser";

  private ProcessDefinitionResource resource;
  private ProcessDefinitionQueryDto queryParameter;

  @BeforeEach
  void init() {

    processEngineConfiguration.getAdminGroups().add(ADMIN_GROUP);
    processEngineConfiguration.getAdminUsers().add(ADMIN_USER);

    deploy("processes/multi-tenancy-call-activity.bpmn");
    deployForTenant(TENANT_ONE, "processes/user-task-process.bpmn");
    deployForTenant(TENANT_TWO, "processes/user-task-process.bpmn");


    ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("multiTenancyCallActivity").execute();

    resource = new ProcessDefinitionResource(getProcessEngine().getName(), processInstance.getProcessDefinitionId());

    queryParameter = new ProcessDefinitionQueryDto();
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.getAdminGroups().remove(ADMIN_GROUP);
    processEngineConfiguration.getAdminUsers().remove(ADMIN_USER);
  }

  @Test
  void calledProcessDefinitionByParentProcessDefinitionIdNoAuthenticatedTenant() {

    identityService.setAuthentication("user", null, null);

    List<ProcessDefinitionDto> result = resource.queryCalledProcessDefinitions(queryParameter);
    assertThat(result).isEmpty();
  }

  @Test
  void calledProcessDefinitionByParentProcessDefinitionIdWithAuthenticatedTenant() {

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    List<ProcessDefinitionDto> result = resource.queryCalledProcessDefinitions(queryParameter);
    assertThat(result).isNotEmpty().hasSize(1);

    assertThat(getCalledFromActivityIds(result)).containsOnly("CallActivity_Tenant1");
  }

  @Test
  void calledProcessDefinitionByParentProcessDefinitionIdDisabledTenantCheck() {

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    List<ProcessDefinitionDto> result = resource.queryCalledProcessDefinitions(queryParameter);
    assertThat(result).isNotEmpty().hasSize(2);

    assertThat(getCalledFromActivityIds(result)).contains("CallActivity_Tenant1", "CallActivity_Tenant2");
  }

  @Test
  void calledProcessDefinitionByParentProcessDefinitionIdWithOperatonAdmin() {

    identityService.setAuthentication("user", Collections.singletonList(Groups.OPERATON_ADMIN), null);

    List<ProcessDefinitionDto> result = resource.queryCalledProcessDefinitions(queryParameter);
    assertThat(result).isNotEmpty().hasSize(2);

    assertThat(getCalledFromActivityIds(result)).contains("CallActivity_Tenant1", "CallActivity_Tenant2");
  }

  @Test
  void calledProcessDefinitionByParentProcessDefinitionIdWithAdminGroups() {

    identityService.setAuthentication("user", Collections.singletonList(ADMIN_GROUP), null);

    List<ProcessDefinitionDto> result = resource.queryCalledProcessDefinitions(queryParameter);
    assertThat(result).isNotEmpty().hasSize(2);

    assertThat(getCalledFromActivityIds(result)).contains("CallActivity_Tenant1", "CallActivity_Tenant2");
  }

  @Test
  void calledProcessDefinitionByParentProcessDefinitionIdWithAdminUsers() {

    identityService.setAuthentication("adminUser", null, null);

    List<ProcessDefinitionDto> result = resource.queryCalledProcessDefinitions(queryParameter);
    assertThat(result).isNotEmpty().hasSize(2);

    assertThat(getCalledFromActivityIds(result)).contains("CallActivity_Tenant1", "CallActivity_Tenant2");
  }

  protected List<String> getCalledFromActivityIds(List<ProcessDefinitionDto> processDefinitions) {
    List<String> activityIds = new ArrayList<>();

    for (ProcessDefinitionDto processDefinition : processDefinitions) {
      activityIds.addAll(processDefinition.getCalledFromActivityIds());
    }

    return activityIds;
  }


}
