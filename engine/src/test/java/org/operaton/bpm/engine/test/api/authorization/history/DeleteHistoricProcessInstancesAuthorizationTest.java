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
package org.operaton.bpm.engine.test.api.authorization.history;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Askar Akhmerov
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
@Parameterized
public class DeleteHistoricProcessInstancesAuthorizationTest {

  protected static final String PROCESS_KEY = "oneTaskProcess";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  protected ProcessInstance processInstance;
  protected ProcessInstance processInstance2;

  protected HistoricProcessInstance historicProcessInstance;
  protected HistoricProcessInstance historicProcessInstance2;

  protected RuntimeService runtimeService;
  protected HistoryService historyService;
  protected ManagementService managementService;

  @Parameter
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
        scenario()
            .withAuthorizations(
                grant(Resources.PROCESS_DEFINITION, "Process", "userId", Permissions.READ_HISTORY)
            )
            .failsDueToRequired(
                grant(Resources.PROCESS_DEFINITION, "Process", "userId", Permissions.DELETE_HISTORY)
            ),
        scenario()
            .withAuthorizations(
                grant(Resources.PROCESS_DEFINITION, "Process", "userId", Permissions.READ_HISTORY, Permissions.DELETE_HISTORY)
            ).succeeds()
    );
  }

  @BeforeEach
  void setUp() {
    authRule.createUserAndGroup("userId", "groupId");
    runtimeService = engineRule.getRuntimeService();
    managementService = engineRule.getManagementService();
    historyService = engineRule.getHistoryService();

    deployAndCompleteProcesses();
  }

  public void deployAndCompleteProcesses() {
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());
    processInstance2 = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());

    List<String> processInstanceIds = List.of(processInstance.getId(), processInstance2.getId());
    runtimeService.deleteProcessInstances(processInstanceIds, null, false, false);

    historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(processInstance.getId()).singleResult();

    historicProcessInstance2 = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(processInstance2.getId()).singleResult();
  }

  @AfterEach
  void tearDown() {
    authRule.deleteUsersAndGroups();
  }

  @TestTemplate
  void testProcessInstancesList() {
    //given
    List<String> processInstanceIds = List.of(historicProcessInstance.getId(), historicProcessInstance2.getId());
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("processInstance1", processInstance.getId())
        .bindResource("processInstance2", processInstance2.getId())
        .start();

    // when
    historyService.deleteHistoricProcessInstances(processInstanceIds);

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
    }
  }
}
