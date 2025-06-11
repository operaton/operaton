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
package org.operaton.bpm.qa.upgrade.scenarios7130.histperms;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.HistoricProcessInstancePermissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HistoricInstancePermissionsAuthorizationTest {

  protected final String BUSINESS_KEY = "HistPermsWithoutProcDefKeyScenarioBusinessKey";

  protected final String USER_ID = getClass().getName() + "-User";

  @Rule
  public ProcessEngineRule engineRule = new ProcessEngineRule("operaton.cfg.xml");

  protected HistoryService historyService;
  protected AuthorizationService authorizationService;
  protected IdentityService identityService;
  protected ProcessEngineConfigurationImpl engineConfiguration;

  @Before
  public void assignServices() {
    historyService = engineRule.getHistoryService();
    authorizationService = engineRule.getAuthorizationService();
    identityService = engineRule.getIdentityService();

    engineConfiguration = engineRule.getProcessEngineConfiguration();
  }

  @Before
  public void authenticate() {
    engineRule.getIdentityService()
        .setAuthenticatedUserId(USER_ID);
  }

  @After
  public void tearDown() {
    engineConfiguration
        .setEnableHistoricInstancePermissions(false)
        .setAuthorizationEnabled(false);

    identityService.clearAuthentication();

    List<Authorization> auths = authorizationService.createAuthorizationQuery()
        .userIdIn(USER_ID)
        .list();

    for (Authorization authorization : auths) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
  }

  @Test
  public void shouldSkipAuthorizationChecksForOperationLogQuery() {
    // given
    engineConfiguration.setEnableHistoricInstancePermissions(true);

    Authorization auth = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    auth.setUserId(USER_ID);
    auth.setPermissions(new HistoricProcessInstancePermissions[] {
        HistoricProcessInstancePermissions.READ });
    auth.setResource(Resources.HISTORIC_PROCESS_INSTANCE);

    HistoricProcessInstance historicProcessInstance =
        historyService.createHistoricProcessInstanceQuery()
            .processInstanceBusinessKey(BUSINESS_KEY + "0")
            .singleResult();

    String processInstanceId = historicProcessInstance.getId();

    auth.setResourceId(processInstanceId);

    authorizationService.saveAuthorization(auth);

    engineConfiguration.setAuthorizationEnabled(true);

    // when
    String processDefinitionId = historicProcessInstance.getProcessDefinitionId();

    UserOperationLogQuery query = historyService.createUserOperationLogQuery()
        .processDefinitionId(processDefinitionId);

    // then
    assertThat(query.list())
        .extracting("processDefinitionId")
        .containsExactly(
            processDefinitionId,
            processDefinitionId,
            processDefinitionId,
            processDefinitionId,
            processDefinitionId
        );
  }

  @Test
  public void shouldSkipAuthorizationChecksForHistoricProcessInstanceQuery() {
    // given
    engineConfiguration.setEnableHistoricInstancePermissions(true);

    Authorization auth = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    auth.setUserId(USER_ID);
    auth.setPermissions(new HistoricProcessInstancePermissions[] {
        HistoricProcessInstancePermissions.READ });
    auth.setResource(Resources.HISTORIC_PROCESS_INSTANCE);

    HistoricProcessInstance historicProcessInstance =
        historyService.createHistoricProcessInstanceQuery()
            .processInstanceBusinessKey(BUSINESS_KEY + "0")
            .singleResult();

    String processInstanceId = historicProcessInstance.getId();

    auth.setResourceId(processInstanceId);

    authorizationService.saveAuthorization(auth);

    engineConfiguration.setAuthorizationEnabled(true);

    // when
    String processDefinitionId = historicProcessInstance.getProcessDefinitionId();

    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
        .processDefinitionId(processDefinitionId);

    // then
    assertThat(query.list())
        .extracting("businessKey")
        .containsExactly(
            BUSINESS_KEY + "0",
            BUSINESS_KEY + "1",
            BUSINESS_KEY + "2",
            BUSINESS_KEY + "3",
            BUSINESS_KEY + "4"
        );
  }


}
