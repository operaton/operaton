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
package org.operaton.bpm.engine.test.api.authorization;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.ProcessInstancePermissions;
import org.operaton.bpm.engine.impl.RepositoryServiceImpl;
import org.operaton.bpm.engine.impl.pvm.ReadOnlyProcessDefinition;
import org.operaton.bpm.engine.repository.CalledProcessDefinition;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DiagramLayout;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE_INSTANCE;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE_INSTANCE;
import static org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions.SUSPEND_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
class ProcessDefinitionAuthorizationTest extends AuthorizationTest {

  protected static final String ONE_TASK_PROCESS_KEY = "oneTaskProcess";
  protected static final String TWO_TASKS_PROCESS_KEY = "twoTasksProcess";

  @Override
  @BeforeEach
  public void setUp() {
    testRule.deploy(
        "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml");
    super.setUp();
  }

  @Test
  void testQueryWithoutAuthorization() {
    // given
    // given user is not authorized to read any process definition

    // when
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithReadPermissionOnAnyProcessDefinition() {
    // given
    // given user gets read permission on any process definition
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);

    // when
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryWithMultiple() {
    // given
    // given user gets read permission on any process definition
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ);

    // when
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryWithReadPermissionOnOneTaskProcess() {
    // given
    // given user gets read permission on "oneTaskProcess" process definition
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ);

    // when
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    // then
    verifyQueryResults(query, 1);

    ProcessDefinition definition = query.singleResult();
    assertThat(definition).isNotNull();
    assertThat(definition.getKey()).isEqualTo(ONE_TASK_PROCESS_KEY);
  }

  @Test
  void testQueryWithRevokedReadPermission() {
    // given
    // given user gets all permissions on any process definition
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, ALL);

    Authorization authorization = createRevokeAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY);
    authorization.setUserId(userId);
    authorization.removePermission(READ);
    saveAuthorization(authorization);

    // when
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    // then
    verifyQueryResults(query, 1);

    ProcessDefinition definition = query.singleResult();
    assertThat(definition).isNotNull();
    assertThat(definition.getKey()).isEqualTo(TWO_TASKS_PROCESS_KEY);
  }

  @Test
  void testQueryWithGroupAuthorizationRevokedReadPermission() {
    // given
    // given user gets all permissions on any process definition
    Authorization authorization = createGrantAuthorization(PROCESS_DEFINITION, ANY);
    authorization.setGroupId(groupId);
    authorization.addPermission(ALL);
    saveAuthorization(authorization);

    authorization = createRevokeAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY);
    authorization.setGroupId(groupId);
    authorization.removePermission(READ);
    saveAuthorization(authorization);

    // when
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    // then
    verifyQueryResults(query, 1);

    ProcessDefinition definition = query.singleResult();
    assertThat(definition).isNotNull();
    assertThat(definition.getKey()).isEqualTo(TWO_TASKS_PROCESS_KEY);
  }

  // get process definition /////////////////////////////////////////////////////

  @Test
  void testGetProcessDefinitionWithoutAuthorizations() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();

    // when
    assertThatThrownBy(() -> repositoryService.getProcessDefinition(processDefinitionId))
      // then
      .withFailMessage("Exception expected: It should not be possible to get the process definition")
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(READ.getName())
      .hasMessageContaining(ONE_TASK_PROCESS_KEY)
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testGetProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ);

    // when
    ProcessDefinition definition = repositoryService.getProcessDefinition(processDefinitionId);

    // then
    assertThat(definition).isNotNull();
  }

  // get deployed process definition /////////////////////////////////////////////////////

  @Test
  void testGetDeployedProcessDefinitionWithoutAuthorizations() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();

    // when + then
    assertThatThrownBy(() -> ((RepositoryServiceImpl)repositoryService).getDeployedProcessDefinition(processDefinitionId),
            "It should not be possible to get the process definition")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testGetDeployedProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ);

    // when
    ReadOnlyProcessDefinition definition = ((RepositoryServiceImpl)repositoryService).getDeployedProcessDefinition(processDefinitionId);

    // then
    assertThat(definition).isNotNull();
  }

  // get process diagram /////////////////////////////////////////////////////

  @Test
  void testGetProcessDiagramWithoutAuthorizations() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();

    // when + then
    assertThatThrownBy(() -> repositoryService.getProcessDiagram(processDefinitionId),
            "It should not be possible to get the process diagram")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testGetProcessDiagram() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ);

    // when
    InputStream stream = repositoryService.getProcessDiagram(processDefinitionId);

    // then
    // no process diagram deployed
    assertThat(stream).isNull();
  }

  // get process model /////////////////////////////////////////////////////

  @Test
  void testGetProcessModelWithoutAuthorizations() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();

    // when + then
    assertThatThrownBy(() -> repositoryService.getProcessModel(processDefinitionId),
            "It should not be possible to get the process model")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testGetProcessModel() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ);

    // when
    InputStream stream = repositoryService.getProcessModel(processDefinitionId);

    // then
    assertThat(stream).isNotNull();
  }

  // get bpmn model instance /////////////////////////////////////////////////////

  @Test
  void testGetBpmnModelInstanceWithoutAuthorizations() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();

    // when + then
    assertThatThrownBy(() -> repositoryService.getBpmnModelInstance(processDefinitionId),
            "It should not be possible to get the bpmn model instance")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testGetBpmnModelInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ);

    // when
    BpmnModelInstance modelInstance = repositoryService.getBpmnModelInstance(processDefinitionId);

    // then
    assertThat(modelInstance).isNotNull();
  }

  // get process diagram layout /////////////////////////////////////////////////

  @Test
  void testGetProcessDiagramLayoutWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();

    // when + then
    assertThatThrownBy(() -> repositoryService.getProcessDiagramLayout(processDefinitionId),
            "It should not be possible to get the process diagram layout")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(READ.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testGetProcessDiagramLayout() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ);

    // when
    DiagramLayout diagramLayout = repositoryService.getProcessDiagramLayout(processDefinitionId);

    // then
    // no process diagram deployed
    assertThat(diagramLayout).isNull();
  }

  // suspend process definition by id ///////////////////////////////////////////

  @Test
  void testSuspendProcessDefinitionByIdWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();

    // when + then
    assertThatThrownBy(() -> repositoryService.suspendProcessDefinitionById(processDefinitionId),
            "It should not be possible to suspend the process definition")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(ProcessDefinitionPermissions.SUSPEND.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testSuspendProcessDefinitionById() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE);

    // when
    repositoryService.suspendProcessDefinitionById(processDefinitionId);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isTrue();
  }

  @Test
  void testSuspendProcessDefinitionByIdWithSuspendPermission() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND);

    // when
    repositoryService.suspendProcessDefinitionById(processDefinitionId);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isTrue();
  }

  // activate process definition by id ///////////////////////////////////////////

  @Test
  void testActivateProcessDefinitionByIdWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessDefinitionById(processDefinitionId);

    // when + then
    assertThatThrownBy(() -> repositoryService.activateProcessDefinitionById(processDefinitionId),
            "It should not be possible to activate the process definition")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(ProcessDefinitionPermissions.SUSPEND.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testActivateProcessDefinitionById() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessDefinitionById(processDefinitionId);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE);

    // when
    repositoryService.activateProcessDefinitionById(processDefinitionId);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isFalse();
  }


  @Test
  void testActivateProcessDefinitionByIdWithSuspendPermission() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessDefinitionById(processDefinitionId);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND);

    // when
    repositoryService.activateProcessDefinitionById(processDefinitionId);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isFalse();
  }

  // suspend process definition by id including instances ///////////////////////////////////////////

  @Test
  void testSuspendProcessDefinitionByIdIncludingInstancesWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE);

    // when + then
    assertThatThrownBy(() -> repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null),
            "It should not be possible to suspend the process definition")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(ProcessInstancePermissions.SUSPEND.getName())
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(PROCESS_INSTANCE.resourceName())
        .hasMessageContaining(SUSPEND_INSTANCE.getName())
        .hasMessageContaining(UPDATE_INSTANCE.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testSuspendProcessDefinitionByIdIncludingInstancesWithUpdatePermissionOnProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE, ProcessDefinitionPermissions.SUSPEND);

    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE, ProcessInstancePermissions.SUSPEND);

    // when + then
    assertThatThrownBy(() -> repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null))
        .withFailMessage("It should not be possible to suspend the process definition")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(ProcessInstancePermissions.SUSPEND.getName())
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(PROCESS_INSTANCE.resourceName())
        .hasMessageContaining(SUSPEND_INSTANCE.getName())
        .hasMessageContaining(UPDATE_INSTANCE.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testSuspendProcessDefinitionByIdIncludingInstancesWithUpdatePermissionOnAnyProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, UPDATE);

    // when
    repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isTrue();

    ProcessInstance instance = selectSingleProcessInstance();
    assertThat(instance.isSuspended()).isTrue();
  }

  @Test
  void testSuspendProcessDefinitionByIdIncludingInstancesWithSuspendPermissionOnAnyProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, ProcessInstancePermissions.SUSPEND);

    // when
    repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);

    // then
    verifyProcessDefinitionSuspendedByKeyIncludingInstances();
  }

  @Test
  void testSuspendProcessDefinitionByIdIncludingInstancesWithUpdateInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE, UPDATE_INSTANCE);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    // when
    repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isTrue();

    ProcessInstance instance = selectSingleProcessInstance();
    assertThat(instance.isSuspended()).isTrue();
  }

  @Test
  void testSuspendProcessDefinitionByIdIncludingInstancesWithUpdateAndSuspendInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE, SUSPEND_INSTANCE);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    // when
    repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);

    // then
    verifyProcessDefinitionSuspendedByKeyIncludingInstances();
  }

  @Test
  void testSuspendProcessDefinitionByIdIncludingInstancesWithSuspendAndSuspendInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND, SUSPEND_INSTANCE);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    // when
    repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);

    // then
    verifyProcessDefinitionSuspendedByKeyIncludingInstances();
  }

  @Test
  void testSuspendProcessDefinitionByIdIncludingInstancesWithSuspendAndUpdateInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND, UPDATE_INSTANCE);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    // when
    repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);

    // then
    verifyProcessDefinitionSuspendedByKeyIncludingInstances();
  }

  // activate process definition by id including instances ///////////////////////////////////////////

  @Test
  void testActivateProcessDefinitionByIdIncludingInstancesWithoutAuthorization() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessDefinitionById(processDefinitionId);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE, ProcessDefinitionPermissions.SUSPEND);

    // when + then
    assertThatThrownBy(() -> repositoryService.activateProcessDefinitionById(processDefinitionId, true, null))
        .withFailMessage("It should not be possible to activate the process definition")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(ProcessInstancePermissions.SUSPEND.getName())
        .hasMessageContaining(PROCESS_INSTANCE.resourceName())
        .hasMessageContaining(SUSPEND_INSTANCE.getName())
        .hasMessageContaining(UPDATE_INSTANCE.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testActivateProcessDefinitionByIdIncludingInstancesWithUpdatePermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessDefinitionById(processDefinitionId);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE);

    // when + then
    assertThatThrownBy(() -> repositoryService.activateProcessDefinitionById(processDefinitionId, true, null))
        .withFailMessage("It should not be possible to activate the process definition")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(ProcessInstancePermissions.SUSPEND.getName())
        .hasMessageContaining(PROCESS_INSTANCE.resourceName())
        .hasMessageContaining(SUSPEND_INSTANCE.getName())
        .hasMessageContaining(UPDATE_INSTANCE.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testActivateProcessDefinitionByIdIncludingInstancesWithUpdatePermissionOnAnyProcessInstance() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, UPDATE);

    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessDefinitionById(processDefinitionId);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE);

    // when
    repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isFalse();

    ProcessInstance instance = selectSingleProcessInstance();
    assertThat(instance.isSuspended()).isFalse();
  }

  @Test
  void testActivateProcessDefinitionByIdIncludingInstancesWithSuspendPermissionOnAnyProcessInstance() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, ProcessInstancePermissions.SUSPEND);

    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessDefinitionById(processDefinitionId);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND);

    // when
    repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);

    // then
    verifyProcessDefinitionActivatedByKeyIncludingInstances();
  }

  @Test
  void testActivateProcessDefinitionByIdIncludingInstancesWithUpdateInstancePermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessDefinitionById(processDefinitionId);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE, UPDATE_INSTANCE);

    // when
    repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isFalse();

    ProcessInstance instance = selectSingleProcessInstance();
    assertThat(instance.isSuspended()).isFalse();
  }

  @Test
  void testActivateProcessDefinitionByIdIncludingInstancesWithSuspendAndSuspendInstancePermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessDefinitionById(processDefinitionId);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND, SUSPEND_INSTANCE);

    // when
    repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);

    // then
    verifyProcessDefinitionActivatedByKeyIncludingInstances();
  }

  @Test
  void testActivateProcessDefinitionByIdIncludingInstancesWithSuspendAndUpdateInstancePermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessDefinitionById(processDefinitionId);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND, UPDATE_INSTANCE);

    // when
    repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);

    // then
    verifyProcessDefinitionActivatedByKeyIncludingInstances();
  }

  @Test
  void testActivateProcessDefinitionByIdIncludingInstancesWithUpdateAndSuspendInstancePermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();
    suspendProcessDefinitionById(processDefinitionId);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE, SUSPEND_INSTANCE);

    // when
    repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);

    // then
    verifyProcessDefinitionActivatedByKeyIncludingInstances();
  }

  // suspend process definition by key ///////////////////////////////////////////

  @Test
  void testSuspendProcessDefinitionByKeyWithoutAuthorization() {
    assertThatThrownBy(() -> repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY),
            "It should not be possible to suspend the process definition")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(ProcessDefinitionPermissions.SUSPEND.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testSuspendProcessDefinitionByKey() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE);

    // when
    repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isTrue();
  }

  @Test
  void testSuspendProcessDefinitionByKeyWithSuspendPermission() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND);

    // when
    repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isTrue();
  }

  // activate process definition by id ///////////////////////////////////////////

  @Test
  void testActivateProcessDefinitionByKeyWithoutAuthorization() {
    // given
    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);

    // when + then
    assertThatThrownBy(() -> repositoryService.activateProcessDefinitionByKey(ONE_TASK_PROCESS_KEY),
            "It should not be possible to activate the process definition")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(ProcessDefinitionPermissions.SUSPEND.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testActivateProcessDefinitionByKey() {
    // given
    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE);

    // when
    repositoryService.activateProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isFalse();
  }

  @Test
  void testActivateProcessDefinitionByKeyWithSuspendPermission() {
    // given
    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND);

    // when
    repositoryService.activateProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isFalse();
  }

  // suspend process definition by key including instances ///////////////////////////////////////////

  @Test
  void testSuspendProcessDefinitionByKeyIncludingInstancesWithoutAuthorization() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE, ProcessDefinitionPermissions.SUSPEND);

    // when + then
    assertThatThrownBy(() -> repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null),
            "It should not be possible to suspend the process definition")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(ProcessInstancePermissions.SUSPEND.getName())
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(PROCESS_INSTANCE.resourceName())
        .hasMessageContaining(SUSPEND_INSTANCE.getName())
        .hasMessageContaining(UPDATE_INSTANCE.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testSuspendProcessDefinitionByKeyIncludingInstancesWithUpdatePermissionOnProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE, ProcessDefinitionPermissions.SUSPEND);

    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE, ProcessInstancePermissions.SUSPEND);

    // when + then
    assertThatThrownBy(() -> repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null),
            "It should not be possible to suspend the process definition")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(ProcessInstancePermissions.SUSPEND.getName())
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(PROCESS_INSTANCE.resourceName())
        .hasMessageContaining(SUSPEND_INSTANCE.getName())
        .hasMessageContaining(UPDATE_INSTANCE.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testSuspendProcessDefinitionByKeyIncludingInstancesWithUpdatePermissionOnAnyProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, UPDATE);

    // when
    repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isTrue();

    ProcessInstance instance = selectSingleProcessInstance();
    assertThat(instance.isSuspended()).isTrue();
  }

  @Test
  void testSuspendProcessDefinitionByKeyIncludingInstancesWithSuspendPermissionOnAnyProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, ProcessInstancePermissions.SUSPEND);

    // when
    repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null);

    // then
    verifyProcessDefinitionSuspendedByKeyIncludingInstances();
  }

  @Test
  void testSuspendProcessDefinitionByKeyIncludingInstancesWithUpdateInstancePermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE, UPDATE_INSTANCE);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    // when
    repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isTrue();

    ProcessInstance instance = selectSingleProcessInstance();
    assertThat(instance.isSuspended()).isTrue();
  }

  @Test
  void testSuspendProcessDefinitionByKeyIncludingInstancesWithSuspendAndSuspendInstancePermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND, SUSPEND_INSTANCE);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    // when
    repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null);

    // then
    verifyProcessDefinitionSuspendedByKeyIncludingInstances();
  }


  @Test
  void testSuspendProcessDefinitionByKeyIncludingInstancesWithSuspendAndUpdateInstancePermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND, UPDATE_INSTANCE);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    // when
    repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null);

    // then
    verifyProcessDefinitionSuspendedByKeyIncludingInstances();
  }


  @Test
  void testSuspendProcessDefinitionByKeyIncludingInstancesWithUpdateAndSuspendInstancePermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE, SUSPEND_INSTANCE);

    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    // when
    repositoryService.suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null);

    // then
    verifyProcessDefinitionSuspendedByKeyIncludingInstances();
  }

  // activate process definition by key including instances ///////////////////////////////////////////

  @Test
  void testActivateProcessDefinitionByKeyIncludingInstancesWithoutAuthorization() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE);

    // when + then
    assertThatThrownBy(() -> repositoryService.activateProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null),
            "It should not be possible to activate the process definition")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(ProcessInstancePermissions.SUSPEND.getName())
        .hasMessageContaining(PROCESS_INSTANCE.resourceName())
        .hasMessageContaining(SUSPEND_INSTANCE.getName())
        .hasMessageContaining(UPDATE_INSTANCE.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testActivateProcessDefinitionByKeyIncludingInstancesWithUpdatePermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE, ProcessInstancePermissions.SUSPEND);

    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE, ProcessDefinitionPermissions.SUSPEND);

    // when + then
    assertThatThrownBy(() -> repositoryService.activateProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null),
            "It should not be possible to activate the process definition")
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(ProcessInstancePermissions.SUSPEND.getName())
        .hasMessageContaining(PROCESS_INSTANCE.resourceName())
        .hasMessageContaining(SUSPEND_INSTANCE.getName())
        .hasMessageContaining(UPDATE_INSTANCE.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  @Test
  void testActivateProcessDefinitionByKeyIncludingInstancesWithUpdatePermissionOnAnyProcessInstance() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, UPDATE);

    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE);

    // when
    repositoryService.activateProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isFalse();

    ProcessInstance instance = selectSingleProcessInstance();
    assertThat(instance.isSuspended()).isFalse();
  }

  @Test
  void testActivateProcessDefinitionByKeyIncludingInstancesWithSuspendPermissionOnAnyProcessInstance() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, ProcessInstancePermissions.SUSPEND);

    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND);

    // when
    repositoryService.activateProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null);

    // then
    verifyProcessDefinitionActivatedByKeyIncludingInstances();
  }

  @Test
  void testActivateProcessDefinitionByKeyIncludingInstancesWithUpdateInstancePermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE, UPDATE_INSTANCE);

    // when
    repositoryService.activateProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null);

    // then
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isFalse();

    ProcessInstance instance = selectSingleProcessInstance();
    assertThat(instance.isSuspended()).isFalse();
  }

  @Test
  void testActivateProcessDefinitionByKeyIncludingInstancesWithSuspendAndSuspendInstancePermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND, SUSPEND_INSTANCE);

    // when
    repositoryService.activateProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null);

    // then
    verifyProcessDefinitionActivatedByKeyIncludingInstances();
  }


  @Test
  void testActivateProcessDefinitionByKeyIncludingInstancesWithSuspendAndUpdateInstancePermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, ProcessDefinitionPermissions.SUSPEND, UPDATE_INSTANCE);

    // when
    repositoryService.activateProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null);

    // then
    verifyProcessDefinitionActivatedByKeyIncludingInstances();
  }


  @Test
  void testActivateProcessDefinitionByKeyIncludingInstancesWithUpdateAndSuspendInstancePermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    suspendProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE, SUSPEND_INSTANCE);

    // when
    repositoryService.activateProcessDefinitionByKey(ONE_TASK_PROCESS_KEY, true, null);

    // then
    verifyProcessDefinitionActivatedByKeyIncludingInstances();
  }


  // update history time to live ///////////////////////////////////////////

  @Test
  void testProcessDefinitionUpdateTimeToLive() {

    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, UPDATE);
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);

    // when
    repositoryService.updateProcessDefinitionHistoryTimeToLive(definition.getId(), 6);

    // then
    definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.getHistoryTimeToLive().intValue()).isEqualTo(6);

  }

  @Test
  void testDecisionDefinitionUpdateTimeToLiveWithoutAuthorizations() {
    //given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY).getId();

    // when
    assertThatThrownBy(() -> repositoryService.updateProcessDefinitionHistoryTimeToLive(processDefinitionId, 6))
      // then
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining(userId)
        .hasMessageContaining(UPDATE.getName())
        .hasMessageContaining(ONE_TASK_PROCESS_KEY)
        .hasMessageContaining(PROCESS_DEFINITION.resourceName());

  }

  // startable in tasklist ///////////////////////////////////////////

  @Test
  void testStartableInTasklist() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ, CREATE_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, "*", userId, CREATE);
    final ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);

    // when
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().startablePermissionCheck().startableInTasklist().list();
    // then
    assertThat(processDefinitions).isNotNull();
    assertThat(repositoryService.createProcessDefinitionQuery().startablePermissionCheck().startableInTasklist().count()).isOne();
    assertThat(processDefinitions.get(0).getId()).isEqualTo(definition.getId());
    assertThat(processDefinitions.get(0).isStartableInTasklist()).isTrue();
  }

  @Test
  void testStartableInTasklistReadAllProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, "*", userId, READ);
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, CREATE_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, "*", userId, CREATE);
    final ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);

    // when
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().startablePermissionCheck().startableInTasklist().list();
    // then
    assertThat(processDefinitions).isNotNull();
    assertThat(repositoryService.createProcessDefinitionQuery().startablePermissionCheck().startableInTasklist().count()).isOne();
    assertThat(processDefinitions.get(0).getId()).isEqualTo(definition.getId());
    assertThat(processDefinitions.get(0).isStartableInTasklist()).isTrue();
  }

  @Test
  void testStartableInTasklistWithoutCreateInstancePerm() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ);
    createGrantAuthorization(PROCESS_INSTANCE, "*", userId, CREATE);
    selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);

    // when
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().startablePermissionCheck().startableInTasklist().list();
    // then
    assertThat(processDefinitions).isNotNull().isEmpty();
  }

  @Test
  void testStartableInTasklistWithoutReadDefPerm() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, CREATE_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, "*", userId, CREATE);
    selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);

    // when
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().startablePermissionCheck().startableInTasklist().list();
    // then
    assertThat(processDefinitions).isNotNull().isEmpty();
  }

  @Test
  void testStartableInTasklistWithoutCreatePerm() {
    // given
    selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);

    // when
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().startablePermissionCheck().startableInTasklist().list();
    // then
    assertThat(processDefinitions).isNotNull().isEmpty();
  }

  @Test
  void shouldNotResolveUnauthorizedCalledProcessDefinitions() {
    Deployment deployment = createDeployment("org/operaton/bpm/engine/test/api/repository/call-activities-with-references.bpmn",
      "org/operaton/bpm/engine/test/api/repository/first-process.bpmn20.xml");
    try {
      //given
      String parentKey = "TestCallActivitiesWithReferences";
      createGrantAuthorization(PROCESS_DEFINITION, parentKey, userId, READ);
      ProcessDefinition parentDefinition = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey(parentKey).singleResult();

      //when
      Collection<CalledProcessDefinition> mappings = repositoryService
        .getStaticCalledProcessDefinitions(parentDefinition.getId());

      //then
      assertThat(mappings).isEmpty();
    } finally {
      deleteDeployment(deployment.getId());
    }

  }

  // helper /////////////////////////////////////////////////////////////////////

  protected void verifyProcessDefinitionSuspendedByKeyIncludingInstances() {
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isTrue();

    ProcessInstance instance = selectSingleProcessInstance();
    assertThat(instance.isSuspended()).isTrue();
  }

  protected void verifyProcessDefinitionActivatedByKeyIncludingInstances() {
    ProcessDefinition definition = selectProcessDefinitionByKey(ONE_TASK_PROCESS_KEY);
    assertThat(definition.isSuspended()).isFalse();

    ProcessInstance instance = selectSingleProcessInstance();
    assertThat(instance.isSuspended()).isFalse();
  }

}
