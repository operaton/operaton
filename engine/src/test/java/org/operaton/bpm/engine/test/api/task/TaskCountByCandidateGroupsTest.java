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
package org.operaton.bpm.engine.test.api.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskCountByCandidateGroupResult;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 * @author Stefan Hentschel
 *
 */
class TaskCountByCandidateGroupsTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);


  TaskService taskService;
  IdentityService identityService;
  AuthorizationService authorizationService;
  ProcessEngineConfiguration processEngineConfiguration;

  protected String userId = "user";
  protected List<String> tasks = new ArrayList<>();
  protected List<String> tenants = List.of("tenant1", "tenant2");
  protected List<String> groups = List.of("aGroupId", "anotherGroupId");


  @BeforeEach
  void setUp() {
    createTask(groups.get(0), tenants.get(0));
    createTask(groups.get(0), tenants.get(1));
    createTask(groups.get(1), tenants.get(1));
    createTask(null, tenants.get(1));
  }

  @AfterEach
  void cleanUp() {
    for (String taskId : tasks) {
      taskService.deleteTask(taskId, true);
    }
  }

  @Test
  void shouldReturnTaskCountsByGroup() {
    // when
    List<TaskCountByCandidateGroupResult> results = taskService.createTaskReport().taskCountByCandidateGroup();

    // then
    assertThat(results).hasSize(3);
  }

  @Test
  void shouldProvideTaskCountForEachGroup() {
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
  void shouldProvideGroupNameForEachGroup() {
    // when
    List<TaskCountByCandidateGroupResult> results = taskService.createTaskReport().taskCountByCandidateGroup();

    // then
    for (TaskCountByCandidateGroupResult result : results ) {
      assertThat(checkResultName(result)).isTrue();
    }
  }

  @Test
  void shouldFetchCountOfTasksWithoutAssignee() {
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
    if (Objects.equals(result.getGroupName(), expectedResultName)) {
      assertThat(result.getTaskCount()).isEqualTo(expectedResultCount);
    }
  }

  protected boolean checkResultName(TaskCountByCandidateGroupResult result) {
    return result.getGroupName() == null ||
           result.getGroupName().equals(groups.get(0)) ||
           result.getGroupName().equals(groups.get(1));
  }
}
