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
package org.operaton.bpm.dmn.feel.impl.scala.function.builder;

import org.operaton.bpm.dmn.feel.impl.scala.ScalaFeelLogger;
import org.operaton.bpm.dmn.feel.impl.scala.function.CustomFunction;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class CustomFunctionBuilderImpl implements CustomFunctionBuilder {

  protected static final ScalaFeelLogger LOGGER = ScalaFeelLogger.LOGGER;

  protected CustomFunction customFunction;
  protected int functionCount;

  public CustomFunctionBuilderImpl() {
    customFunction = new CustomFunction();
    functionCount = 0;
  }

    /**
   * Sets the parameters for the custom function builder.
   * 
   * @param params the parameters to be set
   * @return the custom function builder with the parameters set
   */
  @Override
  public CustomFunctionBuilder setParams(String... params) {
    List<String> paramList = Arrays.asList(params);
    customFunction.setParams(paramList);
    return this;
  }

    /**
   * Enables varargs for the custom function builder.
   *
   * @return the custom function builder with varargs enabled
   */
  @Override
  public CustomFunctionBuilder enableVarargs() {
    customFunction.setHasVarargs(true);
    return this;
  }

    /**
   * Increments the function count, sets the return value of a custom function, and returns the CustomFunctionBuilder object.
   * 
   * @param returnValue the return value to set for the custom function
   * @return the CustomFunctionBuilder object with the return value set
   */
  @Override
  public CustomFunctionBuilder setReturnValue(Object returnValue) {
    functionCount++;
    customFunction.setFunction((args) -> returnValue);
    return this;
  }

    /**
   * Sets the custom function and increments the function count.
   * 
   * @param function the function to set
   * @return the CustomFunctionBuilder instance
   */
  @Override
  public CustomFunctionBuilder setFunction(Function<List<Object>, Object> function) {
    functionCount++;
    customFunction.setFunction(function);
    return this;
  }

    /**
  * Builds and returns a custom function after checking if it has been set.
  *
  * @return the built custom function
  */
  @Override
  public CustomFunction build() {
    checkHasFunction();
    return customFunction;
  }

    /**
   * Checks if the function count exceeds 1 and throws an exception if it does.
   */
  protected void checkHasFunction() {
    if (functionCount > 1) {
      throw LOGGER.functionCountExceededException();
    }
  }

}
