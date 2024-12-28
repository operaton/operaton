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

import org.operaton.bpm.dmn.feel.impl.juel.transform.FeelToJuelTransform;
import org.operaton.bpm.dmn.feel.impl.juel.transform.FeelToJuelTransformImpl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FeelToJuelTransformTest {

  public static FeelToJuelTransform feelToJuelTransform;

  @BeforeAll
  static void initTransform() {
    feelToJuelTransform = new FeelToJuelTransformImpl();
  }

  @Test
  void endpointString() {
    assertTransform("x", "\"Hello World\"", "${x == \"Hello World\"}");
    assertTransform("x", "\"\"", "${x == \"\"}");
    assertTransform("x", "\"123\"", "${x == \"123\"}");
    assertTransform("x", "\"Why.not?\"", "${x == \"Why.not?\"}");
    assertTransform("x", "'Hello'", "${x == 'Hello'}");
    assertTransform("x", "\"1,2,3\"", "${x == \"1,2,3\"}");
  }

  @Test
  void endpointVariable() {
    assertTransform("x", "y", "${x == y}");
    assertTransform("x", "customer.y", "${x == customer.y}");
  }

  @Test
  void endpointVariableGreater() {
    assertTransform("x", "<y", "${x < y}");
    assertTransform("x", "<customer.y", "${x < customer.y}");
  }

  @Test
  void endpointVariableGreaterEqual() {
    assertTransform("x", "<=y", "${x <= y}");
    assertTransform("x", "<=customer.y", "${x <= customer.y}");
  }

  @Test
  void endpointVariableLess() {
    assertTransform("x", ">y", "${x > y}");
    assertTransform("x", ">customer.y", "${x > customer.y}");
  }

  @Test
  void endpointVariableLessEqual() {
    assertTransform("x", ">=y", "${x >= y}");
    assertTransform("x", ">=customer.y", "${x >= customer.y}");
  }

  @Test
  void endpointBoolean() {
    assertTransform("x", "true", "${x == true}");
    assertTransform("x", "false", "${x == false}");
  }

  @Test
  void endpointNumber() {
    assertTransform("x", "13", "${x == 13}");
    assertTransform("x", "13.37", "${x == 13.37}");
    assertTransform("x", ".37", "${x == .37}");
  }

  @Test
  void endpointNumberGreater() {
    assertTransform("x", "<13", "${x < 13}");
    assertTransform("x", "<13.37", "${x < 13.37}");
    assertTransform("x", "<.37", "${x < .37}");
  }

  @Test
  void endpointNumberGreaterEqual() {
    assertTransform("x", "<=13", "${x <= 13}");
    assertTransform("x", "<=13.37", "${x <= 13.37}");
    assertTransform("x", "<=.37", "${x <= .37}");
  }

  @Test
  void endpointNumberLess() {
    assertTransform("x", ">13", "${x > 13}");
    assertTransform("x", ">13.37", "${x > 13.37}");
    assertTransform("x", ">.37", "${x > .37}");
  }

  @Test
  void endpointNumberLessEqual() {
    assertTransform("x", ">=13", "${x >= 13}");
    assertTransform("x", ">=13.37", "${x >= 13.37}");
    assertTransform("x", ">=.37", "${x >= .37}");
  }

  @Test
  void endpointDate() {
    assertTransform("x", "date(\"2015-12-12\")", "${x == date(\"2015-12-12\")}");
  }

  @Test
  void intervalNumber() {
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

  @Test
  void intervalVariable() {
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

  @Test
  void intervalDate() {
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

  @Test
  void not() {
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

  @Test
  void list() {
    assertTransform("x", "a,\"Hello World\"", "${(x == a) || (x == \"Hello World\")}");
    assertTransform("x", "y,12,13.37,.37", "${(x == y) || (x == 12) || (x == 13.37) || (x == .37)}");
    assertTransform("x", "<y,<=12,>13.37,>=.37", "${(x < y) || (x <= 12) || (x > 13.37) || (x >= .37)}");
    assertTransform("x", "a,date(\"2015-12-12\"),date(\"2016-06-06\"),date(\"2017-07-07\")", "${(x == a) || (x == date(\"2015-12-12\")) || (x == date(\"2016-06-06\")) || (x == date(\"2017-07-07\"))}");
    assertTransform("x", "<a,<=date(\"2015-12-12\"),>date(\"2016-06-06\"),>=date(\"2017-07-07\")", "${(x < a) || (x <= date(\"2015-12-12\")) || (x > date(\"2016-06-06\")) || (x >= date(\"2017-07-07\"))}");
    assertTransform("x", "1,\"2,3,4\",5,\"6,7,8\",9", "${(x == 1) || (x == \"2,3,4\") || (x == 5) || (x == \"6,7,8\") || (x == 9)}");
  }

  @Test
  void nested() {
    assertTransform("x", "not(>=a,13.37,].37...42),<.37)", "${not((x >= a) || (x == 13.37) || (x > .37 && x < .42) || (x < .37))}");
  }

  @Test
  void dontCare() {
    assertTransform("x", "-", "${true}");
  }

  @Test
  void whitespace() {
    assertTransform("x", "'Hello World' ", "${x == 'Hello World'}");
    assertTransform("x", " 'Hello World'", "${x == 'Hello World'}");
    assertTransform("x", " 'Hello World' ", "${x == 'Hello World'}");
    assertTransform("x", " 12 ", "${x == 12}");
    assertTransform("x", " <12 ", "${x < 12}");
    assertTransform("x", "< 12 ", "${x < 12}");
    assertTransform("x", "\t>=12 ", "${x >= 12}");
    assertTransform("x", " not( x,\t>0,  <12 , a )\t", "${not((x == x) || (x > 0) || (x < 12) || (x == a))}");
  }

  public void assertTransform(String input, String feelExpression, String expectedExpression) {
    String expression = feelToJuelTransform.transformSimpleUnaryTests(feelExpression, input);
    assertThat(expression).isEqualTo(expectedExpression);
  }

}
