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

import java.util.Collection;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@Parameterized
public class TriggerConditionalEventFromDelegationCodeTest extends AbstractConditionalEventTestCase {

  private interface ConditionalEventProcessSpecifier {
    Class getDelegateClass();
    int getExpectedInterruptingCount();
    int getExpectedNonInterruptingCount();
    String getCondition();
  }


  @Parameters
  public static Collection<Object[]> data() {
    return List.of(new Object[][]{
      {new ConditionalEventProcessSpecifier() {
        @Override
        public Class getDelegateClass() {
          return SetVariableDelegate.class;
        }

        @Override
        public int getExpectedInterruptingCount() {
          return 1;
        }

        @Override
        public int getExpectedNonInterruptingCount() {
          return 1;
        }

        @Override
        public String getCondition() {
          return CONDITION_EXPR;
        }

        @Override
        public String toString() {
          return "SetSingleVariableInDelegate";
        }
      }
      }, {
      new ConditionalEventProcessSpecifier() {
        @Override
        public Class getDelegateClass() {
          return SetMultipleSameVariableDelegate.class;
        }

        @Override
        public int getExpectedInterruptingCount() {
          return 1;
        }

        @Override
        public int getExpectedNonInterruptingCount() {
          return 3;
        }


        @Override
        public String getCondition() {
          return "${variable2 == 1}";
        }

        @Override
        public String toString() {
          return "SetMultipleVariableInDelegate";
        }
      }}});
  }

  @Parameter
  public ConditionalEventProcessSpecifier specifier;

  @TestTemplate
  void testSetVariableInStartListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .userTask(TASK_WITH_CONDITION_ID)
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, specifier.getDelegateClass().getName())
      .endEvent()
      .done();
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, specifier.getCondition(), true);

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
    assertThat(taskQuery.taskName(TASK_AFTER_CONDITION).count()).isEqualTo(specifier.getExpectedInterruptingCount());
  }

  @TestTemplate
  void testNonInterruptingSetVariableInStartListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .userTask(TASK_WITH_CONDITION_ID)
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, specifier.getDelegateClass().getName())
      .name(TASK_WITH_CONDITION)
      .endEvent()
      .done();
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, specifier.getCondition(), false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());

    //when task is completed
    taskService.complete(taskQuery.singleResult().getId());

    //then start listener sets variable
    //non interrupting boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(1 + specifier.getExpectedNonInterruptingCount());
    assertThat(taskQuery.taskName(TASK_AFTER_CONDITION).count()).isEqualTo(specifier.getExpectedNonInterruptingCount());
  }

  @TestTemplate
  void testSetVariableInTakeListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .sequenceFlowId(FLOW_ID)
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent()
      .done();
    OperatonExecutionListener listener = modelInstance.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setOperatonClass(specifier.getDelegateClass().getName());
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, specifier.getCondition(), true);

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
    assertThat(taskQuery.taskName(TASK_AFTER_CONDITION).count()).isEqualTo(specifier.getExpectedInterruptingCount());
  }

  @TestTemplate
  void testNonInterruptingSetVariableInTakeListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .sequenceFlowId(FLOW_ID)
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent()
      .done();
    OperatonExecutionListener listener = modelInstance.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setOperatonClass(specifier.getDelegateClass().getName());
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, specifier.getCondition(), false);

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
    assertThat(tasksAfterVariableIsSet).hasSize(1 + specifier.getExpectedNonInterruptingCount());
    assertThat(taskQuery.taskName(TASK_AFTER_CONDITION).count()).isEqualTo(specifier.getExpectedNonInterruptingCount());
  }

  @TestTemplate
  void testSetVariableInTakeListenerWithAsyncBefore() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .sequenceFlowId(FLOW_ID)
      .userTask(TASK_WITH_CONDITION_ID).operatonAsyncBefore()
      .endEvent()
      .done();
    OperatonExecutionListener listener = modelInstance.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setOperatonClass(specifier.getDelegateClass().getName());
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, specifier.getCondition(), true);

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
    assertThat(taskQuery.taskName(TASK_AFTER_CONDITION).count()).isEqualTo(specifier.getExpectedInterruptingCount());
  }

  @TestTemplate
  void testNonInterruptingSetVariableInTakeListenerWithAsyncBefore() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .sequenceFlowId(FLOW_ID)
      .userTask(TASK_WITH_CONDITION_ID).operatonAsyncBefore()
      .endEvent()
      .done();
    OperatonExecutionListener listener = modelInstance.newInstance(OperatonExecutionListener.class);
    listener.setOperatonEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setOperatonClass(specifier.getDelegateClass().getName());
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, specifier.getCondition(), false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());

    //when task is completed
    taskService.complete(taskQuery.singleResult().getId());

    //then take listener sets variable
    //non interrupting boundary event is triggered
    assertThat(taskService.createTaskQuery().taskName(TASK_AFTER_CONDITION).count()).isEqualTo(specifier.getExpectedNonInterruptingCount());

    //and job was created
    Job job = engine.getManagementService().createJobQuery().singleResult();
    assertThat(job).isNotNull();


    //when job is executed task is created
    engine.getManagementService().executeJob(job.getId());
    //when all tasks are completed
    assertThat(taskQuery.count()).isEqualTo(specifier.getExpectedNonInterruptingCount() + 1);
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
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, specifier.getDelegateClass().getName())
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent()
      .done();
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, specifier.getCondition(), true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();

    //when task is completed
    taskService.complete(task.getId());

    //then end listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(taskQuery.taskName(TASK_AFTER_CONDITION).count()).isEqualTo(specifier.getExpectedInterruptingCount());
  }

  @TestTemplate
  void testNonInterruptingSetVariableInEndListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, specifier.getDelegateClass().getName())
      .userTask(TASK_WITH_CONDITION_ID)
      .name(TASK_WITH_CONDITION)
      .endEvent()
      .done();
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, specifier.getCondition(), false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());

    //when task is completed
    taskService.complete(taskQuery.singleResult().getId());

    //then end listener sets variable
    //non interrupting event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(1 + specifier.getExpectedNonInterruptingCount());
    assertThat(taskQuery.taskName(TASK_AFTER_CONDITION).count()).isEqualTo(specifier.getExpectedNonInterruptingCount());
  }

  @TestTemplate
  void testSetVariableInStartAndEndListener() {
    //given process with start and end listener on user task
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, specifier.getDelegateClass().getName())
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, specifier.getDelegateClass().getName())
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent()
      .done();
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, specifier.getCondition(), true);

    //when process is started
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    //then start listener sets variable and
    //execution stays in task after conditional event in event sub process
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task.getName()).isEqualTo(TASK_AFTER_CONDITION);
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(taskQuery.taskName(TASK_AFTER_CONDITION).count()).isEqualTo(specifier.getExpectedInterruptingCount());
  }
}
