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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.history.HistoricDecisionInputInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionOutputInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.spin.DataFormats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ProcessEngineExtension.class)
class HistoricDecisionInstanceSerializationTest {
  DecisionService decisionService;
  HistoryService historyService;
  @Deployment(resources = {"org/operaton/spin/plugin/DecisionSingleOutput.dmn11.xml"})
  @Test
  void listJsonProperty() {
    JsonListSerializable<String> list = new JsonListSerializable<>();
    list.addElement("foo");

    ObjectValue objectValue = Variables.objectValue(list).serializationDataFormat(DataFormats.JSON_DATAFORMAT_NAME).create();

    VariableMap variables = Variables.createVariables()
      .putValueTyped("input1", objectValue);

    decisionService.evaluateDecisionTableByKey("testDecision", variables);

    HistoricDecisionInstance testDecision = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey("testDecision").includeInputs().includeOutputs().singleResult();
    assertNotNull(testDecision);

    List<HistoricDecisionInputInstance> inputs = testDecision.getInputs();
    assertThat(inputs).hasSize(1);

    HistoricDecisionInputInstance inputInstance = inputs.get(0);
    assertThat(inputInstance.getValue()).isEqualTo(list.getListProperty());

    List<HistoricDecisionOutputInstance> outputs = testDecision.getOutputs();
    assertThat(outputs).hasSize(1);

    HistoricDecisionOutputInstance outputInstance = outputs.get(0);
    assertThat(outputInstance.getValue()).isEqualTo(list.getListProperty());

  }

}
