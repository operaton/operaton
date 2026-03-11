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
package org.operaton.bpm.engine.test.api.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.SuspendedEntityInteractionException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.TimerActivateProcessDefinitionHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerSuspendProcessDefinitionHandler;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Daniel Meyer
 * @author Joram Barrez
 */
class ProcessDefinitionSuspensionTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RepositoryService repositoryService;
  RuntimeService runtimeService;
  FormService formService;
  TaskService taskService;
  ManagementService managementService;

  @AfterEach
  void tearDown() {
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerActivateProcessDefinitionHandler.TYPE);
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerSuspendProcessDefinitionHandler.TYPE);
      return null;
    });
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml"})
  @Test
  void testProcessDefinitionActiveByDefault() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    assertThat(processDefinition.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml"})
  @Test
  void testSuspendActivateProcessDefinitionById() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();

    // suspend
    repositoryService.suspendProcessDefinitionById(processDefinition.getId());
    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isTrue();

    // activate
    repositoryService.activateProcessDefinitionById(processDefinition.getId());
    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml"})
  @Test
  void testSuspendActivateProcessDefinitionByKey() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();

    //suspend
    repositoryService.suspendProcessDefinitionByKey(processDefinition.getKey());
    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isTrue();

    //activate
    repositoryService.activateProcessDefinitionByKey(processDefinition.getKey());
    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml"})
  @Test
  void testActivateAlreadyActiveProcessDefinition() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();

    repositoryService.activateProcessDefinitionById(processDefinition.getId());
    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml"})
  @Test
  void testSuspendAlreadySuspendedProcessDefinition() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();

    repositoryService.suspendProcessDefinitionById(processDefinition.getId());

    repositoryService.suspendProcessDefinitionById(processDefinition.getId());
    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isTrue();

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/repository/processTwo.bpmn20.xml"
  })
  @Test
  void testQueryForActiveDefinitions() {

    // default = all definitions
    List<ProcessDefinition> processDefinitionList = repositoryService.createProcessDefinitionQuery()
      .list();
    assertThat(processDefinitionList).hasSize(2);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isEqualTo(2);

    ProcessDefinition processDefinition = processDefinitionList.get(0);
    repositoryService.suspendProcessDefinitionById(processDefinition.getId());

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isOne();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/repository/processTwo.bpmn20.xml"
  })
  @Test
  void testQueryForSuspendedDefinitions() {

    // default = all definitions
    List<ProcessDefinition> processDefinitionList = repositoryService.createProcessDefinitionQuery()
      .list();
    assertThat(processDefinitionList).hasSize(2);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isEqualTo(2);

    ProcessDefinition processDefinition = processDefinitionList.get(0);
    repositoryService.suspendProcessDefinitionById(processDefinition.getId());

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml"})
  @Test
  void testStartProcessInstanceForSuspendedProcessDefinition() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    var processDefinitionId = processDefinition.getId();
    var processDefinitionKey = processDefinition.getKey();
    repositoryService.suspendProcessDefinitionById(processDefinitionId);

    // By id
    assertThatThrownBy(() -> runtimeService.startProcessInstanceById(processDefinitionId))
        .isInstanceOf(SuspendedEntityInteractionException.class)
        .satisfies(e -> assertThat(e.getMessage().toLowerCase()).contains("is suspended"));

    // By Key
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey(processDefinitionKey))
        .isInstanceOf(SuspendedEntityInteractionException.class)
        .satisfies(e -> assertThat(e.getMessage().toLowerCase()).contains("is suspended"));
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testContinueProcessAfterProcessDefinitionSuspend() {

    // Start Process Instance
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(processDefinition.getKey());

    // Verify one task is created
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    // Suspend process definition
    repositoryService.suspendProcessDefinitionById(processDefinition.getId());

    // Process should be able to continue
    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSuspendProcessInstancesDuringProcessDefinitionSuspend() {

    int nrOfProcessInstances = 9;

    // Fire up a few processes for the deployed process definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    for (int i=0; i<nrOfProcessInstances; i++) {
      runtimeService.startProcessInstanceByKey(processDefinition.getKey());
    }
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(nrOfProcessInstances);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(nrOfProcessInstances);

    // Suspend process definitions and include process instances
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);

    // Verify all process instances are also suspended
    for (ProcessInstance processInstance : runtimeService.createProcessInstanceQuery().list()) {
      assertThat(processInstance.isSuspended()).isTrue();
    }

    // Verify all process instances can't be continued
    for (Task task : taskService.createTaskQuery().list()) {
      assertThat(task.isSuspended()).isTrue();
      var taskId = task.getId();
      assertThatThrownBy(() -> taskService.complete(taskId))
          .isInstanceOf(SuspendedEntityInteractionException.class)
          .satisfies(e -> assertThat(e.getMessage().toLowerCase()).contains("is suspended"));
    }
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(nrOfProcessInstances);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isEqualTo(nrOfProcessInstances);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isZero();

    // Activate the process definition again
    repositoryService.activateProcessDefinitionById(processDefinition.getId(), true, null);

    // Verify that all process instances can be completed
    for (Task task : taskService.createTaskQuery().list()) {
      taskService.complete(task.getId());
    }
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSubmitStartFormAfterProcessDefinitionSuspend() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    var processDefinitionId = processDefinition.getId();
    repositoryService.suspendProcessDefinitionById(processDefinitionId);

    var emptyProperties = new HashMap<String,Object>();
    assertThatThrownBy(() -> formService.submitStartForm(processDefinitionId, emptyProperties))
        .isInstanceOf(ProcessEngineException.class)
        .satisfies(e -> assertThat(e.getMessage().toLowerCase()).contains("is suspended"));

    assertThatThrownBy(() -> formService.submitStartForm(processDefinitionId, "someKey", emptyProperties))
        .isInstanceOf(ProcessEngineException.class)
        .satisfies(e -> assertThat(e.getMessage().toLowerCase()).contains("is suspended"));

  }

  @Deployment
  @Test
  void testJobIsExecutedOnProcessDefinitionSuspend() {

    Date now = new Date();
    ClockUtil.setCurrentTime(now);

    // Suspending the process definition should not stop the execution of jobs
    // Added this test because in previous implementations, this was the case.
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());
    repositoryService.suspendProcessDefinitionById(processDefinition.getId());
    assertThat(managementService.createJobQuery().count()).isOne();

    // The jobs should simply be executed
    Job job = managementService.createJobQuery().singleResult();
    managementService.executeJob(job.getId());
    assertThat(managementService.createJobQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDelayedSuspendProcessDefinition() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    var processDefinitionId = processDefinition.getId();
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);

    // Suspend process definition in one week from now
    long oneWeekFromStartTime = startTime.getTime() + (7 * 24 * 60 * 60 * 1000);
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), false, new Date(oneWeekFromStartTime));

    // Verify we can just start process instances
    runtimeService.startProcessInstanceById(processDefinitionId);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isOne();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isZero();

    // execute job
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
    managementService.executeJob(job.getId());

    // Try to start process instance. It should fail now.
    assertThatThrownBy(() -> runtimeService.startProcessInstanceById(processDefinitionId))
        .isInstanceOf(SuspendedEntityInteractionException.class)
        .satisfies(e -> assertThat(e.getMessage().toLowerCase()).contains("suspended"));
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();

    // Activate again
    repositoryService.activateProcessDefinitionById(processDefinitionId);
    runtimeService.startProcessInstanceById(processDefinitionId);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isOne();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDelayedSuspendProcessDefinitionIncludingProcessInstances() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    var processDefinitionId = processDefinition.getId();
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);

    // Start some process instances
    int nrOfProcessInstances = 30;
    for (int i=0; i<nrOfProcessInstances; i++) {
      runtimeService.startProcessInstanceById(processDefinition.getId());
    }

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(nrOfProcessInstances);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(nrOfProcessInstances);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isZero();
    assertThat(taskService.createTaskQuery().suspended().count()).isZero();
    assertThat(taskService.createTaskQuery().active().count()).isEqualTo(nrOfProcessInstances);
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isOne();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isZero();

    // Suspend process definition in one week from now
    long oneWeekFromStartTime = startTime.getTime() + (7 * 24 * 60 * 60 * 1000);
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, new Date(oneWeekFromStartTime));

    // Verify we can start process instances
    runtimeService.startProcessInstanceById(processDefinition.getId());
    nrOfProcessInstances = nrOfProcessInstances + 1;
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(nrOfProcessInstances);

    // execute job
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
    managementService.executeJob(job.getId());

    // Try to start process instance. It should fail now.
    assertThatThrownBy(() -> runtimeService.startProcessInstanceById(processDefinitionId))
        .isInstanceOf(SuspendedEntityInteractionException.class)
        .satisfies(e -> assertThat(e.getMessage().toLowerCase()).contains("suspended"));
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(nrOfProcessInstances);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isEqualTo(nrOfProcessInstances);
    assertThat(taskService.createTaskQuery().suspended().count()).isEqualTo(nrOfProcessInstances);
    assertThat(taskService.createTaskQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();

    // Activate again
    repositoryService.activateProcessDefinitionById(processDefinition.getId(), true, null);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(nrOfProcessInstances);
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(nrOfProcessInstances);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isZero();
    assertThat(taskService.createTaskQuery().suspended().count()).isZero();
    assertThat(taskService.createTaskQuery().active().count()).isEqualTo(nrOfProcessInstances);
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isOne();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDelayedActivateProcessDefinition() {

    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    var processDefinitionId = processDefinition.getId();
    repositoryService.suspendProcessDefinitionById(processDefinitionId);

    // Try to start process instance. It should fail now.
    assertThatThrownBy(() -> runtimeService.startProcessInstanceById(processDefinitionId))
        .isInstanceOf(SuspendedEntityInteractionException.class)
        .satisfies(e -> assertThat(e.getMessage().toLowerCase()).contains("suspended"));
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();

    // Activate in a day from now
    long oneDayFromStart = startTime.getTime() + (24 * 60 * 60 * 1000);
    repositoryService.activateProcessDefinitionById(processDefinitionId, false, new Date(oneDayFromStart));

    // execute job
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
    managementService.executeJob(job.getId());

    // Starting a process instance should now succeed
    runtimeService.startProcessInstanceById(processDefinitionId);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isOne();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isZero();
  }

  @Test
  void testSuspendMultipleProcessDefinitionsByKey() {

    // Deploy three processes
    int nrOfProcessDefinitions = 3;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml").deploy();
    }
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isZero();

    // Suspend all process definitions with same key
    repositoryService.suspendProcessDefinitionByKey("oneTaskProcess");
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isEqualTo(nrOfProcessDefinitions);

    // Activate again
    repositoryService.activateProcessDefinitionByKey("oneTaskProcess");
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isZero();

    // Start process instance
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // And suspend again, cascading to process instances
    repositoryService.suspendProcessDefinitionByKey("oneTaskProcess", true, null);
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isOne();
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  void testDelayedSuspendMultipleProcessDefinitionsByKey() {

    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    final long hourInMs = 60 * 60 * 1000;

    // Deploy five versions of the same process
    int nrOfProcessDefinitions = 5;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml").deploy();
    }
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isZero();

    // Start process instance
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // Suspend all process definitions with same key in 2 hours from now
    repositoryService.suspendProcessDefinitionByKey("oneTaskProcess", true, new Date(startTime.getTime() + (2 * hourInMs)));
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isOne();

    // the job is associated with the deployment id of the latest version of the process definition
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    ProcessDefinitionQuery latestProcessDefinitionQuery = repositoryService.createProcessDefinitionQuery()
        .orderByProcessDefinitionVersion().desc();
    ProcessDefinition latestProcessDefinition = latestProcessDefinitionQuery.list().get(0);
    assertThat(job.getDeploymentId()).isEqualTo(latestProcessDefinition.getDeploymentId());
    // execute job
    managementService.executeJob(job.getId());

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isOne();

    // Activate again in 5 hours from now
    repositoryService.activateProcessDefinitionByKey("oneTaskProcess", true, new Date(startTime.getTime() + (5 * hourInMs)));
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(runtimeService.createProcessInstanceQuery().suspended().count()).isOne();

    // the job is associated with the deployment id of the latest version of the process definition
    job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    latestProcessDefinition = latestProcessDefinitionQuery.list().get(0);
    assertThat(job.getDeploymentId()).isEqualTo(latestProcessDefinition.getDeploymentId());
    // execute job
    managementService.executeJob(job.getId());

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isEqualTo(nrOfProcessDefinitions);
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isZero();
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isOne();

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testSuspendById_shouldSuspendJobDefinitionAndRetainJob() {
    // given

    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // when
    // the process definition will be suspended
    repositoryService.suspendProcessDefinitionById(processDefinition.getId());

    // then
    // the job definition should be suspended...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // ...and the corresponding job should be still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job job = jobQuery.active().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testSuspendByKey_shouldSuspendJobDefinitionAndRetainJob() {
    // given

    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // when
    // the process definition will be suspended
    repositoryService.suspendProcessDefinitionByKey(processDefinition.getKey());

    // then
    // the job definition should be suspended...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // ...and the corresponding job should be still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job job = jobQuery.active().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testSuspendByIdAndIncludeInstancesFlag_shouldSuspendAlsoJobDefinitionAndRetainJob() {
    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // when
    // the process definition will be suspended
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), false, null);

    // then
    // the job definition should be suspended...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // ...and the corresponding job should be still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job job = jobQuery.active().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testSuspendByKeyAndIncludeInstancesFlag_shouldSuspendAlsoJobDefinitionAndRetainJob() {
    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // when
    // the process definition will be suspended
    repositoryService.suspendProcessDefinitionByKey(processDefinition.getKey(), false, null);

    // then
    // the job definition should be suspended...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // ...and the corresponding job should be still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job job = jobQuery.active().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testSuspendByIdAndIncludeInstancesFlag_shouldSuspendJobDefinitionAndJob() {
    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // when
    // the process definition will be suspended
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);

    // then
    // the job definition should be suspended...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // ...and the corresponding job should be suspended too
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job job = jobQuery.suspended().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testSuspendByKeyAndIncludeInstancesFlag_shouldSuspendJobDefinitionAndJob() {
    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // when
    // the process definition will be suspended
    repositoryService.suspendProcessDefinitionByKey(processDefinition.getKey(), true, null);

    // then
    // the job definition should be suspended...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // ...and the corresponding job should be suspended too
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job job = jobQuery.suspended().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testDelayedSuspendByIdAndIncludeInstancesFlag_shouldSuspendJobDefinitionAndRetainJob() {
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    final long hourInMs = 60 * 60 * 1000;

    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // when
    // the process definition will be suspended in 2 hours
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), false, new Date(startTime.getTime() + (2 * hourInMs)));

    // then
    // there exists a job to suspend process definition
    Job timerToSuspendProcessDefinition = managementService.createJobQuery().timers().singleResult();
    assertThat(timerToSuspendProcessDefinition).isNotNull();

    // the job definition should still active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the job is still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isEqualTo(2); // there exists two jobs, a failing job and a timer job

    // when
    // execute job
    managementService.executeJob(timerToSuspendProcessDefinition.getId());

    // then
    // the job definition should be suspended
    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the job is still active
    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job job = jobQuery.active().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testDelayedSuspendByKeyAndIncludeInstancesFlag_shouldSuspendJobDefinitionAndRetainJob() {
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    final long hourInMs = 60 * 60 * 1000;

    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // when
    // the process definition will be suspended in 2 hours
    repositoryService.suspendProcessDefinitionByKey(processDefinition.getKey(), false,
            new Date(startTime.getTime() + (2 * hourInMs)));

    // then
    // there exists a job to suspend process definition
    Job timerToSuspendProcessDefinition = managementService.createJobQuery().timers().singleResult();
    assertThat(timerToSuspendProcessDefinition).isNotNull();

    // the job definition should still active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the job is still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isEqualTo(2); // there exists two jobs, a failing job and a timer job

    // when
    // execute job
    managementService.executeJob(timerToSuspendProcessDefinition.getId());

    // then
    // the job definition should be suspended
    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the job is still active
    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job job = jobQuery.active().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testDelayedSuspendByIdAndIncludeInstancesFlag_shouldSuspendJobDefinitionAndJob() {
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    final long hourInMs = 60 * 60 * 1000;

    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // when
    // the process definition will be suspended in 2 hours
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true,
            new Date(startTime.getTime() + (2 * hourInMs)));

    // then
    // there exists a job to suspend process definition
    Job timerToSuspendProcessDefinition = managementService.createJobQuery().timers().singleResult();
    assertThat(timerToSuspendProcessDefinition).isNotNull();

    // the job definition should still active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the job is still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isEqualTo(2); // there exists two jobs, a failing job and a timer job

    // when
    // execute job
    managementService.executeJob(timerToSuspendProcessDefinition.getId());

    // then
    // the job definition should be suspended
    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the job is still active
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job job = jobQuery.suspended().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testDelayedSuspendByKeyAndIncludeInstancesFlag_shouldSuspendJobDefinitionAndJob() {
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    final long hourInMs = 60 * 60 * 1000;

    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // when
    // the process definition will be suspended in 2 hours
    repositoryService.suspendProcessDefinitionByKey(processDefinition.getKey(), true,
            new Date(startTime.getTime() + (2 * hourInMs)));

    // then
    // there exists a job to suspend process definition
    Job timerToSuspendProcessDefinition = managementService.createJobQuery().timers().singleResult();
    assertThat(timerToSuspendProcessDefinition).isNotNull();

    // the job definition should still active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the job is still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isEqualTo(2); // there exists two jobs, a failing job and a timer job

    // when
    // execute job
    managementService.executeJob(timerToSuspendProcessDefinition.getId());

    // then
    // the job definition should be suspended
    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the job is still active
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job job = jobQuery.suspended().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isTrue();
  }

  @Test
  void testMultipleSuspendByKey_shouldSuspendJobDefinitionAndRetainJob() {
    String key = "oneFailingServiceTaskProcess";

    // Deploy five versions of the same process, so that there exists
    // five job definitions
    int nrOfProcessDefinitions = 5;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn").deploy();

      // a running process instance with a failed service task
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // when
    // the process definition will be suspended
    repositoryService.suspendProcessDefinitionByKey(key);

    // then
    // the job definitions should be suspended...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(5);

    // ...and the corresponding jobs should be still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isEqualTo(5);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  void testMultipleSuspendByKeyAndIncludeInstances_shouldSuspendJobDefinitionAndRetainJob() {
    String key = "oneFailingServiceTaskProcess";

    // Deploy five versions of the same process, so that there exists
    // five job definitions
    int nrOfProcessDefinitions = 5;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn").deploy();

      // a running process instance with a failed service task
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // when
    // the process definition will be suspended
    repositoryService.suspendProcessDefinitionByKey(key, false, null);

    // then
    // the job definitions should be suspended...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(5);

    // ...and the corresponding jobs should be still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isEqualTo(5);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  void testMultipleSuspendByKeyAndIncludeInstances_shouldSuspendJobDefinitionAndJob() {
    String key = "oneFailingServiceTaskProcess";

    // Deploy five versions of the same process, so that there exists
    // five job definitions
    int nrOfProcessDefinitions = 5;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn").deploy();

      // a running process instance with a failed service task
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // when
    // the process definition will be suspended
    repositoryService.suspendProcessDefinitionByKey(key, true, null);

    // then
    // the job definitions should be suspended...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(5);

    // ...and the corresponding jobs should be suspended too
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(5);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  void testDelayedMultipleSuspendByKeyAndIncludeInstances_shouldSuspendJobDefinitionAndRetainJob() {
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    final long hourInMs = 60 * 60 * 1000;

    String key = "oneFailingServiceTaskProcess";

    // Deploy five versions of the same process, so that there exists
    // five job definitions
    int nrOfProcessDefinitions = 5;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn").deploy();

      // a running process instance with a failed service task
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // when
    // the process definition will be suspended
    repositoryService.suspendProcessDefinitionByKey(key, false, new Date(startTime.getTime() + (2 * hourInMs)));

    // then
    // there exists a timer job to suspend the process definition delayed
    Job timerToSuspendProcessDefinition = managementService.createJobQuery().timers().singleResult();
    assertThat(timerToSuspendProcessDefinition).isNotNull();

    // the job definitions should be still active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(5);

    // ...and the corresponding jobs should be still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isEqualTo(6);

    // when
    // execute job
    managementService.executeJob(timerToSuspendProcessDefinition.getId());

    // then
    // the job definitions should be suspended...
    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(5);

    // ...and the corresponding jobs should be still active
    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isEqualTo(5);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  void testDelayedMultipleSuspendByKeyAndIncludeInstances_shouldSuspendJobDefinitionAndJob() {
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    final long hourInMs = 60 * 60 * 1000;

    String key = "oneFailingServiceTaskProcess";

    // Deploy five versions of the same process, so that there exists
    // five job definitions
    int nrOfProcessDefinitions = 5;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn").deploy();

      // a running process instance with a failed service task
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // when
    // the process definition will be suspended
    repositoryService.suspendProcessDefinitionByKey(key, true, new Date(startTime.getTime() + (2 * hourInMs)));

    // then
    // there exists a timer job to suspend the process definition delayed
    Job timerToSuspendProcessDefinition = managementService.createJobQuery().timers().singleResult();
    assertThat(timerToSuspendProcessDefinition).isNotNull();

    // the job definitions should be still active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(5);

    // ...and the corresponding jobs should be still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isEqualTo(6);

    // when
    // execute job
    managementService.executeJob(timerToSuspendProcessDefinition.getId());

    // then
    // the job definitions should be suspended...
    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(5);

    // ...and the corresponding jobs should be suspended too
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(5);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testActivationById_shouldActivateJobDefinitionAndRetainJob() {
    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isOne();

    // when
    // the process definition will be activated
    repositoryService.activateProcessDefinitionById(processDefinition.getId());

    // then
    // the job definition should be active...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // ...and the corresponding job should be still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job job = jobQuery.suspended().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testActivationByKey_shouldActivateJobDefinitionAndRetainJob() {
    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isOne();

    // when
    // the process definition will be activated
    repositoryService.activateProcessDefinitionByKey(processDefinition.getKey());

    // then
    // the job definition should be activated...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activatedJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activatedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activatedJobDefinition.isSuspended()).isFalse();

    // ...and the corresponding job should be still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job job = jobQuery.suspended().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testActivationByIdAndIncludeInstancesFlag_shouldActivateAlsoJobDefinitionAndRetainJob() {
    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isOne();

    // when
    // the process definition will be activated
    repositoryService.activateProcessDefinitionById(processDefinition.getId(), false, null);

    // then
    // the job definition should be suspended...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activatedJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activatedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activatedJobDefinition.isSuspended()).isFalse();

    // ...and the corresponding job should be still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job job = jobQuery.suspended().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testActivationByKeyAndIncludeInstancesFlag_shouldActivateAlsoJobDefinitionAndRetainJob() {
    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isOne();

    // when
    // the process definition will be activated
    repositoryService.activateProcessDefinitionByKey(processDefinition.getKey(), false, null);

    // then
    // the job definition should be activated...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activatedJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activatedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activatedJobDefinition.isSuspended()).isFalse();

    // ...and the corresponding job should be still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job job = jobQuery.suspended().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testActivationByIdAndIncludeInstancesFlag_shouldActivateJobDefinitionAndJob() {
    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isOne();

    // when
    // the process definition will be activated
    repositoryService.activateProcessDefinitionById(processDefinition.getId(), true, null);

    // then
    // the job definition should be activated...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activatedJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activatedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activatedJobDefinition.isSuspended()).isFalse();

    // ...and the corresponding job should be activated too
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job job = jobQuery.active().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testActivationByKeyAndIncludeInstancesFlag_shouldActivateJobDefinitionAndJob() {
    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isOne();

    // when
    // the process definition will be activated
    repositoryService.activateProcessDefinitionByKey(processDefinition.getKey(), true, null);

    // then
    // the job definition should be activated...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isFalse();

    // ...and the corresponding job should be activated too
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job job = jobQuery.active().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testDelayedActivationByIdAndIncludeInstancesFlag_shouldActivateJobDefinitionAndRetainJob() {
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    final long hourInMs = 60 * 60 * 1000;

    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isOne();

    // when
    // the process definition will be activated in 2 hours
    repositoryService.activateProcessDefinitionById(processDefinition.getId(), false, new Date(startTime.getTime() + (2 * hourInMs)));

    // then
    // there exists a job to activate process definition
    Job timerToActivateProcessDefinition = managementService.createJobQuery().timers().singleResult();
    assertThat(timerToActivateProcessDefinition).isNotNull();

    // the job definition should still be suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isOne();
    assertThat(jobQuery.active().count()).isOne(); // the timer job is active

    // when
    // execute job
    managementService.executeJob(timerToActivateProcessDefinition.getId());

    // then
    // the job definition should be active
    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the job is still suspended
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job job = jobQuery.suspended().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testDelayedActivationByKeyAndIncludeInstancesFlag_shouldActivateJobDefinitionAndRetainJob() {
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    final long hourInMs = 60 * 60 * 1000;

    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isOne();

    // when
    // the process definition will be activated in 2 hours
    repositoryService.activateProcessDefinitionByKey(processDefinition.getKey(), false,
            new Date(startTime.getTime() + (2 * hourInMs)));

    // then
    // there exists a job to activate process definition
    Job timerToActivateProcessDefinition = managementService.createJobQuery().timers().singleResult();
    assertThat(timerToActivateProcessDefinition).isNotNull();

    // the job definition should still be suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isOne();
    assertThat(jobQuery.active().count()).isOne(); // the timer job is active

    // when
    // execute job
    managementService.executeJob(timerToActivateProcessDefinition.getId());

    // then
    // the job definition should be suspended
    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activatedJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activatedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activatedJobDefinition.isSuspended()).isFalse();

    // the job is still suspended
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job job = jobQuery.suspended().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testDelayedActivationByIdAndIncludeInstancesFlag_shouldActivateJobDefinitionAndJob() {
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    final long hourInMs = 60 * 60 * 1000;

    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isOne();

    // when
    // the process definition will be activated in 2 hours
    repositoryService.activateProcessDefinitionById(processDefinition.getId(), true,
            new Date(startTime.getTime() + (2 * hourInMs)));

    // then
    // there exists a job to activate process definition
    Job timerToActivateProcessDefinition = managementService.createJobQuery().timers().singleResult();
    assertThat(timerToActivateProcessDefinition).isNotNull();

    // the job definition should still be suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isOne();
    assertThat(jobQuery.active().count()).isOne(); // the timer job is active

    // when
    // execute job
    managementService.executeJob(timerToActivateProcessDefinition.getId());

    // then
    // the job definition should be activated
    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activatedJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activatedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activatedJobDefinition.isSuspended()).isFalse();

    // the job is activated
    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job job = jobQuery.active().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn"})
  @Test
  void testDelayedActivationByKeyAndIncludeInstancesFlag_shouldActivateJobDefinitionAndJob() {
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    final long hourInMs = 60 * 60 * 1000;

    // a process definition with an asynchronous continuation, so that there
    // exists a job definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // a running process instance with a failed service task
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceById(processDefinition.getId(), params);

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isOne();

    // when
    // the process definition will be activated in 2 hours
    repositoryService.activateProcessDefinitionByKey(processDefinition.getKey(), true,
            new Date(startTime.getTime() + (2 * hourInMs)));

    // then
    // there exists a job to activate process definition
    Job timerToActivateProcessDefinition = managementService.createJobQuery().timers().singleResult();
    assertThat(timerToActivateProcessDefinition).isNotNull();

    // the job definition should still be suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isOne();
    assertThat(jobQuery.active().count()).isOne(); // the timer job is active

    // when
    // execute job
    managementService.executeJob(timerToActivateProcessDefinition.getId());

    // then
    // the job definition should be activated
    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activatedJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activatedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activatedJobDefinition.isSuspended()).isFalse();

    // the job is activated too
    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job job = jobQuery.active().singleResult();

    assertThat(job.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(job.isSuspended()).isFalse();
  }

  @Test
  void testMultipleActivationByKey_shouldActivateJobDefinitionAndRetainJob() {
    String key = "oneFailingServiceTaskProcess";

    // Deploy five versions of the same process, so that there exists
    // five job definitions
    int nrOfProcessDefinitions = 5;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn").deploy();

      // a running process instance with a failed service task
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionByKey(key, true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isEqualTo(5);

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isEqualTo(5);

    // when
    // the process definition will be activated
    repositoryService.activateProcessDefinitionByKey(key);

    // then
    // the job definitions should be activated...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(5);

    // ...and the corresponding jobs should be still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(5);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  void testMultipleActivationByKeyAndIncludeInstances_shouldActivateJobDefinitionAndRetainJob() {
    String key = "oneFailingServiceTaskProcess";

    // Deploy five versions of the same process, so that there exists
    // five job definitions
    int nrOfProcessDefinitions = 5;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn").deploy();

      // a running process instance with a failed service task
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionByKey(key, true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isEqualTo(5);

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isEqualTo(5);

    // when
    // the process definition will be activated
    repositoryService.activateProcessDefinitionByKey(key, false, null);

    // then
    // the job definitions should be activated...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isEqualTo(5);
    assertThat(jobDefinitionQuery.suspended().count()).isZero();

    // ...and the corresponding jobs should still be suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(5);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  void testMultipleActivationByKeyAndIncludeInstances_shouldActivateJobDefinitionAndJob() {

    String key = "oneFailingServiceTaskProcess";

    // Deploy five versions of the same process, so that there exists
    // five job definitions
    int nrOfProcessDefinitions = 5;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn").deploy();

      // a running process instance with a failed service task
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionByKey(key, true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isEqualTo(5);

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isEqualTo(5);

    // when
    // the process definition will be activated
    repositoryService.activateProcessDefinitionByKey(key, true, null);

    // then
    // the job definitions should be activated...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isEqualTo(5);
    assertThat(jobDefinitionQuery.suspended().count()).isZero();

    // ...and the corresponding jobs should be activated too
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(5);
    assertThat(jobQuery.suspended().count()).isZero();

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  void testDelayedMultipleActivationByKeyAndIncludeInstances_shouldActivateJobDefinitionAndRetainJob() {
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    final long hourInMs = 60 * 60 * 1000;

    String key = "oneFailingServiceTaskProcess";

    // Deploy five versions of the same process, so that there exists
    // five job definitions
    int nrOfProcessDefinitions = 5;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn").deploy();

      // a running process instance with a failed service task
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionByKey(key, true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isEqualTo(5);

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isEqualTo(5);

    // when
    // the process definition will be activated
    repositoryService.activateProcessDefinitionByKey(key, false, new Date(startTime.getTime() + (2 * hourInMs)));

    // then
    // there exists a timer job to activate the process definition delayed
    Job timerToActivateProcessDefinition = managementService.createJobQuery().timers().singleResult();
    assertThat(timerToActivateProcessDefinition).isNotNull();

    // the job definitions should be still suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(5);
    assertThat(jobDefinitionQuery.active().count()).isZero();

    // ...and the corresponding jobs should be still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(5);
    assertThat(jobQuery.active().count()).isOne();

    // when
    // execute job
    managementService.executeJob(timerToActivateProcessDefinition.getId());

    // then
    // the job definitions should be activated...
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(5);
    assertThat(jobDefinitionQuery.suspended().count()).isZero();

    // ...and the corresponding jobs should be still suspended
    assertThat(jobQuery.suspended().count()).isEqualTo(5);
    assertThat(jobQuery.active().count()).isZero();

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  void testDelayedMultipleActivationByKeyAndIncludeInstances_shouldActivateJobDefinitionAndJob() {
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    final long hourInMs = 60 * 60 * 1000;

    String key = "oneFailingServiceTaskProcess";

    // Deploy five versions of the same process, so that there exists
    // five job definitions
    int nrOfProcessDefinitions = 5;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testWithOneAsyncServiceTask.bpmn").deploy();

      // a running process instance with a failed service task
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // the process definition, job definition, process instance and job will be suspended
    repositoryService.suspendProcessDefinitionByKey(key, true, null);

    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isEqualTo(5);

    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isEqualTo(5);

    // when
    // the process definition will be activated
    repositoryService.activateProcessDefinitionByKey(key, true, new Date(startTime.getTime() + (2 * hourInMs)));

    // then
    // there exists a timer job to activate the process definition delayed
    Job timerToActivateProcessDefinition = managementService.createJobQuery().timers().singleResult();
    assertThat(timerToActivateProcessDefinition).isNotNull();

    // the job definitions should be still suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(5);
    assertThat(jobDefinitionQuery.active().count()).isZero();

    // ...and the corresponding jobs should be still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(5);
    assertThat(jobQuery.active().count()).isOne();

    // when
    // execute job
    managementService.executeJob(timerToActivateProcessDefinition.getId());

    // then
    // the job definitions should be activated...
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(5);
    assertThat(jobDefinitionQuery.suspended().count()).isZero();

    // ...and the corresponding jobs should be activated too
    assertThat(jobQuery.active().count()).isEqualTo(5);
    assertThat(jobQuery.suspended().count()).isZero();

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }


  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testSuspendStartTimerOnProcessDefinitionSuspension.bpmn20.xml"})
  @Test
  void testSuspendStartTimerOnProcessDefinitionSuspensionByKey() {
    Job startTimer = managementService.createJobQuery().timers().singleResult();

    assertThat(startTimer.isSuspended()).isFalse();

    // when
    repositoryService.suspendProcessDefinitionByKey("process");

    // then

    // refresh job
    startTimer = managementService.createJobQuery().timers().singleResult();
    assertThat(startTimer.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testSuspendStartTimerOnProcessDefinitionSuspension.bpmn20.xml"})
  @Test
  void testSuspendStartTimerOnProcessDefinitionSuspensionById() {
    ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().singleResult();

    Job startTimer = managementService.createJobQuery().timers().singleResult();

    assertThat(startTimer.isSuspended()).isFalse();

    // when
    repositoryService.suspendProcessDefinitionById(pd.getId());

    // then

    // refresh job
    startTimer = managementService.createJobQuery().timers().singleResult();
    assertThat(startTimer.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testSuspendStartTimerOnProcessDefinitionSuspension.bpmn20.xml"})
  @Test
  void testActivateStartTimerOnProcessDefinitionSuspensionByKey() {
    repositoryService.suspendProcessDefinitionByKey("process");

    Job startTimer = managementService.createJobQuery().timers().singleResult();
    assertThat(startTimer.isSuspended()).isTrue();

    // when
    repositoryService.activateProcessDefinitionByKey("process");
    // then

    // refresh job
    startTimer = managementService.createJobQuery().timers().singleResult();
    assertThat(startTimer.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/ProcessDefinitionSuspensionTest.testSuspendStartTimerOnProcessDefinitionSuspension.bpmn20.xml"})
  @Test
  void testActivateStartTimerOnProcessDefinitionSuspensionById() {
    ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().singleResult();
    repositoryService.suspendProcessDefinitionById(pd.getId());

    Job startTimer = managementService.createJobQuery().timers().singleResult();

    assertThat(startTimer.isSuspended()).isTrue();

    // when
    repositoryService.activateProcessDefinitionById(pd.getId());

    // then

    // refresh job
    startTimer = managementService.createJobQuery().timers().singleResult();
    assertThat(startTimer.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testStartBeforeActivityForSuspendProcessDefinition() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    //start process instance
    runtimeService.startProcessInstanceById(processDefinition.getId());
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();

    // Suspend process definition
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("theTask");

    // try to start before activity for suspended processDefinition
    assertThatThrownBy(processInstanceModificationBuilder::execute)
        .isInstanceOf(SuspendedEntityInteractionException.class)
        .satisfies(e -> assertThat(e.getMessage().toLowerCase()).contains("is suspended"));
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testStartAfterActivityForSuspendProcessDefinition() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    //start process instance
    runtimeService.startProcessInstanceById(processDefinition.getId());
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();

    // Suspend process definition
    repositoryService.suspendProcessDefinitionById(processDefinition.getId(), true, null);
    var processInstanceModificationBuilder = runtimeService.createProcessInstanceModification(processInstance.getId()).startAfterActivity("theTask");

    // try to start after activity for suspended processDefinition
    assertThatThrownBy(processInstanceModificationBuilder::execute)
        .isInstanceOf(SuspendedEntityInteractionException.class)
        .satisfies(e -> assertThat(e.getMessage().toLowerCase()).contains("is suspended"));
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml"})
  @Test
  void testSuspendAndActivateProcessDefinitionByIdUsingBuilder() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();

    // suspend
    repositoryService
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .suspend();

    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isTrue();

    // activate
    repositoryService
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .activate();

    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml"})
  @Test
  void testSuspendAndActivateProcessDefinitionByKeyUsingBuilder() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();

    // suspend
    repositoryService
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(processDefinition.getKey())
      .suspend();

    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isTrue();

    // activate
    repositoryService
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(processDefinition.getKey())
      .activate();

    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml"})
  @Test
  void testDelayedSuspendAndActivateProcessDefinitionByKeyUsingBuilder() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();

    // suspend process definition in one week from now
    long oneWeekFromStartTime = new Date().getTime() + (7 * 24 * 60 * 60 * 1000);

    // suspend
    repositoryService
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(processDefinition.getKey())
      .executionDate(new Date(oneWeekFromStartTime))
      .suspend();

    // execute the suspension job
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
    managementService.executeJob(job.getId());

    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isTrue();

    // activate
    repositoryService
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(processDefinition.getKey())
      .executionDate(new Date(oneWeekFromStartTime))
      .activate();

    // execute the activation job
    job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
    managementService.executeJob(job.getId());

    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDelayedSuspendProcessDefinitionUsingBuilder() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // suspend process definition in one week from now
    long oneWeekFromStartTime = new Date().getTime() + (7 * 24 * 60 * 60 * 1000);

    repositoryService
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .executionDate(new Date(oneWeekFromStartTime))
      .suspend();

    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();

    // execute the suspension job
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
    managementService.executeJob(job.getId());

    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDelayedActivateProcessDefinitionUsingBuilder() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // suspend
    repositoryService
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(processDefinition.getKey())
      .suspend();

    // activate process definition in one week from now
    long oneWeekFromStartTime = new Date().getTime() + (7 * 24 * 60 * 60 * 1000);

    repositoryService
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .executionDate(new Date(oneWeekFromStartTime))
      .activate();

    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isTrue();

    // execute the activation job
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
    managementService.executeJob(job.getId());

    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSuspendAndActivateProcessDefinitionIncludeInstancesUsingBuilder() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

    assertThat(processDefinition.isSuspended()).isFalse();
    assertThat(processInstance.isSuspended()).isFalse();

    // suspend
    repositoryService
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .includeProcessInstances(true)
      .suspend();

    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isTrue();

    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isTrue();

    // activate
    repositoryService
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .includeProcessInstances(true)
      .activate();

    processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition.isSuspended()).isFalse();

    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance.isSuspended()).isFalse();
  }

}
