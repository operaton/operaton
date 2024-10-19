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

import org.operaton.bpm.dmn.feel.impl.juel.transform.FeelToJuelTransform;
import org.operaton.bpm.dmn.feel.impl.juel.transform.FeelToJuelTransformImpl;
import org.junit.BeforeClass;
import org.junit.Test;

public class FeelToJuelTransformTest {

  public static FeelToJuelTransform feelToJuelTransform;

    /**
   * Initializes the FeelToJuelTransform object before running test class.
   */
  @BeforeClass
  public static void initTransform() {
    feelToJuelTransform = new FeelToJuelTransformImpl();
  }

    /**
   * This method tests the transformation of endpoint strings.
   */
  @Test
  public void testEndpointString() {
    assertTransform("x", "\"Hello World\"", "${x == \"Hello World\"}");
    assertTransform("x", "\"\"", "${x == \"\"}");
    assertTransform("x", "\"123\"", "${x == \"123\"}");
    assertTransform("x", "\"Why.not?\"", "${x == \"Why.not?\"}");
    assertTransform("x", "'Hello'", "${x == 'Hello'}");
    assertTransform("x", "\"1,2,3\"", "${x == \"1,2,3\"}");
  }

    /**
   * Tests the evaluation of endpoint variables in the transformation.
   */
  @Test
  public void testEndpointVariable() {
    assertTransform("x", "y", "${x == y}");
    assertTransform("x", "customer.y", "${x == customer.y}");
  }

    /**
   * Test method to verify the transformation of comparison operators for endpoint variables being greater than another variable.
   */
  @Test
  public void testEndpointVariableGreater() {
    assertTransform("x", "<y", "${x < y}");
    assertTransform("x", "<customer.y", "${x < customer.y}");
  }

    /**
   * Test method for transforming endpoint variable with greater than or equal comparison
   */
  @Test
  public void testEndpointVariableGreaterEqual() {
    assertTransform("x", "<=y", "${x <= y}");
    assertTransform("x", "<=customer.y", "${x <= customer.y}");
  }

    /**
   * Test method for evaluating endpoint variables without any additional variables.
   * Asserts the transformation of input strings to expected output strings using the assertTransform method.
   */
  @Test
  public void testEndpointVariableLess() {
    assertTransform("x", ">y", "${x > y}");
    assertTransform("x", ">customer.y", "${x > customer.y}");
  }

    /**
   * Test method for verifying transformation of endpoint variables with less than or equal comparison.
   */
  @Test
  public void testEndpointVariableLessEqual() {
    assertTransform("x", ">=y", "${x >= y}");
    assertTransform("x", ">=customer.y", "${x >= customer.y}");
  }

    /**
   * Tests the transformation of a boolean endpoint.
   */
  @Test
  public void testEndpointBoolean() {
    assertTransform("x", "true", "${x == true}");
    assertTransform("x", "false", "${x == false}");
  }

    /**
   * Test method to verify the transformation of endpoints with numbers.
   */
  @Test
  public void testEndpointNumber() {
    assertTransform("x", "13", "${x == 13}");
    assertTransform("x", "13.37", "${x == 13.37}");
    assertTransform("x", ".37", "${x == .37}");
  }

    /**
   * Test method to validate the transformation of numeric comparison operators to expressions using the less than symbol.
   * Asserts that the transformation of variable 'x' being less than 13 results in the expression "${x < 13}",
   * 'x' being less than 13.37 results in "${x < 13.37}", and 'x' being less than 0.37 results in "${x < 0.37}".
   */
  @Test
  public void testEndpointNumberGreater() {
    assertTransform("x", "<13", "${x < 13}");
    assertTransform("x", "<13.37", "${x < 13.37}");
    assertTransform("x", "<.37", "${x < .37}");
  }

    /**
   * Test method for transforming endpoint numbers to the appropriate syntax.
   * Checks if the transformed expression is correct for various input values.
   */
  @Test
    public void testEndpointNumberGreaterEqual() {
      assertTransform("x", "<=13", "${x <= 13}");
      assertTransform("x", "<=13.37", "${x <= 13.37}");
      assertTransform("x", "<=.37", "${x <= .37}");
    }

    /**
   * Test method to verify transformation of number greater than comparison operators.
   */
  @Test
  public void testEndpointNumberLess() {
    assertTransform("x", ">13", "${x > 13}");
    assertTransform("x", ">13.37", "${x > 13.37}");
    assertTransform("x", ">.37", "${x > .37}");
  }

    /**
   * Test method for transforming endpoint number to a string with less than or equal comparison.
   * Asserts transformation of integers, decimals, and decimals without integer part.
   */
  @Test
  public void testEndpointNumberLessEqual() {
    assertTransform("x", ">=13", "${x >= 13}");
    assertTransform("x", ">=13.37", "${x >= 13.37}");
    assertTransform("x", ">=.37", "${x >= .37}");
  }

    /**
   * Test the endpoint date transformation.
   */
  @Test
  public void testEndpointDate() {
    assertTransform("x", "date(\"2015-12-12\")", "${x == date(\"2015-12-12\")}");
  }

    /**
   * Test method for validating interval number transformation.
   */
  @Test
    public void testIntervalNumber() {
      assertTransform("x", "[0..12]", "${x >= 0 && x <= 12}");
      assertTransform("x", "[0..12)", "${x >= 0 && x < 12}");
      assertTransform("x", "[0..12[", "${x >= 0 && x < 12}");
      
      assertTransform("x", "[0.12..13.37]", "${x >= 0.12 && x <= 13.37}");
      assertTransform("x", "[0.12..13.37)", "${x >= 0.12 && x < 13.37}");
      assertTransform("x", "[0.12..13.37[", "${x >= 0.12 && x < 13.37}");
  
      assertTransform("x", "[.12...37]", "${x >= .12 && x <= .37}");
      assertTransform("x", "[.12...37)", "${x >= .12 && x < .37}");
      assertTransform("x", "[.12...37[", "${x >= .12 && x < .37}");
      
      assertTransform("x", "(0..12]", "${x > 0 && x <= 12}");
      assertTransform("x", "(0..12)", "${x > 0 && x < 12}");
      assertTransform("x", "(0..12[", "${x > 0 && x < 12}");
      
      assertTransform("x", "(0.12..13.37]", "${x > 0.12 && x <= 13.37}");
      assertTransform("x", "(0.12..13.37)", "${x > 0.12 && x < 13.37}");
      assertTransform("x", "(0.12..13.37[", "${x > 0.12 && x < 13.37}");
      
      assertTransform("x", "(.12...37]", "${x > .12 && x <= .37}");
      assertTransform("x", "(.12...37)", "${x > .12 && x < .37}");
      assertTransform("x", "(.12...37[", "${x > .12 && x < .37}");
      
      assertTransform("x", "]0..12]", "${x > 0 && x <= 12}");
      assertTransform("x", "]0..12)", "${x > 0 && x < 12}");
      assertTransform("x", "]0..12[", "${x > 0 && x < 12}");
      
      assertTransform("x", "]0.12..13.37]", "${x > 0.12 && x <= 13.37}");
      assertTransform("x", "]0.12..13.37)", "${x > 0.12 && x < 13.37}");
      assertTransform("x", "]0.12..13.37[", "${x > 0.12 && x < 13.37}");
      
      assertTransform("x", "].12...37]", "${x > .12 && x <= .37}");
      assertTransform("x", "].12...37)", "${x > .12 && x < .37}");
      assertTransform("x", "].12...37[", "${x > .12 && x < .37}");
    }

    /**
   * This method is used to test different interval variables by asserting transformations.
   */
  @Test
  public void testIntervalVariable() {
    assertTransform("x", "[a..b]", "${x >= a && x <= b}");
    assertTransform("x", "[a..b)", "${x >= a && x < b}");
    assertTransform("x", "[a..b[", "${x >= a && x < b}");

    assertTransform("x", "(a..b]", "${x > a && x <= b}");
    assertTransform("x", "(a..b)", "${x > a && x < b}");
    assertTransform("x", "(a..b[", "${x > a && x < b}");

    assertTransform("x", "]a..b]", "${x > a && x <= b}");
    assertTransform("x", "]a..b)", "${x > a && x < b}");
    assertTransform("x", "]a..b[", "${x > a && x < b}");
  }

    /**
   * Method to test different interval date conditions and transformations.
   */
  @Test
    public void testIntervalDate() {
      assertTransform("x", "[date(\"2015-12-12\")..date(\"2016-06-06\")]", "${x >= date(\"2015-12-12\") && x <= date(\"2016-06-06\")}");
      assertTransform("x", "[date(\"2015-12-12\")..date(\"2016-06-06\"))", "${x >= date(\"2015-12-12\") && x < date(\"2016-06-06\")}");
      assertTransform("x", "[date(\"2015-12-12\")..date(\"2016-06-06\")[", "${x >= date(\"2015-12-12\") && x < date(\"2016-06-06\")}");
  
      assertTransform("x", "(date(\"2015-12-12\")..date(\"2016-06-06\")]", "${x > date(\"2015-12-12\") && x <= date(\"2016-06-06\")}");
      assertTransform("x", "(date(\"2015-12-12\")..date(\"2016-06-06\"))", "${x > date(\"2015-12-12\") && x < date(\"2016-06-06\")}");
      assertTransform("x", "(date(\"2015-12-12\")..date(\"2016-06-06\")[", "${x > date(\"2015-12-12\") && x < date(\"2016-06-06\")}");
  
      assertTransform("x", "]date(\"2015-12-12\")..date(\"2016-06-06\")]", "${x > date(\"2015-12-12\") && x <= date(\"2016-06-06\")}");
      assertTransform("x", "]date(\"2015-12-12\")..date(\"2016-06-06\"))", "${x > date(\"2015-12-12\") && x < date(\"2016-06-06\")}");
      assertTransform("x", "]date(\"2015-12-12\")..date(\"2016-06-06\")[", "${x > date(\"2015-12-12\") && x < date(\"2016-06-06\")}");
    }

    /**
   * This method is used to test the transformation of "not" operator in the assertTransform method.
   */
  @Test
  public void testNot() {
    assertTransform("x", "not(\"Hello World\")", "${not(x == \"Hello World\")}");
    assertTransform("x", "not(y)", "${not(x == y)}");
    assertTransform("x", "not(<y)", "${not(x < y)}");
    assertTransform("x", "not(<=y)", "${not(x <= y)}");
    assertTransform("x", "not(>y)", "${not(x > y)}");
    assertTransform("x", "not(>=y)", "${not(x >= y)}");
    assertTransform("x", "not(13.37)", "${not(x == 13.37)}");
    assertTransform("x", "not(<13.37)", "${not(x < 13.37)}");
    assertTransform("x", "not(<=13.37)", "${not(x <= 13.37)}");
    assertTransform("x", "not(>13.37)", "${not(x > 13.37)}");
    assertTransform("x", "not(>=13.37)", "${not(x >= 13.37)}");
    assertTransform("x", "not(.37)", "${not(x == .37)}");
    assertTransform("x", "not(<.37)", "${not(x < .37)}");
    assertTransform("x", "not(<=.37)", "${not(x <= .37)}");
    assertTransform("x", "not(>.37)", "${not(x > .37)}");
    assertTransform("x", "not(>=.37)", "${not(x >= .37)}");
    assertTransform("x", "not(date(\"2015-12-12\"))", "${not(x == date(\"2015-12-12\"))}");
    assertTransform("x", "not(<date(\"2015-12-12\"))", "${not(x < date(\"2015-12-12\"))}");
    assertTransform("x", "not(<=date(\"2015-12-12\"))", "${not(x <= date(\"2015-12-12\"))}");
    assertTransform("x", "not(>date(\"2015-12-12\"))", "${not(x > date(\"2015-12-12\"))}");
    assertTransform("x", "not(>=date(\"2015-12-12\"))", "${not(x >= date(\"2015-12-12\"))}");
  }

    /**
   * This method tests the functionality of the assertTransform method by passing different input values and expected output transformations.
   */
  @Test
  public void testList() {
    assertTransform("x", "a,\"Hello World\"", "${(x == a) || (x == \"Hello World\")}");
    assertTransform("x", "y,12,13.37,.37", "${(x == y) || (x == 12) || (x == 13.37) || (x == .37)}");
    assertTransform("x", "<y,<=12,>13.37,>=.37", "${(x < y) || (x <= 12) || (x > 13.37) || (x >= .37)}");
    assertTransform("x", "a,date(\"2015-12-12\"),date(\"2016-06-06\"),date(\"2017-07-07\")", "${(x == a) || (x == date(\"2015-12-12\")) || (x == date(\"2016-06-06\")) || (x == date(\"2017-07-07\"))}");
    assertTransform("x", "<a,<=date(\"2015-12-12\"),>date(\"2016-06-06\"),>=date(\"2017-07-07\")", "${(x < a) || (x <= date(\"2015-12-12\")) || (x > date(\"2016-06-06\")) || (x >= date(\"2017-07-07\"))}");
    assertTransform("x", "1,\"2,3,4\",5,\"6,7,8\",9", "${(x == 1) || (x == \"2,3,4\") || (x == 5) || (x == \"6,7,8\") || (x == 9)}");
  }

    /**
   * Test method for testing nested transformations in assertTransform method.
   */
  @Test
  public void testNested() {
    assertTransform("x", "not(>=a,13.37,].37...42),<.37)", "${not((x >= a) || (x == 13.37) || (x > .37 && x < .42) || (x < .37))}");
  }

    /**
   * Test method for assertTransform with given input values.
   */
  @Test
  public void testDontCare() {
    assertTransform("x", "-", "${true}");
  }

    /**
   * Test method to verify transformation of whitespace in expressions.
   */
  @Test
  public void testWhitespace() {
    assertTransform("x", "'Hello World' ", "${x == 'Hello World'}");
    assertTransform("x", " 'Hello World'", "${x == 'Hello World'}");
    assertTransform("x", " 'Hello World' ", "${x == 'Hello World'}");
    assertTransform("x", " 12 ", "${x == 12}");
    assertTransform("x", " <12 ", "${x < 12}");
    assertTransform("x", "< 12 ", "${x < 12}");
    assertTransform("x", "\t>=12 ", "${x >= 12}");
    assertTransform("x", " not( x,\t>0,  <12 , a )\t", "${not((x == x) || (x > 0) || (x < 12) || (x == a))}");
  }

    /**
   * Asserts that the transformation of a given FEEL expression to JUEL expression matches the expected expression when applied to a specific input.
   *
   * @param input the input value to be used in the transformation
   * @param feelExpression the FEEL expression to be transformed
   * @param expectedExpression the expected JUEL expression after transformation
   */
  public void assertTransform(String input, String feelExpression, String expectedExpression) {
    String expression = feelToJuelTransform.transformSimpleUnaryTests(feelExpression, input);
    assertThat(expression).isEqualTo(expectedExpression);
  }

}
