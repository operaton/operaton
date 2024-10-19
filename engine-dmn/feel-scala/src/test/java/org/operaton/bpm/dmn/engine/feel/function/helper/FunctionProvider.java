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
package org.operaton.bpm.dmn.engine.feel.function.helper;

import org.operaton.bpm.dmn.feel.impl.scala.function.CustomFunction;
import org.operaton.bpm.dmn.feel.impl.scala.function.FeelCustomFunctionProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FunctionProvider implements FeelCustomFunctionProvider {

  protected Map<String, CustomFunction> functions = new HashMap<>();

  public FunctionProvider() {
  }

    /**
   * Retrieves and returns the CustomFunction associated with the given function name from the functions map, if present.
   * 
   * @param functionName the name of the function to resolve
   * @return an Optional containing the CustomFunction if found, otherwise an empty Optional
   */
  @Override
  public Optional<CustomFunction> resolveFunction(String functionName) {
    return Optional.ofNullable(functions.get(functionName));
  }

    /**
   * Returns a collection of function names.
   *
   * @return the collection of function names
   */
  @Override
  public Collection<String> getFunctionNames() {
    return functions.keySet();
  }

    /**
   * Clears all elements from the functions list.
   */
  public void clear() {
    functions.clear();
  }

    /**
   * Registers a custom function with the given name.
   * 
   * @param functionName the name of the function
   * @param function the CustomFunction object to register
   */
  public void register(String functionName, CustomFunction function) {
    functions.put(functionName, function);
  }

}
