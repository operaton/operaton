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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionLiteralExpressionImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableInputImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableRuleImpl;
import org.operaton.bpm.dmn.engine.impl.DmnExpressionImpl;
import org.operaton.bpm.dmn.engine.impl.el.DefaultScriptEngineResolver;
import org.operaton.bpm.dmn.engine.impl.el.JuelElProvider;
import org.operaton.bpm.dmn.engine.impl.spi.el.DmnScriptEngineResolver;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElProvider;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.dmn.feel.impl.FeelException;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.operaton.bpm.dmn.engine.util.DmnExampleVerifier.assertExample;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ExpressionLanguageTest extends DmnEngineTest {

  public static final String GROOVY_DECISION_TABLE_DMN = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.groovy.decisionTable.dmn";
  public static final String GROOVY_DECISION_LITERAL_EXPRESSION_DMN = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.groovy.decisionLiteralExpression.dmn";
  public static final String SCRIPT_DMN = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.script.dmn";
  public static final String EMPTY_EXPRESSIONS_DMN = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.emptyExpressions.dmn";
  public static final String DECISION_WITH_LITERAL_EXPRESSION_DMN = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.decisionLiteralExpression.dmn";
  public static final String CAPITAL_JUEL_DMN = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.JUEL.dmn";
  public static final String JUEL_EXPRESSIONS_WITH_PROPERTIES_DMN = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.JUEL.expressionsWithProperties.dmn";
  public static final String JUEL = "juel";

  protected DefaultScriptEngineResolver scriptEngineResolver;
  protected JuelElProvider elProvider;

    /**
  * Returns the DmnEngineConfiguration with the default configuration settings.
  * 
  * @return the DmnEngineConfiguration with default settings
  */
  @Override
  public DmnEngineConfiguration getDmnEngineConfiguration() {
    DefaultDmnEngineConfiguration configuration = new DefaultDmnEngineConfiguration();

    configuration.setScriptEngineResolver(createScriptEngineResolver());
    configuration.setElProvider(createElProvider());
    configuration.enableFeelLegacyBehavior(true);

    return configuration;
  }

    /**
   * Creates and returns a new ElProvider object by spying on a new instance of JuelElProvider.
   * 
   * @return the newly created ElProvider object
   */
  protected ElProvider createElProvider() {
    elProvider = spy(new JuelElProvider());
    return elProvider;
  }

    /**
   * Creates a script engine resolver by creating a new instance of DefaultScriptEngineResolver
   * and returns it.
   * 
   * @return the created script engine resolver
   */
  protected DmnScriptEngineResolver createScriptEngineResolver() {
    scriptEngineResolver = spy(new DefaultScriptEngineResolver());
    return scriptEngineResolver;
  }

    /**
   * Test method for evaluating a decision table with global expression language set to "groovy".
   */
  @Test
  @DecisionResource(resource = GROOVY_DECISION_TABLE_DMN)
  public void testGlobalExpressionLanguageDecisionTable() {
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

    /**
   * Test method for evaluating a decision with a global expression language set to groovy.
   */
  @Test
  @DecisionResource(resource = GROOVY_DECISION_LITERAL_EXPRESSION_DMN)
  public void testGlobalExpressionLanguageDecisionLiteralExpression() {
    DmnDecisionLiteralExpressionImpl decisionLiteralExpression = (DmnDecisionLiteralExpressionImpl) decision.getDecisionLogic();

    assertThat(decisionLiteralExpression.getExpression().getExpressionLanguage()).isEqualTo("groovy");

    dmnEngine.evaluateDecision(decision,
        Variables.createVariables().putValue("a", 2).putValue("b", 3));

    verify(scriptEngineResolver, atLeastOnce()).getScriptEngineForLanguage("groovy");
    verify(scriptEngineResolver, never()).getScriptEngineForLanguage(JUEL);
  }

    /**
   * Test the execution of the default DMN engine configuration by asserting an example with the DMN engine
   * and verifying that the EL provider creates an expression at least once with any string.
   */
  @Test
  public void testExecuteDefaultDmnEngineConfiguration() {
    assertExample(dmnEngine);

    verify(elProvider, atLeastOnce()).createExpression(anyString());
  }

    /**
   * Test method to execute a decision using the JUEL DMN engine configuration.
   */
  @Test
  @DecisionResource(resource = SCRIPT_DMN)
  public void testExecuteJuelDmnEngineConfiguration() {
    DmnEngine juelEngine = createEngineWithDefaultExpressionLanguage(JUEL);
    assertExample(juelEngine, decision);

    verify(elProvider, atLeastOnce()).createExpression(anyString());
  }

    /**
   * Test method for executing a Groovy DMN engine configuration. 
   */
  @Test
  @DecisionResource(resource = SCRIPT_DMN)
  public void testExecuteGroovyDmnEngineConfiguration() {
    DmnEngine groovyEngine = createEngineWithDefaultExpressionLanguage("groovy");
    assertExample(groovyEngine, decision);

    verify(scriptEngineResolver, atLeastOnce()).getScriptEngineForLanguage("groovy");
    verify(scriptEngineResolver, never()).getScriptEngineForLanguage(JUEL);
  }

    /**
   * This method tests the execution of a DMN decision using a JavaScript engine configuration.
   */
  @Test
  @DecisionResource(resource = SCRIPT_DMN)
  public void testExecuteJavascriptDmnEngineConfiguration() {
    DmnEngine javascriptEngine = createEngineWithDefaultExpressionLanguage("javascript");
    assertExample(javascriptEngine, decision);

    verify(scriptEngineResolver, atLeastOnce()).getScriptEngineForLanguage("javascript");
    verify(scriptEngineResolver, never()).getScriptEngineForLanguage(JUEL);
  }

    /**
   * Test method to evaluate a decision with a literal expression using the default DMN engine configuration.
   */
  @Test
  @DecisionResource(resource = DECISION_WITH_LITERAL_EXPRESSION_DMN)
  public void testExecuteLiteralExpressionWithDefaultDmnEngineConfiguration() {
    dmnEngine.evaluateDecision(decision,
        Variables.createVariables().putValue("a", 1).putValue("b", 2));

    verify(elProvider, atLeastOnce()).createExpression(anyString());
  }

    /**
   * Test method to execute a literal expression using the Groovy DMN engine configuration.
   */
  @Test
  @DecisionResource(resource = DECISION_WITH_LITERAL_EXPRESSION_DMN)
  public void testExecuteLiteralExpressionWithGroovyDmnEngineConfiguration() {
    DmnEngine juelEngine = createEngineWithDefaultExpressionLanguage("groovy");

    juelEngine.evaluateDecision(decision,
        Variables.createVariables().putValue("a", 1).putValue("b", 2));

    verify(scriptEngineResolver, atLeastOnce()).getScriptEngineForLanguage("groovy");
    verify(scriptEngineResolver, never()).getScriptEngineForLanguage("juel");
  }

    /**
   * Test method to verify that the default empty expressions are created correctly.
   */
  @Test
  @DecisionResource(resource = EMPTY_EXPRESSIONS_DMN)
  public void testDefaultEmptyExpressions() {
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(true);

    verify(elProvider).createExpression(anyString());
  }

    /**
   * Test method to verify the behavior of JUEL empty expressions in a decision table result.
   * It creates a DMN engine with default expression language JUEL, asserts that the decision table result has a single result with a single entry as true,
   * and verifies the creation of an expression provider.
   */
  @Test
  @DecisionResource(resource = EMPTY_EXPRESSIONS_DMN)
  public void testJuelEmptyExpressions() {
    dmnEngine = createEngineWithDefaultExpressionLanguage(JUEL);
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(true);

    verify(elProvider).createExpression(anyString());
  }

    /**
   * Test method to verify the behavior of an empty Groovy expression in a decision table.
   */
  @Test
  @DecisionResource(resource = EMPTY_EXPRESSIONS_DMN)
  public void testGroovyEmptyExpressions() {
    dmnEngine = createEngineWithDefaultExpressionLanguage("groovy");
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(true);

    verify(scriptEngineResolver).getScriptEngineForLanguage("groovy");
  }

    /**
   * Test method to verify that empty expressions in a decision table using JavaScript as the expression language
   * results in the expected outcome.
   */
  @Test
  @DecisionResource(resource = EMPTY_EXPRESSIONS_DMN)
  public void testJavascriptEmptyExpressions() {
    dmnEngine = createEngineWithDefaultExpressionLanguage("javascript");
    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(true);

    verify(scriptEngineResolver).getScriptEngineForLanguage("javascript");
  }

    /**
   * Test method to verify failure when using an empty input expression in a decision table.
   */
  @Test
  @DecisionResource(resource = EMPTY_EXPRESSIONS_DMN, decisionKey = "decision2")
  public void testFailFeelUseOfEmptyInputExpression() {
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

    /**
   * Test the resolution of EL expressions using the JUEL engine.
   *
   * @throws Exception if an error occurs during the test
   */
  @Test
  @DecisionResource(resource = CAPITAL_JUEL_DMN)
  public void testElResolution () throws Exception {
    DmnEngine juelEngine = createEngineWithDefaultExpressionLanguage(JUEL);
    assertExample(juelEngine, decision);

    verify(elProvider, atLeastOnce()).createExpression(anyString());
    verify(scriptEngineResolver, never()).getScriptEngineForLanguage(JUEL.toUpperCase());
  }

    /**
   * This method tests that JUEL expressions do not shadow inner property values.
   */
  @Test
  @DecisionResource(resource = JUEL_EXPRESSIONS_WITH_PROPERTIES_DMN)
  public void testJuelDoesNotShadowInnerProperty() {
    VariableMap inputs = Variables.createVariables();
    inputs.putValue("testExpr", "TestProperty");

    Map<String, Object> mapVar = new HashMap<>(1);
    mapVar.put("b", "B_FROM_MAP");
    inputs.putValue("a", mapVar);
    inputs.putValue("b", "B_FROM_CONTEXT");

    DmnDecisionResult result = dmnEngine.evaluateDecision(decision, inputs.asVariableContext());

    assertThat((String) result.getSingleEntry()).isEqualTo("B_FROM_MAP");
  }

    /**
   * This method tests if JUEL can resolve the index of an element in a list.
   */
  @Test
  @DecisionResource(resource = JUEL_EXPRESSIONS_WITH_PROPERTIES_DMN)
  public void testJuelResolvesListIndex() {
    VariableMap inputs = Variables.createVariables();
    inputs.putValue("testExpr", "TestListIndex");

    List<String> listVar = new ArrayList<>(1);
    listVar.add("0_FROM_LIST");
    inputs.putValue("a", listVar);

    DmnDecisionResult result = dmnEngine.evaluateDecision(decision, inputs.asVariableContext());

    assertThat((String) result.getSingleEntry()).isEqualTo("0_FROM_LIST");
  }

    /**
   * Creates a DMN engine with the specified default expression language for input expressions, input entries, output entries, and literal expressions.
   * 
   * @param expressionLanguage the default expression language to be set for all expression types
   * @return the DMN engine created with the specified default expression language
   */
  protected DmnEngine createEngineWithDefaultExpressionLanguage(String expressionLanguage) {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) getDmnEngineConfiguration();

    configuration.setDefaultInputExpressionExpressionLanguage(expressionLanguage);
    configuration.setDefaultInputEntryExpressionLanguage(expressionLanguage);
    configuration.setDefaultOutputEntryExpressionLanguage(expressionLanguage);
    configuration.setDefaultLiteralExpressionLanguage(expressionLanguage);

    return configuration.buildEngine();
  }

}
