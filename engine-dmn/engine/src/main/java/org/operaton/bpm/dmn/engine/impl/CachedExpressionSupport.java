/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.dmn.engine.impl;

import org.operaton.bpm.dmn.engine.impl.spi.el.ElExpression;

/**
 * Interface for managing cached EL (Expression Language) expressions with thread-safe access.
 * <p>
 * This interface extends {@link LockSupport} to provide locking mechanisms
 * for safely caching and retrieving EL expressions in a concurrent environment.
 * It is designed to optimize expression evaluation by reusing pre-parsed expressions.
 * </p>
 * <p>
 * Implementations of this interface should ensure proper synchronization
 * when accessing or modifying the cached expression.
 * </p>
 * Author: Daniel Meyer
 */
public interface CachedExpressionSupport extends LockSupport {

  /**
   * Caches the given EL expression.
   *
   * @param expression the {@link ElExpression} to be cached.
   *                   This expression will be stored for reuse in subsequent evaluations.
   */
  void setCachedExpression(ElExpression expression);

  /**
   * Retrieves the cached EL expression.
   *
   * @return the cached {@link ElExpression}, or {@code null} if no expression is cached.
   */
  ElExpression getCachedExpression();

}
