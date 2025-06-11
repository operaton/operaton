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

import java.util.concurrent.Callable;

/**
 * Interface for lock support functionality.
 * <p>
 * This interface provides a method to execute a task within a lock, ensuring
 * thread-safe execution of critical sections. Implementations of this interface
 * should handle the locking mechanism and guarantee that the lock is properly
 * released after execution.
 * </p>
 */
public interface LockSupport {

  /**
   * Executes the given task within a lock.
   *
   * @param <T> The type of the result returned by the task.
   * @param callable The task to be executed. This task should implement the
   *                 {@link Callable} interface.
   * @return The result of the task execution.
   * @throws IllegalStateException If the task execution throws an exception, it will be propagated.
   */
  <T> T executeWithLock(Callable<T> callable);

}
