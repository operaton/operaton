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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessDefinitionDto;
import org.operaton.bpm.cockpit.impl.plugin.base.dto.query.ProcessDefinitionQueryDto;
import org.operaton.bpm.cockpit.impl.plugin.base.sub.resources.ProcessDefinitionResource;
import org.operaton.bpm.engine.test.Deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;

/**
 * @author Roman Smirnov
 *
 */
class ProcessDefinitionResourceAuthorizationTest extends AuthorizationTest {

  protected static final String USER_TASK_PROCESS_KEY = "userTaskProcess";
  protected static final String CALLING_USER_TASK_PROCESS_KEY = "CallingUserTaskProcess";

  protected String deploymentId;

  protected ProcessDefinitionResource resource;

  @Override
  @BeforeEach
  public void setUp() {
    super.setUp();

    deploymentId = createDeployment(null, "processes/user-task-process.bpmn", "processes/calling-user-task-process.bpmn").getId();

    startProcessInstances(CALLING_USER_TASK_PROCESS_KEY, 3);
  }

  @Override
  @AfterEach
  public void tearDown() {
    deleteDeployment(deploymentId);
    super.tearDown();
  }

  @Test
  void calledProcessDefinitionQueryWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(CALLING_USER_TASK_PROCESS_KEY).getId();
    resource = new ProcessDefinitionResource(engineName, processDefinitionId);

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();

    // when
    List<ProcessDefinitionDto> calledDefinitions = resource.queryCalledProcessDefinitions(queryParameter);

    // then
    assertThat(calledDefinitions).isEmpty();
  }

  @Test
  void calledProcessDefinitionQueryWithReadPermissionOnProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(CALLING_USER_TASK_PROCESS_KEY).getId();
    resource = new ProcessDefinitionResource(engineName, processDefinitionId);

    String processInstanceId = selectAnyProcessInstanceByKey(CALLING_USER_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();

    // when
    List<ProcessDefinitionDto> calledDefinitions = resource.queryCalledProcessDefinitions(queryParameter);

    // then
    assertThat(calledDefinitions).isNotEmpty();

    ProcessDefinitionDto calledProcessDefinition = calledDefinitions.get(0);
    assertThat(calledProcessDefinition.getKey()).isEqualTo(USER_TASK_PROCESS_KEY);
  }

  @Test
  void calledProcessDefinitionQueryWithReadPermissionOnAnyProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(CALLING_USER_TASK_PROCESS_KEY).getId();
    resource = new ProcessDefinitionResource(engineName, processDefinitionId);

    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();

    // when
    List<ProcessDefinitionDto> calledDefinitions = resource.queryCalledProcessDefinitions(queryParameter);

    // then
    assertThat(calledDefinitions).isNotEmpty();

    ProcessDefinitionDto calledProcessDefinition = calledDefinitions.get(0);
    assertThat(calledProcessDefinition.getKey()).isEqualTo(USER_TASK_PROCESS_KEY);
  }

  @Test
  void calledProcessDefinitionQueryWithMultipleReadPermissions() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(CALLING_USER_TASK_PROCESS_KEY).getId();
    resource = new ProcessDefinitionResource(engineName, processDefinitionId);

    String processInstanceId = selectAnyProcessInstanceByKey(CALLING_USER_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();

    // when
    List<ProcessDefinitionDto> calledDefinitions = resource.queryCalledProcessDefinitions(queryParameter);

    // then
    assertThat(calledDefinitions).hasSize(1);

    ProcessDefinitionDto calledProcessDefinition = calledDefinitions.get(0);
    assertThat(calledProcessDefinition.getKey()).isEqualTo(USER_TASK_PROCESS_KEY);
  }

  @Test
  void calledProcessDefinitionQueryWithReadInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(CALLING_USER_TASK_PROCESS_KEY).getId();
    resource = new ProcessDefinitionResource(engineName, processDefinitionId);

    createGrantAuthorization(PROCESS_DEFINITION, CALLING_USER_TASK_PROCESS_KEY, userId, READ_INSTANCE);

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();

    // when
    List<ProcessDefinitionDto> calledDefinitions = resource.queryCalledProcessDefinitions(queryParameter);

    // then
    assertThat(calledDefinitions).isNotEmpty();

    ProcessDefinitionDto calledProcessDefinition = calledDefinitions.get(0);
    assertThat(calledProcessDefinition.getKey()).isEqualTo(USER_TASK_PROCESS_KEY);
  }

  @Test
  @Deployment(resources = {
    "processes/another-user-task-process.bpmn",
    "processes/dynamic-call-activity.bpmn"
  })
  void calledProcessDefinitionQueryDifferentCalledProcesses() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey("DynamicCallActivity").getId();
    resource = new ProcessDefinitionResource(engineName, processDefinitionId);

    disableAuthorization();
    Map<String, Object> vars1 = new HashMap<>();
    vars1.put("callProcess", "anotherUserTaskProcess");
    String firstProcessInstanceId = runtimeService.startProcessInstanceByKey("DynamicCallActivity", vars1).getId();

    Map<String, Object> vars2 = new HashMap<>();
    vars2.put("callProcess", "userTaskProcess");
    runtimeService.startProcessInstanceByKey("DynamicCallActivity", vars2);
    enableAuthorization();

    createGrantAuthorization(PROCESS_INSTANCE, firstProcessInstanceId, userId, READ);

    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto();

    // when
    List<ProcessDefinitionDto> calledDefinitions = resource.queryCalledProcessDefinitions(queryParameter);

    // then
    assertThat(calledDefinitions).isNotEmpty();

    ProcessDefinitionDto calledProcessDefinition = calledDefinitions.get(0);
    assertThat(calledProcessDefinition.getKey()).isEqualTo("anotherUserTaskProcess");
  }

}
