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
package org.operaton.bpm.qa.upgrade.scenarios7110.useroperationlog;


import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.ProcessEngineRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.authorization.Permissions.DELETE_HISTORY;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.junit.Assert.assertEquals;

/**
 * @author Yana.Vasileva
 *
 */
public class SuspendProcessDefinitionDeleteAuthorizationTest {

  private static final String USER_ID = "jane" + "SuspendProcessDefinitionDelete";

  @Rule
  public ProcessEngineRule engineRule = new ProcessEngineRule("operaton.cfg.xml");

  protected HistoryService historyService;
  protected AuthorizationService authorizationService;
  protected ProcessEngineConfigurationImpl engineConfiguration;

  @Before
  public void setUp() {
    historyService = engineRule.getHistoryService();
    authorizationService = engineRule.getAuthorizationService();
    engineConfiguration = engineRule.getProcessEngineConfiguration();

    engineRule.getIdentityService().setAuthenticatedUserId(USER_ID);
  }

  @After
  public void tearDown() {
    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(false);
    engineRule.getIdentityService().clearAuthentication();

    List<Authorization> auths = authorizationService.createAuthorizationQuery().userIdIn(USER_ID).list();
    for (Authorization authorization : auths) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
  }

  @Test
  public void testWithoutAuthorization() {
    // given
    UserOperationLogQuery query = historyService.createUserOperationLogQuery()
        .processDefinitionKey("timerBoundaryProcess")
        .afterTimestamp(new Date(1549110000000L));

    // assume
    assertEquals(1L, query.count());
    UserOperationLogEntry entry = query.singleResult();

    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(true);

    // given
    var entryId = entry.getId();

    // when/then
    assertThatThrownBy(() -> historyService.deleteUserOperationLogEntry(entryId))
      .isInstanceOf(AuthorizationException.class)
      .satisfies(e -> {
        var exception = (AuthorizationException) e;
        var message = exception.getMessage();
        assertThat(message).contains(USER_ID);
        assertThat(message).contains(DELETE_HISTORY.getName());
        assertThat(message).contains(PROCESS_DEFINITION.resourceName());
        assertThat(message).contains("timerBoundaryProcess");
      });
  }

  @Test
  public void testWithDeleteHistoryPermissionOnAnyProcessDefinition() {
    // given
    UserOperationLogQuery query = historyService.createUserOperationLogQuery()
        .processDefinitionKey("timerBoundaryProcess")
        .beforeTimestamp(new Date(1549110000000L));

    // assume
    assertTrue(query.count() == 1 || query.count() == 2);

    Authorization auth = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    auth.setUserId(USER_ID);
    auth.setPermissions(new Permissions[] {Permissions.DELETE_HISTORY});
    auth.setResource(Resources.PROCESS_DEFINITION);
    auth.setResourceId("*");

    authorizationService.saveAuthorization(auth);
    String logId = query.list().get(0).getId();
    String processInstanceId = query.list().get(0).getProcessInstanceId();
    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(true);

    // when
    historyService.deleteUserOperationLogEntry(logId);

    // then
    assertEquals(0, query.processInstanceId(processInstanceId).count());
  }

  @Test
  public void testWithDeleteHistoryPermissionOnProcessDefinition() {
    // given
    UserOperationLogQuery query = historyService.createUserOperationLogQuery()
        .processDefinitionKey("timerBoundaryProcess")
        .beforeTimestamp(new Date(1549110000000L));

    // assume
    assertTrue(query.count() == 1 || query.count() == 2);

    String logId = query.list().get(0).getId();
    String processInstanceId = query.list().get(0).getProcessInstanceId();
    Authorization auth = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    auth.setUserId(USER_ID);
    auth.setPermissions(new Permissions[] {Permissions.DELETE_HISTORY});
    auth.setResource(Resources.PROCESS_DEFINITION);
    auth.setResourceId("timerBoundaryProcess");

    authorizationService.saveAuthorization(auth);

    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(true);

    // when
    historyService.deleteUserOperationLogEntry(logId);

    // then
    assertEquals(0, query.processInstanceId(processInstanceId).count());
  }
}
