/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessInstanceDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.query.ProcessInstanceQueryDto;
import org.operaton.bpm.cockpit.impl.plugin.resources.ProcessInstanceRestService;
import org.operaton.bpm.cockpit.plugin.resource.AbstractCockpitPluginResource;
import org.operaton.bpm.engine.authorization.Permission;

import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Permissions.TASK_WORK;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Cockpit plugin queries honor the {@code disabledPermissions}
 * of the process engine configuration exactly like the engine does: a permission
 * check is skipped only for permissions that are listed, and never because some
 * other permission is listed.
 */
class DisabledPermissionsAuthorizationTest extends AuthorizationTest {

  protected static final String USER_TASK_PROCESS_KEY = "userTaskProcess";

  protected String deploymentId;
  protected ProcessInstanceRestService resource;

  @Override
  @BeforeEach
  public void setUp() {
    super.setUp();
    deploymentId = createDeployment("processes/user-task-process.bpmn").getId();
    startProcessInstances(USER_TASK_PROCESS_KEY, 3);
    resource = new ProcessInstanceRestService(engineName);
  }

  @Override
  @AfterEach
  public void tearDown() {
    processEngineConfiguration.setDisabledPermissions(Collections.emptyList());
    deleteDeployment(deploymentId);
    super.tearDown();
  }

  @Test
  void unrelatedDisabledPermissionDoesNotDisableChecks() {
    // given a disabled permission that is not checked by the query
    processEngineConfiguration.setDisabledPermissions(List.of(TASK_WORK.name()));

    // when the user without any grants queries the process instances
    List<ProcessInstanceDto> instances = resource.queryProcessInstances(new ProcessInstanceQueryDto(), null, null);

    // then the READ and READ_INSTANCE checks are still applied
    assertThat(instances).isEmpty();
  }

  @Test
  void disabledReadPermissionSkipsOnlyReadCheck() {
    // given READ is disabled and the user holds READ on one instance only
    processEngineConfiguration.setDisabledPermissions(List.of(READ.name()));
    String processInstanceId = selectAnyProcessInstanceByKey(USER_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    List<ProcessInstanceDto> instances = resource.queryProcessInstances(new ProcessInstanceQueryDto(), null, null);

    // then the READ check is skipped, the READ_INSTANCE check on the definition still applies
    assertThat(instances).isEmpty();

    // and with READ_INSTANCE on the definition all instances are visible
    createGrantAuthorization(PROCESS_DEFINITION, USER_TASK_PROCESS_KEY, userId, READ_INSTANCE);
    instances = resource.queryProcessInstances(new ProcessInstanceQueryDto(), null, null);
    assertThat(instances).hasSize(3);
  }

  @Test
  void disabledReadInstancePermissionSkipsOnlyReadInstanceCheck() {
    // given READ_INSTANCE is disabled and the user holds READ on one instance only
    processEngineConfiguration.setDisabledPermissions(List.of(READ_INSTANCE.name()));
    String processInstanceId = selectAnyProcessInstanceByKey(USER_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    List<ProcessInstanceDto> instances = resource.queryProcessInstances(new ProcessInstanceQueryDto(), null, null);

    // then the READ check on the instance still applies
    assertThat(instances).hasSize(1);
    assertThat(instances.get(0).getId()).isEqualTo(processInstanceId);
  }

  @Test
  void disablingAllCheckedPermissionsLiftsTheRestriction() {
    // given both permissions checked by the query are disabled
    processEngineConfiguration.setDisabledPermissions(List.of(READ.name(), READ_INSTANCE.name()));

    // when the user without any grants queries the process instances
    List<ProcessInstanceDto> instances = resource.queryProcessInstances(new ProcessInstanceQueryDto(), null, null);

    // then no permission check remains and all instances are visible, as in the engine
    assertThat(instances).hasSize(3);
  }

  @Test
  void nullDisabledPermissionsAppliesAllChecks() {
    // given
    processEngineConfiguration.setDisabledPermissions(null);

    // when
    List<ProcessInstanceDto> instances = resource.queryProcessInstances(new ProcessInstanceQueryDto(), null, null);

    // then
    assertThat(instances).isEmpty();
  }

  @Test
  void isPermissionDisabledMatchesByName() {
    // given
    var pluginResource = new AbstractCockpitPluginResource(engineName) {
      boolean check(Permission permission) {
        return isPermissionDisabled(permission);
      }
    };

    // then with an empty list nothing is disabled
    assertThat(pluginResource.check(READ)).isFalse();
    assertThat(pluginResource.check(READ_INSTANCE)).isFalse();

    // and with a null list nothing is disabled
    processEngineConfiguration.setDisabledPermissions(null);
    assertThat(pluginResource.check(READ)).isFalse();

    // and only the listed permission is disabled
    processEngineConfiguration.setDisabledPermissions(List.of(READ.name(), TASK_WORK.name()));
    assertThat(pluginResource.check(READ)).isTrue();
    assertThat(pluginResource.check(TASK_WORK)).isTrue();
    assertThat(pluginResource.check(READ_INSTANCE)).isFalse();
  }

}
