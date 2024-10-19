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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionRequirementsGraph;

public class DmnDecisionRequirementsGraphImpl implements DmnDecisionRequirementsGraph {

  protected String key;
  protected String name;

  protected Map<String, DmnDecision> decisions = new HashMap<String, DmnDecision>();

    /**
   * Returns the key.
   *
   * @return the key
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
   * Returns the name of the object.
   *
   * @return the name of the object
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
   * Retrieves a collection of DmnDecision objects.
   *
   * @return the collection of DmnDecision objects
   */
  public Collection<DmnDecision> getDecisions() {
      return decisions.values();
    }

    /**
   * Sets the decisions map with the specified decisions.
   * 
   * @param decisions a map of decisions with their corresponding keys
   */
  public void setDecisions(Map<String, DmnDecision> decisions) {
    this.decisions = decisions;
  }

    /**
   * Adds a DMN decision to the decisions map.
   * 
   * @param decision the DMN decision to be added
   */
  public void addDecision(DmnDecision decision) {
    decisions.put(decision.getKey(), decision);
  }

    /**
   * Returns the decision associated with the given key.
   * 
   * @param key the key of the decision to retrieve
   * @return the decision with the specified key, or null if not found
   */
  public DmnDecision getDecision(String key) {
    return decisions.get(key);
  }

    /**
   * Retrieves the set of decision keys from the map of decisions.
   *
   * @return a Set of Strings representing the decision keys
   */
  public Set<String> getDecisionKeys() {
    return decisions.keySet();
  }

    /**
   * Returns a string representation of the DmnDecisionRequirementsGraphImpl object.
   */
  @Override
  public String toString() {
    return "DmnDecisionRequirementsGraphImpl [key=" + key + ", name=" + name + ", decisions=" + decisions + "]";
  }



}
