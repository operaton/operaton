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
package org.operaton.bpm.dmn.engine.api;

import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.engine.variable.Variables;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class ReturnBlankTableOutputAsNullTest extends DmnEngineTest {

  public static final String RESULT_TEST_DMN = "ReturnBlankTableOutputAsNull.dmn";

    /**
   * Configures the DMN engine to return blank table output as null.
   */
  @Before
  public void configure() {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) dmnEngine.getConfiguration();
    configuration.setReturnBlankTableOutputAsNull(true);
  }

    /**
   * Resets the default DMN engine configuration by setting the flag to return blank table output as null to false.
   */
  @After
  public void reset() {
    DefaultDmnEngineConfiguration configuration = (DefaultDmnEngineConfiguration) dmnEngine.getConfiguration();
    configuration.setReturnBlankTableOutputAsNull(false);
  }

    /**
   * Test method to verify that null is returned when the expression is null.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void shouldReturnNullWhenExpressionIsNull() {
    // given

    // when
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, Variables.putValue("name", "A"));

    // then
    assertThat(decisionResult).hasSize(1);
    assertThat(decisionResult.getSingleResult().getEntryMap())
      .containsOnly(entry("output", null));
  }

    /**
   * Test method to verify that the decision engine returns null when the text tag is empty.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void shouldReturnNullWhenTextTagEmpty() {
    // given

    // when
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, Variables.putValue("name", "B"));

    // then
    assertThat(decisionResult).hasSize(1);
    assertThat(decisionResult.getSingleResult().getEntryMap())
      .containsOnly(entry("output", null));
  }

    /**
   * This method tests that the decision engine returns an empty result when evaluating a decision with a specific input value.
   */
  @Test
    @DecisionResource(resource = RESULT_TEST_DMN)
    public void shouldReturnEmpty() {
      // given
  
      // when
      DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, Variables.putValue("name", "C"));
  
      // then
      assertThat(decisionResult).hasSize(1);
      assertThat(decisionResult.getSingleResult().getEntryMap())
        .containsOnly(entry("output", ""));
    }

    /**
   * Test method to verify that the method returns null when the output entry is empty.
   */
  @Test
  @DecisionResource(resource = RESULT_TEST_DMN)
  public void shouldReturnNullWhenOutputEntryEmpty() {
    // given

    // when
    DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, Variables.putValue("name", "D"));

    // then
    assertThat(decisionResult).hasSize(1);
    assertThat(decisionResult.getSingleResult().getEntryMap())
      .containsOnly(entry("output", null));
  }

}
