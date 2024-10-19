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

import java.util.Collections;
import java.util.List;

import org.operaton.bpm.model.dmn.BuiltinAggregator;
import org.operaton.bpm.model.dmn.HitPolicy;

public class CollectMinHitPolicyHandler extends AbstractCollectNumberHitPolicyHandler {
  protected static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.MIN);

    /**
   * Returns the Hit Policy Entry of the class.
   *
   * @return the Hit Policy Entry
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

    /**
   * Returns the built-in aggregator MIN.
   *
   * @return the built-in aggregator MIN
   */
  protected BuiltinAggregator getAggregator() {
    return BuiltinAggregator.MIN;
  }

    /**
   * This method returns the minimum value from a list of Integer values.
   *
   * @param intValues the list of Integer values to find the minimum from
   * @return the minimum Integer value from the list
   */
  @Override
  protected Integer aggregateIntegerValues(List<Integer> intValues) {
    return Collections.min(intValues);
  }

    /**
   * Returns the minimum value from the list of Long values.
   *
   * @param longValues the list of Long values to be aggregated
   * @return the minimum value from the list of Long values
   */
  @Override
  protected Long aggregateLongValues(List<Long> longValues) {
    return Collections.min(longValues);
  }

    /**
   * Returns the minimum value from the list of Double values.
   *
   * @param doubleValues the list of Double values to be aggregated
   * @return the minimum value from the list of Double values
   */
  @Override
  protected Double aggregateDoubleValues(List<Double> doubleValues) {
    return Collections.min(doubleValues);
  }

    /**
   * Returns a string representation of the CollectMinHitPolicyHandler object.
   */
  @Override
  public String toString() {
    return "CollectMinHitPolicyHandler{}";
  }

}
