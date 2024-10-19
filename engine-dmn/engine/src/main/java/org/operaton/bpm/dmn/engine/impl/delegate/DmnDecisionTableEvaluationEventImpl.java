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
package org.operaton.bpm.dmn.engine.impl.delegate;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedInput;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class DmnDecisionTableEvaluationEventImpl implements DmnDecisionTableEvaluationEvent {

  protected DmnDecision decision;
  protected List<DmnEvaluatedInput> inputs = new ArrayList<DmnEvaluatedInput>();
  protected List<DmnEvaluatedDecisionRule> matchingRules = new ArrayList<DmnEvaluatedDecisionRule>();
  protected String collectResultName;
  protected TypedValue collectResultValue;
  protected long executedDecisionElements;

    /**
   * Returns the decision table associated with this decision.
   * 
   * @return the decision table
   */
  public DmnDecision getDecisionTable() {
      return getDecision();
    }

    /**
   * Returns the decision associated with this object.
   *
   * @return the decision
   */
  public DmnDecision getDecision() {
    return decision;
  }

    /**
   * Sets the decision table for the DMN decision.
   * 
   * @param decision the DMN decision to set the decision table for
   */
  public void setDecisionTable(DmnDecision decision) {
   this.decision = decision;
  }

    /**
   * Returns the list of evaluated DMN inputs.
   *
   * @return the list of evaluated DMN inputs
   */
  public List<DmnEvaluatedInput> getInputs() {
    return inputs;
  }

    /**
   * Sets the list of evaluated inputs for the Decision.
   *
   * @param inputs the list of evaluated inputs to be set
   */
  public void setInputs(List<DmnEvaluatedInput> inputs) {
    this.inputs = inputs;
  }

    /**
   * Returns the list of evaluated decision rules that match certain criteria.
   *
   * @return the list of evaluated decision rules
   */
  public List<DmnEvaluatedDecisionRule> getMatchingRules() {
    return matchingRules;
  }

    /**
   * Sets the list of evaluated decision rules that matched the input data.
   *
   * @param matchingRules the list of evaluated decision rules
   */
  public void setMatchingRules(List<DmnEvaluatedDecisionRule> matchingRules) {
    this.matchingRules = matchingRules;
  }

    /**
   * Returns the collect result name.
   *
   * @return the collect result name
   */
  public String getCollectResultName() {
    return collectResultName;
  }

    /**
   * Sets the name of the collect result.
   * 
   * @param collectResultName the name of the collect result to be set
   */
  public void setCollectResultName(String collectResultName) {
    this.collectResultName = collectResultName;
  }

    /**
   * Returns the TypedValue object representing the collectResultValue.
   * 
   * @return the TypedValue object representing the collectResultValue
   */
  public TypedValue getCollectResultValue() {
    return collectResultValue;
  }

    /**
   * Sets the collect result value.
   *
   * @param collectResultValue the value to be set
   */
  public void setCollectResultValue(TypedValue collectResultValue) {
    this.collectResultValue = collectResultValue;
  }

    /**
   * Returns the number of executed decision elements.
   *
   * @return the number of executed decision elements
   */
  public long getExecutedDecisionElements() {
    return executedDecisionElements;
  }

    /**
   * Sets the number of executed decision elements.
   *
   * @param executedDecisionElements the number of executed decision elements
   */
  public void setExecutedDecisionElements(long executedDecisionElements) {
    this.executedDecisionElements = executedDecisionElements;
  }

    /**
   * Returns a string representation of the DmnDecisionTableEvaluationEventImpl object,
   * including key, name, decision logic, inputs, matching rules, collect result name,
   * collect result value, and executed decision elements.
   */
  @Override
  public String toString() {
    return "DmnDecisionTableEvaluationEventImpl{" +
      " key="+ decision.getKey() +
      ", name="+ decision.getName() +
      ", decisionLogic=" + decision.getDecisionLogic() +
      ", inputs=" + inputs +
      ", matchingRules=" + matchingRules +
      ", collectResultName='" + collectResultName + '\'' +
      ", collectResultValue=" + collectResultValue +
      ", executedDecisionElements=" + executedDecisionElements +
      '}';
  }

}
