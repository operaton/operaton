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
package org.operaton.bpm.engine.test.standalone.history;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricIdentityLinkLog;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.operaton.bpm.engine.ProcessEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP;
import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
public class CustomHistoryLevelIdentityLinkTest {

  @Parameters
  public static Collection<Object[]> data() {
    return List.of(new Object[][] {
      new Object[]{ List.of(HistoryEventTypes.IDENTITY_LINK_ADD) },
      new Object[]{ List.of(HistoryEventTypes.IDENTITY_LINK_DELETE, HistoryEventTypes.IDENTITY_LINK_ADD) }
    });
  }

  @Parameter
  public List<HistoryEventTypes> eventTypes;

  static CustomHistoryLevelIdentityLink customHisstoryLevelIL = new CustomHistoryLevelIdentityLink();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(configuration -> {
      configuration.setJdbcUrl("jdbc:h2:mem:" + CustomHistoryLevelIdentityLinkTest.class.getSimpleName());
      List<HistoryLevel> levels = new ArrayList<>();
      levels.add(customHisstoryLevelIL);
      configuration.setCustomHistoryLevels(levels);
      configuration.setHistory("aCustomHistoryLevelIL");
      configuration.setDatabaseSchemaUpdate(DB_SCHEMA_UPDATE_CREATE_DROP);
      configuration.setProcessEngineName("randomProcessEngineName");
    })
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  HistoryService historyService;
  RuntimeService runtimeService;
  IdentityService identityService;
  RepositoryService repositoryService;
  TaskService taskService;

  @BeforeEach
  void setUp() {
    customHisstoryLevelIL.setEventTypes(eventTypes);
  }

  @AfterEach
  void tearDown() {
    customHisstoryLevelIL.setEventTypes(null);
  }

  @TestTemplate
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testDeletingIdentityLinkByProcDefId() {
    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();

    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();

    identityService.setAuthenticatedUserId("anAuthUser");
    taskService.addCandidateUser(taskId, "aUser");
    taskService.deleteCandidateUser(taskId, "aUser");

    // assume
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isNotEmpty();

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey("oneTaskProcess")
      .cascade()
      .delete();

    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();
  }

  @TestTemplate
  void testDeletingIdentityLinkByTaskId() {
    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();

    // given
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    identityService.setAuthenticatedUserId("anAuthUser");
    taskService.addCandidateUser(taskId, "aUser");
    taskService.deleteCandidateUser(taskId, "aUser");

    // assume
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isNotEmpty();

    // when
    taskService.deleteTask(taskId, true);

    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();
  }

}
