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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.util.ResetDmnConfigUtil;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;

public class DecisionInstanceHistoryTest {

  public static final String DECISION_SINGLE_OUTPUT_DMN = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.decisionSingleOutput.dmn11.xml";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .closeEngineAfterAllTests()
    .configurationResource("org/operaton/bpm/engine/test/standalone/history/decisionInstanceHistory.operaton.cfg.xml")
    .build();

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RepositoryService repositoryService;
  DecisionService decisionService;

  @BeforeEach
  void setUp() {
    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        processEngineConfiguration.getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(true)
        .init();
  }

  @AfterEach
  void tearDown() {
    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        processEngineConfiguration.getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(false)
        .init();
  }

  @Deployment(resources = DECISION_SINGLE_OUTPUT_DMN)
  @Test
  void testDecisionDefinitionPassedToHistoryLevel() {
    RecordHistoryLevel historyLevel = (RecordHistoryLevel) processEngineConfiguration.getHistoryLevel();
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().decisionDefinitionKey("testDecision").singleResult();

    VariableMap variables = Variables.createVariables().putValue("input1", true);
    decisionService.evaluateDecisionTableByKey("testDecision", variables);

    List<RecordHistoryLevel.ProducedHistoryEvent> producedHistoryEvents = historyLevel.getProducedHistoryEvents();
    assertThat(producedHistoryEvents).hasSize(1);

    RecordHistoryLevel.ProducedHistoryEvent producedHistoryEvent = producedHistoryEvents.get(0);
    assertThat(producedHistoryEvent.eventType).isEqualTo(HistoryEventTypes.DMN_DECISION_EVALUATE);

    DecisionDefinition entity = (DecisionDefinition) producedHistoryEvent.entity;
    assertThat(entity).isNotNull();
    assertThat(entity.getId()).isEqualTo(decisionDefinition.getId());
  }

}
