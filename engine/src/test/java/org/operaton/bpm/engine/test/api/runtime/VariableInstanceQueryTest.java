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
package org.operaton.bpm.engine.test.api.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.runtime.VariableInstanceQuery;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.runtime.util.CustomSerializable;
import org.operaton.bpm.engine.test.api.runtime.util.FailingSerializable;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.engine.variable.value.ObjectValue;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * @author roman.smirnov
 */
class VariableInstanceQueryTest {

  protected static final String PROC_DEF_KEY = "oneTaskProcess";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  CaseService caseService;
  ManagementService managementService;

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQuery() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("intVar", 123);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    assertThat(query).isNotNull();

    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(query.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getId()).isNotNull();
      if ("intVar".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("intVar");
        assertThat(variableInstance.getValue()).isEqualTo(123);
      } else if ("stringVar".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("stringVar");
        assertThat(variableInstance.getValue()).isEqualTo("test");
      } else {
        fail("An unexpected variable '%s' was found with value %s".formatted(variableInstance.getName(), variableInstance.getValue()));
      }

    }
  }

  @Test
  void testQueryByVariableId() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("var1", "test");
    variables.put("var2", "test");
    Task task = taskService.newTask();
    taskService.saveTask(task);
    taskService.setVariablesLocal(task.getId(), variables);
    VariableInstance result = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(result).isNotNull();

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableId(result.getId());

    // then
    assertThat(query).isNotNull();
    VariableInstance resultById = query.singleResult();
    assertThat(resultById.getId()).isEqualTo(result.getId());

    // delete taskoneTaskProcess
    taskService.deleteTask(task.getId(), true);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByVariableName() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableName("stringVar");

    // then
    verifyQueryResult(query, "test");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByVariableNames() {
    // given
    String variableValue = "a";
    Map<String, Object> variables = new HashMap<>();
    variables.put("process", variableValue);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.setVariableLocal(task.getId(), "task", variableValue);
    runtimeService.setVariableLocal(task.getExecutionId(), "execution", variableValue);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableNameIn("task", "process", "execution");

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    assertThat(query.count()).isEqualTo(3);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getValue()).isEqualTo(variableValue);
      assertThat(variableInstance.getTypeName()).isEqualTo("string");
    }

    assertThat(runtimeService.createVariableInstanceQuery().variableName("task").variableNameIn("task", "execution").count()).isOne();
    assertThat(runtimeService.createVariableInstanceQuery().variableName("task").variableNameIn("process", "execution").count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByVariableNameLike() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("string%Var", "test");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableNameLike("%ing\\%V%");

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    assertThat(query.count()).isOne();

    VariableInstance variableInstance = result.get(0);
    assertThat(variableInstance.getName()).isEqualTo("string%Var");
    assertThat(variableInstance.getValue()).isEqualTo("test");
    assertThat(variableInstance.getTypeName()).isEqualTo("string");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByVariableName_EmptyString() {
    // given
    String varName = "testVar";
    VariableMap variables = Variables.putValue(varName, "");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().variableName(varName).singleResult();

    // then
    assertThat(variableInstance.getValue()).isNotNull();
    assertThat(variableInstance.getValue()).isEqualTo("");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByVariableNameLikeWithoutAnyResult() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableNameLike("%ingV_");

    // then
    List<VariableInstance> result = query.list();
    assertThat(result).isEmpty();

    assertThat(query.count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueEquals_String() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueEquals("stringVar", "test");

    // then
    verifyQueryResult(query, "test");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueEquals_EmptyString() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueEquals("stringVar", "");

    // then
    verifyQueryResult(query, "");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueNotEquals_String() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("stringVar", "test123");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueNotEquals("stringVar", "test123");

    // then
    verifyQueryResult(query, "test");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueNotEquals_EmptyString() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("stringVar", "");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueNotEquals("stringVar", "");

    // then
    verifyQueryResult(query, "test");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueGreaterThan_String() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "a");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("stringVar", "b");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("stringVar", "c");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueGreaterThan("stringVar", "a");

    // then
    verifyQueryResult(query, "b", "c");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueGreaterThanOrEqual_String() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "a");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("stringVar", "b");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("stringVar", "c");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueGreaterThanOrEqual("stringVar", "a");

    // then
    verifyQueryResult(query, "a", "b", "c");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueLessThan_String() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "a");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("stringVar", "b");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("stringVar", "c");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueLessThan("stringVar", "c");

    // then
    verifyQueryResult(query, "b", "a");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueLessThanOrEqual_String() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "a");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("stringVar", "b");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("stringVar", "c");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueLessThanOrEqual("stringVar", "c");

    // then
    verifyQueryResult(query, "a", "b", "c");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueLike_String() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test123");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("stringVar", "test456");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("stringVar", "test789");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueLike("stringVar", "test%");

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    assertThat(query.count()).isEqualTo(3);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("stringVar");
      assertThat(variableInstance.getTypeName()).isEqualTo("string");
      if ("test123".equals(variableInstance.getValue())) {
        assertThat(variableInstance.getValue()).isEqualTo("test123");
      } else if ("test456".equals(variableInstance.getValue())) {
        assertThat(variableInstance.getValue()).isEqualTo("test456");
      } else if ("test789".equals(variableInstance.getValue())) {
        assertThat(variableInstance.getValue()).isEqualTo("test789");
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueLikeWithEscape_String() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test_123");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("stringVar", "test%456");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueLike("stringVar", "test\\_%");
    verifyQueryResult(query, "test_123");

    query = runtimeService.createVariableInstanceQuery().variableValueLike("stringVar", "test\\%%");
    verifyQueryResult(query, "test%456");

  }

  private void verifyQueryResult(VariableInstanceQuery query, String...varValues) {
    // then
    assertThat(query.count()).isEqualTo(varValues.length);

    List<VariableInstance> result = query.list();
    assertThat(result).hasSize(varValues.length);

    List<String> expected = List.of(varValues);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("stringVar");
      assertThat(variableInstance.getTypeName()).isEqualTo("string");
      assertThat(expected).contains(variableInstance.getValue().toString());
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueEquals_Integer() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("intValue", 1234);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueEquals("intValue", 1234);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    assertThat(query.count()).isOne();

    VariableInstance variableInstance = result.get(0);
    assertThat(variableInstance.getName()).isEqualTo("intValue");
    assertThat(variableInstance.getValue()).isEqualTo(1234);
    assertThat(variableInstance.getTypeName()).isEqualTo("integer");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueNotEquals_Integer() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("intValue", 1234);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("intValue", 5555);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueNotEquals("intValue", 5555);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    assertThat(query.count()).isOne();

    VariableInstance variableInstance = result.get(0);
    assertThat(variableInstance.getName()).isEqualTo("intValue");
    assertThat(variableInstance.getValue()).isEqualTo(1234);
    assertThat(variableInstance.getTypeName()).isEqualTo("integer");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableGreaterThan_Integer() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("intValue", 1234);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("intValue", 5555);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("intValue", 9876);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueGreaterThan("intValue", 1234);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(query.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("intValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("integer");
      if (variableInstance.getValue().equals(5555)) {
        assertThat(variableInstance.getValue()).isEqualTo(5555);
      } else if (variableInstance.getValue().equals(9876)) {
        assertThat(variableInstance.getValue()).isEqualTo(9876);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableGreaterThanAndEqual_Integer() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("intValue", 1234);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("intValue", 5555);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("intValue", 9876);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueGreaterThanOrEqual("intValue", 1234);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    assertThat(query.count()).isEqualTo(3);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("intValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("integer");
      if (variableInstance.getValue().equals(1234)) {
        assertThat(variableInstance.getValue()).isEqualTo(1234);
      } else if (variableInstance.getValue().equals(5555)) {
        assertThat(variableInstance.getValue()).isEqualTo(5555);
      } else if (variableInstance.getValue().equals(9876)) {
        assertThat(variableInstance.getValue()).isEqualTo(9876);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableLessThan_Integer() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("intValue", 1234);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("intValue", 5555);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("intValue", 9876);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueLessThan("intValue", 9876);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(query.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("intValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("integer");
      if (variableInstance.getValue().equals(5555)) {
        assertThat(variableInstance.getValue()).isEqualTo(5555);
      } else if (variableInstance.getValue().equals(1234)) {
        assertThat(variableInstance.getValue()).isEqualTo(1234);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableLessThanAndEqual_Integer() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("intValue", 1234);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("intValue", 5555);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("intValue", 9876);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueLessThanOrEqual("intValue", 9876);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    assertThat(query.count()).isEqualTo(3);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("intValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("integer");
      if (variableInstance.getValue().equals(1234)) {
        assertThat(variableInstance.getValue()).isEqualTo(1234);
      } else if (variableInstance.getValue().equals(5555)) {
        assertThat(variableInstance.getValue()).isEqualTo(5555);
      } else if (variableInstance.getValue().equals(9876)) {
        assertThat(variableInstance.getValue()).isEqualTo(9876);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueEquals_Long() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("longValue", 123456L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueEquals("longValue", 123456L);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    assertThat(query.count()).isOne();

    VariableInstance variableInstance = result.get(0);
    assertThat(variableInstance.getName()).isEqualTo("longValue");
    assertThat(variableInstance.getValue()).isEqualTo(123456L);
    assertThat(variableInstance.getTypeName()).isEqualTo("long");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueNotEquals_Long() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("longValue", 123456L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("longValue", 987654L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueNotEquals("longValue", 987654L);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    assertThat(query.count()).isOne();

    VariableInstance variableInstance = result.get(0);
    assertThat(variableInstance.getName()).isEqualTo("longValue");
    assertThat(variableInstance.getValue()).isEqualTo(123456L);
    assertThat(variableInstance.getTypeName()).isEqualTo("long");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableGreaterThan_Long() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("longValue", 123456L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("longValue", 987654L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("longValue", 555555L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueGreaterThan("longValue", 123456L);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(query.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("longValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("long");
      if (variableInstance.getValue().equals(555555L)) {
        assertThat(variableInstance.getValue()).isEqualTo(555555L);
      } else if (variableInstance.getValue().equals(987654L)) {
        assertThat(variableInstance.getValue()).isEqualTo(987654L);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableGreaterThanAndEqual_Long() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("longValue", 123456L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("longValue", 987654L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("longValue", 555555L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueGreaterThanOrEqual("longValue", 123456L);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    assertThat(query.count()).isEqualTo(3);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("longValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("long");
      if (variableInstance.getValue().equals(123456L)) {
        assertThat(variableInstance.getValue()).isEqualTo(123456L);
      } else if (variableInstance.getValue().equals(555555L)) {
        assertThat(variableInstance.getValue()).isEqualTo(555555L);
      } else if (variableInstance.getValue().equals(987654L)) {
        assertThat(variableInstance.getValue()).isEqualTo(987654L);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableLessThan_Long() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("longValue", 123456L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("longValue", 987654L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("longValue", 555555L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueLessThan("longValue", 987654L);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(query.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("longValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("long");
      if (variableInstance.getValue().equals(123456L)) {
        assertThat(variableInstance.getValue()).isEqualTo(123456L);
      } else if (variableInstance.getValue().equals(555555L)) {
        assertThat(variableInstance.getValue()).isEqualTo(555555L);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableLessThanAndEqual_Long() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("longValue", 123456L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("longValue", 987654L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("longValue", 555555L);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueLessThanOrEqual("longValue", 987654L);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    assertThat(query.count()).isEqualTo(3);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("longValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("long");
      if (variableInstance.getValue().equals(123456L)) {
        assertThat(variableInstance.getValue()).isEqualTo(123456L);
      } else if (variableInstance.getValue().equals(555555L)) {
        assertThat(variableInstance.getValue()).isEqualTo(555555L);
      } else if (variableInstance.getValue().equals(987654L)) {
        assertThat(variableInstance.getValue()).isEqualTo(987654L);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueEquals_Double() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("doubleValue", 123.456);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueEquals("doubleValue", 123.456);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    assertThat(query.count()).isOne();

    VariableInstance variableInstance = result.get(0);
    assertThat(variableInstance.getName()).isEqualTo("doubleValue");
    assertThat(variableInstance.getValue()).isEqualTo(123.456);
    assertThat(variableInstance.getTypeName()).isEqualTo("double");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueNotEquals_Double() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("doubleValue", 123.456);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("doubleValue", 654.321);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueNotEquals("doubleValue", 654.321);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    assertThat(query.count()).isOne();

    VariableInstance variableInstance = result.get(0);
    assertThat(variableInstance.getName()).isEqualTo("doubleValue");
    assertThat(variableInstance.getValue()).isEqualTo(123.456);
    assertThat(variableInstance.getTypeName()).isEqualTo("double");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableGreaterThan_Double() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("doubleValue", 123.456);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("doubleValue", 654.321);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("doubleValue", 999.999);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueGreaterThan("doubleValue", 123.456);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(query.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("doubleValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("double");
      if (variableInstance.getValue().equals(654.321)) {
        assertThat(variableInstance.getValue()).isEqualTo(654.321);
      } else if (variableInstance.getValue().equals(999.999)) {
        assertThat(variableInstance.getValue()).isEqualTo(999.999);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableGreaterThanAndEqual_Double() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("doubleValue", 123.456);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("doubleValue", 654.321);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("doubleValue", 999.999);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueGreaterThanOrEqual("doubleValue", 123.456);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    assertThat(query.count()).isEqualTo(3);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("doubleValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("double");
      if (variableInstance.getValue().equals(123.456)) {
        assertThat(variableInstance.getValue()).isEqualTo(123.456);
      } else if (variableInstance.getValue().equals(654.321)) {
        assertThat(variableInstance.getValue()).isEqualTo(654.321);
      } else if (variableInstance.getValue().equals(999.999)) {
        assertThat(variableInstance.getValue()).isEqualTo(999.999);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableLessThan_Double() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("doubleValue", 123.456);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("doubleValue", 654.321);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("doubleValue", 999.999);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueLessThan("doubleValue", 999.999);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(query.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("doubleValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("double");
      if (variableInstance.getValue().equals(123.456)) {
        assertThat(variableInstance.getValue()).isEqualTo(123.456);
      } else if (variableInstance.getValue().equals(654.321)) {
        assertThat(variableInstance.getValue()).isEqualTo(654.321);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableLessThanAndEqual_Double() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("doubleValue", 123.456);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("doubleValue", 654.321);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("doubleValue", 999.999);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueLessThanOrEqual("doubleValue", 999.999);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    assertThat(query.count()).isEqualTo(3);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("doubleValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("double");
      if (variableInstance.getValue().equals(123.456)) {
        assertThat(variableInstance.getValue()).isEqualTo(123.456);
      } else if (variableInstance.getValue().equals(654.321)) {
        assertThat(variableInstance.getValue()).isEqualTo(654.321);
      } else if (variableInstance.getValue().equals(999.999)) {
        assertThat(variableInstance.getValue()).isEqualTo(999.999);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueEquals_Short() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("shortValue", (short) 123);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueEquals("shortValue", (short) 123);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    assertThat(query.count()).isOne();

    VariableInstance variableInstance = result.get(0);
    assertThat(variableInstance.getName()).isEqualTo("shortValue");
    assertThat(variableInstance.getValue()).isEqualTo((short) 123);
    assertThat(variableInstance.getTypeName()).isEqualTo("short");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByVariableValueNotEquals_Short() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("shortValue", (short) 123);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("shortValue", (short) 999);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueNotEquals("shortValue", (short) 999);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    assertThat(query.count()).isOne();

    VariableInstance variableInstance = result.get(0);
    assertThat(variableInstance.getName()).isEqualTo("shortValue");
    assertThat(variableInstance.getValue()).isEqualTo((short) 123);
    assertThat(variableInstance.getTypeName()).isEqualTo("short");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableGreaterThan_Short() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("shortValue", (short) 123);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("shortValue", (short) 999);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("shortValue", (short) 555);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueGreaterThan("shortValue", (short) 123);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(query.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("shortValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("short");
      if (variableInstance.getValue().equals((short) 555)) {
        assertThat(variableInstance.getValue()).isEqualTo((short) 555);
      } else if (variableInstance.getValue().equals((short) 999)) {
        assertThat(variableInstance.getValue()).isEqualTo((short) 999);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableGreaterThanAndEqual_Short() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("shortValue", (short) 123);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("shortValue", (short) 999);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("shortValue", (short) 555);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueGreaterThanOrEqual("shortValue", (short) 123);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    assertThat(query.count()).isEqualTo(3);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("shortValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("short");
      if (variableInstance.getValue().equals((short) 123)) {
        assertThat(variableInstance.getValue()).isEqualTo((short) 123);
      } else if (variableInstance.getValue().equals((short) 555)) {
        assertThat(variableInstance.getValue()).isEqualTo((short) 555);
      } else if (variableInstance.getValue().equals((short) 999)) {
        assertThat(variableInstance.getValue()).isEqualTo((short) 999);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableLessThan_Short() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("shortValue", (short) 123);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("shortValue", (short) 999);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("shortValue", (short) 555);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueLessThan("shortValue", (short) 999);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(query.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("shortValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("short");
      if (variableInstance.getValue().equals((short) 123)) {
        assertThat(variableInstance.getValue()).isEqualTo((short) 123);
      } else if (variableInstance.getValue().equals((short) 555)) {
        assertThat(variableInstance.getValue()).isEqualTo((short) 555);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableLessThanAndEqual_Short() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("shortValue", (short) 123);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("shortValue", (short) 999);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("shortValue", (short) 555);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueLessThanOrEqual("shortValue", (short) 999);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    assertThat(query.count()).isEqualTo(3);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("shortValue");
      assertThat(variableInstance.getTypeName()).isEqualTo("short");
      if (variableInstance.getValue().equals((short) 123)) {
        assertThat(variableInstance.getValue()).isEqualTo((short) 123);
      } else if (variableInstance.getValue().equals((short) 555)) {
        assertThat(variableInstance.getValue()).isEqualTo((short) 555);
      } else if (variableInstance.getValue().equals((short) 999)) {
        assertThat(variableInstance.getValue()).isEqualTo((short) 999);
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueEquals_Bytes() {
    // given
    byte[] bytes = "somebytes".getBytes();
    Map<String, Object> variables = new HashMap<>();
    variables.put("bytesVar", bytes);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueEquals("bytesVar", bytes);

    // then
    assertThatThrownBy(query::list).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueEquals_Date() {
    // given
     Date now = new Date();

    Map<String, Object> variables = new HashMap<>();
    variables.put("date", now);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueEquals("date", now);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    assertThat(query.count()).isOne();

    VariableInstance variableInstance = result.get(0);
    assertThat(variableInstance.getName()).isEqualTo("date");
    assertThat(variableInstance.getValue()).isEqualTo(now);
    assertThat(variableInstance.getTypeName()).isEqualTo("date");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueEqualsWithoutAnyResult() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueEquals("stringVar", "notFoundValue");

    // then
    List<VariableInstance> result = query.list();
    assertThat(result).isEmpty();

    assertThat(query.count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByNameAndVariableValueEquals_NullValue() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("nullValue", null);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueEquals("nullValue", null);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    assertThat(query.count()).isOne();

    VariableInstance variableInstance = result.get(0);
    assertThat(variableInstance.getName()).isEqualTo("nullValue");
    assertThat(variableInstance.getValue()).isNull();
    assertThat(variableInstance.getTypeName()).isEqualTo("null");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByVariableValueNotEquals_NullValue() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("value", null);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("value", (short) 999);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("value", "abc");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableValueNotEquals("value", null);

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(query.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getName()).isEqualTo("value");
      if (variableInstance.getValue().equals((short) 999)) {
        assertThat(variableInstance.getValue()).isEqualTo((short) 999);
        assertThat(variableInstance.getTypeName()).isEqualTo("short");
      } else if ("abc".equals(variableInstance.getValue())) {
        assertThat(variableInstance.getValue()).isEqualTo("abc");
        assertThat(variableInstance.getTypeName()).isEqualTo("string");
      } else {
        fail("A non expected value occurred: " + variableInstance.getValue());
      }

    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByProcessInstanceId() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    variables.put("myVar", "test123");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().processInstanceIdIn(processInstance.getId());

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(query.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getTypeName()).isEqualTo("string");
      if ("myVar".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("myVar");
        assertThat(variableInstance.getValue()).isEqualTo("test123");
      } else if ("stringVar".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("stringVar");
        assertThat(variableInstance.getValue()).isEqualTo("test");
      } else {
        fail("An unexpected variable '%s' was found with value %s".formatted(variableInstance.getName(), variableInstance.getValue()));
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByProcessInstanceIds() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    variables.put("myVar", "test123");
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().processInstanceIdIn(processInstance1.getId(), processInstance2.getId());

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(query.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getTypeName()).isEqualTo("string");
      if ("myVar".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("myVar");
        assertThat(variableInstance.getValue()).isEqualTo("test123");
      } else if ("stringVar".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("stringVar");
        assertThat(variableInstance.getValue()).isEqualTo("test");
      } else {
        fail("An unexpected variable '%s' was found with value %s".formatted(variableInstance.getName(), variableInstance.getValue()));
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByProcessInstanceIdWithoutAnyResult() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().processInstanceIdIn("aProcessInstanceId");

    // then
    List<VariableInstance> result = query.list();
    assertThat(result).isEmpty();

    assertThat(query.count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByExecutionId() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    variables.put("myVar", "test123");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().executionIdIn(processInstance.getId());

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(query.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getTypeName()).isEqualTo("string");
      if ("myVar".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("myVar");
        assertThat(variableInstance.getValue()).isEqualTo("test123");
      } else if ("stringVar".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("stringVar");
        assertThat(variableInstance.getValue()).isEqualTo("test");
      } else {
        fail("An unexpected variable '%s' was found with value %s".formatted(variableInstance.getName(), variableInstance.getValue()));
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByExecutionIds() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    variables1.put("myVar", "test123");
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("myVar", "test123");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().executionIdIn(processInstance1.getId(), processInstance2.getId());

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    assertThat(query.count()).isEqualTo(3);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getTypeName()).isEqualTo("string");
      if ("myVar".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("myVar");
        assertThat(variableInstance.getValue()).isEqualTo("test123");
      } else if ("stringVar".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("stringVar");
        assertThat(variableInstance.getValue()).isEqualTo("test");
      } else {
        fail("An unexpected variable '%s' was found with value %s".formatted(variableInstance.getName(), variableInstance.getValue()));
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByExecutionIdWithoutAnyResult() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().executionIdIn("anExecutionId");

    // then
    List<VariableInstance> result = query.list();
    assertThat(result).isEmpty();

    assertThat(query.count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByTaskId() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    taskService.setVariableLocal(task.getId(), "taskVariable", "aCustomValue");

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().taskIdIn(task.getId());

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    assertThat(query.count()).isOne();

    VariableInstance variableInstance = result.get(0);
    assertThat(variableInstance.getName()).isEqualTo("taskVariable");
    assertThat(variableInstance.getValue()).isEqualTo("aCustomValue");
    assertThat(variableInstance.getTypeName()).isEqualTo("string");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByTaskIds() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY);
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY);

    Task task1 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
    Task task2 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
    Task task3 = taskService.createTaskQuery().processInstanceId(processInstance3.getId()).singleResult();

    taskService.setVariableLocal(task1.getId(), "taskVariable", "aCustomValue");
    taskService.setVariableLocal(task2.getId(), "anotherTaskVariable", "aCustomValue");

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().taskIdIn(task1.getId(), task2.getId(), task3.getId());

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(query.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getTypeName()).isEqualTo("string");
      if ("taskVariable".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("taskVariable");
        assertThat(variableInstance.getValue()).isEqualTo("aCustomValue");
      } else if ("anotherTaskVariable".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("anotherTaskVariable");
        assertThat(variableInstance.getValue()).isEqualTo("aCustomValue");
      } else {
        fail("An unexpected variable '%s' was found with value %s".formatted(variableInstance.getName(), variableInstance.getValue()));
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByTaskIdWithoutAnyResult() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    taskService.setVariableLocal(task.getId(), "taskVariable", "aCustomValue");

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().taskIdIn("aTaskId");

    // then
    List<VariableInstance> result = query.list();
    assertThat(result).isEmpty();

    assertThat(query.count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/VariableInstanceQueryTest.taskInEmbeddedSubProcess.bpmn20.xml"})
  void testQueryByVariableScopeId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task).isNotNull();

    // get variable scope ids
    String taskId = task.getId();
    String executionId = task.getExecutionId();
    String processInstanceId = task.getProcessInstanceId();

    // set variables
    String variableName = "foo";
    Map<String, String> variables = new HashMap<>();
    variables.put(taskId, "task");
    variables.put(executionId, "execution");
    variables.put(processInstanceId, "processInstance");

    taskService.setVariableLocal(taskId, variableName, variables.get(taskId));
    runtimeService.setVariableLocal(executionId, variableName, variables.get(executionId));
    runtimeService.setVariableLocal(processInstanceId, variableName, variables.get(processInstanceId));

    List<VariableInstance> variableInstances;

    // query by variable scope id
    for (String variableScopeId : variables.keySet()) {
      variableInstances = runtimeService.createVariableInstanceQuery().variableScopeIdIn(variableScopeId).list();
      assertThat(variableInstances).hasSize(1);
      assertThat(variableInstances.get(0).getName()).isEqualTo(variableName);
      assertThat(variableInstances.get(0).getValue()).isEqualTo(variables.get(variableScopeId));
    }

    // query by multiple variable scope ids
    variableInstances = runtimeService.createVariableInstanceQuery().variableScopeIdIn(taskId, executionId, processInstanceId).list();
    assertThat(variableInstances).hasSize(3);

    // remove task variable
    taskService.removeVariableLocal(taskId, variableName);

    variableInstances = runtimeService.createVariableInstanceQuery().variableScopeIdIn(taskId).list();
    assertThat(variableInstances).isEmpty();

    variableInstances = runtimeService.createVariableInstanceQuery().variableScopeIdIn(taskId, executionId, processInstanceId).list();
    assertThat(variableInstances).hasSize(2);

    // remove process instance variable variable
    runtimeService.removeVariable(processInstanceId, variableName);

    variableInstances = runtimeService.createVariableInstanceQuery().variableScopeIdIn(processInstanceId, taskId).list();
    assertThat(variableInstances).isEmpty();

    variableInstances = runtimeService.createVariableInstanceQuery().variableScopeIdIn(taskId, executionId, processInstanceId).list();
    assertThat(variableInstances).hasSize(1);

    // remove execution variable
    runtimeService.removeVariable(executionId, variableName);

    variableInstances = runtimeService.createVariableInstanceQuery().variableScopeIdIn(taskId, executionId, processInstanceId).list();
    assertThat(variableInstances).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByActivityInstanceId() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);
    String activityId = runtimeService.getActivityInstance(processInstance.getId()).getChildActivityInstances()[0].getId();

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.setVariableLocal(task.getId(), "taskVariable", "aCustomValue");

    // when
    VariableInstanceQuery taskVariablesQuery = runtimeService.createVariableInstanceQuery().activityInstanceIdIn(activityId);
    VariableInstanceQuery processVariablesQuery = runtimeService.createVariableInstanceQuery().activityInstanceIdIn(processInstance.getId());

    // then
    VariableInstance taskVar = taskVariablesQuery.singleResult();
    assertThat(taskVar).isNotNull();

    assertThat(taskVariablesQuery.count()).isOne();
    assertThat(taskVar.getTypeName()).isEqualTo("string");
    assertThat(taskVar.getName()).isEqualTo("taskVariable");
    assertThat(taskVar.getValue()).isEqualTo("aCustomValue");

    VariableInstance processVar = processVariablesQuery.singleResult();

    assertThat(processVariablesQuery.count()).isOne();
    assertThat(processVar.getTypeName()).isEqualTo("string");
    assertThat(processVar.getName()).isEqualTo("stringVar");
    assertThat(processVar.getValue()).isEqualTo("test");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryByActivityInstanceIds() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    variables1.put("myVar", "test123");
    ProcessInstance procInst1 = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("myVar", "test123");
    ProcessInstance procInst2 =  runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    Map<String, Object> variables3 = new HashMap<>();
    variables3.put("myVar", "test123");
    ProcessInstance procInst3 = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables3);

    Task task1 = taskService.createTaskQuery().processInstanceId(procInst1.getId()).singleResult();
    Task task2 = taskService.createTaskQuery().processInstanceId(procInst2.getId()).singleResult();

    taskService.setVariableLocal(task1.getId(), "taskVariable", "aCustomValue");
    taskService.setVariableLocal(task2.getId(), "anotherTaskVariable", "aCustomValue");

    // when
    VariableInstanceQuery processVariablesQuery = runtimeService.createVariableInstanceQuery().activityInstanceIdIn(procInst1.getId(), procInst2.getId(), procInst3.getId());

    VariableInstanceQuery taskVariablesQuery =
            runtimeService.createVariableInstanceQuery()
                          .activityInstanceIdIn(
                                  runtimeService.getActivityInstance(procInst1.getId()).getChildActivityInstances()[0].getId(),
                                  runtimeService.getActivityInstance(procInst2.getId()).getChildActivityInstances()[0].getId());

    // then (process variables)
    List<VariableInstance> result = processVariablesQuery.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(4);

    assertThat(processVariablesQuery.count()).isEqualTo(4);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getTypeName()).isEqualTo("string");
      if ("myVar".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("myVar");
        assertThat(variableInstance.getValue()).isEqualTo("test123");
      } else if ("stringVar".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("stringVar");
        assertThat(variableInstance.getValue()).isEqualTo("test");
      } else {
        fail("An unexpected variable '%s' was found with value %s".formatted(variableInstance.getName(), variableInstance.getValue()));
      }
    }

    // then (task variables)
    result = taskVariablesQuery.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    assertThat(taskVariablesQuery.count()).isEqualTo(2);

    for (VariableInstance variableInstance : result) {
      assertThat(variableInstance.getTypeName()).isEqualTo("string");
      if ("taskVariable".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("taskVariable");
        assertThat(variableInstance.getValue()).isEqualTo("aCustomValue");
      } else if ("anotherTaskVariable".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("anotherTaskVariable");
        assertThat(variableInstance.getValue()).isEqualTo("aCustomValue");
      } else {
        fail("An unexpected variable '%s' was found with value %s".formatted(variableInstance.getName(), variableInstance.getValue()));
      }
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryOrderByName_Asc() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    variables.put("myVar", "test123");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().orderByVariableName().asc();

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    VariableInstance first = result.get(0);
    VariableInstance second = result.get(1);

    assertThat(first.getName()).isEqualTo("myVar");
    assertThat(second.getName()).isEqualTo("stringVar");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryOrderByName_Desc() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    variables.put("myVar", "test123");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().orderByVariableName().desc();

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    VariableInstance first = result.get(0);
    VariableInstance second = result.get(1);

    assertThat(first.getName()).isEqualTo("stringVar");
    assertThat(first.getTypeName()).isEqualTo("string");
    assertThat(second.getName()).isEqualTo("myVar");
    assertThat(second.getTypeName()).isEqualTo("string");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryOrderByType_Asc() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("intVar", 123);
    variables.put("myVar", "test123");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().orderByVariableType().asc();

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    VariableInstance first = result.get(0);
    VariableInstance second = result.get(1);

    assertThat(first.getName()).isEqualTo("intVar"); // integer
    assertThat(first.getTypeName()).isEqualTo("integer");
    assertThat(second.getName()).isEqualTo("myVar"); // string
    assertThat(second.getTypeName()).isEqualTo("string");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryOrderByType_Desc() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("intVar", 123);
    variables.put("myVar", "test123");
    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().orderByVariableType().desc();

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    VariableInstance first = result.get(0);
    VariableInstance second = result.get(1);

    assertThat(first.getName()).isEqualTo("myVar"); // string
    assertThat(first.getTypeName()).isEqualTo("string");
    assertThat(second.getName()).isEqualTo("intVar"); // integer
    assertThat(second.getTypeName()).isEqualTo("integer");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryOrderByActivityInstanceId_Asc() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("intVar", 123);
    ProcessInstance procInst1 = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);
    String activityId1 = runtimeService.getActivityInstance(procInst1.getId()).getChildActivityInstances()[0].getId();

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("stringVar", "test");
    ProcessInstance procInst2 = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);
    String activityId2 = runtimeService.getActivityInstance(procInst2.getId()).getChildActivityInstances()[0].getId();

    int comparisonResult = activityId1.compareTo(activityId2);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().orderByActivityInstanceId().asc();

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    VariableInstance first = result.get(0);
    VariableInstance second = result.get(1);

    if (comparisonResult < 0) {
      assertThat(first.getName()).isEqualTo("intVar");
      assertThat(first.getTypeName()).isEqualTo("integer");
      assertThat(second.getName()).isEqualTo("stringVar");
      assertThat(second.getTypeName()).isEqualTo("string");
    } else if (comparisonResult > 0) {
      assertThat(first.getName()).isEqualTo("stringVar");
      assertThat(first.getTypeName()).isEqualTo("string");
      assertThat(second.getName()).isEqualTo("intVar");
      assertThat(second.getTypeName()).isEqualTo("integer");
    } else {
      fail("Something went wrong: both activity instances have the same id %s and %s".formatted(activityId1, activityId2));
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testQueryOrderByActivityInstanceId_Desc() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("intVar", 123);
    ProcessInstance procInst1 = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables1);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("stringVar", "test");
    ProcessInstance procInst2 = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables2);

    String activityId1 = runtimeService.getActivityInstance(procInst1.getId()).getChildActivityInstances()[0].getId();
    String activityId2 = runtimeService.getActivityInstance(procInst2.getId()).getChildActivityInstances()[0].getId();

    int comparisonResult = activityId1.compareTo(activityId2);
    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().orderByActivityInstanceId().desc();

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    VariableInstance first = result.get(0);
    VariableInstance second = result.get(1);

    if (comparisonResult < 0) {
      assertThat(first.getName()).isEqualTo("stringVar");
      assertThat(first.getTypeName()).isEqualTo("string");
      assertThat(second.getName()).isEqualTo("intVar");
      assertThat(second.getTypeName()).isEqualTo("integer");
    } else if (comparisonResult > 0) {
      assertThat(first.getName()).isEqualTo("intVar");
      assertThat(first.getTypeName()).isEqualTo("integer");
      assertThat(second.getName()).isEqualTo("stringVar");
      assertThat(second.getTypeName()).isEqualTo("string");
    } else {
      fail("Something went wrong: both activity instances have the same id %s and %s".formatted(activityId1, activityId2));
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testGetValueOfSerializableVar() {
    // given
    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    Map<String, Object> variables = new HashMap<>();
    variables.put("serializableVar", serializable);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().processInstanceIdIn(processInstance.getId());

    // then
    List<VariableInstance> result = query.list();
    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    VariableInstance instance = result.get(0);

    assertThat(instance.getName()).isEqualTo("serializableVar");
    assertThat(instance.getValue()).isNotNull();
    assertThat(instance.getValue()).isEqualTo(serializable);
    assertThat(instance.getTypeName()).isEqualTo(ValueType.OBJECT.getName());

  }


  @Test
  @Deployment
  void testSubProcessVariablesWithParallelGateway() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processWithSubProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).isNotNull();
    ActivityInstance[] subprocessInstances = tree.getActivityInstances("SubProcess_1");
    assertThat(subprocessInstances).hasSize(5);

    //when
    String activityInstanceId1 = subprocessInstances[0].getId();
    VariableInstanceQuery query1 = runtimeService.createVariableInstanceQuery().activityInstanceIdIn(activityInstanceId1);

    String activityInstanceId2 = subprocessInstances[1].getId();
    VariableInstanceQuery query2 = runtimeService.createVariableInstanceQuery().activityInstanceIdIn(activityInstanceId2);

    String activityInstanceId3 = subprocessInstances[2].getId();
    VariableInstanceQuery query3 = runtimeService.createVariableInstanceQuery().activityInstanceIdIn(activityInstanceId3);

    String activityInstanceId4 = subprocessInstances[3].getId();
    VariableInstanceQuery query4 = runtimeService.createVariableInstanceQuery().activityInstanceIdIn(activityInstanceId4);

    String activityInstanceId5 = subprocessInstances[4].getId();
    VariableInstanceQuery query5 = runtimeService.createVariableInstanceQuery().activityInstanceIdIn(activityInstanceId5);

    // then
    checkVariables(query1.list());
    checkVariables(query2.list());
    checkVariables(query3.list());
    checkVariables(query4.list());
    checkVariables(query5.list());
  }

  private void checkVariables(List<VariableInstance> variableInstances) {
    assertThat(variableInstances).isNotEmpty();
    for (VariableInstance instance : variableInstances) {
      if ("nrOfInstances".equals(instance.getName())) {
        assertThat(instance.getName()).isEqualTo("nrOfInstances");
        assertThat(instance.getTypeName()).isEqualTo("integer");
      } else if ("nrOfCompletedInstances".equals(instance.getName())) {
        assertThat(instance.getName()).isEqualTo("nrOfCompletedInstances");
        assertThat(instance.getTypeName()).isEqualTo("integer");
      } else if ("nrOfActiveInstances".equals(instance.getName())) {
        assertThat(instance.getName()).isEqualTo("nrOfActiveInstances");
        assertThat(instance.getTypeName()).isEqualTo("integer");
      } else if ("loopCounter".equals(instance.getName())) {
        assertThat(instance.getName()).isEqualTo("loopCounter");
        assertThat(instance.getTypeName()).isEqualTo("integer");
      } else if ("nullVar".equals(instance.getName())) {
        assertThat(instance.getName()).isEqualTo("nullVar");
        assertThat(instance.getTypeName()).isEqualTo("null");
      } else if ("integerVar".equals(instance.getName())) {
        assertThat(instance.getName()).isEqualTo("integerVar");
        assertThat(instance.getTypeName()).isEqualTo("integer");
      } else if ("dateVar".equals(instance.getName())) {
        assertThat(instance.getName()).isEqualTo("dateVar");
        assertThat(instance.getTypeName()).isEqualTo("date");
      } else if ("stringVar".equals(instance.getName())) {
        assertThat(instance.getName()).isEqualTo("stringVar");
        assertThat(instance.getTypeName()).isEqualTo("string");
      } else if ("shortVar".equals(instance.getName())) {
        assertThat(instance.getName()).isEqualTo("shortVar");
        assertThat(instance.getTypeName()).isEqualTo("short");
      } else if ("longVar".equals(instance.getName())) {
        assertThat(instance.getName()).isEqualTo("longVar");
        assertThat(instance.getTypeName()).isEqualTo("long");
      } else if ("byteVar".equals(instance.getName())) {
        assertThat(instance.getTypeName()).isEqualTo("bytes");
      } else if ("serializableVar".equals(instance.getName())) {
        assertThat(instance.getName()).isEqualTo("serializableVar");
        assertThatCode(instance::getValue).doesNotThrowAnyException();
      } else {
        fail("An unexpected variable '%s' was found with value %s".formatted(instance.getName(), instance.getValue()));
      }
    }
  }

  @Test
  @Deployment
  void testSubProcessVariables() {
    // given
    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("processVariable", "aProcessVariable");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processWithSubProcess", processVariables);

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).isNotNull();
    assertThat(tree.getChildActivityInstances()).hasSize(1);

    // when
    VariableInstanceQuery query1 = runtimeService.createVariableInstanceQuery().activityInstanceIdIn(tree.getId());

    // then
    VariableInstance processVariable = query1.singleResult();
    assertThat(processVariable).isNotNull();
    assertThat(processVariable.getName()).isEqualTo("processVariable");
    assertThat(processVariable.getValue()).isEqualTo("aProcessVariable");

    // when
    ActivityInstance subProcessActivityInstance = tree.getActivityInstances("SubProcess_1")[0];
    VariableInstanceQuery query2 = runtimeService.createVariableInstanceQuery().activityInstanceIdIn(subProcessActivityInstance.getId());

    // then
    checkVariables(query2.list());

    // when setting a task local variable
    Task task = taskService.createTaskQuery().singleResult();
    taskService.setVariableLocal(task.getId(), "taskVariable", "taskVariableValue");

    // skip mi body instance
    ActivityInstance taskActivityInstance = subProcessActivityInstance.getChildActivityInstances()[0];
    VariableInstanceQuery query3 = runtimeService.createVariableInstanceQuery().activityInstanceIdIn(taskActivityInstance.getId());

    // then
    VariableInstance taskVariable = query3.singleResult();
    assertThat(taskVariable).isNotNull();
    assertThat(taskVariable.getName()).isEqualTo("taskVariable");
    assertThat(taskVariable.getValue()).isEqualTo("taskVariableValue");
  }

  @Test
  @Deployment
  void testParallelGatewayVariables() {
    // given
    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("processVariable", "aProcessVariable");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGatewayProcess", processVariables);

    Execution execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();
    runtimeService.setVariableLocal(execution.getId(), "aLocalVariable", "aLocalValue");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree.getChildActivityInstances()).hasSize(2);
    ActivityInstance task1Instance = tree.getActivityInstances("task1")[0];

    VariableInstanceQuery query = runtimeService
        .createVariableInstanceQuery()
        .variableName("aLocalVariable")
        .activityInstanceIdIn(task1Instance.getId());
    VariableInstance localVariable = query.singleResult();
    assertThat(localVariable).isNotNull();
    assertThat(localVariable.getName()).isEqualTo("aLocalVariable");
    assertThat(localVariable.getValue()).isEqualTo("aLocalValue");

    Task task = taskService.createTaskQuery().executionId(execution.getId()).singleResult();
    taskService.complete(task.getId());

    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree.getChildActivityInstances()).hasSize(2);
    ActivityInstance task3Instance = tree.getActivityInstances("task3")[0];

    query = runtimeService
        .createVariableInstanceQuery()
        .variableName("aLocalVariable")
        .activityInstanceIdIn(task3Instance.getId());
    localVariable = query.singleResult();
    assertThat(localVariable).isNotNull();
    assertThat(localVariable.getName()).isEqualTo("aLocalVariable");
    assertThat(localVariable.getValue()).isEqualTo("aLocalValue");
  }

  @Deployment
  @Test
  void testSimpleSubProcessVariables() {
    // given
    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("processVariable", "aProcessVariable");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processWithSubProcess", processVariables);

    Task task = taskService.createTaskQuery().taskDefinitionKey("UserTask_1").singleResult();
    runtimeService.setVariableLocal(task.getExecutionId(), "aLocalVariable", "aLocalValue");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree.getChildActivityInstances()).hasSize(1);
    ActivityInstance subProcessInstance = tree.getActivityInstances("SubProcess_1")[0];

    // then the local variable has activity instance id of the subprocess
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().activityInstanceIdIn(subProcessInstance.getId());
    VariableInstance localVariable = query.singleResult();
    assertThat(localVariable).isNotNull();
    assertThat(localVariable.getName()).isEqualTo("aLocalVariable");
    assertThat(localVariable.getValue()).isEqualTo("aLocalValue");

    // and the global variable has the activity instance id of the process instance:
    query = runtimeService.createVariableInstanceQuery().activityInstanceIdIn(processInstance.getId());
    VariableInstance globalVariable = query.singleResult();
    assertThat(localVariable).isNotNull();
    assertThat(globalVariable.getName()).isEqualTo("processVariable");
    assertThat(globalVariable.getValue()).isEqualTo("aProcessVariable");

    taskService.complete(task.getId());

  }

  @Test
  void testDisableBinaryFetching() {
    byte[] binaryContent = "some binary content".getBytes();

    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("binaryVariable", binaryContent);
    Task task = taskService.newTask();
    taskService.saveTask(task);
    taskService.setVariablesLocal(task.getId(), variables);

    // when binary fetching is enabled (default)
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then value is fetched
    VariableInstance result = query.singleResult();
    assertThat(result.getValue()).isNotNull();

    // when binary fetching is disabled
    query = runtimeService.createVariableInstanceQuery().disableBinaryFetching();

    // then value is not fetched
    result = query.singleResult();
    assertThat(result.getValue()).isNull();

    // delete task
    taskService.deleteTask(task.getId(), true);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
  void testDisableBinaryFetchingForFileValues() {
    // given
    String fileName = "text.txt";
    String encoding = "crazy-encoding";
    String mimeType = "martini/dry";

    FileValue fileValue = Variables
        .fileValue(fileName)
        .file("ABC".getBytes())
        .encoding(encoding)
        .mimeType(mimeType)
        .create();

    runtimeService.startProcessInstanceByKey(PROC_DEF_KEY,
        Variables.createVariables().putValueTyped("fileVar", fileValue));

    // when enabling binary fetching
    VariableInstance fileVariableInstance =
        runtimeService.createVariableInstanceQuery().singleResult();

    // then the binary value is accessible
    assertThat(fileVariableInstance.getValue()).isNotNull();

    // when disabling binary fetching
    fileVariableInstance =
        runtimeService.createVariableInstanceQuery().disableBinaryFetching().singleResult();

    // then the byte value is not fetched
    assertThat(fileVariableInstance).isNotNull();
    assertThat(fileVariableInstance.getName()).isEqualTo("fileVar");

    assertThat(fileVariableInstance.getValue()).isNull();

    FileValue typedValue = (FileValue) fileVariableInstance.getTypedValue();
    assertThat(typedValue.getValue()).isNull();

    // but typed value metadata is accessible
    assertThat(typedValue.getType()).isEqualTo(ValueType.FILE);
    assertThat(typedValue.getFilename()).isEqualTo(fileName);
    assertThat(typedValue.getEncoding()).isEqualTo(encoding);
    assertThat(typedValue.getMimeType()).isEqualTo(mimeType);

  }

  @Test
  void testDisableCustomObjectDeserialization() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("customSerializable", new CustomSerializable());
    variables.put("failingSerializable", new FailingSerializable());
    Task task = taskService.newTask();
    taskService.saveTask(task);
    taskService.setVariablesLocal(task.getId(), variables);

    // when
    VariableInstanceQuery query =
        runtimeService.createVariableInstanceQuery().disableCustomObjectDeserialization();

    // then
    List<VariableInstance> results = query.list();

    // both variables are not deserialized, but their serialized values are available
    assertThat(results).hasSize(2);

    for (VariableInstance variableInstance : results) {
      assertThat(variableInstance.getErrorMessage()).isNull();

      ObjectValue typedValue = (ObjectValue) variableInstance.getTypedValue();
      assertThat(typedValue).isNotNull();
      assertThat(typedValue.isDeserialized()).isFalse();

      // cannot access the deserialized value
      assertThatThrownBy(typedValue::getValue)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Object is not deserialized");

      assertThat(typedValue.getValueSerialized()).isNotNull();
    }

    // delete task
    taskService.deleteTask(task.getId(), true);
  }

  @Test
  void testSerializableErrorMessage() {

    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("customSerializable", new CustomSerializable());
    variables.put("failingSerializable", new FailingSerializable());
    Task task = taskService.newTask();
    taskService.saveTask(task);
    taskService.setVariablesLocal(task.getId(), variables);

    // when
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    // then
    List<VariableInstance> results = query.list();

    // both variables are fetched
    assertThat(results).hasSize(2);

    for (VariableInstance variableInstance : results) {
      if("customSerializable".equals(variableInstance.getName())) {
        assertThat(variableInstance.getValue())
          .isNotNull()
          .isInstanceOf(CustomSerializable.class);
      }
      if("failingSerializable".equals(variableInstance.getName())) {
        // no value was fetched
        assertThat(variableInstance.getValue()).isNull();
        // error message is present
        assertThat(variableInstance.getErrorMessage()).isNotNull();
      }
    }

    // delete task
    taskService.deleteTask(task.getId(), true);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testQueryByCaseExecutionId() {
    CaseInstance instance = caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .setVariable("aVariableName", "abc")
      .create();

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    query
      .caseExecutionIdIn(instance.getId());

    VariableInstance result = query.singleResult();

    assertThat(result).isNotNull();

    assertThat(result.getName()).isEqualTo("aVariableName");
    assertThat(result.getValue()).isEqualTo("abc");
    assertThat(result.getCaseExecutionId()).isEqualTo(instance.getId());
    assertThat(result.getCaseInstanceId()).isEqualTo(instance.getId());

    assertThat(result.getExecutionId()).isNull();
    assertThat(result.getProcessInstanceId()).isNull();
    assertThat(result.getProcessDefinitionId()).isNull();

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testQueryByCaseExecutionIds() {
    CaseInstance instance1 = caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .setVariable("aVariableName", "abc")
      .create();

    CaseInstance instance2 = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .setVariable("anotherVariableName", "xyz")
        .create();

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    query
      .caseExecutionIdIn(instance1.getId(), instance2.getId())
      .orderByVariableName()
      .asc();

    List<VariableInstance> result = query.list();

    assertThat(result).hasSize(2);

    for (VariableInstance variableInstance : result) {
      if ("aVariableName".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("aVariableName");
        assertThat(variableInstance.getValue()).isEqualTo("abc");
      } else if ("anotherVariableName".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("anotherVariableName");
        assertThat(variableInstance.getValue()).isEqualTo("xyz");
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testQueryByCaseInstanceId() {
    CaseInstance instance = caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .setVariable("aVariableName", "abc")
      .create();

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    query
      .caseInstanceIdIn(instance.getId());

    VariableInstance result = query.singleResult();

    assertThat(result).isNotNull();

    assertThat(result.getName()).isEqualTo("aVariableName");
    assertThat(result.getValue()).isEqualTo("abc");
    assertThat(result.getCaseExecutionId()).isEqualTo(instance.getId());
    assertThat(result.getCaseInstanceId()).isEqualTo(instance.getId());

    assertThat(result.getExecutionId()).isNull();
    assertThat(result.getProcessInstanceId()).isNull();
    assertThat(result.getProcessDefinitionId()).isNull();

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testQueryByCaseInstanceIds() {
    CaseInstance instance1 = caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .setVariable("aVariableName", "abc")
      .create();

    CaseInstance instance2 = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .setVariable("anotherVariableName", "xyz")
        .create();

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    query
      .caseInstanceIdIn(instance1.getId(), instance2.getId())
      .orderByVariableName()
      .asc();

    List<VariableInstance> result = query.list();

    assertThat(result).hasSize(2);

    for (VariableInstance variableInstance : result) {
      if ("aVariableName".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("aVariableName");
        assertThat(variableInstance.getValue()).isEqualTo("abc");
      } else if ("anotherVariableName".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("anotherVariableName");
        assertThat(variableInstance.getValue()).isEqualTo("xyz");
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testQueryByCaseActivityInstanceId() {
    CaseInstance instance = caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .setVariable("aVariableName", "abc")
      .create();

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    query
      .activityInstanceIdIn(instance.getId());

    VariableInstance result = query.singleResult();

    assertThat(result).isNotNull();

    assertThat(result.getName()).isEqualTo("aVariableName");
    assertThat(result.getValue()).isEqualTo("abc");
    assertThat(result.getCaseExecutionId()).isEqualTo(instance.getId());
    assertThat(result.getCaseInstanceId()).isEqualTo(instance.getId());

    assertThat(result.getExecutionId()).isNull();
    assertThat(result.getProcessInstanceId()).isNull();
    assertThat(result.getProcessDefinitionId()).isNull();

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testQueryByCaseActivityInstanceIds() {
    CaseInstance instance1 = caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .setVariable("aVariableName", "abc")
      .create();

    CaseInstance instance2 = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .setVariable("anotherVariableName", "xyz")
        .create();

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    query
      // activityInstanceId == caseExecutionId
      .activityInstanceIdIn(instance1.getId(), instance2.getId())
      .orderByVariableName()
      .asc();

    List<VariableInstance> result = query.list();

    assertThat(result).hasSize(2);

    for (VariableInstance variableInstance : result) {
      if ("aVariableName".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("aVariableName");
        assertThat(variableInstance.getValue()).isEqualTo("abc");
      } else if ("anotherVariableName".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("anotherVariableName");
        assertThat(variableInstance.getValue()).isEqualTo("xyz");
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }
  }

  @Deployment
  @Test
  void testSequentialMultiInstanceSubProcess() {
    // given a process instance in sequential MI
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("miSequentialSubprocess");

    // when
    VariableInstance nrOfInstances = runtimeService.createVariableInstanceQuery()
        .variableName("nrOfInstances").singleResult();
    VariableInstance nrOfActiveInstances = runtimeService.createVariableInstanceQuery()
        .variableName("nrOfActiveInstances").singleResult();
    VariableInstance nrOfCompletedInstances = runtimeService.createVariableInstanceQuery()
        .variableName("nrOfCompletedInstances").singleResult();
    VariableInstance loopCounter = runtimeService.createVariableInstanceQuery()
        .variableName("loopCounter").singleResult();

    // then the activity instance ids of the variable instances should be correct
    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());
    assertThat(nrOfInstances.getActivityInstanceId()).isEqualTo(tree.getActivityInstances("miSubProcess#multiInstanceBody")[0].getId());
    assertThat(nrOfActiveInstances.getActivityInstanceId()).isEqualTo(tree.getActivityInstances("miSubProcess#multiInstanceBody")[0].getId());
    assertThat(nrOfCompletedInstances.getActivityInstanceId()).isEqualTo(tree.getActivityInstances("miSubProcess#multiInstanceBody")[0].getId());
    assertThat(loopCounter.getActivityInstanceId()).isEqualTo(tree.getActivityInstances("miSubProcess#multiInstanceBody")[0].getId());

  }

  @Deployment
  @Test
  void testParallelMultiInstanceSubProcess() {
    // given a process instance in sequential MI
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("miSequentialSubprocess");

    // when
    VariableInstance nrOfInstances = runtimeService.createVariableInstanceQuery()
        .variableName("nrOfInstances").singleResult();
    VariableInstance nrOfActiveInstances = runtimeService.createVariableInstanceQuery()
        .variableName("nrOfActiveInstances").singleResult();
    VariableInstance nrOfCompletedInstances = runtimeService.createVariableInstanceQuery()
        .variableName("nrOfCompletedInstances").singleResult();
    List<VariableInstance> loopCounters = runtimeService.createVariableInstanceQuery()
        .variableName("loopCounter").list();

    // then the activity instance ids of the variable instances should be correct
    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());
    assertThat(nrOfInstances.getActivityInstanceId()).isEqualTo(tree.getActivityInstances("miSubProcess#multiInstanceBody")[0].getId());
    assertThat(nrOfActiveInstances.getActivityInstanceId()).isEqualTo(tree.getActivityInstances("miSubProcess#multiInstanceBody")[0].getId());
    assertThat(nrOfCompletedInstances.getActivityInstanceId()).isEqualTo(tree.getActivityInstances("miSubProcess#multiInstanceBody")[0].getId());

    Set<String> loopCounterActivityInstanceIds = new HashSet<>();
    for (VariableInstance loopCounter : loopCounters) {
      loopCounterActivityInstanceIds.add(loopCounter.getActivityInstanceId());
    }

    assertThat(loopCounterActivityInstanceIds).hasSize(4);

    for (ActivityInstance subProcessInstance : tree.getActivityInstances("miSubProcess")) {
      assertThat(loopCounterActivityInstanceIds).contains(subProcessInstance.getId());
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void testVariablesProcessDefinitionId() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROC_DEF_KEY,
        Variables.createVariables().putValue("foo", "bar"));

    // when
    VariableInstance variable = runtimeService.createVariableInstanceQuery().singleResult();

    // then
    assertThat(variable).isNotNull();
    assertThat(variable.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void shouldGetBatchId() {
    // given
    String processInstanceId =
        runtimeService.startProcessInstanceByKey(PROC_DEF_KEY).getId();

    List<String> processInstances = Collections.singletonList(processInstanceId);

    VariableMap variables = Variables.putValue("foo", "bar");

    Batch batch = runtimeService.setVariablesAsync(processInstances, variables);

    // when
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().singleResult();

    // then
    assertThat(variableInstance.getBatchId()).isEqualTo(batch.getId());

    // clear
    managementService.deleteBatch(batch.getId(), true);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void shouldQueryForBatchId() {
    // given
    VariableMap variables = Variables.putValue("foo", "bar");

    String processInstanceId =
        runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables).getId();

    List<String> processInstances = Collections.singletonList(processInstanceId);

    Batch batch = runtimeService.setVariablesAsync(processInstances, variables);

    VariableInstanceQuery variableInstanceQuery = runtimeService.createVariableInstanceQuery();

    // assume
    assertThat(variableInstanceQuery.list())
        .extracting("name", "value", "batchId")
        .containsExactlyInAnyOrder(
            tuple("foo", "bar", batch.getId()),
            tuple("foo", "bar", null)
        );

    // when
    variableInstanceQuery = variableInstanceQuery.batchIdIn(batch.getId());

    // then
    assertThat(variableInstanceQuery.list())
        .extracting("name", "value", "batchId")
        .containsExactly(
            tuple("foo", "bar", batch.getId())
        );

    // clear
    managementService.deleteBatch(batch.getId(), true);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  void shouldQueryForBatchIds() {
    // given
    VariableMap variables = Variables.putValue("foo", "bar");

    String processInstanceId =
        runtimeService.startProcessInstanceByKey(PROC_DEF_KEY, variables).getId();

    List<String> processInstances = Collections.singletonList(processInstanceId);

    Batch batchOne = runtimeService.setVariablesAsync(processInstances, variables);
    Batch batchTwo = runtimeService.setVariablesAsync(processInstances, variables);
    Batch batchThree = runtimeService.setVariablesAsync(processInstances, variables);

    VariableInstanceQuery variableInstanceQuery = runtimeService.createVariableInstanceQuery();

    // assume
    assertThat(variableInstanceQuery.list())
        .extracting("name", "value", "batchId")
        .containsExactlyInAnyOrder(
            tuple("foo", "bar", batchOne.getId()),
            tuple("foo", "bar", batchTwo.getId()),
            tuple("foo", "bar", batchThree.getId()),
            tuple("foo", "bar", null)
        );

    // when
    variableInstanceQuery = variableInstanceQuery.batchIdIn(
        batchOne.getId(),
        batchTwo.getId()
    );

    // then
    assertThat(variableInstanceQuery.list())
        .extracting("name", "value", "batchId")
        .containsExactlyInAnyOrder(
            tuple("foo", "bar", batchOne.getId()),
            tuple("foo", "bar", batchTwo.getId())
        );

    // clear
    managementService.deleteBatch(batchOne.getId(), true);
    managementService.deleteBatch(batchTwo.getId(), true);
    managementService.deleteBatch(batchThree.getId(), true);
  }

}
