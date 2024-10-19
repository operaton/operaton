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
import java.util.Collection;
import java.util.List;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionLogic;

public class DmnDecisionImpl implements DmnDecision {

  protected String key;
  protected String name;

  protected DmnDecisionLogic decisionLogic;

  protected Collection<DmnDecision> requiredDecision = new ArrayList<DmnDecision>();

    /**
   * Returns the key value.
   *
   * @return the key value
   */
  public String getKey() {
    return key;
  }

    /**
   * Sets the key for the object.
   *
   * @param key the key to set
   */
  public void setKey(String key) {
    this.key = key;
  }

    /**
   * Returns the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

    /**
   * Sets the name of the object.
   * 
   * @param name the new name to set
   */
  public void setName(String name) {
    this.name = name;
  }

    /**
   * Sets the decision logic for the DMN decision.
   * 
   * @param decisionLogic the decision logic to be set
   */
  public void setDecisionLogic(DmnDecisionLogic decisionLogic) {
    this.decisionLogic = decisionLogic;
  }

    /**
   * Returns the decision logic associated with this decision.
   * 
   * @return the decision logic associated with this decision
   */
  public DmnDecisionLogic getDecisionLogic() {
    return decisionLogic;
  }

    /**
   * Sets the list of required DMN decisions.
   * 
   * @param requiredDecision the list of required DMN decisions to set
   */
  public void setRequiredDecision(List<DmnDecision> requiredDecision) {
    this.requiredDecision = requiredDecision;
  }

    /**
   * Returns a collection of required decisions.
   *
   * @return the collection of required decisions
   */
  @Override
  public Collection<DmnDecision> getRequiredDecisions() {
    return requiredDecision;
  }

    /**
   * Checks if the decision logic is a DmnDecisionTableImpl instance.
   * 
   * @return true if the decision logic is a DmnDecisionTableImpl instance, false otherwise
   */
  @Override
  public boolean isDecisionTable() {
    return decisionLogic != null && decisionLogic instanceof DmnDecisionTableImpl;
  }

    /**
   * Returns a string representation of the DmnDecisionTableImpl object.
   */
  @Override
  public String toString() {
    return "DmnDecisionTableImpl{" +
      " key= "+ key +
      ", name= "+ name +
      ", requiredDecision=" + requiredDecision +
      ", decisionLogic=" + decisionLogic +
      '}';
  }
}
