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
package org.operaton.bpm.engine.test.api.optimize;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.history.HistoricVariableUpdate;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.OptimizeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.test.RequiredDatabase;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.operaton.bpm.engine.variable.value.builder.ObjectValueBuilder;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class GetHistoricVariableUpdatesForOptimizeTest {

  private static final String USER_ID = "test";

  private static final TypedValue STRING_VARIABLE_DEFAULT_VALUE = Variables.stringValue("aString");
  private static final ObjectValueBuilder OBJECT_VARIABLE_DEFAULT_VALUE = Variables
    .objectValue(List.of("one", "two", "three"));
  private static final TypedValue BYTE_VARIABLE_DEFAULT_VALUE = Variables
    .byteArrayValue(new byte[]{8, 6, 3, 4, 2, 6, 7, 8});
  private static final FileValue FILE_VARIABLE_DEFAULT_VALUE = Variables.fileValue("test.txt")
    .file("some bytes".getBytes())
    .mimeType("text/plain")
    .create();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  OptimizeService optimizeService;
  IdentityService identityService;
  RuntimeService runtimeService;
  AuthorizationService authorizationService;
  TaskService taskService;

  @BeforeEach
  void init() {
    ProcessEngineConfigurationImpl config =
      engineRule.getProcessEngineConfiguration();
    optimizeService = config.getOptimizeService();

    createUser(USER_ID);
  }

  @AfterEach
  void cleanUp() {
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Group group : identityService.createGroupQuery().list()) {
      identityService.deleteGroup(group.getId());
    }
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
    ClockUtil.reset();
  }

  @Test
  void getHistoricVariableUpdates() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "foo");
    runtimeService.startProcessInstanceByKey("process", variables);

    // when
    List<HistoricVariableUpdate> historicVariableUpdates =
      optimizeService.getHistoricVariableUpdates(new Date(1L), null, false, 10);

    // then
    assertThat(historicVariableUpdates).hasSize(1);
    assertThatUpdateHasAllImportantInformation(historicVariableUpdates.get(0));
  }

  @Test
  void occurredAfterParameterWorks() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "value1");
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    runtimeService.startProcessInstanceByKey("process", variables);
    Date nowPlus2Seconds = new Date(new Date().getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    variables.put("stringVar", "value2");
    runtimeService.startProcessInstanceByKey("process", variables);

    // when
    List<HistoricVariableUpdate> variableUpdates =
      optimizeService.getHistoricVariableUpdates(now, null, false, 10);

    // then
    assertThat(variableUpdates).hasSize(1);
    assertThat(variableUpdates.get(0).getValue()).hasToString("value2");
  }

  @Test
  void occurredAtParameterWorks() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "value1");
    runtimeService.startProcessInstanceByKey("process", variables);
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    variables.put("stringVar", "value2");
    runtimeService.startProcessInstanceByKey("process", variables);

    // when
    List<HistoricVariableUpdate> variableUpdates =
      optimizeService.getHistoricVariableUpdates(null, now, false, 10);

    // then
    assertThat(variableUpdates).hasSize(1);
    assertThat(variableUpdates.get(0).getValue()).hasToString("value1");
  }

  @Test
  void occurredAfterAndOccurredAtParameterWorks() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "value1");
    runtimeService.startProcessInstanceByKey("process", variables);
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    variables.put("stringVar", "value2");
    runtimeService.startProcessInstanceByKey("process", variables);

    // when
    List<HistoricVariableUpdate> variableUpdates =
      optimizeService.getHistoricVariableUpdates(now, now, false, 10);

    // then
    assertThat(variableUpdates).isEmpty();
  }

  @Test
  void mixedTypeVariablesByDefaultAllNonBinaryValuesAreFetched() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);

    final Map<String, Object> mixedTypeVariableMap = createMixedTypeVariableMap();
    runtimeService.startProcessInstanceByKey("process", mixedTypeVariableMap);

    // when
    List<HistoricVariableUpdate> historicVariableUpdates =
      optimizeService.getHistoricVariableUpdates(new Date(1L), null, false, 10);

    // then
    assertThat(historicVariableUpdates)
      .extracting(HistoricVariableUpdate::getVariableName)
      .containsExactlyInAnyOrderElementsOf(mixedTypeVariableMap.keySet());

    assertThat(historicVariableUpdates)
      .extracting(this::extractVariableValue)
      .containsExactlyInAnyOrder(
        STRING_VARIABLE_DEFAULT_VALUE.getValue(),
        OBJECT_VARIABLE_DEFAULT_VALUE.create().getValueSerialized(),
        null,
        null
      );

  }

  @Test
  void mixedTypeVariablesExcludeObjectValueDiscardsObjectValue() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);

    final Map<String, Object> mixedTypeVariableMap = createMixedTypeVariableMap();
    runtimeService.startProcessInstanceByKey("process", mixedTypeVariableMap);

    // when
    List<HistoricVariableUpdate> historicVariableUpdates =
      optimizeService.getHistoricVariableUpdates(new Date(1L), null, true, 10);

    // then
    assertThat(historicVariableUpdates)
      .extracting(HistoricVariableUpdate::getVariableName)
      .containsExactlyInAnyOrderElementsOf(mixedTypeVariableMap.keySet());

    assertThat(historicVariableUpdates)
      .extracting(this::extractVariableValue)
      .containsExactlyInAnyOrder(STRING_VARIABLE_DEFAULT_VALUE.getValue(), null, null, null);

  }

  @Test
  void maxResultsParameterWorks() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "value1");
    variables.put("integerVar", 1);
    runtimeService.startProcessInstanceByKey("process", variables);
    runtimeService.startProcessInstanceByKey("process", variables);
    runtimeService.startProcessInstanceByKey("process", variables);
    runtimeService.startProcessInstanceByKey("process", variables);
    runtimeService.startProcessInstanceByKey("process", variables);

    // when
    List<HistoricVariableUpdate> variableUpdates =
      optimizeService.getHistoricVariableUpdates(pastDate(), null, false, 3);

    // then
    assertThat(variableUpdates).hasSize(3);
  }

  @Test
  void resultIsSortedByTime() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    Date nowPlus1Second = new Date(now.getTime() + 1000L);
    ClockUtil.setCurrentTime(nowPlus1Second);
    Map<String, Object> variables = new HashMap<>();
    variables.put("var1", "value1");
    runtimeService.startProcessInstanceByKey("process", variables);
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    variables.clear();
    variables.put("var2", "value2");
    runtimeService.startProcessInstanceByKey("process", variables);
    Date nowPlus4Seconds = new Date(nowPlus2Seconds.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    variables.clear();
    variables.put("var3", "value3");
    runtimeService.startProcessInstanceByKey("process", variables);

    // when
    List<HistoricVariableUpdate> variableUpdates =
      optimizeService.getHistoricVariableUpdates(now, null, false, 10);

    // then
    assertThat(variableUpdates).hasSize(3);
    assertThat(variableUpdates.get(0).getVariableName()).isEqualTo("var1");
    assertThat(variableUpdates.get(1).getVariableName()).isEqualTo("var2");
    assertThat(variableUpdates.get(2).getVariableName()).isEqualTo("var3");
  }

  @Test
  void fetchOnlyVariableUpdates() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");

    Task task = taskService.createTaskQuery().singleResult();
    Map<String, Object> formFields = new HashMap<>();
    formFields.put("var", "foo");
    engineRule.getFormService().submitTaskForm(task.getId(), formFields);
    long detailCount = engineRule.getHistoryService().createHistoricDetailQuery().count();
    assertThat(detailCount).isEqualTo(2L); // variable update + form property

    // when
    List<HistoricVariableUpdate> variableUpdates =
      optimizeService.getHistoricVariableUpdates(pastDate(), null, false, 10);

    // then
    assertThat(variableUpdates).hasSize(1);
  }

  /**
   * Excluded on h2, because the test takes quite some time there (30-40 seconds)
   * and the fixed problem did not occur on h2.
   * <p>
   */
  @Test
  @RequiredDatabase(excludes = DbSqlSessionFactory.H2)
  void testFetchLargeNumberOfObjectVariables() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask("waitState")
      .endEvent()
      .done();

    testHelper.deploy(simpleDefinition);

    int numberOfVariables = 3000;
    VariableMap variables = createVariables(numberOfVariables);

    // creates all variables in one transaction, which can take advantage
    // of SQL batching
    runtimeService.startProcessInstanceByKey("process", variables);

    // when
    List<HistoricVariableUpdate> historicVariableUpdates =
      optimizeService.getHistoricVariableUpdates(new Date(1L), null, false, 10000);

    // then
    assertThat(historicVariableUpdates).hasSize(numberOfVariables);

    for (HistoricVariableUpdate update : historicVariableUpdates) {
      ObjectValue typedValue = (ObjectValue) update.getTypedValue();
      assertThat(typedValue.getValueSerialized()).isNotNull();
    }
  }

  private Object extractVariableValue(HistoricVariableUpdate variableUpdate) {
    final TypedValue typedValue = variableUpdate.getTypedValue();
    if (typedValue instanceof ObjectValue objectValue) {
      return objectValue.isDeserialized() ? objectValue.getValue() : objectValue.getValueSerialized();
    } else {
      return typedValue.getValue();
    }
  }

  private Map<String, Object> createMixedTypeVariableMap() {
    Map<String, Object> variables = new HashMap<>();
    // non binary values
    variables.put("stringVar", STRING_VARIABLE_DEFAULT_VALUE);
    variables.put("objVar", OBJECT_VARIABLE_DEFAULT_VALUE);
    // binary values
    variables.put("byteVar", BYTE_VARIABLE_DEFAULT_VALUE);
    variables.put("fileVar", FILE_VARIABLE_DEFAULT_VALUE);
    return variables;
  }

  private VariableMap createVariables(int num) {
    VariableMap variables = Variables.createVariables();

    for (int i = 0; i < num; i++) {
      variables.put("var" + i, Variables.objectValue(i));
    }

    return variables;
  }

  private Date pastDate() {
    return new Date(2L);
  }

  protected void createUser(String userId) {
    User user = identityService.newUser(userId);
    identityService.saveUser(user);
  }

  private void assertThatUpdateHasAllImportantInformation(HistoricVariableUpdate variableUpdate) {
    assertThat(variableUpdate).isNotNull();
    assertThat(variableUpdate.getId()).isNotNull();
    assertThat(variableUpdate.getProcessDefinitionKey()).isEqualTo("process");
    assertThat(variableUpdate.getProcessDefinitionId()).isNotNull();
    assertThat(variableUpdate.getVariableName()).isEqualTo("stringVar");
    assertThat(variableUpdate.getValue()).hasToString("foo");
    assertThat(variableUpdate.getTypeName()).isEqualTo("string");
    assertThat(variableUpdate.getTime()).isNotNull();
  }

}
