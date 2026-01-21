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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.impl.TaskQueryImpl;
import org.operaton.bpm.engine.impl.db.AuthorizationCheck;
import org.operaton.bpm.engine.impl.db.entitymanager.DbEntityManager;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.Session;
import org.operaton.bpm.engine.impl.persistence.entity.AuthorizationManager;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GroupAuthorizationTest extends AuthorizationTest {

  public static final String TEST_USER_ID = "testUser";
  public static final List<String> TEST_GROUP_IDS = List.of("testGroup1", "testGroup2", "testGroup3");

  @BeforeEach
  @Override
  public void setUp() {
    createUser(TEST_USER_ID);
    for (String testGroupId : TEST_GROUP_IDS) {
      createGroupAndAddUser(testGroupId, TEST_USER_ID);
    }

    identityService.setAuthentication(TEST_USER_ID, TEST_GROUP_IDS);
    processEngineConfiguration.setAuthorizationEnabled(true);
  }


  @Test
  void testTaskQueryWithoutGroupAuthorizations() {
    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      AuthorizationManager authorizationManager = spyOnSession(commandContext, AuthorizationManager.class);

      TaskQueryImpl taskQuery = (TaskQueryImpl) spy(processEngine.getTaskService().createTaskQuery());
      AuthorizationCheck authCheck = spy(new AuthorizationCheck());
      when(taskQuery.getAuthCheck()).thenReturn(authCheck);

      taskQuery.list();

      verify(authorizationManager, atLeastOnce()).filterAuthenticatedGroupIds(TEST_GROUP_IDS);
      verify(authCheck).setAuthGroupIds(Collections.emptyList());

      return null;
    });
  }

  @Test
  void testTaskQueryWithOneGroupAuthorization() {
    createGroupGrantAuthorization(Resources.TASK, Authorization.ANY, TEST_GROUP_IDS.get(0));

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      AuthorizationManager authorizationManager = spyOnSession(commandContext, AuthorizationManager.class);

      TaskQueryImpl taskQuery = (TaskQueryImpl) spy(processEngine.getTaskService().createTaskQuery());
      AuthorizationCheck authCheck = spy(new AuthorizationCheck());
      when(taskQuery.getAuthCheck()).thenReturn(authCheck);

      taskQuery.list();

      verify(authorizationManager, atLeastOnce()).filterAuthenticatedGroupIds(TEST_GROUP_IDS);
      verify(authCheck).setAuthGroupIds(TEST_GROUP_IDS.subList(0, 1));

      return null;
    });
  }

  @Test
  void testTaskQueryWithGroupAuthorization() {
    for (String testGroupId : TEST_GROUP_IDS) {
      createGroupGrantAuthorization(Resources.TASK, Authorization.ANY, testGroupId);
    }

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      AuthorizationManager authorizationManager = spyOnSession(commandContext, AuthorizationManager.class);

      TaskQueryImpl taskQuery = (TaskQueryImpl) spy(processEngine.getTaskService().createTaskQuery());
      AuthorizationCheck authCheck = spy(new AuthorizationCheck());
      when(taskQuery.getAuthCheck()).thenReturn(authCheck);

      taskQuery.list();

      verify(authorizationManager, atLeastOnce()).filterAuthenticatedGroupIds(TEST_GROUP_IDS);
      verify(authCheck, atLeastOnce()).setAuthGroupIds(TEST_GROUP_IDS);
      return null;
    });
  }

  @Test
  void testTaskQueryWithUserWithoutGroups() {
    identityService.setAuthentication(TEST_USER_ID, null);

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      AuthorizationManager authorizationManager = spyOnSession(commandContext, AuthorizationManager.class);

      TaskQueryImpl taskQuery = (TaskQueryImpl) spy(processEngine.getTaskService().createTaskQuery());
      AuthorizationCheck authCheck = spy(new AuthorizationCheck());
      when(taskQuery.getAuthCheck()).thenReturn(authCheck);

      taskQuery.list();

      verify(authorizationManager, atLeastOnce()).filterAuthenticatedGroupIds((List<String>) null);
      verify(authCheck).setAuthGroupIds(Collections.emptyList());

      return null;
    });
  }

  @Test
  void testCheckAuthorizationWithoutGroupAuthorizations() {
    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      AuthorizationManager authorizationManager = spyOnSession(commandContext, AuthorizationManager.class);
      DbEntityManager dbEntityManager = spyOnSession(commandContext, DbEntityManager.class);

      authorizationService.isUserAuthorized(TEST_USER_ID, TEST_GROUP_IDS, Permissions.READ, Resources.TASK);

      verify(authorizationManager, atLeastOnce()).filterAuthenticatedGroupIds(TEST_GROUP_IDS);

      ArgumentCaptor<AuthorizationCheck> authorizationCheckArgument = forClass(AuthorizationCheck.class);
      verify(dbEntityManager).selectBoolean(eq("isUserAuthorizedForResource"), authorizationCheckArgument.capture());

      AuthorizationCheck authorizationCheck = authorizationCheckArgument.getValue();
      assertThat(authorizationCheck.getAuthGroupIds()).isEmpty();

      return null;
    });
  }

  @Test
  void testCheckAuthorizationWithOneGroupAuthorizations() {
    createGroupGrantAuthorization(Resources.TASK, Authorization.ANY, TEST_GROUP_IDS.get(0));

    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      AuthorizationManager authorizationManager = spyOnSession(commandContext, AuthorizationManager.class);
      DbEntityManager dbEntityManager = spyOnSession(commandContext, DbEntityManager.class);

      authorizationService.isUserAuthorized(TEST_USER_ID, TEST_GROUP_IDS, Permissions.READ, Resources.TASK);

      verify(authorizationManager, atLeastOnce()).filterAuthenticatedGroupIds(TEST_GROUP_IDS);

      ArgumentCaptor<AuthorizationCheck> authorizationCheckArgument = forClass(AuthorizationCheck.class);
      verify(dbEntityManager).selectBoolean(eq("isUserAuthorizedForResource"), authorizationCheckArgument.capture());

      AuthorizationCheck authorizationCheck = authorizationCheckArgument.getValue();
      assertThat(authorizationCheck.getAuthGroupIds()).isEqualTo(TEST_GROUP_IDS.subList(0, 1));

      return null;
    });
  }

  @Test
  void testCheckAuthorizationWithGroupAuthorizations() {
    for (String testGroupId : TEST_GROUP_IDS) {
      createGroupGrantAuthorization(Resources.TASK, Authorization.ANY, testGroupId);
    }

    processEngineConfiguration.getCommandExecutorTxRequired().execute((Command<Void>) commandContext -> {
      AuthorizationManager authorizationManager = spyOnSession(commandContext, AuthorizationManager.class);
      DbEntityManager dbEntityManager = spyOnSession(commandContext, DbEntityManager.class);

      authorizationService.isUserAuthorized(TEST_USER_ID, TEST_GROUP_IDS, Permissions.READ, Resources.TASK);

      verify(authorizationManager, atLeastOnce()).filterAuthenticatedGroupIds(TEST_GROUP_IDS);

      ArgumentCaptor<AuthorizationCheck> authorizationCheckArgument = forClass(AuthorizationCheck.class);
      verify(dbEntityManager).selectBoolean(eq("isUserAuthorizedForResource"), authorizationCheckArgument.capture());

      AuthorizationCheck authorizationCheck = authorizationCheckArgument.getValue();
      assertThat(authorizationCheck.getAuthGroupIds()).containsExactlyInAnyOrderElementsOf(TEST_GROUP_IDS);

      return null;
    });
  }

  @Test
  void testCheckAuthorizationWithUserWithoutGroups() {
    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      AuthorizationManager authorizationManager = spyOnSession(commandContext, AuthorizationManager.class);
      DbEntityManager dbEntityManager = spyOnSession(commandContext, DbEntityManager.class);

      authorizationService.isUserAuthorized(TEST_USER_ID, null, Permissions.READ, Resources.TASK);

      verify(authorizationManager, atLeastOnce()).filterAuthenticatedGroupIds((List<String>) null);

      ArgumentCaptor<AuthorizationCheck> authorizationCheckArgument = forClass(AuthorizationCheck.class);
      verify(dbEntityManager).selectBoolean(eq("isUserAuthorizedForResource"), authorizationCheckArgument.capture());

      AuthorizationCheck authorizationCheck = authorizationCheckArgument.getValue();
      assertThat(authorizationCheck.getAuthGroupIds()).isEmpty();

      return null;
    });
  }

  @Test
  void testCheckAuthorizationForNullHostileListOfGroups() {
    // given
    identityService.clearAuthentication();

    BpmnModelInstance process = Bpmn.createExecutableProcess("process").startEvent()
      .userTask("foo")
      .endEvent()
      .done();

    testRule.deploy(process);

    runtimeService.startProcessInstanceByKey("process");

    // a group authorization
    createGroupGrantAuthorization(Resources.TASK, Authorization.ANY, TEST_GROUP_IDS.get(0));

    // a user authorization (i.e. no group id set)
    // this authorization is important to reproduce the bug in CAM-14306
    createGrantAuthorization(Resources.TASK, Authorization.ANY, TEST_USER_ID, Permissions.READ);

    List<String> groupIds = new NullHostileList<>(TEST_GROUP_IDS);

    // when
    boolean isAuthorized = authorizationService.isUserAuthorized(TEST_USER_ID, groupIds, Permissions.READ, Resources.TASK);

    // then
    assertThat(isAuthorized).isTrue();
  }

  protected class NullHostileList<E> extends ArrayList<E> {

    public NullHostileList(Collection<E> other) {
      super(other);
    }

    @Override
    public boolean contains(Object o) {
      // lists that behave similar:
      // List.of (Java 9+) and List.copyOf (Java 10+)
      if (o == null) {
        throw new NullPointerException();
      }
      return super.contains(o);
    }
  }

  protected void createGroupGrantAuthorization(Resource resource, String resourceId, String groupId, Permission... permissions) {
    Authorization authorization = createGrantAuthorization(resource, resourceId);
    authorization.setGroupId(groupId);
    for (Permission permission : permissions) {
      authorization.addPermission(permission);
    }
    saveAuthorization(authorization);
  }

  protected void createGroupAndAddUser(String groupId, String userId) {
    createGroup(groupId);
    identityService.createMembership(userId, groupId);
  }

  protected <T extends Session> T spyOnSession(CommandContext commandContext, Class<T> sessionClass) {
    T manager = commandContext.getSession(sessionClass);
    T spy = spy(manager);
    commandContext.getSessions().put(sessionClass, spy);

    return spy;
  }

}
