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

import java.util.Map;

import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedOutput;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableRuleImpl;

public class DmnEvaluatedDecisionRuleImpl implements DmnEvaluatedDecisionRule {

  protected String id;
  protected Map<String, DmnEvaluatedOutput> outputEntries;

  public DmnEvaluatedDecisionRuleImpl(DmnDecisionTableRuleImpl matchingRule) {
    this.id = matchingRule.getId();
  }

    /**
   * Returns the ID of the object.
   *
   * @return the ID of the object
   */
  public String getId() {
    return id;
  }

    /**
   * Sets the id of the object.
   * 
   * @param id the new id to set
   */
  public void setId(String id) {
    this.id = id;
  }

    /**
   * Returns the output entries of the Dmn evaluation.
   * 
   * @return a Map containing the evaluated output entries
   */
  public Map<String, DmnEvaluatedOutput> getOutputEntries() {
    return outputEntries;
  }

    /**
   * Sets the output entries for the evaluated DMN.
   * 
   * @param outputEntries a map containing the output entries
   */
  public void setOutputEntries(Map<String, DmnEvaluatedOutput> outputEntries) {
    this.outputEntries = outputEntries;
  }

    /**
   * Returns a string representation of the DmnEvaluatedDecisionRuleImpl object, including the id and output entries.
   * 
   * @return a string representation of the DmnEvaluatedDecisionRuleImpl object
   */
  @Override
  public String toString() {
    return "DmnEvaluatedDecisionRuleImpl{" +
      "id='" + id + '\'' +
      ", outputEntries=" + outputEntries +
      '}';
  }

}
