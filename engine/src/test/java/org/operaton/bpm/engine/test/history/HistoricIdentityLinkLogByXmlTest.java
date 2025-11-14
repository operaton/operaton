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
package org.operaton.bpm.engine.test.history;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricIdentityLinkLog;
import org.operaton.bpm.engine.history.HistoricIdentityLinkLogQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
@ExtendWith(ProcessEngineExtension.class)
class HistoricIdentityLinkLogByXmlTest {

  private static final String PROCESS_DEFINITION_KEY_CANDIDATE_USER = "oneTaskProcessForHistoricIdentityLinkWithCanidateUser";
  private static final String PROCESS_DEFINITION_KEY_CANDIDATE_GROUP = "oneTaskProcessForHistoricIdentityLinkWithCanidateGroup";
  private static final String PROCESS_DEFINITION_KEY_ASSIGNEE = "oneTaskProcessForHistoricIdentityLinkWithAssignee";
  private static final String PROCESS_DEFINITION_KEY_CANDIDATE_STARTER_USER = "oneTaskProcessForHistoricIdentityLinkWithCanidateStarterUsers";
  private static final String PROCESS_DEFINITION_KEY_CANDIDATE_STARTER_GROUP = "oneTaskProcessForHistoricIdentityLinkWithCanidateStarterGroups";
  private static final String XML_USER = "demo";
  private static final String XML_GROUP = "demoGroups";
  private static final String XML_ASSIGNEE = "assignee";

  protected static final String TENANT_ONE = "tenant1";

  protected static final String CANDIDATE_STARTER_USER = "org/operaton/bpm/engine/test/api/repository/ProcessDefinitionCandidateTest.testCandidateStarterUser.bpmn20.xml";
  protected static final String CANDIDATE_STARTER_USERS = "org/operaton/bpm/engine/test/api/repository/ProcessDefinitionCandidateTest.testCandidateStarterUsers.bpmn20.xml";

  protected static final String CANDIDATE_STARTER_GROUP = "org/operaton/bpm/engine/test/api/repository/ProcessDefinitionCandidateTest.testCandidateStarterGroup.bpmn20.xml";
  protected static final String CANDIDATE_STARTER_GROUPS = "org/operaton/bpm/engine/test/api/repository/ProcessDefinitionCandidateTest.testCandidateStarterGroups.bpmn20.xml";

  HistoryService historyService;
  RepositoryService repositoryService;
  RuntimeService runtimeService;

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/OneTaskProcessWithCandidateUser.bpmn20.xml"})
  @Test
  void testShouldAddTaskCandidateforAddIdentityLinkUsingXml() {

    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY_CANDIDATE_USER);
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).hasSize(1);

    // query Test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.userId(XML_USER).count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/OneTaskProcessWithTaskAssignee.bpmn20.xml"})
  @Test
  void testShouldAddTaskAssigneeforAddIdentityLinkUsingXml() {

    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY_ASSIGNEE);
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).hasSize(1);

    // query Test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.userId(XML_ASSIGNEE).count()).isOne();


  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/OneTaskProcessWithCandidateGroups.bpmn20.xml"})
  @Test
  void testShouldAddTaskCandidateGroupforAddIdentityLinkUsingXml() {

    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY_CANDIDATE_GROUP);
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).hasSize(1);

    // query Test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.groupId(XML_GROUP).count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/OneTaskProcessWithCandidateStarterUsers.bpmn20.xml"})
  @Test
  void testShouldAddProcessCandidateStarterUserforAddIdentityLinkUsingXml() {

    // Pre test - Historical identity link is added as part of deployment
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).hasSize(1);

    // given
    ProcessDefinition latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY_CANDIDATE_STARTER_USER)
        .singleResult();
    assertThat(latestProcessDef).isNotNull();

    List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
    assertThat(links).hasSize(1);

    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).hasSize(1);

    // query Test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.userId(XML_USER).count()).isOne();
  }
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/OneTaskProcessWithCandidateStarterGroups.bpmn20.xml" })
  public void testShouldAddProcessCandidateStarterGroupforAddIdentityLinkUsingXml() {

    // Pre test - Historical identity link is added as part of deployment
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).hasSize(1);

    // given
    ProcessDefinition latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY_CANDIDATE_STARTER_GROUP)
        .singleResult();
    assertThat(latestProcessDef).isNotNull();

    List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
    assertThat(links).hasSize(1);

    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).hasSize(1);

    // query Test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.groupId(XML_GROUP).count()).isOne();
  }

  @Test
  void testPropagateTenantIdToCandidateStarterUser() {
    // when
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService.createDeployment()
      .addClasspathResource(CANDIDATE_STARTER_USER)
      .tenantId(TENANT_ONE)
      .deploy();

    // then
    List<HistoricIdentityLinkLog> historicLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicLinks).hasSize(1);

    HistoricIdentityLinkLog historicLink = historicLinks.get(0);
    assertThat(historicLink.getTenantId()).isNotNull();
    assertThat(historicLink.getTenantId()).isEqualTo(TENANT_ONE);

    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Test
  void testPropagateTenantIdToCandidateStarterUsers() {
    // when
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService.createDeployment()
        .addClasspathResource(CANDIDATE_STARTER_USERS)
        .tenantId(TENANT_ONE)
        .deploy();

      // then
      List<HistoricIdentityLinkLog> historicLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicLinks).hasSize(3);

    for (HistoricIdentityLinkLog historicLink : historicLinks) {
      assertThat(historicLink.getTenantId()).isNotNull();
      assertThat(historicLink.getTenantId()).isEqualTo(TENANT_ONE);
    }

    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Test
  void testPropagateTenantIdToCandidateStarterGroup() {
    // when
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService.createDeployment()
        .addClasspathResource(CANDIDATE_STARTER_GROUP)
        .tenantId(TENANT_ONE)
        .deploy();

      // then
      List<HistoricIdentityLinkLog> historicLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicLinks).hasSize(1);

      HistoricIdentityLinkLog historicLink = historicLinks.get(0);
    assertThat(historicLink.getTenantId()).isNotNull();
    assertThat(historicLink.getTenantId()).isEqualTo(TENANT_ONE);

      repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Test
  void testPropagateTenantIdToCandidateStarterGroups() {
    // when
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService.createDeployment()
        .addClasspathResource(CANDIDATE_STARTER_GROUPS)
        .tenantId(TENANT_ONE)
        .deploy();

      // then
      List<HistoricIdentityLinkLog> historicLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicLinks).hasSize(3);

    for (HistoricIdentityLinkLog historicLink : historicLinks) {
      assertThat(historicLink.getTenantId()).isNotNull();
      assertThat(historicLink.getTenantId()).isEqualTo(TENANT_ONE);
    }

    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  protected ProcessInstance startProcessInstance(String key) {
    return runtimeService.startProcessInstanceByKey(key);
  }
}
