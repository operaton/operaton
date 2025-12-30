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

import java.util.Objects;

import org.operaton.bpm.model.dmn.BuiltinAggregator;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * Represents a hit policy configuration consisting of a hit policy type and an optional aggregator.
 *
 * <p>This record serves as a composite key for identifying and registering hit policy handlers
 * in the {@link org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandlerRegistry}.
 * The combination of hit policy and aggregator uniquely identifies a specific evaluation strategy
 * for DMN decision tables.</p>
 *
 * <p><strong>Valid Combinations:</strong></p>
 * <ul>
 *   <li><strong>Single Hit Policies:</strong> UNIQUE, FIRST, PRIORITY, ANY (aggregator must be null)</li>
 *   <li><strong>Multiple Hit Policies:</strong> RULE_ORDER, OUTPUT_ORDER (aggregator must be null)</li>
 *   <li><strong>COLLECT without Aggregation:</strong> COLLECT with null aggregator</li>
 *   <li><strong>COLLECT with Aggregation:</strong> COLLECT with COUNT, SUM, MIN, or MAX aggregator</li>
 * </ul>
 *
 * <p><strong>Immutability:</strong> As a record, this class is immutable. Both the hit policy and aggregator
 * cannot be changed after construction.</p>
 *
 * <p><strong>Equality and Hashing:</strong> Two {@code HitPolicyEntry} instances are considered
 * equal if they have the same hit policy and aggregator. Records automatically implement proper
 * {@link #equals(Object)} and {@link #hashCode()} to support use as map keys.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 * // Single hit policy without aggregator
 * HitPolicyEntry unique = new HitPolicyEntry(HitPolicy.UNIQUE, null);
 *
 * // Multiple hit policy with aggregation
 * HitPolicyEntry collectSum = new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.SUM);
 * </pre>
 *
 * @param hitPolicy the hit policy type (e.g., UNIQUE, COLLECT, PRIORITY), must not be null
 * @param aggregator the optional aggregator (e.g., SUM, MIN, MAX), or null if not applicable
 *
 * @author Askar Akhmerov
 * @see org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandlerRegistry
 * @see org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandler
 */
public record HitPolicyEntry(HitPolicy hitPolicy, BuiltinAggregator aggregator) {

  /**
   * Compact constructor that validates the hit policy is not null.
   *
   * <p><strong>Note:</strong> The aggregator should only be provided for the COLLECT hit policy.
   * For all other hit policies (UNIQUE, FIRST, PRIORITY, ANY, RULE_ORDER, OUTPUT_ORDER),
   * the aggregator must be null.</p>
   *
   * @throws NullPointerException if hitPolicy is null
   */
  public HitPolicyEntry {
    Objects.requireNonNull(hitPolicy, "hitPolicy must not be null");
  }

  /**
   * Returns the hit policy type.
   *
   * @return the hit policy (e.g., UNIQUE, COLLECT, PRIORITY), never null
   */
  public HitPolicy getHitPolicy() {
    return hitPolicy;
  }

  /**
   * Returns the optional aggregator.
   *
   * @return the aggregator (e.g., SUM, MIN, MAX), or null if no aggregation is used
   */
  public BuiltinAggregator getAggregator() {
    return aggregator;
  }

}
