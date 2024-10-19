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

import java.lang.reflect.Method;

import org.operaton.bpm.impl.juel.jakarta.el.ELContext;
import org.operaton.bpm.impl.juel.jakarta.el.ELResolver;
import org.operaton.bpm.impl.juel.jakarta.el.ExpressionFactory;
import org.operaton.bpm.impl.juel.jakarta.el.FunctionMapper;
import org.operaton.bpm.impl.juel.jakarta.el.VariableMapper;

import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineLogger;
import org.operaton.bpm.dmn.feel.impl.juel.FeelLogger;
import org.operaton.bpm.engine.variable.context.VariableContext;

import org.operaton.bpm.impl.juel.SimpleResolver;

public class FeelElContextFactory implements ElContextFactory {

  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;

  protected CustomFunctionMapper customFunctionMapper = new CustomFunctionMapper();

    /**
   * Creates a new ELContext with the specified ExpressionFactory and VariableContext.
   * Initializes ELResolver, FunctionMapper, and VariableMapper and returns a new FeelElContext.
   * 
   * @param expressionFactory the ExpressionFactory to be used in the ELContext
   * @param variableContext the VariableContext to be used in the ELContext
   * @return a new ELContext with the initialized components
   */
  public ELContext createContext(ExpressionFactory expressionFactory, VariableContext variableContext) {
    ELResolver elResolver = createElResolver();
    FunctionMapper functionMapper = createFunctionMapper();
    VariableMapper variableMapper = createVariableMapper(expressionFactory, variableContext);

    return new FeelElContext(elResolver, functionMapper, variableMapper);
  }

    /**
   * Creates and returns a new ELResolver using the SimpleResolver class with auto resetting enabled.
   *
   * @return a new ELResolver with auto resetting enabled
   */
  public ELResolver createElResolver() {
    return new SimpleResolver(true);
  }

    /**
   * Creates a FunctionMapper object by creating a CompositeFunctionMapper,
   * adding a FeelFunctionMapper and a customFunctionMapper, and returning the CompositeFunctionMapper.
   * 
   * @return a FunctionMapper object with added function mappers
   */
  public FunctionMapper createFunctionMapper() {
    CompositeFunctionMapper functionMapper = new CompositeFunctionMapper();
    functionMapper.add(new FeelFunctionMapper());
    functionMapper.add(customFunctionMapper);
    return functionMapper;
  }

    /**
   * Creates a new VariableMapper using the provided ExpressionFactory and VariableContext.
   *
   * @param expressionFactory the ExpressionFactory to be used
   * @param variableContext the VariableContext to be used
   * @return a new FeelTypedVariableMapper object
   */
  public VariableMapper createVariableMapper(ExpressionFactory expressionFactory, VariableContext variableContext) {
    return new FeelTypedVariableMapper(expressionFactory, variableContext);
  }

    /**
   * Adds a custom function to the mapper with the specified name and method.
   * 
   * @param name the name of the custom function
   * @param method the method representing the custom function
   */
  public void addCustomFunction(String name, Method method) {
    customFunctionMapper.addMethod(name, method);
  }

}
