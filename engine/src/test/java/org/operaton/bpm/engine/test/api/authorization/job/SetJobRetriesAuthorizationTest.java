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

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.ProcessInstancePermissions;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.revoke;
import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
public class SetJobRetriesAuthorizationTest {

  static final String TIMER_BOUNDARY_PROCESS_KEY = "timerBoundaryProcess";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);

  ManagementService managementService;

  @Parameter
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
      scenario()
        .withoutAuthorizations()
        .failsDueToRequired(
            grant(PROCESS_INSTANCE, "anyProcessInstanceId", "userId", ProcessInstancePermissions.RETRY_JOB),
            grant(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, "userId", ProcessDefinitionPermissions.RETRY_JOB),
            grant(PROCESS_INSTANCE, "anyProcessInstanceId", "userId", UPDATE),
            grant(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, "userId", UPDATE_INSTANCE)),
      scenario()
      .withAuthorizations(
            grant(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, "userId", UPDATE),
            revoke(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, "userId", ProcessDefinitionPermissions.RETRY_JOB))
      .failsDueToRequired(
            grant(PROCESS_INSTANCE, "anyProcessInstanceId", "userId", ProcessInstancePermissions.RETRY_JOB),
            grant(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, "userId", ProcessDefinitionPermissions.RETRY_JOB),
            grant(PROCESS_INSTANCE, "anyProcessInstanceId", "userId", UPDATE),
            grant(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, "userId", UPDATE_INSTANCE)),
      scenario()
        .withAuthorizations(
            grant(PROCESS_INSTANCE, "specProcessInstanceId", "userId", UPDATE, ProcessInstancePermissions.RETRY_JOB))
        .failsDueToRequired(
            grant(PROCESS_INSTANCE, "anyProcessInstanceId", "userId", ProcessInstancePermissions.RETRY_JOB),
            grant(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, "userId", ProcessDefinitionPermissions.RETRY_JOB),
            grant(PROCESS_INSTANCE, "anyProcessInstanceId", "userId", UPDATE),
            grant(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, "userId", UPDATE_INSTANCE)),
      scenario()
        .withAuthorizations(
            grant(PROCESS_INSTANCE, "anyProcessInstanceId", "userId", ProcessInstancePermissions.RETRY_JOB))
        .succeeds(),
      scenario()
        .withAuthorizations(
            grant(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, "userId", ProcessDefinitionPermissions.RETRY_JOB))
        .succeeds(),
      scenario()
        .withAuthorizations(
            grant(PROCESS_INSTANCE, "anyProcessInstanceId", "userId", UPDATE))
        .succeeds(),
      scenario()
        .withAuthorizations(
            grant(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, "userId", UPDATE_INSTANCE))
        .succeeds(),
      scenario()
        .withAuthorizations(
            grant(PROCESS_DEFINITION, "*", "userId", UPDATE_INSTANCE))
        .succeeds(),
      scenario()
        .withAuthorizations(
            grant(PROCESS_DEFINITION, "*", "userId", ProcessDefinitionPermissions.RETRY_JOB))
        .succeeds()
      );
  }

  protected String deploymentId;

  @BeforeEach
  void setUp() {
    managementService = engineRule.getManagementService();
    authRule.createUserAndGroup("userId", "groupId");
  }

  @AfterEach
  void tearDown() {
    authRule.deleteUsersAndGroups();
  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldSetJobRetriesByJobDefinitionId() {
    // given
    String processInstanceId = engineRule.getRuntimeService()
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY)
        .getId();
    JobDefinition jobDefinition = selectJobDefinitionByProcessDefinitionKey(
        TIMER_BOUNDARY_PROCESS_KEY);
    String jobId = selectJobByProcessInstanceId(processInstanceId).getId();
    managementService.setJobRetries(jobId, 0);

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("anyProcessInstanceId", "*")
      .bindResource("specProcessInstanceId", processInstanceId)
      .start();

    managementService.setJobRetriesByJobDefinitionId(jobDefinition.getId(), 1);

    // then
    if (authRule.assertScenario(scenario)) {
      Job job = selectJobById(jobId);
      assertThat(job).isNotNull();
      assertThat(job.getRetries()).isEqualTo(1);
    }

  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml" })
  void shouldSetJobRetries() {
    // given
    String processInstanceId = engineRule.getRuntimeService()
        .startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY)
        .getId();
    String jobId = selectJobByProcessInstanceId(processInstanceId).getId();

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("anyProcessInstanceId", processInstanceId)
      .bindResource("specProcessInstanceId", "unexisting")
      .start();

    managementService.setJobRetries(jobId, 1);

    // then
    if (authRule.assertScenario(scenario)) {
      Job job = selectJobById(jobId);
      assertThat(job).isNotNull();
      assertThat(job.getRetries()).isEqualTo(1);
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

  protected JobDefinition selectJobDefinitionByProcessDefinitionKey(String processDefinitionKey) {
    return managementService
        .createJobDefinitionQuery()
        .processDefinitionKey(processDefinitionKey)
        .singleResult();
  }

}
