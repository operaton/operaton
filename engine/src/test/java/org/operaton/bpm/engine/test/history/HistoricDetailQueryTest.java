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
package org.operaton.bpm.engine.test.history;

import static org.assertj.core.api.Assertions.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricDetailQuery;
import org.operaton.bpm.engine.history.HistoricVariableUpdate;
import org.operaton.bpm.engine.impl.history.event.HistoricVariableUpdateEventEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.models.AsyncProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.bpmn.async.AsyncListener;
import org.operaton.bpm.engine.test.cmmn.decisiontask.TestPojo;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 *
 * @author Svetlana Dorokhova
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class HistoricDetailQueryTest {

  protected static final String PROCESS_KEY = "oneTaskProcess";

  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  public ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain chain = RuleChain.outerRule(engineRule).around(testHelper);


  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected HistoryService historyService;
  protected TaskService taskService;
  protected IdentityService identityService;
  protected CaseService caseService;

  @Before
  public void initServices() {
    runtimeService = engineRule.getRuntimeService();
    managementService = engineRule.getManagementService();
    historyService = engineRule.getHistoryService();
    taskService = engineRule.getTaskService();
    identityService = engineRule.getIdentityService();
    caseService = engineRule.getCaseService();
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryByUserOperationId() {
    startProcessInstance(PROCESS_KEY);

    identityService.setAuthenticatedUserId("demo");

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.resolveTask(taskId, getVariables());

    //then
    String userOperationId = historyService.createHistoricDetailQuery().singleResult().getUserOperationId();

    HistoricDetailQuery query = historyService.createHistoricDetailQuery()
            .userOperationId(userOperationId);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByUserOperationIdAndVariableUpdates() {
    startProcessInstance(PROCESS_KEY);

    identityService.setAuthenticatedUserId("demo");

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.resolveTask(taskId, getVariables());

    //then
    String userOperationId = historyService.createHistoricDetailQuery().singleResult().getUserOperationId();

    HistoricDetailQuery query = historyService.createHistoricDetailQuery()
        .userOperationId(userOperationId)
        .variableUpdates();

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryByInvalidUserOperationId() {
    startProcessInstance(PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.resolveTask(taskId, getVariables());

    //then
    HistoricDetailQuery query = historyService.createHistoricDetailQuery()
            .userOperationId("invalid");

    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isEqualTo(0);

    try {
      query.userOperationId(null);
      fail("It was possible to set a null value as userOperationId.");
    } catch (ProcessEngineException e) { }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryByExecutionId() {
    startProcessInstance(PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.resolveTask(taskId, getVariables());

    //then
    String executionId = historyService.createHistoricDetailQuery().singleResult().getExecutionId();

    HistoricDetailQuery query = historyService.createHistoricDetailQuery()
            .executionId(executionId);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryByInvalidExecutionId() {
    startProcessInstance(PROCESS_KEY);

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.resolveTask(taskId, getVariables());

    //then
    HistoricDetailQuery query = historyService.createHistoricDetailQuery()
            .executionId("invalid");

    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isEqualTo(0);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryByExecutionIdAndProcessInstanceId() {
    // given
    startProcessInstance(PROCESS_KEY);

    Task task = taskService.createTaskQuery().singleResult();

    String processInstanceId = task.getProcessInstanceId();
    String executionId = task.getExecutionId();
    String taskId = task.getId();

    taskService.resolveTask(taskId, getVariables());

    // when
    HistoricDetail detail = historyService.createHistoricDetailQuery()
        .processInstanceId(processInstanceId)
        .executionId(executionId).singleResult();

    //then
    assertThat(detail.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(detail.getExecutionId()).isEqualTo(executionId);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  public void testQueryByVariableTypeIn() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    variables1.put("boolVar", true);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables1);

    // when
    HistoricDetailQuery query =
      historyService.createHistoricDetailQuery()
        .variableTypeIn("string");

    // then
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
    HistoricDetail historicDetail = query.list().get(0);
    if (historicDetail instanceof HistoricVariableUpdate variableUpdate) {
      assertThat(variableUpdate.getVariableName()).isEqualTo("stringVar");
      assertThat(variableUpdate.getTypeName()).isEqualTo("string");
    } else {
      fail("Historic detail should be a variable update!");
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  public void testQueryByVariableTypeInWithCapitalLetter() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    variables1.put("boolVar", true);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables1);

    // when
    HistoricDetailQuery query =
      historyService.createHistoricDetailQuery()
        .variableTypeIn("Boolean");

    // then
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
    HistoricDetail historicDetail = query.list().get(0);
    if (historicDetail instanceof HistoricVariableUpdate variableUpdate) {
      assertThat(variableUpdate.getVariableName()).isEqualTo("boolVar");
      assertThat(variableUpdate.getTypeName()).isEqualTo("boolean");
    } else {
      fail("Historic detail should be a variable update!");
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  public void testQueryByVariableTypeInWithSeveralTypes() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    variables1.put("boolVar", true);
    variables1.put("intVar", 5);
    variables1.put("nullVar", null);
    variables1.put("pojoVar", new TestPojo("str", .0));
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables1);

    // when
    HistoricDetailQuery query =
      historyService.createHistoricDetailQuery()
        .variableTypeIn("boolean", "integer", "Serializable");

    // then
    assertThat(query.list()).hasSize(3);
    assertThat(query.count()).isEqualTo(3);
    Set<String> allowedVariableTypes = new HashSet<>();
    allowedVariableTypes.add("boolean");
    allowedVariableTypes.add("integer");
    allowedVariableTypes.add("object");
    for (HistoricDetail detail : query.list()) {
      if (detail instanceof HistoricVariableUpdate variableUpdate) {
        assertThat(allowedVariableTypes).contains(variableUpdate.getTypeName());
      } else {
        fail("Historic detail should be a variable update!");
      }
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  public void testQueryByInvalidVariableTypeIn() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    variables1.put("boolVar", true);
    variables1.put("intVar", 5);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables1);

    // when
    HistoricDetailQuery query =
      historyService.createHistoricDetailQuery()
        .variableTypeIn("invalid");

    // then
    assertThat(query.count()).isEqualTo(0);

    try {
      // when
      query.variableTypeIn(null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // then fails
    }

    try {
      // when
      query.variableTypeIn((String)null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // then fails
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableNameLike() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("FooBarBaz", "variableValue");
    variables.put("bazBarFoo", "anotherVariableValue");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().variableNameLike("FooBar%").list();

    // then
    assertThat(historicDetails).hasSize(1);
    assertThat(historicDetails.get(0)).isInstanceOf(HistoricVariableUpdate.class);
    HistoricVariableUpdate historicDetail = (HistoricVariableUpdate) historicDetails.get(0);
    assertThat(historicDetail.getVariableName()).isEqualTo("FooBarBaz");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableNameLikeTwoWildcards() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("FooBarBaz", "variableValue");
    variables.put("FooBaz", "anotherVariableValue");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().variableNameLike("%Bar%").list();

    // then
    assertThat(historicDetails).hasSize(1);
    assertThat(historicDetails.get(0)).isInstanceOf(HistoricVariableUpdate.class);
    HistoricVariableUpdate historicDetail = (HistoricVariableUpdate) historicDetails.get(0);
    assertThat(historicDetail.getVariableName()).isEqualTo("FooBarBaz");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableNameLikePrefixWildcard() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("FooBarBaz", "variableValue");
    variables.put("FooBar", "anotherVariableValue");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().variableNameLike("%Bar").list();

    // then
    assertThat(historicDetails).hasSize(1);
    assertThat(historicDetails.get(0)).isInstanceOf(HistoricVariableUpdate.class);
    HistoricVariableUpdate historicDetail = (HistoricVariableUpdate) historicDetails.get(0);
    assertThat(historicDetail.getVariableName()).isEqualTo("FooBar");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableNameLikeInfixWildcard() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("FooBarBaz", "variableValue");
    variables.put("FooBar", "anotherVariableValue");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().variableNameLike("Foo%Baz").list();

    // then
    assertThat(historicDetails).hasSize(1);
    assertThat(historicDetails.get(0)).isInstanceOf(HistoricVariableUpdate.class);
    HistoricVariableUpdate historicDetail = (HistoricVariableUpdate) historicDetails.get(0);
    assertThat(historicDetail.getVariableName()).isEqualTo("FooBarBaz");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableNameLikeIgnoreCase() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("FooBarBaz", "variableValue");
    variables.put("BarBaz", "anotherVariableValue");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().variableNameLike("foo%").list();

    // then
    assertThat(historicDetails).hasSize(1);
    assertThat(historicDetails.get(0)).isInstanceOf(HistoricVariableUpdate.class);
    HistoricVariableUpdate historicDetail = (HistoricVariableUpdate) historicDetails.get(0);
    assertThat(historicDetail.getVariableName()).isEqualTo("FooBarBaz");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableNameLikeEqualsNoWildcard() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("FooBarBaz", "variableValue");
    variables.put("BarBaz", "anotherVariableValue");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().variableNameLike("BarBaz").list();

    // then
    assertThat(historicDetails).hasSize(1);
    assertThat(historicDetails.get(0)).isInstanceOf(HistoricVariableUpdate.class);
    HistoricVariableUpdate historicDetail = (HistoricVariableUpdate) historicDetails.get(0);
    assertThat(historicDetail.getVariableName()).isEqualTo("BarBaz");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableNameLikeEqualsWildcards() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("BarFooBaz", "variableValue");
    variables.put("BarBaz", "anotherVariableValue");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().variableNameLike("%BarBaz%").list();

    // then
    assertThat(historicDetails).hasSize(1);
    assertThat(historicDetails.get(0)).isInstanceOf(HistoricVariableUpdate.class);
    HistoricVariableUpdate historicDetail = (HistoricVariableUpdate) historicDetails.get(0);
    assertThat(historicDetail.getVariableName()).isEqualTo("BarBaz");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableNameLikeTwoMatches() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("FooBarBaz", "variableValue");
    variables.put("BarBazFoo", "anotherVariableValue");
    variables.put("FooBaz", "anotherVariableValue");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().variableNameLike("%Bar%").list();

    // then
    assertThat(historicDetails).hasSize(2);
    assertThat(historicDetails.get(0)).isInstanceOf(HistoricVariableUpdate.class);
    assertThat(historicDetails.get(1)).isInstanceOf(HistoricVariableUpdate.class);
    assertThat(historicDetails).extracting("variableName").containsOnly("FooBarBaz", "BarBazFoo");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableNameLikeTwoProcessInstances() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("FooBarBaz", "variableValue");
    variables.put("BarBazFoo", "anotherVariableValue");
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().variableNameLike("FooBar%").list();

    // then
    assertThat(historicDetails).hasSize(2);
    assertThat(historicDetails.get(0)).isInstanceOf(HistoricVariableUpdate.class);
    assertThat(historicDetails.get(1)).isInstanceOf(HistoricVariableUpdate.class);
    assertThat(historicDetails)
      .extracting("variableName", "processInstanceId")
      .containsExactlyInAnyOrder(
          tuple("FooBarBaz", processInstance1.getId()),
          tuple("FooBarBaz", processInstance2.getId())
      );
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableNameLikeNull() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("FooBarBaz", "variableValue");
    variables.put("FooBaz", "anotherVariableValue");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    HistoricDetailQuery query = historyService.createHistoricDetailQuery();

    // then
    assertThatThrownBy(() -> {
      query.variableNameLike(null);
    })
        .isInstanceOf(NullValueException.class)
        .hasMessageContaining("Variable name like is null");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryBySingleProcessInstanceId() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    HistoricDetailQuery query =
      historyService.createHistoricDetailQuery()
        .variableUpdates()
        .processInstanceIdIn(processInstance.getProcessInstanceId());

    // then
    assertThat(query.count()).isEqualTo(1);
    assertThat(processInstance.getId()).isEqualTo(query.list().get(0).getProcessInstanceId());
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryBySeveralProcessInstanceIds() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    HistoricDetailQuery query =
      historyService.createHistoricDetailQuery()
        .variableUpdates()
        .processInstanceIdIn(processInstance.getProcessInstanceId(), processInstance2.getProcessInstanceId());

    // then
    Set<String> expectedProcessInstanceIds = new HashSet<>();
    expectedProcessInstanceIds.add(processInstance.getId());
    expectedProcessInstanceIds.add(processInstance2.getId());
    assertThat(query.count()).isEqualTo(2);
    assertThat(expectedProcessInstanceIds).containsExactly(query.list().stream().map(HistoricDetail::getProcessInstanceId).toArray(String[]::new));
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryByNonExistingProcessInstanceId() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    HistoricDetailQuery query =
      historyService.createHistoricDetailQuery()
        .processInstanceIdIn("foo");

    // then
    assertThat(query.count()).isEqualTo(0);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  public void testQueryByInvalidProcessInstanceIds() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables1);

    // when
    HistoricDetailQuery query =
      historyService.createHistoricDetailQuery();

    try {
      // when
      query.processInstanceIdIn(null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // then fails
    }

    try {
      // when
      query.processInstanceIdIn((String)null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // then fails
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryByOccurredBefore() {
    // given
    Calendar startTime = Calendar.getInstance();
    ClockUtil.setCurrentTime(startTime.getTime());

    Calendar hourAgo = Calendar.getInstance();
    hourAgo.add(Calendar.HOUR_OF_DAY, -1);
    Calendar hourFromNow = Calendar.getInstance();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    HistoricDetailQuery query =
      historyService.createHistoricDetailQuery();

    // then
    assertThat(query.occurredBefore(hourFromNow.getTime()).count()).isEqualTo(1);
    assertThat(query.occurredBefore(hourAgo.getTime()).count()).isEqualTo(0);

  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryByOccurredAfter() {
    // given
    Calendar startTime = Calendar.getInstance();
    ClockUtil.setCurrentTime(startTime.getTime());

    Calendar hourAgo = Calendar.getInstance();
    hourAgo.add(Calendar.HOUR_OF_DAY, -1);
    Calendar hourFromNow = Calendar.getInstance();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    HistoricDetailQuery query =
      historyService.createHistoricDetailQuery();

    // then
    assertThat(query.occurredAfter(hourFromNow.getTime()).count()).isEqualTo(0);
    assertThat(query.occurredAfter(hourAgo.getTime()).count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testQueryByOccurredAfterAndOccurredBefore() {
    // given
    Calendar startTime = Calendar.getInstance();
    ClockUtil.setCurrentTime(startTime.getTime());

    Calendar hourAgo = Calendar.getInstance();
    hourAgo.add(Calendar.HOUR_OF_DAY, -1);
    Calendar hourFromNow = Calendar.getInstance();
    hourFromNow.add(Calendar.HOUR_OF_DAY, 1);

    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    // when
    HistoricDetailQuery query =
      historyService.createHistoricDetailQuery();

    // then
    assertThat(query.occurredAfter(hourFromNow.getTime()).occurredBefore(hourFromNow.getTime()).count()).isEqualTo(0);
    assertThat(query.occurredAfter(hourAgo.getTime()).occurredBefore(hourFromNow.getTime()).count()).isEqualTo(1);
    assertThat(query.occurredAfter(hourFromNow.getTime()).occurredBefore(hourAgo.getTime()).count()).isEqualTo(0);
    assertThat(query.occurredAfter(hourAgo.getTime()).occurredBefore(hourAgo.getTime()).count()).isEqualTo(0);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  public void testQueryByInvalidOccurredBeforeDate() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables1);

    // when
    HistoricDetailQuery query =
      historyService.createHistoricDetailQuery();

    try {
      // when
      query.occurredBefore(null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // then fails
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  public void testQueryByInvalidOccurredAfterDate() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables1);

    // when
    HistoricDetailQuery query =
      historyService.createHistoricDetailQuery();

    try {
      // when
      query.occurredAfter(null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // then fails
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testQueryByCaseInstanceIdAndCaseExecutionId() {
    // given
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();
    caseService.setVariable(caseInstanceId, "myVariable", 1);

    // when
    HistoricDetail detail = historyService.createHistoricDetailQuery()
        .caseInstanceId(caseInstanceId)
        .caseExecutionId(caseInstanceId).singleResult();

    // then
    assertThat(detail.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(detail.getCaseExecutionId()).isEqualTo(caseInstanceId);
  }

  @Test
  public void testInitialFlagAsyncBeforeUserTask() {
    //given
    BpmnModelInstance model = AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS;

    testHelper.deployAndGetDefinition(model);

    String initalValue = "initial";
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ProcessModels.PROCESS_KEY,
        Variables.createVariables().putValue("foo", initalValue));

    String localValue = "bar";
    runtimeService.setVariableLocal(processInstance.getId(), "local", localValue);

    // when
    List<HistoricDetail> details = historyService.createHistoricDetailQuery()
        .processInstanceId(processInstance.getId())
        .list();

    // then
    assertThat(details).hasSize(2);
    for (HistoricDetail historicDetail : details) {
      HistoricVariableUpdateEventEntity detail = (HistoricVariableUpdateEventEntity) historicDetail;
      String variableValue = detail.getTextValue();
      if (variableValue.equals(initalValue)) {
        assertThat(detail.isInitial()).isTrue();
      } else if (variableValue.equals(localValue)) {
        assertThat(detail.isInitial()).isFalse();
      } else {
        fail("illegal variable value:" + variableValue);
      }
    }
  }

  @Test
  public void testInitialFlagAsyncBeforeStartEvent() {
    //given
    BpmnModelInstance model = AsyncProcessModels.ASYNC_BEFORE_START_EVENT_PROCESS;

    testHelper.deployAndGetDefinition(model);

    String initalValue = "initial";
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ProcessModels.PROCESS_KEY,
        Variables.createVariables().putValue("foo", initalValue));

    String secondValue = "second";
    runtimeService.setVariable(processInstance.getId(), "foo", secondValue);

    // when
    List<HistoricDetail> details = historyService.createHistoricDetailQuery()
        .processInstanceId(processInstance.getId())
        .list();

    // then
    assertThat(details).hasSize(2);
    for (HistoricDetail historicDetail : details) {
      HistoricVariableUpdateEventEntity detail = (HistoricVariableUpdateEventEntity) historicDetail;

      String variableValue = detail.getTextValue();

      if (variableValue.equals(initalValue)) {
        assertThat(detail.isInitial()).isTrue();
      } else if (variableValue.equals(secondValue)) {
        assertThat(detail.isInitial()).isFalse();
      } else {
        fail("illegal variable value:" + variableValue);
      }
    }
  }

  @Test
  public void testInitialFlagAsyncBeforeSubprocess() {
    //given
    BpmnModelInstance model = AsyncProcessModels.ASYNC_BEFORE_SUBPROCESS_START_EVENT_PROCESS;

    testHelper.deployAndGetDefinition(model);

    String initalValue = "initial";
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ProcessModels.PROCESS_KEY,
        Variables.createVariables().putValue("foo", initalValue));

    String secondValue = "second";
    runtimeService.setVariable(processInstance.getId(), "foo", secondValue);

    // when
    List<HistoricDetail> details = historyService.createHistoricDetailQuery()
        .processInstanceId(processInstance.getId())
        .list();

    // then
    assertThat(details).hasSize(2);
    for (HistoricDetail historicDetail : details) {
      HistoricVariableUpdateEventEntity detail = (HistoricVariableUpdateEventEntity) historicDetail;

      String variableValue = detail.getTextValue();

      if (variableValue.equals(initalValue)) {
        assertThat(detail.isInitial()).isTrue();
      } else if (variableValue.equals(secondValue)) {
        assertThat(detail.isInitial()).isFalse();
      } else {
        fail("illegal variable value:" + variableValue);
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/AsyncStartEventTest.testAsyncStartEventListeners.bpmn20.xml"})
  public void testInitialFlagAsyncBeforeStartEventGlobalExecutionListener() {
    // given
    String initalValue = "initial";
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncStartEvent",
        Variables.createVariables().putValue("foo", initalValue));

    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.executeJob(jobId);

    // when
    List<HistoricDetail> details = historyService.createHistoricDetailQuery()
        .processInstanceId(processInstance.getId())
        .list();

    // then
    assertThat(details).hasSize(2);
    for (HistoricDetail historicDetail : details) {
      HistoricVariableUpdateEventEntity detail = (HistoricVariableUpdateEventEntity) historicDetail;

      assertThat(detail.isInitial()).isTrue();

      String variableValue = detail.getTextValue();
      if (variableValue.equals(initalValue)) {
        assertThat(detail.getVariableName()).isEqualTo("foo");
      } else if (variableValue.equals("listener invoked")) {
        assertThat(detail.getVariableName()).isEqualTo("listener");
      } else {
        fail("illegal variable value:" + variableValue);
      }
    }
  }

  @Test
  public void testInitialFlagAsyncBeforeStartEventExecutionListener() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
        .startEvent()
        .operatonAsyncBefore()
        .operatonExecutionListenerClass("start", AsyncListener.class)
        .userTask()
        .endEvent()
        .done();

    testHelper.deployAndGetDefinition(model);
    String initalValue = "initial";
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process",
        Variables.createVariables().putValue("foo", initalValue));

    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.executeJob(jobId);


    // when
    List<HistoricDetail> details = historyService.createHistoricDetailQuery()
        .processInstanceId(processInstance.getId())
        .list();

    // then
    assertThat(details).hasSize(2);
    for (HistoricDetail historicDetail : details) {
      HistoricVariableUpdateEventEntity detail = (HistoricVariableUpdateEventEntity) historicDetail;
      String variableValue = detail.getTextValue();
      assertThat(detail.isInitial()).isTrue();
      if (variableValue.equals(initalValue)) {
        assertThat(detail.getVariableName()).isEqualTo("foo");
      } else if (variableValue.equals("listener invoked")) {
        assertThat(detail.getVariableName()).isEqualTo("listener");
      } else {
        fail("illegal variable value:" + variableValue);
      }
    }
  }

  @Test
  public void testInitialFlagAsyncBeforeStartEventEndExecutionListener() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
        .startEvent()
        .operatonAsyncBefore()
        .operatonExecutionListenerClass("end", AsyncListener.class)
        .userTask()
        .endEvent()
        .done();

    testHelper.deployAndGetDefinition(model);
    String initalValue = "initial";
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process",
        Variables.createVariables().putValue("foo", initalValue));

    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.executeJob(jobId);


    // when
    List<HistoricDetail> details = historyService.createHistoricDetailQuery()
        .processInstanceId(processInstance.getId())
        .list();

    // then
    assertThat(details).hasSize(2);
    for (HistoricDetail historicDetail : details) {
      HistoricVariableUpdateEventEntity detail = (HistoricVariableUpdateEventEntity) historicDetail;
      String variableValue = detail.getTextValue();
      assertThat(detail.isInitial()).isTrue();
      if (variableValue.equals(initalValue)) {
        assertThat(detail.getVariableName()).isEqualTo("foo");
      } else if (variableValue.equals("listener invoked")) {
        assertThat(detail.getVariableName()).isEqualTo("listener");
      } else {
        fail("illegal variable value:" + variableValue);
      }
    }
  }

  protected VariableMap getVariables() {
    return Variables.createVariables()
            .putValue("aVariableName", "aVariableValue");
  }

  protected void startProcessInstance(String key) {
    startProcessInstances(key, 1);
  }

  protected void startProcessInstances(String key, int numberOfInstances) {
    for (int i = 0; i < numberOfInstances; i++) {
      runtimeService.startProcessInstanceByKey(key);
    }

    testHelper.executeAvailableJobs();
  }
}
