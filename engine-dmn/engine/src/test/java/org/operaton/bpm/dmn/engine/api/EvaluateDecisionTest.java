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
package org.operaton.bpm.dmn.engine.api;

import static org.assertj.core.api.Assertions.entry;

import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.junit.Test;

public class EvaluateDecisionTest extends DmnEngineTest {

  public static final String NO_INPUT_DMN = "org/operaton/bpm/dmn/engine/api/NoInput.dmn";
  public static final String ONE_RULE_DMN = "org/operaton/bpm/dmn/engine/api/OneRule.dmn";
  public static final String EXAMPLE_DMN = "org/operaton/bpm/dmn/engine/api/Example.dmn";
  public static final String DATA_TYPE_DMN = "org/operaton/bpm/dmn/engine/api/DataType.dmn";

  public static final String DMN12_NO_INPUT_DMN = "org/operaton/bpm/dmn/engine/api/dmn12/NoInput.dmn";
  public static final String DMN13_NO_INPUT_DMN = "org/operaton/bpm/dmn/engine/api/dmn13/NoInput.dmn";

    /**
   * Returns a new instance of DefaultDmnEngineConfiguration with FEEL legacy behavior enabled.
   *
   * @return the DmnEngineConfiguration with FEEL legacy behavior enabled
   */
  @Override
  public DmnEngineConfiguration getDmnEngineConfiguration() {
    return new DefaultDmnEngineConfiguration()
      .enableFeelLegacyBehavior(true);
  }

    /**
   * Test method to evaluate a rule without input and verify the result.
   */
  @Test
  @DecisionResource(resource = NO_INPUT_DMN)
  public void shouldEvaluateRuleWithoutInput() {
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry("ok");
  }

    /**
   * This method tests the evaluation of a single rule in a decision table.
   * It sets the input variable to "ok" and asserts that the decision table result has a single entry of "ok".
   * It then sets the input variable to "notok" and asserts that the decision table result is empty.
   */
  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  public void shouldEvaluateSingleRule() {
    variables.putValue("input", "ok");

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry("ok");

    variables.putValue("input", "notok");

    assertThatDecisionTableResult()
      .isEmpty();
  }

    /**
   * This method tests the evaluation of a decision table using different variables values.
   */
  @Test
  @DecisionResource(resource = EXAMPLE_DMN)
  public void shouldEvaluateExample() {
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

    /**
   * Test method to detect data types by setting boolean, integer, and double values in variables map
   */
  @Test
  @DecisionResource(resource = DATA_TYPE_DMN)
  public void shouldDetectDataTypes() {
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

    /**
   * Test method to evaluate a decision rule without input for DMN12 standard.
   */
  @Test
  @DecisionResource(resource = DMN12_NO_INPUT_DMN)
  public void shouldEvaluateRuleWithoutInput_Dmn12() {
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry("ok");
  }

    /**
   * Tests the evaluation of a decision rule without input for DMN 1.3 version
   */
  @Test
  @DecisionResource(resource = DMN13_NO_INPUT_DMN)
  public void shouldEvaluateRuleWithoutInput_Dmn13() {
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry("ok");
  }

}

