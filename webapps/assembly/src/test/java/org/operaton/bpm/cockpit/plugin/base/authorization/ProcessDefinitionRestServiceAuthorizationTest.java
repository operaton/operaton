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
package org.operaton.bpm.cockpit.plugin.base.authorization;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessDefinitionStatisticsDto;
import org.operaton.bpm.cockpit.impl.plugin.resources.ProcessDefinitionRestService;
import org.operaton.bpm.engine.rest.dto.CountResultDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;

class ProcessDefinitionRestServiceAuthorizationTest  extends AuthorizationTest {

  private static final String USER_TASK_PROCESS_KEY = "userTaskProcess";
  private static final String CALLING_USER_TASK_PROCESS_KEY = "CallingUserTaskProcess";
  private String deploymentId;
  private ProcessDefinitionRestService resource;
  private UriInfo uriInfo;
  private final MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();

  @Override
  @BeforeEach
  public void setUp() {
    super.setUp();

    resource = new ProcessDefinitionRestService(engineName);

    deploymentId = createDeployment(null, "processes/user-task-process.bpmn", "processes/calling-user-task-process.bpmn").getId();

    startProcessInstances(CALLING_USER_TASK_PROCESS_KEY, 3);

    uriInfo = Mockito.mock(UriInfo.class);
    Mockito.doReturn(queryParameters).when(uriInfo).getQueryParameters();
    queryParameters.add("sortBy", "key");
    queryParameters.add("sortOrder", "asc");
  }

  @Override
  @AfterEach
  public void tearDown() {
    queryParameters.clear();
    deleteDeployment(deploymentId);
    super.tearDown();
  }

  @Test
  void queryStatisticsWithoutAuthorization() {
    // when
    List<ProcessDefinitionStatisticsDto> actual = resource.queryStatistics(uriInfo, null, null);

    // then
    assertThat(actual).isEmpty();
  }

  @Test
  void queryStatisticsWithReadPermissionOnProcessDefinition() {
    // given
    String key = selectProcessDefinitionByKey(CALLING_USER_TASK_PROCESS_KEY).getKey();
    createGrantAuthorization(PROCESS_DEFINITION, key, userId, READ);

    // when
    List<ProcessDefinitionStatisticsDto> actual = resource.queryStatistics(uriInfo, null, null);

    // then
    assertThat(actual).hasSize(1);
    assertThat(actual).extracting("key", "tenantId")
        .containsOnly(tuple(CALLING_USER_TASK_PROCESS_KEY, null));
  }

  @Test
  void queryStatisticsWithReadPermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);

    // when
    List<ProcessDefinitionStatisticsDto> actual = resource.queryStatistics(uriInfo, null, null);

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual).extracting("key", "tenantId")
        .containsOnly(tuple(CALLING_USER_TASK_PROCESS_KEY, null),
                      tuple(USER_TASK_PROCESS_KEY, null));
  }

  @Test
  void queryStatisticsWithMultipleReadPermissions() {
    // given
    String key = selectProcessDefinitionByKey(CALLING_USER_TASK_PROCESS_KEY).getKey();
    createGrantAuthorization(PROCESS_DEFINITION, key, userId, READ);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);

    // when
    List<ProcessDefinitionStatisticsDto> actual = resource.queryStatistics(uriInfo, null, null);

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual).extracting("key", "tenantId")
        .containsOnly(tuple(CALLING_USER_TASK_PROCESS_KEY, null),
                      tuple(USER_TASK_PROCESS_KEY, null));
  }

  @Test
  void getStatisticsCountWithoutAuthorization() {
    // when
    CountResultDto actual = resource.getStatisticsCount(uriInfo);

    // then
    assertThat(actual.getCount()).isZero();
  }

  @Test
  void getStatisticsCountWithReadPermissionOnProcessDefinition() {
    // given
    String key = selectProcessDefinitionByKey(CALLING_USER_TASK_PROCESS_KEY).getKey();
    createGrantAuthorization(PROCESS_DEFINITION, key, userId, READ);

    // when
    CountResultDto actual = resource.getStatisticsCount(uriInfo);

    // then
    assertThat(actual.getCount()).isEqualTo(1);
  }

  @Test
  void getStatisticsCountWithReadPermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);

    // when
    CountResultDto actual = resource.getStatisticsCount(uriInfo);

    // then
    assertThat(actual.getCount()).isEqualTo(2);
  }

  @Test
  void getStatisticsCountWithMultipleReadPermissions() {
    // given
    String key = selectProcessDefinitionByKey(CALLING_USER_TASK_PROCESS_KEY).getKey();
    createGrantAuthorization(PROCESS_DEFINITION, key, userId, READ);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);

    // when
    CountResultDto actual = resource.getStatisticsCount(uriInfo);

    // then
    assertThat(actual.getCount()).isEqualTo(2);
  }

}
