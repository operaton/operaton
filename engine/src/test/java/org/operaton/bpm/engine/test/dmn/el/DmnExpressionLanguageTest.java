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
package org.operaton.bpm.engine.test.dmn.el;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ProcessEngineExtension.class)
class DmnExpressionLanguageTest {

  private static final String JUEL_EXPRESSIONS_WITH_PROPERTIES_DMN =
      "org/operaton/bpm/engine/test/dmn/el/DmnExpressionLanguageTest.dmn";

  DecisionService decisionService;

  @Test
  @Deployment(resources = JUEL_EXPRESSIONS_WITH_PROPERTIES_DMN)
  void testJuelDoesNotShadowInnerProperty() {
    VariableMap inputs = Variables.createVariables();
    inputs.putValue("testExpr", "TestProperty");

    Map<String, Object> mapVar = new HashMap<>(1);
    mapVar.put("b", "B_FROM_MAP");
    inputs.putValue("a", mapVar);
    inputs.putValue("b", "B_FROM_CONTEXT");

    DmnDecisionTableResult result = decisionService.evaluateDecisionTableByKey("decision_1", inputs);

    assertThat((String)result.getSingleEntry()).isEqualTo("B_FROM_MAP");
  }

  @Test
  @Deployment(resources = JUEL_EXPRESSIONS_WITH_PROPERTIES_DMN)
  void testJuelResolvesListIndex() {
    VariableMap inputs = Variables.createVariables();
    inputs.putValue("testExpr", "TestListIndex");

    List<String> listVar = new ArrayList<>(1);
    listVar.add("0_FROM_LIST");
    inputs.putValue("a", listVar);

    DmnDecisionTableResult result = decisionService.evaluateDecisionTableByKey("decision_1", inputs);

    assertThat((String)result.getSingleEntry()).isEqualTo("0_FROM_LIST");
  }
}
