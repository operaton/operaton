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
package org.operaton.bpm.dmn.engine.feel.helper;

import org.operaton.bpm.dmn.engine.feel.function.helper.FunctionProvider;
import org.operaton.bpm.dmn.feel.impl.scala.ScalaFeelEngine;
import org.operaton.bpm.dmn.feel.impl.scala.function.FeelCustomFunctionProvider;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.Collections;
import java.util.List;

public class FeelRule extends TestWatcher {

  protected FunctionProvider functionProvider;
  protected ScalaFeelEngine feelEngine;

  protected FeelRule(FunctionProvider functionProvider) {
    this.functionProvider = functionProvider;
  }

  protected FeelRule() {
    feelEngine = new ScalaFeelEngine(null);
  }

    /**
   * Constructs a FeelRule object with a new FunctionProvider.
   *
   * @return a new FeelRule object with the initialized FunctionProvider
   */
  public static FeelRule buildWithFunctionProvider() {
    FunctionProvider functionProvider = new FunctionProvider();
    return new FeelRule(functionProvider);
  }

    /**
   * Constructs and returns a new instance of FeelRule.
   *
   * @return a new FeelRule instance
   */
  public static FeelRule build() {
    return new FeelRule();
  }

    /**
   * Called when a test has finished running. Clears the function provider if it is not null.
   *
   * @param description the description of the test that has finished
   */
  @Override
  protected void finished(Description description) {
    super.finished(description);

    if (functionProvider != null) {
      functionProvider.clear();
    }
  }

    /**
   * Evaluates the given expression with the default parameters.
   * 
   * @param expression the expression to be evaluated
   * @return the result of evaluating the expression
   */
  public <T> T evaluateExpression(String expression) {
    return evaluateExpression(expression, null);
  }

    /**
   * Evaluates a given expression with the provided value using the FEEL engine.
   *
   * @param expression the expression to evaluate
   * @param value the value to evaluate the expression with
   * @return the result of evaluating the expression with the provided value
   */
  public <T> T evaluateExpression(String expression, Object value) {
    if (functionProvider != null) {
      List<FeelCustomFunctionProvider> functionProviders =
        Collections.singletonList(functionProvider);

      feelEngine = new ScalaFeelEngine(functionProviders);
    }

    /**
   * Returns the FunctionProvider object.
   *
   * @return the FunctionProvider object
   */
  public FunctionProvider getFunctionProvider() {
    return functionProvider;
  }

}
