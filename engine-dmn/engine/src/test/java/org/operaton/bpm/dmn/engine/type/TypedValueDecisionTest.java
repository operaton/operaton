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
package org.operaton.bpm.dmn.engine.type;

import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.junit.Test;

/**
 * @author Philipp Ossler
 */
public class TypedValueDecisionTest extends DmnEngineTest {

  public static final String DMN_FILE = "org/operaton/bpm/dmn/engine/type/TypedValue.dmn";

    /**
   * Sets variables for a decision with untyped value and asserts that the decision table result has a single entry with a value of true.
   */
  @Test
  @DecisionResource(resource = DMN_FILE)
  public void decisionWithUntypedValueSatisfied() {
    variables.put("type", "untyped");
    variables.put("integer", 84);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(true);
  }

    /**
   * This method tests a decision table with untyped values to ensure that the result is not satisfied.
   */
  @Test
  @DecisionResource(resource = DMN_FILE)
  public void decisionWithUntypedValueNotSatisfied() {
    variables.put("type", "untyped");
    variables.put("integer", 21);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(false);
  }

    /**
   * Sets the variables "type" to "typed" and "integer" to 73, then asserts that the decision table result has a single entry with a boolean value of true.
   */
  @Test
  @DecisionResource(resource = DMN_FILE)
  public void decisionWithTypedValueSatisfied() {
    variables.put("type", "typed");
    variables.put("integer", 73);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(true);
  }

    /**
   * Test method to verify the decision with a typed value that is not satisfied.
   */
  @Test
  @DecisionResource(resource = DMN_FILE)
  public void decisionWithTypedValueNotSatisfied() {
    variables.put("type", "typed");
    variables.put("integer", 41);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(false);
  }

}
