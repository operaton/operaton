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
package org.operaton.bpm.dmn.feel.impl.juel.el;

import org.operaton.bpm.impl.juel.jakarta.el.ExpressionFactory;
import org.operaton.bpm.impl.juel.jakarta.el.ValueExpression;
import org.operaton.bpm.impl.juel.jakarta.el.VariableMapper;

import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineLogger;
import org.operaton.bpm.dmn.feel.impl.juel.FeelLogger;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class FeelTypedVariableMapper extends VariableMapper {

  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;

  protected ExpressionFactory expressionFactory;
  protected VariableContext variableContext;

  public FeelTypedVariableMapper(ExpressionFactory expressionFactory, VariableContext variableContext) {
    this.expressionFactory = expressionFactory;
    this.variableContext = variableContext;
  }

    /**
   * Resolves the given variable by checking if it exists in the variable context. 
   * If the variable exists, unpacks its value and creates a ValueExpression with the value. 
   * If the variable does not exist, throws an exception with a message indicating unknown variable.
   * 
   * @param variable the variable to resolve
   * @return the resolved ValueExpression
   */
  public ValueExpression resolveVariable(String variable) {
    if (variableContext.containsVariable(variable)) {
      Object value = unpackVariable(variable);
      return expressionFactory.createValueExpression(value, Object.class);
    }
    else {
      throw LOG.unknownVariable(variable);
    }
  }

    /**
   * Sets a variable with the given expression. This method is read-only and will throw an exception.
   *
   * @param variable the name of the variable to set
   * @param expression the value expression to associate with the variable
   * @return ValueExpression the value expression associated with the variable
   * @throws VariableMapperException if the variable mapper is read-only
   */
  public ValueExpression setVariable(String variable, ValueExpression expression) {
    throw LOG.variableMapperIsReadOnly();
  }

    /**
   * Unpacks the value of a variable from the variable context.
   * 
   * @param variable the name of the variable to unpack
   * @return the value of the variable, or null if the variable is not found
   */
  public Object unpackVariable(String variable) {
    TypedValue valueTyped = variableContext.resolve(variable);
    if(valueTyped != null) {
      return valueTyped.getValue();
    }
    return null;
  }

}
