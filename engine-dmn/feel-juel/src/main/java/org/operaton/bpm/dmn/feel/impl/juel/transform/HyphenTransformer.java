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

public class HyphenTransformer implements FeelToJuelTransformer {

    /**
   * Checks if the given feelExpression is equal to "-"
   * 
   * @param feelExpression the feel expression to check
   * @return true if the feel expression is equal to "-", false otherwise
   */
  public boolean canTransform(String feelExpression) {
    return feelExpression.equals("-");
  }

    /**
   * Transforms a FEEL expression to a JUEL expression.
   * 
   * @param transform the transformation to be applied
   * @param feelExpression the FEEL expression to be transformed
   * @param inputName the name of the input
   * @return the transformed JUEL expression as a String
   */
  public String transform(FeelToJuelTransform transform, String feelExpression, String inputName) {
    return "true";
  }

}
