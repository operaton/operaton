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
package org.operaton.bpm.engine.test.api.runtime.migration;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.SetProcessDefinitionVersionCmd;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.MessageJobDeclaration;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


/**
 * @author Falko Menge
 */
class SetProcessDefinitionVersionCmdTest {

  private static final String TEST_PROCESS_WITH_PARALLEL_GATEWAY = "org/operaton/bpm/engine/test/bpmn/gateway/ParallelGatewayTest.testForkJoin.bpmn20.xml";
  private static final String TEST_PROCESS = "org/operaton/bpm/engine/test/api/runtime/migration/SetProcessDefinitionVersionCmdTest.testSetProcessDefinitionVersion.bpmn20.xml";
  private static final String TEST_PROCESS_ACTIVITY_MISSING = "org/operaton/bpm/engine/test/api/runtime/migration/SetProcessDefinitionVersionCmdTest.testSetProcessDefinitionVersionActivityMissing.bpmn20.xml";

  private static final String TEST_PROCESS_CALL_ACTIVITY = "org/operaton/bpm/engine/test/api/runtime/migration/SetProcessDefinitionVersionCmdTest.withCallActivity.bpmn20.xml";
  private static final String TEST_PROCESS_USER_TASK_V1 = "org/operaton/bpm/engine/test/api/runtime/migration/SetProcessDefinitionVersionCmdTest.testSetProcessDefinitionVersionWithTask.bpmn20.xml";
  private static final String TEST_PROCESS_USER_TASK_V2 = "org/operaton/bpm/engine/test/api/runtime/migration/SetProcessDefinitionVersionCmdTest.testSetProcessDefinitionVersionWithTaskV2.bpmn20.xml";

  private static final String TEST_PROCESS_SERVICE_TASK_V1 = "org/operaton/bpm/engine/test/api/runtime/migration/SetProcessDefinitionVersionCmdTest.testSetProcessDefinitionVersionWithServiceTask.bpmn20.xml";
  private static final String TEST_PROCESS_SERVICE_TASK_V2 = "org/operaton/bpm/engine/test/api/runtime/migration/SetProcessDefinitionVersionCmdTest.testSetProcessDefinitionVersionWithServiceTaskV2.bpmn20.xml";

  private static final String TEST_PROCESS_WITH_MULTIPLE_PARENTS = "org/operaton/bpm/engine/test/api/runtime/migration/SetProcessDefinitionVersionCmdTest.testSetProcessDefinitionVersionWithMultipleParents.bpmn";

  private static final String TEST_PROCESS_ONE_JOB = "org/operaton/bpm/engine/test/api/runtime/migration/SetProcessDefinitionVersionCmdTest.oneJobProcess.bpmn20.xml";
  private static final String TEST_PROCESS_TWO_JOBS = "org/operaton/bpm/engine/test/api/runtime/migration/SetProcessDefinitionVersionCmdTest.twoJobsProcess.bpmn20.xml";

  private static final String TEST_PROCESS_ATTACHED_TIMER = "org/operaton/bpm/engine/test/api/runtime/migration/SetProcessDefinitionVersionCmdTest.testAttachedTimer.bpmn20.xml";

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(rule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  RepositoryService repositoryService;
  HistoryService historyService;
  TaskService taskService;
  FormService formService;
  ManagementService managementService;
  IdentityService identityService;

  @Test
  void testSetProcessDefinitionVersionEmptyArguments() {
    try {
      new SetProcessDefinitionVersionCmd(null, 23);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("The process instance id is mandatory: processInstanceId is null", ae.getMessage());
    }

    try {
      new SetProcessDefinitionVersionCmd("", 23);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
       testRule.assertTextPresent("The process instance id is mandatory: processInstanceId is empty", ae.getMessage());
    }

    try {
      new SetProcessDefinitionVersionCmd("42", null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
       testRule.assertTextPresent("The process definition version is mandatory: processDefinitionVersion is null", ae.getMessage());
    }

    try {
      new SetProcessDefinitionVersionCmd("42", -1);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
       testRule.assertTextPresent("The process definition version must be positive: processDefinitionVersion is not greater than 0", ae.getMessage());
    }
  }

  @Test
  void testSetProcessDefinitionVersionNonExistingPI() {
    // given
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    var setProcessDefinitionVersionCmd = new SetProcessDefinitionVersionCmd("42", 23);
    try {
      commandExecutor.execute(setProcessDefinitionVersionCmd);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
       testRule.assertTextPresent("No process instance found for id = '42'.", ae.getMessage());
    }
  }

  @Deployment(resources = {TEST_PROCESS_WITH_PARALLEL_GATEWAY})
  @Test
  void testSetProcessDefinitionVersionPIIsSubExecution() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("forkJoin");

    Execution execution = runtimeService.createExecutionQuery()
      .activityId("receivePayment")
      .singleResult();
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    SetProcessDefinitionVersionCmd command = new SetProcessDefinitionVersionCmd(execution.getId(), 1);
    try {
      commandExecutor.execute(command);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
       testRule.assertTextPresent("A process instance id is required, but the provided id '"+execution.getId()+"' points to a child execution of process instance '"+pi.getId()+"'. Please invoke the "+command.getClass().getSimpleName()+" with a root execution id.", ae.getMessage());
    }
  }

  @Deployment(resources = {TEST_PROCESS})
  @Test
  void testSetProcessDefinitionVersionNonExistingPD() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("receiveTask");

    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    var setProcessDefinitionVersionCmd = new SetProcessDefinitionVersionCmd(pi.getId(), 23);

    try {
      commandExecutor.execute(setProcessDefinitionVersionCmd);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
       testRule.assertTextPresent("no processes deployed with key = 'receiveTask', version = '23'", ae.getMessage());
    }
  }

  @Deployment(resources = {TEST_PROCESS})
  @Test
  void testSetProcessDefinitionVersionActivityMissing() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("receiveTask");

    // check that receive task has been reached
    Execution execution = runtimeService.createExecutionQuery()
      .activityId("waitState1")
      .singleResult();
    assertThat(execution).isNotNull();

    // deploy new version of the process definition
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService
      .createDeployment()
      .addClasspathResource(TEST_PROCESS_ACTIVITY_MISSING)
      .deploy();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);

    // migrate process instance to new process definition version
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    SetProcessDefinitionVersionCmd setProcessDefinitionVersionCmd = new SetProcessDefinitionVersionCmd(pi.getId(), 2);
    try {
      commandExecutor.execute(setProcessDefinitionVersionCmd);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
       testRule.assertTextPresent("The new process definition (key = 'receiveTask') does not contain the current activity 'waitState1' of the process instance '", ae.getMessage());
    }

    // undeploy "manually" deployed process definition
    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Deployment
  @Test
  void testSetProcessDefinitionVersion() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("receiveTask");

    // check that receive task has been reached
    Execution execution = runtimeService.createExecutionQuery()
      .processInstanceId(pi.getId())
      .activityId("waitState1")
      .singleResult();
    assertThat(execution).isNotNull();

    // deploy new version of the process definition
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService
      .createDeployment()
      .addClasspathResource(TEST_PROCESS)
      .deploy();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);

    // migrate process instance to new process definition version
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(new SetProcessDefinitionVersionCmd(pi.getId(), 2));

    // signal process instance
    runtimeService.signal(execution.getId());

    // check that the instance now uses the new process definition version
    ProcessDefinition newProcessDefinition = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionVersion(2)
      .singleResult();
    pi = runtimeService
      .createProcessInstanceQuery()
      .processInstanceId(pi.getId())
      .singleResult();
    assertThat(pi.getProcessDefinitionId()).isEqualTo(newProcessDefinition.getId());

    // check history
    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      HistoricProcessInstance historicPI = historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceId(pi.getId())
        .singleResult();

      assertThat(historicPI.getProcessDefinitionId()).isEqualTo(newProcessDefinition.getId());
    }

    // undeploy "manually" deployed process definition
    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Deployment(resources = {TEST_PROCESS_WITH_PARALLEL_GATEWAY})
  @Test
  void testSetProcessDefinitionVersionSubExecutions() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("forkJoin");

    // check that the user tasks have been reached
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // deploy new version of the process definition
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService
      .createDeployment()
      .addClasspathResource(TEST_PROCESS_WITH_PARALLEL_GATEWAY)
      .deploy();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);

    // migrate process instance to new process definition version
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(new SetProcessDefinitionVersionCmd(pi.getId(), 2));

    // check that all executions of the instance now use the new process definition version
    ProcessDefinition newProcessDefinition = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionVersion(2)
      .singleResult();
    List<Execution> executions = runtimeService
      .createExecutionQuery()
      .processInstanceId(pi.getId())
      .list();
    for (Execution execution : executions) {
      assertThat(((ExecutionEntity) execution).getProcessDefinitionId()).isEqualTo(newProcessDefinition.getId());
    }

    // undeploy "manually" deployed process definition
    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Deployment(resources = {TEST_PROCESS_CALL_ACTIVITY})
  @Test
  void testSetProcessDefinitionVersionWithCallActivity() {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("parentProcess");

    // check that receive task has been reached
    Execution execution = runtimeService.createExecutionQuery()
      .activityId("waitState1")
      .processDefinitionKey("childProcess")
      .singleResult();
    assertThat(execution).isNotNull();

    // deploy new version of the process definition
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService
      .createDeployment()
      .addClasspathResource(TEST_PROCESS_CALL_ACTIVITY)
      .deploy();
    assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionKey("parentProcess").count()).isEqualTo(2);

    // migrate process instance to new process definition version
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(new SetProcessDefinitionVersionCmd(pi.getId(), 2));

    // signal process instance
    runtimeService.signal(execution.getId());

    // should be finished now
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).count()).isZero();

    // undeploy "manually" deployed process definition
    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Deployment(resources = {TEST_PROCESS_USER_TASK_V1})
  @Test
  void testSetProcessDefinitionVersionWithWithTask() {
    try {
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("userTask");

      // check that user task has been reached
      assertThat(taskService.createTaskQuery().processInstanceId(pi.getId()).count()).isOne();

    // deploy new version of the process definition
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService
      .createDeployment()
      .addClasspathResource(TEST_PROCESS_USER_TASK_V2)
      .deploy();
      assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionKey("userTask").count()).isEqualTo(2);

    ProcessDefinition newProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("userTask").processDefinitionVersion(2).singleResult();

    // migrate process instance to new process definition version
    processEngineConfiguration.getCommandExecutorTxRequired().execute(new SetProcessDefinitionVersionCmd(pi.getId(), 2));

    // check UserTask
    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
      assertThat(task.getProcessDefinitionId()).isEqualTo(newProcessDefinition.getId());
      assertThat(formService.getTaskFormData(task.getId()).getFormKey()).isEqualTo("testFormKey");

    // continue
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());

    // undeploy "manually" deployed process definition
    repositoryService.deleteDeployment(deployment.getId(), true);
    }
    catch (Exception ex) {
     ex.printStackTrace();
    }
  }

  @Deployment(resources = TEST_PROCESS_SERVICE_TASK_V1)
  @Test
  void testSetProcessDefinitionVersionWithFollowUpTask() {
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    String secondDeploymentId =
        repositoryService.createDeployment().addClasspathResource(TEST_PROCESS_SERVICE_TASK_V2).deploy().getId();

    runtimeService.startProcessInstanceById(processDefinitionId);

    // execute job that triggers the migrating service task
    Job migrationJob = managementService.createJobQuery().singleResult();
    assertThat(migrationJob).isNotNull();

    managementService.executeJob(migrationJob.getId());

    Task followUpTask = taskService.createTaskQuery().singleResult();

    assertThat(followUpTask).as("Should have migrated to the new version and immediately executed the correct follow-up activity").isNotNull();

    repositoryService.deleteDeployment(secondDeploymentId, true);
  }

  @Deployment(resources = {TEST_PROCESS_WITH_MULTIPLE_PARENTS})
  @Test
  void testSetProcessDefinitionVersionWithMultipleParents(){
    // start process instance
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("multipleJoins");

    // check that the user tasks have been reached
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    //finish task1
    Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
    taskService.complete(task.getId());

    //we have reached task4
    task = taskService.createTaskQuery().taskDefinitionKey("task4").singleResult();
    assertThat(task).isNotNull();

    //The timer job has been created
    Job job = managementService.createJobQuery().executionId(task.getExecutionId()).singleResult();
    assertThat(job).isNotNull();

    // check there are 2 user tasks task4 and task2
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // deploy new version of the process definition
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService
      .createDeployment()
      .addClasspathResource(TEST_PROCESS_WITH_MULTIPLE_PARENTS)
      .deploy();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);

    // migrate process instance to new process definition version
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(new SetProcessDefinitionVersionCmd(pi.getId(), 2));

    // check that all executions of the instance now use the new process definition version
    ProcessDefinition newProcessDefinition = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionVersion(2)
      .singleResult();
    List<Execution> executions = runtimeService
      .createExecutionQuery()
      .processInstanceId(pi.getId())
      .list();
    for (Execution execution : executions) {
      assertThat(((ExecutionEntity) execution).getProcessDefinitionId()).isEqualTo(newProcessDefinition.getId());
    }

    // undeploy "manually" deployed process definition
  	repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Deployment(resources = TEST_PROCESS_ONE_JOB)
  @Test
  void testSetProcessDefinitionVersionMigrateJob() {
    // given a process instance
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneJobProcess");

    // with a job
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    // and a second deployment of the process
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService
      .createDeployment()
      .addClasspathResource(TEST_PROCESS_ONE_JOB)
      .deploy();

    ProcessDefinition newDefinition =
        repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
    assertThat(newDefinition).isNotNull();

    // when the process instance is migrated
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(new SetProcessDefinitionVersionCmd(instance.getId(), 2));

    // then the job should also be migrated
    Job migratedJob = managementService.createJobQuery().singleResult();
    assertThat(migratedJob).isNotNull();
    assertThat(migratedJob.getId()).isEqualTo(job.getId());
    assertThat(migratedJob.getProcessDefinitionId()).isEqualTo(newDefinition.getId());
    assertThat(migratedJob.getDeploymentId()).isEqualTo(deployment.getId());

    JobDefinition newJobDefinition = managementService
        .createJobDefinitionQuery().processDefinitionId(newDefinition.getId()).singleResult();
    assertThat(newJobDefinition).isNotNull();
    assertThat(migratedJob.getJobDefinitionId()).isEqualTo(newJobDefinition.getId());

    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Deployment(resources = TEST_PROCESS_TWO_JOBS)
  @Test
  void testMigrateJobWithMultipleDefinitionsOnActivity() {
    // given a process instance
    ProcessInstance asyncAfterInstance = runtimeService.startProcessInstanceByKey("twoJobsProcess");

    // with an async after job
    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.executeJob(jobId);
    Job asyncAfterJob = managementService.createJobQuery().singleResult();

    // and a process instance with a before after job
    ProcessInstance asyncBeforeInstance = runtimeService.startProcessInstanceByKey("twoJobsProcess");
    Job asyncBeforeJob = managementService.createJobQuery()
        .processInstanceId(asyncBeforeInstance.getId()).singleResult();

    // and a second deployment of the process
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService
      .createDeployment()
      .addClasspathResource(TEST_PROCESS_TWO_JOBS)
      .deploy();

    ProcessDefinition newDefinition =
        repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
    assertThat(newDefinition).isNotNull();

    JobDefinition asnycBeforeJobDefinition =
        managementService.createJobDefinitionQuery()
          .jobConfiguration(MessageJobDeclaration.ASYNC_BEFORE)
          .processDefinitionId(newDefinition.getId())
          .singleResult();
    JobDefinition asnycAfterJobDefinition =
        managementService.createJobDefinitionQuery()
          .jobConfiguration(MessageJobDeclaration.ASYNC_AFTER)
          .processDefinitionId(newDefinition.getId())
          .singleResult();

    assertThat(asnycBeforeJobDefinition).isNotNull();
    assertThat(asnycAfterJobDefinition).isNotNull();

    // when the process instances are migrated
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(new SetProcessDefinitionVersionCmd(asyncBeforeInstance.getId(), 2));
    commandExecutor.execute(new SetProcessDefinitionVersionCmd(asyncAfterInstance.getId(), 2));

    // then the job's definition reference should also be migrated
    Job migratedAsyncBeforeJob = managementService.createJobQuery()
        .processInstanceId(asyncBeforeInstance.getId()).singleResult();
    assertThat(migratedAsyncBeforeJob.getId()).isEqualTo(asyncBeforeJob.getId());
    assertThat(migratedAsyncBeforeJob).isNotNull();
    assertThat(migratedAsyncBeforeJob.getJobDefinitionId()).isEqualTo(asnycBeforeJobDefinition.getId());

    Job migratedAsyncAfterJob = managementService.createJobQuery()
        .processInstanceId(asyncAfterInstance.getId()).singleResult();
    assertThat(migratedAsyncAfterJob.getId()).isEqualTo(asyncAfterJob.getId());
    assertThat(migratedAsyncAfterJob).isNotNull();
    assertThat(migratedAsyncAfterJob.getJobDefinitionId()).isEqualTo(asnycAfterJobDefinition.getId());

    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Deployment(resources = TEST_PROCESS_ONE_JOB)
  @Test
  void testSetProcessDefinitionVersionMigrateIncident() {
    // given a process instance
    ProcessInstance instance =
        runtimeService.startProcessInstanceByKey("oneJobProcess", Variables.createVariables().putValue("shouldFail", true));

    // with a failed job
    testRule.executeAvailableJobs();

    // and an incident
    Incident incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident).isNotNull();

    // and a second deployment of the process
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService
      .createDeployment()
      .addClasspathResource(TEST_PROCESS_ONE_JOB)
      .deploy();

    ProcessDefinition newDefinition =
        repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
    assertThat(newDefinition).isNotNull();

    // when the process instance is migrated
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(new SetProcessDefinitionVersionCmd(instance.getId(), 2));

    // then the incident should also be migrated
    Incident migratedIncident = runtimeService.createIncidentQuery().singleResult();
    assertThat(migratedIncident).isNotNull();
    assertThat(migratedIncident.getProcessDefinitionId()).isEqualTo(newDefinition.getId());
    assertThat(migratedIncident.getProcessInstanceId()).isEqualTo(instance.getId());
    assertThat(migratedIncident.getExecutionId()).isEqualTo(instance.getId());

    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  /**
   * See https://app.camunda.com/jira/browse/CAM-9505
   */
  @Deployment(resources = TEST_PROCESS_ONE_JOB)
  @Test
  void testPreserveTimestampOnUpdatedIncident() {
    // given
    ProcessInstance instance =
        runtimeService.startProcessInstanceByKey("oneJobProcess", Variables.createVariables().putValue("shouldFail", true));

    testRule.executeAvailableJobs();

    Incident incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident).isNotNull();

    Date timestamp = incident.getIncidentTimestamp();

    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService
      .createDeployment()
      .addClasspathResource(TEST_PROCESS_ONE_JOB)
      .deploy();

    ProcessDefinition newDefinition =
        repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
    assertThat(newDefinition).isNotNull();

    // when
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(new SetProcessDefinitionVersionCmd(instance.getId(), 2));

    Incident migratedIncident = runtimeService.createIncidentQuery().singleResult();

    // then
    assertThat(migratedIncident.getIncidentTimestamp()).isEqualTo(timestamp);

    // cleanup
    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Deployment(resources = TEST_PROCESS_ATTACHED_TIMER)
  @Test
  void testSetProcessDefinitionVersionAttachedTimer() {
    // given a process instance
    ProcessInstance instance =
        runtimeService.startProcessInstanceByKey("attachedTimer");

    // and a second deployment of the process
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService
      .createDeployment()
      .addClasspathResource(TEST_PROCESS_ATTACHED_TIMER)
      .deploy();

    ProcessDefinition newDefinition =
        repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
    assertThat(newDefinition).isNotNull();

    // when the process instance is migrated
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(new SetProcessDefinitionVersionCmd(instance.getId(), 2));

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getProcessDefinitionId()).isEqualTo(newDefinition.getId());

    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Test
  void testHistoryOfSetProcessDefinitionVersionCmd() {
    // given
    String resource = "org/operaton/bpm/engine/test/api/runtime/migration/SetProcessDefinitionVersionCmdTest.bpmn";

    // Deployments
    org.operaton.bpm.engine.repository.Deployment firstDeployment = repositoryService
        .createDeployment()
        .addClasspathResource(resource)
        .deploy();

    org.operaton.bpm.engine.repository.Deployment secondDeployment = repositoryService
        .createDeployment()
        .addClasspathResource(resource)
        .deploy();

    // Process definitions
    ProcessDefinition processDefinitionV1 = repositoryService
        .createProcessDefinitionQuery()
        .deploymentId(firstDeployment.getId())
        .singleResult();

    ProcessDefinition processDefinitionV2 = repositoryService
        .createProcessDefinitionQuery()
        .deploymentId(secondDeployment.getId())
        .singleResult();

    // start process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinitionV1.getId());

    // when
    setProcessDefinitionVersion(processInstance.getId(), 2);

    // then
    ProcessInstance processInstanceAfterMigration = runtimeService
        .createProcessInstanceQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();
    assertThat(processInstanceAfterMigration.getProcessDefinitionId()).isEqualTo(processDefinitionV2.getId());

    if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      HistoricProcessInstance historicProcessInstance = historyService
          .createHistoricProcessInstanceQuery()
          .processInstanceId(processInstance.getId())
          .singleResult();
      assertThat(historicProcessInstance.getProcessDefinitionId()).isEqualTo(processDefinitionV2.getId());
    }

    // Clean up the test
    repositoryService.deleteDeployment(firstDeployment.getId(), true);
    repositoryService.deleteDeployment(secondDeployment.getId(), true);
  }

  @Test
  void testOpLogSetProcessDefinitionVersionCmd() {
    // given
    try {
      identityService.setAuthenticatedUserId("demo");
      String resource = "org/operaton/bpm/engine/test/api/runtime/migration/SetProcessDefinitionVersionCmdTest.bpmn";

      // Deployments
      org.operaton.bpm.engine.repository.Deployment firstDeployment = repositoryService
          .createDeployment()
          .addClasspathResource(resource)
          .deploy();

      org.operaton.bpm.engine.repository.Deployment secondDeployment = repositoryService
          .createDeployment()
          .addClasspathResource(resource)
          .deploy();

      // Process definitions
      ProcessDefinition processDefinitionV1 = repositoryService
          .createProcessDefinitionQuery()
          .deploymentId(firstDeployment.getId())
          .singleResult();

      ProcessDefinition processDefinitionV2 = repositoryService
          .createProcessDefinitionQuery()
          .deploymentId(secondDeployment.getId())
          .singleResult();

      // start process instance
      ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinitionV1.getId());

      // when
      setProcessDefinitionVersion(processInstance.getId(), 2);

      // then
      ProcessInstance processInstanceAfterMigration = runtimeService
          .createProcessInstanceQuery()
          .processInstanceId(processInstance.getId())
          .singleResult();
      assertThat(processInstanceAfterMigration.getProcessDefinitionId()).isEqualTo(processDefinitionV2.getId());

      if (processEngineConfiguration.getHistoryLevel().equals(HistoryLevel.HISTORY_LEVEL_FULL)) {
        List<UserOperationLogEntry> userOperations = historyService
            .createUserOperationLogQuery()
            .processInstanceId(processInstance.getId())
            .operationType(UserOperationLogEntry.OPERATION_TYPE_MODIFY_PROCESS_INSTANCE)
            .list();

        assertThat(userOperations).hasSize(1);

        UserOperationLogEntry userOperationLogEntry = userOperations.get(0);
        assertThat(userOperationLogEntry.getProperty()).isEqualTo("processDefinitionVersion");
        assertThat(userOperationLogEntry.getOrgValue()).isEqualTo("1");
        assertThat(userOperationLogEntry.getNewValue()).isEqualTo("2");
      }

      // Clean up the test
      repositoryService.deleteDeployment(firstDeployment.getId(), true);
      repositoryService.deleteDeployment(secondDeployment.getId(), true);
    } finally {
      identityService.clearAuthentication();
    }
  }

  protected void setProcessDefinitionVersion(String processInstanceId, int newProcessDefinitionVersion) {
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequiresNew();
    commandExecutor.execute(new SetProcessDefinitionVersionCmd(processInstanceId, newProcessDefinitionVersion));
  }
}
