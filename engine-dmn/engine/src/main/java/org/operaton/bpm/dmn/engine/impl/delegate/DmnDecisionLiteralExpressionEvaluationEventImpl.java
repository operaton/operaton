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

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionLiteralExpressionEvaluationEvent;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class DmnDecisionLiteralExpressionEvaluationEventImpl implements DmnDecisionLiteralExpressionEvaluationEvent {

  protected DmnDecision decision;

  protected String outputName;
  protected TypedValue outputValue;

  protected long executedDecisionElements;

    /**
   * Returns the DmnDecision object.
   * 
   * @return the DmnDecision object
   */
  public DmnDecision getDecision() {
    return decision;
  }

    /**
   * Sets the specified DMN decision for this object.
   * 
   * @param decision the DMN decision to be set
   */
  public void setDecision(DmnDecision decision) {
    this.decision = decision;
  }

    /**
   * Returns the output name.
   * 
   * @return the output name
   */
  public String getOutputName() {
    return outputName;
  }

    /**
   * Sets the output name for the method.
   *
   * @param outputName the new output name
   */
  public void setOutputName(String outputName) {
    this.outputName = outputName;
  }

    /**
   * Returns the output value.
   *
   * @return the output value
   */
  public TypedValue getOutputValue() {
    return outputValue;
  }

    /**
   * Sets the output value for the method.
   * 
   * @param outputValue the value to be set as the output
   */
  public void setOutputValue(TypedValue outputValue) {
    this.outputValue = outputValue;
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
   * Returns a string representation of the DmnDecisionLiteralExpressionEvaluationEventImpl object,
   * including key, name, decision logic, output name, output value, and executed decision elements.
   */
  @Override
  public String toString() {
    return "DmnDecisionLiteralExpressionEvaluationEventImpl [" +
        " key="+ decision.getKey() +
        ", name="+ decision.getName() +
        ", decisionLogic=" + decision.getDecisionLogic() +
        ", outputName=" + outputName +
        ", outputValue=" + outputValue +
        ", executedDecisionElements=" + executedDecisionElements +
        "]";
  }



}
