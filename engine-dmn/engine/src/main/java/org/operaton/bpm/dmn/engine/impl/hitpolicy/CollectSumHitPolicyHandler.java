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

import org.operaton.bpm.model.dmn.BuiltinAggregator;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * Implements the COLLECT hit policy with SUM aggregation as defined in the DMN 1.3 specification (section 8.2.8).
 *
 * <p>The SUM aggregator calculates the sum of all numeric output values from matching rules.
 * It supports Integer, Long, and Double types, automatically detecting the appropriate type
 * and performing the summation accordingly.</p>
 *
 * <p><strong>Key Characteristics:</strong></p>
 * <ul>
 *   <li>Sums all numeric output values from matching rules</li>
 *   <li>Supports Integer, Long, and Double types</li>
 *   <li>Null values are skipped (treated as 0 for summation purposes)</li>
 *   <li>Result type matches the detected input type</li>
 *   <li>Requires single output (not supported for compound outputs)</li>
 * </ul>
 *
 * <p><strong>Type Handling:</strong></p>
 * <ul>
 *   <li><strong>All Integers:</strong> Returns Integer sum</li>
 *   <li><strong>Integers and Longs:</strong> Returns Long sum</li>
 *   <li><strong>Mixed numeric types:</strong> Returns Double sum</li>
 * </ul>
 *
 * <p><strong>Difference to other COLLECT aggregators:</strong></p>
 * <ul>
 *   <li><strong>SUM vs COUNT:</strong> SUM adds values; COUNT counts rules</li>
 *   <li><strong>SUM vs MIN/MAX:</strong> SUM accumulates; MIN/MAX select single value</li>
 * </ul>
 *
 * @see AbstractCollectNumberHitPolicyHandler
 * @see CollectHitPolicyHandler
 */
public class CollectSumHitPolicyHandler extends AbstractCollectNumberHitPolicyHandler {
  protected static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.SUM);

  /**
   * Retrieves the hit policy entry for the COLLECT SUM hit policy.
   *
   * @return the hit policy entry defining COLLECT with SUM aggregator
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

  /**
   * Returns the SUM aggregator.
   *
   * @return {@link BuiltinAggregator#SUM}
   */
  @Override
  protected BuiltinAggregator getAggregator() {
    return BuiltinAggregator.SUM;
  }

  /**
   * Aggregates integer values by summing them.
   *
   * <p>Null values are treated as 0 and do not contribute to the sum.</p>
   *
   * @param intValues the integer values to sum
   * @return the sum of all non-null integer values
   */
  @Override
  protected Integer aggregateIntegerValues(List<Integer> intValues) {
    int sum = 0;
    for (Integer intValue : intValues) {
      if (intValue != null) {
        sum += intValue;
      }
    }
    return sum;
  }

  /**
   * Aggregates long values by summing them.
   *
   * <p>Null values are treated as 0 and do not contribute to the sum.</p>
   *
   * @param longValues the long values to sum
   * @return the sum of all non-null long values
   */
  @Override
  protected Long aggregateLongValues(List<Long> longValues) {
    long sum = 0L;
    for (Long longValue : longValues) {
      if (longValue != null) {
        sum += longValue;
      }
    }
    return sum;
  }

  /**
   * Aggregates double values by summing them.
   *
   * <p>Null values are treated as 0.0 and do not contribute to the sum.</p>
   *
   * @param doubleValues the double values to sum
   * @return the sum of all non-null double values
   */
  @Override
  protected Double aggregateDoubleValues(List<Double> doubleValues) {
    double sum = 0.0;
    for (Double doubleValue : doubleValues) {
      if (doubleValue != null) {
        sum += doubleValue;
      }
    }
    return sum;
  }

  /**
   * Returns a string representation of the CollectSumHitPolicyHandler.
   *
   * @return a string that represents this handler instance with its hit policy and aggregator
   */
  @Override
  public String toString() {
    return "CollectSumHitPolicyHandler{hitPolicy=" + HIT_POLICY.getHitPolicy() + ", aggregator=" + HIT_POLICY.getAggregator() + "}";
  }

}
