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
package org.operaton.bpm.engine.test.api.runtime.migration;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.BOUNDARY_ID;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.CONDITION_ID;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.PROC_DEF_KEY;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.SUB_PROCESS_ID;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.USER_TASK_ID;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.VARIABLE_NAME;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.VAR_CONDITION;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.EventSubProcessModels.EVENT_SUB_PROCESS_START_ID;
import static org.operaton.bpm.engine.test.bpmn.event.conditional.AbstractConditionalEventTestCase.TASK_AFTER_CONDITION_ID;
import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.bpmn.event.conditional.SetVariableDelegate;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public class MigrationWithoutTriggerConditionTest {

  protected ProcessEngineRule rule = new ProvidedProcessEngineRule();
  protected MigrationTestRule testHelper = new MigrationTestRule(rule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(rule).around(testHelper);

  @Test
  public void testIntermediateConditionalEventWithSetVariableOnEndListener() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(Bpmn.createExecutableProcess()
      .startEvent()
      .subProcess()
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, SetVariableDelegate.class.getName())
      .embeddedSubProcess()
        .startEvent()
        .intermediateCatchEvent(CONDITION_ID)
          .conditionalEventDefinition()
           .condition(VAR_CONDITION)
          .conditionalEventDefinitionDone()
        .userTask(TASK_AFTER_CONDITION_ID)
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done());
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(Bpmn.createExecutableProcess()
      .startEvent()
      .intermediateCatchEvent(CONDITION_ID)
        .conditionalEventDefinition()
         .condition(VAR_CONDITION)
        .conditionalEventDefinitionDone()
      .userTask(TASK_AFTER_CONDITION_ID)
      .endEvent()
      .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(CONDITION_ID, CONDITION_ID).updateEventTrigger()
      .build();

    //when sub process is removed, end listener is called and sets variable
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);
    testHelper.assertEventSubscriptionMigrated(CONDITION_ID, CONDITION_ID, null);
    assertThat(rule.getRuntimeService().getVariable(processInstance.getId(), VARIABLE_NAME)).isEqualTo(1);

    //then conditional event is not triggered
    assertThat(rule.getTaskService().createTaskQuery().singleResult()).isNull();

    //when any var is set
    testHelper.setAnyVariable(processInstance.getId());

    //then condition is satisfied, since variable is already set which satisfies condition
    testHelper.completeTask(TASK_AFTER_CONDITION_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }


  @Test
  public void testIntermediateConditionalEventWithSetVariableOnStartListener() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(Bpmn.createExecutableProcess()
      .startEvent()
      .intermediateCatchEvent(CONDITION_ID)
      .conditionalEventDefinition()
        .condition(VAR_CONDITION)
      .conditionalEventDefinitionDone()
      .userTask(TASK_AFTER_CONDITION_ID)
      .endEvent()
      .done());

    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(Bpmn.createExecutableProcess()
      .startEvent()
      .subProcess()
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, SetVariableDelegate.class.getName())
      .embeddedSubProcess()
        .startEvent()
        .intermediateCatchEvent(CONDITION_ID)
        .conditionalEventDefinition()
         .condition(VAR_CONDITION)
        .conditionalEventDefinitionDone()
        .userTask(TASK_AFTER_CONDITION_ID)
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(CONDITION_ID, CONDITION_ID)
      .updateEventTrigger()
      .build();

    //when sub process is added, start listener is called and sets variable
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);
    testHelper.assertEventSubscriptionMigrated(CONDITION_ID, CONDITION_ID, null);
    assertThat(rule.getRuntimeService().getVariable(processInstance.getId(), VARIABLE_NAME)).isEqualTo(1);

    //then conditional event is not triggered
    assertThat(rule.getTaskService().createTaskQuery().singleResult()).isNull();

    //when any var is set
    testHelper.setAnyVariable(processInstance.getId());

    //then condition is satisfied, since variable is already set which satisfies condition
    testHelper.completeTask(TASK_AFTER_CONDITION_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void testBoundaryConditionalEventWithSetVariableOnStartListener() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
      .userTaskBuilder(USER_TASK_ID)
      .boundaryEvent(BOUNDARY_ID)
        .condition(VAR_CONDITION)
        .userTask(TASK_AFTER_CONDITION_ID)
        .endEvent()
      .done());

    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(
      Bpmn.createExecutableProcess()
        .startEvent()
        .subProcess(SUB_PROCESS_ID)
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, SetVariableDelegate.class.getName())
        .embeddedSubProcess()
          .startEvent()
          .userTask(USER_TASK_ID)
          .endEvent()
        .subProcessDone()
        .endEvent()
        .done())
      .userTaskBuilder(USER_TASK_ID)
      .boundaryEvent(BOUNDARY_ID)
        .condition(VAR_CONDITION)
        .endEvent()
      .moveToActivity(SUB_PROCESS_ID)
      .boundaryEvent()
        .condition(VAR_CONDITION)
        .userTask(TASK_AFTER_CONDITION_ID)
        .endEvent()
      .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(USER_TASK_ID, USER_TASK_ID)
      .mapActivities(BOUNDARY_ID, BOUNDARY_ID)
      .updateEventTrigger()
      .build();

    //when sub process is added, start listener is called and sets variable
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);
    testHelper.assertEventSubscriptionMigrated(BOUNDARY_ID, BOUNDARY_ID, null);
    assertThat(rule.getRuntimeService().getVariable(processInstance.getId(), VARIABLE_NAME)).isEqualTo(1);

    //then conditional event is not triggered
    assertThat(rule.getTaskService().createTaskQuery().singleResult().getTaskDefinitionKey()).isEqualTo(USER_TASK_ID);

    //when any var is set
    testHelper.setAnyVariable(processInstance.getId());

    //then condition is satisfied, since variable is already set which satisfies condition
    testHelper.completeTask(TASK_AFTER_CONDITION_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void testBoundaryConditionalEventWithSetVariableOnEndListener() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(modify(
      Bpmn.createExecutableProcess(PROC_DEF_KEY)
        .startEvent()
        .subProcess(SUB_PROCESS_ID)
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, SetVariableDelegate.class.getName())
        .embeddedSubProcess()
          .startEvent()
          .userTask(USER_TASK_ID)
          .endEvent()
        .subProcessDone()
        .endEvent()
        .done())
      .userTaskBuilder(USER_TASK_ID)
      .boundaryEvent(BOUNDARY_ID)
        .condition(VAR_CONDITION)
        .endEvent()
      .moveToActivity(SUB_PROCESS_ID)
      .boundaryEvent()
        .condition(VAR_CONDITION)
        .userTask(TASK_AFTER_CONDITION_ID)
        .endEvent()
      .done());
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
      .userTaskBuilder(USER_TASK_ID)
      .boundaryEvent(BOUNDARY_ID)
        .condition(VAR_CONDITION)
        .userTask(TASK_AFTER_CONDITION_ID)
        .endEvent()
      .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(USER_TASK_ID, USER_TASK_ID)
      .mapActivities(BOUNDARY_ID, BOUNDARY_ID)
      .updateEventTrigger()
      .build();

    //when sub process is removed, end listener is called and sets variable
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);
    testHelper.assertEventSubscriptionMigrated(BOUNDARY_ID, BOUNDARY_ID, null);
    assertThat(rule.getRuntimeService().getVariable(processInstance.getId(), VARIABLE_NAME)).isEqualTo(1);

    //then conditional event is not triggered
    assertThat(rule.getTaskService().createTaskQuery().singleResult().getTaskDefinitionKey()).isEqualTo(USER_TASK_ID);

    //when any var is set
    testHelper.setAnyVariable(processInstance.getId());

    //then condition is satisfied, since variable is already set which satisfies condition
    testHelper.completeTask(TASK_AFTER_CONDITION_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void tesConditionalEventSubProcessWithSetVariableOnStartListener() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
      .addSubProcessTo(PROC_DEF_KEY)
      .triggerByEvent()
      .embeddedSubProcess()
        .startEvent(EVENT_SUB_PROCESS_START_ID)
        .condition(VAR_CONDITION)
        .userTask(TASK_AFTER_CONDITION_ID)
        .endEvent()
      .done());

    BpmnModelInstance targetModel = modify(
      Bpmn.createExecutableProcess(PROC_DEF_KEY)
        .startEvent()
        .subProcess(SUB_PROCESS_ID)
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, SetVariableDelegate.class.getName())
        .embeddedSubProcess()
          .startEvent()
          .userTask(USER_TASK_ID)
          .endEvent()
        .subProcessDone()
        .endEvent()
        .done())
      .addSubProcessTo(SUB_PROCESS_ID)
      .triggerByEvent()
      .embeddedSubProcess()
        .startEvent()
        .condition(VAR_CONDITION)
        .endEvent()
      .done();

    targetModel = modify(targetModel)
      .addSubProcessTo(PROC_DEF_KEY)
      .triggerByEvent()
      .embeddedSubProcess()
        .startEvent(EVENT_SUB_PROCESS_START_ID)
        .condition(VAR_CONDITION)
        .userTask(TASK_AFTER_CONDITION_ID)
        .endEvent()
      .done();
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetModel);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(USER_TASK_ID, USER_TASK_ID)
      .mapActivities(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID)
      .updateEventTrigger()
      .build();

    //when sub process is added, start listener is called and sets variable
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);
    testHelper.assertEventSubscriptionMigrated(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID, null);
    assertThat(rule.getRuntimeService().getVariable(processInstance.getId(), VARIABLE_NAME)).isEqualTo(1);

    //then conditional event is not triggered
    assertThat(rule.getTaskService().createTaskQuery().singleResult().getTaskDefinitionKey()).isEqualTo(USER_TASK_ID);

    //when any var is set
    testHelper.setAnyVariable(processInstance.getId());

    //then condition is satisfied, since variable is already set which satisfies condition
    testHelper.completeTask(TASK_AFTER_CONDITION_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void testConditionalEventSubProcessWithSetVariableOnEndListener() {
    // given
    BpmnModelInstance sourceModel = modify(
      Bpmn.createExecutableProcess(PROC_DEF_KEY)
        .startEvent()
        .subProcess(SUB_PROCESS_ID)
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, SetVariableDelegate.class.getName())
        .embeddedSubProcess()
          .startEvent()
          .userTask(USER_TASK_ID)
          .endEvent()
        .subProcessDone()
        .endEvent()
        .done())
      .addSubProcessTo(PROC_DEF_KEY)
      .triggerByEvent()
      .embeddedSubProcess()
        .startEvent(EVENT_SUB_PROCESS_START_ID)
          .condition(VAR_CONDITION)
        .endEvent()
      .done();

    sourceModel = modify(sourceModel)
      .addSubProcessTo(SUB_PROCESS_ID)
      .triggerByEvent()
      .embeddedSubProcess()
        .startEvent()
        .condition(VAR_CONDITION)
        .userTask(TASK_AFTER_CONDITION_ID)
        .endEvent()
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceModel);

    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
      .addSubProcessTo(PROC_DEF_KEY)
      .triggerByEvent()
      .embeddedSubProcess()
        .startEvent(EVENT_SUB_PROCESS_START_ID)
        .condition(VAR_CONDITION)
        .userTask(TASK_AFTER_CONDITION_ID)
        .endEvent()
      .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(USER_TASK_ID, USER_TASK_ID)
      .mapActivities(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID)
      .updateEventTrigger()
      .build();

    //when sub process is removed, end listener is called and sets variable
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);
    testHelper.assertEventSubscriptionMigrated(EVENT_SUB_PROCESS_START_ID, EVENT_SUB_PROCESS_START_ID, null);
    assertThat(rule.getRuntimeService().getVariable(processInstance.getId(), VARIABLE_NAME)).isEqualTo(1);

    //then conditional event is not triggered
    assertThat(rule.getTaskService().createTaskQuery().singleResult().getTaskDefinitionKey()).isEqualTo(USER_TASK_ID);

    //when any var is set
    testHelper.setAnyVariable(processInstance.getId());

    //then condition is satisfied, since variable is already set which satisfies condition
    testHelper.completeTask(TASK_AFTER_CONDITION_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }
}
