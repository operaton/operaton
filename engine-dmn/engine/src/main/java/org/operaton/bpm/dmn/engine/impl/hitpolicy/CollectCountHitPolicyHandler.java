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

import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.operaton.bpm.model.dmn.BuiltinAggregator;
import org.operaton.bpm.model.dmn.HitPolicy;

import java.util.List;

public class CollectCountHitPolicyHandler extends AbstractCollectNumberHitPolicyHandler {

  protected static final HitPolicyEntry HIT_POLICY = new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.COUNT);

    /**
   * Returns the HitPolicyEntry constant for this class.
   *
   * @return the HitPolicyEntry constant
   */
  @Override
  public HitPolicyEntry getHitPolicyEntry() {
    return HIT_POLICY;
  }

    /**
   * Returns the built-in aggregator COUNT.
   * 
   * @return the built-in aggregator COUNT
   */
  @Override
  protected BuiltinAggregator getAggregator() {
    return BuiltinAggregator.COUNT;
  }

    /**
   * Aggregates a list of TypedValue objects by returning the total number of elements in the list.
   *
   * @param values the list of TypedValue objects to be aggregated
   * @return a TypedValue object representing the total number of elements in the list
   */
  @Override
  protected TypedValue aggregateValues(List<TypedValue> values) {
    return Variables.integerValue(values.size());
  }

    /**
   * This method aggregates a list of Integer values, but it always returns 0 as it is not used.
   * 
   * @param intValues a list of Integer values to aggregate
   * @return 0
   */
  @Override
  protected Integer aggregateIntegerValues(List<Integer> intValues) {
    // not used
    return 0;
  }

    /**
   * This method aggregates a list of Long values. 
   * However, it is not used and always returns 0L.
   * 
   * @param longValues the list of Long values to aggregate
   * @return the aggregated Long value, which is always 0L
   */
  @Override
  protected Long aggregateLongValues(List<Long> longValues) {
    // not used
    return 0L;
  }

    /**
   * This method aggregates a list of Double values, but it is not used in the current implementation.
   * Returns 0.0.
   */
  @Override
  protected Double aggregateDoubleValues(List<Double> doubleValues) {
    // not used
    return 0.0;
  }

    /**
   * Returns a string representation of the CollectCountHitPolicyHandler object.
   * 
   * @return a string representing the CollectCountHitPolicyHandler object
   */
  @Override
  public String toString() {
    return "CollectCountHitPolicyHandler{}";
  }

}
