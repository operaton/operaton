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
 * Implements the COLLECT hit policy without aggregation, as defined in the DMN 1.3 specification (section 8.2.8).
 *
 * <p>The COLLECT hit policy is a multiple hit policy that returns all matching rules.
 * When used without an aggregator, it behaves similarly to RULE_ORDER, preserving the
 * order of rules as they appear in the decision table.</p>
 *
 * <p><strong>Key Characteristics:</strong></p>
 * <ul>
 *   <li>Returns all matching rules (multiple hit policy)</li>
 *   <li>No aggregation is performed (returns raw rule outputs)</li>
 *   <li>Preserves the order of rules as defined in the decision table</li>
 *   <li>Can be combined with aggregation functions (COUNT, SUM, MIN, MAX) via other handlers</li>
 * </ul>
 *
 * <p><strong>Difference to other multiple hit policies:</strong></p>
 * <ul>
 *   <li><strong>COLLECT vs RULE_ORDER:</strong> Functionally identical when no aggregator is used;
 *       COLLECT is the policy that supports optional aggregation</li>
 *   <li><strong>COLLECT vs OUTPUT_ORDER:</strong> COLLECT preserves table order; OUTPUT_ORDER
 *       sorts by output values</li>
 * </ul>
 *
 * <p><strong>Aggregation Support:</strong> To use aggregation functions with COLLECT, use the
 * specific aggregation handlers:</p>
 * <ul>
 *   <li>{@link CollectCountHitPolicyHandler} - Counts matching rules</li>
 *   <li>{@link CollectSumHitPolicyHandler} - Sums numeric output values</li>
 *   <li>{@link CollectMinHitPolicyHandler} - Returns minimum numeric value</li>
 *   <li>{@link CollectMaxHitPolicyHandler} - Returns maximum numeric value</li>
 * </ul>
 *
 * <p><strong>Implementation Note:</strong> This handler is effectively a pass-through that
 * returns the evaluation event unchanged, similar to {@link RuleOrderHitPolicyHandler}.</p>
 *
 * @see DmnHitPolicyHandler
 * @see RuleOrderHitPolicyHandler
 * @see AbstractCollectNumberHitPolicyHandler
 */
public class CollectHitPolicyHandler implements DmnHitPolicyHandler {
  protected static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.COLLECT, null);

  /**
   * Retrieves the hit policy entry for the COLLECT hit policy without aggregation.
   *
   * @return the hit policy entry defining the COLLECT hit policy (without aggregator)
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

  /**
   * Applies the COLLECT hit policy logic without aggregation to the evaluation event.
   *
   * <p>This implementation returns the evaluation event unchanged, as no aggregation
   * or transformation is required. All matching rules are returned as-is.</p>
   *
   * @param decisionTableEvaluationEvent the evaluation event containing the matching rules
   * @return the unchanged evaluation event with all matching rules
   */
  @Override
  public DmnDecisionTableEvaluationEvent apply(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    return decisionTableEvaluationEvent;
  }

  /**
   * Returns a string representation of the CollectHitPolicyHandler.
   *
   * @return a string that represents this handler instance with its hit policy
   */
  @Override
  public String toString() {
    return "CollectHitPolicyHandler{hitPolicy=" + HIT_POLICY.getHitPolicy() + "}";
  }

}
