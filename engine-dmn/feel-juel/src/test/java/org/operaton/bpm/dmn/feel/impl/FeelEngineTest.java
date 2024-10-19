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
package org.operaton.bpm.dmn.feel.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineFactoryImpl;
import org.operaton.bpm.dmn.feel.impl.juel.el.FeelFunctionMapper;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.DateValue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FeelEngineTest {

  public static final String INPUT_VARIABLE = "input";

  public static FeelEngine feelEngine;

  public VariableMap variables;

    /**
   * Initializes the FeelEngine for testing purposes.
   */
  @BeforeClass
  public static void initFeelEngine() {
    feelEngine = new FeelEngineFactoryImpl().createInstance();
  }

    /**
   * Initializes the variables by creating a new Variables object.
   */
  @Before
  public void initVariables() {
    variables = Variables.createVariables();
  }

    /**
   * Test method to verify the behavior of comparing long values stored in different types of variables.
   */
  @Test
  public void testLong() {
    variables.putValue("integer", 12);
    variables.putValue("primitive", 12L);
    variables.putValue("typed", Variables.longValue(12L));

    assertEvaluatesToTrue(Variables.longValue(12L), "<= typed");
    assertEvaluatesToTrue(Variables.longValue(12L), "<= primitive");
    assertEvaluatesToTrue(Variables.longValue(12L), "<= integer");
  }

    /**
   * This method tests the evaluation of two strings to determine if they are equal or not.
   */
  @Test
  public void testEndpointString() {
    assertEvaluatesToTrue("Hello World", "\"Hello World\"");
    assertEvaluatesToFalse("Hello World", "\"Hello Operaton\"");
    assertEvaluatesToFalse("Hello World", "\"\"");
    assertEvaluatesToTrue("", "\"\"");
    assertEvaluatesToTrue("123", "\"123\"");
    assertEvaluatesToTrue("Why.not?", "\"Why.not?\"");
  }

    /**
   * Test method for evaluating endpoint variables.
   * This method tests the evaluation of variables put into a map, comparing expected values with actual values.
   */
  @Test
  public void testEndpointVariable() {
    variables.put("y", "a");
    assertEvaluatesToTrue("a", "y");
    assertEvaluatesToFalse("b", "y");

    variables.put("customer", Collections.singletonMap("name", "operaton"));
    assertEvaluatesToTrue("operaton", "customer.name");
    assertEvaluatesToFalse("hello", "customer.name");
  }

    /**
   * Test method to verify if a given value is lesser than a variable stored in a map.
   */
  @Test
  public void testEndpointVariableGreater() {
    variables.put("y", 13.37);
    assertEvaluatesToTrue(12, "<y");
    assertEvaluatesToFalse(13.38, "<y");
  }

    /**
   * Test method to verify the functionality of evaluating if a variable is greater than or equal to a given value.
   * Variables map is updated with a value for 'y', then the method asserts if the given values are greater than or equal to 'y'.
   */
  @Test
  public void testEndpointVariableGreaterEqual() {
    variables.put("y", 13.37);
    assertEvaluatesToTrue(12, "<=y");
    assertEvaluatesToTrue(13.37, "<=y");
    assertEvaluatesToFalse(13.38, "<=y");
  }

    /**
   * Test method for evaluating an endpoint variable.
   */
  @Test
  public void testEndpointVariableLess() {
    variables.put("y", 13.37);
    assertEvaluatesToFalse(12, ">y");
    assertEvaluatesToTrue(13.38, ">y");
  }

    /**
   * Test method to evaluate if a variable is less than or equal to a specified value.
   */
  @Test
  public void testEndpointVariableLessEqual() {
    variables.put("y", 13.37);
    assertEvaluatesToFalse(12, ">=y");
    assertEvaluatesToTrue(13.37, ">=y");
    assertEvaluatesToTrue(13.38, ">=y");
  }

    /**
   * This method tests the evaluation of boolean values using the assertEvaluatesToTrue and assertEvaluatesToFalse methods.
   */
  @Test
  public void testEndpointBoolean() {
    assertEvaluatesToTrue(true, "true");
    assertEvaluatesToFalse(true, "false");
    assertEvaluatesToTrue(false, "false");
    assertEvaluatesToFalse(false, "true");
  }

    /**
   * This method tests various endpoint numbers to see if they evaluate to true or false.
   */
  @Test
  public void testEndpointNumber() {
    assertEvaluatesToTrue(13, "13");
    assertEvaluatesToTrue(13.37, "13.37");
    assertEvaluatesToTrue(0.37, ".37");
    assertEvaluatesToFalse(13.37, "23.42");
    assertEvaluatesToFalse(0.42, ".37");
  }

    /**
   * Test method to check if numbers are greater than a specified value.
   */
  @Test
  public void testEndpointNumberGreater() {
    assertEvaluatesToTrue(12, "<13");
    assertEvaluatesToTrue(13.35, "<13.37");
    assertEvaluatesToTrue(0.337, "<.37");
    assertEvaluatesToFalse(13.37, "<13.37");
    assertEvaluatesToFalse(0.37, "<.37");
  }

    /**
   * This method tests if the endpoint number is greater than or equal to the specified value.
   */
  @Test
  public void testEndpointNumberGreaterEqual() {
    assertEvaluatesToTrue(13.37, "<=13.37");
    assertEvaluatesToTrue(13.337, "<=13.37");
    assertEvaluatesToTrue(0.37, "<=.37");
    assertEvaluatesToTrue(0.337, "<=.37");
    assertEvaluatesToFalse(13.42, "<=13.37");
    assertEvaluatesToFalse(0.42, "<=.37");
  }

    /**
   * Tests that the endpoint number is less than the specified value.
   */
  @Test
  public void testEndpointNumberLess() {
    assertEvaluatesToTrue(13.37, ">13");
    assertEvaluatesToTrue(13.42, ">13.37");
    assertEvaluatesToTrue(0.42, ">.37");
    assertEvaluatesToFalse(13.37, ">13.37");
    assertEvaluatesToFalse(0.37, ">.37");
  }

    /**
   * Test method to evaluate if a number is less than or equal to the specified endpoint.
   * 
   * The method performs several tests using the assertEvaluatesToTrue and assertEvaluatesToFalse methods.
   */
  @Test
  public void testEndpointNumberLessEqual() {
    assertEvaluatesToTrue(13.37, ">=13");
    assertEvaluatesToTrue(13.37, ">=13.37");
    assertEvaluatesToTrue(0.37, ">=.37");
    assertEvaluatesToTrue(0.42, ">=.37");
    assertEvaluatesToFalse(13.337, ">=13.37");
    assertEvaluatesToFalse(0.23, ">=.37");
  }

    /**
   * Test method for evaluating date and time values using a specific endpoint.
   */
  @Test
  public void testEndpointDateAndTime() {
    DateValue dateTime = parseDateAndTime("2015-12-12T22:12:53");

    assertEvaluatesToTrue(dateTime, "date and time(\"2015-12-12T22:12:53\")");

    variables.put("y", "2015-12-12T22:12:53");
    assertEvaluatesToTrue(dateTime, "date and time(y)");
  }

    /**
   * This method tests the thread safety of parsing date and time strings using a ThreadPoolExecutor with multiple threads.
   * It creates a pool of threads, submits tasks to parse date and time strings, and compares the actual parsed dates with the expected dates.
   * @throws ExecutionException if an error occurs during the execution of the task
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  @Test
  public void testThreadSafetyDateAndTimeParsing() throws ExecutionException, InterruptedException {
    int threadCount = 2;
    ExecutorService pool = Executors.newFixedThreadPool(threadCount);

    Set<Future<Date>> futureSet = new HashSet<Future<Date>>();
    Set<Date> expectedDates = new HashSet<Date>();

    for(int i = 1; i <= 3 * threadCount; i++) {
      final String dateAndTimeString = "2015-12-12T22:12:5" + i;

      expectedDates.add(
        FeelFunctionMapper.parseDateAndTime(dateAndTimeString)
      );

      futureSet.add(pool.submit(new Callable<Date>() {
        public Date call() throws Exception {
          return FeelFunctionMapper.parseDateAndTime(dateAndTimeString);
        }
      }));
    }

    pool.shutdown();

    Set<Date> actualDates = new HashSet<Date>();
    for( Future<Date> dateFuture : futureSet ) {
      actualDates.add(dateFuture.get());
    }


    assertThat(actualDates).hasSameElementsAs(expectedDates);
  }

    /**
   * This method tests the interval number evaluation by asserting if a given number falls within the specified intervals.
   */
  @Test
  public void testIntervalNumber() {
    assertEvaluatesToTrue(0.23, "[.12...37]");
    assertEvaluatesToTrue(0.23, "[.12...37)");
    assertEvaluatesToTrue(0.23, "[.12...37[");

    assertEvaluatesToTrue(0.23, "(.12...37]");
    assertEvaluatesToTrue(0.23, "(.12...37)");
    assertEvaluatesToTrue(0.23, "(.12...37[");

    assertEvaluatesToTrue(0.23, "].12...37]");
    assertEvaluatesToTrue(0.23, "].12...37)");
    assertEvaluatesToTrue(0.23, "].12...37[");

    assertEvaluatesToFalse(13.37, "[.12...37]");
    assertEvaluatesToFalse(13.37, "[.12...37)");
    assertEvaluatesToFalse(13.37, "[.12...37[");

    assertEvaluatesToFalse(13.37, "(.12...37]");
    assertEvaluatesToFalse(13.37, "(.12...37)");
    assertEvaluatesToFalse(13.37, "(.12...37[");

    assertEvaluatesToFalse(13.37, "].12...37]");
    assertEvaluatesToFalse(13.37, "].12...37)");
    assertEvaluatesToFalse(13.37, "].12...37[");
  }

    /**
   * This method tests various interval variable scenarios by evaluating different interval expressions.
   */
  @Test
  public void testIntervalVariable() {
    variables.put("a", 10);
    variables.put("b", 15);

    assertEvaluatesToTrue(13.37, "[a..b]");
    assertEvaluatesToTrue(13.37, "[a..b)");
    assertEvaluatesToTrue(13.37, "[a..b[");

    assertEvaluatesToTrue(13.37, "(a..b]");
    assertEvaluatesToTrue(13.37, "(a..b)");
    assertEvaluatesToTrue(13.37, "(a..b[");

    assertEvaluatesToTrue(13.37, "]a..b]");
    assertEvaluatesToTrue(13.37, "]a..b)");
    assertEvaluatesToTrue(13.37, "]a..b[");

    assertEvaluatesToFalse(0.37, "[a..b]");
    assertEvaluatesToFalse(0.37, "[a..b)");
    assertEvaluatesToFalse(0.37, "[a..b[");

    assertEvaluatesToFalse(0.37, "(a..b]");
    assertEvaluatesToFalse(0.37, "(a..b)");
    assertEvaluatesToFalse(0.37, "(a..b[");

    assertEvaluatesToFalse(0.37, "]a..b]");
    assertEvaluatesToFalse(0.37, "]a..b)");
    assertEvaluatesToFalse(0.37, "]a..b[");
  }

    /**
   * This method tests the interval of a given date and time against various ranges
   */
  @Test
  public void testIntervalDateAndTime() {
    DateValue dateAndTime = parseDateAndTime("2016-03-03T00:00:00");
    assertEvaluatesToTrue(dateAndTime, "[date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]");
    assertEvaluatesToTrue(dateAndTime, "[date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\"))");
    assertEvaluatesToTrue(dateAndTime, "[date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")[");

    assertEvaluatesToTrue(dateAndTime, "(date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]");
    assertEvaluatesToTrue(dateAndTime, "(date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\"))");
    assertEvaluatesToTrue(dateAndTime, "(date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")[");

    assertEvaluatesToTrue(dateAndTime, "]date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]");
    assertEvaluatesToTrue(dateAndTime, "]date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\"))");
    assertEvaluatesToTrue(dateAndTime, "]date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")[");

    assertEvaluatesToTrue(dateAndTime, "[date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-03-03T00:00:00\")]");
    assertEvaluatesToTrue(dateAndTime, "[date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-03-03T00:00:01\")[");
    assertEvaluatesToTrue(dateAndTime, "[date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-03-03T00:00:01\"))");

    assertEvaluatesToTrue(dateAndTime, "[date and time(\"2016-03-03T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]");
    assertEvaluatesToTrue(dateAndTime, "]date and time(\"2016-03-02T23:59:59\")..date and time(\"2016-06-06T00:00:00\")]");
    assertEvaluatesToTrue(dateAndTime, "(date and time(\"2016-03-02T23:59:59\")..date and time(\"2016-06-06T00:00:00\")]");


    dateAndTime = parseDateAndTime("2013-03-03T00:00:00");
    assertEvaluatesToFalse(dateAndTime, "[date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]");
    assertEvaluatesToFalse(dateAndTime, "[date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\"))");
    assertEvaluatesToFalse(dateAndTime, "[date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")[");

    assertEvaluatesToFalse(dateAndTime, "(date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]");
    assertEvaluatesToFalse(dateAndTime, "(date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\"))");
    assertEvaluatesToFalse(dateAndTime, "(date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")[");

    assertEvaluatesToFalse(dateAndTime, "]date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]");
    assertEvaluatesToFalse(dateAndTime, "]date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\"))");
    assertEvaluatesToFalse(dateAndTime, "]date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-06-06T00:00:00\")[");

    assertEvaluatesToFalse(dateAndTime, "[date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-03-02T23:59:59\")]");
    assertEvaluatesToFalse(dateAndTime, "[date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-03-03T00:00:00\")[");
    assertEvaluatesToFalse(dateAndTime, "[date and time(\"2015-12-12T00:00:00\")..date and time(\"2016-03-03T00:00:00\"))");

    assertEvaluatesToFalse(dateAndTime, "[date and time(\"2016-03-03T00:00:01\")..date and time(\"2016-06-06T00:00:00\")]");
    assertEvaluatesToFalse(dateAndTime, "]date and time(\"2016-03-03T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]");
    assertEvaluatesToFalse(dateAndTime, "(date and time(\"2016-03-03T00:00:00\")..date and time(\"2016-06-06T00:00:00\")]");
  }

    /**
   * This method tests the behavior of the "not" operation in the context of evaluating expressions.
   */
  @Test
  public void testNot() {
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

    /**
   * This method is used to test the functionality of the list by adding variables and asserting the evaluations of different expressions.
   */
  @Test
  public void testList() {
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

    /**
   * Test method for evaluating nested expressions with variables.
   */
  @Test
  public void testNested() {
    variables.put("a", 23.42);
    assertEvaluatesToTrue(0.37, "not(>=a,13.37,].37...42),<.37)");
    assertEvaluatesToFalse(23.42, "not(>=a,13.37,].37...42),<.37)");
    assertEvaluatesToFalse(13.37, "not(>=a,13.37,].37...42),<.37)");
    assertEvaluatesToFalse(0.38, "not(>=a,13.37,].37...42),<.37)");
    assertEvaluatesToFalse(0, "not(>=a,13.37,].37...42),<.37)");
  }

    /**
   * Test method to verify that assertEvaluatesToTrue method returns true when passing in the values 13.37 and "-".
   */
  @Test
  public void testDontCare() {
    assertEvaluatesToTrue(13.37, "-");
  }

    /**
   * Test method for evaluating expressions with whitespace.
   */
  @Test
  public void testWhitespace() {
    assertEvaluatesToTrue("Hello World", "'Hello World' ");
    assertEvaluatesToTrue("Hello World", " 'Hello World'");
    assertEvaluatesToTrue("Hello World", " 'Hello World' ");
    assertEvaluatesToTrue(12, " 12 ");
    assertEvaluatesToTrue(10.2, " <12 ");
    assertEvaluatesToTrue(0, "< 12 ");
    assertEvaluatesToTrue(12.3, "\t>=12 ");
    assertEvaluatesToTrue(0, " not( 13 ,\t>0)\t");
  }

    /**
   * This method tests the evaluation of variables in a TestPojo object.
   */
  @Test
  public void testPojo() {
    variables.putValue("pojo", new TestPojo("foo", 13.37));
    assertEvaluatesToTrue("foo", "pojo.foo");
    assertEvaluatesToFalse("operaton", "pojo.foo");
    assertEvaluatesToTrue(12, "<= pojo.bar");
    assertEvaluatesToFalse(13.33, ">= pojo.bar");
  }

    /**
   * Evaluates the given FEEL expression with the provided input and asserts that the result is true.
   * 
   * @param input the input object to evaluate
   * @param feelExpression the FEEL expression to evaluate
   */
  public void assertEvaluatesToTrue(Object input, String feelExpression) {
    boolean result = evaluateFeel(input, feelExpression);
    assertThat(result).isTrue();
  }

    /**
   * Asserts that the given FEEL expression evaluates to false for the specified input object.
   * 
   * @param input the input object for the FEEL expression evaluation
   * @param feelExpression the FEEL expression to be evaluated
   */
  public void assertEvaluatesToFalse(Object input, String feelExpression) {
    boolean result = evaluateFeel(input, feelExpression);
    assertThat(result).isFalse();
  }

    /**
   * Evaluates a FEEL expression with the given input object.
   *
   * @param input the input object to evaluate the expression with
   * @param feelExpression the FEEL expression to evaluate
   * @return the result of evaluating the expression with the input object
   */
  public boolean evaluateFeel(Object input, String feelExpression) {
    variables.putValue(INPUT_VARIABLE, input);
    return feelEngine.evaluateSimpleUnaryTests(feelExpression, INPUT_VARIABLE, variables.asVariableContext());
  }

    /**
   * Parses a string containing a date and time and returns a DateValue object.
   * 
   * @param dateAndTimeString the string containing the date and time to be parsed
   * @return the DateValue object representing the parsed date and time
   */
  protected DateValue parseDateAndTime(String dateAndTimeString) {
    Date date = FeelFunctionMapper.parseDateAndTime(dateAndTimeString);
    return Variables.dateValue(date);
  }

  public class TestPojo {

    protected String foo;
    protected Double bar;

    public TestPojo(String foo, Double bar) {
      this.foo = foo;
      this.bar = bar;
    }

        /**
     * Returns the value of foo.
     *
     * @return the value of foo
     */
    public String getFoo() {
      return foo;
    }

        /**
     * Returns the value of the Double variable bar.
     *
     * @return the value of the Double variable bar
     */
    public Double getBar() {
      return bar;
    }

        /**
     * Returns a string representation of the TestPojo object, including the values of foo and bar.
     */
    public String toString() {
      return "TestPojo{" +
        "foo='" + foo + '\'' +
        ", bar=" + bar +
        '}';
    }

  }

}
