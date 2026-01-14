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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Comment;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GLOBAL;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_REVOKE;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;
import static org.operaton.bpm.engine.authorization.Resources.AUTHORIZATION;
import static org.operaton.bpm.engine.authorization.Resources.USER;
import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobIgnoringException;

/**
 * @author Roman Smirnov
 */
public abstract class AuthorizationTest {

  @RegisterExtension
  protected static ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder().build();
  @RegisterExtension
  protected ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(processEngineExtension);

  protected String userId = "test";
  protected String groupId = "accounting";
  protected User testUser;
  protected Group testGroup;

  protected static final String VARIABLE_NAME = "aVariableName";
  protected static final String VARIABLE_VALUE = "aVariableValue";
  protected static final String TASK_ID = "myTask";

  protected ProcessEngine processEngine;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected IdentityService identityService;
  protected AuthorizationService authorizationService;
  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected ManagementService managementService;
  protected RepositoryService repositoryService;
  protected CaseService caseService;
  protected FormService formService;
  protected ExternalTaskService externalTaskService;
  protected HistoryService historyService;
  protected DecisionService decisionService;

  protected List<String> deploymentIds = new ArrayList<>();

  @BeforeEach
  public void setUp() {
    processEngineConfiguration.setAuthorizationEnabled(false);
    testUser = createUser(userId);
    testGroup = createGroup(groupId);
    identityService.createMembership(userId, groupId);
    identityService.setAuthentication(userId, Arrays.asList(groupId));
    processEngineConfiguration.setAuthorizationEnabled(true);
  }

  @AfterEach
  public void tearDown() {
    processEngineConfiguration.setAuthorizationEnabled(false);
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Group group : identityService.createGroupQuery().list()) {
      identityService.deleteGroup(group.getId());
    }
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
    for (String deploymentId : deploymentIds) {
      deleteDeployment(deploymentId);
    }
  }

  protected <T> T runWithoutAuthorization(Callable<T> runnable) {
    boolean authorizationEnabled = processEngineConfiguration.isAuthorizationEnabled();
    try {
      disableAuthorization();
      return runnable.call();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (authorizationEnabled) {
        enableAuthorization();
      }
    }
  }

  protected String permissionException(Resource resource, Permission permission) {
    return "ENGINE-03110 Required admin authenticated group or user or any of the following permissions: '"
        + permission.getName() + "' permission on resource '" + resource.resourceName() + "'";
  }

  // user ////////////////////////////////////////////////////////////////

  protected User createUser(String userId) {
    User user = identityService.newUser(userId);
    identityService.saveUser(user);

    // give user all permission to manipulate authorizations
    Authorization authorization = createGrantAuthorization(AUTHORIZATION, ANY);
    authorization.setUserId(userId);
    authorization.addPermission(ALL);
    saveAuthorization(authorization);

    // give user all permission to manipulate users
    authorization = createGrantAuthorization(USER, ANY);
    authorization.setUserId(userId);
    authorization.addPermission(Permissions.ALL);
    saveAuthorization(authorization);

    return user;
  }

  // group //////////////////////////////////////////////////////////////

  protected Group createGroup(final String groupId) {
    return runWithoutAuthorization(() -> {
      Group group = identityService.newGroup(groupId);
      identityService.saveGroup(group);
      return group;
    });
  }

  // authorization ///////////////////////////////////////////////////////

  protected Authorization createGrantAuthorization(Resource resource, String resourceId,
                                                   String userId, Permission... permissions) {
    Authorization authorization = createGrantAuthorization(resource, resourceId);
    authorization.setUserId(userId);
    for (Permission permission : permissions) {
      authorization.addPermission(permission);
    }
    saveAuthorization(authorization);
    return authorization;
  }

  protected Authorization createGrantAuthorizationWithoutAuthentication(Resource resource,
                                                                        String resourceId,
                                                                        String userId,
                                                                        Permission... permissions) {
    Authentication currentAuthentication = identityService.getCurrentAuthentication();
    identityService.clearAuthentication();
    try {
      return createGrantAuthorization(resource, resourceId, userId, permissions);
    } finally {
      identityService.setAuthentication(currentAuthentication);
    }
  }

  protected void createGrantAuthorizationGroup(Resource resource, String resourceId,
                                               String groupId, Permission... permissions) {
    Authorization authorization = createGrantAuthorization(resource, resourceId);
    authorization.setGroupId(groupId);
    for (Permission permission : permissions) {
      authorization.addPermission(permission);
    }
    saveAuthorization(authorization);
  }

  protected void createRevokeAuthorizationWithoutAuthentication(Resource resource,
                                                                String resourceId, String userId,
                                                                Permission... permissions) {
    Authentication currentAuthentication = identityService.getCurrentAuthentication();
    identityService.clearAuthentication();
    try {
      createRevokeAuthorization(resource, resourceId, userId, permissions);
    } finally {
      identityService.setAuthentication(currentAuthentication);
    }
  }

  protected void createRevokeAuthorization(Resource resource, String resourceId, String userId,
                                           Permission... permissions) {
    Authorization authorization = createRevokeAuthorization(resource, resourceId);
    authorization.setUserId(userId);
    for (Permission permission : permissions) {
      authorization.removePermission(permission);
    }
    saveAuthorization(authorization);
  }

  protected Authorization createGlobalAuthorization(Resource resource, String resourceId) {
    return createAuthorization(AUTH_TYPE_GLOBAL, resource, resourceId);
  }

  protected Authorization createGrantAuthorization(Resource resource, String resourceId) {
    return createAuthorization(AUTH_TYPE_GRANT, resource, resourceId);
  }

  protected Authorization createRevokeAuthorization(Resource resource, String resourceId) {
    return createAuthorization(AUTH_TYPE_REVOKE, resource, resourceId);
  }

  protected Authorization createAuthorization(int type, Resource resource, String resourceId) {
    Authorization authorization = authorizationService.createNewAuthorization(type);

    authorization.setResource(resource);
    if (resourceId != null) {
      authorization.setResourceId(resourceId);
    }

    return authorization;
  }

  protected void saveAuthorization(Authorization authorization) {
    authorizationService.saveAuthorization(authorization);
  }

  // enable/disable authorization //////////////////////////////////////////////

  protected void enableAuthorization() {
    processEngineConfiguration.setAuthorizationEnabled(true);
  }

  protected void disableAuthorization() {
    processEngineConfiguration.setAuthorizationEnabled(false);
  }

  // actions (executed without authorization) ///////////////////////////////////

  protected ProcessInstance startProcessInstanceByKey(String key) {
    return startProcessInstanceByKey(key, null);
  }

  protected ProcessInstance startProcessInstanceByKey(final String key,
                                                      final Map<String, Object> variables) {
    return runWithoutAuthorization(() -> runtimeService.startProcessInstanceByKey(key, variables));
  }

  public void executeAvailableJobs() {
    runWithoutAuthorization((Callable<Void>) () -> {
      testRule.executeAvailableJobs();
      return null;
    });
  }

  protected CaseInstance createCaseInstanceByKey(final String key,
                                                 final Map<String, Object> variables) {
    return runWithoutAuthorization(() -> caseService.createCaseInstanceByKey(key, variables));
  }

  protected void createTask(final String taskId) {
    runWithoutAuthorization((Callable<Void>) () -> {
      Task task = taskService.newTask(taskId);
      taskService.saveTask(task);
      return null;
    });
  }

  protected void deleteTask(final String taskId, final boolean cascade) {
    runWithoutAuthorization((Callable<Void>) () -> {
      taskService.deleteTask(taskId, cascade);
      return null;
    });
  }

  protected Comment createComment(String taskId, String processInstanceId, String message) {
    return runWithoutAuthorization(() -> taskService.createComment(taskId, processInstanceId, message));
  }

  protected void addCandidateUser(final String taskId, final String user) {
    runWithoutAuthorization((Callable<Void>) () -> {
      taskService.addCandidateUser(taskId, user);
      return null;
    });
  }

  protected void addCandidateGroup(final String taskId, final String group) {
    runWithoutAuthorization((Callable<Void>) () -> {
      taskService.addCandidateGroup(taskId, group);
      return null;
    });
  }

  protected void setAssignee(final String taskId, final String userId) {
    runWithoutAuthorization((Callable<Void>) () -> {
      taskService.setAssignee(taskId, userId);
      return null;
    });
  }

  protected void delegateTask(final String taskId, final String userId) {
    runWithoutAuthorization((Callable<Void>) () -> {
      taskService.delegateTask(taskId, userId);
      return null;
    });
  }

  protected Task selectSingleTask() {
    return runWithoutAuthorization(() -> taskService.createTaskQuery().singleResult());
  }

  protected void setTaskVariables(final String taskId,
                                  final Map<String, ? extends Object> variables) {
    runWithoutAuthorization((Callable<Void>) () -> {
      taskService.setVariables(taskId, variables);
      return null;
    });
  }

  protected void setTaskVariablesLocal(final String taskId,
                                       final Map<String, ? extends Object> variables) {
    runWithoutAuthorization((Callable<Void>) () -> {
      taskService.setVariablesLocal(taskId, variables);
      return null;
    });
  }

  protected void setTaskVariable(final String taskId, final String name, final Object value) {
    runWithoutAuthorization((Callable<Void>) () -> {
      taskService.setVariable(taskId, name, value);
      return null;
    });
  }

  protected void setTaskVariableLocal(final String taskId, final String name, final Object value) {
    runWithoutAuthorization((Callable<Void>) () -> {
      taskService.setVariableLocal(taskId, name, value);
      return null;
    });
  }

  protected void setExecutionVariable(final String executionId, final String name,
                                      final Object value) {
    runWithoutAuthorization((Callable<Void>) () -> {
      runtimeService.setVariable(executionId, name, value);
      return null;
    });
  }

  protected void setExecutionVariableLocal(final String executionId, final String name,
                                           final Object value) {
    runWithoutAuthorization((Callable<Void>) () -> {
      runtimeService.setVariableLocal(executionId, name, value);
      return null;
    });
  }

  protected void setCaseVariable(final String caseExecution, final String name,
                                 final Object value) {
    runWithoutAuthorization((Callable<Void>) () -> {
      caseService.setVariable(caseExecution, name, value);
      return null;
    });
  }

  protected void setCaseVariableLocal(final String caseExecution, final String name,
                                      final Object value) {
    runWithoutAuthorization((Callable<Void>) () -> {
      caseService.setVariableLocal(caseExecution, name, value);
      return null;
    });
  }

  protected ProcessDefinition selectProcessDefinitionByKey(final String processDefinitionKey) {
    return runWithoutAuthorization(() -> repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(processDefinitionKey)
        .singleResult());
  }

  protected ProcessInstance selectSingleProcessInstance() {
    return runWithoutAuthorization(() -> runtimeService
        .createProcessInstanceQuery()
        .singleResult());
  }

  protected void suspendProcessDefinitionByKey(final String processDefinitionKey) {
    runWithoutAuthorization((Callable<Void>) () -> {
      repositoryService.suspendProcessDefinitionByKey(processDefinitionKey);
      return null;
    });
  }

  protected void suspendProcessDefinitionById(final String processDefinitionId) {
    runWithoutAuthorization((Callable<Void>) () -> {
      repositoryService.suspendProcessDefinitionById(processDefinitionId);
      return null;
    });
  }

  protected void suspendProcessInstanceById(final String processInstanceId) {
    runWithoutAuthorization((Callable<Void>) () -> {
      runtimeService.suspendProcessInstanceById(processInstanceId);
      return null;
    });
  }

  protected void suspendJobById(final String jobId) {
    runWithoutAuthorization((Callable<Void>) () -> {
      managementService.suspendJobById(jobId);
      return null;
    });
  }

  protected void suspendJobByProcessInstanceId(final String processInstanceId) {
    runWithoutAuthorization((Callable<Void>) () -> {
      managementService.suspendJobByProcessInstanceId(processInstanceId);
      return null;
    });
  }

  protected void suspendJobByJobDefinitionId(final String jobDefinitionId) {
    runWithoutAuthorization((Callable<Void>) () -> {
      managementService.suspendJobByJobDefinitionId(jobDefinitionId);
      return null;
    });
  }

  protected void suspendJobByProcessDefinitionId(final String processDefinitionId) {
    runWithoutAuthorization((Callable<Void>) () -> {
      managementService.suspendJobByProcessDefinitionId(processDefinitionId);
      return null;
    });
  }

  protected void suspendJobByProcessDefinitionKey(final String processDefinitionKey) {
    runWithoutAuthorization((Callable<Void>) () -> {
      managementService.suspendJobByProcessDefinitionKey(processDefinitionKey);
      return null;
    });
  }

  protected void suspendJobDefinitionById(final String jobDefinitionId) {
    runWithoutAuthorization((Callable<Void>) () -> {
      managementService.suspendJobDefinitionById(jobDefinitionId);
      return null;
    });
  }

  protected void suspendJobDefinitionByProcessDefinitionId(final String processDefinitionId) {
    runWithoutAuthorization((Callable<Void>) () -> {
      managementService.suspendJobDefinitionByProcessDefinitionId(processDefinitionId);
      return null;
    });
  }

  protected void suspendJobDefinitionByProcessDefinitionKey(final String processDefinitionKey) {
    runWithoutAuthorization((Callable<Void>) () -> {
      managementService.suspendJobDefinitionByProcessDefinitionKey(processDefinitionKey);
      return null;
    });
  }

  protected void suspendJobDefinitionIncludingJobsById(final String jobDefinitionId) {
    runWithoutAuthorization((Callable<Void>) () -> {
      managementService.suspendJobDefinitionById(jobDefinitionId, true);
      return null;
    });
  }

  protected void suspendJobDefinitionIncludingJobsByProcessDefinitionId(final String processDefinitionId) {
    runWithoutAuthorization((Callable<Void>) () -> {
      managementService.suspendJobDefinitionByProcessDefinitionId(processDefinitionId, true);
      return null;
    });
  }

  protected void suspendJobDefinitionIncludingJobsByProcessDefinitionKey(final String processDefinitionKey) {
    runWithoutAuthorization((Callable<Void>) () -> {
      managementService.suspendJobDefinitionByProcessDefinitionKey(processDefinitionKey, true);
      return null;
    });
  }

  protected Deployment createDeployment(final String... resources) {
    return runWithoutAuthorization(() -> {
      DeploymentBuilder builder = repositoryService.createDeployment();
      for (String resource : resources) {
        builder.addClasspathResource(resource);
      }
      Deployment deployment = builder.deploy();
      deploymentIds.add(deployment.getId());
      return deployment;
    });
  }

  protected void deleteDeployment(String deploymentId) {
    deleteDeployment(deploymentId, true);
  }

  protected void deleteDeployment(final String deploymentId, final boolean cascade) {
    Authentication authentication = identityService.getCurrentAuthentication();
    try {
      identityService.clearAuthentication();
      runWithoutAuthorization((Callable<Void>) () -> {
        repositoryService.deleteDeployment(deploymentId, cascade);
        return null;
      });
    } finally {
      if (authentication != null) {
        identityService.setAuthentication(authentication);
      }
    }
  }

  protected ProcessInstance startProcessAndExecuteJob(final String key) {
    return runWithoutAuthorization(() -> {
      ProcessInstance processInstance = startProcessInstanceByKey(key);
      executeAvailableJobs(key);
      return processInstance;
    });
  }

  protected void executeAvailableJobs(final String key) {
    runWithoutAuthorization((Callable<Void>) () -> {
      List<Job> jobs =
          managementService.createJobQuery().processDefinitionKey(key).withRetriesLeft().list();

      if (jobs.isEmpty()) {
        enableAuthorization();
        return null;
      }

      for (Job job : jobs) {
        executeJobIgnoringException(managementService, job.getId());
      }

      executeAvailableJobs(key);
      return null;
    });
  }

  protected DecisionDefinition selectDecisionDefinitionByKey(final String decisionDefinitionKey) {
    return runWithoutAuthorization(() -> repositoryService
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(decisionDefinitionKey)
        .singleResult());
  }

  // helper ////////////////////////////////////////////////////////////////////

  protected VariableMap getVariables() {
    return Variables.createVariables().putValue(VARIABLE_NAME, VARIABLE_VALUE);
  }

  protected String getMissingPermissionMessageRegex(Permission permission, Resource resource) {
    return ".*'"+ permission.getName() + "' permission .* type '" + resource.resourceName() + "'.*";
  }
}
