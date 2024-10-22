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
package org.operaton.bpm.dmn.feel.impl.custom;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.dmn.feel.impl.FeelEngine;
import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineFactoryImpl;
import org.operaton.bpm.dmn.feel.impl.juel.transform.FeelToJuelFunctionTransformer;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.junit.Before;
import org.junit.Test;

public class FeelEngineCustomFunctionTest {

  public static final String INPUT_VARIABLE = "input";

  public FeelEngine feelEngine;

  public VariableMap variables;

    /**
   * Initializes the engine by creating variables, custom function transformers, and an instance of the FEEL engine.
   */
  @Before
  public void initEngine() {
    variables = Variables.createVariables();

    List<FeelToJuelFunctionTransformer> customFunctionTransformers = new ArrayList<FeelToJuelFunctionTransformer>();
    customFunctionTransformers.add(new StartsWithFunctionTransformer());

    FeelEngineFactoryImpl feelEngineFactory = new FeelEngineFactoryImpl(customFunctionTransformers);

    feelEngine = feelEngineFactory.createInstance();
  }

    /**
   * This method tests the functionality of the starts with method by evaluating different scenarios.
   */
  @Test
    public void testStringStartsWith() {
      assertEvaluatesToTrue("foobar", "starts with(\"foo\")");
      assertEvaluatesToFalse("foobar", "starts with(\"foa\")");
      assertEvaluatesToFalse("foobar", "starts with(\"afoo\")");
      assertEvaluatesToTrue("foobar", "starts with(\"foobar\")");
      assertEvaluatesToFalse("", "starts with(\"foobar\")");
      assertEvaluatesToFalse(null, "starts with(\"foobar\")");
    }

    /**
   * Asserts that the given FEEL expression evaluates to true when applied to the input object.
   * 
   * @param input the input object to evaluate the FEEL expression on
   * @param feelExpression the FEEL expression to evaluate
   */
  public void assertEvaluatesToTrue(Object input, String feelExpression) {
    boolean result = evaluateFeel(input, feelExpression);
    assertThat(result).isTrue();
  }

    /**
   * Asserts that the given FEEL expression evaluates to false when applied to the provided input.
   *
   * @param input the input object to evaluate the FEEL expression against
   * @param feelExpression the FEEL expression to evaluate
   */
  public void assertEvaluatesToFalse(Object input, String feelExpression) {
    boolean result = evaluateFeel(input, feelExpression);
    assertThat(result).isFalse();
  }

    /**
   * Evaluates the specified feel expression with the given input object.
   * 
   * @param input the input object to be evaluated
   * @param feelExpression the feel expression to be evaluated
   * @return true if the expression evaluates to true, false otherwise
   */
  public boolean evaluateFeel(Object input, String feelExpression) {
    variables.putValue(INPUT_VARIABLE, input);
    return feelEngine.evaluateSimpleUnaryTests(feelExpression, INPUT_VARIABLE, variables.asVariableContext());
  }

}
