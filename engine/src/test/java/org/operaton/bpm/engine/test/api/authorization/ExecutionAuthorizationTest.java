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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.runtime.AdHocActivity;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ExecutionQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
class ExecutionAuthorizationTest extends AuthorizationTest {

  protected static final String ONE_TASK_PROCESS_KEY = "oneTaskProcess";
  protected static final String MESSAGE_BOUNDARY_PROCESS_KEY = "messageBoundaryProcess";
  protected static final String AD_HOC_PROCESS_KEY = "adHocSubProcessBasic";

  @Override
  @BeforeEach
  public void setUp() {
    testRule.deploy(
        "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/messageBoundaryEventProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.modelIdleNoInitialTasks.bpmn20.xml");
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

  @Test
  void shouldRequireReadAuthorizationForStartableAdHocActivities() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(AD_HOC_PROCESS_KEY);
    String executionId = findAdHocSubProcessExecutionId(processInstance.getId());

    // when/then
    assertThatThrownBy(() -> runtimeService.getStartableAdHocActivities(executionId))
      .isInstanceOf(AuthorizationException.class)
      .hasMessageMatching(getMissingPermissionMessageRegex(READ, PROCESS_INSTANCE))
      .hasMessageMatching(getMissingPermissionMessageRegex(READ_INSTANCE, PROCESS_DEFINITION));
  }

  @Test
  void shouldGetStartableAdHocActivitiesWithReadPermissionOnProcessInstance() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(AD_HOC_PROCESS_KEY);
    String executionId = findAdHocSubProcessExecutionId(processInstance.getId());
    createGrantAuthorization(PROCESS_INSTANCE, processInstance.getId(), userId, READ);

    // when
    List<AdHocActivity> activities = runtimeService.getStartableAdHocActivities(executionId);

    // then
    assertThat(activities)
      .extracting(AdHocActivity::getActivityId)
      .containsExactlyInAnyOrder("taskA", "taskB");
  }

  @Test
  void shouldRequireUpdateAuthorizationForTriggeringAdHocActivities() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(AD_HOC_PROCESS_KEY);
    String executionId = findAdHocSubProcessExecutionId(processInstance.getId());

    // when/then
    assertThatThrownBy(() -> runtimeService.triggerAdHocActivities(executionId, Collections.singletonList("taskA"), null))
      .isInstanceOf(AuthorizationException.class)
      .hasMessageMatching(getMissingPermissionMessageRegex(UPDATE, PROCESS_INSTANCE))
      .hasMessageMatching(getMissingPermissionMessageRegex(UPDATE_INSTANCE, PROCESS_DEFINITION));
  }

  @Test
  void shouldTriggerAdHocActivitiesWithUpdatePermissionOnProcessInstance() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(AD_HOC_PROCESS_KEY);
    String executionId = findAdHocSubProcessExecutionId(processInstance.getId());
    createGrantAuthorization(PROCESS_INSTANCE, processInstance.getId(), userId, UPDATE);

    // when
    runtimeService.triggerAdHocActivities(executionId, Collections.singletonList("taskA"), null);

    // then
    assertThat(countTasks(processInstance.getId(), "taskA")).isEqualTo(1L);
  }

  @Test
  void shouldTriggerAdHocActivitiesWithUpdateInstancePermissionOnProcessDefinition() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(AD_HOC_PROCESS_KEY);
    String executionId = findAdHocSubProcessExecutionId(processInstance.getId());
    createGrantAuthorization(PROCESS_DEFINITION, AD_HOC_PROCESS_KEY, userId, UPDATE_INSTANCE);

    // when
    runtimeService.triggerAdHocActivities(executionId, Collections.singletonList("taskA"), null);

    // then
    assertThat(countTasks(processInstance.getId(), "taskA")).isEqualTo(1L);
  }

  @Test
  void shouldRequireUpdateAuthorizationForCompletingAdHocSubProcess() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(AD_HOC_PROCESS_KEY);
    String executionId = findAdHocSubProcessExecutionId(processInstance.getId());

    // when/then
    assertThatThrownBy(() -> runtimeService.completeAdHocSubProcess(executionId))
      .isInstanceOf(AuthorizationException.class)
      .hasMessageMatching(getMissingPermissionMessageRegex(UPDATE, PROCESS_INSTANCE))
      .hasMessageMatching(getMissingPermissionMessageRegex(UPDATE_INSTANCE, PROCESS_DEFINITION));
  }

  @Test
  void shouldCompleteAdHocSubProcessWithUpdatePermissionOnProcessInstance() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(AD_HOC_PROCESS_KEY);
    String executionId = findAdHocSubProcessExecutionId(processInstance.getId());
    createGrantAuthorization(PROCESS_INSTANCE, processInstance.getId(), userId, UPDATE);

    // when
    runtimeService.completeAdHocSubProcess(executionId);

    // then
    assertThat(countTasks(processInstance.getId(), "taskAfter")).isEqualTo(1L);
  }

  @Test
  void shouldCompleteAdHocSubProcessWithUpdateInstancePermissionOnProcessDefinition() {
    // given
    ProcessInstance processInstance = startProcessInstanceByKey(AD_HOC_PROCESS_KEY);
    String executionId = findAdHocSubProcessExecutionId(processInstance.getId());
    createGrantAuthorization(PROCESS_DEFINITION, AD_HOC_PROCESS_KEY, userId, UPDATE_INSTANCE);

    // when
    runtimeService.completeAdHocSubProcess(executionId);

    // then
    assertThat(countTasks(processInstance.getId(), "taskAfter")).isEqualTo(1L);
  }

  protected String findAdHocSubProcessExecutionId(String processInstanceId) {
    return runWithoutAuthorization(() -> runtimeService.createExecutionQuery()
        .processInstanceId(processInstanceId)
        .activityId("adHocSubProcess")
        .singleResult()
        .getId());
  }

  protected long countTasks(String processInstanceId, String taskDefinitionKey) {
    return runWithoutAuthorization(() -> taskService.createTaskQuery()
        .processInstanceId(processInstanceId)
        .taskDefinitionKey(taskDefinitionKey)
        .count());
  }

}
