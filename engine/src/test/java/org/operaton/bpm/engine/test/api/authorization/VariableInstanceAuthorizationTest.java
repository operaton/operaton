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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.runtime.VariableInstanceQuery;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions.READ_INSTANCE_VARIABLE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.TASK;
import static org.operaton.bpm.engine.authorization.TaskPermissions.READ_VARIABLE;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
class VariableInstanceAuthorizationTest extends AuthorizationTest {

  protected static final String PROCESS_KEY = "oneTaskProcess";
  protected static final String CASE_KEY = "oneTaskCase";

  protected String deploymentId;
  protected boolean ensureSpecificVariablePermission;

  @Override
  @BeforeEach
  public void setUp() {
    deploymentId = testRule.deploy(
        "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/oneTaskCase.cmmn").getId();
    ensureSpecificVariablePermission = processEngineConfiguration.isEnforceSpecificVariablePermission();
    super.setUp();
  }

  @Override
  @AfterEach
  public void tearDown() {
    super.tearDown();
    deleteDeployment(deploymentId);
    processEngineConfiguration.setEnforceSpecificVariablePermission(ensureSpecificVariablePermission);
  }

  @Test
  void testProcessVariableQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testCaseVariableQueryWithoutAuthorization() {
    // given
    createCaseInstanceByKey(CASE_KEY, getVariables());

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testProcessLocalTaskVariableQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testCaseLocalTaskVariableQueryWithoutAuthorization() {
    // given
    testRule.createCaseInstanceByKey(CASE_KEY);
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testStandaloneTaskVariableQueryWithoutAuthorization() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    setTaskVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);

    deleteTask(taskId, true);
  }

  @Test
  void testProcessVariableQueryWithReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY, getVariables()).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testProcessVariableQueryWithReadInstancesPermissionOnOneTaskProcess() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY, getVariables()).getId();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_INSTANCE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testProcessVariableQueryWithReadInstanceVariablePermission() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY, getVariables()).getId();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_INSTANCE_VARIABLE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  // CAM-9888
  public void failingTestProcessVariableQueryWithReadVariablePermission() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(TASK, ANY, userId, READ_VARIABLE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testProcessVariableQueryWithReadProcessInstanceWhenReadVariableIsEnabled() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY, getVariables()).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testProcessVariableQueryWithReadTaskWhenReadVariableIsEnabled() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(PROCESS_KEY, getVariables()).getId();
    createGrantAuthorization(TASK, ANY, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testProcessLocalTaskVariableQueryWithReadPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }


  @Test
  void testProcessLocalTaskVariableQueryWithMultiple() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(TASK, taskId, userId, READ);
    createGrantAuthorization(TASK, ANY, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testProcessLocalTaskVariableQueryWithReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testProcessLocalTaskVariableQueryWithReadPermissionOnOneProcessTask() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_INSTANCE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testProcessLocalTaskVariableQueryWithReadInstanceVariablePermission() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_INSTANCE_VARIABLE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testProcessLocalTaskVariableQueryWithReadVariablePermission() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(TASK, taskId, userId, READ_VARIABLE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    VariableInstance variable = query.singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testProcessLocalTaskVariableQueryWithReadProcessInstanceWhenReadVariableIsEnabled() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testProcessLocalTaskVariableQueryWithReadTaskWhenReadVariableIsEnabled() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    setTaskVariableLocal(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testStandaloneTaskVariableQueryWithReadPermissionOnTask() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    setTaskVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    deleteTask(taskId, true);
  }

  @Test
  void testStandaloneTaskVariableQueryWithReadVariablePermissionOnTask() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String taskId = "myTask";
    createTask(taskId);
    setTaskVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);
    createGrantAuthorization(TASK, taskId, userId, READ_VARIABLE);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    deleteTask(taskId, true);
  }

  @Test
  void testMixedVariables() {
    // given
    String taskId = "myTask";
    createTask(taskId);
    setTaskVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY, getVariables()).getProcessInstanceId();

    createCaseInstanceByKey(CASE_KEY, getVariables());

    // when (1)
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then (1)
    verifyQueryResults(query, 1);

    // when (2)
    createGrantAuthorization(TASK, taskId, userId, READ);

    // then (2)
    verifyQueryResults(query, 2);

    // when (3)
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // then (3)
    verifyQueryResults(query, 3);

    deleteTask(taskId, true);
  }

  @Test
  void testMixedVariablesWhenReadVariableIsEnabled() {
    // given
    setReadVariableAsDefaultReadVariablePermission();
    String taskId = "myTask";
    createTask(taskId);
    setTaskVariable(taskId, VARIABLE_NAME, VARIABLE_VALUE);

    startProcessInstanceByKey(PROCESS_KEY, getVariables()).getProcessInstanceId();

    createCaseInstanceByKey(CASE_KEY, getVariables());

    // when (1)
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then (1)
    verifyQueryResults(query, 1);

    // when (2)
    createGrantAuthorization(TASK, taskId, userId, READ_VARIABLE);

    // then (2)
    verifyQueryResults(query, 2);

    // when (3)
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_INSTANCE_VARIABLE);

    // then (3)
    verifyQueryResults(query, 3);

    deleteTask(taskId, true);
  }

  /*
   * CAM-10864: Tests that the query itself works if authorization is used and a value matcher
   */
  @Test
  void testQueryWithVariableValueFilter() {
    // given
    startProcessInstanceByKey(PROCESS_KEY, getVariables());
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_INSTANCE);

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueEquals(VARIABLE_NAME, VARIABLE_VALUE);
    VariableInstanceQuery ignoreCaseQuery = runtimeService.createVariableInstanceQuery().variableValueEquals(VARIABLE_NAME, VARIABLE_VALUE).matchVariableNamesIgnoreCase();

    // when
    List<VariableInstance> results = query.list();
    List<VariableInstance> ignoreCaseResults = ignoreCaseQuery.list();

    // then
    assertThat(results).hasSize(1);
    assertThat(ignoreCaseResults).hasSize(1);
  }

  @Test
  void shouldNotFindProcessVariableWithRevokedReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY, getVariables()).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, ANY, ALL);
    createRevokeAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  // helper ////////////////////////////////////////////////////////////////

  protected void setReadVariableAsDefaultReadVariablePermission() {
    processEngineConfiguration.setEnforceSpecificVariablePermission(true);
  }

}
