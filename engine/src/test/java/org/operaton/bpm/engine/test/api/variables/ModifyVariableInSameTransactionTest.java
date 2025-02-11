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
package org.operaton.bpm.engine.test.api.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Rule;
import org.junit.Test;

public class ModifyVariableInSameTransactionTest {
  @Rule
  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  @Rule
  public ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  public void testDeleteAndInsertTheSameVariableByteArray() {
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
    VariableMap variables = Variables.createVariables().putValue("listVar", Arrays.asList(new int[] { 1, 2, 3 }));
    ProcessInstance instance = engineRule.getRuntimeService().startProcessInstanceById(processDefinition.getId(), variables);

    Task task = engineRule.getTaskService().createTaskQuery().singleResult();
    engineRule.getTaskService().complete(task.getId());

    VariableInstance variable = engineRule.getRuntimeService().createVariableInstanceQuery().processInstanceIdIn(instance.getId()).variableName("listVar").singleResult();
    assertNotNull(variable);
    assertThat(variable.getValue()).isEqualTo("stringValue");
    HistoricVariableInstance historicVariable = engineRule.getHistoryService().createHistoricVariableInstanceQuery().singleResult();
    assertThat(historicVariable.getName()).isEqualTo(variable.getName());
    assertThat(historicVariable.getState()).isEqualTo(HistoricVariableInstance.STATE_CREATED);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  public void testDeleteAndInsertTheSameVariable() {
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
    assertNotNull(variable);
    assertThat(variable.getValue()).isEqualTo("secondValue");
    HistoricVariableInstance historicVariable = engineRule.getHistoryService().createHistoricVariableInstanceQuery().singleResult();
    assertThat(historicVariable.getName()).isEqualTo(variable.getName());
    assertThat(historicVariable.getState()).isEqualTo(HistoricVariableInstance.STATE_CREATED);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  public void testInsertDeleteInsertTheSameVariable() {
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
    VariableMap variables = Variables.createVariables().putValue("listVar", Arrays.asList(new int[] { 1, 2, 3 }));
    ProcessInstance instance = engineRule.getRuntimeService().startProcessInstanceById(processDefinition.getId(), variables);

    Task task = engineRule.getTaskService().createTaskQuery().singleResult();
    engineRule.getTaskService().complete(task.getId());

    VariableInstance variable = engineRule.getRuntimeService().createVariableInstanceQuery().processInstanceIdIn(instance.getId()).variableName("foo")
        .singleResult();
    assertNotNull(variable);
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
