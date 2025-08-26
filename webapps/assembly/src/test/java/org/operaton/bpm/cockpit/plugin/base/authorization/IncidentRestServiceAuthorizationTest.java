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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.cockpit.impl.plugin.base.dto.IncidentDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.query.IncidentQueryDto;
import org.operaton.bpm.cockpit.impl.plugin.resources.IncidentRestService;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
class IncidentRestServiceAuthorizationTest extends AuthorizationTest {

  protected static final String FAILING_PROCESS_KEY = "FailingProcess";

  protected String deploymentId;

  protected IncidentRestService resource;

  @Override
  @BeforeEach
  public void setUp() {
    super.setUp();

    deploymentId = createDeployment(null, "processes/failing-process.bpmn").getId();

    resource = new IncidentRestService(engineName);

    startProcessInstances(FAILING_PROCESS_KEY, 3);
    disableAuthorization();
    executeAvailableJobs();
    enableAuthorization();
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
    IncidentQueryDto queryParameter = new IncidentQueryDto();

    // when
    List<IncidentDto> incidents = resource.queryIncidents(queryParameter, null, null);

    // then
    assertThat(incidents).isEmpty();
  }

  @Test
  void queryWithReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = selectAnyProcessInstanceByKey(FAILING_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    IncidentQueryDto queryParameter = new IncidentQueryDto();

    // when
    List<IncidentDto> incidents = resource.queryIncidents(queryParameter, null, null);

    // then
    assertThat(incidents).isNotEmpty().hasSize(1);
    assertThat(incidents.get(0).getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void queryWithReadPermissionOnAnyProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    IncidentQueryDto queryParameter = new IncidentQueryDto();

    // when
    List<IncidentDto> incidents = resource.queryIncidents(queryParameter, null, null);

    // then
    assertThat(incidents).isNotEmpty().hasSize(3);
  }

  @Test
  void queryWithMultipleReadPermissions() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);
    String processInstanceId = selectAnyProcessInstanceByKey(FAILING_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    IncidentQueryDto queryParameter = new IncidentQueryDto();

    // when
    List<IncidentDto> incidents = resource.queryIncidents(queryParameter, null, null);

    // then
    assertThat(incidents).isNotEmpty().hasSize(3);
  }

  @Test
  void queryWithReadPermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, FAILING_PROCESS_KEY, userId, READ_INSTANCE);

    IncidentQueryDto queryParameter = new IncidentQueryDto();

    // when
    List<IncidentDto> incidents = resource.queryIncidents(queryParameter, null, null);

    // then
    assertThat(incidents).isNotEmpty().hasSize(3);
  }

}
