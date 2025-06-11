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
package org.operaton.bpm.qa.performance.engine.dmn;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.qa.performance.engine.junit.ProcessEnginePerformanceTestCase;
import org.operaton.bpm.qa.performance.engine.steps.EvaluateDecisionTableStep;

/**
 * Evaluate DMN decision tables via decision service.
 *
 * @author Philipp Ossler
 */
class DmnEnginePerformanceTest extends ProcessEnginePerformanceTestCase {

  // 1.0 => 100% - all rules of the decision table will match
  // 0.5 => 50% - half of the rules of the decision table will match
  private static final double NUMBER_OF_MATCHING_RULES = 1.0;

  // decision ids
  private static final String TWO_RULES = "twoRules";
  private static final String FIVE_RULES = "fiveRules";
  private static final String TEN_RULES = "tenRules";
  private static final String ONE_HUNDRED_RULES = "oneHundredRules";

  private static final String TWO_RULES_TWO_INPUTS = "twoRulesTwoInputs";
  private static final String FIVE_RULES_TWO_INPUTS = "fiveRulesTwoInputs";
  private static final String TEN_RULES_TWO_INPUTS = "tenRulesTwoInputs";
  private static final String ONE_HUNDRED_RULES_TWO_INPUTS = "oneHundredRulesTwoInputs";

  @Test
  @Deployment
  void twoRules() {
    performanceTest()
      .step(evaluateDecisionTableStep(TWO_RULES))
    .run();
  }

  @Test
  @Deployment
  void fiveRules() {
    performanceTest()
      .step(evaluateDecisionTableStep(FIVE_RULES))
    .run();
  }

  @Test
  @Deployment
  void tenRules() {
    performanceTest()
      .step(evaluateDecisionTableStep(TEN_RULES))
    .run();
  }

  @Test
  @Deployment
  void oneHundredRules() {
    performanceTest()
      .step(evaluateDecisionTableStep(ONE_HUNDRED_RULES))
    .run();
  }

  @Test
  @Deployment
  void twoRulesTwoInputs() {
    performanceTest()
      .step(evaluateDecisionTableStep(TWO_RULES_TWO_INPUTS))
    .run();
  }

  @Test
  @Deployment
  void fiveRulesTwoInputs() {
   performanceTest()
      .step(evaluateDecisionTableStep(FIVE_RULES_TWO_INPUTS))
    .run();
  }

  @Test
  @Deployment
  void tenRulesTwoInputs() {
    performanceTest()
      .step(evaluateDecisionTableStep(TEN_RULES_TWO_INPUTS))
    .run();
  }

  @Test
  @Deployment
  void oneHundredRulesTwoInputs() {
    performanceTest()
      .step(evaluateDecisionTableStep(ONE_HUNDRED_RULES_TWO_INPUTS))
    .run();
  }

  private EvaluateDecisionTableStep evaluateDecisionTableStep(String decisionKey) {
    Map<String, Object> variables = createVariables();

    return new EvaluateDecisionTableStep(engine, decisionKey, variables);
  }

  private Map<String, Object> createVariables() {
    return Variables.createVariables()
        .putValue("input", NUMBER_OF_MATCHING_RULES);
  }

}
