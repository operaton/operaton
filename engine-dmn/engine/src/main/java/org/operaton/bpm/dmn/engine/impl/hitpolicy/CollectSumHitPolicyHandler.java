/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

public class CollectSumHitPolicyHandler extends AbstractCollectNumberHitPolicyHandler {
  protected static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.SUM);

    /**
   * Returns the HitPolicyEntry object representing the hit policy.
   *
   * @return the HitPolicyEntry object representing the hit policy
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

    /**
   * Returns the built-in aggregator SUM.
   *
   * @return the built-in aggregator SUM
   */
  protected BuiltinAggregator getAggregator() {
    return BuiltinAggregator.SUM;
  }

    /**
   * Aggregates a list of Integer values by summing them up.
   * 
   * @param intValues the list of Integer values to be aggregated
   * @return the sum of all non-null Integer values in the list
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
   * Aggregates a list of Long values by summing them up.
   * 
   * @param longValues the list of Long values to be aggregated
   * @return the aggregated sum of the Long values
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
   * Aggregate a list of Double values by summing them up.
   * 
   * @param doubleValues the list of Double values to be aggregated
   * @return the sum of the Double values in the list
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
   * Returns a string representation of the CollectSumHitPolicyHandler object.
   */
  @Override
  public String toString() {
    return "CollectSumHitPolicyHandler{}";
  }

}
