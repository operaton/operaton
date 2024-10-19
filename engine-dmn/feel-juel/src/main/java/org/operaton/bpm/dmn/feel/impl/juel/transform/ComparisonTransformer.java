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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineLogger;
import org.operaton.bpm.dmn.feel.impl.juel.FeelLogger;

public class ComparisonTransformer implements FeelToJuelTransformer {

  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;
  public static final Pattern COMPARISON_PATTERN = Pattern.compile("^(<=|>=|<|>)([^=].*)$");

    /**
   * Checks if the given feelExpression starts with "<=", "<", ">=", or ">".
   * @param feelExpression the FEEL expression to check
   * @return true if the feelExpression starts with one of the specified operators, false otherwise
   */
  public boolean canTransform(String feelExpression) {
    return feelExpression.startsWith("<=") || feelExpression.startsWith("<") || feelExpression.startsWith(">=") || feelExpression.startsWith(">");
  }

    /**
   * Transforms a given FEEL expression using the specified transformation, input name, and comparison pattern.
   *
   * @param transform The transformation to be applied
   * @param feelExpression The FEEL expression to transform
   * @param inputName The input name to be used
   * @return The transformed expression
   * @throws IllegalArgumentException if the FEEL expression is not a valid comparison expression
   */
  public String transform(FeelToJuelTransform transform, String feelExpression, String inputName) {
    Matcher matcher = COMPARISON_PATTERN.matcher(feelExpression);
    if (matcher.matches()) {
      return transformComparison(transform, matcher.group(1), matcher.group(2), inputName);
    }
    else {
      throw LOG.invalidComparisonExpression(feelExpression);
    }
  }

    /**
   * Transforms a comparison expression from FEEL to JUEL format.
   *
   * @param transform the FEEL to JUEL transform object
   * @param operator the comparison operator
   * @param endpoint the endpoint to transform
   * @param inputName the name of the input
   * @return the transformed comparison expression
   */
  protected String transformComparison(FeelToJuelTransform transform, String operator, String endpoint, String inputName) {
    String juelEndpoint = transform.transformEndpoint(endpoint, inputName);
    return String.format("%s %s %s", inputName, operator, juelEndpoint);
  }

}
