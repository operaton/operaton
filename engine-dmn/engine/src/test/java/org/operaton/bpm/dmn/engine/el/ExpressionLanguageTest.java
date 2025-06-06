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
package org.operaton.bpm.dmn.engine.el;

import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.*;
import org.operaton.bpm.dmn.engine.impl.el.DefaultScriptEngineResolver;
import org.operaton.bpm.dmn.engine.impl.el.JuelElProvider;
import org.operaton.bpm.dmn.engine.impl.spi.el.DmnScriptEngineResolver;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElProvider;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.dmn.feel.impl.FeelException;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import static org.operaton.bpm.dmn.engine.util.DmnExampleVerifier.assertExample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.*;

class ExpressionLanguageTest extends DmnEngineTest {

  private static final String GROOVY_DECISION_TABLE_DMN = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.groovy.decisionTable.dmn";
  private static final String GROOVY_DECISION_LITERAL_EXPRESSION_DMN = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.groovy.decisionLiteralExpression.dmn";
  private static final String SCRIPT_DMN = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.script.dmn";
  private static final String EMPTY_EXPRESSIONS_DMN = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.emptyExpressions.dmn";
  private static final String DECISION_WITH_LITERAL_EXPRESSION_DMN = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.decisionLiteralExpression.dmn";
  private static final String CAPITAL_JUEL_DMN = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.JUEL.dmn";
  private static final String JUEL_EXPRESSIONS_WITH_PROPERTIES_DMN = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.JUEL.expressionsWithProperties.dmn";
  private static final String JUEL = "juel";

  DefaultScriptEngineResolver scriptEngineResolver;
  JuelElProvider elProvider;

  @Override
  protected DmnEngineConfiguration getDmnEngineConfiguration() {
    DefaultDmnEngineConfiguration configuration = new DefaultDmnEngineConfiguration();

    configuration.setScriptEngineResolver(createScriptEngineResolver());
    configuration.setElProvider(createElProvider());
    configuration.enableFeelLegacyBehavior(true);

    return configuration;
  }

  protected ElProvider createElProvider() {
    elProvider = spy(new JuelElProvider());
    return elProvider;
  }

  protected DmnScriptEngineResolver createScriptEngineResolver() {
    scriptEngineResolver = spy(new DefaultScriptEngineResolver());
    return scriptEngineResolver;
  }

  @Test
  @DecisionResource(resource = GROOVY_DECISION_TABLE_DMN)
  void globalExpressionLanguageDecisionTable() {
    DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decision.getDecisionLogic();
    for (DmnDecisionTableInputImpl dmnInput : decisionTable.getInputs()) {
      assertThat(dmnInput.getExpression().getExpressionLanguage()).isEqualTo("groovy");
    }

    for (DmnDecisionTableRuleImpl dmnRule : decisionTable.getRules()) {
      for (DmnExpressionImpl condition : dmnRule.getConditions()) {
        assertThat(condition.getExpressionLanguage()).isEqualTo("groovy");
      }
      for (DmnExpressionImpl conclusion : dmnRule.getConclusions()) {
        assertThat(conclusion.getExpressionLanguage()).isEqualTo("groovy");
      }
    }

    assertExample(dmnEngine, decision);
    verify(scriptEngineResolver, atLeastOnce()).getScriptEngineForLanguage("groovy");
    verify(scriptEngineResolver, never()).getScriptEngineForLanguage(JUEL);
  }

  @Test
  @DecisionResource(resource = GROOVY_DECISION_LITERAL_EXPRESSION_DMN)
  void globalExpressionLanguageDecisionLiteralExpression() {
    DmnDecisionLiteralExpressionImpl decisionLiteralExpression = (DmnDecisionLiteralExpressionImpl) decision.getDecisionLogic();

    assertThat(decisionLiteralExpression.getExpression().getExpressionLanguage()).isEqualTo("groovy");

    dmnEngine.evaluateDecision(decision,
        Variables.createVariables().putValue("a", 2).putValue("b", 3));

    verify(scriptEngineResolver, atLeastOnce()).getScriptEngineForLanguage("groovy");
    verify(scriptEngineResolver, never()).getScriptEngineForLanguage(JUEL);
  }

  @Test
  void executeDefaultDmnEngineConfiguration() {
    assertExample(dmnEngine);

    verify(elProvider, atLeastOnce()).createExpression(anyString());
  }

  @Test
  @DecisionResource(resource = SCRIPT_DMN)
  void executeJuelDmnEngineConfiguration() {
    DmnEngine juelEngine = createEngineWithDefaultExpressionLanguage(JUEL);
    assertExample(juelEngine, decision);

    verify(elProvider, atLeastOnce()).createExpression(anyString());
  }

  @Test
  @DecisionResource(resource = SCRIPT_DMN)
  void executeGroovyDmnEngineConfiguration() {
    DmnEngine groovyEngine = createEngineWithDefaultExpressionLanguage("groovy");
    assertExample(groovyEngine, decision);

    verify(scriptEngineResolver, atLeastOnce()).getScriptEngineForLanguage("groovy");
    verify(scriptEngineResolver, never()).getScriptEngineForLanguage(JUEL);
  }

  @Test
  @DecisionResource(resource = SCRIPT_DMN)
  void executeJavascriptDmnEngineConfiguration() {
    DmnEngine javascriptEngine = createEngineWithDefaultExpressionLanguage("javascript");
    assertExample(javascriptEngine, decision);

    verify(scriptEngineResolver, atLeastOnce()).getScriptEngineForLanguage("javascript");
    verify(scriptEngineResolver, never()).getScriptEngineForLanguage(JUEL);
  }

  @Test
  @DecisionResource(resource = DECISION_WITH_LITERAL_EXPRESSION_DMN)
  void executeLiteralExpressionWithDefaultDmnEngineConfiguration() {
    dmnEngine.evaluateDecision(decision,
        Variables.createVariables().putValue("a", 1).putValue("b", 2));

    verify(elProvider, atLeastOnce()).createExpression(anyString());
  }

  @Test
  @DecisionResource(resource = DECISION_WITH_LITERAL_EXPRESSION_DMN)
  void executeLiteralExpressionWithGroovyDmnEngineConfiguration() {
    DmnEngine juelEngine = createEngineWithDefaultExpressionLanguage("groovy");

    juelEngine.evaluateDecision(decision,
        Variables.createVariables().putValue("a", 1).putValue("b", 2));

    verify(scriptEngineResolver, atLeastOnce()).getScriptEngineForLanguage("groovy");
    verify(scriptEngineResolver, never()).getScriptEngineForLanguage("juel");
  }

  @Test
  @DecisionResource(resource = EMPTY_EXPRESSIONS_DMN)
  void defaultEmptyExpressions() {
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(true);

    verify(elProvider).createExpression(anyString());
  }

  @Test
  @DecisionResource(resource = EMPTY_EXPRESSIONS_DMN)
  void juelEmptyExpressions() {
    dmnEngine = createEngineWithDefaultExpressionLanguage(JUEL);
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(true);

    verify(elProvider).createExpression(anyString());
  }

  @Test
  @DecisionResource(resource = EMPTY_EXPRESSIONS_DMN)
  void groovyEmptyExpressions() {
    dmnEngine = createEngineWithDefaultExpressionLanguage("groovy");
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(true);

    verify(scriptEngineResolver).getScriptEngineForLanguage("groovy");
  }

  @Test
  @DecisionResource(resource = EMPTY_EXPRESSIONS_DMN)
  void javascriptEmptyExpressions() {
    dmnEngine = createEngineWithDefaultExpressionLanguage("javascript");
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(true);

    verify(scriptEngineResolver).getScriptEngineForLanguage("javascript");
  }

  @Test
  @DecisionResource(resource = EMPTY_EXPRESSIONS_DMN, decisionKey = "decision2")
  void failFeelUseOfEmptyInputExpression() {
    try {
      evaluateDecisionTable();
      failBecauseExceptionWasNotThrown(FeelException.class);
    }
    catch (FeelException e) {
      assertThat(e).hasMessageStartingWith("FEEL-01017");
      assertThat(e).hasMessageContaining("'10'");
      assertThat(e.getMessage()).doesNotContain("cellInput");
    }
  }

  @Test
  @DecisionResource(resource = CAPITAL_JUEL_DMN)
  void elResolution() {
    DmnEngine juelEngine = createEngineWithDefaultExpressionLanguage(JUEL);
    assertExample(juelEngine, decision);

    verify(elProvider, atLeastOnce()).createExpression(anyString());
    verify(scriptEngineResolver, never()).getScriptEngineForLanguage(JUEL.toUpperCase());
  }

  @Test
  @DecisionResource(resource = JUEL_EXPRESSIONS_WITH_PROPERTIES_DMN)
  void juelDoesNotShadowInnerProperty() {
    VariableMap inputs = Variables.createVariables();
    inputs.putValue("testExpr", "TestProperty");

    Map<String, Object> mapVar = new HashMap<>(1);
    mapVar.put("b", "B_FROM_MAP");
    inputs.putValue("a", mapVar);
    inputs.putValue("b", "B_FROM_CONTEXT");

    DmnDecisionResult result = dmnEngine.evaluateDecision(decision, inputs.asVariableContext());

    assertThat((String) result.getSingleEntry()).isEqualTo("B_FROM_MAP");
  }

  @Test
  @DecisionResource(resource = JUEL_EXPRESSIONS_WITH_PROPERTIES_DMN)
  void juelResolvesListIndex() {
    VariableMap inputs = Variables.createVariables();
    inputs.putValue("testExpr", "TestListIndex");

    List<String> listVar = new ArrayList<>(1);
    listVar.add("0_FROM_LIST");
    inputs.putValue("a", listVar);

    DmnDecisionResult result = dmnEngine.evaluateDecision(decision, inputs.asVariableContext());

    assertThat((String) result.getSingleEntry()).isEqualTo("0_FROM_LIST");
  }

  protected DmnEngine createEngineWithDefaultExpressionLanguage(String expressionLanguage) {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) getDmnEngineConfiguration();

    configuration.setDefaultInputExpressionExpressionLanguage(expressionLanguage);
    configuration.setDefaultInputEntryExpressionLanguage(expressionLanguage);
    configuration.setDefaultOutputEntryExpressionLanguage(expressionLanguage);
    configuration.setDefaultLiteralExpressionLanguage(expressionLanguage);

    return configuration.buildEngine();
  }

}
