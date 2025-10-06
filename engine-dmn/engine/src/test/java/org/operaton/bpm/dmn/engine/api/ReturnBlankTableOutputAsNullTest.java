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
package org.operaton.bpm.dmn.engine.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class ReturnBlankTableOutputAsNullTest extends DmnEngineTest {

  private static final String RESULT_TEST_DMN = "ReturnBlankTableOutputAsNull.dmn";

  @BeforeEach
  void configure() {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) dmnEngine.getConfiguration();
    configuration.setReturnBlankTableOutputAsNull(true);
  }

  @AfterEach
  void reset() {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) dmnEngine.getConfiguration();
    configuration.setReturnBlankTableOutputAsNull(false);
  }

  @ParameterizedTest(name = "{index} => {0}")
  @CsvSource({
    "'Expression is null', 'A'",
    "'Text tag is empty', 'B'",
    "'Output entry is empty', 'D'"
  })
  @DecisionResource(resource = RESULT_TEST_DMN)
  @DisplayName("Test cases for null outputs")
  @SuppressWarnings("unused")
  void shouldReturnNullForVariousInputs(String testName, String name) {
    // given

    // when
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, Variables.putValue("name", name));

    // then
    assertThat(decisionResult).hasSize(1);
    assertThat(decisionResult.getSingleResult().getEntryMap())
      .containsOnly(entry("output", null));
  }

  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  @DisplayName("Test case for empty output")
  void shouldReturnEmpty() {
    // given

    // when
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, Variables.putValue("name", "C"));

    // then
    assertThat(decisionResult).hasSize(1);
    assertThat(decisionResult.getSingleResult().getEntryMap())
      .containsOnly(entry("output", ""));
  }
}
