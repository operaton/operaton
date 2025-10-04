/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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

/**
 * Implements the FIRST hit policy as defined in the DMN 1.3 specification (section 8.2.8).
 *
 * <p>The FIRST hit policy returns the first rule that matches, according to the order
 * of rules in the decision table. All subsequent matching rules are ignored. This policy
 * is useful when rules are ordered by priority or when early termination is desired.</p>
 *
 * <p><strong>Key Characteristics:</strong></p>
 * <ul>
 *   <li>Returns only the first matching rule in table order</li>
 *   <li>Evaluation can short-circuit after the first match (implementation-dependent)</li>
 *   <li>Rule order in the decision table definition is significant</li>
 *   <li>No validation that only one rule matches (unlike UNIQUE)</li>
 * </ul>
 *
 * <p><strong>Difference to other single hit policies:</strong></p>
 * <ul>
 *   <li><strong>FIRST vs UNIQUE:</strong> FIRST allows multiple matches but returns only the first;
 *       UNIQUE enforces that at most one rule matches</li>
 *   <li><strong>FIRST vs PRIORITY:</strong> FIRST uses table order; PRIORITY uses output value order</li>
 *   <li><strong>FIRST vs ANY:</strong> FIRST returns the first match without validation;
 *       ANY validates all matches have identical outputs</li>
 * </ul>
 *
 * @see DmnHitPolicyHandler
 * @see UniqueHitPolicyHandler
 * @see PriorityHitPolicyHandler
 */
public class FirstHitPolicyHandler implements DmnHitPolicyHandler {
  private static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.FIRST, null);

  /**
   * Applies the FIRST hit policy logic to the evaluation event.
   *
   * <p>If any rules match, this method retains only the first matching rule (index 0)
   * and discards all others. If no rules match, the event is returned unchanged.</p>
   *
   * @param decisionTableEvaluationEvent the evaluation event containing the matching rules
   * @return the evaluation event with only the first matching rule (or empty if no matches)
   */
  @Override
  public DmnDecisionTableEvaluationEvent apply(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    if (!decisionTableEvaluationEvent.getMatchingRules().isEmpty()) {
      DmnEvaluatedDecisionRule firstMatchedRule = decisionTableEvaluationEvent.getMatchingRules().get(0);
      if (decisionTableEvaluationEvent instanceof DmnDecisionTableEvaluationEventImpl impl) {
        impl.setMatchingRules(Collections.singletonList(firstMatchedRule));
      }
    }
    return decisionTableEvaluationEvent;
  }

  /**
   * Retrieves the hit policy entry for the FIRST hit policy.
   *
   * @return the hit policy entry defining the FIRST hit policy (without aggregator)
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

  /**
   * Returns a string representation of the FirstHitPolicyHandler.
   *
   * @return a string that represents this handler instance with its hit policy
   */
  @Override
  public String toString() {
    return "FirstHitPolicyHandler{hitPolicy=" + HIT_POLICY.getHitPolicy() + "}";
  }

}
