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

public class DmnDecisionTableInputImpl {

  public static final String DEFAULT_INPUT_VARIABLE_NAME = "cellInput";

  public String id;
  public String name;

  protected DmnExpressionImpl expression;
  protected String inputVariable;

    /**
   * Returns the name of the object.
   *
   * @return the name of the object
   */
  public String getName() {
    return name;
  }

    /**
   * Sets the name of the object.
   * 
   * @param name the new name to set
   */
  public void setName(String name) {
    this.name = name;
  }

    /**
   * Returns the id of the object.
   *
   * @return the id of the object
   */
  public String getId() {
    return id;
  }

    /**
   * Sets the ID of the object.
   *
   * @param id the ID to set
   */
  public void setId(String id) {
    this.id = id;
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
  * Returns the expression associated with this DmnExpressionImpl object.
  * 
  * @return the expression associated with this DmnExpressionImpl object
  */
  public DmnExpressionImpl getExpression() {
    return expression;
  }

    /**
   * Retrieves the input variable. If the input variable is not null, returns the input variable. 
   * Otherwise, returns the default input variable name.
   * 
   * @return the input variable or the default input variable name
   */
  public String getInputVariable() {
    if (inputVariable != null) {
      return inputVariable;
    }
    else {
      return DEFAULT_INPUT_VARIABLE_NAME;
    }
  }

    /**
   * Sets the input variable to the specified value.
   *
   * @param inputVariable the new value for the input variable
   */
  public void setInputVariable(String inputVariable) {
    this.inputVariable = inputVariable;
  }

    /**
   * Returns a string representation of the DmnDecisionTableInputImpl object
   */
  @Override
  public String toString() {
    return "DmnDecisionTableInputImpl{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", expression=" + expression +
      ", inputVariable='" + inputVariable + '\'' +
      '}';
  }

}
