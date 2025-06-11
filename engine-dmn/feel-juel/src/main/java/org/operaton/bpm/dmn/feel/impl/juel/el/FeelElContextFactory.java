/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.dmn.feel.impl.juel.el;

import jakarta.el.*;
import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineLogger;
import org.operaton.bpm.dmn.feel.impl.juel.FeelLogger;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.impl.juel.SimpleResolver;

import java.lang.reflect.Method;

public class FeelElContextFactory implements ElContextFactory {

  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;

  protected CustomFunctionMapper customFunctionMapper = new CustomFunctionMapper();

  @Override
  public ELContext createContext(ExpressionFactory expressionFactory, VariableContext variableContext) {
    ELResolver elResolver = createElResolver();
    FunctionMapper functionMapper = createFunctionMapper();
    VariableMapper variableMapper = createVariableMapper(expressionFactory, variableContext);

    return new FeelElContext(elResolver, functionMapper, variableMapper);
  }

  public ELResolver createElResolver() {
    return new SimpleResolver(true);
  }

  public FunctionMapper createFunctionMapper() {
    CompositeFunctionMapper functionMapper = new CompositeFunctionMapper();
    functionMapper.add(new FeelFunctionMapper());
    functionMapper.add(customFunctionMapper);
    return functionMapper;
  }

  public VariableMapper createVariableMapper(ExpressionFactory expressionFactory, VariableContext variableContext) {
    return new FeelTypedVariableMapper(expressionFactory, variableContext);
  }

  @Override
  public void addCustomFunction(String name, Method method) {
    customFunctionMapper.addMethod(name, method);
  }

}
