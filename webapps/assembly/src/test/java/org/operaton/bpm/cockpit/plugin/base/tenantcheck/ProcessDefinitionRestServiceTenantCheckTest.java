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

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessDefinitionStatisticsDto;
import org.operaton.bpm.cockpit.impl.plugin.resources.ProcessDefinitionRestService;
import org.operaton.bpm.cockpit.plugin.test.AbstractCockpitPluginTest;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.rest.dto.CountResultDto;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ProcessDefinitionRestServiceTenantCheckTest extends AbstractCockpitPluginTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";
  protected static final String ADMIN_GROUP = "adminGroup";
  protected static final String ADMIN_USER = "adminUser";

  private ProcessDefinitionRestService resource;
  private UriInfo uriInfo;
  private final MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();

  @BeforeEach
  void init() {
    processEngineConfiguration.getAdminGroups().add(ADMIN_GROUP);
    processEngineConfiguration.getAdminUsers().add(ADMIN_USER);

    deploy("processes/multi-tenancy-call-activity.bpmn");
    deployForTenant(TENANT_ONE, "processes/user-task-process.bpmn");
    deployForTenant(TENANT_TWO, "processes/user-task-process.bpmn");

    resource = new ProcessDefinitionRestService(getProcessEngine().getName());

    uriInfo = Mockito.mock(UriInfo.class);
    Mockito.doReturn(queryParameters).when(uriInfo).getQueryParameters();
    queryParameters.add("sortBy", "tenantId");
    queryParameters.add("sortOrder", "asc");
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.getAdminGroups().remove(ADMIN_GROUP);
    processEngineConfiguration.getAdminUsers().remove(ADMIN_USER);
    queryParameters.clear();
  }

  @Test
  void queryStatisticsNoAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, null);

    // when
    var actual = resource.queryStatistics(uriInfo, null, null);

    // then
    assertThat(actual).hasSize(1);
    assertThat(actual).extracting("key", "tenantId")
        .containsOnly(tuple("multiTenancyCallActivity", null));
  }

  @Test
  void queryStatisticsWithAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_ONE));

    // when
    List<ProcessDefinitionStatisticsDto> actual = resource.queryStatistics(uriInfo, null, null);

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual).extracting("key", "tenantId")
        .containsOnly(tuple("multiTenancyCallActivity", null),
                      tuple("userTaskProcess", TENANT_ONE));
  }

  @Test
  void queryStatisticsWithDisabledTenantCheck() {
    // given
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    // when
    List<ProcessDefinitionStatisticsDto> actual = resource.queryStatistics(uriInfo, null, null);

    // then
    assertThat(actual).hasSize(3);
    assertThat(actual).extracting("key", "tenantId")
        .containsOnly(tuple("multiTenancyCallActivity", null),
                      tuple("userTaskProcess", TENANT_ONE),
                      tuple("userTaskProcess", TENANT_TWO));
  }

  @Test
  void queryStatisticsWithOperatonAdmin() {
    // given
    identityService.setAuthentication("user", Collections.singletonList(Groups.OPERATON_ADMIN), null);

    // when
    List<ProcessDefinitionStatisticsDto> actual = resource.queryStatistics(uriInfo, null, null);

    // then
    assertThat(actual).hasSize(3);
    assertThat(actual).extracting("key", "tenantId")
        .containsOnly(tuple("multiTenancyCallActivity", null),
                      tuple("userTaskProcess", TENANT_ONE),
                      tuple("userTaskProcess", TENANT_TWO));
  }

  @Test
  void queryStatisticsWithAdminGroups() {
    // given
    identityService.setAuthentication("user", Collections.singletonList(ADMIN_GROUP), null);

    // when
    List<ProcessDefinitionStatisticsDto> actual = resource.queryStatistics(uriInfo, null, null);

    // then
    assertThat(actual).hasSize(3);
    assertThat(actual).extracting("key", "tenantId")
        .containsOnly(tuple("multiTenancyCallActivity", null),
                      tuple("userTaskProcess", TENANT_ONE),
                      tuple("userTaskProcess", TENANT_TWO));
  }

  @Test
  void queryStatisticsWithAdminUsers() {
    // given
    identityService.setAuthentication("adminUser", null, null);

    // when
    List<ProcessDefinitionStatisticsDto> actual = resource.queryStatistics(uriInfo, null, null);

    // then
    assertThat(actual).hasSize(3);
    assertThat(actual).extracting("key", "tenantId")
        .containsOnly(tuple("multiTenancyCallActivity", null),
                      tuple("userTaskProcess", TENANT_ONE),
                      tuple("userTaskProcess", TENANT_TWO));
  }

  @Test
  void getStatisticsCountNoAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, null);

    // when
    CountResultDto actual = resource.getStatisticsCount(uriInfo);

    // then
    assertThat(actual.getCount()).isEqualTo(1);
  }

  @Test
  void getStatisticsCountWithAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, Collections.singletonList(TENANT_ONE));

    // when
    CountResultDto actual = resource.getStatisticsCount(uriInfo);
    List<ProcessDefinitionStatisticsDto> actualDtos = resource.queryStatistics(uriInfo, null, null);

    // then
    assertThat(actual.getCount()).isEqualTo(2);
  }

  @Test
  void getStatisticsCountWithDisabledTenantCheck() {
    // given
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    // when
    CountResultDto actual = resource.getStatisticsCount(uriInfo);

    // then
    assertThat(actual.getCount()).isEqualTo(3);
  }

  @Test
  void getStatisticsCountWithOperatonAdmin() {
    // given
    identityService.setAuthentication("user", Collections.singletonList(Groups.OPERATON_ADMIN), null);

    // when
    CountResultDto actual = resource.getStatisticsCount(uriInfo);

    // then
    assertThat(actual.getCount()).isEqualTo(3);
  }

  @Test
  void getStatisticsCountWithAdminGroups() {
    // given
    identityService.setAuthentication("user", Collections.singletonList(ADMIN_GROUP), null);

    // when
    CountResultDto actual = resource.getStatisticsCount(uriInfo);

    // then
    assertThat(actual.getCount()).isEqualTo(3);
  }

  @Test
  void getStatisticsCountWithAdminUsers() {
    // given
    identityService.setAuthentication("adminUser", null, null);

    // when
    CountResultDto actual = resource.getStatisticsCount(uriInfo);

    // then
    assertThat(actual.getCount()).isEqualTo(3);
  }

}
