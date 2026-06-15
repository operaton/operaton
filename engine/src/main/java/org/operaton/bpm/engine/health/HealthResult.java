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
package org.operaton.bpm.engine.health;

import java.time.Instant;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * Immutable value object describing Operaton health.
 *
 * @author <a href="mailto:tomnm77@gmail.com">Tomasz Korcz</a>
 * @since 2.1
 */
public record HealthResult(String status, String timestamp, String version, Map<String, Object> details) {

  public HealthResult {
    requireNonNull(status, "status must not be null");
    timestamp = timestamp != null ? timestamp : Instant.now().toString();
    details = details != null ? unmodifiableMap(details) : emptyMap();
  }
}