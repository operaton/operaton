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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.EventSubscriptionQueryImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.event.EventType;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public abstract class AbstractConditionalEventTestCase {

  protected static final String CONDITIONAL_EVENT_PROCESS_KEY = "conditionalEventProcess";
  protected static final String CONDITIONAL_EVENT = "conditionalEvent";
  protected static final String CONDITION_EXPR = "${variable == 1}";
  protected static final String EXPR_SET_VARIABLE = "${execution.setVariable(\"variable\", 1)}";
  protected static final String EXPR_SET_VARIABLE_ON_PARENT = "${execution.getParent().setVariable(\"variable\", 1)}";
  protected static final String CONDITIONAL_MODEL = "conditionalModel.bpmn20.xml";
  protected static final String CONDITIONAL_VAR_EVENTS = "create, update";
  protected static final String CONDITIONAL_VAR_EVENT_UPDATE = "update";

  protected static final String TASK_BEFORE_CONDITION = "Before Condition";
  protected static final String TASK_BEFORE_CONDITION_ID = "beforeConditionId";
  protected static final String TASK_AFTER_CONDITION = "After Condition";
  public static final String TASK_AFTER_CONDITION_ID = "afterConditionId";
  protected static final String TASK_AFTER_SERVICE_TASK = "afterServiceTask";
  protected static final String TASK_IN_SUB_PROCESS_ID = "taskInSubProcess";
  protected static final String TASK_IN_SUB_PROCESS = "Task in Subprocess";
  protected static final String TASK_WITH_CONDITION = "Task with condition";
  protected static final String TASK_WITH_CONDITION_ID = "taskWithCondition";
  protected static final String AFTER_TASK = "After Task";

  protected static final String VARIABLE_NAME = "variable";
  protected static final String TRUE_CONDITION = "${true}";
  protected static final String SUB_PROCESS_ID = "subProcess";
  protected static final String FLOW_ID = "flow";
  protected static final String DELEGATED_PROCESS_KEY = "delegatedProcess";

  protected static final BpmnModelInstance TASK_MODEL = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
          .startEvent()
          .userTask(TASK_BEFORE_CONDITION_ID)
          .name(TASK_BEFORE_CONDITION)
          .endEvent().done();
  protected static final BpmnModelInstance DELEGATED_PROCESS = Bpmn.createExecutableProcess(DELEGATED_PROCESS_KEY)
    .startEvent()
    .serviceTask()
    .operatonExpression(EXPR_SET_VARIABLE)
    .endEvent()
    .done();
  protected static final String TASK_AFTER_OUTPUT_MAPPING = "afterOutputMapping";

  protected List<Task> tasksAfterVariableIsSet;

  @RegisterExtension
  protected static ProcessEngineExtension engine = ProcessEngineExtension.builder().build();
  @RegisterExtension
  protected ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engine);

  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected RepositoryService repositoryService;
  protected HistoryService historyService;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected EventSubscriptionQueryImpl conditionEventSubscriptionQuery;

  @BeforeEach
  public void init() {
    this.conditionEventSubscriptionQuery = new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutorTxRequired()).eventType(EventType.CONDITONAL.name());
  }

  @AfterEach
  public void checkIfProcessCanBeFinished() {
    //given tasks after variable was set
    assertThat(tasksAfterVariableIsSet).isNotNull();

    //when tasks are completed
    for (Task task : tasksAfterVariableIsSet) {
      taskService.complete(task.getId());
    }

    //then
    assertThat(conditionEventSubscriptionQuery.list()).isEmpty();
    assertThat(taskService.createTaskQuery().singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNull();
    tasksAfterVariableIsSet = null;
  }



  public static void assertTaskNames(List<Task> actualTasks, String ... expectedTaskNames ) {
    assertThat(actualTasks.stream().map(Task::getName)).contains(expectedTaskNames);
  }

  // conditional event sub process //////////////////////////////////////////////////////////////////////////////////////////

  protected void deployConditionalEventSubProcess(BpmnModelInstance model, String parentId, boolean isInterrupting) {
    deployConditionalEventSubProcess(model, parentId, CONDITION_EXPR, isInterrupting);
  }

  protected void deployConditionalEventSubProcess(BpmnModelInstance model, String parentId, String conditionExpr, boolean isInterrupting) {
    final BpmnModelInstance modelInstance = addConditionalEventSubProcess(model, parentId, conditionExpr, TASK_AFTER_CONDITION_ID, isInterrupting);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());
  }


  protected BpmnModelInstance addConditionalEventSubProcess(BpmnModelInstance model, String parentId, String userTaskId, boolean isInterrupting) {
    return addConditionalEventSubProcess(model, parentId, CONDITION_EXPR, userTaskId, isInterrupting);
  }

  protected BpmnModelInstance addConditionalEventSubProcess(BpmnModelInstance model, String parentId, String conditionExpr, String userTaskId, boolean isInterrupting) {
    return modify(model)
      .addSubProcessTo(parentId)
      .triggerByEvent()
      .embeddedSubProcess()
      .startEvent()
      .interrupting(isInterrupting)
      .condition(conditionExpr)
      .userTask(userTaskId)
      .name(TASK_AFTER_CONDITION)
      .endEvent().done();
  }

  // conditional boundary event //////////////////////////////////////////////////////////////////////////////////////////


  protected void deployConditionalBoundaryEventProcess(BpmnModelInstance model, String activityId, boolean isInterrupting) {
    deployConditionalBoundaryEventProcess(model, activityId, CONDITION_EXPR, isInterrupting);
  }

  protected void deployConditionalBoundaryEventProcess(BpmnModelInstance model, String activityId, String conditionExpr, boolean isInterrupting) {
    final BpmnModelInstance modelInstance = addConditionalBoundaryEvent(model, activityId, conditionExpr, TASK_AFTER_CONDITION_ID, isInterrupting);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());
  }

  protected BpmnModelInstance addConditionalBoundaryEvent(BpmnModelInstance model,
                                                          String activityId,
                                                          String userTaskId,
                                                          boolean isInterrupting) {
    return addConditionalBoundaryEvent(model, activityId, CONDITION_EXPR, userTaskId, isInterrupting);
  }

  protected BpmnModelInstance addConditionalBoundaryEvent(BpmnModelInstance model,
                                                          String activityId,
                                                          String conditionExpr,
                                                          String userTaskId,
                                                          boolean isInterrupting) {
    return modify(model)
      .activityBuilder(activityId)
      .boundaryEvent()
        .cancelActivity(isInterrupting)
        .condition(conditionExpr)
      .userTask(userTaskId)
        .name(TASK_AFTER_CONDITION)
      .endEvent()
      .done();
  }
}
