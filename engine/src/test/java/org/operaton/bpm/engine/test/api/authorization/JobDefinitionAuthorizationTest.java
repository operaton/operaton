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
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.runtime.Job;

/**
 * @author Roman Smirnov
 *
 */
class JobDefinitionAuthorizationTest extends AuthorizationTest {

  protected static final String TIMER_START_PROCESS_KEY = "timerStartProcess";
  protected static final String TIMER_BOUNDARY_PROCESS_KEY = "timerBoundaryProcess";

  @Override
  @BeforeEach
  public void setUp() {
    testRule.deploy(
        "org/operaton/bpm/engine/test/api/authorization/timerStartEventProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml");
    super.setUp();
  }

  // job definition query ///////////////////////////////////////

  @Test
  void testQueryWithoutAuthorization() {
    // given

    // when
    JobDefinitionQuery query = managementService.createJobDefinitionQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithReadPermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_START_PROCESS_KEY, userId, READ);

    // when
    JobDefinitionQuery query = managementService.createJobDefinitionQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryWithReadPermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);

    // when
    JobDefinitionQuery query = managementService.createJobDefinitionQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryWithMultiple() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_START_PROCESS_KEY, userId, READ);

    // when
    JobDefinitionQuery query = managementService.createJobDefinitionQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void shouldNotFindJobDefinitionWithRevokedReadPermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, ANY, READ);
    createRevokeAuthorization(PROCESS_DEFINITION, TIMER_START_PROCESS_KEY, userId, READ);

    // when
    JobDefinitionQuery query = managementService.createJobDefinitionQuery();

    // then
    verifyQueryResults(query, 0);
  }

  // suspend job definition by id ///////////////////////////////

  @Test
  void testSuspendByIdWithoutAuthorization() {
    // given
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    try {
      // when
      managementService.suspendJobDefinitionById(jobDefinitionId);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testSuspendByIdWithUpdatePermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    // when
    managementService.suspendJobDefinitionById(jobDefinitionId);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();
  }

  @Test
  void testSuspendByIdWithUpdatePermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE);
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    // when
    managementService.suspendJobDefinitionById(jobDefinitionId);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();
  }

  // activate job definition by id ///////////////////////////////

  @Test
  void testActivateByIdWithoutAuthorization() {
    // given
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionById(jobDefinitionId);

    try {
      // when
      managementService.activateJobDefinitionById(jobDefinitionId);
      fail("Exception expected: It should not be possible to activate a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testActivateByIdWithUpdatePermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionById(jobDefinitionId);

    // when
    managementService.activateJobDefinitionById(jobDefinitionId);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();
  }

  @Test
  void testActivateByIdWithUpdatePermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE);
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionById(jobDefinitionId);

    // when
    managementService.activateJobDefinitionById(jobDefinitionId);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();
  }

  // suspend job definition by id (including jobs) ///////////////////////////////

  @Test
  void testSuspendIncludingJobsByIdWithoutAuthorization() {
    // given
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);

    try {
      // when
      managementService.suspendJobDefinitionById(jobDefinitionId, true);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
      testRule.assertTextPresent(UPDATE_INSTANCE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testSuspendIncludingJobsByIdWithUpdatePermissionOnProcessInstance() {
    // given
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    try {
      // when
      managementService.suspendJobDefinitionById(jobDefinitionId, true);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
      testRule.assertTextPresent(UPDATE_INSTANCE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testSuspendIncludingJobsByIdWithUpdatePermissionOnAnyProcessInstance() {
    // given
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, UPDATE);

    // when
    managementService.suspendJobDefinitionById(jobDefinitionId, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isTrue();
  }

  @Test
  void testSuspendIncludingJobsByIdWithUpdateInstancePermissionOnProcessDefinition() {
    // given
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE, UPDATE_INSTANCE);

    // when
    managementService.suspendJobDefinitionById(jobDefinitionId, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isTrue();
  }

  @Test
  void testSuspendIncludingJobsByIdWithUpdateInstancePermissionOnAnyProcessDefinition() {
    // given
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE_INSTANCE);

    // when
    managementService.suspendJobDefinitionById(jobDefinitionId, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isTrue();
  }

  // activate job definition by id (including jobs) ///////////////////////////////

  @Test
  void testActivateIncludingJobsByIdWithoutAuthorization() {
    // given
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    suspendJobDefinitionIncludingJobsById(jobDefinitionId);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);

    try {
      // when
      managementService.activateJobDefinitionById(jobDefinitionId, true);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
      testRule.assertTextPresent(UPDATE_INSTANCE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testActivateIncludingJobsByIdWithUpdatePermissionOnProcessInstance() {
    // given
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionIncludingJobsById(jobDefinitionId);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    try {
      // when
      managementService.activateJobDefinitionById(jobDefinitionId, true);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
      testRule.assertTextPresent(UPDATE_INSTANCE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testActivateIncludingJobsByIdWithUpdatePermissionOnAnyProcessInstance() {
    // given
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionIncludingJobsById(jobDefinitionId);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, UPDATE);

    // when
    managementService.activateJobDefinitionById(jobDefinitionId, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isFalse();
  }

  @Test
  void testActivateIncludingJobsByIdWithUpdateInstancePermissionOnProcessDefinition() {
    // given
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionIncludingJobsById(jobDefinitionId);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE, UPDATE_INSTANCE);

    // when
    managementService.activateJobDefinitionById(jobDefinitionId, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isFalse();
  }

  @Test
  void testActivateIncludingJobsByIdWithUpdateInstancePermissionOnAnyProcessDefinition() {
    // given
    String jobDefinitionId = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionIncludingJobsById(jobDefinitionId);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE_INSTANCE);

    // when
    managementService.activateJobDefinitionById(jobDefinitionId, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isFalse();
  }

  // suspend job definition by process definition id ///////////////////////////////

  @Test
  void testSuspendByProcessDefinitionIdWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    try {
      // when
      managementService.suspendJobDefinitionByProcessDefinitionId(processDefinitionId);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testSuspendByProcessDefinitionIdWithUpdatePermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    // when
    managementService.suspendJobDefinitionByProcessDefinitionId(processDefinitionId);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();
  }

  @Test
  void testSuspendByProcessDefinitionIdWithUpdatePermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE);
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    // when
    managementService.suspendJobDefinitionByProcessDefinitionId(processDefinitionId);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();
  }

  // activate job definition by process definition id ///////////////////////////////

  @Test
  void testActivateByProcessDefinitionIdWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionByProcessDefinitionId(processDefinitionId);

    try {
      // when
      managementService.activateJobDefinitionByProcessDefinitionId(processDefinitionId);
      fail("Exception expected: It should not be possible to activate a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testActivateByProcessDefinitionIdWithUpdatePermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionByProcessDefinitionId(processDefinitionId);

    // when
    managementService.activateJobDefinitionByProcessDefinitionId(processDefinitionId);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();
  }

  @Test
  void testActivateByProcessDefinitionIdWithUpdatePermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE);
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionByProcessDefinitionId(processDefinitionId);

    // when
    managementService.activateJobDefinitionByProcessDefinitionId(processDefinitionId);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();
  }

  // suspend job definition by process definition id (including jobs) ///////////////////////////////

  @Test
  void testSuspendIncludingJobsByProcessDefinitionIdWithoutAuthorization() {
    // given
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);

    try {
      // when
      managementService.suspendJobDefinitionByProcessDefinitionId(processDefinitionId, true);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
      testRule.assertTextPresent(UPDATE_INSTANCE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testSuspendIncludingJobsByProcessDefinitionIdWithUpdatePermissionOnProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    try {
      // when
      managementService.suspendJobDefinitionByProcessDefinitionId(processDefinitionId, true);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
      testRule.assertTextPresent(UPDATE_INSTANCE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testSuspendIncludingJobsByProcessDefinitionIdWithUpdatePermissionOnAnyProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, UPDATE);

    // when
    managementService.suspendJobDefinitionByProcessDefinitionId(processDefinitionId, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isTrue();
  }

  @Test
  void testSuspendIncludingJobsByProcessDefinitionIdWithUpdateInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE, UPDATE_INSTANCE);

    // when
    managementService.suspendJobDefinitionByProcessDefinitionId(processDefinitionId, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isTrue();
  }

  @Test
  void testSuspendIncludingJobsByProcessDefinitionIdWithUpdateInstancePermissionOnAnyProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE_INSTANCE);

    // when
    managementService.suspendJobDefinitionByProcessDefinitionId(processDefinitionId, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isTrue();
  }

  // activate job definition by id (including jobs) ///////////////////////////////

  @Test
  void testActivateIncludingJobsByProcessDefinitionIdWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    suspendJobDefinitionIncludingJobsByProcessDefinitionId(processDefinitionId);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);

    try {
      // when
      managementService.activateJobDefinitionByProcessDefinitionId(processDefinitionId, true);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
      testRule.assertTextPresent(UPDATE_INSTANCE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testActivateIncludingJobsByProcessDefinitionIdWithUpdatePermissionOnProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionIncludingJobsByProcessDefinitionId(processDefinitionId);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    try {
      // when
      managementService.activateJobDefinitionByProcessDefinitionId(processDefinitionId, true);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
      testRule.assertTextPresent(UPDATE_INSTANCE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testActivateIncludingJobsByProcessDefinitionIdWithUpdatePermissionOnAnyProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionIncludingJobsByProcessDefinitionId(processDefinitionId);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, UPDATE);

    // when
    managementService.activateJobDefinitionByProcessDefinitionId(processDefinitionId, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isFalse();
  }

  @Test
  void testActivateIncludingJobsByProcessDefinitionIdWithUpdateInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionIncludingJobsByProcessDefinitionId(processDefinitionId);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE, UPDATE_INSTANCE);

    // when
    managementService.activateJobDefinitionByProcessDefinitionId(processDefinitionId, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isFalse();
  }

  @Test
  void testActivateIncludingJobsByProcessDefinitionIdWithUpdateInstancePermissionOnAnyProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionIncludingJobsByProcessDefinitionId(processDefinitionId);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE_INSTANCE);

    // when
    managementService.activateJobDefinitionByProcessDefinitionId(processDefinitionId, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isFalse();
  }

  // suspend job definition by process definition key ///////////////////////////////

  @Test
  void testSuspendByProcessDefinitionKeyWithoutAuthorization() {
    // given

    try {
      // when
      managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testSuspendByProcessDefinitionKeyWithUpdatePermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);

    // when
    managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();
  }

  @Test
  void testSuspendByProcessDefinitionKeyWithUpdatePermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE);

    // when
    managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();
  }

  // activate job definition by process definition key ///////////////////////////////

  @Test
  void testActivateByProcessDefinitionKeyWithoutAuthorization() {
    // given
    suspendJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);

    try {
      // when
      managementService.activateJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testActivateByProcessDefinitionKeyWithUpdatePermissionOnProcessDefinition() {
    // given
    suspendJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);

    // when
    managementService.activateJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();
  }

  @Test
  void testActivateByProcessDefinitionKeyWithUpdatePermissionOnAnyProcessDefinition() {
    // given
    suspendJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE);

    // when
    managementService.activateJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();
  }

  // suspend job definition by process definition key (including jobs) ///////////////////////////////

  @Test
  void testSuspendIncludingJobsByProcessDefinitionKeyWithoutAuthorization() {
    // given
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);

    try {
      // when
      managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY, true);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
      testRule.assertTextPresent(UPDATE_INSTANCE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testSuspendIncludingJobsByProcessDefinitionKeyWithUpdatePermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    try {
      // when
      managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY, true);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
      testRule.assertTextPresent(UPDATE_INSTANCE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testSuspendIncludingJobsByProcessDefinitionKeyWithUpdatePermissionOnAnyProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, UPDATE);

    // when
    managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isTrue();
  }

  @Test
  void testSuspendIncludingJobsByProcessDefinitionKeyWithUpdateInstancePermissionOnProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE, UPDATE_INSTANCE);

    // when
    managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isTrue();
  }

  @Test
  void testSuspendIncludingJobsByProcessDefinitionKeyWithUpdateInstancePermissionOnAnyProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE_INSTANCE);

    // when
    managementService.suspendJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isTrue();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isTrue();
  }

  // activate job definition by id (including jobs) ///////////////////////////////

  @Test
  void testActivateIncludingJobsByProcessDefinitionKeyWithoutAuthorization() {
    // given
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    suspendJobDefinitionIncludingJobsByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);

    try {
      // when
      managementService.activateJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY, true);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
      testRule.assertTextPresent(UPDATE_INSTANCE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testActivateIncludingJobsByProcessDefinitionKeyWithUpdatePermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionIncludingJobsByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    try {
      // when
      managementService.activateJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY, true);
      fail("Exception expected: It should not be possible to suspend a job definition");
    } catch (AuthorizationException e) {
      // then
      String message = e.getMessage();
      testRule.assertTextPresent(userId, message);
      testRule.assertTextPresent(UPDATE.getName(), message);
      testRule.assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
      testRule.assertTextPresent(UPDATE_INSTANCE.getName(), message);
      testRule.assertTextPresent(TIMER_BOUNDARY_PROCESS_KEY, message);
      testRule.assertTextPresent(PROCESS_DEFINITION.resourceName(), message);
    }
  }

  @Test
  void testActivateIncludingJobsByProcessDefinitionKeyWithUpdatePermissionOnAnyProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionIncludingJobsByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, UPDATE);

    // when
    managementService.activateJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isFalse();
  }

  @Test
  void testActivateIncludingJobsByProcessDefinitionKeyWithUpdateInstancePermissionOnProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionIncludingJobsByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE, UPDATE_INSTANCE);

    // when
    managementService.activateJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isFalse();
  }

  @Test
  void testActivateIncludingJobsByProcessDefinitionKeyWithUpdateInstancePermissionOnAnyProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    suspendJobDefinitionIncludingJobsByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, UPDATE);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, UPDATE_INSTANCE);

    // when
    managementService.activateJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY, true);

    // then
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.isSuspended()).isFalse();

    Job job = selectJobByProcessInstanceId(processInstanceId);
    assertThat(job).isNotNull();
    assertThat(job.isSuspended()).isFalse();
  }

  // helper /////////////////////////////////////////////////////

  protected JobDefinition selectJobDefinitionByProcessDefinitionKey(String processDefinitionKey) {
    disableAuthorization();
    JobDefinition jobDefinition = managementService
        .createJobDefinitionQuery()
        .processDefinitionKey(processDefinitionKey)
        .singleResult();
    enableAuthorization();
    return jobDefinition;
  }

  protected Job selectJobByProcessInstanceId(String processInstanceId) {
    disableAuthorization();
    Job job = managementService
        .createJobQuery()
        .processInstanceId(processInstanceId)
        .singleResult();
    enableAuthorization();
    return job;
  }

}
