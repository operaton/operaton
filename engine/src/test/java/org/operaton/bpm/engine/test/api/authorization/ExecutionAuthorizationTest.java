/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.api.authorization;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ExecutionQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;

/**
 * @author Roman Smirnov
 *
 */
class ExecutionAuthorizationTest extends AuthorizationTest {

  protected static final String ONE_TASK_PROCESS_KEY = "oneTaskProcess";
  protected static final String MESSAGE_BOUNDARY_PROCESS_KEY = "messageBoundaryProcess";

  @Override
  @BeforeEach
  public void setUp() {
    testRule.deploy(
        "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/messageBoundaryEventProcess.bpmn20.xml");
    super.setUp();
  }

  @Test
  void testSimpleQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    // when
    ExecutionQuery query = runtimeService.createExecutionQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testSimpleQueryWithReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    ExecutionQuery query = runtimeService.createExecutionQuery();

    // then
    verifyQueryResults(query, 1);

    Execution execution = query.singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testSimpleQueryWithReadPermissionOnAnyProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    ExecutionQuery query = runtimeService.createExecutionQuery();

    // then
    verifyQueryResults(query, 1);

    Execution execution = query.singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testSimpleQueryWithMultiple() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    ExecutionQuery query = runtimeService.createExecutionQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void shouldNotFindExecutionWithRevokedReadPermissionOnProcess() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, ANY, READ);
    createRevokeAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    ExecutionQuery query = runtimeService.createExecutionQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testSimpleQueryWithReadInstancesPermissionOnOneTaskProcess() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_INSTANCE);

    // when
    ExecutionQuery query = runtimeService.createExecutionQuery();

    // then
    verifyQueryResults(query, 1);

    Execution execution = query.singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testSimpleQueryWithReadInstancesPermissionOnAnyProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE);

    // when
    ExecutionQuery query = runtimeService.createExecutionQuery();

    // then
    verifyQueryResults(query, 1);

    Execution execution = query.singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);

    // when
    ExecutionQuery query = runtimeService.createExecutionQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithReadPermissionOnProcessInstance() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();

    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    ExecutionQuery query = runtimeService.createExecutionQuery();

    // then
    verifyQueryResults(query, 1);

    Execution execution = query.singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testQueryWithReadPermissionOnAnyProcessInstance() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    ExecutionQuery query = runtimeService.createExecutionQuery();

    // then
    verifyQueryResults(query, 11);
  }

  @Test
  void testQueryWithReadInstancesPermissionOnOneTaskProcess() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_INSTANCE);

    // when
    ExecutionQuery query = runtimeService.createExecutionQuery();

    // then
    verifyQueryResults(query, 3);
  }

  @Test
  void testQueryWithReadInstancesPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE);

    // when
    ExecutionQuery query = runtimeService.createExecutionQuery();

    // then
    verifyQueryResults(query, 11);
  }

  @Test
  void testQueryShouldReturnAllExecutions() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, processInstance.getId(), userId, READ);

    // when
    ExecutionQuery query = runtimeService.createExecutionQuery();

    // then
    verifyQueryResults(query, 2);
  }

}
