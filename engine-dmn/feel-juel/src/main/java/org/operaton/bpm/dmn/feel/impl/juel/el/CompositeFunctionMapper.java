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
import java.util.ArrayList;
import java.util.List;
import org.operaton.bpm.impl.juel.jakarta.el.FunctionMapper;

import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineLogger;
import org.operaton.bpm.dmn.feel.impl.juel.FeelLogger;

public class CompositeFunctionMapper extends FunctionMapper {

  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;

  protected List<FunctionMapper> functionMappers = new ArrayList<>();

    /**
   * Resolves a function by searching through a list of FunctionMappers for the given prefix and local name.
   * 
   * @param prefix the prefix of the function
   * @param localName the local name of the function
   * @return the Method object representing the resolved function
   * @throws UnknownFunctionException if the function cannot be resolved
   */
  public Method resolveFunction(String prefix, String localName) {
    for (FunctionMapper functionMapper : functionMappers) {
      Method method = functionMapper.resolveFunction(prefix, localName);
      if (method != null) {
        return method;
      }
    }
    throw LOG.unknownFunction(prefix, localName);
  }

    /**
   * Sets the list of function mappers for this object.
   * 
   * @param functionMappers the list of FunctionMapper objects to set
   */
  public void setFunctionMappers(List<FunctionMapper> functionMappers) {
    this.functionMappers = functionMappers;
  }

    /**
   * Adds a FunctionMapper to the list of function mappers.
   * 
   * @param functionMapper the FunctionMapper to add
   */
  public void add(FunctionMapper functionMapper) {
    functionMappers.add(functionMapper);
  }

    /**
   * Removes the specified FunctionMapper from the list of function mappers.
   *
   * @param functionMapper the FunctionMapper to be removed
   */
  public void remove(FunctionMapper functionMapper) {
    functionMappers.remove(functionMapper);
  }

}
