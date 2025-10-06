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
package org.operaton.bpm.dmn.feel.impl;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineFactoryImpl;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.context.VariableContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FeelExceptionTest {

  public static final String INPUT_VARIABLE = "input";

  public static FeelEngine feelEngine;

  public VariableMap variables;

  @BeforeAll
  static void initFeelEngine() {
    feelEngine = new FeelEngineFactoryImpl().createInstance();
  }

  @Test
  void simpleExpressionNotSupported() {
    VariableContext emptyVariableContext = Variables.emptyVariableContext();
    assertThatThrownBy(() -> feelEngine.evaluateSimpleExpression("12 == 12", emptyVariableContext))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageStartingWith("FEEL-01016");
  }

  @BeforeEach
  void initVariables() {
    variables = Variables.createVariables();
    variables.putValue(INPUT_VARIABLE, 13);
  }

  @Test
  void invalidNot() {
    assertException("FEEL-01001",
      "not(",
      "not(2",
      "not(2, 12",
      "not(]",
      "not(("
      );
  }

  @Test
  void invalidInterval() {
    assertException("FEEL-01002",
      "[1..3",
      "(1..3",
      "]1..3",
      "[1..3(",
      "(1..3(",
      "]1..3(",
      "[1..",
      "(1..",
      "]1..",
      "[1.3",
      "[1.3]",
      "[1.3)",
      "[1.3[",
      "(1.3",
      "(1.3]",
      "(1.3)",
      "(1.3[",
      "]1.3",
      "]1.3]",
      "]1.3)",
      "]1.3[",
      "(1.3",
      "(1.3]",
      "(1.3)",
      "(1.3[",
      "[1....3"
    );
  }

  @Test
  void invalidComparison() {
    assertException("FEEL-01003",
      ">",
      ">=",
      "<",
      "<="
    );
  }

  @Test
  void unknownMethod() {
    assertException("FEEL-01007",
      "unknown(12)",
      "not(unknown(12))",
      "12,13,unknown(12),14",
      "not(12,13,unknown(12),14)",
      "[12..unknown(12))",
      "not([12..unknown(12)))",
      "12,13,[12..unknown(12)),14",
      "not(12,13,[12..unknown(12)),14)"
    );
  }

  @Test
  void unknownVariable() {
    assertException("FEEL-01009",
      "a",
      "not(a)",
      "12,13,a,14",
      "not(12,13,a,14)",
      "[12..a)",
      "not([12..a))",
      "12,13,[12..a),14",
      "not(12,13,[12..a),14)"
    );
  }

  @Test
  void invalidSyntax() {
    assertException("FEEL-01010",
      "!= x",
      "== x",
      "=< 12",
      "=> 12",
      "< = 12",
      "> = 12",
      "1..3]",
      "1..3)",
      "1..3[",
      ")1..3",
      "1..3(",
      "[1....3]",
      "[1....3)",
      "[1....3[",
      "< [1..3)",
      ">= [1..3)",
      "${cellInput == 12}"
    );
  }

  @Test
  void unableToConvertToBoolean() {
    variables.putValue(INPUT_VARIABLE, true);
    assertException("FEEL-01015",
      "''",
      "'operaton'",
      "12",
      "'true'",
      "\"false\""
    );
  }

  @Test
  void unableToConvertToBigDecimal() {
    variables.putValue(INPUT_VARIABLE, BigDecimal.valueOf(10));
    assertException("FEEL-01015",
      "''",
      "< ''",
      "'operaton'",
      "< 'operaton'",
      "false",
      "< true",
      "'12'",
      "< '12'",
      "\"12\"",
      "< \"12\""
    );
  }

  @Test
  void unableToConvertToBigInteger() {
    variables.putValue(INPUT_VARIABLE, BigInteger.valueOf(10));
    assertException("FEEL-01015",
      "''",
      "< ''",
      "'operaton'",
      "< 'operaton'",
      "false",
      "< true",
      "'12'",
      "< '12'",
      "\"12\"",
      "< \"12\""
    );
  }

  @Test
  void unableToConvertToDouble() {
    variables.putValue(INPUT_VARIABLE, 10.0);
    assertException("FEEL-01015",
      "''",
      "< ''",
      "'operaton'",
      "< 'operaton'",
      "false",
      "< true",
      "'12.2'",
      "< '12.2'",
      "\"12.2\"",
      "< \"12.2\""
    );
  }

  @Test
  void unableToConvertToLong() {
    variables.putValue(INPUT_VARIABLE, 10L);
    assertException("FEEL-01015",
      "''",
      "< ''",
      "'operaton'",
      "< 'operaton'",
      "false",
      "< true",
      "'12'",
      "< '12'",
      "\"12\"",
      "< \"12\""
    );
  }

  @Test
  void unableToConvertToString() {
    variables.putValue(INPUT_VARIABLE, "operaton");
    assertException("FEEL-01015",
      "false",
      "< true",
      "12",
      "< 12"
    );
  }

  @Test
  void missingInputVariable() {
    variables.remove(INPUT_VARIABLE);
    assertException("FEEL-01017",
      "false",
      "12",
      "< 12",
      "'Hello'"
    );
  }

  @Test
  void invalidDateAndTimeFormat() {
    assertException("FEEL-01019",
      "date and time('operaton')",
      "date and time('2012-13-13')",
      "date and time('13:13:13')",
      "date and time('2012-12-12T25:00')"
    );
  }

  @Test
  void invalidListFormat() {
    assertException("FEEL-01020",
      ",",
      "1,",
      "1,2,,3",
      ",1,2",
      "1,2,   ,3,4",
      "1,\t,2"
      );
  }

  void assertException(String exceptionCode, String... feelExpressions) {
    for (String feelExpression : feelExpressions) {
      assertThatThrownBy(() -> evaluateFeel(feelExpression))
        .isInstanceOf(FeelException.class)
        .hasMessageStartingWith(exceptionCode)
        .hasMessageContaining(feelExpression)
        .has(new Condition<>(ex -> feelExpression.startsWith("${") || !ex.getMessage().contains("${"),
          "message must not contain ${"));
    }
  }

  void evaluateFeel(String feelExpression) {
    feelEngine.evaluateSimpleUnaryTests(feelExpression, INPUT_VARIABLE, variables.asVariableContext());
  }

}
