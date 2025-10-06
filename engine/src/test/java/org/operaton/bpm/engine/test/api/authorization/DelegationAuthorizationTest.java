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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.form.StartFormData;
import org.operaton.bpm.engine.form.TaskFormData;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.runtime.VariableInstanceQuery;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.authorization.service.ExecuteCommandDelegate;
import org.operaton.bpm.engine.test.api.authorization.service.ExecuteCommandListener;
import org.operaton.bpm.engine.test.api.authorization.service.ExecuteCommandTaskListener;
import org.operaton.bpm.engine.test.api.authorization.service.ExecuteQueryDelegate;
import org.operaton.bpm.engine.test.api.authorization.service.ExecuteQueryListener;
import org.operaton.bpm.engine.test.api.authorization.service.ExecuteQueryTaskListener;
import org.operaton.bpm.engine.test.api.authorization.service.MyDelegationService;
import org.operaton.bpm.engine.test.api.authorization.service.MyFormFieldValidator;
import org.operaton.bpm.engine.test.api.authorization.service.MyServiceTaskActivityBehaviorExecuteCommand;
import org.operaton.bpm.engine.test.api.authorization.service.MyServiceTaskActivityBehaviorExecuteQuery;
import org.operaton.bpm.engine.test.api.authorization.service.MyTaskService;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE_INSTANCE;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.TASK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
public class DelegationAuthorizationTest extends AuthorizationTest {

  public static final String DEFAULT_PROCESS_KEY = "process";

  @BeforeEach
  @Override
  public void setUp() {
    MyDelegationService.clearProperties();
    processEngineConfiguration.setAuthorizationEnabledForCustomCode(false);
    super.setUp();
  }

  @Deployment
  @Test
  void testJavaDelegateExecutesQueryAfterUserCompletesTask() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testJavaDelegateExecutesCommandAfterUserCompletesTask() {
    // given
    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    disableAuthorization();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
    enableAuthorization();
  }

  @Deployment
  @Test
  void testJavaDelegateExecutesQueryAfterUserCompletesTaskAsDelegateExpression() {
    // given
    processEngineConfiguration.getBeans().put("myDelegate", new ExecuteQueryDelegate());

    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testJavaDelegateExecutesCommandAfterUserCompletesTaskAsDelegateExpression() {
    // given
    processEngineConfiguration.getBeans().put("myDelegate", new ExecuteCommandDelegate());

    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    disableAuthorization();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
    enableAuthorization();
  }

  @Deployment
  @Test
  void testJavaDelegateExecutesQueryAfterUserCompletesTaskAsExpression() {
    // given
    processEngineConfiguration.getBeans().put("myDelegate", new ExecuteQueryDelegate());

    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testJavaDelegateExecutesCommandAfterUserCompletesTaskAsExpression() {
    // given
    processEngineConfiguration.getBeans().put("myDelegate", new ExecuteCommandDelegate());

    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    disableAuthorization();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
    enableAuthorization();
  }

  @Deployment
  @Test
  void testCustomActivityBehaviorExecutesQueryAfterUserCompletesTask() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testCustomActivityBehaviorExecutesCommandAfterUserCompletesTask() {
    // given
    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    disableAuthorization();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
    enableAuthorization();
  }

  @Deployment
  @Test
  void testCustomActivityBehaviorExecutesQueryAfterUserCompletesTaskAsDelegateExpression() {
    // given
    processEngineConfiguration.getBeans().put("myBehavior", new MyServiceTaskActivityBehaviorExecuteQuery());

    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testCustomActivityBehaviorExecutesCommandAfterUserCompletesTaskAsDelegateExpression() {
    // given
    processEngineConfiguration.getBeans().put("myBehavior", new MyServiceTaskActivityBehaviorExecuteCommand());

    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    disableAuthorization();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
    enableAuthorization();
  }

  @Deployment
  @Test
  void testSignallableActivityBehaviorAsClass() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 4);
    String processInstanceId = startProcessInstanceByKey(DEFAULT_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    // when
    runtimeService.signal(processInstanceId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testSignallableActivityBehaviorAsDelegateExpression() {
    // given
    processEngineConfiguration.getBeans().put("activityBehavior", new MyServiceTaskActivityBehaviorExecuteQuery());

    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 4);
    String processInstanceId = startProcessInstanceByKey(DEFAULT_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, UPDATE);

    // when
    runtimeService.signal(processInstanceId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testExecutionListenerExecutesQueryAfterUserCompletesTask() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testExecutionListenerExecutesCommandAfterUserCompletesTask() {
    // given
    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    disableAuthorization();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
    enableAuthorization();
  }

  @Deployment
  @Test
  void testExecutionListenerExecutesQueryAfterUserCompletesTaskAsDelegateExpression() {
    // given
    processEngineConfiguration.getBeans().put("myListener", new ExecuteQueryListener());

    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testExecutionListenerExecutesCommandAfterUserCompletesTaskAsDelegateExpression() {
    // given
    processEngineConfiguration.getBeans().put("myListener", new ExecuteCommandListener());

    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    disableAuthorization();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
    enableAuthorization();
  }

  @Deployment
  @Test
  void testExecutionListenerExecutesQueryAfterUserCompletesTaskAsExpression() {
    // given
    processEngineConfiguration.getBeans().put("myListener", new ExecuteQueryListener());

    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testExecutionListenerExecutesCommandAfterUserCompletesTaskAsExpression() {
    // given
    processEngineConfiguration.getBeans().put("myListener", new ExecuteCommandListener());

    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    disableAuthorization();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
    enableAuthorization();
  }

  @Deployment
  @Test
  void testTaskListenerExecutesQueryAfterUserCompletesTask() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testTaskListenerExecutesCommandAfterUserCompletesTask() {
    // given
    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    disableAuthorization();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
    enableAuthorization();
  }

  @Deployment
  @Test
  void testTaskListenerExecutesQueryAfterUserCompletesTaskAsDelegateExpression() {
    // given
    processEngineConfiguration.getBeans().put("myListener", new ExecuteQueryTaskListener());

    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testTaskListenerExecutesCommandAfterUserCompletesTaskAsDelegateExpression() {
    // given
    processEngineConfiguration.getBeans().put("myListener", new ExecuteCommandTaskListener());

    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    disableAuthorization();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
    enableAuthorization();
  }

  @Deployment
  @Test
  void testTaskListenerExecutesQueryAfterUserCompletesTaskAsExpression() {
    // given
    processEngineConfiguration.getBeans().put("myListener", new ExecuteQueryTaskListener());

    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testTaskListenerExecutesCommandAfterUserCompletesTaskAsExpression() {
    // given
    processEngineConfiguration.getBeans().put("myListener", new ExecuteCommandTaskListener());

    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    disableAuthorization();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
    enableAuthorization();
  }

  @Deployment
  @Test
  void testTaskAssigneeExpression() {
    // given
    processEngineConfiguration.getBeans().put("myTaskService", new MyTaskService());

    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testScriptTaskExecutesQueryAfterUserCompletesTask() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    Task task = selectAnyTask();

    String taskId = task.getId();
    String processInstanceId = task.getProcessInstanceId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    disableAuthorization();

    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstanceId);

    VariableInstance variableUser = query
        .variableName("userId")
        .singleResult();
    assertThat(variableUser).isNotNull();
    assertThat(variableUser.getValue()).isEqualTo(userId);

    VariableInstance variableCount = query
        .variableName("count")
        .singleResult();
    assertThat(variableCount).isNotNull();
    assertThat(variableCount.getValue()).isEqualTo(5l);

    enableAuthorization();
  }

  @Deployment
  @Test
  void testScriptTaskExecutesCommandAfterUserCompletesTask() {
    // given
    String processInstanceId = startProcessInstanceByKey(DEFAULT_PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    disableAuthorization();

    VariableInstance variableUser = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstanceId)
        .variableName("userId")
        .singleResult();
    assertThat(variableUser).isNotNull();
    assertThat(variableUser.getValue()).isEqualTo(userId);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);

    enableAuthorization();
  }

  @Deployment
  @Test
  void testScriptExecutionListenerExecutesQueryAfterUserCompletesTask() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    Task task = selectAnyTask();

    String taskId = task.getId();
    String processInstanceId = task.getProcessInstanceId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    disableAuthorization();

    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstanceId);

    VariableInstance variableUser = query
        .variableName("userId")
        .singleResult();
    assertThat(variableUser).isNotNull();
    assertThat(variableUser.getValue()).isEqualTo(userId);

    VariableInstance variableCount = query
        .variableName("count")
        .singleResult();
    assertThat(variableCount).isNotNull();
    assertThat(variableCount.getValue()).isEqualTo(5l);

    enableAuthorization();
  }

  @Deployment
  @Test
  void testScriptExecutionListenerExecutesCommandAfterUserCompletesTask() {
    // given
    String processInstanceId = startProcessInstanceByKey(DEFAULT_PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    disableAuthorization();

    VariableInstance variableUser = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstanceId)
        .variableName("userId")
        .singleResult();
    assertThat(variableUser).isNotNull();
    assertThat(variableUser.getValue()).isEqualTo(userId);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);

    enableAuthorization();
  }

  @Deployment
  @Test
  void testScriptTaskListenerExecutesQueryAfterUserCompletesTask() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    Task task = selectAnyTask();

    String taskId = task.getId();
    String processInstanceId = task.getProcessInstanceId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    disableAuthorization();

    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstanceId);

    VariableInstance variableUser = query
        .variableName("userId")
        .singleResult();
    assertThat(variableUser).isNotNull();
    assertThat(variableUser.getValue()).isEqualTo(userId);

    VariableInstance variableCount = query
        .variableName("count")
        .singleResult();
    assertThat(variableCount).isNotNull();
    assertThat(variableCount.getValue()).isEqualTo(5l);

    enableAuthorization();
  }

  @Deployment
  @Test
  void testScriptTaskListenerExecutesCommandAfterUserCompletesTask() {
    // given
    String processInstanceId = startProcessInstanceByKey(DEFAULT_PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    disableAuthorization();

    VariableInstance variableUser = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstanceId)
        .variableName("userId")
        .singleResult();
    assertThat(variableUser).isNotNull();
    assertThat(variableUser.getValue()).isEqualTo(userId);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);

    enableAuthorization();
  }

  @Deployment
  @Test
  void testScriptConditionExecutesQueryAfterUserCompletesTask() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    Task task = selectAnyTask();

    String taskId = task.getId();
    String processInstanceId = task.getProcessInstanceId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    disableAuthorization();

    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstanceId);

    VariableInstance variableUser = query
        .variableName("userId")
        .singleResult();
    assertThat(variableUser).isNotNull();
    assertThat(variableUser.getValue()).isEqualTo(userId);

    VariableInstance variableCount = query
        .variableName("count")
        .singleResult();
    assertThat(variableCount).isNotNull();
    assertThat(variableCount.getValue()).isEqualTo(5l);

    enableAuthorization();
  }

  @Deployment
  @Test
  void testScriptConditionExecutesCommandAfterUserCompletesTask() {
    // given
    String processInstanceId = startProcessInstanceByKey(DEFAULT_PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    disableAuthorization();

    VariableInstance variableUser = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstanceId)
        .variableName("userId")
        .singleResult();
    assertThat(variableUser).isNotNull();
    assertThat(variableUser.getValue()).isEqualTo(userId);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);

    enableAuthorization();
  }

  @Deployment
  @Test
  void testScriptIoMappingExecutesQueryAfterUserCompletesTask() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    Task task = selectAnyTask();

    String taskId = task.getId();
    String processInstanceId = task.getProcessInstanceId();

    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    disableAuthorization();

    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstanceId);

    VariableInstance variableUser = query
        .variableName("userId")
        .singleResult();
    assertThat(variableUser).isNotNull();
    assertThat(variableUser.getValue()).isEqualTo(userId);

    VariableInstance variableCount = query
        .variableName("count")
        .singleResult();
    assertThat(variableCount).isNotNull();
    assertThat(variableCount.getValue()).isEqualTo(5l);

    enableAuthorization();
  }

  @Deployment
  @Test
  void testScriptIoMappingExecutesCommandAfterUserCompletesTask() {
    // given
    String processInstanceId = startProcessInstanceByKey(DEFAULT_PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    disableAuthorization();

    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstanceId);

    VariableInstance variableUser = query
        .variableName("userId")
        .singleResult();
    assertThat(variableUser).isNotNull();
    assertThat(variableUser.getValue()).isEqualTo(userId);

    VariableInstance variableCount = query
        .variableName("count")
        .singleResult();
    assertThat(variableCount).isNotNull();
    assertThat(variableCount.getValue()).isEqualTo(1l);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);

    enableAuthorization();
  }

  @Deployment
  @Test
  void testCustomStartFormHandlerExecutesQuery() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);

    String processDefinitionId = selectProcessDefinitionByKey(DEFAULT_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, DEFAULT_PROCESS_KEY, userId, READ);

    // when
    StartFormData startFormData = formService.getStartFormData(processDefinitionId);

    // then
    assertThat(startFormData).isNotNull();

    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testCustomTaskFormHandlerExecutesQuery() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);

    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, READ);

    // when
    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    // then
    assertThat(taskFormData).isNotNull();

    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/authorization/DelegationAuthorizationTest.testCustomStartFormHandlerExecutesQuery.bpmn20.xml"})
  @Test
  void testSubmitCustomStartFormHandlerExecutesQuery() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);

    String processDefinitionId = selectProcessDefinitionByKey(DEFAULT_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, DEFAULT_PROCESS_KEY, userId, CREATE_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, CREATE);

    // when
    formService.submitStartForm(processDefinitionId, null);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/authorization/DelegationAuthorizationTest.testCustomTaskFormHandlerExecutesQuery.bpmn20.xml"})
  @Test
  void testSubmitCustomTaskFormHandlerExecutesQuery() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);

    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    formService.submitTaskForm(taskId, null);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testCustomFormFieldValidator() {
    // given
    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);

    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    formService.submitTaskForm(taskId, null);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment
  @Test
  void testCustomFormFieldValidatorAsDelegateExpression() {
    // given
    processEngineConfiguration.getBeans().put("myValidator", new MyFormFieldValidator());

    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);

    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    formService.submitTaskForm(taskId, null);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(5));
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/authorization/DelegationAuthorizationTest.testJavaDelegateExecutesQueryAfterUserCompletesTask.bpmn20.xml"})
  @Test
  void testPerformAuthorizationCheckByExecutingQuery() {
    // given
    processEngineConfiguration.setAuthorizationEnabledForCustomCode(true);

    startProcessInstancesByKey(DEFAULT_PROCESS_KEY, 5);
    String taskId = selectAnyTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when
    taskService.complete(taskId);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    assertThat(MyDelegationService.instancesCount).isEqualTo(Long.valueOf(0));
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/authorization/DelegationAuthorizationTest.testJavaDelegateExecutesCommandAfterUserCompletesTask.bpmn20.xml"})
  @Test
  void testPerformAuthorizationCheckByExecutingCommand() {
    // given
    processEngineConfiguration.setAuthorizationEnabledForCustomCode(true);

    startProcessInstanceByKey(DEFAULT_PROCESS_KEY);
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    assertThatThrownBy(() -> taskService.complete(taskId), "It should not be possible to execute the command inside JavaDelegate")
        .isInstanceOf(AuthorizationException.class);

    // then
    assertThat(MyDelegationService.currentAuthentication).isNotNull();
    assertThat(MyDelegationService.currentAuthentication.getUserId()).isEqualTo(userId);

    disableAuthorization();
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
    enableAuthorization();
  }

  @Deployment
  @Test
  void testTaskListenerOnCreateAssignsTask() {
    // given
    String processInstanceId = startProcessInstanceByKey(DEFAULT_PROCESS_KEY).getId();
    String taskId = selectSingleTask().getId();
    createGrantAuthorization(TASK, taskId, userId, UPDATE);

    // when (1)
    taskService.complete(taskId);

    // then (1)
    identityService.clearAuthentication();
    identityService.setAuthentication("demo", null);

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    // when (2)
    taskService.complete(task.getId());

    // then (2)
    testRule.assertProcessEnded(processInstanceId);
  }

  // helper /////////////////////////////////////////////////////////////////////////

  protected void startProcessInstancesByKey(String key, int count) {
    for (int i = 0; i < count; i++) {
      startProcessInstanceByKey(key);
    }
  }

  protected Task selectAnyTask() {
    disableAuthorization();
    Task task = taskService.createTaskQuery().listPage(0, 1).get(0);
    enableAuthorization();
    return task;
  }

}
