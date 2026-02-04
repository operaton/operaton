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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
class TriggerConditionalEventOnStartAtActivityTest extends AbstractConditionalEventTestCase {

  @Test
  void testTriggerGlobalEventSubProcess() {
    //given
    deployConditionalEventSubProcess(TASK_MODEL, CONDITIONAL_EVENT_PROCESS_KEY, true);

    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(1);
    assertThat(tasksAfterVariableIsSet.get(0).getTaskDefinitionKey()).isEqualTo(TASK_AFTER_CONDITION_ID);
  }


  @Test
  void testNonInterruptingTriggerGlobalEventSubProcess() {
    //given
    deployConditionalEventSubProcess(TASK_MODEL, CONDITIONAL_EVENT_PROCESS_KEY, false);

    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
    assertThat(taskService.createTaskQuery().taskName(TASK_AFTER_CONDITION).count()).isOne();
    assertThat(conditionEventSubscriptionQuery.count()).isOne();
  }


  @Test
  void testTriggerInnerEventSubProcess() {
    //given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
        .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    deployConditionalEventSubProcess(modelInstance, SUB_PROCESS_ID, true);


    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(1);
    assertThat(tasksAfterVariableIsSet.get(0).getTaskDefinitionKey()).isEqualTo(TASK_AFTER_CONDITION_ID);
  }

  @Test
  void testNonInterruptingTriggerInnerEventSubProcess() {
    //given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    deployConditionalEventSubProcess(modelInstance, SUB_PROCESS_ID, false);


    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
    assertThat(taskService.createTaskQuery().taskName(TASK_AFTER_CONDITION).count()).isOne();
    assertThat(conditionEventSubscriptionQuery.count()).isOne();
  }

  @Test
  void testTriggerGlobalEventSubProcessFromInnerSubProcess() {
    //given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, true);

    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(1);
    assertThat(tasksAfterVariableIsSet.get(0).getTaskDefinitionKey()).isEqualTo(TASK_AFTER_CONDITION_ID);
  }

  @Test
  void testNonInterruptingTriggerGlobalEventSubProcessFromInnerSubProcess() {
    //given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
        .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);

    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
    assertThat(taskService.createTaskQuery().taskName(TASK_AFTER_CONDITION).count()).isOne();
    assertThat(conditionEventSubscriptionQuery.count()).isOne();
  }


  @Test
  void testTriggerGlobalAndInnerEventSubProcessFromInnerSubProcess() {
    //given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    modelInstance = addConditionalEventSubProcess(modelInstance, SUB_PROCESS_ID, TASK_AFTER_CONDITION_ID + 1, true);
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, true);

    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(1);
    assertThat(tasksAfterVariableIsSet.get(0).getTaskDefinitionKey()).isEqualTo(TASK_AFTER_CONDITION_ID);
  }


  @Test
  void testNonInterruptingTriggerGlobalAndInnerEventSubProcessFromInnerSubProcess() {
    //given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
        .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    modelInstance = addConditionalEventSubProcess(modelInstance, SUB_PROCESS_ID, TASK_AFTER_CONDITION_ID + 1, false);
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, false);

    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(3);
    assertThat(taskService.createTaskQuery().taskName(TASK_AFTER_CONDITION).count()).isEqualTo(2);
    assertThat(taskService.createTaskQuery().taskDefinitionKey(TASK_AFTER_CONDITION_ID).count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey(TASK_AFTER_CONDITION_ID + 1).count()).isOne();
    assertThat(conditionEventSubscriptionQuery.count()).isEqualTo(2);
  }


  @Test
  void testTriggerBoundaryEvent() {
    //given
    deployConditionalBoundaryEventProcess(TASK_MODEL, TASK_BEFORE_CONDITION_ID, true);

    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(1);
    assertThat(tasksAfterVariableIsSet.get(0).getTaskDefinitionKey()).isEqualTo(TASK_AFTER_CONDITION_ID);
  }

  @Test
  void testNonInterruptingTriggerBoundaryEvent() {
    //given
    deployConditionalBoundaryEventProcess(TASK_MODEL, TASK_BEFORE_CONDITION_ID, false);

    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
    assertThat(taskService.createTaskQuery().taskName(TASK_AFTER_CONDITION).count()).isOne();
    assertThat(conditionEventSubscriptionQuery.count()).isOne();
  }


  @Test
  void testTriggerBoundaryEventFromInnerSubProcess() {
    //given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    deployConditionalBoundaryEventProcess(modelInstance, SUB_PROCESS_ID, true);


    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(1);
    assertThat(tasksAfterVariableIsSet.get(0).getTaskDefinitionKey()).isEqualTo(TASK_AFTER_CONDITION_ID);
  }

  @Test
  void testNonInterruptingTriggerBoundaryEventFromInnerSubProcess() {
    //given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    deployConditionalBoundaryEventProcess(modelInstance, SUB_PROCESS_ID, false);


    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
    assertThat(taskService.createTaskQuery().taskName(TASK_AFTER_CONDITION).count()).isOne();
    assertThat(conditionEventSubscriptionQuery.count()).isOne();
  }

  @Test
  void testTriggerUserAndSubProcessBoundaryEventFromInnerSubProcess() {
    //given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
        .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID)
          .name(TASK_BEFORE_CONDITION)
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    modelInstance = addConditionalBoundaryEvent(modelInstance, TASK_BEFORE_CONDITION_ID, TASK_AFTER_CONDITION_ID + 1, true);
    deployConditionalBoundaryEventProcess(modelInstance, SUB_PROCESS_ID, true);


    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(1);
    assertThat(tasksAfterVariableIsSet.get(0).getTaskDefinitionKey()).isEqualTo(TASK_AFTER_CONDITION_ID);
  }


  @Test
  void testNonInterruptingTriggerUserAndSubProcessBoundaryEventFromInnerSubProcess() {
    //given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
        .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    modelInstance = addConditionalBoundaryEvent(modelInstance, TASK_BEFORE_CONDITION_ID, TASK_AFTER_CONDITION_ID + 1, false);
    deployConditionalBoundaryEventProcess(modelInstance, SUB_PROCESS_ID, false);


    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(3);
    assertThat(taskService.createTaskQuery().taskName(TASK_AFTER_CONDITION).count()).isEqualTo(2);
    assertThat(taskService.createTaskQuery().taskDefinitionKey(TASK_AFTER_CONDITION_ID + 1).count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey(TASK_AFTER_CONDITION_ID).count()).isOne();
    assertThat(conditionEventSubscriptionQuery.count()).isEqualTo(2);
  }

  @Test
  void testTriggerMixedProcess() {
    //given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    boolean isInterrupting = true;
    modelInstance = addConditionalBoundaryEvent(modelInstance, TASK_BEFORE_CONDITION_ID, TASK_AFTER_CONDITION_ID + 1, isInterrupting);
    modelInstance = addConditionalBoundaryEvent(modelInstance, SUB_PROCESS_ID, TASK_AFTER_CONDITION_ID + 2, isInterrupting);
    modelInstance = addConditionalEventSubProcess(modelInstance, SUB_PROCESS_ID, TASK_AFTER_CONDITION_ID + 3, isInterrupting);
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, isInterrupting);

    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(1);
    assertThat(tasksAfterVariableIsSet.get(0).getTaskDefinitionKey()).isEqualTo(TASK_AFTER_CONDITION_ID);
  }


  @Test
  void testNonInterruptingTriggerMixedProcess() {
    //given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent()
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    boolean isInterrupting = false;
    modelInstance = addConditionalBoundaryEvent(modelInstance, TASK_BEFORE_CONDITION_ID, TASK_AFTER_CONDITION_ID + 1, isInterrupting);
    modelInstance = addConditionalBoundaryEvent(modelInstance, SUB_PROCESS_ID, TASK_AFTER_CONDITION_ID + 2, isInterrupting);
    modelInstance = addConditionalEventSubProcess(modelInstance, SUB_PROCESS_ID, TASK_AFTER_CONDITION_ID + 3, isInterrupting);
    deployConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, isInterrupting);

    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(5);
    assertThat(taskService.createTaskQuery().taskName(TASK_AFTER_CONDITION).count()).isEqualTo(4);
    assertThat(taskService.createTaskQuery().taskDefinitionKey(TASK_AFTER_CONDITION_ID + 1).count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey(TASK_AFTER_CONDITION_ID + 2).count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey(TASK_AFTER_CONDITION_ID + 3).count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey(TASK_AFTER_CONDITION_ID).count()).isOne();
    assertThat(conditionEventSubscriptionQuery.count()).isEqualTo(4);
  }

  @Test
  @Disabled("The expected 2nd task instance from the boundary event is missing")
  void testTwoInstructions() {
    //given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent("start")
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
        .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
        .endEvent()
      .subProcessDone()
      .endEvent()
      .moveToNode("start")
      .subProcess(SUB_PROCESS_ID + 1)
      .embeddedSubProcess()
        .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID + 1)
        .name(TASK_BEFORE_CONDITION + 1)
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
    boolean isInterrupting = true;
    modelInstance = addConditionalBoundaryEvent(modelInstance, SUB_PROCESS_ID, CONDITION_EXPR, TASK_AFTER_CONDITION_ID, TASK_AFTER_CONDITION_ID, isInterrupting);
    modelInstance = addConditionalBoundaryEvent(modelInstance, SUB_PROCESS_ID + 1, CONDITION_EXPR, TASK_AFTER_CONDITION_ID + 1, TASK_AFTER_CONDITION_ID + 1, isInterrupting);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    //when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
      .setVariable(VARIABLE_NAME, "1")
      .startBeforeActivity(TASK_BEFORE_CONDITION_ID + 1)
      .executeWithVariablesInReturn();

    //then
    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertTaskNames(tasksAfterVariableIsSet, TASK_AFTER_CONDITION_ID, TASK_AFTER_CONDITION_ID + 1);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testSubProcessNonInterruptingTriggerGlobalEventSubProcess() {
    // given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent("start")
      .userTask("beforeSubProcess")
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
        .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done();

    modelInstance = addConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, TASK_AFTER_CONDITION_ID, false);

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
    .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
    .setVariable(VARIABLE_NAME, "1")
    .executeWithVariablesInReturn();

    // then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isOne();
    assertThat(historyService.createHistoricVariableInstanceQuery().singleResult().getName()).isEqualTo("variable");

    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(2);
    assertThat(taskService.createTaskQuery().taskDefinitionKey(TASK_BEFORE_CONDITION_ID).count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey(TASK_AFTER_CONDITION_ID).count()).isOne();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testSubProcessInterruptingTriggerGlobalEventSubProcess() {
    // given
    BpmnModelInstance modelInstance =  Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent("start")
      .userTask("beforeSubProcess")
      .subProcess(SUB_PROCESS_ID)
      .embeddedSubProcess()
        .startEvent()
        .userTask(TASK_BEFORE_CONDITION_ID)
        .name(TASK_BEFORE_CONDITION)
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done();

    modelInstance = addConditionalEventSubProcess(modelInstance, CONDITIONAL_EVENT_PROCESS_KEY, TASK_AFTER_CONDITION_ID, true);

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // when
    runtimeService.createProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY)
    .startBeforeActivity(TASK_BEFORE_CONDITION_ID)
    .setVariable(VARIABLE_NAME, "1")
    .executeWithVariablesInReturn();

    // then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isOne();
    assertThat(historyService.createHistoricVariableInstanceQuery().singleResult().getName()).isEqualTo("variable");

    tasksAfterVariableIsSet = taskService.createTaskQuery().list();
    assertThat(tasksAfterVariableIsSet).hasSize(1);
    assertThat(tasksAfterVariableIsSet.get(0).getTaskDefinitionKey()).isEqualTo(TASK_AFTER_CONDITION_ID);
  }
}
