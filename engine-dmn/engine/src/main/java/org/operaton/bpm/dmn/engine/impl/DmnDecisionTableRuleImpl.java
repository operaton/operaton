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

public class DmnDecisionTableRuleImpl {

  public String id;
  public String name;

  protected List<DmnExpressionImpl> conditions = new ArrayList<DmnExpressionImpl>();
  protected List<DmnExpressionImpl> conclusions = new ArrayList<DmnExpressionImpl>();

    /**
   * Returns the id of the object.
   *
   * @return the id of the object
   */
  public String getId() {
    return id;
  }

    /**
   * Sets the ID of the object.
   * 
   * @param id the new ID to set
   */
  public void setId(String id) {
    this.id = id;
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
   * @param name the new name to be set
   */
  public void setName(String name) {
    this.name = name;
  }

    /**
   * Returns the list of conditions.
   *
   * @return the list of conditions
   */
  public List<DmnExpressionImpl> getConditions() {
    return conditions;
  }

    /**
   * Sets the list of DMN expressions representing conditions.
   * 
   * @param conditions the list of DMN expressions representing conditions
   */
  public void setConditions(List<DmnExpressionImpl> conditions) {
    this.conditions = conditions;
  }

    /**
   * Returns the list of DmnExpressionImpl objects representing the conclusions.
   * 
   * @return the list of conclusions
   */
  public List<DmnExpressionImpl> getConclusions() {
    return conclusions;
  }

    /**
   * Sets the list of conclusions for the decision.
   *
   * @param conclusions the list of conclusions to set
   */
  public void setConclusions(List<DmnExpressionImpl> conclusions) {
    this.conclusions = conclusions;
  }

    /**
   * Returns a string representation of the DmnDecisionTableRuleImpl object including its id, name, conditions, and conclusions.
   * 
   * @return a string representation of the DmnDecisionTableRuleImpl object
   */
    @Override
    public String toString() {
      return "DmnDecisionTableRuleImpl{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        ", conditions=" + conditions +
        ", conclusions=" + conclusions +
        '}';
    }

}
