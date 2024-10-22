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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.operaton.bpm.dmn.engine.util.DmnExampleVerifier.assertExample;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.dmn.feel.impl.FeelEngine;
import org.operaton.bpm.dmn.feel.impl.FeelEngineFactory;
import org.operaton.bpm.dmn.feel.impl.FeelException;
import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineFactoryImpl;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.junit.Test;

public class FeelIntegrationTest extends DmnEngineTest {

  protected static final String DMN = "org/operaton/bpm/dmn/engine/el/FeelIntegrationTest.dmn";
  protected static final String DMN_12 = "org/operaton/bpm/dmn/engine/el/dmn12/FeelIntegrationTest.dmn";
  protected static final String DMN_13 = "org/operaton/bpm/dmn/engine/el/dmn13/FeelIntegrationTest.dmn";

  protected FeelEngine feelEngineSpy;

    /**
   * Returns the DMN engine configuration with customized settings.
   *
   * @return the DMN engine configuration
   */
  @Override
  public DmnEngineConfiguration getDmnEngineConfiguration() {
    DefaultDmnEngineConfiguration configuration = new DefaultDmnEngineConfiguration();
    configuration.enableFeelLegacyBehavior(true);
    configuration.setFeelEngineFactory(new TestFeelEngineFactory());
    return configuration;
  }

    /**
   * Test method to evaluate a decision using FEEL input entry.
   */
  @Test
  @DecisionResource(resource = DMN)
  public void testFeelInputEntry() {
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, Variables.createVariables().putValue("score", 3));

    assertThat((String) decisionResult.getSingleEntry()).isEqualTo("a");

    verify(feelEngineSpy, atLeastOnce()).evaluateSimpleUnaryTests(anyString(), anyString(), any(VariableContext.class));
  }

    /**
   * Test method to evaluate a decision using FEEL input entry with alternative name.
   */
  @Test
  @DecisionResource(resource = DMN)
  public void testFeelInputEntryWithAlternativeName() {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) getDmnEngineConfiguration();
    configuration.setDefaultInputEntryExpressionLanguage("feel");
    DmnEngine dmnEngine = configuration.buildEngine();

    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, Variables.createVariables().putValue("score", 3));

    assertThat((String) decisionResult.getSingleEntry()).isEqualTo("a");

    verify(feelEngineSpy, atLeastOnce()).evaluateSimpleUnaryTests(anyString(), anyString(), any(VariableContext.class));
  }

    /**
   * Test method to verify that FEEL exception does not contain JUEL expression.
   */
  @Test
  @DecisionResource(resource = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.script.dmn")
  public void testFeelExceptionDoesNotContainJuel() {
    try {
      assertExample(dmnEngine, decision);
      failBecauseExceptionWasNotThrown(FeelException.class);
    }
    catch (FeelException e) {
      assertThat(e).hasMessageStartingWith("FEEL-01015");
      assertThat(e.getMessage()).doesNotContain("${");
    }
  }

    /**
   * This method tests the integration of date and time values by setting a specific date, 
   * formatting it, and then asserting the result against a decision table.
   */
  @Test
  @DecisionResource()
  public void testDateAndTimeIntegration() {
    Date testDate = new Date(1445526087000L);
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    variables.putValue("dateString", format.format(testDate));

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntryTyped(Variables.dateValue(testDate));
  }

    /**
   * This method tests the input expression of a decision using the FEEL expression language.
   */
  @Test
  @DecisionResource(resource = DMN)
  public void testFeelInputExpression() {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) getDmnEngineConfiguration();
    configuration.setDefaultInputExpressionExpressionLanguage(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE);
    DmnEngine engine = configuration.buildEngine();

    try {
      engine.evaluateDecision(decision, Variables.createVariables().putValue("score", 3));

      failBecauseExceptionWasNotThrown(UnsupportedOperationException.class);
    }
    catch (UnsupportedOperationException e) {
      assertThat(e).hasMessageStartingWith("FEEL-01016");
      verify(feelEngineSpy).evaluateSimpleExpression(anyString(), any(VariableContext.class));
    }
  }

    /**
   * Test the input expression for Decision Model and Notation 1.2 version.
   */
  @Test
  @DecisionResource(resource = DMN_12)
  public void testFeelInputExpression_Dmn12() {
    testFeelInputExpression();
  }

    /**
   * Test the input expression for DMN 13 by invoking the testFeelInputExpression method.
   */
  @Test
  @DecisionResource(resource = DMN_13)
  public void testFeelInputExpression_Dmn13() {
    testFeelInputExpression();
  }

    /**
   * This method tests the evaluation of a decision using a FEEL output entry expression language.
   */
  @Test
  @DecisionResource(resource = DMN)
  public void testFeelOutputEntry() {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) getDmnEngineConfiguration();
    configuration.setDefaultOutputEntryExpressionLanguage(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE);
    DmnEngine engine = configuration.buildEngine();

    try {
      engine.evaluateDecision(decision, Variables.createVariables().putValue("score", 3));

      failBecauseExceptionWasNotThrown(UnsupportedOperationException.class);
    }
    catch (UnsupportedOperationException e) {
      assertThat(e).hasMessageStartingWith("FEEL-01016");
      verify(feelEngineSpy).evaluateSimpleExpression(anyString(), any(VariableContext.class));
    }
  }

    /**
   * Test method to evaluate input expression with a custom DMN engine.
   */
  @Test
  @DecisionResource(resource = DMN)
  public void testFeelInputExpressionWithCustomEngine() {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) getDmnEngineConfiguration();
    configuration.setDefaultInputExpressionExpressionLanguage(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE);
    DmnEngine engine = configuration.buildEngine();

    // stubbing the default FEEL engine behavior
    doReturn(3).when(feelEngineSpy).evaluateSimpleExpression(eq("score"), any(VariableContext.class));

    DmnDecisionResult decisionResult = engine.evaluateDecision(decision, Variables.createVariables().putValue("score", 3));

    assertThat((String) decisionResult.getSingleEntry()).isEqualTo("a");

    verify(feelEngineSpy).evaluateSimpleExpression(anyString(), any(VariableContext.class));
  }

    /**
   * This method tests the evaluation of a FEEL output entry with a custom engine configuration.
   */
  @Test
  @DecisionResource(resource = DMN)
  public void testFeelOutputEntryWithCustomEngine() {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) getDmnEngineConfiguration();
    configuration.setDefaultOutputEntryExpressionLanguage(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE);
    DmnEngine engine = configuration.buildEngine();

    // stubbing the default FEEL engine behavior
    doReturn("a").when(feelEngineSpy).evaluateSimpleExpression(eq("\"a\""), any(VariableContext.class));

    DmnDecisionResult decisionResult = engine.evaluateDecision(decision, Variables.createVariables().putValue("score", 3));

    assertThat((String) decisionResult.getSingleEntry()).isEqualTo("a");

    verify(feelEngineSpy).evaluateSimpleExpression(anyString(), any(VariableContext.class));
  }

  public class TestFeelEngineFactory implements FeelEngineFactory {

    public TestFeelEngineFactory() {
      FeelEngineFactoryImpl feelEngineFactory = new FeelEngineFactoryImpl();
      feelEngineSpy = spy(feelEngineFactory.createInstance());
    }

        /**
     * Returns an instance of the FeelEngine.
     * 
     * @return the FeelEngine instance
     */
    public FeelEngine createInstance() {
      return feelEngineSpy;
    }

  }

}
