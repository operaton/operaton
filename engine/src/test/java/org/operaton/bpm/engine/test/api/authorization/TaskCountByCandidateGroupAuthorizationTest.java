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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.task.TaskCountByCandidateGroupResult;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Resources.TASK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Stefan Hentschel.
 */
class TaskCountByCandidateGroupAuthorizationTest {

  @RegisterExtension
  static ProcessEngineExtension processEngineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension processEngineTestRule = new ProcessEngineTestExtension(processEngineRule);


  protected TaskService taskService;
  protected IdentityService identityService;
  protected AuthorizationService authorizationService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  protected String userId = "user";

  @BeforeEach
  void setUp() {
    taskService = processEngineRule.getTaskService();
    identityService = processEngineRule.getIdentityService();
    authorizationService = processEngineRule.getAuthorizationService();
    processEngineConfiguration = processEngineRule.getProcessEngineConfiguration();
  }

  @Test
  void shouldFetchTaskCountWithAuthorization() {
    // given
    User user = identityService.newUser(userId);
    identityService.saveUser(user);

    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.addPermission(READ);
    authorization.setResource(TASK);
    authorization.setResourceId(ANY);
    authorization.setUserId(userId);
    authorizationService.saveAuthorization(authorization);

    processEngineConfiguration.setAuthorizationEnabled(true);
    authenticate();

    // when
    List<TaskCountByCandidateGroupResult> results = taskService.createTaskReport().taskCountByCandidateGroup();
    processEngineConfiguration.setAuthorizationEnabled(false);
    authorizationService.deleteAuthorization(authorization.getId());
    identityService.deleteUser(userId);

    assertThat(results).isEmpty();
  }

  @Test
  void shouldFailToFetchTaskCountWithMissingAuthorization() {
    // given
    boolean testFailed = false;
    processEngineConfiguration.setAuthorizationEnabled(true);
    authenticate();

    // when
    try {
      taskService.createTaskReport().taskCountByCandidateGroup();
      testFailed = true;

    } catch (AuthorizationException aex) {
      if (!aex.getMessage().contains(userId + "' does not have 'READ' permission on resource '*' of type 'Task'")) {
        testFailed = true;
      }
    }

    // then
    processEngineConfiguration.setAuthorizationEnabled(false);

    if (testFailed) {
      fail("There should be an authorization exception for '%s' because of a missing 'READ' permission on 'Task'.".formatted(userId));
    }
  }

  protected void authenticate() {
    identityService.setAuthentication(userId, null, null);
  }
}
