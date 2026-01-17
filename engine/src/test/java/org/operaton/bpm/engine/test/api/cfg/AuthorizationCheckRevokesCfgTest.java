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
package org.operaton.bpm.engine.test.api.cfg;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.AuthorizationCheck;
import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.db.entitymanager.DbEntityManager;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.AuthorizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Daniel Meyer
 *
 */
class AuthorizationCheckRevokesCfgTest {

  private static final List<String> AUTHENTICATED_GROUPS = List.of("aGroup");
  private static final String AUTHENTICATED_USER_ID = "userId";

  CommandContext mockedCmdContext;
  ProcessEngineConfigurationImpl mockedConfiguration;
  AuthorizationManager authorizationManager;
  DbEntityManager mockedEntityManager;

  @BeforeEach
  void setup() {

    mockedCmdContext = mock(CommandContext.class);
    mockedConfiguration = mock(ProcessEngineConfigurationImpl.class);
    authorizationManager = spy(new AuthorizationManager());
    mockedEntityManager = mock(DbEntityManager.class);

    when(mockedCmdContext.getSession(DbEntityManager.class)).thenReturn(mockedEntityManager);

    when(authorizationManager.filterAuthenticatedGroupIds(eq(AUTHENTICATED_GROUPS))).thenReturn(AUTHENTICATED_GROUPS);
    when(mockedCmdContext.getAuthentication()).thenReturn(new Authentication(AUTHENTICATED_USER_ID, AUTHENTICATED_GROUPS));
    when(mockedCmdContext.isAuthorizationCheckEnabled()).thenReturn(true);
    when(mockedConfiguration.isAuthorizationEnabled()).thenReturn(true);

    Context.setCommandContext(mockedCmdContext);
    Context.setProcessEngineConfiguration(mockedConfiguration);
  }

  @AfterEach
  void cleanup() {
    Context.removeCommandContext();
    Context.removeProcessEngineConfiguration();
  }

  @Test
  void shouldUseCfgValue_always() {
    final ListQueryParameterObject query = new ListQueryParameterObject();
    final AuthorizationCheck authCheck = query.getAuthCheck();

    // given
    when(mockedConfiguration.getAuthorizationCheckRevokes()).thenReturn(ProcessEngineConfiguration.AUTHORIZATION_CHECK_REVOKE_ALWAYS);

    // if
    authorizationManager.configureQuery(query);

    // then
    assertThat(authCheck.isRevokeAuthorizationCheckEnabled()).isTrue();
    verifyNoMoreInteractions(mockedEntityManager);
  }

  @Test
  void shouldUseCfgValue_never() {
    final ListQueryParameterObject query = new ListQueryParameterObject();
    final AuthorizationCheck authCheck = query.getAuthCheck();

    // given
    when(mockedConfiguration.getAuthorizationCheckRevokes()).thenReturn(ProcessEngineConfiguration.AUTHORIZATION_CHECK_REVOKE_NEVER);

    // if
    authorizationManager.configureQuery(query);

    // then
    assertThat(authCheck.isRevokeAuthorizationCheckEnabled()).isFalse();
    verify(mockedEntityManager, never()).selectBoolean(eq("selectRevokeAuthorization"), any());
    verifyNoMoreInteractions(mockedEntityManager);
  }

  @Test
  void shouldCheckDbForCfgValue_auto() {
    final ListQueryParameterObject query = new ListQueryParameterObject();
    final AuthorizationCheck authCheck = query.getAuthCheck();

    final HashMap<String, Object> expectedQueryParams = new HashMap<>();
    expectedQueryParams.put("userId", AUTHENTICATED_USER_ID);
    expectedQueryParams.put("authGroupIds", AUTHENTICATED_GROUPS);

    // given
    when(mockedConfiguration.getAuthorizationCheckRevokes()).thenReturn(ProcessEngineConfiguration.AUTHORIZATION_CHECK_REVOKE_AUTO);
    when(mockedEntityManager.selectBoolean("selectRevokeAuthorization", expectedQueryParams)).thenReturn(true);

    // if
    authorizationManager.configureQuery(query);

    // then
    assertThat(authCheck.isRevokeAuthorizationCheckEnabled()).isTrue();
    verify(mockedEntityManager, times(1)).selectBoolean("selectRevokeAuthorization", expectedQueryParams);
  }

  @Test
  void shouldCheckDbForCfgValueWithNoRevokes_auto() {
    final ListQueryParameterObject query = new ListQueryParameterObject();
    final AuthorizationCheck authCheck = query.getAuthCheck();

    final HashMap<String, Object> expectedQueryParams = new HashMap<>();
    expectedQueryParams.put("userId", AUTHENTICATED_USER_ID);
    expectedQueryParams.put("authGroupIds", AUTHENTICATED_GROUPS);

    // given
    when(mockedConfiguration.getAuthorizationCheckRevokes()).thenReturn(ProcessEngineConfiguration.AUTHORIZATION_CHECK_REVOKE_AUTO);
    when(mockedEntityManager.selectBoolean("selectRevokeAuthorization", expectedQueryParams)).thenReturn(false);

    // if
    authorizationManager.configureQuery(query);

    // then
    assertThat(authCheck.isRevokeAuthorizationCheckEnabled()).isFalse();
    verify(mockedEntityManager, times(1)).selectBoolean("selectRevokeAuthorization", expectedQueryParams);
  }

  @Test
  void shouldCheckDbForCfgCaseInsensitive() {
    final ListQueryParameterObject query = new ListQueryParameterObject();
    final AuthorizationCheck authCheck = query.getAuthCheck();

    final HashMap<String, Object> expectedQueryParams = new HashMap<>();
    expectedQueryParams.put("userId", AUTHENTICATED_USER_ID);
    expectedQueryParams.put("authGroupIds", AUTHENTICATED_GROUPS);

    // given
    when(mockedConfiguration.getAuthorizationCheckRevokes()).thenReturn("AuTo");
    when(mockedEntityManager.selectBoolean("selectRevokeAuthorization", expectedQueryParams)).thenReturn(true);

    // if
    authorizationManager.configureQuery(query);

    // then
    assertThat(authCheck.isRevokeAuthorizationCheckEnabled()).isTrue();
    verify(mockedEntityManager, times(1)).selectBoolean("selectRevokeAuthorization", expectedQueryParams);
  }

  @Test
  void shouldCacheCheck() {
    final ListQueryParameterObject query = new ListQueryParameterObject();
    final AuthorizationCheck authCheck = query.getAuthCheck();

    final HashMap<String, Object> expectedQueryParams = new HashMap<>();
    expectedQueryParams.put("userId", AUTHENTICATED_USER_ID);
    expectedQueryParams.put("authGroupIds", AUTHENTICATED_GROUPS);

    // given
    when(mockedConfiguration.getAuthorizationCheckRevokes()).thenReturn(ProcessEngineConfiguration.AUTHORIZATION_CHECK_REVOKE_AUTO);
    when(mockedEntityManager.selectBoolean("selectRevokeAuthorization", expectedQueryParams)).thenReturn(true);

    // if
    authorizationManager.configureQuery(query);
    authorizationManager.configureQuery(query);

    // then
    assertThat(authCheck.isRevokeAuthorizationCheckEnabled()).isTrue();
    verify(mockedEntityManager, times(1)).selectBoolean("selectRevokeAuthorization", expectedQueryParams);
  }

  @Test
  void testAutoIsDefault() {
    assertThat(new StandaloneProcessEngineConfiguration().getAuthorizationCheckRevokes()).isEqualTo(ProcessEngineConfiguration.AUTHORIZATION_CHECK_REVOKE_AUTO);
  }

}
