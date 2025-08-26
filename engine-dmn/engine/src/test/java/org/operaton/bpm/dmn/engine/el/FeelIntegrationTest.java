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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;

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
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.context.VariableContext;

import static org.operaton.bpm.dmn.engine.util.DmnExampleVerifier.assertExample;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FeelIntegrationTest extends DmnEngineTest {

  private static final String DMN = "org/operaton/bpm/dmn/engine/el/FeelIntegrationTest.dmn";
  private static final String DMN_12 = "org/operaton/bpm/dmn/engine/el/dmn12/FeelIntegrationTest.dmn";
  private static final String DMN_13 = "org/operaton/bpm/dmn/engine/el/dmn13/FeelIntegrationTest.dmn";

  private FeelEngine feelEngineSpy;

  @Override
  protected DmnEngineConfiguration getDmnEngineConfiguration() {
    DefaultDmnEngineConfiguration configuration = new DefaultDmnEngineConfiguration();
    configuration.enableFeelLegacyBehavior(true);
    configuration.setFeelEngineFactory(new TestFeelEngineFactory());
    return configuration;
  }

  @Test
  @DecisionResource(resource = DMN)
  void feelInputEntry() {
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision,
      Variables.createVariables().putValue("score", 3));

    assertThat((String) decisionResult.getSingleEntry()).isEqualTo("a");

    verify(feelEngineSpy, atLeastOnce()).evaluateSimpleUnaryTests(anyString(), anyString(), any(VariableContext.class));
  }

  @Test
  @DecisionResource(resource = DMN)
  void feelInputEntryWithAlternativeName() {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) getDmnEngineConfiguration();
    configuration.setDefaultInputEntryExpressionLanguage("feel");
    DmnEngine dmnEngine = configuration.buildEngine();

    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision,
      Variables.createVariables().putValue("score", 3));

    assertThat((String) decisionResult.getSingleEntry()).isEqualTo("a");

    verify(feelEngineSpy, atLeastOnce()).evaluateSimpleUnaryTests(anyString(), anyString(), any(VariableContext.class));
  }

  @Test
  @DecisionResource(resource = "org/operaton/bpm/dmn/engine/el/ExpressionLanguageTest.script.dmn")
  void feelExceptionDoesNotContainJuel() {
    assertThatThrownBy(() -> assertExample(dmnEngine, decision)).isInstanceOf(FeelException.class)
      .hasMessageStartingWith("FEEL-01015")
      .hasMessageNotContaining("${");
  }

  @Test
  @DecisionResource()
  void dateAndTimeIntegration() {
    Date testDate = new Date(1445526087000L);
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    variables.putValue("dateString", format.format(testDate));

    assertThatDecisionTableResult().hasSingleResult().hasSingleEntryTyped(Variables.dateValue(testDate));
  }

  @Test
  @DecisionResource(resource = DMN)
  void feelInputExpression() {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) getDmnEngineConfiguration();
    configuration.setDefaultInputExpressionExpressionLanguage(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE);
    DmnEngine engine = configuration.buildEngine();

    VariableMap variableMap = Variables.createVariables().putValue("score", 3);
    assertThatThrownBy(() -> engine.evaluateDecision(decision, variableMap))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageStartingWith("FEEL-01016");
    verify(feelEngineSpy).evaluateSimpleExpression(anyString(), any(VariableContext.class));
  }

  @Test
  @DecisionResource(resource = DMN_12)
  void feelInputExpressionDmn12() {
    feelInputExpression();
  }

  @Test
  @DecisionResource(resource = DMN_13)
  void feelInputExpressionDmn13() {
    feelInputExpression();
  }

  @Test
  @DecisionResource(resource = DMN)
  void feelOutputEntry() {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) getDmnEngineConfiguration();
    configuration.setDefaultOutputEntryExpressionLanguage(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE);
    DmnEngine engine = configuration.buildEngine();

    VariableMap variableMap = Variables.createVariables().putValue("score", 3);
    assertThatThrownBy(() -> engine.evaluateDecision(decision, variableMap)).isInstanceOf(
      UnsupportedOperationException.class).hasMessageStartingWith("FEEL-01016");
    verify(feelEngineSpy).evaluateSimpleExpression(anyString(), any(VariableContext.class));
  }

  @Test
  @DecisionResource(resource = DMN)
  void feelInputExpressionWithCustomEngine() {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) getDmnEngineConfiguration();
    configuration.setDefaultInputExpressionExpressionLanguage(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE);
    DmnEngine engine = configuration.buildEngine();

    // stubbing the default FEEL engine behavior
    doReturn(3).when(feelEngineSpy).evaluateSimpleExpression(eq("score"), any(VariableContext.class));

    DmnDecisionResult decisionResult = engine.evaluateDecision(decision,
      Variables.createVariables().putValue("score", 3));

    assertThat((String) decisionResult.getSingleEntry()).isEqualTo("a");

    verify(feelEngineSpy).evaluateSimpleExpression(anyString(), any(VariableContext.class));
  }

  @Test
  @DecisionResource(resource = DMN)
  void feelOutputEntryWithCustomEngine() {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) getDmnEngineConfiguration();
    configuration.setDefaultOutputEntryExpressionLanguage(DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE);
    DmnEngine engine = configuration.buildEngine();

    // stubbing the default FEEL engine behavior
    doReturn("a").when(feelEngineSpy).evaluateSimpleExpression(eq("\"a\""), any(VariableContext.class));

    DmnDecisionResult decisionResult = engine.evaluateDecision(decision,
      Variables.createVariables().putValue("score", 3));

    assertThat((String) decisionResult.getSingleEntry()).isEqualTo("a");

    verify(feelEngineSpy).evaluateSimpleExpression(anyString(), any(VariableContext.class));
  }

  public class TestFeelEngineFactory implements FeelEngineFactory {

    public TestFeelEngineFactory() {
      FeelEngineFactoryImpl feelEngineFactory = new FeelEngineFactoryImpl();
      feelEngineSpy = spy(feelEngineFactory.createInstance());
    }

    @Override
    public FeelEngine createInstance() {
      return feelEngineSpy;
    }

  }

}
