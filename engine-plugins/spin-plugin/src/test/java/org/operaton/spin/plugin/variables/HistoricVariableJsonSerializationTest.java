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
package org.operaton.spin.plugin.variables;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableUpdate;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.spin.DataFormats;
import static org.operaton.bpm.engine.variable.Variables.objectValue;

import java.util.List;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ProcessEngineExtension.class)
class HistoricVariableJsonSerializationTest {

  protected static final String ONE_TASK_PROCESS = "org/operaton/spin/plugin/oneTaskProcess.bpmn20.xml";

  protected static final String JSON_FORMAT_NAME = DataFormats.json().getName();

  HistoryService historyService;
  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;

  @BeforeEach
  void setUp () {
    List<String> processInstanceIds = historyService.createHistoricVariableInstanceQuery().list().stream().map(HistoricVariableInstance::getProcessInstanceId).toList();
    if (!processInstanceIds.isEmpty()) {
      historyService.deleteHistoricProcessInstances(processInstanceIds);
    }
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void selectHistoricVariableInstances() {
    if (processEngineConfiguration.getHistoryLevel().getId() >=
        HistoryLevel.HISTORY_LEVEL_AUDIT.getId()) {
      ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

      JsonSerializable bean = new JsonSerializable("a String", 42, false);
      runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(bean).serializationDataFormat(JSON_FORMAT_NAME).create());

      HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().singleResult();
      assertNotNull(historicVariable.getValue());
      assertNull(historicVariable.getErrorMessage());

      assertEquals(ValueType.OBJECT.getName(), historicVariable.getTypeName());
      assertEquals(ValueType.OBJECT.getName(), historicVariable.getVariableTypeName());

      JsonSerializable historyValue = (JsonSerializable) historicVariable.getValue();
      assertEquals(bean.getStringProperty(), historyValue.getStringProperty());
      assertEquals(bean.getIntProperty(), historyValue.getIntProperty());
      assertEquals(bean.getBooleanProperty(), historyValue.getBooleanProperty());
    }
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void selectHistoricSerializedValues() throws JSONException {
    if (processEngineConfiguration.getHistoryLevel().getId() >=
        HistoryLevel.HISTORY_LEVEL_AUDIT.getId()) {


      ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

      JsonSerializable bean = new JsonSerializable("a String", 42, false);
      runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(bean).serializationDataFormat(JSON_FORMAT_NAME));

      HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().singleResult();
      assertNotNull(historicVariable.getValue());
      assertNull(historicVariable.getErrorMessage());

      ObjectValue typedValue = (ObjectValue) historicVariable.getTypedValue();
      assertEquals(JSON_FORMAT_NAME, typedValue.getSerializationDataFormat());
      JSONAssert.assertEquals(bean.toExpectedJsonString(),new String(typedValue.getValueSerialized()), true);
      assertEquals(JsonSerializable.class.getName(), typedValue.getObjectTypeName());
    }
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void selectHistoricSerializedValuesUpdate() throws JSONException {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    JsonSerializable bean = new JsonSerializable("a String", 42, false);
    runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(bean).serializationDataFormat(JSON_FORMAT_NAME));

    if (ProcessEngineConfiguration.HISTORY_FULL.equals(processEngineConfiguration.getHistory())) {

      HistoricVariableUpdate historicUpdate = (HistoricVariableUpdate)
          historyService.createHistoricDetailQuery().variableUpdates().singleResult();

      assertNotNull(historicUpdate.getValue());
      assertNull(historicUpdate.getErrorMessage());

      assertEquals(ValueType.OBJECT.getName(), historicUpdate.getTypeName());
      assertEquals(ValueType.OBJECT.getName(), historicUpdate.getVariableTypeName());

      JsonSerializable historyValue = (JsonSerializable) historicUpdate.getValue();
      assertEquals(bean.getStringProperty(), historyValue.getStringProperty());
      assertEquals(bean.getIntProperty(), historyValue.getIntProperty());
      assertEquals(bean.getBooleanProperty(), historyValue.getBooleanProperty());

      ObjectValue typedValue = (ObjectValue) historicUpdate.getTypedValue();
      assertEquals(JSON_FORMAT_NAME, typedValue.getSerializationDataFormat());
      JSONAssert.assertEquals(bean.toExpectedJsonString(),new String(typedValue.getValueSerialized()), true);
      assertEquals(JsonSerializable.class.getName(), typedValue.getObjectTypeName());

    }
  }

}
