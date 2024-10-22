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
package org.operaton.bpm.dmn.feel.impl.scala;

import org.operaton.bpm.dmn.feel.impl.FeelEngine;
import org.operaton.bpm.dmn.feel.impl.scala.function.CustomFunctionTransformer;
import org.operaton.bpm.dmn.feel.impl.scala.function.FeelCustomFunctionProvider;
import org.operaton.bpm.dmn.feel.impl.scala.spin.SpinValueMapperFactory;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.camunda.feel.FeelEngine$;
import org.camunda.feel.FeelEngine.Builder;
import org.camunda.feel.FeelEngine.Failure;
import org.camunda.feel.context.CustomContext;
import org.camunda.feel.context.VariableProvider;
import org.camunda.feel.context.VariableProvider.StaticVariableProvider;
import org.camunda.feel.impl.JavaValueMapper;
import org.camunda.feel.valuemapper.CustomValueMapper;
import org.camunda.feel.valuemapper.ValueMapper.CompositeValueMapper;
import camundajar.impl.scala.collection.immutable.List;
import camundajar.impl.scala.collection.immutable.Map;
import camundajar.impl.scala.runtime.BoxesRunTime;
import camundajar.impl.scala.util.Either;
import camundajar.impl.scala.util.Left;
import camundajar.impl.scala.util.Right;

import java.util.Arrays;

import static org.camunda.feel.context.VariableProvider.CompositeVariableProvider;
import static camundajar.impl.scala.jdk.CollectionConverters.ListHasAsScala;

public class ScalaFeelEngine implements FeelEngine {

  protected static final String INPUT_VARIABLE_NAME = "inputVariableName";

  protected static final ScalaFeelLogger LOGGER = ScalaFeelLogger.LOGGER;

  protected org.camunda.feel.FeelEngine feelEngine;

  public ScalaFeelEngine(java.util.List<FeelCustomFunctionProvider> functionProviders) {
    List<CustomValueMapper> valueMappers = getValueMappers();

    CompositeValueMapper compositeValueMapper = new CompositeValueMapper(valueMappers);

    CustomFunctionTransformer customFunctionTransformer =
      new CustomFunctionTransformer(functionProviders, compositeValueMapper);

    feelEngine = buildFeelEngine(customFunctionTransformer, compositeValueMapper);
  }

    /**
   * Evaluates a simple expression using the provided expression string and variable context.
   * Returns the result of the evaluation as type T.
   * If the evaluation fails, an EvaluationException is thrown with the error message.
   *
   * @param expression the expression to evaluate
   * @param variableContext the context containing variables for the expression evaluation
   * @param <T> the type of the evaluation result
   * @return the result of the evaluation as type T
   * @throws EvaluationException if the evaluation fails
   */
  public <T> T evaluateSimpleExpression(String expression, VariableContext variableContext) {

    CustomContext context = new CustomContext() {
      public VariableProvider variableProvider() {
        return new ContextVariableWrapper(variableContext);
      }
    };

    Either either = feelEngine.evalExpression(expression, context);

    if (either instanceof Right) {
      Right right = (Right) either;

      return (T) right.value();

    } else {
      Left left = (Left) either;
      Failure failure = (Failure) left.value();
      String message = failure.message();

      throw LOGGER.evaluationException(message);

    }
  }

    /**
   * Evaluates a simple unary test expression with the given input variable and variable context.
   * 
   * @param expression the unary test expression to evaluate
   * @param inputVariable the input variable to use in the evaluation
   * @param variableContext the variable context to use in the evaluation
   * @return true if the evaluation result is true, false otherwise
   */
  public boolean evaluateSimpleUnaryTests(String expression,
                                          String inputVariable,
                                          VariableContext variableContext) {
    Map inputVariableMap = new Map.Map1(INPUT_VARIABLE_NAME, inputVariable);

    StaticVariableProvider inputVariableContext = new StaticVariableProvider(inputVariableMap);

    ContextVariableWrapper contextVariableWrapper = new ContextVariableWrapper(variableContext);

    CustomContext context = new CustomContext() {
      public VariableProvider variableProvider() {
        return new CompositeVariableProvider(toScalaList(inputVariableContext, contextVariableWrapper));
      }
    };

    Either either = feelEngine.evalUnaryTests(expression, context);

    if (either instanceof Right) {
      Right right = (Right) either;
      Object value = right.value();

      return BoxesRunTime.unboxToBoolean(value);

    } else {
      Left left = (Left) either;
      Failure failure = (Failure) left.value();
      String message = failure.message();

      throw LOGGER.evaluationException(message);

    }
  }

    /**
   * Retrieves a list of CustomValueMapper instances, including a JavaValueMapper and a SpinValueMapper if available.
   * 
   * @return List of CustomValueMapper instances
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
   * Converts the specified elements into a Scala List.
   * 
   * @param elements the elements to be converted
   * @return a Scala List containing the elements
   */
  @SafeVarargs
  protected final <T> List<T> toScalaList(T... elements) {
    java.util.List<T> listAsJava = Arrays.asList(elements);

    return toList(listAsJava);
  }

    /**
   * Converts a Java List to a Scala List and returns it
   * 
   * @param list the Java List to be converted
   * @return the converted Scala List
   */
  protected <T> List<T> toList(java.util.List list) {
    return ListHasAsScala(list).asScala().toList();
  }

    /**
   * Builds a FEEL (Friendly Enough Expression Language) engine with the provided custom function transformer
   * and composite value mapper.
   *
   * @param transformer the custom function transformer to be used by the engine
   * @param valueMapper the composite value mapper to be used by the engine
   * @return a FEEL engine with the specified transformer and value mapper
   */
  protected org.camunda.feel.FeelEngine buildFeelEngine(CustomFunctionTransformer transformer,
                                                        CompositeValueMapper valueMapper) {
    return new Builder()
      .functionProvider(transformer)
      .valueMapper(valueMapper)
      .enableExternalFunctions(false)
      .build();
  }

}

