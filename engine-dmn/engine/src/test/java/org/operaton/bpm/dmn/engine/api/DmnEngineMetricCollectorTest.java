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

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.variable.Variables.createVariables;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.operaton.bpm.dmn.engine.spi.DmnEngineMetricCollector;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.engine.variable.VariableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DmnEngineMetricCollectorTest extends DmnEngineTest {

  public static final String EXAMPLE_DMN = "org/operaton/bpm/dmn/engine/api/Example.dmn";
  public static final String DISH_EXAMPLE_DMN = "org/operaton/bpm/dmn/engine/api/DrdDishDecisionExample.dmn";
  public static final String DRG_WITH_LITERAL_EXPRESSIONS = "org/operaton/bpm/dmn/engine/api/DrgWithLiteralExpressions.dmn";

  protected DmnEngineMetricCollector metricCollector;

    /**
   * Retrieves the engine metric collector from the DMN engine configuration.
   */
  @Before
  public void getEngineMetricCollector() {
    metricCollector = dmnEngine.getConfiguration().getEngineMetricCollector();
  }

    /**
   * Sets test variables with the status as "bronze" and sum as 100.
   */
  @Before
  public void setTestVariables() {
    variables.putValue("status", "bronze");
    variables.putValue("sum", 100);
  }

    /**
   * Clears the executed decision elements in the metric collector after each test.
   */
  @After
  public void clearEngineMetrics() {
    metricCollector.clearExecutedDecisionElements();
  }

    /**
   * Test to verify that the initial value of executed decision elements is 0.
   */
  @Test
  public void testInitialExecutedDecisionElementsValue() {
    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(0L);
  }

    /**
   * This method tests the execution of decision elements in a decision table by evaluating the decision table multiple times and verifying the number of executed decision elements.
   */
  @Test
  @DecisionResource(resource = EXAMPLE_DMN)
  public void testExecutedDecisionElementsOfDecisionTable() {
    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(0L);

    dmnEngine.evaluateDecisionTable(decision, variables);
    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(16L);

    dmnEngine.evaluateDecisionTable(decision, variables);
    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(32L);

    dmnEngine.evaluateDecisionTable(decision, variables);
    dmnEngine.evaluateDecisionTable(decision, variables);
    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(64L);
  }

    /**
   * Test method to verify the execution of decision elements in a DRG for a specific dish example DMN.
   */
  @Test
  @DecisionResource(resource = DISH_EXAMPLE_DMN, decisionKey = "Dish")
  public void testExecutedDecisionElementsOfDrg() {
    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(0L);

    VariableMap variableMap = createVariables()
      .putValue("temperature", 20)
      .putValue("dayType", "Weekend");

    dmnEngine.evaluateDecisionTable(decision, variableMap);
    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(30L);

    dmnEngine.evaluateDecisionTable(decision, variableMap);
    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(60L);

    dmnEngine.evaluateDecisionTable(decision, variableMap);
    dmnEngine.evaluateDecisionTable(decision, variableMap);
    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(120L);
  }

    /**
   * Test method to evaluate decision elements of decision literal expressions.
   */
  @Test
  public void testExecutedDecisionElementsOfDecisionLiteralExpression() {
    // evaluate one decision with a single literal expression
    dmnEngine.evaluateDecision(parseDecisionFromFile("c", DRG_WITH_LITERAL_EXPRESSIONS), createVariables());
    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(1);

    metricCollector.clearExecutedDecisionElements();

    // evaluate three decisions with single literal expressions
    dmnEngine.evaluateDecision(parseDecisionFromFile("a", DRG_WITH_LITERAL_EXPRESSIONS), createVariables());
    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(3);
  }

    /**
   * Test method to verify the functionality of clearing the executed decision elements value in the metric collector.
   */
  @Test
  @DecisionResource(resource = EXAMPLE_DMN)
  public void testClearExecutedDecisionElementsValue() {
    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(0L);

    dmnEngine.evaluateDecisionTable(decision, variables);
    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(16L);
    assertThat(metricCollector.clearExecutedDecisionElements()).isEqualTo(16L);

    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(0L);
  }

    /**
   * This method tests the DRD Dish decision example by evaluating a decision table with certain input values
   * and asserts the expected results using metric collector.
   */
  @Test
  @DecisionResource(resource = DISH_EXAMPLE_DMN, decisionKey = "Dish")
  public void testDrdDishDecisionExample() {
    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(0L);

    dmnEngine.evaluateDecisionTable(decision, createVariables()
      .putValue("temperature", 20)
      .putValue("dayType", "Weekend"));

    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(30L);
    assertThat(metricCollector.clearExecutedDecisionElements()).isEqualTo(30L);

    assertThat(metricCollector.getExecutedDecisionElements()).isEqualTo(0L);
  }

    /**
   * Test method for custom engine metric collector.
   * Creates a default DmnEngineConfiguration, sets a mock metric collector,
   * builds an engine, evaluates decision tables and literal expressions,
   * and verifies that the metric collector is notified.
   */
  @Test
  public void testCustomEngineMetricCollector() {
    DmnEngineConfiguration configuration = DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
    DmnEngineMetricCollector mockMetricCollector = mock(DmnEngineMetricCollector.class);
    configuration.setEngineMetricCollector(mockMetricCollector);
    DmnEngine engine = configuration.buildEngine();

    // evaluate one decision table
    engine.evaluateDecisionTable(parseDecisionFromFile("decision", EXAMPLE_DMN), variables);
    verify(mockMetricCollector, times(1)).notify(any(DmnDecisionTableEvaluationEvent.class));

    // evaluate one decision literal expression
    engine.evaluateDecision(parseDecisionFromFile("c", DRG_WITH_LITERAL_EXPRESSIONS), variables);
    verify(mockMetricCollector, times(1)).notify(any(DmnDecisionTableEvaluationEvent.class));
  }

}
