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

public class CollectMaxHitPolicyHandler extends AbstractCollectNumberHitPolicyHandler {
  protected static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.MAX);

    /**
   * Returns the HitPolicyEntry constant value.
   *
   * @return the HitPolicyEntry constant value
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

    /**
   * Returns the MAX aggregator for built-in aggregators.
   */
  protected BuiltinAggregator getAggregator() {
    return BuiltinAggregator.MAX;
  }

    /**
   * Returns the maximum value from a list of Integer values.
   * 
   * @param intValues the list of Integer values to aggregate
   * @return the maximum Integer value in the list
   */
  @Override
  protected Integer aggregateIntegerValues(List<Integer> intValues) {
    return Collections.max(intValues);
  }

    /**
   * Returns the maximum value from a list of Long values.
   * 
   * @param longValues the list of Long values to find the maximum from
   * @return the maximum Long value in the list
   */
  @Override
  protected Long aggregateLongValues(List<Long> longValues) {
    return Collections.max(longValues);
  }

    /**
   * Returns the maximum value from a list of Double values.
   * 
   * @param doubleValues the list of Double values to aggregate
   * @return the maximum value from the list of Double values
   */
  @Override
  protected Double aggregateDoubleValues(List<Double> doubleValues) {
    return Collections.max(doubleValues);
  }

    /**
   * Returns a string representation of the CollectMaxHitPolicyHandler object.
   */
  @Override
  public String toString() {
    return "CollectMaxHitPolicyHandler{}";
  }

}
