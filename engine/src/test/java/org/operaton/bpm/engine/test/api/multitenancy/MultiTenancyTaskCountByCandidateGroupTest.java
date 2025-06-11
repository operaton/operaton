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
package org.operaton.bpm.engine.test.api.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskCountByCandidateGroupResult;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * @author Stefan Hentschel.
 */
class MultiTenancyTaskCountByCandidateGroupTest {

  @RegisterExtension
  protected static ProcessEngineExtension processEngineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension procesessEngineTestRule = new ProcessEngineTestExtension(processEngineRule);

  protected TaskService taskService;
  protected IdentityService identityService;
  protected AuthorizationService authorizationService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  protected String userId = "aUser";
  protected String groupId = "aGroup";
  protected String tenantId = "aTenant";
  protected String anotherTenantId = "anotherTenant";

  protected List<String> taskIds = new ArrayList<>();

  @BeforeEach
  void setUp() {
    createTask(groupId, tenantId);
    createTask(groupId, anotherTenantId);
    createTask(groupId, anotherTenantId);

    processEngineConfiguration.setTenantCheckEnabled(true);
  }

  @AfterEach
  void cleanUp() {
    processEngineConfiguration.setTenantCheckEnabled(false);

    for (String taskId : taskIds) {
      taskService.deleteTask(taskId, true);
    }
  }

  @Test
  void shouldOnlyShowTenantSpecificTasks() {
    // given

    identityService.setAuthentication(userId, null, Collections.singletonList(tenantId));

    // when
    List<TaskCountByCandidateGroupResult> results = taskService.createTaskReport().taskCountByCandidateGroup();

    // then
    assertThat(results).hasSize(1);
  }

  protected void createTask(String groupId, String tenantId) {
    Task task = taskService.newTask();
    task.setTenantId(tenantId);
    taskService.saveTask(task);

    if (groupId != null) {
      taskService.addCandidateGroup(task.getId(), groupId);
      taskIds.add(task.getId());
    }
  }
}
