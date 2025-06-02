/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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

/**
 * The ListTransformer class is responsible for transforming FEEL (Friendly Enough Expression Language)
 * list expressions into JUEL (Java Unified Expression Language) expressions.
 */
public class ListTransformer implements FeelToJuelTransformer {

  /** Logger instance for logging errors and debug information. */
  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;

  /**
   * Determines if the given FEEL expression can be transformed.
   *
   * @param feelExpression the FEEL expression to check
   * @return true if the expression can be transformed, false otherwise
   */
  @Override
  public boolean canTransform(String feelExpression) {
    return splitExpression(feelExpression).size() > 1;
  }

  /**
   * Transforms a FEEL expression into a JUEL expression.
   *
   * @param transform the transformation logic to apply
   * @param feelExpression the FEEL expression to transform
   * @param inputName the name of the input variable
   * @return the transformed JUEL expression
   */
  @Override
  public String transform(FeelToJuelTransform transform, String feelExpression, String inputName) {
    List<String> juelExpressions = transformExpressions(transform, feelExpression, inputName);
    return joinExpressions(juelExpressions);
  }

  /**
   * Collects individual expressions from a FEEL list expression.
   *
   * @param feelExpression the FEEL expression to split
   * @return a list of individual expressions
   */
  protected List<String> collectExpressions(String feelExpression) {
    return splitExpression(feelExpression);
  }

  /**
   * Splits a FEEL expression into a list of sub-expressions based on the comma separator.
   * Handles quoted strings to ensure commas within quotes are not treated as separators.
   * <p>
   * This method iterates through the characters of the FEEL expression.
   * That way, it can prevent from StackOverflowError
   * </p>
   * @param feelExpression the FEEL expression to split
   * @return a list of sub-expressions
   */
  private List<String> splitExpression(String feelExpression) {
    List<String> result = new ArrayList<>();
    boolean inQuotes = false; // Tracks whether the current character is inside quotes
    StringBuilder currentExpression = new StringBuilder(); // Holds the current sub-expression
    for (int i = 0; i < feelExpression.length(); i++) {
      char c = feelExpression.charAt(i);

      if (c == '"') {
        inQuotes = !inQuotes; // Toggle the inQuotes flag when encountering a quote
        currentExpression.append(c);
      } else if (c == ',' && !inQuotes) {
        // If a comma is encountered outside quotes, finalize the current expression
        result.add(currentExpression.toString());
        currentExpression = new StringBuilder();
      } else {
        currentExpression.append(c); // Append the character to the current expression
      }
    }

    // Add the last expression if it exists
    result.add(currentExpression.toString());

    return result;
  }

  /**
   * Transforms individual FEEL expressions into JUEL expressions.
   *
   * @param transform the transformation logic to apply
   * @param feelExpression the FEEL expression to transform
   * @param inputName the name of the input variable
   * @return a list of transformed JUEL expressions
   * @throws IllegalArgumentException if an empty expression is encountered
   */
  protected List<String> transformExpressions(FeelToJuelTransform transform, String feelExpression, String inputName) {
    List<String> expressions = collectExpressions(feelExpression);
    List<String> juelExpressions = new ArrayList<>();
    for (String expression : expressions) {
      if (!expression.trim().isEmpty()) {
        // Transform each non-empty FEEL expression into a JUEL expression
        String juelExpression = transform.transformSimplePositiveUnaryTest(expression, inputName);
        juelExpressions.add(juelExpression);
      }
      else {
        // Log and throw an exception if an empty expression is encountered
        throw LOG.invalidListExpression(feelExpression);
      }
    }
    return juelExpressions;
  }

  /**
   * Joins a list of JUEL expressions into a single expression using logical OR (||).
   *
   * @param juelExpressions the list of JUEL expressions to join
   * @return the combined JUEL expression
   */
  protected String joinExpressions(List<String> juelExpressions) {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(juelExpressions.get(0)).append(")");
    for (int i = 1; i < juelExpressions.size(); i++) {
      builder.append(" || (").append(juelExpressions.get(i)).append(")");
    }
    return builder.toString();
  }

}
