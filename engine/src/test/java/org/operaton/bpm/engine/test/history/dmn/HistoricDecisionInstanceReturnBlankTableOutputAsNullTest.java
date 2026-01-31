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
package org.operaton.bpm.engine.test.history.dmn;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class HistoricDecisionInstanceReturnBlankTableOutputAsNullTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(config -> config.setDmnReturnBlankTableOutputAsNull(true))
    .build();

  public static final String RESULT_TEST_DMN = "org/operaton/bpm/engine/test/history/ReturnBlankTableOutputAsNull.dmn";

  @ParameterizedTest
  @CsvSource({
    "A, ",
    "B, ",
    "C, <empty>",
    "D, "
  })
  @Deployment(resources = RESULT_TEST_DMN)
  void shouldEvaluteToExpectedGivenName(String name, String expectedOutput) {
    // given
    expectedOutput = "<empty>".equals(expectedOutput) ? "" : expectedOutput;

    // when
    engineRule.getDecisionService().evaluateDecisionByKey("Decision_0vmcc71")
        .variables(Variables.putValue("name", name))
        .evaluate();

    // then
    HistoricDecisionInstance historicDecisionInstance = engineRule.getProcessEngine()
        .getHistoryService()
        .createHistoricDecisionInstanceQuery()
        .includeOutputs()
        .singleResult();

    assertThat(historicDecisionInstance.getOutputs())
        .extracting("variableName", "value")
        .containsOnly(tuple("output", expectedOutput));
  }

}
