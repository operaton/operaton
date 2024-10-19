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

public class EqualTransformer implements FeelToJuelTransformer {

    /**
   * Checks if the given feel expression can be transformed.
   *
   * @param feelExpression the feel expression to check
   * @return true if the expression can be transformed, false otherwise
   */
  public boolean canTransform(String feelExpression) {
    return true;
  }

    /**
   * Transforms a FEEL expression to a JUEL expression using the specified transform,
   * then constructs and returns a string combining the input name and the transformed JUEL endpoint.
   * 
   * @param transform the transformation object to use
   * @param feelExpression the FEEL expression to transform
   * @param inputName the name of the input
   * @return a string representing the input name and transformed JUEL endpoint
   */
  public String transform(FeelToJuelTransform transform, String feelExpression, String inputName) {
    String juelEndpoint = transform.transformEndpoint(feelExpression, inputName);
    return String.format("%s == %s", inputName, juelEndpoint);
  }

}
