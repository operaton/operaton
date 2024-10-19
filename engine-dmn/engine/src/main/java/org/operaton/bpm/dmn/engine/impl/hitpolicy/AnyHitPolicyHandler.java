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
package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedOutput;
import org.operaton.bpm.dmn.engine.impl.DmnLogger;
import org.operaton.bpm.dmn.engine.impl.delegate.DmnDecisionTableEvaluationEventImpl;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandler;
import org.operaton.bpm.model.dmn.HitPolicy;

public class AnyHitPolicyHandler implements DmnHitPolicyHandler {

  public static final DmnHitPolicyLogger LOG = DmnLogger.HIT_POLICY_LOGGER;
  protected static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.ANY, null);

    /**
   * Returns the Hit Policy Entry.
   *
   * @return the Hit Policy Entry
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

    /**
   * Applies the any hit policy to the given decision table evaluation event.
   * If there are multiple matching rules, only the first one is kept if all outputs are equal.
   * Otherwise, an exception is thrown.
   * 
   * @param decisionTableEvaluationEvent the decision table evaluation event to apply the any hit policy to
   * @return the updated decision table evaluation event
   */
  public DmnDecisionTableEvaluationEvent apply(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    List<DmnEvaluatedDecisionRule> matchingRules = decisionTableEvaluationEvent.getMatchingRules();

    if (!matchingRules.isEmpty()) {
      if (allOutputsAreEqual(matchingRules)) {
        DmnEvaluatedDecisionRule firstMatchingRule = matchingRules.get(0);
        ((DmnDecisionTableEvaluationEventImpl) decisionTableEvaluationEvent).setMatchingRules(Collections.singletonList(firstMatchingRule));
      } else {
        throw LOG.anyHitPolicyRequiresThatAllOutputsAreEqual(matchingRules);
      }
    }

    return decisionTableEvaluationEvent;
  }

    /**
   * Checks if all the evaluated decision rules have the same output entries.
   * 
   * @param matchingRules the list of evaluated decision rules to compare
   * @return true if all output entries are equal, false otherwise
   */
  protected boolean allOutputsAreEqual(List<DmnEvaluatedDecisionRule> matchingRules) {
    Map<String, DmnEvaluatedOutput> firstOutputEntries = matchingRules.get(0).getOutputEntries();
    if (firstOutputEntries == null) {
      for (int i = 1; i < matchingRules.size(); i++) {
        if (matchingRules.get(i).getOutputEntries() != null) {
          return false;
        }
      }
    } else {
      for (int i = 1; i < matchingRules.size(); i++) {
        if (!firstOutputEntries.equals(matchingRules.get(i).getOutputEntries())) {
          return false;
        }
      }
    }
    return true;
  }

    /**
   * Returns a string representation of the AnyHitPolicyHandler object.
   * 
   * @return a string with the format "AnyHitPolicyHandler{}"
   */
  @Override
  public String toString() {
    return "AnyHitPolicyHandler{}";
  }

}
