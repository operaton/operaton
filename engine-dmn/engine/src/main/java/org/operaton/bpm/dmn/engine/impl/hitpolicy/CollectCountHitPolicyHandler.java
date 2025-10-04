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

import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.operaton.bpm.model.dmn.BuiltinAggregator;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * Implements the COLLECT hit policy with COUNT aggregation as defined in the DMN 1.3 specification (section 8.2.8).
 *
 * <p>The COUNT aggregator returns the number of matching rules, regardless of their output values.
 * Unlike other numeric aggregators (SUM, MIN, MAX), COUNT does not require numeric output values
 * and simply counts the number of rules that matched.</p>
 *
 * <p><strong>Key Characteristics:</strong></p>
 * <ul>
 *   <li>Counts the number of matching rules</li>
 *   <li>Always returns an Integer value</li>
 *   <li>Does not process or validate output values (only counts rules)</li>
 *   <li>Returns 0 if no rules match (handled by parent class returning null)</li>
 * </ul>
 *
 * <p><strong>Difference to other COLLECT aggregators:</strong></p>
 * <ul>
 *   <li><strong>COUNT vs SUM:</strong> COUNT counts rules; SUM adds output values</li>
 *   <li><strong>COUNT vs MIN/MAX:</strong> COUNT ignores output values; MIN/MAX compare them</li>
 * </ul>
 *
 * <p><strong>Implementation Note:</strong> This handler overrides {@link #aggregateValues}
 * to bypass type detection and conversion, since it only needs to count rules, not process values.</p>
 *
 * @see AbstractCollectNumberHitPolicyHandler
 * @see CollectHitPolicyHandler
 */
public class CollectCountHitPolicyHandler extends AbstractCollectNumberHitPolicyHandler {

  protected static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.COUNT);

  /**
   * Retrieves the hit policy entry for the COLLECT COUNT hit policy.
   *
   * @return the hit policy entry defining COLLECT with COUNT aggregator
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

  /**
   * Returns the COUNT aggregator.
   *
   * @return {@link BuiltinAggregator#COUNT}
   */
  @Override
  protected BuiltinAggregator getAggregator() {
    return BuiltinAggregator.COUNT;
  }

  /**
   * Aggregates values by counting them.
   *
   * <p>This method overrides the parent implementation to simply return the count
   * of values without type detection or conversion.</p>
   *
   * @param values the list of values to count
   * @return an Integer typed value containing the count
   */
  @Override
  protected TypedValue aggregateValues(List<TypedValue> values) {
    return Variables.integerValue(values.size());
  }

  /**
   * Not used by COUNT aggregator (overridden in {@link #aggregateValues}).
   *
   * @param intValues unused
   * @return always returns 0
   */
  @Override
  protected Integer aggregateIntegerValues(List<Integer> intValues) {
    // not used
    return 0;
  }

  /**
   * Not used by COUNT aggregator (overridden in {@link #aggregateValues}).
   *
   * @param longValues unused
   * @return always returns 0L
   */
  @Override
  protected Long aggregateLongValues(List<Long> longValues) {
    // not used
    return 0L;
  }

  /**
   * Not used by COUNT aggregator (overridden in {@link #aggregateValues}).
   *
   * @param doubleValues unused
   * @return always returns 0.0
   */
  @Override
  protected Double aggregateDoubleValues(List<Double> doubleValues) {
    // not used
    return 0.0;
  }

  /**
   * Returns a string representation of the CollectCountHitPolicyHandler.
   *
   * @return a string that represents this handler instance with its hit policy and aggregator
   */
  @Override
  public String toString() {
    return "CollectCountHitPolicyHandler{hitPolicy=" + HIT_POLICY.getHitPolicy() + ", aggregator=" + HIT_POLICY.getAggregator() + "}";
  }

}
