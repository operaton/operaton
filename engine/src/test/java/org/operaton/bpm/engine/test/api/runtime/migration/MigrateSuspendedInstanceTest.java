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
package org.operaton.bpm.engine.test.api.runtime.migration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.SuspendedEntityInteractionException;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.migration.MigrationPlanExecutionBuilder;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MigrateSuspendedInstanceTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(engineRule);

  RuntimeService runtimeService;
  RepositoryService repositoryService;
  ManagementService managementService;

  @Test
  void shouldNotExecuteTimerJobAfterMigrateSuspendedInstance() {
    // given
    // process instance with single user task
    BpmnModelInstance modelInstanceVersion1 = Bpmn.createExecutableProcess("processId")
        .startEvent()
        .userTask("userTask")
        .endEvent()
        .done();
    ProcessDefinition definition1 = testHelper.deployAndGetDefinition(modelInstanceVersion1);
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceById(definition1.getId());
    // suspend instance, jobs belonging to suspended instances should not execute
    runtimeService.suspendProcessInstanceById(processInstance1.getId());

    // version two has a cycle timer
    BpmnModelInstance modelInstanceVersion2 = Bpmn.createExecutableProcess("processId")
        .startEvent()
        .userTask("userTask")
        .endEvent()
        .moveToActivity("userTask")
        .boundaryEvent()
        .cancelActivity(false)
        .timerWithCycle("R3/PT5S")
        .endEvent()
        .done();
    ProcessDefinition definition2 = testHelper.deployAndGetDefinition(modelInstanceVersion2);

    // migrate process instance to version 2
    MigrationPlan migrationPlan = runtimeService
        .createMigrationPlan(definition1.getId(), definition2.getId())
        .mapEqualActivities()
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance1);

    // then
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job.isSuspended()).isTrue();
    List<Incident> incidents = runtimeService.createIncidentQuery().list();
    assertThat(incidents).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldInsertSuspendedScopeWithoutResumingExecution(boolean skipCustomListeners) {
    String sourceChildren = "<startEvent id=\"outerStart\"/>"
        + "<sequenceFlow id=\"outerEnter\" sourceRef=\"outerStart\" targetRef=\"userTask\"/>"
        + "<userTask id=\"userTask\"/>";
    String targetChildren = "<startEvent id=\"outerStart\"/>"
        + "<sequenceFlow id=\"outerEnter\" sourceRef=\"outerStart\" targetRef=\"inner\"/>"
        + "<subProcess id=\"inner\"><extensionElements>"
        + "<operaton:executionListener event=\"start\" "
        + "expression=\"${execution.setVariable('listenerSawSuspended', execution.isSuspended())}\"/>"
        + "<operaton:inputOutput><operaton:inputParameter name=\"wrapperLocal\">retained</operaton:inputParameter>"
        + "</operaton:inputOutput></extensionElements><startEvent id=\"innerStart\"/>"
        + "<sequenceFlow id=\"innerEnter\" sourceRef=\"innerStart\" targetRef=\"userTask\"/>"
        + "<userTask id=\"userTask\"/></subProcess>"
        + "<boundaryEvent id=\"timer\" attachedToRef=\"inner\"><timerEventDefinition>"
        + "<timeDuration>PT1H</timeDuration></timerEventDefinition></boundaryEvent>";
    ProcessDefinition source = testHelper.deployAndGetDefinition(suspendedScopeModel(sourceChildren));
    ProcessDefinition target = testHelper.deployAndGetDefinition(suspendedScopeModel(targetChildren));
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId());
    Task before = engineRule.getTaskService().createTaskQuery().processInstanceId(process.getId()).singleResult();
    runtimeService.setVariableLocal(before.getExecutionId(), "ordinaryPayload", "preserved");
    runtimeService.suspendProcessInstanceById(process.getId());
    List<String> sourceExecutionIds = runtimeService.createExecutionQuery().processInstanceId(process.getId()).list()
        .stream().map(Execution::getId).toList();

    // Normal modification must still reject entering an unstarted scope on a suspended instance.
    assertThatThrownBy(() -> runtimeService.createProcessInstanceModification(process.getId())
        .startBeforeActivity("extra").execute()).isInstanceOf(SuspendedEntityInteractionException.class);
    assertThat(runtimeService.createExecutionQuery().processInstanceId(process.getId()).list())
        .extracting(Execution::getId).containsExactlyInAnyOrderElementsOf(sourceExecutionIds);
    assertThat(runtimeService.getActivityInstance(process.getId()).getActivityInstances("unused")).isEmpty();
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("outer", "outer").mapActivities("userTask", "userTask").build();
    MigrationPlanExecutionBuilder migration = runtimeService.newMigration(plan).processInstanceIds(process.getId());
    if (skipCustomListeners) {
      migration.skipCustomListeners();
    }

    migration.execute();

    ProcessInstance migrated = runtimeService.createProcessInstanceQuery()
        .processInstanceId(process.getId()).singleResult();
    assertThat(migrated.getProcessDefinitionId()).isEqualTo(target.getId());
    assertThat(migrated.isSuspended()).isTrue();
    assertThat(runtimeService.createExecutionQuery().processInstanceId(process.getId()).list())
        .allMatch(Execution::isSuspended);
    Task after = engineRule.getTaskService().createTaskQuery().processInstanceId(process.getId()).singleResult();
    assertThat(after.getId()).isEqualTo(before.getId());
    assertThat(after.isSuspended()).isTrue();
    assertThat(runtimeService.getVariable(after.getExecutionId(), "ordinaryPayload")).isEqualTo("preserved");
    String wrapper = runtimeService.createVariableInstanceQuery().processInstanceIdIn(process.getId())
        .variableName("wrapperLocal").singleResult().getExecutionId();
    assertThat(sourceExecutionIds).doesNotContain(wrapper);
    assertThat(runtimeService.createExecutionQuery().executionId(wrapper).singleResult().isSuspended()).isTrue();
    assertThat(runtimeService.getActivityInstance(process.getId()).getActivityInstances("inner")).hasSize(1);
    if (skipCustomListeners) {
      assertThat(runtimeService.getVariables(process.getId())).doesNotContainKey("listenerSawSuspended");
    } else {
      assertThat(runtimeService.getVariable(process.getId(), "listenerSawSuspended")).isEqualTo(true);
    }
    Job timer = managementService.createJobQuery().processInstanceId(process.getId()).singleResult();
    assertThat(timer.isSuspended()).isTrue();
    assertThat(managementService.createJobQuery().processInstanceId(process.getId()).active().count()).isZero();

    runtimeService.activateProcessInstanceById(process.getId());
    assertThat(managementService.createJobQuery().jobId(timer.getId()).singleResult().isSuspended()).isFalse();
    engineRule.getTaskService().complete(after.getId());
    assertThat(runtimeService.getActivityInstance(process.getId()).getActivityInstances("inner")).isEmpty();
    assertThat(managementService.createJobQuery().processInstanceId(process.getId()).count()).isZero();
    Task afterScope = engineRule.getTaskService().createTaskQuery().processInstanceId(process.getId()).singleResult();
    assertThat(afterScope.getTaskDefinitionKey()).isEqualTo("after");
    engineRule.getTaskService().complete(afterScope.getId());
    testHelper.assertProcessEnded(process.getId());
  }

  @Test
  void shouldRollBackSuspendedMigrationWhenListenerTriesToStartActivity() {
    String sourceChildren = "<startEvent id=\"outerStart\"/>"
        + "<sequenceFlow id=\"outerEnter\" sourceRef=\"outerStart\" targetRef=\"userTask\"/>"
        + "<userTask id=\"userTask\"/>";
    String targetChildren = "<startEvent id=\"outerStart\"/>"
        + "<sequenceFlow id=\"outerEnter\" sourceRef=\"outerStart\" targetRef=\"inner\"/>"
        + "<subProcess id=\"inner\"><extensionElements>"
        + "<operaton:executionListener event=\"start\" expression=\"${execution.getProcessEngineServices()"
        + ".getRuntimeService().createProcessInstanceModification(execution.getProcessInstanceId())"
        + ".startBeforeActivity('extra').execute()}\"/>"
        + "<operaton:inputOutput><operaton:inputParameter name=\"wrapperLocal\">retained</operaton:inputParameter>"
        + "</operaton:inputOutput></extensionElements><startEvent id=\"innerStart\"/>"
        + "<sequenceFlow id=\"innerEnter\" sourceRef=\"innerStart\" targetRef=\"userTask\"/>"
        + "<userTask id=\"userTask\"/></subProcess>"
        + "<boundaryEvent id=\"timer\" attachedToRef=\"inner\"><timerEventDefinition>"
        + "<timeDuration>PT1H</timeDuration></timerEventDefinition></boundaryEvent>";
    ProcessDefinition source = testHelper.deployAndGetDefinition(suspendedScopeModel(sourceChildren));
    ProcessDefinition target = testHelper.deployAndGetDefinition(suspendedScopeModel(targetChildren));
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId());
    Task before = engineRule.getTaskService().createTaskQuery().processInstanceId(process.getId()).singleResult();
    runtimeService.setVariableLocal(before.getExecutionId(), "ordinaryPayload", "preserved");
    runtimeService.suspendProcessInstanceById(process.getId());
    List<String> executionIds = runtimeService.createExecutionQuery().processInstanceId(process.getId()).list()
        .stream().map(Execution::getId).toList();
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("outer", "outer").mapActivities("userTask", "userTask").build();

    assertThatThrownBy(() -> runtimeService.newMigration(plan).processInstanceIds(process.getId()).execute())
        .isInstanceOf(ProcessEngineException.class).hasRootCauseInstanceOf(SuspendedEntityInteractionException.class);

    ProcessInstance unchanged = runtimeService.createProcessInstanceQuery()
        .processInstanceId(process.getId()).singleResult();
    assertThat(unchanged.getProcessDefinitionId()).isEqualTo(source.getId());
    assertThat(unchanged.isSuspended()).isTrue();
    assertThat(engineRule.getTaskService().createTaskQuery().processInstanceId(process.getId()).singleResult().getId())
        .isEqualTo(before.getId());
    assertThat(runtimeService.getVariableLocal(before.getExecutionId(), "ordinaryPayload")).isEqualTo("preserved");
    assertThat(runtimeService.createExecutionQuery().processInstanceId(process.getId()).list())
        .extracting(Execution::getId).containsExactlyInAnyOrderElementsOf(executionIds);
    assertThat(runtimeService.createExecutionQuery().processInstanceId(process.getId()).list())
        .allMatch(Execution::isSuspended);
    assertThat(runtimeService.getActivityInstance(process.getId()).getActivityInstances("inner")).isEmpty();
    assertThat(runtimeService.getActivityInstance(process.getId()).getActivityInstances("unused")).isEmpty();
    assertThat(runtimeService.createVariableInstanceQuery().processInstanceIdIn(process.getId())
        .variableName("wrapperLocal").count()).isZero();
    assertThat(managementService.createJobQuery().processInstanceId(process.getId()).count()).isZero();
    runtimeService.activateProcessInstanceById(process.getId());
    engineRule.getTaskService().complete(before.getId());
    Task afterScope = engineRule.getTaskService().createTaskQuery().processInstanceId(process.getId()).singleResult();
    assertThat(afterScope.getTaskDefinitionKey()).isEqualTo("after");
    engineRule.getTaskService().complete(afterScope.getId());
    testHelper.assertProcessEnded(process.getId());
  }

  private BpmnModelInstance suspendedScopeModel(String children) {
    String xml = "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
        + "xmlns:operaton=\"http://operaton.org/schema/1.0/bpmn\" targetNamespace=\"suspended-scope-migration\">"
        + "<process id=\"process\" isExecutable=\"true\"><startEvent id=\"start\"/>"
        + "<sequenceFlow id=\"enter\" sourceRef=\"start\" targetRef=\"outer\"/>"
        + "<subProcess id=\"outer\">" + children
        + "<subProcess id=\"unused\"><startEvent id=\"unusedStart\"/>"
        + "<sequenceFlow id=\"unusedEnter\" sourceRef=\"unusedStart\" targetRef=\"extra\"/>"
        + "<userTask id=\"extra\"/></subProcess></subProcess>"
        + "<sequenceFlow id=\"leave\" sourceRef=\"outer\" targetRef=\"after\"/><userTask id=\"after\"/>"
        + "</process></definitions>";
    return Bpmn.readModelFromStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }

}
