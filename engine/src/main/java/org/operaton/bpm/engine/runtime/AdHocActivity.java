/*
 * Copyright 2026 the Operaton contributors.
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
package org.operaton.bpm.engine.runtime;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Metadata for an activity that can currently be started in an active ad-hoc subprocess.
 *
 * @since 2.2
 */
public @NullMarked interface AdHocActivity {

  /**
   * Returns the BPMN activity id.
   */
  String getActivityId();

  /**
   * Returns the BPMN activity name, or {@code null} if none is declared.
   */
  @Nullable String getActivityName();

  /**
   * Returns the BPMN activity type, for example {@code userTask}.
   */
  String getActivityType();

  /**
   * Whether this activity can start without consuming an incoming-flow token.
   * @since 2.2
   */
  default boolean isStarterActivity() {
    return false;
  }

  /**
   * Persisted enabled token execution IDs, in deterministic execution-ID order.
   * Activation selects the first token eligible under the current ordering constraints.
   * Execution IDs may be reused after activation, for example by a loop.
   * An empty list can still represent an available starter activity.
   * @since 2.2
   */
  default List<String> getEnabledExecutionIds() {
    return List.of();
  }

}
