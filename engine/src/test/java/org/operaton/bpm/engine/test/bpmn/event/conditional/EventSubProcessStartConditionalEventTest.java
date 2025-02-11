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
package org.operaton.bpm.engine.test.bpmn.event.conditional;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.SuspendedEntityInteractionException;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Test;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public class EventSubProcessStartConditionalEventTest extends AbstractConditionalEventTestCase {

  @Test
  @Deployment
  public void testTrueCondition() {
    //given process with event sub process conditional start event

    //when process instance is started with true condition
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    //then event sub process is triggered via default evaluation behavior
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(0);
  }

  @Test
  @Deployment
  public void testFalseCondition() {
    //given process with event sub process conditional start event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when variable is set on task with condition
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution stays at user task
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_BEFORE_CONDITION);
  }

  @Test
  @Deployment
  public void testVariableCondition() {
    //given process with event sub process conditional start event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when variable is set on task with condition
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional start event
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  @Deployment(resources ={ "org/operaton/bpm/engine/test/bpmn/event/conditional/EventSubProcessStartConditionalEventTest.testVariableCondition.bpmn20.xml"})
  public void testVariableConditionAndStartingWithVar() {
    //given process with event sub process conditional start event
    Map<String, Object> vars = Variables.createVariables();
    vars.put(VARIABLE_NAME, 1);

    //when starting process with variable
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY, vars);

    //then event sub process is triggered via default evaluation behavior
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(0);
  }

  @Test
  @Deployment(resources ={ "org/operaton/bpm/engine/test/bpmn/event/conditional/EventSubProcessStartConditionalEventTest.testVariableCondition.bpmn20.xml"})
  public void testWrongVariableCondition() {
    //given process with event sub process conditional start event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);

    //when variable is set on task with condition
    taskService.setVariable(task.getId(), VARIABLE_NAME+1, 1);

    //then execution stays at user task
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_BEFORE_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);
  }

  @Test
  @Deployment
  public void testNonInterruptingVariableCondition() {
    //given process with event sub process conditional start event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);

    //when variable is set on task with condition
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional start event
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(2);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);
  }

  @Test
  @Deployment
  public void testSubProcessVariableCondition() {
    //given process with event sub process conditional start event and user task in sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when local variable is set on task with condition
    taskService.setVariableLocal(task.getId(), VARIABLE_NAME, 1);

    //then execution stays at user task
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_BEFORE_CONDITION);
  }

  @Test
  @Deployment(resources ={ "org/operaton/bpm/engine/test/bpmn/event/conditional/EventSubProcessStartConditionalEventTest.testSubProcessVariableCondition.bpmn20.xml"})
  public void testSubProcessSetVariableOnTaskCondition() {
    //given process with event sub process conditional start event and user task in sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when variable is set on task, variable is propagated to process instance
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional start event
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  @Deployment(resources ={ "org/operaton/bpm/engine/test/bpmn/event/conditional/EventSubProcessStartConditionalEventTest.testSubProcessVariableCondition.bpmn20.xml"})
  public void testSubProcessSetVariableOnExecutionCondition() {
    //given process with event sub process conditional start event and user task in sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when local variable is set on task execution
    runtimeService.setVariableLocal(task.getExecutionId(), VARIABLE_NAME, 1);

    //then execution stays at user task
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_BEFORE_CONDITION);
  }


  protected void deployConditionalEventSubProcess(BpmnModelInstance model, boolean isInterrupting) {
    deployConditionalEventSubProcess(model, CONDITIONAL_EVENT_PROCESS_KEY, isInterrupting);
  }

  @Override
  protected void deployConditionalEventSubProcess(BpmnModelInstance model, String parentId, boolean isInterrupting) {

    final BpmnModelInstance modelInstance = modify(model)
            .addSubProcessTo(parentId)
            .id("eventSubProcess")
            .triggerByEvent()
            .embeddedSubProcess()
            .startEvent()
            .interrupting(isInterrupting)
            .conditionalEventDefinition(CONDITIONAL_EVENT)
            .condition(CONDITION_EXPR)
            .conditionalEventDefinitionDone()
            .userTask("taskAfterCond")
            .name(TASK_AFTER_CONDITION)
            .endEvent().done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());
  }

  @Test
  public void testSetVariableInDelegate() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask()
                                                    .operatonClass(SetVariableDelegate.class.getName())
                                                  .endEvent().done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, true);

    // given process with event sub process conditional start event and service task with delegate class which sets a variable
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);

    //when task is completed
    taskService.complete(task.getId());

    //then service task with delegated code is called and variable is set
    //-> conditional event is triggered and execution stays at user task after condition
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(0);
  }

  @Test
  public void testNonInterruptingSetVariableInDelegate() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask()
                                                    .operatonClass(SetVariableDelegate.class.getName())
                                                  .userTask()
                                                  .endEvent().done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);

    // given process with event sub process conditional start event and service task with delegate class which sets a variable
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);

    //when task before service task is completed
    taskService.complete(task.getId());

    //then service task with delegated code is called and variable is set
    //-> non interrupting conditional event is triggered
    //execution stays at user task after condition and after service task
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(2);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);
  }

  @Test
  public void testSetVariableInDelegateWithSynchronousEvent() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask().name(TASK_BEFORE_CONDITION)
      .serviceTask().operatonClass(SetVariableDelegate.class.getName())
      .endEvent().done();

    modelInstance = modify(modelInstance)
      .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
      .triggerByEvent()
      .embeddedSubProcess()
      .startEvent()
      .interrupting(true)
      .conditionalEventDefinition(CONDITIONAL_EVENT)
      .condition(CONDITION_EXPR)
      .conditionalEventDefinitionDone()
      .endEvent().done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();

    //when task is completed
    taskService.complete(task.getId());

    //then service task with delegated code is called and variable is set
    //-> conditional event is triggered and process instance ends
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(0);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(0);
    assertNull(runtimeService.createProcessInstanceQuery().singleResult());
  }

  @Test
  public void testNonInterruptingSetVariableInDelegateWithSynchronousEvent() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent().userTask().name(TASK_BEFORE_CONDITION)
      .serviceTask()
      .operatonClass(SetVariableDelegate.class.getName())
      .userTask()
      .endEvent().done();

    modelInstance = modify(modelInstance)
      .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
      .triggerByEvent()
      .embeddedSubProcess()
      .startEvent()
      .interrupting(false)
      .conditionalEventDefinition(CONDITIONAL_EVENT)
      .condition(CONDITION_EXPR)
      .conditionalEventDefinitionDone()
      .endEvent().done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given process with event sub process conditional start event and service task with delegate class which sets a variable
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);

    //when task before service task is completed
    taskService.complete(task.getId());

    //then service task with delegated code is called and variable is set
    //-> non interrupting conditional event is triggered
    //execution stays at user task after service task
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(1);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);
  }

  @Test
  public void testSetVariableInInputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .operatonInputParameter(VARIABLE_NAME, "1")
                                                    .operatonExpression(TRUE_CONDITION)
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent().done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then service task with input mapping is called and variable is set
    //-> interrupting conditional event is not triggered
    //since variable is only locally
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_SERVICE_TASK);
  }

  @Test
  public void testNonInterruptingSetVariableInInputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .operatonInputParameter(VARIABLE_NAME, "1")
                                                    .operatonExpression(TRUE_CONDITION)
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent().done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task before service task is completed
    taskService.complete(task.getId());

    //then service task with input mapping is called and variable is set
    //-> non interrupting conditional event is not triggered
    //since variable is only locally
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_SERVICE_TASK);
  }

  @Test
  public void testSetVariableInExpression() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .operatonExpression("${execution.setVariable(\"variable\", 1)}")
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent().done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then service task with expression is called and variable is set
    //-> interrupting conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  public void testNonInterruptingSetVariableInExpression() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .operatonExpression("${execution.setVariable(\"variable\", 1)}")
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent().done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task before service task is completed
    taskService.complete(task.getId());

    //then service task with expression is called and variable is set
    //-> non interrupting conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(2);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);
  }

  @Test
  public void testSetVariableInInputMappingOfSubProcess() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUB_PROCESS_ID)
                                                    .operatonInputParameter(VARIABLE_NAME, "1")
                                                    .embeddedSubProcess()
                                                    .startEvent("startSubProcess")
                                                    .userTask().name(TASK_IN_SUB_PROCESS_ID)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
     deployConditionalEventSubProcess(modelInstance, SUB_PROCESS_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then input mapping from sub process sets variable
    //-> interrupting conditional event is triggered by default evaluation behavior
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  public void testNonInterruptingSetVariableInInputMappingOfSubProcess() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUB_PROCESS_ID)
                                                    .operatonInputParameter(VARIABLE_NAME, "1")
                                                    .embeddedSubProcess()
                                                    .startEvent()
                                                    .userTask().name(TASK_IN_SUB_PROCESS_ID)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
     deployConditionalEventSubProcess(modelInstance, SUB_PROCESS_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task before service task is completed
    taskService.complete(task.getId());

    //then input mapping from sub process sets variable
    //-> non interrupting conditional event is triggered via default evaluation behavior
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(2);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);
  }

  @Test
  public void testSetVariableInOutputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                    .operatonOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask()
                                                  .endEvent()
                                                  .done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then output mapping from user task sets variable
    //-> interrupting conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  public void testNonInterruptingSetVariableInOutputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                    .operatonOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask()
                                                  .endEvent()
                                                  .done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then output mapping from user task sets variable
    //-> non interrupting conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(2);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);
  }

  @Test
  public void testSetVariableInOutputMappingOfCallActivity() {
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, DELEGATED_PROCESS).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .callActivity(TASK_WITH_CONDITION_ID)
                                                    .calledElement(DELEGATED_PROCESS_KEY)
                                                    .operatonOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask()
                                                  .endEvent()
                                                  .done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then output mapping from call activity sets variable
    //-> interrupting conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  public void testNonInterruptingSetVariableInOutputMappingOfCallActivity() {
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, DELEGATED_PROCESS).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .callActivity(TASK_WITH_CONDITION_ID)
                                                    .calledElement(DELEGATED_PROCESS_KEY)
                                                    .operatonOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask()
                                                  .endEvent()
                                                  .done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);


    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then output mapping from call activity sets variable
    //-> non interrupting conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(2);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);
  }

  @Test
  public void testSetVariableInOutMappingOfCallActivity() {
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, DELEGATED_PROCESS).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .callActivity(TASK_WITH_CONDITION_ID)
      .calledElement(DELEGATED_PROCESS_KEY)
      .operatonOut(VARIABLE_NAME, VARIABLE_NAME)
      .userTask().name(TASK_AFTER_OUTPUT_MAPPING)
      .endEvent()
      .done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then out mapping from call activity sets variable
    //-> interrupting conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  public void testNonInterruptingSetVariableInOutMappingOfCallActivity() {
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, DELEGATED_PROCESS).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .callActivity(TASK_WITH_CONDITION_ID)
      .calledElement(DELEGATED_PROCESS_KEY)
      .operatonOut(VARIABLE_NAME, VARIABLE_NAME)
      .userTask().name(TASK_AFTER_OUTPUT_MAPPING)
      .endEvent()
      .done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);


    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task before service task is completed
    taskService.complete(task.getId());

    //then out mapping of call activity sets a variable
    //-> non interrupting conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(2);
    assertThat(conditionEventSubscriptionQuery.count()).isEqualTo(1);
  }


  @Test
  public void testSetVariableInInMappingOfCallActivity() {
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, DELEGATED_PROCESS).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .callActivity(TASK_WITH_CONDITION_ID)
      .calledElement(DELEGATED_PROCESS_KEY)
      .operatonIn(VARIABLE_NAME, VARIABLE_NAME)
      .userTask().name(TASK_AFTER_OUTPUT_MAPPING)
      .endEvent()
      .done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then in mapping from call activity sets variable
    //-> interrupting conditional event is not triggered, since variable is only locally
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_OUTPUT_MAPPING);
  }

  @Test
  public void testNonInterruptingSetVariableInInMappingOfCallActivity() {
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, DELEGATED_PROCESS).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .callActivity(TASK_WITH_CONDITION_ID)
      .calledElement(DELEGATED_PROCESS_KEY)
      .operatonIn(VARIABLE_NAME, VARIABLE_NAME)
      .userTask().name(TASK_AFTER_OUTPUT_MAPPING)
      .endEvent()
      .done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then in mapping from call activity sets variable
    //-> interrupting conditional event is not triggered, since variable is only locally
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_OUTPUT_MAPPING);
  }

  @Test
  public void testSetVariableInCallActivity() {
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, DELEGATED_PROCESS).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .callActivity(TASK_WITH_CONDITION_ID)
                                                    .calledElement(DELEGATED_PROCESS_KEY)
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent()
                                                  .done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then service task in call activity sets variable
    //conditional event is not triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_SERVICE_TASK);
  }

  @Test
  public void testNonInterruptingSetVariableInCallActivity() {
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, DELEGATED_PROCESS).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .callActivity(TASK_WITH_CONDITION_ID)
                                                    .calledElement(DELEGATED_PROCESS_KEY)
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent()
                                                  .done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then service task in call activity sets variable
    //conditional event is not triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_SERVICE_TASK);
  }

  @Test
  public void testSetVariableInSubProcessInDelegatedCode() {

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUB_PROCESS_ID)
                                                  .embeddedSubProcess()
                                                    .startEvent()
                                                    .serviceTask()
                                                    .operatonExpression(EXPR_SET_VARIABLE)
                                                    .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
     deployConditionalEventSubProcess(modelInstance, SUB_PROCESS_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then service task in sub process sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  public void testNonInterruptingSetVariableInSubProcessInDelegatedCode() {

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUB_PROCESS_ID)
                                                  .embeddedSubProcess()
                                                    .startEvent()
                                                    .serviceTask()
                                                    .operatonExpression(EXPR_SET_VARIABLE)
                                                    .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
     deployConditionalEventSubProcess(modelInstance, SUB_PROCESS_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then service task in sub process sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(2);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);
  }

  @Test
  public void testSetVariableInSubProcessInDelegatedCodeConditionOnPI() {

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUB_PROCESS_ID)
                                                  .embeddedSubProcess()
                                                    .startEvent()
                                                    .serviceTask()
                                                    .operatonExpression(EXPR_SET_VARIABLE)
                                                    .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then service task in sub process sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  public void testNonInterruptingSetVariableInSubProcessInDelegatedCodeConditionOnPI() {

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUB_PROCESS_ID)
                                                  .embeddedSubProcess()
                                                    .startEvent()
                                                    .serviceTask()
                                                    .operatonExpression(EXPR_SET_VARIABLE)
                                                    .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then service task in sub process sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(2);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);
  }

  @Test
  @Deployment(resources ={ "org/operaton/bpm/engine/test/bpmn/event/conditional/EventSubProcessStartConditionalEventTest.testSubProcessVariableCondition.bpmn20.xml"})
  public void testSubProcessSetVariableOnProcessInstanceCondition() {
    //given process with event sub process conditional start event and user task in sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when variable is set on process instance
    runtimeService.setVariable(procInst.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional start event
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  public void testSuspendedProcess() {

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent().done();

     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, true);

    // given suspended process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    var processInstanceId = procInst.getId();
    runtimeService.suspendProcessInstanceById(procInst.getId());

    //when wrong variable is set
    runtimeService.setVariable(processInstanceId, VARIABLE_NAME+1, 1);

    //then nothing happens
    assertTrue(runtimeService.createProcessInstanceQuery().singleResult().isSuspended());

    //when variable which triggers condition is set
    //then exception is expected
    try {
      runtimeService.setVariable(processInstanceId, VARIABLE_NAME, 1);
      fail("Should fail!");
    } catch (SuspendedEntityInteractionException seie) {
      //expected
    }
    runtimeService.activateProcessInstanceById(processInstanceId);
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
  }

  @Test
  public void testNonInterruptingConditionalSuspendedProcess() {

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent().done();


     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);

    // given suspended process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    var processInstanceId = procInst.getId();
    runtimeService.suspendProcessInstanceById(procInst.getId());

    //when wrong variable is set
    runtimeService.setVariable(processInstanceId, VARIABLE_NAME+1, 1);

    //then nothing happens
    assertTrue(runtimeService.createProcessInstanceQuery().singleResult().isSuspended());

    //when variable which triggers condition is set
    //then exception is expected
    try {
      runtimeService.setVariable(processInstanceId, VARIABLE_NAME, 1);
      fail("Should fail!");
    } catch (SuspendedEntityInteractionException seie) {
      //expected
    }
    runtimeService.activateProcessInstanceById(processInstanceId);
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
  }

  @Test
  public void testNonInterruptingSetMultipleVariables() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
      .endEvent().done();
     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);

    //given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    Task task = taskQuery.singleResult();

    //when multiple variable are set on task execution
    VariableMap variables = Variables.createVariables();
    variables.put("variable", 1);
    variables.put("variable1", 1);
    runtimeService.setVariables(task.getExecutionId(), variables);

    //then event sub process should be triggered more than once
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(3);
  }

  @Test
  @Deployment
  public void testLoop() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    Task task = taskService.createTaskQuery().taskDefinitionKey("Task_1").singleResult();

    // when
    taskService.complete(task.getId());

    //then process instance will be in endless loop
    //to end the instance we have a conditional branch in the java delegate
    //after 3 instantiations the variable will be set to the instantiation count
    //execution stays in task 2
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(1);
    assertThat(tasksAfterVariableIsSet.get(0).getTaskDefinitionKey()).isEqualTo("Task_2");
    assertThat(runtimeService.getVariable(processInstance.getId(), VARIABLE_NAME)).isEqualTo(3);
  }

  @Test
  public void testTriggerAnotherEventSubprocess() {
    //given process with user task
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
      .endEvent().done();

    //and event sub process with true condition
    modelInstance = modify(modelInstance)
      .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
      .triggerByEvent()
      .embeddedSubProcess()
        .startEvent()
        .interrupting(true)
        .conditionalEventDefinition()
          .condition(TRUE_CONDITION)
        .conditionalEventDefinitionDone()
        .userTask(TASK_AFTER_CONDITION_ID + 1)
          .name(TASK_AFTER_CONDITION + 1)
        .endEvent()
      .done();
    //a second event sub process
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);

    //when
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());

    //then first event sub process is on starting of process instance triggered
    Task task = taskQuery.singleResult();
    assertThat(task.getName()).isEqualTo(TASK_AFTER_CONDITION + 1);

    //when variable is set, second condition becomes true -> but since first event sub process has
    // interrupt the process instance the second event sub process can't be triggered
    runtimeService.setVariable(processInstance.getId(), VARIABLE_NAME, 1);
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(1);
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION + 1);
  }

  @Test
  public void testNonInterruptingTriggerAnotherEventSubprocess() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
      .endEvent().done();

    //first event sub process
    modelInstance = modify(modelInstance)
      .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
      .id("eventSubProcess1")
      .triggerByEvent()
      .embeddedSubProcess()
      .startEvent()
      .interrupting(false)
      .conditionalEventDefinition()
      .condition(TRUE_CONDITION)
      .conditionalEventDefinitionDone()
      .userTask("taskAfterCond1")
      .name(TASK_AFTER_CONDITION + 1)
      .endEvent().done();

     deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);

    //given process with two event sub processes

    //when process is started
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());

    //then first event sub process is triggered because condition is true
    Task task = taskQuery.taskName(TASK_AFTER_CONDITION + 1).singleResult();
    assertNotNull(task);
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    //when variable is set, second condition becomes true -> second event sub process is triggered
    runtimeService.setVariable(processInstance.getId(), "variable", 1);
    task = taskService.createTaskQuery().taskName(TASK_AFTER_CONDITION).singleResult();
    assertNotNull(task);
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(4);
  }

  @Test
  @Deployment
  public void testNonInterruptingSetMultipleVariableInDelegate()
  {
    // when
    runtimeService.startProcessInstanceByKey("process");

    // then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(5);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("Task_3").count()).isEqualTo(3);
  }

  @Test
  public void testSetVariableInTriggeredEventSubProcess() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
      .serviceTask()
      .operatonClass(SetVariableDelegate.class.getName())
      .endEvent()
      .done();

    modelInstance = modify(modelInstance)
      .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
      .triggerByEvent()
      .embeddedSubProcess()
      .startEvent()
      .interrupting(true)
      .conditionalEventDefinition(CONDITIONAL_EVENT)
      .condition(CONDITION_EXPR)
      .conditionalEventDefinitionDone()
      .serviceTask()
      .operatonClass(LoopDelegate.class.getName())
      .userTask().name(TASK_AFTER_CONDITION)
      .endEvent().done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());


    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstance.getId());
    Task task = taskQuery.singleResult();
    assertThat(task.getName()).isEqualTo(TASK_WITH_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then variable is set
    //event sub process is triggered
    //and service task in event sub process triggers again sub process
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(1);
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/conditional/EventSubProcessStartConditionalEventTest.testVariableCondition.bpmn20.xml")
  public void testVariableConditionWithHistory() {
    // given process with event sub process conditional start event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY,
        Variables.createVariables()
        .putValue(VARIABLE_NAME, 1)
        .putValue("donotloseme", "here"));

    // assume
    tasksAfterVariableIsSet = taskService.createTaskQuery().processInstanceId(procInst.getId()).list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(1);

    // then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName(VARIABLE_NAME).count()).isEqualTo(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("donotloseme").count()).isEqualTo(1);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment
  public void testNonInterruptingVariableConditionWithHistory() {
    // given process with event sub process conditional start event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY,
        Variables.createVariables()
        .putValue(VARIABLE_NAME, 1)
        .putValue("donotloseme", "here"));

    // assume
    tasksAfterVariableIsSet = taskService.createTaskQuery().processInstanceId(procInst.getId()).list();
    assertThat(tasksAfterVariableIsSet.size()).isEqualTo(2);
    assertThat(conditionEventSubscriptionQuery.list().size()).isEqualTo(1);

    // then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName(VARIABLE_NAME).count()).isEqualTo(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("donotloseme").count()).isEqualTo(1);
  }
}
