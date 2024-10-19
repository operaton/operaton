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

import java.util.List;

import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.impl.DmnLogger;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandler;
import org.operaton.bpm.model.dmn.HitPolicy;

public class UniqueHitPolicyHandler implements DmnHitPolicyHandler {

  public static final DmnHitPolicyLogger LOG = DmnLogger.HIT_POLICY_LOGGER;
  protected static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.UNIQUE, null);

    /**
   * Applies the hit policy to the decision table evaluation event.
   * If there are less than 2 matching rules, returns the decision table evaluation event.
   * Otherwise, throws an exception indicating that the unique hit policy only allows a single matching rule.
   *
   * @param decisionTableEvaluationEvent the decision table evaluation event to apply the hit policy to
   * @return the decision table evaluation event after applying the hit policy
   */
  public DmnDecisionTableEvaluationEvent apply(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    List<DmnEvaluatedDecisionRule> matchingRules = decisionTableEvaluationEvent.getMatchingRules();

    if (matchingRules.size() < 2) {
      return decisionTableEvaluationEvent;
    }
    else {
      throw LOG.uniqueHitPolicyOnlyAllowsSingleMatchingRule(matchingRules);
    }
  }

    /**
   * Returns the HitPolicyEntry object representing the hit policy of this class.
   *
   * @return the hit policy entry object
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

    /**
   * Returns a string representation of the UniqueHitPolicyHandler object.
   */
  @Override
  public String toString() {
    return "UniqueHitPolicyHandler{}";
  }

}
