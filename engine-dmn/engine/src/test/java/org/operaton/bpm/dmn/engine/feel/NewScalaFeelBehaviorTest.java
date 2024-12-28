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
package org.operaton.bpm.dmn.engine.feel;

import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.dmn.feel.impl.scala.ScalaFeelEngineFactory;
import org.operaton.bpm.engine.variable.Variables;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NewScalaFeelBehaviorTest extends DmnEngineTest {

  @Override
  protected DmnEngineConfiguration getDmnEngineConfiguration() {
    DefaultDmnEngineConfiguration configuration = new DefaultDmnEngineConfiguration();
    configuration.setFeelEngineFactory(new ScalaFeelEngineFactory());
    return configuration;
  }

  @Test
  @DecisionResource(resource = "scala_input_expression.dmn")
  void shouldEvaluateInputExpression_Simple() {
    // given
    getVariables()
      .putValue("date1", new Date())
      .putValue("date2", new Date());

    // when
    String result = evaluateDecision().getSingleEntry();

    // then
    assertThat(result).isEqualTo("bar");
  }

  @Test
  @DecisionResource(resource = "scala_input_expression_builtin_function.dmn")
  void shouldEvaluateInputExpression_BuiltInFunction() {
    // given
    getVariables()
      .putValue("date1", new Date());

    // when
    String result = evaluateDecision().getSingleEntry();

    // then
    assertThat(result).isEqualTo("foo");
  }

  @Test
  @DecisionResource(resource = "scala_compare_date_with_time_zone_non_typed.dmn")
  void shouldEvaluateTimezoneComparisonWithZonedDateTime() {
    variables.putValue("date1", ZonedDateTime.now());

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntryTyped(Variables.stringValue("bar"));
  }

  @Test
  @DecisionResource(resource = "scala_unary_builtin_function.dmn")
  void shouldEvaluateBuiltInFunctionInUnaryTest() {
    variables.putValue("integerString", "45");

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntryTyped(Variables.integerValue(45));
  }

  @Test
  @DecisionResource(resource = "scala_compare_date_untyped.dmn")
  void shouldEvaluateLocalDate_NonInputClauseType() {
    // given
    getVariables()
      .putValue("date1", LocalDateTime.now());

    // when
    String result = evaluateDecision().getSingleEntry();

    // then
    assertThat(result).isEqualTo("not ok");
  }

  @Test
  @DecisionResource(resource = "scala_literal_expression_date_typed.dmn")
  void shouldEvaluateToUtilDateWithLiteralExpression() {
    // given
    getVariables()
      .putValue("date1", new Date());

    // when
    Object result = evaluateDecision().getSingleEntry();

    // then
    assertThat(result).isInstanceOf(Date.class);
  }

  @Test
  @DecisionResource(resource = "scala_date_typed_output.dmn")
  void shouldEvaluateToUtilDateForTypedOutputClause() {
    // given

    // when
    Date result = evaluateDecision().getSingleEntry();

    // then
    assertThat(result).isEqualTo("2019-08-08T22:22:22");
  }

  // https://jira.camunda.com/browse/CAM-11382
  @Test
  @DecisionResource(resource = "scala_output_expression_double.dmn")
  void shouldReturnMaxDouble() {
    // given
    getVariables().putValue("myVariable", Double.MAX_VALUE);

    // when
    double result = evaluateDecision().getSingleEntry();

    // then
    assertThat(result).isEqualTo(Double.MAX_VALUE);
  }

}
