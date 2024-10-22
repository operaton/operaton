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
package org.operaton.bpm.dmn.engine.impl;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.dmn.engine.DmnDecisionLogic;
import org.operaton.bpm.dmn.engine.impl.hitpolicy.DefaultHitPolicyHandlerRegistry;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandler;
import org.operaton.bpm.model.dmn.BuiltinAggregator;
import org.operaton.bpm.model.dmn.HitPolicy;

public class DmnDecisionTableImpl implements DmnDecisionLogic {

  protected DmnHitPolicyHandler hitPolicyHandler;

  protected List<DmnDecisionTableInputImpl> inputs = new ArrayList<DmnDecisionTableInputImpl>();
  protected List<DmnDecisionTableOutputImpl> outputs = new ArrayList<DmnDecisionTableOutputImpl>();
  protected List<DmnDecisionTableRuleImpl> rules = new ArrayList<DmnDecisionTableRuleImpl>();

    /**
   * Returns the hit policy handler for the DMN engine.
   *
   * @return the hit policy handler
   */
  public DmnHitPolicyHandler getHitPolicyHandler() {
    return hitPolicyHandler;
  }

    /**
   * Sets the hit policy handler for the DMN processor.
   * 
   * @param hitPolicyHandler the hit policy handler to be set
   */
  public void setHitPolicyHandler(DmnHitPolicyHandler hitPolicyHandler) {
    this.hitPolicyHandler = hitPolicyHandler;
  }

    /**
   * Returns the list of DMN decision table inputs.
   *
   * @return the list of DMN decision table inputs
   */
  public List<DmnDecisionTableInputImpl> getInputs() {
    return inputs;
  }

    /**
   * Sets the inputs for the decision table.
   * 
   * @param inputs the list of decision table inputs to set
   */
  public void setInputs(List<DmnDecisionTableInputImpl> inputs) {
    this.inputs = inputs;
  }

    /**
   * Returns the list of DmnDecisionTableOutputImpl objects.
   *
   * @return the list of DmnDecisionTableOutputImpl objects
   */
  public List<DmnDecisionTableOutputImpl> getOutputs() {
    return outputs;
  }

    /**
   * Sets the list of decision table outputs for this decision table.
   * 
   * @param outputs the list of decision table outputs to be set
   */
  public void setOutputs(List<DmnDecisionTableOutputImpl> outputs) {
    this.outputs = outputs;
  }

    /**
   * Returns the list of DmnDecisionTableRuleImpl objects.
   *
   * @return the list of DmnDecisionTableRuleImpl objects
   */
  public List<DmnDecisionTableRuleImpl> getRules() {
    return rules;
  }

    /**
   * Sets the list of decision table rules for this decision table.
   *
   * @param rules the list of decision table rules
   */
  public void setRules(List<DmnDecisionTableRuleImpl> rules) {
    this.rules = rules;
  }

    /**
   * Returns a string representation of the DmnDecisionTableImpl object.
   */
  @Override
  public String toString() {
    return "DmnDecisionTableImpl{" +
      " hitPolicyHandler=" + hitPolicyHandler +
      ", inputs=" + inputs +
      ", outputs=" + outputs +
      ", rules=" + rules +
      '}';
  }
}
