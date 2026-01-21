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
package org.operaton.bpm.engine.test.bpmn.event.conditional;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.SuspendedEntityInteractionException;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.AbstractActivityBuilder;
import org.operaton.bpm.model.bpmn.instance.SequenceFlow;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonExecutionListener;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
class BoundaryConditionalEventTest extends AbstractConditionalEventTestCase {

  @Test
  @Deployment
  void testTrueCondition() {
    //given process with boundary conditional event

    //when process is started and execution arrives user task with boundary event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    //then default evaluation behavior triggers boundary event
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list()).isEmpty();
  }

  @Test
  @Deployment
  void testNonInterruptingTrueCondition() {
    //given process with boundary conditional event

    //when process is started and execution arrives activity with boundary event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    //then default evaluation behavior triggers conditional event
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);
  }

  @Test
  @Deployment
  void testFalseCondition() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery();
    Task task = taskQuery.processInstanceId(procInst.getId()).singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_WITH_CONDITION);

    //when variable is set on task execution
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution stays in task with boundary condition
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_WITH_CONDITION);
  }

  @Test
  @Deployment
  void testVariableCondition() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_WITH_CONDITION);

    //when local variable is set on task with condition
    taskService.setVariableLocal(task.getId(), VARIABLE_NAME, 1);

    //then execution should remain on task
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_WITH_CONDITION);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testVariableCondition.bpmn20.xml"})
  void testVariableSetOnExecutionCondition() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_WITH_CONDITION);

    //when variable is set on task execution
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution ends
    Execution execution = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .activityId(TASK_WITH_CONDITION_ID)
            .singleResult();
    assertThat(execution).isNull();

    //and execution is at user task after boundary event
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  @Deployment
  void testNonInterruptingVariableCondition() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_WITH_CONDITION);

    //when variable is set on task with condition
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after boundary event and in the task with the boundary event
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testVariableCondition.bpmn20.xml"})
  void testWrongVariableCondition() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_WITH_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);

    //when wrong variable is set on task execution
    taskService.setVariable(task.getId(), VARIABLE_NAME + 1, 1);

    //then execution stays at user task with condition
    task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_WITH_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);

    //when correct variable is set
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is on user task after condition
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  @Deployment
  void testParallelVariableCondition() {
    //given process with parallel user tasks and boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    List<Task> tasks = taskQuery.list();
    assertThat(tasks).hasSize(2);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(2);

    Task task = tasks.get(0);

    //when local variable is set on task
    taskService.setVariableLocal(task.getId(), VARIABLE_NAME, 1);

    //then nothing happens
    tasks = taskQuery.list();
    assertThat(tasks).hasSize(2);

    //when local variable is set on task execution
    runtimeService.setVariableLocal(task.getExecutionId(), VARIABLE_NAME, 1);

    //then boundary event is triggered of this task and task ends (subscription is deleted)
    //other execution stays in other task
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(1);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testParallelVariableCondition.bpmn20.xml"})
  void testParallelSetVariableOnTaskCondition() {
    //given process with parallel user tasks and boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    List<Task> tasks = taskQuery.list();
    assertThat(tasks).hasSize(2);

    Task task = tasks.get(0);

    //when variable is set on execution
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then both boundary event are triggered and process instance ends
    List<Execution> executions = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .list();
    assertThat(executions).isEmpty();

    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testParallelVariableCondition.bpmn20.xml"})
  void testParallelSetVariableOnExecutionCondition() {
    //given process with parallel user tasks and boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    List<Task> tasks = taskQuery.list();
    assertThat(tasks).hasSize(2);

    //when variable is set on execution
    runtimeService.setVariable(procInst.getId(), VARIABLE_NAME, 1);

    //then both boundary events are triggered and process instance ends
    List<Execution> executions = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .list();
    assertThat(executions).isEmpty();

    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).isEmpty();
  }

  @Test
  @Deployment
  void testSubProcessVariableCondition() {
    //given process with boundary conditional event on sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_IN_SUB_PROCESS);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);

    //when local variable is set on task with condition
    taskService.setVariableLocal(task.getId(), VARIABLE_NAME, 1);

    //then execution stays on user task
    List<Execution> executions = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .list();
    assertThat(executions).hasSize(2);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);

    //when local variable is set on task execution
    runtimeService.setVariableLocal(task.getExecutionId(), VARIABLE_NAME, 1);

    //then process instance ends
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testSubProcessVariableCondition.bpmn20.xml"})
  void testSubProcessSetVariableOnTaskCondition() {
    //given process with boundary conditional event on sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_IN_SUB_PROCESS);

    //when variable is set on task execution with condition
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then process instance ends
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testSubProcessVariableCondition.bpmn20.xml"})
  void testSubProcessSetVariableOnExecutionCondition() {
    //given process with boundary conditional event on sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_IN_SUB_PROCESS);

    //when variable is set on task execution with condition
    runtimeService.setVariable(task.getExecutionId(), VARIABLE_NAME, 1);

    //then process instance ends
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).isEmpty();
  }

  @Test
  @Deployment
  void testNonInterruptingSubProcessVariableCondition() {
    //given process with boundary conditional event on sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_IN_SUB_PROCESS);

    //when variable is set on task with condition
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution stays on user task and at task after condition
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
  }

  @Test
  @Deployment
  void testCleanUpConditionalEventSubscriptions() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();

    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);

    //when task is completed
    taskService.complete(task.getId());

    //then conditional subscription should be deleted
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(1);
    assertThat(conditionEventSubscriptionQuery.list()).isEmpty();
  }

  protected void deployBoundaryEventProcess(AbstractActivityBuilder builder, boolean isInterrupting) {
    deployBoundaryEventProcess(builder, CONDITION_EXPR, isInterrupting);
  }

  protected void deployBoundaryEventProcess(AbstractActivityBuilder builder, String conditionExpr, boolean isInterrupting) {
    final BpmnModelInstance modelInstance = builder
            .boundaryEvent()
            .cancelActivity(isInterrupting)
            .conditionalEventDefinition(CONDITIONAL_EVENT)
            .condition(conditionExpr)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent()
            .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());
  }

  @Test
  void testSetVariableInDelegate() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .operatonClass(SetVariableDelegate.class.getName())
                                                  .endEvent().done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then service task with delegated code is called and variable is set
    //-> conditional event is triggered and execution stays at user task after condition
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list()).isEmpty();
  }

  @Test
  void testNonInterruptingSetVariableInDelegate() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .operatonClass(SetVariableDelegate.class.getName())
                                                  .userTask()
                                                  .endEvent().done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task before service task is completed
    taskService.complete(task.getId());

    //then service task with delegated code is called and variable is set
    //-> non interrupting conditional event is triggered
    //execution stays at user task after condition and after service task
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
  }

  @Test
  void testSetVariableInDelegateWithSynchronousEvent() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask().name(TASK_BEFORE_CONDITION)
      .serviceTask(TASK_WITH_CONDITION_ID).operatonClass(SetVariableDelegate.class.getName())
      .endEvent().done();

    modelInstance = modify(modelInstance)
      .serviceTaskBuilder(TASK_WITH_CONDITION_ID)
      .boundaryEvent()
      .cancelActivity(true)
      .conditionalEventDefinition(CONDITIONAL_EVENT)
      .condition(CONDITION_EXPR)
      .conditionalEventDefinitionDone()
      .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());

    //when task is completed
    taskService.complete(taskQuery.singleResult().getId());

    //then service task with delegated code is called and variable is set
    //-> conditional event is triggered and process instance ends
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).isEmpty();
    assertThat(conditionEventSubscriptionQuery.list()).isEmpty();
    assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNull();
  }

  @Test
  void testNonInterruptingSetVariableInDelegateWithSynchronousEvent() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask().name(TASK_BEFORE_CONDITION)
      .serviceTask(TASK_WITH_CONDITION_ID)
      .operatonClass(SetVariableDelegate.class.getName())
      .userTask()
      .endEvent().done();

    modelInstance = modify(modelInstance)
      .serviceTaskBuilder(TASK_WITH_CONDITION_ID)
      .boundaryEvent()
      .cancelActivity(false)
      .conditionalEventDefinition(CONDITIONAL_EVENT)
      .condition(CONDITION_EXPR)
      .conditionalEventDefinitionDone()
      .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given process with event sub process conditional start event and service task with delegate class which sets a variable
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());

    //when task before service task is completed
    taskService.complete(taskQuery.singleResult().getId());

    //then service task with delegated code is called and variable is set
    //-> non interrupting conditional event is triggered
    //execution stays at user task after service task
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(1);
    assertThat(conditionEventSubscriptionQuery.list()).isEmpty();
  }

  @Test
  void testSetVariableInInputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .operatonInputParameter(VARIABLE_NAME, "1")
                                                    .operatonExpression(TRUE_CONDITION)
                                                  .endEvent().done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    // input mapping does trigger boundary event with help of default evaluation behavior and process ends regularly
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  void testNonInterruptingSetVariableInInputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .operatonInputParameter(VARIABLE_NAME, "1")
                                                    .operatonExpression(TRUE_CONDITION)
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent().done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task before service task is completed
    taskService.complete(task.getId());

    // then the variable is set in an input mapping
    // -> non interrupting conditional event is not triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_SERVICE_TASK);
  }

  @Test
  void testSetVariableInExpression() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .operatonExpression(EXPR_SET_VARIABLE)
                                                  .endEvent().done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then service task with expression is called and variable is set
    //-> interrupting conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  void testNonInterruptingSetVariableInExpression() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .operatonExpression(EXPR_SET_VARIABLE)
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent().done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task before service task is completed
    taskService.complete(task.getId());

    //then service task with expression is called and variable is set
    //->non interrupting conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
  }

  @Test
  void testSetVariableInInputMappingOfSubProcess() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask().name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUB_PROCESS_ID)
                                                    .operatonInputParameter(VARIABLE_NAME, "1")
                                                    .embeddedSubProcess()
                                                    .startEvent()
                                                    .userTask().name(TASK_IN_SUB_PROCESS_ID)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
    deployConditionalBoundaryEventProcess(modelInstance, SUB_PROCESS_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    // Then input mapping from sub process sets variable,
    // interrupting conditional event is triggered by default evaluation behavior
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list()).isEmpty();
  }


  @Test
  void testNonInterruptingSetVariableInInputMappingOfSubProcess() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask().name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUB_PROCESS_ID)
                                                    .operatonInputParameter(VARIABLE_NAME, "1")
                                                    .embeddedSubProcess()
                                                    .startEvent()
                                                    .userTask().name(TASK_IN_SUB_PROCESS_ID)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
    deployConditionalBoundaryEventProcess(modelInstance, SUB_PROCESS_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task before is completed
    taskService.complete(task.getId());

    // Then input mapping from sub process sets variable, but
    // non interrupting conditional event is not triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_IN_SUB_PROCESS_ID);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);
  }

  @Test
  void testSetVariableInStartListenerOfSubProcess() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask().name(TASK_BEFORE_CONDITION)
      .subProcess(SUB_PROCESS_ID)
      .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_START, EXPR_SET_VARIABLE)
      .embeddedSubProcess()
      .startEvent()
      .userTask().name(TASK_IN_SUB_PROCESS_ID)
      .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    deployConditionalBoundaryEventProcess(modelInstance, SUB_PROCESS_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    // Then start listener from sub process sets variable,
    // interrupting conditional event is triggered by default evaluation behavior
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list()).isEmpty();
  }

  @Test
  void testNonInterruptingSetVariableInStartListenerOfSubProcess() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask().name(TASK_BEFORE_CONDITION)
      .subProcess(SUB_PROCESS_ID)
      .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_START, EXPR_SET_VARIABLE)
      .embeddedSubProcess()
      .startEvent()
      .userTask().name(TASK_IN_SUB_PROCESS_ID)
      .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    deployConditionalBoundaryEventProcess(modelInstance, SUB_PROCESS_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task before is completed
    taskService.complete(task.getId());

    // Then start listener from sub process sets variable,
    // non interrupting conditional event is triggered by default evaluation behavior
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);
  }

  @Test
  void testSetVariableInOutputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                    .operatonOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask().name(TASK_AFTER_OUTPUT_MAPPING)
                                                  .endEvent()
                                                  .done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_BEFORE_CONDITION_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then output mapping sets variable
    //boundary event is not triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_OUTPUT_MAPPING);
  }

  @Test
  void testSetVariableInOutputMappingWithBoundary() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                    .operatonOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
                                                  .endEvent()
                                                  .done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then output mapping sets variable
    //boundary event is triggered by default evaluation behavior
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  void testNonInterruptingSetVariableInOutputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                    .operatonOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
                                                  .endEvent()
                                                  .done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task with output mapping is completed
    taskService.complete(task.getId());

    //then output mapping sets variable
    //boundary event is triggered from default evaluation behavior
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
  }

  @Test
  void testNonInterruptingSetVariableInOutputMappingWithBoundary() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_WITH_CONDITION_ID)
                                                    .name(TASK_WITH_CONDITION)
                                                    .operatonOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask().name(TASK_AFTER_OUTPUT_MAPPING)
                                                  .endEvent()
                                                  .done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_WITH_CONDITION);

    //when task with output mapping is completed
    taskService.complete(task.getId());

    //then output mapping sets variable
    //boundary event is not triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_OUTPUT_MAPPING);
  }

  @Test
  void testSetVariableInOutputMappingOfCallActivity() {
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, DELEGATED_PROCESS).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .callActivity(TASK_WITH_CONDITION_ID)
                                                    .calledElement(DELEGATED_PROCESS_KEY)
                                                    .operatonOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask().name(TASK_AFTER_OUTPUT_MAPPING)
                                                  .endEvent()
                                                  .done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then output mapping from call activity sets variable
    //-> interrupting conditional event is not triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_OUTPUT_MAPPING);
  }

  @Test
  void testNonInterruptingSetVariableInOutputMappingOfCallActivity() {
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, DELEGATED_PROCESS).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .callActivity(TASK_WITH_CONDITION_ID)
                                                    .calledElement(DELEGATED_PROCESS_KEY)
                                                    .operatonOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask().name(TASK_AFTER_OUTPUT_MAPPING)
                                                  .endEvent()
                                                  .done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task before service task is completed
    taskService.complete(task.getId());

    //then out mapping of call activity sets a variable
    //-> non interrupting conditional event is not triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_OUTPUT_MAPPING);
  }

  @Test
  void testSetVariableInOutMappingOfCallActivity() {
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
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then out mapping from call activity sets variable
    //-> interrupting conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  void testNonInterruptingSetVariableInOutMappingOfCallActivity() {
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
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task before service task is completed
    taskService.complete(task.getId());

    //then out mapping of call activity sets a variable
    //-> non interrupting conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
    assertThat(conditionEventSubscriptionQuery.count()).isZero();
  }

  @Test
  void testSetVariableInInMappingOfCallActivity() {
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
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then in mapping from call activity sets variable
    //-> interrupting conditional event is not triggered, since variable is only locally
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_OUTPUT_MAPPING);
  }

  @Test
  void testNonInterruptingSetVariableInInMappingOfCallActivity() {
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
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then in mapping from call activity sets variable
    //-> interrupting conditional event is not triggered, since variable is only locally
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_OUTPUT_MAPPING);
  }

  @Test
  void testSetVariableInStartListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
                                                    .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_START, EXPR_SET_VARIABLE)
                                                  .endEvent()
                                                  .done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //boundary event is triggered by default evaluation behavior
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  void testNonInterruptingSetVariableInStartListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
                                                    .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_START, EXPR_SET_VARIABLE)
                                                  .endEvent()
                                                  .done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //non interrupting boundary event is not triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_WITH_CONDITION);
  }

  @Test
  void testSetVariableInTakeListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .sequenceFlowId(FLOW_ID)
                                                  .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
                                                  .endEvent()
                                                  .done();
    OperatonExecutionListener listener = modelInstance.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setOperatonExpression(EXPR_SET_VARIABLE);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then take listener sets variable
    //non interrupting boundary event is triggered with default evaluation behavior
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  void testNonInterruptingSetVariableInTakeListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .sequenceFlowId(FLOW_ID)
                                                  .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
                                                  .endEvent()
                                                  .done();
    OperatonExecutionListener listener = modelInstance.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setOperatonExpression(EXPR_SET_VARIABLE);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then take listener sets variable
    //non interrupting boundary event is triggered with default evaluation behavior
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
  }

  @Test
  void testSetVariableInEndListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_WITH_CONDITION_ID)
                                                    .name(TASK_WITH_CONDITION)
                                                    .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_END, EXPR_SET_VARIABLE)
                                                  .userTask().name(AFTER_TASK)
                                                  .endEvent()
                                                  .done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_WITH_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then end listener sets variable
    //conditional event is not triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(AFTER_TASK);
  }

  @Test
  void testNonInterruptingSetVariableInEndListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                .startEvent()
                                                .userTask(TASK_WITH_CONDITION_ID)
                                                  .name(TASK_WITH_CONDITION)
                                                  .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_END, EXPR_SET_VARIABLE)
                                                .userTask().name(AFTER_TASK)
                                                .endEvent()
                                                .done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_WITH_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then end listener sets variable
    //non interrupting boundary event is not triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(AFTER_TASK);
  }

  @Test
  void testSetVariableInMultiInstance() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .userTask(TASK_WITH_CONDITION_ID)
                                                  .multiInstance()
                                                    .cardinality("3")
                                                    .parallel()
                                                  .done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, "${nrOfInstances == 3}", true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then multi instance is created
    //and boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  void testNonInterruptingSetVariableInMultiInstance() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .userTask(TASK_WITH_CONDITION_ID)
                                                  .multiInstance()
                                                    .cardinality("3")
                                                    .parallel()
                                                  .done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, "${nrOfInstances == 3}", false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then multi instance is created
    //and boundary event is triggered for each multi instance creation
    List<Task> multiInstanceTasks = taskQuery.taskDefinitionKey(TASK_WITH_CONDITION_ID).list();
    assertThat(multiInstanceTasks).hasSize(3);
    assertThat(taskService.createTaskQuery().taskName(TASK_AFTER_CONDITION).count()).isEqualTo(3);

    //when multi instances are completed
    for (Task multiInstanceTask : multiInstanceTasks) {
      taskService.complete(multiInstanceTask.getId());
    }

    //then non boundary events are triggered
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(9);
    assertThat(conditionEventSubscriptionQuery.list()).isEmpty();
  }

  @Test
  void testSetVariableInSeqMultiInstance() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                .startEvent()
                                                .userTask(TASK_BEFORE_CONDITION_ID)
                                                .name(TASK_BEFORE_CONDITION)
                                                .userTask(TASK_WITH_CONDITION_ID)
                                                  .multiInstance()
                                                    .cardinality("3")
                                                    .sequential()
                                                .done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, "${true}", true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then multi instance is created
    //and boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  void testNonInterruptingSetVariableInSeqMultiInstance() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                .startEvent()
                                                .userTask(TASK_BEFORE_CONDITION_ID)
                                                .name(TASK_BEFORE_CONDITION)
                                                .userTask(TASK_WITH_CONDITION_ID)
                                                .multiInstance()
                                                .cardinality("3")
                                                .sequential()
                                                .done();
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, "${true}", false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then multi instance is created
    //and boundary event is triggered for each multi instance creation and also from the default evaluation behavior
    //since the condition is true. That means one time from the default behavior and 4 times for the variables which are set:
    //nrOfInstances, nrOfCompletedInstances, nrOfActiveInstances, loopCounter
    for (int i = 0; i < 3; i++) {
      Task multiInstanceTask = taskQuery.taskDefinitionKey(TASK_WITH_CONDITION_ID).singleResult();
      assertThat(multiInstanceTask).isNotNull();
      assertThat(taskService.createTaskQuery().taskName(TASK_AFTER_CONDITION).count()).isEqualTo(i == 0 ? 5 : 5 + i * 2);
      taskService.complete(multiInstanceTask.getId());
    }

    //then non boundary events are triggered 9 times
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(9);
    assertThat(conditionEventSubscriptionQuery.list()).isEmpty();
  }

  @Test
  void testSetVariableInCallActivity() {
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
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then service task in call activity sets variable
    //conditional event is not triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_SERVICE_TASK);
  }

  @Test
  void testNonInterruptingSetVariableInCallActivity() {
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
    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then service task in call activity sets variable
    //conditional event is not triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_SERVICE_TASK);
  }

  @Test
  void testSetVariableInSubProcessInDelegatedCode() {

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
    deployConditionalBoundaryEventProcess(modelInstance, SUB_PROCESS_ID, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then service task in sub process sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
  }

  @Test
  void testNonInterruptingSetVariableInSubProcessInDelegatedCode() {

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
    deployConditionalBoundaryEventProcess(modelInstance, SUB_PROCESS_ID, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then service task in sub process sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);
  }

  @Test
  void testSetMultipleVariables() {

    // given
    BpmnModelInstance modelInstance = modify(TASK_MODEL)
      .userTaskBuilder(TASK_BEFORE_CONDITION_ID)
      .boundaryEvent()
      .cancelActivity(true)
      .conditionalEventDefinition("event1")
      .condition("${variable1 == 1}")
      .conditionalEventDefinitionDone()
      .userTask("afterBoundary1")
      .endEvent()
      .moveToActivity(TASK_BEFORE_CONDITION_ID)
      .boundaryEvent()
      .cancelActivity(true)
      .conditionalEventDefinition("event2")
      .condition("${variable2 == 1}")
      .conditionalEventDefinitionDone()
      .userTask("afterBoundary2")
      .endEvent()
      .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());
    runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY, Variables.createVariables().putValue("variable1", "44").putValue("variable2", "44"));
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.setVariables(task.getId(), Variables
      .createVariables()
      .putValue("variable1", 1)
      .putValue("variable2", 1));

    // then
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    String taskDefinitionKey = tasksAfterVariableIsSet.get(0).getTaskDefinitionKey();
    assertThat("afterBoundary1".equals(taskDefinitionKey) || "afterBoundary2".equals(taskDefinitionKey)).isTrue();
  }

  @Test
  @Deployment
  void testTrueConditionWithExecutionListener() {
    //given process with boundary conditional event

    //when process is started and execution arrives activity with boundary event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    //then default evaluation behavior triggers conditional event
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list()).isEmpty();
  }

  @Test
  void testSuspendedProcess() {

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent().done();

    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, true);

    // given suspended process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    var processInstanceId = procInst.getId();
    runtimeService.suspendProcessInstanceById(processInstanceId);

    //when wrong variable is set
    runtimeService.setVariable(processInstanceId, VARIABLE_NAME+1, 1);

    //then nothing happens
    assertThat(runtimeService.createProcessInstanceQuery().singleResult().isSuspended()).isTrue();

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
  void testNonInterruptingConditionalSuspendedProcess() {

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent().done();

    deployConditionalBoundaryEventProcess(modelInstance, TASK_WITH_CONDITION_ID, false);

    // given suspended process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    var processInstanceId = procInst.getId();
    runtimeService.suspendProcessInstanceById(processInstanceId);

    //when wrong variable is set
    runtimeService.setVariable(processInstanceId, VARIABLE_NAME+1, 1);

    //then nothing happens
    assertThat(runtimeService.createProcessInstanceQuery().singleResult().isSuspended()).isTrue();

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
}
