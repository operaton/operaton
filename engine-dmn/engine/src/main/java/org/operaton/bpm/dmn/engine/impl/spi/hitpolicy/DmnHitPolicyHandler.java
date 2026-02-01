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
package org.operaton.bpm.dmn.engine.impl.spi.hitpolicy;

import org.operaton.bpm.dmn.engine.DmnEngineException;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.operaton.bpm.dmn.engine.impl.hitpolicy.HitPolicyEntry;

/**
 * Service Provider Interface (SPI) for implementing DMN decision table hit policy handlers.
 *
 * <p>Hit policies define how to handle multiple matching rules in a DMN decision table,
 * as specified in the DMN 1.3 specification (section 8.2.8). A hit policy handler is
 * responsible for processing the matching rules according to a specific hit policy strategy.</p>
 *
 * <p><strong>DMN Hit Policy Types:</strong></p>
 * <ul>
 *   <li><strong>Single Hit Policies:</strong> UNIQUE, FIRST, PRIORITY, ANY - return at most one result</li>
 *   <li><strong>Multiple Hit Policies:</strong> COLLECT, RULE ORDER, OUTPUT ORDER - can return multiple results</li>
 *   <li><strong>Aggregation Functions:</strong> Available for COLLECT policy (SUM, MIN, MAX, COUNT)</li>
 * </ul>
 *
 * <p>Implementations of this interface must handle:</p>
 * <ul>
 *   <li>Validation of matching rules according to the hit policy constraints</li>
 *   <li>Filtering and/or sorting of matching rules based on hit policy logic</li>
 *   <li>Aggregation of results (if applicable to the hit policy)</li>
 *   <li>Proper error handling and reporting via {@link DmnEngineException}</li>
 * </ul>
 *
 * @see org.operaton.bpm.dmn.engine.impl.hitpolicy.SortingHitPolicyHandler
 * @see HitPolicyEntry
 */
public interface DmnHitPolicyHandler {

  /**
   * Applies the hit policy logic to the decision table evaluation event.
   *
   * <p>Depending on the specific hit policy, this method performs one or more of the following operations:</p>
   * <ul>
   *   <li><strong>Filtering:</strong> Selecting a subset of matching rules (e.g., FIRST, PRIORITY, ANY)</li>
   *   <li><strong>Sorting:</strong> Ordering matching rules according to specific criteria (e.g., PRIORITY, OUTPUT ORDER)</li>
   *   <li><strong>Aggregation:</strong> Combining output values using aggregation functions (e.g., COLLECT with SUM, MIN, MAX)</li>
   *   <li><strong>Validation:</strong> Ensuring matching rules conform to hit policy requirements</li>
   * </ul>
   *
   * <p>The method modifies the evaluation event to reflect the result of applying the hit policy,
   * which may include updating the list of matching rules or computing aggregated outputs.</p>
   *
   * @param decisionTableEvaluationEvent the evaluation event containing the decision table,
   *                                     matching rules, and evaluation context
   * @return the evaluation event with the hit policy applied, containing the final set of
   *         matching rules or aggregated results
   * @throws DmnEngineException if the hit policy cannot be applied due to:
   *         <ul>
   *           <li>Invalid decision table structure or configuration</li>
   *           <li>Missing required output values or definitions</li>
   *           <li>Hit policy constraint violations (e.g., UNIQUE with multiple matches)</li>
   *           <li>Unsupported aggregation operations</li>
   *         </ul>
   */
  DmnDecisionTableEvaluationEvent apply(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent);

  /**
   * Retrieves the hit policy entry that this handler implements.
   *
   * <p>The hit policy entry identifies both the hit policy type (e.g., PRIORITY, COLLECT)
   * and the optional aggregation function (e.g., SUM, MIN, MAX) for hit policies that support
   * aggregation.</p>
   *
   * @return the hit policy entry defining the hit policy and optional aggregator that this
   *         handler implements
   */
  HitPolicyEntry getHitPolicyEntry();

}
