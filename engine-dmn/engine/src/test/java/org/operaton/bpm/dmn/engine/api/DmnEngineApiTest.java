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

import org.operaton.bpm.dmn.engine.*;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionImpl;
import org.operaton.bpm.dmn.engine.impl.DmnEvaluationException;
import org.operaton.bpm.dmn.engine.impl.transform.DmnTransformException;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.model.dmn.Dmn;
import org.operaton.bpm.model.dmn.DmnModelInstance;
import org.operaton.commons.utils.IoUtil;
import static org.operaton.bpm.dmn.engine.test.asserts.DmnEngineTestAssertions.assertThat;
import static org.operaton.bpm.engine.variable.Variables.createVariables;
import static org.operaton.bpm.engine.variable.Variables.emptyVariableContext;

import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Simple api test making sure the api methods are there and accept the right parameters
 *
 * @author Daniel Meyer
 */
class DmnEngineApiTest extends DmnEngineTest {

  private static final String ONE_RULE_DMN = "org/operaton/bpm/dmn/engine/api/OneRule.dmn";
  private static final String NOT_A_DMN_FILE = "org/operaton/bpm/dmn/engine/api/NotADmnFile.bpmn";
  private static final String DECISION_LITERAL_EXPRESSION_DMN = "org/operaton/bpm/dmn/engine/api/DecisionWithLiteralExpression.dmn";

  private static final String INPUT_VALUE = "ok";
  private static final String EXPECTED_OUTPUT_VALUE = "ok";
  private static final String DECISION_KEY = "decision";
  private final DmnModelInstance dmnModelInstance = createDmnModelInstance();
  private final VariableMap variableMap = createVariables();
  private final VariableContext emptyVariableContext = emptyVariableContext();

  @Override
  protected DmnEngineConfiguration getDmnEngineConfiguration() {
    return new DefaultDmnEngineConfiguration().enableFeelLegacyBehavior(true);
  }

  @Test
  void shouldFailParsingIfInputStreamIsNull() {
    assertThatThrownBy(() -> dmnEngine.parseDecisions((InputStream) null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");

    assertThatThrownBy(() -> dmnEngine.parseDecision(DECISION_KEY, (InputStream) null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");
  }

  @Test
  void shouldFailParsingIfInputStreamIsInvalid() throws Exception {
    try (var is = createInvalidInputStream()) {
      assertThatThrownBy(() -> dmnEngine.parseDecisions(is))
        .isInstanceOf(DmnTransformException.class)
        .hasMessageContaining("DMN-02003");
    }

    try (var is = createInvalidInputStream()) {
      assertThatThrownBy(() -> dmnEngine.parseDecision(DECISION_KEY, is))
        .isInstanceOf(DmnTransformException.class)
        .hasMessageContaining("DMN-02003");
    }
  }

  @Test
  void shouldFailParsingIfModelInstanceIsNull() {
    assertThatThrownBy(() -> dmnEngine.parseDecisions((DmnModelInstance) null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");

    assertThatThrownBy(() -> dmnEngine.parseDecision(DECISION_KEY, (DmnModelInstance) null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");
  }

  @Test
  void shouldFailParsingIfDecisionKeyIsNull() throws Exception {
    try (var is = createInputStream()) {
      assertThatThrownBy(() -> dmnEngine.parseDecision(null, is))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("UTILS-02001");
    }

    assertThatThrownBy(() -> dmnEngine.parseDecision(null, dmnModelInstance))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");
  }

  @Test
  void shouldFailParsingIfDecisionKeyIsUnknown() throws Exception {
    try (var is = createInputStream()) {
      assertThatThrownBy(() -> dmnEngine.parseDecision("unknown", is))
        .isInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-01001")
        .hasMessageContaining("unknown");
    }

    assertThatThrownBy(() -> dmnEngine.parseDecision("unknown", dmnModelInstance))
      .isInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-01001")
      .hasMessageContaining("unknown");
  }

  @Test
  void shouldFailParsingDrgIfInputStreamIsNull() {
    assertThatThrownBy(() -> dmnEngine.parseDecisionRequirementsGraph((InputStream) null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");
  }

  @Test
  void shouldFailParsingDrgIfInputStreamIsInvalid() throws Exception {
    try (var is = createInvalidInputStream()) {
      assertThatThrownBy(() -> dmnEngine.parseDecisionRequirementsGraph(is))
        .isInstanceOf(DmnTransformException.class)
        .hasMessageContaining("DMN-02003");
    }
  }

  @Test
  void shouldFailParsingDrgIfModelInstanceIsNull() {
    assertThatThrownBy(() -> dmnEngine.parseDecisionRequirementsGraph((DmnModelInstance) null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");
  }

  @Test
  void shouldFailEvaluatingDecisionTableIfInputStreamIsNull() {
    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(DECISION_KEY, (InputStream) null, variableMap))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");

    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(DECISION_KEY, (InputStream) null, emptyVariableContext))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");
  }

  @Test
  void shouldFailEvaluatingDecisionTableIfInputStreamIsInvalid() throws Exception {
    try (var is = createInvalidInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(DECISION_KEY, is, variableMap))
        .isInstanceOf(DmnTransformException.class)
        .hasMessageContaining("DMN-02003");
    }

    try (var is = createInvalidInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(DECISION_KEY, is, emptyVariableContext))
        .isInstanceOf(DmnTransformException.class)
        .hasMessageContaining("DMN-02003");
    }
  }

  @Test
  void shouldFailEvaluatingDecisionTableIfModelInstanceIsNull() {
    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(DECISION_KEY, (DmnModelInstance) null, variableMap))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");

    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(DECISION_KEY, (DmnModelInstance) null, emptyVariableContext))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");
  }

  @Test
  void shouldFailEvaluatingDecisionTableIfDecisionKeyIsNull() throws Exception {
    try (var is = createInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(null, is, variableMap))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("UTILS-02001");
    }

    try (var is = createInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(null, is, emptyVariableContext))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("UTILS-02001");
    }

    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(null, dmnModelInstance, variableMap))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");

    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(null, dmnModelInstance, emptyVariableContext))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");
  }

  @Test
  void shouldFailEvaluatingDecisionTableIfDecisionKeyIsUnknown() throws Exception {
    try (var is = createInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable("unknown", is, variableMap))
        .isInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-01001")
        .hasMessageContaining("unknown");
    }

    try (var is = createInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable("unknown", is, emptyVariableContext))
        .isInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-01001")
        .hasMessageContaining("unknown");
    }

    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable("unknown", dmnModelInstance, variableMap))
      .isInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-01001")
      .hasMessageContaining("unknown");

    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable("unknown", dmnModelInstance, emptyVariableContext))
      .isInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-01001")
      .hasMessageContaining("unknown");
  }

  @Test
  void shouldFailEvaluatingDecisionTableIfDecisionIsNull() {
    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(null, variableMap))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");

    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(null, emptyVariableContext))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");
  }

  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  void shouldFailEvaluatingDecisionTableIfVariablesIsNull() throws Exception {
    try (var is = createInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(DECISION_KEY, is, (Map<String, Object>) null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("UTILS-02001");
    }

    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(DECISION_KEY, dmnModelInstance, (Map<String, Object>) null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");

    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(decision, (Map<String, Object>) null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");
  }

  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  void shouldFailEvaluatingDecisionTableIfVariableContextIsNull() throws Exception {
    try (var is = createInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(DECISION_KEY, is, (VariableContext) null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("UTILS-02001");
    }

    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(DECISION_KEY, dmnModelInstance, (VariableContext) null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");

    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(decision, (VariableContext) null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");
  }

  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  void shouldFailEvaluatingDecisionTableWithEmptyVariableMap() {
    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(decision, variableMap))
      .isInstanceOf(DmnEvaluationException.class)
      .hasMessageStartingWith("DMN-01002");
  }

  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  void shouldFailEvaluatingDecisionTableWithEmptyVariableContext() {
    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(decision, emptyVariableContext))
      .isInstanceOf(DmnEvaluationException.class)
      .hasMessageStartingWith("DMN-01002");
  }

  @Test
  void shouldFailEvaluatingIfInputStreamIsNull() {
    assertThatThrownBy(() -> dmnEngine.evaluateDecision(DECISION_KEY, (InputStream) null, variableMap))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");

    assertThatThrownBy(() -> dmnEngine.evaluateDecision(DECISION_KEY, (InputStream) null, emptyVariableContext))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");
  }

  @Test
  void shouldFailEvaluatingIfInputStreamIsInvalid() throws Exception {
    try (var is = createInvalidInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecision(DECISION_KEY, is, variableMap))
        .isInstanceOf(DmnTransformException.class)
        .hasMessageContaining("DMN-02003");
    }

    try (var is = createInvalidInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecision(DECISION_KEY, is, emptyVariableContext))
        .isInstanceOf(DmnTransformException.class)
        .hasMessageContaining("DMN-02003");
    }
  }

  @Test
  void shouldFailEvaluatingIfModelInstanceIsNull() {
    assertThatThrownBy(() -> dmnEngine.evaluateDecision(DECISION_KEY, (DmnModelInstance) null, variableMap))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");

    assertThatThrownBy(
      () -> dmnEngine.evaluateDecision(DECISION_KEY, (DmnModelInstance) null, emptyVariableContext))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("UTILS-02001");
  }

  @Test
  void shouldFailEvaluatingIfDecisionKeyIsNull() throws Exception {
    try (var is = createInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecision(null, is, variableMap))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("UTILS-02001");
    }

    try (var is = createInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecision(null, is, emptyVariableContext))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("UTILS-02001");
    }

    assertThatThrownBy(() -> dmnEngine.evaluateDecision(null, dmnModelInstance, variableMap))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");

    assertThatThrownBy(() -> dmnEngine.evaluateDecision(null, dmnModelInstance, emptyVariableContext))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");
  }

  @Test
  void shouldFailEvaluatingIfDecisionKeyIsUnknown() throws Exception {
    try (var is = createInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecision("unknown", is, variableMap))
        .isInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-01001")
        .hasMessageContaining("unknown");
    }

    try (var is = createInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecision("unknown", is, emptyVariableContext))
        .isInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-01001")
        .hasMessageContaining("unknown");
    }

    assertThatThrownBy(
      () -> dmnEngine.evaluateDecision("unknown", dmnModelInstance, variableMap))
      .isInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-01001")
      .hasMessageContaining("unknown");

    assertThatThrownBy(() -> dmnEngine.evaluateDecision("unknown", dmnModelInstance, emptyVariableContext))
      .isInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-01001")
      .hasMessageContaining("unknown");
  }

  @Test
  void shouldFailEvaluatingIfDecisionIsNull() {
    assertThatThrownBy(() -> dmnEngine.evaluateDecision(null, variableMap))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");

    assertThatThrownBy(() -> dmnEngine.evaluateDecision(null, emptyVariableContext))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");
  }

  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  void shouldFailEvaluatingIfVariablesIsNull() throws Exception {
    try (var is = createInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecision(DECISION_KEY, is, (Map<String, Object>) null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("UTILS-02001");
    }

    assertThatThrownBy(() -> dmnEngine.evaluateDecision(DECISION_KEY, dmnModelInstance, (Map<String, Object>) null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");

    assertThatThrownBy(() -> dmnEngine.evaluateDecision(decision, (Map<String, Object>) null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");
  }

  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  void shouldFailEvaluatingIfVariableContextIsNull() throws Exception {
    try (var is = createInputStream()) {
      assertThatThrownBy(() -> dmnEngine.evaluateDecision(DECISION_KEY, is, (VariableContext) null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("UTILS-02001");
    }

    assertThatThrownBy(() -> dmnEngine.evaluateDecision(DECISION_KEY, dmnModelInstance, (VariableContext) null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");

    assertThatThrownBy(() -> dmnEngine.evaluateDecision(decision, (VariableContext) null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("UTILS-02001");
  }

  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  void shouldFailEvaluatingDecisionWithEmptyVariableMap() {
    assertThatThrownBy(() -> dmnEngine.evaluateDecision(decision, variableMap))
      .isInstanceOf(
      DmnEvaluationException.class)
      .hasMessageStartingWith("DMN-01002");
  }

  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  void shouldFailEvaluatingDecisionWithEmptyVariableContext() {
    assertThatThrownBy(() -> dmnEngine.evaluateDecision(decision, emptyVariableContext))
      .isInstanceOf(DmnEvaluationException.class)
      .hasMessageStartingWith("DMN-01002");
  }

  @Test
  @DecisionResource(resource = DECISION_LITERAL_EXPRESSION_DMN)
  void shouldFailEvaluatingDecisionTableIfDecisionIsNotATable() {
    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(decision, variables))
      .isInstanceOf(DmnEngineException.class)
      .hasMessageStartingWith("DMN-01013");
  }

  @Test
  void shouldFailEvaluatingDecisionTableIfDecisionTypeIsNotSupported() {
    assertThatThrownBy(() -> dmnEngine.evaluateDecisionTable(mock(DmnDecision.class), variables))
      .isInstanceOf(DmnEngineException.class)
      .hasMessageStartingWith("DMN-01013");
  }

  @Test
  void shouldFailEvaluatingDecisionIfDecisionTypeIsNotSupported() {
    assertThatThrownBy(() -> dmnEngine.evaluateDecision(mock(DmnDecision.class), variables))
      .isInstanceOf(DmnEngineException.class)
      .hasMessageStartingWith("DMN-01004");
  }

  @Test
  void shouldFailEvaluatingDecisionIfDecisionLogicIsNotSupported() {
    DmnDecisionImpl decision = new DmnDecisionImpl();
    decision.setKey("decision");
    decision.setDecisionLogic(mock(DmnDecisionLogic.class));

    assertThatThrownBy(() -> dmnEngine.evaluateDecision(decision, variables))
      .isInstanceOf(DmnEngineException.class)
      .hasMessageStartingWith("DMN-01012");
  }

  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  void shouldEvaluateDecisionTableWithVariableMap() {
    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(decision,
      variableMap.putValue("input", INPUT_VALUE));
    assertThat(results).hasSingleResult().hasSingleEntry(EXPECTED_OUTPUT_VALUE);
  }

  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  void shouldEvaluateDecisionTableWithVariableContext() {
    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(decision,
      variableMap.putValue("input", INPUT_VALUE).asVariableContext());
    assertThat(results).hasSingleResult().hasSingleEntry(EXPECTED_OUTPUT_VALUE);
  }

  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  void shouldEvaluateDecisionWithVariableMap() {
    DmnDecisionResult results = dmnEngine.evaluateDecision(decision, variableMap.putValue("input", INPUT_VALUE));

    assertThat((String) results.getSingleEntry()).isNotNull().isEqualTo(EXPECTED_OUTPUT_VALUE);
  }

  @Test
  @DecisionResource(resource = ONE_RULE_DMN)
  void shouldEvaluateDecisionWithVariableContext() {
    DmnDecisionResult results = dmnEngine.evaluateDecision(decision,
      variableMap.putValue("input", INPUT_VALUE).asVariableContext());

    assertThat((String) results.getSingleEntry()).isNotNull().isEqualTo(EXPECTED_OUTPUT_VALUE);
  }

  @Test
  @DecisionResource(resource = DECISION_LITERAL_EXPRESSION_DMN)
  void shouldEvaluateDecisionLiteralExpression() {
    DmnDecisionResult results = dmnEngine.evaluateDecision(decision, variableMap.putValue("input", INPUT_VALUE));

    assertThat((String) results.getSingleEntry()).isNotNull().isEqualTo(EXPECTED_OUTPUT_VALUE);
  }

  @Test
  void shouldEvaluateDecisionOfDrg() {
    DmnDecisionRequirementsGraph drd = dmnEngine.parseDecisionRequirementsGraph(createInputStream());
    decision = drd.getDecision("decision");

    DmnDecisionTableResult results = dmnEngine.evaluateDecisionTable(decision,
      variableMap.putValue("input", INPUT_VALUE));
    assertThat(results).hasSingleResult().hasSingleEntry(EXPECTED_OUTPUT_VALUE);
  }

  // helper ///////////////////////////////////////////////////////////////////

  private InputStream createInputStream() {
    return IoUtil.fileAsStream(ONE_RULE_DMN);
  }

  private InputStream createInvalidInputStream() {
    return IoUtil.fileAsStream(NOT_A_DMN_FILE);
  }

  private DmnModelInstance createDmnModelInstance () {
    return Dmn.readModelFromStream(createInputStream());
  }

}
