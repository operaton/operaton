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

import org.operaton.bpm.model.dmn.BuiltinAggregator;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * Represents a hit policy configuration consisting of a hit policy type and an optional aggregator.
 *
 * <p>This class serves as a composite key for identifying and registering hit policy handlers
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
 * <p><strong>Immutability:</strong> This class is immutable. Both the hit policy and aggregator
 * are final and cannot be changed after construction.</p>
 *
 * <p><strong>Equality and Hashing:</strong> Two {@code HitPolicyEntry} instances are considered
 * equal if they have the same hit policy and aggregator. The class properly implements
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
 * @author Askar Akhmerov
 * @see org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandlerRegistry
 * @see org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandler
 */
public class HitPolicyEntry {

  protected final HitPolicy hitPolicy;
  protected final BuiltinAggregator aggregator;

  /**
   * Creates a new hit policy entry with the specified hit policy and optional aggregator.
   *
   * <p><strong>Note:</strong> The aggregator should only be provided for the COLLECT hit policy.
   * For all other hit policies (UNIQUE, FIRST, PRIORITY, ANY, RULE_ORDER, OUTPUT_ORDER),
   * the aggregator must be null.</p>
   *
   * @param hitPolicy the hit policy type (e.g., UNIQUE, COLLECT, PRIORITY)
   * @param builtinAggregator the optional aggregator (e.g., SUM, MIN, MAX), or null if not applicable
   */
  public HitPolicyEntry(HitPolicy hitPolicy, BuiltinAggregator builtinAggregator) {
    this.hitPolicy = hitPolicy;
    this.aggregator = builtinAggregator;
  }

  /**
   * Compares this hit policy entry to another object for equality.
   *
   * <p>Two hit policy entries are considered equal if they have the same hit policy
   * and the same aggregator (both may be null).</p>
   *
   * @param o the object to compare with
   * @return true if the objects are equal, false otherwise
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HitPolicyEntry that = (HitPolicyEntry) o;

    if (hitPolicy != that.hitPolicy) {
      return false;
    }
    return aggregator == that.aggregator;

  }

  /**
   * Returns a hash code value for this hit policy entry.
   *
   * <p>The hash code is computed from the hit policy and aggregator to ensure
   * consistent behavior when used as a map key.</p>
   *
   * @return a hash code value for this object
   */
  @Override
  public int hashCode() {
    int result = hitPolicy != null ? hitPolicy.hashCode() : 0;
    return 31 * result + (aggregator != null ? aggregator.hashCode() : 0);
  }

  /**
   * Returns the hit policy type.
   *
   * @return the hit policy (e.g., UNIQUE, COLLECT, PRIORITY)
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

  /**
   * Returns a string representation of this hit policy entry.
   *
   * <p>The format is:</p>
   * <ul>
   *   <li>Without aggregator: {@code HitPolicyEntry{hitPolicy=UNIQUE, aggregator=null}}</li>
   *   <li>With aggregator: {@code HitPolicyEntry{hitPolicy=COLLECT, aggregator=SUM}}</li>
   * </ul>
   *
   * @return a string representation of this hit policy entry
   */
  @Override
  public String toString() {
    return "HitPolicyEntry{" +
        "hitPolicy=" + hitPolicy +
        ", aggregator=" + aggregator +
        '}';
  }

}
