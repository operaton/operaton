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

import org.operaton.bpm.model.dmn.BuiltinAggregator;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * Service Provider Interface (SPI) for registering and retrieving DMN hit policy handlers.
 *
 * <p>A hit policy handler registry manages the mapping between hit policy configurations
 * (hit policy type + optional aggregator) and their corresponding handler implementations.
 * This allows for flexible registration of custom handlers and retrieval of appropriate
 * handlers during decision table evaluation.</p>
 *
 * <p><strong>Supported Combinations:</strong></p>
 * <ul>
 *   <li><strong>Single Hit Policies:</strong> UNIQUE, FIRST, PRIORITY, ANY (aggregator is null)</li>
 *   <li><strong>Multiple Hit Policies:</strong> RULE_ORDER, OUTPUT_ORDER (aggregator is null)</li>
 *   <li><strong>COLLECT without Aggregation:</strong> COLLECT with null aggregator</li>
 *   <li><strong>COLLECT with Aggregation:</strong> COLLECT with COUNT, SUM, MIN, or MAX aggregator</li>
 * </ul>
 *
 * @see DmnHitPolicyHandler
 * @see org.operaton.bpm.dmn.engine.impl.hitpolicy.DefaultHitPolicyHandlerRegistry
 */
public interface DmnHitPolicyHandlerRegistry {

  /**
   * Retrieves the hit policy handler for a specific hit policy and aggregator combination.
   *
   * <p>The registry uses both the hit policy and the optional aggregator to determine the
   * correct handler. For example:</p>
   * <ul>
   *   <li>UNIQUE policy with null aggregator returns {@code UniqueHitPolicyHandler}</li>
   *   <li>COLLECT policy with null aggregator returns {@code CollectHitPolicyHandler}</li>
   *   <li>COLLECT policy with SUM aggregator returns {@code CollectSumHitPolicyHandler}</li>
   * </ul>
   *
   * @param hitPolicy the hit policy type (e.g., UNIQUE, FIRST, COLLECT)
   * @param builtinAggregator the optional aggregator (e.g., SUM, MIN, MAX) or null if not applicable
   * @return the handler registered for this combination, or null if no handler is registered
   */
  DmnHitPolicyHandler getHandler(HitPolicy hitPolicy, BuiltinAggregator builtinAggregator);

  /**
   * Registers a hit policy handler for a specific hit policy and aggregator combination.
   *
   * <p>This method allows custom hit policy handlers to be registered or existing handlers
   * to be replaced. The handler will be returned by {@link #getHandler} for the specified
   * hit policy and aggregator combination.</p>
   *
   * <p><strong>Note:</strong> Registering a handler for an existing combination will replace
   * the previous handler.</p>
   *
   * @param hitPolicy the hit policy type to register the handler for
   * @param builtinAggregator the optional aggregator or null if not applicable
   * @param hitPolicyHandler the hit policy handler implementation to register
   */
  void addHandler(HitPolicy hitPolicy, BuiltinAggregator builtinAggregator, DmnHitPolicyHandler hitPolicyHandler);

}
