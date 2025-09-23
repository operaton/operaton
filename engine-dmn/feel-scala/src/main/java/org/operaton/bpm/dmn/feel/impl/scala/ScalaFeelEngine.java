/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements.
 * Modifications Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.dmn.feel.impl.scala;

import java.util.Arrays;

import camundajar.impl.scala.collection.immutable.List;
import camundajar.impl.scala.runtime.BoxesRunTime;
import camundajar.impl.scala.util.Either;
import camundajar.impl.scala.util.Left;
import camundajar.impl.scala.util.Right;
import org.camunda.feel.FeelEngine.Failure;
import org.camunda.feel.api.FeelEngineApi;
import org.camunda.feel.api.FeelEngineBuilder;
import org.camunda.feel.context.CustomContext;
import org.camunda.feel.context.VariableProvider;
import org.camunda.feel.impl.JavaValueMapper;
import org.camunda.feel.valuemapper.CustomValueMapper;
import org.camunda.feel.valuemapper.ValueMapper.CompositeValueMapper;

import org.operaton.bpm.dmn.feel.impl.FeelEngine;
import org.operaton.bpm.dmn.feel.impl.scala.function.CustomFunctionTransformer;
import org.operaton.bpm.dmn.feel.impl.scala.function.FeelCustomFunctionProvider;
import org.operaton.bpm.dmn.feel.impl.scala.spin.SpinValueMapperFactory;
import org.operaton.bpm.engine.variable.context.VariableContext;

import static camundajar.impl.scala.jdk.CollectionConverters.ListHasAsScala;
import static org.camunda.feel.context.VariableProvider.CompositeVariableProvider;

/**
 * Implementation of the {@link FeelEngine} interface to evaluate FEEL expressions
 * using the Scala FEEL engine.
 *
 * <p>
 * This class provides functionalities for evaluating FEEL simple expressions and
 * unary tests while allowing integration with custom function providers and value mappers.
 * </p>
 */
public class ScalaFeelEngine implements FeelEngine {

  protected static final ScalaFeelLogger LOGGER = ScalaFeelLogger.LOGGER;

  /**
   * Provides the underlying API implementation for evaluating FEEL (Friendly
   * Enough Expression Language) expressions. This object plays a central role
   * within the ScalaFeelEngine class, enabling the evaluation of simple
   * expressions, unary tests, and the transformation of custom functions or
   * value mappings.
   *
   * <p>
   * It is initialized within the ScalaFeelEngine using a composite value mapper
   * and custom function transformer. The API encapsulates the internal logic for
   * processing FEEL expressions in compliance with DMN (Decision Model and
   * Notation) standards.
   * </p>
   *
   * <p>
   * This field is final, ensuring immutability and thread safety for
   * consistent evaluation behavior throughout the lifespan of the ScalaFeelEngine
   * instance.
   * </p>
   */
  protected final FeelEngineApi feelEngineApi;

  /**
   * Constructs an instance of the ScalaFeelEngine.
   *
   * @param functionProviders the list of custom function providers to be used
   *                          when transforming and resolving functions in the FEEL engine.
   *                          It allows the integration and registration of custom logic
   *                          that can be executed as part of FEEL expressions.
   */
  public ScalaFeelEngine(java.util.List<FeelCustomFunctionProvider> functionProviders) {
    List<CustomValueMapper> valueMappers = getValueMappers();

    CompositeValueMapper compositeValueMapper = new CompositeValueMapper(valueMappers);

    CustomFunctionTransformer customFunctionTransformer = new CustomFunctionTransformer(functionProviders,
      compositeValueMapper);

    feelEngineApi = buildFeelEngineApi(customFunctionTransformer, compositeValueMapper);
  }

  /**
   * Evaluates a simple FEEL (Friendly Enough Expression Language) expression
   * using the provided variable context for resolving variables at runtime.
   *
   * @param expression      the FEEL expression to evaluate as a String
   * @param variableContext the variable context used to resolve variables within the expression
   * @param <T>             the type of the result returned by the evaluation
   * @return the result of the FEEL expression evaluation, cast to the specified type
   * @throws org.operaton.bpm.dmn.feel.impl.FeelException if the expression evaluation fails
   */
  @Override
  public <T> T evaluateSimpleExpression(String expression, VariableContext variableContext) {

    CustomContext context = new CustomContext() {
      @Override
      public VariableProvider variableProvider() {
        return new ContextVariableWrapper(variableContext);
      }
    };

    Either<Failure, Object> either = feelEngineApi.evaluateExpression(expression, context).toEither();

    return handleEvaluationResult(either);
  }

  /**
   * Evaluates a FEEL (Friendly Enough Expression Language) unary test expression
   * against a given input variable using a variable context. This method is used
   * to determine whether the input satisfies the specified unary test conditions.
   *
   * @param expression      the unary test expression to evaluate
   * @param inputName       the name of the input variable to be resolved from the variable context
   * @param variableContext the context containing variables, used to resolve the input
   * @return true if the input variable satisfies the unary test conditions, false otherwise
   */
  @Override
  public boolean evaluateSimpleUnaryTests(String expression, String inputName, VariableContext variableContext) {
    // Resolve the input variable from the variable context
    Object inputVariable;
    if (inputName != null && variableContext != null && variableContext.containsVariable(inputName)) {
      inputVariable = variableContext.resolve(inputName).getValue();
    } else {
      inputVariable = null;
    }

    CustomContext context = new CustomContext() {
      @Override
      public VariableProvider variableProvider() {
        return new CompositeVariableProvider(toScalaList(new ContextVariableWrapper(variableContext)));
      }
    };

    // Evaluate the unary tests using the FeelEngineApi
    Either<Failure, Object> either = feelEngineApi.evaluateUnaryTests(expression, inputVariable, context).toEither();

    // Handle the evaluation result
    Object result = handleEvaluationResult(either);
    // Unbox the result to a boolean value. Null values are treated as false.
    return BoxesRunTime.unboxToBoolean(result);
  }

  /**
   * Handles the result of an evaluation by processing the provided `Either` value,
   * which can represent either a successful or failed evaluation.
   *
   * <p>
   * If the `Either` instance is a `Right`, it extracts and returns the value contained within it.
   * If the `Either` instance is a `Left`, it extracts the `Failure` object, retrieves its associated
   * message, and throws a custom `FeelException` with the given message.
   * </p>
   *
   * @param either a result of type `Either<Failure, Object>` that represents either
   *               a success (`Right`) or a failure (`Left`).
   * @param <T>    the type of the result value contained in the `Right` if the evaluation succeeds.
   * @return the result of the evaluation as type `T` if the evaluation is successful.
   * @throws org.operaton.bpm.dmn.feel.impl.FeelException if the evaluation fails and a `Left`
   *                                                      containing a `Failure` is provided.
   */
  private <T> T handleEvaluationResult(Either<Failure, Object> either) {
    if (either instanceof Right<Failure, Object> right) {
      //noinspection unchecked
      return (T) right.value();
    } else {
      Left<Failure, Object> left = (Left<Failure, Object>) either;
      Failure failure = left.value();
      String message = failure.message();

      throw LOGGER.evaluationException(message);
    }
  }

  /**
   * Retrieves a list of custom value mappers to be used for value transformation or processing.
   * The method creates a default Java value mapper and optionally includes a Spin value mapper
   * if it can be instantiated successfully. The list is then converted into a Scala-compatible list.
   *
   * @return a list of {@link CustomValueMapper}, including the default Java value mapper
   *         and, if available, the Spin value mapper.
   */
  protected List<CustomValueMapper> getValueMappers() {
    SpinValueMapperFactory spinValueMapperFactory = new SpinValueMapperFactory();

    CustomValueMapper javaValueMapper = new JavaValueMapper();

    CustomValueMapper spinValueMapper = spinValueMapperFactory.createInstance();
    if (spinValueMapper != null) {
      return toScalaList(javaValueMapper, spinValueMapper);

    } else {
      return toScalaList(javaValueMapper);

    }
  }

  /**
   * Converts a provided sequence of elements into a Scala-compatible `List`.
   * This method accepts a variable number of arguments and converts them
   * into a Scala `List`, ensuring interoperability between Java and Scala data types.
   *
   * @param <T>       the type of elements in the list
   * @param elements  a variable number of elements to be converted into a Scala list
   * @return a Scala-compatible `List` containing the given elements
   */
  @SafeVarargs
  protected final <T> List<T> toScalaList(T... elements) {
    java.util.List<T> listAsJava = Arrays.asList(elements);

    return toList(listAsJava);
  }

  /**
   * Converts a given Java List to a Scala List.
   *
   * @param <T>  the type of elements in the list
   * @param list the Java List to be converted
   * @return a Scala List containing the same elements as the provided Java List
   */
  protected <T> List<T> toList(java.util.List<T> list) {
    return ListHasAsScala(list).asScala().toList();
  }

  /**
   * Builds and initializes a {@link FeelEngineApi} instance with configured
   * custom function transformers and value mappers. Disables external functions
   * during the FEEL engine configuration.
   *
   * @param transformer the {@link CustomFunctionTransformer} to provide custom
   *                     functions for the FEEL engine.
   * @param valueMapper  the {@link CompositeValueMapper} for transforming and
   *                     processing values during FEEL evaluation.
   * @return a fully configured {@link FeelEngineApi} instance.
   */
  protected FeelEngineApi buildFeelEngineApi(CustomFunctionTransformer transformer, CompositeValueMapper valueMapper) {
    return FeelEngineBuilder.forJava()
      .withFunctionProvider(transformer)
      .withValueMapper(valueMapper)
      .withEnabledExternalFunctions(false)
      .build();
  }

}

