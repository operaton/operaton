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
package org.operaton.bpm.engine.test.api.variables.scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.ScriptEvaluationException;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessInstanceWithVariablesImpl;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.SequenceFlow;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonExecutionListener;

/**
 * @author Askar Akhmerov
 * @author Tassilo Weidner
 */
class TargetVariableScopeTest {
  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/variables/scope/TargetVariableScopeTest.testExecutionWithDelegateProcess.bpmn", "org/operaton/bpm/engine/test/api/variables/scope/doer.bpmn"})
  void testExecutionWithDelegateProcess() {
    // Given we create a new process instance
    VariableMap variables = Variables.createVariables().putValue("orderIds", List.of(new int[] { 1, 2, 3 }));
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process_MultiInstanceCallActivity",variables);

    // it runs without any problems
    assertThat(processInstance.isEnded()).isTrue();
    assertThat(((ProcessInstanceWithVariablesImpl) processInstance).getVariables().containsKey("targetOrderId")).isFalse();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/variables/scope/TargetVariableScopeTest.testExecutionWithScriptTargetScope.bpmn", "org/operaton/bpm/engine/test/api/variables/scope/doer.bpmn"})
  void testExecutionWithScriptTargetScope() {
    VariableMap variables = Variables.createVariables().putValue("orderIds", List.of(new int[] { 1, 2, 3 }));
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process_MultiInstanceCallActivity",variables);

    // it runs without any problems
    assertThat(processInstance.isEnded()).isTrue();
    assertThat(((ProcessInstanceWithVariablesImpl) processInstance).getVariables().containsKey("targetOrderId")).isFalse();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/variables/scope/TargetVariableScopeTest.testExecutionWithoutProperTargetScope.bpmn", "org/operaton/bpm/engine/test/api/variables/scope/doer.bpmn"})
  void testExecutionWithoutProperTargetScope() {
    VariableMap variables = Variables.createVariables().putValue("orderIds", List.of(new int[] { 1, 2, 3 }));
    ProcessDefinition processDefinition = engineRule.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey("Process_MultiInstanceCallActivity").singleResult();
    RuntimeService runtimeService1 = runtimeService;

    // when/then
    //fails due to inappropriate variable scope target
    assertThatThrownBy(() -> runtimeService1.startProcessInstanceByKey("Process_MultiInstanceCallActivity",variables))
      .isInstanceOf(ScriptEvaluationException.class)
      .hasMessageContaining("Unable to evaluate script while executing activity 'CallActivity_1' in the process definition with id '"
          + processDefinition.getId() + "': org.operaton.bpm.engine.ProcessEngineException: ENGINE-20011 "
              + "Scope with specified activity Id NOT_EXISTING and execution");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/variables/scope/doer.bpmn"})
  void testWithDelegateVariableMapping() {
    BpmnModelInstance instance = Bpmn.createExecutableProcess("process1")
        .startEvent()
          .subProcess("SubProcess_1")
          .embeddedSubProcess()
            .startEvent()
              .callActivity()
                .calledElement("Process_StuffDoer")
                .operatonVariableMappingClass("org.operaton.bpm.engine.test.api.variables.scope.SetVariableMappingDelegate")
              .serviceTask()
                .operatonClass("org.operaton.bpm.engine.test.api.variables.scope.AssertVariableScopeDelegate")
            .endEvent()
          .subProcessDone()
        .endEvent()
        .done();
    instance = modify(instance)
        .activityBuilder("SubProcess_1")
        .multiInstance()
        .parallel()
        .operatonCollection("orderIds")
        .operatonElementVariable("orderId")
        .done();

    ProcessDefinition processDefinition = testHelper.deployAndGetDefinition(instance);
    String processDefinitionId = processDefinition.getId();
    VariableMap variables = Variables.createVariables().putValue("orderIds", List.of(new int[] { 1, 2, 3 }));
    assertThatCode(() -> runtimeService.startProcessInstanceById(processDefinitionId,variables))
      .doesNotThrowAnyException();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/variables/scope/doer.bpmn"})
  void testWithDelegateVariableMappingAndChildScope() {
    BpmnModelInstance instance = Bpmn.createExecutableProcess("process1")
        .startEvent()
          .parallelGateway()
            .subProcess("SubProcess_1")
            .embeddedSubProcess()
              .startEvent()
              .callActivity()
                .calledElement("Process_StuffDoer")
                .operatonVariableMappingClass("org.operaton.bpm.engine.test.api.variables.scope.SetVariableToChildMappingDelegate")
              .serviceTask()
                .operatonClass("org.operaton.bpm.engine.test.api.variables.scope.AssertVariableScopeDelegate")
              .endEvent()
            .subProcessDone()
          .moveToLastGateway()
            .subProcess("SubProcess_2")
            .embeddedSubProcess()
              .startEvent()
                .userTask("ut")
              .endEvent()
            .subProcessDone()
        .endEvent()
        .done();
    instance = modify(instance)
        .activityBuilder("SubProcess_1")
        .multiInstance()
        .parallel()
        .operatonCollection("orderIds")
        .operatonElementVariable("orderId")
        .done();

    ProcessDefinition processDefinition = testHelper.deployAndGetDefinition(instance);
    String processDefinitionId = processDefinition.getId();

    VariableMap variables = Variables.createVariables().putValue("orderIds", List.of(new int[] { 1, 2, 3 }));

    // when/then
    //fails due to inappropriate variable scope target
    assertThatThrownBy(() -> runtimeService.startProcessInstanceById(processDefinitionId, variables))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("org.operaton.bpm.engine.ProcessEngineException: ENGINE-20011 Scope with specified activity Id SubProcess_2 and execution");

  }

  public static class JavaDelegate implements org.operaton.bpm.engine.delegate.JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
      execution.setVariable("varName", "varValue", "activityId");
      assertThat(execution.getVariableLocal("varName")).isNotNull();
    }

  }

  public static class ExecutionListener implements org.operaton.bpm.engine.delegate.ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
      execution.setVariable("varName", "varValue", "activityId");
      assertThat(execution.getVariableLocal("varName")).isNotNull();
    }

  }

  public static class TaskListener implements org.operaton.bpm.engine.delegate.TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
      DelegateExecution execution = delegateTask.getExecution();
      execution.setVariable("varName", "varValue", "activityId");
      assertThat(execution.getVariableLocal("varName")).isNotNull();
    }
  }

  @Test
  void testSetLocalScopeWithJavaDelegate() {
    testHelper.deploy(Bpmn.createExecutableProcess("process")
      .startEvent()
      .serviceTask()
        .id("activityId")
        .operatonClass(JavaDelegate.class)
      .endEvent()
      .done());

    assertThatCode(() -> runtimeService.startProcessInstanceByKey("process")).doesNotThrowAnyException();
  }

  @Test
  void testSetLocalScopeWithExecutionListenerStart() {
    testHelper.deploy(Bpmn.createExecutableProcess("process")
      .startEvent().id("activityId")
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, ExecutionListener.class)
      .endEvent()
      .done());

    assertThatCode(() -> runtimeService.startProcessInstanceByKey("process")).doesNotThrowAnyException();
  }

  @Test
  void testSetLocalScopeWithExecutionListenerEnd() {
    testHelper.deploy(Bpmn.createExecutableProcess("process")
      .startEvent()
      .endEvent().id("activityId")
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, ExecutionListener.class)
      .done());

    assertThatCode(() -> runtimeService.startProcessInstanceByKey("process")).doesNotThrowAnyException();
  }

  @Test
  void testSetLocalScopeWithExecutionListenerTake() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process")
      .startEvent().id("activityId")
      .sequenceFlowId("sequenceFlow")
      .endEvent()
      .done();

    OperatonExecutionListener listener = modelInstance.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setOperatonClass(ExecutionListener.class.getName());
    modelInstance.<SequenceFlow>getModelElementById("sequenceFlow").builder().addExtensionElement(listener);

    testHelper.deploy(modelInstance);
    assertThatCode(() -> runtimeService.startProcessInstanceByKey("process")).doesNotThrowAnyException();
  }

  @Test
  void testSetLocalScopeWithTaskListener() {
    testHelper.deploy(Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask().id("activityId")
        .operatonTaskListenerClass(TaskListener.EVENTNAME_CREATE, TaskListener.class)
      .endEvent()
      .done());

    assertThatCode(() -> runtimeService.startProcessInstanceByKey("process")).doesNotThrowAnyException();
  }

  @Test
  void testSetLocalScopeInSubprocessWithJavaDelegate() {
    testHelper.deploy(Bpmn.createExecutableProcess("process")
      .startEvent()
      .subProcess().embeddedSubProcess()
        .startEvent()
          .serviceTask().id("activityId")
            .operatonClass(JavaDelegate.class)
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done());

    assertThatCode(() -> runtimeService.startProcessInstanceByKey("process")).doesNotThrowAnyException();
  }

  @Test
  void testSetLocalScopeInSubprocessWithStartExecutionListener() {
    testHelper.deploy(Bpmn.createExecutableProcess("process")
      .startEvent()
      .subProcess().embeddedSubProcess()
        .startEvent().id("activityId")
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, ExecutionListener.class)
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done());

    assertThatCode(() -> runtimeService.startProcessInstanceByKey("process")).doesNotThrowAnyException();
  }

  @Test
  void testSetLocalScopeInSubprocessWithEndExecutionListener() {
    testHelper.deploy(Bpmn.createExecutableProcess("process")
      .startEvent()
      .subProcess().embeddedSubProcess()
        .startEvent()
        .endEvent().id("activityId")
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, ExecutionListener.class)
      .subProcessDone()
      .endEvent()
      .done());

    assertThatCode(() -> runtimeService.startProcessInstanceByKey("process")).doesNotThrowAnyException();
  }

  @Test
  void testSetLocalScopeInSubprocessWithTaskListener() {
    testHelper.deploy(Bpmn.createExecutableProcess("process")
      .startEvent()
      .subProcess().embeddedSubProcess()
        .startEvent()
        .userTask().id("activityId")
        .operatonTaskListenerClass(TaskListener.EVENTNAME_CREATE, TaskListener.class)
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done());

    assertThatCode(() -> runtimeService.startProcessInstanceByKey("process")).doesNotThrowAnyException();
  }

}
