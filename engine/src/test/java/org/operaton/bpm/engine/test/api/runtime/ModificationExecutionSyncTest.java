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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("java:S1874") // Use of synchronous execute() method is a acceptable in test code
class ModificationExecutionSyncTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(rule);
  BatchModificationHelper helper = new BatchModificationHelper(rule);

  RuntimeService runtimeService;
  HistoryService historyService;
  BpmnModelInstance instance;

  @BeforeEach
  void createBpmnModelInstance() {
    this.instance = Bpmn.createExecutableProcess("process1")
        .startEvent("start")
        .userTask("user1")
        .sequenceFlowId("seq")
        .userTask("user2")
        .userTask("user3")
        .endEvent("end")
        .done();
  }

  @AfterEach
  void removeInstanceIds() {
    helper.currentProcessInstances = new ArrayList<>();
  }

  @Test
  void createSimpleModificationPlan() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    List<String> instances = helper.startInstances("process1", 2);
    runtimeService.createModification(processDefinition.getId()).startBeforeActivity("user2").cancelAllForActivity("user1").processInstanceIds(instances).execute();

    for (String instanceId : instances) {

      List<String> activeActivityIds = runtimeService.getActiveActivityIds(instanceId);
      assertThat(activeActivityIds).hasSize(1);
      assertThat(activeActivityIds.iterator().next()).isEqualTo("user2");
    }
  }

  @Test
  void createSimpleModificationPlanWithHistoricQuery() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    List<String> instances = helper.startInstances("process1", 2);
    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    historicProcessInstanceQuery.processDefinitionId(processDefinition.getId());

    runtimeService.createModification(processDefinition.getId()).startBeforeActivity("user2")
        .cancelAllForActivity("user1").historicProcessInstanceQuery(historicProcessInstanceQuery).execute();

    for (String instanceId : instances) {
      List<String> activeActivityIds = runtimeService.getActiveActivityIds(instanceId);
      assertThat(activeActivityIds).hasSize(1);
      assertThat(activeActivityIds.iterator().next()).isEqualTo("user2");
    }
  }

  @Test
  void createSimpleModificationPlanWithIdenticalRuntimeAndHistoryQuery() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    List<String> instances = helper.startInstances("process1", 2);
    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    historicProcessInstanceQuery.processDefinitionId(processDefinition.getId());
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery();
    processInstanceQuery.processDefinitionId(processDefinition.getId());

    runtimeService.createModification(processDefinition.getId()).startBeforeActivity("user2")
        .cancelAllForActivity("user1").historicProcessInstanceQuery(historicProcessInstanceQuery).processInstanceQuery(processInstanceQuery).execute();

    for (String instanceId : instances) {
      List<String> activeActivityIds = runtimeService.getActiveActivityIds(instanceId);
      assertThat(activeActivityIds).hasSize(1);
      assertThat(activeActivityIds.iterator().next()).isEqualTo("user2");
    }
  }

  @Test
  void createSimpleModificationPlanWithComplementaryRuntimeAndHistoryQueries() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    List<String> instances = helper.startInstances("process1", 2);
    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    historicProcessInstanceQuery.processInstanceId(instances.get(0));
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery();
    processInstanceQuery.processInstanceId(instances.get(1));

    runtimeService.createModification(processDefinition.getId()).startBeforeActivity("user2")
        .cancelAllForActivity("user1").historicProcessInstanceQuery(historicProcessInstanceQuery).processInstanceQuery(processInstanceQuery).execute();

    for (String instanceId : instances) {
      List<String> activeActivityIds = runtimeService.getActiveActivityIds(instanceId);
      assertThat(activeActivityIds).hasSize(1);
      assertThat(activeActivityIds.iterator().next()).isEqualTo("user2");
    }
  }

  @Test
  void createSimpleModificationPlanWithHistoricQueryUnfinished() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    List<String> instances = helper.startInstances("process1", 2);
    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    historicProcessInstanceQuery.processDefinitionId(processDefinition.getId()).unfinished();

    runtimeService.deleteProcessInstance(instances.get(0), "test");

    runtimeService.createModification(processDefinition.getId()).startBeforeActivity("user2")
        .cancelAllForActivity("user1").historicProcessInstanceQuery(historicProcessInstanceQuery).execute();

    List<String> activeActivityIds = runtimeService.getActiveActivityIds(instances.get(1));
    assertThat(activeActivityIds).hasSize(1);
    assertThat(activeActivityIds.iterator().next()).isEqualTo("user2");
  }

  @Test
  void createModificationWithNullProcessInstanceIdsList() {
    var modificationBuilder = runtimeService.createModification("processDefinitionId").startAfterActivity("user1") .processInstanceIds((List<String>) null);

    assertThatThrownBy(modificationBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Process instance ids is empty");
  }

  @Test
  void createModificationUsingProcessInstanceIdsListWithNullValue() {
    var modificationBuilder = runtimeService.createModification("processDefinitionId").startAfterActivity("user1").processInstanceIds(Arrays.asList("foo", null, "bar"));

    assertThatThrownBy(modificationBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Process instance ids contains null value");
  }

  @Test
  void createModificationWithEmptyProcessInstanceIdsList() {
    var modificationBuilder = runtimeService.createModification("processDefinitionId").startAfterActivity("user1").processInstanceIds(Collections.<String> emptyList());

    assertThatThrownBy(modificationBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Process instance ids is empty");
  }

  @Test
  void createModificationWithNullProcessDefinitionId() {
    assertThatThrownBy(() -> runtimeService.createModification(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("processDefinitionId is null");
  }

  @Test
  void createModificationWithNullProcessInstanceIdsArray() {
    var modificationBuilder = runtimeService.createModification("processDefinitionId")
      .startAfterActivity("user1")
      .processInstanceIds((String[]) null);

    assertThatThrownBy(modificationBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Process instance ids is empty");
  }

  @Test
  void createModificationUsingProcessInstanceIdsArrayWithNullValue() {
    var modificationBuilder = runtimeService.createModification("processDefinitionId").cancelAllForActivity("user1").processInstanceIds("foo", null, "bar");

    assertThatThrownBy(modificationBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Process instance ids contains null value");
  }

  @Test
  void testNullProcessInstanceQuery() {
    var modificationBuilder = runtimeService.createModification("processDefinitionId").startAfterActivity("user1").processInstanceQuery(null);

    assertThatThrownBy(modificationBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Process instance ids is empty");
  }

  @Test
  void testNullHistoricProcessInstanceQuery() {
    var modificationBuilder = runtimeService.createModification("processDefinitionId").startAfterActivity("user1").historicProcessInstanceQuery(null);

    assertThatThrownBy(modificationBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Process instance ids is empty");
  }

  @Test
  void createModificationWithNotMatchingProcessDefinitionId() {
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    deployment.getDeployedProcessDefinitions().get(0);

    List<String> processInstanceIds = helper.startInstances("process1", 2);
    var modificationBuilder = runtimeService.createModification("foo").cancelAllForActivity("activityId").processInstanceIds(processInstanceIds);

    assertThatThrownBy(modificationBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("processDefinition is null");
  }

  @Test
  void testStartBefore() {
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition definition = deployment.getDeployedProcessDefinitions().get(0);

    List<String> processInstanceIds = helper.startInstances("process1", 2);

    runtimeService.createModification(definition.getId()).startBeforeActivity("user2").processInstanceIds(processInstanceIds).execute();

    for (String processInstanceId : processInstanceIds) {
      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      Assertions.assertThat(updatedTree).isNotNull();
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

      assertThat(updatedTree).hasStructure(
          describeActivityInstanceTree(
              definition.getId())
          .activity("user1")
          .activity("user2")
          .done());
    }
  }

  @Test
  void testStartAfter() {
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition definition = deployment.getDeployedProcessDefinitions().get(0);

    List<String> processInstanceIds = helper.startInstances("process1", 2);

    runtimeService.createModification(definition.getId()).startAfterActivity("user2").processInstanceIds(processInstanceIds).execute();

    for (String processInstanceId : processInstanceIds) {
      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      Assertions.assertThat(updatedTree).isNotNull();
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

      assertThat(updatedTree).hasStructure(describeActivityInstanceTree(definition.getId()).activity("user1").activity("user3").done());
    }
  }

  @Test
  void testStartTransition() {
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition definition = deployment.getDeployedProcessDefinitions().get(0);

    List<String> processInstanceIds = helper.startInstances("process1", 2);

    runtimeService.createModification(definition.getId()).startTransition("seq").processInstanceIds(processInstanceIds).execute();

    for (String processInstanceId : processInstanceIds) {
      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      Assertions.assertThat(updatedTree).isNotNull();
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

      assertThat(updatedTree).hasStructure(describeActivityInstanceTree(definition.getId()).activity("user1").activity("user2").done());
    }
  }

  @Test
  void testCancelAll() {
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);
    List<String> processInstanceIds = helper.startInstances("process1", 2);

    runtimeService.createModification(processDefinition.getId()).cancelAllForActivity("user1").processInstanceIds(processInstanceIds).execute();

    for (String processInstanceId : processInstanceIds) {
      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      Assertions.assertThat(updatedTree).isNull();
    }
  }

  @Test
  void testStartBeforeAndCancelAll() {
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition definition = deployment.getDeployedProcessDefinitions().get(0);

    List<String> processInstanceIds = helper.startInstances("process1", 2);

    runtimeService.createModification(definition.getId()).cancelAllForActivity("user1").startBeforeActivity("user2").processInstanceIds(processInstanceIds).execute();

    for (String processInstanceId : processInstanceIds) {
      ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
      Assertions.assertThat(updatedTree).isNotNull();
      assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

      assertThat(updatedTree).hasStructure(describeActivityInstanceTree(definition.getId()).activity("user2").done());
    }
  }

  @Test
  void testDifferentStates() {
    DeploymentWithDefinitions deployment = testRule.deploy(instance);
    ProcessDefinition definition = deployment.getDeployedProcessDefinitions().get(0);

    List<String> processInstanceIds = helper.startInstances("process1", 1);
    Task task = rule.getTaskService().createTaskQuery().singleResult();
    rule.getTaskService().complete(task.getId());

    List<String> anotherProcessInstanceIds = helper.startInstances("process1", 1);
    processInstanceIds.addAll(anotherProcessInstanceIds);

    runtimeService.createModification(definition.getId()).startBeforeActivity("user3").processInstanceIds(processInstanceIds).execute();

    ActivityInstance updatedTree = null;
    String processInstanceId = processInstanceIds.get(0);
    updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(definition.getId()).activity("user2").activity("user3").done());

    processInstanceId = processInstanceIds.get(1);
    updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);
    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(definition.getId()).activity("user1").activity("user3").done());
  }

  @Test
  void testCancelWithoutFlag() {
    // given
    this.instance = Bpmn.createExecutableProcess("process1")
        .startEvent("start")
        .serviceTask("ser").operatonExpression("${true}")
        .userTask("user")
        .endEvent("end")
        .done();

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);

    List<String> processInstanceIds = helper.startInstances("process1", 1);

    // when
    runtimeService.createModification(processDefinition.getId())
      .startBeforeActivity("ser")
      .cancelAllForActivity("user")
      .processInstanceIds(processInstanceIds)
      .execute();

    // then
    assertThat(runtimeService.createExecutionQuery().list()).isEmpty();
  }

  @Test
  void testCancelWithoutFlag2() {
    // given
    this.instance = Bpmn.createExecutableProcess("process1")
        .startEvent("start")
        .serviceTask("ser").operatonExpression("${true}")
        .userTask("user")
        .endEvent("end")
        .done();

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);

    List<String> processInstanceIds = helper.startInstances("process1", 1);

    // when
    runtimeService.createModification(processDefinition.getId())
      .startBeforeActivity("ser")
      .cancelAllForActivity("user", false)
      .processInstanceIds(processInstanceIds)
      .execute();

    // then
    assertThat(runtimeService.createExecutionQuery().list()).isEmpty();
  }

  @Test
  void testCancelWithFlag() {
    // given
    this.instance = Bpmn.createExecutableProcess("process1")
        .startEvent("start")
        .serviceTask("ser").operatonExpression("${true}")
        .userTask("user")
        .endEvent("end")
        .done();

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);

    List<String> processInstanceIds = helper.startInstances("process1", 1);

    // when
    runtimeService.createModification(processDefinition.getId())
      .startBeforeActivity("ser")
      .cancelAllForActivity("user", true)
      .processInstanceIds(processInstanceIds)
      .execute();

    // then
    ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.getActivityId()).isEqualTo("user");
  }

  @Test
  void testCancelWithFlagForManyInstances() {
    // given
    this.instance = Bpmn.createExecutableProcess("process1")
        .startEvent("start")
        .serviceTask("ser").operatonExpression("${true}")
        .userTask("user")
        .endEvent("end")
        .done();

    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(instance);

    List<String> processInstanceIds = helper.startInstances("process1", 10);

    // when
    runtimeService.createModification(processDefinition.getId())
      .startBeforeActivity("ser")
      .cancelAllForActivity("user", true)
      .processInstanceIds(processInstanceIds)
      .execute();

    // then
    for (String processInstanceId : processInstanceIds) {
      Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).singleResult();
      assertThat(execution).isNotNull();
      assertThat(((ExecutionEntity) execution).getActivityId()).isEqualTo("user");
    }
  }

}
