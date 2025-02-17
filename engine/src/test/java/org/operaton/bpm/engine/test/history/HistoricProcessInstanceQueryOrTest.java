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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
public class HistoricProcessInstanceQueryOrTest {

  @Rule
  public ProcessEngineRule processEngineRule = new ProvidedProcessEngineRule();

  protected HistoryService historyService;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected ManagementService managementService;

  protected List<String> deploymentIds = new ArrayList<>();

  @Before
  public void init() {
    historyService = processEngineRule.getHistoryService();
    runtimeService = processEngineRule.getRuntimeService();
    repositoryService = processEngineRule.getRepositoryService();
    managementService = processEngineRule.getManagementService();
  }

  @After
  public void deleteDeployments() {
    for (String deploymentId : deploymentIds) {
      repositoryService.deleteDeployment(deploymentId, true);
    }
  }

  @Test
  public void shouldThrowExceptionByMissingStartOr() {
    // given
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
      .or()
      .endOr();
    // when/then
    assertThatThrownBy(historicProcessInstanceQuery::endOr)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set endOr() before or()");
  }

  @Test
  public void shouldThrowExceptionByNesting() {
    // given
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
      .or();
    // when/then
    assertThatThrownBy(historicProcessInstanceQuery::or)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set or() within 'or' query");
  }

  @Test
  public void shouldThrowExceptionOnOrderByProcessInstanceId() {
    // given
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
      .or();
    // when/then
    assertThatThrownBy(historicProcessInstanceQuery::orderByProcessInstanceId)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByProcessInstanceId() within 'or' query");
  }

  @Test
  public void shouldThrowExceptionOnOrderByProcessDefinitionId() {
    // given
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
      .or();
    // when/then
    assertThatThrownBy(historicProcessInstanceQuery::orderByProcessDefinitionId
      )
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByProcessDefinitionId() within 'or' query");
  }

  @Test
  public void shouldThrowExceptionOnOrderByProcessDefinitionKey() {
    // given
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
      .or();
    // when/then
    assertThatThrownBy(historicProcessInstanceQuery::orderByProcessDefinitionKey
      )
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByProcessDefinitionKey() within 'or' query");
  }

  @Test
  public void shouldThrowExceptionOnOrderByTenantId() {
    // given
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
      .or();
    // when/then
    assertThatThrownBy(historicProcessInstanceQuery::orderByTenantId
      )
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByTenantId() within 'or' query");
  }

  @Test
  public void shouldThrowExceptionOnOrderByProcessInstanceDuration() {
    // given
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
      .or();
    // when/then
    assertThatThrownBy(historicProcessInstanceQuery::orderByProcessInstanceDuration
      )
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByProcessInstanceDuration() within 'or' query");
  }

  @Test
  public void shouldThrowExceptionOnOrderByProcessInstanceStartTime() {
    // given
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
      .or();
    // when/then
    assertThatThrownBy(historicProcessInstanceQuery::orderByProcessInstanceStartTime
        )
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByProcessInstanceStartTime() within 'or' query");
  }

  @Test
  public void shouldThrowExceptionOnOrderByProcessInstanceEndTime() {
    // given
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
      .or();
    // when/then
    assertThatThrownBy(historicProcessInstanceQuery::orderByProcessInstanceEndTime
        )
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByProcessInstanceEndTime() within 'or' query");
  }

  @Test
  public void shouldThrowExceptionOnOrderByProcessInstanceBusinessKey() {
    // given
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
      .or();
    // when/then
    assertThatThrownBy(historicProcessInstanceQuery::orderByProcessInstanceBusinessKey
        )
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByProcessInstanceBusinessKey() within 'or' query");
  }

  @Test
  public void shouldThrowExceptionOnOrderByProcessDefinitionName() {
    // given
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
      .or();
    // when/then
    assertThatThrownBy(historicProcessInstanceQuery::orderByProcessDefinitionName
        )
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByProcessDefinitionName() within 'or' query");
  }

  @Test
  public void shouldThrowExceptionOnOrderByProcessDefinitionVersion() {
    // given
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
      .or();
    // when/then
    assertThatThrownBy(historicProcessInstanceQuery::orderByProcessDefinitionVersion
        )
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage: cannot set orderByProcessDefinitionVersion() within 'or' query");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldReturnHistoricProcInstWithEmptyOrQuery() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
    runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

    // when
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
      .or()
      .endOr()
      .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldReturnHistoricProcInstWithVarValue1OrVarValue2() {
    // given
    Map<String, Object> vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "ghijkl");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // when
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
      .or()
        .variableValueEquals("stringVar", "abcdef")
        .variableValueEquals("stringVar", "ghijkl")
      .endOr()
      .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_1() {
    // GIVEN
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar")).getProcessInstanceId();

    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("bar", "foo")).getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .or()
          .variableValueEquals("foo", "bar")
          .variableValueEquals("bar", "foo")
        .endOr()
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdOne, processInstanceIdTwo);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_2() {
    // GIVEN
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar").putValue("bar", "foo")).getProcessInstanceId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar").putValue("bar", "foo")).getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .or()
          .variableValueEquals("foo", "bar")
          .variableValueEquals("bar", "foo")
        .endOr()
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdOne, processInstanceIdTwo);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_3() {
    // GIVEN
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar")).getProcessInstanceId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar")).getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .or()
          .variableValueEquals("foo", "bar")
        .endOr()
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdOne, processInstanceIdTwo);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_4() {
    // GIVEN
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar")).getProcessInstanceId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar")).getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .or()
          .variableValueEquals("foo", "bar")
          .variableValueEquals("foo", "bar")
        .endOr()
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdOne, processInstanceIdTwo);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_5() {
    // GIVEN
    String processInstanceIdOne = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar")).getProcessInstanceId();
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar").putValue("bar", 5)).getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .or()
          .variableValueEquals("foo", "bar")
          .variableValueEquals("foo", "baz")
          .variableValueEquals("bar", 5)
        .endOr()
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdOne, processInstanceIdTwo);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldQueryByVariableValue_6() {
    // GIVEN
    runtimeService.startProcessInstanceByKey("oneTaskProcess", Variables.putValue("foo", "bar"));
    String processInstanceIdTwo = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.putValue("foo", "bar").putValue("bar", 5)).getProcessInstanceId();

    // WHEN
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .or()
          .variableValueEquals("foo", "baz")
          .variableValueEquals("bar", 5)
        .endOr()
        .list();

    // THEN
    assertThat(processInstances)
        .extracting("processInstanceId")
        .containsExactlyInAnyOrder(processInstanceIdTwo);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldReturnHistoricProcInstWithMultipleOrCriteria() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Map<String, Object> vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    runtimeService.setVariable(processInstance2.getProcessInstanceId(), "aVarName", "varValue");

    vars = new HashMap<>();
    vars.put("stringVar2", "aaabbbaaa");
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    runtimeService.setVariable(processInstance3.getProcessInstanceId(), "bVarName", "bTestb");

    vars = new HashMap<>();
    vars.put("stringVar2", "cccbbbccc");
    ProcessInstance processInstance4 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    runtimeService.setVariable(processInstance4.getProcessInstanceId(), "bVarName", "aTesta");

    // when
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
      .or()
        .variableValueEquals("stringVar", "abcdef")
        .variableValueLike("stringVar2", "%bbb%")
        .processInstanceId(processInstance1.getId())
      .endOr()
      .list();

    // then
    assertThat(processInstances).hasSize(4);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldReturnHistoricProcInstFilteredByMultipleOrAndCriteria() {
    // given
    Map<String, Object> vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    vars.put("longVar", 12345L);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "ghijkl");
    vars.put("longVar", 56789L);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    vars.put("longVar", 56789L);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "ghijkl");
    vars.put("longVar", 12345L);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // when
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
      .or()
        .variableValueEquals("longVar", 56789L)
        .processInstanceId(processInstance1.getId())
      .endOr()
      .variableValueEquals("stringVar", "abcdef")
      .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldReturnHistoricProcInstFilteredByMultipleOrQueries() {
    // given
    Map<String, Object> vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    vars.put("longVar", 12345L);
    vars.put("boolVar", true);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "ghijkl");
    vars.put("longVar", 56789L);
    vars.put("boolVar", true);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    vars.put("longVar", 56789L);
    vars.put("boolVar", true);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "ghijkl");
    vars.put("longVar", 12345L);
    vars.put("boolVar", true);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "ghijkl");
    vars.put("longVar", 56789L);
    vars.put("boolVar", false);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    vars.put("longVar", 12345L);
    vars.put("boolVar", false);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // when
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
      .or()
        .variableValueEquals("stringVar", "abcdef")
        .variableValueEquals("longVar", 12345L)
      .endOr()
      .or()
        .variableValueEquals("boolVar", true)
        .variableValueEquals("longVar", 12345L)
      .endOr()
      .or()
        .variableValueEquals("stringVar", "ghijkl")
        .variableValueEquals("longVar", 56789L)
      .endOr()
      .or()
        .variableValueEquals("stringVar", "ghijkl")
        .variableValueEquals("boolVar", false)
        .variableValueEquals("longVar", 56789L)
      .endOr()
      .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldReturnHistoricProcInstWhereSameCriterionWasAppliedThreeTimesInOneQuery() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
      .or()
        .processInstanceId(processInstance1.getId())
        .processInstanceId(processInstance2.getId())
        .processInstanceId(processInstance3.getId())
      .endOr()
      .list();

    // then
    assertThat(processInstances).hasSize(1);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldReturnHistoricProcInstWithVariableValueEqualsOrVariableValueGreaterThan() {
    // given
    Map<String, Object> vars = new HashMap<>();
    vars.put("longVar", 12345L);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("longerVar", 56789L);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    vars = new HashMap<>();
    vars.put("longerVar", 56789L);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

    // when
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
      .or()
        .variableValueEquals("longVar", 12345L)
        .variableValueGreaterThan("longerVar", 20000L)
      .endOr();

    // then
    assertThat(query.count()).isEqualTo(3);
  }

  @Test
  public void shouldReturnHistoricProcInstWithProcessDefinitionNameOrProcessDefinitionKey() {
    // given
    BpmnModelInstance aProcessDefinition = Bpmn.createExecutableProcess("aProcessDefinition")
      .name("process1")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId = repositoryService
      .createDeployment()
      .addModelInstance("foo.bpmn", aProcessDefinition)
      .deploy()
      .getId();

    deploymentIds.add(deploymentId);

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("aProcessDefinition");

    BpmnModelInstance anotherProcessDefinition = Bpmn.createExecutableProcess("anotherProcessDefinition")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    deploymentId = repositoryService
       .createDeployment()
       .addModelInstance("foo.bpmn", anotherProcessDefinition)
       .deploy()
       .getId();

    deploymentIds.add(deploymentId);

    runtimeService.startProcessInstanceByKey("anotherProcessDefinition");

    // when
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
      .or()
        .processDefinitionId(processInstance1.getProcessDefinitionId())
        .processDefinitionKey("anotherProcessDefinition")
      .endOr()
      .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

  @Test
  public void shouldReturnHistoricProcInstWithBusinessKeyOrBusinessKeyLike() {
    // given
    BpmnModelInstance aProcessDefinition = Bpmn.createExecutableProcess("aProcessDefinition")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId = repositoryService
      .createDeployment()
      .addModelInstance("foo.bpmn", aProcessDefinition)
      .deploy()
      .getId();

    deploymentIds.add(deploymentId);

    runtimeService.startProcessInstanceByKey("aProcessDefinition", "aBusinessKey");

    BpmnModelInstance anotherProcessDefinition = Bpmn.createExecutableProcess("anotherProcessDefinition")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

     deploymentId = repositoryService
      .createDeployment()
      .addModelInstance("foo.bpmn", anotherProcessDefinition)
      .deploy()
      .getId();

     deploymentIds.add(deploymentId);

    runtimeService.startProcessInstanceByKey("anotherProcessDefinition", "anotherBusinessKey");

    // when
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
      .or()
        .processInstanceBusinessKey("aBusinessKey")
        .processInstanceBusinessKeyLike("anotherBusinessKey")
      .endOr()
      .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

  @Test
  public void shouldReturnHistoricProcInstWithActivityIdInOrProcInstId() {
    // given
    BpmnModelInstance aProcessDefinition = Bpmn.createExecutableProcess("aProcessDefinition")
      .startEvent()
        .userTask()
      .endEvent()
      .done();

    String deploymentId = repositoryService
      .createDeployment()
      .addModelInstance("foo.bpmn", aProcessDefinition)
      .deploy()
      .getId();

    deploymentIds.add(deploymentId);

    ProcessInstance processInstance1 = runtimeService
      .startProcessInstanceByKey("aProcessDefinition");

    String activityInstanceId = runtimeService.getActivityInstance(processInstance1.getId())
      .getChildActivityInstances()[0].getActivityId();

    ProcessInstance processInstance2 = runtimeService
      .startProcessInstanceByKey("aProcessDefinition");

    // when
    List<HistoricProcessInstance> tasks = historyService.createHistoricProcessInstanceQuery()
      .or()
        .activeActivityIdIn(activityInstanceId)
        .processInstanceId(processInstance2.getId())
      .endOr()
      .list();

    // then
    assertThat(tasks).hasSize(2);
  }

  @Test
  public void shouldReturnHistoricProcInstByVariableAndActiveProcesses() {
    // given
    BpmnModelInstance aProcessDefinition = Bpmn.createExecutableProcess("oneTaskProcess")
        .startEvent()
          .userTask("testQuerySuspensionStateTask")
        .endEvent()
        .done();

      String deploymentId = repositoryService
        .createDeployment()
        .addModelInstance("foo.bpmn", aProcessDefinition)
        .deploy()
        .getId();

      deploymentIds.add(deploymentId);

    // start two process instance and leave them active
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // start one process instance and suspend it
    Map<String, Object> variables = new HashMap<>();
    variables.put("foo", 0);
    ProcessInstance suspendedProcessInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);
    runtimeService.suspendProcessInstanceById(suspendedProcessInstance.getProcessInstanceId());

    // assume
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("oneTaskProcess").active().count()).isEqualTo(2);
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("oneTaskProcess").suspended().count()).isEqualTo(1);

    // then
    assertThat(historyService.createHistoricProcessInstanceQuery().or().active().variableValueEquals("foo", 0).endOr().list()).hasSize(3);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldReturnByProcessDefinitionKeyOrActivityId() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    BpmnModelInstance aProcessDefinition = Bpmn.createExecutableProcess("process")
        .startEvent()
          .userTask("aUserTask")
        .endEvent()
        .done();

    String deploymentId = repositoryService
        .createDeployment()
        .addModelInstance("foo.bpmn", aProcessDefinition)
        .deploy()
        .getId();

    deploymentIds.add(deploymentId);

    runtimeService.startProcessInstanceByKey("process");

    // when
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .or()
          .activeActivityIdIn("theTask")
          .processDefinitionKey("process")
        .endOr()
        .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void shouldReturnByProcessDefinitionIdOrIncidentType() {
    // given
    String processDefinitionId = runtimeService.startProcessInstanceByKey("oneTaskProcess")
        .getProcessDefinitionId();

    BpmnModelInstance aProcessDefinition = Bpmn.createExecutableProcess("process")
        .startEvent().operatonAsyncBefore()
          .userTask("aUserTask")
        .endEvent()
        .done();

    String deploymentId = repositoryService
        .createDeployment()
        .addModelInstance("foo.bpmn", aProcessDefinition)
        .deploy()
        .getId();

    deploymentIds.add(deploymentId);

    runtimeService.startProcessInstanceByKey("process");

    String jobId = managementService.createJobQuery().singleResult().getId();

    managementService.setJobRetries(jobId, 0);

    // when
    List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
        .or()
          .incidentType("failedJob")
          .processDefinitionId(processDefinitionId)
        .endOr()
        .list();

    // then
    assertThat(processInstances).hasSize(2);
  }

}
