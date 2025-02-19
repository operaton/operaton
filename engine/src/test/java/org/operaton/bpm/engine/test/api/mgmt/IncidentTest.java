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
package org.operaton.bpm.engine.test.api.mgmt;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertNotSame;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.IncidentQuery;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.runtime.util.ChangeVariablesDelegate;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.engine.test.util.Removable;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Test;

public class IncidentTest extends PluggableProcessEngineTest {

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncident.bpmn"})
  @Test
  public void shouldCreateOneIncident() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    testRule.executeAvailableJobs();

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    assertThat(incident).isNotNull();

    assertThat(incident.getId()).isNotNull();
    assertThat(incident.getIncidentTimestamp()).isNotNull();
    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentMessage()).isEqualTo(AlwaysFailingDelegate.MESSAGE);
    assertThat(incident.getExecutionId()).isEqualTo(processInstance.getId());
    assertThat(incident.getActivityId()).isEqualTo("theServiceTask");
    assertThat(incident.getFailedActivityId()).isEqualTo("theServiceTask");
    assertThat(incident.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(incident.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(incident.getCauseIncidentId()).isEqualTo(incident.getId());
    assertThat(incident.getRootCauseIncidentId()).isEqualTo(incident.getId());

    Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

    assertThat(job).isNotNull();

    assertThat(incident.getConfiguration()).isEqualTo(job.getId());
    assertThat(incident.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncident.bpmn"})
  @Test
  public void shouldCreateOneIncidentAfterSetRetries() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    testRule.executeAvailableJobs();

    List<Incident> incidents = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).list();

    assertThat(incidents)
            .isNotEmpty()
            .hasSize(1);

    Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

    assertThat(job).isNotNull();

    // set job retries to 1 -> should fail again and a second incident should be created
    managementService.setJobRetries(job.getId(), 1);

    testRule.executeAvailableJobs();

    incidents = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).list();

    // There is still one incident
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncident.bpmn"})
  @Test
  public void shouldCreateOneIncidentAfterExecuteJob() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    testRule.executeAvailableJobs();

    List<Incident> incidents = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).list();

    assertThat(incidents)
            .isNotEmpty()
            .hasSize(1);

    Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

    assertThat(job).isNotNull();
    var jobId = job.getId();

    // set job retries to 1 -> should fail again and a second incident should be created
    try {
      managementService.executeJob(jobId);
      fail("Exception was expected.");
    } catch (ProcessEngineException e) {
      // exception expected
    }

    incidents = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).list();

    // There is still one incident
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncidentForNestedExecution.bpmn"})
  @Test
  public void shouldCreateOneIncidentForNestedExecution() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcessWithNestedExecutions");

    testRule.executeAvailableJobs();

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(incident).isNotNull();

    Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(job).isNotNull();

    String executionIdOfNestedFailingExecution = job.getExecutionId();

    assertNotSame(processInstance.getId(), executionIdOfNestedFailingExecution);

    assertThat(incident.getId()).isNotNull();
    assertThat(incident.getIncidentTimestamp()).isNotNull();
    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentMessage()).isEqualTo(AlwaysFailingDelegate.MESSAGE);
    assertThat(incident.getExecutionId()).isEqualTo(executionIdOfNestedFailingExecution);
    assertThat(incident.getActivityId()).isEqualTo("theServiceTask");
    assertThat(incident.getFailedActivityId()).isEqualTo("theServiceTask");
    assertThat(incident.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(incident.getCauseIncidentId()).isEqualTo(incident.getId());
    assertThat(incident.getRootCauseIncidentId()).isEqualTo(incident.getId());
    assertThat(incident.getConfiguration()).isEqualTo(job.getId());
    assertThat(incident.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
  }

  @Test
  public void shouldCreateIncidentWithCorrectMessageWhenZeroRetriesAreDefined() {
    // given
    String key = "process";
    BpmnModelInstance model = Bpmn.createExecutableProcess(key)
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .serviceTask("theServiceTask")
        .operatonClass(AlwaysFailingDelegate.class)
        .operatonAsyncBefore()
        .operatonFailedJobRetryTimeCycle("R0/PT30S")
        .endEvent()
        .done();

    testRule.deploy(model);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key);

    // when
    testRule.executeAvailableJobs();

    // then
    Incident incident = runtimeService.createIncidentQuery().singleResult();

    assertThat(incident.getId()).isNotNull();
    assertThat(incident.getIncidentTimestamp()).isNotNull();
    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentMessage()).isEqualTo(AlwaysFailingDelegate.MESSAGE);
    assertThat(incident.getExecutionId()).isEqualTo(processInstance.getId());
    assertThat(incident.getActivityId()).isEqualTo("theServiceTask");
    assertThat(incident.getFailedActivityId()).isEqualTo("theServiceTask");
    assertThat(incident.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(incident.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(incident.getCauseIncidentId()).isEqualTo(incident.getId());
    assertThat(incident.getRootCauseIncidentId()).isEqualTo(incident.getId());

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job.getExceptionMessage()).isEqualTo(AlwaysFailingDelegate.MESSAGE);

    String stacktrace = managementService.getJobExceptionStacktrace(job.getId());
    assertThat(stacktrace).isNotNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateRecursiveIncidents.bpmn",
      "org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncident.bpmn"})
  @Test
  public void shouldCreateRecursiveIncidents() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callFailingProcess");

    testRule.executeAvailableJobs();

    List<Incident> incidents = runtimeService.createIncidentQuery().list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(2);

    ProcessInstance failingProcess = runtimeService.createProcessInstanceQuery().processDefinitionKey("failingProcess").singleResult();
    assertThat(failingProcess).isNotNull();

    ProcessInstance callProcess = runtimeService.createProcessInstanceQuery().processDefinitionKey("callFailingProcess").singleResult();
    assertThat(callProcess).isNotNull();

    // Root cause incident
    Incident causeIncident = runtimeService.createIncidentQuery().processDefinitionId(failingProcess.getProcessDefinitionId()).singleResult();
    assertThat(causeIncident).isNotNull();

    Job job = managementService.createJobQuery().executionId(causeIncident.getExecutionId()).singleResult();
    assertThat(job).isNotNull();

    assertThat(causeIncident.getId()).isNotNull();
    assertThat(causeIncident.getIncidentTimestamp()).isNotNull();
    assertThat(causeIncident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(causeIncident.getIncidentMessage()).isEqualTo(AlwaysFailingDelegate.MESSAGE);
    assertThat(causeIncident.getExecutionId()).isEqualTo(job.getExecutionId());
    assertThat(causeIncident.getActivityId()).isEqualTo("theServiceTask");
    assertThat(causeIncident.getFailedActivityId()).isEqualTo("theServiceTask");
    assertThat(causeIncident.getProcessInstanceId()).isEqualTo(failingProcess.getId());
    assertThat(causeIncident.getCauseIncidentId()).isEqualTo(causeIncident.getId());
    assertThat(causeIncident.getRootCauseIncidentId()).isEqualTo(causeIncident.getId());
    assertThat(causeIncident.getConfiguration()).isEqualTo(job.getId());
    assertThat(causeIncident.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());

    // Recursive created incident
    Incident recursiveCreatedIncident = runtimeService.createIncidentQuery().processDefinitionId(callProcess.getProcessDefinitionId()).singleResult();
    assertThat(recursiveCreatedIncident).isNotNull();

    Execution theCallActivityExecution = runtimeService.createExecutionQuery().activityId("theCallActivity").singleResult();
    assertThat(theCallActivityExecution).isNotNull();

    assertThat(recursiveCreatedIncident.getId()).isNotNull();
    assertThat(recursiveCreatedIncident.getIncidentTimestamp()).isNotNull();
    assertThat(recursiveCreatedIncident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(recursiveCreatedIncident.getIncidentMessage()).isNull();
    assertThat(recursiveCreatedIncident.getExecutionId()).isEqualTo(theCallActivityExecution.getId());
    assertThat(recursiveCreatedIncident.getActivityId()).isEqualTo("theCallActivity");
    assertThat(recursiveCreatedIncident.getFailedActivityId()).isEqualTo("theCallActivity");
    assertThat(recursiveCreatedIncident.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(recursiveCreatedIncident.getCauseIncidentId()).isEqualTo(causeIncident.getId());
    assertThat(recursiveCreatedIncident.getRootCauseIncidentId()).isEqualTo(causeIncident.getId());
    assertThat(recursiveCreatedIncident.getConfiguration()).isNull();
    assertThat(recursiveCreatedIncident.getJobDefinitionId()).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateRecursiveIncidentsForNestedCallActivity.bpmn",
  		"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateRecursiveIncidents.bpmn",
  "org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncident.bpmn"})
  @Test
  public void shouldCreateRecursiveIncidentsForNestedCallActivity() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callingFailingCallActivity");

    testRule.executeAvailableJobs();

    List<Incident> incidents = runtimeService.createIncidentQuery().list();
    assertThat(incidents)
            .isNotEmpty()
            .hasSize(3);

    // Root Cause Incident
    ProcessInstance failingProcess = runtimeService.createProcessInstanceQuery().processDefinitionKey("failingProcess").singleResult();
    assertThat(failingProcess).isNotNull();

    Incident rootCauseIncident = runtimeService.createIncidentQuery().processDefinitionId(failingProcess.getProcessDefinitionId()).singleResult();
    assertThat(rootCauseIncident).isNotNull();

    Job job = managementService.createJobQuery().executionId(rootCauseIncident.getExecutionId()).singleResult();
    assertThat(job).isNotNull();

    assertThat(rootCauseIncident.getId()).isNotNull();
    assertThat(rootCauseIncident.getIncidentTimestamp()).isNotNull();
    assertThat(rootCauseIncident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(rootCauseIncident.getIncidentMessage()).isEqualTo(AlwaysFailingDelegate.MESSAGE);
    assertThat(rootCauseIncident.getExecutionId()).isEqualTo(job.getExecutionId());
    assertThat(rootCauseIncident.getActivityId()).isEqualTo("theServiceTask");
    assertThat(rootCauseIncident.getFailedActivityId()).isEqualTo("theServiceTask");
    assertThat(rootCauseIncident.getProcessInstanceId()).isEqualTo(failingProcess.getId());
    assertThat(rootCauseIncident.getCauseIncidentId()).isEqualTo(rootCauseIncident.getId());
    assertThat(rootCauseIncident.getRootCauseIncidentId()).isEqualTo(rootCauseIncident.getId());
    assertThat(rootCauseIncident.getConfiguration()).isEqualTo(job.getId());
    assertThat(rootCauseIncident.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());

    // Cause Incident
    ProcessInstance callFailingProcess = runtimeService.createProcessInstanceQuery().processDefinitionKey("callFailingProcess").singleResult();
    assertThat(callFailingProcess).isNotNull();

    Incident causeIncident = runtimeService.createIncidentQuery().processDefinitionId(callFailingProcess.getProcessDefinitionId()).singleResult();
    assertThat(causeIncident).isNotNull();

    Execution theCallActivityExecution = runtimeService.createExecutionQuery().activityId("theCallActivity").singleResult();
    assertThat(theCallActivityExecution).isNotNull();

    assertThat(causeIncident.getId()).isNotNull();
    assertThat(causeIncident.getIncidentTimestamp()).isNotNull();
    assertThat(causeIncident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(causeIncident.getIncidentMessage()).isNull();
    assertThat(causeIncident.getExecutionId()).isEqualTo(theCallActivityExecution.getId());
    assertThat(causeIncident.getActivityId()).isEqualTo("theCallActivity");
    assertThat(causeIncident.getFailedActivityId()).isEqualTo("theCallActivity");
    assertThat(causeIncident.getProcessInstanceId()).isEqualTo(callFailingProcess.getId());
    assertThat(causeIncident.getCauseIncidentId()).isEqualTo(rootCauseIncident.getId());
    assertThat(causeIncident.getRootCauseIncidentId()).isEqualTo(rootCauseIncident.getId());
    assertThat(causeIncident.getConfiguration()).isNull();
    assertThat(causeIncident.getJobDefinitionId()).isNull();

    // Top level incident of the startet process (recursive created incident for super super process instance)
    Incident topLevelIncident = runtimeService.createIncidentQuery().processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();
    assertThat(topLevelIncident).isNotNull();

    Execution theCallingCallActivity = runtimeService.createExecutionQuery().activityId("theCallingCallActivity").singleResult();
    assertThat(theCallingCallActivity).isNotNull();

    assertThat(topLevelIncident.getId()).isNotNull();
    assertThat(topLevelIncident.getIncidentTimestamp()).isNotNull();
    assertThat(topLevelIncident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(topLevelIncident.getIncidentMessage()).isNull();
    assertThat(topLevelIncident.getExecutionId()).isEqualTo(theCallingCallActivity.getId());
    assertThat(topLevelIncident.getActivityId()).isEqualTo("theCallingCallActivity");
    assertThat(topLevelIncident.getFailedActivityId()).isEqualTo("theCallingCallActivity");
    assertThat(topLevelIncident.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(topLevelIncident.getCauseIncidentId()).isEqualTo(causeIncident.getId());
    assertThat(topLevelIncident.getRootCauseIncidentId()).isEqualTo(rootCauseIncident.getId());
    assertThat(topLevelIncident.getConfiguration()).isNull();
    assertThat(topLevelIncident.getJobDefinitionId()).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncident.bpmn"})
  @Test
  public void shouldDeleteIncidentAfterJobHasBeenDeleted() {
    // start failing process
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    testRule.executeAvailableJobs();

    // get the job
    Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(job).isNotNull();

    // there exists one incident to failed
    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(incident).isNotNull();

    // delete the job
    managementService.deleteJob(job.getId());

    // the incident has been deleted too.
    incident = runtimeService.createIncidentQuery().incidentId(incident.getId()).singleResult();
    assertThat(incident).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldDeleteIncidentAfterJobWasSuccessfully.bpmn"})
  @Test
  public void shouldDeleteIncidentAfterJobWasSuccessfully() {
    // Start process instance
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcessWithUserTask", parameters);

    testRule.executeAvailableJobs();

    // job exists
    Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(job).isNotNull();

    // incident was created
    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(incident).isNotNull();

    // set execution variable from "true" to "false"
    runtimeService.setVariable(processInstance.getId(), "fail", Boolean.FALSE);

    // set retries of failed job to 1, with the change of the fail variable the job
    // will be executed successfully
    managementService.setJobRetries(job.getId(), 1);

    testRule.executeAvailableJobs();

    // Update process instance
    processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(processInstance).isInstanceOf(ExecutionEntity.class);

    // should stay in the user task
    ExecutionEntity exec = (ExecutionEntity) processInstance;
    assertThat(exec.getActivityId()).isEqualTo("theUserTask");

    // there does not exist any incident anymore
    incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(incident).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateIncidentOnFailedStartTimerEvent.bpmn"})
  @Test
  public void shouldCreateIncidentOnFailedStartTimerEvent() {
    // After process start, there should be timer created
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.count()).isEqualTo(1);

    Job job = jobQuery.singleResult();
    String jobId = job.getId();

    while(0 != job.getRetries()) {
      try {
        managementService.executeJob(jobId);
        fail("Exception expected");
      } catch (Exception e) {
        // expected
      }
      job = jobQuery.jobId(jobId).singleResult();

    }

    // job exists
    job = jobQuery.singleResult();
    assertThat(job).isNotNull();

    assertThat(job.getRetries()).isZero();

    // incident was created
    Incident incident = runtimeService.createIncidentQuery().configuration(job.getId()).singleResult();
    assertThat(incident).isNotNull();

    // manually delete job for timer start event
    managementService.deleteJob(job.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncident.bpmn"})
  @Test
  public void shouldNotCreateNewIncident() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    testRule.executeAvailableJobs();

    IncidentQuery query = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId());
    Incident incident = query.singleResult();
    assertThat(incident).isNotNull();

    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // set retries to 1 by job definition id
    managementService.setJobRetriesByJobDefinitionId(jobDefinition.getId(), 1);

    // the incident still exists
    Incident tmp = query.singleResult();
    assertThat(tmp.getId()).isEqualTo(incident.getId());

    // execute the available job (should fail again)
    testRule.executeAvailableJobs();

    // the incident still exists and there
    // should be not a new incident
    assertThat(query.count()).isEqualTo(1);
    tmp = query.singleResult();
    assertThat(tmp.getId()).isEqualTo(incident.getId());
  }

  @Deployment
  @Test
  public void shouldUpdateIncidentAfterCompaction() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    testRule.executeAvailableJobs();

    Incident incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident).isNotNull();
    assertNotSame(processInstanceId, incident.getExecutionId());

    runtimeService.correlateMessage("Message");

    incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident).isNotNull();

    // incident updated with new execution id after execution tree is compacted
    assertThat(incident.getExecutionId()).isEqualTo(processInstanceId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncident.bpmn"})
  @Test
  public void shouldNotSetNegativeRetries() {
    runtimeService.startProcessInstanceByKey("failingProcess");

    testRule.executeAvailableJobs();

    // it exists a job with 0 retries and an incident
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isZero();

    assertThat(runtimeService.createIncidentQuery().count()).isEqualTo(1);

    // it should not be possible to set negative retries
    final JobEntity jobEntity = (JobEntity) job;
    processEngineConfiguration
      .getCommandExecutorTxRequired()
      .execute(commandContext -> {
      jobEntity.setRetries(-100);
      return null;
    });

    assertThat(job.getRetries()).isZero();
    var jobId = job.getJobDefinitionId();
    var jobDefinitionId = job.getJobDefinitionId();

    // retries should still be 0 after execution this job again
    try {
      managementService.executeJob(jobId);
      fail("Exception expected");
    }
    catch (ProcessEngineException e) {
      // expected
    }

    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isZero();

    // also no new incident was created
    assertThat(runtimeService.createIncidentQuery().count()).isEqualTo(1);

    // it should not be possible to set the retries to a negative number with the management service
    try {
      managementService.setJobRetries(jobId, -200);
      fail("Exception expected");
    }
    catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.setJobRetriesByJobDefinitionId(jobDefinitionId, -300);
      fail("Exception expected");
    }
    catch (ProcessEngineException e) {
      // expected
    }

  }

  @Deployment
  @Test
  public void shouldSetActivityIdProperty() {
    testRule.executeAvailableJobs();

    Incident incident = runtimeService
      .createIncidentQuery()
      .singleResult();

    assertThat(incident).isNotNull();

    assertThat(incident.getActivityId()).isNotNull();
    assertThat(incident.getActivityId()).isEqualTo("theStart");
    assertThat(incident.getProcessInstanceId()).isNull();
    assertThat(incident.getExecutionId()).isNull();
  }

  @Test
  public void shouldShowFailedActivityIdPropertyForFailingAsyncTask() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .serviceTask("theTask")
        .operatonAsyncBefore()
        .operatonClass(FailingDelegate.class)
        .endEvent()
        .done());

    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("fail", true));

    // when
    testRule.executeAvailableJobs();

    // then
    Incident incident = runtimeService
       .createIncidentQuery()
       .singleResult();

    assertThat(incident).isNotNull();

    assertThat(incident.getFailedActivityId()).isNotNull();
    assertThat(incident.getFailedActivityId()).isEqualTo("theTask");
  }

  @Test
  public void shouldShowFailedActivityIdPropertyForAsyncTaskWithFailingFollowUp() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .serviceTask("theTask")
        .operatonAsyncBefore()
        .operatonClass(ChangeVariablesDelegate.class)
        .serviceTask("theTask2")
        .operatonClass(ChangeVariablesDelegate.class)
        .serviceTask("theTask3")
        .operatonClass(FailingDelegate.class)
        .endEvent()
        .done());

    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("fail", true));

    // when
    testRule.executeAvailableJobs();

    // then
    Incident incident = runtimeService
       .createIncidentQuery()
       .singleResult();

    assertThat(incident).isNotNull();

    assertThat(incident.getFailedActivityId()).isNotNull();
    assertThat(incident.getFailedActivityId()).isEqualTo("theTask3");
  }

  @Test
  public void shouldSetBoundaryEventIncidentActivityId() {
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask("userTask")
        .endEvent()
        .moveToActivity("userTask")
        .boundaryEvent("boundaryEvent")
        .timerWithDuration("PT5S")
        .endEvent()
        .done());

    // given
    runtimeService.startProcessInstanceByKey("process");
    Job timerJob = managementService.createJobQuery().singleResult();

    // when creating an incident
    managementService.setJobRetries(timerJob.getId(), 0);

    // then
    Incident incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident).isNotNull();
    assertThat(incident.getActivityId()).isEqualTo("boundaryEvent");
  }

  @Test
  public void shouldSetAnnotationForIncident() {
    // given
    String annotation = "my annotation";
    Incident incident = createIncident();

    // when
    runtimeService.setAnnotationForIncidentById(incident.getId(), annotation);

    // then
    incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident.getAnnotation()).isEqualTo(annotation);
  }

  @Test
  public void shouldSetAnnotationForStandaloneIncident() {
    // given
    String annotation = "my annotation";
    String jobId = createStandaloneIncident();
    Incident incident = runtimeService.createIncidentQuery().singleResult();

    // when
    runtimeService.setAnnotationForIncidentById(incident.getId(), annotation);

    // then
    incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident.getAnnotation()).isEqualTo(annotation);

    // clean up
    cleanupStandaloneIncident(jobId);
  }

  @Test
  public void shouldFailSetAnnotationForIncidentWithNullId() {
    // when & then
    assertThatThrownBy(() -> runtimeService.setAnnotationForIncidentById(null, "my annotation"))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("incident id");
  }

  @Test
  public void shouldFailSetAnnotationForIncidentWithNonExistingIncidentId() {
    // when & then
    assertThatThrownBy(() -> runtimeService.setAnnotationForIncidentById("not existing", "my annotation"))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("incident");
  }

  @Test
  public void shouldUpdateAnnotationForIncident() {
    // given
    String annotation = "my new annotation";
    Incident incident = createIncident();
    runtimeService.setAnnotationForIncidentById(incident.getId(), "old annotation");

    // when
    runtimeService.setAnnotationForIncidentById(incident.getId(), annotation);

    // then
    incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident.getAnnotation()).isEqualTo(annotation);
  }

  @Test
  public void shouldClearAnnotationForIncident() {
    // given
    Incident incident = createIncident();
    runtimeService.setAnnotationForIncidentById(incident.getId(), "old annotation");

    // when
    runtimeService.clearAnnotationForIncidentById(incident.getId());

    // then
    incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident.getAnnotation()).isNull();
  }

  @Test
  public void shouldClearAnnotationForStandaloneIncident() {
    // given
    String jobId = createStandaloneIncident();
    Incident incident = runtimeService.createIncidentQuery().singleResult();
    runtimeService.setAnnotationForIncidentById(incident.getId(), "old annotation");

    // when
    runtimeService.clearAnnotationForIncidentById(incident.getId());

    // then
    incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident.getAnnotation()).isNull();

    // cleanup
    cleanupStandaloneIncident(jobId);
  }

  @Test
  public void shouldFailClearAnnotationForIncidentWithNullId() {
    // when & then
    assertThatThrownBy(() -> runtimeService.clearAnnotationForIncidentById(null))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("incident id");
  }

  @Test
  public void shouldFailClearAnnotationForIncidentWithNonExistingIncidentId() {
    // when & then
    assertThatThrownBy(() -> runtimeService.clearAnnotationForIncidentById("not existing"))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("incident");
  }

  protected Incident createIncident() {
    String key = "process";
    BpmnModelInstance model = Bpmn.createExecutableProcess(key)
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .serviceTask("theServiceTask")
        .operatonClass(AlwaysFailingDelegate.class)
        .operatonAsyncBefore()
        .operatonFailedJobRetryTimeCycle("R0/PT30S")
        .endEvent()
        .done();

    testRule.deploy(model);

    runtimeService.startProcessInstanceByKey(key);
    testRule.executeAvailableJobs();
    return runtimeService.createIncidentQuery().singleResult();
  }

  protected String createStandaloneIncident() {
    repositoryService.suspendProcessDefinitionByKey("process", true, new Date());
    String jobId = null;
    List<Job> jobs = managementService.createJobQuery().list();
    for (Job job : jobs) {
      if (job.getProcessDefinitionKey() == null) {
        jobId = job.getId();
        break;
      }
    }
    managementService.setJobRetries(jobId, 0);
    return jobId;
  }

  protected void cleanupStandaloneIncident(String jobId) {
    managementService.deleteJob(jobId);
    Removable.of(processEngine).remove(HistoricIncident.class);
  }
}
