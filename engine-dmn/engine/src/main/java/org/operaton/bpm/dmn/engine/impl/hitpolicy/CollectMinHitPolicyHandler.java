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
import java.util.List;

import org.operaton.bpm.model.dmn.BuiltinAggregator;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * Implements the COLLECT hit policy with MIN aggregation as defined in the DMN 1.3 specification (section 8.2.8).
 *
 * <p>The MIN aggregator returns the minimum value among all numeric output values from matching rules.
 * It supports Integer, Long, and Double types, automatically detecting the appropriate type
 * and finding the minimum value using natural ordering.</p>
 *
 * <p><strong>Key Characteristics:</strong></p>
 * <ul>
 *   <li>Returns the minimum numeric output value from matching rules</li>
 *   <li>Supports Integer, Long, and Double types</li>
 *   <li>Uses {@link Collections#min} for comparison (natural ordering)</li>
 *   <li>Result type matches the detected input type</li>
 *   <li>Requires single output (not supported for compound outputs)</li>
 *   <li>Requires at least one matching rule with a value</li>
 * </ul>
 *
 * <p><strong>Type Handling:</strong></p>
 * <ul>
 *   <li><strong>All Integers:</strong> Returns Integer minimum</li>
 *   <li><strong>Integers and Longs:</strong> Returns Long minimum</li>
 *   <li><strong>Mixed numeric types:</strong> Returns Double minimum</li>
 * </ul>
 *
 * <p><strong>Difference to other COLLECT aggregators:</strong></p>
 * <ul>
 *   <li><strong>MIN vs MAX:</strong> MIN returns smallest value; MAX returns largest</li>
 *   <li><strong>MIN vs SUM:</strong> MIN selects single value; SUM accumulates all</li>
 *   <li><strong>MIN vs COUNT:</strong> MIN processes values; COUNT ignores them</li>
 * </ul>
 *
 * @see AbstractCollectNumberHitPolicyHandler
 * @see CollectMaxHitPolicyHandler
 * @see CollectHitPolicyHandler
 */
public class CollectMinHitPolicyHandler extends AbstractCollectNumberHitPolicyHandler {
  private static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.MIN);

  /**
   * Retrieves the hit policy entry for the COLLECT MIN hit policy.
   *
   * @return the hit policy entry defining COLLECT with MIN aggregator
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

  /**
   * Returns the MIN aggregator.
   *
   * @return {@link BuiltinAggregator#MIN}
   */
  @Override
  protected BuiltinAggregator getAggregator() {
    return BuiltinAggregator.MIN;
  }

  /**
   * Aggregates integer values by finding the minimum.
   *
   * @param intValues the integer values to compare
   * @return the minimum integer value
   */
  @Override
  protected Integer aggregateIntegerValues(List<Integer> intValues) {
    return Collections.min(intValues);
  }

  /**
   * Aggregates long values by finding the minimum.
   *
   * @param longValues the long values to compare
   * @return the minimum long value
   */
  @Override
  protected Long aggregateLongValues(List<Long> longValues) {
    return Collections.min(longValues);
  }

  /**
   * Aggregates double values by finding the minimum.
   *
   * @param doubleValues the double values to compare
   * @return the minimum double value
   */
  @Override
  protected Double aggregateDoubleValues(List<Double> doubleValues) {
    return Collections.min(doubleValues);
  }

  /**
   * Returns a string representation of the CollectMinHitPolicyHandler.
   *
   * @return a string that represents this handler instance with its hit policy and aggregator
   */
  @Override
  public String toString() {
    return "CollectMinHitPolicyHandler{hitPolicy=" + HIT_POLICY.getHitPolicy() + ", aggregator=" + HIT_POLICY.getAggregator() + "}";
  }

}
