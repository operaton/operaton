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

public class NotTransformer implements FeelToJuelTransformer {

  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;
  public static final Pattern NOT_PATTERN = Pattern.compile("^not\\((.+)\\)$");

    /**
   * Checks if the given feel expression starts with "not("
   * @param feelExpression the feel expression to check
   * @return true if the expression starts with "not(", false otherwise
   */
  public boolean canTransform(String feelExpression) {
    return feelExpression.startsWith("not(");
  }

    /**
   * Transforms a FEEL expression into a JUEL expression by extracting inner expressions, transforming them, and adding a 'not' operator.
   *
   * @param transform the transformation object used to convert FEEL to JUEL expressions
   * @param feelExpression the FEEL expression to transform
   * @param inputName the name of the input variable
   * @return the transformed JUEL expression with a 'not' operator added
   */
  public String transform(FeelToJuelTransform transform, String feelExpression, String inputName) {
    String simplePositiveUnaryTests = extractInnerExpression(feelExpression);
    String juelExpression = transform.transformSimplePositiveUnaryTests(simplePositiveUnaryTests, inputName);
    return "not(" + juelExpression + ")";
  }

    /**
   * Extracts the inner expression from a FEEL expression that starts with a "not" keyword.
   * 
   * @param feelExpression the FEEL expression to extract the inner expression from
   * @return the inner expression extracted from the input FEEL expression
   * @throws IllegalArgumentException if the input FEEL expression does not start with a "not" keyword
   */
  public String extractInnerExpression(String feelExpression) {
    Matcher matcher = NOT_PATTERN.matcher(feelExpression);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    else {
      throw LOG.invalidNotExpression(feelExpression);
    }
  }

}
