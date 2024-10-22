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
package org.operaton.bpm.dmn.engine.impl;

import org.operaton.bpm.dmn.engine.DmnDecisionLogic;

public class DmnDecisionLiteralExpressionImpl implements DmnDecisionLogic {

  protected DmnVariableImpl variable;
  protected DmnExpressionImpl expression;

    /**
   * Returns the variable associated with this DmnVariableImpl object.
   *
   * @return the variable associated with this DmnVariableImpl object
   */
  public DmnVariableImpl getVariable() {
    return variable;
  }
    /**
   * Sets the variable for the DMN.
   * 
   * @param variable the DMN variable to be set
   */
  public void setVariable(DmnVariableImpl variable) {
    this.variable = variable;
  }
    /**
   * Returns the expression associated with this DmnExpressionImpl object.
   *
   * @return the expression associated with this DmnExpressionImpl object
   */
  public DmnExpressionImpl getExpression() {
    return expression;
  }
    /**
   * Sets the DMN expression for this object.
   *
   * @param expression the DMN expression to be set
   */
  public void setExpression(DmnExpressionImpl expression) {
    this.expression = expression;
  }

    /**
   * Returns a string representation of the DmnDecisionLiteralExpressionImpl object.
   */
  @Override
  public String toString() {
    return "DmnDecisionLiteralExpressionImpl [variable=" + variable + ", expression=" + expression + "]";
  }

}
