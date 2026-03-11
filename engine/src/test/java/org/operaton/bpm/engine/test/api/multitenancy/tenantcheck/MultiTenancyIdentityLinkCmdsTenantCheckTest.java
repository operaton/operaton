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
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.task.IdentityLinkType;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyIdentityLinkCmdsTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";

  protected static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";

  protected static final BpmnModelInstance ONE_TASK_PROCESS = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
    .startEvent()
    .userTask()
    .endEvent()
    .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected TaskService taskService;
  protected IdentityService identityService;

  protected Task task;
  String taskId;

  @BeforeEach
  void init() {

    testRule.deployForTenant(TENANT_ONE, ONE_TASK_PROCESS);

    engineRule.getRuntimeService().startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();

    task = engineRule.getTaskService().createTaskQuery().singleResult();
    taskId = task.getId();

    taskService = engineRule.getTaskService();
    identityService = engineRule.getIdentityService();
  }

  // set Assignee
  @Test
  void setAssigneeForTaskWithAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    taskService.setAssignee(taskId, "demo");

    // then
    assertThat(taskService.createTaskQuery().taskAssignee("demo").count()).isOne();
  }

  @Test
  void setAssigneeForTaskWithNoAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.setAssignee(taskId, "demo"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot assign the task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void setAssigneeForTaskWithDisabledTenantCheck() {

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    taskService.setAssignee(taskId, "demo");
    // then
    assertThat(taskService.createTaskQuery().taskAssignee("demo").count()).isOne();
  }

  // set owner test cases
  @Test
  void setOwnerForTaskWithAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    taskService.setOwner(taskId, "demo");

    // then
    assertThat(taskService.createTaskQuery().taskOwner("demo").count()).isOne();
  }

  @Test
  void setOwnerForTaskWithNoAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.setOwner(taskId, "demo"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot assign the task '"
          + taskId + "' because it belongs to no authenticated tenant.");

  }

  @Test
  void setOwnerForTaskWithDisabledTenantCheck() {

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    taskService.setOwner(taskId, "demo");
    // then
    assertThat(taskService.createTaskQuery().taskOwner("demo").count()).isOne();
  }

  // get identity links
  @Test
  void getIdentityLinkWithAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    taskService.setOwner(taskId, "demo");

    assertThat(taskService.getIdentityLinksForTask(taskId).get(0).getType()).isEqualTo("owner");
  }

  @Test
  void getIdentityLinkWitNoAuthenticatedTenant() {

    taskService.setOwner(taskId, "demo");
    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.getIdentityLinksForTask(taskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot read the task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void getIdentityLinkWithDisabledTenantCheck() {

    taskService.setOwner(taskId, "demo");
    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // then
    assertThat(taskService.getIdentityLinksForTask(taskId).get(0).getType()).isEqualTo("owner");

  }

  // add candidate user
  @Test
  void addCandidateUserWithAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    taskService.addCandidateUser(taskId, "demo");

    // then
    assertThat(taskService.createTaskQuery().taskCandidateUser("demo").count()).isOne();
  }

  @Test
  void addCandidateUserWithNoAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.addCandidateUser(taskId, "demo"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot assign the task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void addCandidateUserWithDisabledTenantCheck() {

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // when
    taskService.addCandidateUser(taskId, "demo");

    // then
    assertThat(taskService.createTaskQuery().taskCandidateUser("demo").count()).isOne();
  }

  // add candidate group
  @Test
  void addCandidateGroupWithAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    taskService.addCandidateGroup(taskId, "demo");

    // then
    assertThat(taskService.createTaskQuery().taskCandidateGroup("demo").count()).isOne();
  }

  @Test
  void addCandidateGroupWithNoAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.addCandidateGroup(taskId, "demo"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot assign the task '"
          +taskId+ "' because it belongs to no authenticated tenant.");

  }

  @Test
  void addCandidateGroupWithDisabledTenantCheck() {

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // when
    taskService.addCandidateGroup(taskId, "demo");

    // then
    assertThat(taskService.createTaskQuery().taskCandidateGroup("demo").count()).isOne();
  }

  // delete candidate users
  @Test
  void deleteCandidateUserWithAuthenticatedTenant() {

    taskService.addCandidateUser(taskId, "demo");
    assertThat(taskService.createTaskQuery().taskCandidateUser("demo").count()).isOne();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    taskService.deleteCandidateUser(taskId, "demo");
    // then
    assertThat(taskService.createTaskQuery().taskCandidateUser("demo").count()).isZero();
  }

  @Test
  void deleteCandidateUserWithNoAuthenticatedTenant() {

    taskService.addCandidateUser(taskId, "demo");
    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.deleteCandidateUser(taskId, "demo"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot assign the task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void deleteCandidateUserWithDisabledTenantCheck() {

    taskService.addCandidateUser(taskId, "demo");
    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // when
    taskService.deleteCandidateUser(taskId, "demo");

    // then
    assertThat(taskService.createTaskQuery().taskCandidateUser("demo").count()).isZero();
  }

  // delete candidate groups
  @Test
  void deleteCandidateGroupWithAuthenticatedTenant() {

    taskService.addCandidateGroup(taskId, "demo");
    assertThat(taskService.createTaskQuery().taskCandidateGroup("demo").count()).isOne();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    taskService.deleteCandidateGroup(taskId, "demo");
    // then
    assertThat(taskService.createTaskQuery().taskCandidateGroup("demo").count()).isZero();
  }

  @Test
  void deleteCandidateGroupWithNoAuthenticatedTenant() {

    taskService.addCandidateGroup(taskId, "demo");
    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.deleteCandidateGroup(taskId, "demo"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot assign the task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void deleteCandidateGroupWithDisabledTenantCheck() {

    taskService.addCandidateGroup(taskId, "demo");
    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // when
    taskService.deleteCandidateGroup(taskId, "demo");

    // then
    assertThat(taskService.createTaskQuery().taskCandidateGroup("demo").count()).isZero();
  }

  // add user identity link
  @Test
  void addUserIdentityLinkWithAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    assertThat(taskService.createTaskQuery().taskCandidateUser("demo").count()).isOne();
  }

  @Test
  void addUserIdentityLinkWithNoAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot assign the task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void addUserIdentityLinkWithDisabledTenantCheck() {

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // when
    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.ASSIGNEE);

    // then
    assertThat(taskService.createTaskQuery().taskAssignee("demo").count()).isOne();
  }

  // add group identity link
  @Test
  void addGroupIdentityLinkWithAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));
    taskService.addGroupIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    assertThat(taskService.createTaskQuery().taskCandidateGroup("demo").count()).isOne();
  }

  @Test
  void addGroupIdentityLinkWithNoAuthenticatedTenant() {

    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.addGroupIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot assign the task '"
          + taskId + "' because it belongs to no authenticated tenant.");

  }

  @Test
  void addGroupIdentityLinkWithDisabledTenantCheck() {

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // when
    taskService.addGroupIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    assertThat(taskService.createTaskQuery().taskCandidateGroup("demo").count()).isOne();
  }

  // delete user identity link
  @Test
  void deleteUserIdentityLinkWithAuthenticatedTenant() {

    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.ASSIGNEE);
    assertThat(taskService.createTaskQuery().taskAssignee("demo").count()).isOne();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.ASSIGNEE);
    // then
    assertThat(taskService.createTaskQuery().taskAssignee("demo").count()).isZero();
  }

  @Test
  void deleteUserIdentityLinkWithNoAuthenticatedTenant() {

    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.ASSIGNEE);
    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.ASSIGNEE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot assign the task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void deleteUserIdentityLinkWithDisabledTenantCheck() {

    taskService.addUserIdentityLink(taskId, "demo", IdentityLinkType.ASSIGNEE);
    assertThat(taskService.createTaskQuery().taskAssignee("demo").count()).isOne();

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // when
    taskService.deleteUserIdentityLink(taskId, "demo", IdentityLinkType.ASSIGNEE);

    // then
    assertThat(taskService.createTaskQuery().taskAssignee("demo").count()).isZero();
  }

  // delete group identity link
  @Test
  void deleteGroupIdentityLinkWithAuthenticatedTenant() {

    taskService.addGroupIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);
    assertThat(taskService.createTaskQuery().taskCandidateGroup("demo").count()).isOne();

    identityService.setAuthentication("aUserId", null, List.of(TENANT_ONE));

    taskService.deleteGroupIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);
    // then
    assertThat(taskService.createTaskQuery().taskCandidateGroup("demo").count()).isZero();
  }

  @Test
  void deleteGroupIdentityLinkWithNoAuthenticatedTenant() {

    taskService.addGroupIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);
    identityService.setAuthentication("aUserId", null);

    // when/then
    assertThatThrownBy(() -> taskService.deleteGroupIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot assign the task '"
          + taskId +"' because it belongs to no authenticated tenant.");

  }

  @Test
  void deleteGroupIdentityLinkWithDisabledTenantCheck() {

    taskService.addGroupIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);
    assertThat(taskService.createTaskQuery().taskCandidateGroup("demo").count()).isOne();

    identityService.setAuthentication("aUserId", null);
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);

    // when
    taskService.deleteGroupIdentityLink(taskId, "demo", IdentityLinkType.CANDIDATE);

    // then
    assertThat(taskService.createTaskQuery().taskCandidateGroup("demo").count()).isZero();
  }
}
