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
package org.operaton.bpm.dmn.engine.el;

import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.engine.variable.Variables;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionEvaluationTest extends DmnEngineTest {

  private static final String DMN_INPUT_VARIABLE = "org/operaton/bpm/dmn/engine/el/ExpressionEvaluationTest.inputVariableName.dmn";
  private static final String DMN_OVERRIDE_INPUT_VARIABLE = "org/operaton/bpm/dmn/engine/el/ExpressionEvaluationTest.overrideInputVariableName.dmn";
  private static final String DMN_VARIABLE_CONTEXT = "org/operaton/bpm/dmn/engine/el/ExpressionEvaluationTest.variableContext.dmn";
  private static final String DMN_VARIABLE_CONTEXT_WITH_INPUT_VARIABLE = "org/operaton/bpm/dmn/engine/el/ExpressionEvaluationTest.variableContextWithInputVariable.dmn";

  @Test
  @DecisionResource(resource = DMN_INPUT_VARIABLE)
  void hasInputVariableName() {
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision,
      Variables.createVariables().putValue("inVar", 2));

    assertThat((boolean) decisionResult.getSingleEntry()).isTrue();
  }

  @Test
  @DecisionResource(resource = DMN_OVERRIDE_INPUT_VARIABLE)
  void overrideInputVariableName() {
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision,
      Variables.createVariables().putValue("inVar", 2));

    assertThat((boolean) decisionResult.getSingleEntry()).isTrue();
  }

  @Test
  @DecisionResource(resource = DMN_VARIABLE_CONTEXT)
  void hasVariableContext() {
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision,
      Variables.createVariables().putValue("inVar", 3));

    assertThat((boolean) decisionResult.getSingleEntry()).isTrue();
  }

  @Test
  @DecisionResource(resource = DMN_VARIABLE_CONTEXT_WITH_INPUT_VARIABLE)
  void hasInputVariableNameInVariableContext() {
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision,
      Variables.createVariables().putValue("inVar", 3));

    assertThat((boolean) decisionResult.getSingleEntry()).isTrue();
  }

}
