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
package org.operaton.bpm.dmn.feel.impl.scala.function;

import org.operaton.bpm.dmn.feel.impl.scala.function.builder.CustomFunctionBuilder;
import org.operaton.bpm.dmn.feel.impl.scala.function.builder.CustomFunctionBuilderImpl;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class CustomFunction {

  protected List<String> params;
  protected Function<List<Object>, Object> function;
  protected boolean hasVarargs;

  public CustomFunction() {
    params = Collections.emptyList();
  }

  /**
   * Creates a fluent builder to configure a custom function
   *
   * @return builder to apply configurations on
   */
  public static CustomFunctionBuilder create() {
    return new CustomFunctionBuilderImpl();
  }

    /**
   * Returns the list of parameters.
   *
   * @return the list of parameters
   */
  public List<String> getParams() {
    return params;
  }

    /**
   * Sets the list of parameters for the object.
   *
   * @param params the list of parameters to be set
   */
  public void setParams(List<String> params) {
    this.params = params;
  }

    /**
   * Returns the function used by this object.
   *
   * @return the function
   */
  public Function<List<Object>, Object> getFunction() {
    return function;
  }

    /**
   * Sets the function to be used by the method.
   * 
   * @param function the function to be set
   */
  public void setFunction(Function<List<Object>, Object> function) {
    this.function = function;
  }

    /**
   * Returns a boolean indicating if the method has varargs.
   *
   * @return true if the method has varargs, false otherwise
   */
  public boolean hasVarargs() {
    return hasVarargs;
  }

    /**
   * Sets whether the method has varargs or not.
   * 
   * @param hasVarargs true if the method has varargs, false otherwise
   */
  public void setHasVarargs(boolean hasVarargs) {
    this.hasVarargs = hasVarargs;
  }

}
