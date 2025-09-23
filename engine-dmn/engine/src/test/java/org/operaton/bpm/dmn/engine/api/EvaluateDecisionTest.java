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

import org.junit.jupiter.api.Test;

import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;

import static org.assertj.core.api.Assertions.entry;

class EvaluateDecisionTest extends DmnEngineTest {

  private static final String NO_INPUT_DMN = "org/operaton/bpm/dmn/engine/api/NoInput.dmn";
  private static final String ONE_RULE_DMN = "org/operaton/bpm/dmn/engine/api/OneRule.dmn";
  private static final String EXAMPLE_DMN = "org/operaton/bpm/dmn/engine/api/Example.dmn";
  private static final String DATA_TYPE_DMN = "org/operaton/bpm/dmn/engine/api/DataType.dmn";

  private static final String DMN12_NO_INPUT_DMN = "org/operaton/bpm/dmn/engine/api/dmn12/NoInput.dmn";
  private static final String DMN13_NO_INPUT_DMN = "org/operaton/bpm/dmn/engine/api/dmn13/NoInput.dmn";

  @Override
  protected DmnEngineConfiguration getDmnEngineConfiguration() {
    return new DefaultDmnEngineConfiguration()
      .enableFeelLegacyBehavior(true);
  }

  @Test
  @DecisionResource(resource = NO_INPUT_DMN)
  void shouldEvaluateRuleWithoutInput() {
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry("ok");
  }

  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  void shouldEvaluateSingleRule() {
    variables.putValue("input", "ok");

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry("ok");

    variables.putValue("input", "notok");

    assertThatDecisionTableResult()
      .isEmpty();
  }

  @Test
  @DecisionResource(resource = EXAMPLE_DMN)
  void shouldEvaluateExample() {
    variables.put("status", "bronze");
    variables.put("sum", 200);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .containsOnly(entry("result", "notok"), entry("reason", "work on your status first, as bronze you're not going to get anything"));

    variables.put("status", "silver");

    assertThatDecisionTableResult()
      .hasSingleResult()
      .containsOnly(entry("result", "ok"), entry("reason", "you little fish will get what you want"));

    variables.put("sum", 1200);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .containsOnly(entry("result", "notok"), entry("reason", "you took too much man, you took too much!"));

    variables.put("status", "gold");
    variables.put("sum", 200);


    assertThatDecisionTableResult()
      .hasSingleResult()
      .containsOnly(entry("result", "ok"), entry("reason", "you get anything you want"));
  }

  @Test
  @DecisionResource(resource = DATA_TYPE_DMN)
  void shouldDetectDataTypes() {
    variables.put("boolean", true);
    variables.put("integer", 9000);
    variables.put("double", 13.37);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(true);

    variables.put("boolean", false);
    variables.put("integer", 10000);
    variables.put("double", 21.42);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(true);

    variables.put("boolean", true);
    variables.put("integer", -9000);
    variables.put("double", -13.37);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(true);
  }

  @Test
  @DecisionResource(resource = DMN12_NO_INPUT_DMN)
  void shouldEvaluateRuleWithoutInput_Dmn12() {
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry("ok");
  }

  @Test
  @DecisionResource(resource = DMN13_NO_INPUT_DMN)
  void shouldEvaluateRuleWithoutInput_Dmn13() {
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry("ok");
  }

}

