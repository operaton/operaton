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
package org.operaton.bpm.engine.test.standalone.history;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableUpdate;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Thorben Lindhauer
 *
 */
class CustomHistoryTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .closeEngineAfterAllTests()
    .configurationResource("org/operaton/bpm/engine/test/standalone/history/customhistory.operaton.cfg.xml")
    .build();

  RuntimeService runtimeService;
  HistoryService historyService;

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testReceivesVariableUpdates() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    String value = "a Variable Value";
    runtimeService.setVariable(instance.getId(), "aStringVariable", value);
    runtimeService.setVariable(instance.getId(), "aBytesVariable", value.getBytes());

    // then the historic variable instances and their values exist
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(2);

    HistoricVariableInstance historicStringVariable =
        historyService.createHistoricVariableInstanceQuery().variableName("aStringVariable").singleResult();
    assertThat(historicStringVariable).isNotNull();
    assertThat(historicStringVariable.getValue()).isEqualTo(value);

    HistoricVariableInstance historicBytesVariable =
        historyService.createHistoricVariableInstanceQuery().variableName("aBytesVariable").singleResult();
    assertThat(historicBytesVariable).isNotNull();
    assertThat(value.getBytes()).isEqualTo(historicBytesVariable.getValue());

    // then the historic variable updates and their values exist
    assertThat(historyService.createHistoricDetailQuery().variableUpdates().count()).isEqualTo(2);

    HistoricVariableUpdate historicStringVariableUpdate =
        (HistoricVariableUpdate) historyService.createHistoricDetailQuery()
          .variableUpdates()
          .variableInstanceId(historicStringVariable.getId())
          .singleResult();

    assertThat(historicStringVariableUpdate).isNotNull();
    assertThat(historicStringVariableUpdate.getValue()).isEqualTo(value);

    HistoricVariableUpdate historicByteVariableUpdate =
        (HistoricVariableUpdate) historyService.createHistoricDetailQuery()
          .variableUpdates()
          .variableInstanceId(historicBytesVariable.getId())
          .singleResult();
    assertThat(historicByteVariableUpdate).isNotNull();
    assertThat(value.getBytes()).isEqualTo(historicBytesVariable.getValue());
  }
}
