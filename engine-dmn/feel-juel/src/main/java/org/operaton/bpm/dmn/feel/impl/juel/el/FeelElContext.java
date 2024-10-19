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

import org.operaton.bpm.impl.juel.jakarta.el.ELContext;
import org.operaton.bpm.impl.juel.jakarta.el.ELResolver;
import org.operaton.bpm.impl.juel.jakarta.el.FunctionMapper;
import org.operaton.bpm.impl.juel.jakarta.el.VariableMapper;

public class FeelElContext extends ELContext {

  protected ELResolver elResolver;
  protected FunctionMapper functionMapper;
  protected VariableMapper variableMapper;

  public FeelElContext(ELResolver elResolver, FunctionMapper functionMapper, VariableMapper variableMapper) {
    this.elResolver = elResolver;
    this.functionMapper = functionMapper;
    this.variableMapper = variableMapper;
  }

    /**
   * Returns the ELResolver associated with this object.
   *
   * @return the ELResolver
   */
  public ELResolver getELResolver() {
    return elResolver;
  }

    /**
   * Returns the function mapper for this object.
   *
   * @return the function mapper
   */
  public FunctionMapper getFunctionMapper() {
    return functionMapper;
  }

    /**
   * Returns the VariableMapper associated with this object.
   *
   * @return the VariableMapper
   */
  public VariableMapper getVariableMapper() {
    return variableMapper;
  }

}
