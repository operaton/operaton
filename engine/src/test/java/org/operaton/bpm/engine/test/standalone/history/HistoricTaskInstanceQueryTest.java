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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstanceQuery;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.Variables;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
@ExtendWith(ProcessEngineExtension.class)
class HistoricTaskInstanceQueryTest {

  protected static final String VARIABLE_NAME = "variableName";
  protected static final String VARIABLE_NAME_LC = VARIABLE_NAME.toLowerCase();
  protected static final String VARIABLE_VALUE = "variableValue";
  protected static final String VARIABLE_VALUE_LC = VARIABLE_VALUE.toLowerCase();
  protected static final String VARIABLE_VALUE_LC_LIKE = "%" + VARIABLE_VALUE_LC.substring(2, 10) + "%";
  protected static final String VARIABLE_VALUE_NE = "nonExistent";
  protected static final Map<String, Object> VARIABLES = new HashMap<>();
  static {
    VARIABLES.put(VARIABLE_NAME, VARIABLE_VALUE);
  }
  
  RuntimeService runtimeService;
  HistoryService historyService;
  TaskService taskService;
  IdentityService identityService;

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueEqualsNumber() {
    // long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        singletonMap("var", 123L));

    // non-matching long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        singletonMap("var", 12345L));

    // short
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        singletonMap("var", (short) 123));

    // double
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        singletonMap("var", 123.0d));

    // integer
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        singletonMap("var", 123));

    // untyped null (should not match)
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        singletonMap("var", null));

    // typed null (should not match)
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        singletonMap("var", Variables.longValue(null)));

    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        singletonMap("var", "123"));

    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("var", Variables.numberValue(123L)).count()).isEqualTo(4);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("var", Variables.numberValue(123.0d)).count()).isEqualTo(4);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("var", Variables.numberValue((short) 123)).count()).isEqualTo(4);

    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("var", Variables.numberValue(null)).count()).isOne();

    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotEquals("var", 999L).count()).isEqualTo(8);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotEquals("var", (short) 999).count()).isEqualTo(8);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotEquals("var", 999).count()).isEqualTo(8);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotEquals("var", "999").count()).isEqualTo(8);
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotEquals("var", false).count()).isEqualTo(8);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueLike() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
            singletonMap("requester", "vahid alizadeh"));

    var historicTaskInstanceQuery = historyService.createHistoricTaskInstanceQuery();
    assertThat(historicTaskInstanceQuery.processVariableValueLike("requester", "vahid%").count()).isOne();
    assertThat(historicTaskInstanceQuery.processVariableValueLike("requester", "%alizadeh").count()).isOne();
    assertThat(historicTaskInstanceQuery.processVariableValueLike("requester", "%ali%").count()).isOne();

    assertThat(historicTaskInstanceQuery.processVariableValueLike("requester", "requester%").count()).isZero();
    assertThat(historicTaskInstanceQuery.processVariableValueLike("requester", "%ali").count()).isZero();

    assertThat(historicTaskInstanceQuery.processVariableValueLike("requester", "vahid").count()).isZero();
    assertThat(historicTaskInstanceQuery.processVariableValueLike("nonExistingVar", "string%").count()).isZero();

    // when/then test with null value
    assertThatThrownBy(() -> historicTaskInstanceQuery.processVariableValueLike("requester", null))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Booleans and null cannot be used in 'like' condition");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueNotLike() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
            singletonMap("requester", "vahid alizadeh"));

    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotLike("requester", "vahid%").count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotLike("requester", "%alizadeh").count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotLike("requester", "%ali%").count()).isZero();

    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotLike("requester", "requester%").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotLike("requester", "%ali").count()).isOne();

    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotLike("requester", "vahid").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotLike("nonExistingVar", "string%").count()).isZero();

    // test with null value
    var historicTaskInstanceQuery = historyService.createHistoricTaskInstanceQuery();
    assertThatThrownBy(() -> historicTaskInstanceQuery.processVariableValueNotLike("requester", null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueGreaterThan() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
            singletonMap("requestNumber", 123));

    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueGreaterThan("requestNumber", 122).count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueGreaterThanOrEqual() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
            singletonMap("requestNumber", 123));

    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueGreaterThanOrEquals("requestNumber", 122).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueGreaterThanOrEquals("requestNumber", 123).count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueLessThan() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
            singletonMap("requestNumber", 123));

    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueLessThan("requestNumber", 124).count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueLessThanOrEqual() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
            singletonMap("requestNumber", 123));

    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueLessThanOrEquals("requestNumber", 123).count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueLessThanOrEquals("requestNumber", 124).count()).isOne();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testProcessVariableNameEqualsIgnoreCase() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess", VARIABLES);

    // when
    List<HistoricTaskInstance> eq = queryNameIgnoreCase().processVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> eqNameLC = queryNameIgnoreCase().processVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> eqValueLC = queryNameIgnoreCase().processVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE_LC).list();
    List<HistoricTaskInstance> eqNameValueLC = queryNameIgnoreCase().processVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_LC).list();

    // then
    assertThatListContainsOnlyExpectedElement(eq, instance);
    assertThatListContainsOnlyExpectedElement(eqNameLC, instance);
    assertThat(eqValueLC).isEmpty();
    assertThat(eqNameValueLC).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testProcessVariableNameNotEqualsIgnoreCase() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess", VARIABLES);
    // when
    List<HistoricTaskInstance> neq = queryNameIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> neqNameLC = queryNameIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> neqValueNE = queryNameIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME, VARIABLE_VALUE_NE).list();
    List<HistoricTaskInstance> neqNameLCValueNE = queryNameIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_NE).list();

    // then
    assertThat(neq).isEmpty();
    assertThat(neqNameLC).isEmpty();
    assertThatListContainsOnlyExpectedElement(neqValueNE, instance);
    assertThatListContainsOnlyExpectedElement(neqNameLCValueNE, instance);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testProcessVariableValueEqualsIgnoreCase() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess", VARIABLES);
    // when
    List<HistoricTaskInstance> eq = queryValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> eqNameLC = queryValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> eqValueLC = queryValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE_LC).list();
    List<HistoricTaskInstance> eqNameValueLC = queryValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_LC).list();

    // then
    assertThatListContainsOnlyExpectedElement(eq, instance);
    assertThat(eqNameLC).isEmpty();
    assertThatListContainsOnlyExpectedElement(eqValueLC, instance);
    assertThat(eqNameValueLC).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testProcessVariableValueNotEqualsIgnoreCase() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess", VARIABLES);
    // when
    List<HistoricTaskInstance> neq = queryValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> neqNameLC = queryValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> neqValueNE = queryValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME, VARIABLE_VALUE_NE).list();
    List<HistoricTaskInstance> neqNameLCValueNE = queryValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_NE).list();

    // then
    assertThat(neq).isEmpty();
    assertThat(neqNameLC).isEmpty();
    assertThatListContainsOnlyExpectedElement(neqValueNE, instance);
    assertThat(neqNameLCValueNE).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testProcessVariableValueLikeIgnoreCase() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess", VARIABLES);
    // when
    List<HistoricTaskInstance> like = queryNameValueIgnoreCase().processVariableValueLike(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> likeValueLC = queryValueIgnoreCase().processVariableValueLike(VARIABLE_NAME, VARIABLE_VALUE_LC_LIKE).list();

    // then
    assertThatListContainsOnlyExpectedElement(like, instance);
    assertThatListContainsOnlyExpectedElement(likeValueLC, instance);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testProcessVariableValueNotLikeIgnoreCase() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess", VARIABLES);
    // when
    List<HistoricTaskInstance> notLike = queryValueIgnoreCase().processVariableValueNotLike(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> notLikeValueNE = queryValueIgnoreCase().processVariableValueNotLike(VARIABLE_NAME, VARIABLE_VALUE_NE).list();
    List<HistoricTaskInstance> notLikeNameLC = queryValueIgnoreCase().processVariableValueNotLike(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> notLikeNameLCValueNE = queryValueIgnoreCase().processVariableValueNotLike(VARIABLE_NAME_LC, VARIABLE_VALUE_NE).list();

    // then
    assertThat(notLike).isEmpty();
    assertThatListContainsOnlyExpectedElement(notLikeValueNE, instance);
    assertThat(notLikeNameLC).isEmpty();
    assertThat(notLikeNameLCValueNE).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testProcessVariableNameAndValueEqualsIgnoreCase() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess", VARIABLES);
    // when
    List<HistoricTaskInstance> eq = queryNameValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> eqNameLC = queryNameValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> eqValueLC = queryNameValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE_LC).list();
    List<HistoricTaskInstance> eqValueNE = queryNameValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE_NE).list();
    List<HistoricTaskInstance> eqNameValueLC = queryNameValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_LC).list();
    List<HistoricTaskInstance> eqNameLCValueNE = queryNameValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_NE).list();

    // then
    assertThatListContainsOnlyExpectedElement(eq, instance);
    assertThatListContainsOnlyExpectedElement(eqNameLC, instance);
    assertThatListContainsOnlyExpectedElement(eqValueLC, instance);
    assertThat(eqValueNE).isEmpty();
    assertThatListContainsOnlyExpectedElement(eqNameValueLC, instance);
    assertThat(eqNameLCValueNE).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testProcessVariableNameAndValueNotEqualsIgnoreCase() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess", VARIABLES);
    // when
    List<HistoricTaskInstance> neq = queryNameValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> neqNameLC = queryNameValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> neqValueLC = queryNameValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME, VARIABLE_VALUE_LC).list();
    List<HistoricTaskInstance> neqValueNE = queryNameValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME, VARIABLE_VALUE_NE).list();
    List<HistoricTaskInstance> neqNameValueLC = queryNameValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_LC).list();
    List<HistoricTaskInstance> neqNameLCValueNE = queryNameValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_NE).list();

    // then
    assertThat(neq).isEmpty();
    assertThat(neqNameLC).isEmpty();
    assertThat(neqValueLC).isEmpty();
    assertThatListContainsOnlyExpectedElement(neqValueNE, instance);
    assertThat(neqNameValueLC).isEmpty();
    assertThatListContainsOnlyExpectedElement(neqNameLCValueNE, instance);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testTaskVariableValueEqualsNumber() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    List<Task> tasks = taskService.createTaskQuery().processDefinitionKey("oneTaskProcess").list();
    assertThat(tasks).hasSize(8);
    taskService.setVariableLocal(tasks.get(0).getId(), "var", 123L);
    taskService.setVariableLocal(tasks.get(1).getId(), "var", 12345L);
    taskService.setVariableLocal(tasks.get(2).getId(), "var", (short) 123);
    taskService.setVariableLocal(tasks.get(3).getId(), "var", 123.0d);
    taskService.setVariableLocal(tasks.get(4).getId(), "var", 123);
    taskService.setVariableLocal(tasks.get(5).getId(), "var", null);
    taskService.setVariableLocal(tasks.get(6).getId(), "var", Variables.longValue(null));
    taskService.setVariableLocal(tasks.get(7).getId(), "var", "123");

    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("var", Variables.numberValue(123L)).count()).isEqualTo(4);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("var", Variables.numberValue(123.0d)).count()).isEqualTo(4);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("var", Variables.numberValue((short) 123)).count()).isEqualTo(4);

    assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("var", Variables.numberValue(null)).count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testTaskVariableValueEqualsNumberIgnoreCase() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().singleResult();
    taskService.setVariablesLocal(task.getId(), VARIABLES);

    // when
    List<HistoricTaskInstance> eq =  queryValueIgnoreCase().taskVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> eqNameLC = queryValueIgnoreCase().taskVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> eqValueLC = queryValueIgnoreCase().taskVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE_LC).list();
    List<HistoricTaskInstance> eqNameValueLC = queryValueIgnoreCase().taskVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_LC).list();

    // then
    assertThatListContainsOnlyExpectedElement(eq, instance);
    assertThat(eqNameLC).isEmpty();
    assertThatListContainsOnlyExpectedElement(eqValueLC, instance);
    assertThat(eqNameValueLC).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testTaskVariableNameEqualsIgnoreCase() {
 // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().singleResult();
    taskService.setVariablesLocal(task.getId(), VARIABLES);

    // when
    List<HistoricTaskInstance> eq = queryNameIgnoreCase().taskVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> eqNameLC = queryNameIgnoreCase().taskVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> eqValueLC = queryNameIgnoreCase().taskVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE_LC).list();
    List<HistoricTaskInstance> eqNameValueLC = queryNameIgnoreCase().taskVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_LC).list();

    // then
    assertThatListContainsOnlyExpectedElement(eq, instance);
    assertThatListContainsOnlyExpectedElement(eqNameLC, instance);
    assertThat(eqValueLC).isEmpty();
    assertThat(eqNameValueLC).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void testTaskVariableNameAndValueEqualsIgnoreCase() {
 // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().singleResult();
    taskService.setVariablesLocal(task.getId(), VARIABLES);

    // when
    List<HistoricTaskInstance> eq = queryNameValueIgnoreCase().taskVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> eqNameLC = queryNameValueIgnoreCase().taskVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<HistoricTaskInstance> eqValueLC = queryNameValueIgnoreCase().taskVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE_LC).list();
    List<HistoricTaskInstance> eqValueNE = queryNameValueIgnoreCase().taskVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE_NE).list();
    List<HistoricTaskInstance> eqNameValueLC = queryNameValueIgnoreCase().taskVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_LC).list();
    List<HistoricTaskInstance> eqNameLCValueNE = queryNameValueIgnoreCase().taskVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_NE).list();

    // then
    assertThatListContainsOnlyExpectedElement(eq, instance);
    assertThatListContainsOnlyExpectedElement(eqNameLC, instance);
    assertThatListContainsOnlyExpectedElement(eqValueLC, instance);
    assertThat(eqValueNE).isEmpty();
    assertThatListContainsOnlyExpectedElement(eqNameValueLC, instance);
    assertThat(eqNameLCValueNE).isEmpty();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testTaskInvolvedUser() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    // if
    identityService.setAuthenticatedUserId("aAssignerId");
    taskService.addCandidateUser(taskId, "aUserId");
    taskService.addCandidateUser(taskId, "bUserId");
    taskService.deleteCandidateUser(taskId, "aUserId");
    taskService.deleteCandidateUser(taskId, "bUserId");
    Task taskAssignee = taskService.newTask("newTask");
    taskAssignee.setAssignee("aUserId");
    taskService.saveTask(taskAssignee);
    // query test
    assertThat(historyService.createHistoricTaskInstanceQuery().taskInvolvedUser("aUserId").count()).isEqualTo(2);
    assertThat(historyService.createHistoricTaskInstanceQuery().taskInvolvedUser("bUserId").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskInvolvedUser("invalidUserId").count()).isZero();
    taskService.deleteTask("newTask",true);
  }


  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testTaskInvolvedUserAsOwner() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.setOwner(taskId, "user");

    // when
    List<HistoricTaskInstance> historicTasks =
        historyService.createHistoricTaskInstanceQuery().taskInvolvedUser("user").list();

    // query test
    assertThat(historicTasks).hasSize(1);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testTaskInvolvedGroup() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    // if
    identityService.setAuthenticatedUserId("aAssignerId");
    taskService.addCandidateGroup(taskId, "aGroupId");
    taskService.addCandidateGroup(taskId, "bGroupId");
    taskService.deleteCandidateGroup(taskId, "aGroupId");
    taskService.deleteCandidateGroup(taskId, "bGroupId");
    // query test
    assertThat(historyService.createHistoricTaskInstanceQuery().taskInvolvedGroup("aGroupId").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskInvolvedGroup("bGroupId").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskInvolvedGroup("invalidGroupId").count()).isZero();

    taskService.deleteTask("newTask",true);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testTaskHadCandidateUser() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    // if
    identityService.setAuthenticatedUserId("aAssignerId");
    taskService.addCandidateUser(taskId, "aUserId");
    taskService.addCandidateUser(taskId, "bUserId");
    taskService.deleteCandidateUser(taskId, "bUserId");
    Task taskAssignee = taskService.newTask("newTask");
    taskAssignee.setAssignee("aUserId");
    taskService.saveTask(taskAssignee);
    // query test
    assertThat(historyService.createHistoricTaskInstanceQuery().taskHadCandidateUser("aUserId").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskHadCandidateUser("bUserId").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskHadCandidateUser("invalidUserId").count()).isZero();
    // delete test
    taskService.deleteTask("newTask",true);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testTaskHadCandidateGroup() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    // if
    identityService.setAuthenticatedUserId("aAssignerId");
    taskService.addCandidateGroup(taskId, "bGroupId");
    taskService.deleteCandidateGroup(taskId, "bGroupId");
    // query test
    assertThat(historyService.createHistoricTaskInstanceQuery().taskHadCandidateGroup("bGroupId").count()).isOne();
    assertThat(historyService.createHistoricTaskInstanceQuery().taskHadCandidateGroup("invalidGroupId").count()).isZero();
    // delete test
    taskService.deleteTask("newTask",true);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testWithCandidateGroups() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    identityService.setAuthenticatedUserId("aAssignerId");
    taskService.addCandidateGroup(taskId, "aGroupId");

    // then
    assertThat(historyService.createHistoricTaskInstanceQuery().withCandidateGroups().count()).isOne();

    // cleanup
    taskService.deleteTask("newTask", true);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testWithoutCandidateGroups() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    identityService.setAuthenticatedUserId("aAssignerId");
    taskService.addCandidateGroup(taskId, "aGroupId");

    // when
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // then
    assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(2);
    assertThat(historyService.createHistoricTaskInstanceQuery().withoutCandidateGroups().count()).isOne();

    // cleanup
    taskService.deleteTask("newTask", true);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testGroupTaskQuery() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    // if
    identityService.setAuthenticatedUserId("aAssignerId");
    taskService.addCandidateUser(taskId, "aUserId");
    taskService.addCandidateGroup(taskId, "aGroupId");
    taskService.addCandidateGroup(taskId, "bGroupId");
    Task taskOne = taskService.newTask("taskOne");
    taskOne.setAssignee("aUserId");
    taskService.saveTask(taskOne);
    Task taskTwo = taskService.newTask("taskTwo");
    taskTwo.setAssignee("aUserId");
    taskService.saveTask(taskTwo);
    Task taskThree = taskService.newTask("taskThree");
    taskThree.setOwner("aUserId");
    taskService.saveTask(taskThree);
    taskService.deleteCandidateGroup(taskId, "aGroupId");
    taskService.deleteCandidateGroup(taskId, "bGroupId");
    historyService.createHistoricTaskInstanceQuery();

    // Query test
    HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();
    assertThat(query.taskInvolvedUser("aUserId").count()).isEqualTo(4);
    query = historyService.createHistoricTaskInstanceQuery();
    assertThat(query.taskHadCandidateUser("aUserId").count()).isOne();
    query = historyService.createHistoricTaskInstanceQuery();
    assertThat(query.taskHadCandidateGroup("aGroupId").count()).isOne();
    assertThat(query.taskHadCandidateGroup("bGroupId").count()).isOne();
    assertThat(query.taskInvolvedUser("aUserId").count()).isZero();
    query = historyService.createHistoricTaskInstanceQuery();
    assertThat(query.taskInvolvedUser("aUserId").count()).isEqualTo(4);
    assertThat(query.taskHadCandidateUser("aUserId").count()).isOne();
    assertThat(query.taskInvolvedUser("aUserId").count()).isOne();
    // delete task
    taskService.deleteTask("taskOne",true);
    taskService.deleteTask("taskTwo",true);
    taskService.deleteTask("taskThree",true);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testTaskWasAssigned() {
    // given
    Task taskOne = taskService.newTask("taskOne");
    Task taskTwo = taskService.newTask("taskTwo");
    Task taskThree = taskService.newTask("taskThree");

    // when
    taskOne.setAssignee("aUserId");
    taskService.saveTask(taskOne);

    taskTwo.setAssignee("anotherUserId");
    taskService.saveTask(taskTwo);

    taskService.saveTask(taskThree);

    List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().taskAssigned().list();

    // then
    assertThat(list).hasSize(2);

    // cleanup
    taskService.deleteTask("taskOne",true);
    taskService.deleteTask("taskTwo",true);
    taskService.deleteTask("taskThree",true);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testTaskWasUnassigned() {
    // given
    Task taskOne = taskService.newTask("taskOne");
    Task taskTwo = taskService.newTask("taskTwo");
    Task taskThree = taskService.newTask("taskThree");

    // when
    taskOne.setAssignee("aUserId");
    taskService.saveTask(taskOne);

    taskTwo.setAssignee("anotherUserId");
    taskService.saveTask(taskTwo);

    taskService.saveTask(taskThree);

    List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().taskUnassigned().list();

    // then
    assertThat(list).hasSize(1);

    // cleanup
    taskService.deleteTask("taskOne",true);
    taskService.deleteTask("taskTwo",true);
    taskService.deleteTask("taskThree",true);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testTaskReturnedBeforeEndTime() {
    // given
    Task taskOne = taskService.newTask("taskOne");

    // when
    taskOne.setAssignee("aUserId");
    taskService.saveTask(taskOne);

    Calendar hourAgo = Calendar.getInstance();
    hourAgo.add(Calendar.HOUR_OF_DAY, -1);
    ClockUtil.setCurrentTime(hourAgo.getTime());

    taskService.complete(taskOne.getId());

    List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
            .finishedBefore(hourAgo.getTime()).list();

    // then
    assertThat(list).hasSize(1);

    // cleanup
    taskService.deleteTask("taskOne",true);
    ClockUtil.reset();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testTaskNotReturnedAfterEndTime() {
    // given
    Task taskOne = taskService.newTask("taskOne");

    // when
    taskOne.setAssignee("aUserId");
    taskService.saveTask(taskOne);

    Calendar hourAgo = Calendar.getInstance();
    hourAgo.add(Calendar.HOUR_OF_DAY, -1);
    ClockUtil.setCurrentTime(hourAgo.getTime());

    taskService.complete(taskOne.getId());

    List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
            .finishedAfter(Calendar.getInstance().getTime()).list();

    // then
    assertThat(list).isEmpty();

    // cleanup
    taskService.deleteTask("taskOne",true);

    ClockUtil.reset();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldQueryForTasksWithoutDueDate() {
    // given
    Task taskOne = taskService.newTask("taskOne");
    taskOne.setDueDate(new Date());
    taskService.saveTask(taskOne);
    Task taskTwo = taskService.newTask("taskTwo");
    taskService.saveTask(taskTwo);

    // when
    taskService.complete(taskOne.getId());
    taskService.complete(taskTwo.getId());

    // then
    assertThat(historyService.createHistoricTaskInstanceQuery().withoutTaskDueDate().count()).isOne();

    // cleanup
    taskService.deleteTask("taskOne", true);
    taskService.deleteTask("taskTwo", true);
  }

  private void assertThatListContainsOnlyExpectedElement(List<HistoricTaskInstance> instances, ProcessInstance instance) {
    assertThat(instances).hasSize(1);
    assertThat(instances.get(0).getProcessInstanceId()).isEqualTo(instance.getId());
  }

  private HistoricTaskInstanceQuery queryNameIgnoreCase() {
    return historyService.createHistoricTaskInstanceQuery().matchVariableNamesIgnoreCase();
  }

  private HistoricTaskInstanceQuery queryValueIgnoreCase() {
    return historyService.createHistoricTaskInstanceQuery().matchVariableValuesIgnoreCase();
  }

  private HistoricTaskInstanceQuery queryNameValueIgnoreCase() {
    return historyService.createHistoricTaskInstanceQuery().matchVariableNamesIgnoreCase().matchVariableValuesIgnoreCase();
  }
}
