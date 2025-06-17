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

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessInstanceDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.query.ProcessInstanceQueryDto;
import org.operaton.bpm.cockpit.impl.plugin.resources.ProcessInstanceRestService;

/**
 * @author Roman Smirnov
 *
 */
class ProcessInstanceRestServiceAuthorizationTest extends AuthorizationTest {

  protected static final String USER_TASK_PROCESS_KEY = "userTaskProcess";

  protected String deploymentId;

  protected ProcessInstanceRestService resource;

  @Override
  @BeforeEach
  public void setUp() {
    super.setUp();

    deploymentId = createDeployment(null, "processes/user-task-process.bpmn").getId();

    startProcessInstances(USER_TASK_PROCESS_KEY, 3);

    resource = new ProcessInstanceRestService(engineName);
  }

  @Override
  @AfterEach
  public void tearDown() {
    deleteDeployment(deploymentId);
    super.tearDown();
  }

  @Test
  void queryWithoutAuthorization() {
    // given
    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    // when
    List<ProcessInstanceDto> instances = resource.queryProcessInstances(queryParameter, null, null);

    // then
    assertThat(instances).isEmpty();
  }

  @Test
  void queryWithReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = selectAnyProcessInstanceByKey(USER_TASK_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    // when
    List<ProcessInstanceDto> instances = resource.queryProcessInstances(queryParameter, null, null);

    // then
    assertThat(instances).isNotEmpty().hasSize(1);
    assertThat(instances.get(0).getId()).isEqualTo(processInstanceId);
  }

  @Test
  void queryWithReadPermissionOnAnyProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    // when
    List<ProcessInstanceDto> instances = resource.queryProcessInstances(queryParameter, null, null);

    // then
    assertThat(instances).isNotEmpty().hasSize(3);
  }

  @Test
  void queryWithReadInstancePermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, USER_TASK_PROCESS_KEY, userId, READ_INSTANCE);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    // when
    List<ProcessInstanceDto> instances = resource.queryProcessInstances(queryParameter, null, null);

    // then
    assertThat(instances).isNotEmpty().hasSize(3);
  }

  @Test
  void queryPaginationWithOverlappingPermissions() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, USER_TASK_PROCESS_KEY, userId, READ_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    ProcessInstanceQueryDto queryParameter = new ProcessInstanceQueryDto();

    // when
    List<ProcessInstanceDto> result = resource.queryProcessInstances(queryParameter, 0, 3);

    // then
    assertThat(result).isNotEmpty().hasSize(3);

    result = resource.queryProcessInstances(queryParameter, 0, 2);
    assertThat(result).isNotEmpty().hasSize(2);

    result = resource.queryProcessInstances(queryParameter, 2, 2);
    assertThat(result).isNotEmpty().hasSize(1);
  }

}
