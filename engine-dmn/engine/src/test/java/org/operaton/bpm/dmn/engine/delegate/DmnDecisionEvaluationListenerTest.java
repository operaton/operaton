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
package org.operaton.bpm.dmn.engine.delegate;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.commons.utils.IoUtil;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */

public class DmnDecisionEvaluationListenerTest extends DmnEngineTest {

  public static final String DMN_FILE = "org/operaton/bpm/dmn/engine/delegate/DrdDishDecisionExampleWithInsufficientRules.dmn";

  public TestDecisionEvaluationListener listener;

    /**
   * Returns a new instance of TestDecisionEvaluationListenerConfiguration as the DmnEngineConfiguration.
   *
   * @return a new TestDecisionEvaluationListenerConfiguration instance
   */
  @Override
  public DmnEngineConfiguration getDmnEngineConfiguration() {
    return new TestDecisionEvaluationListenerConfiguration();
  }

    /**
   * Initializes the decision evaluation listener by retrieving the configuration from the DMN engine and assigning the test decision listener from the configuration to the class variable 'listener'.
   */
  @Before
  public void initListener() {
    TestDecisionEvaluationListenerConfiguration configuration = (TestDecisionEvaluationListenerConfiguration) dmnEngine.getConfiguration();
    listener = configuration.testDecisionListener;
  }

    /**
   * Executes a decision and verifies that the listener's evaluation event is not null.
   */
  @Test
  @DecisionResource(resource = DMN_FILE)
  public void shouldCallListener() {
    evaluateDecision(20, "Weekend", IoUtil.fileAsStream(DMN_FILE));
    assertThat(listener.getEvaluationEvent()).isNotNull();
  }

    /**
   * This method tests the execution of decision elements by evaluating a DMN decision model with specific inputs and asserts the expected results.
   */
  @Test
  @DecisionResource(resource = DMN_FILE)
  public void shouldGetExecutedDecisionElements() {
    evaluateDecision(35, "Weekend",IoUtil.fileAsStream(DMN_FILE));

    DmnDecisionEvaluationEvent evaluationEvent = listener.getEvaluationEvent();
    assertThat(evaluationEvent).isNotNull();
    assertThat(evaluationEvent.getExecutedDecisionElements()).isEqualTo(24L);

    DmnDecisionLogicEvaluationEvent dmnDecisionTableEvaluationEvent = getDmnDecisionTable(evaluationEvent.getRequiredDecisionResults(),"Season");
    assertThat(dmnDecisionTableEvaluationEvent).isNotNull();
    assertThat(dmnDecisionTableEvaluationEvent.getExecutedDecisionElements()).isEqualTo(6L);

    dmnDecisionTableEvaluationEvent = getDmnDecisionTable(evaluationEvent.getRequiredDecisionResults(),"GuestCount");
    assertThat(dmnDecisionTableEvaluationEvent).isNotNull();
    assertThat(dmnDecisionTableEvaluationEvent.getExecutedDecisionElements()).isEqualTo(6L);

  }

    /**
   * Verifies the root decision result after evaluating a DMN decision table with specific inputs.
   */
  @Test
  @DecisionResource(resource = DMN_FILE)
  public void shouldVerifyRootDecisionResult() {
    evaluateDecision(35, "Weekend", IoUtil.fileAsStream(DMN_FILE));

    assertThat(listener.getEvaluationEvent()).isNotNull();
    DmnDecisionTableEvaluationEvent decisionResult = (DmnDecisionTableEvaluationEvent) listener.getEvaluationEvent().getDecisionResult();
    assertThat(decisionResult).isNotNull();
    assertThat(decisionResult.getDecision().getKey()).isEqualTo("Dish");

    List<DmnEvaluatedInput> inputs = decisionResult.getInputs();
    assertThat(inputs.size()).isEqualTo(2);
    assertThat(inputs.get(0).getName()).isEqualTo("Season");
    assertThat(inputs.get(0).getValue().getValue()).isEqualTo("Summer");
    assertThat(inputs.get(1).getName()).isEqualTo("How many guests");
    assertThat(inputs.get(1).getValue().getValue()).isEqualTo(15);

    assertThat(decisionResult.getMatchingRules().size()).isEqualTo(1);
    Map<String, DmnEvaluatedOutput> outputEntries = decisionResult.getMatchingRules().get(0).getOutputEntries();
    assertThat(outputEntries.size()).isEqualTo(1);
    assertThat(outputEntries.containsKey("desiredDish")).isTrue();
    assertThat(outputEntries.get("desiredDish").getValue().getValue()).isEqualTo("Light salad");
    assertThat(decisionResult.getExecutedDecisionElements()).isEqualTo(12L);

  }

    /**
   * Test method to verify the root decision result when there is no matching output.
   */
  @Test
  @DecisionResource(resource = DMN_FILE)
  public void shouldVerifyRootDecisionResultWithNoMatchingOutput() {
    evaluateDecision(20, "Weekend", IoUtil.fileAsStream(DMN_FILE));

    assertThat(listener.getEvaluationEvent()).isNotNull();
    DmnDecisionTableEvaluationEvent decisionResult = (DmnDecisionTableEvaluationEvent) listener.getEvaluationEvent().getDecisionResult();
    assertThat(decisionResult).isNotNull();
    assertThat(decisionResult.getDecisionTable().getKey()).isEqualTo("Dish");
    assertThat(decisionResult.getMatchingRules().size()).isEqualTo(0);
    assertThat(decisionResult.getExecutedDecisionElements()).isEqualTo(12L);

  }

    /**
   * This method verifies the results of required decisions by evaluating a DMN file with given inputs and asserting the expected outcomes.
   */
  @Test
  @DecisionResource(resource = DMN_FILE)
  public void shouldVerifyRequiredDecisionResults() {
    evaluateDecision(35, "Weekend",IoUtil.fileAsStream(DMN_FILE));

    assertThat(listener.getEvaluationEvent()).isNotNull();
    Collection<DmnDecisionLogicEvaluationEvent> requiredDecisions = listener.getEvaluationEvent().getRequiredDecisionResults();
    assertThat(requiredDecisions.size()).isEqualTo(2);

    DmnDecisionTableEvaluationEvent dmnDecisionTableEvaluationEvent = getDmnDecisionTable(requiredDecisions,"Season");
    assertThat(dmnDecisionTableEvaluationEvent).isNotNull();
    List<DmnEvaluatedInput> inputs = dmnDecisionTableEvaluationEvent.getInputs();
    assertThat(inputs.size()).isEqualTo(1);
    assertThat(inputs.get(0).getName()).isEqualTo("Weather in Celsius");
    assertThat(inputs.get(0).getValue().getValue()).isEqualTo(35);
    List<DmnEvaluatedDecisionRule> matchingRules = dmnDecisionTableEvaluationEvent.getMatchingRules();
    assertThat(matchingRules.size()).isEqualTo(1);
    assertThat(matchingRules.get(0).getOutputEntries().get("season").getValue().getValue()).isEqualTo("Summer");

    dmnDecisionTableEvaluationEvent = getDmnDecisionTable(requiredDecisions,"GuestCount");
    assertThat(dmnDecisionTableEvaluationEvent).isNotNull();
    inputs = dmnDecisionTableEvaluationEvent.getInputs();
    assertThat(inputs.size()).isEqualTo(1);
    assertThat(inputs.get(0).getName()).isEqualTo("Type of day");
    assertThat(inputs.get(0).getValue().getValue()).isEqualTo("Weekend");
    matchingRules = dmnDecisionTableEvaluationEvent.getMatchingRules();
    assertThat(matchingRules.size()).isEqualTo(1);
    assertThat(matchingRules.get(0).getOutputEntries().get("guestCount").getValue().getValue()).isEqualTo(15);

  }

  // helper
    /**
   * Retrieves the DmnDecisionTableEvaluationEvent from a collection of required decision events based on the key.
   *
   * @param requiredDecisionEvents the collection of required decision events
   * @param key the key to search for
   * @return the DmnDecisionTableEvaluationEvent with the specified key, or null if not found
   */
  protected DmnDecisionTableEvaluationEvent getDmnDecisionTable(Collection<DmnDecisionLogicEvaluationEvent> requiredDecisionEvents, String key) {
    for(DmnDecisionLogicEvaluationEvent event : requiredDecisionEvents) {
      if(event.getDecision().getKey().equals(key)) {
        return (DmnDecisionTableEvaluationEvent) event;
      }
    }
    return null;
  }

    /**
   * Evaluates a decision table based on the provided inputs and input stream.
   * 
   * @param input1 the input for temperature
   * @param input2 the input for day type
   * @param inputStream the input stream for the decision table
   * @return the result of evaluating the decision table
   */
  protected DmnDecisionTableResult evaluateDecision(Object input1, Object input2, InputStream inputStream) {
    variables.put("temperature", input1);
    variables.put("dayType", input2);
    return dmnEngine.evaluateDecisionTable("Dish", inputStream, variables);
  }

  public static class TestDecisionEvaluationListenerConfiguration extends DefaultDmnEngineConfiguration {

    public TestDecisionEvaluationListener testDecisionListener = new TestDecisionEvaluationListener();

    public TestDecisionEvaluationListenerConfiguration() {
      customPostDecisionEvaluationListeners.add(testDecisionListener);
    }

  }

  public static class TestDecisionEvaluationListener implements DmnDecisionEvaluationListener {

    public DmnDecisionEvaluationEvent evaluationEvent;

        /**
     * Sets the provided DmnDecisionEvaluationEvent as the evaluationEvent attribute of the class.
     * 
     * @param evaluationEvent the DmnDecisionEvaluationEvent to be set
     */
    public void notify(DmnDecisionEvaluationEvent evaluationEvent) {
          this.evaluationEvent = evaluationEvent;
        }

        /**
     * Returns the evaluation event associated with the decision.
     *
     * @return the evaluation event
     */
    public DmnDecisionEvaluationEvent getEvaluationEvent() {
      return evaluationEvent;
    }
  }

}
