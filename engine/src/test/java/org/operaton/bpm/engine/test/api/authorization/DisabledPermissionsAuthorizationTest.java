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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.ProcessInstancePermissions;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.authorization.TaskPermissions;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.management.ActivityStatistics;
import org.operaton.bpm.engine.management.DeploymentStatistics;
import org.operaton.bpm.engine.management.DeploymentStatisticsQuery;
import org.operaton.bpm.engine.management.IncidentStatistics;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.identity.TestPermissions;
import org.operaton.bpm.engine.test.api.identity.TestResource;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE_INSTANCE;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisabledPermissionsAuthorizationTest {

  protected static final String USER_ID = "user";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RepositoryService repositoryService;
  AuthorizationService authorizationService;
  RuntimeService runtimeService;
  ManagementService managementService;
  TaskService taskService;

  @BeforeEach
  void setUp() {
    authRule.createUserAndGroup(USER_ID, "group");
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    repositoryService = engineRule.getRepositoryService();
    authorizationService = engineRule.getAuthorizationService();
    runtimeService = engineRule.getRuntimeService();
    managementService = engineRule.getManagementService();
    taskService = engineRule.getTaskService();
  }

  @AfterEach
  void tearDown() {
    authRule.disableAuthorization();
    authRule.deleteUsersAndGroups();
    processEngineConfiguration.setDisabledPermissions(null);
  }

  @Test
  void testIsUserAuthorizedForIgnoredPermission() {
    // given
    processEngineConfiguration.setDisabledPermissions(List.of(READ.name()));

    authRule.createGrantAuthorization(PROCESS_INSTANCE, ANY, USER_ID, ProcessInstancePermissions.RETRY_JOB);

    authRule.enableAuthorization(USER_ID);

    // when/then
    assertThatThrownBy(() -> authorizationService.isUserAuthorized(USER_ID, null, READ, PROCESS_DEFINITION))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("The 'READ' permission is disabled, please check your process engine configuration.");
  }

  @Test
  void testCustomPermissionDuplicateValue() {
    // given
    processEngineConfiguration.setDisabledPermissions(List.of(ProcessInstancePermissions.SUSPEND.name()));
    Resource resource1 = TestResource.RESOURCE1;
    Resource resource2 = TestResource.RESOURCE2;

    // assume
    assertThat(TestPermissions.RANDOM.getValue()).isEqualTo(ProcessInstancePermissions.SUSPEND.getValue());

    // when
    authRule.createGrantAuthorization(resource1, ANY, USER_ID, TestPermissions.RANDOM);
    authRule.createGrantAuthorization(resource2, "resource2-1", USER_ID, TestPermissions.RANDOM);
    authRule.enableAuthorization(USER_ID);

    // then
    // verify that the custom permission with the same value is not affected by disabling the build-in permission
    assertThat(authorizationService.isUserAuthorized(USER_ID, null, TestPermissions.RANDOM, resource1)).isTrue();
    assertThat(authorizationService.isUserAuthorized(USER_ID, null, TestPermissions.RANDOM, resource2, "resource2-1")).isTrue();
  }

  // specific scenarios //////////////////////////////////////
  // the next tests cover different combination in the authorization checks
  // i.e. the query doesn't fail if all permissions are disabled in the specific authorization check

  @Test
  void testGetVariableIgnoreTaskRead() {
    // given
    processEngineConfiguration.setDisabledPermissions(List.of(TaskPermissions.READ.name()));
    String taskId = "taskId";
    Task task = taskService.newTask(taskId);
    taskService.saveTask(task);

    taskService.setVariables(taskId, Variables.createVariables().putValue("foo", "bar"));
    authRule.enableAuthorization(USER_ID);

    // when
    Object variable = taskService.getVariable(taskId, "foo");

    // then
    assertThat(variable).isEqualTo("bar");
    authRule.disableAuthorization();
    taskService.deleteTask(taskId, true);
  }

  @Test
  void testQueryTaskIgnoreTaskRead() {
    // given
    List<String> permissions = new ArrayList<>();
    permissions.add(TaskPermissions.READ.name());
    permissions.add(ProcessDefinitionPermissions.READ_TASK.name());
    processEngineConfiguration.setDisabledPermissions(permissions);
    String taskId = "taskId";
    Task task = taskService.newTask(taskId);
    taskService.saveTask(task);

    authRule.enableAuthorization(USER_ID);

    // when
    Task returnedTask = taskService.createTaskQuery().singleResult();

    // then
    assertThat(returnedTask).isNotNull();
    authRule.disableAuthorization();
    taskService.deleteTask(taskId, true);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testDeleteHistoricProcessInstanceIgnoreDeleteHistory() {
    // given
    processEngineConfiguration.setDisabledPermissions(List.of(Permissions.DELETE_HISTORY.name()));

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.deleteProcessInstance(processInstance.getId(), "any");
    authRule.enableAuthorization(USER_ID);

    engineRule.getHistoryService().deleteHistoricProcessInstance(processInstance.getId());
    authRule.disableAuthorization();
    assertThat(engineRule.getHistoryService().createHistoricProcessInstanceQuery().singleResult()).isNull();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testQueryDeploymentIgnoreRead() {
    // given
    engineRule.getProcessEngineConfiguration().setDisabledPermissions(List.of(READ.name()));

    // when
    authRule.enableAuthorization(USER_ID);
    List<org.operaton.bpm.engine.repository.Deployment> deployments = engineRule.getRepositoryService().createDeploymentQuery().list();

    // then
    assertThat(deployments).hasSize(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testStartableInTasklistIgnoreRead() {
    // given
    processEngineConfiguration.setDisabledPermissions(List.of(READ.name()));
    authRule.createGrantAuthorization(PROCESS_DEFINITION, "oneTaskProcess", USER_ID, CREATE_INSTANCE);
    authRule.createGrantAuthorization(PROCESS_INSTANCE, "*", USER_ID, CREATE);

    authRule.disableAuthorization();
    ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").singleResult();
    authRule.enableAuthorization(USER_ID);

    // when
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().startablePermissionCheck().startableInTasklist().list();
    // then
    assertThat(processDefinitions).isNotNull();
    assertThat(repositoryService.createProcessDefinitionQuery().startablePermissionCheck().startableInTasklist().count()).isOne();
    assertThat(processDefinitions.get(0).getId()).isEqualTo(definition.getId());
    assertThat(processDefinitions.get(0).isStartableInTasklist()).isTrue();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml")
  void testDeploymentStatisticsIgnoreReadInstance() {
    // given
    processEngineConfiguration.setDisabledPermissions(List.of(READ_INSTANCE.name()));

    runtimeService.startProcessInstanceByKey("timerBoundaryProcess");

    authRule.enableAuthorization(USER_ID);

    // when
    DeploymentStatisticsQuery query = engineRule.getManagementService().createDeploymentStatisticsQuery();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      assertThat(deploymentStatistics.getInstances()).as("Instances").isEqualTo(1);
      assertThat(deploymentStatistics.getFailedJobs()).as("Failed Jobs").isZero();

      List<IncidentStatistics> incidentStatistics = deploymentStatistics.getIncidentStatistics();
      assertThat(incidentStatistics).as("Incidents supposed to be empty").isEmpty();
    }

  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml")
  void testActivityStatisticsIgnoreRead() {
    // given
    List<String> permissions = new ArrayList<>();
    permissions.add(READ.name());
    permissions.add(READ_INSTANCE.name());
    processEngineConfiguration.setDisabledPermissions(permissions);
    String processDefinitionId = runtimeService.startProcessInstanceByKey("timerBoundaryProcess").getProcessDefinitionId();

    authRule.enableAuthorization(USER_ID);

    // when
    ActivityStatistics statistics = managementService.createActivityStatisticsQuery(processDefinitionId).singleResult();

    // then
    assertThat(statistics).isNotNull();
    assertThat(statistics.getId()).isEqualTo("task");
    assertThat(statistics.getInstances()).isEqualTo(1);
    assertThat(statistics.getFailedJobs()).isZero();
    assertThat(statistics.getIncidentStatistics()).isEmpty();
  }

  @Test
  @Disabled("CAM-9888")
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  void testFetchAndLockIgnoreRead() {
    // given
    List<String> permissions = new ArrayList<>();
    permissions.add(READ.name());
    permissions.add(READ_INSTANCE.name());
    processEngineConfiguration.setDisabledPermissions(permissions);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    authRule.createGrantAuthorization(PROCESS_INSTANCE, "*", USER_ID, UPDATE);

    authRule.enableAuthorization(USER_ID);

    // when
    List<LockedExternalTask> externalTasks = engineRule.getExternalTaskService()
        .fetchAndLock(1, "aWorkerId")
        .topic("externalTaskTopic", 10000L)
        .execute();

    // then
    assertThat(externalTasks).hasSize(1);

    LockedExternalTask task = externalTasks.get(0);
    assertThat(task.getId()).isNotNull();
    assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(task.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(task.getActivityId()).isEqualTo("externalTask");
    assertThat(task.getProcessDefinitionKey()).isEqualTo("oneExternalTaskProcess");
  }

  protected void executeAvailableJobs(final String key) {
    List<Job> jobs = managementService.createJobQuery().processDefinitionKey(key).withRetriesLeft().list();

    if (jobs.isEmpty()) {
      return;
    }

    for (Job job : jobs) {
        managementService.executeJob(job.getId());
    }

    executeAvailableJobs(key);
  }

}
