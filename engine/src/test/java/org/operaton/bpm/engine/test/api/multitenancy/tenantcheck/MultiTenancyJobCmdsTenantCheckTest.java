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
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyJobCmdsTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";

  protected static final String PROCESS_DEFINITION_KEY = "exceptionInJobExecution";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessInstance processInstance;

  protected ManagementService managementService;

  protected IdentityService identityService;

  protected static final BpmnModelInstance BPMN_PROCESS = Bpmn.createExecutableProcess("exceptionInJobExecution")
    .startEvent()
     .userTask("aUserTask")
       .boundaryEvent("timerEvent")
         .timerWithDuration("PT4H")
           .serviceTask()
           .operatonExpression("${failing}")
    .endEvent()
    .done();

  static final BpmnModelInstance BPMN_NO_FAIL_PROCESS = Bpmn.createExecutableProcess("noFail")
    .startEvent()
     .userTask("aUserTask")
       .boundaryEvent("timerEvent")
         .timerWithDuration("PT4H")
    .endEvent()
    .done();

  @BeforeEach
  void init() {
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS);
    testRule.deployForTenant(TENANT_ONE, BPMN_NO_FAIL_PROCESS);

    processInstance = engineRule.getRuntimeService()
      .startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
  }

  // set jobRetries
  @Test
  void testSetJobRetriesWithAuthenticatedTenant() {

    Job timerJob = managementService.createJobQuery()
      .processInstanceId(processInstance.getId())
      .singleResult();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    managementService.setJobRetries(timerJob.getId(), 5);

    assertThat(managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult()
        .getRetries()).isEqualTo(5);
  }

  @Test
  void testSetJobRetriesWithNoAuthenticatedTenant() {

    Job timerJob = managementService.createJobQuery()
      .processInstanceId(processInstance.getId())
      .singleResult();
    String timerJobId = timerJob.getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> managementService.setJobRetries(timerJobId, 5))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the job '"+ timerJobId
      +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testSetJobRetriesWithDisabledTenantCheck() {

    Job timerJob = managementService.createJobQuery()
      .processInstanceId(processInstance.getId())
      .singleResult();

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    managementService.setJobRetries(timerJob.getId(), 5);

    // then
    assertThat(managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult()
        .getRetries()).isEqualTo(5);

  }

  // set Jobretries based on job definition
  @Test
  void testSetJobRetriesDefinitionWithAuthenticatedTenant() {

    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().list().get(0);

    String jobId = selectJobByProcessInstanceId(processInstance.getId()).getId();

    managementService.setJobRetries(jobId, 0);

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    // sets the retries for failed jobs - That's the reason why job retries are made 0 in the above step
    managementService.setJobRetriesByJobDefinitionId(jobDefinition.getId(), 1);

    // then
    assertThat(selectJobByProcessInstanceId(processInstance.getId())
        .getRetries()).isEqualTo(1);

  }

  @Test
  void testSetJobRetriesDefinitionWithNoAuthenticatedTenant() {

    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().list().get(0);
    String jobDefinitionId = jobDefinition.getId();

    String jobId = selectJobByProcessInstanceId(processInstance.getId()).getId();

    managementService.setJobRetries(jobId, 0);
    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> managementService.setJobRetriesByJobDefinitionId(jobDefinitionId, 1))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process definition '"
          + jobDefinition.getProcessDefinitionId() + "' because it belongs to no authenticated tenant.");
  }

  @Test
  void testSetJobRetriesDefinitionWithDisabledTenantCheck() {

    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().list().get(0);

    String jobId = selectJobByProcessInstanceId(processInstance.getId()).getId();

    managementService.setJobRetries(jobId, 0);
    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    managementService.setJobRetriesByJobDefinitionId(jobDefinition.getId(), 1);
    // then
    assertThat(selectJobByProcessInstanceId(processInstance.getId()).getRetries()).isEqualTo(1);

  }

  // set JobDueDate
  @Test
  void testSetJobDueDateWithAuthenticatedTenant() {
    Job timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

    assertThat(managementService.createJobQuery().duedateLowerThan(new Date()).count()).isZero();

    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.DATE, -3);

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    managementService.setJobDuedate(timerJob.getId(), cal.getTime());

    // then
    assertThat(managementService.createJobQuery()
      .duedateLowerThan(new Date()).count()).isOne();
  }

  @Test
  void testSetJobDueDateWithNoAuthenticatedTenant() {
    // given
    Job timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
    String timerJobId = timerJob.getId();
    Date duedate = new Date();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> managementService.setJobDuedate(timerJobId, duedate))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the job '" + timerJobId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testSetJobDueDateWithDisabledTenantCheck() {
    Job timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.DATE, -3);

    managementService.setJobDuedate(timerJob.getId(), cal.getTime());
    // then
    assertThat(managementService.createJobQuery()
      .duedateLowerThan(new Date()).count()).isOne();

  }

  // set jobPriority test cases
  @Test
  void testSetJobPriorityWithAuthenticatedTenant() {
    Job timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    managementService.setJobPriority(timerJob.getId(), 5);

    // then
    assertThat(managementService.createJobQuery().priorityHigherThanOrEquals(5).count()).isOne();
  }

  @Test
  void testSetJobPriorityWithNoAuthenticatedTenant() {
    // given
    Job timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
    String timerJobId = timerJob.getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> managementService.setJobPriority(timerJobId, 5))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the job '"+ timerJobId + "' because it belongs to no authenticated tenant.");
  }

  @Test
  void testSetJobPriorityWithDisabledTenantCheck() {
    Job timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    managementService.setJobPriority(timerJob.getId(), 5);
    // then
    assertThat(managementService.createJobQuery().priorityHigherThanOrEquals(5).count()).isOne();
  }

  // setOverridingJobPriorityForJobDefinition without cascade
  @Test
  void testSetOverridingJobPriorityWithAuthenticatedTenant() {
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().list().get(0);
    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 1701);

    // then
    assertThat(managementService.createJobDefinitionQuery()
      .jobDefinitionId(jobDefinition.getId()).singleResult()
      .getOverridingJobPriority()).isEqualTo(1701L);
  }

  @Test
  void testSetOverridingJobPriorityWithNoAuthenticatedTenant() {
    JobDefinition jobDefinition = managementService
      .createJobDefinitionQuery()
      .list().get(0);
    String jobDefinitionId = jobDefinition.getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> managementService.setOverridingJobPriorityForJobDefinition(jobDefinitionId, 1701))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process definition '"
          + jobDefinition.getProcessDefinitionId() +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testSetOverridingJobPriorityWithDisabledTenantCheck() {
    JobDefinition jobDefinition = managementService
      .createJobDefinitionQuery()
      .list().get(0);

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 1701);
    // then
    assertThat(managementService.createJobDefinitionQuery()
      .jobDefinitionId(jobDefinition.getId()).singleResult()
      .getOverridingJobPriority()).isEqualTo(1701L);
  }

  // setOverridingJobPriority with cascade
  @Test
  void testSetOverridingJobPriorityWithCascadeAndAuthenticatedTenant() {
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().list().get(0);
    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 1701, true);

    // then
    assertThat(managementService.createJobDefinitionQuery()
      .jobDefinitionId(jobDefinition.getId()).singleResult()
      .getOverridingJobPriority()).isEqualTo(1701L);
  }

  @Test
  void testSetOverridingJobPriorityWithCascadeAndNoAuthenticatedTenant() {
    // given
    JobDefinition jobDefinition = managementService
      .createJobDefinitionQuery()
      .list().get(0);
    String jobDefinitionId = jobDefinition.getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> managementService.setOverridingJobPriorityForJobDefinition(jobDefinitionId, 1701, true))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process definition '"
          + jobDefinition.getProcessDefinitionId() +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testSetOverridingJobPriorityWithCascadeAndDisabledTenantCheck() {
    JobDefinition jobDefinition = managementService
      .createJobDefinitionQuery()
      .list().get(0);

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 1701, true);
    // then
    assertThat(managementService.createJobDefinitionQuery()
      .jobDefinitionId(jobDefinition.getId()).singleResult()
      .getOverridingJobPriority()).isEqualTo(1701L);
  }

  // clearOverridingJobPriorityForJobDefinition
  @Test
  void testClearOverridingJobPriorityWithAuthenticatedTenant() {
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().list().get(0);

    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 1701);

    assertThat(managementService.createJobDefinitionQuery()
      .jobDefinitionId(jobDefinition.getId()).singleResult()
      .getOverridingJobPriority()).isEqualTo(1701L);

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    managementService.clearOverridingJobPriorityForJobDefinition(jobDefinition.getId());

    // then
    assertThat(managementService.createJobDefinitionQuery()
    .jobDefinitionId(jobDefinition.getId()).singleResult()
    .getOverridingJobPriority()).isNull();

  }

  @Test
  void testClearOverridingJobPriorityWithNoAuthenticatedTenant() {
    // given
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().list().get(0);
    String jobDefinitionId = jobDefinition.getId();

    managementService.setOverridingJobPriorityForJobDefinition(jobDefinitionId, 1701);

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> managementService.clearOverridingJobPriorityForJobDefinition(jobDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process definition '"
          + jobDefinition.getProcessDefinitionId() +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testClearOverridingJobPriorityWithDisabledTenantCheck() {
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().list().get(0);

    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 1701);

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    managementService.clearOverridingJobPriorityForJobDefinition(jobDefinition.getId());
    // then
    assertThat(managementService.createJobDefinitionQuery()
      .jobDefinitionId(jobDefinition.getId()).singleResult()
      .getOverridingJobPriority()).isNull();
  }

  // getJobExceptionStackTrace
  @Test
  void testGetJobExceptionStackTraceWithAuthenticatedTenant() {

    String processInstanceId = engineRule.getRuntimeService()
      .startProcessInstanceByKey(PROCESS_DEFINITION_KEY)
      .getId();

    testRule.executeAvailableJobs();

    String timerJobId = managementService.createJobQuery()
      .processInstanceId(processInstanceId)
      .singleResult()
      .getId();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    assertThat(managementService.getJobExceptionStacktrace(timerJobId)).isNotNull();
  }

  @Test
  void testGetJobExceptionStackTraceWithNoAuthenticatedTenant() {

    String processInstanceId = engineRule.getRuntimeService()
      .startProcessInstanceByKey(PROCESS_DEFINITION_KEY)
      .getId();

    testRule.executeAvailableJobs();

    String timerJobId = managementService.createJobQuery()
      .processInstanceId(processInstanceId)
      .singleResult()
      .getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> managementService.getJobExceptionStacktrace(timerJobId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the job '" + timerJobId
          +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void testGetJobExceptionStackTraceWithDisabledTenantCheck() {

    String processInstanceId = engineRule.getRuntimeService()
      .startProcessInstanceByKey(PROCESS_DEFINITION_KEY)
      .getId();

    testRule.executeAvailableJobs();

    String timerJobId = managementService.createJobQuery()
      .processInstanceId(processInstanceId)
      .singleResult()
      .getId();

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // when
    managementService.getJobExceptionStacktrace(timerJobId);
    assertThat(managementService.getJobExceptionStacktrace(timerJobId)).isNotNull();
  }

  // deleteJobs
  @Test
  void testDeleteJobWithAuthenticatedTenant() {
    String timerJobId = managementService.createJobQuery()
      .processInstanceId(processInstance.getId())
      .singleResult()
      .getId();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    managementService.deleteJob(timerJobId);

    // then
    assertThat(managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .count()).isZero();
  }

  @Test
  void testDeleteJobWithNoAuthenticatedTenant() {
    String timerJobId = managementService.createJobQuery()
      .processInstanceId(processInstance.getId())
      .singleResult()
      .getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> managementService.deleteJob(timerJobId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the job '" + timerJobId
          +"' because it belongs to no authenticated tenant.");
  }

  @Test
  void testDeleteJobWithDisabledTenantCheck() {
    String timerJobId = managementService.createJobQuery()
      .processInstanceId(processInstance.getId())
      .singleResult()
      .getId();

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    managementService.deleteJob(timerJobId);

    // then
    assertThat(managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .count()).isZero();
  }

  //executeJobs
  @Test
  void testExecuteJobWithAuthenticatedTenant() {

    String noFailProcessInstanceId = engineRule.getRuntimeService()
      .startProcessInstanceByKey("noFail")
      .getId();

    TaskQuery taskQuery = engineRule.getTaskService()
      .createTaskQuery()
      .processInstanceId(noFailProcessInstanceId);

    assertThat(taskQuery.list()).hasSize(1);

    String timerJobId = managementService.createJobQuery()
      .processInstanceId(noFailProcessInstanceId)
      .singleResult()
      .getId();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    managementService.executeJob(timerJobId);

    // then
    assertThat(taskQuery.list()).isEmpty();
  }

  @Test
  void testExecuteJobWithNoAuthenticatedTenant() {

    String noFailProcessInstanceId = engineRule.getRuntimeService()
      .startProcessInstanceByKey("noFail")
      .getId();

    String timerJobId = managementService.createJobQuery()
      .processInstanceId(noFailProcessInstanceId)
      .singleResult()
      .getId();

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> managementService.executeJob(timerJobId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the job '" + timerJobId
          +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void testExecuteJobWithDisabledTenantCheck() {

    String noFailProcessInstanceId = engineRule.getRuntimeService()
      .startProcessInstanceByKey("noFail")
      .getId();

    String timerJobId = managementService.createJobQuery()
      .processInstanceId(noFailProcessInstanceId)
      .singleResult()
      .getId();

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    managementService.executeJob(timerJobId);

    TaskQuery taskQuery = engineRule.getTaskService()
      .createTaskQuery()
      .processInstanceId(noFailProcessInstanceId);

    // then
    assertThat(taskQuery.list()).isEmpty();
  }

  protected Job selectJobByProcessInstanceId(String processInstanceId) {
    return managementService
        .createJobQuery()
        .processInstanceId(processInstanceId)
        .singleResult();
  }
}
