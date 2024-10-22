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

import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.impl.delegate.DmnDecisionTableEvaluationEventImpl;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandler;
import org.operaton.bpm.model.dmn.HitPolicy;


public class FirstHitPolicyHandler implements DmnHitPolicyHandler {
  protected static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.FIRST, null);

    /**
   * Applies the first matched rule to the decision table evaluation event.
   * If there are matching rules, the first one is set as the only matching rule in the event.
   * 
   * @param decisionTableEvaluationEvent the decision table evaluation event to apply the first matched rule to
   * @return the updated decision table evaluation event
   */
  public DmnDecisionTableEvaluationEvent apply(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    if (!decisionTableEvaluationEvent.getMatchingRules().isEmpty()) {
      DmnEvaluatedDecisionRule firstMatchedRule = decisionTableEvaluationEvent.getMatchingRules().get(0);
      ((DmnDecisionTableEvaluationEventImpl) decisionTableEvaluationEvent).setMatchingRules(Collections.singletonList(firstMatchedRule));
    }
    return decisionTableEvaluationEvent;
  }

    /**
   * Returns the HitPolicyEntry constant value.
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

    /**
   * Returns a string representation of the FirstHitPolicyHandler object.
   */
  @Override
  public String toString() {
    return "FirstHitPolicyHandler{}";
  }

}
