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
import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.impl.juel.jakarta.el.FunctionMapper;

import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineLogger;
import org.operaton.bpm.dmn.feel.impl.juel.FeelLogger;

public class CustomFunctionMapper extends FunctionMapper {

  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;

  protected final Map<String, Method> methods = new HashMap<String, Method>();

    /**
   * Resolves a function with the given prefix and local name.
   * 
   * @param prefix the prefix of the function
   * @param localName the local name of the function
   * @return the resolved function method
   */
  @Override
  public Method resolveFunction(String prefix, String localName) {
    return methods.get(localName);
  }

    /**
   * Adds a method to the collection with the given name.
   * 
   * @param name the name of the method
   * @param method the method to add
   */
  public void addMethod(final String name, final Method method) {
    methods.put(name, method);
  }

}
