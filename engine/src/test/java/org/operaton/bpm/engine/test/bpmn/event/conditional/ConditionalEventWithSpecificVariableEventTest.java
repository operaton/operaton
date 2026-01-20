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
import java.util.Map;

import org.junit.jupiter.api.TestTemplate;

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@Parameterized
public class ConditionalEventWithSpecificVariableEventTest extends AbstractConditionalEventTestCase {

  private interface ConditionalProcessVarSpecification {
    BpmnModelInstance getProcessWithVarName(boolean interrupting, String condition);
    BpmnModelInstance getProcessWithVarNameAndEvents(boolean interrupting, String varEvent);
    BpmnModelInstance getProcessWithVarEvents(boolean interrupting, String varEvent);
  }

  @Parameters
  public static Collection<Object[]> data() {
    return List.of(new Object[][] {
      {
        //conditional boundary event
        new ConditionalProcessVarSpecification() {
        @Override
        public BpmnModelInstance getProcessWithVarName(boolean interrupting, String condition) {
          return modify(TASK_MODEL)
            .userTaskBuilder(TASK_BEFORE_CONDITION_ID)
            .boundaryEvent()
            .cancelActivity(interrupting)
            .conditionalEventDefinition(CONDITIONAL_EVENT)
            .condition(condition)
            .operatonVariableName(VARIABLE_NAME)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent()
            .done();
        }

        @Override
        public BpmnModelInstance getProcessWithVarNameAndEvents(boolean interrupting, String varEvent) {
          return modify(TASK_MODEL)
            .userTaskBuilder(TASK_BEFORE_CONDITION_ID)
            .boundaryEvent()
            .cancelActivity(interrupting)
            .conditionalEventDefinition(CONDITIONAL_EVENT)
            .condition(CONDITION_EXPR)
            .operatonVariableName(VARIABLE_NAME)
            .operatonVariableEvents(varEvent)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent()
            .done();
        }

        @Override
        public BpmnModelInstance getProcessWithVarEvents(boolean interrupting, String varEvent) {
          return modify(TASK_MODEL)
            .userTaskBuilder(TASK_BEFORE_CONDITION_ID)
            .boundaryEvent()
            .cancelActivity(interrupting)
            .conditionalEventDefinition(CONDITIONAL_EVENT)
            .condition(CONDITION_EXPR)
            .operatonVariableEvents(varEvent)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent()
            .done();
        }

          @Override
          public String toString() {
            return "ConditionalBoundaryEventWithVarEvents";
          }
        }},

      //conditional start event of event sub process
      {new ConditionalProcessVarSpecification() {
        @Override
        public BpmnModelInstance getProcessWithVarName(boolean interrupting, String condition) {
          return modify(TASK_MODEL)
            .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
            .triggerByEvent()
            .embeddedSubProcess()
            .startEvent()
            .interrupting(interrupting)
            .conditionalEventDefinition(CONDITIONAL_EVENT)
            .condition(condition)
            .operatonVariableName(VARIABLE_NAME)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent()
            .done();
        }

        @Override
        public BpmnModelInstance getProcessWithVarNameAndEvents(boolean interrupting, String varEvent) {
          return modify(TASK_MODEL)
            .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
            .triggerByEvent()
            .embeddedSubProcess()
            .startEvent()
            .interrupting(interrupting)
            .conditionalEventDefinition(CONDITIONAL_EVENT)
            .condition(CONDITION_EXPR)
            .operatonVariableName(VARIABLE_NAME)
            .operatonVariableEvents(varEvent)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent()
            .done();
        }

        @Override
        public BpmnModelInstance getProcessWithVarEvents(boolean interrupting, String varEvent) {
          return modify(TASK_MODEL)
            .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
            .triggerByEvent()
            .embeddedSubProcess()
            .startEvent()
            .interrupting(interrupting)
            .conditionalEventDefinition(CONDITIONAL_EVENT)
            .condition(CONDITION_EXPR)
            .operatonVariableEvents(varEvent)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent()
            .done();
        }

        @Override
        public String toString() {
          return "ConditionalStartEventWithVarEvents";
        }
      }}
    });
  }


  @Parameter
  public ConditionalProcessVarSpecification specifier;

  @TestTemplate
  void testVariableConditionWithVariableName() {

    //given process with boundary conditional event and defined variable name
    final BpmnModelInstance modelInstance = specifier.getProcessWithVarName(true, CONDITION_EXPR);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();

    //when variable with name `variable1` is set on execution
    taskService.setVariable(task.getId(), VARIABLE_NAME + 1, 1);

    //then nothing happens
    task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);

    //when variable with name `variable` is set on execution
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional event
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list()).isEmpty();
  }

  @TestTemplate
  void testVariableConditionWithVariableNameAndEvent() {

    //given process with boundary conditional event and defined variable name and event
    final BpmnModelInstance modelInstance = specifier.getProcessWithVarNameAndEvents(true, CONDITIONAL_VAR_EVENT_UPDATE);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();

    //when variable with name `variable` is set on execution
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then nothing happens
    task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);

    //when variable with name `variable` is updated
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional event
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list()).isEmpty();
  }

  @TestTemplate
  void testNonInterruptingVariableConditionWithVariableName() {

    //given process with non interrupting boundary conditional event and defined variable name and true condition
    final BpmnModelInstance modelInstance = specifier.getProcessWithVarName(false, TRUE_CONDITION);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    //when process is started
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());

    //then first event is triggered since condition is true
    List<Task> tasks = taskQuery.list();
    assertThat(tasks).hasSize(2);

    //when variable with name `variable1` is set on execution
    runtimeService.setVariable(procInst.getId(), VARIABLE_NAME + 1, 1);

    //then nothing happens
    tasks = taskQuery.list();
    assertThat(tasks).hasSize(2);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);

    //when variable with name `variable` is set, updated and deleted
    runtimeService.setVariable(procInst.getId(), VARIABLE_NAME, 1); //create
    runtimeService.setVariable(procInst.getId(), VARIABLE_NAME, 1); //update
    runtimeService.removeVariable(procInst.getId(), VARIABLE_NAME); //delete

    //then execution is for four times at user task after conditional event
    //one from default behavior and three times from the variable events
    assertThat(taskService.createTaskQuery().taskName(TASK_AFTER_CONDITION).count()).isEqualTo(4);
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(5);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);
  }

  @TestTemplate
  void testNonInterruptingVariableConditionWithVariableNameAndEvents() {

    //given process with non interrupting boundary conditional event and defined variable name and events
    final BpmnModelInstance modelInstance = specifier.getProcessWithVarNameAndEvents(false, CONDITIONAL_VAR_EVENTS);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();

    //when variable with name `variable` is set, updated and deleted
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //create
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //update
    taskService.removeVariable(task.getId(), VARIABLE_NAME); //delete

    //then execution is for two times at user task after conditional start event
    assertThat(taskService.createTaskQuery().taskName(TASK_AFTER_CONDITION).count()).isEqualTo(2);
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet).hasSize(3);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);
  }


  @TestTemplate
  void testVariableConditionWithVariableEvent() {

    //given process with boundary conditional event and defined variable event
    final BpmnModelInstance modelInstance = specifier.getProcessWithVarEvents(true, CONDITIONAL_VAR_EVENT_UPDATE);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME + 1, 0);
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY, variables);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();

    //when variable with name `variable` is set on execution
    runtimeService.setVariable(procInst.getId(), VARIABLE_NAME, 1);

    //then nothing happens
    task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(TASK_BEFORE_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);

    //when variable with name `variable1` is updated
    runtimeService.setVariable(procInst.getId(), VARIABLE_NAME + 1, 1);

    //then execution is at user task after conditional intermediate event
    tasksAfterVariableIsSet = taskQuery.list();
    assertThat(tasksAfterVariableIsSet.get(0).getName()).isEqualTo(TASK_AFTER_CONDITION);
    assertThat(conditionEventSubscriptionQuery.list()).isEmpty();
  }

  @TestTemplate
  void testNonInterruptingVariableConditionWithVariableEvent() {

    //given process with non interrupting boundary conditional event and defined variable event
    final BpmnModelInstance modelInstance = specifier.getProcessWithVarEvents(false, CONDITIONAL_VAR_EVENT_UPDATE);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertThat(task).isNotNull();

    //when variable with name `variable` is set
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //create

    //then nothing happens
    task = taskQuery.singleResult();
    assertThat(task).isNotNull();

    //when variable is updated twice
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //update
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //update

    //then execution is for two times at user task after conditional event
    assertThat(taskQuery.taskName(TASK_AFTER_CONDITION).count()).isEqualTo(2);
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(3);
    assertThat(conditionEventSubscriptionQuery.list()).hasSize(1);
  }

}
