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

import java.util.List;

import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.impl.DmnLogger;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandler;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * Implements the UNIQUE hit policy as defined in the DMN 1.3 specification (section 8.2.8).
 *
 * <p>The UNIQUE hit policy ensures that only one rule can match for any given input.
 * If multiple rules match, an exception is thrown. This policy is typically used when
 * the decision table is designed such that rules are mutually exclusive by definition.</p>
 *
 * <p><strong>Key Characteristics:</strong></p>
 * <ul>
 *   <li>Validates that at most one rule matches</li>
 *   <li>Returns the single matching rule, or no result if no rules match</li>
 *   <li>Throws an exception if multiple rules match</li>
 *   <li>No sorting or filtering is performed</li>
 * </ul>
 *
 * <p><strong>Difference to other single hit policies:</strong></p>
 * <ul>
 *   <li><strong>UNIQUE vs FIRST:</strong> UNIQUE enforces mutual exclusivity; FIRST allows
 *       multiple matches but returns only the first</li>
 *   <li><strong>UNIQUE vs ANY:</strong> UNIQUE allows at most one match; ANY allows multiple
 *       matches if all have identical outputs</li>
 * </ul>
 *
 * @see DmnHitPolicyHandler
 * @see FirstHitPolicyHandler
 * @see AnyHitPolicyHandler
 */
public class UniqueHitPolicyHandler implements DmnHitPolicyHandler {

  public static final DmnHitPolicyLogger LOG = DmnLogger.HIT_POLICY_LOGGER;
  private static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.UNIQUE, null);
  private static final int MAX_ALLOWED_MATCHES = 1;

  /**
   * Applies the UNIQUE hit policy logic to the evaluation event.
   *
   * <p>This method validates that at most one rule has matched. If zero or one rule
   * matches, the event is returned unchanged. If two or more rules match, an exception
   * is thrown.</p>
   *
   * @param decisionTableEvaluationEvent the evaluation event containing the matching rules
   * @return the unchanged evaluation event if validation passes
   * @throws DmnHitPolicyException if more than one rule matches
   */
  @Override
  public DmnDecisionTableEvaluationEvent apply(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    List<DmnEvaluatedDecisionRule> matchingRules = decisionTableEvaluationEvent.getMatchingRules();

    if (matchingRules.size() <= MAX_ALLOWED_MATCHES) {
      return decisionTableEvaluationEvent;
    }
    else {
      throw LOG.uniqueHitPolicyOnlyAllowsSingleMatchingRule(matchingRules);
    }
  }

  /**
   * Retrieves the hit policy entry for the UNIQUE hit policy.
   *
   * @return the hit policy entry defining the UNIQUE hit policy (without aggregator)
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

  /**
   * Returns a string representation of the UniqueHitPolicyHandler.
   *
   * @return a string that represents this handler instance with its hit policy
   */
  @Override
  public String toString() {
    return "UniqueHitPolicyHandler{hitPolicy=" + HIT_POLICY.getHitPolicy() + "}";
  }

}
