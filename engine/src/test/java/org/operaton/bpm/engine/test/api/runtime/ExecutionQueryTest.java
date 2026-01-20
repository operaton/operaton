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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ExecutionQuery;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.executionByProcessDefinitionId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.executionByProcessDefinitionKey;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.executionByProcessInstanceId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.hierarchical;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;
import static org.assertj.core.api.Assertions.*;


/**
 * @author Joram Barrez
 * @author Frederik Heremans
 */
class ExecutionQueryTest {

  private static final String CONCURRENT_PROCESS_KEY = "concurrent";
  private static final String SEQUENTIAL_PROCESS_KEY = "oneTaskProcess";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  ProcessEngineConfigurationImpl processEngineConfiguration;
  RepositoryService repositoryService;
  RuntimeService runtimeService;
  ManagementService managementService;

  private List<String> concurrentProcessInstanceIds;
  private List<String> sequentialProcessInstanceIds;

  @BeforeEach
  void setUp() {

    repositoryService.createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
      .addClasspathResource("org/operaton/bpm/engine/test/api/runtime/concurrentExecution.bpmn20.xml")
      .deploy();

    concurrentProcessInstanceIds = new ArrayList<>();
    sequentialProcessInstanceIds = new ArrayList<>();

    for (int i = 0; i < 4; i++) {
      concurrentProcessInstanceIds.add(runtimeService.startProcessInstanceByKey(CONCURRENT_PROCESS_KEY, "BUSINESS-KEY-" + i).getId());
    }
    sequentialProcessInstanceIds.add(runtimeService.startProcessInstanceByKey(SEQUENTIAL_PROCESS_KEY).getId());
  }

  @AfterEach
  void tearDown() {
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

  }

  @Test
  void testQueryByProcessDefinitionKey() {
    // Concurrent process with 3 executions for each process instance
    assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).list()).hasSize(12);
    assertThat(runtimeService.createExecutionQuery().processDefinitionKey(SEQUENTIAL_PROCESS_KEY).list()).hasSize(1);
    assertThat(runtimeService.createExecutionQuery().processDefinitionKey(SEQUENTIAL_PROCESS_KEY).singleResult().getProcessDefinitionKey()).isNotNull();
  }

  @Test
  void testQueryByInvalidProcessDefinitionKey() {
    ExecutionQuery query = runtimeService.createExecutionQuery().processDefinitionKey("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
  }

  @Test
  void testQueryByProcessInstanceId() {
    for (String processInstanceId : concurrentProcessInstanceIds) {
      ExecutionQuery query =  runtimeService.createExecutionQuery().processInstanceId(processInstanceId);
      assertThat(query.list()).hasSize(3);
      assertThat(query.count()).isEqualTo(3);
    }
    assertThat(runtimeService.createExecutionQuery().processInstanceId(sequentialProcessInstanceIds.get(0)).list()).hasSize(1);
  }

  @Test
  void testQueryByInvalidProcessInstanceId() {
    ExecutionQuery query = runtimeService.createExecutionQuery().processInstanceId("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
  }

  @Test
  void testQueryExecutionId() {
    Execution execution = runtimeService.createExecutionQuery().processDefinitionKey(SEQUENTIAL_PROCESS_KEY).singleResult();
    assertThat(runtimeService.createExecutionQuery().executionId(execution.getId())).isNotNull();
  }

  @Test
  void testQueryByInvalidExecutionId() {
    ExecutionQuery query = runtimeService.createExecutionQuery().executionId("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
  }

  @Test
  void testQueryByActivityId() {
    ExecutionQuery query = runtimeService.createExecutionQuery().activityId("receivePayment");
    assertThat(query.list()).hasSize(4);
    assertThat(query.count()).isEqualTo(4);

    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByInvalidActivityId() {
    ExecutionQuery query = runtimeService.createExecutionQuery().activityId("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
  }

  @Test
  void testQueryPaging() {
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(13);
    assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).listPage(0, 4)).hasSize(4);
    assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).listPage(2, 1)).hasSize(1);
    assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).listPage(1, 10)).hasSize(10);
    assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).listPage(0, 20)).hasSize(12);
  }

  @SuppressWarnings("unchecked")
  @Test
  void testQuerySorting() {

    // 13 executions: 3 for each concurrent, 1 for the sequential
    List<Execution> executions = runtimeService.createExecutionQuery().orderByProcessInstanceId().asc().list();
    assertThat(executions).hasSize(13);
    verifySorting(executions, executionByProcessInstanceId());

    executions = runtimeService.createExecutionQuery().orderByProcessDefinitionId().asc().list();
    assertThat(executions).hasSize(13);
    verifySorting(executions, executionByProcessDefinitionId());

    executions = runtimeService.createExecutionQuery().orderByProcessDefinitionKey().asc().list();
    assertThat(executions).hasSize(13);
    verifySorting(executions, executionByProcessDefinitionKey(processEngine));

    executions = runtimeService.createExecutionQuery().orderByProcessInstanceId().desc().list();
    assertThat(executions).hasSize(13);
    verifySorting(executions, inverted(executionByProcessInstanceId()));

    executions = runtimeService.createExecutionQuery().orderByProcessDefinitionId().desc().list();
    assertThat(executions).hasSize(13);
    verifySorting(executions, inverted(executionByProcessDefinitionId()));

    executions = runtimeService.createExecutionQuery().orderByProcessDefinitionKey().desc().list();
    assertThat(executions).hasSize(13);
    verifySorting(executions, inverted(executionByProcessDefinitionKey(processEngine)));

    executions = runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).orderByProcessDefinitionId().asc().list();
    assertThat(executions).hasSize(12);
    verifySorting(executions, executionByProcessDefinitionId());

    executions = runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).orderByProcessDefinitionId().desc().list();
    assertThat(executions).hasSize(12);
    verifySorting(executions, executionByProcessDefinitionId());

    executions = runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).orderByProcessDefinitionKey().asc()
        .orderByProcessInstanceId().desc().list();
    assertThat(executions).hasSize(12);
    verifySorting(executions, hierarchical(executionByProcessDefinitionKey(processEngine), inverted(executionByProcessInstanceId())));
  }

  @Test
  void testQueryInvalidSorting() {
    var executionQuery = runtimeService.createExecutionQuery().orderByProcessDefinitionKey();
    assertThatThrownBy(executionQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByBusinessKey() {
    assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).processInstanceBusinessKey("BUSINESS-KEY-1").list()).hasSize(3);
    assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).processInstanceBusinessKey("BUSINESS-KEY-2").list()).hasSize(3);
    assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).processInstanceBusinessKey("NON-EXISTING").list()).isEmpty();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryStringVariable() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    vars.put("stringVar2", "ghijkl");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "azerty");
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // Test EQUAL on single string variable, should result in 2 matches
    ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("stringVar", "abcdef");
    List<Execution> executions = query.list();
    assertThat(executions)
            .isNotNull()
            .hasSize(2);

    // Test EQUAL on two string variables, should result in single match
    query = runtimeService.createExecutionQuery().variableValueEquals("stringVar", "abcdef").variableValueEquals("stringVar2", "ghijkl");
    Execution execution = query.singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance2.getId());

    // Test NOT_EQUAL, should return only 1 execution
    execution = runtimeService.createExecutionQuery().variableValueNotEquals("stringVar", "abcdef").singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    // Test GREATER_THAN, should return only matching 'azerty'
    execution = runtimeService.createExecutionQuery().variableValueGreaterThan("stringVar", "abcdef").singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    execution = runtimeService.createExecutionQuery().variableValueGreaterThan("stringVar", "z").singleResult();
    assertThat(execution).isNull();

    // Test GREATER_THAN_OR_EQUAL, should return 3 results
    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("stringVar", "abcdef").count()).isEqualTo(3);
    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("stringVar", "z").count()).isZero();

    // Test LESS_THAN, should return 2 results
    executions = runtimeService.createExecutionQuery().variableValueLessThan("stringVar", "abcdeg").list();
    assertThat(executions).hasSize(2);
    List<String> expectedIds = List.of(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<>(List.of(executions.get(0).getId(), executions.get(1).getId()));
    ids.removeAll(expectedIds);
    assertThat(ids).isEmpty();

    assertThat(runtimeService.createExecutionQuery().variableValueLessThan("stringVar", "abcdef").count()).isZero();
    assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("stringVar", "z").count()).isEqualTo(3);

    // Test LESS_THAN_OR_EQUAL
    executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("stringVar", "abcdef").list();
    assertThat(executions).hasSize(2);
    expectedIds = List.of(processInstance1.getId(), processInstance2.getId());
    ids = new ArrayList<>(List.of(executions.get(0).getId(), executions.get(1).getId()));
    ids.removeAll(expectedIds);
    assertThat(ids).isEmpty();

    assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("stringVar", "z").count()).isEqualTo(3);
    assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("stringVar", "aa").count()).isZero();

    // Test LIKE
    execution = runtimeService.createExecutionQuery().variableValueLike("stringVar", "azert%").singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    execution = runtimeService.createExecutionQuery().variableValueLike("stringVar", "%y").singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    execution = runtimeService.createExecutionQuery().variableValueLike("stringVar", "%zer%").singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createExecutionQuery().variableValueLike("stringVar", "a%").count()).isEqualTo(3);
    assertThat(runtimeService.createExecutionQuery().variableValueLike("stringVar", "%x%").count()).isZero();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryLongVariable() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("longVar", 12345L);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("longVar", 12345L);
    vars.put("longVar2", 67890L);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("longVar", 55555L);
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // Query on single long variable, should result in 2 matches
    ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("longVar", 12345L);
    List<Execution> executions = query.list();
    assertThat(executions)
            .isNotNull()
            .hasSize(2);

    // Query on two long variables, should result in single match
    query = runtimeService.createExecutionQuery().variableValueEquals("longVar", 12345L).variableValueEquals("longVar2", 67890L);
    Execution execution = query.singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance2.getId());

    // Query with unexisting variable value
    execution = runtimeService.createExecutionQuery().variableValueEquals("longVar", 999L).singleResult();
    assertThat(execution).isNull();

    // Test NOT_EQUALS
    execution = runtimeService.createExecutionQuery().variableValueNotEquals("longVar", 12345L).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    // Test GREATER_THAN
    execution = runtimeService.createExecutionQuery().variableValueGreaterThan("longVar", 44444L).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("longVar", 55555L).count()).isZero();
    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("longVar", 1L).count()).isEqualTo(3);

    // Test GREATER_THAN_OR_EQUAL
    execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("longVar", 44444L).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("longVar", 55555L).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("longVar", 1L).count()).isEqualTo(3);

    // Test LESS_THAN
    executions = runtimeService.createExecutionQuery().variableValueLessThan("longVar", 55555L).list();
    assertThat(executions).hasSize(2);

    List<String> expectedIds = List.of(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<>(List.of(executions.get(0).getId(), executions.get(1).getId()));
    ids.removeAll(expectedIds);
    assertThat(ids).isEmpty();

    assertThat(runtimeService.createExecutionQuery().variableValueLessThan("longVar", 12345L).count()).isZero();
    assertThat(runtimeService.createExecutionQuery().variableValueLessThan("longVar", 66666L).count()).isEqualTo(3);

    // Test LESS_THAN_OR_EQUAL
    executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("longVar", 55555L).list();
    assertThat(executions).hasSize(3);

    assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("longVar", 12344L).count()).isZero();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryDoubleVariable() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("doubleVar", 12345.6789);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("doubleVar", 12345.6789);
    vars.put("doubleVar2", 9876.54321);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("doubleVar", 55555.5555);
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // Query on single double variable, should result in 2 matches
    ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("doubleVar", 12345.6789);
    List<Execution> executions = query.list();
    assertThat(executions)
            .isNotNull()
            .hasSize(2);

    // Query on two double variables, should result in single value
    query = runtimeService.createExecutionQuery().variableValueEquals("doubleVar", 12345.6789).variableValueEquals("doubleVar2", 9876.54321);
    Execution execution = query.singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance2.getId());

    // Query with unexisting variable value
    execution = runtimeService.createExecutionQuery().variableValueEquals("doubleVar", 9999.99).singleResult();
    assertThat(execution).isNull();

    // Test NOT_EQUALS
    execution = runtimeService.createExecutionQuery().variableValueNotEquals("doubleVar", 12345.6789).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    // Test GREATER_THAN
    execution = runtimeService.createExecutionQuery().variableValueGreaterThan("doubleVar", 44444.4444).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("doubleVar", 55555.5555).count()).isZero();
    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("doubleVar", 1.234).count()).isEqualTo(3);

    // Test GREATER_THAN_OR_EQUAL
    execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("doubleVar", 44444.4444).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("doubleVar", 55555.5555).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("doubleVar", 1.234).count()).isEqualTo(3);

    // Test LESS_THAN
    executions = runtimeService.createExecutionQuery().variableValueLessThan("doubleVar", 55555.5555).list();
    assertThat(executions).hasSize(2);

    List<String> expectedIds = List.of(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<>(List.of(executions.get(0).getId(), executions.get(1).getId()));
    ids.removeAll(expectedIds);
    assertThat(ids).isEmpty();

    assertThat(runtimeService.createExecutionQuery().variableValueLessThan("doubleVar", 12345.6789).count()).isZero();
    assertThat(runtimeService.createExecutionQuery().variableValueLessThan("doubleVar", 66666.6666).count()).isEqualTo(3);

    // Test LESS_THAN_OR_EQUAL
    executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("doubleVar", 55555.5555).list();
    assertThat(executions).hasSize(3);

    assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("doubleVar", 12344.6789).count()).isZero();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryIntegerVariable() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("integerVar", 12345);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("integerVar", 12345);
    vars.put("integerVar2", 67890);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("integerVar", 55555);
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // Query on single integer variable, should result in 2 matches
    ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("integerVar", 12345);
    List<Execution> executions = query.list();
    assertThat(executions)
            .isNotNull()
            .hasSize(2);

    // Query on two integer variables, should result in single value
    query = runtimeService.createExecutionQuery().variableValueEquals("integerVar", 12345).variableValueEquals("integerVar2", 67890);
    Execution execution = query.singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance2.getId());

    // Query with unexisting variable value
    execution = runtimeService.createExecutionQuery().variableValueEquals("integerVar", 9999).singleResult();
    assertThat(execution).isNull();

    // Test NOT_EQUALS
    execution = runtimeService.createExecutionQuery().variableValueNotEquals("integerVar", 12345).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    // Test GREATER_THAN
    execution = runtimeService.createExecutionQuery().variableValueGreaterThan("integerVar", 44444).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("integerVar", 55555).count()).isZero();
    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("integerVar", 1).count()).isEqualTo(3);

    // Test GREATER_THAN_OR_EQUAL
    execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("integerVar", 44444).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("integerVar", 55555).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("integerVar", 1).count()).isEqualTo(3);

    // Test LESS_THAN
    executions = runtimeService.createExecutionQuery().variableValueLessThan("integerVar", 55555).list();
    assertThat(executions).hasSize(2);

    List<String> expectedIds = List.of(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<>(List.of(executions.get(0).getId(), executions.get(1).getId()));
    ids.removeAll(expectedIds);
    assertThat(ids).isEmpty();

    assertThat(runtimeService.createExecutionQuery().variableValueLessThan("integerVar", 12345).count()).isZero();
    assertThat(runtimeService.createExecutionQuery().variableValueLessThan("integerVar", 66666).count()).isEqualTo(3);

    // Test LESS_THAN_OR_EQUAL
    executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("integerVar", 55555).list();
    assertThat(executions).hasSize(3);

    assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("integerVar", 12344).count()).isZero();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryShortVariable() {
    Map<String, Object> vars = new HashMap<>();
    short shortVar = 1234;
    vars.put("shortVar", shortVar);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    short shortVar2 = 6789;
    vars = new HashMap<>();
    vars.put("shortVar", shortVar);
    vars.put("shortVar2", shortVar2);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("shortVar", (short)5555);
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // Query on single short variable, should result in 2 matches
    ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("shortVar", shortVar);
    List<Execution> executions = query.list();
    assertThat(executions)
            .isNotNull()
            .hasSize(2);

    // Query on two short variables, should result in single value
    query = runtimeService.createExecutionQuery().variableValueEquals("shortVar", shortVar).variableValueEquals("shortVar2", shortVar2);
    Execution execution = query.singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance2.getId());

    // Query with unexisting variable value
    short unexistingValue = (short)9999;
    execution = runtimeService.createExecutionQuery().variableValueEquals("shortVar", unexistingValue).singleResult();
    assertThat(execution).isNull();

    // Test NOT_EQUALS
    execution = runtimeService.createExecutionQuery().variableValueNotEquals("shortVar", (short)1234).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    // Test GREATER_THAN
    execution = runtimeService.createExecutionQuery().variableValueGreaterThan("shortVar", (short)4444).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("shortVar", (short) 5555).count()).isZero();
    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("shortVar", (short) 1).count()).isEqualTo(3);

    // Test GREATER_THAN_OR_EQUAL
    execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("shortVar", (short)4444).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("shortVar", (short)5555).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("shortVar", (short) 1).count()).isEqualTo(3);

    // Test LESS_THAN
    executions = runtimeService.createExecutionQuery().variableValueLessThan("shortVar", (short)5555).list();
    assertThat(executions).hasSize(2);

    List<String> expectedIds = List.of(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<>(List.of(executions.get(0).getId(), executions.get(1).getId()));
    ids.removeAll(expectedIds);
    assertThat(ids).isEmpty();

    assertThat(runtimeService.createExecutionQuery().variableValueLessThan("shortVar", (short) 1234).count()).isZero();
    assertThat(runtimeService.createExecutionQuery().variableValueLessThan("shortVar", (short) 6666).count()).isEqualTo(3);

    // Test LESS_THAN_OR_EQUAL
    executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("shortVar", (short)5555).list();
    assertThat(executions).hasSize(3);

    assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("shortVar", (short) 1233).count()).isZero();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryDateVariable() throws Exception {
    Map<String, Object> vars = new HashMap<>();
    Date date1 = Calendar.getInstance().getTime();
    vars.put("dateVar", date1);

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    Date date2 = Calendar.getInstance().getTime();
    vars = new HashMap<>();
    vars.put("dateVar", date1);
    vars.put("dateVar2", date2);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    Calendar nextYear = Calendar.getInstance();
    nextYear.add(Calendar.YEAR, 1);
    vars = new HashMap<>();
    vars.put("dateVar",nextYear.getTime());
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    Calendar nextMonth = Calendar.getInstance();
    nextMonth.add(Calendar.MONTH, 1);

    Calendar twoYearsLater = Calendar.getInstance();
    twoYearsLater.add(Calendar.YEAR, 2);

    Calendar oneYearAgo = Calendar.getInstance();
    oneYearAgo.add(Calendar.YEAR, -1);

    // Query on single short variable, should result in 2 matches
    ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("dateVar", date1);
    List<Execution> executions = query.list();
    assertThat(executions)
            .isNotNull()
            .hasSize(2);

    // Query on two short variables, should result in single value
    query = runtimeService.createExecutionQuery().variableValueEquals("dateVar", date1).variableValueEquals("dateVar2", date2);
    Execution execution = query.singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance2.getId());

    // Query with unexisting variable value
    Date unexistingDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/01/1989 12:00:00");
    execution = runtimeService.createExecutionQuery().variableValueEquals("dateVar", unexistingDate).singleResult();
    assertThat(execution).isNull();

    // Test NOT_EQUALS
    execution = runtimeService.createExecutionQuery().variableValueNotEquals("dateVar", date1).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    // Test GREATER_THAN
    execution = runtimeService.createExecutionQuery().variableValueGreaterThan("dateVar", nextMonth.getTime()).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("dateVar", nextYear.getTime()).count()).isZero();
    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("dateVar", oneYearAgo.getTime()).count()).isEqualTo(3);

    // Test GREATER_THAN_OR_EQUAL
    execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("dateVar", nextMonth.getTime()).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("dateVar", nextYear.getTime()).singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getId()).isEqualTo(processInstance3.getId());

    assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("dateVar", oneYearAgo.getTime()).count()).isEqualTo(3);

    // Test LESS_THAN
    executions = runtimeService.createExecutionQuery().variableValueLessThan("dateVar", nextYear.getTime()).list();
    assertThat(executions).hasSize(2);

    List<String> expectedIds = List.of(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<>(List.of(executions.get(0).getId(), executions.get(1).getId()));
    ids.removeAll(expectedIds);
    assertThat(ids).isEmpty();

    assertThat(runtimeService.createExecutionQuery().variableValueLessThan("dateVar", date1).count()).isZero();
    assertThat(runtimeService.createExecutionQuery().variableValueLessThan("dateVar", twoYearsLater.getTime()).count()).isEqualTo(3);

    // Test LESS_THAN_OR_EQUAL
    executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("dateVar", nextYear.getTime()).list();
    assertThat(executions).hasSize(3);

    assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("dateVar", oneYearAgo.getTime()).count()).isZero();

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testBooleanVariable() {

    // TEST EQUALS
    HashMap<String, Object> vars = new HashMap<>();
    vars.put("booleanVar", true);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("booleanVar", false);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().variableValueEquals("booleanVar", true).list();

    assertThat(instances)
            .isNotNull()
            .hasSize(1);
    assertThat(instances.get(0).getId()).isEqualTo(processInstance1.getId());

    instances = runtimeService.createProcessInstanceQuery().variableValueEquals("booleanVar", false).list();

    assertThat(instances)
            .isNotNull()
            .hasSize(1);
    assertThat(instances.get(0).getId()).isEqualTo(processInstance2.getId());

    // TEST NOT_EQUALS
    instances = runtimeService.createProcessInstanceQuery().variableValueNotEquals("booleanVar", true).list();

    assertThat(instances)
            .isNotNull()
            .hasSize(1);
    assertThat(instances.get(0).getId()).isEqualTo(processInstance2.getId());

    instances = runtimeService.createProcessInstanceQuery().variableValueNotEquals("booleanVar", false).list();

    assertThat(instances)
            .isNotNull()
            .hasSize(1);
    assertThat(instances.get(0).getId()).isEqualTo(processInstance1.getId());
    var processInstanceQuery = runtimeService.createProcessInstanceQuery();

    // Test unsupported operations
    try {
      processInstanceQuery.variableValueGreaterThan("booleanVar", true);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("Booleans and null cannot be used in 'greater than' condition", ae.getMessage());
    }

    try {
      processInstanceQuery.variableValueGreaterThanOrEqual("booleanVar", true);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("Booleans and null cannot be used in 'greater than or equal' condition", ae.getMessage());
    }

    try {
      processInstanceQuery.variableValueLessThan("booleanVar", true);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("Booleans and null cannot be used in 'less than' condition", ae.getMessage());
    }

    try {
      processInstanceQuery.variableValueLessThanOrEqual("booleanVar", true);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("Booleans and null cannot be used in 'less than or equal' condition", ae.getMessage());
    }

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryVariablesUpdatedToNullValue() {
    // Start process instance with different types of variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("longVar", 928374L);
    variables.put("shortVar", (short) 123);
    variables.put("integerVar", 1234);
    variables.put("stringVar", "coca-cola");
    variables.put("booleanVar", true);
    variables.put("dateVar", new Date());
    variables.put("nullVar", null);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    ExecutionQuery query = runtimeService.createExecutionQuery()
      .variableValueEquals("longVar", null)
      .variableValueEquals("shortVar", null)
      .variableValueEquals("integerVar", null)
      .variableValueEquals("stringVar", null)
      .variableValueEquals("booleanVar", null)
      .variableValueEquals("dateVar", null);

    ExecutionQuery notQuery = runtimeService.createExecutionQuery()
    .variableValueNotEquals("longVar", null)
    .variableValueNotEquals("shortVar", null)
    .variableValueNotEquals("integerVar", null)
    .variableValueNotEquals("stringVar", null)
    .variableValueNotEquals("booleanVar", null)
    .variableValueNotEquals("dateVar", null);

    assertThat(query.singleResult()).isNull();
    assertThat(notQuery.singleResult()).isNotNull();

    // Set all existing variables values to null
    runtimeService.setVariable(processInstance.getId(), "longVar", null);
    runtimeService.setVariable(processInstance.getId(), "shortVar", null);
    runtimeService.setVariable(processInstance.getId(), "integerVar", null);
    runtimeService.setVariable(processInstance.getId(), "stringVar", null);
    runtimeService.setVariable(processInstance.getId(), "booleanVar", null);
    runtimeService.setVariable(processInstance.getId(), "dateVar", null);
    runtimeService.setVariable(processInstance.getId(), "nullVar", null);

    Execution queryResult = query.singleResult();
    assertThat(queryResult).isNotNull();
    assertThat(queryResult.getId()).isEqualTo(processInstance.getId());
    assertThat(notQuery.singleResult()).isNull();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryNullVariable() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("nullVar", null);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("nullVar", "notnull");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("nullVarLong", "notnull");
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("nullVarDouble", "notnull");
    ProcessInstance processInstance4 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("nullVarByte", "testbytes".getBytes());
    ProcessInstance processInstance5 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // Query on null value, should return one value
    ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("nullVar", null);
    List<Execution> executions = query.list();
    assertThat(executions)
            .isNotNull()
            .hasSize(1);
    assertThat(executions.get(0).getId()).isEqualTo(processInstance1.getId());

    // Test NOT_EQUALS null
    assertThat(runtimeService.createExecutionQuery().variableValueNotEquals("nullVar", null).count()).isOne();
    assertThat(runtimeService.createExecutionQuery().variableValueNotEquals("nullVarLong", null).count()).isOne();
    assertThat(runtimeService.createExecutionQuery().variableValueNotEquals("nullVarDouble", null).count()).isOne();
    // When a byte-array reference is present, the variable is not considered null
    assertThat(runtimeService.createExecutionQuery().variableValueNotEquals("nullVarByte", null).count()).isOne();
    var executionQuery = runtimeService.createExecutionQuery();

    // All other variable queries with null should throw exception
    try {
      executionQuery.variableValueGreaterThan("nullVar", null);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("Booleans and null cannot be used in 'greater than' condition", ae.getMessage());
    }

    try {
      executionQuery.variableValueGreaterThanOrEqual("nullVar", null);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("Booleans and null cannot be used in 'greater than or equal' condition", ae.getMessage());
    }

    try {
      executionQuery.variableValueLessThan("nullVar", null);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("Booleans and null cannot be used in 'less than' condition", ae.getMessage());
    }

    try {
      executionQuery.variableValueLessThanOrEqual("nullVar", null);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("Booleans and null cannot be used in 'less than or equal' condition", ae.getMessage());
    }

    try {
      executionQuery.variableValueLike("nullVar", null);
      fail("Exception expected");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("Booleans and null cannot be used in 'like' condition", ae.getMessage());
    }

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance4.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance5.getId(), "test");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryInvalidTypes() {
    Map<String, Object> vars = new HashMap<>();
    byte[] testBytes = "test".getBytes();
    vars.put("bytesVar", testBytes);
    DummySerializable dummySerializable = new DummySerializable();
    vars.put("serializableVar", dummySerializable);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    ExecutionQuery executionQuery1 = runtimeService.createExecutionQuery().variableValueEquals("bytesVar", "test".getBytes());
    ExecutionQuery executionQuery2 = runtimeService.createExecutionQuery().variableValueEquals("serializableVar", new DummySerializable());

    try {
      executionQuery1.list();
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("Variables of type ByteArray cannot be used to query", ae.getMessage());
    }

    try {
      executionQuery2.list();
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("Object values cannot be used to query", ae.getMessage());
    }

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");
  }

  @Test
  void testQueryVariablesNullNameArgument() {
    var executionQuery = runtimeService.createExecutionQuery();
    try {
      executionQuery.variableValueEquals(null, "value");
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("name is null", ae.getMessage());
    }
    try {
      executionQuery.variableValueNotEquals(null, "value");
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("name is null", ae.getMessage());
    }
    try {
      executionQuery.variableValueGreaterThan(null, "value");
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("name is null", ae.getMessage());
    }
    try {
      executionQuery.variableValueGreaterThanOrEqual(null, "value");
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("name is null", ae.getMessage());
    }
    try {
      executionQuery.variableValueLessThan(null, "value");
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("name is null", ae.getMessage());
    }
    try {
      executionQuery.variableValueLessThanOrEqual(null, "value");
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("name is null", ae.getMessage());
    }
    try {
      executionQuery.variableValueLike(null, "value");
      fail("Expected exception");
    } catch(ProcessEngineException ae) {
      testRule.assertTextPresent("name is null", ae.getMessage());
    }
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryAllVariableTypes() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("nullVar", null);
    vars.put("stringVar", "string");
    vars.put("longVar", 10L);
    vars.put("doubleVar", 1.2);
    vars.put("integerVar", 1234);
    vars.put("booleanVar", true);
    vars.put("shortVar", (short) 123);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    ExecutionQuery query = runtimeService.createExecutionQuery()
      .variableValueEquals("nullVar", null)
      .variableValueEquals("stringVar", "string")
      .variableValueEquals("longVar", 10L)
      .variableValueEquals("doubleVar", 1.2)
      .variableValueEquals("integerVar", 1234)
      .variableValueEquals("booleanVar", true)
      .variableValueEquals("shortVar", (short) 123);

    List<Execution> executions = query.list();
    assertThat(executions)
            .isNotNull()
            .hasSize(1);
    assertThat(executions.get(0).getId()).isEqualTo(processInstance.getId());

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testClashingValues() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", 1234L);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    Map<String, Object> vars2 = new HashMap<>();
    vars2.put("var", 1234);

    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars2);

    List<Execution> executions = runtimeService.createExecutionQuery()
    .processDefinitionKey("oneTaskProcess")
    .variableValueEquals("var", 1234L)
    .list();

    assertThat(executions).hasSize(1);
    assertThat(executions.get(0).getProcessInstanceId()).isEqualTo(processInstance.getId());

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
}

  @Deployment
  @Test
  @SuppressWarnings("deprecation")
  void testQueryBySignalSubscriptionName() {
    runtimeService.startProcessInstanceByKey("catchSignal");

    // it finds subscribed instances
    Execution execution = runtimeService.createExecutionQuery()
      .signalEventSubscription("alert")
      .singleResult();
    assertThat(execution).isNotNull();

    // test query for nonexisting subscription
    execution = runtimeService.createExecutionQuery()
            .signalEventSubscription("nonExisting")
            .singleResult();
    assertThat(execution).isNull();

    // it finds more than one
    runtimeService.startProcessInstanceByKey("catchSignal");
    assertThat(runtimeService.createExecutionQuery().signalEventSubscription("alert").count()).isEqualTo(2);
  }

  @Deployment
  @Test
  @SuppressWarnings("deprecation")
  void testQueryBySignalSubscriptionNameBoundary() {
    runtimeService.startProcessInstanceByKey("signalProces");

    // it finds subscribed instances
    Execution execution = runtimeService.createExecutionQuery()
      .signalEventSubscription("Test signal")
      .singleResult();
    assertThat(execution).isNotNull();

    // test query for non-existing subscription
    execution = runtimeService.createExecutionQuery()
            .signalEventSubscription("nonExisting")
            .singleResult();
    assertThat(execution).isNull();

    // it finds more than one
    runtimeService.startProcessInstanceByKey("signalProces");
    assertThat(runtimeService.createExecutionQuery().signalEventSubscription("Test signal").count()).isEqualTo(2);
  }

  @Test
  void testNativeQuery() {
    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();
    // just test that the query will be constructed and executed, details are tested in the TaskQueryTest
    assertThat(managementService.getTableName(Execution.class)).isEqualTo(tablePrefix + "ACT_RU_EXECUTION");

    long executionCount = runtimeService.createExecutionQuery().count();

    assertThat(runtimeService.createNativeExecutionQuery().sql("SELECT * FROM " + managementService.getTableName(Execution.class)).list()).hasSize((int)executionCount);
    assertThat(runtimeService.createNativeExecutionQuery().sql("SELECT count(*) FROM " + managementService.getTableName(Execution.class)).count()).isEqualTo(executionCount);
  }

  @Test
  void testNativeQueryPaging() {
    assertThat(runtimeService.createNativeExecutionQuery().sql("SELECT * FROM " + managementService.getTableName(Execution.class)).listPage(1, 5)).hasSize(5);
    assertThat(runtimeService.createNativeExecutionQuery().sql("SELECT * FROM " + managementService.getTableName(Execution.class)).listPage(2, 1)).hasSize(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/concurrentExecution.bpmn20.xml"})
  @Test
  void testExecutionQueryWithProcessVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("x", "parent");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("concurrent", variables);

    List<Execution> concurrentExecutions = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).list();
    assertThat(concurrentExecutions).hasSize(3);
    for (Execution execution : concurrentExecutions) {
      if (!((ExecutionEntity)execution).isProcessInstanceExecution()) {
        // only the concurrent executions, not the root one, would be cooler to query that directly, see http://jira.codehaus.org/browse/ACT-1373
        runtimeService.setVariableLocal(execution.getId(), "x", "child");
      }
    }

    assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).variableValueEquals("x", "child").count()).isEqualTo(2);
    assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).variableValueEquals("x", "parent").count()).isOne();

    assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).processVariableValueEquals("x", "parent").count()).isEqualTo(3);
    assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).processVariableValueNotEquals("x", "xxx").count()).isEqualTo(3);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/concurrentExecution.bpmn20.xml"})
  @Test
  void testExecutionQueryForSuspendedExecutions() {
    List<Execution> suspendedExecutions = runtimeService.createExecutionQuery().suspended().list();
    assertThat(suspendedExecutions).isEmpty();

    for (String instanceId : concurrentProcessInstanceIds) {
      runtimeService.suspendProcessInstanceById(instanceId);
    }

    suspendedExecutions = runtimeService.createExecutionQuery().suspended().list();
    assertThat(suspendedExecutions).hasSize(12);

    List<Execution> activeExecutions = runtimeService.createExecutionQuery().active().list();
    assertThat(activeExecutions).hasSize(1);

    for (Execution activeExecution : activeExecutions) {
      assertThat(sequentialProcessInstanceIds.get(0)).isEqualTo(activeExecution.getProcessInstanceId());
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/failingProcessCreateOneIncident.bpmn20.xml"})
  @Test
  void testQueryByIncidentId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    testRule.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    List<Execution> executionList = runtimeService
        .createExecutionQuery()
        .incidentId(incident.getId()).list();

    assertThat(executionList).hasSize(1);
  }

  @Test
  void testQueryByInvalidIncidentId() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    assertThat(query.incidentId("invalid").count()).isZero();

    assertThatThrownBy(() -> query.incidentId(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/failingProcessCreateOneIncident.bpmn20.xml"})
  @Test
  void testQueryByIncidentType() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    testRule.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    List<Execution> executionList = runtimeService
        .createExecutionQuery()
        .incidentType(incident.getIncidentType()).list();

    assertThat(executionList).hasSize(1);
  }

  @Test
  void testQueryByInvalidIncidentType() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    assertThat(query.incidentType("invalid").count()).isZero();

    assertThatThrownBy(() -> query.incidentType(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/failingProcessCreateOneIncident.bpmn20.xml"})
  @Test
  void testQueryByIncidentMessage() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    testRule.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    List<Execution> executionList = runtimeService
        .createExecutionQuery()
        .incidentMessage(incident.getIncidentMessage()).list();

    assertThat(executionList).hasSize(1);
  }

  @Test
  void testQueryByInvalidIncidentMessage() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    assertThat(query.incidentMessage("invalid").count()).isZero();

    assertThatThrownBy(() -> query.incidentMessage(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/failingProcessCreateOneIncident.bpmn20.xml"})
  @Test
  void testQueryByIncidentMessageLike() {
    runtimeService.startProcessInstanceByKey("failingProcess");

    testRule.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    List<Execution> executionList = runtimeService
        .createExecutionQuery()
        .incidentMessageLike("%\\_exception%").list();

    assertThat(executionList).hasSize(1);
  }

  @Test
  void testQueryByInvalidIncidentMessageLike() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    assertThat(query.incidentMessageLike("invalid").count()).isZero();

    assertThatThrownBy(() -> query.incidentMessageLike(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/failingSubProcessCreateOneIncident.bpmn20.xml"})
  @Test
  void testQueryByIncidentIdSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingSubProcess");

    testRule.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    List<Execution> executionList = runtimeService
        .createExecutionQuery()
        .incidentId(incident.getId()).list();

    assertThat(executionList).hasSize(1);
    // execution id of subprocess != process instance id
    assertThat(executionList.get(0).getId()).isNotSameAs(processInstance.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/failingSubProcessCreateOneIncident.bpmn20.xml"})
  @Test
  void testQueryByIncidentTypeInSubprocess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingSubProcess");

    testRule.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    List<Execution> executionList = runtimeService
        .createExecutionQuery()
        .incidentType(incident.getIncidentType()).list();

    assertThat(executionList).hasSize(1);
    // execution id of subprocess != process instance id
    assertThat(executionList.get(0).getId()).isNotSameAs(processInstance.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/failingSubProcessCreateOneIncident.bpmn20.xml"})
  @Test
  void testQueryByIncidentMessageInSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingSubProcess");

    testRule.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    List<Execution> executionList = runtimeService
        .createExecutionQuery()
        .incidentMessage(incident.getIncidentMessage()).list();

    assertThat(executionList).hasSize(1);
    // execution id of subprocess != process instance id
    assertThat(executionList.get(0).getId()).isNotSameAs(processInstance.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/failingSubProcessCreateOneIncident.bpmn20.xml"})
  @Test
  void testQueryByIncidentMessageLikeSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingSubProcess");

    testRule.executeAvailableJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    List<Execution> executionList = runtimeService
        .createExecutionQuery()
        .incidentMessageLike("%exception%").list();

    assertThat(executionList).hasSize(1);
    // execution id of subprocess != process instance id
    assertThat(executionList.get(0).getId()).isNotSameAs(processInstance.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/oneMessageCatchProcess.bpmn20.xml"})
  @Test
  void testQueryForExecutionsWithMessageEventSubscriptions() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneMessageCatchProcess");
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("oneMessageCatchProcess");

    List<Execution> executions = runtimeService.createExecutionQuery()
        .messageEventSubscription().orderByProcessInstanceId().asc().list();

    assertThat(executions).hasSize(2);
    if (instance1.getId().compareTo(instance2.getId()) < 0) {
      assertThat(executions.get(0).getProcessInstanceId()).isEqualTo(instance1.getId());
      assertThat(executions.get(1).getProcessInstanceId()).isEqualTo(instance2.getId());
    } else {
      assertThat(executions.get(0).getProcessInstanceId()).isEqualTo(instance2.getId());
      assertThat(executions.get(1).getProcessInstanceId()).isEqualTo(instance1.getId());
    }

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/oneMessageCatchProcess.bpmn20.xml")
  @Test
  void testQueryForExecutionsWithMessageEventSubscriptionsOverlappingFilters() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneMessageCatchProcess");

    Execution execution = runtimeService
      .createExecutionQuery()
      .messageEventSubscriptionName("newInvoiceMessage")
      .messageEventSubscription()
      .singleResult();

    assertThat(execution).isNotNull();
    assertThat(execution.getProcessInstanceId()).isEqualTo(instance.getId());

    runtimeService
      .createExecutionQuery()
      .messageEventSubscription()
      .messageEventSubscriptionName("newInvoiceMessage")
      .list();

    assertThat(execution).isNotNull();
    assertThat(execution.getProcessInstanceId()).isEqualTo(instance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/twoBoundaryEventSubscriptions.bpmn20.xml")
  @Test
  void testQueryForExecutionsWithMultipleSubscriptions() {
    // given two message event subscriptions
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");

    List<EventSubscription> subscriptions =
        runtimeService.createEventSubscriptionQuery().processInstanceId(instance.getId()).list();
    assertThat(subscriptions).hasSize(2);
    assertThat(subscriptions.get(1).getExecutionId()).isEqualTo(subscriptions.get(0).getExecutionId());

    // should return the execution once (not twice)
    Execution execution = runtimeService
      .createExecutionQuery()
      .messageEventSubscription()
      .singleResult();

    assertThat(execution).isNotNull();
    assertThat(execution.getProcessInstanceId()).isEqualTo(instance.getId());

    // should return the execution once
    execution = runtimeService
      .createExecutionQuery()
      .messageEventSubscriptionName("messageName_1")
      .singleResult();

    assertThat(execution).isNotNull();
    assertThat(execution.getProcessInstanceId()).isEqualTo(instance.getId());

    // should return the execution once
    execution = runtimeService
      .createExecutionQuery()
      .messageEventSubscriptionName("messageName_2")
      .singleResult();

    assertThat(execution).isNotNull();
    assertThat(execution.getProcessInstanceId()).isEqualTo(instance.getId());

    // should return the execution once
    execution = runtimeService
      .createExecutionQuery()
      .messageEventSubscriptionName("messageName_1")
      .messageEventSubscriptionName("messageName_2")
      .singleResult();

    assertThat(execution).isNotNull();
    assertThat(execution.getProcessInstanceId()).isEqualTo(instance.getId());

    // should not return the execution
    execution = runtimeService
      .createExecutionQuery()
      .messageEventSubscriptionName("messageName_1")
      .messageEventSubscriptionName("messageName_2")
      .messageEventSubscriptionName("another")
      .singleResult();

    assertThat(execution).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueEqualsNumber() {
    // long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 123L));

    // non-matching long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 12345L));

    // short
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", (short) 123));

    // double
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 123.0d));

    // integer
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 123));

    // untyped null (should not match)
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", null));

    // typed null (should not match)
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", Variables.longValue(null)));

    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "123"));

    assertThat(runtimeService.createExecutionQuery().processVariableValueEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(runtimeService.createExecutionQuery().processVariableValueEquals("var", Variables.numberValue(123L)).count()).isEqualTo(4);
    assertThat(runtimeService.createExecutionQuery().processVariableValueEquals("var", Variables.numberValue(123.0d)).count()).isEqualTo(4);
    assertThat(runtimeService.createExecutionQuery().processVariableValueEquals("var", Variables.numberValue((short) 123)).count()).isEqualTo(4);

    assertThat(runtimeService.createExecutionQuery().processVariableValueEquals("var", Variables.numberValue(null)).count()).isOne();

    assertThat(runtimeService.createExecutionQuery().variableValueEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(runtimeService.createExecutionQuery().variableValueEquals("var", Variables.numberValue(123L)).count()).isEqualTo(4);
    assertThat(runtimeService.createExecutionQuery().variableValueEquals("var", Variables.numberValue(123.0d)).count()).isEqualTo(4);
    assertThat(runtimeService.createExecutionQuery().variableValueEquals("var", Variables.numberValue((short) 123)).count()).isEqualTo(4);

    assertThat(runtimeService.createExecutionQuery().variableValueEquals("var", Variables.numberValue(null)).count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueNumberComparison() {
    // long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 123L));

    // non-matching long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 12345L));

    // short
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", (short) 123));

    // double
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 123.0d));

    // integer
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 123));

    // untyped null
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", null));

    // typed null
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", Variables.longValue(null)));

    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "123"));

    assertThat(runtimeService.createExecutionQuery().processVariableValueNotEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
  }

  @Test
  void testNullBusinessKeyForChildExecutions() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CONCURRENT_PROCESS_KEY, "76545");
    List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
    for (Execution e : executions) {
      if (((ExecutionEntity) e).isProcessInstanceExecution()) {
        assertThat(((ExecutionEntity) e).getBusinessKeyWithoutCascade()).isEqualTo("76545");
      } else {
        assertThat(((ExecutionEntity) e).getBusinessKeyWithoutCascade()).isNull();
      }
    }
  }

}
