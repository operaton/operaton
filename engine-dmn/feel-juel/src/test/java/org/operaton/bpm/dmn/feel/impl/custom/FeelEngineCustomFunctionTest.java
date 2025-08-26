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
package org.operaton.bpm.dmn.feel.impl.custom;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.dmn.feel.impl.FeelEngine;
import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineFactoryImpl;
import org.operaton.bpm.dmn.feel.impl.juel.transform.FeelToJuelFunctionTransformer;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;

public class FeelEngineCustomFunctionTest {

  public static final String INPUT_VARIABLE = "input";

  public FeelEngine feelEngine;

  public VariableMap variables;

  @BeforeEach
  void initEngine() {
    variables = Variables.createVariables();

    List<FeelToJuelFunctionTransformer> customFunctionTransformers = new ArrayList<>();
    customFunctionTransformers.add(new StartsWithFunctionTransformer());

    FeelEngineFactoryImpl feelEngineFactory = new FeelEngineFactoryImpl(customFunctionTransformers);

    feelEngine = feelEngineFactory.createInstance();
  }

  @Test
  void stringStartsWith() {
    assertEvaluatesToTrue("foobar", "starts with(\"foo\")");
    assertEvaluatesToFalse("foobar", "starts with(\"foa\")");
    assertEvaluatesToFalse("foobar", "starts with(\"afoo\")");
    assertEvaluatesToTrue("foobar", "starts with(\"foobar\")");
    assertEvaluatesToFalse("", "starts with(\"foobar\")");
    assertEvaluatesToFalse(null, "starts with(\"foobar\")");
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

}
