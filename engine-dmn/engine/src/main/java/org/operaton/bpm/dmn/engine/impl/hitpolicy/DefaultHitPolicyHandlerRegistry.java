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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandler;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandlerRegistry;
import org.operaton.bpm.model.dmn.BuiltinAggregator;
import org.operaton.bpm.model.dmn.HitPolicy;

/**
 * Default implementation of the {@link DmnHitPolicyHandlerRegistry} that provides
 * pre-configured handlers for all standard DMN 1.3 hit policies.
 *
 * <p>This registry contains handlers for:</p>
 * <ul>
 *   <li><strong>Single Hit Policies:</strong>
 *     <ul>
 *       <li>UNIQUE - Only one rule can match, enforced at evaluation time</li>
 *       <li>FIRST - Returns the first matching rule in table order</li>
 *       <li>PRIORITY - Returns the highest-priority rule based on output values</li>
 *       <li>ANY - Returns any matching rule (all must have equal outputs)</li>
 *     </ul>
 *   </li>
 *   <li><strong>Multiple Hit Policies:</strong>
 *     <ul>
 *       <li>RULE_ORDER - Returns all matching rules in table order</li>
 *       <li>OUTPUT_ORDER - Returns all matching rules sorted by output values</li>
 *       <li>COLLECT - Returns all matching rules without specific ordering</li>
 *     </ul>
 *   </li>
 *   <li><strong>Aggregation Functions (COLLECT only):</strong>
 *     <ul>
 *       <li>COUNT - Counts the number of matching rules</li>
 *       <li>SUM - Sums numeric output values</li>
 *       <li>MIN - Returns the minimum numeric output value</li>
 *       <li>MAX - Returns the maximum numeric output value</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This implementation uses a {@link ConcurrentHashMap} for thread-safe
 * handler registration and retrieval. Multiple threads can safely call {@link #getHandler} and
 * {@link #addHandler} concurrently.</p>
 *
 * @see DmnHitPolicyHandlerRegistry
 * @see DmnHitPolicyHandler
 */
public class DefaultHitPolicyHandlerRegistry implements DmnHitPolicyHandlerRegistry {

  protected static final Map<HitPolicyEntry, DmnHitPolicyHandler> handlers = new ConcurrentHashMap<>(getDefaultHandlers());

  /**
   * Creates and initializes the map of default hit policy handlers.
   *
   * <p>This method registers all standard DMN 1.3 hit policy handlers according to the
   * specification. The handlers are instantiated once and reused for all evaluations.</p>
   *
   * @return a map containing all default hit policy handler registrations
   */
  protected static Map<HitPolicyEntry, DmnHitPolicyHandler> getDefaultHandlers() {
    Map<HitPolicyEntry, DmnHitPolicyHandler> defaultHandlers = new HashMap<>();

    // Single hit policies
    defaultHandlers.put(new HitPolicyEntry(HitPolicy.UNIQUE, null), new UniqueHitPolicyHandler());
    defaultHandlers.put(new HitPolicyEntry(HitPolicy.FIRST, null), new FirstHitPolicyHandler());
    defaultHandlers.put(new HitPolicyEntry(HitPolicy.ANY, null), new AnyHitPolicyHandler());

    // Multiple hit policies with sorting
    defaultHandlers.put(new HitPolicyEntry(HitPolicy.RULE_ORDER, null), new RuleOrderHitPolicyHandler());
    defaultHandlers.put(new HitPolicyEntry(HitPolicy.OUTPUT_ORDER, null), new OutputOrderHitPolicyHandler());
    defaultHandlers.put(new HitPolicyEntry(HitPolicy.PRIORITY, null), new PriorityHitPolicyHandler());

    // COLLECT policy without aggregation
    defaultHandlers.put(new HitPolicyEntry(HitPolicy.COLLECT, null), new CollectHitPolicyHandler());

    // COLLECT policy with aggregation
    defaultHandlers.put(new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.COUNT), new CollectCountHitPolicyHandler());
    defaultHandlers.put(new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.SUM), new CollectSumHitPolicyHandler());
    defaultHandlers.put(new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.MIN), new CollectMinHitPolicyHandler());
    defaultHandlers.put(new HitPolicyEntry(HitPolicy.COLLECT, BuiltinAggregator.MAX), new CollectMaxHitPolicyHandler());

    return defaultHandlers;
  }

  /**
   * Retrieves the hit policy handler for the specified hit policy and aggregator combination.
   *
   * @param hitPolicy the hit policy to look up, must not be null
   * @param builtinAggregator the aggregator to look up, or null for policies without aggregation
   * @return the registered handler, or null if no handler is found for this combination
   * @throws NullPointerException if hitPolicy is null
   */
  @Override
  public DmnHitPolicyHandler getHandler(HitPolicy hitPolicy, BuiltinAggregator builtinAggregator) {
    return handlers.get(new HitPolicyEntry(hitPolicy, builtinAggregator));
  }

  /**
   * Registers a custom hit policy handler or replaces an existing one.
   *
   * <p><strong>Thread Safety:</strong> This method is thread-safe and can be called concurrently
   * from multiple threads.</p>
   *
   * @param hitPolicy the hit policy to register the handler for, must not be null
   * @param builtinAggregator the aggregator, or null for policies without aggregation
   * @param hitPolicyHandler the handler implementation to register, must not be null
   * @throws NullPointerException if hitPolicy or hitPolicyHandler is null
   */
  @Override
  public void addHandler(HitPolicy hitPolicy, BuiltinAggregator builtinAggregator, DmnHitPolicyHandler hitPolicyHandler) {
    Objects.requireNonNull(hitPolicyHandler, "hitPolicyHandler must not be null");
    handlers.put(new HitPolicyEntry(hitPolicy, builtinAggregator), hitPolicyHandler);
  }

}
