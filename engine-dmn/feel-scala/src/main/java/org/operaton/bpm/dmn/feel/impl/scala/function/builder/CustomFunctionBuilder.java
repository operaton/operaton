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
package org.operaton.bpm.dmn.feel.impl.scala.function.builder;

import java.util.List;
import java.util.function.Function;

import org.operaton.bpm.dmn.feel.impl.FeelException;
import org.operaton.bpm.dmn.feel.impl.scala.function.CustomFunction;
import org.operaton.bpm.dmn.feel.impl.scala.function.FeelCustomFunctionProvider;

/**
 * Fluent builder to create a {@link CustomFunction}.
 */
public interface CustomFunctionBuilder {

  /**
   * Define the parameters of the custom function.
   *
   * @param params of the custom function
   * @return the builder
   */
  CustomFunctionBuilder setParams(String... params);

  /**
   * Enable variable arguments
   *
   * @return the builder
   */
  CustomFunctionBuilder enableVarargs();

  /**
   * Define a custom function that only returns a value and
   * has no further business logic (method body).
   *
   * <p>
   * It is not possible to use this method together with
   * {@link #setFunction}.
   * </p>
   *
   * @param result that should be returned by the custom function
   * @return the builder
   */
  CustomFunctionBuilder setReturnValue(Object result);

  /**
   * Pass a {@link Function} with a {@link List} of objects as argument
   * and an object as return value.
   *
   * <p>
   * It is not possible to use this method together with
   * {@link #setReturnValue}.
   * </p>
   *
   * @param function to be called
   * @return the builder
   */
  CustomFunctionBuilder setFunction(Function<List<Object>, Object> function);

  /**
   * Returns the custom function to be registered in
   * {@link FeelCustomFunctionProvider}.
   *
   * @throws FeelException when both {@link #setFunction} and {@link #setReturnValue} were called
   *
   * @return a custom function
   */
  CustomFunction build();

}
