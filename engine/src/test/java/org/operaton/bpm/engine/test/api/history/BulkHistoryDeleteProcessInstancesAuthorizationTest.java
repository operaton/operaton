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
package org.operaton.bpm.engine.test.api.history;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;

/**
 * @author Svetlana Dorokhova
 */
@Parameterized
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class BulkHistoryDeleteProcessInstancesAuthorizationTest {

  protected static final String ONE_TASK_PROCESS = "oneTaskProcess";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);
  @RegisterExtension
  static ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  private HistoryService historyService;
  private RuntimeService runtimeService;

  @BeforeEach
  void init() {
    authRule.createUserAndGroup("demo", "groupId");
  }

  @Parameter
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
        scenario()
            .failsDueToRequired(
                grant(Resources.PROCESS_DEFINITION, "processDefinition", "demo", Permissions.DELETE_HISTORY)
            )
                ,
        scenario()
            .withAuthorizations(
                grant(Resources.PROCESS_DEFINITION, "processDefinition", "demo", Permissions.DELETE_HISTORY)
            )
            .succeeds(),
        scenario()
            .withAuthorizations(
                grant(Resources.PROCESS_DEFINITION, "*", "demo", Permissions.DELETE_HISTORY)
            )
            .succeeds()
    );
  }

  @AfterEach
  void tearDown() {
    authRule.deleteUsersAndGroups();
  }

  @TestTemplate
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testCleanupHistory() {
    //given
    final List<String> ids = prepareHistoricProcesses();
    runtimeService.deleteProcessInstances(ids, null, true, true);

    // when
    authRule
        .init(scenario)
        .bindResource("processDefinition", "oneTaskProcess")
        .withUser("demo")
        .start();

    //when
    historyService.deleteHistoricProcessInstancesBulk(ids);

    if (authRule.assertScenario(scenario)) {
      assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
    }

  }

  private List<String> prepareHistoricProcesses() {
    return prepareHistoricProcesses(ONE_TASK_PROCESS);
  }

  private List<String> prepareHistoricProcesses(String businessKey) {
    return prepareHistoricProcesses(businessKey, null);
  }

  private List<String> prepareHistoricProcesses(String businessKey, VariableMap variables) {
    List<String> processInstanceIds = new ArrayList<>();

    for (int i = 0; i < 5; i++) {
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(businessKey, variables);
      processInstanceIds.add(processInstance.getId());
    }

    return processInstanceIds;
  }

}
