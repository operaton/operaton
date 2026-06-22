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
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ExecutionAssert;
import org.operaton.bpm.engine.test.util.ExecutionTree;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Svetlana Dorokhova.
 */
@SuppressWarnings("java:S1874") // Use of synchronous execute() method is a acceptable in test code
class ProcessInstanceModificationSubProcessTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(rule);

  RuntimeService runtimeService;
  RepositoryService repositoryService;
  TaskService taskService;
  HistoryService historyService;

  @Disabled("CAM-9354")
  @Test
  void shouldHaveEqualParentActivityInstanceId() {
    // given
    testHelper.deploy(Bpmn.createExecutableProcess("process")
      .startEvent()
        .subProcess("subprocess").embeddedSubProcess()
          .startEvent()
            .scriptTask("scriptTaskInSubprocess")
              .scriptFormat("groovy")
              .scriptText("throw new org.operaton.bpm.engine.delegate.BpmnError(\"anErrorCode\");")
            .userTask()
          .endEvent()
        .subProcessDone()
      .endEvent()
      .moveToActivity("subprocess")
        .boundaryEvent("boundary").error("anErrorCode")
          .userTask("userTaskAfterBoundaryEvent")
        .endEvent().done());

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // when
    runtimeService.createModification(processInstance.getProcessDefinitionId())
      .startAfterActivity("scriptTaskInSubprocess")
      .processInstanceIds(processInstance.getId())
      .execute();

    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId())
      .getActivityInstances("subprocess")[0];

    HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
      .activityId("subprocess")
      .unfinished()
      .singleResult();

    // assume
    assertThat(activityInstance).isNotNull();
    assertThat(historicActivityInstance).isNotNull();

    // then
    assertThat(activityInstance.getParentActivityInstanceId()).isEqualTo(historicActivityInstance.getParentActivityInstanceId());
  }

  @Test
  void shouldCompleteParentProcess() {
    final BpmnModelInstance parentProcessInstance =
      Bpmn.createExecutableProcess("parentProcess")
        .startEvent()
          .callActivity("callActivity").calledElement("subprocess")
        .endEvent()
        .done();

    final BpmnModelInstance subprocessInstance =
      Bpmn.createExecutableProcess("subprocess")
        .startEvent()
          .userTask("userTask")
        .endEvent("subEnd")
        .done();

    testHelper.deploy(parentProcessInstance, subprocessInstance);

    // given I start the process, which wait as user task in subprocess
    runtimeService.startProcessInstanceByKey("parentProcess");

    assertThat(taskService.createTaskQuery().taskName("userTask").singleResult()).isNotNull();

    final ProcessInstance subprocess = runtimeService.createProcessInstanceQuery().processDefinitionKey("subprocess").singleResult();
    assertThat(subprocess).isNotNull();

    // when I do process instance modification
    runtimeService.createProcessInstanceModification(
      subprocess.getProcessInstanceId())
      .cancelAllForActivity("userTask")
      .startAfterActivity("userTask")
      .execute();

    // then the process should be finished
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Test
  void shouldContinueParentProcess() {
    final BpmnModelInstance parentProcessInstance =
      Bpmn.createExecutableProcess("parentProcess")
        .startEvent()
        .callActivity("callActivity").calledElement("subprocess")
        .userTask()
        .endEvent()
        .done();

    final BpmnModelInstance subprocessInstance =
      Bpmn.createExecutableProcess("subprocess")
        .startEvent()
          .userTask("userTask")
        .endEvent("subEnd")
        .done();

    testHelper.deploy(parentProcessInstance, subprocessInstance);

    // given I start the process, which wait as user task in subprocess
    ProcessInstance parentPI = runtimeService.startProcessInstanceByKey("parentProcess");

    assertThat(taskService.createTaskQuery().taskName("userTask").singleResult()).isNotNull();

    final ProcessInstance subprocess = runtimeService.createProcessInstanceQuery().processDefinitionKey("subprocess").singleResult();
    assertThat(subprocess).isNotNull();

    // when I do process instance modification
    runtimeService.createProcessInstanceModification(
      subprocess.getProcessInstanceId())
      .cancelAllForActivity("userTask")
      .startAfterActivity("userTask")
      .execute();

    // then the parent process instance is still active
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getProcessInstanceId()).isEqualTo(parentPI.getId());
  }

  @Test
  void shouldCompleteParentProcessWithParallelGateway() {

    final BpmnModelInstance modelInstance =
      Bpmn.createExecutableProcess("parentProcess").startEvent()
        .parallelGateway()
          .serviceTask("doNothingServiceTask").operatonExpression("${true}")
        .moveToLastGateway()
          .callActivity("callActivity").calledElement("subprocess")
        .parallelGateway("mergingParallelGateway")
      .endEvent()
      .done();

    final BpmnModelInstance parentProcessInstance =
      modify(modelInstance)
        .flowNodeBuilder("doNothingServiceTask").connectTo("mergingParallelGateway").done();

    final BpmnModelInstance subprocessInstance =
        Bpmn.createExecutableProcess("subprocess")
          .startEvent()
            .userTask("userTask")
          .endEvent("subEnd")
          .done();

    testHelper.deploy(parentProcessInstance, subprocessInstance);

    // given I start the process, which waits at user task in subprocess
    runtimeService.startProcessInstanceByKey("parentProcess");

    final ProcessInstance subprocess = runtimeService.createProcessInstanceQuery().processDefinitionKey("subprocess").singleResult();
    assertThat(subprocess).isNotNull();

    assertThat(taskService.createTaskQuery().taskName("userTask").singleResult()).isNotNull();

    // when I do process instance modification
    runtimeService.createProcessInstanceModification(
      subprocess.getProcessInstanceId())
      .cancelAllForActivity("userTask")
      .startAfterActivity("userTask")
      .execute();

    // then the process should be finished
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

  }

  @Test
  void shouldContinueParentProcessWithParallelGateway() {

    final BpmnModelInstance modelInstance =
      Bpmn.createExecutableProcess("parentProcess").startEvent()
        .parallelGateway()
          .serviceTask("doNothingServiceTask").operatonExpression("${true}")
        .moveToLastGateway()
          .callActivity("callActivity").calledElement("subprocess")
        .parallelGateway("mergingParallelGateway")
        .userTask()
      .endEvent()
      .done();

    final BpmnModelInstance parentProcessInstance =
      modify(modelInstance)
        .flowNodeBuilder("doNothingServiceTask").connectTo("mergingParallelGateway").done();

    final BpmnModelInstance subprocessInstance =
        Bpmn.createExecutableProcess("subprocess")
          .startEvent()
            .userTask("userTask")
          .endEvent("subEnd")
          .done();

    testHelper.deploy(parentProcessInstance, subprocessInstance);

    // given I start the process, which waits at user task in subprocess
    ProcessInstance parentPI = runtimeService.startProcessInstanceByKey("parentProcess");

    final ProcessInstance subprocess = runtimeService.createProcessInstanceQuery().processDefinitionKey("subprocess").singleResult();
    assertThat(subprocess).isNotNull();

    assertThat(taskService.createTaskQuery().taskName("userTask").singleResult()).isNotNull();

    // when I do process instance modification
    runtimeService.createProcessInstanceModification(
      subprocess.getProcessInstanceId())
      .cancelAllForActivity("userTask")
      .startAfterActivity("userTask")
      .execute();

    // then the parent process instance is still active
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getProcessInstanceId()).isEqualTo(parentPI.getId());

  }

  @Test
  void shouldCompleteParentProcessWithMultiInstance() {

    final BpmnModelInstance parentProcessInstance =
      Bpmn.createExecutableProcess("parentProcess")
        .startEvent()
          .callActivity("callActivity").calledElement("subprocess")
            .multiInstance().cardinality("3").multiInstanceDone()
        .endEvent()
        .done();

    final BpmnModelInstance subprocessInstance =
      Bpmn.createExecutableProcess("subprocess")
        .startEvent()
          .userTask("userTask")
        .endEvent("subEnd")
        .done();

    testHelper.deploy(parentProcessInstance, subprocessInstance);
    final String subprocessPrDefId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("subprocess").singleResult().getId();

    // given I start the process, which waits at user task inside multiinstance subprocess
    runtimeService.startProcessInstanceByKey("parentProcess");

    final List<ProcessInstance> subprocesses = runtimeService.createProcessInstanceQuery().processDefinitionKey("subprocess").list();
    assertThat(subprocesses).hasSize(3);

    // when I do process instance modification
    runtimeService.createModification(subprocessPrDefId)
      .cancelAllForActivity("userTask")
      .startAfterActivity("userTask")
      .processInstanceIds(collectIds(subprocesses))
      .execute();

    // then the process should be finished
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

  }

  @Test
  void shouldContinueParentProcessWithMultiInstance() {

    final BpmnModelInstance parentProcessInstance =
      Bpmn.createExecutableProcess("parentProcess")
        .startEvent()
          .callActivity("callActivity").calledElement("subprocess")
            .multiInstance().cardinality("3").multiInstanceDone()
        .userTask()
        .endEvent()
        .done();

    final BpmnModelInstance subprocessInstance =
      Bpmn.createExecutableProcess("subprocess")
        .startEvent()
          .userTask("userTask")
        .endEvent("subEnd")
        .done();

    testHelper.deploy(parentProcessInstance, subprocessInstance);
    final String subprocessPrDefId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("subprocess").singleResult().getId();

    // given I start the process, which waits at user task inside multiinstance subprocess
    ProcessInstance parentPI = runtimeService.startProcessInstanceByKey("parentProcess");

    final List<ProcessInstance> subprocesses = runtimeService.createProcessInstanceQuery().processDefinitionKey("subprocess").list();
    assertThat(subprocesses).hasSize(3);

    // when I do process instance modification
    runtimeService.createModification(subprocessPrDefId)
      .cancelAllForActivity("userTask")
      .startAfterActivity("userTask")
      .processInstanceIds(collectIds(subprocesses))
      .execute();

    // then the parent process instance is still active
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getProcessInstanceId()).isEqualTo(parentPI.getId());

  }

  @Test
  void shouldCompleteParentProcessWithMultiInstanceInsideEmbeddedSubProcess() {

    final BpmnModelInstance parentProcessInstance =
        Bpmn.createExecutableProcess("parentProcess")
          .startEvent()
          .subProcess()
            .embeddedSubProcess()
              .startEvent()
              .callActivity("callActivity")
                .calledElement("subprocess")
                .multiInstance()
                .cardinality("3")
                .multiInstanceDone()
              .endEvent()
          .subProcessDone()
          .endEvent()
          .done();

    final BpmnModelInstance subprocessInstance =
      Bpmn.createExecutableProcess("subprocess")
        .startEvent()
          .userTask("userTask")
        .endEvent("subEnd")
        .done();

    testHelper.deploy(parentProcessInstance, subprocessInstance);
    final String subprocessPrDefId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("subprocess").singleResult().getId();

    // given I start the process, which waits at user task inside multiinstance subprocess
    runtimeService.startProcessInstanceByKey("parentProcess");

    final List<ProcessInstance> subprocesses = runtimeService.createProcessInstanceQuery().processDefinitionKey("subprocess").list();
    assertThat(subprocesses).hasSize(3);

    // when I do process instance modification
    runtimeService.createModification(subprocessPrDefId)
      .cancelAllForActivity("userTask")
      .startAfterActivity("userTask")
      .processInstanceIds(collectIds(subprocesses))
      .execute();

    // then the process should be finished
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Test
  void shouldContinueParentProcessWithMultiInstanceInsideEmbeddedSubProcess() {

    final BpmnModelInstance parentProcessInstance =
        Bpmn.createExecutableProcess("parentProcess")
          .startEvent()
          .subProcess()
            .embeddedSubProcess()
              .startEvent()
              .callActivity("callActivity")
                .calledElement("subprocess")
                .multiInstance()
                .cardinality("3")
                .multiInstanceDone()
              .endEvent()
          .subProcessDone()
          .userTask()
          .endEvent()
          .done();

    final BpmnModelInstance subprocessInstance =
      Bpmn.createExecutableProcess("subprocess")
        .startEvent()
          .userTask("userTask")
        .endEvent("subEnd")
        .done();

    testHelper.deploy(parentProcessInstance, subprocessInstance);
    final String subprocessPrDefId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("subprocess").singleResult().getId();

    // given I start the process, which waits at user task inside multiinstance subprocess
    ProcessInstance parentPI = runtimeService.startProcessInstanceByKey("parentProcess");

    final List<ProcessInstance> subprocesses = runtimeService.createProcessInstanceQuery().processDefinitionKey("subprocess").list();
    assertThat(subprocesses).hasSize(3);

    // when I do process instance modification
    runtimeService.createModification(subprocessPrDefId)
      .cancelAllForActivity("userTask")
      .startAfterActivity("userTask")
      .processInstanceIds(collectIds(subprocesses))
      .execute();

    // then the parent process instance is still active
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getProcessInstanceId()).isEqualTo(parentPI.getId());
  }

  @Test
  void shouldCompleteParentProcessWithMultiInstanceEmbeddedSubProcess() {

    final BpmnModelInstance parentProcessInstance =
        Bpmn.createExecutableProcess("parentProcess")
          .startEvent()
          .subProcess()
            .embeddedSubProcess()
              .startEvent()
              .callActivity("callActivity")
                .calledElement("subprocess")
              .endEvent()
          .subProcessDone()
          .multiInstance()
          .cardinality("3")
          .multiInstanceDone()
          .endEvent()
          .done();

    final BpmnModelInstance subprocessInstance =
      Bpmn.createExecutableProcess("subprocess")
        .startEvent()
          .userTask("userTask")
        .endEvent("subEnd")
        .done();

    testHelper.deploy(parentProcessInstance, subprocessInstance);
    final String subprocessPrDefId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("subprocess").singleResult().getId();

    // given I start the process, which waits at user task inside multiinstance subprocess
    runtimeService.startProcessInstanceByKey("parentProcess");

    final List<ProcessInstance> subprocesses = runtimeService.createProcessInstanceQuery().processDefinitionKey("subprocess").list();
    assertThat(subprocesses).hasSize(3);

    // when I do process instance modification
    runtimeService.createModification(subprocessPrDefId)
      .cancelAllForActivity("userTask")
      .startAfterActivity("userTask")
      .processInstanceIds(collectIds(subprocesses))
      .execute();

    // then the process should be finished
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Test
  void shouldContinueParentProcessWithMultiInstanceEmbeddedSubProcess() {

    final BpmnModelInstance parentProcessInstance =
        Bpmn.createExecutableProcess("parentProcess")
          .startEvent()
          .subProcess()
            .embeddedSubProcess()
              .startEvent()
              .callActivity("callActivity")
                .calledElement("subprocess")
              .endEvent()
          .subProcessDone()
            .multiInstance()
            .cardinality("3")
            .multiInstanceDone()
          .userTask()
          .endEvent()
          .done();

    final BpmnModelInstance subprocessInstance =
      Bpmn.createExecutableProcess("subprocess")
        .startEvent()
          .userTask("userTask")
        .endEvent("subEnd")
        .done();

    testHelper.deploy(parentProcessInstance, subprocessInstance);
    final String subprocessPrDefId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("subprocess").singleResult().getId();

    // given I start the process, which waits at user task inside multiinstance subprocess
    ProcessInstance parentPI = runtimeService.startProcessInstanceByKey("parentProcess");

    final List<ProcessInstance> subprocesses = runtimeService.createProcessInstanceQuery().processDefinitionKey("subprocess").list();
    assertThat(subprocesses).hasSize(3);

    // when I do process instance modification
    runtimeService.createModification(subprocessPrDefId)
      .cancelAllForActivity("userTask")
      .startAfterActivity("userTask")
      .processInstanceIds(collectIds(subprocesses))
      .execute();

    // then the parent process instance is still active
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getProcessInstanceId()).isEqualTo(parentPI.getId());
  }

  @Test
  void shouldCancelParentProcessWithMultiInstanceCallActivity() {
    BpmnModelInstance parentProcess = Bpmn.createExecutableProcess("parentProcess")
      .startEvent()
      .callActivity("callActivity")
        .calledElement("subprocess")
        .multiInstance()
        .cardinality("3")
        .multiInstanceDone()
      .endEvent()
      .userTask()
      .endEvent()
      .done();

    BpmnModelInstance subProcess = Bpmn.createExecutableProcess("subprocess")
      .startEvent()
        .userTask("userTask")
      .endEvent("subEnd")
      .done();

    testHelper.deploy(parentProcess, subProcess);
    ProcessDefinition subProcessDefinition = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("subprocess")
        .singleResult();

    // given
    runtimeService.startProcessInstanceByKey("parentProcess");

    // assume
    List<ProcessInstance> subProcessInstances = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey("subprocess")
        .list();
    assertThat(subProcessInstances).hasSize(3);

    // when
    runtimeService.createModification(subProcessDefinition.getId())
      .startAfterActivity("userTask")
      .cancelAllForActivity("userTask")
      .processInstanceIds(collectIds(subProcessInstances))
      .execute();

    // then
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Test
  void shouldCancelParentProcessWithCallActivityInMultiInstanceEmbeddedSubprocess() {
    BpmnModelInstance parentProcess = Bpmn.createExecutableProcess("parentProcess")
      .startEvent()
      .subProcess()
        .embeddedSubProcess()
        .startEvent()
        .callActivity("callActivity")
          .calledElement("subprocess")
        .endEvent()
      .subProcessDone()
        .multiInstance()
        .cardinality("3")
        .multiInstanceDone()
      .endEvent()
      .userTask()
      .endEvent()
      .done();

    BpmnModelInstance subProcess = Bpmn.createExecutableProcess("subprocess")
      .startEvent()
        .userTask("userTask")
      .endEvent("subEnd")
      .done();

    testHelper.deploy(parentProcess, subProcess);
    ProcessDefinition subProcessDefinition = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("subprocess")
        .singleResult();

    // given
    runtimeService.startProcessInstanceByKey("parentProcess");

    // assume
    List<ProcessInstance> subProcessInstances = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey("subprocess")
        .list();
    assertThat(subProcessInstances).hasSize(3);

    // when
    runtimeService.createModification(subProcessDefinition.getId())
      .startAfterActivity("userTask")
      .cancelAllForActivity("userTask")
      .processInstanceIds(collectIds(subProcessInstances))
      .execute();

    // then
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Test
  void shouldCancelConcurrentExecutionInCallingProcess()
  {
    // given
    final BpmnModelInstance parentProcessInstance =
        Bpmn.createExecutableProcess("parentProcess")
          .startEvent()
          .parallelGateway("split")
            .callActivity("callActivity").calledElement("subprocess")
            .endEvent()
          .moveToLastGateway()
            .userTask("parentUserTask")
            .endEvent()
          .done();

      final BpmnModelInstance subprocessInstance =
        Bpmn.createExecutableProcess("subprocess")
          .startEvent()
            .userTask("childUserTask")
          .endEvent("subEnd")
          .done();

      testHelper.deploy(parentProcessInstance, subprocessInstance);

      ProcessInstance callingInstance = runtimeService.startProcessInstanceByKey("parentProcess");
      ProcessInstance calledInstance = runtimeService.createProcessInstanceQuery()
          .superProcessInstanceId(callingInstance.getId()).singleResult();

      // when
      runtimeService
        .createProcessInstanceModification(calledInstance.getId())
        .cancelAllForActivity("childUserTask")
        .execute();

      // then
      ProcessInstance calledInstanceAfterModification = runtimeService
          .createProcessInstanceQuery()
          .processInstanceId(calledInstance.getId())
          .singleResult();

    assertThat(calledInstanceAfterModification).isNull();

      ExecutionTree executionTree = ExecutionTree.forExecution(callingInstance.getId(), rule.getProcessEngine());
      ExecutionAssert.assertThat(executionTree)
        .matches(
          describeExecutionTree("parentUserTask").scope()
        .done());
  }

  @Test
  void shouldContinueParentWithEscalationEndEvent() {
    BpmnModelInstance parentProcess = Bpmn.createExecutableProcess("parentProcess")
                                          .startEvent()
                                          .callActivity("callActivity")
                                            .calledElement("subprocess")
                                          .boundaryEvent("escalationEvent")
                                            .escalation("escalation")
                                            .userTask("escalationTask")
                                            .endEvent()
                                          .moveToActivity("callActivity")
                                            .userTask("normalTask")
                                            .endEvent()
                                          .done();

    BpmnModelInstance subProcess = Bpmn.createExecutableProcess("subprocess")
                                       .startEvent()
                                       .userTask("userTask")
                                       .endEvent("subEnd")
                                       .escalation("escalation")
                                       .done();

    testHelper.deploy(parentProcess, subProcess);

    // given I start the process, which wait as user task in subprocess
    ProcessInstance parentPI = runtimeService.startProcessInstanceByKey("parentProcess");

    assertThat(taskService.createTaskQuery().taskName("userTask").singleResult()).isNotNull();

    final ProcessInstance subprocess = runtimeService.createProcessInstanceQuery().processDefinitionKey("subprocess").singleResult();
    assertThat(subprocess).isNotNull();

    // when I do process instance modification
    runtimeService.createProcessInstanceModification(
            subprocess.getProcessInstanceId())
                  .cancelAllForActivity("userTask")
                  .startAfterActivity("userTask")
                  .execute();

    // then the parent process instance is still active
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().taskName("escalationTask").singleResult()).isNotNull();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getProcessInstanceId()).isEqualTo(parentPI.getId());
  }

  @Test
  void shouldContinueParentWithErrorEndEvent() {
    BpmnModelInstance parentProcess = Bpmn.createExecutableProcess("parentProcess")
                                          .startEvent()
                                          .callActivity("callActivity")
                                            .calledElement("subprocess")
                                          .boundaryEvent("errorEvent")
                                            .error("error")
                                            .userTask("errorTask")
                                            .endEvent()
                                          .moveToActivity("callActivity")
                                            .userTask("normalTask")
                                            .endEvent()
                                          .done();

    BpmnModelInstance subProcess = Bpmn.createExecutableProcess("subprocess")
                                       .startEvent()
                                       .userTask("userTask")
                                       .endEvent("subEnd")
                                       .error("error")
                                       .done();

    testHelper.deploy(parentProcess, subProcess);

    // given I start the process, which wait as user task in subprocess
    ProcessInstance parentPI = runtimeService.startProcessInstanceByKey("parentProcess");

    assertThat(taskService.createTaskQuery().taskName("userTask").singleResult()).isNotNull();

    final ProcessInstance subprocess = runtimeService.createProcessInstanceQuery().processDefinitionKey("subprocess").singleResult();
    assertThat(subprocess).isNotNull();

    // when I do process instance modification
    runtimeService.createProcessInstanceModification(
            subprocess.getProcessInstanceId())
                  .cancelAllForActivity("userTask")
                  .startAfterActivity("userTask")
                  .execute();

    // then the parent process instance is still active
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();
    assertThat(taskService.createTaskQuery().taskName("errorTask").singleResult()).isNotNull();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getProcessInstanceId()).isEqualTo(parentPI.getId());
  }


  private List<String> collectIds(List<ProcessInstance> processInstances) {
    List<String> supbrocessIds = new ArrayList<>();
    for (ProcessInstance processInstance: processInstances) {
      supbrocessIds.add(processInstance.getId());
    }
    return supbrocessIds;
  }

}
