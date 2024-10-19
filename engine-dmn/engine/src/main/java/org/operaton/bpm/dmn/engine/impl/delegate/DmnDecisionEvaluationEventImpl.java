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
import java.util.Collection;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionLogicEvaluationEvent;

public class DmnDecisionEvaluationEventImpl implements DmnDecisionEvaluationEvent {

  protected DmnDecisionLogicEvaluationEvent decisionResult;
  protected Collection<DmnDecisionLogicEvaluationEvent> requiredDecisionResults = new ArrayList<DmnDecisionLogicEvaluationEvent>();
  protected long executedDecisionInstances;
  protected long executedDecisionElements;

    /**
   * Returns the decision result.
   *
   * @return the decision result
   */
   @Override
    public DmnDecisionLogicEvaluationEvent getDecisionResult() {
      return decisionResult;
    }

    /**
   * Sets the decision result for a DMN Decision Logic Evaluation Event.
   * 
   * @param decisionResult the decision result to set
   */
  public void setDecisionResult(DmnDecisionLogicEvaluationEvent decisionResult) {
    this.decisionResult = decisionResult;
  }

    /**
   * Returns a collection of DmnDecisionLogicEvaluationEvent objects representing the required decision results
   * 
   * @return the collection of required decision results
   */
  @Override
  public Collection<DmnDecisionLogicEvaluationEvent> getRequiredDecisionResults() {
    return requiredDecisionResults;
  }

    /**
   * Sets the required decision results for the evaluation event.
   *
   * @param requiredDecisionResults the required decision results to set
   */
  public void setRequiredDecisionResults(Collection<DmnDecisionLogicEvaluationEvent> requiredDecisionResults) {
    this.requiredDecisionResults = requiredDecisionResults;
  }

    /**
   * Returns the number of executed decision instances.
   *
   * @return the number of executed decision instances
   */
  @Override
  public long getExecutedDecisionInstances() {
    return executedDecisionInstances;
  }

    /**
   * Sets the number of executed decision instances.
   * 
   * @param executedDecisionInstances the number of executed decision instances to set
   */
  public void setExecutedDecisionInstances(long executedDecisionInstances) {
    this.executedDecisionInstances = executedDecisionInstances;
  }

    /**
   * Returns the number of executed decision elements.
   *
   * @return the number of executed decision elements
   */
  @Override
  public long getExecutedDecisionElements() {
    return executedDecisionElements;
  }

    /**
   * Sets the number of executed decision elements.
   * 
   * @param executedDecisionElements the number of executed decision elements to set
   */
  public void setExecutedDecisionElements(long executedDecisionElements) {
    this.executedDecisionElements = executedDecisionElements;
  }

    /**
   * Returns a String representation of the DmnDecisionEvaluationEventImpl object, including key, name, decisionLogic, requiredDecisionResults, executedDecisionInstances, and executedDecisionElements.
   */
  @Override
  public String toString() {
    DmnDecision dmnDecision = decisionResult.getDecision();
    return "DmnDecisionEvaluationEventImpl{" +
      " key="+ dmnDecision.getKey() +
      ", name="+ dmnDecision.getName() +
      ", decisionLogic=" + dmnDecision.getDecisionLogic() +
      ", requiredDecisionResults=" + requiredDecisionResults +
      ", executedDecisionInstances=" + executedDecisionInstances +
      ", executedDecisionElements=" + executedDecisionElements +
      '}';
  }

}
