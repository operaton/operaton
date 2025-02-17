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
package org.operaton.bpm.engine.test.api.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskCountByCandidateGroupResult;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author Daniel Meyer
 * @author Stefan Hentschel
 *
 */
public class TaskCountByCandidateGroupsTest {

  public ProcessEngineRule processEngineRule = new ProvidedProcessEngineRule();
  public ProcessEngineTestRule processEngineTestRule = new ProcessEngineTestRule(processEngineRule);

  @Rule
  public RuleChain ruleChain = RuleChain
    .outerRule(processEngineTestRule)
    .around(processEngineRule);


  protected TaskService taskService;
  protected IdentityService identityService;
  protected AuthorizationService authorizationService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  protected String userId = "user";
  protected List<String> tasks = new ArrayList<>();
  protected List<String> tenants = Arrays.asList("tenant1", "tenant2");
  protected List<String> groups = Arrays.asList("aGroupId", "anotherGroupId");


  @Before
  public void setUp() {
    taskService = processEngineRule.getTaskService();
    identityService = processEngineRule.getIdentityService();
    authorizationService = processEngineRule.getAuthorizationService();
    processEngineConfiguration = processEngineRule.getProcessEngineConfiguration();

    createTask(groups.get(0), tenants.get(0));
    createTask(groups.get(0), tenants.get(1));
    createTask(groups.get(1), tenants.get(1));
    createTask(null, tenants.get(1));
  }

  @After
  public void cleanUp() {
    for (String taskId : tasks) {
      taskService.deleteTask(taskId, true);
    }
  }

  @Test
  public void shouldReturnTaskCountsByGroup() {
    // when
    List<TaskCountByCandidateGroupResult> results = taskService.createTaskReport().taskCountByCandidateGroup();

    // then
    assertThat(results).hasSize(3);
  }

  @Test
  public void shouldProvideTaskCountForEachGroup() {
    // when
    List<TaskCountByCandidateGroupResult> results = taskService.createTaskReport().taskCountByCandidateGroup();

    // then
    for (TaskCountByCandidateGroupResult result : results ) {
      checkResultCount(result, null, 1);
      checkResultCount(result, groups.get(0), 2);
      checkResultCount(result, groups.get(1), 1);
    }
  }

  @Test
  public void shouldProvideGroupNameForEachGroup() {
    // when
    List<TaskCountByCandidateGroupResult> results = taskService.createTaskReport().taskCountByCandidateGroup();

    // then
    for (TaskCountByCandidateGroupResult result : results ) {
      assertThat(checkResultName(result)).isTrue();
    }
  }

  @Test
  public void shouldFetchCountOfTasksWithoutAssignee() {
    // given
    User user = identityService.newUser(userId);
    identityService.saveUser(user);

    // when
    taskService.delegateTask(tasks.get(2), userId);
    List<TaskCountByCandidateGroupResult> results = taskService.createTaskReport().taskCountByCandidateGroup();

    identityService.deleteUser(userId);

    // then
    assertThat(results).hasSize(2);
  }

  protected void createTask(String groupId, String tenantId) {
    Task task = taskService.newTask();
    task.setTenantId(tenantId);
    taskService.saveTask(task);

    if (groupId != null) {
      taskService.addCandidateGroup(task.getId(), groupId);
    }

    tasks.add(task.getId());
  }

  protected void checkResultCount(TaskCountByCandidateGroupResult result, String expectedResultName, int expectedResultCount) {
    if((expectedResultName == null && result.getGroupName() == null) ||
       (result.getGroupName() != null && result.getGroupName().equals(expectedResultName))) {
      assertThat(result.getTaskCount()).isEqualTo(expectedResultCount);
    }
  }

  protected boolean checkResultName(TaskCountByCandidateGroupResult result) {
    return result.getGroupName() == null ||
           result.getGroupName().equals(groups.get(0)) ||
           result.getGroupName().equals(groups.get(1));
  }
}
