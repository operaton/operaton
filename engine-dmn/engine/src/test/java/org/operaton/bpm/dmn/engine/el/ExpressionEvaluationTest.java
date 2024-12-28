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

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.engine.variable.Variables;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionEvaluationTest extends DmnEngineTest {

  @ParameterizedTest(name = "{index} => resource={0}, input={1}")
  @CsvSource({
    "ExpressionEvaluationTest.inputVariableName.dmn, 2",
    "ExpressionEvaluationTest.overrideInputVariableName.dmn, 2",
    "ExpressionEvaluationTest.variableContext.dmn, 3",
    "ExpressionEvaluationTest.variableContextWithInputVariable.dmn, 3"
  })
  void evaluateDecision(String resource, int input) {
    List<DmnDecision> dmnDecisions = dmnEngine.parseDecisions(getClass().getResourceAsStream(resource));
    assertThat(dmnDecisions).hasSize(1);
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(dmnDecisions.get(0),
      Variables.createVariables().putValue("inVar", input));

    assertThat((boolean) decisionResult.getSingleEntry()).isTrue();
  }
}
