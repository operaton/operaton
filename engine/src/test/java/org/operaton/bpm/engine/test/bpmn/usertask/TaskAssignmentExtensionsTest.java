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
package org.operaton.bpm.engine.test.bpmn.usertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.test.TestHelper;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * Testcase for the non-spec extensions to the task candidate use case.
 * 
 * @author Joram Barrez
 */
class TaskAssignmentExtensionsTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  RepositoryService repositoryService;
  IdentityService identityService;
  TaskService taskService;

  @BeforeEach
  void setUp() {
    identityService.saveUser(identityService.newUser("kermit"));
    identityService.saveUser(identityService.newUser("gonzo"));
    identityService.saveUser(identityService.newUser("fozzie"));

    identityService.saveGroup(identityService.newGroup("management"));
    identityService.saveGroup(identityService.newGroup("accountancy"));

    identityService.createMembership("kermit", "management");
    identityService.createMembership("kermit", "accountancy");
    identityService.createMembership("fozzie", "management");
  }

  @AfterEach
  void tearDown() {
    identityService.deleteGroup("accountancy");
    identityService.deleteGroup("management");
    identityService.deleteUser("fozzie");
    identityService.deleteUser("gonzo");
    identityService.deleteUser("kermit");
  }

  @Deployment
  @Test
  void testAssigneeExtension() {
    runtimeService.startProcessInstanceByKey("assigneeExtension");
    List<Task> tasks = taskService
      .createTaskQuery()
      .taskAssignee("kermit")
      .list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("my task");
  }

  @Test
  void testDuplicateAssigneeDeclaration() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testDuplicateAssigneeDeclaration");
    var deploymentBuilder = repositoryService.createDeployment().addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Invalid BPMN 2.0 process should not parse, but it gets parsed sucessfully");
    } catch (ProcessEngineException e) {
      // Exception is to be expected
    }
  }

  @Deployment
  @Test
  void testCandidateUsersExtension() {
    runtimeService.startProcessInstanceByKey("candidateUsersExtension");
    List<Task> tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
    assertThat(tasks).hasSize(1);
    tasks = taskService.createTaskQuery().taskCandidateUser("gonzo").list();
    assertThat(tasks).hasSize(1);
  }

  @Deployment
  @Test
  void testCandidateGroupsExtension() {
    runtimeService.startProcessInstanceByKey("candidateGroupsExtension");

    // Bugfix check: potentially the query could return 2 tasks since
    // kermit is a member of the two candidate groups
    List<Task> tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("make profit");

    tasks = taskService.createTaskQuery().taskCandidateUser("fozzie").list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("make profit");

    // Test the task query find-by-candidate-group operation
    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.taskCandidateGroup("management").count()).isEqualTo(1);
    assertThat(query.taskCandidateGroup("accountancy").count()).isEqualTo(1);
  }

  // Test where the candidate user extension is used together
  // with the spec way of defining candidate users
  @Deployment
  @Test
  void testMixedCandidateUserDefinition() {
    runtimeService.startProcessInstanceByKey("mixedCandidateUser");

    List<Task> tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
    assertThat(tasks).hasSize(1);

    tasks = taskService.createTaskQuery().taskCandidateUser("fozzie").list();
    assertThat(tasks).hasSize(1);

    tasks = taskService.createTaskQuery().taskCandidateUser("gonzo").list();
    assertThat(tasks).hasSize(1);

    tasks = taskService.createTaskQuery().taskCandidateUser("mispiggy").list();
    assertThat(tasks).isEmpty();
  }

}
