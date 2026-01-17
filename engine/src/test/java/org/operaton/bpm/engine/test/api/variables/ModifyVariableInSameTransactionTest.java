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
package org.operaton.bpm.engine.test.api.variables;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

class ModifyVariableInSameTransactionTest {
  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void testDeleteAndInsertTheSameVariableByteArray() {
    BpmnModelInstance bpmnModel =
        Bpmn.createExecutableProcess("serviceTaskProcess")
        .startEvent()
        .userTask("userTask")
        .serviceTask("service")
          .operatonClass(DeleteAndInsertVariableDelegate.class)
        .userTask("userTask1")
        .endEvent()
        .done();
    ProcessDefinition processDefinition = testHelper.deployAndGetDefinition(bpmnModel);
    VariableMap variables = Variables.createVariables().putValue("listVar", List.of( 1, 2, 3));
    ProcessInstance instance = engineRule.getRuntimeService().startProcessInstanceById(processDefinition.getId(), variables);

    Task task = engineRule.getTaskService().createTaskQuery().singleResult();
    engineRule.getTaskService().complete(task.getId());

    VariableInstance variable = engineRule.getRuntimeService().createVariableInstanceQuery().processInstanceIdIn(instance.getId()).variableName("listVar").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo("stringValue");
    HistoricVariableInstance historicVariable = engineRule.getHistoryService().createHistoricVariableInstanceQuery().singleResult();
    assertThat(historicVariable.getName()).isEqualTo(variable.getName());
    assertThat(historicVariable.getState()).isEqualTo(HistoricVariableInstance.STATE_CREATED);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void testDeleteAndInsertTheSameVariable() {
    BpmnModelInstance bpmnModel =
        Bpmn.createExecutableProcess("serviceTaskProcess")
        .startEvent()
        .userTask("userTask")
        .serviceTask("service")
          .operatonClass(DeleteAndInsertVariableDelegate.class)
        .userTask("userTask1")
        .endEvent()
        .done();
    ProcessDefinition processDefinition = testHelper.deployAndGetDefinition(bpmnModel);
    VariableMap variables = Variables.createVariables().putValue("foo", "firstValue");
    ProcessInstance instance = engineRule.getRuntimeService().startProcessInstanceById(processDefinition.getId(), variables);

    Task task = engineRule.getTaskService().createTaskQuery().singleResult();
    engineRule.getTaskService().complete(task.getId());

    VariableInstance variable = engineRule.getRuntimeService().createVariableInstanceQuery().processInstanceIdIn(instance.getId()).variableName("foo").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo("secondValue");
    HistoricVariableInstance historicVariable = engineRule.getHistoryService().createHistoricVariableInstanceQuery().singleResult();
    assertThat(historicVariable.getName()).isEqualTo(variable.getName());
    assertThat(historicVariable.getState()).isEqualTo(HistoricVariableInstance.STATE_CREATED);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void testInsertDeleteInsertTheSameVariable() {
    BpmnModelInstance bpmnModel =
        Bpmn.createExecutableProcess("serviceTaskProcess")
        .startEvent()
        .userTask("userTask")
        .serviceTask("service")
          .operatonClass(InsertDeleteInsertVariableDelegate.class)
        .userTask("userTask1")
        .endEvent()
        .done();
    ProcessDefinition processDefinition = testHelper.deployAndGetDefinition(bpmnModel);
    VariableMap variables = Variables.createVariables().putValue("listVar", List.of( 1, 2, 3));
    ProcessInstance instance = engineRule.getRuntimeService().startProcessInstanceById(processDefinition.getId(), variables);

    Task task = engineRule.getTaskService().createTaskQuery().singleResult();
    engineRule.getTaskService().complete(task.getId());

    VariableInstance variable = engineRule.getRuntimeService().createVariableInstanceQuery().processInstanceIdIn(instance.getId()).variableName("foo")
        .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo("bar");
    List<HistoricVariableInstance> historyVariables = engineRule.getHistoryService().createHistoricVariableInstanceQuery().list();
    for (HistoricVariableInstance historicVariable : historyVariables) {
      if (variable.getName().equals(historicVariable.getName())) {
        assertThat(historicVariable.getState()).isEqualTo(HistoricVariableInstance.STATE_CREATED);
        break;
      }
    }
  }
}
