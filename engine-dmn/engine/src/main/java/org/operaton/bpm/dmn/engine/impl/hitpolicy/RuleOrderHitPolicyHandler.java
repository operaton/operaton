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

import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandler;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * Implements the RULE_ORDER hit policy as defined in the DMN 1.3 specification (section 8.2.8).
 *
 * <p>The RULE_ORDER hit policy is a multiple hit policy that returns all matching rules
 * in the order they appear in the decision table. No filtering, sorting, or aggregation
 * is performed. This is the simplest multiple hit policy.</p>
 *
 * <p><strong>Key Characteristics:</strong></p>
 * <ul>
 *   <li>Returns all matching rules (multiple hit policy)</li>
 *   <li>Preserves the order of rules as defined in the decision table</li>
 *   <li>No validation, filtering, or transformation is applied</li>
 *   <li>Rule order in the decision table definition is significant</li>
 * </ul>
 *
 * <p><strong>Difference to other multiple hit policies:</strong></p>
 * <ul>
 *   <li><strong>RULE_ORDER vs OUTPUT_ORDER:</strong> RULE_ORDER preserves table order;
 *       OUTPUT_ORDER sorts by output values lexicographically</li>
 *   <li><strong>RULE_ORDER vs COLLECT:</strong> Functionally identical when no aggregator
 *       is used; COLLECT can optionally aggregate results</li>
 * </ul>
 *
 * <p><strong>Implementation Note:</strong> This handler is effectively a pass-through that
 * returns the evaluation event unchanged, as the matching rules are already in table order.</p>
 *
 * @see DmnHitPolicyHandler
 * @see OutputOrderHitPolicyHandler
 * @see CollectHitPolicyHandler
 */
public class RuleOrderHitPolicyHandler implements DmnHitPolicyHandler {

  protected static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.RULE_ORDER, null);

  /**
   * Retrieves the hit policy entry for the RULE_ORDER hit policy.
   *
   * @return the hit policy entry defining the RULE_ORDER hit policy (without aggregator)
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

  /**
   * Applies the RULE_ORDER hit policy logic to the evaluation event.
   *
   * <p>This implementation returns the evaluation event unchanged, as the matching rules
   * are already in the correct order (decision table order). No processing is required.</p>
   *
   * @param decisionTableEvaluationEvent the evaluation event containing the matching rules
   * @return the unchanged evaluation event with all matching rules in table order
   */
  @Override
  public DmnDecisionTableEvaluationEvent apply(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    return decisionTableEvaluationEvent;
  }

  /**
   * Returns a string representation of the RuleOrderHitPolicyHandler.
   *
   * @return a string that represents this handler instance with its hit policy
   */
  @Override
  public String toString() {
    return "RuleOrderHitPolicyHandler{hitPolicy=" + HIT_POLICY.getHitPolicy() + "}";
  }

}
