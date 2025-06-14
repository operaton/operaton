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
package org.operaton.bpm.engine.test.api.authorization.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

@Parameterized
public class UpdateJobAuthorizationTest {

  static final String TIMER_BOUNDARY_PROCESS_KEY = "timerBoundaryProcess";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);

  ManagementService managementService;
  RuntimeService runtimeService;

  @Parameter
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
      scenario()
        .withoutAuthorizations()
        .failsDueToRequired(
            grant(PROCESS_INSTANCE, "processInstanceId", "userId", UPDATE),
            grant(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, "userId", UPDATE_INSTANCE)),
      scenario()
        .withAuthorizations(
            grant(PROCESS_INSTANCE, "processInstanceId", "userId", UPDATE))
        .succeeds(),
      scenario()
        .withAuthorizations(
            grant(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, "userId", UPDATE_INSTANCE))
        .succeeds(),
        scenario()
        .withAuthorizations(
            grant(PROCESS_INSTANCE, "someProcessInstanceId", "userId", UPDATE))
        .failsDueToRequired(
            grant(PROCESS_INSTANCE, "processInstanceId", "userId", UPDATE),
            grant(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, "userId", UPDATE_INSTANCE))
        .succeeds()
      );
  }

  protected String deploymentId;

  @BeforeEach
  void setUp() {
    managementService = engineRule.getManagementService();
    runtimeService = engineRule.getRuntimeService();
    authRule.createUserAndGroup("userId", "groupId");
  }

  @AfterEach
  void tearDown() {
    authRule.deleteUsersAndGroups();
  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldExecuteJob() {
    // given
    String processInstanceId = runtimeService
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY)
        .getId();
    String jobId = selectJobByProcessInstanceId(processInstanceId).getId();

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("processInstanceId", processInstanceId)
      .start();

    managementService.executeJob(jobId);

    // then
    if (authRule.assertScenario(scenario)) {
      String taskDefinitionKey = engineRule.getTaskService()
          .createTaskQuery()
          .singleResult()
          .getTaskDefinitionKey();
      assertThat(taskDefinitionKey).isEqualTo("taskAfterBoundaryEvent");
    }
  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldSuspendJobById() {
    // given
    String processInstanceId = runtimeService
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY)
        .getId();
    String jobId = selectJobByProcessInstanceId(processInstanceId).getId();

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("processInstanceId", processInstanceId)
      .bindResource("someProcessInstanceId", "unexisting")
      .start();

    managementService.suspendJobById(jobId);

    // then
    if (authRule.assertScenario(scenario)) {
      Job job = selectJobById(jobId);
      assertThat(job.isSuspended()).isTrue();
    }
  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldActivateJobById() {
    // given
    String processInstanceId = runtimeService
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY)
        .getId();
    String jobId = selectJobByProcessInstanceId(processInstanceId).getId();

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("processInstanceId", processInstanceId)
      .bindResource("someProcessInstanceId", "unexisting")
      .start();

    managementService.activateJobById(jobId);

    // then
    if (authRule.assertScenario(scenario)) {
      Job job = selectJobById(jobId);
      assertThat(job.isSuspended()).isFalse();
    }
  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldSuspendJobByProcessInstanceId() {
    // given
    String processInstanceId = runtimeService
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY)
        .getId();
    String jobId = selectJobByProcessInstanceId(processInstanceId).getId();

    // when
    authRule
    .init(scenario)
    .withUser("userId")
    .bindResource("processInstanceId", processInstanceId)
    .bindResource("someProcessInstanceId", "unexisting")
    .start();

    managementService.suspendJobByProcessInstanceId(processInstanceId);

    // then
    if (authRule.assertScenario(scenario)) {
      Job job = selectJobById(jobId);
      assertThat(job.isSuspended()).isTrue();
    }
  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldActivateJobByProcessInstanceId() {
    // given
    String processInstanceId = runtimeService
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY)
        .getId();
    String jobId = selectJobByProcessInstanceId(processInstanceId).getId();

    // when
    authRule
    .init(scenario)
    .withUser("userId")
    .bindResource("processInstanceId", processInstanceId)
    .bindResource("someProcessInstanceId", "unexisting")
    .start();

    managementService.activateJobByProcessInstanceId(processInstanceId);

    // then
    if (authRule.assertScenario(scenario)) {
      Job job = selectJobById(jobId);
      assertThat(job.isSuspended()).isFalse();
    }
  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldSuspendJobByJobDefinitionId() {
    // given
    String processInstanceId = runtimeService
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY)
        .getId();
    String jobDefinitionId = selectJobDefinitionIdByProcessDefinitionKey(
        TIMER_BOUNDARY_PROCESS_KEY);

    // when
    authRule
    .init(scenario)
    .withUser("userId")
    .bindResource("processInstanceId", "*")
    .bindResource("someProcessInstanceId", processInstanceId)
    .start();

    managementService.suspendJobByJobDefinitionId(jobDefinitionId);

    // then
    if (authRule.assertScenario(scenario)) {
      Job job = selectJobByProcessInstanceId(processInstanceId);
      assertThat(job.isSuspended()).isTrue();
    }
  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldActivateJobByJobDefinitionId() {
    // given
    String processInstanceId = runtimeService
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY)
        .getId();
    String jobDefinitionId = selectJobDefinitionIdByProcessDefinitionKey(
        TIMER_BOUNDARY_PROCESS_KEY);

    // when
    authRule
    .init(scenario)
    .withUser("userId")
    .bindResource("processInstanceId", "*")
    .bindResource("someProcessInstanceId", processInstanceId)
    .start();

    managementService.activateJobByJobDefinitionId(jobDefinitionId);

    // then
    if (authRule.assertScenario(scenario)) {
      Job job = selectJobByProcessInstanceId(processInstanceId);
      assertThat(job.isSuspended()).isFalse();
    }
  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldSuspendJobByProcessDefinitionId() {
    // given
    ProcessInstance processInstance = runtimeService
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    // when
    authRule
    .init(scenario)
    .withUser("userId")
    .bindResource("processInstanceId", "*")
    .bindResource("someProcessInstanceId", processInstance.getId())
    .start();

    managementService.suspendJobByProcessDefinitionId(processInstance.getProcessDefinitionId());

    // then
    if (authRule.assertScenario(scenario)) {
      Job job = selectJobByProcessInstanceId(processInstance.getId());
      assertThat(job.isSuspended()).isTrue();
    }
  }


  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldActivateJobByProcessDefinitionId() {
    // given
    ProcessInstance processInstance = runtimeService
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    // when
    authRule
    .init(scenario)
    .withUser("userId")
    .bindResource("processInstanceId", "*")
    .bindResource("someProcessInstanceId", processInstance.getId())
    .start();

    managementService.activateJobByProcessDefinitionId(processInstance.getProcessDefinitionId());

    // then
    if (authRule.assertScenario(scenario)) {
      Job job = selectJobByProcessInstanceId(processInstance.getId());
      assertThat(job.isSuspended()).isFalse();
    }
  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldSuspendJobByProcessDefinitionKey() {
    // given
    String processInstanceId = runtimeService
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY)
        .getId();

    // when
    authRule
    .init(scenario)
    .withUser("userId")
    .bindResource("processInstanceId", "*")
    .bindResource("someProcessInstanceId", processInstanceId)
    .start();

    managementService.suspendJobByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);

    // then
    if (authRule.assertScenario(scenario)) {
      Job job = selectJobByProcessInstanceId(processInstanceId);
      assertThat(job.isSuspended()).isTrue();
    }
  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldActivateJobByProcessDefinitionKey() {
    // given
    String processInstanceId = runtimeService
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY)
        .getId();

    // when
    authRule
    .init(scenario)
    .withUser("userId")
    .bindResource("processInstanceId", "*")
    .bindResource("someProcessInstanceId", processInstanceId)
    .start();

    managementService.activateJobByProcessDefinitionKey(TIMER_BOUNDARY_PROCESS_KEY);

    // then
    if (authRule.assertScenario(scenario)) {
      Job job = selectJobByProcessInstanceId(processInstanceId);
      assertThat(job.isSuspended()).isFalse();
    }
  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldSetJobDueDate() {
    // given
    String processInstanceId = runtimeService
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY)
        .getId();
    String jobId = selectJobByProcessInstanceId(processInstanceId).getId();

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("processInstanceId", processInstanceId)
      .bindResource("someProcessInstanceId", "unexisting")
      .start();

    managementService.setJobDuedate(jobId, null);

    // then
    if (authRule.assertScenario(scenario)) {
      Job job = selectJobById(jobId);
      assertThat(job.getDuedate()).isNull();
    }
  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldDeleteJob() {
    // given
    String processInstanceId = runtimeService
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY)
        .getId();
    String jobId = selectJobByProcessInstanceId(processInstanceId).getId();

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("processInstanceId", processInstanceId)
      .bindResource("someProcessInstanceId", "unexisting")
      .start();

    managementService.deleteJob(jobId);

    // then
    if (authRule.assertScenario(scenario)) {
      Job job = selectJobByProcessInstanceId(processInstanceId);
      assertThat(job).isNull();
    }
  }

  // helper /////////////////////////////////////////////////////

  protected Job selectJobByProcessInstanceId(String processInstanceId) {
    return managementService
        .createJobQuery()
        .processInstanceId(processInstanceId)
        .singleResult();
  }

  protected Job selectJobById(String jobId) {
    return managementService
        .createJobQuery()
        .jobId(jobId)
        .singleResult();
  }

  protected String selectJobDefinitionIdByProcessDefinitionKey(String processDefinitionKey) {
    JobDefinition jobDefinition = managementService
        .createJobDefinitionQuery()
        .processDefinitionKey(processDefinitionKey)
        .singleResult();
    return jobDefinition.getId();
  }

}
