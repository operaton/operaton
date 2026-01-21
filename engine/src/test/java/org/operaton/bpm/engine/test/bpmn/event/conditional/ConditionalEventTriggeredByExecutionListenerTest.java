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

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.TestTemplate;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.SequenceFlow;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonExecutionListener;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@Parameterized
public class ConditionalEventTriggeredByExecutionListenerTest extends AbstractConditionalEventTestCase {

  protected static final String TASK_AFTER_CONDITIONAL_BOUNDARY_EVENT = "Task after conditional boundary event";
  protected static final String TASK_AFTER_CONDITIONAL_START_EVENT = "Task after conditional start event";
  protected static final String START_EVENT_ID = "startEventId";
  protected static final String END_EVENT_ID = "endEventId";

  private interface ConditionalEventProcessSpecifier {
    BpmnModelInstance specifyConditionalProcess(BpmnModelInstance modelInstance, boolean isInterrupting);
    void assertTaskNames(List<Task> tasks, boolean isInterrupting, boolean isAyncBefore);
    int expectedSubscriptions();
    int expectedTaskCount();
  }

  @Parameters
  public static Collection<Object[]> data() {
    return List.of(new Object[][]{
      {new ConditionalEventProcessSpecifier() {
        @Override
        public BpmnModelInstance specifyConditionalProcess(BpmnModelInstance modelInstance, boolean isInterrupting) {
          return modify(modelInstance)
            .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
            .triggerByEvent()
            .embeddedSubProcess()
            .startEvent()
            .interrupting(isInterrupting)
            .conditionalEventDefinition()
            .condition(CONDITION_EXPR)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent()
            .done();
        }

        @Override
        public void assertTaskNames(List<Task> tasks, boolean isInterrupting, boolean isAyncBefore) {
            if (isInterrupting || isAyncBefore) {
              ConditionalEventTriggeredByExecutionListenerTest.assertTaskNames(tasks, TASK_AFTER_CONDITION);
            } else {
              ConditionalEventTriggeredByExecutionListenerTest.assertTaskNames(tasks,
                TASK_WITH_CONDITION,
                TASK_AFTER_CONDITION);
            }
        }

        @Override
        public int expectedSubscriptions() {
          return 1;
        }

        @Override
        public int expectedTaskCount() {
          return 2;
        }

        @Override
        public String toString() {
          return "ConditionalEventSubProcess";
        }
      }}, {
      new ConditionalEventProcessSpecifier() {
        @Override
        public BpmnModelInstance specifyConditionalProcess(BpmnModelInstance modelInstance, boolean isInterrupting) {
          modelInstance = modify(modelInstance)
            .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
            .triggerByEvent()
            .embeddedSubProcess()
            .startEvent()
            .interrupting(isInterrupting)
            .conditionalEventDefinition()
            .condition(CONDITION_EXPR)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITIONAL_START_EVENT)
            .endEvent()
            .done();

          return modify(modelInstance)
            .activityBuilder(TASK_WITH_CONDITION_ID)
            .boundaryEvent()
            .cancelActivity(isInterrupting)
            .conditionalEventDefinition()
            .condition(CONDITION_EXPR)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITIONAL_BOUNDARY_EVENT)
            .endEvent()
            .done();
        }

        @Override
        public void assertTaskNames(List<Task> tasks, boolean isInterrupting, boolean isAyncBefore) {
          assertThat(tasks).isNotNull();
          if (isInterrupting || isAyncBefore) {
            ConditionalEventTriggeredByExecutionListenerTest.assertTaskNames(tasks, TASK_AFTER_CONDITIONAL_START_EVENT);
          } else {
            ConditionalEventTriggeredByExecutionListenerTest.assertTaskNames(tasks,
              TASK_WITH_CONDITION,
              TASK_AFTER_CONDITIONAL_BOUNDARY_EVENT,
              TASK_AFTER_CONDITIONAL_START_EVENT);

          }
        }

        @Override
        public int expectedSubscriptions() {
          return 2;
        }

        @Override
        public int expectedTaskCount() {
          return 3;
        }

        @Override
        public String toString() {
          return "MixedConditionalProcess";
        }
      }}});
  }

  @Parameter
  public ConditionalEventProcessSpecifier specifier;


  @TestTemplate
  void testSetVariableInStartListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
      .userTask(TASK_WITH_CONDITION_ID)
        .name(TASK_WITH_CONDITION)
        .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_START, EXPR_SET_VARIABLE)
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, true);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    specifier.assertTaskNames(tasksAfterVariableIsSet, true, false);
  }

  @TestTemplate
  void testNonInterruptingSetVariableInStartListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
      .userTask(TASK_WITH_CONDITION_ID)
        .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_START, EXPR_SET_VARIABLE)
        .name(TASK_WITH_CONDITION)
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, false);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());

    //when task is completed
    taskService.complete(taskQuery.singleResult().getId());

    //then start listener sets variable
    //non interrupting boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(specifier.expectedTaskCount());
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(specifier.expectedSubscriptions());
    specifier.assertTaskNames(tasksAfterVariableIsSet, false, false);
  }

  @TestTemplate
  void testSetVariableInTakeListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
      .sequenceFlowId(FLOW_ID)
      .userTask(TASK_WITH_CONDITION_ID)
        .name(TASK_WITH_CONDITION)
      .endEvent(END_EVENT_ID)
      .done();
    OperatonExecutionListener listener = modelInstance.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setOperatonExpression(EXPR_SET_VARIABLE);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    modelInstance = specifier.specifyConditionalProcess(modelInstance, true);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then take listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    specifier.assertTaskNames(tasksAfterVariableIsSet, true, false);
  }

  @TestTemplate
  void testNonInterruptingSetVariableInTakeListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
      .sequenceFlowId(FLOW_ID)
      .userTask(TASK_WITH_CONDITION_ID)
        .name(TASK_WITH_CONDITION)
      .endEvent(END_EVENT_ID)
      .done();
    OperatonExecutionListener listener = modelInstance.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setOperatonExpression(EXPR_SET_VARIABLE);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    modelInstance = specifier.specifyConditionalProcess(modelInstance, false);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then take listener sets variable
    //non interrupting boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(specifier.expectedTaskCount());
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(specifier.expectedSubscriptions());
    specifier.assertTaskNames(tasksAfterVariableIsSet, false, false);
  }

  @TestTemplate
  void testSetVariableInTakeListenerWithAsyncBefore() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
      .sequenceFlowId(FLOW_ID)
      .userTask(TASK_WITH_CONDITION_ID)
        .name(TASK_WITH_CONDITION)
        .operatonAsyncBefore()
      .endEvent(END_EVENT_ID)
      .done();
    OperatonExecutionListener listener = modelInstance.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setOperatonExpression(EXPR_SET_VARIABLE);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    modelInstance = specifier.specifyConditionalProcess(modelInstance, true);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then take listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    specifier.assertTaskNames(tasksAfterVariableIsSet, true, false);
  }

  @TestTemplate
  void testNonInterruptingSetVariableInTakeListenerWithAsyncBefore() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
      .sequenceFlowId(FLOW_ID)
      .userTask(TASK_WITH_CONDITION_ID)
        .name(TASK_WITH_CONDITION)
        .operatonAsyncBefore()
      .endEvent(END_EVENT_ID)
      .done();
    OperatonExecutionListener listener = modelInstance.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setOperatonExpression(EXPR_SET_VARIABLE);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    modelInstance = specifier.specifyConditionalProcess(modelInstance, false);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());

    //when task is completed
    taskService.complete(taskQuery.singleResult().getId());

    //then take listener sets variable
    //non interrupting boundary event is triggered
    specifier.assertTaskNames(taskQuery.list(), false, true);

    //and job was created
    Job job = engine.getManagementService().createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);

    //when job is executed task is created
    engine.getManagementService().executeJob(job.getId());
    //when tasks are completed
    for (Task task : taskQuery.list()) {
      taskService.complete(task.getId());
    }

    //then no task exist and process instance is ended
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).isEmpty();
    assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNull();
  }

  @TestTemplate
  void testSetVariableInEndListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
        .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_END, EXPR_SET_VARIABLE)
      .userTask(TASK_WITH_CONDITION_ID)
        .name(TASK_WITH_CONDITION)
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, true);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();

    //when task is completed
    taskService.complete(task.getId());

    //then end listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    specifier.assertTaskNames(tasksAfterVariableIsSet, true, false);
  }

  @TestTemplate
  void testNonInterruptingSetVariableInEndListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
        .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_END, EXPR_SET_VARIABLE)
      .userTask(TASK_WITH_CONDITION_ID)
        .name(TASK_WITH_CONDITION)
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, false);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());

    //when task is completed
    taskService.complete(taskQuery.singleResult().getId());

    //then end listener sets variable
    //non interrupting boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(specifier.expectedTaskCount());
    specifier.assertTaskNames(tasksAfterVariableIsSet, false, false);
  }

  @TestTemplate
  void testSetVariableOnParentScopeInTakeListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .subProcess()
      .embeddedSubProcess()
      .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID)
          .name(TASK_BEFORE_CONDITION)
        .sequenceFlowId(FLOW_ID)
        .userTask(TASK_WITH_CONDITION_ID)
          .name(TASK_WITH_CONDITION)
        .endEvent()
      .subProcessDone()
      .endEvent(END_EVENT_ID)
      .done();
    OperatonExecutionListener listener = modelInstance.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setOperatonExpression(EXPR_SET_VARIABLE_ON_PARENT);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    modelInstance = specifier.specifyConditionalProcess(modelInstance, true);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    specifier.assertTaskNames(tasksAfterVariableIsSet, true, false);
  }

  @TestTemplate
  void testNonInterruptingSetVariableOnParentScopeInTakeListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .subProcess()
      .embeddedSubProcess()
        .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID)
          .name(TASK_BEFORE_CONDITION)
        .sequenceFlowId(FLOW_ID)
          .userTask(TASK_WITH_CONDITION_ID)
          .name(TASK_WITH_CONDITION)
        .endEvent()
      .subProcessDone()
      .endEvent(END_EVENT_ID)
      .done();
    OperatonExecutionListener listener = modelInstance.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setOperatonExpression(EXPR_SET_VARIABLE_ON_PARENT);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    modelInstance = specifier.specifyConditionalProcess(modelInstance, false);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //non interrupting boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    specifier.assertTaskNames(tasksAfterVariableIsSet, false, false);
  }

  @TestTemplate
  void testSetVariableOnParentScopeInStartListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .subProcess()
      .embeddedSubProcess()
        .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID)
          .name(TASK_BEFORE_CONDITION)
        .userTask(TASK_WITH_CONDITION_ID)
          .name(TASK_WITH_CONDITION)
          .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_START, EXPR_SET_VARIABLE_ON_PARENT)
        .endEvent()
      .subProcessDone()
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, true);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    specifier.assertTaskNames(tasksAfterVariableIsSet, true, false);
  }

  @TestTemplate
  void testNonInterruptingSetVariableOnParentScopeInStartListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .subProcess()
      .embeddedSubProcess()
        .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID)
          .name(TASK_BEFORE_CONDITION)
        .userTask(TASK_WITH_CONDITION_ID)
          .name(TASK_WITH_CONDITION)
          .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_START, EXPR_SET_VARIABLE_ON_PARENT)
        .endEvent()
      .subProcessDone()
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, false);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //non interrupting boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    specifier.assertTaskNames(tasksAfterVariableIsSet, false, false);
  }

  @TestTemplate
  void testSetVariableOnParentScopeInEndListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .subProcess()
      .embeddedSubProcess()
        .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID)
          .name(TASK_BEFORE_CONDITION)
          .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_END, EXPR_SET_VARIABLE_ON_PARENT)
        .userTask(TASK_WITH_CONDITION_ID)
          .name(TASK_WITH_CONDITION)
        .endEvent()
      .subProcessDone()
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, true);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then end listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    specifier.assertTaskNames(tasksAfterVariableIsSet, true, false);
  }

  @TestTemplate
  void testNonInterruptingSetVariableOnParentScopeInEndListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .subProcess()
      .embeddedSubProcess()
        .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID)
          .name(TASK_BEFORE_CONDITION)
          .operatonExecutionListenerExpression(ExecutionListener.EVENTNAME_END, EXPR_SET_VARIABLE_ON_PARENT)
        .userTask(TASK_WITH_CONDITION_ID)
          .name(TASK_WITH_CONDITION)
        .endEvent()
      .subProcessDone()
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, false);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);

    //when task is completed
    taskService.complete(task.getId());

    //then end listener sets variable
    //non interrupting boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    specifier.assertTaskNames(tasksAfterVariableIsSet, false, false);
  }

}
