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
  protected List<DmnEvaluatedInput> inputs = new ArrayList<>();
  protected List<DmnEvaluatedDecisionRule> matchingRules = new ArrayList<>();
  protected String collectResultName;
  protected TypedValue collectResultValue;
  protected long executedDecisionElements;

  @Override
  public DmnDecision getDecisionTable() {
    return getDecision();
  }

  @Override
  public DmnDecision getDecision() {
    return decision;
  }

  public void setDecisionTable(DmnDecision decision) {
   this.decision = decision;
  }

  @Override
  public List<DmnEvaluatedInput> getInputs() {
    return inputs;
  }

  public void setInputs(List<DmnEvaluatedInput> inputs) {
    this.inputs = inputs;
  }

  @Override
  public List<DmnEvaluatedDecisionRule> getMatchingRules() {
    return matchingRules;
  }

  public void setMatchingRules(List<DmnEvaluatedDecisionRule> matchingRules) {
    this.matchingRules = matchingRules;
  }

  @Override
  public String getCollectResultName() {
    return collectResultName;
  }

  public void setCollectResultName(String collectResultName) {
    this.collectResultName = collectResultName;
  }

  @Override
  public TypedValue getCollectResultValue() {
    return collectResultValue;
  }

  public void setCollectResultValue(TypedValue collectResultValue) {
    this.collectResultValue = collectResultValue;
  }

  @Override
  public long getExecutedDecisionElements() {
    return executedDecisionElements;
  }

  public void setExecutedDecisionElements(long executedDecisionElements) {
    this.executedDecisionElements = executedDecisionElements;
  }

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
