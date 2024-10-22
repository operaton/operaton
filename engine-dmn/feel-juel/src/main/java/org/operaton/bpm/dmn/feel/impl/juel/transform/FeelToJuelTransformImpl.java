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
package org.operaton.bpm.dmn.feel.impl.juel.transform;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineLogger;
import org.operaton.bpm.dmn.feel.impl.juel.FeelLogger;

public class FeelToJuelTransformImpl implements FeelToJuelTransform {

  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;

  public static final FeelToJuelTransformer NOT_TRANSFORMER = new NotTransformer();
  public static final FeelToJuelTransformer HYPHEN_TRANSFORMER = new HyphenTransformer();
  public static final FeelToJuelTransformer LIST_TRANSFORMER = new ListTransformer();
  public static final FeelToJuelTransformer INTERVAL_TRANSFORMER = new IntervalTransformer();
  public static final FeelToJuelTransformer COMPARISON_TRANSFORMER = new ComparisonTransformer();
  public static final FeelToJuelTransformer EQUAL_TRANSFORMER = new EqualTransformer();
  public static final FeelToJuelTransformer ENDPOINT_TRANSFORMER = new EndpointTransformer();
  public static final List<FeelToJuelTransformer> CUSTOM_FUNCTION_TRANSFORMERS = new ArrayList<FeelToJuelTransformer>();

    /**
   * Transforms a simple unary test expression into a JUEL expression by checking if it can be transformed using either the hyphen transformer, not transformer, or default positive unary test transformer.
   * 
   * @param simpleUnaryTests the simple unary test expression to transform
   * @param inputName the input name to be used in the transformation
   * @return the transformed JUEL expression
   */
  public String transformSimpleUnaryTests(String simpleUnaryTests, String inputName) {
    simpleUnaryTests = simpleUnaryTests.trim();
    String juelExpression;
    if (HYPHEN_TRANSFORMER.canTransform(simpleUnaryTests)) {
      juelExpression = HYPHEN_TRANSFORMER.transform(this, simpleUnaryTests, inputName);
    }
    else if (NOT_TRANSFORMER.canTransform(simpleUnaryTests)) {
      juelExpression = NOT_TRANSFORMER.transform(this, simpleUnaryTests, inputName);
    }
    else {
      juelExpression = transformSimplePositiveUnaryTests(simpleUnaryTests, inputName);
    }

    /**
   * Transforms a simple positive unary test to a specific format based on the input name.
   * 
   * @param simplePositiveUnaryTests the simple positive unary test to be transformed
   * @param inputName the name of the input
   * @return the transformed simple positive unary test
   */
  public String transformSimplePositiveUnaryTests(String simplePositiveUnaryTests, String inputName) {
    simplePositiveUnaryTests = simplePositiveUnaryTests.trim();
    if (LIST_TRANSFORMER.canTransform(simplePositiveUnaryTests)) {
      return LIST_TRANSFORMER.transform(this, simplePositiveUnaryTests, inputName);
    }
    else {
      return transformSimplePositiveUnaryTest(simplePositiveUnaryTests, inputName);
    }
  }

    /**
   * Transforms a simple positive unary test expression into a different format based on the available transformers.
   * 
   * @param simplePositiveUnaryTest the simple positive unary test expression to transform
   * @param inputName the name of the input
   * @return the transformed expression
   */
  public String transformSimplePositiveUnaryTest(String simplePositiveUnaryTest, String inputName) {
    simplePositiveUnaryTest = simplePositiveUnaryTest.trim();

    for (FeelToJuelTransformer functionTransformer : CUSTOM_FUNCTION_TRANSFORMERS) {
      if (functionTransformer.canTransform(simplePositiveUnaryTest)) {
        return functionTransformer.transform(this, simplePositiveUnaryTest, inputName);
      }
    }

    /**
   * Transforms the given endpoint using the specified input name.
   * 
   * @param endpoint the endpoint to transform
   * @param inputName the name of the input
   * @return the transformed endpoint
   */
  public String transformEndpoint(String endpoint, String inputName) {
    endpoint = endpoint.trim();
    return ENDPOINT_TRANSFORMER.transform(this, endpoint, inputName);
  }

    /**
   * Adds a custom function transformer to the list of custom function transformers.
   * 
   * @param functionTransformer the custom function transformer to be added
   */
  public void addCustomFunctionTransformer(FeelToJuelTransformer functionTransformer) {
    CUSTOM_FUNCTION_TRANSFORMERS.add(functionTransformer);
  }
}
