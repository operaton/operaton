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
package org.operaton.bpm.engine.test.bpmn.event.compensate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * @author Thorben Lindhauer
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
class CompensateEventHistoryTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventHistoryTest.testBoundaryCompensationHandlerHistory.bpmn20.xml")
  @Test
  void testBoundaryCompensationHandlerHistoryActivityInstance() {
    // given a process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryHandlerProcess");

    // when throwing compensation
    Task beforeCompensationTask = taskService.createTaskQuery().singleResult();
    taskService.complete(beforeCompensationTask.getId());

    String compensationHandlerActivityInstanceId = runtimeService
        .getActivityInstance(processInstance.getId())
        .getActivityInstances("compensationHandler")[0]
        .getId();

    // .. and completing compensation
    Task compensationHandler = taskService.createTaskQuery().singleResult();
    taskService.complete(compensationHandler.getId());

    // then there is a historic activity instance for the compensation handler
    HistoricActivityInstance historicCompensationHandlerInstance = historyService
        .createHistoricActivityInstanceQuery()
        .activityId("compensationHandler")
        .singleResult();

    assertThat(historicCompensationHandlerInstance).isNotNull();
    assertThat(historicCompensationHandlerInstance.getId()).isEqualTo(compensationHandlerActivityInstanceId);
    assertThat(historicCompensationHandlerInstance.getParentActivityInstanceId()).isEqualTo(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventHistoryTest.testBoundaryCompensationHandlerHistory.bpmn20.xml")
  @Disabled("Fix CAM-4351")
  @Test
  void testBoundaryCompensationHandlerHistoryVariableInstance() {
    // given a process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryHandlerProcess");

    // when throwing compensation
    Task beforeCompensationTask = taskService.createTaskQuery().singleResult();
    taskService.complete(beforeCompensationTask.getId());

    String compensationHandlerActivityInstanceId = runtimeService
        .getActivityInstance(processInstance.getId())
        .getActivityInstances("compensationHandler")[0]
        .getId();

    // .. setting a variable via task service API
    Task compensationHandler = taskService.createTaskQuery().singleResult();
    runtimeService.setVariableLocal(compensationHandler.getExecutionId(), "apiVariable", "someValue");

    // .. and completing compensation
    taskService.complete(compensationHandler.getId());

    // then there is a historic variable instance for the variable set by API
    HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();

    assertThat(historicVariableInstance).isNotNull();
    assertThat(historicVariableInstance.getActivityInstanceId()).isEqualTo(compensationHandlerActivityInstanceId);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventHistoryTest.testDefaultCompensationHandlerHistory.bpmn20.xml")
  @Test
  void testDefaultCompensationHandlerHistoryActivityInstance() {
    // given a process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("defaultHandlerProcess");

    // when throwing compensation
    Task beforeCompensationTask = taskService.createTaskQuery().singleResult();
    taskService.complete(beforeCompensationTask.getId());

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    String compensationHandlerActivityInstanceId = tree
        .getActivityInstances("compensationHandler")[0]
        .getId();

    String subProcessActivityInstanceId = tree
        .getActivityInstances("subProcess")[0]
        .getId();

    // .. and completing compensation
    Task compensationHandler = taskService.createTaskQuery().singleResult();
    taskService.complete(compensationHandler.getId());

    // then there is a historic activity instance for the compensation handler
    HistoricActivityInstance historicCompensationHandlerInstance = historyService
        .createHistoricActivityInstanceQuery()
        .activityId("compensationHandler")
        .singleResult();

    assertThat(historicCompensationHandlerInstance).isNotNull();
    assertThat(historicCompensationHandlerInstance.getId()).isEqualTo(compensationHandlerActivityInstanceId);
    assertThat(historicCompensationHandlerInstance.getParentActivityInstanceId()).isEqualTo(subProcessActivityInstanceId);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventHistoryTest.testDefaultCompensationHandlerHistory.bpmn20.xml")
  @Disabled("Fix CAM-4351")
  @Test
  void testDefaultCompensationHandlerHistoryVariableInstance() {
    // given a process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("defaultHandlerProcess");

    // when throwing compensation
    Task beforeCompensationTask = taskService.createTaskQuery().singleResult();
    taskService.complete(beforeCompensationTask.getId());

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    String compensationHandlerActivityInstanceId = tree
        .getActivityInstances("compensationHandler")[0]
        .getId();

    // .. setting a variable via task service API
    Task compensationHandler = taskService.createTaskQuery().singleResult();
    runtimeService.setVariableLocal(compensationHandler.getExecutionId(), "apiVariable", "someValue");

    // .. and completing compensation
    taskService.complete(compensationHandler.getId());

    // then there is a historic variable instance for the variable set by API
    HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();

    assertThat(historicVariableInstance).isNotNull();
    assertThat(historicVariableInstance.getActivityInstanceId()).isEqualTo(compensationHandlerActivityInstanceId);
  }


}
