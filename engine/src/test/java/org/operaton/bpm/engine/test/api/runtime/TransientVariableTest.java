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

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.StringValue;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.CONDITIONAL_PROCESS_KEY;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.CONDITION_ID;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.USER_TASK_ID;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.VARIABLE_NAME;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.VAR_CONDITION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
class TransientVariableTest {

  private static final int OUTPUT_VALUE = 2;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  HistoryService historyService;
  TaskService taskService;

  @Test
  void createTransientTypedVariablesUsingVariableMap() throws Exception {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process")
      .startEvent()
      .serviceTask()
        .operatonClass(ReadTransientVariablesOfAllTypesDelegate.class.getName())
      .userTask("user")
      .endEvent()
      .done();

    testRule.deploy(instance);

    // when
    runtimeService.startProcessInstanceByKey("Process",
        Variables.createVariables()
            .putValueTyped("a", Variables.stringValue("bar", true))
            .putValueTyped("b", Variables.booleanValue(true, true))
            .putValueTyped("c", Variables.byteArrayValue("test".getBytes(), true))
            .putValueTyped("d", Variables.dateValue(new Date(), true))
            .putValueTyped("e", Variables.doubleValue(20., true))
            .putValueTyped("f", Variables.integerValue(10, true))
            .putValueTyped("g", Variables.longValue((long) 10, true))
            .putValueTyped("h", Variables.shortValue((short) 10, true))
            .putValueTyped("i", Variables.objectValue(100, true).create())
            .putValueTyped("j", Variables.untypedValue(null, true))
            .putValueTyped("k", Variables.untypedValue(Variables.booleanValue(true), true))
            .putValueTyped("l", Variables.fileValue(new File(this.getClass().getClassLoader()
                .getResource("org/operaton/bpm/engine/test/standalone/variables/simpleFile.txt").toURI()), true)));

    // then
    List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery().list();
    List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().list();
    assertThat(historicVariableInstances).isEmpty();
    assertThat(variableInstances).isEmpty();
  }

  @Test
  void createTransientVariablesUsingVariableMap() throws Exception {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process")
      .startEvent()
      .serviceTask()
        .operatonClass(ReadTransientVariablesOfAllTypesDelegate.class.getName())
      .userTask("user")
      .endEvent()
      .done();

    testRule.deploy(instance);

    // when
    runtimeService.startProcessInstanceByKey("Process",
        Variables.createVariables().putValue("a", Variables.stringValue("bar", true))
        .putValue("b", Variables.booleanValue(true, true))
        .putValue("c", Variables.byteArrayValue("test".getBytes(), true))
        .putValue("d", Variables.dateValue(new Date(), true))
        .putValue("e", Variables.doubleValue(20., true))
        .putValue("f", Variables.integerValue(10, true))
        .putValue("g", Variables.longValue((long) 10, true))
        .putValue("h", Variables.shortValue((short) 10, true))
        .putValue("i", Variables.objectValue(100, true).create())
        .putValue("j", Variables.untypedValue(null, true))
        .putValue("k", Variables.untypedValue(Variables.booleanValue(true), true))
        .putValue("l", Variables.fileValue(new File(this.getClass().getClassLoader()
            .getResource("org/operaton/bpm/engine/test/standalone/variables/simpleFile.txt").toURI()), true)));

    // then
    List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery().list();
    List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().list();
    assertThat(historicVariableInstances).isEmpty();
    assertThat(variableInstances).isEmpty();
  }

  @Test
  void createTransientVariablesUsingFluentBuilder() {
    // given
    BpmnModelInstance simpleInstanceWithListener = Bpmn.createExecutableProcess("Process")
        .startEvent()
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, ReadTransientVariableExecutionListener.class)
        .userTask()
        .endEvent()
        .done();
    testRule.deploy(simpleInstanceWithListener);

    // when
    runtimeService.createProcessInstanceByKey("Process")
      .setVariables(Variables.createVariables().putValue(VARIABLE_NAME, Variables.stringValue("dlsd", true)))
      .execute();

    // then
    List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().list();
    List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery().list();
    assertThat(variableInstances).isEmpty();
    assertThat(historicVariableInstances).isEmpty();
  }

  @Test
  void createVariablesUsingVariableMap() {
    // given
    BpmnModelInstance simpleInstanceWithListener = Bpmn.createExecutableProcess("Process")
        .startEvent()
          .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, ReadTransientVariableExecutionListener.class)
        .userTask()
        .endEvent()
        .done();
    testRule.deploy(simpleInstanceWithListener);

    // when
    VariableMap variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.untypedValue(true, true));
    runtimeService.startProcessInstanceByKey("Process",
       variables
        );

    // then
    List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().list();
    List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery().list();
    assertThat(variableInstances).isEmpty();
    assertThat(historicVariableInstances).isEmpty();
  }

  @Test
  void triggerConditionalEventWithTransientVariable() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess(CONDITIONAL_PROCESS_KEY)
        .startEvent()
        .serviceTask()
        .operatonClass(SetVariableTransientDelegate.class.getName())
        .intermediateCatchEvent(CONDITION_ID)
        .conditionalEventDefinition()
        .condition(VAR_CONDITION)
        .conditionalEventDefinitionDone()
        .endEvent()
        .done();

    testRule.deploy(instance);

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CONDITIONAL_PROCESS_KEY);

    // then
    assertThat(processInstance.isEnded()).isTrue();
  }


  @Test
  void testParallelProcessWithSetVariableTransientAfterReachingEventBasedGW() {
    BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess(CONDITIONAL_PROCESS_KEY)
          .startEvent()
          .parallelGateway()
          .id("parallel")
          .userTask("taskBeforeGw")
          .eventBasedGateway()
          .id("evenBased")
          .intermediateCatchEvent()
          .conditionalEventDefinition()
          .condition(VAR_CONDITION)
          .operatonVariableEvents(Arrays.asList("create", "update"))
          .conditionalEventDefinitionDone()
          .userTask()
          .name("taskAfter")
          .endEvent()
          .moveToNode("parallel")
          .userTask("taskBefore")
          .serviceTask()
          .operatonClass(SetVariableTransientDelegate.class.getName())
          .endEvent()
          .done();

    testRule.deploy(modelInstance);

    //given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task taskBeforeEGW = taskService.createTaskQuery().taskDefinitionKey("taskBeforeGw").singleResult();
    Task taskBeforeServiceTask = taskService.createTaskQuery().taskDefinitionKey("taskBefore").singleResult();

    //when task before event based gateway is completed and after that task before service task
    taskService.complete(taskBeforeEGW.getId());
    taskService.complete(taskBeforeServiceTask.getId());

    //then event based gateway is reached and executions stays there
    //variable is set after reaching event based gateway
    //after setting variable the conditional event is triggered and evaluated to true
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo("taskAfter");
    //completing this task ends process instance
    taskService.complete(task.getId());
    assertThat(taskQuery.singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNull();
  }

  @Test
  void setVariableTransientInRunningProcessInstance() {
    // given
    testRule.deploy(ProcessModels.ONE_TASK_PROCESS);

    // when
    runtimeService.startProcessInstanceByKey(ProcessModels.PROCESS_KEY);
    Execution execution = runtimeService.createExecutionQuery().singleResult();
    runtimeService.setVariable(execution.getId(), "foo", Variables.stringValue("bar", true));

    // then
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().list();
    assertThat(variables).isEmpty();
  }

  @Test
  void setVariableTransientForCase() {
    // given
    testRule.deploy("org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn");

    // when
    engineRule.getCaseService().withCaseDefinitionByKey("oneTaskCase")
        .setVariable("foo", Variables.stringValue("bar", true)).create();

    // then
    List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().list();
    assertThat(variables).isEmpty();
  }

  @Test
  void testTransientVariableOvewritesPersistedVariableInSameScope() {
    testRule.deploy(ProcessModels.ONE_TASK_PROCESS);
    runtimeService.startProcessInstanceByKey("Process", Variables.createVariables().putValue("foo", "bar"));
    Execution execution = runtimeService.createExecutionQuery().singleResult();

    try {
      runtimeService.setVariable(execution.getId(), "foo", Variables.stringValue("xyz", true));
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Cannot set transient variable with name 'foo'");
    }
  }

  @Test
  void testSameNamesDifferentScopes() {
    testRule.deploy(ProcessModels.SUBPROCESS_PROCESS);
    runtimeService.startProcessInstanceByKey("Process", Variables.createVariables().putValue("foo", Variables.stringValue("bar")));
    Execution execution = runtimeService.createExecutionQuery().activityId(USER_TASK_ID).singleResult();

    try {
      runtimeService.setVariable(execution.getId(), "foo", Variables.stringValue("xyz", true));
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Cannot set transient variable with name 'foo'");
    }
  }

  @Test
  void testFormFieldsWithCustomTransientFlags() {
    // given
    testRule.deploy("org/operaton/bpm/engine/test/api/form/TransientVariableTest.taskFormFieldsWithTransientFlags.bpmn20.xml");
    runtimeService.startProcessInstanceByKey("testProcess");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    Map<String, Object> formValues = new HashMap<>();
    formValues.put("stringField", Variables.stringValue("foobar", true));
    formValues.put("longField", 9L);
    engineRule.getFormService().submitTaskForm(task.getId(), formValues);

    // then
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().list();
    assertThat(variables).hasSize(1);
    assertThat(variables.get(0).getValue()).isEqualTo(9L);
  }

  @Test
  void testStartProcessInstanceWithFormsUsingTransientVariables() {
    // given
    testRule.deploy("org/operaton/bpm/engine/test/api/form/TransientVariableTest.startFormFieldsWithTransientFlags.bpmn20.xml");
    ProcessDefinition processDefinition = engineRule.getRepositoryService().createProcessDefinitionQuery().singleResult();

    // when
    Map<String, Object> formValues = new HashMap<>();
    formValues.put("stringField", Variables.stringValue("foobar", true));
    formValues.put("longField", 9L);
    engineRule.getFormService().submitStartForm(processDefinition.getId(), formValues);

    // then
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().list();
    assertThat(variables).hasSize(1);
    assertThat(variables.get(0).getValue()).isEqualTo(9L);
  }

  @Test
  void testSignalWithTransientVariables() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process")
    .startEvent()
    .intermediateCatchEvent("signalCatch")
      .signal("signal")
    .scriptTask("scriptTask")
      .scriptFormat("javascript")
      .operatonResultVariable("abc")
      .scriptText("execution.setVariable('abc', foo);")
    .endEvent()
    .done();

    testRule.deploy(instance);
    runtimeService.startProcessInstanceByKey("Process");

    // when
    runtimeService.signalEventReceived("signal",
        Variables.createVariables().putValue("foo", Variables.stringValue("bar", true)));

    // then
    List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().list();
    assertThat(variables).hasSize(1);
    assertThat(variables.get(0).getName()).isEqualTo("abc");
  }

  @Test
  void testStartMessageCorrelationWithTransientVariable() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("process")
      .startEvent()
        .message("message")
      .scriptTask("scriptTask")
        .scriptFormat("javascript")
        .operatonResultVariable("abc")
        .scriptText("execution.setVariable('abc', foo);")
      .endEvent()
      .done();

    testRule.deploy(instance);

    // when
    runtimeService.createMessageCorrelation("message")
      .setVariable("foo", Variables.stringValue("bar", true))
      .correlate();

    // then
    List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().list();
    assertThat(variableInstances).isEmpty();
    List<HistoricVariableInstance> historicInstances = historyService.createHistoricVariableInstanceQuery().list();
    assertThat(historicInstances).hasSize(1);
    assertThat(historicInstances.get(0).getName()).isEqualTo("abc");
    assertThat(historicInstances.get(0).getValue()).isEqualTo("bar");
  }

  @Test
  void testMessageCorrelationWithTransientVariable() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("process")
      .startEvent()
      .intermediateCatchEvent()
        .message("message")
      .scriptTask("scriptTask")
        .scriptFormat("javascript")
        .operatonResultVariable("abc")
        .scriptText("execution.setVariable('abc', blob);")
      .endEvent()
      .done();

    testRule.deploy(instance);
    runtimeService.startProcessInstanceByKey("process",
        Variables.createVariables().putValueTyped("foo", Variables.stringValue("foo", false)));

    // when
    VariableMap correlationKeys = Variables.createVariables().putValueTyped("foo", Variables.stringValue("foo", true));
    VariableMap variables = Variables.createVariables().putValueTyped("blob", Variables.stringValue("blob", true));
    runtimeService.correlateMessage("message", correlationKeys, variables);

    // then
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().singleResult();
    assertThat(variableInstance).isNull();
    HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
        .variableName("abc").singleResult();
    assertThat(historicVariableInstance).isNotNull();
    assertThat(historicVariableInstance.getValue()).isEqualTo("blob");
  }

  @Test
  void testParallelExecutions() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process")
      .startEvent()
      .parallelGateway()
      .scriptTask()
        .scriptFormat("javascript")
        .operatonResultVariable("abc")
        .scriptText("execution.setVariableLocal('abc', foo);")
      .endEvent()
      .moveToLastGateway()
      .scriptTask()
        .scriptFormat("javascript")
        .operatonResultVariable("abc")
        .scriptText("execution.setVariableLocal('abc', foo);")
      .endEvent()
      .done();

    testRule.deploy(instance);

    // when
    runtimeService.startProcessInstanceByKey("Process",
        Variables.createVariables().putValueTyped("foo", Variables.stringValue("bar", true)));

    // then
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().list();
    assertThat(variables).isEmpty();

    List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery().variableName("abc").list();
    assertThat(historicVariables).hasSize(2);
  }

  @Test
  void testExclusiveGateway() {
    // given
    testRule.deploy("org/operaton/bpm/engine/test/bpmn/gateway/ExclusiveGatewayTest.testDivergingExclusiveGateway.bpmn20.xml");

    // when
    runtimeService.startProcessInstanceByKey("exclusiveGwDiverging",
        Variables.createVariables().putValueTyped("input", Variables.integerValue(1, true)));

    // then
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().list();
    assertThat(variables).isEmpty();
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask1");
  }

  @Test
  void testChangeTransientVariable() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process")
      .startEvent()
      .serviceTask()
        .operatonClass(ChangeVariableTransientDelegate.class.getName())
      .userTask("user")
      .endEvent()
      .done();

    testRule.deploy(instance);

    String output = "transientVariableOutput";
    Map<String, Object> variables = new HashMap<>();
    variables.put(output, false);

    // when
    runtimeService.startProcessInstanceByKey("Process", variables);

    // then
    List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery().list();
    List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().list();
    assertThat(historicVariableInstances).hasSize(1);
    assertThat(variableInstances).hasSize(1);
    assertThat(variableInstances.get(0).getName()).isEqualTo(output);
    assertThat(variableInstances.get(0).getValue()).isEqualTo(OUTPUT_VALUE);
  }

  @Test
  void testSwitchTransientToNonVariable() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process")
      .startEvent()
      .serviceTask()
        .operatonClass(SwitchTransientVariableDelegate.class.getName())
      .userTask("user")
      .endEvent()
      .done();

    testRule.deploy(instance);


    Map<String, Object> variables = new HashMap<>();
    variables.put("transient1", true);
    variables.put("transient2", false);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("Process", variables))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot set transient variable with name 'variable' to non-transient variable and vice versa.");

  }

  @Test
  void testSwitchNonToTransientVariable() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process")
      .startEvent()
      .serviceTask()
        .operatonClass(SwitchTransientVariableDelegate.class.getName())
      .userTask("user")
      .endEvent()
      .done();

    testRule.deploy(instance);

    Map<String, Object> variables = new HashMap<>();
    variables.put("transient1", false);
    variables.put("transient2", true);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("Process", variables))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot set transient variable with name 'variable' to non-transient variable and vice versa.");
  }


  @Test
  void testSwitchNonToTransientLocalVariable() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process")
      .startEvent()
      .serviceTask()
        .operatonClass(SetTransientLocalVariableDelegate.class)
      .endEvent()
      .done();

    testRule.deploy(instance);

    Map<String, Object> variables = new HashMap<>();
    variables.put("var", false);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("Process", variables))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot set transient variable with name 'var' to non-transient variable and vice versa.");
  }

  /**
   * CAM-9932
   */
  @Test
  void testKeepTransientIfUntypedValueIsAccessed() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcess")
      .startEvent()
      .serviceTask()
        .operatonClass(ReadTypedTransientVariableDelegate.class)
      .userTask()
      .endEvent()
      .done();
    testRule.deploy(modelInstance);

    // when
    String processInstanceId = runtimeService.startProcessInstanceByKey("aProcess").getId();

    // then
    Object value = runtimeService.getVariable(processInstanceId, "var");
    assertThat(value).isNull();
  }


  @Test
  void testTransientLocalVariable() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask().operatonClass(SetTransientLocalVariableDelegate.class)
        .userTask()
        .endEvent()
        .done();

    testRule.deploy(model);

    // when
    runtimeService.startProcessInstanceByKey("process");

    // then
    long numVariables =
        runtimeService
          .createVariableInstanceQuery()
          .count();

    assertThat(numVariables).isZero();
  }

  @Test
  void shouldRemoveNonTransientAndSetNonTransient() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask().operatonClass(RemoveAndSetVariableDelegate.class)
        .userTask()
        .endEvent()
        .done();

    testRule.deploy(model);

    // when
    runtimeService.startProcessInstanceByKey("process",
        Variables.putValue("transient1", false).putValue("transient2", false));

    // then
    assertThat(runtimeService.createVariableInstanceQuery().variableName(VARIABLE_NAME).count()).isOne();
  }

  @Test
  void shouldRemoveTransientAndSetTransient() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask().operatonClass(RemoveAndSetVariableDelegate.class)
        .userTask()
        .endEvent()
        .done();

    testRule.deploy(model);

    // when
    runtimeService.startProcessInstanceByKey("process",
        Variables.putValue("transient1", true).putValue("transient2", true));

    // then
    assertThat(runtimeService.createVariableInstanceQuery().variableName(VARIABLE_NAME).count())
      .isZero();
  }

  @Test
  void shouldFailRemoveTransientAndSetNonTransient() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask().operatonClass(RemoveAndSetVariableDelegate.class)
        .userTask()
        .endEvent()
        .done();

    testRule.deploy(model);

    var variables = Variables.putValue("transient1", true).putValue("transient2", false);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process", variables))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot set transient variable with name 'variable' to non-transient variable and vice versa.");
  }

  @Test
  void shouldFailRemoveNonTransientAndSetTransient() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask().operatonClass(RemoveAndSetVariableDelegate.class)
        .userTask()
        .endEvent()
        .done();

    testRule.deploy(model);

    var variables = Variables.putValue("transient1", false).putValue("transient2", true);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process", variables))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot set transient variable with name 'variable' to non-transient variable and vice versa.");
  }

  public static class ReadTypedTransientVariableDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      execution.setVariable("var", Variables.untypedValue(Variables.objectValue("aString"), true));

      // when
      TypedValue typedValue = execution.getVariableTyped("var");

      // then
      assertThat(typedValue.isTransient()).isTrue();
    }
  }

  public static class SetVariableTransientDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
      execution.setVariable(VARIABLE_NAME, Variables.integerValue(1, true));
    }
  }

  public static class SetTransientLocalVariableDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      StringValue transientValue = Variables.stringValue("value", true);
      execution.setVariableLocal("var", transientValue);
    }
  }

  public static class ReadTransientVariablesOfAllTypesDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      for (char i = 'a'; i < 'm'; i++) {
        Object value = execution.getVariable(String.valueOf(i));
        // variable 'j' is a transient null
        if (i != 'j' ) {
          assertThat(value).isNotNull();
        } else {
          assertThat(value).isNull();
        }
      }
    }
  }

  public static class ReadTransientVariableExecutionListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) throws Exception {
      Object variable = execution.getVariable(VARIABLE_NAME);
      assertThat(variable).isNotNull();
    }
  }

  public static class ChangeVariableTransientDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
      execution.setVariable(VARIABLE_NAME, Variables.integerValue(1, true));
      execution.setVariable(VARIABLE_NAME, Variables.integerValue(OUTPUT_VALUE, true));
      execution.setVariable("transientVariableOutput", execution.getVariable(VARIABLE_NAME));
    }
  }

  public static class SwitchTransientVariableDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
      Boolean transient1 = (Boolean) execution.getVariable("transient1");
      Boolean transient2 = (Boolean) execution.getVariable("transient2");
      execution.setVariable(VARIABLE_NAME, Variables.integerValue(1, transient1));
      execution.setVariable(VARIABLE_NAME, Variables.integerValue(2, transient2));
    }
  }

  public static class RemoveAndSetVariableDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
      Boolean transient1 = (Boolean) execution.getVariable("transient1");
      Boolean transient2 = (Boolean) execution.getVariable("transient2");
      execution.setVariable(VARIABLE_NAME, Variables.integerValue(1, transient1));
      execution.removeVariable(VARIABLE_NAME);
      execution.setVariable(VARIABLE_NAME, Variables.integerValue(2, transient2));
    }
  }

}
