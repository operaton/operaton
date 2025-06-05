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

import javax.script.CompiledScript;

/**
 * Interface for managing cached compiled scripts with thread-safe access.
 * <p>
 * This interface extends {@link LockSupport} to provide locking mechanisms
 * for safely caching and retrieving compiled scripts in a concurrent environment.
 * It is designed to optimize script execution by reusing precompiled scripts.
 * </p>
 * <p>
 * Implementations of this interface should ensure proper synchronization
 * when accessing or modifying the cached script.
 * </p>
 * Author: Daniel Meyer
 */
public interface CachedCompiledScriptSupport extends LockSupport {

  /**
   * Caches the given compiled script.
   *
   * @param compiledScript the {@link CompiledScript} to be cached.
   *                       This script will be stored for reuse in subsequent evaluations.
   */
  void cacheCompiledScript(CompiledScript compiledScript);

  /**
   * Retrieves the cached compiled script.
   *
   * @return the cached {@link CompiledScript}, or {@code null} if no script is cached.
   */
  CompiledScript getCachedCompiledScript();

}
