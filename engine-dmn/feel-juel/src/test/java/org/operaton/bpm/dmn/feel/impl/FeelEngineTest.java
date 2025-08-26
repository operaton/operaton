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

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineFactoryImpl;
import org.operaton.bpm.dmn.feel.impl.juel.el.FeelFunctionMapper;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.DateValue;

import static org.assertj.core.api.Assertions.assertThat;

public class FeelEngineTest {

  public static final String INPUT_VARIABLE = "input";

  public static FeelEngine feelEngine;

  public VariableMap variables;

  @BeforeAll
  static void initFeelEngine() {
    feelEngine = new FeelEngineFactoryImpl().createInstance();
  }

  @BeforeEach
  void initVariables() {
    variables = Variables.createVariables();
  }

  @Test
  void testLong() {
    variables.putValue("integer", 12);
    variables.putValue("primitive", 12L);
    variables.putValue("typed", Variables.longValue(12L));

    assertEvaluatesToTrue(Variables.longValue(12L), "<= typed");
    assertEvaluatesToTrue(Variables.longValue(12L), "<= primitive");
    assertEvaluatesToTrue(Variables.longValue(12L), "<= integer");
  }

  @Test
  void endpointString() {
    assertEvaluatesToTrue("Hello World", "\"Hello World\"");
    assertEvaluatesToFalse("Hello World", "\"Hello Operaton\"");
    assertEvaluatesToFalse("Hello World", "\"\"");
    assertEvaluatesToTrue("", "\"\"");
    assertEvaluatesToTrue("123", "\"123\"");
    assertEvaluatesToTrue("Why.not?", "\"Why.not?\"");
  }

  @Test
  void endpointVariable() {
    variables.put("y", "a");
    assertEvaluatesToTrue("a", "y");
    assertEvaluatesToFalse("b", "y");

    variables.put("customer", Collections.singletonMap("name", "operaton"));
    assertEvaluatesToTrue("operaton", "customer.name");
    assertEvaluatesToFalse("hello", "customer.name");
  }

  @Test
  void endpointVariableGreater() {
    variables.put("y", 13.37);
    assertEvaluatesToTrue(12, "<y");
    assertEvaluatesToFalse(13.38, "<y");
  }

  @Test
  void endpointVariableGreaterEqual() {
    variables.put("y", 13.37);
    assertEvaluatesToTrue(12, "<=y");
    assertEvaluatesToTrue(13.37, "<=y");
    assertEvaluatesToFalse(13.38, "<=y");
  }

  @Test
  void endpointVariableLess() {
    variables.put("y", 13.37);
    assertEvaluatesToFalse(12, ">y");
    assertEvaluatesToTrue(13.38, ">y");
  }

  @Test
  void endpointVariableLessEqual() {
    variables.put("y", 13.37);
    assertEvaluatesToFalse(12, ">=y");
    assertEvaluatesToTrue(13.37, ">=y");
    assertEvaluatesToTrue(13.38, ">=y");
  }

  @Test
  void endpointBoolean() {
    assertEvaluatesToTrue(true, "true");
    assertEvaluatesToFalse(true, "false");
    assertEvaluatesToTrue(false, "false");
    assertEvaluatesToFalse(false, "true");
  }

  @Test
  void endpointNumber() {
    assertEvaluatesToTrue(13, "13");
    assertEvaluatesToTrue(13.37, "13.37");
    assertEvaluatesToTrue(0.37, ".37");
    assertEvaluatesToFalse(13.37, "23.42");
    assertEvaluatesToFalse(0.42, ".37");
  }

  @Test
  void endpointNumberGreater() {
    assertEvaluatesToTrue(12, "<13");
    assertEvaluatesToTrue(13.35, "<13.37");
    assertEvaluatesToTrue(0.337, "<.37");
    assertEvaluatesToFalse(13.37, "<13.37");
    assertEvaluatesToFalse(0.37, "<.37");
  }

  @Test
  void endpointNumberGreaterEqual() {
    assertEvaluatesToTrue(13.37, "<=13.37");
    assertEvaluatesToTrue(13.337, "<=13.37");
    assertEvaluatesToTrue(0.37, "<=.37");
    assertEvaluatesToTrue(0.337, "<=.37");
    assertEvaluatesToFalse(13.42, "<=13.37");
    assertEvaluatesToFalse(0.42, "<=.37");
  }

  @Test
  void endpointNumberLess() {
    assertEvaluatesToTrue(13.37, ">13");
    assertEvaluatesToTrue(13.42, ">13.37");
    assertEvaluatesToTrue(0.42, ">.37");
    assertEvaluatesToFalse(13.37, ">13.37");
    assertEvaluatesToFalse(0.37, ">.37");
  }

  @ParameterizedTest
  @CsvSource({
      "13.37, >=13, true",
      "13.37, >=13.37, true",
      "0.37, >=.37, true",
      "0.42, >=.37, true",
      "13.337, >=13.37, false",
      "0.23, >=.37, false"
  })
  void endpointNumberLessEqual(double input, String feelExpression, boolean expectTrueResult) {
      if (expectTrueResult) {
          assertEvaluatesToTrue(input, feelExpression);
      } else {
          assertEvaluatesToFalse(input, feelExpression);
      }
  }

  @Test
  void endpointDateAndTime() {
    DateValue dateTime = parseDateAndTime("2015-12-12T22:12:53");

    assertEvaluatesToTrue(dateTime, "date and time(\"2015-12-12T22:12:53\")");

    variables.put("y", "2015-12-12T22:12:53");
    assertEvaluatesToTrue(dateTime, "date and time(y)");
  }

  @Test
  void threadSafetyDateAndTimeParsing() throws Exception {
    int threadCount = 2;
    ExecutorService pool = Executors.newFixedThreadPool(threadCount);

    Set<Future<Date>> futureSet = new HashSet<>();
    Set<Date> expectedDates = new HashSet<>();

    for(int i = 1; i <= 3 * threadCount; i++) {
      final String dateAndTimeString = "2015-12-12T22:12:5" + i;

      expectedDates.add(
        FeelFunctionMapper.parseDateAndTime(dateAndTimeString)
      );

      futureSet.add(pool.submit(() -> FeelFunctionMapper.parseDateAndTime(dateAndTimeString)));
    }

    pool.shutdown();

    Set<Date> actualDates = new HashSet<>();
    for( Future<Date> dateFuture : futureSet ) {
      actualDates.add(dateFuture.get());
    }


    assertThat(actualDates).hasSameElementsAs(expectedDates);
  }

  @ParameterizedTest
  @CsvSource({
      "0.23, [.12...37], true",
      "0.23, [.12...37), true",
      "0.23, [.12...37[, true",
      "0.23, (.12...37], true",
      "0.23, (.12...37), true",
      "0.23, (.12...37[, true",
      "0.23, ].12...37], true",
      "0.23, ].12...37), true",
      "0.23, ].12...37[, true",
      "13.37, [.12...37], false",
      "13.37, [.12...37), false",
      "13.37, [.12...37[, false",
      "13.37, (.12...37], false",
      "13.37, (.12...37), false",
      "13.37, (.12...37[, false",
      "13.37, ].12...37], false",
      "13.37, ].12...37), false",
      "13.37, ].12...37[, false"
  })
  void intervalNumber(double input, String feelExpression, boolean expectedResult) {
      if (expectedResult) {
          assertEvaluatesToTrue(input, feelExpression);
      } else {
          assertEvaluatesToFalse(input, feelExpression);
      }
  }

  @ParameterizedTest
  @CsvSource({
      "13.37, [a..b], true",
      "13.37, [a..b), true",
      "13.37, [a..b[, true",
      "13.37, (a..b], true",
      "13.37, (a..b), true",
      "13.37, (a..b[, true",
      "13.37, ]a..b], true",
      "13.37, ]a..b), true",
      "13.37, ]a..b[, true",
      "0.37, [a..b], false",
      "0.37, [a..b), false",
      "0.37, [a..b[, false",
      "0.37, (a..b], false",
      "0.37, (a..b), false",
      "0.37, (a..b[, false",
      "0.37, ]a..b], false",
      "0.37, ]a..b), false",
      "0.37, ]a..b[, false"
  })
  void intervalVariable(double input, String feelExpression, boolean expectTrue) {
      variables.put("a", 10);
      variables.put("b", 15);
      if (expectTrue) {
          assertEvaluatesToTrue(input, feelExpression);
      } else {
          assertEvaluatesToFalse(input, feelExpression);
      }
  }

  @ParameterizedTest
  @CsvSource({
      "2016-03-03T00:00:00, true, [date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]",
      "2016-03-03T00:00:00, true, [date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\"))",
      "2016-03-03T00:00:00, true, [date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")[",
      "2016-03-03T00:00:00, true, (date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]",
      "2016-03-03T00:00:00, true, (date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\"))",
      "2016-03-03T00:00:00, true, (date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")[",
      "2016-03-03T00:00:00, true, ]date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]",
      "2016-03-03T00:00:00, true, ]date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\"))",
      "2016-03-03T00:00:00, true, ]date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")[",
      "2016-03-03T00:00:00, true, [date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-03-03T00:00:00\")]",
      "2016-03-03T00:00:00, true, [date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-03-03T00:00:01\")[",
      "2016-03-03T00:00:00, true, [date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-03-03T00:00:01\"))",
      "2016-03-03T00:00:00, true, [date and time(\"2016-03-03T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]",
      "2016-03-03T00:00:00, true, ]date and time(\"2016-03-02T23:59:59\")..date and time(\"2016-06-06T00:00:00\")]",
      "2016-03-03T00:00:00, true, (date and time(\"2016-03-02T23:59:59\")..date and time(\"2016-06-06T00:00:00\")]",
      "2013-03-03T00:00:00, false, [date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]",
      "2013-03-03T00:00:00, false, [date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\"))",
      "2013-03-03T00:00:00, false, [date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")[",
      "2013-03-03T00:00:00, false, (date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]",
      "2013-03-03T00:00:00, false, (date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\"))",
      "2013-03-03T00:00:00, false, (date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")[",
      "2013-03-03T00:00:00, false, ]date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]",
      "2013-03-03T00:00:00, false, ]date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\"))",
      "2013-03-03T00:00:00, false, ]date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")[",
      "2013-03-03T00:00:00, false, [date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-03-02T23:59:59\")]",
      "2013-03-03T00:00:00, false, [date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-03-03T00:00:00\")[",
      "2013-03-03T00:00:00, false, [date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-03-03T00:00:00\"))",
      "2013-03-03T00:00:00, false, [date and time(\"2016-03-03T00:00:01\")..date and time(\"2016-06-06T00:00:00\")]",
      "2013-03-03T00:00:00, false, ]date and time(\"2016-03-03T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]",
      "2013-03-03T00:00:00, false, (date and time(\"2016-03-03T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]"
  })
  void intervalDateAndTime(String dateTimeString, boolean expectedResult, String feelExpression) {
      DateValue dateTime = parseDateAndTime(dateTimeString);
      if (expectedResult) {
          assertEvaluatesToTrue(dateTime, feelExpression);
      } else {
          assertEvaluatesToFalse(dateTime, feelExpression);
      }
  }

  @Test
  void not() {
    variables.put("y", 13.37);

    assertEvaluatesToTrue("Hello operaton", "not(\"Hello World\")");
    assertEvaluatesToTrue(0.37, "not(y)");
    assertEvaluatesToFalse(0.37, "not(<y)");
    assertEvaluatesToFalse(0.37, "not(<=y)");
    assertEvaluatesToTrue(0.37, "not(>y)");
    assertEvaluatesToTrue(0.37, "not(>=y)");
    assertEvaluatesToTrue(0.37, "not(13.37)");
    assertEvaluatesToFalse(0.37, "not(<13.37)");
    assertEvaluatesToFalse(0.37, "not(<=13.37)");
    assertEvaluatesToTrue(0.37, "not(>13.37)");
    assertEvaluatesToTrue(0.37, "not(>=13.37)");
    assertEvaluatesToFalse(0.37, "not(.37)");
    assertEvaluatesToTrue(0.37, "not(<.37)");
    assertEvaluatesToFalse(0.37, "not(<=.37)");
    assertEvaluatesToTrue(0.37, "not(>.37)");
    assertEvaluatesToFalse(0.37, "not(>=.37)");
  }

  @Test
  void list() {
    variables.put("a", "Hello operaton");
    variables.put("y", 0);

    assertEvaluatesToTrue("Hello World", "a,\"Hello World\"");
    assertEvaluatesToTrue("Hello operaton", "a,\"Hello World\"");
    assertEvaluatesToFalse("Hello unknown", "a,\"Hello World\"");
    assertEvaluatesToTrue(0, "y,12,13.37,.37");
    assertEvaluatesToTrue(12, "y,12,13.37,.37");
    assertEvaluatesToTrue(13.37, "y,12,13.37,.37");
    assertEvaluatesToTrue(0.37, "y,12,13.37,.37");
    assertEvaluatesToFalse(0.23, "y,12,13.37,.37");
    assertEvaluatesToTrue(-1, "<y,>13.37,>=.37");
    assertEvaluatesToTrue(0.37, "<y,>13.37,>=.37");
    assertEvaluatesToFalse(0, "<y,>13.37,>=.37");
  }

  @Test
  void nested() {
    variables.put("a", 23.42);
    assertEvaluatesToTrue(0.37, "not(>=a,13.37,].37...42),<.37)");
    assertEvaluatesToFalse(23.42, "not(>=a,13.37,].37...42),<.37)");
    assertEvaluatesToFalse(13.37, "not(>=a,13.37,].37...42),<.37)");
    assertEvaluatesToFalse(0.38, "not(>=a,13.37,].37...42),<.37)");
    assertEvaluatesToFalse(0, "not(>=a,13.37,].37...42),<.37)");
  }

  @Test
  void dontCare() {
    assertEvaluatesToTrue(13.37, "-");
  }

  @Test
  void whitespace() {
    assertEvaluatesToTrue("Hello World", "'Hello World' ");
    assertEvaluatesToTrue("Hello World", " 'Hello World'");
    assertEvaluatesToTrue("Hello World", " 'Hello World' ");
    assertEvaluatesToTrue(12, " 12 ");
    assertEvaluatesToTrue(10.2, " <12 ");
    assertEvaluatesToTrue(0, "< 12 ");
    assertEvaluatesToTrue(12.3, "\t>=12 ");
    assertEvaluatesToTrue(0, " not( 13 ,\t>0)\t");
  }

  @Test
  void pojo() {
    variables.putValue("pojo", new TestPojo("foo", 13.37));
    assertEvaluatesToTrue("foo", "pojo.foo");
    assertEvaluatesToFalse("operaton", "pojo.foo");
    assertEvaluatesToTrue(12, "<= pojo.bar");
    assertEvaluatesToFalse(13.33, ">= pojo.bar");
  }

  public void assertEvaluatesToTrue(Object input, String feelExpression) {
    boolean result = evaluateFeel(input, feelExpression);
    assertThat(result).isTrue();
  }

  public void assertEvaluatesToFalse(Object input, String feelExpression) {
    boolean result = evaluateFeel(input, feelExpression);
    assertThat(result).isFalse();
  }

  public boolean evaluateFeel(Object input, String feelExpression) {
    variables.putValue(INPUT_VARIABLE, input);
    return feelEngine.evaluateSimpleUnaryTests(feelExpression, INPUT_VARIABLE, variables.asVariableContext());
  }

  protected DateValue parseDateAndTime(String dateAndTimeString) {
    Date date = FeelFunctionMapper.parseDateAndTime(dateAndTimeString);
    return Variables.dateValue(date);
  }

  public static class TestPojo {

    protected String foo;
    protected Double bar;

    public TestPojo(String foo, Double bar) {
      this.foo = foo;
      this.bar = bar;
    }

    public String getFoo() {
      return foo;
    }

    public Double getBar() {
      return bar;
    }

    @Override
    public String toString() {
      return "TestPojo{" +
        "foo='" + foo + '\'' +
        ", bar=" + bar +
        '}';
    }

  }

}
